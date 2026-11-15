package com.uqlism.emoji_deco.text.mfm.parser.core;

import com.uqlism.emoji_deco.text.mfm.parser.core.utils.ParserUtils;

import java.util.List;

/**
 * Ported from mfm.kt's {@code api/src/main/kotlin/com/github/samunohito/mfm/api/parser/core/AlternateParser.kt}
 *
 * <p>An implementation of {@link IMfmParser} that searches for a given string in a given string
 * using a collection of other parsers. If all parsers passed to this constructor fail, this
 * implementation treats the search as failing as well.
 */
public final class AlternateParser implements IMfmParser {
  private final List<IMfmParser> parsers;

  public AlternateParser(List<IMfmParser> parsers) {
    this.parsers = List.copyOf(parsers);
  }

  public AlternateParser(IMfmParser... parsers) {
    this(List.of(parsers));
  }

  @Override
  public IMfmParserResult find(String input, int startAt, IMfmParserContext context) {
    return ParserUtils.alternate(input, startAt, parsers, context);
  }
}
