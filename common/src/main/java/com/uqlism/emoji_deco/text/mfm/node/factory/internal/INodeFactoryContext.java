package com.uqlism.emoji_deco.text.mfm.node.factory.internal;

/**
 * Ported from mfm.kt's
 * {@code api/src/main/kotlin/com/github/samunohito/mfm/api/node/factory/internal/INodeFactoryContext.kt}
 *
 * <p>The Kotlin source models this as a {@code data class} with a mutable {@code var nestLevel};
 * since Java records are immutable, {@link Impl} is a plain mutable class instead, mirroring how
 * {@code IMfmParserContext.Impl} was ported in the parser/core layer.
 */
public interface INodeFactoryContext {
  /** The maximum nest level of the factory. */
  int maximumNestLevel();

  /**
   * The nest level of the factory. If this number exceeds {@link #maximumNestLevel()}, all
   * parsing results below that level will be treated as {@code MfmText} nodes.
   */
  int nestLevel();

  void setNestLevel(int nestLevel);

  final class Impl implements INodeFactoryContext {
    private final int maximumNestLevel;
    private int nestLevel;

    public Impl() {
      this(20, 0);
    }

    public Impl(int maximumNestLevel, int nestLevel) {
      this.maximumNestLevel = maximumNestLevel;
      this.nestLevel = nestLevel;
    }

    @Override
    public int maximumNestLevel() {
      return maximumNestLevel;
    }

    @Override
    public int nestLevel() {
      return nestLevel;
    }

    @Override
    public void setNestLevel(int nestLevel) {
      this.nestLevel = nestLevel;
    }
  }
}
