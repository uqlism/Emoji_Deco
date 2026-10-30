package com.uqlism.emoji_deco.text.mfm.parser.core.fixed;

import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserContext;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult;
import com.uqlism.emoji_deco.text.mfm.parser.core.StringParser;

/**
 * Ported from mfm.kt's {@code api/src/main/kotlin/com/github/samunohito/mfm/api/parser/core/fixed/LfParser.kt}
 *
 * <p>An implementation of {@link IMfmParser} that searches for line feed characters in a given
 * string.
 */
public final class LfParser implements IMfmParser {
  public static final LfParser INSTANCE = new LfParser();

  private static final StringParser DELEGATE = new StringParser("\n");

  private LfParser() {
  }

  @Override
  public IMfmParserResult find(String input, int startAt, IMfmParserContext context) {
    return DELEGATE.find(input, startAt, context);
  }
}
