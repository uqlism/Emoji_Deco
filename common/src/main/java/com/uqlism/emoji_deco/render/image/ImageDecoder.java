package com.uqlism.emoji_deco.render.image;

import com.mojang.blaze3d.platform.NativeImage;

import java.io.IOException;
import java.util.List;

/** バイト列を NativeImage フレーム列にデコードする。 */
public interface ImageDecoder {

    /**
     * @param data 画像ファイルのバイト列
     * @return フレームリスト。静止画は size()==1、アニメは複数。
     *         durationMs==0 は「タイミング情報なし（静止画）」を意味する。
     */
    List<Frame> decode(byte[] data) throws IOException;

    /** 1フレーム。pixels は呼び出し側が close() する責任を持つ。 */
    record Frame(NativeImage pixels, int durationMs) {}
}
