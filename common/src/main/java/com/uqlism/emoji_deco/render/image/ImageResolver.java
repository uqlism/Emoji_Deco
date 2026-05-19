package com.uqlism.emoji_deco.render.image;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import com.uqlism.emoji_deco.render.AnimatedGlyphTexture;
import com.uqlism.emoji_deco.render.StaticGlyphTexture;
import com.uqlism.emoji_deco.render.image.ImageFormatDetector.Format;
import com.uqlism.emoji_deco.render.image.decoder.ApngDecoder;
import com.uqlism.emoji_deco.render.image.decoder.GifDecoder;
import com.uqlism.emoji_deco.render.image.decoder.StbDecoder;
import com.uqlism.emoji_deco.render.image.decoder.WebpDecoder;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * デコーダ選択と GPU アップロードの共通ユーティリティ。
 * ResourceSourceResolver と UrlSourceResolver の共通処理を集約する。
 */
public final class ImageResolver {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final AtomicInteger counter = new AtomicInteger();

    /** フォーマットに対応するデコーダを返す。 */
    public static ImageDecoder decoderFor(Format fmt) {
        return switch (fmt) {
            case GIF       -> new GifDecoder();
            case WEBP      -> new WebpDecoder();
            case PNG, APNG -> new ApngDecoder();
            default        -> new StbDecoder();
        };
    }

    /**
     * デコード済みフレームを GPU にアップロードして ResolvedSource を返す。
     * レンダースレッドから呼ぶこと。
     */
    public static ResolvedSource uploadSync(List<ImageDecoder.Frame> frames) {
        int w = frames.get(0).pixels().getWidth();
        int h = frames.get(0).pixels().getHeight();
        ResourceLocation texRL = ResourceLocation.parse("emoji_deco:img_" + counter.getAndIncrement());
        if (frames.size() == 1) {
            NativeImage pixels = frames.get(0).pixels();
            StaticGlyphTexture tex = StaticGlyphTexture.fromPixels(pixels);
            Minecraft.getInstance().getTextureManager().register(texRL, tex);
            return new ResolvedSource.Static(texRL, 0f, 0f, 1f, 1f, w, h, tex);
        } else {
            AnimatedGlyphTexture animator = AnimatedGlyphTexture.fromFrames(frames);
            Minecraft.getInstance().getTextureManager().register(texRL, animator);
            return new ResolvedSource.Animated(texRL, 0f, 0f, 1f, 1f, w, h, animator);
        }
    }

    /**
     * デコード済みフレームを GPU にアップロードする非同期版。任意のスレッドから呼べる。
     * mc.execute() でレンダースレッドに uploadSync を委譲する。
     */
    public static CompletableFuture<ResolvedSource> uploadAsync(List<ImageDecoder.Frame> frames) {
        CompletableFuture<ResolvedSource> result = new CompletableFuture<>();
        Minecraft.getInstance().execute(() -> {
            try {
                result.complete(uploadSync(frames));
            } catch (Exception e) {
                LOGGER.error("[EmojiDeco] GPU upload failed", e);
                result.completeExceptionally(e);
            }
        });
        return result;
    }

    private ImageResolver() {}
}
