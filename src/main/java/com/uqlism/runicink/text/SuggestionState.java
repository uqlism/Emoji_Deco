package com.uqlism.runicink.text;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class SuggestionState {

    public enum TriggerType { SHORTCODE, USERNAME }

    public record Entry(String label, Component preview, String insertion, TriggerType type) {}

    public static String lastInput = "";
    public static List<Entry> suggestions = Collections.emptyList();
    public static int selectedIndex = 0;

    public static void update(String text) {
        lastInput = text;

        int colonPos = findActiveColonPos(text);
        int atPos    = findActiveAtPos(text);

        if (colonPos < 0 && atPos < 0) {
            clear();
            return;
        }

        if (atPos > colonPos) {
            String prefix = text.substring(atPos + 1);
            suggestions = buildUsernameSuggestions(prefix);
        } else {
            String prefix = text.substring(colonPos + 1);
            suggestions = buildShortcodeSuggestions(prefix);
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

    private static boolean isUsernameChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
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
            results.add(new Entry(":" + alias + ":", ShortcodeManager.resolve(canonical), canonical, TriggerType.SHORTCODE));
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
     * Returns the text after applying the selected completion to the current input.
     * Shortcode: replaces from the last ':' → "prefix:insertion:"
     * Username:  replaces from the last '@' → "prefix@insertion"
     */
    public static String applyTo(String text, Entry entry) {
        if (entry.type() == TriggerType.SHORTCODE) {
            int pos = text.lastIndexOf(':');
            return pos < 0 ? text : text.substring(0, pos + 1) + entry.insertion() + ":";
        } else {
            int pos = text.lastIndexOf('@');
            return pos < 0 ? text : text.substring(0, pos + 1) + entry.insertion();
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
