package com.uqlism.emoji_deco.mixin;

import com.uqlism.emoji_deco.render.sequence.DynamicFormattedCharSequence;
import com.uqlism.emoji_deco.text.DynamicComponentContents;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Intercepts ClientLanguage.getVisualOrder(FormattedText) (m_5536_).
 *
 * ChatComponent calls Language.getVisualOrder(component) to produce the
 * FormattedCharSequence it stores per chat line — NOT Font.split().
 * When the component has DynamicComponentContents, we return a
 * DynamicFormattedCharSequence that re-evaluates the original message
 * component on every accept() call so that time-dependent colors animate.
 */
@Mixin(ClientLanguage.class)
public class MixinLanguage {

    // m_5536_ = Language.getVisualOrder(FormattedText) -> FormattedCharSequence
    @Inject(method = "m_5536_", at = @At("HEAD"), cancellable = true, remap = false)
    private void runicink$dynamicVisualOrder(
            FormattedText text,
            CallbackInfoReturnable<FormattedCharSequence> cir) {
        if (!(text instanceof Component c)) return;
        if (!(c.getContents() instanceof DynamicComponentContents dcc)) return;
        cir.setReturnValue(new DynamicFormattedCharSequence(dcc.original()));
    }
}
