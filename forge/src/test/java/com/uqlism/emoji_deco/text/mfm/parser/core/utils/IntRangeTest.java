package com.uqlism.emoji_deco.text.mfm.parser.core.utils;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IntRangeTest {

    @Test
    void lastIsEndExclusiveMinusOne() {
        assertEquals(4, new IntRange(2, 5).last());
    }

    @Test
    void nextEqualsEndExclusive() {
        assertEquals(5, new IntRange(2, 5).next());
    }

    @Test
    void offsetShiftsBothBounds() {
        IntRange shifted = new IntRange(2, 5).offset(10);
        assertEquals(12, shifted.start());
        assertEquals(15, shifted.endExclusive());
    }

    @Test
    void mergeSpansFromFirstStartToLastEnd() {
        IntRange merged = IntRange.merge(List.of(new IntRange(2, 5), new IntRange(8, 9), new IntRange(20, 30)));
        assertEquals(2, merged.start());
        assertEquals(30, merged.endExclusive());
    }

    @Test
    void emptyIsZeroLength() {
        assertEquals(IntRange.EMPTY.start(), IntRange.EMPTY.endExclusive());
    }
}
