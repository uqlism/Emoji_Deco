package com.uqlism.emoji_deco.text.mfm.parser.inline;

import com.uqlism.emoji_deco.text.mfm.parser.core.AlternateParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.FoundType;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserContext;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult;
import com.uqlism.emoji_deco.text.mfm.parser.core.RegexParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.SequentialParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.StringParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.SubstringFoundInfo;
import com.uqlism.emoji_deco.text.mfm.parser.core.charsequence.AlternateScanningParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.fixed.NewLineParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.fixed.SpaceParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.utils.IntRange;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult.failure;
import static com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult.success;

/**
 * Ported from mfm.kt's {@code api/src/main/kotlin/com/github/samunohito/mfm/api/parser/inline/HashtagParser.kt}
 *
 * <p>An {@link IMfmParser} implementation for detecting "hashtag" syntax. Strings starting with
 * "#" will be search results.
 *
 * <p>Notes: the content cannot be left empty; the content cannot contain half-width spaces,
 * full-width spaces, newlines, or tab characters; the content cannot include
 * {@code . , ! ? ' " # : /} or bracket characters; parentheses can only be included in the
 * content when they come in pairs (relevant pairs: {@code () [] 「」 （）}); recognized as a
 * hashtag only if the character before "#" does not match {@code [a-z0-9]} (case-insensitive);
 * if the content is only numbers, it will not be recognized as a hashtag.
 */
public final class HashtagParser implements IMfmParser {
  public static final HashtagParser INSTANCE = new HashtagParser();

  private static final Pattern REGEX_ALPHA_AND_NUMERIC_TAIL = Pattern.compile("[a-z0-9]$", Pattern.CASE_INSENSITIVE);
  private static final Pattern REGEX_NUMERIC_ONLY = Pattern.compile("^[0-9]+$");
  private static final StringParser MARK = new StringParser("#");
  private static final SequentialParser HASHTAG_PARSER = new SequentialParser(MARK, HashtagBodyParser.INSTANCE);

  private HashtagParser() {
  }

  @Override
  public IMfmParserResult find(String input, int startAt, IMfmParserContext context) {
    IMfmParserResult result = HASHTAG_PARSER.find(input, startAt, context);
    if (!result.success()) {
      return failure();
    }

    // ハッシュタグの直前が英数字の場合はハッシュタグとして認識しない
    String beforeStr = input.substring(0, startAt);
    if (REGEX_ALPHA_AND_NUMERIC_TAIL.matcher(beforeStr).find()) {
      return failure();
    }

    // 検出された文字が数値のみの場合はハッシュタグではない
    SubstringFoundInfo hashtagNameResult = result.foundInfo().get(1);
    String hashtagName = input.substring(hashtagNameResult.contentRange().start(), hashtagNameResult.contentRange().endExclusive());
    if (REGEX_NUMERIC_ONLY.matcher(hashtagName).find()) {
      return failure();
    }

    return success(
        FoundType.Hashtag,
        result.foundInfo().overallRange(),
        hashtagNameResult.contentRange(),
        result.foundInfo().resumeIndex()
    );
  }

  private static final class HashtagBodyParser implements IMfmParser {
    static final HashtagBodyParser INSTANCE = new HashtagBodyParser();

    private static final RegexParser OPEN_REGEX_BRACKET = new RegexParser(Pattern.compile("[(\\[「（]"));
    private static final RegexParser CLOSE_REGEX_BRACKET = new RegexParser(Pattern.compile("[)\\]」）]"));
    private static final AlternateParser NESTABLE_PARSER = new AlternateParser(
        // パターンにカッコを含めると、終了カッコのみを誤検出してしまう。
        // …ので、サポートされている種類の開始・終了カッコが揃っている場合は、その中身を再帰的に検索する
        new SequentialParser(OPEN_REGEX_BRACKET, INSTANCE, CLOSE_REGEX_BRACKET),
        // このパターンに合致する文字が登場するまでを繰り返し検索する
        AlternateScanningParser.ofUntil(
            SpaceParser.INSTANCE,
            NewLineParser.INSTANCE,
            new RegexParser(Pattern.compile("[ 　\\t.,!?'\"#:/\\[\\]【】()「」（）<>]"))
        )
    );

    private HashtagBodyParser() {
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
      IntRange contentRange = IntRange.merge(foundInfos.stream().map(SubstringFoundInfo::contentRange).toList());
      return success(FoundType.Hashtag, fullRange, contentRange, fullRange.next(), foundInfos);
    }
  }
}
