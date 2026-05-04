package com.uqlism.emoji_deco.render.image;

import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.font.SheetGlyphInfo;
import com.mojang.logging.LogUtils;
import com.uqlism.emoji_deco.network.UrlFetcher;
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
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * コードポイントプール（最大 10,000 スロット、2 セグメント構成）。
 *
 * セグメント 1: 0xD000–0xD7FF（2048 スロット、インデックス 0–2047）
 * セグメント 2: 0xE000–0xFEEF（7952 スロット、インデックス 2048–9999）
 * ※ 0xD800–0xDFFF は Java サロゲート範囲のため使用しない。
 *
 * コードポイントの再利用はしない。再利用すると既存 Component が別画像を指してしまう
 * （黒四角・壊れた表示）ため、一度割り当てたスロットはゲームプレイ中は解放しない。
 * プール満杯時は新規画像を tofu として表示する。
 *
 * GPU メモリ管理:
 * - AnimatedGlyphTexture: GPU_UNLOAD_TICKS（5 秒）未使用で VRAM 解放、次回描画時に再アップロード。
 * - StaticGlyphTexture:   同上（CPU コピーを保持するため再アップロードは即座）。
 * - Atlas / Skin（glyphTexture == null）: Minecraft 管理のため解放しない。
 *
 * リソースリロード時: URL 以外のスロットは evict、URL スロットは glyph のみクリア。
 */
public class ImageGlyphPool {

    public static final ResourceLocation IMAGE_FONT = ResourceLocation.parse("emoji_deco:image");
    public static final int  BASE_CP         = 0xD000;
    private static final int  POOL_SIZE        = 10_000;
    /** セグメント 1: 0xD000–0xD7FF */
    private static final int  SEG1_CP          = 0xD000;
    private static final int  SEG1_SIZE        = 0x800;   // 2048
    /** セグメント 2: 0xE000–0xFEEF */
    private static final int  SEG2_CP          = 0xE000;
    /** この tick 数以上描画されなければ GL テクスチャを解放する（30秒）。解放後は次回描画時に再デコードする。 */
    private static final long GPU_UNLOAD_TICKS = 600L;

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
        boolean tofuLogged;

        boolean isEmpty() { return key == null; }

        void evict(Map<String, Integer> index) {
            if (resolved instanceof ResolvedSource.Animated a) a.animator().close();
            else if (resolved instanceof ResolvedSource.Static st && st.glyphTexture() != null)
                st.glyphTexture().close();
            index.remove(key);
            key         = null;
            imageSpec   = null;
            resolved    = null;
            glyph       = null;
            frameGlyphs = null;
            future      = null;
            tofuLogged  = false;
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
        if (existing != null) { slots[existing].lastUsed = tick; return cpForIdx(existing); }

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
        return cpForIdx(idx);
    }

    public static synchronized void tick() {
        tick++;
        long gpuThreshold = tick - GPU_UNLOAD_TICKS;
        for (Slot s : slots) {
            if (s.isEmpty() || s.resolved == null) continue;
            boolean idle = s.lastUsed < gpuThreshold;

            // バックグラウンドリフレッシュ完了 + 長時間非表示: 新テクスチャを破棄して GPU エビクションへ
            if (s.future != null && s.future.isDone() && idle) {
                if (!s.future.isCompletedExceptionally()) {
                    try { closeOwnedTexture(s.future.get()); } catch (Exception ignored) {}
                }
                s.future = null;
            }

            // GPU エビクション: future がない状態で長時間非表示
            if (s.future == null && idle) {
                if (s.resolved instanceof ResolvedSource.Animated a) {
                    a.animator().close();
                } else if (s.resolved instanceof ResolvedSource.Static st && st.glyphTexture() != null) {
                    st.glyphTexture().close();
                } else {
                    continue; // Atlas / Skin: Minecraft 管理のため解放しない
                }
                s.resolved    = null;
                s.glyph       = null;
                s.frameGlyphs = null;
                // s.future は null のまま（次回 getGlyph() で再デコードを起動）
                continue;
            }

            // TTL バックグラウンドリフレッシュ: 表示中の URL 画像が stale になった
            if (s.future == null
                    && s.imageSpec instanceof ImageSpec.Decoded d
                    && d.source() instanceof BinarySource.Url u
                    && UrlFetcher.isStale(u)) {
                s.future = resolveAsync(s); // 旧テクスチャを保持したまま裏で再フェッチ
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
            // 実描画が必要になった初回（または GPU 解放後の再描画時）に非同期解決を起動する
            s.future = resolveAsync(s);
        } else if (s.future != null && s.resolved == null && s.future.isDone()) {
            if (!s.future.isCompletedExceptionally()) {
                try {
                    s.resolved = s.future.get();
                    s.future   = null; // クリアしないと次フレームでスワップ分岐が誤発火する
                } catch (Exception ignored) {}
            } else if (s.imageSpec instanceof ImageSpec.Decoded d
                    && d.source() instanceof BinarySource.Url u) {
                // 失敗 Future の場合は UrlSourceResolver に再問い合わせ。
                // 新規ダウンロードが起動した場合（!isDone）のみ切り替える。
                // isDone() の場合はバイトキャッシュが未失効で即失敗するため切り替えない
                // （毎フレーム失敗 Future を生成するホットループを防ぐ）。
                CompletableFuture<ResolvedSource> retry =
                        UrlSourceResolver.resolve(u, d.format());
                if (!retry.isDone()) s.future = retry;
            }
        } else if (s.future != null && s.resolved != null && s.future.isDone()) {
            // バックグラウンドリフレッシュ完了: 旧テクスチャとシームレスに差し替え
            if (!s.future.isCompletedExceptionally()) {
                try {
                    ResolvedSource fresh = s.future.get();
                    closeOwnedTexture(s.resolved);
                    s.resolved    = fresh;
                    s.glyph       = null;
                    s.frameGlyphs = null;
                } catch (Exception ignored) {}
            }
            s.future = null;
        }

        if (s.resolved == null) {
            if (s.future != null && !s.future.isDone()) return currentLoadingGlyph(s.w, s.h);
            // resolved が null のまま future が完了 → tofu
            if (!s.tofuLogged) {
                s.tofuLogged = true;
                String urlInfo = (s.imageSpec instanceof ImageSpec.Decoded d
                        && d.source() instanceof BinarySource.Url u) ? u.url() : String.valueOf(s.imageSpec);
                Throwable cause = null;
                if (s.future != null) {
                    try { s.future.get(); } catch (java.util.concurrent.ExecutionException ee) { cause = ee.getCause(); }
                    catch (Exception ignored) {}
                }
                if (cause != null) {
                    LOGGER.warn("[EmojiDeco] tofu: {} — {}", urlInfo, cause.toString());
                } else {
                    LOGGER.warn("[EmojiDeco] tofu: {} — resolved が null（future={}）", urlInfo,
                            s.future == null ? "null" : "done");
                }
            }
            return null;
        }

        if (s.resolved instanceof ResolvedSource.Animated a) {
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
        int idx = idxForCp(codePoint);
        if (idx < 0) return null;
        Slot s = slots[idx];
        return s.isEmpty() ? null : s;
    }

    private static int cpForIdx(int idx) {
        return idx < SEG1_SIZE ? SEG1_CP + idx : SEG2_CP + (idx - SEG1_SIZE);
    }

    private static int idxForCp(int cp) {
        if (cp >= SEG1_CP && cp < SEG1_CP + SEG1_SIZE) return cp - SEG1_CP;
        if (cp >= SEG2_CP && cp < SEG2_CP + (POOL_SIZE - SEG1_SIZE)) return SEG1_SIZE + (cp - SEG2_CP);
        return -1;
    }

    /** 自前管理テクスチャを閉じる。Atlas / Skin（glyphTexture == null）は no-op。 */
    private static void closeOwnedTexture(ResolvedSource rs) {
        if (rs instanceof ResolvedSource.Animated a) a.animator().close();
        else if (rs instanceof ResolvedSource.Static st && st.glyphTexture() != null) st.glyphTexture().close();
    }

    private static CompletableFuture<ResolvedSource> resolveAsync(Slot s) {
        if (s.imageSpec instanceof ImageSpec.Decoded d) {
            if (d.source() instanceof BinarySource.Url u)
                return UrlSourceResolver.resolve(u, d.format());
            if (d.source() instanceof BinarySource.Resource r)
                return ResourceSourceResolver.resolveAsync(r.path(), d.format());
        }
        if (s.imageSpec instanceof ImageSpec.Atlas a) {
            ResolvedSource rs = AtlasSourceResolver.resolveSync(a.atlas(), a.sprite());
            return rs != null ? CompletableFuture.completedFuture(rs)
                              : CompletableFuture.failedFuture(new Exception("Atlas sprite not found"));
        }
        if (s.imageSpec instanceof ImageSpec.Skin sk) {
            ResolvedSource rs = SkinSourceResolver.resolveSync(sk.player());
            return rs != null ? CompletableFuture.completedFuture(rs)
                              : CompletableFuture.failedFuture(new Exception("Skin not available"));
        }
        return CompletableFuture.failedFuture(new IllegalStateException("Unknown ImageSpec: " + s.imageSpec));
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
