package com.uqlism.runicink.text;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

public class RichTextParser {

    public static Component parse(String raw) {
        if (raw == null || raw.isEmpty()) return Component.empty();

        // Header: lines starting with one or more '#' followed by a space
        if (raw.startsWith("#")) {
            int level = 0;
            while (level < raw.length() && raw.charAt(level) == '#') level++;
            if (level <= 6 && level < raw.length() && raw.charAt(level) == ' ') {
                MutableComponent content = parseInline(raw.substring(level + 1), Style.EMPTY);
                return HeaderRegistry.createMarker(level).append(content);
            }
        }

        return parseInline(raw, Style.EMPTY);
    }

    public static MutableComponent parseInline(String text, Style base) {
        MutableComponent result = Component.empty();
        int pos = 0;
        int litStart = 0;

        while (pos < text.length()) {
            char c = text.charAt(pos);

            // **bold**
            if (c == '*' && pos + 1 < text.length() && text.charAt(pos + 1) == '*') {
                int close = text.indexOf("**", pos + 2);
                if (close != -1) {
                    flush(result, text, litStart, pos, base);
                    result.append(parseInline(text.substring(pos + 2, close), base.withBold(true)));
                    pos = close + 2;
                    litStart = pos;
                    continue;
                }
            }

            // *italic* (not **)
            if (c == '*' && (pos + 1 >= text.length() || text.charAt(pos + 1) != '*')) {
                int close = findSingleStar(text, pos + 1);
                if (close != -1) {
                    flush(result, text, litStart, pos, base);
                    result.append(parseInline(text.substring(pos + 1, close), base.withItalic(true)));
                    pos = close + 1;
                    litStart = pos;
                    continue;
                }
            }

            // ~~strikethrough~~
            if (c == '~' && pos + 1 < text.length() && text.charAt(pos + 1) == '~') {
                int close = text.indexOf("~~", pos + 2);
                if (close != -1) {
                    flush(result, text, litStart, pos, base);
                    result.append(parseInline(text.substring(pos + 2, close), base.withStrikethrough(true)));
                    pos = close + 2;
                    litStart = pos;
                    continue;
                }
            }

            // __underline__
            if (c == '_' && pos + 1 < text.length() && text.charAt(pos + 1) == '_') {
                int close = text.indexOf("__", pos + 2);
                if (close != -1) {
                    flush(result, text, litStart, pos, base);
                    result.append(parseInline(text.substring(pos + 2, close), base.withUnderlined(true)));
                    pos = close + 2;
                    litStart = pos;
                    continue;
                }
            }

            // :shortcode:
            if (c == ':') {
                int close = text.indexOf(':', pos + 1);
                if (close > pos + 1) {
                    String code = text.substring(pos + 1, close);
                    if (isValidShortcodeName(code) && ShortcodeManager.has(code)) {
                        flush(result, text, litStart, pos, base);
                        result.append(ShortcodeManager.resolve(code));
                        pos = close + 1;
                        litStart = pos;
                        continue;
                    }
                }
            }

            pos++;
        }

        flush(result, text, litStart, text.length(), base);
        return result;
    }

    private static void flush(MutableComponent out, String text, int start, int end, Style style) {
        if (start < end) {
            out.append(Component.literal(text.substring(start, end)).withStyle(style));
        }
    }

    // Finds the next '*' that is not part of '**', starting from `from`
    private static int findSingleStar(String text, int from) {
        for (int i = from; i < text.length(); i++) {
            if (text.charAt(i) == '*') {
                if (i + 1 < text.length() && text.charAt(i + 1) == '*') {
                    i++; // skip **
                } else {
                    return i;
                }
            }
        }
        return -1;
    }

    private static boolean isValidShortcodeName(String code) {
        if (code.isEmpty()) return false;
        for (char ch : code.toCharArray()) {
            if (!Character.isLetterOrDigit(ch) && ch != '_' && ch != '-') return false;
        }
        return true;
    }

}
