package com.uqlism.emoji_deco.text;

import net.minecraft.util.FormattedCharSequence;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatNewlineTest {
    private static final FormattedCharSequence B = com.uqlism.emoji_deco.text.ir.RichNode.LINE_BREAK;
    private static final FormattedCharSequence A = sink -> true;
    private static final FormattedCharSequence C = sink -> true;

    @Test void noBreakIsOneGroup() {
        assertEquals(1, ComponentConverter.splitAtBreaks(List.of(A, C)).size());
    }

    @Test void breakSplitsGroups() {
        var g = ComponentConverter.splitAtBreaks(List.of(A, B, C));
        assertEquals(2, g.size());
        assertEquals(List.of(A), g.get(0));
        assertEquals(List.of(C), g.get(1));
    }

    @Test void consecutiveBreaksKeepBlankLine() {
        var g = ComponentConverter.splitAtBreaks(List.of(A, B, B, C));
        assertEquals(3, g.size());
        assertTrue(g.get(1).isEmpty());
    }

    @Test void leadingBreakGivesEmptyFirstLine() {
        var g = ComponentConverter.splitAtBreaks(List.of(B, C));
        assertEquals(2, g.size());
        assertTrue(g.get(0).isEmpty());
    }

    @Test void trailingBreakDoesNotAddExtraLine() {
        assertEquals(1, ComponentConverter.splitAtBreaks(List.of(A, B)).size());
    }

    @Test void splitNewlinesKeepsEmptySegments() {
        assertEquals(java.util.List.of("a", "", "b"), java.util.Arrays.asList(split("a\n\nb")));
        assertEquals(java.util.List.of("```", "code", "```"), java.util.Arrays.asList(split("```\ncode\n```")));
        assertEquals(java.util.List.of("a", ""), java.util.Arrays.asList(split("a\n")));
    }

    @Test void normalizeNewlinesHandlesCr() {
        assertEquals("a\nb", com.uqlism.emoji_deco.text.ir.RichNode.normalizeNewlines("a\r\nb"));
        assertEquals("a\nb", com.uqlism.emoji_deco.text.ir.RichNode.normalizeNewlines("a\rb"));
        assertEquals("a\n\nb", com.uqlism.emoji_deco.text.ir.RichNode.normalizeNewlines("a\r\n\rb"));
        assertEquals(java.util.List.of("a", "b"), java.util.Arrays.asList(split("a\r\nb")));
    }

    private static String[] split(String s) {
        return com.uqlism.emoji_deco.text.ir.RichNode.splitNewlines(s);
    }
}
