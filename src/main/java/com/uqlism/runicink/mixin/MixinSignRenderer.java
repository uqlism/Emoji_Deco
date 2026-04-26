package com.uqlism.runicink.mixin;

import com.uqlism.runicink.Config;
import com.uqlism.runicink.text.RichTextParser;
import net.minecraft.client.renderer.blockentity.SignRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.entity.SignText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Function;

@Mixin(SignRenderer.class)
public class MixinSignRenderer {

@Redirect(
    method = "m_278841_",
    at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/world/level/block/entity/SignText;m_277130_(ZLjava/util/function/Function;)[Lnet/minecraft/util/FormattedCharSequence;"
    ),
    remap = false,
    require = 1
)
private FormattedCharSequence[] runicink$parseSignText(
        SignText signText, boolean filtered, Function<Component, FormattedCharSequence> lineConverter) {
    if (!Config.enableSigns) {
        return signText.getRenderMessages(filtered, lineConverter);
    }
    FormattedCharSequence[] result = new FormattedCharSequence[4];
    for (int i = 0; i < 4; i++) {
        Component original = signText.getMessage(i, filtered);
        String raw = original.getString();
        Component parsed = raw.isBlank() ? original : RichTextParser.parse(raw);
        result[i] = lineConverter.apply(parsed);
    }
    return result;
}
}
