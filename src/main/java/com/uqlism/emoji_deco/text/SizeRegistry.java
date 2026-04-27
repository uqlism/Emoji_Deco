package com.uqlism.emoji_deco.text;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.LiteralContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

public class SizeRegistry {

    public static final String SIZE_KEY = "emoji_deco:size";
    public static final String GLOW_KEY = "emoji_deco:glow";

    /**
     * Converts a parsed {@link Component} into a {@link FormattedCharSequence} that
     * carries scaling and glow metadata for sign/graffiti rendering.
     */
    public static FormattedCharSequence buildScaledLine(Font font, Component parsed) {
        List<Component> siblings = parsed.getSiblings();

        List<Leaf> leaves = new ArrayList<>();
        if (siblings.isEmpty()) {
            leaves.addAll(extractLeaves(parsed, 1.0f, false));
        } else {
            for (Component sib : siblings) {
                leaves.addAll(extractLeaves(sib, 1.0f, false));
            }
        }

        boolean anyEffect = leaves.stream().anyMatch(Leaf::hasEffect);
        if (!anyEffect) return split(font, parsed);

        List<CompositeScaledSequence.Segment> segments = new ArrayList<>();
        for (Leaf leaf : leaves) {
            FormattedCharSequence fcs = split(font, leaf.component());
            if (leaf.glow()) fcs = new GlowSequence(fcs);
            segments.add(new CompositeScaledSequence.Segment(fcs, leaf.scale()));
        }
        if (segments.size() == 1) {
            var seg = segments.get(0);
            return new ScaledSequence(seg.chars(), seg.scale());
        }
        return new CompositeScaledSequence(segments);
    }

    /**
     * Recursively decomposes {@code c} into a list of leaf segments, each carrying
     * the accumulated scale and glow for that portion of the text.
     *
     * <p>When a node's children have uniform effects the node is rebuilt as one
     * component (preserving the parent's style cascade). When effects are mixed
     * (e.g. {@code #aqua[#glow[hello] world]}) the node is split into per-child
     * components, each wrapped with the parent's style so colors etc. are kept.
     */
    private static List<Leaf> extractLeaves(Component c, float scale, boolean glow) {
        ComponentContents contents = c.getContents();
        Style style = c.getStyle();
        List<Component> siblings = c.getSiblings();

        // Decorator translates: strip the translate node and adjust accumulated state
        if (contents instanceof TranslatableContents tc) {
            if (SIZE_KEY.equals(tc.getKey())) {
                return extractLeaves(argComponent(tc, 1), scale * scaleArg(tc), glow);
            }
            if (GLOW_KEY.equals(tc.getKey())) {
                return extractLeaves(argComponent(tc, 0), scale, true);
            }
        }

        // Leaf node (no siblings): return as-is
        if (siblings.isEmpty()) return List.of(new Leaf(scale, glow, c));

        // Gather child leaves
        List<Leaf> childLeaves = new ArrayList<>();
        // Own non-empty text counts as a virtual first child
        if (contents instanceof LiteralContents lc && !lc.text().isEmpty()) {
            childLeaves.add(new Leaf(scale, glow, MutableComponent.create(contents).withStyle(style)));
        }
        for (Component sib : siblings) {
            childLeaves.addAll(extractLeaves(sib, scale, glow));
        }
        if (childLeaves.isEmpty()) return List.of(new Leaf(scale, glow, c));

        // Check uniformity
        float s0 = childLeaves.get(0).scale();
        boolean g0 = childLeaves.get(0).glow();
        boolean uniform = childLeaves.stream().allMatch(l -> l.scale() == s0 && l.glow() == g0);

        if (uniform) {
            // All children have the same effects: rebuild as one component so the
            // parent style cascades naturally to every descendant.
            boolean ownTextAdded = contents instanceof LiteralContents lc && !lc.text().isEmpty();
            MutableComponent rebuilt = MutableComponent.create(
                    ownTextAdded ? contents : ComponentContents.EMPTY).withStyle(style);
            for (int i = ownTextAdded ? 1 : 0; i < childLeaves.size(); i++) {
                rebuilt.append(childLeaves.get(i).component());
            }
            return List.of(new Leaf(s0, g0, rebuilt));
        } else {
            // Mixed effects: split into per-child components, each inheriting parent style.
            List<Leaf> result = new ArrayList<>();
            for (Leaf l : childLeaves) {
                Component comp = style.isEmpty() ? l.component()
                        : MutableComponent.create(ComponentContents.EMPTY).withStyle(style).append(l.component());
                result.add(new Leaf(l.scale(), l.glow(), comp));
            }
            return result;
        }
    }

    private static float scaleArg(TranslatableContents tc) {
        Object[] args = tc.getArgs();
        if (args.length < 1) return 1.0f;
        try { return Float.parseFloat(args[0].toString()); }
        catch (NumberFormatException e) { return 1.0f; }
    }

    private static Component argComponent(TranslatableContents tc, int index) {
        Object[] args = tc.getArgs();
        if (args.length <= index) return Component.empty();
        return args[index] instanceof Component comp ? comp : Component.literal(args[index].toString());
    }

    private static FormattedCharSequence split(Font font, Component c) {
        List<FormattedCharSequence> lines = font.split(c, Integer.MAX_VALUE / 2);
        return lines.isEmpty() ? FormattedCharSequence.EMPTY : lines.get(0);
    }

    private record Leaf(float scale, boolean glow, Component component) {
        boolean hasEffect() { return scale != 1.0f || glow; }
    }
}
