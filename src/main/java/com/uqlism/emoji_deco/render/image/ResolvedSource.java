package com.uqlism.emoji_deco.render.image;

import com.uqlism.emoji_deco.render.AnimatedGlyphTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * image source の解決結果。GPU テクスチャへのポインタ（RL + UV）。
 *
 * Static:   テクスチャ内容は不変。
 * Animated: 同一 RL を持つが animator が tick() ごとにテクスチャ内容を差し替える。
 *           スロット退避時は animator.close() でGL リソースを解放する。
 */
public sealed interface ResolvedSource permits ResolvedSource.Static, ResolvedSource.Animated {

    ResourceLocation texture();
    float u0(); float v0();
    float u1(); float v1();
    int nativeW(); int nativeH();

    record Static(
            ResourceLocation texture,
            float u0, float v0, float u1, float v1,
            int nativeW, int nativeH) implements ResolvedSource {}

    record Animated(
            ResourceLocation texture,
            float u0, float v0, float u1, float v1,
            int nativeW, int nativeH,
            AnimatedGlyphTexture animator) implements ResolvedSource {}
}
