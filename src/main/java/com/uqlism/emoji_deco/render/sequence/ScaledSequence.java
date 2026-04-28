package com.uqlism.emoji_deco.render.sequence;

import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;

/** A {@link FormattedCharSequence} that carries independent x/y scale factors. */
public final class ScaledSequence implements FormattedCharSequence {

    private final FormattedCharSequence inner;
    private final float scaleX;
    private final float scaleY;

    public ScaledSequence(FormattedCharSequence inner, float scaleX, float scaleY) {
        this.inner = inner;
        this.scaleX = scaleX;
        this.scaleY = scaleY;
    }

    @Override
    public boolean accept(FormattedCharSink sink) {
        return inner.accept(sink);
    }

    public FormattedCharSequence inner() { return inner; }
    public float scaleX()               { return scaleX; }
    public float scaleY()               { return scaleY; }
}
