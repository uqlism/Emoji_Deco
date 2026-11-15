package com.uqlism.emoji_deco.text.mfm.parser.core;

import com.uqlism.emoji_deco.text.mfm.parser.core.utils.IntRange;

import java.util.List;

/**
 * Ported from mfm.kt's {@code api/src/main/kotlin/com/github/samunohito/mfm/api/parser/SubstringFoundInfo.kt}
 *
 * <p>Represents information about a found substring.
 *
 * @param type The type of the found substring.
 * @param overallRange The range of the found substring in the overall string. The "whole string"
 *     is the range that includes the format string (e.g. symbols such as #, @, *, etc.).
 * @param contentRange The range of the found substring in the actual content. The "actual content"
 *     is a range that does not contain format strings (e.g. symbols like #, @, *, etc.).
 * @param resumeIndex The restart index of the found substring. This index makes it easier to
 *     restart searches.
 * @param nestedInfos The list of nested {@link SubstringFoundInfo} objects.
 */
public record SubstringFoundInfo(
    FoundType type,
    IntRange overallRange,
    IntRange contentRange,
    int resumeIndex,
    List<SubstringFoundInfo> nestedInfos
) {
  /** A {@link SubstringFoundInfo} object to treat as empty. */
  public static final SubstringFoundInfo EMPTY = new SubstringFoundInfo(
      FoundType.Empty,
      IntRange.EMPTY,
      IntRange.EMPTY,
      Integer.MIN_VALUE,
      List.of()
  );

  /** Convenience constructor defaulting {@code nestedInfos} to an empty list. */
  public SubstringFoundInfo(FoundType type, IntRange overallRange, IntRange contentRange, int resumeIndex) {
    this(type, overallRange, contentRange, resumeIndex, List.of());
  }

  /** Creates a new {@link SubstringFoundInfo} object re-typing an existing one. */
  public SubstringFoundInfo(FoundType type, SubstringFoundInfo info) {
    this(type, info.overallRange(), info.contentRange(), info.resumeIndex(), info.nestedInfos());
  }

  /**
   * Get the element at {@code index} in {@link #nestedInfos()}.
   * This is to support a notation for simpler access to {@link #nestedInfos()}.
   */
  public SubstringFoundInfo get(int index) {
    return nestedInfos.get(index);
  }

  /**
   * Get the element at {@code index} in {@link #nestedInfos()}.
   * This is to support a notation for simpler access to {@link #nestedInfos()}.
   */
  public SubstringFoundInfo get(ISubIndex index) {
    return nestedInfos.get(index.index());
  }
}
