package com.uqlism.emoji_deco.text.mfm.parser.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlternateParserTest {

    private static final IMfmParserContext CONTEXT = new IMfmParserContext.Impl();

    @Test
    void succeedsWithFirstMatchingParser() {
        AlternateParser parser = new AlternateParser(new StringParser("foo"), new StringParser("bar"));
        IMfmParserResult result = parser.find("bar", 0, CONTEXT);
        assertTrue(result.success());
        assertEquals(3, result.foundInfo().resumeIndex());
    }

    @Test
    void prefersEarlierParserWhenBothWouldMatch() {
        // "foo" and "fo" both start matching at position 0; AlternateParser must try candidates in
        // order and return the first success, not the longest.
        AlternateParser parser = new AlternateParser(new StringParser("foo"), new StringParser("fo"));
        IMfmParserResult result = parser.find("foo", 0, CONTEXT);
        assertTrue(result.success());
        assertEquals(3, result.foundInfo().resumeIndex());
    }

    @Test
    void failsWhenNoParserMatches() {
        AlternateParser parser = new AlternateParser(new StringParser("foo"), new StringParser("bar"));
        assertFalse(parser.find("baz", 0, CONTEXT).success());
    }
}
