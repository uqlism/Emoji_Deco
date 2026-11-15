package com.uqlism.emoji_deco.text.mfm.parser;

import com.uqlism.emoji_deco.text.mfm.parser.core.FoundType;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.fixed.CharSequenceTerminateParser;
import com.uqlism.emoji_deco.text.mfm.parser.inline.EmojiCodeParser;
import com.uqlism.emoji_deco.text.mfm.parser.inline.UnicodeEmojiParser;

import java.util.List;

/**
 * Ported from mfm.kt's {@code api/src/main/kotlin/com/github/samunohito/mfm/api/parser/SimpleParser.kt}
 */
public final class SimpleParser extends RecursiveParserBase {
  private static final List<IMfmParser> PARSERS = List.of(
      UnicodeEmojiParser.INSTANCE,
      EmojiCodeParser.INSTANCE
  );

  public SimpleParser() {
    this(CharSequenceTerminateParser.INSTANCE);
  }

  public SimpleParser(IMfmParser terminateParser) {
    super(terminateParser);
  }

  @Override
  protected List<IMfmParser> parsers() {
    return PARSERS;
  }

  @Override
  protected FoundType foundType() {
    return FoundType.Simple;
  }
}
