package com.uqlism.emoji_deco.render.registry;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Manages codepoint allocation and Component creation for player-head glyphs (HEAD_FONT).
 *
 * Each player uses two consecutive codepoints:
 *   baseCp     — base face layer  (8×8, advance=0 so overlay sits on top)
 *   baseCp + 1 — hat overlay layer (9×9, z-shifted via MixinBakedGlyph)
 */
public class PlayerHeadRegistry {

    public static final ResourceLocation HEAD_FONT = new ResourceLocation("emoji_deco", "head");

    private static final Map<String, Integer> USERNAME_TO_CP = new HashMap<>();
    private static final Map<Integer, String> CP_TO_USERNAME = new HashMap<>();
    private static final Set<Integer>         OVERLAY_CPS    = new HashSet<>();
    private static int nextCp = 0xF000;

    public static synchronized int allocate(String username) {
        return USERNAME_TO_CP.computeIfAbsent(username, k -> {
            int cp = nextCp;
            nextCp += 2;
            CP_TO_USERNAME.put(cp,     k);
            CP_TO_USERNAME.put(cp + 1, k);
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

    public static MutableComponent createComponent(String username) {
        int baseCp = allocate(username);
        String chars = new String(Character.toChars(baseCp))
                     + new String(Character.toChars(baseCp + 1));
        return Component.literal(chars).withStyle(Style.EMPTY.withFont(HEAD_FONT).withColor(0xFFFFFF));
    }
}
