package com.uqlism.runicink.text;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SpriteRegistry {

    public static final ResourceLocation SPRITE_FONT = new ResourceLocation("runicink", "sprite");
    private static final Map<SpriteKey, Integer> TEXTURE_TO_CP = new HashMap<>();
    private static final Map<Integer, SpriteKey> CP_TO_TEXTURE = new HashMap<>();
    private static int nextSpriteCp = 0xE000;

    public static final ResourceLocation HEAD_FONT = new ResourceLocation("runicink", "head");
    private static final Map<String, Integer> USERNAME_TO_CP = new HashMap<>();
    private static final Map<Integer, String> CP_TO_USERNAME = new HashMap<>();
    private static int nextHeadCp = 0xF000;


    public static synchronized int allocateSprite(String atlasName, String textureName) {
        SpriteKey spriteKey = new SpriteKey(ResourceLocation.parse(atlasName), ResourceLocation.parse(textureName));
        return TEXTURE_TO_CP.computeIfAbsent(spriteKey, k -> {
            int cp = nextSpriteCp++;
            CP_TO_TEXTURE.put(cp, k);
            return cp;
        });
    }

    public static SpriteKey getTexture(int codePoint) {
        return CP_TO_TEXTURE.get(codePoint);
    }

    public static Component createComponent(String atlasName, String textureName) {
        int cp = allocateSprite(atlasName, textureName);
        return Component.literal(new String(Character.toChars(cp)))
                .withStyle(Style.EMPTY.withFont(SPRITE_FONT));
    }

    public record SpriteKey(ResourceLocation atlas, ResourceLocation sprite) {}
    
    public static synchronized int allocateHead(String username) {
    return USERNAME_TO_CP.computeIfAbsent(username, k -> {
        int cp = nextHeadCp++;
        CP_TO_USERNAME.put(cp, k);
        return cp;
    });
}

    public static String getUsername(int codePoint) {
        return CP_TO_USERNAME.get(codePoint);
    }

    public static MutableComponent createHeadComponent(String username) {
        int cp = allocateHead(username);
        return Component.literal(new String(Character.toChars(cp)))
                .withStyle(Style.EMPTY.withFont(HEAD_FONT));
    }


}
