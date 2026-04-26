package com.uqlism.runicink.client;

import java.util.List;

import com.uqlism.runicink.Config;
import com.uqlism.runicink.RunicInk;
import com.uqlism.runicink.mixin.MixinChatScreen;
import com.uqlism.runicink.text.ChatInputState;
import com.uqlism.runicink.text.ChatSuggestionState;
import com.uqlism.runicink.text.ComponentTransformer;
import com.uqlism.runicink.text.RichTextParser;
import com.uqlism.runicink.text.ShortcodeManager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = RunicInk.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void onChatMessage(ClientChatReceivedEvent event) {
        if (!Config.enableChat) return;
        event.setMessage(ComponentTransformer.transform(event.getMessage()));
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (!Config.enableItemNames) return;
        List<Component> tooltip = event.getToolTip();
        if (tooltip.isEmpty()) return;
        String raw = tooltip.get(0).getString();
        tooltip.set(0, RichTextParser.parse(raw));
    }

@SubscribeEvent
public static void onScreenRender(ScreenEvent.Render.Post event) {
    if (!(event.getScreen() instanceof ChatScreen)) return;

    List<String> suggestions = ChatSuggestionState.suggestions;
    if (suggestions.isEmpty()) return;

    Minecraft mc = Minecraft.getInstance();
    GuiGraphics graphics = event.getGuiGraphics();
    int screenHeight = mc.getWindow().getGuiScaledHeight();

    int x = 2;
    int itemHeight = 12;
    int maxVisible = Math.min(suggestions.size(), 5);
    int totalHeight = itemHeight * maxVisible;
    int baseY = screenHeight - 40 - totalHeight;

    // 最大幅を計算
    int maxWidth = 0;
    for (int i = 0; i < maxVisible; i++) {
        String code = suggestions.get(i);
        Component preview = ShortcodeManager.resolve(code);
        int w = mc.font.width(":" + code + ":  ") + mc.font.width(preview);
        maxWidth = Math.max(maxWidth, w);
    }
    maxWidth = Math.max(maxWidth + 8, 100);

    // 背景
    graphics.fill(x - 2, baseY - 2, x + maxWidth, baseY + totalHeight + 2, 0xB0000000);

    for (int i = 0; i < maxVisible; i++) {
        // 選択中を中心に前後を表示
        int idx = (ChatSuggestionState.selectedIndex - (maxVisible / 2) + i + suggestions.size()) % suggestions.size();
        String code = suggestions.get(idx);
        Component preview = ShortcodeManager.resolve(code);
        int y = baseY + i * itemHeight;

        boolean selected = (idx == ChatSuggestionState.selectedIndex);
        if (selected) {
            graphics.fill(x - 2, y - 1, x + maxWidth, y + itemHeight - 1, 0x60FFFFFF);
        }

        int codeColor  = selected ? 0xFFFFFF : 0x888888;

        graphics.drawString(mc.font, preview, x, y, codeColor, false);
        // 展開後のComponent（スプライト等も描画される）
        int previewX = x + mc.font.width(preview) + 4;
        
        // ":stone:" の部分
        String codeLabel = ":" + code + ":";
        graphics.drawString(mc.font, codeLabel, previewX, y, codeColor, false);

    }
}
}
