package com.uqlism.emoji_deco;

import com.uqlism.emoji_deco.client.SuggestionState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SuggestionStateTest extends MinecraftTestBase {

    @BeforeEach
    void resetState() {
        SuggestionState.clear();
        SuggestionState.pendingCursor = -1;
    }

    // ── applyTo: decorator ────────────────────────────────────────────────────

    @Test
    void applyDecorator() {
        SuggestionState.lastInput  = "#bo";
        SuggestionState.lastCursor = 3;
        var entry = entry("bold", SuggestionState.TriggerType.DECORATOR);
        assertEquals("#bold[]", SuggestionState.applyTo("#bo", entry));
        assertEquals(6, SuggestionState.pendingCursor); // カーソルは [] の中
    }

    @Test
    void applyDecoratorWithTrailingText() {
        SuggestionState.lastInput  = "#bo world";
        SuggestionState.lastCursor = 3;
        var entry = entry("bold", SuggestionState.TriggerType.DECORATOR);
        assertEquals("#bold[] world", SuggestionState.applyTo("#bo world", entry));
    }

    // ── applyTo: shortcode ────────────────────────────────────────────────────

    @Test
    void applyShortcode() {
        SuggestionState.lastInput  = ":hea";
        SuggestionState.lastCursor = 4;
        var entry = entry("heart", SuggestionState.TriggerType.SHORTCODE);
        assertEquals(":heart:", SuggestionState.applyTo(":hea", entry));
        assertEquals(7, SuggestionState.pendingCursor);
    }

    // ── applyTo: decorator arg ────────────────────────────────────────────────

    @Test
    void applyDecoratorArg() {
        SuggestionState.lastInput  = "#color.r";
        SuggestionState.lastCursor = 8;
        var entry = entry("red", SuggestionState.TriggerType.DECORATOR_ARG);
        assertEquals("#color.red[]", SuggestionState.applyTo("#color.r", entry));
        assertEquals(11, SuggestionState.pendingCursor);
    }

    @Test
    void applyDecoratorArgSecond() {
        // 2番目の引数を補完: #gradient.red,bl → #gradient.red,blue[
        SuggestionState.lastInput  = "#gradient.red,bl";
        SuggestionState.lastCursor = 16;
        var entry = entry("blue", SuggestionState.TriggerType.DECORATOR_ARG);
        assertEquals("#gradient.red,blue[]", SuggestionState.applyTo("#gradient.red,bl", entry));
    }

    // ── applyTo: shortcode arg ────────────────────────────────────────────────

    @Test
    void applyShortcodeArg() {
        SuggestionState.lastInput  = ":player.Ste";
        SuggestionState.lastCursor = 11;
        var entry = entry("Steve", SuggestionState.TriggerType.SHORTCODE_ARG);
        assertEquals(":player.Steve:", SuggestionState.applyTo(":player.Ste", entry));
        assertEquals(14, SuggestionState.pendingCursor);
    }

    // ── trigger detection: escape ─────────────────────────────────────────────

    @Test
    void escapedHashNotTriggered() {
        SuggestionState.update("\\#bold", 6);
        assertTrue(SuggestionState.suggestions.isEmpty());
    }

    @Test
    void escapedColonNotTriggered() {
        SuggestionState.update("\\:heart", 7);
        assertTrue(SuggestionState.suggestions.isEmpty());
    }

    @Test
    void unescapedHashTriggersWhenValid() {
        // 未登録デコレーターでも # は検出される (候補リストが空になるだけ)
        SuggestionState.update("#bold", 5);
        // DecoratorManager に何も登録されていないので suggestions は空だが例外が出ないこと
        assertNotNull(SuggestionState.suggestions);
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private static SuggestionState.Entry entry(String insertion, SuggestionState.TriggerType type) {
        return new SuggestionState.Entry(insertion, null, insertion, type);
    }
}
