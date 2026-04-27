package com.uqlism.emoji_deco.text;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import org.slf4j.Logger;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;

public class ShortcodeManager implements PreparableReloadListener {

    public static final ShortcodeManager INSTANCE = new ShortcodeManager();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, Component> REGISTRY = new ConcurrentHashMap<>();
    /** alias text → canonical shortcode name */
    private static final Map<String, String> ALIASES = new ConcurrentHashMap<>();

    private record LoadResult(Map<String, Component> registry, Map<String, String> aliases) {}

    @Override
    public CompletableFuture<Void> reload(
            PreparationBarrier stage,
            ResourceManager resourceManager,
            ProfilerFiller preparationsProfiler,
            ProfilerFiller reloadProfiler,
            Executor backgroundExecutor,
            Executor gameExecutor) {

        return CompletableFuture
                .supplyAsync(() -> loadAll(resourceManager), backgroundExecutor)
                .thenCompose(stage::wait)
                .thenAcceptAsync(result -> {
                    REGISTRY.clear();
                    REGISTRY.putAll(result.registry());
                    ALIASES.clear();
                    ALIASES.putAll(result.aliases());
                    LOGGER.info("[EmojiDeco] Loaded {} shortcode(s), {} alias(es)",
                            REGISTRY.size(), ALIASES.size());
                }, gameExecutor);
    }

    private static LoadResult loadAll(ResourceManager resourceManager) {
        Map<String, Component> loaded = new HashMap<>();
        Map<String, String> loadedAliases = new HashMap<>();
        Map<ResourceLocation, Resource> resources = resourceManager.listResources(
                "shortcodes", path -> path.getPath().endsWith(".json"));

        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            ResourceLocation location = entry.getKey();
            try (InputStreamReader reader = new InputStreamReader(
                    entry.getValue().open(), StandardCharsets.UTF_8)) {

                JsonObject json = GsonHelper.parse(reader);

                if (json.has("enable") && !json.get("enable").getAsBoolean()) continue;
                if (!json.has("display")) continue;

                // "shortcodes/mycode.json" -> "mycode"
                String path = location.getPath();
                String name = path.substring("shortcodes/".length(), path.length() - ".json".length());

                Component component = parseDisplay(json.get("display"));
                loaded.put(name, component);

                if (json.has("aliases") && json.get("aliases").isJsonArray()) {
                    for (JsonElement alias : json.getAsJsonArray("aliases")) {
                        String aliasStr = alias.getAsString().trim();
                        if (!aliasStr.isEmpty()) loadedAliases.put(aliasStr, name);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("[EmojiDeco] Failed to load shortcode {}: {}", location, e.getMessage());
            }
        }
        return new LoadResult(loaded, loadedAliases);
    }

    private static Component parseDisplay(JsonElement displayElement) {
        return EmojiDecoComponentParser.parse(displayElement, null);
    }

    public static boolean has(String code) {
        return REGISTRY.containsKey(code);
    }

    public static Component resolve(String code) {
        return REGISTRY.getOrDefault(code, Component.literal(":" + code + ":"));
    }

public static boolean hasAnyShortcode(String text) {
    int start = text.indexOf(':');
    while (start >= 0) {
        int end = text.indexOf(':', start + 1);
        if (end > start + 1) {
            String code = text.substring(start + 1, end);
            if (REGISTRY.containsKey(code)) return true;
        }
        start = text.indexOf(':', start + 1);
    }
    return false;
}

public static List<String> getSuggestions(String prefix) {
    return REGISTRY.keySet().stream()
        .filter(key -> key.startsWith(prefix))
        .sorted()
        .collect(java.util.stream.Collectors.toList());
}

/** Returns alias→canonical pairs whose alias starts with prefix, sorted by alias. */
public static List<Map.Entry<String, String>> getAliasSuggestions(String prefix) {
    return ALIASES.entrySet().stream()
        .filter(e -> e.getKey().startsWith(prefix))
        .sorted(Map.Entry.comparingByKey())
        .collect(java.util.stream.Collectors.toList());
}
}
