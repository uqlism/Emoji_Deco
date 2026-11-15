package com.uqlism.emoji_deco.text.mfm.node;

import java.util.List;

/**
 * Ported from mfm.kt's {@code api/src/main/kotlin/com/github/samunohito/mfm/api/node/MfmCenter.kt}
 */
public record MfmCenter(List<IMfmNode> children) implements IMfmNodeChildrenHolder {
  public MfmCenter {
    children = List.copyOf(children);
  }

  public MfmCenter(IMfmNode... children) {
    this(List.of(children));
  }

  @Override
  public MfmNodeType type() {
    return MfmNodeType.Center;
  }

  @Override
  public String stringify() {
    return "<center>\n" + MfmUtils.stringify(children) + "\n</center>";
  }
}
