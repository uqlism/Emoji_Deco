package com.uqlism.emoji_deco.text;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.contents.LiteralContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

public class SizeRegistry {

    public static final String KEY = "emoji_deco:size";

    /**
     * Returns the scale factor if {@code c} represents a size-decorated component
     * (i.e. its effective root is a {@code emoji_deco:size} translate), or {@code 1.0f}.
     */
    public static float extractScale(Component c) {
        TranslatableContents tc = findTranslate(c);
        if (tc == null) return 1.0f;
        Object[] args = tc.getArgs();
        if (args.length < 1) return 1.0f;
        try {
            return Float.parseFloat(args[0].toString());
        } catch (NumberFormatException e) {
            return 1.0f;
        }
    }

    /**
     * Returns the inner content component from a size-decorated component.
     * If {@code c} is not a size-decorated component, returns {@code c} unchanged.
     */
    public static Component extractContent(Component c) {
        TranslatableContents tc = findTranslate(c);
        if (tc == null) return c;
        Object[] args = tc.getArgs();
        if (args.length < 2) return Component.empty();
        return args[1] instanceof Component comp ? comp : Component.literal(args[1].toString());
    }

    /**
     * Converts a parsed {@link Component} into a {@link FormattedCharSequence} that
     * carries scaling metadata, for use in sign and graffiti rendering.
     *
     * <ul>
     *   <li>If the entire line is a single {@code emoji_deco:size} decorator →
     *       {@link ScaledSequence}</li>
     *   <li>If the line has mixed scaled/unscaled siblings →
     *       {@link CompositeScaledSequence}</li>
     *   <li>Otherwise → plain {@link FormattedCharSequence} from {@code font.split}</li>
     * </ul>
     */
    public static FormattedCharSequence buildScaledLine(Font font, Component parsed) {
        // Case 1: entire line is a single size-decorated component
        float sizeScale = extractScale(parsed);
        if (sizeScale != 1.0f) {
            Component content = extractContent(parsed);
            List<FormattedCharSequence> lines = font.split(content, Integer.MAX_VALUE / 2);
            FormattedCharSequence fcs = lines.isEmpty() ? FormattedCharSequence.EMPTY : lines.get(0);
            return new ScaledSequence(fcs, sizeScale);
        }

        // Case 2: mixed siblings — check if any sibling carries a size scale
        List<Component> siblings = parsed.getSiblings();
        boolean anyScaled = false;
        for (Component sib : siblings) {
            if (extractScale(sib) != 1.0f) { anyScaled = true; break; }
        }
        if (!anyScaled || siblings.isEmpty()) {
            List<FormattedCharSequence> lines = font.split(parsed, Integer.MAX_VALUE / 2);
            return lines.isEmpty() ? FormattedCharSequence.EMPTY : lines.get(0);
        }

        // Build one segment per sibling, each with its own scale
        List<CompositeScaledSequence.Segment> segments = new ArrayList<>();
        for (Component sib : siblings) {
            float s = extractScale(sib);
            if (s != 1.0f) {
                Component content = extractContent(sib);
                List<FormattedCharSequence> lines = font.split(content, Integer.MAX_VALUE / 2);
                segments.add(new CompositeScaledSequence.Segment(
                        lines.isEmpty() ? FormattedCharSequence.EMPTY : lines.get(0), s));
            } else {
                List<FormattedCharSequence> lines = font.split(sib, Integer.MAX_VALUE / 2);
                segments.add(new CompositeScaledSequence.Segment(
                        lines.isEmpty() ? FormattedCharSequence.EMPTY : lines.get(0), 1.0f));
            }
        }
        return new CompositeScaledSequence(segments);
    }

    private static TranslatableContents findTranslate(Component c) {
        if (c.getContents() instanceof TranslatableContents tc && KEY.equals(tc.getKey()))
            return tc;

        List<Component> siblings = c.getSiblings();
        if (siblings.size() == 1 && isTransparentContent(c.getContents())) {
            Component child = siblings.get(0);
            if (child.getContents() instanceof TranslatableContents tc && KEY.equals(tc.getKey()))
                return tc;
        }
        return null;
    }

    private static boolean isTransparentContent(ComponentContents contents) {
        return contents == ComponentContents.EMPTY
            || (contents instanceof LiteralContents lc && lc.text().isEmpty());
    }
}
