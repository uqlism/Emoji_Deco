package com.uqlism.runicink.mixin;

import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.logging.LogUtils;
import com.uqlism.runicink.text.SpriteGlyphInfo;
import com.uqlism.runicink.text.SpriteRegistry;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FontSet.class)
public abstract class MixinFontSet {

    private static final Logger LOGGER = LogUtils.getLogger();

    // SRG: f_95052_
    @Shadow(remap = false)
    private ResourceLocation f_95052_;

    // SRG: m_243128_ -> getGlyphInfo(int, boolean)
    // advance を返すことでテキストレイアウト(改行計算)に正しい幅を伝える
    @Inject(method = "m_243128_", at = @At("HEAD"), cancellable = true, remap = false)
    private void runicink$injectBlockSpriteInfo(int codePoint, boolean filterFishyGlyphs,
                                                CallbackInfoReturnable<GlyphInfo> cir) {
        if (this.f_95052_ == null) return;
        if (!this.f_95052_.getNamespace().equals("runicink")) return;
        if (!this.f_95052_.equals(new ResourceLocation("runicink", "sprite"))) return;
        SpriteRegistry.SpriteKey spriteKey = SpriteRegistry.getTexture(codePoint);
        if (spriteKey == null) return;
        cir.setReturnValue(new SpriteGlyphInfo(spriteKey.atlas(), spriteKey.sprite()));
    }

    // SRG: m_95078_ -> getGlyph(int)
    @Inject(method = "m_95078_", at = @At("HEAD"), cancellable = true, remap = false)
    private void runicink$injectBlockSprite(int codePoint,
                                            CallbackInfoReturnable<BakedGlyph> cir) {
        if (this.f_95052_ == null) return;
        if (!this.f_95052_.getNamespace().equals("runicink")) return;
        LOGGER.info("[RunicInk] getGlyph fired: font={} cp=U+{}", this.f_95052_, Integer.toHexString(codePoint));
        if (!this.f_95052_.equals(new ResourceLocation("runicink", "sprite"))) return;
        SpriteRegistry.SpriteKey spriteKey = SpriteRegistry.getTexture(codePoint);
        if (spriteKey == null) {
            LOGGER.warn("[RunicInk] No texture mapped for cp=U+{}", Integer.toHexString(codePoint));
            return;
        }
        LOGGER.info("[RunicInk] Baking glyph for {} (cp=U+{})", spriteKey.sprite(), Integer.toHexString(codePoint));
        BakedGlyph glyph = new SpriteGlyphInfo(spriteKey.atlas(), spriteKey.sprite()).bake(null);
        LOGGER.info("[RunicInk] bake() returned: {}", glyph);
        if (glyph != null) cir.setReturnValue(glyph);
    }
}
