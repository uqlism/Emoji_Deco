package com.uqlism.emoji_deco.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.uqlism.emoji_deco.render.image.ImageDecoder;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * アニメーショングリフテクスチャ。
 *
 * 全フレームを縦並びアトラス（W × H×N）として GPU にアップロードし、
 * アップロード後は CPU 側コピーを保持しない。フレーム切り替えは UV 座標のみ。
 *
 * 非表示期間後の再描画が必要な場合は、ImageGlyphPool が resolved をリセットし
 * ソースから再デコード・再アップロードする（ensureGpu/unloadGpu は廃止）。
 */
public class AnimatedGlyphTexture extends AbstractTexture {

    private final int numFrames;
    private final int frameH;
    private final long[] cumulativeMs;
    private final long   totalLoopMs;
    private final boolean animated;
    private int  currentFrame = 0;
    private boolean closed    = false;

    private AnimatedGlyphTexture(List<ImageDecoder.Frame> frames) {
        numFrames = frames.size();
        animated  = numFrames > 1;
        int fw = frames.get(0).pixels().getWidth();
        frameH = frames.get(0).pixels().getHeight();

        // アトラスを構築して即 GPU にアップロードし、CPU コピーは解放する
        try (NativeImage atlas = new NativeImage(fw, frameH * numFrames, false)) {
            for (int i = 0; i < numFrames; i++) {
                NativeImage src = frames.get(i).pixels();
                for (int y = 0; y < frameH; y++)
                    for (int x = 0; x < fw; x++)
                        atlas.setPixelRGBA(x, i * frameH + y, src.getPixelRGBA(x, y));
                src.close();
            }
            TextureUtil.prepareImage(getId(), fw, frameH * numFrames);
            RenderSystem.bindTexture(getId());
            atlas.upload(0, 0, 0, false);
        }

        int n = numFrames;
        cumulativeMs = new long[n];
        long sum = 0;
        for (int i = 0; i < n; i++) {
            cumulativeMs[i] = sum;
            sum += Math.max(1, frames.get(i).durationMs());
        }
        totalLoopMs = sum;
    }

    /** フレームリストから生成する。レンダースレッドから呼ぶこと。 */
    public static AnimatedGlyphTexture fromFrames(List<ImageDecoder.Frame> frames) {
        if (frames.isEmpty()) throw new IllegalArgumentException("Empty frame list");
        return new AnimatedGlyphTexture(frames);
    }

    // ── アニメーション ────────────────────────────────────────────────────────

    /** 現在のウォールクロック時刻からフレームを更新する。GPU 転送なし。 */
    public void tickMs(long currentMs) {
        if (!animated || closed) return;
        long t = currentMs % totalLoopMs;
        int newFrame = 0;
        for (int i = numFrames - 1; i > 0; i--) {
            if (t >= cumulativeMs[i]) { newFrame = i; break; }
        }
        currentFrame = newFrame;
    }

    public int numFrames()           { return numFrames; }
    public int currentFrame()        { return currentFrame; }
    /** フレーム i のアトラス内 V 開始座標（0–1 正規化）。 */
    public float frameV0(int frame)  { return (float) frame / numFrames; }
    /** フレーム i のアトラス内 V 終端座標（0–1 正規化）。 */
    public float frameV1(int frame)  { return (float) (frame + 1) / numFrames; }

    public boolean isAnimated() { return animated; }

    @Override public void load(ResourceManager rm) {}

    @Override
    public void reset(TextureManager manager, ResourceManager resourceManager,
                      ResourceLocation location, Executor executor) {}

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        releaseId();
    }
}
