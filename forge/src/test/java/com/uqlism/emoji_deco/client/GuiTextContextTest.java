package com.uqlism.emoji_deco.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiTextContextTest {

    @Test void generalGuiTextFollowsEnableGui() {
        assertTrue(GuiTextContext.isEnabled(false, true, false));
        assertFalse(GuiTextContext.isEnabled(false, false, true));
    }

    @Test void hotbarItemNameFollowsEnableItemNames() {
        assertTrue(GuiTextContext.isEnabled(true, false, true));
        assertFalse(GuiTextContext.isEnabled(true, true, false));
    }
}
