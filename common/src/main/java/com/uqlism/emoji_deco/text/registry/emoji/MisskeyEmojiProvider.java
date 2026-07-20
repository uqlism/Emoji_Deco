package com.uqlism.emoji_deco.text.registry.emoji;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.uqlism.emoji_deco.Config;
import com.uqlism.emoji_deco.network.UrlFetcher;
import com.uqlism.emoji_deco.render.image.BinarySource;
import com.uqlism.emoji_deco.render.image.ImageSpec;
import com.uqlism.emoji_deco.render.image.source.UrlAllowlist;
import com.uqlism.emoji_deco.text.ir.RichNode;
import com.uqlism.emoji_deco.text.parse.RichTextParser;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Resolves MFM {@code :name:} emoji codes against a Misskey server's public emoji list
 * ({@code GET /api/emojis}, no auth required).
 *
 * <p>Metadata (name → image URL) is fetched lazily and cached in memory as a whole; the
 * emoji images themselves are <b>not</b> pre-downloaded. {@link #resolve(String)} returns
 * a {@link RichNode.Image} pointing at the emoji's URL and leaves the actual fetch/decode
 * to the existing lazy image pipeline ({@code ImageGlyphPool} + {@link UrlFetcher}).
 *
 * <p>Network failures never throw — {@link #resolve(String)} simply keeps returning
 * {@code null} until a fetch eventually succeeds, letting {@code CustomEmojiResolver}
 * fall back to the {@code :name:} literal.
 */
public final class MisskeyEmojiProvider {
    private MisskeyEmojiProvider() {}

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();

    // misskey.io alone serves 13,000+ emojis (~3.2MB of JSON) — leave generous headroom.
    private static final int MAX_BYTES = 16 * 1024 * 1024;
    private static final int API_TTL_SECONDS = 3600;
    private static final int IMAGE_TTL_SECONDS = 86400;

    private static volatile Map<String, String> nameToUrl = Map.of();

    // Tracks which domain the current in-memory map was successfully fetched for, so a
    // domain change (or the very first resolve() call) triggers a re-fetch. UrlFetcher's
    // own TTL/backoff cache is the source of truth for retry pacing — this class only
    // needs a CAS gate to avoid piling up duplicate parse work while a fetch is in flight.
    private static volatile String fetchedForDomain = null;
    private static final AtomicBoolean fetchInFlight = new AtomicBoolean(false);

    /** Kicks off a background fetch if one hasn't started yet. Safe to call repeatedly. */
    public static void prefetch() {
        ensureFetchStarted();
    }

    /** Resolves {@code name} to a display node, or {@code null} if unresolved (yet). */
    public static @Nullable RichNode resolve(String name) {
        ensureFetchStarted();
        String url = nameToUrl.get(name);
        if (url == null) return null;
        return new RichNode.Image(
                new ImageSpec.Decoded(null, new BinarySource.Url(url, true, IMAGE_TTL_SECONDS, "GET", Map.of(), null)),
                null, 8, 8, Float.NaN);
    }

    private static void ensureFetchStarted() {
        String domain = Config.emojiProviderMisskeyDomain;
        if (domain == null || domain.isBlank()) return;
        if (domain.equals(fetchedForDomain)) return;
        if (!fetchInFlight.compareAndSet(false, true)) return;

        // The API domain itself must be allowlisted before UrlFetcher will fetch it.
        UrlAllowlist.INSTANCE.allowDynamic(domain);

        BinarySource.Url source = new BinarySource.Url(
                "https://" + domain + "/api/emojis", false, API_TTL_SECONDS, "GET", Map.of(), null);
        UrlFetcher.fetch(source, MAX_BYTES)
                .thenAccept(bytes -> {
                    Map<String, String> parsed = parseEmojiList(new String(bytes, StandardCharsets.UTF_8));
                    nameToUrl = parsed;
                    fetchedForDomain = domain;

                    Set<String> hosts = new HashSet<>();
                    for (String url : parsed.values()) {
                        try {
                            hosts.add(new URL(url).getHost());
                        } catch (MalformedURLException ignored) {}
                    }
                    UrlAllowlist.INSTANCE.allowDynamic(hosts);
                    RichTextParser.invalidateParseCache();
                    LOGGER.info("[EmojiDeco] Misskey emoji-provider: {} 件の絵文字を {} から取得しました",
                            parsed.size(), domain);
                })
                .exceptionally(t -> {
                    LOGGER.warn("[EmojiDeco] Misskey emoji-provider: {} からの絵文字リスト取得に失敗しました: {}",
                            domain, t.toString());
                    return null;
                })
                .whenComplete((r, t) -> fetchInFlight.set(false));
    }

    // ── JSON parsing ─────────────────────────────────────────────────────────────
    // Kept separate from network I/O so it can be unit-tested without a live server.

    /** Parses a {@code {"emojis":[{"name":...,"url":...,...}, ...]}} body into a name→url map. */
    static Map<String, String> parseEmojiList(String json) {
        Map<String, String> result = new HashMap<>();
        try {
            JsonElement root = GSON.fromJson(json, JsonElement.class);
            if (root == null || !root.isJsonObject()) return Map.of();
            JsonElement emojisEl = root.getAsJsonObject().get("emojis");
            if (emojisEl == null || !emojisEl.isJsonArray()) return Map.of();

            for (JsonElement el : emojisEl.getAsJsonArray()) {
                if (!el.isJsonObject()) continue;
                JsonObject obj = el.getAsJsonObject();
                JsonElement nameEl = obj.get("name");
                JsonElement urlEl = obj.get("url");
                if (nameEl == null || !nameEl.isJsonPrimitive()) continue;
                if (urlEl == null || !urlEl.isJsonPrimitive()) continue;
                String name = nameEl.getAsString();
                String url = urlEl.getAsString();
                if (!name.isEmpty() && !url.isEmpty()) result.put(name, url);
            }
        } catch (Exception e) {
            LOGGER.warn("[EmojiDeco] Misskey emoji-provider: JSON パース失敗: {}", e.getMessage());
            return Map.of();
        }
        return Map.copyOf(result);
    }
}
