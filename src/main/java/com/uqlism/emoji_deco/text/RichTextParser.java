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

            // :shortcode: または :shortcode.arg1,arg2:
            if (c == ':') {
                int close = text.indexOf(':', pos + 1);
                if (close > pos + 1) {
                    String inner = text.substring(pos + 1, close);
                    int dot = inner.indexOf('.');
                    String code = dot > 0 ? inner.substring(0, dot) : inner;
                    String[] args = dot > 0 ? inner.substring(dot + 1).split(",", -1) : new String[0];
                    if (isValidShortcodeName(code) && ShortcodeManager.has(code)) {
                        flush(result, text, litStart, pos, base);
                        result.append(ShortcodeManager.resolve(code, args));
                        pos = close + 1;
                        litStart = pos;
                        continue;
                    }
                }
            }

            // #tagname[content] または #tagname.arg1,arg2[content]
            if (c == '#') {
                int nameEnd = pos + 1;
                while (nameEnd < text.length() && DecoratorManager.isValidTagName(String.valueOf(text.charAt(nameEnd)))) {
                    nameEnd++;
                }
                if (nameEnd > pos + 1 && nameEnd < text.length()) {
                    String tagName = text.substring(pos + 1, nameEnd);
                    // 引数ブロック: .arg1,arg2 の解析
                    String[] args = new String[0];
                    int contentStart = nameEnd;
                    if (text.charAt(nameEnd) == '.') {
                        int bracketPos = text.indexOf('[', nameEnd + 1);
                        if (bracketPos > nameEnd) {
                            args = text.substring(nameEnd + 1, bracketPos).split(",", -1);
                            contentStart = bracketPos;
                        }
                    }
                    if (text.charAt(contentStart) == '[' && DecoratorManager.has(tagName)) {
                        int bracketClose = findMatchingBracket(text, contentStart);
                        if (bracketClose != -1) {
                            flush(result, text, litStart, pos, base);
                            MutableComponent slotComponent = parseInline(text.substring(contentStart + 1, bracketClose), base);
                            Component resolved = DecoratorManager.resolve(tagName, slotComponent, args);
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

    private static boolean isValidShortcodeName(String code) {
        if (code.isEmpty()) return false;
        for (char ch : code.toCharArray()) {
            if (!Character.isLetterOrDigit(ch) && ch != '_' && ch != '-') return false;
        }
        return true;
    }

}
