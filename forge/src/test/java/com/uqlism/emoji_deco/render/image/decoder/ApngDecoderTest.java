package com.uqlism.emoji_deco.render.image.decoder;

import com.uqlism.emoji_deco.MinecraftTestBase;
import com.uqlism.emoji_deco.render.image.ImageDecoder;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.DeflaterOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class ApngDecoderTest extends MinecraftTestBase {

    @Test void alphaColorTypesDropPlteAndTrns() {
        assertFalse(ApngDecoder.keepAncillary("tRNS", 6));
        assertFalse(ApngDecoder.keepAncillary("PLTE", 6));
        assertFalse(ApngDecoder.keepAncillary("tRNS", 4));
        assertTrue(ApngDecoder.keepAncillary("sRGB", 6));
    }

    @Test void nonAlphaColorTypesKeepPlteAndTrns() {
        assertTrue(ApngDecoder.keepAncillary("tRNS", 3));
        assertTrue(ApngDecoder.keepAncillary("PLTE", 3));
        assertTrue(ApngDecoder.keepAncillary("tRNS", 2));
    }

    /** color type 6 + tRNS を持つ 1x1・2 フレームの APNG が Corrupt PNG にならないこと。 */
    @Test void rgbaApngWithTrnsDecodes() throws Exception {
        List<ImageDecoder.Frame> frames = new ApngDecoder().decode(buildRgbaApngWithTrns());
        assertEquals(2, frames.size());
    }

    private static byte[] buildRgbaApngWithTrns() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream d = new DataOutputStream(out);
        d.write(new byte[]{(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'});
        chunk(d, "IHDR", new byte[]{0, 0, 0, 1, 0, 0, 0, 1, 8, 6, 0, 0, 0});
        chunk(d, "acTL", new byte[]{0, 0, 0, 2, 0, 0, 0, 0});
        chunk(d, "tRNS", new byte[]{0, 0, 0, 0, 0, 0});
        chunk(d, "fcTL", fctl(0));
        byte[] pixels = deflate(new byte[]{0, (byte) 255, 0, 0, (byte) 255});
        chunk(d, "IDAT", pixels);
        chunk(d, "fcTL", fctl(1));
        byte[] fd = new byte[4 + pixels.length];
        fd[3] = 2;
        System.arraycopy(pixels, 0, fd, 4, pixels.length);
        chunk(d, "fdAT", fd);
        chunk(d, "IEND", new byte[0]);
        return out.toByteArray();
    }

    private static byte[] fctl(int seq) {
        // seq, w=1, h=1, x=0, y=0, delay 1/10, dispose 0, blend 0
        return new byte[]{0, 0, 0, (byte) seq, 0, 0, 0, 1, 0, 0, 0, 1,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 10, 0, 0};
    }

    private static byte[] deflate(byte[] raw) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        try (DeflaterOutputStream z = new DeflaterOutputStream(b)) { z.write(raw); }
        return b.toByteArray();
    }

    private static void chunk(DataOutputStream d, String type, byte[] data) throws IOException {
        byte[] t = type.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
        d.writeInt(data.length);
        d.write(t);
        d.write(data);
        CRC32 c = new CRC32();
        c.update(t);
        c.update(data);
        d.writeInt((int) c.getValue());
    }
}
