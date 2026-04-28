package com.uqlism.emoji_deco.render.sequence;

import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;

/** A {@link FormattedCharSequence} that carries an x/y rendering offset. */
public final class OffsetSequence implements FormattedCharSequence {

    private final FormattedCharSequence inner;
    private final float offsetX;
    private final float offsetY;

    public OffsetSequence(FormattedCharSequence inner, float offsetX, float offsetY) {
        this.inner = inner;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    @Override
    public boolean accept(FormattedCharSink sink) {
        return inner.accept(sink);
    }

    public FormattedCharSequence inner()  { return inner; }
    public float offsetX()               { return offsetX; }
    public float offsetY()               { return offsetY; }
}
