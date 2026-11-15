package com.uqlism.emoji_deco.text.mfm.node;

import java.util.List;

/**
 * Ported from mfm.kt's {@code api/src/main/kotlin/com/github/samunohito/mfm/api/node/MfmBold.kt}
 */
public record MfmBold(List<IMfmNode> children) implements IMfmNodeChildrenHolder {
  public MfmBold {
    children = List.copyOf(children);
  }

  public MfmBold(IMfmNode... children) {
    this(List.of(children));
  }

  @Override
  public MfmNodeType type() {
    return MfmNodeType.Bold;
  }

  @Override
  public String stringify() {
    return "**" + MfmUtils.stringify(children) + "**";
  }
}
