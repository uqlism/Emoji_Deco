package com.uqlism.emoji_deco.render.image.source;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.logging.LogUtils;
import com.uqlism.emoji_deco.render.AnimatedGlyphTexture;
import com.uqlism.emoji_deco.render.image.ImageDecoder;
import com.uqlism.emoji_deco.render.image.ImageFormatDetector;
import com.uqlism.emoji_deco.render.image.ImageFormatDetector.Format;
import com.uqlism.emoji_deco.render.image.ResolvedSource;
import com.uqlism.emoji_deco.render.image.decoder.ApngDecoder;
import com.uqlism.emoji_deco.render.image.decoder.GifDecoder;
import com.uqlism.emoji_deco.render.image.decoder.StbDecoder;
import com.uqlism.emoji_deco.render.image.decoder.WebpDecoder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
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
    private static final int MAX_BYTES  = 2 * 1024 * 1024;
    private static final int TIMEOUT_MS = 5_000;
    private static final AtomicInteger counter = new AtomicInteger(0);

    private static final ClassLoader MOD_CLASSLOADER = UrlSourceResolver.class.getClassLoader();
    private static final java.util.concurrent.Executor FETCH_EXECUTOR = task ->
            java.util.concurrent.ForkJoinPool.commonPool().execute(() -> {
                Thread t = Thread.currentThread();
                ClassLoader prev = t.getContextClassLoader();
                t.setContextClassLoader(MOD_CLASSLOADER);
                try { task.run(); } finally { t.setContextClassLoader(prev); }
            });

    private record CacheEntry(CompletableFuture<ResolvedSource> future, long expiresAt) {
        boolean isExpired() {
            return expiresAt != Long.MAX_VALUE && System.currentTimeMillis() > expiresAt;
        }
    }

    private static final Map<String, CacheEntry> CACHE = new ConcurrentHashMap<>();

    public static CompletableFuture<ResolvedSource> resolve(
            String url, @Nullable String format, boolean diskCache, int ttlSeconds) {
        if (!url.startsWith("https://") && !url.startsWith("http://"))
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("Only http/https URLs are allowed"));

        String cacheKey = url + "\0" + (format != null ? format : "");
        long expiresAt = ttlSeconds > 0
                ? System.currentTimeMillis() + (long) ttlSeconds * 1000
                : Long.MAX_VALUE;

        return CACHE.compute(cacheKey, (k, existing) -> {
            if (existing != null && !existing.isExpired()) return existing;
            return new CacheEntry(fetch(url, format, diskCache, ttlSeconds), expiresAt);
        }).future();
    }


    private static CompletableFuture<ResolvedSource> fetch(
            String url, @Nullable String format, boolean diskCache, int ttlSeconds) {
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

                int w = frames.get(0).pixels().getWidth();
                int h = frames.get(0).pixels().getHeight();

                CompletableFuture<ResolvedSource> upload = new CompletableFuture<>();
                Minecraft.getInstance().execute(() -> {
                    ResourceLocation rl = ResourceLocation.parse(
                            "emoji_deco:fetch_url_" + counter.getAndIncrement());
                    if (frames.size() == 1) {
                        NativeImage pixels = frames.get(0).pixels();
                        DynamicTexture tex = new DynamicTexture(pixels);
                        TextureUtil.prepareImage(
                                tex.getId(), pixels.getWidth(), pixels.getHeight());
                        tex.upload();   // GL にピクセルデータを転送（これを忘れると黒になる）
                        Minecraft.getInstance().getTextureManager().register(rl, tex);
                        upload.complete(new ResolvedSource.Static(rl, 0f, 0f, 1f, 1f, w, h));
                    } else {
                        AnimatedGlyphTexture animator = AnimatedGlyphTexture.fromFrames(frames);
                        Minecraft.getInstance().getTextureManager().register(rl, animator);
                        upload.complete(new ResolvedSource.Animated(rl, 0f, 0f, 1f, 1f, w, h, animator));
                    }
                });
                return upload.join();

            } catch (Exception e) {
                LOGGER.error("[EmojiDeco] URL fetch failed for {}: {}", url, e.getMessage());
                throw new RuntimeException(e);
            }
        }, FETCH_EXECUTOR);
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
        return Minecraft.getInstance().gameDirectory.toPath().resolve("emoji_deco_cache");
    }

    // ── HTTP download ─────────────────────────────────────────────────────────

    private static DownloadResult download(String urlStr) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);
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
