package com.uqlism.emoji_deco.text.mfm.parser.inline;

import com.uqlism.emoji_deco.text.mfm.parser.InlineParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.FoundType;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserContext;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult;
import com.uqlism.emoji_deco.text.mfm.parser.core.SequentialParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.StringParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.SubstringFoundInfo;

import static com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult.failure;
import static com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult.success;

/**
 * Ported from mfm.kt's {@code api/src/main/kotlin/com/github/samunohito/mfm/api/parser/inline/SmallTagParser.kt}
 *
 * <p>An {@link IMfmParser} implementation for detecting "small" syntax. The string enclosed by
 * {@code <small>} tags will be the search result.
 *
 * <p>Notes: applies {@link InlineParser} to the content again to recursively detect inline
 * syntax; any character and newline can be used in the content; the content cannot be left empty.
 */
public final class SmallTagParser implements IMfmParser {
  public static final SmallTagParser INSTANCE = new SmallTagParser();

  private static final StringParser OPEN = new StringParser("<small>");
  private static final StringParser CLOSE = new StringParser("</small>");
  private static final SequentialParser PARSER = new SequentialParser(OPEN, new InlineParser(CLOSE), CLOSE);

  private SmallTagParser() {
  }

  @Override
  public IMfmParserResult find(String input, int startAt, IMfmParserContext context) {
    IMfmParserResult result = PARSER.find(input, startAt, context);
    if (!result.success()) {
      return failure();
    }

    SubstringFoundInfo contents = result.foundInfo().get(1);
    return success(
        FoundType.SmallTag,
        result.foundInfo().overallRange(),
        contents.contentRange(),
        result.foundInfo().resumeIndex(),
        contents.nestedInfos()
    );
  }
}
