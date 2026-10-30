package com.uqlism.emoji_deco.text.mfm.parser.core.utils;

import java.util.List;

/**
 * Ported from mfm.kt's {@code api/src/main/kotlin/com/github/samunohito/mfm/api/utils/IntRangeUtils.kt}
 * and the semantics of Kotlin's {@code IntRange} type as used via {@code a until b} throughout the parser.
 *
 * <p>Represents a half-open interval {@code [start, endExclusive)}, mirroring the Kotlin idiom
 * {@code a until b}. {@link #next()} therefore always equals {@link #endExclusive()}, since Kotlin's
 * {@code IntRange.last} (inclusive) is {@code endExclusive - 1} and {@code calcNext} returns
 * {@code last + 1}.
 */
public record IntRange(int start, int endExclusive) {
  /**
   * Mirrors Kotlin's {@code IntRange.EMPTY}, which is defined as the inclusive range {@code 1..0}
   * (i.e. {@code first = 1}, {@code last = 0}). In half-open form that is {@code [1, 1)}.
   */
  public static final IntRange EMPTY = new IntRange(1, 1);

  /** The inclusive last index, equivalent to Kotlin's {@code IntRange.last}. */
  public int last() {
    return endExclusive - 1;
  }

  /** Equivalent to Kotlin's {@code IntRange.next()} (i.e. {@code IntRangeUtils.calcNext(this)}). */
  public int next() {
    return endExclusive;
  }

  /** Equivalent to Kotlin's {@code IntRange.offset(offset)}. */
  public IntRange offset(int offset) {
    return new IntRange(start + offset, endExclusive + offset);
  }

  /**
   * Equivalent to Kotlin's {@code Iterable<IntRange>.merge()}: spans from the first range's start
   * to the last range's end, ignoring any ranges in between.
   */
  public static IntRange merge(List<IntRange> ranges) {
    IntRange first = ranges.get(0);
    IntRange last = ranges.get(ranges.size() - 1);
    return new IntRange(first.start(), last.endExclusive());
  }
}
