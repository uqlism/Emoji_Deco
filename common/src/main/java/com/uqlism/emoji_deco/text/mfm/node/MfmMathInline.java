package com.uqlism.emoji_deco.text.mfm.node;

/**
 * Ported from mfm.kt's
 * {@code api/src/main/kotlin/com/github/samunohito/mfm/api/node/MfmMathInline.kt}
 */
public record MfmMathInline(String formula) implements IMfmNode {
  @Override
  public MfmNodeType type() {
    return MfmNodeType.MathInline;
  }

  @Override
  public String stringify() {
    return "\\(" + formula + "\\)";
  }
}
