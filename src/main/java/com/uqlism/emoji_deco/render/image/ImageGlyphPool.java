package com.uqlism.emoji_deco.render.image;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.font.SheetGlyphInfo;
import com.uqlism.emoji_deco.render.image.source.AtlasSourceResolver;
import com.uqlism.emoji_deco.render.image.source.ResourceSourceResolver;
import com.uqlism.emoji_deco.render.image.source.SkinSourceResolver;
import com.uqlism.emoji_deco.render.image.source.UrlSourceResolver;
import net.minecraft.client.gui.font.GlyphRenderTypes;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * 固定サイズ（POOL_SIZE スロット）のコードポイントプール。
 *
 * tick 最適化: アニメーションは ANIM_PAUSE_TICKS 以内に描画されたスロットのみ進める。
 * 退避クリーンアップ: LRU 退避時に Animated スロットの animator.close() を呼ぶ。
 */
public class ImageGlyphPool {

    public static final ResourceLocation IMAGE_FONT = ResourceLocation.parse("emoji_deco:image");
    public static final int BASE_CP    = 0xD000;
    private static final int POOL_SIZE = 256;
    private static final long ANIM_PAUSE_TICKS = 20L;

    // ── スロット ──────────────────────────────────────────────────────────────

    /** 1スロット分の全状態。key == null なら空きスロット。 */
    private static final class Slot {
        // 割り当て時に確定する不変情報
        String     key;
        JsonObject spec;
        int[]      crop;
        int        w, h;
        float      advance;
        // 実行時に変化する状態
        long       lastUsed;
        ResolvedSource resolved;
        BakedGlyph     glyph;
        CompletableFuture<ResolvedSource> future;

        boolean isEmpty() { return key == null; }

        /** LRU 退避。Animated なら animator を close() する。 */
        void evict(Map<String, Integer> index) {
            if (resolved instanceof ResolvedSource.Animated a) a.animator().close();
            index.remove(key);
            key      = null;
            resolved = null;
            glyph    = null;
            future   = null;
        }
    }

    private static final Slot[] slots = new Slot[POOL_SIZE];
    static { for (int i = 0; i < POOL_SIZE; i++) slots[i] = new Slot(); }

    private static final Map<String, Integer> keyToSlot = new HashMap<>();
    private static long tick = 0L;

    private ImageGlyphPool() {}

    // ── Public API ────────────────────────────────────────────────────────────

    public static synchronized int getOrAllocate(
            JsonObject sourceSpec, @Nullable int[] crop,
            int displayW, int displayH, float advanceOverride) {
        String key = buildKey(sourceSpec, crop, displayW, displayH, advanceOverride);
        Integer existing = keyToSlot.get(key);
        if (existing != null) { slots[existing].lastUsed = tick; return BASE_CP + existing; }

        int idx = claimSlot(key);
        Slot s    = slots[idx];
        s.key     = key;
        s.spec    = sourceSpec;
        s.crop    = crop;
        s.w       = displayW;
        s.h       = displayH;
        s.advance = advanceOverride;
        s.lastUsed = tick;
        s.resolved = null;
        s.glyph    = null;
        s.future   = "emoji_deco:url".equals(typeOf(sourceSpec))
                ? UrlSourceResolver.resolve(sourceSpec) : null;
        return BASE_CP + idx;
    }

    public static void tick() {
        tick++;
        long threshold = tick - ANIM_PAUSE_TICKS;
        for (Slot s : slots) {
            if (s.resolved instanceof ResolvedSource.Animated a && s.lastUsed >= threshold) {
                a.animator().tick(tick);
            }
        }
    }

    public static void onResourceReload() {
        synchronized (ImageGlyphPool.class) {
            for (Slot s : slots) {
                if (s.isEmpty()) continue;
                if (!"emoji_deco:url".equals(typeOf(s.spec))) s.evict(keyToSlot);
                else s.glyph = null;
            }
        }
        ResourceSourceResolver.onResourceReload();
    }

    // ── MixinFontSet から呼ばれる（レンダースレッド） ─────────────────────────

    @Nullable
    public static GlyphInfo getGlyphInfo(int codePoint) {
        Slot s = slotAt(codePoint);
        if (s == null) return null;
        float adv = Float.isNaN(s.advance) ? s.w + 1.0f : s.advance;
        return new FixedAdvanceInfo(adv);
    }

    @Nullable
    public static BakedGlyph getGlyph(int codePoint) {
        Slot s = slotAt(codePoint);
        if (s == null) return null;
        s.lastUsed = tick;

        if (s.future == null) {
            ResolvedSource fresh = resolveSync(s);
            if (!Objects.equals(fresh, s.resolved)) { s.resolved = fresh; s.glyph = null; }
        } else if (s.resolved == null && s.future.isDone()) {
            try { s.resolved = s.future.get(); } catch (Exception ignored) {}
        }

        if (s.resolved == null) return null;
        if (s.glyph   == null) s.glyph = bake(s);
        return s.glyph;
    }

    // ── 内部ヘルパー ──────────────────────────────────────────────────────────

    @Nullable
    private static Slot slotAt(int codePoint) {
        int idx = codePoint - BASE_CP;
        if (idx < 0 || idx >= POOL_SIZE) return null;
        Slot s = slots[idx];
        return s.isEmpty() ? null : s;
    }

    @Nullable
    private static ResolvedSource resolveSync(Slot s) {
        return switch (typeOf(s.spec)) {
            case "emoji_deco:atlas"    -> AtlasSourceResolver.resolveSync(s.spec);
            case "emoji_deco:resource" -> ResourceSourceResolver.resolveSync(s.spec);
            case "emoji_deco:skin"     -> SkinSourceResolver.resolveSync(s.spec);
            default                    -> null;
        };
    }

    private static BakedGlyph bake(Slot s) {
        ResolvedSource src = s.resolved;
        float u0, v0, u1, v1;
        if (s.crop != null && src.nativeW() > 0 && src.nativeH() > 0) {
            float du = src.u1() - src.u0(), dv = src.v1() - src.v0();
            float nw = src.nativeW(), nh = src.nativeH();
            u0 = src.u0() + (s.crop[0] / nw) * du;
            v0 = src.v0() + (s.crop[1] / nh) * dv;
            u1 = src.u0() + (s.crop[2] / nw) * du;
            v1 = src.v0() + (s.crop[3] / nh) * dv;
        } else {
            u0 = src.u0(); v0 = src.v0(); u1 = src.u1(); v1 = src.v1();
        }
        GlyphRenderTypes rt = GlyphRenderTypes.createForColorTexture(src.texture());
        return new BakedGlyph(rt, u0, u1, v0, v1, 0f, s.w, 3f, 3f + s.h);
    }

    private static int claimSlot(String key) {
        for (int i = 0; i < POOL_SIZE; i++) {
            if (slots[i].isEmpty()) { slots[i].key = key; keyToSlot.put(key, i); return i; }
        }
        int oldest = 0;
        for (int i = 1; i < POOL_SIZE; i++) {
            if (slots[i].lastUsed < slots[oldest].lastUsed) oldest = i;
        }
        slots[oldest].evict(keyToSlot);
        slots[oldest].key = key;
        keyToSlot.put(key, oldest);
        return oldest;
    }

    private static String typeOf(@Nullable JsonObject spec) {
        return (spec != null && spec.has("type")) ? spec.get("type").getAsString() : "";
    }

    private static String buildKey(JsonObject spec, @Nullable int[] crop,
                                   int w, int h, float advance) {
        return spec.toString() + Arrays.toString(crop) + w + "x" + h + ":" + advance;
    }

    private record FixedAdvanceInfo(float advance) implements GlyphInfo {
        @Override public float getAdvance()    { return advance; }
        @Override public float getBoldOffset() { return 0f; }
        @Override public BakedGlyph bake(Function<SheetGlyphInfo, BakedGlyph> fn) { return null; }
    }
}
