package com.uqlism.emoji_deco.text.mfm.node;

/**
 * Ported from mfm.kt's
 * {@code api/src/main/kotlin/com/github/samunohito/mfm/api/node/MfmInlineCode.kt}
 */
public record MfmInlineCode(String code) implements IMfmNode {
  @Override
  public MfmNodeType type() {
    return MfmNodeType.InlineCode;
  }

  @Override
  public String stringify() {
    return "`" + code + "`";
  }
}
