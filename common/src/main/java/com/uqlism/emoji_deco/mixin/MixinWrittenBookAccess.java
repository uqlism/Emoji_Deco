package com.uqlism.emoji_deco.mixin;

import com.uqlism.emoji_deco.Config;
import com.uqlism.emoji_deco.text.ComponentConverter;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// 1.21.1 で BookViewScreen$WrittenBookAccess が削除され BookViewScreen$BookAccess (record) に変わった。
// 1.21.1 は Mojang マッピングを使用しているため、SRG 名ではなく Mojang 名（render / getPage）で指定する。
// remap = false + Mojang 名で開発環境・リリース環境ともに動作する。
@Mixin(BookViewScreen.class)
public class MixinWrittenBookAccess {

    // render(GuiGraphics, int, int, float) が内部で BookAccess.getPage(index) を呼ぶ。
    // その呼び出しを Redirect してリッチテキスト変換を挟む。
    @Redirect(
        method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/inventory/BookViewScreen$BookAccess;getPage(I)Lnet/minecraft/network/chat/FormattedText;"
        ),
        remap = false,
        require = 0
    )
    private FormattedText runicink$redirectGetPage(BookViewScreen.BookAccess bookAccess, int index) {
        FormattedText page = bookAccess.getPage(index);
        if (!Config.enableBooks) return page;
        if (!(page instanceof Component component)) return page;
        if (component.getString().isBlank()) return page;
        return ComponentConverter.toComponent(component);
    }
}
