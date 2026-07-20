package com.uqlism.emoji_deco.text.mfm.parser.core;

import java.util.Set;

/**
 * Ported from mfm.kt's {@code api/src/main/kotlin/com/github/samunohito/mfm/api/parser/IMfmParserContext.kt}
 *
 * <p>This is an interface definition for values used to exchange special parameters between
 * {@link IMfmParser}s.
 *
 * <p>The Kotlin source models {@code excludeParsers} as a mutable {@code var} property backed by
 * a {@code data class}; since Java records are immutable, {@link Impl} is a plain mutable class
 * instead of a record.
 */
public interface IMfmParserContext {
  /**
   * Used in recursive parser implementations. Implementations of {@link IMfmParser} registered in
   * this set are not used during parsing.
   */
  Set<Class<? extends IMfmParser>> excludeParsers();

  void setExcludeParsers(Set<Class<? extends IMfmParser>> excludeParsers);

  final class Impl implements IMfmParserContext {
    private Set<Class<? extends IMfmParser>> excludeParsers;

    public Impl() {
      this(Set.of());
    }

    public Impl(Set<Class<? extends IMfmParser>> excludeParsers) {
      this.excludeParsers = excludeParsers;
    }

    @Override
    public Set<Class<? extends IMfmParser>> excludeParsers() {
      return excludeParsers;
    }

    @Override
    public void setExcludeParsers(Set<Class<? extends IMfmParser>> excludeParsers) {
      this.excludeParsers = excludeParsers;
    }
  }
}
