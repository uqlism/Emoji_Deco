package com.uqlism.runicink.text;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;

public class HeaderRegistry {

    public static final ResourceLocation HEADER_FONT = new ResourceLocation("runicink", "header");

    // H1-H6 markers in PUA, separated from sprite range (0xE000+)
    private static final int BASE_CP = 0xE800;

    public static MutableComponent createMarker(int level) {
        int cp = BASE_CP + (level - 1);
        return Component.literal(new String(Character.toChars(cp)))
                .withStyle(Style.EMPTY.withFont(HEADER_FONT));
    }

    /** Returns the scale factor if {@code c} is a header marker root, or 1.0 otherwise. */
    public static float extractScale(Component c) {
        if (!HEADER_FONT.equals(c.getStyle().getFont())) return 1.0f;
        String s = c.getString();
        if (s.isEmpty()) return 1.0f;
        int cp = s.codePointAt(0);
        int level = cp - BASE_CP + 1;
        if (level < 1 || level > 6) return 1.0f;
        return scaleForLevel(level);
    }

    /** Strips the marker root, returning only the content siblings. */
    public static Component stripMarker(Component c) {
        MutableComponent result = Component.empty();
        for (Component sibling : c.getSiblings()) {
            result.append(sibling);
        }
        return result;
    }

    private static float scaleForLevel(int level) {
        // H1=4.0, H2=3.5, ... H6=1.5  (step -0.5 per level)
        return Math.max(1.5f, 4.5f - level * 0.5f);
    }
}
