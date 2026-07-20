package com.uqlism.emoji_deco.text.mfm.node;

/**
 * Ported from mfm.kt's
 * {@code api/src/main/kotlin/com/github/samunohito/mfm/api/node/MfmEmojiCode.kt}
 */
public record MfmEmojiCode(String name) implements IMfmNode {
  @Override
  public MfmNodeType type() {
    return MfmNodeType.EmojiCode;
  }

  @Override
  public String stringify() {
    return ":" + name + ":";
  }
}
