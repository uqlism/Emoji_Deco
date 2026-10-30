package com.uqlism.emoji_deco.text.mfm.node;

/**
 * Ported from mfm.kt's
 * {@code api/src/main/kotlin/com/github/samunohito/mfm/api/node/MfmSearch.kt}
 */
public record MfmSearch(String query, String content) implements IMfmNode {
  @Override
  public MfmNodeType type() {
    return MfmNodeType.Search;
  }

  @Override
  public String stringify() {
    return content;
  }
}
