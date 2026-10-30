package com.uqlism.emoji_deco.text.mfm.parser.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SequentialParserTest {

    private static final IMfmParserContext CONTEXT = new IMfmParserContext.Impl();

    @Test
    void succeedsWhenAllPartsMatchInOrder() {
        SequentialParser parser = new SequentialParser(new StringParser("foo"), new StringParser("bar"));
        IMfmParserResult result = parser.find("foobar", 0, CONTEXT);
        assertTrue(result.success());
        assertEquals(0, result.foundInfo().overallRange().start());
        assertEquals(6, result.foundInfo().overallRange().endExclusive());
        assertEquals(6, result.foundInfo().resumeIndex());
        assertEquals(2, result.foundInfo().nestedInfos().size());
    }

    @Test
    void failsWhenAnyPartFails() {
        SequentialParser parser = new SequentialParser(new StringParser("foo"), new StringParser("bar"));
        assertFalse(parser.find("foobaz", 0, CONTEXT).success());
    }

    @Test
    void failsWhenFirstPartFails() {
        SequentialParser parser = new SequentialParser(new StringParser("foo"), new StringParser("bar"));
        assertFalse(parser.find("xoobar", 0, CONTEXT).success());
    }
}
