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
import net.minecraft.server.packs.resources.Resource;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * emoji_deco:resource ソースリゾルバ。
 *
 * フォーマット: JSON "format" フィールド > ファイル拡張子 で判別。
 *   GIF  → GifDecoder（アニメーション対応）
 *   その他 → StbDecoder（PNG / JPEG 静止画）
 *
 * アニメーションの tick 管理は ImageGlyphPool が担う。
 */
public class ResourceSourceResolver {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Map<ResourceLocation, ResolvedSource> LOADED = new ConcurrentHashMap<>();
    private static final Set<ResourceLocation> FAILED = ConcurrentHashMap.newKeySet();

    @Nullable
    public static ResolvedSource resolveSync(JsonObject spec) {
        if (!spec.has("texture")) return null;
        ResourceLocation rl = ResourceLocation.tryParse(spec.get("texture").getAsString());
        if (rl == null) return null;

        ResolvedSource cached = LOADED.get(rl);
        if (cached != null) return cached;
        if (FAILED.contains(rl)) return null;

        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return null;

        // フォーマット判定
        String formatHint = spec.has("format") ? spec.get("format").getAsString() : null;
        Format fmt = (formatHint != null)
                ? ImageFormatDetector.fromHint(formatHint)
                : ImageFormatDetector.fromHint(extension(rl.getPath()));

        ImageDecoder decoder = (fmt == Format.GIF) ? new GifDecoder() : new StbDecoder();

        ResolvedSource result = resolve(mc, rl, decoder);
        if (result != null) LOADED.put(rl, result);
        else                FAILED.add(rl);
        return result;
    }

    public static void onResourceReload() {
        LOADED.clear();
        FAILED.clear();
    }

    // ── 内部ヘルパー ──────────────────────────────────────────────────────────

    @Nullable
    private static ResolvedSource resolve(Minecraft mc, ResourceLocation rl, ImageDecoder decoder) {
        try {
            byte[] data = readBytes(mc, rl);
            List<ImageDecoder.Frame> frames = decoder.decode(data);
            if (frames.isEmpty()) return null;

            int w = frames.get(0).pixels().getWidth();
            int h = frames.get(0).pixels().getHeight();

            if (frames.size() == 1) {
                mc.getTextureManager().register(rl, new DynamicTexture(frames.get(0).pixels()));
                return new ResolvedSource.Static(rl, 0f, 0f, 1f, 1f, w, h);
            }
            AnimatedGlyphTexture animator = AnimatedGlyphTexture.fromFrames(frames);
            mc.getTextureManager().register(rl, animator);
            return new ResolvedSource.Animated(rl, 0f, 0f, 1f, 1f, w, h, animator);

        } catch (IOException e) {
            LOGGER.warn("[EmojiDeco] Failed to load texture {}: {}", rl, e.getMessage());
            return null;
        }
    }

    private static byte[] readBytes(Minecraft mc, ResourceLocation rl) throws IOException {
        Resource res = mc.getResourceManager().getResource(rl)
                .orElseThrow(() -> new IOException("Missing resource: " + rl));
        try (var is = res.open()) { return is.readAllBytes(); }
    }

    private static String extension(String path) {
        int dot = path.lastIndexOf('.');
        return dot >= 0 ? path.substring(dot + 1) : "";
    }
}
