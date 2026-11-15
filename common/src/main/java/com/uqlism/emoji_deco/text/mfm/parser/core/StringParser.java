package com.uqlism.emoji_deco.text.mfm.parser.core;

import com.uqlism.emoji_deco.text.mfm.parser.core.utils.IntRange;

import static com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult.failure;
import static com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult.success;

/**
 * Ported from mfm.kt's {@code api/src/main/kotlin/com/github/samunohito/mfm/api/parser/core/StringParser.kt}
 *
 * <p>An implementation of {@link IMfmParser} that searches for a given string in a given string.
 */
public final class StringParser implements IMfmParser {
  private final String word;

  public StringParser(String word) {
    this.word = word;
  }

  @Override
  public IMfmParserResult find(String input, int startAt, IMfmParserContext context) {
    IntRange resultRange = new IntRange(startAt, startAt + word.length());
    if ((input.length() - startAt) < word.length()) {
      return failure();
    } else if (!input.substring(resultRange.start(), resultRange.endExclusive()).equals(word)) {
      return failure();
    } else {
      return success(FoundType.Core, resultRange, resultRange, resultRange.next());
    }
  }
}
