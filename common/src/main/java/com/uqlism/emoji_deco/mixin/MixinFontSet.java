package com.uqlism.emoji_deco.mixin;

import com.mojang.blaze3d.font.GlyphInfo;
import com.uqlism.emoji_deco.render.image.ImageGlyphPool;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FontSet.class)
public abstract class MixinFontSet {

    @Shadow(remap = false)
    private ResourceLocation name;

    @Inject(method = "getGlyphInfo", at = @At("HEAD"), cancellable = true, remap = false)
    private void runicink$injectGlyphInfo(int codePoint, boolean filterFishyGlyphs,
                                          CallbackInfoReturnable<GlyphInfo> cir) {
        if (!ImageGlyphPool.IMAGE_FONT.equals(this.name)) return;
        GlyphInfo info = ImageGlyphPool.getGlyphInfo(codePoint);
        if (info != null) cir.setReturnValue(info);
    }

    @Inject(method = "getGlyph", at = @At("HEAD"), cancellable = true, remap = false)
    private void runicink$injectGlyph(int codePoint,
                                      CallbackInfoReturnable<BakedGlyph> cir) {
        if (!ImageGlyphPool.IMAGE_FONT.equals(this.name)) return;
        BakedGlyph glyph = ImageGlyphPool.getGlyph(codePoint);
        if (glyph != null) cir.setReturnValue(glyph);
    }
}
