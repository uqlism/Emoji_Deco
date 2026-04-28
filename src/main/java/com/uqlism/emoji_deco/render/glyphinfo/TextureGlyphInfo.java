package com.uqlism.emoji_deco.render.glyphinfo;

import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.font.SheetGlyphInfo;
import com.mojang.logging.LogUtils;
import com.uqlism.emoji_deco.render.AnimatedGlyphTexture;
import com.uqlism.emoji_deco.render.registry.TextureRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.font.GlyphRenderTypes;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.function.Function;

/**
 * GlyphInfo for an individual texture file (not atlas-based).
 * width/height control the rendered display size in font pixels (default 8×8).
 * UV covers the full texture (0→1) for both static and animated textures.
 */
public class TextureGlyphInfo implements GlyphInfo {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final ResourceLocation texture;
    private final int width;
    private final int height;

    public TextureGlyphInfo(ResourceLocation texture, int width, int height) {
        this.texture = texture;
        this.width = width;
        this.height = height;
    }

    @Override
    public float getAdvance() {
        return width + 1.0f;
    }

    @Override
    public BakedGlyph bake(Function<SheetGlyphInfo, BakedGlyph> function) {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null) {
            AnimatedGlyphTexture animTex = new AnimatedGlyphTexture(texture);
            try {
                animTex.load(mc.getResourceManager());
            } catch (IOException e) {
                LOGGER.warn("[EmojiDeco] Failed to load texture {}: {}", texture, e.getMessage());
            }
            mc.getTextureManager().register(texture, animTex);
            if (animTex.isAnimated()) {
                TextureRegistry.putAnimated(texture, animTex);
            }
        }
        GlyphRenderTypes renderTypes = GlyphRenderTypes.createForColorTexture(texture);
        return new BakedGlyph(
                renderTypes,
                0f, 1f,             // u0, u1 — full texture width
                0f, 1f,             // v0, v1 — full texture height
                0f, (float) width,  // left, right
                3f, 3f + height     // up, down (baseline-aligned like SpriteGlyphInfo)
        );
    }
}
