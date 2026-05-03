package com.uqlism.emoji_deco.text;

import com.uqlism.emoji_deco.render.sequence.ConcatSequence;
import com.uqlism.emoji_deco.render.sequence.DynamicFormattedCharSequence;
import com.uqlism.emoji_deco.text.hydrate.HydrateContext;
import com.uqlism.emoji_deco.text.ir.RichNode;
import com.uqlism.emoji_deco.text.parse.RichTextParser;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.LiteralContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Component をリッチテキスト変換する統一変換器。
 *
 * toComponent — Component ツリーを書き換えて Component を返す（本・アイテムツールチップ用）
 * toSequence  — FCS を返す。動的コンテンツは DynamicFormattedCharSequence でラップ
 * computeNow  — FCS を即時評価して返す（DynamicFormattedCharSequence.accept() から使用）
 * toLines     — ワードラップ済みの FCS リストを返す（チャット用）
 */
public final class ComponentConverter {

    private ComponentConverter() {}

    /**
     * Component ツリーを走査して各 LiteralContents に RichTextParser を適用し、
     * 変換済みの Component ツリーを返す。
     * Font.split() 経由でレンダリングされる本・アイテムツールチップ用。
     * scale/glow は Font.split() 内部で FCS に潰されるため未対応。
     */
    public static Component toComponent(Component root) {
        return walkComponent(root);
    }

    private static MutableComponent walkComponent(Component component) {
        var contents = component.getContents();
        var style = component.getStyle();
        var siblings = component.getSiblings();

        if (contents instanceof LiteralContents lc) {
            String text = lc.text();
            HydrateContext.Tracked ctx = HydrateContext.EMPTY.track();
            RichTextParser.parseTracked(text, ctx);
            MutableComponent base;
            if (ctx.extractPattern().usesTime() || ctx.extractPattern().usesPlayerNames()) {
                base = MutableComponent.create(new DynamicRichContents(text, style));
            } else {
                base = RichTextParser.parseInline(text, style).toComponent().withStyle(style);
            }
            for (Component sibling : siblings) base.append(walkComponent(sibling));
            return base;
        }

        if (contents instanceof TranslatableContents tc) {
            Object[] args = tc.getArgs();
            Object[] newArgs = new Object[args.length];
            for (int i = 0; i < args.length; i++) {
                newArgs[i] = (args[i] instanceof Component c) ? walkComponent(c) : args[i];
            }
            MutableComponent base = MutableComponent
                    .create(new TranslatableContents(tc.getKey(), tc.getFallback(), newArgs))
                    .withStyle(style);
            for (Component sibling : siblings) base.append(walkComponent(sibling));
            return base;
        }

        MutableComponent base = MutableComponent.create(contents).withStyle(style);
        for (Component sibling : siblings) base.append(walkComponent(sibling));
        return base;
    }

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
                words.addAll(RichNode.toWordSegments(font, RichTextParser.parse(str), style, width));
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
