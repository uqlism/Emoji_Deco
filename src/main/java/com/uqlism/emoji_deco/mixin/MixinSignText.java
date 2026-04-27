package com.uqlism.emoji_deco.mixin;

import com.uqlism.emoji_deco.Config;
import com.uqlism.emoji_deco.text.RichNode;
import com.uqlism.emoji_deco.text.RichTextParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.entity.SignText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Function;

@Mixin(SignText.class)
public class MixinSignText {

    @Inject(method = "m_277130_", at = @At("HEAD"), cancellable = true, remap = false)
    private void runicink$parseRichText(boolean filtered,
            Function<Component, FormattedCharSequence> lineConverter,
            CallbackInfoReturnable<FormattedCharSequence[]> cir) {
        if (!Config.enableSigns) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.font == null) return;
        Font font = mc.font;
        SignText self = (SignText)(Object)this;

        FormattedCharSequence[] result = new FormattedCharSequence[4];
        for (int i = 0; i < 4; i++) {
            Component original = self.getMessage(i, filtered);
            String raw = original.getString();
            if (raw.isBlank()) {
                result[i] = FormattedCharSequence.EMPTY;
                continue;
            }
            result[i] = RichNode.toSequence(font, RichTextParser.parse(raw));
        }
        cir.setReturnValue(result);
    }
}
