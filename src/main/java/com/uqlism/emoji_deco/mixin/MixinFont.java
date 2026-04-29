package com.uqlism.emoji_deco.mixin;

import com.uqlism.emoji_deco.render.sequence.AffineSequence;
import com.uqlism.emoji_deco.render.sequence.ConcatSequence;
import com.uqlism.emoji_deco.render.sequence.LightSequence;
import com.uqlism.emoji_deco.render.sequence.LightMode;
import com.uqlism.emoji_deco.render.image.ImageGlyphPool;
import com.uqlism.emoji_deco.text.ComponentTransformer;
import org.lwjgl.opengl.GL11;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
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
    /** Re-entry guard: prevents the recursive drawInBatch call we make from triggering this inject again. */
    private static final ThreadLocal<Boolean> inComponentTransform = ThreadLocal.withInitial(() -> false);

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

    // ── drawInBatch(Component) → catch-all transform ──────────────────────────

    // m_272077_ = drawInBatch(Component, float, float, int, boolean, Matrix4f, MultiBufferSource, DisplayMode, int, int)
    // This intercepts every Component-based text draw (including third-party mod
    // pedestals, item frames, etc.) so markup/shortcodes work without per-mod mixins.
    // Fast-path: components whose getString() contains no '#' or ':' are skipped at
    // negligible cost. Already-transformed components (from specific hooks) pass the
    // fast-path because markup characters are gone after parsing — no double transform.
    // @Inject + cancel: also corrects x for callers that centre text by calling
    // font.width(rawComponent) before drawInBatch (e.g. Supplementaries pedestal).
    // inComponentTransform guards against mutual recursion: text containing ':'
    // (e.g. "journeymap:waypoint") may still have ':' in getString() after transform,
    // which would re-trigger this inject infinitely without the guard.
    @Inject(method = "m_272077_", at = @At("HEAD"), cancellable = true, remap = false)
    private void runicink$transformDrawBatchComponent(
            Component text, float x, float y,
            int color, boolean dropShadow,
            Matrix4f matrix, MultiBufferSource buffers,
            Font.DisplayMode mode, int bgColor, int packedLight,
            CallbackInfoReturnable<Integer> cir) {
        if (inComponentTransform.get()) return;
        String raw = text.getString();
        if (raw.indexOf('#') < 0 && raw.indexOf(':') < 0) return;
        Component transformed = ComponentTransformer.transform(text);
        Font self = (Font)(Object)this;
        float adj = (self.width(text) - self.width(transformed)) / 2.0f;
        inComponentTransform.set(true);
        try {
            cir.setReturnValue(self.drawInBatch(
                    transformed, x + adj, y, color, dropShadow,
                    matrix, buffers, mode, bgColor, packedLight));
        } finally {
            inComponentTransform.set(false);
        }
    }

    // ── drawInBatch(FormattedCharSequence) ────────────────────────────────────

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
        if (text instanceof AffineSequence as) {
            Matrix4f combined = new Matrix4f(matrix).translate(x, y, 0f).mul(as.localTransform());
            // Negative determinant means the transform contains a reflection (e.g. #flip).
            // Flip GL_FRONT_FACE so culled surfaces still render the correct face.
            // Flush the buffer BEFORE resetting GL state — drawInBatch only queues vertices;
            // the actual GL draw happens at flush time, so the state must still be active then.
            boolean flipWinding = as.localTransform().determinant() < 0;
            if (flipWinding) {
                // Flush any geometry accumulated before this sequence (e.g. preceding plain text
                // in a ConcatSequence) so it is drawn with the current GL_CCW state, not GL_CW.
                if (buffers instanceof MultiBufferSource.BufferSource bs) bs.endBatch();
                GL11.glFrontFace(GL11.GL_CW);
            }
            int ret = self.drawInBatch(as.inner(), 0f, 0f, color, dropShadow, combined, buffers, mode, bgColor, packedLight);
            if (flipWinding) {
                if (buffers instanceof MultiBufferSource.BufferSource bs) bs.endBatch();
                GL11.glFrontFace(GL11.GL_CCW);
            }
            cir.setReturnValue(ret);
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
        if (text instanceof AffineSequence as) {
            Matrix4f combined = new Matrix4f(matrix).translate(x, y, 0f).mul(as.localTransform());
            boolean flipWinding = as.localTransform().determinant() < 0;
            if (flipWinding) {
                if (buffers instanceof MultiBufferSource.BufferSource bs) bs.endBatch();
                GL11.glFrontFace(GL11.GL_CW);
            }
            self.drawInBatch8xOutline(as.inner(), 0f, 0f, color, outlineColor, combined, buffers, packedLight);
            if (flipWinding) {
                if (buffers instanceof MultiBufferSource.BufferSource bs) bs.endBatch();
                GL11.glFrontFace(GL11.GL_CCW);
            }
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
        if (text instanceof AffineSequence as) {
            if (as.useInnerWidth()) {
                cir.setReturnValue(self.width(as.inner()));
                return;
            }
            // Transform the bounding box corners through localTransform and return the max X extent.
            // JOML naming: mCR = column C, row R.  result.x = m00*vx + m10*vy + m30
            float w = self.width(as.inner());
            float h = self.lineHeight;
            org.joml.Matrix4f m = as.localTransform();
            float maxX = 0f;
            for (int i = 0; i < 4; i++) {
                float vx = (i == 1 || i == 2) ? w : 0f;
                float vy = (i == 2 || i == 3) ? h : 0f;
                float tx = m.m00() * vx + m.m10() * vy + m.m30();
                if (tx > maxX) maxX = tx;
            }
            cir.setReturnValue(Math.max(0, Math.round(maxX)));
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
        return ImageGlyphPool.IMAGE_FONT.equals(style.getFont());
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
