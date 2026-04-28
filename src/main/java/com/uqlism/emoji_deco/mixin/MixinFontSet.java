package com.uqlism.emoji_deco.mixin;

import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.logging.LogUtils;
import com.uqlism.emoji_deco.render.HeadGlyphInfo;
import com.uqlism.emoji_deco.render.SpriteGlyphInfo;
import com.uqlism.emoji_deco.render.registry.PlayerHeadRegistry;
import com.uqlism.emoji_deco.render.registry.SpriteRegistry;
import net.minecraft.client.Minecraft;
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

    @Shadow(remap = false)
    private ResourceLocation f_95052_;

    // SRG: m_243128_ -> getGlyphInfo(int, boolean)
    @Inject(method = "m_243128_", at = @At("HEAD"), cancellable = true, remap = false)
    private void runicink$injectGlyphInfo(int codePoint, boolean filterFishyGlyphs,
                                          CallbackInfoReturnable<GlyphInfo> cir) {
        if (this.f_95052_ == null) return;
        if (SpriteRegistry.SPRITE_FONT.equals(this.f_95052_)) {
            SpriteRegistry.SpriteKey key = SpriteRegistry.getTexture(codePoint);
            if (key != null) cir.setReturnValue(new SpriteGlyphInfo(key.atlas(), key.sprite()));
        } else if (PlayerHeadRegistry.HEAD_FONT.equals(this.f_95052_)) {
            String username = PlayerHeadRegistry.getUsername(codePoint);
            if (username == null) return;
            ResourceLocation skin = getSkinTexture(username);
            if (skin != null) cir.setReturnValue(new HeadGlyphInfo(skin, PlayerHeadRegistry.isOverlay(codePoint)));
        }
    }

    // SRG: m_95078_ -> getGlyph(int)
    @Inject(method = "m_95078_", at = @At("HEAD"), cancellable = true, remap = false)
    private void runicink$injectGlyph(int codePoint,
                                      CallbackInfoReturnable<BakedGlyph> cir) {
        if (this.f_95052_ == null) return;
        if (SpriteRegistry.SPRITE_FONT.equals(this.f_95052_)) {
            SpriteRegistry.SpriteKey key = SpriteRegistry.getTexture(codePoint);
            if (key == null) {
                LOGGER.warn("[EmojiDeco] No texture mapped for cp=U+{}", Integer.toHexString(codePoint));
                return;
            }
            BakedGlyph glyph = new SpriteGlyphInfo(key.atlas(), key.sprite()).bake(null);
            if (glyph != null) cir.setReturnValue(glyph);
        } else if (PlayerHeadRegistry.HEAD_FONT.equals(this.f_95052_)) {
            String username = PlayerHeadRegistry.getUsername(codePoint);
            if (username == null) return;
            ResourceLocation skin = getSkinTexture(username);
            if (skin == null) return;
            BakedGlyph glyph = new HeadGlyphInfo(skin, PlayerHeadRegistry.isOverlay(codePoint)).bake(null);
            if (glyph != null) cir.setReturnValue(glyph);
        }
    }

    private static ResourceLocation getSkinTexture(String username) {
        Minecraft mc = Minecraft.getInstance();
        // Check local player first (always available)
        if (mc.player != null && mc.player.getName().getString().equalsIgnoreCase(username)) {
            return mc.getSkinManager().getInsecureSkinLocation(mc.player.getGameProfile());
        }
        if (mc.level == null) return null;
        for (var player : mc.level.players()) {
            if (player.getName().getString().equalsIgnoreCase(username)) {
                return mc.getSkinManager().getInsecureSkinLocation(player.getGameProfile());
            }
        }
        return null;
    }
}
