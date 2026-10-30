package com.uqlism.emoji_deco.text.mfm.parser.core.fixed;

import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserContext;
import com.uqlism.emoji_deco.text.mfm.parser.core.IMfmParserResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FixedParsersTest {

    private static final IMfmParserContext CONTEXT = new IMfmParserContext.Impl();

    @Test
    void crLfParserOnlyMatchesCrLfNotBareCrOrLf() {
        assertTrue(CrLfParser.INSTANCE.find("\r\nx", 0, CONTEXT).success());
        assertFalse(CrLfParser.INSTANCE.find("\rx", 0, CONTEXT).success());
        assertFalse(CrLfParser.INSTANCE.find("\nx", 0, CONTEXT).success());
    }

    @Test
    void newLineParserMatchesAnyLineBreakVariant() {
        assertTrue(NewLineParser.INSTANCE.find("\r\n", 0, CONTEXT).success());
        assertTrue(NewLineParser.INSTANCE.find("\r", 0, CONTEXT).success());
        assertTrue(NewLineParser.INSTANCE.find("\n", 0, CONTEXT).success());
        assertFalse(NewLineParser.INSTANCE.find("x", 0, CONTEXT).success());
    }

    @Test
    void spaceParserMatchesHalfAndFullWidthSpaceAndTab() {
        assertTrue(SpaceParser.INSTANCE.find(" x", 0, CONTEXT).success());
        assertTrue(SpaceParser.INSTANCE.find("\tx", 0, CONTEXT).success());
        assertTrue(SpaceParser.INSTANCE.find("　x", 0, CONTEXT).success());
        assertFalse(SpaceParser.INSTANCE.find("x", 0, CONTEXT).success());
    }

    @Test
    void charSequenceTerminateParserOnlySucceedsAtEndOfInput() {
        assertFalse(CharSequenceTerminateParser.INSTANCE.find("abc", 0, CONTEXT).success());
        assertFalse(CharSequenceTerminateParser.INSTANCE.find("abc", 2, CONTEXT).success());
        assertTrue(CharSequenceTerminateParser.INSTANCE.find("abc", 3, CONTEXT).success());
    }

    @Test
    void lineBeginParserSucceedsAtStartAndAfterLineBreak() {
        String input = "abc\ndef";
        assertTrue(LineBeginParser.INSTANCE.find(input, 0, CONTEXT).success());
        assertTrue(LineBeginParser.INSTANCE.find(input, 4, CONTEXT).success()); // right after '\n'
        assertFalse(LineBeginParser.INSTANCE.find(input, 1, CONTEXT).success());
        assertFalse(LineBeginParser.INSTANCE.find(input, 3, CONTEXT).success()); // the '\n' itself
    }

    @Test
    void lineEndParserSucceedsAtLineBreakAndEndOfInput() {
        String input = "abc\ndef";
        assertTrue(LineEndParser.INSTANCE.find(input, 3, CONTEXT).success()); // at the '\n'
        assertTrue(LineEndParser.INSTANCE.find(input, 7, CONTEXT).success()); // end of string
        assertFalse(LineEndParser.INSTANCE.find(input, 0, CONTEXT).success());
        assertFalse(LineEndParser.INSTANCE.find(input, 4, CONTEXT).success()); // right after '\n'
    }

    @Test
    void crAndLfParsersAreExclusive() {
        assertTrue(CrParser.INSTANCE.find("\r", 0, CONTEXT).success());
        assertFalse(LfParser.INSTANCE.find("\r", 0, CONTEXT).success());
        assertTrue(LfParser.INSTANCE.find("\n", 0, CONTEXT).success());
        assertFalse(CrParser.INSTANCE.find("\n", 0, CONTEXT).success());
    }
}
