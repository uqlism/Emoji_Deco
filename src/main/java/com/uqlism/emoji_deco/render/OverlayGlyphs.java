package com.uqlism.emoji_deco.render;

import net.minecraft.client.gui.font.glyphs.BakedGlyph;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class OverlayGlyphs {
    /** Z オフセットを適用すべき BakedGlyph インスタンスを追跡する。WeakHashMap なので GC によって自動解放される。 */
    public static final Set<BakedGlyph> OVERLAYS =
            Collections.newSetFromMap(new WeakHashMap<>());
}
