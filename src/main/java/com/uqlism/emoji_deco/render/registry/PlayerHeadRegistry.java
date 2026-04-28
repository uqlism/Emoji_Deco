package com.uqlism.emoji_deco.render.registry;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages codepoint allocation and Component creation for player-head glyphs.
 *
 * Two fonts are used so the rendering layer can distinguish base/overlay by font
 * alone, eliminating any per-codepoint isOverlay bookkeeping.
 *
 *   HEAD_FONT         — base face layer  (8×8, advance=0)
 *   HEAD_OVERLAY_FONT — hat overlay layer (9×9, z-shifted via MixinBakedGlyph, advance=9)
 *
 * Both fonts share the same codepoint space: each player gets one codepoint that
 * appears in both fonts, so username lookup works identically for both.
 */
public class PlayerHeadRegistry {

    public static final ResourceLocation HEAD_FONT         = new ResourceLocation("emoji_deco", "head");
    public static final ResourceLocation HEAD_OVERLAY_FONT = new ResourceLocation("emoji_deco", "head_overlay");

    private static final Map<String, Integer> USERNAME_TO_CP = new HashMap<>();
    private static final Map<Integer, String> CP_TO_USERNAME = new HashMap<>();
    private static int nextCp = 0xF000;

    public static synchronized int allocate(String username) {
        return USERNAME_TO_CP.computeIfAbsent(username, k -> {
            int cp = nextCp++;
            CP_TO_USERNAME.put(cp, k);
            return cp;
        });
    }

    public static String getUsername(int codePoint) {
        return CP_TO_USERNAME.get(codePoint);
    }

    /**
     * Returns a 2-component sequence: base face (HEAD_FONT, advance=0) followed by
     * overlay (HEAD_OVERLAY_FONT, advance=9). Both use the same codepoint so the
     * overlay renders at the same X position as the base.
     */
    public static MutableComponent createComponent(String username) {
        int cp = allocate(username);
        String ch = new String(Character.toChars(cp));
        MutableComponent base    = Component.literal(ch).withStyle(Style.EMPTY.withFont(HEAD_FONT).withColor(0xFFFFFF));
        MutableComponent overlay = Component.literal(ch).withStyle(Style.EMPTY.withFont(HEAD_OVERLAY_FONT).withColor(0xFFFFFF));
        return base.append(overlay);
    }
}
