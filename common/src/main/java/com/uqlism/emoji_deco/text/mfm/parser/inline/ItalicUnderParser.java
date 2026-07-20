package com.uqlism.emoji_deco.text.mfm.parser.inline;

import com.uqlism.emoji_deco.text.mfm.parser.core.FoundType;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserContext;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult;
import com.uqlism.emoji_deco.text.mfm.parser.core.RegexParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.SequentialParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.StringParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.SubstringFoundInfo;
import com.uqlism.emoji_deco.text.mfm.parser.core.charsequence.AlternateScanningParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.fixed.SpaceParser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult.failure;
import static com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult.success;

/**
 * Ported from mfm.kt's {@code api/src/main/kotlin/com/github/samunohito/mfm/api/parser/inline/ItalicUnderParser.kt}
 *
 * <p>An {@link IMfmParser} implementation for detecting "italic" syntax. The string enclosed by
 * "_" will be the search result.
 *
 * <p>Notes: only allows characters matching {@code [a-z0-9 \t]} (case-insensitive); if the
 * character before the starting symbol matches {@code [a-z0-9]} (case-insensitive), it is not
 * recognized as italic formatting; the content cannot be left empty.
 */
public final class ItalicUnderParser implements IMfmParser {
  public static final ItalicUnderParser INSTANCE = new ItalicUnderParser();

  private static final Pattern REGEX_ALPHA_AND_NUMERIC_TAIL = Pattern.compile("[a-z0-9]$", Pattern.CASE_INSENSITIVE);
  private static final StringParser MARK = new StringParser("_");
  private static final SequentialParser PARSER = new SequentialParser(
      MARK,
      AlternateScanningParser.ofWhile(
          new RegexParser(Pattern.compile("[a-z0-9]", Pattern.CASE_INSENSITIVE)),
          SpaceParser.INSTANCE
      ),
      MARK
  );

  private ItalicUnderParser() {
  }

  @Override
  public IMfmParserResult find(String input, int startAt, IMfmParserContext context) {
    IMfmParserResult result = PARSER.find(input, startAt, context);
    if (!result.success()) {
      return failure();
    }

    // 直前が英数字だったら認識しない
    String beforeStr = input.substring(0, startAt);
    Matcher matcher = REGEX_ALPHA_AND_NUMERIC_TAIL.matcher(beforeStr);
    if (matcher.find()) {
      return failure();
    }

    SubstringFoundInfo contents = result.foundInfo().get(1);
    return success(
        FoundType.ItalicUnder,
        result.foundInfo().overallRange(),
        contents.contentRange(),
        result.foundInfo().resumeIndex()
    );
  }
}
