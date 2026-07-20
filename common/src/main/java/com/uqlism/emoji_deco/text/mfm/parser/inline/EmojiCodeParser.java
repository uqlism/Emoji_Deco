package com.uqlism.emoji_deco.text.mfm.parser.inline;

import com.uqlism.emoji_deco.text.mfm.parser.core.FoundType;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserContext;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult;
import com.uqlism.emoji_deco.text.mfm.parser.core.RegexParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.SequentialParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.StringParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.SubstringFoundInfo;
import com.uqlism.emoji_deco.text.mfm.parser.core.fixed.LineBeginParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.fixed.LineEndParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.utils.IntRange;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult.failure;
import static com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult.success;

/**
 * Ported from mfm.kt's {@code api/src/main/kotlin/com/github/samunohito/mfm/api/parser/inline/EmojiCodeParser.kt}
 *
 * <p>An {@link IMfmParser} implementation for detecting "emoji code" syntax. The string enclosed
 * by ":" will be the search result.
 *
 * <p>Notes: only allows characters matching {@code [a-z0-9_+-]} (case-insensitive); the content
 * cannot be left empty.
 */
public final class EmojiCodeParser implements IMfmParser {
  public static final EmojiCodeParser INSTANCE = new EmojiCodeParser();

  private static final Pattern REGEX_SIDE = Pattern.compile("[a-z0-9]", Pattern.CASE_INSENSITIVE);
  private static final StringParser MARK = new StringParser(":");
  private static final SequentialParser PARSER = new SequentialParser(
      LineBeginParser.INSTANCE.optional(),
      MARK,
      new SequentialParser(new RegexParser(Pattern.compile("[a-z0-9_+-]+", Pattern.CASE_INSENSITIVE))),
      MARK,
      LineEndParser.INSTANCE.optional()
  );

  private EmojiCodeParser() {
  }

  @Override
  public IMfmParserResult find(String input, int startAt, IMfmParserContext context) {
    IMfmParserResult result = PARSER.find(input, startAt, context);
    if (!result.success()) {
      return failure();
    }

    if (!validateBeforeSide(input, startAt, result.foundInfo()) && !validateAfterSide(input, result.foundInfo())) {
      // 両脇がコロンだった場合は絵文字コードとして認識しない
      return failure();
    }

    SubstringFoundInfo contents = result.foundInfo().get(2);
    return success(
        FoundType.EmojiCode,
        result.foundInfo().overallRange(),
        contents.contentRange(),
        result.foundInfo().resumeIndex()
    );
  }

  private boolean validateBeforeSide(String input, int startAt, SubstringFoundInfo foundInfo) {
    // コロンの直前が半角英数の場合は絵文字コードとして認識しない
    if (startAt >= 1) {
      int idx = foundInfo.contentRange().start();
      String beforeStr = input.substring(idx - 1, idx);
      Matcher matcher = REGEX_SIDE.matcher(beforeStr);
      if (matcher.find()) {
        return false;
      }
    }

    return true;
  }

  private boolean validateAfterSide(String input, SubstringFoundInfo foundInfo) {
    // コロンの直後が半角英数の場合は絵文字コードとして認識しない
    IntRange contentRange = foundInfo.contentRange();
    if ((input.length() - 1) >= contentRange.last() + 1) {
      // 範囲を外れた次の1文字目を取りたいので+1している
      int idx = contentRange.last() + 1;
      String afterStr = input.substring(idx, idx + 1);
      Matcher matcher = REGEX_SIDE.matcher(afterStr);
      if (matcher.find()) {
        return false;
      }
    }

    return true;
  }
}
