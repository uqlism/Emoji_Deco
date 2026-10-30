package com.uqlism.emoji_deco.text.mfm.parser.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StringParserTest {

    private static final IMfmParserContext CONTEXT = new IMfmParserContext.Impl();

    @Test
    void matchesAtStartOfInput() {
        IMfmParserResult result = new StringParser("abc").find("abcdef", 0, CONTEXT);
        assertTrue(result.success());
        assertEquals(0, result.foundInfo().overallRange().start());
        assertEquals(3, result.foundInfo().overallRange().endExclusive());
        assertEquals(3, result.foundInfo().resumeIndex());
    }

    @Test
    void matchesAtGivenStartOffset() {
        // "abc" only occurs at index 1, so a search starting at 0 must fail but one starting at 1 must succeed —
        // this is what distinguishes StringParser (anchored at startAt) from a general "contains" search.
        assertFalse(new StringParser("abc").find("xabcdef", 0, CONTEXT).success());
        assertTrue(new StringParser("abc").find("xabcdef", 1, CONTEXT).success());
    }

    @Test
    void failsWhenRemainingInputIsShorterThanWord() {
        assertFalse(new StringParser("abcdef").find("abc", 0, CONTEXT).success());
    }

    @Test
    void failsOnMismatch() {
        assertFalse(new StringParser("abc").find("abd", 0, CONTEXT).success());
    }
}
