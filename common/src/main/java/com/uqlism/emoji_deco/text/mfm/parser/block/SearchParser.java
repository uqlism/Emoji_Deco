package com.uqlism.emoji_deco.text.mfm.parser.block;

import com.uqlism.emoji_deco.text.mfm.parser.core.FoundType;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserContext;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult;
import com.uqlism.emoji_deco.text.mfm.parser.core.ISubIndex;
import com.uqlism.emoji_deco.text.mfm.parser.core.RegexParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.SequentialParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.SubstringFoundInfo;
import com.uqlism.emoji_deco.text.mfm.parser.core.fixed.LineBeginParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.fixed.LineEndParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.fixed.NewLineParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.fixed.SpaceParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.utils.IntRange;

import java.util.List;
import java.util.regex.Pattern;

import static com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult.success;

/**
 * Ported from mfm.kt's {@code api/src/main/kotlin/com/github/samunohito/mfm/api/parser/block/SearchParser.kt}
 *
 * <p>An {@link IMfmParser} implementation for detecting "search" syntax. Strings that match the
 * pattern {@code keyword [検索]} or {@code keyword [search]} are the search results.
 *
 * <p>Notes: the presence or absence of "[]" is not distinguished; "search" is case-insensitive.
 */
public final class SearchParser implements IMfmParser {
  public static final SearchParser INSTANCE = new SearchParser();

  private static final RegexParser BUTTON_PARSER =
      new RegexParser(Pattern.compile("\\[?(検索|search)]?", Pattern.CASE_INSENSITIVE));
  private static final SequentialParser SEARCH_FORM_PARSER = new SequentialParser(
      NewLineParser.INSTANCE.optional(),
      LineBeginParser.INSTANCE,
      QueryParser.INSTANCE,
      SpaceParser.INSTANCE,
      BUTTON_PARSER,
      LineEndParser.INSTANCE,
      NewLineParser.INSTANCE.optional()
  );

  private SearchParser() {
  }

  @Override
  public IMfmParserResult find(String input, int startAt, IMfmParserContext context) {
    IMfmParserResult result = SEARCH_FORM_PARSER.find(input, startAt, context);
    if (!result.success()) {
      return IMfmParserResult.failure();
    }

    SubstringFoundInfo query = result.foundInfo().get(2);
    SubstringFoundInfo space = result.foundInfo().get(3);
    SubstringFoundInfo button = result.foundInfo().get(4);

    List<SubstringFoundInfo> sub = List.of(query, space, button);
    SubstringFoundInfo info = new SubstringFoundInfo(
        FoundType.Search,
        result.foundInfo().overallRange(),
        IntRange.merge(sub.stream().map(SubstringFoundInfo::contentRange).toList()),
        result.foundInfo().resumeIndex(),
        sub
    );

    return success(info);
  }

  private static final class QueryParser implements IMfmParser {
    static final QueryParser INSTANCE = new QueryParser();

    private static final SequentialParser SEARCH_BUTTON_PARSER =
        new SequentialParser(SpaceParser.INSTANCE, BUTTON_PARSER, LineEndParser.INSTANCE);

    private QueryParser() {
    }

    @Override
    public IMfmParserResult find(String input, int startAt, IMfmParserContext context) {
      for (int i = startAt; i < input.length(); i++) {
        if (NewLineParser.INSTANCE.find(input, i, context).success()) {
          // 改行されていたら検索ボタン形式が破綻するので中断
          return IMfmParserResult.failure();
        }

        IMfmParserResult result = SEARCH_BUTTON_PARSER.find(input, i, context);
        if (result.success()) {
          // 削除ボタンの検出が成功したらクエリの範囲がわかるので、それを返す
          IntRange queryRange = new IntRange(startAt, i);
          return success(FoundType.Search, queryRange, queryRange, queryRange.next());
        }
      }

      // 検索ボタンが見つからなかった
      return IMfmParserResult.failure();
    }
  }

  public enum SubIndex implements ISubIndex {
    Query(0),
    Space(1),
    Button(2);

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
