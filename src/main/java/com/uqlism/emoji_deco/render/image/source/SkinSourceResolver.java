package com.uqlism.emoji_deco.render.image.source;

import com.google.gson.JsonObject;
import com.uqlism.emoji_deco.render.image.ResolvedSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SkinSourceResolver {

    // スキンテクスチャは 64×64。フルテクスチャを返し、crop 側でリージョンを切り出す。
    private static final Map<String, ResourceLocation> SKIN_CACHE = new ConcurrentHashMap<>();

    public static void clearCache() { SKIN_CACHE.clear(); }

    /** スキンテクスチャ全体を返す（64×64）。未取得の場合は null。 */
    @Nullable
    public static ResolvedSource resolveSync(String player) {
        ResourceLocation skin = getSkinTexture(player);
        if (skin == null) return null;
        return new ResolvedSource.Static(skin, 0f, 0f, 1f, 1f, 64, 64, null);
    }

    @Nullable
    private static ResourceLocation getSkinTexture(String username) {
        ResourceLocation cached = SKIN_CACHE.get(username.toLowerCase(Locale.ROOT));
        if (cached != null) return cached;

        Minecraft mc = Minecraft.getInstance();
        ResourceLocation skin = null;
        if (mc.player != null && mc.player.getName().getString().equalsIgnoreCase(username)) {
            skin = mc.player.getSkin().texture();
        } else if (mc.level != null) {
            for (AbstractClientPlayer p : mc.level.players()) {
                if (p.getName().getString().equalsIgnoreCase(username)) {
                    skin = p.getSkin().texture();
                    break;
                }
            }
        }
        if (skin != null && !isDefaultSkin(skin)) {
            SKIN_CACHE.put(username.toLowerCase(Locale.ROOT), skin);
        }
        return skin;
    }

    private static boolean isDefaultSkin(ResourceLocation skin) {
        return skin.getNamespace().equals("minecraft")
                && skin.getPath().startsWith("textures/entity/player/");
    }
}
