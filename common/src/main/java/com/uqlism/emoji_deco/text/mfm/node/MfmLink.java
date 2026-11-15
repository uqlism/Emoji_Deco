package com.uqlism.emoji_deco.text.mfm.node;

import java.util.List;

/**
 * Ported from mfm.kt's {@code api/src/main/kotlin/com/github/samunohito/mfm/api/node/MfmLink.kt}
 */
public record MfmLink(boolean silent, String url, List<IMfmNode> children) implements IMfmNodeChildrenHolder {
  public MfmLink {
    children = List.copyOf(children);
  }

  public MfmLink(boolean silent, String url, IMfmNode... children) {
    this(silent, url, List.of(children));
  }

  @Override
  public MfmNodeType type() {
    return MfmNodeType.Link;
  }

  @Override
  public String stringify() {
    String prefix = silent ? "?" : "";
    return prefix + "[" + MfmUtils.stringify(children) + "](" + url + ")";
  }
}
