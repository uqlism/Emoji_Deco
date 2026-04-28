package com.uqlism.emoji_deco.render.sequence;

import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;

/** A {@link FormattedCharSequence} that carries a rotation angle (degrees, clockwise). */
public final class RotatedSequence implements FormattedCharSequence {

    private final FormattedCharSequence inner;
    private final float angle;

    public RotatedSequence(FormattedCharSequence inner, float angle) {
        this.inner = inner;
        this.angle = angle;
    }

    @Override
    public boolean accept(FormattedCharSink sink) {
        return inner.accept(sink);
    }

    public FormattedCharSequence inner() { return inner; }
    public float angle()                 { return angle; }
}
