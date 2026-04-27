package com.uqlism.emoji_deco.text;

import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;

/** A {@link FormattedCharSequence} that carries a scale factor for rendering. */
public final class ScaledSequence implements FormattedCharSequence {

    private final FormattedCharSequence inner;
    private final float scale;

    public ScaledSequence(FormattedCharSequence inner, float scale) {
        this.inner = inner;
        this.scale = scale;
    }

    @Override
    public boolean accept(FormattedCharSink sink) {
        return inner.accept(sink);
    }

    public FormattedCharSequence inner() { return inner; }
    public float scale()                 { return scale; }
}
