package com.uqlism.emoji_deco.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.concurrent.Executor;

/**
 * 静止画グリフテクスチャ。ピクセルデータを GPU にアップロードした後は
 * CPU 側コピーを保持しない。非表示後の再描画が必要な場合は ImageGlyphPool が
 * resolved をリセットしてソースから再デコード・再アップロードする。
 */
public class StaticGlyphTexture extends AbstractTexture {

    private boolean closed = false;

    private StaticGlyphTexture() {}

    /**
     * ピクセルデータを GPU にアップロードして返す。
     * pixels はこのメソッド内で close される。レンダースレッドから呼ぶこと。
     */
    public static StaticGlyphTexture fromPixels(NativeImage pixels) {
        StaticGlyphTexture tex = new StaticGlyphTexture();
        TextureUtil.prepareImage(tex.getId(), pixels.getWidth(), pixels.getHeight());
        RenderSystem.bindTexture(tex.getId());
        pixels.upload(0, 0, 0, false);
        pixels.close();
        return tex;
    }

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
