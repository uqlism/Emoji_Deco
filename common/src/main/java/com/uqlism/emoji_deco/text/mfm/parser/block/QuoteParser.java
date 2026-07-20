package com.uqlism.emoji_deco.text.mfm.parser.block;

import com.uqlism.emoji_deco.text.mfm.parser.core.FoundType;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserContext;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult;
import com.uqlism.emoji_deco.text.mfm.parser.core.SequentialParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.StringParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.SubstringFoundInfo;
import com.uqlism.emoji_deco.text.mfm.parser.core.charsequence.SequentialScanningParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.fixed.LineBeginParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.fixed.NewLineParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.fixed.SpaceParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.utils.IntRange;

import java.util.ArrayList;
import java.util.List;

import static com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult.failure;
import static com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult.success;

/**
 * Ported from mfm.kt's {@code api/src/main/kotlin/com/github/samunohito/mfm/api/parser/block/QuoteParser.kt}
 *
 * <p>An {@link IMfmParser} implementation for detecting "quote" syntax. The string enclosed by
 * ">" will be the search result.
 *
 * <p>Notes: performs recursive parsing on quoted content (not only inline syntax but also block
 * syntax is covered); ignores 0-1 spaces after '&gt;'; lines of adjacent citations are merged;
 * blank lines can be included in multi-line citations; empty lines after quotes are ignored.
 */
public final class QuoteParser implements IMfmParser {
  public static final QuoteParser INSTANCE = new QuoteParser();

  private static final SequentialParser QUOTE_PARSER = new SequentialParser(
      NewLineParser.INSTANCE.optional(),
      NewLineParser.INSTANCE.optional(),
      LineBeginParser.INSTANCE,
      QuoteLinesParser.INSTANCE,
      NewLineParser.INSTANCE.optional(),
      NewLineParser.INSTANCE.optional()
  );

  private QuoteParser() {
  }

  @Override
  public IMfmParserResult find(String input, int startAt, IMfmParserContext context) {
    IMfmParserResult result = QUOTE_PARSER.find(input, startAt, context);
    if (!result.success()) {
      return failure();
    }

    SubstringFoundInfo contents = result.foundInfo().get(3);
    return success(
        FoundType.Quote,
        result.foundInfo().overallRange(),
        contents.contentRange(),
        result.foundInfo().resumeIndex(),
        contents.nestedInfos()
    );
  }

  private static final class QuoteLinesParser implements IMfmParser {
    static final QuoteLinesParser INSTANCE = new QuoteLinesParser();

    private static final SequentialParser ONE_LINE_PARSER = new SequentialParser(
        new StringParser(">"),
        SpaceParser.INSTANCE.optional(),
        SequentialScanningParser.ofUntil(NewLineParser.INSTANCE).optional(),
        NewLineParser.INSTANCE.optional()
    );

    private QuoteLinesParser() {
    }

    @Override
    public IMfmParserResult find(String input, int startAt, IMfmParserContext context) {
      int latestIndex = startAt;
      List<SubstringFoundInfo> lines = new ArrayList<>();
      while (true) {
        IMfmParserResult result = ONE_LINE_PARSER.find(input, latestIndex, context);
        if (!result.success()) {
          break;
        }

        latestIndex = result.foundInfo().resumeIndex();

        // 引用符を省いた本文部分だけを蓄積したい
        lines.add(result.foundInfo().get(2));
      }

      if (startAt == latestIndex || lines.isEmpty()) {
        // 引用符つきの行が見当たらないときは認識しない
        return failure();
      }

      IntRange firstLineContent = lines.get(0).contentRange();
      if (lines.size() == 1 && firstLineContent.start() == firstLineContent.endExclusive()) {
        // 引用符つきの行が1行のみで、なおかつ引用符のみの行だったときは認識しない
        return failure();
      }

      IntRange range = new IntRange(startAt, latestIndex);
      return success(FoundType.Quote, range, range, range.next(), lines);
    }
  }
}
