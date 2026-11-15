package com.uqlism.emoji_deco.text.mfm.node;

import java.util.Set;

/**
 * Ported from mfm.kt's
 * {@code api/src/main/kotlin/com/github/samunohito/mfm/api/node/MfmNodeType.kt}
 */
public enum MfmNodeType {
  None(Set.of(MfmNodeAttribute.Virtuality)),
  Quote(Set.of(MfmNodeAttribute.Block)),
  Search(Set.of(MfmNodeAttribute.Block)),
  BlockCode(Set.of(MfmNodeAttribute.Block)),
  MathBlock(Set.of(MfmNodeAttribute.Block)),
  Center(Set.of(MfmNodeAttribute.Block)),
  UnicodeEmoji(Set.of(MfmNodeAttribute.Inline, MfmNodeAttribute.Simple)),
  EmojiCode(Set.of(MfmNodeAttribute.Inline, MfmNodeAttribute.Simple)),
  Bold(Set.of(MfmNodeAttribute.Inline)),
  Small(Set.of(MfmNodeAttribute.Inline)),
  Italic(Set.of(MfmNodeAttribute.Inline)),
  Strike(Set.of(MfmNodeAttribute.Inline)),
  InlineCode(Set.of(MfmNodeAttribute.Inline)),
  MathInline(Set.of(MfmNodeAttribute.Inline)),
  Mention(Set.of(MfmNodeAttribute.Inline)),
  HashTag(Set.of(MfmNodeAttribute.Inline)),
  Url(Set.of(MfmNodeAttribute.Inline)),
  Link(Set.of(MfmNodeAttribute.Inline)),
  Fn(Set.of(MfmNodeAttribute.Inline)),
  Plain(Set.of(MfmNodeAttribute.Inline)),
  Text(Set.of(MfmNodeAttribute.Inline, MfmNodeAttribute.Simple)),
  Nest(Set.of(MfmNodeAttribute.Virtuality));

  private final Set<MfmNodeAttribute> attributes;

  MfmNodeType(Set<MfmNodeAttribute> attributes) {
    this.attributes = attributes;
  }

  public Set<MfmNodeAttribute> attributes() {
    return attributes;
  }
}
