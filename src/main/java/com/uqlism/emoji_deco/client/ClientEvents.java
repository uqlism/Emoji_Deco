package com.uqlism.emoji_deco.client;

import java.util.List;

import com.uqlism.emoji_deco.Config;
import com.uqlism.emoji_deco.EmojiDeco;
import com.uqlism.emoji_deco.text.ComponentTransformer;
import com.uqlism.emoji_deco.text.RichTextParser;
import com.uqlism.emoji_deco.text.SuggestionState;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = EmojiDeco.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
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
        if (SuggestionState.suggestions.isEmpty()) return;
        var screen = event.getScreen();
        if (!(screen instanceof ChatScreen) && !(screen instanceof AbstractSignEditScreen)) return;

        Minecraft mc = Minecraft.getInstance();
        GuiGraphics graphics = event.getGuiGraphics();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        int maxVisible  = Math.min(SuggestionState.suggestions.size(), 5);
        int totalHeight = 12 * maxVisible;
        int baseY       = screenHeight - 45 - totalHeight;

        CompletionRenderer.render(
                graphics, mc.font,
                SuggestionState.suggestions, SuggestionState.selectedIndex,
                2, baseY);
    }

    @SubscribeEvent
    public static void onScreenClose(ScreenEvent.Closing event) {
        SuggestionState.clear();
    }
}
