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
 * GL テクスチャは 1フレーム分のサイズで確保し、tick(gameTick) の呼び出し時に
 * gameTick % totalLoopTicks でフレームを決定してアップロードする。
 * 絶対時刻ベースのため、オフスクリーン復帰時も整合性が保たれる。
 */
public class AnimatedGlyphTexture extends AbstractTexture {

    private final List<ImageDecoder.Frame> frames;
    private final int[] cumulativeTicks;
    private final int   totalLoopTicks;
    private final int   frameW, frameH;
    private final boolean animated;
    private int currentFrame = 0;
    private boolean closed = false;

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
            sum += Math.max(1, frames.get(i).durationMs() / 50);  // ms → tick
        }
        totalLoopTicks = Math.max(1, sum);
    }

    /**
     * フレームリストから生成する。レンダースレッドから呼ぶこと。
     * frames の所有権は移る（close() で解放される）。
     */
    public static AnimatedGlyphTexture fromFrames(List<ImageDecoder.Frame> frames) {
        if (frames.isEmpty()) throw new IllegalArgumentException("Empty frame list");
        AnimatedGlyphTexture tex = new AnimatedGlyphTexture(frames);
        TextureUtil.prepareImage(tex.getId(), tex.frameW, tex.frameH);
        RenderSystem.bindTexture(tex.getId());
        frames.get(0).pixels().upload(0, 0, 0, false);
        return tex;
    }

    /**
     * 絶対 tick からフレームを決定してアップロードする。レンダースレッドから呼ぶこと。
     */
    public void tick(long gameTick) {
        if (!animated || closed) return;
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

    /** AbstractTexture が要求する load() は使用しない。 */
    @Override
    public void load(ResourceManager rm) {}

    /**
     * TextureManager がリソースリロード時に reset() → close() → load() を呼ぶが、
     * このテクスチャは ImageGlyphPool が管理するため何もしない。
     * reset() をオーバーライドしないと close() が呼ばれて NativeImage が解放されクラッシュする。
     */
    @Override
    public void reset(TextureManager manager, ResourceManager resourceManager,
                      ResourceLocation location, Executor executor) {
        // リソースリロードに影響されない — ImageGlyphPool.onResourceReload() で管理する
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        frames.forEach(f -> f.pixels().close());
        releaseId();
    }
}
