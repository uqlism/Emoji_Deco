package com.uqlism.emoji_deco.text;

import java.util.Collections;
import java.util.List;

public class ChatSuggestionState {
    public static String lastInput = "";
    public static List<String> suggestions = Collections.emptyList();
    public static int selectedIndex = 0;
    public static String pendingCompletion = null;

    // 入力から現在補完対象のプレフィックスを取得
    // 最後の未閉じ ":xxx" を返す。なければnull
    public static String getActivePrefix(String text) {
        int lastColon = text.lastIndexOf(':');
        if (lastColon < 0) return null;
        String after = text.substring(lastColon + 1);
        // すでに閉じられている（":stone:" の後ろの:）なら無効
        if (after.contains(":")) return null;
        return after;
    }

    public static void update(String text) {
        lastInput = text;
        String prefix = getActivePrefix(text);
        if (prefix == null || prefix.isEmpty()) {
            suggestions = Collections.emptyList();
            selectedIndex = 0;
            return;
        }
        suggestions = ShortcodeManager.getSuggestions(prefix);
        selectedIndex = Math.min(selectedIndex, Math.max(0, suggestions.size() - 1));
    }

    public static void moveUp() {
        if (suggestions.isEmpty()) return;
        selectedIndex = (selectedIndex - 1 + suggestions.size()) % suggestions.size();
    }

    public static void moveDown() {
        if (suggestions.isEmpty()) return;
        selectedIndex = (selectedIndex + 1) % suggestions.size();
    }

    public static String getSelected() {
        if (suggestions.isEmpty()) return null;
        return suggestions.get(selectedIndex);
    }
}