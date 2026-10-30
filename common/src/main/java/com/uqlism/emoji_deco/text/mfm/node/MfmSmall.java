package com.uqlism.emoji_deco.text.mfm.node;

import java.util.List;

/**
 * Ported from mfm.kt's {@code api/src/main/kotlin/com/github/samunohito/mfm/api/node/MfmSmall.kt}
 */
public record MfmSmall(List<IMfmNode> children) implements IMfmNodeChildrenHolder {
  public MfmSmall {
    children = List.copyOf(children);
  }

  public MfmSmall(IMfmNode... children) {
    this(List.of(children));
  }

  @Override
  public MfmNodeType type() {
    return MfmNodeType.Small;
  }

  @Override
  public String stringify() {
    return "<small>" + MfmUtils.stringify(children) + "</small>";
  }
}
