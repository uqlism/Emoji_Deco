package com.uqlism.emoji_deco.render.image.source;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.uqlism.emoji_deco.render.AnimatedGlyphTexture;
import com.uqlism.emoji_deco.render.image.ImageDecoder;
import com.uqlism.emoji_deco.render.image.ImageFormatDetector;
import com.uqlism.emoji_deco.render.image.ImageFormatDetector.Format;
import com.uqlism.emoji_deco.render.image.ResolvedSource;
import com.uqlism.emoji_deco.render.image.decoder.GifDecoder;
import com.uqlism.emoji_deco.render.image.decoder.StbDecoder;
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
    private static final Map<String, CompletableFuture<ResolvedSource>> CACHE = new ConcurrentHashMap<>();

    public static CompletableFuture<ResolvedSource> resolve(JsonObject spec) {
        if (!spec.has("url"))
            return CompletableFuture.failedFuture(new IllegalArgumentException("Missing url"));
        String url = spec.get("url").getAsString();
        if (!url.startsWith("https://") && !url.startsWith("http://"))
            return CompletableFuture.failedFuture(new IllegalArgumentException("Only http/https URLs are allowed"));
        String formatHint = spec.has("format") ? spec.get("format").getAsString() : null;
        // キャッシュキーは URL + format ヒントの組み合わせ
        String cacheKey = url + "\0" + (formatHint != null ? formatHint : "");
        return CACHE.computeIfAbsent(cacheKey, k -> fetch(url, formatHint));
    }

    private static CompletableFuture<ResolvedSource> fetch(String url, @Nullable String formatHint) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                DownloadResult dl = download(url);

                // フォーマット判定: JSON 明示 > Content-Type > マジックバイト
                String hint = (formatHint != null) ? formatHint : dl.contentType();
                Format fmt = ImageFormatDetector.detect(dl.bytes(), hint);

                // デコード
                ImageDecoder decoder = switch (fmt) {
                    case GIF -> new GifDecoder();
                    default  -> new StbDecoder();   // PNG / JPEG / UNKNOWN
                };
                List<ImageDecoder.Frame> frames = decoder.decode(dl.bytes());
                if (frames.isEmpty()) throw new IOException("No frames decoded from " + url);

                int w = frames.get(0).pixels().getWidth();
                int h = frames.get(0).pixels().getHeight();

                // GL 登録はレンダースレッドで
                CompletableFuture<ResolvedSource> upload = new CompletableFuture<>();
                Minecraft.getInstance().execute(() -> {
                    ResourceLocation rl = ResourceLocation.parse("emoji_deco:url_" + counter.getAndIncrement());
                    if (frames.size() == 1) {
                        Minecraft.getInstance().getTextureManager()
                                .register(rl, new DynamicTexture(frames.get(0).pixels()));
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
        });
    }

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
