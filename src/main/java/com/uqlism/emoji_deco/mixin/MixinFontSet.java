package com.uqlism.emoji_deco.mixin;

import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.logging.LogUtils;
import com.uqlism.emoji_deco.render.glyphinfo.HeadGlyphInfo;
import com.uqlism.emoji_deco.render.glyphinfo.SpriteGlyphInfo;
import com.uqlism.emoji_deco.render.glyphinfo.TextureGlyphInfo;
import com.uqlism.emoji_deco.render.registry.PlayerHeadRegistry;
import com.uqlism.emoji_deco.render.registry.SpriteRegistry;
import com.uqlism.emoji_deco.render.registry.TextureRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;

@Mixin(FontSet.class)
public abstract class MixinFontSet {

    private static final Logger LOGGER = LogUtils.getLogger();

    @Shadow(remap = false)
    private ResourceLocation f_95052_;

    // Sprite glyph cache: texture never changes, so simple codepoint→BakedGlyph is fine.
    @Unique
    private final Map<Integer, BakedGlyph> runicink$glyphCache = new HashMap<>();

    // Head glyph cache: skin may change after initial load (async download).
    // We track which skin RL the cached BakedGlyph was built from and recreate on change.
    @Unique
    private final Map<Integer, ResourceLocation> runicink$headSkins = new HashMap<>();

    // SRG: m_243128_ -> getGlyphInfo(int, boolean)
    @Inject(method = "m_243128_", at = @At("HEAD"), cancellable = true, remap = false)
    private void runicink$injectGlyphInfo(int codePoint, boolean filterFishyGlyphs,
                                          CallbackInfoReturnable<GlyphInfo> cir) {
        if (this.f_95052_ == null) return;
        if (SpriteRegistry.SPRITE_FONT.equals(this.f_95052_)) {
            SpriteRegistry.SpriteKey key = SpriteRegistry.getTexture(codePoint);
            if (key != null) cir.setReturnValue(new SpriteGlyphInfo(key.atlas(), key.sprite(), key.width(), key.height()));
        } else if (TextureRegistry.TEXTURE_FONT.equals(this.f_95052_)) {
            TextureRegistry.TextureKey key = TextureRegistry.getTextureKey(codePoint);
            if (key != null) cir.setReturnValue(new TextureGlyphInfo(key.texture(), key.width(), key.height()));
        } else if (PlayerHeadRegistry.HEAD_FONT.equals(this.f_95052_)) {
            String username = PlayerHeadRegistry.getUsername(codePoint);
            if (username == null) return;
            ResourceLocation skin = getSkinTexture(username);
            if (skin != null) cir.setReturnValue(new HeadGlyphInfo(skin, false));
        } else if (PlayerHeadRegistry.HEAD_OVERLAY_FONT.equals(this.f_95052_)) {
            String username = PlayerHeadRegistry.getUsername(codePoint);
            if (username == null) return;
            ResourceLocation skin = getSkinTexture(username);
            if (skin != null) cir.setReturnValue(new HeadGlyphInfo(skin, true));
        }
    }

    // SRG: m_95078_ -> getGlyph(int)
    @Inject(method = "m_95078_", at = @At("HEAD"), cancellable = true, remap = false)
    private void runicink$injectGlyph(int codePoint,
                                      CallbackInfoReturnable<BakedGlyph> cir) {
        if (this.f_95052_ == null) return;

        if (SpriteRegistry.SPRITE_FONT.equals(this.f_95052_)) {
            BakedGlyph cached = runicink$glyphCache.get(codePoint);
            if (cached != null) { cir.setReturnValue(cached); return; }
            SpriteRegistry.SpriteKey key = SpriteRegistry.getTexture(codePoint);
            if (key == null) {
                LOGGER.warn("[EmojiDeco] No texture mapped for cp=U+{}", Integer.toHexString(codePoint));
                return;
            }
            BakedGlyph glyph = new SpriteGlyphInfo(key.atlas(), key.sprite(), key.width(), key.height()).bake(null);
            if (glyph == null) return;
            runicink$glyphCache.put(codePoint, glyph);
            cir.setReturnValue(glyph);

        } else if (TextureRegistry.TEXTURE_FONT.equals(this.f_95052_)) {
            TextureRegistry.TextureKey key = TextureRegistry.getTextureKey(codePoint);
            if (key == null) return;
            TextureRegistry.markUsed(codePoint);
            if (TextureRegistry.isEvicted(codePoint)) {
                // GL texture was released by LRU; force re-bake and clear the flag.
                TextureRegistry.clearEvicted(codePoint);
                runicink$glyphCache.remove(codePoint);
            } else {
                BakedGlyph cached = runicink$glyphCache.get(codePoint);
                if (cached != null) { cir.setReturnValue(cached); return; }
            }
            BakedGlyph glyph = new TextureGlyphInfo(key.texture(), key.width(), key.height()).bake(null);
            runicink$glyphCache.put(codePoint, glyph);
            cir.setReturnValue(glyph);

        } else if (PlayerHeadRegistry.HEAD_FONT.equals(this.f_95052_)
                || PlayerHeadRegistry.HEAD_OVERLAY_FONT.equals(this.f_95052_)) {
            boolean overlay = PlayerHeadRegistry.HEAD_OVERLAY_FONT.equals(this.f_95052_);
            String username = PlayerHeadRegistry.getUsername(codePoint);
            if (username == null) return;
            ResourceLocation skin = getSkinTexture(username);
            if (skin == null) return;

            // Invalidate cached BakedGlyph when the skin ResourceLocation changes
            // (e.g. default skin → real skin after async download completes).
            ResourceLocation prevSkin = runicink$headSkins.get(codePoint);
            if (skin.equals(prevSkin)) {
                BakedGlyph cached = runicink$glyphCache.get(codePoint);
                if (cached != null) { cir.setReturnValue(cached); return; }
            }

            BakedGlyph glyph = new HeadGlyphInfo(skin, overlay).bake(null);
            runicink$glyphCache.put(codePoint, glyph);
            runicink$headSkins.put(codePoint, skin);
            cir.setReturnValue(glyph);
        }
    }

    private static ResourceLocation getSkinTexture(String username) {
        // Return already-confirmed real skin immediately.
        ResourceLocation cached = PlayerHeadRegistry.getCachedSkin(username);
        if (cached != null) return cached;

        Minecraft mc = Minecraft.getInstance();
        ResourceLocation skin = null;
        if (mc.player != null && mc.player.getName().getString().equalsIgnoreCase(username)) {
            // getSkinTextureLocation() triggers async skin loading if not yet started.
            skin = mc.player.getSkinTextureLocation();
        } else if (mc.level != null) {
            for (AbstractClientPlayer player : mc.level.players()) {
                if (player.getName().getString().equalsIgnoreCase(username)) {
                    skin = player.getSkinTextureLocation();
                    break;
                }
            }
        }
        // Cache only once the real (non-default) skin has loaded.
        // Default skins have paths like "textures/entity/player/wide/steve.png".
        if (skin != null && !isDefaultSkin(skin)) {
            PlayerHeadRegistry.cacheSkin(username, skin);
        }
        return skin;
    }

    private static boolean isDefaultSkin(ResourceLocation skin) {
        return skin.getNamespace().equals("minecraft")
                && skin.getPath().startsWith("textures/entity/player/");
    }
}
