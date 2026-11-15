package com.uqlism.emoji_deco.text.mfm.parser.core;

import com.uqlism.emoji_deco.text.mfm.parser.core.utils.IntRange;

import java.util.List;

/**
 * Ported from mfm.kt's {@code api/src/main/kotlin/com/github/samunohito/mfm/api/parser/IMfmParserResult.kt}
 *
 * <p>Interface for the result of an MFM parser operation. The Kotlin file's top-level factory
 * functions ({@code success}/{@code failure}) are ported here as static methods.
 */
public interface IMfmParserResult {
  /** Returns true if the substring was found successfully, false otherwise. */
  boolean success();

  /** Returns information about the substring that was found, if any. */
  SubstringFoundInfo foundInfo();

  /** Creates an instance of {@link IMfmParserResult} with a success flag and found information. */
  static IMfmParserResult success(SubstringFoundInfo result) {
    return new Impl(true, result);
  }

  /**
   * Creates an instance of {@link IMfmParserResult} with a success flag, found information, and
   * no nested information.
   */
  static IMfmParserResult success(FoundType foundType, IntRange overallRange, IntRange contentRange, int resumeIndex) {
    return success(foundType, overallRange, contentRange, resumeIndex, List.of());
  }

  /**
   * Creates an instance of {@link IMfmParserResult} with a success flag, found information, and a
   * list of nested information.
   *
   * @see SubstringFoundInfo
   */
  static IMfmParserResult success(
      FoundType foundType,
      IntRange overallRange,
      IntRange contentRange,
      int resumeIndex,
      List<SubstringFoundInfo> nestedInfos
  ) {
    return new Impl(true, new SubstringFoundInfo(foundType, overallRange, contentRange, resumeIndex, nestedInfos));
  }

  /** Creates an instance of {@link IMfmParserResult} with a success flag and a nested result. */
  static IMfmParserResult success(FoundType foundType, IMfmParserResult result) {
    return new Impl(true, new SubstringFoundInfo(foundType, result.foundInfo()));
  }

  /** Creates an instance of {@link IMfmParserResult} with a success flag and found information. */
  static IMfmParserResult success(FoundType foundType, SubstringFoundInfo info) {
    return new Impl(true, new SubstringFoundInfo(foundType, info));
  }

  /** Creates an instance of {@link IMfmParserResult} with a failure flag and empty found information. */
  static IMfmParserResult failure() {
    return new Impl(false, SubstringFoundInfo.EMPTY);
  }

  /** Private implementation record for {@link IMfmParserResult}. */
  record Impl(boolean success, SubstringFoundInfo foundInfo) implements IMfmParserResult {
  }
}
