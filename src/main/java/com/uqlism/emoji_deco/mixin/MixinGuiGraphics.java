package com.uqlism.emoji_deco.mixin;

import com.uqlism.emoji_deco.render.sequence.DynamicFormattedCharSequence;
import com.uqlism.emoji_deco.text.ComponentConverter;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI テキスト描画を乗っ取り、emoji_deco のリッチテキストを適用する。
 *
 * drawString(Component) / drawCenteredString(Component) は内部で
 * Language.getVisualOrder() を呼んで静的 FCS に変換するため、
 * MixinFont のキャッチオールでは動的デコレータ（#rainbow 等）が毎フレーム
 * 再評価されない。
 * → HEAD inject でキャンセルし、DynamicFormattedCharSequence でラップして渡す。
 *   MixinFont.drawInBatch の HEAD inject が DynamicFormattedCharSequence を検出して
 *   毎フレーム computeNow() を呼ぶことで、時間依存デコレータのアニメーションが動く。
 *
 * SRG: m_280304_ → GuiGraphics.renderComponentHoverEffect(Font, Style, int, int)
 */
@Mixin(GuiGraphics.class)
public abstract class MixinGuiGraphics {

    // ── Component ベース描画の変換 ─────────────────────────────────────────────
    // GuiGraphics.drawString(Component) / drawCenteredString(Component) は
    // Language.getVisualOrder() を経由して FCS に変換するため MixinFont catch-all は効かない。
    // Language は抽象クラスで inject 不可のため、ここで Component → FCS を直接処理する。
    //
    // 動的デコレータ（#rainbow 等）は isDynamic() にかかわらず DynamicFormattedCharSequence で
    // ラップする。isDynamic() = false でも幅計算には computeNow() が使われるため問題ない。
    // MixinFont.drawInBatch が DynamicFormattedCharSequence を毎フレーム computeNow() で解決する。

    // drawString(Font, Component, int, int, int, boolean) -> int
    @Inject(method = "drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)I", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void runicink$drawStringComponent(
            Font font, Component text, int x, int y, int color, boolean dropShadow,
            CallbackInfoReturnable<Integer> cir) {
        String raw = text.getString();
        if (raw.indexOf('#') < 0 && raw.indexOf(':') < 0) return;
        // 静的評価 (computeNow) で幅だけ先に取得してセンタリング補正を計算する。
        // 実際の描画は DynamicFormattedCharSequence → MixinFont が毎フレーム computeNow() を呼ぶ。
        FormattedCharSequence staticFcs = ComponentConverter.computeNow(font, text);
        int adj = (font.width(text) - font.width(staticFcs)) / 2;
        FormattedCharSequence fcs = new DynamicFormattedCharSequence(text);
        cir.setReturnValue(((GuiGraphics)(Object)this).drawString(font, fcs, x + adj, y, color, dropShadow));
    }

    // drawCenteredString(Font, Component, int, int, int) -> void
    @Inject(method = "drawCenteredString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void runicink$drawCenteredStringComponent(
            Font font, Component text, int x, int y, int color,
            CallbackInfo ci) {
        String raw = text.getString();
        if (raw.indexOf('#') < 0 && raw.indexOf(':') < 0) return;
        // 幅計算は静的評価で取得し、描画は DynamicFormattedCharSequence で動的再評価する。
        FormattedCharSequence staticFcs = ComponentConverter.computeNow(font, text);
        int halfW = font.width(staticFcs) / 2;
        FormattedCharSequence fcs = new DynamicFormattedCharSequence(text);
        ((GuiGraphics)(Object)this).drawString(font, fcs, x - halfW, y, color);
        ci.cancel();
    }

    // ── ホバーツールチップ ────────────────────────────────────────────────────
    // renderComponentHoverEffect(Font, Style, int, int)
    @Inject(method = "renderComponentHoverEffect", at = @At("HEAD"), cancellable = true, remap = false)
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
