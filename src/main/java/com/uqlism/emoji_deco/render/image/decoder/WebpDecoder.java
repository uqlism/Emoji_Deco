package com.uqlism.emoji_deco.render.image.decoder;

import com.mojang.blaze3d.platform.NativeImage;
import com.uqlism.emoji_deco.render.image.ImageDecoder;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * WebP デコーダ。
 * 静止画 WebP は STB (NativeImage) に委譲。
 * アニメーション WebP は RIFF を自前でパースし、各 ANMF フレームを STB でデコードして
 * GifDecoder 同様にキャンバス合成する。
 */
public class WebpDecoder implements ImageDecoder {

    @Override
    public List<Frame> decode(byte[] data) throws IOException {
        if (isAnimated(data)) return decodeAnimated(data);
        return new StbDecoder().decode(data);
    }

    // ── アニメーション検出 ────────────────────────────────────────────────────

    /**
     * VP8X チャンクの Animation フラグ (bit 1 = 0x02) が立っているか確認する。
     * libwebp では ANIMATION_FLAG = 0x00000002。
     */
    private static boolean isAnimated(byte[] data) {
        if (data.length < 30) return false;
        ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        int pos = 12; // skip RIFF/WEBP header
        while (pos + 8 <= data.length) {
            int chunkSize = buf.getInt(pos + 4);
            if (chunkSize < 0 || chunkSize > data.length) break;
            if (matches(data, pos, "VP8X") && pos + 9 <= data.length) {
                return (data[pos + 8] & 0x02) != 0;
            }
            pos += 8 + chunkSize + (chunkSize & 1);
        }
        return false;
    }

    // ── アニメーション WebP デコード ─────────────────────────────────────────

    private static List<Frame> decodeAnimated(byte[] data) throws IOException {
        ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);

        int canvasW = 0, canvasH = 0;
        int bgColorBgra = 0; // from ANIM chunk

        // 1st pass: VP8X (canvas size) と ANIM (background color) を収集
        int pos = 12;
        while (pos + 8 <= data.length) {
            int chunkSize = buf.getInt(pos + 4);
            if (chunkSize < 0 || chunkSize > data.length) break;
            if (matches(data, pos, "VP8X") && chunkSize >= 10) {
                // VP8X data: byte0=flags, bytes1-3=reserved, bytes4-6=canvasW-1, bytes7-9=canvasH-1
                canvasW = read24le(buf, pos + 12) + 1;
                canvasH = read24le(buf, pos + 15) + 1;
            } else if (matches(data, pos, "ANIM") && chunkSize >= 6) {
                bgColorBgra = buf.getInt(pos + 8); // BGRA (LE int: bits 0-7=B, 8-15=G, 16-23=R, 24-31=A)
            }
            pos += 8 + chunkSize + (chunkSize & 1);
        }
        if (canvasW <= 0 || canvasH <= 0)
            throw new IOException("Animated WebP missing canvas size");

        BufferedImage canvas = new BufferedImage(canvasW, canvasH, BufferedImage.TYPE_INT_ARGB);
        fillBg(canvas, bgColorBgra);

        List<Frame> result = new ArrayList<>();
        boolean prevDispose = false;

        // 2nd pass: ANMF フレームを処理
        pos = 12;
        while (pos + 8 <= data.length) {
            int chunkSize = buf.getInt(pos + 4);
            if (chunkSize < 0 || chunkSize > data.length) break;
            int chunkEnd = pos + 8 + chunkSize;

            if (matches(data, pos, "ANMF") && chunkSize >= 16) {
                int base  = pos + 8;
                int fx    = read24le(buf, base)      * 2;
                int fy    = read24le(buf, base + 3)  * 2;
                int fw    = read24le(buf, base + 6)  + 1;
                int fh    = read24le(buf, base + 9)  + 1;
                int durMs = read24le(buf, base + 12);
                int flags = data[base + 15] & 0xFF;
                // bit 0: dispose (0=keep, 1=dispose to background)
                // bit 1: blend   (0=alpha-blend, 1=overwrite)
                boolean dispose = (flags & 0x01) != 0;
                boolean noBlend = (flags & 0x02) != 0;

                if (prevDispose) fillBg(canvas, bgColorBgra);

                byte[] fwp = buildFrameWebp(data, base + 16, chunkEnd);
                try (NativeImage ni = NativeImage.read(new ByteArrayInputStream(fwp))) {
                    int dw = Math.min(fw, ni.getWidth());
                    int dh = Math.min(fh, ni.getHeight());
                    BufferedImage frameBuf = toBufferedImage(ni, dw, dh);
                    Graphics2D g = canvas.createGraphics();
                    g.setComposite(noBlend ? AlphaComposite.Src : AlphaComposite.SrcOver);
                    g.drawImage(frameBuf, fx, fy, null);
                    g.dispose();
                }

                result.add(new Frame(GifDecoder.toNativeImage(canvas), Math.max(20, durMs)));
                prevDispose = dispose;
            }

            pos = chunkEnd + (chunkSize & 1);
        }

        if (result.isEmpty()) throw new IOException("No ANMF frames found in animated WebP");
        return result;
    }

    // ── ユーティリティ ───────────────────────────────────────────────────────

    /** ANMF 内部サブチャンク列 (VP8/VP8L/VP8X …) を RIFF/WEBP でラップして STB が読める WebP にする */
    private static byte[] buildFrameWebp(byte[] data, int start, int end) {
        int innerLen = end - start;
        byte[] out = new byte[12 + innerLen];
        out[0] = 'R'; out[1] = 'I'; out[2] = 'F'; out[3] = 'F';
        int sz = 4 + innerLen; // "WEBP" + chunks
        out[4] = (byte)sz; out[5] = (byte)(sz >> 8); out[6] = (byte)(sz >> 16); out[7] = (byte)(sz >> 24);
        out[8] = 'W'; out[9] = 'E'; out[10] = 'B'; out[11] = 'P';
        System.arraycopy(data, start, out, 12, innerLen);
        return out;
    }

    /**
     * NativeImage (ABGR: bits 0-7=R, 8-15=G, 16-23=B, 24-31=A) を
     * BufferedImage (TYPE_INT_ARGB) に変換する。
     */
    private static BufferedImage toBufferedImage(NativeImage ni, int w, int h) {
        BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int p = ni.getPixelRGBA(x, y);
                int r = p         & 0xFF;
                int g = (p >>  8) & 0xFF;
                int b = (p >> 16) & 0xFF;
                int a = (p >> 24) & 0xFF;
                bi.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }
        return bi;
    }

    /** キャンバスを ANIM 背景色 (BGRA LE int) で塗りつぶす。 */
    private static void fillBg(BufferedImage canvas, int bgColorBgra) {
        int b = bgColorBgra         & 0xFF;
        int g = (bgColorBgra >>  8) & 0xFF;
        int r = (bgColorBgra >> 16) & 0xFF;
        int a = (bgColorBgra >> 24) & 0xFF;
        Graphics2D gfx = canvas.createGraphics();
        gfx.setComposite(AlphaComposite.Src);
        gfx.setColor(new Color(r, g, b, a));
        gfx.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        gfx.dispose();
    }

    private static boolean matches(byte[] data, int pos, String tag) {
        return pos + 4 <= data.length
            && data[pos]     == (byte)tag.charAt(0)
            && data[pos + 1] == (byte)tag.charAt(1)
            && data[pos + 2] == (byte)tag.charAt(2)
            && data[pos + 3] == (byte)tag.charAt(3);
    }

    private static int read24le(ByteBuffer buf, int pos) {
        return (buf.get(pos) & 0xFF) | ((buf.get(pos + 1) & 0xFF) << 8) | ((buf.get(pos + 2) & 0xFF) << 16);
    }
}
