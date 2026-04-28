package com.uqlism.emoji_deco.render.glyphinfo;

import java.util.function.Function;

import org.slf4j.Logger;

import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.font.SheetGlyphInfo;
import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.font.GlyphRenderTypes;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

public class SpriteGlyphInfo implements GlyphInfo {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final ResourceLocation atlasRL;
    private final ResourceLocation textureRL;
    private final int width;
    private final int height;

    public SpriteGlyphInfo(ResourceLocation atlasRL, ResourceLocation textureRL, int width, int height) {
        this.atlasRL = atlasRL;
        this.textureRL = textureRL;
        this.width = width;
        this.height = height;
    }

    @Override
    public float getAdvance() {
        return width + 1.0f;
    }

    @Override
    public BakedGlyph bake(Function<SheetGlyphInfo, BakedGlyph> function) {
        TextureAtlas atlas = Minecraft.getInstance().getModelManager()
                .getAtlas(atlasRL);
        TextureAtlasSprite sprite = atlas.getSprite(textureRL);

        if (sprite.contents().name().equals(MissingTextureAtlasSprite.getLocation())) {
            LOGGER.warn("[EmojiDeco] Sprite not found in block atlas: {} (try e.g. block/fire_0)", textureRL);
            // Return the missing-texture sprite itself rather than null to prevent NPE in the font renderer.
        }

        GlyphRenderTypes renderTypes = GlyphRenderTypes.createForColorTexture(atlasRL);
        return new BakedGlyph(
                renderTypes,
                sprite.getU0(), sprite.getU1(),
                sprite.getV0(), sprite.getV1(),
                0.0f, (float) width,
                3.0f, 3.0f + height
        );
    }
}
