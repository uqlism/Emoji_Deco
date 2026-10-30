package com.uqlism.emoji_deco.text;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TooltipBreakTest {
    @Test void detectsLfAndCr() {
        assertTrue(ComponentConverter.containsLineBreak("first\nsecond"));
        assertTrue(ComponentConverter.containsLineBreak("a\rb"));
        assertTrue(ComponentConverter.containsLineBreak("\n"));
    }

    @Test void plainTextHasNoBreak() {
        assertFalse(ComponentConverter.containsLineBreak("plain text"));
        assertFalse(ComponentConverter.containsLineBreak(""));
    }
}
