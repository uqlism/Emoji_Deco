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

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;
public class DecoratorManager implements PreparableReloadListener {

    public static final DecoratorManager INSTANCE = new DecoratorManager();
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Map<String, JsonObject> REGISTRY = new ConcurrentHashMap<>();

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
                .thenAcceptAsync(loaded -> {
                    REGISTRY.clear();
                    REGISTRY.putAll(loaded);
                    LOGGER.info("[EmojiDeco] Loaded {} style tag(s)", REGISTRY.size());
                }, gameExecutor);
    }

    private static Map<String, JsonObject> loadAll(ResourceManager resourceManager) {
        Map<String, JsonObject> result = new HashMap<>();
        Map<ResourceLocation, Resource> resources = resourceManager.listResources(
                "decorators", path -> path.getPath().endsWith(".json"));

        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            ResourceLocation location = entry.getKey();
            try (InputStreamReader reader = new InputStreamReader(
                    entry.getValue().open(), StandardCharsets.UTF_8)) {

                JsonObject json = GsonHelper.parse(reader);

                if (json.has("enable") && !json.get("enable").getAsBoolean()) continue;
                if (!json.has("display")) continue;

                // "decorators/bold.json" -> "bold"
                String path = location.getPath();
                String name = path.substring("decorators/".length(), path.length() - ".json".length());

                result.put(name, json);

            } catch (Exception e) {
                LOGGER.error("[EmojiDeco] Failed to load decorator {}: {}", location, e.getMessage());
            }
        }
        return result;
    }

    public static boolean has(String name) {
        return REGISTRY.containsKey(name);
    }

    public static RichNode resolve(String name, RichNode slotNode, String[] args) {
        JsonObject json = REGISTRY.get(name);
        if (json == null) return null;
        try {
            return EmojiDecoComponentParser.parse(json.get("display"), slotNode, args);
        } catch (Exception e) {
            LOGGER.error("[EmojiDeco] Failed to resolve style tag '{}': {}", name, e.getMessage());
        }
        return slotNode;
    }

    public static RichNode resolve(String name, RichNode slotNode) {
        return resolve(name, slotNode, new String[0]);
    }

    /**
     * Returns the raw "suggestions" JsonElement for the emoji_deco:arg at argIndex
     * inside the given decorator's display JSON, or null if absent.
     */
    public static com.google.gson.JsonElement getArgSuggestionsSpec(String tagName, int argIndex) {
        JsonObject json = REGISTRY.get(tagName);
        if (json == null || !json.has("display")) return null;
        JsonObject argSpec = EmojiDecoComponentParser.findArgSpec(json.get("display"), argIndex);
        if (argSpec == null || !argSpec.has("suggestions")) return null;
        return argSpec.get("suggestions");
    }

    /** Returns all tag names that start with prefix, sorted. */
    public static List<String> getSuggestions(String prefix) {
        return REGISTRY.keySet().stream()
                .filter(key -> key.startsWith(prefix))
                .sorted()
                .collect(Collectors.toList());
    }

    /** Returns true if text contains at least one valid #tag[...] pattern. */
    public static boolean hasAnyTag(String text) {
        int pos = text.indexOf('#');
        while (pos >= 0) {
            int bracket = text.indexOf('[', pos + 1);
            if (bracket > pos + 1) {
                String segment = text.substring(pos + 1, bracket);
                int dot = segment.indexOf('.');
                String name = dot > 0 ? segment.substring(0, dot) : segment;
                if (isValidTagName(name) && REGISTRY.containsKey(name)) return true;
            }
            pos = text.indexOf('#', pos + 1);
        }
        return false;
    }

    static boolean isValidTagName(String name) {
        if (name.isEmpty()) return false;
        for (char c : name.toCharArray()) {
            if (!Character.isLetterOrDigit(c) && c != '_' && c != '-') return false;
        }
        return true;
    }
}
