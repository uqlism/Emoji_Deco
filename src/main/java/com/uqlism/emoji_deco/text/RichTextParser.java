package com.uqlism.emoji_deco.text;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

public class RichTextParser {

    public static Component parse(String raw) {
        if (raw == null || raw.isEmpty()) return Component.empty();
        return parseInline(raw, Style.EMPTY);
    }

    public static MutableComponent parseInline(String text, Style base) {
        MutableComponent result = Component.empty();
        int pos = 0;
        int litStart = 0;

        while (pos < text.length()) {
            char c = text.charAt(pos);

            // @username → player head glyph
            if (c == '@') {
                int end = pos + 1;
                while (end < text.length() && isUsernameChar(text.charAt(end))) end++;
                if (end > pos + 1) {
                    flush(result, text, litStart, pos, base);
                    result.append(SpriteRegistry.createHeadComponent(text.substring(pos + 1, end)));
                    pos = end;
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

            // #tagname[content] style tags
            if (c == '#') {
                int nameEnd = pos + 1;
                while (nameEnd < text.length() && DecoratorManager.isValidTagName(String.valueOf(text.charAt(nameEnd)))) {
                    nameEnd++;
                }
                if (nameEnd > pos + 1 && nameEnd < text.length() && text.charAt(nameEnd) == '[') {
                    String tagName = text.substring(pos + 1, nameEnd);
                    if (DecoratorManager.has(tagName)) {
                        int bracketClose = findMatchingBracket(text, nameEnd);
                        if (bracketClose != -1) {
                            flush(result, text, litStart, pos, base);
                            MutableComponent slotComponent = parseInline(text.substring(nameEnd + 1, bracketClose), base);
                            Component resolved = DecoratorManager.resolve(tagName, slotComponent);
                            result.append(resolved != null ? resolved : slotComponent);
                            pos = bracketClose + 1;
                            litStart = pos;
                            continue;
                        }
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

    private static int findMatchingBracket(String text, int openPos) {
        int depth = 0;
        for (int i = openPos; i < text.length(); i++) {
            if (text.charAt(i) == '[') depth++;
            else if (text.charAt(i) == ']') {
                if (--depth == 0) return i;
            }
        }
        return -1;
    }

    private static boolean isUsernameChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    private static boolean isValidShortcodeName(String code) {
        if (code.isEmpty()) return false;
        for (char ch : code.toCharArray()) {
            if (!Character.isLetterOrDigit(ch) && ch != '_' && ch != '-') return false;
        }
        return true;
    }

}
