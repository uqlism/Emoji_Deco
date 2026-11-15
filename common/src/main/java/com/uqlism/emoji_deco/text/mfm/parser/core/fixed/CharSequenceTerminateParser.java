package com.uqlism.emoji_deco.text.mfm.parser.core.fixed;

import com.uqlism.emoji_deco.text.mfm.parser.core.FoundType;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserContext;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult;
import com.uqlism.emoji_deco.text.mfm.parser.core.utils.IntRange;

import static com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult.failure;
import static com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult.success;

/**
 * Ported from mfm.kt's {@code api/src/main/kotlin/com/github/samunohito/mfm/api/parser/core/fixed/CharSequenceTerminateParser.kt}
 *
 * <p>An implementation of {@link IMfmParser} that detects whether the search start position has
 * reached the end of the search string.
 */
public final class CharSequenceTerminateParser implements IMfmParser {
  public static final CharSequenceTerminateParser INSTANCE = new CharSequenceTerminateParser();

  private CharSequenceTerminateParser() {
  }

  @Override
  public IMfmParserResult find(String input, int startAt, IMfmParserContext context) {
    if (input.length() <= startAt) {
      return success(FoundType.Core, IntRange.EMPTY, IntRange.EMPTY, startAt);
    } else {
      return failure();
    }
  }
}
