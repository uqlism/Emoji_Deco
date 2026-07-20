package com.uqlism.emoji_deco.text.mfm.parser.core;

import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegexParserTest {

    private static final IMfmParserContext CONTEXT = new IMfmParserContext.Impl();

    @Test
    void matchesAnchoredAtStartPosition() {
        // digits occur at index 3, so starting the search there must succeed
        IMfmParserResult result = new RegexParser(Pattern.compile("[0-9]+")).find("abc123", 3, CONTEXT);
        assertTrue(result.success());
        assertEquals(3, result.foundInfo().overallRange().start());
        assertEquals(6, result.foundInfo().overallRange().endExclusive());
        assertEquals(6, result.foundInfo().resumeIndex());
    }

    @Test
    void doesNotMatchPastTheStartPosition() {
        // The pattern would match "123" further into the string, but since the parser is anchored
        // to startAt (not a general "contains" search), starting before the digits must fail.
        assertFalse(new RegexParser(Pattern.compile("[0-9]+")).find("abc123", 0, CONTEXT).success());
    }

    @Test
    void failsWhenNoMatchAtStartPosition() {
        assertFalse(new RegexParser(Pattern.compile("[0-9]+")).find("abcdef", 0, CONTEXT).success());
    }

    @Test
    void matchesShortestPrefixSatisfyingPattern() {
        // "^" is prepended internally; the match is whatever the regex consumes from startAt.
        IMfmParserResult result = new RegexParser(Pattern.compile("[a-z]+")).find("hello world", 0, CONTEXT);
        assertTrue(result.success());
        assertEquals(5, result.foundInfo().resumeIndex());
    }
}
