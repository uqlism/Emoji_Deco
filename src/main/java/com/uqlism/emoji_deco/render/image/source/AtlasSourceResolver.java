package com.uqlism.emoji_deco.render.image.source;

import com.google.gson.JsonObject;
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
    public static ResolvedSource resolveSync(JsonObject spec) {
        if (!spec.has("atlas") || !spec.has("sprite")) return null;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return null;

        String atlasStr  = spec.get("atlas").getAsString();
        String spriteStr = spec.get("sprite").getAsString();
        ResourceLocation atlasRL  = ResourceLocation.tryParse(atlasStr);
        ResourceLocation spriteRL = ResourceLocation.tryParse(spriteStr);
        if (atlasRL == null || spriteRL == null) return null;

        TextureAtlas atlas = mc.getModelManager().getAtlas(atlasRL);
        if (atlas == null) return null;
        TextureAtlasSprite sprite = atlas.getSprite(spriteRL);
        if (sprite.contents().name().equals(MissingTextureAtlasSprite.getLocation())) {
            LOGGER.warn("[EmojiDeco] Sprite not found in atlas {}: {}", atlasRL, spriteRL);
        }
        return new ResolvedSource.Static(
                atlasRL,
                sprite.getU0(), sprite.getV0(),
                sprite.getU1(), sprite.getV1(),
                sprite.contents().width(), sprite.contents().height());
    }
}
