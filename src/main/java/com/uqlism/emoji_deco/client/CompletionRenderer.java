package com.uqlism.emoji_deco.client;

import com.uqlism.emoji_deco.client.SuggestionState;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

public class CompletionRenderer {

    public static final int ITEM_HEIGHT = 12;
    public static final int MAX_VISIBLE = 10;
    private static final int PADDING_H   = 4;  // horizontal inner padding
    private static final int PADDING_TOP = 2;  // text top offset within row

    private static final int COLOR_SELECTED = 0xFFFF55; // vanilla yellow (§e)
    private static final int COLOR_NORMAL   = 0xFFFFFF;
    private static final int BG_COLOR       = 0x80000000;

    public static void render(GuiGraphics graphics, Font font,
                               List<SuggestionState.Entry> suggestions,
                               int selectedIndex, int x, int baseY, int maxX) {
        if (suggestions.isEmpty()) return;

        int visible = Math.min(suggestions.size(), MAX_VISIBLE);

        // Measure box width from all visible entries
        int innerWidth = 0;
        for (SuggestionState.Entry e : suggestions) {
            int w = font.width(e.label());
            int pw = font.width(e.preview());
            if (pw > 0) w += pw + 2;
            innerWidth = Math.max(innerWidth, w);
        }
        int boxWidth = innerWidth + PADDING_H * 2;

        // Clamp X so box stays on screen
        x = Math.min(x, maxX - boxWidth);
        x = Math.max(x, 0);

        int totalHeight = ITEM_HEIGHT * visible;
        graphics.fill(x, baseY, x + boxWidth, baseY + totalHeight, BG_COLOR);

        for (int i = 0; i < visible; i++) {
            // Selected item at top; wrap-around for short lists
            int idx = (selectedIndex + i) % suggestions.size();
            SuggestionState.Entry e = suggestions.get(idx);
            int y = baseY + i * ITEM_HEIGHT + PADDING_TOP;
            int color = (i == 0) ? COLOR_SELECTED : COLOR_NORMAL;

            int tx = x + PADDING_H;
            int pw = font.width(e.preview());
            if (pw > 0) {
                graphics.drawString(font, e.preview(), tx, y, color, false);
                tx += pw + 2;
            }
            graphics.drawString(font, e.label(), tx, y, color, false);
        }
    }
}
