package com.uqlism.emoji_deco.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

public class CompletionRenderer {

    public static final int ITEM_HEIGHT  = 12;
    public static final int MAX_VISIBLE  = 10;
    private static final int PADDING_H   = 4;
    private static final int PADDING_TOP = 2;

    private static final int COLOR_SELECTED = 0xFFFF55;
    private static final int COLOR_NORMAL   = 0xFFFFFF;
    private static final int BG_COLOR       = 0x80000000;

    public static void render(GuiGraphics graphics, Font font,
                               List<SuggestionState.Entry> suggestions,
                               int selectedIndex, int x, int baseY, int maxX) {
        if (suggestions.isEmpty()) return;

        int visible = Math.min(suggestions.size(), MAX_VISIBLE);

        // 表示対象のインデックスリストを先に確定し、preview は visible 分だけ評価する
        int[] visibleIdx = new int[visible];
        Component[] previews = new Component[visible];
        for (int i = 0; i < visible; i++) {
            visibleIdx[i] = (selectedIndex + i) % suggestions.size();
            previews[i] = suggestions.get(visibleIdx[i]).preview().get();
        }

        // 幅は表示される分だけで測定する
        int innerWidth = 0;
        for (int i = 0; i < visible; i++) {
            SuggestionState.Entry e = suggestions.get(visibleIdx[i]);
            int w = font.width(e.label());
            int pw = font.width(previews[i]);
            if (pw > 0) w += pw + 2;
            innerWidth = Math.max(innerWidth, w);
        }
        int boxWidth = innerWidth + PADDING_H * 2;

        x = Math.min(x, maxX - boxWidth);
        x = Math.max(x, 0);

        int totalHeight = ITEM_HEIGHT * visible;
        graphics.fill(x, baseY, x + boxWidth, baseY + totalHeight, BG_COLOR);

        for (int i = 0; i < visible; i++) {
            SuggestionState.Entry e = suggestions.get(visibleIdx[i]);
            int y = baseY + i * ITEM_HEIGHT + PADDING_TOP;
            int color = (i == 0) ? COLOR_SELECTED : COLOR_NORMAL;

            int tx = x + PADDING_H;
            int pw = font.width(previews[i]);
            if (pw > 0) {
                graphics.drawString(font, previews[i], tx, y, color, false);
                tx += pw + 2;
            }
            graphics.drawString(font, e.label(), tx, y, color, false);
        }
    }
}
