package com.uqlism.emoji_deco.text.fetch;

import com.mojang.logging.LogUtils;
import com.uqlism.emoji_deco.render.image.BinarySource;
import com.uqlism.emoji_deco.render.image.source.UrlAllowlist;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * decode_str 用の非同期テキスト取得キャッシュ。
 * レンダースレッドから getNow() を呼ぶと、未取得なら裏でフェッチを開始し null を返す。
 * フェッチ完了後は TTL が切れるまでキャッシュから即返す。
 */
public final class TextFetchManager {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_BYTES         = 1024 * 1024;
    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS   = 30_000;
    private static final long RETRY_DELAY_MS   = 30_000;
    private static final int MAX_RETRIES       = 3;

    private static final AtomicInteger COUNTER = new AtomicInteger();
    private static final ClassLoader MOD_CL = TextFetchManager.class.getClassLoader();
    private static final java.util.concurrent.Executor EXECUTOR =
            new ThreadPoolExecutor(0, 8, 60L, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(),
                    r -> {
                        Thread t = new Thread(r, "EmojiDeco-TextFetch-" + COUNTER.getAndIncrement());
                        t.setDaemon(true);
                        t.setContextClassLoader(MOD_CL);
                        return t;
                    });

    private static final class CacheEntry {
        final CompletableFuture<byte[]> future;
        final long expiresAt;
        volatile long    retryAfter = Long.MAX_VALUE;
        volatile int     failCount  = 0;
        volatile byte[]  lastBytes;  // 再フェッチ中も前回の結果を保持する

        CacheEntry(CompletableFuture<byte[]> future, long expiresAt, @Nullable byte[] prevBytes) {
            this.future    = future;
            this.expiresAt = expiresAt;
            this.lastBytes = prevBytes;
        }

        boolean isStale() {
            if (expiresAt != Long.MAX_VALUE && System.currentTimeMillis() > expiresAt) return true;
            return future.isCompletedExceptionally()
                    && failCount <= MAX_RETRIES
                    && System.currentTimeMillis() >= retryAfter;
        }
    }

    private static final Map<BinarySource.Url, CacheEntry> CACHE = new ConcurrentHashMap<>();

    /**
     * レンダースレッドから安全に呼べる同期メソッド。
     * フェッチ完了済みならバイト列を即返す。未完了・失敗中は null を返し、裏でフェッチを走らせる。
     */
    public static @Nullable byte[] getNow(BinarySource.Url source) {
        String url = source.url();
        if (!url.startsWith("http://") && !url.startsWith("https://")) return null;
        if (!UrlAllowlist.INSTANCE.isAllowed(url)) {
            LOGGER.warn("[EmojiDeco] テキストフェッチ拒否 (allowlist 未登録): {}", url);
            return null;
        }

        long expiresAt = source.ttlSeconds() > 0
                ? System.currentTimeMillis() + (long) source.ttlSeconds() * 1000
                : Long.MAX_VALUE;

        CacheEntry entry = CACHE.compute(source, (k, existing) -> {
            if (existing != null && !existing.isStale()) return existing;
            int    prevFails = existing != null ? existing.failCount  : 0;
            byte[] prevBytes = existing != null ? existing.lastBytes  : null;
            CacheEntry e = new CacheEntry(fetchAsync(source), expiresAt, prevBytes);
            e.future.whenComplete((r, t) -> {
                if (t == null) {
                    e.lastBytes = r;
                } else {
                    e.failCount = prevFails + 1;
                    if (e.failCount <= MAX_RETRIES) {
                        LOGGER.warn("[EmojiDeco] テキストフェッチ リトライ {}/{} を {}s 後に予定 [{}]",
                                e.failCount, MAX_RETRIES, RETRY_DELAY_MS / 1000, url);
                        e.retryAfter = System.currentTimeMillis() + RETRY_DELAY_MS;
                    } else {
                        LOGGER.error("[EmojiDeco] テキストフェッチ リトライ上限({})到達 [{}]", MAX_RETRIES, url);
                    }
                }
            });
            return e;
        });

        // lastBytes が null かつ future が成功済みの場合（whenComplete 到着前の僅かな窓）は直接取得
        byte[] b = entry.lastBytes;
        if (b == null && entry.future.isDone() && !entry.future.isCompletedExceptionally()) {
            try { return entry.future.get(); } catch (Exception e) { return null; }
        }
        return b;
    }

    private static CompletableFuture<byte[]> fetchAsync(BinarySource.Url source) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return download(source);
            } catch (Exception e) {
                LOGGER.error("[EmojiDeco] テキストフェッチ失敗 [{}]: {}", source.url(), e.toString());
                throw new RuntimeException(e);
            }
        }, EXECUTOR);
    }

    private static byte[] download(BinarySource.Url source) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(source.url()).openConnection();
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        conn.setRequestProperty("User-Agent", "EmojiDeco/1.0");
        conn.setRequestMethod(source.method().toUpperCase());
        for (var h : source.headers().entrySet())
            conn.setRequestProperty(h.getKey(), h.getValue());

        String body = source.body();
        if (body != null && !body.isEmpty()) {
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }
        }

        int code = conn.getResponseCode();
        if (code < 200 || code >= 300)
            throw new IOException("HTTP " + code + " for " + source.url());

        try (InputStream is = conn.getInputStream();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) != -1) {
                if (baos.size() + n > MAX_BYTES)
                    throw new IOException("レスポンスが " + MAX_BYTES + " バイト上限を超えました");
                baos.write(buf, 0, n);
            }
            return baos.toByteArray();
        }
    }

    private TextFetchManager() {}
}
