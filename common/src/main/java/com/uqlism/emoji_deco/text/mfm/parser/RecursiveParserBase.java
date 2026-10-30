package com.uqlism.emoji_deco.text.mfm.parser;

import com.uqlism.emoji_deco.text.mfm.parser.core.FoundType;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserContext;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult;
import com.uqlism.emoji_deco.text.mfm.parser.core.SubstringFoundInfo;
import com.uqlism.emoji_deco.text.mfm.parser.core.utils.IntRange;

import java.util.ArrayList;
import java.util.List;

import static com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult.failure;
import static com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult.success;

/**
 * Ported from mfm.kt's {@code api/src/main/kotlin/com/github/samunohito/mfm/api/parser/RecursiveParserBase.kt}
 *
 * <p>Base implementation for detecting nested MFM forms within MFM forms. Parses recursively
 * until {@code terminateParser} is satisfied. Note that the {@link IMfmParser}s registered in
 * {@link IMfmParserContext#excludeParsers()}, which are passed to {@link IMfmParser#find}, are
 * not used during analysis.
 */
public abstract class RecursiveParserBase implements IMfmParser {
  private final IMfmParser terminateParser;

  protected RecursiveParserBase(IMfmParser terminateParser) {
    this.terminateParser = terminateParser;
  }

  protected abstract List<IMfmParser> parsers();

  protected abstract FoundType foundType();

  @Override
  public final IMfmParserResult find(String input, int startAt, IMfmParserContext context) {
    int textNodeStartAt = startAt;
    int latestIndex = startAt;
    List<SubstringFoundInfo> foundInfos = new ArrayList<>();

    while (!shouldTerminate(input, latestIndex, context)) {
      IMfmParserResult findResult = findWithParsers(input, latestIndex, context);
      if (findResult.success()) {
        if (textNodeStartAt != latestIndex) {
          // 前回見つかったノードと今回見つかったノードの間にある文字たちをTextノードとして登録する
          IntRange range = new IntRange(textNodeStartAt, latestIndex);
          foundInfos.add(new SubstringFoundInfo(FoundType.Text, range, range, range.next()));
        }
        foundInfos.add(findResult.foundInfo());

        textNodeStartAt = findResult.foundInfo().resumeIndex();
        latestIndex = findResult.foundInfo().resumeIndex();
      } else {
        latestIndex++;
      }
    }

    if (startAt == latestIndex) {
      return failure();
    } else {
      if (textNodeStartAt != latestIndex) {
        // ここに来てprevとlatestに差がある場合、リストに登録してないTextが存在するということ
        IntRange range = new IntRange(textNodeStartAt, latestIndex);
        foundInfos.add(new SubstringFoundInfo(FoundType.Text, range, range, range.next()));
      }

      IntRange fullRange = new IntRange(startAt, latestIndex);
      IntRange contentRange = IntRange.merge(foundInfos.stream().map(SubstringFoundInfo::contentRange).toList());
      return success(foundType(), fullRange, contentRange, fullRange.next(), foundInfos);
    }
  }

  private boolean shouldTerminate(String input, int latestIndex, IMfmParserContext context) {
    if (input.length() < latestIndex) {
      return true;
    }

    return terminateParser.find(input, latestIndex, context).success();
  }

  private IMfmParserResult findWithParsers(String input, int latestIndex, IMfmParserContext context) {
    for (IMfmParser parser : parsers()) {
      IMfmParserResult result = context.excludeParsers().contains(parser.getClass())
          ? failure()
          : parser.find(input, latestIndex, context);

      if (result.success()) {
        return result;
      }
    }
    return failure();
  }
}
