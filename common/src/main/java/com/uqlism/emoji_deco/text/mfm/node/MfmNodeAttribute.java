package com.uqlism.emoji_deco.text.mfm.node;

import java.util.Set;

/**
 * Ported from mfm.kt's
 * {@code api/src/main/kotlin/com/github/samunohito/mfm/api/node/MfmNodeAttribute.kt}
 */
public enum MfmNodeAttribute {
  Block,
  Inline,
  Simple,
  Virtuality;

  /** Ported from the Kotlin companion object's {@code setOfAll}. */
  public static final Set<MfmNodeAttribute> SET_OF_ALL = Set.of(Block, Inline, Simple, Virtuality);

  /** Ported from the Kotlin companion object's {@code setOfInline}. */
  public static final Set<MfmNodeAttribute> SET_OF_INLINE = Set.of(Inline, Virtuality);

  /** Ported from the Kotlin companion object's {@code setOfSimple}. */
  public static final Set<MfmNodeAttribute> SET_OF_SIMPLE = Set.of(Simple);
}
