package com.uqlism.emoji_deco.mixin;

import com.uqlism.emoji_deco.Config;
import com.uqlism.emoji_deco.render.sequence.DynamicLineSequence;
import com.uqlism.emoji_deco.text.ComponentConverter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.List;

/**
 * ComponentRenderUtils.wrapComponents() を差し替えてリッチテキストを適用する。
 *
 * ComponentConverter.toLines(font, component, width) で
 * ワードラップ・scale/glow・hover/click を同時に処理する。
 * 動的コンテンツ（#rainbow 等）は DynamicLineSequence でラップして毎フレーム再評価。
 */
@Mixin(ChatComponent.class)
public class MixinChatComponent {

    @Redirect(
        method = "m_240465_",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/ComponentRenderUtils;m_94005_(Lnet/minecraft/network/chat/FormattedText;ILnet/minecraft/client/gui/Font;)Ljava/util/List;",
            remap = false
        ),
        remap = false,
        require = 0
    )
    private List<FormattedCharSequence> runicink$wrapDynamicAdd(FormattedText text, int width, Font font) {
        return wrap(text, width, font);
    }

    @Redirect(
        method = "m_93795_",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/ComponentRenderUtils;m_94005_(Lnet/minecraft/network/chat/FormattedText;ILnet/minecraft/client/gui/Font;)Ljava/util/List;",
            remap = false
        ),
        remap = false,
        require = 0
    )
    private List<FormattedCharSequence> runicink$wrapDynamicRescale(FormattedText text, int width, Font font) {
        return wrap(text, width, font);
    }

    private static List<FormattedCharSequence> wrap(FormattedText text, int width, Font font) {
        if (!Config.enableChat || !(text instanceof Component c)) {
            return font.split(text, width);
        }
        List<FormattedCharSequence> lines = ComponentConverter.toLines(font, c, width);
        if (!ComponentConverter.isDynamic(c)) return lines;
        // 動的コンテンツは各行を毎フレーム再評価する DynamicLineSequence でラップ
        List<FormattedCharSequence> dynamic = new ArrayList<>(lines.size());
        for (int i = 0; i < lines.size(); i++) {
            dynamic.add(new DynamicLineSequence(c, i, width));
        }
        return dynamic;
    }
}
