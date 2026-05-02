package com.uqlism.emoji_deco.mixin;

import com.uqlism.emoji_deco.text.ComponentSequenceConverter;
import net.minecraft.client.Minecraft;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * GuiGraphics.drawString(Component) / drawCenteredString(Component) / Font.width(Component) は
 * すべて Language.getVisualOrder() を経由して FormattedCharSequence に変換する。
 * ここで Component を ComponentSequenceConverter に通すことで
 * ホットバーアイテム名・タイトル・GUI ラベル等の shortcode / decorator を一括処理する。
 */
@Mixin(Language.class)
public class MixinLanguage {

    // m_5536_ = getVisualOrder(FormattedText)
    @Inject(method = "m_5536_", at = @At("HEAD"), cancellable = true, remap = false)
    private void runicink$getVisualOrder(
            FormattedText text,
            CallbackInfoReturnable<FormattedCharSequence> cir) {

        if (!(text instanceof Component component)) return;
        String raw = component.getString();
        if (raw.indexOf('#') < 0 && raw.indexOf(':') < 0) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.font == null) return;

        cir.setReturnValue(ComponentSequenceConverter.toSequence(mc.font, component));
    }
}
