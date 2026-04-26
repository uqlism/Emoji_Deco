package com.uqlism.runicink.client;

import java.util.List;

import com.uqlism.runicink.Config;
import com.uqlism.runicink.RunicInk;
import com.uqlism.runicink.mixin.MixinChatScreen;
import com.uqlism.runicink.text.ChatInputState;
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
        String text = ChatInputState.lastInput;
        if (text.isBlank() || !ShortcodeManager.hasAnyShortcode(text)) return;

        Minecraft mc = Minecraft.getInstance();
        Component preview = RichTextParser.parse(text);
        GuiGraphics graphics = event.getGuiGraphics();

        int x = 2;
        int y = mc.getWindow().getGuiScaledHeight() - 40;
        int width = mc.font.width(preview);

        graphics.fill(x - 2, y - 2, x + width + 2, y + 10, 0x80000000);
        graphics.drawString(mc.font, preview, x, y, 0xFFFFFF, false);
    }
}
