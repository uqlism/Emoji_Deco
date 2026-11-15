package com.uqlism.emoji_deco.text.mfm.node;

/**
 * Ported from mfm.kt's {@code api/src/main/kotlin/com/github/samunohito/mfm/api/node/MfmUrl.kt}
 *
 * @param brackets Boxed to allow {@code null}, matching the Kotlin {@code brackets: Boolean?}
 *     property.
 */
public record MfmUrl(String url, Boolean brackets) implements IMfmNode {
  @Override
  public MfmNodeType type() {
    return MfmNodeType.Url;
  }

  @Override
  public String stringify() {
    return Boolean.TRUE.equals(brackets) ? "<" + url + ">" : url;
  }
}
