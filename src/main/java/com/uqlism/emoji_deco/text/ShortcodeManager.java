package com.uqlism.emoji_deco.text;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

public class ShortcodeManager implements PreparableReloadListener {

    public static final ShortcodeManager INSTANCE = new ShortcodeManager();
    private static final Logger LOGGER = LogUtils.getLogger();

    /** Pre-parsed nodes for shortcodes without dynamic content */
    private static final Map<String, RichNode> REGISTRY = new ConcurrentHashMap<>();
    /** Raw display JSON for shortcodes that contain dynamic nodes — parsed per call */
    private static final Map<String, JsonElement> PARAM_REGISTRY = new ConcurrentHashMap<>();
    /** alias text → canonical shortcode name */
    private static final Map<String, String> ALIASES = new ConcurrentHashMap<>();

    private record LoadResult(
            Map<String, RichNode>   registry,
            Map<String, JsonElement> paramRegistry,
            Map<String, String>     aliases) {}

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
                    PARAM_REGISTRY.clear();
                    PARAM_REGISTRY.putAll(result.paramRegistry());
                    ALIASES.clear();
                    ALIASES.putAll(result.aliases());
                    LOGGER.info("[EmojiDeco] Loaded {} shortcode(s) ({} parametric), {} alias(es)",
                            REGISTRY.size() + PARAM_REGISTRY.size(), PARAM_REGISTRY.size(), ALIASES.size());
                }, gameExecutor);
    }

    private static LoadResult loadAll(ResourceManager resourceManager) {
        Map<String, RichNode>   loaded        = new HashMap<>();
        Map<String, JsonElement> paramLoaded  = new HashMap<>();
        Map<String, String>     loadedAliases = new HashMap<>();
        Map<ResourceLocation, Resource> resources = resourceManager.listResources(
                "shortcodes", path -> path.getPath().endsWith(".json"));

        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            ResourceLocation location = entry.getKey();
            try (InputStreamReader reader = new InputStreamReader(
                    entry.getValue().open(), StandardCharsets.UTF_8)) {

                JsonObject json = GsonHelper.parse(reader);
                if (json.has("enable") && !json.get("enable").getAsBoolean()) continue;
                if (!json.has("display")) continue;

                String path = location.getPath();
                String name = path.substring("shortcodes/".length(), path.length() - ".json".length());

                JsonElement display = json.get("display");
                if (isDynamic(display)) {
                    paramLoaded.put(name, display);
                } else {
                    loaded.put(name, EmojiDecoComponentParser.parse(display, null));
                }

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
        return new LoadResult(loaded, paramLoaded, loadedAliases);
    }

    private static boolean isDynamic(JsonElement el) {
        if (el.isJsonObject()) {
            JsonObject obj = el.getAsJsonObject();
            if (obj.has("emoji_deco:arg") || obj.has("emoji_deco:player_names")) return true;
            for (var e : obj.entrySet()) if (isDynamic(e.getValue())) return true;
        } else if (el.isJsonArray()) {
            for (JsonElement child : el.getAsJsonArray()) if (isDynamic(child)) return true;
        }
        return false;
    }

    public static JsonElement getArgSuggestionsSpec(String code, int argIndex) {
        JsonElement display = PARAM_REGISTRY.get(code);
        if (display == null) return null;
        JsonObject argSpec = EmojiDecoComponentParser.findArgSpec(display, argIndex);
        if (argSpec == null || !argSpec.has("suggestions")) return null;
        return argSpec.get("suggestions");
    }

    public static boolean has(String code) {
        return REGISTRY.containsKey(code) || PARAM_REGISTRY.containsKey(code);
    }

    public static RichNode resolve(String code, String[] args) {
        JsonElement display = PARAM_REGISTRY.get(code);
        if (display != null) return EmojiDecoComponentParser.parse(display, null, args);
        return REGISTRY.getOrDefault(code, new RichNode.Text(":" + code + ":", Style.EMPTY, List.of()));
    }

    public static RichNode resolve(String code) {
        return resolve(code, new String[0]);
    }

    public static boolean hasAnyShortcode(String text) {
        int start = text.indexOf(':');
        while (start >= 0) {
            int end = text.indexOf(':', start + 1);
            if (end > start + 1) {
                String inner = text.substring(start + 1, end);
                int dot = inner.indexOf('.');
                String code = dot > 0 ? inner.substring(0, dot) : inner;
                if (has(code)) return true;
            }
            start = text.indexOf(':', start + 1);
        }
        return false;
    }

    public static List<String> getSuggestions(String prefix) {
        return Stream.concat(REGISTRY.keySet().stream(), PARAM_REGISTRY.keySet().stream())
                .filter(key -> key.startsWith(prefix))
                .sorted()
                .collect(Collectors.toList());
    }

    public static List<Map.Entry<String, String>> getAliasSuggestions(String prefix) {
        return ALIASES.entrySet().stream()
                .filter(e -> e.getKey().startsWith(prefix))
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toList());
    }
}
