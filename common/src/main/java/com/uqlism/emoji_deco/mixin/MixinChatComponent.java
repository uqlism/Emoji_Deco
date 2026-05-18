package com.uqlism.emoji_deco.mixin;

import com.uqlism.emoji_deco.Config;
import com.uqlism.emoji_deco.render.sequence.DynamicLineSequence;
import com.uqlism.emoji_deco.text.ComponentConverter;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.List;

/**
 * チャットメッセージにリッチテキストを適用する。
 *
 * 1.21.1: @ModifyArg で addMessage 内の addMessageToDisplayQueue 呼び出しの
 * GuiMessage を差し替えて Component をリッチテキスト変換する。
 * 旧 @Redirect (require=0) は互換性のため残す。
 */
@Mixin(ChatComponent.class)
public class MixinChatComponent {

    /**
     * 1.21.1: addMessage 内の addMessageToDisplayQueue 呼び出しの GuiMessage 引数を
     * @ModifyArg で差し替えて Component をリッチテキスト変換する。
     */
    @ModifyArg(
        method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/ChatComponent;addMessageToDisplayQueue(Lnet/minecraft/client/GuiMessage;)V",
            remap = false
        ),
        index = 0, remap = false, require = 0
    )
    private net.minecraft.client.GuiMessage runicink$modifyGuiMessage(net.minecraft.client.GuiMessage message) {
        if (!Config.enableChat) return message;
        Component transformed = ComponentConverter.toComponent(message.content());
        if (transformed == message.content()) return message;
        return new net.minecraft.client.GuiMessage(
            message.addedTime(), transformed, message.signature(), message.tag()
        );
    }

    /**
     * 旧方式 (wrapComponents を直接呼ぶ Forge バージョン向け): require=0 で互換性確保。
     */
    @Redirect(
        method = "addMessageToDisplayQueue",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/ComponentRenderUtils;wrapComponents(Lnet/minecraft/network/chat/FormattedText;ILnet/minecraft/client/gui/Font;)Ljava/util/List;",
            remap = false
        ),
        remap = false,
        require = 0
    )
    private List<FormattedCharSequence> runicink$wrapDynamicAdd(FormattedText text, int width, Font font) {
        return wrap(text, width, font);
    }

    private static List<FormattedCharSequence> wrap(FormattedText text, int width, Font font) {
        if (!Config.enableChat || !(text instanceof Component c)) {
            return font.split(text, width);
        }
        List<FormattedCharSequence> lines = ComponentConverter.toLines(font, c, width);
        if (!ComponentConverter.isDynamic(c)) return lines;
        List<FormattedCharSequence> dynamic = new ArrayList<>(lines.size());
        for (int i = 0; i < lines.size(); i++) {
            dynamic.add(new DynamicLineSequence(c, i, width));
        }
        return dynamic;
    }
}
