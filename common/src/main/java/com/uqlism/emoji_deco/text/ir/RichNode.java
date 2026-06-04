package com.uqlism.emoji_deco.text.ir;

import net.minecraft.client.gui.Font;
import com.uqlism.emoji_deco.render.image.ImageGlyphPool;
import com.uqlism.emoji_deco.render.image.ImageSpec;

import com.uqlism.emoji_deco.render.sequence.AffineSequence;
import com.uqlism.emoji_deco.render.sequence.ConcatSequence;
import com.uqlism.emoji_deco.render.sequence.LightMode;
import com.uqlism.emoji_deco.render.sequence.LightSequence;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Typed intermediate representation of parsed rich text.
 *
 * Nodes:
 *   Text(literal, style, children) — text fragment with optional style delta and sub-nodes
 *   Glowing(children)              — glow effect wrapper
 *   Image(sourceSpec, crop, w, h, advanceOverride) — inline image via ImageGlyphPool
 *   Offset(x, y, z, children)     — spatial offset; z shifts depth for overlay ordering
 *   Scaled / Rotated               — other spatial transform wrappers (sign/graffiti only)
 */
public sealed interface RichNode permits RichNode.Text, RichNode.Glowing,
                                         RichNode.Image, RichNode.Hover,
                                         RichNode.Click, RichNode.Insertion,
                                         RichNode.Offset, RichNode.Scaled, RichNode.Rotated {

    record Text(String literal, Style style, List<RichNode> children)     implements RichNode {}
    record Glowing(LightMode lightMode, List<RichNode> children)           implements RichNode {}
    /**
     * インライン画像グリフ。
     * imageSpec:       fetch/decode 層を表す型付き仕様
     * crop:            null=フル / [x0,y0,x1,y1] ソース画像内のピクセル座標
     * advanceOverride: Float.NaN = displayW+1 のデフォルト（face は 0 を指定）
     */
    record Image(ImageSpec imageSpec, @Nullable int[] crop,
                 int displayW, int displayH,
                 float advanceOverride) implements RichNode {
        @Override public boolean equals(Object o) {
            if (!(o instanceof Image i)) return false;
            return Objects.equals(imageSpec, i.imageSpec) && Arrays.equals(crop, i.crop)
                    && displayW == i.displayW && displayH == i.displayH
                    && Float.compare(advanceOverride, i.advanceOverride) == 0;
        }
        @Override public int hashCode() {
            return Objects.hash(imageSpec, Arrays.hashCode(crop), displayW, displayH, advanceOverride);
        }
    }
    /** hover/* wrapper。MC の HoverEvent を保持（hover/text は HoverRichContents 経由で SHOW_TEXT）。 */
    record Hover(HoverEvent hoverEvent, List<RichNode> children)         implements RichNode {}
    /** click/* wrapper。MC の ClickEvent を保持。 */
    record Click(ClickEvent clickEvent, List<RichNode> children)           implements RichNode {}
    /** insertion wrapper。Shift+クリックでチャット欄に挿入されるテキストを保持。 */
    record Insertion(String insertion, List<RichNode> children)            implements RichNode {}
    /** x/y は描画位置オフセット。z は深度オフセット（hat オーバーレイに 0.01f を使用）。 */
    record Offset(float x, float y, float z, List<RichNode> children)     implements RichNode {}
    record Scaled(float scaleX, float scaleY, List<RichNode> children)    implements RichNode {}
    record Rotated(float angle, List<RichNode> children)                   implements RichNode {}

    static RichNode empty() { return new Text("", Style.EMPTY, List.of()); }

    // ── toComponent ──────────────────────────────────────────────────────────

    default MutableComponent toComponent() {
        if (this instanceof Text t) {
            MutableComponent c = Component.literal(t.literal());
            if (!t.style().isEmpty()) c = c.withStyle(t.style());
            for (RichNode child : t.children()) c.append(child.toComponent());
            return c;
        }
        if (this instanceof Glowing g) {
            MutableComponent c = Component.empty();
            for (RichNode child : g.children()) c.append(child.toComponent());
            return c;
        }
        if (this instanceof Image img) return imageComponent(img).copy();
        if (this instanceof Hover hm) {
            MutableComponent c = Component.empty();
            for (RichNode child : hm.children()) c.append(child.toComponent());
            HoverEvent ev = hm.hoverEvent();
            if (ev.getAction() == HoverEvent.Action.SHOW_TEXT) {
                Component val = ev.getValue(HoverEvent.Action.SHOW_TEXT);
                if (val != null && val.getContents() instanceof HoverRichContents hrc) {
                    ev = new HoverEvent(HoverEvent.Action.SHOW_TEXT, hrc.node().toComponent());
                }
            }
            final HoverEvent finalEv = ev;
            return c.withStyle(s -> s.withHoverEvent(finalEv));
        }
        if (this instanceof Click cl) {
            MutableComponent c = Component.empty();
            for (RichNode child : cl.children()) c.append(child.toComponent());
            return c.withStyle(s -> s.withClickEvent(cl.clickEvent()));
        }
        if (this instanceof Insertion ins) {
            MutableComponent c = Component.empty();
            for (RichNode child : ins.children()) c.append(child.toComponent());
            return c.withStyle(s -> s.withInsertion(ins.insertion()));
        }
        // transform wrappers: render contents without transform
        if (this instanceof Offset o) {
            MutableComponent c = Component.empty();
            for (RichNode child : o.children()) c.append(child.toComponent());
            return c;
        }
        if (this instanceof Scaled s) {
            MutableComponent c = Component.empty();
            for (RichNode child : s.children()) c.append(child.toComponent());
            return c;
        }
        if (this instanceof Rotated r) {
            MutableComponent c = Component.empty();
            for (RichNode child : r.children()) c.append(child.toComponent());
            return c;
        }
        return Component.empty();
    }

    private static MutableComponent imageComponent(Image img) {
        int cp = ImageGlyphPool.getOrAllocate(
                img.imageSpec(), img.crop(), img.displayW(), img.displayH(), img.advanceOverride());
        return Component.literal(new String(Character.toChars(cp)))
                .withStyle(Style.EMPTY.withFont(ImageGlyphPool.IMAGE_FONT));
    }

    // ── toSequence ─────────────────────────────────────────────────────────

    static FormattedCharSequence toSequence(Font font, RichNode root) {
        return toSequence(font, root, Style.EMPTY);
    }

    public static FormattedCharSequence toSequence(Font font, RichNode root, Style inherited) {
        List<FormattedCharSequence> parts = new ArrayList<>();
        collectSegments(font, root, LightMode.BYPASS, inherited, parts);
        if (parts.isEmpty()) return FormattedCharSequence.EMPTY;
        if (parts.size() == 1) return parts.get(0);
        return new ConcatSequence(parts);
    }

    private static void collectSegments(Font font, RichNode node,
                                        LightMode lightMode, Style inherited,
                                        List<FormattedCharSequence> out) {
        if (node instanceof Text t) {
            Style combined = t.style().applyTo(inherited);
            if (!t.literal().isEmpty())
                addLeaf(font, Component.literal(t.literal()).withStyle(combined), lightMode, out);
            for (RichNode child : t.children())
                collectSegments(font, child, lightMode, combined, out);
        } else if (node instanceof Glowing g) {
            for (RichNode child : g.children())
                collectSegments(font, child, g.lightMode(), inherited, out);
        } else if (node instanceof Image img) {
            addLeaf(font, withInherited(imageComponent(img), inherited), lightMode, out);
        } else if (node instanceof Hover hm) {
            Style withHover = inherited.withHoverEvent(hm.hoverEvent());
            for (RichNode child : hm.children())
                collectSegments(font, child, lightMode, withHover, out);
        } else if (node instanceof Click cl) {
            Style withClick = inherited.withClickEvent(cl.clickEvent());
            for (RichNode child : cl.children())
                collectSegments(font, child, lightMode, withClick, out);
        } else if (node instanceof Insertion ins) {
            Style withIns = inherited.withInsertion(ins.insertion());
            for (RichNode child : ins.children())
                collectSegments(font, child, lightMode, withIns, out);
        } else if (node instanceof Offset o) {
            wrapAffine(font, o.children(), lightMode, inherited, out,
                    new Matrix4f().translate(o.x(), o.y(), o.z()), true);
        } else if (node instanceof Scaled s) {
            wrapAffine(font, s.children(), lightMode, inherited, out, seq -> {
                float ox = s.scaleX() < 0 ? font.width(seq) * (-s.scaleX()) : 0f;
                float baseline = font.lineHeight - 2f;
                float oy = s.scaleY() < 0 ? font.lineHeight * (-s.scaleY()) : baseline * (1f - s.scaleY());
                return new Matrix4f().translate(ox, oy, 0f).scale(s.scaleX(), s.scaleY(), 1f);
            }, false);
        } else if (node instanceof Rotated r) {
            wrapAffine(font, r.children(), lightMode, inherited, out, seq -> {
                float cx = font.width(seq) / 2f;
                float cy = font.lineHeight / 2f;
                return new Matrix4f()
                        .translate(cx, cy, 0f)
                        .rotateZ((float) Math.toRadians(r.angle()))
                        .translate(-cx, -cy, 0f);
            }, true);
        }
    }

    private static void wrapAffine(Font font, List<RichNode> children,
                                    LightMode lightMode, Style inherited,
                                    List<FormattedCharSequence> out, Matrix4f matrix,
                                    boolean useInnerWidth) {
        List<FormattedCharSequence> inner = new ArrayList<>();
        for (RichNode child : children)
            collectSegments(font, child, lightMode, inherited, inner);
        if (!inner.isEmpty()) {
            FormattedCharSequence seq = inner.size() == 1 ? inner.get(0) : new ConcatSequence(inner);
            out.add(new AffineSequence(seq, matrix, useInnerWidth));
        }
    }

    private static void wrapAffine(Font font, List<RichNode> children,
                                    LightMode lightMode, Style inherited,
                                    List<FormattedCharSequence> out,
                                    java.util.function.Function<FormattedCharSequence, Matrix4f> matrixFn,
                                    boolean useInnerWidth) {
        List<FormattedCharSequence> inner = new ArrayList<>();
        for (RichNode child : children)
            collectSegments(font, child, lightMode, inherited, inner);
        if (!inner.isEmpty()) {
            FormattedCharSequence seq = inner.size() == 1 ? inner.get(0) : new ConcatSequence(inner);
            out.add(new AffineSequence(seq, matrixFn.apply(seq), useInnerWidth));
        }
    }

    // ── toWordSegments ────────────────────────────────────────────────────────

    /**
     * RichNode ツリーをスペース区切りの「単語」FCS リストに分解する。
     * scale/glow 等のトランスフォームは各単語に個別に適用されるため、
     * packIntoLines でそれぞれの表示幅を正確に計測してワードラップできる。
     * maxWidth を超えるスペースなし Latin 文字列は1文字単位に分解して折り返しを可能にする。
     */
    public static List<FormattedCharSequence> toWordSegments(Font font, RichNode root, Style inherited, int maxWidth) {
        List<FormattedCharSequence> words = new ArrayList<>();
        collectWordSegments(font, root, LightMode.BYPASS, inherited, words, maxWidth);
        return words;
    }

    private static void collectWordSegments(Font font, RichNode node, LightMode lightMode,
                                             Style inherited, List<FormattedCharSequence> out, int maxWidth) {
        if (node instanceof Text t) {
            Style combined = t.style().applyTo(inherited);
            splitAtSpaces(font, t.literal(), combined, lightMode, out, maxWidth);
            for (RichNode child : t.children())
                collectWordSegments(font, child, lightMode, combined, out, maxWidth);
        } else if (node instanceof Glowing g) {
            for (RichNode child : g.children())
                collectWordSegments(font, child, g.lightMode(), inherited, out, maxWidth);
        } else if (node instanceof Image img) {
            addLeaf(font, withInherited(imageComponent(img), inherited), lightMode, out);
        } else if (node instanceof Hover h) {
            Style s = inherited.withHoverEvent(h.hoverEvent());
            for (RichNode child : h.children()) collectWordSegments(font, child, lightMode, s, out, maxWidth);
        } else if (node instanceof Click cl) {
            Style s = inherited.withClickEvent(cl.clickEvent());
            for (RichNode child : cl.children()) collectWordSegments(font, child, lightMode, s, out, maxWidth);
        } else if (node instanceof Insertion ins) {
            Style s = inherited.withInsertion(ins.insertion());
            for (RichNode child : ins.children()) collectWordSegments(font, child, lightMode, s, out, maxWidth);
        } else if (node instanceof Offset o) {
            Matrix4f mat = new Matrix4f().translate(o.x(), o.y(), o.z());
            wrapWordsAffine(font, o.children(), lightMode, inherited, out, seq -> mat, true);
        } else if (node instanceof Scaled s) {
            wrapWordsAffine(font, s.children(), lightMode, inherited, out, seq -> {
                float ox = s.scaleX() < 0 ? font.width(seq) * (-s.scaleX()) : 0f;
                float baseline = font.lineHeight - 2f;
                float oy = s.scaleY() < 0 ? font.lineHeight * (-s.scaleY()) : baseline * (1f - s.scaleY());
                return new Matrix4f().translate(ox, oy, 0f).scale(s.scaleX(), s.scaleY(), 1f);
            }, false);
        } else if (node instanceof Rotated r) {
            wrapWordsAffine(font, r.children(), lightMode, inherited, out, seq -> {
                float cx = font.width(seq) / 2f;
                float cy = font.lineHeight / 2f;
                return new Matrix4f().translate(cx, cy, 0f)
                        .rotateZ((float) Math.toRadians(r.angle()))
                        .translate(-cx, -cy, 0f);
            }, true);
        }
    }

    /**
     * 各単語を個別にトランスフォームでラップして追加する。
     * 変換内部ではスケールが不明なため maxWidth を使った文字単位分解は行わない。
     */
    private static void wrapWordsAffine(Font font, List<RichNode> children, LightMode lightMode,
                                         Style inherited, List<FormattedCharSequence> out,
                                         Function<FormattedCharSequence, Matrix4f> matrixFn,
                                         boolean useInnerWidth) {
        List<FormattedCharSequence> inner = new ArrayList<>();
        for (RichNode child : children)
            collectWordSegments(font, child, lightMode, inherited, inner, Integer.MAX_VALUE);
        for (FormattedCharSequence word : inner)
            out.add(new AffineSequence(word, matrixFn.apply(word), useInnerWidth));
    }

    /**
     * テキストリテラルを折り返し単位に分割して追加する。
     * - ラテン系: スペース区切り。maxWidth を超える連続列は1文字ずつに分解する。
     * - CJK・かな・ハングル: 1文字ずつ独立したユニット。後続スペースがあれば同ユニットに含める。
     * 先頭スペースはスキップ（折り返し後の行頭スペース除去と同様の扱い）。
     */
    private static void splitAtSpaces(Font font, String literal, Style style,
                                       LightMode lightMode, List<FormattedCharSequence> out, int maxWidth) {
        if (literal.isEmpty()) return;
        int i = 0, len = literal.length();
        while (i < len && literal.charAt(i) == ' ') i++;

        int wordStart = i;
        while (i < len) {
            int cp = literal.codePointAt(i);
            int cpLen = Character.charCount(cp);
            if (cp == ' ') {
                // 現在の単語 + 後続スペースをひとつのユニットとして追加
                int end = i;
                while (end < len && literal.charAt(end) == ' ') end++;
                emitLatinRun(font, literal, wordStart, end, style, lightMode, out, maxWidth);
                wordStart = end;
                i = end;
            } else if (isCjkCodePoint(cp)) {
                // CJK文字の前に溜まったラテン系単語を先に追加
                if (i > wordStart) emitLatinRun(font, literal, wordStart, i, style, lightMode, out, maxWidth);
                // CJK文字1文字 + 後続スペースをひとつのユニットとして追加
                int end = i + cpLen;
                while (end < len && literal.charAt(end) == ' ') end++;
                addLeaf(font, Component.literal(literal.substring(i, end)).withStyle(style), lightMode, out);
                wordStart = end;
                i = end;
            } else {
                i += cpLen;
            }
        }
        if (wordStart < len) emitLatinRun(font, literal, wordStart, len, style, lightMode, out, maxWidth);
    }

    /**
     * Latin テキストのひと塊を追加する。
     * maxWidth 以下なら1ユニット、超過する場合は1文字ずつに分解して packIntoLines に折り返しを委ねる。
     */
    private static void emitLatinRun(Font font, String literal, int from, int to,
                                      Style style, LightMode lightMode,
                                      List<FormattedCharSequence> out, int maxWidth) {
        String run = literal.substring(from, to);
        if (maxWidth == Integer.MAX_VALUE
                || font.width(Component.literal(run).withStyle(style)) <= maxWidth) {
            addLeaf(font, Component.literal(run).withStyle(style), lightMode, out);
        } else {
            for (int k = from; k < to; ) {
                int cp = literal.codePointAt(k);
                int cpLen = Character.charCount(cp);
                addLeaf(font, Component.literal(literal.substring(k, k + cpLen)).withStyle(style), lightMode, out);
                k += cpLen;
            }
        }
    }

    private static boolean isCjkCodePoint(int cp) {
        return Character.isIdeographic(cp)                    // CJK統合漢字・拡張各種・互換漢字
            || (cp >= 0x3040 && cp <= 0x309F)                // ひらがな
            || (cp >= 0x30A0 && cp <= 0x30FF)                // カタカナ
            || (cp >= 0x31F0 && cp <= 0x31FF)                // カタカナ拡張
            || (cp >= 0x3000 && cp <= 0x303F)                // CJK記号・句読点
            || (cp >= 0xAC00 && cp <= 0xD7AF)                // ハングル音節
            || (cp >= 0x1100 && cp <= 0x11FF);               // ハングル字母
    }

    private static Component withInherited(Component c, Style inherited) {
        if (inherited.isEmpty()) return c;
        return c.copy().withStyle(c.getStyle().applyTo(inherited));
    }

    private static void addLeaf(Font font, Component c, LightMode lightMode,
                                 List<FormattedCharSequence> out) {
        List<FormattedCharSequence> lines = font.split(c, Integer.MAX_VALUE / 2);
        FormattedCharSequence fcs = lines.isEmpty() ? FormattedCharSequence.EMPTY : lines.get(0);
        if (!(lightMode instanceof LightMode.Bypass)) fcs = new LightSequence(fcs, lightMode);
        out.add(fcs);
    }
}
