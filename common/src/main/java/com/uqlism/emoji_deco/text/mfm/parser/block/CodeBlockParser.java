package com.uqlism.emoji_deco.text.mfm.parser.block;

import com.uqlism.emoji_deco.text.mfm.parser.core.FoundType;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserContext;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult;
import com.uqlism.emoji_deco.text.mfm.parser.core.ISubIndex;
import com.uqlism.emoji_deco.text.mfm.parser.core.SequentialParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.StringParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.SubstringFoundInfo;
import com.uqlism.emoji_deco.text.mfm.parser.core.charsequence.SequentialScanningParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.fixed.LineBeginParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.fixed.LineEndParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.fixed.NewLineParser;

import java.util.List;

import static com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult.failure;
import static com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult.success;

/**
 * Ported from mfm.kt's {@code api/src/main/kotlin/com/github/samunohito/mfm/api/parser/block/CodeBlockParser.kt}
 *
 * <p>An {@link IMfmParser} implementation for detecting "code block" syntax. The string enclosed
 * by "```" will be the search result.
 *
 * <p>Notes: nesting of MFM syntax is not possible (all content is interpreted as text); "```"
 * must be the beginning of the line.
 */
public final class CodeBlockParser implements IMfmParser {
  public static final CodeBlockParser INSTANCE = new CodeBlockParser();

  private static final StringParser MARK = new StringParser("```");
  private static final SequentialParser CODE_BLOCK_PARSER = new SequentialParser(
      NewLineParser.INSTANCE.optional(),
      LineBeginParser.INSTANCE,
      MARK,
      SequentialScanningParser.ofUntil(NewLineParser.INSTANCE).optional(),
      NewLineParser.INSTANCE,
      SequentialScanningParser.ofUntil(NewLineParser.INSTANCE, MARK, LineEndParser.INSTANCE),
      NewLineParser.INSTANCE,
      MARK,
      LineEndParser.INSTANCE,
      NewLineParser.INSTANCE.optional()
  );

  private CodeBlockParser() {
  }

  @Override
  public IMfmParserResult find(String input, int startAt, IMfmParserContext context) {
    IMfmParserResult result = CODE_BLOCK_PARSER.find(input, startAt, context);
    if (!result.success()) {
      return failure();
    }

    SubstringFoundInfo langResult = result.foundInfo().get(3);
    SubstringFoundInfo codeResult = result.foundInfo().get(5);
    boolean langEmpty = langResult.contentRange().start() == langResult.contentRange().endExclusive();
    return success(
        FoundType.CodeBlock,
        result.foundInfo().overallRange(),
        result.foundInfo().contentRange(),
        result.foundInfo().overallRange().next(),
        List.of(
            langEmpty
                ? SubstringFoundInfo.EMPTY
                : new SubstringFoundInfo(
                    FoundType.CodeBlock,
                    langResult.overallRange(),
                    langResult.contentRange(),
                    langResult.contentRange().next()
                ),
            new SubstringFoundInfo(
                FoundType.CodeBlock,
                codeResult.overallRange(),
                codeResult.contentRange(),
                codeResult.contentRange().next()
            )
        )
    );
  }

  public enum SubIndex implements ISubIndex {
    Lang(0),
    Code(1);

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
