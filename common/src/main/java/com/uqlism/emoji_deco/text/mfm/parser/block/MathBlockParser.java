package com.uqlism.emoji_deco.text.mfm.parser.block;

import com.uqlism.emoji_deco.text.mfm.parser.core.FoundType;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserContext;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult;
import com.uqlism.emoji_deco.text.mfm.parser.core.SequentialParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.StringParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.SubstringFoundInfo;
import com.uqlism.emoji_deco.text.mfm.parser.core.charsequence.SequentialScanningParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.fixed.LineBeginParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.fixed.LineEndParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.fixed.NewLineParser;

import static com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult.failure;
import static com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult.success;

/**
 * Ported from mfm.kt's {@code api/src/main/kotlin/com/github/samunohito/mfm/api/parser/block/MathBlockParser.kt}
 *
 * <p>An {@link IMfmParser} implementation for detecting "math block" syntax. The string enclosed
 * by {@code \[} and {@code \]} will be the search result.
 *
 * <p>Notes: nesting of MFM syntax is not possible (all content is interpreted as text);
 * {@code \[} must be the beginning of the line; {@code \]} must be the end of the line; leading
 * and trailing spaces and newlines are trimmed.
 */
public final class MathBlockParser implements IMfmParser {
  public static final MathBlockParser INSTANCE = new MathBlockParser();

  private static final StringParser OPEN = new StringParser("\\[");
  private static final StringParser CLOSE = new StringParser("\\]");
  private static final SequentialParser PARSER = new SequentialParser(
      NewLineParser.INSTANCE.optional(),
      LineBeginParser.INSTANCE,
      OPEN,
      NewLineParser.INSTANCE.optional(),
      SequentialScanningParser.ofUntil(NewLineParser.INSTANCE.optional(), CLOSE),
      NewLineParser.INSTANCE.optional(),
      CLOSE,
      LineEndParser.INSTANCE,
      NewLineParser.INSTANCE.optional()
  );

  private MathBlockParser() {
  }

  @Override
  public IMfmParserResult find(String input, int startAt, IMfmParserContext context) {
    IMfmParserResult result = PARSER.find(input, startAt, context);
    if (!result.success()) {
      return failure();
    }

    SubstringFoundInfo contents = result.foundInfo().get(4);
    return success(
        FoundType.MathBlock,
        result.foundInfo().overallRange(),
        contents.contentRange(),
        result.foundInfo().resumeIndex()
    );
  }
}
