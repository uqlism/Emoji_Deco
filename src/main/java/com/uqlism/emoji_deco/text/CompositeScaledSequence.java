package com.uqlism.emoji_deco.text;

import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;

import java.util.List;

/**
 * A {@link FormattedCharSequence} composed of segments, each with an independent render scale.
 * Used for sign lines where only part of the text is size-decorated.
 */
public final class CompositeScaledSequence implements FormattedCharSequence {

    public record Segment(FormattedCharSequence chars, float scale) {}

    private final List<Segment> segments;

    public CompositeScaledSequence(List<Segment> segments) {
        this.segments = List.copyOf(segments);
    }

    /** Iterates all characters from all segments (without scale info — used by default accept callers). */
    @Override
    public boolean accept(FormattedCharSink sink) {
        for (Segment seg : segments) {
            if (!seg.chars().accept(sink)) return false;
        }
        return true;
    }

    public List<Segment> segments() { return segments; }
}
