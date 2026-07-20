package com.uqlism.emoji_deco.text.mfm.parser.core.charsequence;

import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserContext;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult;
import com.uqlism.emoji_deco.text.mfm.parser.core.utils.ParserUtils;

import java.util.List;

/**
 * Ported from mfm.kt's
 * {@code api/src/main/kotlin/com/github/samunohito/mfm/api/parser/core/charsequence/SequentialScanningParser.kt}
 *
 * <p>Implementation of {@link ScanningParserBase}. {@code hasNext} becomes false once all {@code
 * terminates} return success; otherwise it is true.
 */
public final class SequentialScanningParser extends ScanningParserBase {
  private final List<IMfmParser> terminates;

  private SequentialScanningParser(List<IMfmParser> terminates) {
    this.terminates = List.copyOf(terminates);
  }

  @Override
  protected boolean hasNext(String text, int startAt, IMfmParserContext context) {
    IMfmParserResult result = ParserUtils.sequential(text, startAt, terminates, context);
    return !result.success();
  }

  public static SequentialScanningParser ofUntil(List<IMfmParser> terminates) {
    return new SequentialScanningParser(terminates);
  }

  public static SequentialScanningParser ofUntil(IMfmParser... terminates) {
    return new SequentialScanningParser(List.of(terminates));
  }
}
