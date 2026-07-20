package com.uqlism.emoji_deco.text.mfm.node;

/**
 * Ported from mfm.kt's
 * {@code api/src/main/kotlin/com/github/samunohito/mfm/api/node/MfmMathBlock.kt}
 */
public record MfmMathBlock(String formula) implements IMfmNode {
  @Override
  public MfmNodeType type() {
    return MfmNodeType.MathBlock;
  }

  @Override
  public String stringify() {
    return "\\[\n" + formula + "\n\\]";
  }
}
