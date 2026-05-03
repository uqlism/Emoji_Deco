package com.uqlism.emoji_deco.render.image.source;

import com.mojang.logging.LogUtils;
import com.uqlism.emoji_deco.render.image.ResolvedSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class AtlasSourceResolver {

    private static final Logger LOGGER = LogUtils.getLogger();

    @Nullable
    public static ResolvedSource resolveSync(String atlas, String sprite) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return null;

        ResourceLocation atlasRL  = ResourceLocation.tryParse(atlas);
        ResourceLocation spriteRL = ResourceLocation.tryParse(sprite);
        if (atlasRL == null || spriteRL == null) return null;

        TextureAtlas textureAtlas = mc.getModelManager().getAtlas(atlasRL);
        if (textureAtlas == null) return null;
        TextureAtlasSprite textureSprite = textureAtlas.getSprite(spriteRL);
        if (textureSprite.contents().name().equals(MissingTextureAtlasSprite.getLocation())) {
            LOGGER.warn("[EmojiDeco] Sprite not found in atlas {}: {}", atlasRL, spriteRL);
        }
        return new ResolvedSource.Static(
                atlasRL,
                textureSprite.getU0(), textureSprite.getV0(),
                textureSprite.getU1(), textureSprite.getV1(),
                textureSprite.contents().width(), textureSprite.contents().height(),
                null);
    }
}
