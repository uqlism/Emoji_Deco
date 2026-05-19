package com.uqlism.emoji_deco.network;

import com.mojang.logging.LogUtils;
import com.uqlism.emoji_deco.render.image.BinarySource;
import com.uqlism.emoji_deco.render.image.source.UrlAllowlist;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * URL バイト取得の共通インフラ。メモリキャッシュ・TTL・オプションのディスクキャッシュ・
 * リトライロジック・allowlist チェックをここに集約する。
 *
 * 返り値は byte[] のみ。デコードや文字列変換は呼び出し側（TextFetchManager, UrlSourceResolver）が行う。
 */
public final class UrlFetcher {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int  CONNECT_TIMEOUT_MS = 5_000;
    private static final int  READ_TIMEOUT_MS    = 30_000;
    private static final long RETRY_DELAY_MS     = 30_000;
    private static final int  MAX_RETRIES        = 3;

    private static final AtomicInteger COUNTER = new AtomicInteger();
    private static final ClassLoader MOD_CL = UrlFetcher.class.getClassLoader();
    private static final Executor EXECUTOR = new ThreadPoolExecutor(
            0, 16, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(),
            r -> {
                Thread t = new Thread(r, "EmojiDeco-Fetch-" + COUNTER.getAndIncrement());
                t.setDaemon(true);
                t.setContextClassLoader(MOD_CL);
                return t;
            });

    private static final class CacheEntry {
        final CompletableFuture<byte[]> future;
        final long expiresAt;
        volatile long retryAfter = Long.MAX_VALUE;
        volatile int  failCount  = 0;

        CacheEntry(CompletableFuture<byte[]> future, long expiresAt) {
            this.future    = future;
            this.expiresAt = expiresAt;
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
     * 指定ソースのバイト列を非同期取得する。結果はキャッシュし、TTL が切れるまで再利用する。
     * allowlist 未登録または http(s) 以外の場合は即失敗 Future を返す。
     */
    public static CompletableFuture<byte[]> fetch(BinarySource.Url source, int maxBytes) {
        String url = source.url();
        if (!url.startsWith("http://") && !url.startsWith("https://"))
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("Only http/https URLs are allowed: " + url));
        if (!UrlAllowlist.INSTANCE.isAllowed(url)) {
            LOGGER.warn("[EmojiDeco] URL フェッチ拒否 (allowlist 未登録): {}", url);
            return CompletableFuture.failedFuture(new SecurityException("URL not permitted: " + url));
        }

        long expiresAt = source.ttlSeconds() > 0
                ? System.currentTimeMillis() + (long) source.ttlSeconds() * 1000
                : Long.MAX_VALUE;

        return CACHE.compute(source, (k, existing) -> {
            if (existing != null && !existing.isStale()) return existing;
            int prevFails = existing != null ? existing.failCount : 0;
            CacheEntry entry = new CacheEntry(download(source, maxBytes), expiresAt);
            entry.future.whenComplete((r, t) -> {
                if (t != null) {
                    entry.failCount = prevFails + 1;
                    if (entry.failCount <= MAX_RETRIES) {
                        LOGGER.warn("[EmojiDeco] リトライ {}/{} を {}s 後に予定 [{}]",
                                entry.failCount, MAX_RETRIES, RETRY_DELAY_MS / 1000, url);
                        entry.retryAfter = System.currentTimeMillis() + RETRY_DELAY_MS;
                    } else {
                        LOGGER.error("[EmojiDeco] リトライ上限({})到達、永続的に失敗 [{}]",
                                MAX_RETRIES, url);
                    }
                }
            });
            return entry;
        }).future;
    }

    /** キャッシュエントリが stale かどうかを返す。未登録の場合は false。 */
    public static boolean isStale(BinarySource.Url source) {
        CacheEntry entry = CACHE.get(source);
        return entry != null && entry.isStale();
    }

    // ── ダウンロード ─────────────────────────────────────────────────────────────

    private static CompletableFuture<byte[]> download(BinarySource.Url source, int maxBytes) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (source.diskCache()) {
                    Path file = diskCacheFile(source);
                    if (Files.exists(file) && !isDiskExpired(file, source.ttlSeconds()))
                        return Files.readAllBytes(file);
                    byte[] bytes = httpDownload(source, maxBytes);
                    writeDiskCache(file, bytes);
                    return bytes;
                }
                return httpDownload(source, maxBytes);
            } catch (Exception e) {
                LOGGER.error("[EmojiDeco] フェッチ失敗 [{}]: {}", source.url(), e.toString(), e);
                throw new RuntimeException(e);
            }
        }, EXECUTOR);
    }

    private static byte[] httpDownload(BinarySource.Url source, int maxBytes) throws IOException {
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
                if (baos.size() + n > maxBytes)
                    throw new IOException("レスポンスが " + maxBytes + " バイト上限を超えました");
                baos.write(buf, 0, n);
            }
            return baos.toByteArray();
        }
    }

    // ── disk cache ───────────────────────────────────────────────────────────────

    private static Path diskCacheFile(BinarySource.Url source) {
        // キャッシュキー: URL + メソッド + ボディ（HTTP レスポンスを決定する要素のみ）
        String key = source.url() + "\0" + source.method()
                + "\0" + (source.body() != null ? source.body() : "");
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(key.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 16; i++) sb.append(String.format("%02x", hash[i]));
            return cacheDir().resolve(sb.toString() + ".bin");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isDiskExpired(Path file, int ttlSeconds) {
        if (ttlSeconds <= 0) return false;
        try {
            long age = System.currentTimeMillis() - Files.getLastModifiedTime(file).toMillis();
            return age > (long) ttlSeconds * 1000;
        } catch (IOException e) {
            return true;
        }
    }

    private static void writeDiskCache(Path file, byte[] bytes) {
        try {
            Files.createDirectories(file.getParent());
            Files.write(file, bytes);
        } catch (IOException e) {
            LOGGER.warn("[EmojiDeco] ディスクキャッシュ書き込み失敗 {}: {}", file, e.getMessage());
        }
    }

    private static Path cacheDir() {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("emoji_deco").resolve("url_cache");
    }

    private UrlFetcher() {}
}
