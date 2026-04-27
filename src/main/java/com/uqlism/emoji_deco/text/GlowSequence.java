package com.uqlism.emoji_deco.text;

import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;

/** A {@link FormattedCharSequence} that renders at maximum brightness regardless of ambient light. */
public final class GlowSequence implements FormattedCharSequence {

    public static final int FULL_LIGHT = 0xF000F0;

    private final FormattedCharSequence inner;

    public GlowSequence(FormattedCharSequence inner) {
        this.inner = inner;
    }

    @Override
    public boolean accept(FormattedCharSink sink) {
        return inner.accept(sink);
    }

    public FormattedCharSequence inner() { return inner; }
}
