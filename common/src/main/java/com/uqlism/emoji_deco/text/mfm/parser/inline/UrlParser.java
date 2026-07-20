package com.uqlism.emoji_deco.text.mfm.parser.inline;

import com.uqlism.emoji_deco.text.mfm.parser.core.AlternateParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.FoundType;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserContext;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult;
import com.uqlism.emoji_deco.text.mfm.parser.core.ISubIndex;
import com.uqlism.emoji_deco.text.mfm.parser.core.RegexParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.SequentialParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.SubstringFoundInfo;
import com.uqlism.emoji_deco.text.mfm.parser.core.utils.IntRange;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult.failure;
import static com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult.success;

/**
 * Ported from mfm.kt's {@code api/src/main/kotlin/com/github/samunohito/mfm/api/parser/inline/UrlParser.kt}
 *
 * <p>An {@link IMfmParser} implementation for detecting "URL" syntax. Strings that match the
 * pattern {@code https?://...} are the search results.
 *
 * <p>Notes: the content can contain characters matching {@code [.,a-z0-9_/:%#@$&?!~=+-]}
 * (case-insensitive); parentheses can be used in the content if they come in pairs (relevant
 * pairs: {@code ()[]}); "." or "," cannot be the last character.
 */
public final class UrlParser implements IMfmParser {
  public static final UrlParser INSTANCE = new UrlParser();

  private static final Pattern REGEX_COMMA_AND_PERIOD_TAIL = Pattern.compile("[.,]+$");
  private static final SequentialParser URL_PARSER = new SequentialParser(
      new RegexParser(Pattern.compile("https?://")),
      UrlBodyParser.INSTANCE
  );

  private UrlParser() {
  }

  @Override
  public IMfmParserResult find(String input, int startAt, IMfmParserContext context) {
    IMfmParserResult result = URL_PARSER.find(input, startAt, context);
    if (!result.success()) {
      return failure();
    }

    IMfmParserResult proceedResult = processTrailingPeriodAndComma(input, result);
    if (!proceedResult.success()) {
      return failure();
    }

    return success(
        FoundType.Url,
        proceedResult.foundInfo().overallRange(),
        proceedResult.foundInfo().contentRange(),
        proceedResult.foundInfo().resumeIndex(),
        proceedResult.foundInfo().nestedInfos()
    );
  }

  /** URLの末尾にピリオドやカンマがある場合は、それを除外した範囲を作成する */
  private IMfmParserResult processTrailingPeriodAndComma(String input, IMfmParserResult parserResult) {
    SubstringFoundInfo foundInfo = parserResult.foundInfo();
    SubstringFoundInfo body = foundInfo.get(1);
    String extractUrlBody = input.substring(body.contentRange().start(), body.contentRange().endExclusive());
    Matcher matcher = REGEX_COMMA_AND_PERIOD_TAIL.matcher(extractUrlBody);
    if (!matcher.find()) {
      // 末尾にピリオドやカンマがない場合はそのまま返す
      return parserResult;
    }

    if (extractUrlBody.length() - matcher.group().length() <= 0) {
      // スキーマ形式で始まるが、それ以降がカンマとピリオドのみの場合はスキーマ形式として認識しない
      return failure();
    }

    // parserResult の段階でuntilされているので、ここではやらない（多重にやると範囲がおかしくなる）
    IntRange contentRange = foundInfo.contentRange();
    IntRange modifyRange = new IntRange(contentRange.start(), contentRange.last() - matcher.group().length() + 1);
    return success(FoundType.Url, modifyRange, modifyRange, modifyRange.next());
  }

  private static final class UrlBodyParser implements IMfmParser {
    static final UrlBodyParser INSTANCE = new UrlBodyParser();

    private static final RegexParser OPEN_REGEX_BRACKET = new RegexParser(Pattern.compile("([(\\[])"));
    private static final RegexParser CLOSE_REGEX_BRACKET = new RegexParser(Pattern.compile("([)\\]])"));
    private static final AlternateParser NESTABLE_PARSER = new AlternateParser(
        // パターンにカッコを含めると、終了カッコのみを誤検出してしまう。
        // …ので、サポートされている種類の開始・終了カッコが揃っている場合は、その中身を再帰的に検索する
        new SequentialParser(OPEN_REGEX_BRACKET, INSTANCE, CLOSE_REGEX_BRACKET),
        // このパターンに合致する文字が登場するまでを繰り返し検索する
        new RegexParser(Pattern.compile("[.,a-zA-Z0-9_/:%#@\\\\$&?!~=+\\-]+"))
    );

    private UrlBodyParser() {
    }

    @Override
    public IMfmParserResult find(String input, int startAt, IMfmParserContext context) {
      int latestIndex = startAt;
      List<SubstringFoundInfo> foundInfos = new ArrayList<>();

      while (true) {
        IMfmParserResult result = NESTABLE_PARSER.find(input, latestIndex, context);
        if (!result.success()) {
          break;
        }

        foundInfos.add(result.foundInfo());
        latestIndex = result.foundInfo().resumeIndex();
      }

      if (foundInfos.isEmpty()) {
        return failure();
      }

      IntRange fullRange = new IntRange(startAt, latestIndex);
      IntRange resultRange = IntRange.merge(foundInfos.stream().map(SubstringFoundInfo::contentRange).toList());
      return success(FoundType.Url, fullRange, resultRange, fullRange.next(), foundInfos);
    }
  }

  public enum SubIndex implements ISubIndex {
    Scheme(0),
    Body(1);

    private final int index;

    SubIndex(int index) {
      this.index = index;
    }

    @Override
    public int index() {
      return index;
    }
  }
}
