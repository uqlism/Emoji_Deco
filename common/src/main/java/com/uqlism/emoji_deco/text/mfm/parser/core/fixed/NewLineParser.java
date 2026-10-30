package com.uqlism.emoji_deco.text.mfm.parser.core.fixed;

import com.uqlism.emoji_deco.text.mfm.parser.core.AlternateParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserContext;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult;

/**
 * Ported from mfm.kt's {@code api/src/main/kotlin/com/github/samunohito/mfm/api/parser/core/fixed/NewLineParser.kt}
 *
 * <p>An implementation of {@link IMfmParser} that searches for newline characters (line feed,
 * carriage return, and carriage return followed by line feed) in a given string.
 */
public final class NewLineParser implements IMfmParser {
  public static final NewLineParser INSTANCE = new NewLineParser();

  private static final AlternateParser DELEGATE =
      new AlternateParser(CrParser.INSTANCE, LfParser.INSTANCE, CrLfParser.INSTANCE);

  private NewLineParser() {
  }

  @Override
  public IMfmParserResult find(String input, int startAt, IMfmParserContext context) {
    return DELEGATE.find(input, startAt, context);
  }
}
