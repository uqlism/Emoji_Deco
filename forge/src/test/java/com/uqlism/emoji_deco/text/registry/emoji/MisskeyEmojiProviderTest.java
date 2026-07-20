package com.uqlism.emoji_deco.text.registry.emoji;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers only the JSON→map parsing half of MisskeyEmojiProvider. resolve()/prefetch() hit
 * the network via UrlFetcher and are intentionally left untested here (see project convention
 * of avoiding live-network unit tests).
 */
class MisskeyEmojiProviderTest {

    @Test void parsesNameToUrlMap() {
        String json = """
                {"emojis":[
                    {"name":"blobcat","category":"cats","url":"https://misskey.io/files/blobcat.png","aliases":["cat"]},
                    {"name":"ablobcatrave","category":null,"url":"https://misskey.io/files/rave.gif","aliases":[]}
                ]}""";
        Map<String, String> result = MisskeyEmojiProvider.parseEmojiList(json);
        assertEquals(2, result.size());
        assertEquals("https://misskey.io/files/blobcat.png", result.get("blobcat"));
        assertEquals("https://misskey.io/files/rave.gif", result.get("ablobcatrave"));
    }

    @Test void skipsEntriesMissingUrlOrName() {
        String json = """
                {"emojis":[
                    {"name":"noUrl","category":null,"aliases":[]},
                    {"category":null,"url":"https://misskey.io/files/noname.png","aliases":[]},
                    {"name":"ok","url":"https://misskey.io/files/ok.png","aliases":[]}
                ]}""";
        Map<String, String> result = MisskeyEmojiProvider.parseEmojiList(json);
        assertEquals(1, result.size());
        assertEquals("https://misskey.io/files/ok.png", result.get("ok"));
    }

    @Test void emptyEmojisArrayYieldsEmptyMap() {
        assertTrue(MisskeyEmojiProvider.parseEmojiList("{\"emojis\":[]}").isEmpty());
    }

    @Test void malformedJsonDoesNotThrow() {
        assertTrue(MisskeyEmojiProvider.parseEmojiList("not json at all").isEmpty());
        assertTrue(MisskeyEmojiProvider.parseEmojiList("").isEmpty());
        assertTrue(MisskeyEmojiProvider.parseEmojiList("{}").isEmpty());
        assertTrue(MisskeyEmojiProvider.parseEmojiList("[]").isEmpty());
        assertTrue(MisskeyEmojiProvider.parseEmojiList("{\"emojis\":\"not an array\"}").isEmpty());
    }
}
