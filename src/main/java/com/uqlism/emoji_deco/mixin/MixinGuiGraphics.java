package com.uqlism.emoji_deco.mixin;

import com.uqlism.emoji_deco.text.ir.RichNode;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * ホバーツールチップ描画を乗っ取り、emoji_deco:hover/text の hover_contents を
 * RichNode.toSequence() 経由でレンダリングする。
 * scale / glow 等のトランスフォームが MixinFont.drawInBatch 経由でホバーでも有効になる。
 */
@Mixin(Screen.class)
public abstract class MixinGuiGraphics {

    @Inject(method = "renderComponentHoverEffect", at = @At("HEAD"), cancellable = true)
    private static void onRenderComponentHoverEffect(
            GuiGraphics guiGraphics, Font font, @Nullable HoverEvent hoverEvent,
            int mouseX, int mouseY, CallbackInfo ci) {
        if (hoverEvent == null || hoverEvent.getAction() != HoverEvent.Action.SHOW_TEXT) return;
        Component hoverComp = hoverEvent.getValue(HoverEvent.Action.SHOW_TEXT);
        if (hoverComp == null) return;

        RichNode node = RichNode.lookupHoverNode(hoverComp);
        if (node == null) return;

        FormattedCharSequence seq = RichNode.toSequence(font, node);
        guiGraphics.renderTooltip(font, List.of(seq), mouseX, mouseY);
        ci.cancel();
    }
}
