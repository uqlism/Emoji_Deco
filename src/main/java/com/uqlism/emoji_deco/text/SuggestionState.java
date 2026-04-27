package com.uqlism.emoji_deco.text;

import com.google.gson.JsonElement;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class SuggestionState {

    public enum TriggerType { SHORTCODE, DECORATOR, DECORATOR_ARG, SHORTCODE_ARG }

    public record Entry(String label, Component preview, String insertion, TriggerType type) {}

    public static String lastInput = "";
    public static int lastCursor = 0;
    public static int pendingCursor = -1;
    public static List<Entry> suggestions = Collections.emptyList();
    public static int selectedIndex = 0;
    public static int triggerPos = 0;

    public static void update(String text, int cursor) {
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
        if (text.indexOf(':', pos + 1) >= 0) return -1;
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
        DecoratorManager.getSuggestions(prefix).forEach(name -> {
            Component preview = DecoratorManager.resolve(name, Component.literal(name));
            if (preview == null) preview = Component.literal("#" + name + "[]");
            results.add(new Entry("#" + name + "[]", preview, name, TriggerType.DECORATOR));
        });
        return results;
    }

    private static List<Entry> buildShortcodeSuggestions(String prefix) {
        if (prefix.isEmpty()) return Collections.emptyList();
        List<Entry> results = new ArrayList<>();
        ShortcodeManager.getSuggestions(prefix).forEach(code ->
                results.add(new Entry(":" + code + ":", ShortcodeManager.resolve(code), code, TriggerType.SHORTCODE)));
        ShortcodeManager.getAliasSuggestions(prefix).forEach(e -> {
            String canonical = e.getValue();
            results.add(new Entry(":" + canonical + ":", ShortcodeManager.resolve(canonical), canonical, TriggerType.SHORTCODE));
        });
        return results;
    }

    private static List<Entry> buildArgSuggestions(JsonElement spec, String prefix, TriggerType type) {
        if (spec == null) return Collections.emptyList();
        return EmojiDecoComponentParser.resolveStringList(spec).stream()
                .filter(v -> v.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT)))
                .map(v -> {
                    TextColor color = TextColor.parseColor(v);
                    Component preview = color != null
                            ? Component.literal("■ " + v).withStyle(Style.EMPTY.withColor(color))
                            : Component.literal(v);
                    return new Entry(v, preview, v, type);
                })
                .collect(Collectors.toList());
    }

    // ── apply completion ──────────────────────────────────────────────────────

    public static String applyTo(String text, Entry entry) {
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

    public static void clear() {
        suggestions = Collections.emptyList();
        selectedIndex = 0;
    }

    public static boolean hasSuggestions() { return !suggestions.isEmpty(); }

    public static void moveUp() {
        if (!suggestions.isEmpty()) selectedIndex = (selectedIndex - 1 + suggestions.size()) % suggestions.size();
    }

    public static void moveDown() {
        if (!suggestions.isEmpty()) selectedIndex = (selectedIndex + 1) % suggestions.size();
    }

    public static Entry getSelected() {
        return suggestions.isEmpty() ? null : suggestions.get(selectedIndex);
    }
}
