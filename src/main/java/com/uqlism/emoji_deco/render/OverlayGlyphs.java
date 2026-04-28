package com.uqlism.emoji_deco.render;

import net.minecraft.client.gui.font.glyphs.BakedGlyph;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class OverlayGlyphs {
    /**
     * Z オフセットを適用すべき BakedGlyph インスタンスを追跡する。WeakHashMap なので GC によって自動解放される。
     *
     * <p>スレッド安全性: 書き込み（HeadGlyphInfo.bake）・読み取り（MixinBakedGlyph）は
     * 現状どちらもレンダースレッドからのみ呼ばれるため非同期の問題は発生しない。
     * bake() がワーカースレッドから呼ばれるようになった場合は
     * Collections.synchronizedSet(...) または ConcurrentHashMap.newKeySet() への変更が必要。
     */
    public static final Set<BakedGlyph> OVERLAYS =
            Collections.newSetFromMap(new WeakHashMap<>());
}
