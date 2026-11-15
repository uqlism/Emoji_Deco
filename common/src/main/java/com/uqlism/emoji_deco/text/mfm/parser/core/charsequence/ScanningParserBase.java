package com.uqlism.emoji_deco.text.mfm.parser.core.charsequence;

import com.uqlism.emoji_deco.text.mfm.parser.core.FoundType;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParser;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserContext;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult;
import com.uqlism.emoji_deco.text.mfm.parser.core.utils.IntRange;

import static com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult.failure;
import static com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult.success;

/**
 * Ported from mfm.kt's
 * {@code api/src/main/kotlin/com/github/samunohito/mfm/api/parser/core/charsequence/ScanningParserBase.kt}
 *
 * <p>An implementation of {@link IMfmParser} that searches the input string until {@link
 * #hasNext} returns false. Starts counting from the search start position and increments the
 * counter if {@link #hasNext} is true. If {@link #hasNext} is false, the range from the search
 * start position to the position of the counter is returned as search results.
 */
public abstract class ScanningParserBase implements IMfmParser {
  @Override
  public final IMfmParserResult find(String input, int startAt, IMfmParserContext context) {
    int latestIndex = startAt;

    for (int i = startAt; i < input.length(); i++) {
      if (!hasNext(input, i, context)) {
        break;
      }

      latestIndex++;
    }

    if (startAt == latestIndex) {
      // 一回も成功していない場合は失敗扱い
      return failure();
    } else {
      IntRange range = new IntRange(startAt, latestIndex);
      return success(FoundType.Core, range, range, range.next());
    }
  }

  /** Returns true if the search should continue. */
  protected abstract boolean hasNext(String text, int startAt, IMfmParserContext context);
}
