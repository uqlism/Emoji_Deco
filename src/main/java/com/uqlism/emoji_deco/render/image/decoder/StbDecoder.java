package com.uqlism.emoji_deco.render.image.decoder;

import com.mojang.blaze3d.platform.NativeImage;
import com.uqlism.emoji_deco.render.image.ImageDecoder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

/**
 * STB（NativeImage 経由）で PNG / JPEG をデコードする。
 * 静止画のみ（フレーム数は常に 1）。
 */
public class StbDecoder implements ImageDecoder {

    @Override
    public List<Frame> decode(byte[] data) throws IOException {
        NativeImage img = NativeImage.read(new ByteArrayInputStream(data));
        return List.of(new Frame(img, 0));
    }
}
