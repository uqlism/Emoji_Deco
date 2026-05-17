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
// record クラスへの @Inject RETURN は Mixin 0.8.7 で動作しない場合があるため、
// BookViewScreen 側から @Redirect で BookAccess.m_98310_() 呼び出しをインターセプトする。
@Mixin(BookViewScreen.class)
public class MixinWrittenBookAccess {

    // m_98302_ は BookViewScreen のページ更新メソッドで、内部で BookAccess.m_98310_(index) を呼ぶ。
    // その呼び出しを Redirect してリッチテキスト変換を挟む。
    @Redirect(
        method = "m_98302_",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/inventory/BookViewScreen$BookAccess;m_98310_(I)Lnet/minecraft/network/chat/FormattedText;",
            remap = false
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
