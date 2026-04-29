package com.uqlism.emoji_deco.render.image;

import net.minecraft.resources.ResourceLocation;

/** GPU テクスチャ上の image source 解決結果。UV は 0..1 正規化済み。 */
public record ResolvedSource(
        ResourceLocation texture,
        float u0, float v0,
        float u1, float v1,
        int nativeW, int nativeH) {}
