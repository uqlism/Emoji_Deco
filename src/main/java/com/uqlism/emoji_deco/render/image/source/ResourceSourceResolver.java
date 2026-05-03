package com.uqlism.emoji_deco.render.image.source;

import com.mojang.logging.LogUtils;
import com.uqlism.emoji_deco.render.image.ImageFormatDetector;
import com.uqlism.emoji_deco.render.image.ImageFormatDetector.Format;
import com.uqlism.emoji_deco.render.image.ImageResolver;
import com.uqlism.emoji_deco.render.image.ResolvedSource;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * emoji_deco:fetch_resource ソースリゾルバ。
 *
 * resolveAsync: バイト読み込み（レンダースレッド）→ デコード（バックグラウンド）
 *               → GPU アップロード（レンダースレッド, ImageResolver.uploadAsync）。
 * resolveSync:  ローディング GIF 専用の同期解決（リロードごとに 1 回のみ呼ばれる）。
 */
public class ResourceSourceResolver {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final AtomicInteger counter = new AtomicInteger();
    private static final ClassLoader MOD_CLASSLOADER = ResourceSourceResolver.class.getClassLoader();

    /** デコード専用バックグラウンドスレッドプール（最大 4 スレッド）。 */
    private static final java.util.concurrent.Executor DECODE_EXECUTOR =
            new ThreadPoolExecutor(0, 4, 60L, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(),
                    r -> {
                        Thread t = new Thread(r, "EmojiDeco-Decode-" + counter.getAndIncrement());
                        t.setDaemon(true);
                        t.setContextClassLoader(MOD_CLASSLOADER);
                        return t;
                    });

    /**
     * 非同期解決。
     * バイト読み込みはレンダースレッドで行い（ResourceManager はスレッドセーフでないため）、
     * デコードをバックグラウンドで実行後、ImageResolver.uploadAsync で GPU に転送する。
     */
    public static CompletableFuture<ResolvedSource> resolveAsync(String path, @Nullable String format) {
        ResourceLocation rl = ResourceLocation.tryParse(path);
        if (rl == null)
            return CompletableFuture.failedFuture(new IllegalArgumentException("Invalid path: " + path));

        Minecraft mc = Minecraft.getInstance();
        if (mc == null)
            return CompletableFuture.failedFuture(new IllegalStateException("Minecraft unavailable"));

        final byte[] data;
        try {
            data = readBytes(mc, rl);
        } catch (IOException e) {
            LOGGER.warn("[EmojiDeco] Failed to read {}: {}", path, e.getMessage());
            return CompletableFuture.failedFuture(e);
        }

        Format fmt = (format != null)
                ? ImageFormatDetector.fromHint(format)
                : ImageFormatDetector.fromHint(extension(rl.getPath()));

        return CompletableFuture.supplyAsync(() -> {
            try {
                var frames = ImageResolver.decoderFor(fmt).decode(data);
                if (frames.isEmpty()) throw new IOException("No frames decoded from " + path);
                return frames;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, DECODE_EXECUTOR)
        .thenCompose(ImageResolver::uploadAsync);
    }

    /**
     * ローディング GIF 専用の同期解決。レンダースレッドから呼ぶこと。
     * キャッシュなし（呼び出し側が loadingSlot.resolved で 1 回保持する）。
     */
    @Nullable
    public static ResolvedSource resolveSync(String path, @Nullable String format) {
        ResourceLocation rl = ResourceLocation.tryParse(path);
        if (rl == null) return null;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return null;

        Format fmt = (format != null)
                ? ImageFormatDetector.fromHint(format)
                : ImageFormatDetector.fromHint(extension(rl.getPath()));

        try {
            var frames = ImageResolver.decoderFor(fmt).decode(readBytes(mc, rl));
            if (frames.isEmpty()) return null;
            return ImageResolver.uploadSync(frames);
        } catch (IOException e) {
            LOGGER.warn("[EmojiDeco] Failed to load {}: {}", path, e.getMessage());
            return null;
        }
    }

    /** onResourceReload はキャッシュ廃止につき no-op。 */
    public static void onResourceReload() {}

    private static byte[] readBytes(Minecraft mc, ResourceLocation rl) throws IOException {
        Resource res = mc.getResourceManager().getResource(rl)
                .orElseThrow(() -> new IOException("Missing resource: " + rl));
        try (var is = res.open()) { return is.readAllBytes(); }
    }

    private static String extension(String path) {
        int dot = path.lastIndexOf('.');
        return dot >= 0 ? path.substring(dot + 1) : "";
    }

    private ResourceSourceResolver() {}
}
