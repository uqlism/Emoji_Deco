package com.uqlism.emoji_deco.text;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class SuggestionState {

    public enum TriggerType { SHORTCODE, USERNAME, DECORATOR }

    public record Entry(String label, Component preview, String insertion, TriggerType type) {}

    public static String lastInput = "";
    public static int lastCursor = 0;
    public static int pendingCursor = -1;
    public static List<Entry> suggestions = Collections.emptyList();
    public static int selectedIndex = 0;
    public static int triggerPos = 0; // screen-X anchor: index of the trigger char in lastInput

    /** Called with cursor position so trigger detection only looks at text before the cursor. */
    public static void update(String text, int cursor) {
        lastInput = text;
        lastCursor = Math.min(cursor, text.length());
        String active = text.substring(0, lastCursor);

        int colonPos = findActiveColonPos(active);
        int atPos    = findActiveAtPos(active);
        int hashPos  = findActiveHashPos(active);

        if (colonPos < 0 && atPos < 0 && hashPos < 0) {
            clear();
            return;
        }

        int maxPos = Math.max(Math.max(colonPos, atPos), hashPos);

        if (maxPos == hashPos) {
            triggerPos = hashPos;
            suggestions = buildStyleTagSuggestions(active.substring(hashPos + 1));
        } else if (maxPos == atPos) {
            triggerPos = atPos;
            suggestions = buildUsernameSuggestions(active.substring(atPos + 1));
        } else {
            triggerPos = colonPos;
            suggestions = buildShortcodeSuggestions(active.substring(colonPos + 1));
        }
        selectedIndex = 0;
    }

    /** Returns the position of the last unclosed ':' trigger, or -1. */
    private static int findActiveColonPos(String text) {
        int pos = text.lastIndexOf(':');
        if (pos < 0) return -1;
        // Already closed if there's another ':' after this one
        if (text.indexOf(':', pos + 1) >= 0) return -1;
        return pos;
    }

    /** Returns the position of the last '@' trigger (all following chars are username-safe), or -1. */
    private static int findActiveAtPos(String text) {
        int pos = text.lastIndexOf('@');
        if (pos < 0) return -1;
        for (int i = pos + 1; i < text.length(); i++) {
            if (!isUsernameChar(text.charAt(i))) return -1;
        }
        return pos;
    }

    /**
     * Returns the position of the last '#' trigger where all following chars are tag-name-safe
     * (letters, digits, '_', '-') and the trigger is NOT followed by a space (which would be a header).
     * Returns -1 if no active '#' trigger found.
     */
    private static int findActiveHashPos(String text) {
        int pos = text.lastIndexOf('#');
        if (pos < 0) return -1;
        String after = text.substring(pos + 1);
        // Must not be a header (# followed by space or another #)
        if (after.isEmpty()) return pos; // just typed '#', show all suggestions
        if (after.charAt(0) == ' ' || after.charAt(0) == '#') return -1;
        // All chars after '#' must be valid tag-name chars
        for (char c : after.toCharArray()) {
            if (!Character.isLetterOrDigit(c) && c != '_' && c != '-') return -1;
        }
        return pos;
    }

    private static boolean isUsernameChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    private static List<Entry> buildStyleTagSuggestions(String prefix) {
        // Show all tags when prefix is empty (just '#' was typed)
        List<Entry> results = new java.util.ArrayList<>();
        DecoratorManager.getSuggestions(prefix).forEach(name -> {
            // preview: apply the tag to a sample content so user can see what it looks like
            Component preview = DecoratorManager.resolve(name, Component.literal(name));
            if (preview == null) preview = Component.literal("#" + name + "[]");
            results.add(new Entry("#" + name + "[]", preview, name, TriggerType.DECORATOR));
        });
        return results;
    }

    private static List<Entry> buildShortcodeSuggestions(String prefix) {
        if (prefix.isEmpty()) return Collections.emptyList();
        List<Entry> results = new java.util.ArrayList<>();

        // Canonical name matches
        ShortcodeManager.getSuggestions(prefix).forEach(code ->
                results.add(new Entry(":" + code + ":", ShortcodeManager.resolve(code), code, TriggerType.SHORTCODE)));

        // Alias matches — label shows the alias, insertion uses the canonical name
        ShortcodeManager.getAliasSuggestions(prefix).forEach(e -> {
            String alias = e.getKey();
            String canonical = e.getValue();
            results.add(new Entry(":" + canonical + ":", ShortcodeManager.resolve(canonical), canonical, TriggerType.SHORTCODE));
        });

        return results;
    }

    private static List<Entry> buildUsernameSuggestions(String prefix) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getConnection() == null) return Collections.emptyList();
        return mc.getConnection().getOnlinePlayers().stream()
                .map(pi -> pi.getProfile().getName())
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT)))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .map(name -> new Entry(
                        "@" + name,
                        SpriteRegistry.createHeadComponent(name),
                        name,
                        TriggerType.USERNAME))
                .collect(Collectors.toList());
    }

    /**
     * Applies the selected completion. Only text before lastCursor is used for trigger
     * detection; text after lastCursor is preserved. Sets pendingCursor to the desired
     * cursor position in the resulting string (-1 if no repositioning needed).
     */
    public static String applyTo(String text, Entry entry) {
        int split = Math.min(lastCursor, text.length());
        String before = text.substring(0, split);
        String after  = text.substring(split);

        if (entry.type() == TriggerType.SHORTCODE) {
            int pos = before.lastIndexOf(':');
            if (pos < 0) { pendingCursor = -1; return text; }
            String head = before.substring(0, pos + 1) + entry.insertion() + ":";
            pendingCursor = head.length();
            return head + after;
        } else if (entry.type() == TriggerType.DECORATOR) {
            int pos = before.lastIndexOf('#');
            if (pos < 0) { pendingCursor = -1; return text; }
            String head = before.substring(0, pos) + "#" + entry.insertion() + "[";
            pendingCursor = head.length(); // cursor inside []
            return head + "]" + after;
        } else {
            int pos = before.lastIndexOf('@');
            if (pos < 0) { pendingCursor = -1; return text; }
            String head = before.substring(0, pos + 1) + entry.insertion();
            pendingCursor = head.length();
            return head + after;
        }
    }

    public static void clear() {
        suggestions = Collections.emptyList();
        selectedIndex = 0;
    }

    public static boolean hasSuggestions() {
        return !suggestions.isEmpty();
    }

    public static void moveUp() {
        if (suggestions.isEmpty()) return;
        selectedIndex = (selectedIndex - 1 + suggestions.size()) % suggestions.size();
    }

    public static void moveDown() {
        if (suggestions.isEmpty()) return;
        selectedIndex = (selectedIndex + 1) % suggestions.size();
    }

    public static Entry getSelected() {
        if (suggestions.isEmpty()) return null;
        return suggestions.get(selectedIndex);
    }
}
