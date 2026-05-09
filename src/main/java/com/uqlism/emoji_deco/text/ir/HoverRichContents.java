package com.uqlism.emoji_deco.text.ir;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;

import java.util.Optional;

/**
 * hoverComp の ComponentContents として RichNode を直接格納する。
 * HOVER_REGISTRY (WeakHashMap) を廃止し参照ライフタイムの問題を根本解決する。
 *
 * visit() は MC の標準テキスト処理（アクセシビリティ・クリップボードコピー等）向けに
 * node.toComponent() に委譲する。ホバー描画は MixinGuiGraphics が getContents() で
 * このクラスを検出して toSequence() を使うため visit() の結果は使われない。
 */
public record HoverRichContents(RichNode node) implements ComponentContents {

    public static final ComponentContents.Type<HoverRichContents> TYPE = new ComponentContents.Type<>(
            MapCodec.unit(new HoverRichContents(RichNode.empty())), "emoji_deco:hover_rich");

    @Override
    public ComponentContents.Type<HoverRichContents> type() {
        return TYPE;
    }

    @Override
    public <T> Optional<T> visit(FormattedText.StyledContentConsumer<T> consumer, Style style) {
        return node.toComponent().visit(consumer, style);
    }

    @Override
    public <T> Optional<T> visit(FormattedText.ContentConsumer<T> consumer) {
        return node.toComponent().visit(consumer);
    }

    @Override
    public String toString() {
        return "hover_rich(" + node + ")";
    }
}
