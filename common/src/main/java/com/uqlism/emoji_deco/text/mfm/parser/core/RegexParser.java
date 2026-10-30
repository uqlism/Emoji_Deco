package com.uqlism.emoji_deco.text.mfm.parser.core;

import com.uqlism.emoji_deco.text.mfm.parser.core.utils.IntRange;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult.failure;
import static com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult.success;

/**
 * Ported from mfm.kt's {@code api/src/main/kotlin/com/github/samunohito/mfm/api/parser/core/RegexParser.kt}
 *
 * <p>An implementation of {@link IMfmParser} that searches for a regular expression in a given
 * string.
 */
public final class RegexParser implements IMfmParser {
  private final Pattern pattern;

  /**
   * @param regex A regular expression pattern to search for. The search will succeed only if
   *     there is a string that matches this pattern at the search start position.
   */
  public RegexParser(Pattern regex) {
    // 行頭フラグをつけておかないと、検索開始位置よりも後ろにある文字列に反応してしまって正しい検索結果にならない
    // (Without a line-start anchor, the pattern could match further along the string than the
    // search start position, producing an incorrect result.) Concatenated verbatim, without extra
    // grouping, to mirror Kotlin's `Regex("^${regex.pattern}", regex.options)` exactly (including
    // its quirk of only anchoring the first alternative of a top-level `|`).
    this.pattern = Pattern.compile("^" + regex.pattern(), regex.flags());
  }

  @Override
  public IMfmParserResult find(String input, int startAt, IMfmParserContext context) {
    String text = input.substring(startAt);
    Matcher matcher = pattern.matcher(text);
    if (!matcher.find()) {
      return failure();
    } else {
      IntRange resultRange = new IntRange(startAt, startAt + matcher.group().length());
      return success(FoundType.Core, resultRange, resultRange, resultRange.next());
    }
  }
}
