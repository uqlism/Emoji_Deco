package com.uqlism.emoji_deco.text.mfm.parser.core.fixed;

import com.uqlism.emoji_deco.text.mfm.parser.core.FoundType;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserContext;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult;
import com.uqlism.emoji_deco.text.mfm.parser.core.utils.IntRange;

import static com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult.failure;
import static com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult.success;

/**
 * Ported from mfm.kt's {@code api/src/main/kotlin/com/github/samunohito/mfm/api/parser/core/fixed/LineEndParser.kt}
 *
 * <p>An implementation of {@link IMfmParser} that searches for the end of a line in a given
 * string. "End of line" indicates the following:
 * <ul>
 *   <li>When the search start position is the end of the string (when the length of the string
 *       and the search start position are the same)</li>
 *   <li>When the position to start searching is a line feed code</li>
 * </ul>
 */
public final class LineEndParser implements IMfmParser {
  public static final LineEndParser INSTANCE = new LineEndParser();

  private LineEndParser() {
  }

  @Override
  public IMfmParserResult find(String input, int startAt, IMfmParserContext context) {
    boolean result;
    if (input.length() == startAt) {
      result = true;
    } else if (CrParser.INSTANCE.find(input, startAt, context).success()) {
      result = true;
    } else if (LfParser.INSTANCE.find(input, startAt, context).success()) {
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
