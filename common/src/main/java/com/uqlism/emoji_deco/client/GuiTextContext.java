package com.uqlism.emoji_deco.client;

/**
 * GuiGraphics.drawString(Component) 経路の変換可否を決める。
 * ホットバーのアイテム名は enableItemNames、それ以外の GUI テキスト (title/actionbar/ボスバー等) は
 * enableGui に従う。ホットバー名の描画中かどうかは MixinGui が renderSelectedItemName の前後で立てる。
 * クライアント描画スレッドからのみアクセスされる。
 */
public final class GuiTextContext {
    private GuiTextContext() {}

    private static boolean inHotbarItemName = false;

    public static void setInHotbarItemName(boolean v) { inHotbarItemName = v; }

    public static boolean isInHotbarItemName() { return inHotbarItemName; }

    public static boolean isEnabled(boolean inHotbarItemName, boolean enableGui, boolean enableItemNames) {
        return inHotbarItemName ? enableItemNames : enableGui;
    }
}
