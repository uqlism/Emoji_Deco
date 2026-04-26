package com.uqlism.runicink.text;

import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;

/** A {@link FormattedCharSequence} that carries a per-line render scale for header text. */
public final class HeaderScaledSequence implements FormattedCharSequence {

    private final FormattedCharSequence inner;
    private final float scale;

    public HeaderScaledSequence(FormattedCharSequence inner, float scale) {
        this.inner = inner;
        this.scale = scale;
    }

    @Override
    public boolean accept(FormattedCharSink sink) {
        return inner.accept(sink);
    }

    public FormattedCharSequence inner() { return inner; }
    public float scale() { return scale; }
}
