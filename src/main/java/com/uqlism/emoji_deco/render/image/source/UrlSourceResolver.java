package com.uqlism.emoji_deco.render.image.source;

import com.mojang.logging.LogUtils;
import com.uqlism.emoji_deco.render.image.ImageDecoder;
import com.uqlism.emoji_deco.render.image.ImageResolver;
import com.uqlism.emoji_deco.render.image.ImageFormatDetector;
import com.uqlism.emoji_deco.render.image.ImageFormatDetector.Format;
import com.uqlism.emoji_deco.render.image.ResolvedSource;
import com.uqlism.emoji_deco.render.image.decoder.ApngDecoder;
import com.uqlism.emoji_deco.render.image.decoder.GifDecoder;
import com.uqlism.emoji_deco.render.image.decoder.StbDecoder;
import com.uqlism.emoji_deco.render.image.decoder.WebpDecoder;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class UrlSourceResolver {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_BYTES         = 2 * 1024 * 1024;
    private static final int CONNECT_TIMEOUT_MS =  5_000;  // 接続確立は早く諦める
    private static final int READ_TIMEOUT_MS    = 30_000;  // データ受信は遅いサーバーに合わせて長め
    private static final AtomicInteger counter = new AtomicInteger(0);

    private static final ClassLoader MOD_CLASSLOADER = UrlSourceResolver.class.getClassLoader();
    /**
     * IO バウンドなダウンロード専用プール（最大 16 スレッド、60 秒アイドルで回収）。
     * SynchronousQueue により空きスレッドがあれば即実行、なければ新スレッドを生成し
     * 上限（16）に達したらキューで待機する。cachedThreadPool のような際限ない生成を防ぐ。
     * Java 17 では仮想スレッドが使えないが IO 待ちスレッドは CPU をほぼ消費しない。
     */
    private static final java.util.concurrent.Executor FETCH_EXECUTOR =
            new java.util.concurrent.ThreadPoolExecutor(
                    0, 16, 60L, java.util.concurrent.TimeUnit.SECONDS,
                    new java.util.concurrent.LinkedBlockingQueue<>(),
                    r -> {
                        Thread t = new Thread(r, "EmojiDeco-Fetch-" + counter.getAndIncrement());
                        t.setDaemon(true);
                        t.setContextClassLoader(MOD_CLASSLOADER);
                        return t;
                    });

    /** 失敗後にこの時間が経過すると再取得を試みる */
    private static final long RETRY_DELAY_MS = 30_000;
    /** 最大リトライ回数（初回失敗後に最大 MAX_RETRIES 回再試行する） */
    private static final int MAX_RETRIES = 3;

    private static final class CacheEntry {
        final CompletableFuture<ResolvedSource> future;
        final long expiresAt;
        volatile long retryAfter = Long.MAX_VALUE;
        volatile int failCount = 0; // この URL のこれまでの失敗回数

        CacheEntry(CompletableFuture<ResolvedSource> future, long expiresAt) {
            this.future   = future;
            this.expiresAt = expiresAt;
        }

        boolean isStale() {
            if (expiresAt != Long.MAX_VALUE && System.currentTimeMillis() > expiresAt) return true;
            // failCount <= MAX_RETRIES の間だけ retryAfter 到達でリトライを許可
            return future.isCompletedExceptionally()
                    && failCount <= MAX_RETRIES
                    && System.currentTimeMillis() >= retryAfter;
        }
    }

    private static final Map<String, CacheEntry> CACHE = new ConcurrentHashMap<>();

    /** GPU エビクション後の再デコードのため、指定 URL のキャッシュエントリを削除する。 */
    public static void invalidate(String url, @Nullable String format) {
        CACHE.remove(url + "\0" + (format != null ? format : ""));
    }

    public static CompletableFuture<ResolvedSource> resolve(
            String url, @Nullable String format, boolean diskCache, int ttlSeconds) {
        if (!url.startsWith("https://") && !url.startsWith("http://"))
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("Only http/https URLs are allowed"));
        if (!UrlAllowlist.INSTANCE.isAllowed(url)) {
            LOGGER.warn("[EmojiDeco] URL フェッチ拒否 (allowlist 未登録またはURLフェッチ無効): {}", url);
            return CompletableFuture.failedFuture(
                    new SecurityException("URL not permitted by allowlist: " + url));
        }

        String cacheKey = url + "\0" + (format != null ? format : "");
        long expiresAt = ttlSeconds > 0
                ? System.currentTimeMillis() + (long) ttlSeconds * 1000
                : Long.MAX_VALUE;

        return CACHE.compute(cacheKey, (k, existing) -> {
            if (existing != null && !existing.isStale()) return existing;
            int prevFails = (existing != null) ? existing.failCount : 0;
            CacheEntry entry = new CacheEntry(fetch(url, format, diskCache, ttlSeconds), expiresAt);
            entry.future.whenComplete((r, t) -> {
                if (t != null) {
                    entry.failCount = prevFails + 1;
                    if (entry.failCount <= MAX_RETRIES) {
                        LOGGER.warn("[EmojiDeco] リトライ {}/{} を {}s 後に予定 [{}]",
                                entry.failCount, MAX_RETRIES, RETRY_DELAY_MS / 1000, url);
                        entry.retryAfter = System.currentTimeMillis() + RETRY_DELAY_MS;
                    } else {
                        LOGGER.error("[EmojiDeco] リトライ上限({})到達、永続的に tofu [{}]",
                                MAX_RETRIES, url);
                    }
                }
            });
            return entry;
        }).future;
    }


    private static CompletableFuture<ResolvedSource> fetch(
            String url, @Nullable String format, boolean diskCache, int ttlSeconds) {
        // Stage 1: ダウンロード + デコード（FETCH_EXECUTOR スレッド）
        return CompletableFuture.supplyAsync(() -> {
            try {
                byte[] bytes;
                String contentTypeHint = null;

                if (diskCache) {
                    Path cacheFile = diskCacheFile(url, format);
                    if (Files.exists(cacheFile) && !isDiskExpired(cacheFile, ttlSeconds)) {
                        bytes = Files.readAllBytes(cacheFile);
                    } else {
                        DownloadResult dl = download(url);
                        bytes = dl.bytes();
                        contentTypeHint = dl.contentType();
                        writeDiskCache(cacheFile, bytes);
                    }
                } else {
                    DownloadResult dl = download(url);
                    bytes = dl.bytes();
                    contentTypeHint = dl.contentType();
                }

                String hint = format != null ? format : contentTypeHint;
                Format fmt = ImageFormatDetector.detect(bytes, hint);
                ImageDecoder decoder = switch (fmt) {
                    case GIF  -> new GifDecoder();
                    case WEBP -> new WebpDecoder();
                    case PNG, APNG -> new ApngDecoder();
                    default   -> new StbDecoder();
                };
                List<ImageDecoder.Frame> frames = decoder.decode(bytes);
                if (frames.isEmpty()) throw new IOException("No frames decoded from " + url);
                return frames;

            } catch (Exception e) {
                LOGGER.error("[EmojiDeco] DL/decode 失敗 [{}]: {}", url, e.toString(), e);
                throw new RuntimeException(e);
            }
        }, FETCH_EXECUTOR)
        // Stage 2: GPU アップロード（ImageResolver に委譲）
        .thenCompose(ImageResolver::uploadAsync);
    }

    // ── disk cache ────────────────────────────────────────────────────────────

    private static Path diskCacheFile(String url, @Nullable String format) {
        String key = url + "\0" + (format != null ? format : "");
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
            LOGGER.warn("[EmojiDeco] Failed to write disk cache {}: {}", file, e.getMessage());
        }
    }

    private static Path cacheDir() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("emoji_deco").resolve("url_cache");
    }

    // ── HTTP download ─────────────────────────────────────────────────────────

    private static DownloadResult download(String urlStr) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        conn.setRequestProperty("User-Agent", "EmojiDeco/1.0");
        conn.connect();
        int code = conn.getResponseCode();
        if (code != 200) throw new IOException("HTTP " + code + " for " + urlStr);

        String contentType = conn.getContentType();

        try (InputStream is = conn.getInputStream();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) != -1) {
                if (baos.size() + n > MAX_BYTES)
                    throw new IOException("Image exceeds " + MAX_BYTES + " byte limit");
                baos.write(buf, 0, n);
            }
            return new DownloadResult(baos.toByteArray(), contentType);
        }
    }

    private record DownloadResult(byte[] bytes, @Nullable String contentType) {}
}
