package com.uqlism.emoji_deco.text.mfm.parser.core.charsequence;

import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserContext;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult;
import com.uqlism.emoji_deco.text.mfm.parser.core.utils.ParserUtils;

import java.util.List;

/**
 * Ported from mfm.kt's
 * {@code api/src/main/kotlin/com/github/samunohito/mfm/api/parser/core/charsequence/AlternateScanningParser.kt}
 *
 * <p>Implementation of {@link ScanningParserBase}. As long as any of the {@code terminates}
 * returns success, {@code hasNext} will be true. This behaviour can be reversed (while-&gt;until)
 * by using {@link #ofUntil}.
 */
public final class AlternateScanningParser extends ScanningParserBase {
  private final List<IMfmParser> terminates;
  private final boolean isWhile;

  private AlternateScanningParser(List<IMfmParser> terminates, boolean isWhile) {
    this.terminates = List.copyOf(terminates);
    this.isWhile = isWhile;
  }

  @Override
  protected boolean hasNext(String text, int startAt, IMfmParserContext context) {
    IMfmParserResult result = ParserUtils.alternate(text, startAt, terminates, context);
    return isWhile == result.success();
  }

  public static AlternateScanningParser ofWhile(List<IMfmParser> terminates) {
    return new AlternateScanningParser(terminates, true);
  }

  public static AlternateScanningParser ofWhile(IMfmParser... terminates) {
    return new AlternateScanningParser(List.of(terminates), true);
  }

  public static AlternateScanningParser ofUntil(List<IMfmParser> terminates) {
    return new AlternateScanningParser(terminates, false);
  }

  public static AlternateScanningParser ofUntil(IMfmParser... terminates) {
    return new AlternateScanningParser(List.of(terminates), false);
  }
}
