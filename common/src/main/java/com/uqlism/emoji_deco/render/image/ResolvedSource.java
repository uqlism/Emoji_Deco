package com.uqlism.emoji_deco.render.image;

import com.uqlism.emoji_deco.render.AnimatedGlyphTexture;
import com.uqlism.emoji_deco.render.StaticGlyphTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * image source の解決結果。GPU テクスチャへのポインタ（RL + UV）。
 *
 * Static:   テクスチャ内容は不変。glyphTexture が非 null の場合は LRU で GPU アンロード可能。
 *           Atlas / Skin など外部管理テクスチャは glyphTexture = null。
 * Animated: 同一 RL を持つが animator が tick() ごとにテクスチャ内容を差し替える。
 *           スロット退避時は animator.close() で GL リソースを解放する。
 */
public sealed interface ResolvedSource permits ResolvedSource.Static, ResolvedSource.Animated {

    ResourceLocation texture();
    float u0(); float v0();
    float u1(); float v1();
    int nativeW(); int nativeH();

    /** 自前管理テクスチャへの参照。Atlas / Skin は null。 */
    @Nullable default StaticGlyphTexture glyphTexture() { return null; }

    record Static(
            ResourceLocation texture,
            float u0, float v0, float u1, float v1,
            int nativeW, int nativeH,
            @Nullable StaticGlyphTexture glyphTexture) implements ResolvedSource {}

    record Animated(
            ResourceLocation texture,
            float u0, float v0, float u1, float v1,
            int nativeW, int nativeH,
            AnimatedGlyphTexture animator) implements ResolvedSource {}
}
