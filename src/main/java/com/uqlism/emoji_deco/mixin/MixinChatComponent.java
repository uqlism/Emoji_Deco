package com.uqlism.emoji_deco.mixin;

import com.uqlism.emoji_deco.Config;
import com.uqlism.emoji_deco.text.ComponentSequenceConverter;
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
 * Intercepts ComponentRenderUtils.wrapComponents() (m_94005_) calls inside ChatComponent.
 *
 * This is the actual method used by ChatComponent to convert a Component into
 * FormattedCharSequence lines — NOT Font.split() or Language.getVisualOrder().
 * For DynamicComponentContents messages, we return DynamicFormattedCharSequence
 * so the color re-evaluates each render frame.
 */
@Mixin(ChatComponent.class)
public class MixinChatComponent {

    @Redirect(
        method = "m_240465_",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/ComponentRenderUtils;m_94005_(Lnet/minecraft/network/chat/FormattedText;ILnet/minecraft/client/gui/Font;)Ljava/util/List;",
            remap = false
        ),
        remap = false,
        require = 0
    )
    private List<FormattedCharSequence> runicink$wrapDynamicAdd(FormattedText text, int width, Font font) {
        if (Config.enableChat && text instanceof Component c) {
            return Collections.singletonList(ComponentSequenceConverter.toSequence(font, c));
        }
        return font.split(text, width);
    }

    @Redirect(
        method = "m_93795_",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/ComponentRenderUtils;m_94005_(Lnet/minecraft/network/chat/FormattedText;ILnet/minecraft/client/gui/Font;)Ljava/util/List;",
            remap = false
        ),
        remap = false,
        require = 0
    )
    private List<FormattedCharSequence> runicink$wrapDynamicRescale(FormattedText text, int width, Font font) {
        if (Config.enableChat && text instanceof Component c) {
            return Collections.singletonList(ComponentSequenceConverter.toSequence(font, c));
        }
        return font.split(text, width);
    }
}
