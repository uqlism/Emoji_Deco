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

import com.google.gson.JsonArray;
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
            JsonArray topArgSpecs = json.has("args") ? json.getAsJsonArray("args") : null;
            return EmojiDecoComponentParser.parse(json.get("display"), slotNode, args, topArgSpecs);
        } catch (Exception e) {
            LOGGER.error("[EmojiDeco] Failed to resolve style tag '{}': {}", name, e.getMessage());
        }
        return slotNode;
    }

    public static RichNode resolve(String name, RichNode slotNode) {
        return resolve(name, slotNode, new String[0]);
    }

    /**
     * Returns the raw "suggestions" JsonElement for the arg at argIndex, or null if absent.
     * Checks the top-level "args" array first, then falls back to scanning the display JSON.
     */
    public static com.google.gson.JsonElement getArgSuggestionsSpec(String tagName, int argIndex) {
        JsonObject json = REGISTRY.get(tagName);
        if (json == null) return null;
        if (json.has("args") && json.get("args").isJsonArray()) {
            JsonArray topArgs = json.getAsJsonArray("args");
            if (argIndex >= 0 && argIndex < topArgs.size() && topArgs.get(argIndex).isJsonObject()) {
                JsonObject spec = topArgs.get(argIndex).getAsJsonObject();
                if (spec.has("suggestions")) return spec.get("suggestions");
            }
        }
        if (!json.has("display")) return null;
        JsonObject argSpec = EmojiDecoComponentParser.findArgSpec(json.get("display"), argIndex);
        if (argSpec == null || !argSpec.has("suggestions")) return null;
        return argSpec.get("suggestions");
    }

    /**
     * Returns the label for autocomplete display.
     * Priority: explicit top-level "label" > auto-built from arg "label" fields > "#name[]".
     */
    public static String getLabel(String name) {
        JsonObject json = REGISTRY.get(name);
        if (json == null) return "#" + name + "[]";

        if (json.has("label") && json.get("label").isJsonPrimitive())
            return json.get("label").getAsString();

        var args = effectiveArgSpecs(json);
        if (!args.isEmpty()) {
            int lastVisible = -1;
            for (var entry : args.entrySet()) {
                JsonObject spec = entry.getValue();
                boolean hidden = spec.has("hidden") && spec.get("hidden").isJsonPrimitive()
                        && spec.get("hidden").getAsBoolean();
                if (!hidden) lastVisible = entry.getKey();
            }
            if (lastVisible >= 0) {
                StringBuilder sb = new StringBuilder("#").append(name).append(".");
                boolean first = true;
                for (var entry : args.entrySet()) {
                    if (entry.getKey() > lastVisible) break;
                    if (!first) sb.append(",");
                    first = false;
                    JsonObject spec = entry.getValue();
                    boolean hidden = spec.has("hidden") && spec.get("hidden").isJsonPrimitive()
                            && spec.get("hidden").getAsBoolean();
                    if (!hidden && spec.has("label") && spec.get("label").isJsonPrimitive())
                        sb.append(spec.get("label").getAsString());
                    else
                        sb.append("<arg").append(entry.getKey() + 1).append(">");
                }
                sb.append("[]");
                return sb.toString();
            }
        }

        return "#" + name + "[]";
    }

    /** display 内スキャン結果をベースにトップレベル args で上書きした有効スペックマップを返す。 */
    private static java.util.Map<Integer, JsonObject> effectiveArgSpecs(JsonObject json) {
        java.util.Map<Integer, JsonObject> specs = json.has("display")
                ? new java.util.TreeMap<>(EmojiDecoComponentParser.findAllArgSpecs(json.get("display")))
                : new java.util.TreeMap<>();
        if (json.has("args") && json.get("args").isJsonArray()) {
            JsonArray topArgs = json.getAsJsonArray("args");
            for (int i = 0; i < topArgs.size(); i++) {
                if (topArgs.get(i).isJsonObject()) specs.put(i, topArgs.get(i).getAsJsonObject());
            }
        }
        return specs;
    }

    /**
     * Returns a Component built from the preview array defined in the decorator JSON.
     * Falls back to resolving the decorator with the name as slot content if absent.
     */
    public static net.minecraft.network.chat.Component getPreview(String name) {
        JsonObject json = REGISTRY.get(name);
        if (json != null && json.has("preview")) {
            try {
                RichNode node = EmojiDecoComponentParser.parse(json.get("preview"), null);
                if (node != null) return node.toComponent();
            } catch (Exception e) {
                LOGGER.warn("[EmojiDeco] Failed to parse preview for decorator '{}': {}", name, e.getMessage());
            }
        }
        RichNode fallback = resolve(name, new RichNode.Text(name, net.minecraft.network.chat.Style.EMPTY, List.of()));
        return fallback != null ? fallback.toComponent() : net.minecraft.network.chat.Component.literal(getLabel(name));
    }

    /** Returns all tag names that start with prefix, sorted. */
    public static List<String> getSuggestions(String prefix) {
        return REGISTRY.keySet().stream()
                .filter(key -> key.startsWith(prefix))
                .sorted()
                .collect(Collectors.toList());
    }

    static boolean isValidTagName(String name) {
        if (name.isEmpty()) return false;
        for (char c : name.toCharArray()) {
            if (!Character.isLetterOrDigit(c) && c != '_' && c != '-') return false;
        }
        return true;
    }
}
