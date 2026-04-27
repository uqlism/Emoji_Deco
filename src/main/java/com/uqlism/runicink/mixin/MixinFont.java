package com.uqlism.runicink.mixin;

import com.uqlism.runicink.text.HeaderScaledSequence;
import com.uqlism.runicink.text.SpriteRegistry;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(Font.class)
public class MixinFont {

    // m_272191_ = drawInBatch(FormattedCharSequence, float, float, int, boolean, Matrix4f, MultiBufferSource, DisplayMode, int, int)
    @Inject(method = "m_272191_", at = @At("HEAD"), cancellable = true, remap = false)
    private void runicink$drawScaledLine(
            FormattedCharSequence text, float x, float y,
            int color, boolean dropShadow,
            Matrix4f matrix, MultiBufferSource buffers,
            Font.DisplayMode mode, int bgColor, int packedLight,
            CallbackInfoReturnable<Integer> cir) {
        if (!(text instanceof HeaderScaledSequence hss)) return;
        float s = hss.scale();
        Matrix4f scaled = new Matrix4f(matrix).scale(s, s, 1.0f);
        Font self = (Font)(Object)this;
        // Pass hss.inner() (not hss) to avoid re-triggering this injection
        cir.setReturnValue(self.drawInBatch(hss.inner(), x, y / s, color, dropShadow, scaled, buffers, mode, bgColor, packedLight));
    }

    // m_168645_ = drawInBatch8xOutline(FormattedCharSequence, float, float, int, int, Matrix4f, MultiBufferSource, int)
    @Inject(method = "m_168645_", at = @At("HEAD"), cancellable = true, remap = false)
    private void runicink$drawScaledLineGlow(
            FormattedCharSequence text, float x, float y,
            int color, int outlineColor,
            Matrix4f matrix, MultiBufferSource buffers, int packedLight,
            CallbackInfo ci) {
        if (text instanceof HeaderScaledSequence hss) {
            float s = hss.scale();
            Matrix4f scaled = new Matrix4f(matrix).scale(s, s, 1.0f);
            Font self = (Font)(Object)this;
            self.drawInBatch8xOutline(hss.inner(), x, y / s, color, outlineColor, scaled, buffers, packedLight);
            ci.cancel();
            return;
        }
        if (hasNoGlowFont(text)) {
            Font self = (Font)(Object)this;

            // Collect all char entries
            List<int[]> cpEntries = new ArrayList<>();
            List<Style> styleEntries = new ArrayList<>();
            text.accept((idx, style, cp) -> {
                cpEntries.add(new int[]{idx, cp});
                styleEntries.add(style);
                return true;
            });

            // Split into runs: no-glow glyphs → drawInBatch, others → drawInBatch8xOutline
            float curX = x;
            int i = 0;
            while (i < cpEntries.size()) {
                final boolean noGlow = isNoGlowFont(styleEntries.get(i));
                final int start = i;
                int j = i + 1;
                while (j < cpEntries.size() && isNoGlowFont(styleEntries.get(j)) == noGlow) {
                    j++;
                }
                final int end = j;
                FormattedCharSequence runSeq = sink -> {
                    for (int k = start; k < end; k++) {
                        if (!sink.accept(cpEntries.get(k)[0], styleEntries.get(k), cpEntries.get(k)[1])) return false;
                    }
                    return true;
                };
                if (noGlow) {
                    self.drawInBatch(runSeq, curX, y, 0xFFFFFF, false, matrix, buffers, Font.DisplayMode.NORMAL, 0, packedLight);
                } else {
                    self.drawInBatch8xOutline(runSeq, curX, y, color, outlineColor, matrix, buffers, packedLight);
                }
                curX += self.width(runSeq);
                i = j;
            }
            ci.cancel();
        }
    }

    private static boolean isNoGlowFont(Style style) {
        var font = style.getFont();
        return SpriteRegistry.HEAD_FONT.equals(font) || SpriteRegistry.SPRITE_FONT.equals(font);
    }

    private static boolean hasNoGlowFont(FormattedCharSequence seq) {
        boolean[] found = {false};
        seq.accept((index, style, codePoint) -> {
            if (isNoGlowFont(style)) {
                found[0] = true;
                return false;
            }
            return true;
        });
        return found[0];
    }
}
