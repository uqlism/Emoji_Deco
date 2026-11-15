package com.uqlism.emoji_deco.network;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UrlFetcherLogTest {

    @Test void deniedIsLoggedOncePerKey() {
        Set<String> warned = new HashSet<>();
        assertTrue(UrlFetcher.shouldLogDenied(warned, "misskey.io"));
        assertFalse(UrlFetcher.shouldLogDenied(warned, "misskey.io"));
        assertTrue(UrlFetcher.shouldLogDenied(warned, "other.example"));
    }

    @Test void hostOfExtractsHost() {
        assertEquals("misskey.io", UrlFetcher.hostOf("https://misskey.io/api/emojis"));
        assertEquals("not a url", UrlFetcher.hostOf("not a url"));
    }
}
