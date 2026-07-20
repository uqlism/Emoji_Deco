package com.uqlism.emoji_deco.text.mfm.parser.inline;

import com.uqlism.emoji_deco.text.mfm.parser.core.FoundType;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserContext;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult;
import com.uqlism.emoji_deco.text.mfm.parser.core.SequentialParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.StringParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.SubstringFoundInfo;
import com.uqlism.emoji_deco.text.mfm.parser.core.charsequence.SequentialScanningParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.fixed.NewLineParser;

import static com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult.failure;
import static com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult.success;

/**
 * Ported from mfm.kt's {@code api/src/main/kotlin/com/github/samunohito/mfm/api/parser/inline/PlainTagParser.kt}
 *
 * <p>An {@link IMfmParser} implementation for detecting "plain tag" syntax. The string enclosed
 * by {@code <plain>} tags will be the search result.
 *
 * <p>Notes: nesting of MFM syntax is not possible; all content is interpreted as text.
 */
public final class PlainTagParser implements IMfmParser {
  public static final PlainTagParser INSTANCE = new PlainTagParser();

  private static final StringParser OPEN = new StringParser("<plain>");
  private static final StringParser CLOSE = new StringParser("</plain>");
  private static final SequentialParser PARSER = new SequentialParser(
      OPEN,
      NewLineParser.INSTANCE.optional(),
      SequentialScanningParser.ofUntil(NewLineParser.INSTANCE.optional(), CLOSE),
      NewLineParser.INSTANCE.optional(),
      CLOSE
  );

  private PlainTagParser() {
  }

  @Override
  public IMfmParserResult find(String input, int startAt, IMfmParserContext context) {
    IMfmParserResult result = PARSER.find(input, startAt, context);
    if (!result.success()) {
      return failure();
    }

    SubstringFoundInfo contents = result.foundInfo().get(2);
    return success(
        FoundType.PlainTag,
        result.foundInfo().overallRange(),
        contents.contentRange(),
        result.foundInfo().resumeIndex()
    );
  }
}
