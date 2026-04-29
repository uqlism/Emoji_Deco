package com.uqlism.emoji_deco.mixin;

import com.uqlism.emoji_deco.Config;
import com.uqlism.emoji_deco.text.parse.RichTextParser;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BookViewScreen.WrittenBookAccess.class)
public class MixinWrittenBookAccess {

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
        String raw = component.getString();
        if (raw.isBlank()) return;
        cir.setReturnValue(RichTextParser.parse(raw).toComponent());
    }
}
