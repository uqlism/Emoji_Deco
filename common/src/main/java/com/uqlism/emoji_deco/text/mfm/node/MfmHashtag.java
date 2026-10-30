package com.uqlism.emoji_deco.text.mfm.node;

/**
 * Ported from mfm.kt's
 * {@code api/src/main/kotlin/com/github/samunohito/mfm/api/node/MfmHashtag.kt}
 */
public record MfmHashtag(String hashtag) implements IMfmNode {
  @Override
  public MfmNodeType type() {
    return MfmNodeType.HashTag;
  }

  @Override
  public String stringify() {
    return "#" + hashtag;
  }
}
