package com.uqlism.emoji_deco.text.mfm.parser.block;

import com.uqlism.emoji_deco.text.mfm.parser.InlineParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.FoundType;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserContext;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult;
import com.uqlism.emoji_deco.text.mfm.parser.core.SequentialParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.StringParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.SubstringFoundInfo;
import com.uqlism.emoji_deco.text.mfm.parser.core.fixed.LineBeginParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.fixed.LineEndParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.fixed.NewLineParser;

import static com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult.failure;
import static com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult.success;

/**
 * Ported from mfm.kt's {@code api/src/main/kotlin/com/github/samunohito/mfm/api/parser/block/CenterTagParser.kt}
 *
 * <p>An {@link IMfmParser} implementation for detecting "center" syntax. The string enclosed by
 * {@code <center>} tags will be the search result.
 *
 * <p>Notes: applies {@link InlineParser} to the content again to recursively detect inline
 * syntax; {@code <center>} must be the beginning of the line; {@code </center>} must be the end
 * of the line.
 */
public final class CenterTagParser implements IMfmParser {
  public static final CenterTagParser INSTANCE = new CenterTagParser();

  private static final StringParser OPEN = new StringParser("<center>");
  private static final StringParser CLOSE = new StringParser("</center>");
  private static final SequentialParser PARSER = new SequentialParser(
      NewLineParser.INSTANCE.optional(),
      LineBeginParser.INSTANCE,
      OPEN,
      NewLineParser.INSTANCE.optional(),
      new InlineParser(new SequentialParser(NewLineParser.INSTANCE.optional(), CLOSE)),
      NewLineParser.INSTANCE.optional(),
      CLOSE,
      LineEndParser.INSTANCE,
      NewLineParser.INSTANCE.optional()
  );

  private CenterTagParser() {
  }

  @Override
  public IMfmParserResult find(String input, int startAt, IMfmParserContext context) {
    IMfmParserResult result = PARSER.find(input, startAt, context);
    if (!result.success()) {
      return failure();
    }

    SubstringFoundInfo contents = result.foundInfo().get(4);
    return success(
        FoundType.CenterTag,
        result.foundInfo().overallRange(),
        contents.contentRange(),
        result.foundInfo().resumeIndex(),
        contents.nestedInfos()
    );
  }
}
