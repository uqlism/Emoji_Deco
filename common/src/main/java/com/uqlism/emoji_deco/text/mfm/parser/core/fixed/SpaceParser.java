package com.uqlism.emoji_deco.text.mfm.parser.core.fixed;

import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserContext;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult;
import com.uqlism.emoji_deco.text.mfm.parser.core.RegexParser;

import java.util.regex.Pattern;

/**
 * Ported from mfm.kt's {@code api/src/main/kotlin/com/github/samunohito/mfm/api/parser/core/fixed/SpaceParser.kt}
 *
 * <p>An implementation of {@link IMfmParser} that searches for whitespace characters (space, tab,
 * and full-width space) in a given string.
 */
public final class SpaceParser implements IMfmParser {
  public static final SpaceParser INSTANCE = new SpaceParser();

  private static final RegexParser DELEGATE = new RegexParser(Pattern.compile("[ 　\t]"));

  private SpaceParser() {
  }

  @Override
  public IMfmParserResult find(String input, int startAt, IMfmParserContext context) {
    return DELEGATE.find(input, startAt, context);
  }
}
