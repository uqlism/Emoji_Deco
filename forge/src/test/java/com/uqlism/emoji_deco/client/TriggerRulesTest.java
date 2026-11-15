package com.uqlism.emoji_deco.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TriggerRulesTest {

    @Test void nonCommandIsNotSuppressed() {
        assertFalse(TriggerRules.isSuppressedCommand("hello :he"));
        assertFalse(TriggerRules.isSuppressedCommand(""));
        assertFalse(TriggerRules.isSuppressedCommand(null));
    }

    @Test void gameplayCommandsAreSuppressed() {
        assertTrue(TriggerRules.isSuppressedCommand("/give @p minecraft:di"));
        assertTrue(TriggerRules.isSuppressedCommand("/execute as @a run give @s minecraft:di"));
        assertTrue(TriggerRules.isSuppressedCommand("/"));
        assertTrue(TriggerRules.isSuppressedCommand("/tag @s add #foo"));
    }

    @Test void messageCommandsAreNotSuppressed() {
        assertFalse(TriggerRules.isSuppressedCommand("/say :he"));
        assertFalse(TriggerRules.isSuppressedCommand("/me :he"));
        assertFalse(TriggerRules.isSuppressedCommand("/msg Steve :he"));
        assertFalse(TriggerRules.isSuppressedCommand("/tell Steve :he"));
        assertFalse(TriggerRules.isSuppressedCommand("/w Steve :he"));
        assertFalse(TriggerRules.isSuppressedCommand("/teammsg :he"));
        assertFalse(TriggerRules.isSuppressedCommand("/SAY :he"));
        assertFalse(TriggerRules.isSuppressedCommand("/minecraft:say :he"));
    }

    @Test void colonAfterIdentifierIsNamespaceNotTrigger() {
        String t = "minecraft:di";
        assertFalse(TriggerRules.isColonTriggerAllowed(t, t.indexOf(':')));
        t = "a:he";
        assertFalse(TriggerRules.isColonTriggerAllowed(t, 1));
        t = "my_mod.x-y:di";
        assertFalse(TriggerRules.isColonTriggerAllowed(t, t.indexOf(':')));
    }

    @Test void colonAtStartOrAfterSpaceOrSymbolIsTrigger() {
        assertTrue(TriggerRules.isColonTriggerAllowed(":he", 0));
        assertTrue(TriggerRules.isColonTriggerAllowed("hello :he", 6));
        assertTrue(TriggerRules.isColonTriggerAllowed("(:he", 1));
        assertTrue(TriggerRules.isColonTriggerAllowed("こん:he", 2));
        assertTrue(TriggerRules.isColonTriggerAllowed("**:he", 2));
    }
}
