package com.uqlism.emoji_deco.text;

import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;

/**
 * A {@link FormattedCharSequence} that carries a {@link LightMode} controlling
 * how packedLight is resolved during rendering.
 */
public final class LightSequence implements FormattedCharSequence {

    private final FormattedCharSequence inner;
    private final LightMode mode;

    public LightSequence(FormattedCharSequence inner, LightMode mode) {
        this.inner = inner;
        this.mode = mode;
    }

    @Override
    public boolean accept(FormattedCharSink sink) {
        return inner.accept(sink);
    }

    public FormattedCharSequence inner() { return inner; }
    public LightMode mode()             { return mode; }
}
