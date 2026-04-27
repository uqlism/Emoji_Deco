package com.uqlism.emoji_deco.mixin;

import com.uqlism.emoji_deco.render.HeadOverlayGlyphs;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BakedGlyph.class)
public class MixinBakedGlyph {

    // m_5626_ = render(boolean, float, float, Matrix4f, VertexConsumer, float, float, float, float, int)
    @Redirect(
        method = "m_5626_",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;m_252986_(Lorg/joml/Matrix4f;FFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;"
        ),
        remap = false
    )
    private VertexConsumer runicink$shiftOverlayZ(
            VertexConsumer buffer, Matrix4f matrix, float x, float y, float z) {
        float newZ = HeadOverlayGlyphs.OVERLAYS.contains((BakedGlyph)(Object)this) ? 0.01f : z;
        return buffer.vertex(matrix, x, y, newZ);
    }
}
