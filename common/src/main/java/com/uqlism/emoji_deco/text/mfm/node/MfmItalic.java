package com.uqlism.emoji_deco.text.mfm.node;

import java.util.List;

/**
 * Ported from mfm.kt's {@code api/src/main/kotlin/com/github/samunohito/mfm/api/node/MfmItalic.kt}
 */
public record MfmItalic(List<IMfmNode> children) implements IMfmNodeChildrenHolder {
  public MfmItalic {
    children = List.copyOf(children);
  }

  public MfmItalic(IMfmNode... children) {
    this(List.of(children));
  }

  @Override
  public MfmNodeType type() {
    return MfmNodeType.Italic;
  }

  @Override
  public String stringify() {
    return "<i>" + MfmUtils.stringify(children) + "</i>";
  }
}
