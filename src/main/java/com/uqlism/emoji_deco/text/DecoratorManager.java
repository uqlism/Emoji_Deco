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
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

/**
 * Loads decorator definitions from assets/emoji_deco/decorators/*.json in datapacks.
 *
 * JSON format:
 * {
 *   "enable": true,
 *   "display": {
 *     "text": "",
 *     "bold": true,
 *     "extra": [ { "translate": "emoji_deco:slot" } ]
 *   }
 * }
 *
 * The special translate key "emoji_deco:slot" is replaced at render time
 * with the content inside the brackets: #bold[content]
 */
public class DecoratorManager implements PreparableReloadListener {

    public static final DecoratorManager INSTANCE = new DecoratorManager();
    private static final Logger LOGGER = LogUtils.getLogger();

    /** Raw JsonObject for each tag — we re-parse per invocation to inject slot content */
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

    /**
     * Resolves a decorator, substituting slotComponent for any "emoji_deco:slot" translate nodes.
     * Returns null if the decorator is not found.
     */
    public static Component resolve(String name, Component slotComponent) {
        JsonObject json = REGISTRY.get(name);
        if (json == null) return null;

        try {
            com.google.gson.JsonElement display = json.get("display").deepCopy();
            injectSlot(display, slotComponent);
            Component parsed = Component.Serializer.fromJson(display);
            if (parsed != null) {
                return ComponentTransformer.transform(parsed);
            }
        } catch (Exception e) {
            LOGGER.error("[EmojiDeco] Failed to resolve style tag '{}': {}", name, e.getMessage());
        }
        return slotComponent;
    }

    /**
     * Recursively walks the JsonElement tree and replaces any
     * {"translate":"emoji_deco:slot"} object with the serialized slotComponent JSON.
     */
    private static void injectSlot(com.google.gson.JsonElement el, Component slotComponent) {
        if (el.isJsonObject()) {
            com.google.gson.JsonObject obj = el.getAsJsonObject();
            if (obj.has("translate") && "emoji_deco:slot".equals(obj.get("translate").getAsString())) {
                com.google.gson.JsonElement slotJson = Component.Serializer.toJsonTree(slotComponent);
                com.google.gson.JsonObject slotObj;
                if (slotJson.isJsonObject()) {
                    slotObj = slotJson.getAsJsonObject();
                } else if (slotJson.isJsonPrimitive()) {
                    slotObj = new com.google.gson.JsonObject();
                    slotObj.addProperty("text", slotJson.getAsString());
                } else {
                    slotObj = new com.google.gson.JsonObject();
                    slotObj.addProperty("text", slotComponent.getString());
                }
                new java.util.ArrayList<>(obj.keySet()).forEach(obj::remove);
                slotObj.entrySet().forEach(e -> obj.add(e.getKey(), e.getValue()));
            } else {
                for (Map.Entry<String, com.google.gson.JsonElement> entry : obj.entrySet()) {
                    injectSlot(entry.getValue(), slotComponent);
                }
            }
        } else if (el.isJsonArray()) {
            for (com.google.gson.JsonElement child : el.getAsJsonArray()) {
                injectSlot(child, slotComponent);
            }
        }
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
                String name = text.substring(pos + 1, bracket);
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
