package com.uqlism.emoji_deco.text.mfm.parser.core;

/**
 * Ported from mfm.kt's {@code api/src/main/kotlin/com/github/samunohito/mfm/api/parser/core/FoundType.kt}
 *
 * <p>Indicates the type of format found by {@link IMfmParser}.
 */
public enum FoundType {
  Unknown,
  Empty,
  Core,
  Big,
  BoldAsta,
  BoldTag,
  BoldUnder,
  CenterTag,
  CodeBlock,
  EmojiCode,
  Fn,
  Full,
  Hashtag,
  InlineCode,
  Inline,
  ItalicAsta,
  ItalicTag,
  ItalicUnder,
  Link,
  MathBlock,
  MathInline,
  Mention,
  PlainTag,
  Quote,
  Search,
  Simple,
  SmallTag,
  StrikeTag,
  StrikeWave,
  UnicodeEmoji,
  Url,
  UrlAlt,
  Text,
}
