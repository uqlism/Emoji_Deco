package com.uqlism.emoji_deco.text.mfm.node;

/**
 * Ported from mfm.kt's {@code api/src/main/kotlin/com/github/samunohito/mfm/api/node/MfmText.kt}
 *
 * <p>The Kotlin {@code Props(text)} indirection is dropped per the porting plan; {@code text} is
 * a direct record component.
 */
public record MfmText(String text) implements IMfmNode {
  @Override
  public MfmNodeType type() {
    return MfmNodeType.Text;
  }

  @Override
  public String stringify() {
    return text;
  }
}
