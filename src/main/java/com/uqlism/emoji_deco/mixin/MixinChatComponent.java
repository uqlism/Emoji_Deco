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
        if (text instanceof Component c && c.getContents() instanceof DynamicComponentContents dcc) {
            return Collections.singletonList(new DynamicFormattedCharSequence(dcc.original()));
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
        if (text instanceof Component c && c.getContents() instanceof DynamicComponentContents dcc) {
            return Collections.singletonList(new DynamicFormattedCharSequence(dcc.original()));
        }
        return font.split(text, width);
    }
}
