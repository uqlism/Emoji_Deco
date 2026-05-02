package com.uqlism.emoji_deco.text;

import com.uqlism.emoji_deco.render.sequence.ConcatSequence;
import com.uqlism.emoji_deco.render.sequence.DynamicFormattedCharSequence;
import com.uqlism.emoji_deco.text.hydrate.HydrateContext;
import com.uqlism.emoji_deco.text.ir.RichNode;
import com.uqlism.emoji_deco.text.parse.RichTextParser;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Component ツリーを RichTextParser + RichNode.toSequence() で変換する統一変換器。
 *
 * Component.visit(StyledContentConsumer, Style) を使って MC 側に
 *   - TranslatableContents の翻訳解決・%s 展開
 *   - sibling の走査
 *   - Style の継承（ClickEvent / HoverEvent 含む）
 * を委ねつつ、各テキスト片に RichTextParser を適用する。
 * これにより scale/glow 対応と ClickEvent 保持を両立する。
 */
public final class ComponentSequenceConverter {

    private ComponentSequenceConverter() {}

    /**
     * Component を変換して FormattedCharSequence を返す。
     * 動的コンテンツを含む場合は DynamicFormattedCharSequence でラップする。
     */
    public static FormattedCharSequence toSequence(Font font, Component component) {
        if (isDynamic(component)) {
            return new DynamicFormattedCharSequence(component);
        }
        return computeNow(font, component);
    }

    /**
     * 毎フレーム呼ばれる変換。DynamicFormattedCharSequence.accept() から使用。
     */
    public static FormattedCharSequence computeNow(Font font, Component component) {
        List<FormattedCharSequence> parts = new ArrayList<>();
        component.visit((style, str) -> {
            if (!str.isEmpty()) {
                FormattedCharSequence seq = RichNode.toSequence(font, RichTextParser.parse(str), style);
                if (seq != FormattedCharSequence.EMPTY) parts.add(seq);
            }
            return Optional.empty();
        }, Style.EMPTY);
        if (parts.isEmpty()) return FormattedCharSequence.EMPTY;
        if (parts.size() == 1) return parts.get(0);
        return new ConcatSequence(parts);
    }

    /**
     * Component を変換しつつワードラップした FormattedCharSequence のリストを返す。
     * RichNode.toWordSegments() 経由なので scale/glow も正しく折り返せる。
     */
    public static List<FormattedCharSequence> toLines(Font font, Component component, int width) {
        List<FormattedCharSequence> words = new ArrayList<>();
        component.visit((style, str) -> {
            if (!str.isEmpty()) {
                words.addAll(RichNode.toWordSegments(font, RichTextParser.parse(str), style));
            }
            return Optional.empty();
        }, Style.EMPTY);
        return packIntoLines(font, words, width);
    }

    private static List<FormattedCharSequence> packIntoLines(Font font,
                                                              List<FormattedCharSequence> words,
                                                              int maxWidth) {
        List<FormattedCharSequence> lines = new ArrayList<>();
        List<FormattedCharSequence> currentLine = new ArrayList<>();
        int lineWidth = 0;
        for (FormattedCharSequence word : words) {
            int ww = font.width(word);
            if (ww == 0) continue;
            if (lineWidth > 0 && lineWidth + ww > maxWidth) {
                lines.add(currentLine.size() == 1 ? currentLine.get(0) : new ConcatSequence(new ArrayList<>(currentLine)));
                currentLine.clear();
                lineWidth = 0;
            }
            currentLine.add(word);
            lineWidth += ww;
        }
        if (!currentLine.isEmpty())
            lines.add(currentLine.size() == 1 ? currentLine.get(0) : new ConcatSequence(new ArrayList<>(currentLine)));
        return lines.isEmpty() ? List.of(FormattedCharSequence.EMPTY) : lines;
    }

    /**
     * Component ツリーのいずれかのテキスト片に時刻・プレイヤー名などの動的パターンが含まれるか検査。
     * 見つかり次第 Optional.of(true) で短絡。
     */
    public static boolean isDynamic(Component component) {
        return component.visit((style, str) -> {
            if (!str.isEmpty()) {
                HydrateContext.Tracked ctx = HydrateContext.EMPTY.track();
                RichTextParser.parseTracked(str, ctx);
                var pattern = ctx.extractPattern();
                if (pattern.usesTime() || pattern.usesPlayerNames()) {
                    return Optional.of(true);
                }
            }
            return Optional.empty();
        }, Style.EMPTY).isPresent();
    }
}
