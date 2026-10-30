package com.uqlism.emoji_deco.text.mfm.node;

import java.util.List;

/**
 * Ported from mfm.kt's {@code api/src/main/kotlin/com/github/samunohito/mfm/api/node/MfmStrike.kt}
 */
public record MfmStrike(List<IMfmNode> children) implements IMfmNodeChildrenHolder {
  public MfmStrike {
    children = List.copyOf(children);
  }

  public MfmStrike(IMfmNode... children) {
    this(List.of(children));
  }

  @Override
  public MfmNodeType type() {
    return MfmNodeType.Strike;
  }

  @Override
  public String stringify() {
    return "~~" + MfmUtils.stringify(children) + "~~";
  }
}
