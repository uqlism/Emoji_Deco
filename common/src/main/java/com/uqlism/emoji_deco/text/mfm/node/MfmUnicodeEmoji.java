package com.uqlism.emoji_deco.text.mfm.node;

/**
 * Ported from mfm.kt's
 * {@code api/src/main/kotlin/com/github/samunohito/mfm/api/node/MfmUnicodeEmoji.kt}
 */
public record MfmUnicodeEmoji(String emoji) implements IMfmNode {
  @Override
  public MfmNodeType type() {
    return MfmNodeType.UnicodeEmoji;
  }

  @Override
  public String stringify() {
    return emoji;
  }
}
