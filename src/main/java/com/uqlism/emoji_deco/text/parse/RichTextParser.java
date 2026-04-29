package com.uqlism.emoji_deco.text.parse;

import com.uqlism.emoji_deco.text.ir.ParsedNode;
import com.uqlism.emoji_deco.text.ir.RichNode;
import com.uqlism.emoji_deco.text.registry.DecoratorManager;
import com.uqlism.emoji_deco.text.registry.ShortcodeManager;

import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;

public class RichTextParser {

    // ── ParsedNode path (dehydrated, cached structure) ────────────────────────

    public static ParsedNode parseToParsedNode(String raw) {
        if (raw == null || raw.isEmpty()) return ParsedNode.empty();
        return parseInlineToParsed(raw);
    }

    private static ParsedNode parseInlineToParsed(String text) {
        List<ParsedNode> children = new ArrayList<>();
        int pos = 0, litStart = 0;
        while (pos < text.length()) {
            char c = text.charAt(pos);
            if (c == '\\' && pos + 1 < text.length() && isEscapable(text.charAt(pos + 1))) {
                flushP(children, text, litStart, pos);
                children.add(new ParsedNode.Text(String.valueOf(text.charAt(pos + 1)), ParsedNode.ParsedStyle.EMPTY, List.of()));
                pos += 2; litStart = pos; continue;
            }
            if (c == ':') {
                int close = text.indexOf(':', pos + 1);
                if (close > pos + 1) {
                    String inner = text.substring(pos + 1, close);
                    int dot = inner.indexOf('.');
                    String code = dot > 0 ? inner.substring(0, dot) : inner;
                    String[] argsArr = dot > 0 ? inner.substring(dot + 1).split(",", -1) : new String[0];
                    if (isValidShortcodeName(code) && ShortcodeManager.has(code)) {
                        flushP(children, text, litStart, pos);
                        List<ParsedNode.StringVal> ca = new ArrayList<>();
                        for (String a : argsArr) ca.add(new ParsedNode.StringVal.Literal(a));
                        children.add(new ParsedNode.ApplyShortcode(code, ca));
                        pos = close + 1; litStart = pos; continue;
                    }
                }
            }
            if (c == '#') {
                int nameEnd = pos + 1;
                while (nameEnd < text.length() && DecoratorManager.isValidTagName(String.valueOf(text.charAt(nameEnd)))) nameEnd++;
                if (nameEnd > pos + 1 && nameEnd < text.length()) {
                    String tagName = text.substring(pos + 1, nameEnd);
                    String[] argsArr = new String[0];
                    int contentStart = nameEnd;
                    if (text.charAt(nameEnd) == '.') {
                        int bp = text.indexOf('[', nameEnd + 1);
                        if (bp > nameEnd) { argsArr = text.substring(nameEnd + 1, bp).split(",", -1); contentStart = bp; }
                    }
                    if (contentStart < text.length() && text.charAt(contentStart) == '[' && DecoratorManager.has(tagName)) {
                        int bc = findMatchingBracket(text, contentStart);
                        if (bc != -1) {
                            flushP(children, text, litStart, pos);
                            ParsedNode slot = parseInlineToParsed(text.substring(contentStart + 1, bc));
                            List<ParsedNode.StringVal> ca = new ArrayList<>();
                            for (String a : argsArr) ca.add(new ParsedNode.StringVal.Literal(a));
                            children.add(new ParsedNode.ApplyDecorator(tagName, slot, ca));
                            pos = bc + 1; litStart = pos; continue;
                        }
                    }
                }
            }
            pos++;
        }
        flushP(children, text, litStart, text.length());
        return new ParsedNode.Text("", ParsedNode.ParsedStyle.EMPTY, children);
    }

    private static void flushP(List<ParsedNode> out, String text, int start, int end) {
        if (start < end) out.add(new ParsedNode.Text(text.substring(start, end), ParsedNode.ParsedStyle.EMPTY, List.of()));
    }

    // ── RichNode path (immediate hydration, used by ComponentTransformer etc.) ─

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
