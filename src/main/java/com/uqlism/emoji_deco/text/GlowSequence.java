package com.uqlism.emoji_deco.text;

import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;

/**
 * A {@link FormattedCharSequence} that carries a glow flag.
 * When glow=true, renders at maximum brightness regardless of ambient light.
 * When glow=false, acts as an explicit "no glow" override for nested content.
 */
public final class GlowSequence implements FormattedCharSequence {

    public static final int FULL_LIGHT = 0xF000F0;

    private final FormattedCharSequence inner;
    private final boolean glow;

    public GlowSequence(FormattedCharSequence inner, boolean glow) {
        this.inner = inner;
        this.glow = glow;
    }

    @Override
    public boolean accept(FormattedCharSink sink) {
        return inner.accept(sink);
    }

    public FormattedCharSequence inner() { return inner; }
    public boolean glow()               { return glow; }
}
