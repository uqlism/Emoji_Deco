package com.uqlism.emoji_deco.render.image.decoder;

import com.mojang.blaze3d.platform.NativeImage;
import com.uqlism.emoji_deco.render.image.ImageDecoder;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Java 標準 ImageIO で GIF をデコードし、フレームを合成して返す。
 * 各フレームは disposal method に従い canvas に合成済みの NativeImage となる。
 */
public class GifDecoder implements ImageDecoder {

    @Override
    public List<Frame> decode(byte[] data) throws IOException {
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
            Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("gif");
            if (!readers.hasNext()) throw new IOException("No GIF ImageReader available");
            ImageReader reader = readers.next();
            reader.setInput(iis, false);
            try {
                return readFrames(reader);
            } finally {
                reader.dispose();
            }
        }
    }

    private static List<Frame> readFrames(ImageReader reader) throws IOException {
        int numFrames = reader.getNumImages(true);
        if (numFrames <= 0) throw new IOException("GIF contains no frames");

        // ── キャンバスサイズをストリームメタデータから取得 ─────────────────────
        int canvasW = reader.getWidth(0);
        int canvasH = reader.getHeight(0);
        try {
            IIOMetadataNode stream = (IIOMetadataNode)
                    reader.getStreamMetadata().getAsTree("javax_imageio_gif_stream_1.0");
            IIOMetadataNode lsd = (IIOMetadataNode)
                    stream.getElementsByTagName("LogicalScreenDescriptor").item(0);
            if (lsd != null) {
                canvasW = Integer.parseInt(lsd.getAttribute("logicalScreenWidth"));
                canvasH = Integer.parseInt(lsd.getAttribute("logicalScreenHeight"));
            }
        } catch (Exception ignored) {}

        BufferedImage canvas = new BufferedImage(canvasW, canvasH, BufferedImage.TYPE_INT_ARGB);
        List<Frame> result = new ArrayList<>(numFrames);
        String prevDisposal = "none";
        BufferedImage prevCanvasSnapshot = null;

        for (int i = 0; i < numFrames; i++) {
            FrameMeta meta = parseFrameMeta(reader, i);
            BufferedImage frame = reader.read(i);

            // ── 直前フレームの disposal を canvas に適用 ──────────────────────
            switch (prevDisposal) {
                case "restoreToBackgroundColor" -> clearRect(canvas, 0, 0, canvasW, canvasH);
                case "restoreToPrevious" -> { if (prevCanvasSnapshot != null) canvas = copy(prevCanvasSnapshot); }
            }

            // 今フレームを「前の状態に戻す」必要があれば保存
            if ("restoreToPrevious".equals(meta.disposal)) {
                prevCanvasSnapshot = copy(canvas);
            }

            // ── 現フレームをキャンバスに描画 ──────────────────────────────────
            Graphics2D g = canvas.createGraphics();
            g.setComposite(AlphaComposite.SrcOver);
            g.drawImage(frame, meta.x, meta.y, null);
            g.dispose();

            result.add(new Frame(toNativeImage(canvas), Math.max(20, meta.durationMs)));
            prevDisposal = meta.disposal;
        }
        return result;
    }

    // ── ユーティリティ ─────────────────────────────────────────────────────────

    private static FrameMeta parseFrameMeta(ImageReader reader, int index) {
        int x = 0, y = 0, durationMs = 100;
        String disposal = "none";
        try {
            IIOMetadataNode root = (IIOMetadataNode)
                    reader.getImageMetadata(index).getAsTree("javax_imageio_gif_image_1.0");

            IIOMetadataNode desc = first(root, "ImageDescriptor");
            if (desc != null) {
                x = intAttr(desc, "imageLeftPosition", 0);
                y = intAttr(desc, "imageTopPosition",  0);
            }
            IIOMetadataNode gce = first(root, "GraphicControlExtension");
            if (gce != null) {
                durationMs = intAttr(gce, "delayTime", 10) * 10;  // centiseconds → ms
                disposal   = gce.getAttribute("disposalMethod");
            }
        } catch (Exception ignored) {}
        return new FrameMeta(x, y, durationMs, disposal);
    }

    private static IIOMetadataNode first(IIOMetadataNode root, String tag) {
        var nodes = root.getElementsByTagName(tag);
        return nodes.getLength() > 0 ? (IIOMetadataNode) nodes.item(0) : null;
    }

    private static int intAttr(IIOMetadataNode node, String attr, int fallback) {
        try { return Integer.parseInt(node.getAttribute(attr)); }
        catch (Exception e) { return fallback; }
    }

    private static void clearRect(BufferedImage img, int x, int y, int w, int h) {
        Graphics2D g = img.createGraphics();
        g.setComposite(AlphaComposite.Clear);
        g.fillRect(x, y, w, h);
        g.dispose();
    }

    private static BufferedImage copy(BufferedImage src) {
        BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dst.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return dst;
    }

    /**
     * BufferedImage (ARGB) → NativeImage (ABGR / R在LSB)。
     * NativeImage.setPixelRGBA は ABGR 形式: bits 0-7=R, 8-15=G, 16-23=B, 24-31=A。
     */
    static NativeImage toNativeImage(BufferedImage bi) {
        int w = bi.getWidth(), h = bi.getHeight();
        NativeImage ni = new NativeImage(w, h, false);
        for (int py = 0; py < h; py++) {
            for (int px = 0; px < w; px++) {
                int argb = bi.getRGB(px, py);
                int a = (argb >> 24) & 0xFF;
                int r = (argb >> 16) & 0xFF;
                int g = (argb >>  8) & 0xFF;
                int b =  argb        & 0xFF;
                ni.setPixelRGBA(px, py, (a << 24) | (b << 16) | (g << 8) | r);
            }
        }
        return ni;
    }

    private record FrameMeta(int x, int y, int durationMs, String disposal) {}
}
