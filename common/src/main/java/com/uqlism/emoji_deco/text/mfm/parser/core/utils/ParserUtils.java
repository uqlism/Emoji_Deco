package com.uqlism.emoji_deco.text.mfm.parser.core.utils;

import com.uqlism.emoji_deco.text.mfm.parser.core.FoundType;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserContext;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult;
import com.uqlism.emoji_deco.text.mfm.parser.core.SubstringFoundInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult.failure;
import static com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult.success;

/**
 * Ported from mfm.kt's {@code api/src/main/kotlin/com/github/samunohito/mfm/api/parser/core/utils/ParserUtils.kt}
 *
 * <p>Shared implementation logic for {@link com.uqlism.emoji_deco.text.mfm.parser.core.SequentialParser}
 * and {@link com.uqlism.emoji_deco.text.mfm.parser.core.AlternateParser}.
 */
public final class ParserUtils {
  private ParserUtils() {
  }

  public static IMfmParserResult sequential(
      String input,
      int startAt,
      Collection<IMfmParser> parsers,
      IMfmParserContext context
  ) {
    int latestIndex = startAt;
    List<SubstringFoundInfo> results = new ArrayList<>();

    for (IMfmParser parser : parsers) {
      IMfmParserResult result = parser.find(input, latestIndex, context);
      if (!result.success()) {
        return failure();
      }

      latestIndex = result.foundInfo().resumeIndex();
      results.add(result.foundInfo());
    }

    IntRange resultRange = new IntRange(startAt, latestIndex);
    return success(FoundType.Core, resultRange, resultRange, resultRange.next(), results);
  }

  public static IMfmParserResult alternate(
      String input,
      int startAt,
      Collection<IMfmParser> parsers,
      IMfmParserContext context
  ) {
    for (IMfmParser parser : parsers) {
      IMfmParserResult result = parser.find(input, startAt, context);
      if (result.success()) {
        return result;
      }
    }

    return failure();
  }
}
