package com.uqlism.emoji_deco.render.image;

import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/** マジックバイト・Content-Type・拡張子・明示指定からフォーマットを判別する。 */
public class ImageFormatDetector {

    public enum Format { PNG, JPEG, GIF, WEBP, UNKNOWN }

    /**
     * フォーマットを判別する。
     * @param data     先頭バイト列（少なくとも 12 バイトあれば十分）
     * @param hint     null でなければ優先される。明示フォーマット名・Content-Type・拡張子を受け付ける
     */
    public static Format detect(byte[] data, @Nullable String hint) {
        if (hint != null) {
            Format f = fromHint(hint);
            if (f != Format.UNKNOWN) return f;
        }
        return fromMagic(data);
    }

    /** "png" / "image/gif" / "anim.gif" など、任意の文字列からフォーマットを推定する。 */
    public static Format fromHint(String hint) {
        String s = hint.toLowerCase(Locale.ROOT);
        if (s.contains("png"))                    return Format.PNG;
        if (s.contains("jpeg") || s.contains("jpg")) return Format.JPEG;
        if (s.contains("gif"))                    return Format.GIF;
        if (s.contains("webp"))                   return Format.WEBP;
        return Format.UNKNOWN;
    }

    /** マジックバイトでフォーマットを判別する。 */
    public static Format fromMagic(byte[] data) {
        if (data.length >= 4
                && data[0] == (byte)0x89 && data[1] == 'P'
                && data[2] == 'N' && data[3] == 'G') return Format.PNG;
        if (data.length >= 2
                && data[0] == (byte)0xFF && data[1] == (byte)0xD8) return Format.JPEG;
        if (data.length >= 3
                && data[0] == 'G' && data[1] == 'I' && data[2] == 'F') return Format.GIF;
        if (data.length >= 12
                && data[8] == 'W' && data[9] == 'E'
                && data[10] == 'B' && data[11] == 'P') return Format.WEBP;
        return Format.UNKNOWN;
    }
}
