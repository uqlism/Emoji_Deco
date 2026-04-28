package com.uqlism.emoji_deco.mixin;

import com.uqlism.emoji_deco.render.sequence.ConcatSequence;
import com.uqlism.emoji_deco.render.sequence.LightSequence;
import com.uqlism.emoji_deco.render.sequence.LightMode;
import com.uqlism.emoji_deco.render.sequence.OffsetSequence;
import com.uqlism.emoji_deco.render.sequence.ScaledSequence;
import com.uqlism.emoji_deco.render.registry.PlayerHeadRegistry;
import com.uqlism.emoji_deco.render.registry.SpriteRegistry;
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

    // ── depth tracking for Ambient light restoration ──────────────────────────

    /**
     * Tracks nesting depth of drawInBatch / drawInBatch8xOutline calls.
     * At depth 0 (outermost renderer call) the natural packedLight is stored as ambient.
     */
    private static final ThreadLocal<Integer> drawDepth    = ThreadLocal.withInitial(() -> 0);
    private static final ThreadLocal<Integer> ambientLight = new ThreadLocal<>();

    private static void enterDraw(int packedLight) {
        int d = drawDepth.get();
        drawDepth.set(d + 1);
        if (d == 0) ambientLight.set(packedLight);
    }

    private static void exitDraw() {
        int nd = drawDepth.get() - 1;
        drawDepth.set(nd);
        if (nd == 0) ambientLight.remove();
    }

    private static int resolveLight(LightMode mode, int current) {
        if (mode instanceof LightMode.Bypass)  return current;
        if (mode instanceof LightMode.Ambient) { Integer a = ambientLight.get(); return a != null ? a : current; }
        if (mode instanceof LightMode.Fixed f) return f.value();
        return current;
    }

    // ── drawInBatch ───────────────────────────────────────────────────────────

    // m_272191_ = drawInBatch(FormattedCharSequence, float, float, int, boolean, Matrix4f, MultiBufferSource, DisplayMode, int, int)
    @Inject(method = "m_272191_", at = @At("HEAD"), cancellable = true, remap = false)
    private void runicink$drawBatch_head(
            FormattedCharSequence text, float x, float y,
            int color, boolean dropShadow,
            Matrix4f matrix, MultiBufferSource buffers,
            Font.DisplayMode mode, int bgColor, int packedLight,
            CallbackInfoReturnable<Integer> cir) {

        enterDraw(packedLight);
        Font self = (Font)(Object)this;

        if (text instanceof LightSequence gs) {
            int light = resolveLight(gs.mode(), packedLight);
            cir.setReturnValue(self.drawInBatch(gs.inner(), x, y, color, dropShadow, matrix, buffers, mode, bgColor, light));
            return;
        }
        if (text instanceof ScaledSequence ss) {
            // Negative scale flips around the origin; compensate so the result stays in-place.
            float ox = ss.scaleX() < 0 ? self.width(ss.inner()) * (-ss.scaleX()) : 0f;
            float oy = ss.scaleY() < 0 ? self.lineHeight * (-ss.scaleY()) : 0f;
            Matrix4f scaled = new Matrix4f(matrix).translate(x + ox, y + oy, 0f).scale(ss.scaleX(), ss.scaleY(), 1.0f);
            cir.setReturnValue(self.drawInBatch(ss.inner(), 0f, 0f, color, dropShadow, scaled, buffers, mode, bgColor, packedLight));
            return;
        }
        if (text instanceof OffsetSequence os) {
            cir.setReturnValue(self.drawInBatch(os.inner(), x + os.offsetX(), y + os.offsetY(), color, dropShadow, matrix, buffers, mode, bgColor, packedLight));
            return;
        }
        if (text instanceof ConcatSequence cs) {
            float curX = x;
            int retVal = 0;
            for (FormattedCharSequence part : cs.parts()) {
                retVal = self.drawInBatch(part, curX, y, color, dropShadow, matrix, buffers, mode, bgColor, packedLight);
                curX += self.width(part);
            }
            cir.setReturnValue(retVal);
            return;
        }
    }

    @Inject(method = "m_272191_", at = @At("RETURN"), remap = false)
    private void runicink$drawBatch_tail(
            FormattedCharSequence text, float x, float y,
            int color, boolean dropShadow,
            Matrix4f matrix, MultiBufferSource buffers,
            Font.DisplayMode mode, int bgColor, int packedLight,
            CallbackInfoReturnable<Integer> cir) {
        exitDraw();
    }

    // ── drawInBatch8xOutline ──────────────────────────────────────────────────

    // m_168645_ = drawInBatch8xOutline(FormattedCharSequence, float, float, int, int, Matrix4f, MultiBufferSource, int)
    @Inject(method = "m_168645_", at = @At("HEAD"), cancellable = true, remap = false)
    private void runicink$drawOutline_head(
            FormattedCharSequence text, float x, float y,
            int color, int outlineColor,
            Matrix4f matrix, MultiBufferSource buffers, int packedLight,
            CallbackInfo ci) {

        enterDraw(packedLight);
        Font self = (Font)(Object)this;

        if (text instanceof LightSequence gs) {
            int light = resolveLight(gs.mode(), packedLight);
            self.drawInBatch8xOutline(gs.inner(), x, y, color, outlineColor, matrix, buffers, light);
            ci.cancel();
            return;
        }
        if (text instanceof ScaledSequence ss) {
            float ox = ss.scaleX() < 0 ? self.width(ss.inner()) * (-ss.scaleX()) : 0f;
            float oy = ss.scaleY() < 0 ? self.lineHeight * (-ss.scaleY()) : 0f;
            Matrix4f scaled = new Matrix4f(matrix).translate(x + ox, y + oy, 0f).scale(ss.scaleX(), ss.scaleY(), 1.0f);
            self.drawInBatch8xOutline(ss.inner(), 0f, 0f, color, outlineColor, scaled, buffers, packedLight);
            ci.cancel();
            return;
        }
        if (text instanceof OffsetSequence os) {
            self.drawInBatch8xOutline(os.inner(), x + os.offsetX(), y + os.offsetY(), color, outlineColor, matrix, buffers, packedLight);
            ci.cancel();
            return;
        }
        if (text instanceof ConcatSequence cs) {
            float curX = x;
            for (FormattedCharSequence part : cs.parts()) {
                self.drawInBatch8xOutline(part, curX, y, color, outlineColor, matrix, buffers, packedLight);
                curX += self.width(part);
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
                while (j < cpEntries.size() && isNoGlowFont(styleEntries.get(j)) == noGlow) j++;
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

    @Inject(method = "m_168645_", at = @At("RETURN"), remap = false)
    private void runicink$drawOutline_tail(
            FormattedCharSequence text, float x, float y,
            int color, int outlineColor,
            Matrix4f matrix, MultiBufferSource buffers, int packedLight,
            CallbackInfo ci) {
        exitDraw();
    }

    // ── width ─────────────────────────────────────────────────────────────────

    // m_92724_ = width(FormattedCharSequence)
    @Inject(method = "m_92724_", at = @At("HEAD"), cancellable = true, remap = false)
    private void runicink$width(FormattedCharSequence text, CallbackInfoReturnable<Integer> cir) {
        Font self = (Font)(Object)this;
        if (text instanceof LightSequence gs) {
            cir.setReturnValue(self.width(gs.inner()));
            return;
        }
        if (text instanceof ScaledSequence sxy) {
            cir.setReturnValue(Math.round(self.width(sxy.inner()) * sxy.scaleX()));
            return;
        }
        if (text instanceof OffsetSequence os) {
            cir.setReturnValue(self.width(os.inner()));
            return;
        }
        if (text instanceof ConcatSequence cs) {
            int total = 0;
            for (FormattedCharSequence part : cs.parts()) total += self.width(part);
            cir.setReturnValue(total);
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static boolean isNoGlowFont(Style style) {
        var font = style.getFont();
        return PlayerHeadRegistry.HEAD_FONT.equals(font)
            || PlayerHeadRegistry.HEAD_OVERLAY_FONT.equals(font)
            || SpriteRegistry.SPRITE_FONT.equals(font);
    }

    private static boolean hasNoGlowFont(FormattedCharSequence seq) {
        boolean[] found = {false};
        seq.accept((index, style, codePoint) -> {
            if (isNoGlowFont(style)) { found[0] = true; return false; }
            return true;
        });
        return found[0];
    }
}
