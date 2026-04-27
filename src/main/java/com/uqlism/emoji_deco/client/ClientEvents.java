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
        int screenWidth  = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        int visible     = Math.min(SuggestionState.suggestions.size(), CompletionRenderer.MAX_VISIBLE);
        int totalHeight = CompletionRenderer.ITEM_HEIGHT * visible;
        // Position just above the chat input bar (input is 12px tall at the bottom)
        int baseY = screenHeight - 14 - totalHeight;

        // Align X to the trigger character in the EditBox.
        // EditBox starts at x=2 with 4px inner padding; text renders from x=6.
        // triggerPos is the index of ':' / '@' / '#' in lastInput.
        int triggerPos = Math.min(SuggestionState.triggerPos, SuggestionState.lastInput.length());
        int x = 2 + 4 + mc.font.width(SuggestionState.lastInput.substring(0, triggerPos));

        CompletionRenderer.render(
                graphics, mc.font,
                SuggestionState.suggestions, SuggestionState.selectedIndex,
                x, baseY, screenWidth - 2);
    }

    @SubscribeEvent
    public static void onScreenClose(ScreenEvent.Closing event) {
        SuggestionState.clear();
    }
}
