package com.uqlism.emoji_deco.text.mfm.node;

import java.util.List;

/**
 * Ported from mfm.kt's {@code api/src/main/kotlin/com/github/samunohito/mfm/api/node/MfmNest.kt}
 *
 * <p>A purely structural wrapper produced by {@code NodeFactory} when grouping parser results that
 * don't correspond to a "real" syntax node (e.g. the top-level list of nodes in a document). Its
 * {@link MfmNodeType#Nest} carries only the {@link com.uqlism.emoji_deco.text.mfm.node.MfmNodeAttribute#Virtuality}
 * attribute.
 */
public record MfmNest(List<IMfmNode> children) implements IMfmNodeChildrenHolder {
  public MfmNest {
    children = List.copyOf(children);
  }

  public MfmNest(IMfmNode... children) {
    this(List.of(children));
  }

  @Override
  public MfmNodeType type() {
    return MfmNodeType.Nest;
  }

  @Override
  public String stringify() {
    return MfmUtils.stringify(children);
  }
}
