package com.uqlism.emoji_deco.render.image.source;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.uqlism.emoji_deco.render.AnimatedGlyphTexture;
import com.uqlism.emoji_deco.render.image.ResolvedSource;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ResourceSourceResolver {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<ResourceLocation, AnimatedGlyphTexture> ANIMATED = new ConcurrentHashMap<>();
    private static final Set<ResourceLocation> LOADED = ConcurrentHashMap.newKeySet();

    @Nullable
    public static ResolvedSource resolveSync(JsonObject spec) {
        if (!spec.has("texture")) return null;
        ResourceLocation rl = ResourceLocation.tryParse(spec.get("texture").getAsString());
        if (rl == null) return null;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return null;

        if (LOADED.add(rl)) {
            AnimatedGlyphTexture animTex = new AnimatedGlyphTexture(rl);
            try { animTex.load(mc.getResourceManager()); }
            catch (IOException e) { LOGGER.warn("[EmojiDeco] Failed to load texture {}: {}", rl, e.getMessage()); }
            mc.getTextureManager().register(rl, animTex);
            if (animTex.isAnimated()) ANIMATED.put(rl, animTex);
        }
        // UV は常にフルテクスチャ。nativeW/H=1 は「crop なし」用のダミー（UV が 0..1 なので比率計算不要）。
        return new ResolvedSource(rl, 0f, 0f, 1f, 1f, 1, 1);
    }

    public static void tickAnimated() {
        for (AnimatedGlyphTexture tex : ANIMATED.values()) tex.tick();
    }

    public static void onResourceReload() {
        LOADED.clear();
        ANIMATED.clear();
    }
}
