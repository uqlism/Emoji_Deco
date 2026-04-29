package com.uqlism.emoji_deco.render;

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
 * 事前デコード済みフレームリストから生成されるアニメーションテクスチャ。
 *
 * コードポイント ↔ ResourceLocation のマッピングは永続。
 * GL テクスチャ（VRAM）は ImageGlyphPool から unloadGpu() が呼ばれたとき解放され、
 * 次回 getGlyph() 時に ensureGpu() で NativeImage から再アップロードする。
 * NativeImage（RAM）は close() まで保持し続ける。
 */
public class AnimatedGlyphTexture extends AbstractTexture {

    private final List<ImageDecoder.Frame> frames;
    private final int[] cumulativeTicks;
    private final int   totalLoopTicks;
    private final int   frameW, frameH;
    private final boolean animated;
    private int  currentFrame = 0;
    private boolean closed    = false;
    private boolean gpuLoaded = false;

    private AnimatedGlyphTexture(List<ImageDecoder.Frame> frames) {
        this.frames   = frames;
        this.animated = frames.size() > 1;
        this.frameW   = frames.get(0).pixels().getWidth();
        this.frameH   = frames.get(0).pixels().getHeight();

        int n = frames.size();
        cumulativeTicks = new int[n];
        int sum = 0;
        for (int i = 0; i < n; i++) {
            cumulativeTicks[i] = sum;
            sum += Math.max(1, frames.get(i).durationMs() / 50);
        }
        totalLoopTicks = Math.max(1, sum);
    }

    /** フレームリストから生成する。レンダースレッドから呼ぶこと。 */
    public static AnimatedGlyphTexture fromFrames(List<ImageDecoder.Frame> frames) {
        if (frames.isEmpty()) throw new IllegalArgumentException("Empty frame list");
        AnimatedGlyphTexture tex = new AnimatedGlyphTexture(frames);
        tex.ensureGpu();
        return tex;
    }

    // ── GPU ストリーミング ────────────────────────────────────────────────────

    /**
     * GL テクスチャが未ロードなら現フレームをアップロードする。
     * レンダースレッドから getGlyph() 経由で呼ばれる。
     */
    public void ensureGpu() {
        if (closed || gpuLoaded) return;
        TextureUtil.prepareImage(getId(), frameW, frameH);
        RenderSystem.bindTexture(getId());
        frames.get(currentFrame).pixels().upload(0, 0, 0, false);
        gpuLoaded = true;
    }

    /**
     * GL テクスチャ ID を解放して VRAM を返却する。NativeImage は保持。
     * 長時間描画されなかったとき ImageGlyphPool から呼ばれる。
     */
    public void unloadGpu() {
        if (!gpuLoaded) return;
        releaseId();   // id を NOT_ASSIGNED に戻す（次の getId() で新規割り当て）
        gpuLoaded = false;
    }

    // ── アニメーション ────────────────────────────────────────────────────────

    /** 絶対 tick からフレームを決定してアップロードする。レンダースレッドから呼ぶこと。 */
    public void tick(long gameTick) {
        if (!animated || closed || !gpuLoaded) return;
        int t = (int)(gameTick % totalLoopTicks);
        int newFrame = 0;
        for (int i = frames.size() - 1; i > 0; i--) {
            if (t >= cumulativeTicks[i]) { newFrame = i; break; }
        }
        if (newFrame != currentFrame) {
            currentFrame = newFrame;
            RenderSystem.bindTexture(getId());
            frames.get(currentFrame).pixels().upload(0, 0, 0, false);
        }
    }

    public boolean isAnimated() { return animated; }

    @Override public void load(ResourceManager rm) {}

    /**
     * TextureManager がリソースリロード時に reset() を呼ぶが、
     * このテクスチャは ImageGlyphPool が管理するため何もしない。
     */
    @Override
    public void reset(TextureManager manager, ResourceManager resourceManager,
                      ResourceLocation location, Executor executor) {}

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        if (gpuLoaded) { releaseId(); gpuLoaded = false; }
        frames.forEach(f -> f.pixels().close());
    }
}
