package com.uqlism.emoji_deco.text.mfm.parser.core;

import com.uqlism.emoji_deco.text.mfm.parser.core.utils.IntRange;

import static com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult.failure;
import static com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult.success;

/**
 * Ported from mfm.kt's {@code api/src/main/kotlin/com/github/samunohito/mfm/api/parser/IMfmParser.kt}
 *
 * <p>Interface for MFM parser. Used to find format in a given input string.
 *
 * <p>The Kotlin source nests {@code Optional}/{@code Not} as {@code private} inner classes; Java
 * interfaces cannot declare private member classes, so they are ported as package-private
 * top-level classes in this file instead.
 */
public interface IMfmParser {
  /**
   * Finds a substring in the input string starting from a given index.
   *
   * @param input The input string to search.
   * @param startAt The index to start searching from.
   * @param context Context shared between parsers during a single parse.
   * @return The result of the substring search as an {@link IMfmParserResult}.
   */
  IMfmParserResult find(String input, int startAt, IMfmParserContext context);

  /**
   * Returns an optional version of the current substring parser.
   * An optional parser will return a successful result even if the substring is not found.
   */
  default IMfmParser optional() {
    return new OptionalMfmParser(this);
  }

  /**
   * Returns a negative (not) version of the current MFM parser.
   * A negative parser will return a failure result if the substring is found.
   */
  default IMfmParser not() {
    return new NotMfmParser(this);
  }
}

/**
 * Finds a substring in the input string starting from a given index.
 * If the substring is not found, it returns a successful result with empty ranges.
 */
final class OptionalMfmParser implements IMfmParser {
  private final IMfmParser delegate;

  OptionalMfmParser(IMfmParser delegate) {
    this.delegate = delegate;
  }

  @Override
  public IMfmParserResult find(String input, int startAt, IMfmParserContext context) {
    IMfmParserResult result = delegate.find(input, startAt, context);
    if (result.success()) {
      return result;
    } else {
      return success(FoundType.Core, IntRange.EMPTY, IntRange.EMPTY, startAt);
    }
  }
}

/**
 * Finds a substring in the input string starting from a given index.
 * If the substring is found, it returns a failure result.
 */
final class NotMfmParser implements IMfmParser {
  private final IMfmParser delegate;

  NotMfmParser(IMfmParser delegate) {
    this.delegate = delegate;
  }

  @Override
  public IMfmParserResult find(String input, int startAt, IMfmParserContext context) {
    IMfmParserResult result = delegate.find(input, startAt, context);
    if (result.success()) {
      return failure();
    } else {
      return success(FoundType.Core, result.foundInfo());
    }
  }
}
