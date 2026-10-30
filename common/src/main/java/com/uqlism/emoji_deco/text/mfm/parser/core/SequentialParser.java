package com.uqlism.emoji_deco.text.mfm.parser.core;

import com.uqlism.emoji_deco.text.mfm.parser.core.utils.ParserUtils;

import java.util.List;

/**
 * Ported from mfm.kt's {@code api/src/main/kotlin/com/github/samunohito/mfm/api/parser/core/SequentialParser.kt}
 *
 * <p>An implementation of {@link IMfmParser} that sequentially searches for substrings using
 * multiple other MFM parsers. We expect the input string to satisfy all parsers passed to this
 * constructor; if any one of them fails, this implementation will also treat the search as
 * failed.
 */
public final class SequentialParser implements IMfmParser {
  private final List<IMfmParser> parsers;

  public SequentialParser(List<IMfmParser> parsers) {
    this.parsers = List.copyOf(parsers);
  }

  public SequentialParser(IMfmParser... parsers) {
    this(List.of(parsers));
  }

  @Override
  public IMfmParserResult find(String input, int startAt, IMfmParserContext context) {
    return ParserUtils.sequential(input, startAt, parsers, context);
  }
}
