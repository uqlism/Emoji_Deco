package com.uqlism.emoji_deco.text.parse;

import com.uqlism.emoji_deco.text.ir.RichNode;
import com.uqlism.emoji_deco.text.registry.DecoratorManager;
import com.uqlism.emoji_deco.text.registry.ShortcodeManager;

import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;

public class RichTextParser {

    // ── RichNode path ─────────────────────────────────────────────────────────

    public static RichNode parse(String raw) {
        if (raw == null || raw.isEmpty()) return RichNode.empty();
        return parseInline(raw, Style.EMPTY);
    }

    public static RichNode parseInline(String text, Style base) {
        List<RichNode> children = new ArrayList<>();
        int pos = 0;
        int litStart = 0;

        while (pos < text.length()) {
            char c = text.charAt(pos);

            // バックスラッシュエスケープ: \# \: \[ \] \. \, \\
            if (c == '\\' && pos + 1 < text.length() && isEscapable(text.charAt(pos + 1))) {
                flush(children, text, litStart, pos, base);
                children.add(new RichNode.Text(String.valueOf(text.charAt(pos + 1)), base, List.of()));
                pos += 2;
                litStart = pos;
                continue;
            }

            // :shortcode: または :shortcode.arg1,arg2:
            if (c == ':') {
                int close = text.indexOf(':', pos + 1);
                if (close > pos + 1) {
                    String inner = text.substring(pos + 1, close);
                    int dot = inner.indexOf('.');
                    String code = dot > 0 ? inner.substring(0, dot) : inner;
                    String[] args = dot > 0 ? inner.substring(dot + 1).split(",", -1) : new String[0];
                    if (isValidShortcodeName(code) && ShortcodeManager.has(code)) {
                        flush(children, text, litStart, pos, base);
                        children.add(ShortcodeManager.resolve(code, args));
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
                            flush(children, text, litStart, pos, base);
                            RichNode slotNode = parseInline(text.substring(contentStart + 1, bracketClose), base);
                            RichNode resolved = DecoratorManager.resolve(tagName, slotNode, args);
                            children.add(resolved != null ? resolved : slotNode);
                            pos = bracketClose + 1;
                            litStart = pos;
                            continue;
                        }
                    }
                }
            }

            pos++;
        }

        flush(children, text, litStart, text.length(), base);
        return new RichNode.Text("", Style.EMPTY, children);
    }

    private static void flush(List<RichNode> out, String text, int start, int end, Style style) {
        if (start < end) {
            out.add(new RichNode.Text(text.substring(start, end), style, List.of()));
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

    private static boolean isEscapable(char c) {
        return c == '#' || c == ':' || c == '[' || c == ']'
            || c == '.' || c == ',' || c == '\\';
    }

    private static boolean isValidShortcodeName(String code) {
        if (code.isEmpty()) return false;
        for (char ch : code.toCharArray()) {
            if (!Character.isLetterOrDigit(ch) && ch != '_' && ch != '-') return false;
        }
        return true;
    }
}
