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
import com.uqlism.emoji_deco.text.mfm.parser.core.utils.IntRange;

import java.util.List;
import java.util.regex.Pattern;

import static com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult.failure;
import static com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult.success;

/**
 * Ported from mfm.kt's {@code api/src/main/kotlin/com/github/samunohito/mfm/api/parser/inline/UrlAltParser.kt}
 *
 * <p>An {@link IMfmParser} implementation for detecting "URL" syntax. Search results match
 * patterns where URLs are enclosed in angle brackets, e.g. {@code <http://example.com/>}.
 *
 * <p>Notes: allows https as well as http; any characters other than line breaks and spaces can be
 * used in the content.
 */
public final class UrlAltParser implements IMfmParser {
  public static final UrlAltParser INSTANCE = new UrlAltParser();

  private static final StringParser OPEN = new StringParser("<");
  private static final StringParser CLOSE = new StringParser(">");
  private static final RegexParser SCHEMA = new RegexParser(Pattern.compile("https?://"));
  private static final SequentialParser PARSER = new SequentialParser(
      OPEN,
      SCHEMA,
      AlternateScanningParser.ofUntil(CLOSE),
      CLOSE
  );

  private UrlAltParser() {
  }

  @Override
  public IMfmParserResult find(String input, int startAt, IMfmParserContext context) {
    // 開始・終了のブラケットが見つかるまで
    IMfmParserResult scanResult = PARSER.find(input, startAt, context);
    if (!scanResult.success()) {
      return failure();
    }

    SubstringFoundInfo schema = scanResult.foundInfo().get(1);
    SubstringFoundInfo body = scanResult.foundInfo().get(2);
    IntRange urlRange = IntRange.merge(List.of(schema.contentRange(), body.contentRange()));
    return success(FoundType.UrlAlt, scanResult.foundInfo().overallRange(), urlRange, scanResult.foundInfo().resumeIndex());
  }
}
