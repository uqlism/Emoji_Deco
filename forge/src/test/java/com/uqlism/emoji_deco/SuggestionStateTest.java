package com.uqlism.emoji_deco;

import com.uqlism.emoji_deco.client.SuggestionState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SuggestionStateTest extends MinecraftTestBase {

    private SuggestionState state;

    @BeforeEach
    void resetState() {
        state = new SuggestionState();
        state.pendingCursor = -1;
    }

    // ── applyTo: decorator ────────────────────────────────────────────────────

    @Test
    void applyDecorator() {
        state.lastInput  = "#bo";
        state.lastCursor = 3;
        var entry = entry("bold", SuggestionState.TriggerType.DECORATOR);
        assertEquals("#bold[]", state.applyTo("#bo", entry));
        assertEquals(6, state.pendingCursor); // カーソルは [] の中
    }

    @Test
    void applyDecoratorWithTrailingText() {
        state.lastInput  = "#bo world";
        state.lastCursor = 3;
        var entry = entry("bold", SuggestionState.TriggerType.DECORATOR);
        assertEquals("#bold[] world", state.applyTo("#bo world", entry));
    }

    // ── applyTo: shortcode ────────────────────────────────────────────────────

    @Test
    void applyShortcode() {
        state.lastInput  = ":hea";
        state.lastCursor = 4;
        var entry = entry("heart", SuggestionState.TriggerType.SHORTCODE);
        assertEquals(":heart:", state.applyTo(":hea", entry));
        assertEquals(7, state.pendingCursor);
    }

    // ── applyTo: decorator arg ────────────────────────────────────────────────

    @Test
    void applyDecoratorArg() {
        state.lastInput  = "#color.r";
        state.lastCursor = 8;
        var entry = entry("red", SuggestionState.TriggerType.DECORATOR_ARG);
        assertEquals("#color.red[]", state.applyTo("#color.r", entry));
        assertEquals(11, state.pendingCursor);
    }

    @Test
    void applyDecoratorArgSecond() {
        // 2番目の引数を補完: #gradient.red,bl → #gradient.red,blue[
        state.lastInput  = "#gradient.red,bl";
        state.lastCursor = 16;
        var entry = entry("blue", SuggestionState.TriggerType.DECORATOR_ARG);
        assertEquals("#gradient.red,blue[]", state.applyTo("#gradient.red,bl", entry));
    }

    // ── applyTo: shortcode arg ────────────────────────────────────────────────

    @Test
    void applyShortcodeArg() {
        state.lastInput  = ":player.Ste";
        state.lastCursor = 11;
        var entry = entry("Steve", SuggestionState.TriggerType.SHORTCODE_ARG);
        assertEquals(":player.Steve:", state.applyTo(":player.Ste", entry));
        assertEquals(14, state.pendingCursor);
    }

    // ── trigger detection: escape ─────────────────────────────────────────────

    @Test
    void escapedHashNotTriggered() {
        state.update("\\#bold", 6);
        assertTrue(state.suggestions.isEmpty());
    }

    @Test
    void escapedColonNotTriggered() {
        state.update("\\:heart", 7);
        assertTrue(state.suggestions.isEmpty());
    }

    @Test
    void unescapedHashTriggersWhenValid() {
        // 未登録デコレーターでも # は検出される (候補リストが空になるだけ)
        state.update("#bold", 5);
        // DecoratorManager に何も登録されていないので suggestions は空だが例外が出ないこと
        assertNotNull(state.suggestions);
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private static SuggestionState.Entry entry(String insertion, SuggestionState.TriggerType type) {
        return new SuggestionState.Entry(insertion, null, insertion, type);
    }
}
