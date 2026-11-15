package com.uqlism.emoji_deco.text.mfm.parser.core.fixed;

import com.uqlism.emoji_deco.text.mfm.parser.core.FoundType;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserContext;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult;
import com.uqlism.emoji_deco.text.mfm.parser.core.utils.IntRange;

import static com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult.failure;
import static com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult.success;

/**
 * Ported from mfm.kt's {@code api/src/main/kotlin/com/github/samunohito/mfm/api/parser/core/fixed/LineBeginParser.kt}
 *
 * <p>An implementation of {@link IMfmParser} that finds the beginning of a line within a given
 * string. "Beginning of line" indicates the following:
 * <ul>
 *   <li>If the search start position is 0</li>
 *   <li>If one character before the search start position is a linefeed code and the search
 *       start position is immediately after the linefeed code</li>
 * </ul>
 */
public final class LineBeginParser implements IMfmParser {
  public static final LineBeginParser INSTANCE = new LineBeginParser();

  private LineBeginParser() {
  }

  @Override
  public IMfmParserResult find(String input, int startAt, IMfmParserContext context) {
    // Order matters: startAt == 0 is checked before startAt - 1 is used as an index below.
    boolean result;
    if (input.length() == startAt) {
      result = true;
    } else if (startAt == 0) {
      result = true;
    } else if (CrParser.INSTANCE.find(input, startAt - 1, context).success()) {
      result = true;
    } else if (LfParser.INSTANCE.find(input, startAt - 1, context).success()) {
      result = true;
    } else {
      result = false;
    }

    if (result) {
      return success(FoundType.Core, IntRange.EMPTY, IntRange.EMPTY, startAt);
    } else {
      return failure();
    }
  }
}
