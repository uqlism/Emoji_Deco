package com.uqlism.runicink.mixin;

import com.uqlism.runicink.Config;
import com.uqlism.runicink.text.HeaderRegistry;
import com.uqlism.runicink.text.HeaderScaledSequence;
import com.uqlism.runicink.text.RichTextParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.SignRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.entity.SignText;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
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
        Font fontRenderer = Minecraft.getInstance().font;
        FormattedCharSequence[] result = new FormattedCharSequence[4];
        for (int i = 0; i < 4; i++) {
            Component original = signText.getMessage(i, filtered);
            String raw = original.getString();
            if (raw.isBlank()) {
                result[i] = FormattedCharSequence.EMPTY;
                continue;
            }
            Component parsed = RichTextParser.parse(raw);
            float scale = HeaderRegistry.extractScale(parsed);
            if (scale != 1.0f) {
                Component stripped = HeaderRegistry.stripMarker(parsed);
                List<FormattedCharSequence> lines = fontRenderer.split(stripped, Integer.MAX_VALUE / 2);
                FormattedCharSequence fcs = lines.isEmpty() ? FormattedCharSequence.EMPTY : lines.get(0);
                result[i] = new HeaderScaledSequence(fcs, scale);
            } else {
                List<FormattedCharSequence> lines = fontRenderer.split(parsed, Integer.MAX_VALUE / 2);
                result[i] = lines.isEmpty() ? FormattedCharSequence.EMPTY : lines.get(0);
            }
        }
        return result;
    }

    // SRG: Font.drawInBatch(FormattedCharSequence, ...) = m_272191_  (normal signs)
    @Redirect(
        method = "m_278841_",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Font;m_272191_(Lnet/minecraft/util/FormattedCharSequence;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)I"
        ),
        remap = false,
        require = 1
    )
    private int runicink$drawScaledLine(
            Font font,
            FormattedCharSequence text, float x, float y,
            int color, boolean dropShadow,
            Matrix4f matrix, MultiBufferSource buffers,
            Font.DisplayMode mode, int bgColor, int packedLight) {
        if (text instanceof HeaderScaledSequence hss) {
            float s = hss.scale();
            Matrix4f scaled = new Matrix4f(matrix).scale(s, s, 1.0f);
            // y/s: top of text stays at the original y; text expands downward
            return font.drawInBatch(hss, x, y / s, color, dropShadow, scaled, buffers, mode, bgColor, packedLight);
        }
        return font.drawInBatch(text, x, y, color, dropShadow, matrix, buffers, mode, bgColor, packedLight);
    }

    // SRG: Font.drawInBatch8xOutline(FormattedCharSequence, ...) = m_168645_  (glowing signs)
    @Redirect(
        method = "m_278841_",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Font;m_168645_(Lnet/minecraft/util/FormattedCharSequence;FFIILorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"
        ),
        remap = false,
        require = 1
    )
    private void runicink$drawScaledLineGlow(
            Font font,
            FormattedCharSequence text, float x, float y,
            int color, int outlineColor,
            Matrix4f matrix, MultiBufferSource buffers, int packedLight) {
        if (text instanceof HeaderScaledSequence hss) {
            float s = hss.scale();
            Matrix4f scaled = new Matrix4f(matrix).scale(s, s, 1.0f);
            font.drawInBatch8xOutline(hss, x, y / s, color, outlineColor, scaled, buffers, packedLight);
        } else {
            font.drawInBatch8xOutline(text, x, y, color, outlineColor, matrix, buffers, packedLight);
        }
    }
}
