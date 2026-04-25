package com.uqlism.runicink.mixin;

import com.uqlism.runicink.Config;
import com.uqlism.runicink.text.RichTextParser;
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
        method = "getPageRaw",
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
        cir.setReturnValue(RichTextParser.parse(raw));
    }
}
