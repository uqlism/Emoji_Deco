package com.uqlism.emoji_deco.mixin;

import com.uqlism.emoji_deco.Config;
import com.uqlism.emoji_deco.text.ComponentConverter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// TODO: BookViewScreen.WrittenBookAccess was removed in 1.21.1. Find the replacement target.
@Mixin(targets = "net.minecraft.client.gui.screens.inventory.BookViewScreen$WrittenBookAccess", remap = false)
public class MixinWrittenBookAccess {

    // 書籍は font.split() → font.drawInBatch(FormattedCharSequence) の経路を使うため
    // MixinFont.drawInBatch(Component) の catch-all が効かない。
    // ComponentConverter.toComponent() で Component ツリーを保持したまま変換する。
    // （scale/glow は font.split() 経由では未対応のため将来課題）
    @Inject(
        method = "m_7303_",
        at = @At("RETURN"),
        cancellable = true,
        remap = false
    )
    private void runicink$parseBookPage(int index, CallbackInfoReturnable<FormattedText> cir) {
        if (!Config.enableBooks) return;
        FormattedText page = cir.getReturnValue();
        if (!(page instanceof Component component)) return;
        if (component.getString().isBlank()) return;
        cir.setReturnValue(ComponentConverter.toComponent(component));
    }
}
