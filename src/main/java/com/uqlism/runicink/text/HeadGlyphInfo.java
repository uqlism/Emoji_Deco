package com.uqlism.runicink.text;

import java.util.function.Function;

import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.font.SheetGlyphInfo;

import net.minecraft.client.gui.font.GlyphRenderTypes;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.resources.ResourceLocation;

public class HeadGlyphInfo implements GlyphInfo {

    private final ResourceLocation skinTexture;
    private final boolean overlay;

    public HeadGlyphInfo(ResourceLocation skinTexture, boolean overlay) {
        this.skinTexture = skinTexture;
        this.overlay = overlay;
    }

    @Override
    public float getAdvance() {
        // base: advance=0 so overlay renders at same x; overlay: advance=9 to move cursor past
        return overlay ? 9.0f : 0.0f;
    }

    @Override
    public BakedGlyph bake(Function<SheetGlyphInfo, BakedGlyph> function) {
        // スキンテクスチャは 64x64
        // 顔ベース: x=8-16, y=8-16; オーバーレイ(帽子): x=40-48, y=8-16
        float u0 = (overlay ? 40f : 8f)  / 64f;
        float u1 = (overlay ? 48f : 16f) / 64f;
        float v0 = 8f  / 64f;
        float v1 = 16f / 64f;

        GlyphRenderTypes renderTypes = GlyphRenderTypes.createForColorTexture(skinTexture);
        return new BakedGlyph(
                renderTypes,
                u0, u1,
                v0, v1,
                0.0f, 8.0f,
                3.0f, 11.0f
        );
    }
}