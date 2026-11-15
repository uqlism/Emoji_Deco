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
 * Ported from mfm.kt's {@code api/src/main/kotlin/com/github/samunohito/mfm/api/parser/inline/InlineCodeParser.kt}
 *
 * <p>An {@link IMfmParser} implementation for detecting "inline code" syntax. The string enclosed
 * by "`" will be the search result.
 *
 * <p>Notes: nesting of MFM syntax is not possible (all content is interpreted as text); any
 * character and newline can be used in the content; the content cannot be left empty; the content
 * cannot contain the character "`".
 */
public final class InlineCodeParser implements IMfmParser {
  public static final InlineCodeParser INSTANCE = new InlineCodeParser();

  private static final StringParser MARK = new StringParser("`");
  private static final StringParser MARK2 = new StringParser("´");
  private static final SequentialParser PARSER = new SequentialParser(
      MARK,
      AlternateScanningParser.ofUntil(MARK, MARK2, NewLineParser.INSTANCE),
      MARK
  );

  private InlineCodeParser() {
  }

  @Override
  public IMfmParserResult find(String input, int startAt, IMfmParserContext context) {
    IMfmParserResult result = PARSER.find(input, startAt, context);
    if (!result.success()) {
      return failure();
    }

    SubstringFoundInfo contents = result.foundInfo().get(1);
    return success(
        FoundType.InlineCode,
        result.foundInfo().overallRange(),
        contents.contentRange(),
        result.foundInfo().resumeIndex()
    );
  }
}
