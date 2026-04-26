package com.uqlism.runicink.text;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SpriteRegistry {

    public static final ResourceLocation SPRITE_FONT = new ResourceLocation("runicink", "sprite");
    private static final Map<SpriteKey, Integer> TEXTURE_TO_CP = new HashMap<>();
    private static final Map<Integer, SpriteKey> CP_TO_TEXTURE = new HashMap<>();
    private static int nextSpriteCp = 0xE000;

    public static final ResourceLocation HEAD_FONT = new ResourceLocation("runicink", "head");
    private static final Map<String, Integer> USERNAME_TO_CP = new HashMap<>();
    private static final Map<Integer, String> CP_TO_USERNAME = new HashMap<>();
    private static final Set<Integer> OVERLAY_CPS = new HashSet<>();
    // Each player uses 2 consecutive CPs: baseCp (face) + baseCp+1 (overlay)
    private static int nextHeadCp = 0xF000;

    public static synchronized int allocate(String atlasName, String textureName) {
        SpriteKey key = new SpriteKey(ResourceLocation.parse(atlasName), ResourceLocation.parse(textureName));
        return TEXTURE_TO_CP.computeIfAbsent(key, k -> {
            int cp = nextSpriteCp++;
            CP_TO_TEXTURE.put(cp, k);
            return cp;
        });
    }

    public static SpriteKey getTexture(int codePoint) {
        return CP_TO_TEXTURE.get(codePoint);
    }

    public static Component createComponent(String atlasName, String textureName) {
        int cp = allocate(atlasName, textureName);
        return Component.literal(new String(Character.toChars(cp)))
                .withStyle(Style.EMPTY.withFont(SPRITE_FONT));
    }

    /** Allocates (or returns existing) base CP for this username. Overlay is baseCp+1. */
    public static synchronized int allocateHead(String username) {
        return USERNAME_TO_CP.computeIfAbsent(username, k -> {
            int cp = nextHeadCp;
            nextHeadCp += 2;
            CP_TO_USERNAME.put(cp,     k);  // base face
            CP_TO_USERNAME.put(cp + 1, k);  // overlay
            OVERLAY_CPS.add(cp + 1);
            return cp;
        });
    }

    public static String getUsername(int codePoint) {
        return CP_TO_USERNAME.get(codePoint);
    }

    public static boolean isOverlay(int codePoint) {
        return OVERLAY_CPS.contains(codePoint);
    }

    /**
     * Returns a 2-character component: base face (advance=0) + overlay (advance=9).
     * Both characters render at the same x position so the overlay appears on top.
     */
    public static MutableComponent createHeadComponent(String username) {
        int baseCp = allocateHead(username);
        String chars = new String(Character.toChars(baseCp))
                     + new String(Character.toChars(baseCp + 1));
        return Component.literal(chars).withStyle(Style.EMPTY.withFont(HEAD_FONT).withColor(0xFFFFFF));
    }

    /** Placeholder until player-head sprite rendering is implemented. */
    public static MutableComponent createHeadComponent_unused(String username) {
        return Component.literal("[" + username + "]");
    }

    public record SpriteKey(ResourceLocation atlas, ResourceLocation sprite) {}
}
