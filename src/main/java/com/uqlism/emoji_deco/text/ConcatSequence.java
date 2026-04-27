package com.uqlism.emoji_deco.text;

import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;

import java.util.List;

/** A {@link FormattedCharSequence} that concatenates multiple sequences in order. */
public final class ConcatSequence implements FormattedCharSequence {

    private final List<FormattedCharSequence> parts;

    public ConcatSequence(List<FormattedCharSequence> parts) {
        this.parts = List.copyOf(parts);
    }

    @Override
    public boolean accept(FormattedCharSink sink) {
        for (FormattedCharSequence part : parts) {
            if (!part.accept(sink)) return false;
        }
        return true;
    }

    public List<FormattedCharSequence> parts() { return parts; }
}
