package com.uqlism.emoji_deco.text.parse;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.uqlism.emoji_deco.text.hydrate.HydrateContext;
import com.uqlism.emoji_deco.text.hydrate.Hydrators;
import com.uqlism.emoji_deco.text.ir.RichNode;
import com.uqlism.emoji_deco.text.registry.DecoratorManager;
import com.uqlism.emoji_deco.text.registry.ShortcodeManager;

import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RichTextParser {

    // ── String → JsonElement cache ────────────────────────────────────────────
    // Same string always returns the same JsonElement object, giving CACHED_NODE
    // a stable WeakMap key across frames.  Cleared on resource pack reload so
    // CACHED_NODE's stale entries become unreachable and are GC'd automatically.

    private static final Map<String, JsonElement> PARSE_CACHE = new ConcurrentHashMap<>();

    public static void invalidateParseCache() { PARSE_CACHE.clear(); }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Parses a raw rich-text string into a JsonElement structure.
     * Result is cached by string identity; repeated calls with the same string
     * return the same JsonElement object (stable CACHED_NODE key).
     */
    public static JsonElement parseToJson(String raw) {
        if (raw == null || raw.isEmpty()) return new JsonPrimitive("");
        return PARSE_CACHE.computeIfAbsent(raw, RichTextParser::buildJson);
    }

    /**
     * Parses and hydrates a raw rich-text string to RichNode.
     * Goes through PARSE_CACHE + CACHED_NODE, so repeated calls with the same
     * string hit the cache rather than re-evaluating shortcodes/decorators.
     */
    public static RichNode parse(String raw) {
        if (raw == null || raw.isEmpty()) return RichNode.empty();
        RichNode r = Hydrators.CACHED_NODE.hydrate(
                parseToJson(raw), HydrateContext.EMPTY.track());
        return r != null ? r : RichNode.empty();
    }

    /**
     * Eager evaluation with style inheritance — used by ComponentTransformer
     * where the parent component's style must be propagated to literal text.
     */
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
                pos += 2; litStart = pos; continue;
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
                        pos = close + 1; litStart = pos; continue;
                    }
                }
            }

            // #tagname[content] または #tagname.arg1,arg2[content]
            if (c == '#') {
                int nameEnd = pos + 1;
                while (nameEnd < text.length() && DecoratorManager.isValidTagName(String.valueOf(text.charAt(nameEnd)))) nameEnd++;
                if (nameEnd > pos + 1 && nameEnd < text.length()) {
                    String tagName = text.substring(pos + 1, nameEnd);
                    String[] args = new String[0];
                    int contentStart = nameEnd;
                    if (text.charAt(nameEnd) == '.') {
                        int bracketPos = text.indexOf('[', nameEnd + 1);
                        if (bracketPos > nameEnd) { args = text.substring(nameEnd + 1, bracketPos).split(",", -1); contentStart = bracketPos; }
                    }
                    if (text.charAt(contentStart) == '[' && DecoratorManager.has(tagName)) {
                        int bracketClose = findMatchingBracket(text, contentStart);
                        if (bracketClose != -1) {
                            flush(children, text, litStart, pos, base);
                            RichNode slotNode = parseInline(text.substring(contentStart + 1, bracketClose), base);
                            RichNode resolved = DecoratorManager.resolve(tagName, slotNode, args);
                            children.add(resolved != null ? resolved : slotNode);
                            pos = bracketClose + 1; litStart = pos; continue;
                        }
                    }
                }
            }

            pos++;
        }

        flush(children, text, litStart, text.length(), base);
        return new RichNode.Text("", Style.EMPTY, children);
    }

    // ── String → JsonElement conversion ──────────────────────────────────────

    private static JsonElement buildJson(String raw) {
        JsonArray result = new JsonArray();
        int pos = 0, litStart = 0;

        while (pos < raw.length()) {
            char c = raw.charAt(pos);

            if (c == '\\' && pos + 1 < raw.length() && isEscapable(raw.charAt(pos + 1))) {
                if (pos > litStart) result.add(raw.substring(litStart, pos));
                result.add(String.valueOf(raw.charAt(pos + 1)));
                pos += 2; litStart = pos; continue;
            }

            if (c == ':') {
                int close = raw.indexOf(':', pos + 1);
                if (close > pos + 1) {
                    String inner = raw.substring(pos + 1, close);
                    int dot = inner.indexOf('.');
                    String code = dot > 0 ? inner.substring(0, dot) : inner;
                    String[] args = dot > 0 ? inner.substring(dot + 1).split(",", -1) : new String[0];
                    if (isValidShortcodeName(code) && ShortcodeManager.has(code)) {
                        if (pos > litStart) result.add(raw.substring(litStart, pos));
                        result.add(shortcodeJson(code, args));
                        pos = close + 1; litStart = pos; continue;
                    }
                }
            }

            if (c == '#') {
                int nameEnd = pos + 1;
                while (nameEnd < raw.length() && DecoratorManager.isValidTagName(String.valueOf(raw.charAt(nameEnd)))) nameEnd++;
                if (nameEnd > pos + 1 && nameEnd < raw.length()) {
                    String tagName = raw.substring(pos + 1, nameEnd);
                    String[] args = new String[0];
                    int contentStart = nameEnd;
                    if (raw.charAt(nameEnd) == '.') {
                        int bp = raw.indexOf('[', nameEnd + 1);
                        if (bp > nameEnd) { args = raw.substring(nameEnd + 1, bp).split(",", -1); contentStart = bp; }
                    }
                    if (contentStart < raw.length() && raw.charAt(contentStart) == '[' && DecoratorManager.has(tagName)) {
                        int bc = findMatchingBracket(raw, contentStart);
                        if (bc != -1) {
                            if (pos > litStart) result.add(raw.substring(litStart, pos));
                            result.add(decoratorJson(tagName, args, buildJson(raw.substring(contentStart + 1, bc))));
                            pos = bc + 1; litStart = pos; continue;
                        }
                    }
                }
            }

            pos++;
        }

        if (litStart < raw.length()) result.add(raw.substring(litStart));
        if (result.size() == 0) return new JsonPrimitive("");
        if (result.size() == 1) return result.get(0);
        return result;
    }

    private static JsonObject shortcodeJson(String code, String[] args) {
        JsonObject o = new JsonObject();
        o.addProperty("type", "emoji_deco:apply_shortcode");
        o.addProperty("shortcode", code);
        if (args.length > 0) { JsonArray a = new JsonArray(); for (String s : args) a.add(s); o.add("args", a); }
        return o;
    }

    private static JsonObject decoratorJson(String tag, String[] args, JsonElement slot) {
        JsonObject o = new JsonObject();
        o.addProperty("type", "emoji_deco:apply_decorator");
        o.addProperty("decorator", tag);
        if (args.length > 0) { JsonArray a = new JsonArray(); for (String s : args) a.add(s); o.add("args", a); }
        o.add("slot", slot);
        return o;
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private static void flush(List<RichNode> out, String text, int start, int end, Style style) {
        if (start < end) out.add(new RichNode.Text(text.substring(start, end), style, List.of()));
    }

    private static int findMatchingBracket(String text, int openPos) {
        int depth = 0;
        for (int i = openPos; i < text.length(); i++) {
            if (text.charAt(i) == '[') depth++;
            else if (text.charAt(i) == ']') { if (--depth == 0) return i; }
        }
        return -1;
    }

    private static boolean isEscapable(char c) {
        return c == '#' || c == ':' || c == '[' || c == ']' || c == '.' || c == ',' || c == '\\';
    }

    private static boolean isValidShortcodeName(String code) {
        if (code.isEmpty()) return false;
        for (char ch : code.toCharArray())
            if (!Character.isLetterOrDigit(ch) && ch != '_' && ch != '-') return false;
        return true;
    }
}
