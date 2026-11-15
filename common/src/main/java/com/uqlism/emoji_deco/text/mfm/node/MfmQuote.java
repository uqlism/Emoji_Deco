package com.uqlism.emoji_deco.text.mfm.node;

import java.util.List;

/**
 * Ported from mfm.kt's {@code api/src/main/kotlin/com/github/samunohito/mfm/api/node/MfmQuote.kt}
 */
public record MfmQuote(List<IMfmNode> children) implements IMfmNodeChildrenHolder {
  public MfmQuote {
    children = List.copyOf(children);
  }

  public MfmQuote(IMfmNode... children) {
    this(List.of(children));
  }

  @Override
  public MfmNodeType type() {
    return MfmNodeType.Quote;
  }

  @Override
  public String stringify() {
    return MfmUtils.prependIndent(MfmUtils.stringify(children), "> ");
  }
}
