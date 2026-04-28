package com.uqlism.emoji_deco.text;

import net.minecraft.client.gui.Font;
import com.uqlism.emoji_deco.render.registry.PlayerHeadRegistry;
import com.uqlism.emoji_deco.render.registry.SpriteRegistry;
import com.uqlism.emoji_deco.render.registry.TextureRegistry;
import net.minecraft.resources.ResourceLocation;
import com.uqlism.emoji_deco.render.sequence.ConcatSequence;
import com.uqlism.emoji_deco.render.sequence.LightMode;
import com.uqlism.emoji_deco.render.sequence.LightSequence;
import com.uqlism.emoji_deco.render.sequence.ScaledSequence;
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
                                         RichNode.Sprite, RichNode.Head, RichNode.Texture,
                                         RichNode.Offset {

    record Text(String literal, Style style, List<RichNode> children) implements RichNode {}
    record Sized(float scale, List<RichNode> children)                 implements RichNode {}
    record Glowing(LightMode lightMode, List<RichNode> children)       implements RichNode {}
    record Sprite(String atlas, String sprite, int width, int height)  implements RichNode {}
    record Head(String username)                                       implements RichNode {}
    record Texture(ResourceLocation texture, int width, int height)    implements RichNode {}
    record Offset(float x, float y, List<RichNode> children)          implements RichNode {}

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
        if (this instanceof Sprite s)   return SpriteRegistry.createComponent(s.atlas(), s.sprite(), s.width(), s.height()).copy();
        if (this instanceof Head h)     return PlayerHeadRegistry.createComponent(h.username()).copy();
        if (this instanceof Texture t)  return TextureRegistry.createComponent(t.texture(), t.width(), t.height()).copy();
        if (this instanceof Offset o) {
            // Standard renderer has no offset support; render contents as-is.
            MutableComponent c = Component.empty();
            for (RichNode child : o.children()) c.append(child.toComponent());
            return c;
        }
        return Component.empty();
    }

    // ── toSequence ─────────────────────────────────────────────────────────

    /**
     * Converts to a FormattedCharSequence for scaled rendering (signs, graffiti).
     * Sized and Glowing nodes are fully honoured.
     */
    static FormattedCharSequence toSequence(Font font, RichNode root) {
        List<FormattedCharSequence> parts = new ArrayList<>();
        collectSegments(font, root, 1.0f, LightMode.BYPASS, Style.EMPTY, parts);
        if (parts.isEmpty()) return FormattedCharSequence.EMPTY;
        if (parts.size() == 1) return parts.get(0);
        return new ConcatSequence(parts);
    }

    private static void collectSegments(Font font, RichNode node,
                                        float scale, LightMode lightMode, Style inherited,
                                        List<FormattedCharSequence> out) {
        if (node instanceof Text t) {
            Style combined = t.style().applyTo(inherited);
            if (!t.literal().isEmpty())
                addLeaf(font, Component.literal(t.literal()).withStyle(combined), scale, lightMode, out);
            for (RichNode child : t.children())
                collectSegments(font, child, scale, lightMode, combined, out);
        } else if (node instanceof Sized s) {
            for (RichNode child : s.children())
                collectSegments(font, child, scale * s.scale(), lightMode, inherited, out);
        } else if (node instanceof Glowing g) {
            for (RichNode child : g.children())
                collectSegments(font, child, scale, g.lightMode(), inherited, out);
        } else if (node instanceof Sprite s) {
            addLeaf(font, withInherited(SpriteRegistry.createComponent(s.atlas(), s.sprite(), s.width(), s.height()), inherited), scale, lightMode, out);
        } else if (node instanceof Head h) {
            addLeaf(font, withInherited(PlayerHeadRegistry.createComponent(h.username()), inherited), scale, lightMode, out);
        } else if (node instanceof Texture t) {
            addLeaf(font, withInherited(TextureRegistry.createComponent(t.texture(), t.width(), t.height()), inherited), scale, lightMode, out);
        } else if (node instanceof Offset o) {
            List<FormattedCharSequence> inner = new ArrayList<>();
            for (RichNode child : o.children())
                collectSegments(font, child, scale, lightMode, inherited, inner);
            if (!inner.isEmpty()) {
                FormattedCharSequence seq = inner.size() == 1 ? inner.get(0) : new ConcatSequence(inner);
                out.add(new com.uqlism.emoji_deco.render.sequence.OffsetSequence(seq, o.x() * scale, o.y() * scale));
            }
        }
    }

    /** Merges inherited style into a leaf component, preserving the leaf's own explicit fields. */
    private static Component withInherited(Component c, Style inherited) {
        if (inherited.isEmpty()) return c;
        // c.getStyle() (e.g. {font:SPRITE_FONT}) overrides; unset fields fall back to inherited (e.g. color)
        return c.copy().withStyle(c.getStyle().applyTo(inherited));
    }

    private static void addLeaf(Font font, Component c, float scale, LightMode lightMode,
                                 List<FormattedCharSequence> out) {
        List<FormattedCharSequence> lines = font.split(c, Integer.MAX_VALUE / 2);
        FormattedCharSequence fcs = lines.isEmpty() ? FormattedCharSequence.EMPTY : lines.get(0);
        if (!(lightMode instanceof LightMode.Bypass)) fcs = new LightSequence(fcs, lightMode);
        if (scale != 1.0f) fcs = new ScaledSequence(fcs, scale);
        out.add(fcs);
    }
}
