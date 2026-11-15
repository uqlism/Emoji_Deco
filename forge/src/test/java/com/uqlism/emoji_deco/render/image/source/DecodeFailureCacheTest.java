package com.uqlism.emoji_deco.render.image.source;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DecodeFailureCacheTest {

    @Test void blocksAfterFailureUntilExpiry() {
        DecodeFailureCache c = new DecodeFailureCache();
        assertFalse(c.isBlocked("u", 0));
        assertTrue(c.recordFailure("u", 0));
        assertTrue(c.isBlocked("u", 1000));
        assertFalse(c.recordFailure("u", 2000));
        assertFalse(c.isBlocked("other", 1000));
        assertFalse(c.isBlocked("u", 2000 + DecodeFailureCache.BLOCK_MS));
    }
}
