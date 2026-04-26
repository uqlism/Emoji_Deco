package com.uqlism.runicink.client;

import com.uqlism.runicink.text.SuggestionState;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

public class CompletionRenderer {

    private static final int ITEM_HEIGHT  = 12;
    private static final int MAX_VISIBLE  = 5;
    private static final int MIN_WIDTH    = 100;

    public static void render(GuiGraphics graphics, Font font,
                               List<SuggestionState.Entry> suggestions,
                               int selectedIndex, int x, int baseY) {
        if (suggestions.isEmpty()) return;

        int maxVisible = Math.min(suggestions.size(), MAX_VISIBLE);

        int maxWidth = 0;
        for (int i = 0; i < maxVisible; i++) {
            SuggestionState.Entry e = suggestions.get(i);
            int w = font.width(e.preview()) + 4 + font.width(e.label());
            maxWidth = Math.max(maxWidth, w);
        }
        maxWidth = Math.max(maxWidth + 8, MIN_WIDTH);

        int totalHeight = ITEM_HEIGHT * maxVisible;
        graphics.fill(x - 2, baseY - 2, x + maxWidth, baseY + totalHeight + 2, 0xB0000000);

        for (int i = 0; i < maxVisible; i++) {
            int idx = (selectedIndex - (maxVisible / 2) + i + suggestions.size()) % suggestions.size();
            SuggestionState.Entry e = suggestions.get(idx);
            int y = baseY + i * ITEM_HEIGHT;

            boolean selected = (idx == selectedIndex);
            if (selected) {
                graphics.fill(x - 2, y - 1, x + maxWidth, y + ITEM_HEIGHT - 1, 0x60FFFFFF);
            }

            int color = selected ? 0xFFFFFF : 0xAAAAAA;
            graphics.drawString(font, e.preview(), x, y, color, false);
            graphics.drawString(font, e.label(), x + font.width(e.preview()) + 4, y, color, false);
        }
    }
}
