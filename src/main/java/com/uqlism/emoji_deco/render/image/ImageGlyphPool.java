package com.uqlism.emoji_deco.render.image;

import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.font.SheetGlyphInfo;
import com.mojang.logging.LogUtils;
import com.uqlism.emoji_deco.render.image.source.AtlasSourceResolver;
import com.uqlism.emoji_deco.render.image.source.ResourceSourceResolver;
import com.uqlism.emoji_deco.render.image.source.SkinSourceResolver;
import com.uqlism.emoji_deco.render.image.source.UrlSourceResolver;
import net.minecraft.client.gui.font.GlyphRenderTypes;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * コードポイントプール（0xD000–0xD7FF、最大 2048 スロット）。
 *
 * LRU 退避は行わない。コードポイントを再利用すると既存の Component が
 * 別の画像を指してしまう（黒四角・壊れた表示）ため、一度割り当てたスロットは
 * ゲームプレイ中は解放しない。プール満杯時は新規画像を tofu として表示する。
 *
 * リソースリロード時: URL 以外のスロットは evict、URL スロットは glyph のみクリア。
 * tick 最適化: ANIM_PAUSE_TICKS 以内に描画されたアニメーションスロットのみ進める。
 */
public class ImageGlyphPool {

    public static final ResourceLocation IMAGE_FONT = ResourceLocation.parse("emoji_deco:image");
    public static final int  BASE_CP         = 0xD000;
    /** 0xD000+2048 = 0xD800（サロゲート開始）の直前まで安全に使用できる最大値 */
    private static final int  POOL_SIZE        = 2048;
    /** この tick 数以上描画されなければ GL テクスチャを解放する（5秒） */
    private static final long GPU_UNLOAD_TICKS = 100L;

    private static final Logger LOGGER = LogUtils.getLogger();

    // ── スロット ──────────────────────────────────────────────────────────────

    private static final class Slot {
        String     key;
        ImageSpec  imageSpec;
        int[]      crop;
        int        w, h;
        float      advance;
        long       lastUsed;
        ResolvedSource resolved;
        BakedGlyph     glyph;
        BakedGlyph[]   frameGlyphs;
        CompletableFuture<ResolvedSource> future;

        boolean isEmpty() { return key == null; }

        void evict(Map<String, Integer> index) {
            if (resolved instanceof ResolvedSource.Animated a) a.animator().close();
            index.remove(key);
            key         = null;
            imageSpec   = null;
            resolved    = null;
            glyph       = null;
            frameGlyphs = null;
            future      = null;
        }
    }

    private static final Slot[] slots = new Slot[POOL_SIZE];
    static { for (int i = 0; i < POOL_SIZE; i++) slots[i] = new Slot(); }

    private static final Map<String, Integer> keyToSlot = new HashMap<>();
    private static long tick = 0L;
    private static boolean poolFullLogged = false;

    // URL フェッチ中のプレースホルダー（ローディングアニメーション）
    private static final Slot loadingSlot = new Slot();
    static {
        loadingSlot.key     = "__loading__";
        loadingSlot.advance = Float.NaN;
    }

    private ImageGlyphPool() {}

    // ── Public API ────────────────────────────────────────────────────────────

    public static synchronized int getOrAllocate(
            ImageSpec imageSpec, @Nullable int[] crop,
            int displayW, int displayH, float advanceOverride) {
        String key = buildKey(imageSpec, crop, displayW, displayH, advanceOverride);
        Integer existing = keyToSlot.get(key);
        if (existing != null) { slots[existing].lastUsed = tick; return BASE_CP + existing; }

        int idx = claimSlot(key);
        if (idx < 0) return 0; // pool full → tofu (0 はフォントに存在しないコードポイント)

        Slot s     = slots[idx];
        s.key      = key;
        s.imageSpec = imageSpec;
        s.crop     = crop;
        s.w        = displayW;
        s.h        = displayH;
        s.advance  = advanceOverride;
        s.lastUsed = tick;
        s.resolved = null;
        s.glyph    = null;
        s.future   = null; // URL フェッチは getGlyph() 呼び出し時（実描画時）まで遅延する
        return BASE_CP + idx;
    }

    public static synchronized void tick() {
        tick++;
        long gpuThreshold = tick - GPU_UNLOAD_TICKS;
        for (Slot s : slots) {
            if (!(s.resolved instanceof ResolvedSource.Animated a)) continue;
            if (s.lastUsed < gpuThreshold) {
                a.animator().unloadGpu();  // 長時間非表示: VRAM 解放
            }
        }
    }

    public static void onResourceReload() {
        synchronized (ImageGlyphPool.class) {
            for (Slot s : slots) {
                if (s.isEmpty()) continue;
                boolean isUrl = s.imageSpec instanceof ImageSpec.Decoded d
                    && d.source() instanceof BinarySource.Url;
                if (!isUrl) {
                    s.evict(keyToSlot);
                } else {
                    // URL テクスチャはコードポイントを維持し glyph だけ再ベイク
                    s.glyph       = null;
                    s.frameGlyphs = null;
                }
            }
            poolFullLogged = false;
            // ローディングスロットをリセット（リロード後に再取得させる）
            if (loadingSlot.resolved instanceof ResolvedSource.Animated a) a.animator().close();
            loadingSlot.resolved    = null;
            loadingSlot.glyph       = null;
            loadingSlot.frameGlyphs = null;
        }
        ResourceSourceResolver.onResourceReload();
    }

    // ── MixinFontSet から呼ばれる ──────────────────────────────────────────────

    @Nullable
    public static synchronized GlyphInfo getGlyphInfo(int codePoint) {
        Slot s = slotAt(codePoint);
        if (s == null) return null;
        float adv = Float.isNaN(s.advance) ? s.w + 1.0f : s.advance;
        return new FixedAdvanceInfo(adv);
    }

    @Nullable
    public static synchronized BakedGlyph getGlyph(int codePoint) {
        Slot s = slotAt(codePoint);
        if (s == null) return null;
        s.lastUsed = tick;

        if (s.future == null && s.resolved == null) {
            if (s.imageSpec instanceof ImageSpec.Decoded d && d.source() instanceof BinarySource.Url u) {
                // 実描画が必要になった初回にフェッチ起動（getOrAllocate 時には起動しない）
                s.future = UrlSourceResolver.resolve(u.url(), d.format(), u.diskCache(), u.ttlSeconds());
            } else {
                ResolvedSource fresh = resolveSync(s);
                if (!Objects.equals(fresh, s.resolved)) {
                    s.resolved    = fresh;
                    s.glyph       = null;
                    s.frameGlyphs = null;
                }
            }
        } else if (s.future != null && s.resolved == null && s.future.isDone()) {
            if (!s.future.isCompletedExceptionally()) {
                try { s.resolved = s.future.get(); } catch (Exception ignored) {}
            } else if (s.imageSpec instanceof ImageSpec.Decoded d
                    && d.source() instanceof BinarySource.Url u) {
                // 失敗 Future の場合は UrlSourceResolver に再問い合わせ。
                // RETRY_DELAY_MS 経過後に新しい Future が発行されていれば切り替え
                // （未経過なら同じ失敗 Future が返るので切り替えは起きない）。
                CompletableFuture<ResolvedSource> retry =
                        UrlSourceResolver.resolve(u.url(), d.format(), u.diskCache(), u.ttlSeconds());
                if (retry != s.future) s.future = retry;
            }
        }

        if (s.resolved == null) {
            // フェッチ中のみローディングアニメーションを表示、失敗時は tofu のまま
            return (s.future != null && !s.future.isDone()) ? currentLoadingGlyph(s.w, s.h) : null;
        }

        if (s.resolved instanceof ResolvedSource.Animated a) {
            // 非表示期間中に GL テクスチャが解放されていれば再アップロード
            a.animator().ensureGpu();
            // フレーム更新はウォールクロックで行う（GPU 転送なし）
            a.animator().tickMs(System.currentTimeMillis());
            if (s.frameGlyphs == null) s.frameGlyphs = bakeAllFrames(s, a);
            return s.frameGlyphs[a.animator().currentFrame()];
        }

        if (s.glyph == null) s.glyph = bake(s);
        return s.glyph;
    }

    // ── 内部ヘルパー ──────────────────────────────────────────────────────────

    /** URL フェッチ中に表示するローディングアニメーションの現フレームを返す。 */
    @Nullable
    private static BakedGlyph currentLoadingGlyph(int w, int h) {
        if (loadingSlot.resolved == null) {
            ResolvedSource r = ResourceSourceResolver.resolveSync(
                    "emoji_deco:textures/loading.gif", null);
            if (r == null) return null;
            loadingSlot.resolved    = r;
            loadingSlot.glyph       = null;
            loadingSlot.frameGlyphs = null;
        }
        // 表示サイズが変わった場合はキャッシュを破棄して再 bake
        if (loadingSlot.w != w || loadingSlot.h != h) {
            loadingSlot.w           = w;
            loadingSlot.h           = h;
            loadingSlot.glyph       = null;
            loadingSlot.frameGlyphs = null;
        }
        if (loadingSlot.resolved instanceof ResolvedSource.Animated a) {
            a.animator().ensureGpu();
            a.animator().tickMs(System.currentTimeMillis());
            if (loadingSlot.frameGlyphs == null)
                loadingSlot.frameGlyphs = bakeAllFrames(loadingSlot, a);
            return loadingSlot.frameGlyphs[a.animator().currentFrame()];
        }
        if (loadingSlot.glyph == null) loadingSlot.glyph = bake(loadingSlot);
        return loadingSlot.glyph;
    }

    @Nullable
    private static Slot slotAt(int codePoint) {
        int idx = codePoint - BASE_CP;
        if (idx < 0 || idx >= POOL_SIZE) return null;
        Slot s = slots[idx];
        return s.isEmpty() ? null : s;
    }

    @Nullable
    private static ResolvedSource resolveSync(Slot s) {
        if (s.imageSpec instanceof ImageSpec.Decoded d && d.source() instanceof BinarySource.Resource r)
            return ResourceSourceResolver.resolveSync(r.path(), d.format());
        if (s.imageSpec instanceof ImageSpec.Atlas a)
            return AtlasSourceResolver.resolveSync(a.atlas(), a.sprite());
        if (s.imageSpec instanceof ImageSpec.Skin sk)
            return SkinSourceResolver.resolveSync(sk.player());
        return null;
    }

    private static BakedGlyph[] bakeAllFrames(Slot s, ResolvedSource.Animated a) {
        int n = a.animator().numFrames();
        GlyphRenderTypes rt = GlyphRenderTypes.createForColorTexture(a.texture());
        BakedGlyph[] glyphs = new BakedGlyph[n];
        for (int i = 0; i < n; i++) {
            glyphs[i] = new BakedGlyph(rt, 0f, 1f,
                    a.animator().frameV0(i), a.animator().frameV1(i),
                    0f, s.w, 3f, 3f + s.h);
        }
        return glyphs;
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

    /** 空きスロットを確保して返す。プールが満杯なら -1 を返す（LRU 退避は行わない）。 */
    private static int claimSlot(String key) {
        for (int i = 0; i < POOL_SIZE; i++) {
            if (slots[i].isEmpty()) {
                slots[i].key = key;
                keyToSlot.put(key, i);
                return i;
            }
        }
        if (!poolFullLogged) {
            LOGGER.warn("[EmojiDeco] Image glyph pool is full ({} slots). New images will appear as tofu.", POOL_SIZE);
            poolFullLogged = true;
        }
        return -1;
    }

    private static String buildKey(ImageSpec imageSpec, @Nullable int[] crop,
                                   int w, int h, float advance) {
        return imageSpec.toString() + Arrays.toString(crop) + w + "x" + h + ":" + advance;
    }

    private record FixedAdvanceInfo(float advance) implements GlyphInfo {
        @Override public float getAdvance()    { return advance; }
        @Override public float getBoldOffset() { return 0f; }
        @Override public BakedGlyph bake(Function<SheetGlyphInfo, BakedGlyph> fn) { return null; }
    }
}
