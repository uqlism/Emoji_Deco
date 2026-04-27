package com.uqlism.emoji_deco.mixin;

import com.uqlism.emoji_deco.text.CompositeScaledSequence;
import com.uqlism.emoji_deco.text.GlowSequence;
import com.uqlism.emoji_deco.text.ScaledSequence;
import com.uqlism.emoji_deco.text.SpriteRegistry;
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

        Font self = (Font)(Object)this;

        if (text instanceof GlowSequence gs) {
            cir.setReturnValue(self.drawInBatch(gs.inner(), x, y, color, dropShadow, matrix, buffers, mode, bgColor, GlowSequence.FULL_LIGHT));
            return;
        }

        if (text instanceof ScaledSequence ss) {
            float s = ss.scale();
            // translate(x,y,0) then scale(s,s,1): text starts at (x,y) in any coordinate system
            Matrix4f scaled = new Matrix4f(matrix).translate(x, y, 0f).scale(s, s, 1.0f);
            cir.setReturnValue(self.drawInBatch(ss.inner(), 0f, 0f, color, dropShadow, scaled, buffers, mode, bgColor, packedLight));
            return;
        }

        if (text instanceof CompositeScaledSequence css) {
            float curX = x;
            int retVal = 0;
            for (var seg : css.segments()) {
                float s = seg.scale();
                FormattedCharSequence chars = seg.chars();
                if (s == 1.0f) {
                    retVal = self.drawInBatch(chars, curX, y, color, dropShadow, matrix, buffers, mode, bgColor, packedLight);
                } else {
                    Matrix4f scaled = new Matrix4f(matrix).translate(curX, y, 0f).scale(s, s, 1.0f);
                    retVal = self.drawInBatch(chars, 0f, 0f, color, dropShadow, scaled, buffers, mode, bgColor, packedLight);
                }
                curX += self.width(chars) * s;
            }
            cir.setReturnValue(retVal);
        }
    }

    // m_168645_ = drawInBatch8xOutline(FormattedCharSequence, float, float, int, int, Matrix4f, MultiBufferSource, int)
    @Inject(method = "m_168645_", at = @At("HEAD"), cancellable = true, remap = false)
    private void runicink$drawScaledLineGlow(
            FormattedCharSequence text, float x, float y,
            int color, int outlineColor,
            Matrix4f matrix, MultiBufferSource buffers, int packedLight,
            CallbackInfo ci) {

        Font self = (Font)(Object)this;

        if (text instanceof GlowSequence gs) {
            self.drawInBatch8xOutline(gs.inner(), x, y, color, outlineColor, matrix, buffers, GlowSequence.FULL_LIGHT);
            ci.cancel();
            return;
        }

        if (text instanceof ScaledSequence ss) {
            float s = ss.scale();
            Matrix4f scaled = new Matrix4f(matrix).translate(x, y, 0f).scale(s, s, 1.0f);
            self.drawInBatch8xOutline(ss.inner(), 0f, 0f, color, outlineColor, scaled, buffers, packedLight);
            ci.cancel();
            return;
        }

        if (text instanceof CompositeScaledSequence css) {
            float curX = x;
            for (var seg : css.segments()) {
                float s = seg.scale();
                FormattedCharSequence chars = seg.chars();
                if (s == 1.0f) {
                    self.drawInBatch8xOutline(chars, curX, y, color, outlineColor, matrix, buffers, packedLight);
                } else {
                    Matrix4f scaled = new Matrix4f(matrix).translate(curX, y, 0f).scale(s, s, 1.0f);
                    self.drawInBatch8xOutline(chars, 0f, 0f, color, outlineColor, scaled, buffers, packedLight);
                }
                curX += self.width(chars) * s;
            }
            ci.cancel();
            return;
        }

        if (hasNoGlowFont(text)) {
            List<int[]> cpEntries = new ArrayList<>();
            List<Style> styleEntries = new ArrayList<>();
            text.accept((idx, style, cp) -> {
                cpEntries.add(new int[]{idx, cp});
                styleEntries.add(style);
                return true;
            });

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

    // m_92724_ = width(FormattedCharSequence) — returns visual (scaled) width
    @Inject(method = "m_92724_", at = @At("HEAD"), cancellable = true, remap = false)
    private void runicink$scaledWidth(FormattedCharSequence text, CallbackInfoReturnable<Integer> cir) {
        Font self = (Font)(Object)this;
        if (text instanceof ScaledSequence ss) {
            cir.setReturnValue(Math.round(self.width(ss.inner()) * ss.scale()));
            return;
        }
        if (text instanceof CompositeScaledSequence css) {
            int total = 0;
            for (var seg : css.segments()) {
                total += Math.round(self.width(seg.chars()) * seg.scale());
            }
            cir.setReturnValue(total);
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
