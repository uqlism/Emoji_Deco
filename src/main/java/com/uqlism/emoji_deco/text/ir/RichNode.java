package com.uqlism.emoji_deco.text.ir;

import net.minecraft.client.gui.Font;
import com.uqlism.emoji_deco.render.image.ImageGlyphPool;
import com.uqlism.emoji_deco.render.image.ImageSpec;

import com.uqlism.emoji_deco.render.sequence.AffineSequence;
import com.uqlism.emoji_deco.render.sequence.ConcatSequence;
import com.uqlism.emoji_deco.render.sequence.LightMode;
import com.uqlism.emoji_deco.render.sequence.LightSequence;
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
    /** ホバーテキスト wrapper。toSequence では children のみ描画（hover は Component 専用）。 */
    record Hover(RichNode hoverText, List<RichNode> children)              implements RichNode {}
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
        if (this instanceof Hover h) {
            MutableComponent hoverComp = MutableComponent.create(new HoverRichContents(h.hoverText()));
            MutableComponent c = Component.empty();
            for (RichNode child : h.children()) c.append(child.toComponent());
            return c.withStyle(s -> s.withHoverEvent(
                    new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverComp)));
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
        List<FormattedCharSequence> parts = new ArrayList<>();
        collectSegments(font, root, LightMode.BYPASS, Style.EMPTY, parts);
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
        } else if (node instanceof Hover h) {
            for (RichNode child : h.children())
                collectSegments(font, child, lightMode, inherited, out);
        } else if (node instanceof Offset o) {
            wrapAffine(font, o.children(), lightMode, inherited, out,
                    new Matrix4f().translate(o.x(), o.y(), o.z()), true);
        } else if (node instanceof Scaled s) {
            wrapAffine(font, s.children(), lightMode, inherited, out, seq -> {
                float ox = s.scaleX() < 0 ? font.width(seq) * (-s.scaleX()) : 0f;
                float oy = s.scaleY() < 0 ? font.lineHeight * (-s.scaleY()) : 0f;
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
