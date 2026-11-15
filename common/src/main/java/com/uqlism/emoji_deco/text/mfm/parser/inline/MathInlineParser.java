package com.uqlism.emoji_deco.text.mfm.parser.inline;

import com.uqlism.emoji_deco.text.mfm.parser.core.FoundType;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserContext;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult;
import com.uqlism.emoji_deco.text.mfm.parser.core.SequentialParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.StringParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.SubstringFoundInfo;
import com.uqlism.emoji_deco.text.mfm.parser.core.charsequence.AlternateScanningParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.fixed.NewLineParser;

import static com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult.failure;
import static com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult.success;

/**
 * Ported from mfm.kt's {@code api/src/main/kotlin/com/github/samunohito/mfm/api/parser/inline/MathInlineParser.kt}
 *
 * <p>An {@link IMfmParser} implementation for detecting "math inline" syntax. The string enclosed
 * by {@code \(} and {@code \)} will be the search result.
 *
 * <p>Notes: nesting of MFM syntax is not possible (all content is interpreted as text); content
 * cannot be empty; the content cannot contain line breaks.
 */
public final class MathInlineParser implements IMfmParser {
  public static final MathInlineParser INSTANCE = new MathInlineParser();

  private static final StringParser OPEN = new StringParser("\\(");
  private static final StringParser CLOSE = new StringParser("\\)");
  private static final SequentialParser PARSER = new SequentialParser(
      OPEN,
      AlternateScanningParser.ofUntil(CLOSE, NewLineParser.INSTANCE),
      CLOSE
  );

  private MathInlineParser() {
  }

  @Override
  public IMfmParserResult find(String input, int startAt, IMfmParserContext context) {
    IMfmParserResult result = PARSER.find(input, startAt, context);
    if (!result.success()) {
      return failure();
    }

    SubstringFoundInfo contents = result.foundInfo().get(1);
    return success(
        FoundType.MathInline,
        result.foundInfo().overallRange(),
        contents.contentRange(),
        result.foundInfo().resumeIndex()
    );
  }
}
