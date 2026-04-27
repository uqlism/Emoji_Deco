package com.uqlism.emoji_deco.text;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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

        if (siblings.isEmpty()) {
            var h = hoistDecorators(parsed);
            if (h.hasEffect()) {
                FormattedCharSequence fcs = split(font, h.component());
                if (h.glow()) fcs = new GlowSequence(fcs);
                return new ScaledSequence(fcs, h.scale());
            }
            return split(font, parsed);
        }

        boolean anyEffect = false;
        List<CompositeScaledSequence.Segment> segments = new ArrayList<>();
        for (Component sib : siblings) {
            var h = hoistDecorators(sib);
            if (h.hasEffect()) anyEffect = true;
            FormattedCharSequence fcs = split(font, h.component());
            if (h.glow()) fcs = new GlowSequence(fcs);
            segments.add(new CompositeScaledSequence.Segment(fcs, h.scale()));
        }

        if (!anyEffect) return split(font, parsed);
        if (segments.size() == 1) {
            var seg = segments.get(0);
            return new ScaledSequence(seg.chars(), seg.scale());
        }
        return new CompositeScaledSequence(segments);
    }

    /**
     * Recursively walks {@code c}, extracting {@code emoji_deco:size} (scale multiplier)
     * and {@code emoji_deco:glow} (max-brightness flag) from any depth in the tree.
     * The returned component has those translate nodes replaced by their inner content,
     * with outer styles preserved.
     */
    private static HoistResult hoistDecorators(Component c) {
        if (c.getContents() instanceof TranslatableContents tc) {
            if (SIZE_KEY.equals(tc.getKey())) {
                HoistResult inner = hoistDecorators(contentFromTc(tc, 1));
                return new HoistResult(scaleFromTc(tc) * inner.scale(), inner.glow(), inner.component());
            }
            if (GLOW_KEY.equals(tc.getKey())) {
                HoistResult inner = hoistDecorators(contentFromTc(tc, 0));
                return new HoistResult(inner.scale(), true, inner.component());
            }
        }

        List<Component> siblings = c.getSiblings();
        if (siblings.isEmpty()) return new HoistResult(1.0f, false, c);

        float scale = 1.0f;
        boolean glow = false;
        MutableComponent rebuilt = MutableComponent.create(c.getContents()).withStyle(c.getStyle());
        for (Component sib : siblings) {
            HoistResult h = hoistDecorators(sib);
            if (h.scale() != 1.0f && scale == 1.0f) scale = h.scale();
            if (h.glow()) glow = true;
            rebuilt.append(h.component());
        }
        return new HoistResult(scale, glow, rebuilt);
    }

    private static float scaleFromTc(TranslatableContents tc) {
        Object[] args = tc.getArgs();
        if (args.length < 1) return 1.0f;
        try { return Float.parseFloat(args[0].toString()); }
        catch (NumberFormatException e) { return 1.0f; }
    }

    private static Component contentFromTc(TranslatableContents tc, int argIndex) {
        Object[] args = tc.getArgs();
        if (args.length <= argIndex) return Component.empty();
        return args[argIndex] instanceof Component c ? c : Component.literal(args[argIndex].toString());
    }

    private static FormattedCharSequence split(Font font, Component c) {
        List<FormattedCharSequence> lines = font.split(c, Integer.MAX_VALUE / 2);
        return lines.isEmpty() ? FormattedCharSequence.EMPTY : lines.get(0);
    }

    private record HoistResult(float scale, boolean glow, Component component) {
        boolean hasEffect() { return scale != 1.0f || glow; }
    }
}
