package com.uqlism.emoji_deco.render.sequence;

import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;
import org.joml.Matrix4f;

/**
 * A {@link FormattedCharSequence} that carries a local affine transform matrix.
 * The matrix is applied relative to the draw origin (x, y) at render time.
 * All spatial decorators (scale, offset, rotate) produce this type.
 */
public final class AffineSequence implements FormattedCharSequence {

    private final FormattedCharSequence inner;
    private final Matrix4f localTransform;

    public AffineSequence(FormattedCharSequence inner, Matrix4f localTransform) {
        this.inner = inner;
        this.localTransform = localTransform;
    }

    @Override
    public boolean accept(FormattedCharSink sink) {
        return inner.accept(sink);
    }

    public FormattedCharSequence inner()  { return inner; }
    public Matrix4f localTransform()      { return localTransform; }
}
