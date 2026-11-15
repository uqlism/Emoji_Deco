package com.uqlism.emoji_deco.text.mfm.parser.inline;

import com.uqlism.emoji_deco.text.mfm.parser.InlineParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.FoundType;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserContext;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult;
import com.uqlism.emoji_deco.text.mfm.parser.core.ISubIndex;
import com.uqlism.emoji_deco.text.mfm.parser.core.RegexParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.SequentialParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.StringParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.SubstringFoundInfo;
import com.uqlism.emoji_deco.text.mfm.parser.core.utils.IntRange;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult.failure;
import static com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult.success;

/**
 * Ported from mfm.kt's {@code api/src/main/kotlin/com/github/samunohito/mfm/api/parser/inline/FnParser.kt}
 *
 * <p>An {@link IMfmParser} implementation for detecting "function" syntax. Strings that match the
 * pattern {@code $[functionName.arg1=value,arg2=value contents]} will be the search results.
 *
 * <p>Notes: applies {@link InlineParser} to the content again to recursively detect inline
 * syntax; any character and newline can be used in the content; the content cannot be left empty.
 */
public final class FnParser implements IMfmParser {
  public static final FnParser INSTANCE = new FnParser();

  private static final StringParser OPEN = new StringParser("$[");
  private static final StringParser CLOSE = new StringParser("]");
  private static final RegexParser NAME_PARSER = new RegexParser(Pattern.compile("[a-z0-9_]+", Pattern.CASE_INSENSITIVE));
  private static final SequentialParser ARGS_PARSER = new SequentialParser(new StringParser("."), ArgsParser.INSTANCE);
  private static final SequentialParser FUNC_PARSER = new SequentialParser(
      OPEN,
      NAME_PARSER,
      ARGS_PARSER.optional(),
      new StringParser(" "),
      new InlineParser(CLOSE),
      CLOSE
  );

  private FnParser() {
  }

  @Override
  public IMfmParserResult find(String input, int startAt, IMfmParserContext context) {
    IMfmParserResult result = FUNC_PARSER.find(input, startAt, context);
    if (!result.success()) {
      return failure();
    }

    SubstringFoundInfo name = result.foundInfo().get(1);
    SubstringFoundInfo argsGroup = result.foundInfo().get(2);
    SubstringFoundInfo args = argsGroup.contentRange().start() == argsGroup.contentRange().endExclusive()
        ? SubstringFoundInfo.EMPTY
        // 先頭要素は関数名との区切りに使っているピリオドなので不要
        : argsGroup.get(1);
    SubstringFoundInfo content = result.foundInfo().get(4);

    return success(
        FoundType.Fn,
        result.foundInfo().overallRange(),
        result.foundInfo().contentRange(),
        result.foundInfo().resumeIndex(),
        List.of(name, args, content)
    );
  }

  private static final class ArgsParser implements IMfmParser {
    static final ArgsParser INSTANCE = new ArgsParser();

    private static final SequentialParser ARG_PARSER = new SequentialParser(
        new RegexParser(Pattern.compile("[a-z0-9_]+", Pattern.CASE_INSENSITIVE)),
        new SequentialParser(
            new StringParser("="),
            new RegexParser(Pattern.compile("[a-z0-9_.-]+", Pattern.CASE_INSENSITIVE))
        ).optional()
    );
    private static final StringParser ARG_SEPARATOR = new StringParser(",");

    private ArgsParser() {
    }

    @Override
    public IMfmParserResult find(String input, int startAt, IMfmParserContext context) {
      int latestIndex = startAt;
      List<SubstringFoundInfo> args = new ArrayList<>();
      while (true) {
        IMfmParserResult argResult = ARG_PARSER.find(input, latestIndex, context);
        if (argResult.success()) {
          args.add(argResult.foundInfo());
          latestIndex = argResult.foundInfo().resumeIndex();
        } else {
          IMfmParserResult separatorResult = ARG_SEPARATOR.find(input, latestIndex, context);
          if (separatorResult.success()) {
            latestIndex = separatorResult.foundInfo().resumeIndex();
          } else {
            break;
          }
        }
      }

      IntRange fullRange = new IntRange(startAt, latestIndex);
      IntRange contentRange = IntRange.merge(args.stream().map(SubstringFoundInfo::contentRange).toList());
      return success(FoundType.Fn, fullRange, contentRange, fullRange.next(), args);
    }
  }

  public enum SubIndex implements ISubIndex {
    Name(0),
    Args(1),
    Content(2);

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
