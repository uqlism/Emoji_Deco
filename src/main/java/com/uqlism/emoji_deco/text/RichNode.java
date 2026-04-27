package com.uqlism.emoji_deco.text;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/**
 * Typed intermediate representation of parsed rich text.
 *
 * Nodes:
 *   Text(literal, style, children) — text fragment with optional style delta and sub-nodes
 *   Sized(scale, children)         — size multiplier wrapper
 *   Glowing(children)              — glow effect wrapper
 *   Sprite(atlas, sprite)          — sprite glyph via custom font
 *   Head(username)                 — player-head glyph pair via HEAD_FONT
 *
 * Conversion:
 *   toComponent()              — for chat / tooltip / entity names / books
 *   toSequence(font, node)   — for signs / graffiti (honours Sized and Glowing)
 */
public sealed interface RichNode permits RichNode.Text, RichNode.Sized, RichNode.Glowing,
                                         RichNode.Sprite, RichNode.Head {

    record Text(String literal, Style style, List<RichNode> children) implements RichNode {}
    record Sized(float scale, List<RichNode> children)                 implements RichNode {}
    record Glowing(List<RichNode> children)                            implements RichNode {}
    record Sprite(String atlas, String sprite)                         implements RichNode {}
    record Head(String username)                                       implements RichNode {}

    static RichNode empty() { return new Text("", Style.EMPTY, List.of()); }

    // ── toComponent ──────────────────────────────────────────────────────────

    /**
     * Converts to a Minecraft Component for standard rendering contexts.
     * Sized and Glowing effects are not preserved (standard renderer has no support).
     */
    default MutableComponent toComponent() {
        if (this instanceof Text t) {
            MutableComponent c = Component.literal(t.literal());
            if (!t.style().isEmpty()) c = c.withStyle(t.style());
            for (RichNode child : t.children()) c.append(child.toComponent());
            return c;
        }
        if (this instanceof Sized s) {
            MutableComponent c = Component.empty();
            for (RichNode child : s.children()) c.append(child.toComponent());
            return c;
        }
        if (this instanceof Glowing g) {
            MutableComponent c = Component.empty();
            for (RichNode child : g.children()) c.append(child.toComponent());
            return c;
        }
        if (this instanceof Sprite s) return SpriteRegistry.createComponent(s.atlas(), s.sprite()).copy();
        if (this instanceof Head h)   return SpriteRegistry.createHeadComponent(h.username()).copy();
        return Component.empty();
    }

    // ── toSequence ─────────────────────────────────────────────────────────

    /**
     * Converts to a FormattedCharSequence for scaled rendering (signs, graffiti).
     * Sized and Glowing nodes are fully honoured.
     */
    static FormattedCharSequence toSequence(Font font, RichNode root) {
        List<FormattedCharSequence> parts = new ArrayList<>();
        collectSegments(font, root, 1.0f, false, Style.EMPTY, parts);
        if (parts.isEmpty()) return FormattedCharSequence.EMPTY;
        if (parts.size() == 1) return parts.get(0);
        return new ConcatSequence(parts);
    }

    private static void collectSegments(Font font, RichNode node,
                                        float scale, boolean glow, Style inherited,
                                        List<FormattedCharSequence> out) {
        if (node instanceof Text t) {
            Style combined = t.style().applyTo(inherited);
            if (!t.literal().isEmpty())
                addLeaf(font, Component.literal(t.literal()).withStyle(combined), scale, glow, out);
            for (RichNode child : t.children())
                collectSegments(font, child, scale, glow, combined, out);
        } else if (node instanceof Sized s) {
            for (RichNode child : s.children())
                collectSegments(font, child, scale * s.scale(), glow, inherited, out);
        } else if (node instanceof Glowing g) {
            for (RichNode child : g.children())
                collectSegments(font, child, scale, true, inherited, out);
        } else if (node instanceof Sprite s) {
            addLeaf(font, SpriteRegistry.createComponent(s.atlas(), s.sprite()), scale, glow, out);
        } else if (node instanceof Head h) {
            addLeaf(font, SpriteRegistry.createHeadComponent(h.username()), scale, glow, out);
        }
    }

    private static void addLeaf(Font font, Component c, float scale, boolean glow,
                                 List<FormattedCharSequence> out) {
        List<FormattedCharSequence> lines = font.split(c, Integer.MAX_VALUE / 2);
        FormattedCharSequence fcs = lines.isEmpty() ? FormattedCharSequence.EMPTY : lines.get(0);
        if (glow) fcs = new LightSequence(fcs, LightMode.GLOW);
        if (scale != 1.0f) fcs = new ScaledSequence(fcs, scale);
        out.add(fcs);
    }
}
