package com.uqlism.emoji_deco.render.image.decoder;

import com.mojang.blaze3d.platform.NativeImage;
import com.uqlism.emoji_deco.render.image.ImageDecoder;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.CRC32;

/**
 * APNG デコーダ。PNG チャンクを直接解析し acTL/fcTL/fdAT からフレームを復元する。
 * acTL チャンクが存在しない通常 PNG は StbDecoder へフォールバック。
 *
 * APNG 仕様: https://wiki.mozilla.org/APNG_Specification
 */
public class ApngDecoder implements ImageDecoder {

    private static final byte[] PNG_SIG = {
        (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'
    };

    @Override
    public List<Frame> decode(byte[] data) throws IOException {
        List<PngChunk> chunks = parseChunks(data);
        boolean hasActl = chunks.stream().anyMatch(c -> "acTL".equals(c.type));
        if (!hasActl) return new StbDecoder().decode(data);
        return decodeAnimated(chunks);
    }

    // ── アニメーション デコード ───────────────────────────────────────────────

    private static List<Frame> decodeAnimated(List<PngChunk> chunks) throws IOException {
        PngChunk ihdrChunk = chunks.stream()
                .filter(c -> "IHDR".equals(c.type)).findFirst()
                .orElseThrow(() -> new IOException("APNG: IHDR missing"));
        int canvasW = readInt(ihdrChunk.data, 0);
        int canvasH = readInt(ihdrChunk.data, 4);

        // IHDR より後、最初の IDAT/fdAT より前の ancillary チャンク (PLTE, sRGB, gAMA 等)
        List<PngChunk> ancillary = new ArrayList<>();
        boolean hitData = false;
        for (PngChunk c : chunks) {
            String t = c.type;
            if ("IHDR".equals(t)) continue;
            if ("IDAT".equals(t) || "fdAT".equals(t) || "IEND".equals(t)) { hitData = true; }
            if (hitData || "acTL".equals(t) || "fcTL".equals(t)) continue;
            ancillary.add(c);
        }

        // フレームバケットを構築
        // fcTL が IDAT より前に来た場合のみその IDAT をフレームデータとして扱う
        List<FrameBucket> buckets = new ArrayList<>();
        FrameBucket current = null;

        for (PngChunk c : chunks) {
            switch (c.type) {
                case "fcTL" -> {
                    current = new FrameBucket(parseFctl(c.data));
                    buckets.add(current);
                }
                case "IDAT" -> {
                    // current が null = fcTL なしで登場したデフォルト画像 IDAT → スキップ
                    if (current != null) current.idatFragments.add(c.data);
                }
                case "fdAT" -> {
                    // fdAT の先頭 4 バイトはシーケンス番号 → 除いて IDAT データとして追加
                    if (current != null && c.data.length > 4)
                        current.idatFragments.add(Arrays.copyOfRange(c.data, 4, c.data.length));
                }
            }
        }

        if (buckets.isEmpty()) throw new IOException("APNG: no animation frames found");

        BufferedImage canvas = new BufferedImage(canvasW, canvasH, BufferedImage.TYPE_INT_ARGB);
        List<Frame> result = new ArrayList<>(buckets.size());

        for (FrameBucket bucket : buckets) {
            FctlData fctl = bucket.fctl;

            // APNG_DISPOSE_OP_PREVIOUS: このフレームを描画する前のキャンバス状態を保存
            BufferedImage preSnapshot = (fctl.disposeOp == 2) ? copyRegion(canvas, fctl) : null;

            // フレーム PNG を再構成して STB でデコード
            byte[] framePng = buildFramePng(ihdrChunk, ancillary, fctl, bucket.idatFragments);
            BufferedImage frameImg;
            NativeImage ni = NativeImage.read(new ByteArrayInputStream(framePng));
            try {
                frameImg = nativeToBuffered(ni);
            } finally {
                ni.close();
            }

            // blend_op に従いキャンバスへ合成
            Graphics2D g = canvas.createGraphics();
            if (fctl.blendOp == 0) {
                // APNG_BLEND_OP_SOURCE: フレーム領域を透明でクリアしてから上書き
                g.setComposite(AlphaComposite.Clear);
                g.fillRect(fctl.xOffset, fctl.yOffset, fctl.width, fctl.height);
                g.setComposite(AlphaComposite.Src);
            } else {
                // APNG_BLEND_OP_OVER: アルファブレンド
                g.setComposite(AlphaComposite.SrcOver);
            }
            g.drawImage(frameImg, fctl.xOffset, fctl.yOffset, null);
            g.dispose();

            result.add(new Frame(GifDecoder.toNativeImage(canvas), fctl.durationMs()));

            // dispose_op に従い後処理
            switch (fctl.disposeOp) {
                case 1 -> clearRect(canvas, fctl.xOffset, fctl.yOffset, fctl.width, fctl.height);
                case 2 -> restoreRegion(canvas, preSnapshot, fctl);
            }
        }

        return result;
    }

    // ── フレーム PNG 再構成 ───────────────────────────────────────────────────

    /**
     * 元 IHDR・ancillary チャンク・フレームの IDAT データから、STB が読める
     * 単独 PNG バイト列を組み立てる。IHDR の width/height はフレームサイズに差し替える。
     */
    private static byte[] buildFramePng(PngChunk originalIhdr, List<PngChunk> ancillary,
                                         FctlData fctl, List<byte[]> idatFragments) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.write(PNG_SIG);

        // IHDR: width/height をフレームサイズに差し替え、他フィールドはそのまま
        byte[] ihdrData = originalIhdr.data.clone();
        writeInt(ihdrData, 0, fctl.width);
        writeInt(ihdrData, 4, fctl.height);
        writeChunk(dos, "IHDR", ihdrData);

        for (PngChunk c : ancillary) writeChunk(dos, c.type, c.data);

        // 全 IDAT フラグメントを結合して 1 本の IDAT チャンクとして出力
        int totalLen = idatFragments.stream().mapToInt(b -> b.length).sum();
        byte[] combined = new byte[totalLen];
        int pos = 0;
        for (byte[] frag : idatFragments) {
            System.arraycopy(frag, 0, combined, pos, frag.length);
            pos += frag.length;
        }
        writeChunk(dos, "IDAT", combined);
        writeChunk(dos, "IEND", new byte[0]);

        dos.flush();
        return baos.toByteArray();
    }

    private static void writeChunk(DataOutputStream dos, String type, byte[] data) throws IOException {
        byte[] typeBytes = type.getBytes("ISO-8859-1");
        dos.writeInt(data.length);
        dos.write(typeBytes);
        dos.write(data);
        CRC32 crc = new CRC32();
        crc.update(typeBytes);
        crc.update(data);
        dos.writeInt((int) crc.getValue());
    }

    // ── PNG チャンク解析 ──────────────────────────────────────────────────────

    private static List<PngChunk> parseChunks(byte[] data) throws IOException {
        if (data.length < 8) throw new IOException("Not a PNG: file too short");
        for (int i = 0; i < 8; i++) {
            if (data[i] != PNG_SIG[i]) throw new IOException("Not a PNG: bad signature");
        }
        List<PngChunk> chunks = new ArrayList<>();
        int pos = 8;
        while (pos + 12 <= data.length) {
            int len = readInt(data, pos);
            if (len < 0 || pos + 12 + len > data.length) break;
            String type = new String(data, pos + 4, 4, java.nio.charset.StandardCharsets.ISO_8859_1);
            byte[] chunkData = Arrays.copyOfRange(data, pos + 8, pos + 8 + len);
            chunks.add(new PngChunk(type, chunkData));
            pos += 12 + len;
            if ("IEND".equals(type)) break;
        }
        return chunks;
    }

    // ── fcTL パース ────────────────────────────────────────────────────────────

    /** fcTL データ (26バイト) を FctlData に変換する。 */
    private static FctlData parseFctl(byte[] d) {
        // sequence(4), width(4), height(4), x_offset(4), y_offset(4),
        // delay_num(2), delay_den(2), dispose_op(1), blend_op(1)
        return new FctlData(
                readInt(d, 4),          // width
                readInt(d, 8),          // height
                readInt(d, 12),         // xOffset
                readInt(d, 16),         // yOffset
                readShort(d, 20),       // delayNum
                readShort(d, 22),       // delayDen
                d[24] & 0xFF,           // disposeOp
                d[25] & 0xFF            // blendOp
        );
    }

    // ── ユーティリティ ────────────────────────────────────────────────────────

    private static int readInt(byte[] d, int p) {
        return ((d[p] & 0xFF) << 24) | ((d[p + 1] & 0xFF) << 16)
                | ((d[p + 2] & 0xFF) << 8) | (d[p + 3] & 0xFF);
    }

    private static int readShort(byte[] d, int p) {
        return ((d[p] & 0xFF) << 8) | (d[p + 1] & 0xFF);
    }

    private static void writeInt(byte[] d, int p, int v) {
        d[p]     = (byte) (v >> 24);
        d[p + 1] = (byte) (v >> 16);
        d[p + 2] = (byte) (v >> 8);
        d[p + 3] = (byte)  v;
    }

    private static void clearRect(BufferedImage img, int x, int y, int w, int h) {
        Graphics2D g = img.createGraphics();
        g.setComposite(AlphaComposite.Clear);
        g.fillRect(x, y, w, h);
        g.dispose();
    }

    /** APNG_DISPOSE_OP_PREVIOUS 用: フレーム領域のスナップショットを取る。 */
    private static BufferedImage copyRegion(BufferedImage src, FctlData fctl) {
        BufferedImage snap = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = snap.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return snap;
    }

    /** APNG_DISPOSE_OP_PREVIOUS 用: スナップショットのフレーム領域をキャンバスに書き戻す。 */
    private static void restoreRegion(BufferedImage canvas, BufferedImage snapshot, FctlData fctl) {
        if (snapshot == null) return;
        Graphics2D g = canvas.createGraphics();
        g.setComposite(AlphaComposite.Src);
        g.drawImage(snapshot,
                fctl.xOffset, fctl.yOffset, fctl.xOffset + fctl.width, fctl.yOffset + fctl.height,
                fctl.xOffset, fctl.yOffset, fctl.xOffset + fctl.width, fctl.yOffset + fctl.height,
                null);
        g.dispose();
    }

    /**
     * NativeImage (ABGR: R=LSB, A=MSB) → BufferedImage (ARGB)。
     * GifDecoder.toNativeImage の逆変換。
     */
    private static BufferedImage nativeToBuffered(NativeImage ni) {
        int w = ni.getWidth(), h = ni.getHeight();
        BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int py = 0; py < h; py++) {
            for (int px = 0; px < w; px++) {
                int abgr = ni.getPixelRGBA(px, py);
                int r = (abgr)        & 0xFF;
                int gv = (abgr >> 8)  & 0xFF;
                int b = (abgr >> 16)  & 0xFF;
                int a = (abgr >> 24)  & 0xFF;
                bi.setRGB(px, py, (a << 24) | (r << 16) | (gv << 8) | b);
            }
        }
        return bi;
    }

    // ── データクラス ──────────────────────────────────────────────────────────

    private record PngChunk(String type, byte[] data) {}

    private record FctlData(int width, int height, int xOffset, int yOffset,
                             int delayNum, int delayDen, int disposeOp, int blendOp) {
        int durationMs() {
            int den = delayDen == 0 ? 100 : delayDen;
            return Math.max(20, delayNum * 1000 / den);
        }
    }

    private static class FrameBucket {
        final FctlData fctl;
        final List<byte[]> idatFragments = new ArrayList<>();
        FrameBucket(FctlData fctl) { this.fctl = fctl; }
    }
}
