package com.uqlism.emoji_deco.text.mfm.node;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Ported from mfm.kt's {@code api/src/main/kotlin/com/github/samunohito/mfm/api/node/MfmPlain.kt}
 */
public record MfmPlain(List<IMfmNode> children) implements IMfmNodeChildrenHolder {
  public MfmPlain {
    children = List.copyOf(children);
  }

  public MfmPlain(IMfmNode... children) {
    this(List.of(children));
  }

  /** Ported from the Kotlin {@code constructor(vararg children: String)} overload. */
  public MfmPlain(String... children) {
    this(Arrays.stream(children).map(MfmText::new).collect(Collectors.toList()));
  }

  @Override
  public MfmNodeType type() {
    return MfmNodeType.Plain;
  }

  @Override
  public String stringify() {
    return "<plain>\n" + MfmUtils.stringify(children) + "\n</plain>";
  }
}
