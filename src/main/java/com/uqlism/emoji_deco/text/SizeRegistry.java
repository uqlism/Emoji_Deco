package com.uqlism.emoji_deco.text;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

public class SizeRegistry {

    public static final String KEY = "emoji_deco:size";

    /**
     * Converts a parsed {@link Component} into a {@link FormattedCharSequence} that
     * carries scaling metadata. Handles:
     * <ul>
     *   <li>Whole-line single scale: {@link ScaledSequence}</li>
     *   <li>Partial line or nested decorators: {@link CompositeScaledSequence}</li>
     *   <li>No scaling: plain split result</li>
     * </ul>
     */
    public static FormattedCharSequence buildScaledLine(Font font, Component parsed) {
        List<Component> siblings = parsed.getSiblings();

        if (siblings.isEmpty()) {
            // The component itself may be (or deeply contain) a size translate
            var h = hoistSizeScale(parsed);
            if (h.scale() != 1.0f) {
                List<FormattedCharSequence> lines = font.split(h.component(), Integer.MAX_VALUE / 2);
                return new ScaledSequence(lines.isEmpty() ? FormattedCharSequence.EMPTY : lines.get(0), h.scale());
            }
            List<FormattedCharSequence> lines = font.split(parsed, Integer.MAX_VALUE / 2);
            return lines.isEmpty() ? FormattedCharSequence.EMPTY : lines.get(0);
        }

        // Hoist each sibling; build segments if any has a scale
        boolean anyScaled = false;
        List<CompositeScaledSequence.Segment> segments = new ArrayList<>();
        for (Component sib : siblings) {
            var h = hoistSizeScale(sib);
            if (h.scale() != 1.0f) anyScaled = true;
            List<FormattedCharSequence> lines = font.split(h.component(), Integer.MAX_VALUE / 2);
            segments.add(new CompositeScaledSequence.Segment(
                    lines.isEmpty() ? FormattedCharSequence.EMPTY : lines.get(0), h.scale()));
        }

        if (!anyScaled) {
            List<FormattedCharSequence> lines = font.split(parsed, Integer.MAX_VALUE / 2);
            return lines.isEmpty() ? FormattedCharSequence.EMPTY : lines.get(0);
        }
        if (segments.size() == 1) {
            var seg = segments.get(0);
            return new ScaledSequence(seg.chars(), seg.scale());
        }
        return new CompositeScaledSequence(segments);
    }

    /**
     * Recursively walks {@code c} looking for an {@code emoji_deco:size} translate.
     * When found, returns its scale and replaces the translate node with its inner
     * content, preserving all outer styles/structure.
     * If no size translate exists, returns (1.0f, c) unchanged.
     */
    private static HoistResult hoistSizeScale(Component c) {
        // This node IS the size translate
        if (c.getContents() instanceof TranslatableContents tc && KEY.equals(tc.getKey())) {
            return new HoistResult(scaleFromTc(tc), contentFromTc(tc));
        }

        // Recurse into siblings, rebuilding the component with any found scale
        List<Component> siblings = c.getSiblings();
        if (siblings.isEmpty()) return new HoistResult(1.0f, c);

        float foundScale = 1.0f;
        MutableComponent rebuilt = MutableComponent.create(c.getContents()).withStyle(c.getStyle());
        for (Component sib : siblings) {
            HoistResult h = hoistSizeScale(sib);
            if (h.scale() != 1.0f && foundScale == 1.0f) foundScale = h.scale();
            rebuilt.append(h.component());
        }
        return new HoistResult(foundScale, rebuilt);
    }

    private static float scaleFromTc(TranslatableContents tc) {
        Object[] args = tc.getArgs();
        if (args.length < 1) return 1.0f;
        try { return Float.parseFloat(args[0].toString()); }
        catch (NumberFormatException e) { return 1.0f; }
    }

    private static Component contentFromTc(TranslatableContents tc) {
        Object[] args = tc.getArgs();
        if (args.length < 2) return Component.empty();
        return args[1] instanceof Component comp ? comp : Component.literal(args[1].toString());
    }

    private record HoistResult(float scale, Component component) {}
}
