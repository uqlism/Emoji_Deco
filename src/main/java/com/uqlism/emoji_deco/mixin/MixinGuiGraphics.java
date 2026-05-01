package com.uqlism.emoji_deco.mixin;

import com.uqlism.emoji_deco.text.ir.RichNode;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
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
 *
 * SRG: m_280304_ → GuiGraphics.renderComponentHoverEffect(Font, Style, int, int)
 */
@Mixin(GuiGraphics.class)
public abstract class MixinGuiGraphics {

    // SRG: m_280304_ → renderComponentHoverEffect(Font, Style, int, int)
    @Inject(method = "m_280304_", at = @At("HEAD"), cancellable = true, remap = false)
    private void onRenderComponentHoverEffect(Font font, @Nullable Style style,
                                               int mouseX, int mouseY, CallbackInfo ci) {
        if (style == null) return;
        HoverEvent hoverEvent = style.getHoverEvent();
        if (hoverEvent == null || hoverEvent.getAction() != HoverEvent.Action.SHOW_TEXT) return;

        Component hoverComp = hoverEvent.getValue(HoverEvent.Action.SHOW_TEXT);
        if (hoverComp == null) return;

        RichNode node = RichNode.lookupHoverNode(hoverComp);
        if (node == null) return;

        FormattedCharSequence seq = RichNode.toSequence(font, node);
        ((GuiGraphics)(Object)this).renderTooltip(font, List.of(seq), mouseX, mouseY);
        ci.cancel();
    }
}
