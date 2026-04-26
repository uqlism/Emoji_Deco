package com.uqlism.runicink.mixin;

import com.uqlism.runicink.text.HeaderScaledSequence;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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
        if (!(text instanceof HeaderScaledSequence hss)) return;
        float s = hss.scale();
        Matrix4f scaled = new Matrix4f(matrix).scale(s, s, 1.0f);
        Font self = (Font)(Object)this;
        self.drawInBatch8xOutline(hss.inner(), x, y / s, color, outlineColor, scaled, buffers, packedLight);
        ci.cancel();
    }
}
