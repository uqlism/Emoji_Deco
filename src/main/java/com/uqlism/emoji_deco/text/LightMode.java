package com.uqlism.emoji_deco.text;

/**
 * Controls how a {@link GlowSequence} affects the packedLight value during rendering.
 *
 *   Bypass        — pass through whatever packedLight was received (including parent overrides)
 *   Ambient       — restore the original ambient light from before any GlowSequence override
 *   Fixed(value)  — force a specific packedLight value
 */
public sealed interface LightMode permits LightMode.Bypass, LightMode.Ambient, LightMode.Fixed {

    record Bypass()         implements LightMode {}
    record Ambient()        implements LightMode {}
    record Fixed(int value) implements LightMode {}

    Bypass  BYPASS  = new Bypass();
    Ambient AMBIENT = new Ambient();
    /** Maximum brightness — sky=15, block=15. */
    Fixed   GLOW    = new Fixed(0xF000F0);
}
