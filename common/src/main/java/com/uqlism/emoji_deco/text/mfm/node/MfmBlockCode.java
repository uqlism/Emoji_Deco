package com.uqlism.emoji_deco.text.mfm.node;

/**
 * Ported from mfm.kt's
 * {@code api/src/main/kotlin/com/github/samunohito/mfm/api/node/MfmBlockCode.kt}
 *
 * @param lang Nullable, matching the Kotlin {@code lang: String?} property.
 */
public record MfmBlockCode(String code, String lang) implements IMfmNode {
  public MfmBlockCode(String code) {
    this(code, null);
  }

  @Override
  public MfmNodeType type() {
    return MfmNodeType.BlockCode;
  }

  @Override
  public String stringify() {
    return "```" + (lang != null ? lang : "") + "\n" + code + "\n```";
  }
}
