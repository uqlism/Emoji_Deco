package com.uqlism.emoji_deco.render.glyphinfo;

import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.font.SheetGlyphInfo;
import net.minecraft.client.gui.font.GlyphRenderTypes;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

/**
 * GlyphInfo for an individual texture file (not atlas-based).
 * UV covers the full texture (0→1), so any image aspect ratio is supported.
 * Actual texture loading is deferred to Minecraft's TextureManager on first render.
 */
public class TextureGlyphInfo implements GlyphInfo {

    private final ResourceLocation texture;

    public TextureGlyphInfo(ResourceLocation texture) {
        this.texture = texture;
    }

    @Override
    public float getAdvance() {
        return 9.0f;
    }

    @Override
    public BakedGlyph bake(Function<SheetGlyphInfo, BakedGlyph> function) {
        GlyphRenderTypes renderTypes = GlyphRenderTypes.createForColorTexture(texture);
        return new BakedGlyph(
                renderTypes,
                0f, 1f,   // u0, u1 — full texture width
                0f, 1f,   // v0, v1 — full texture height
                0f, 8f,   // left, right  (8px wide)
                3f, 11f   // up, down     (8px tall, baseline-aligned with standard glyphs)
        );
    }
}
