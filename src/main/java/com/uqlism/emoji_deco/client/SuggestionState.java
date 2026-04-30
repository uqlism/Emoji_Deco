package com.uqlism.emoji_deco.client;

import com.google.gson.JsonElement;
import com.uqlism.emoji_deco.text.parse.EmojiDecoComponentParser;
import com.uqlism.emoji_deco.text.ir.RichNode;
import com.uqlism.emoji_deco.text.registry.DecoratorManager;
import com.uqlism.emoji_deco.text.registry.ShortcodeManager;
import com.uqlism.emoji_deco.text.registry.SuggestionEngine;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.WeakHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class SuggestionState {

    public enum TriggerType { SHORTCODE, DECORATOR, DECORATOR_ARG, SHORTCODE_ARG }

    public record Entry(String label, Supplier<Component> preview, String insertion, TriggerType type) {}

    // ── instance state ────────────────────────────────────────────────────────

    public String lastInput = "";
    public int lastCursor = 0;
    public int pendingCursor = -1;
    public List<Entry> suggestions = Collections.emptyList();
    public int selectedIndex = 0;
    public int triggerPos = 0;

    // ── per-screen registry ───────────────────────────────────────────────────

    // Accessed only on the client main thread; no synchronization needed.
    // WeakHashMap: entry is removed automatically when the screen is GC'd.
    private static final WeakHashMap<Object, SuggestionState> REGISTRY = new WeakHashMap<>();

    public static SuggestionState of(Object screen) {
        return REGISTRY.computeIfAbsent(screen, k -> new SuggestionState());
    }

    // ── update ────────────────────────────────────────────────────────────────

    public void update(String text, int cursor) {
        lastInput = text;
        lastCursor = Math.min(cursor, text.length());
        String active = text.substring(0, lastCursor);

        int colonPos = findActiveColonPos(active);
        int hashPos  = findActiveHashPos(active);

        if (colonPos < 0 && hashPos < 0) { clear(); return; }

        int maxPos = Math.max(colonPos, hashPos);

        if (maxPos == hashPos) {
            String prefix = active.substring(hashPos + 1);
            int dotPos = prefix.indexOf('.');
            if (dotPos >= 0 && DecoratorManager.has(prefix.substring(0, dotPos))) {
                String tagName  = prefix.substring(0, dotPos);
                String afterDot = prefix.substring(dotPos + 1);
                int commaCount  = countCommas(afterDot);
                int lastComma   = afterDot.lastIndexOf(',');
                String argPrefix = lastComma >= 0 ? afterDot.substring(lastComma + 1) : afterDot;
                triggerPos = hashPos + 1 + dotPos + 1 + (lastComma >= 0 ? lastComma + 1 : 0);
                suggestions = buildArgSuggestions(
                        DecoratorManager.getArgSuggestionsSpec(tagName, commaCount),
                        argPrefix, TriggerType.DECORATOR_ARG);
            } else {
                triggerPos = hashPos;
                suggestions = buildStyleTagSuggestions(prefix);
            }
        } else {
            String prefix = active.substring(colonPos + 1);
            int dotPos = prefix.indexOf('.');
            if (dotPos >= 0 && ShortcodeManager.has(prefix.substring(0, dotPos))) {
                String code     = prefix.substring(0, dotPos);
                String afterDot = prefix.substring(dotPos + 1);
                int commaCount  = countCommas(afterDot);
                int lastComma   = afterDot.lastIndexOf(',');
                String argPrefix = lastComma >= 0 ? afterDot.substring(lastComma + 1) : afterDot;
                triggerPos = colonPos + 1 + dotPos + 1 + (lastComma >= 0 ? lastComma + 1 : 0);
                suggestions = buildArgSuggestions(
                        ShortcodeManager.getArgSuggestionsSpec(code, commaCount),
                        argPrefix, TriggerType.SHORTCODE_ARG);
            } else {
                triggerPos = colonPos;
                suggestions = buildShortcodeSuggestions(prefix);
            }
        }
        selectedIndex = 0;
    }

    private static int findActiveColonPos(String text) {
        int pos = text.lastIndexOf(':');
        if (pos < 0) return -1;
        if (pos > 0 && text.charAt(pos - 1) == '\\') return -1; // escaped
        // If there is a previous ':' and the text between them contains no spaces,
        // pos is a closing colon (completing a :name: pair) — do not suggest.
        int prev = text.lastIndexOf(':', pos - 1);
        if (prev >= 0) {
            String between = text.substring(prev + 1, pos);
            if (!between.isEmpty() && !between.contains(" ")) return -1;
        }
        return pos;
    }

    /**
     * Returns the position of the last '#' trigger whose suffix matches:
     *   validName ( '.' argChars )?
     * where argChars are any characters except '[' and ']'.
     */
    private static int findActiveHashPos(String text) {
        int pos = text.lastIndexOf('#');
        if (pos < 0) return -1;
        if (pos > 0 && text.charAt(pos - 1) == '\\') return -1; // escaped
        String after = text.substring(pos + 1);
        if (after.isEmpty()) return pos;
        if (after.charAt(0) == ' ' || after.charAt(0) == '#') return -1;
        boolean inArgPart = false;
        for (char c : after.toCharArray()) {
            if (!inArgPart) {
                if (c == '.') { inArgPart = true; }
                else if (!Character.isLetterOrDigit(c) && c != '_' && c != '-') return -1;
            } else {
                if (c == '[' || c == ']') return -1;
            }
        }
        return pos;
    }

    // ── suggestion builders ───────────────────────────────────────────────────

    private static List<Entry> buildStyleTagSuggestions(String prefix) {
        List<Entry> results = new ArrayList<>();
        DecoratorManager.searchSuggestions(prefix, 30).forEach(r -> {
            String canonical = r.canonical();
            results.add(new Entry(
                    DecoratorManager.getLabel(canonical),
                    () -> DecoratorManager.getPreview(canonical),
                    canonical, TriggerType.DECORATOR));
        });
        return results;
    }

    private static List<Entry> buildShortcodeSuggestions(String prefix) {
        if (prefix.isEmpty()) return Collections.emptyList();
        List<Entry> results = new ArrayList<>();
        ShortcodeManager.searchSuggestions(prefix, 30).forEach(r -> {
            String canonical = r.canonical();
            String alias     = r.matchedAlias();
            // alias でヒットした場合はそのエイリアス名をラベルに表示
            String label = alias != null ? ":" + alias + ":" : ShortcodeManager.getLabel(canonical);
            results.add(new Entry(label,
                    () -> ShortcodeManager.getPreview(canonical),
                    canonical, TriggerType.SHORTCODE));
        });
        return results;
    }

    private static List<Entry> buildArgSuggestions(JsonElement spec, String prefix, TriggerType type) {
        if (spec == null) return Collections.emptyList();
        return EmojiDecoComponentParser.resolveStringList(spec).stream()
                .filter(v -> v.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT)))
                .map(v -> {
                    TextColor color = TextColor.parseColor(v);
                    Supplier<Component> preview = color != null
                            ? () -> Component.literal("■ " + v).withStyle(Style.EMPTY.withColor(color))
                            : () -> Component.literal(v);
                    return new Entry(v, preview, v, type);
                })
                .collect(Collectors.toList());
    }

    // ── apply completion ──────────────────────────────────────────────────────

    public String applyTo(String text, Entry entry) {
        int split = Math.min(lastCursor, text.length());
        String before = text.substring(0, split);
        String after  = text.substring(split);

        switch (entry.type()) {
            case SHORTCODE -> {
                int pos = before.lastIndexOf(':');
                if (pos < 0) { pendingCursor = -1; return text; }
                String head = before.substring(0, pos + 1) + entry.insertion() + ":";
                pendingCursor = head.length();
                return head + after;
            }
            case DECORATOR -> {
                int pos = before.lastIndexOf('#');
                if (pos < 0) { pendingCursor = -1; return text; }
                String head = before.substring(0, pos) + "#" + entry.insertion() + "[";
                pendingCursor = head.length();
                return head + "]" + after;
            }
            case DECORATOR_ARG -> {
                int hashPos = before.lastIndexOf('#');
                if (hashPos < 0) { pendingCursor = -1; return text; }
                int dotPos  = before.indexOf('.', hashPos);
                if (dotPos  < 0) { pendingCursor = -1; return text; }
                int lastComma = before.lastIndexOf(',');
                int argStart  = (lastComma > dotPos) ? lastComma + 1 : dotPos + 1;
                String head = before.substring(0, argStart) + entry.insertion() + "[";
                pendingCursor = head.length();
                return head + "]" + after;
            }
            case SHORTCODE_ARG -> {
                int colonPos = before.lastIndexOf(':');
                if (colonPos < 0) { pendingCursor = -1; return text; }
                int dotPos   = before.indexOf('.', colonPos);
                if (dotPos   < 0) { pendingCursor = -1; return text; }
                int lastComma = before.lastIndexOf(',');
                int argStart  = (lastComma > dotPos) ? lastComma + 1 : dotPos + 1;
                String head = before.substring(0, argStart) + entry.insertion() + ":";
                pendingCursor = head.length();
                return head + after;
            }
            default -> { pendingCursor = -1; return text; }
        }
    }

    // ── state helpers ─────────────────────────────────────────────────────────

    private static int countCommas(String s) {
        int count = 0;
        for (char c : s.toCharArray()) if (c == ',') count++;
        return count;
    }

    public void clear() {
        suggestions = Collections.emptyList();
        selectedIndex = 0;
    }

    public boolean hasSuggestions() { return !suggestions.isEmpty(); }

    public void moveUp() {
        if (!suggestions.isEmpty()) selectedIndex = (selectedIndex - 1 + suggestions.size()) % suggestions.size();
    }

    public void moveDown() {
        if (!suggestions.isEmpty()) selectedIndex = (selectedIndex + 1) % suggestions.size();
    }

    public Entry getSelected() {
        return suggestions.isEmpty() ? null : suggestions.get(selectedIndex);
    }
}
