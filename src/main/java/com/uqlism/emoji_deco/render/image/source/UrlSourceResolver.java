package com.uqlism.emoji_deco.render.image.source;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import com.uqlism.emoji_deco.render.image.ResolvedSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class UrlSourceResolver {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_BYTES  = 2 * 1024 * 1024;  // 2 MB
    private static final int TIMEOUT_MS = 5_000;
    private static final AtomicInteger counter = new AtomicInteger(0);
    private static final Map<String, CompletableFuture<ResolvedSource>> CACHE = new ConcurrentHashMap<>();

    public static CompletableFuture<ResolvedSource> resolve(JsonObject spec) {
        if (!spec.has("url"))
            return CompletableFuture.failedFuture(new IllegalArgumentException("Missing url in source spec"));
        String url = spec.get("url").getAsString();
        if (!url.startsWith("https://") && !url.startsWith("http://"))
            return CompletableFuture.failedFuture(new IllegalArgumentException("Only http/https URLs are allowed"));
        return CACHE.computeIfAbsent(url, UrlSourceResolver::fetch);
    }

    private static CompletableFuture<ResolvedSource> fetch(String url) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                byte[] bytes = download(url);
                NativeImage img = NativeImage.read(new ByteArrayInputStream(bytes));
                int w = img.getWidth(), h = img.getHeight();

                // GL テクスチャ登録はレンダースレッドで行う
                CompletableFuture<ResolvedSource> upload = new CompletableFuture<>();
                Minecraft.getInstance().execute(() -> {
                    ResourceLocation rl = ResourceLocation.parse(
                            "emoji_deco:url_image_" + counter.getAndIncrement());
                    Minecraft.getInstance().getTextureManager().register(rl, new DynamicTexture(img));
                    upload.complete(new ResolvedSource(rl, 0f, 0f, 1f, 1f, w, h));
                });
                return upload.join();
            } catch (Exception e) {
                LOGGER.error("[EmojiDeco] URL fetch failed for {}: {}", url, e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }

    private static byte[] download(String urlStr) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);
        conn.setRequestProperty("User-Agent", "EmojiDeco/1.0");
        conn.connect();
        int code = conn.getResponseCode();
        if (code != 200) throw new IOException("HTTP " + code + " for " + urlStr);

        try (InputStream is = conn.getInputStream();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) != -1) {
                if (baos.size() + n > MAX_BYTES)
                    throw new IOException("Image exceeds " + MAX_BYTES + " byte limit");
                baos.write(buf, 0, n);
            }
            return baos.toByteArray();
        }
    }
}
