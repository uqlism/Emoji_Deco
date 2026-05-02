package com.uqlism.emoji_deco.mixin;

import com.uqlism.emoji_deco.Config;
import com.uqlism.emoji_deco.render.sequence.DynamicLineSequence;
import com.uqlism.emoji_deco.text.ComponentSequenceConverter;
import com.uqlism.emoji_deco.text.ComponentTransformer;
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
 * Vanilla のワードラップ（font.split）を活かした上で mod の変換を重ねる:
 *   1. ComponentTransformer.transform() → toComponent() 経路（絵文字・hover・click 対応）
 *   2. font.split(transformed, width)  → vanilla ワードラップ
 *   3. 動的コンテンツは DynamicLineSequence でラップして毎フレーム再評価
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
        // vanilla ワードラップ（toComponent 経路: 絵文字・hover・click 対応、scale/glow は非対応）
        Component transformed = ComponentTransformer.transform(c);
        List<FormattedCharSequence> lines = font.split(transformed, width);

        // 動的コンテンツ（#rainbow 等）は各行を毎フレーム再評価する DynamicLineSequence に差し替える
        if (!ComponentSequenceConverter.isDynamic(c)) return lines;
        List<FormattedCharSequence> dynamic = new ArrayList<>(lines.size());
        for (int i = 0; i < lines.size(); i++) {
            dynamic.add(new DynamicLineSequence(c, i, width));
        }
        return dynamic;
    }
}
