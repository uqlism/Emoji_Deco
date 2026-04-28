package com.uqlism.emoji_deco;

import net.minecraft.SharedConstants;
import org.junit.jupiter.api.BeforeAll;

/**
 * Base class that performs the minimum setup needed to use Minecraft value classes
 * (Style, Component, ResourceLocation, etc.) in unit tests.
 *
 * Full Bootstrap.bootStrap() is intentionally avoided: it triggers Forge's networking
 * and event-bus reflection which fails outside of a running game instance.
 * SharedConstants.tryDetectVersion() is sufficient for our parsing tests.
 */
public class MinecraftTestBase {

    private static boolean initialized = false;

    @BeforeAll
    static void initSharedConstants() {
        if (!initialized) {
            SharedConstants.tryDetectVersion();
            initialized = true;
        }
    }
}
