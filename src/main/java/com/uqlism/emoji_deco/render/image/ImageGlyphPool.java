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
    /** 描画が途絶えてからアニメーションを停止するまでの tick 数（= 1 秒）。 */
    private static final long ANIM_PAUSE_TICKS = 20L;

    @SuppressWarnings("unchecked")
    private static final CompletableFuture<ResolvedSource>[] futures =
            (CompletableFuture<ResolvedSource>[]) new CompletableFuture[POOL_SIZE];

    private static final String[]          slotKeys    = new String[POOL_SIZE];
    private static final JsonObject[]       slotSpecs   = new JsonObject[POOL_SIZE];
    private static final int[][]            slotCrops   = new int[POOL_SIZE][];
    private static final int[]              slotW       = new int[POOL_SIZE];
    private static final int[]              slotH       = new int[POOL_SIZE];
    private static final float[]            slotAdvance = new float[POOL_SIZE];
    private static final long[]             lastUsed    = new long[POOL_SIZE];
    private static final ResolvedSource[]   resolved    = new ResolvedSource[POOL_SIZE];
    private static final BakedGlyph[]       glyphs      = new BakedGlyph[POOL_SIZE];

    private static final Map<String, Integer> keyToSlot = new HashMap<>();
    private static long tick = 0L;

    private ImageGlyphPool() {}

    // ── Public API ────────────────────────────────────────────────────────────

    public static synchronized int getOrAllocate(
            JsonObject sourceSpec, @Nullable int[] crop,
            int displayW, int displayH, float advanceOverride) {
        String key = buildKey(sourceSpec, crop, displayW, displayH, advanceOverride);
        Integer existing = keyToSlot.get(key);
        if (existing != null) { lastUsed[existing] = tick; return BASE_CP + existing; }

        int slot = claimSlot(key);
        slotSpecs[slot]   = sourceSpec;
        slotCrops[slot]   = crop;
        slotW[slot]       = displayW;
        slotH[slot]       = displayH;
        slotAdvance[slot] = advanceOverride;
        lastUsed[slot]    = tick;
        resolved[slot]    = null;
        glyphs[slot]      = null;

        futures[slot] = "url".equals(typeOf(sourceSpec)) ? UrlSourceResolver.resolve(sourceSpec) : null;
        return BASE_CP + slot;
    }

    /**
     * 毎 client tick 呼ぶ。
     * 最近描画されたアニメーションスロットのみ animator.tick() を実行する。
     */
    public static void tick() {
        tick++;
        long threshold = tick - ANIM_PAUSE_TICKS;
        for (int i = 0; i < POOL_SIZE; i++) {
            if (resolved[i] instanceof ResolvedSource.Animated a && lastUsed[i] >= threshold) {
                a.animator().tick(tick);
            }
        }
    }

    public static void onResourceReload() {
        synchronized (ImageGlyphPool.class) {
            for (int i = 0; i < POOL_SIZE; i++) {
                if (slotKeys[i] == null) continue;
                if (!"url".equals(typeOf(slotSpecs[i]))) {
                    evictSlot(i);
                } else {
                    glyphs[i] = null;   // URL: テクスチャ ID が変わりうるので再ベイク
                }
            }
        }
        ResourceSourceResolver.onResourceReload();
    }

    // ── MixinFontSet から呼ばれる（レンダースレッド） ─────────────────────────

    @Nullable
    public static GlyphInfo getGlyphInfo(int codePoint) {
        int slot = codePoint - BASE_CP;
        if (slot < 0 || slot >= POOL_SIZE || slotKeys[slot] == null) return null;
        float adv = Float.isNaN(slotAdvance[slot]) ? slotW[slot] + 1.0f : slotAdvance[slot];
        return new FixedAdvanceInfo(adv);
    }

    @Nullable
    public static BakedGlyph getGlyph(int codePoint) {
        int slot = codePoint - BASE_CP;
        if (slot < 0 || slot >= POOL_SIZE || slotKeys[slot] == null) return null;
        lastUsed[slot] = tick;

        String type = typeOf(slotSpecs[slot]);

        if (futures[slot] == null) {
            ResolvedSource fresh = resolveSync(slot, type);
            if (!Objects.equals(fresh, resolved[slot])) {
                resolved[slot] = fresh;
                glyphs[slot]   = null;
            }
        } else if (resolved[slot] == null && futures[slot].isDone()) {
            try { resolved[slot] = futures[slot].get(); } catch (Exception ignored) {}
        }

        if (resolved[slot] == null) return null;

        if (glyphs[slot] == null) glyphs[slot] = bake(slot);
        return glyphs[slot];
    }

    // ── 内部ヘルパー ──────────────────────────────────────────────────────────

    @Nullable
    private static ResolvedSource resolveSync(int slot, String type) {
        return switch (type) {
            case "atlas"    -> AtlasSourceResolver.resolveSync(slotSpecs[slot]);
            case "resource" -> ResourceSourceResolver.resolveSync(slotSpecs[slot]);
            case "skin"     -> SkinSourceResolver.resolveSync(slotSpecs[slot]);
            default         -> null;
        };
    }

    private static BakedGlyph bake(int slot) {
        ResolvedSource src = resolved[slot];
        int[] crop = slotCrops[slot];
        int displayW = slotW[slot], displayH = slotH[slot];

        float u0, v0, u1, v1;
        if (crop != null && src.nativeW() > 0 && src.nativeH() > 0) {
            float du = src.u1() - src.u0(), dv = src.v1() - src.v0();
            float nw = src.nativeW(), nh = src.nativeH();
            u0 = src.u0() + (crop[0] / nw) * du;
            v0 = src.v0() + (crop[1] / nh) * dv;
            u1 = src.u0() + (crop[2] / nw) * du;
            v1 = src.v0() + (crop[3] / nh) * dv;
        } else {
            u0 = src.u0(); v0 = src.v0(); u1 = src.u1(); v1 = src.v1();
        }

        GlyphRenderTypes rt = GlyphRenderTypes.createForColorTexture(src.texture());
        return new BakedGlyph(rt, u0, u1, v0, v1, 0f, displayW, 3f, 3f + displayH);
    }

    /** スロットを確保する。空きがなければ LRU 退避してから返す。 */
    private static int claimSlot(String key) {
        for (int i = 0; i < POOL_SIZE; i++) {
            if (slotKeys[i] == null) { slotKeys[i] = key; keyToSlot.put(key, i); return i; }
        }
        int oldest = 0;
        for (int i = 1; i < POOL_SIZE; i++) {
            if (lastUsed[i] < lastUsed[oldest]) oldest = i;
        }
        evictSlot(oldest);
        slotKeys[oldest] = key;
        keyToSlot.put(key, oldest);
        return oldest;
    }

    /** スロットを退避する。Animated の場合は animator を close() する。 */
    private static void evictSlot(int slot) {
        if (resolved[slot] instanceof ResolvedSource.Animated a) a.animator().close();
        keyToSlot.remove(slotKeys[slot]);
        slotKeys[slot] = null;
        resolved[slot] = null;
        glyphs[slot]   = null;
        futures[slot]  = null;
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
