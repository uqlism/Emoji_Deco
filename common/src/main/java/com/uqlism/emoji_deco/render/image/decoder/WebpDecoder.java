package com.uqlism.emoji_deco.render.image.decoder;

import com.uqlism.emoji_deco.render.image.ImageDecoder;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * WebP デコーダ。TwelveMonkeys ImageIO (imageio-webp) を使用して静止画・アニメーション
 * 両方の WebP をデコードする。TwelveMonkeys が存在しない場合は STB にフォールバック
 * （静止画のみ）。フレームのピクセルデータは ImageReader.read(i) で取得し、
 * フレーム位置・継続時間・dispose/blend はバイナリ RIFF を直接解析して取得する。
 */
public class WebpDecoder implements ImageDecoder {

    @Override
    public List<Frame> decode(byte[] data) throws IOException {
        Iterator<ImageReader> readers = ImageIO.getImageReadersBySuffix("webp");
        if (readers.hasNext()) {
            return decodeWithImageIO(readers.next(), data);
        }
        // TwelveMonkeys が存在しない場合は STB で静止画のみ試みる
        return new StbDecoder().decode(data);
    }

    // ── ImageIO デコード ──────────────────────────────────────────────────────

    private static List<Frame> decodeWithImageIO(ImageReader reader, byte[] data) throws IOException {
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
            reader.setInput(iis, false);

            int numFrames;
            try {
                numFrames = reader.getNumImages(true);
            } catch (Exception e) {
                numFrames = 1;
            }
            if (numFrames <= 0) throw new IOException("WebP has no frames");

            // フレームメタデータは RIFF バイナリから直接取得（ImageIO メタデータ API は不確実）
            List<ANMFMeta> binaryMeta = parseANMFFrames(data);
            int[] canvas = parseCanvasSize(data);

            int canvasW = canvas != null ? canvas[0] : reader.getWidth(0);
            int canvasH = canvas != null ? canvas[1] : reader.getHeight(0);

            BufferedImage canvasBuf = new BufferedImage(canvasW, canvasH, BufferedImage.TYPE_INT_ARGB);
            List<Frame> result = new ArrayList<>(numFrames);

            for (int i = 0; i < numFrames; i++) {
                BufferedImage frame = reader.read(i);
                if (frame == null) continue;

                int fx = 0, fy = 0, durationMs = 100;
                boolean alphaBlend = true, dispose = false;
                if (i < binaryMeta.size()) {
                    ANMFMeta m = binaryMeta.get(i);
                    fx = m.x; fy = m.y; durationMs = m.durationMs;
                    alphaBlend = m.alphaBlend; dispose = m.dispose;
                }

                Graphics2D g = canvasBuf.createGraphics();
                g.setComposite(alphaBlend ? AlphaComposite.SrcOver : AlphaComposite.Src);
                g.drawImage(frame, fx, fy, null);
                g.dispose();

                result.add(new Frame(GifDecoder.toNativeImage(canvasBuf), Math.max(20, durationMs)));

                if (dispose) {
                    Graphics2D gc = canvasBuf.createGraphics();
                    gc.setComposite(AlphaComposite.Clear);
                    gc.fillRect(fx, fy, frame.getWidth(), frame.getHeight());
                    gc.dispose();
                }
            }

            if (result.isEmpty()) throw new IOException("No frames decoded from WebP");
            return result;
        } finally {
            reader.dispose();
        }
    }

    // ── RIFF バイナリ解析 ─────────────────────────────────────────────────────

    /** VP8X チャンクからキャンバスサイズを返す。VP8X が存在しない場合は null。 */
    private static int[] parseCanvasSize(byte[] data) {
        if (data.length < 30) return null;
        int pos = 12;
        if (data[pos]=='V' && data[pos+1]=='P' && data[pos+2]=='8' && data[pos+3]=='X') {
            int size = read32le(data, pos + 4);
            if (size >= 10 && pos + 18 <= data.length) {
                int w = read24le(data, pos + 12) + 1;
                int h = read24le(data, pos + 15) + 1;
                return new int[]{w, h};
            }
        }
        return null;
    }

    /** すべての ANMF チャンクのメタデータ（位置・時間・dispose/blend）を返す。 */
    private static List<ANMFMeta> parseANMFFrames(byte[] data) {
        List<ANMFMeta> list = new ArrayList<>();
        if (data.length < 12) return list;
        int pos = 12;
        while (pos + 8 <= data.length) {
            int size = read32le(data, pos + 4);
            if (size < 0) break;
            if (data[pos]=='A' && data[pos+1]=='N' && data[pos+2]=='M' && data[pos+3]=='F'
                    && pos + 24 <= data.length) {
                ANMFMeta m = new ANMFMeta();
                m.x          = read24le(data, pos +  8) * 2;
                m.y          = read24le(data, pos + 11) * 2;
                m.durationMs = read24le(data, pos + 20);
                byte flags   = data[pos + 23];
                m.alphaBlend = ((flags >> 1) & 1) == 0; // bit1=1 → no blend
                m.dispose    = (flags & 1) != 0;         // bit0=1 → dispose
                list.add(m);
            }
            pos += 8 + size + (size & 1);
        }
        return list;
    }

    private static int read24le(byte[] d, int p) {
        return (d[p] & 0xFF) | ((d[p+1] & 0xFF) << 8) | ((d[p+2] & 0xFF) << 16);
    }

    private static int read32le(byte[] d, int p) {
        return (d[p] & 0xFF) | ((d[p+1] & 0xFF) << 8) | ((d[p+2] & 0xFF) << 16) | ((d[p+3] & 0xFF) << 24);
    }

    private static class ANMFMeta {
        int x, y, durationMs;
        boolean alphaBlend, dispose;
    }
}
