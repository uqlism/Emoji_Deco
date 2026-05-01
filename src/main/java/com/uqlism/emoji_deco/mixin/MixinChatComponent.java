package com.uqlism.emoji_deco.mixin;

import com.uqlism.emoji_deco.render.sequence.DynamicFormattedCharSequence;
import com.uqlism.emoji_deco.text.DynamicComponentContents;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collections;
import java.util.List;

/**
 * Intercepts Font.split() calls inside ChatComponent so that
 * DynamicComponentContents-backed messages produce DynamicFormattedCharSequence
 * lines (which re-evaluate on every accept() call) instead of static sequences.
 *
 * This covers both the initial addMessage and the rescaleChat rebuild path.
 */
@Mixin(ChatComponent.class)
public class MixinChatComponent {

    // m_240465_ = addMessage(Component, MessageSignature, int, GuiMessageTag, boolean)
    @Redirect(
        method = "m_240465_",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Font;m_92923_(Lnet/minecraft/network/chat/FormattedText;I)Ljava/util/List;",
            remap = false
        ),
        remap = false,
        require = 0
    )
    private List<FormattedCharSequence> runicink$splitDynamicAdd(Font font, FormattedText text, int maxWidth) {
        if (text instanceof Component c && c.getContents() instanceof DynamicComponentContents) {
            return Collections.singletonList(new DynamicFormattedCharSequence(c));
        }
        return font.split(text, maxWidth);
    }

    // m_93795_ = rescaleChat(boolean) — rebuilds trimmedMessages from allMessages
    @Redirect(
        method = "m_93795_",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Font;m_92923_(Lnet/minecraft/network/chat/FormattedText;I)Ljava/util/List;",
            remap = false
        ),
        remap = false,
        require = 0
    )
    private List<FormattedCharSequence> runicink$splitDynamicRescale(Font font, FormattedText text, int maxWidth) {
        if (text instanceof Component c && c.getContents() instanceof DynamicComponentContents) {
            return Collections.singletonList(new DynamicFormattedCharSequence(c));
        }
        return font.split(text, maxWidth);
    }
}
