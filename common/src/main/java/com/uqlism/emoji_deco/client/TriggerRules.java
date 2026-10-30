package com.uqlism.emoji_deco.client;

import java.util.Set;

/**
 * 補完 UI (':' / '#' トリガー) を出してよいかの純関数群。
 *
 * <ul>
 *   <li>チャットで '/' 始まりのコマンド入力は、メッセージ送信系 (/say /me /msg 等) を除き補完しない。
 *       {@code /give @p minecraft:di} の Tab が絵文字補完に奪われてコマンドが壊れるのを防ぐ。</li>
 *   <li>':' の直前が識別子文字 ({@code [A-Za-z0-9_.-]}) なら namespace:id とみなして補完しない。
 *       MFM の絵文字コードも直前が英数字だと無効 (EmojiCodeParser の REGEX_SIDE) なので整合する。
 *       行頭・空白・記号 ({@code ( 、 !} 等) の直後なら有効。</li>
 * </ul>
 */
public final class TriggerRules {
    private TriggerRules() {}

    /** 引数がそのままチャット本文になるコマンド。これらは通常文と同様に補完してよい。 */
    private static final Set<String> MESSAGE_COMMANDS =
            Set.of("say", "me", "msg", "tell", "w", "teammsg", "tm");

    /** 補完を抑止すべきコマンド入力か (メッセージ系コマンドは false)。 */
    public static boolean isSuppressedCommand(String input) {
        if (input == null || !input.startsWith("/")) return false;
        int end = 1;
        while (end < input.length() && !Character.isWhitespace(input.charAt(end))) end++;
        String name = input.substring(1, end).toLowerCase(java.util.Locale.ROOT);
        int colon = name.indexOf(':'); // /minecraft:say 形式
        if (colon >= 0) name = name.substring(colon + 1);
        return !MESSAGE_COMMANDS.contains(name);
    }

    /** colonPos の ':' をショートコード補完のトリガーとして扱ってよいか。 */
    public static boolean isColonTriggerAllowed(String text, int colonPos) {
        if (colonPos <= 0) return true;
        return !isIdentifierChar(text.charAt(colonPos - 1));
    }

    private static boolean isIdentifierChar(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                || c == '_' || c == '.' || c == '-';
    }
}
