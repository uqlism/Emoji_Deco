package com.uqlism.emoji_deco.mixin;

import com.uqlism.emoji_deco.text.ir.HoverRichContents;
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

import java.util.ArrayList;
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
        if (!(hoverComp != null && hoverComp.getContents() instanceof HoverRichContents hrc)) return;
        RichNode node = hrc.node();

        FormattedCharSequence seq = RichNode.toSequence(font, node);

        // scale 等で視覚高さが lineHeight を超える場合、空行を足してボックスを膨らませる。
        // 1行目の AffineSequence が scale 分だけ下に広がり、空行の領域を自然に埋める。
        int visualH   = visualHeight(font, node);
        int lineH     = font.lineHeight;
        int lineCount = Math.max(1, (visualH + lineH - 1) / lineH);

        List<FormattedCharSequence> lines = new ArrayList<>(lineCount);
        lines.add(seq);
        for (int i = 1; i < lineCount; i++) lines.add(FormattedCharSequence.EMPTY);

        ((GuiGraphics)(Object)this).renderTooltip(font, lines, mouseX, mouseY);
        ci.cancel();
    }

    // ── 視覚高さ計算 ──────────────────────────────────────────────────────────

    private static int visualHeight(Font font, RichNode node) {
        if (node instanceof RichNode.Scaled s)
            return Math.max(1, Math.round(maxChildH(font, s.children()) * Math.abs(s.scaleY())));
        if (node instanceof RichNode.Image img)
            return img.displayH();
        if (node instanceof RichNode.Offset o)
            return maxChildH(font, o.children()) + Math.max(0, Math.round(o.y()));
        List<RichNode> children = childrenOf(node);
        return children.isEmpty() ? font.lineHeight : maxChildH(font, children);
    }

    private static int maxChildH(Font font, List<RichNode> children) {
        return children.stream().mapToInt(c -> visualHeight(font, c)).max().orElse(font.lineHeight);
    }

    private static List<RichNode> childrenOf(RichNode node) {
        if (node instanceof RichNode.Text t)    return t.children();
        if (node instanceof RichNode.Glowing g) return g.children();
        if (node instanceof RichNode.Hover hm) return hm.children();
        if (node instanceof RichNode.Rotated r) return r.children();
        return List.of();
    }
}
