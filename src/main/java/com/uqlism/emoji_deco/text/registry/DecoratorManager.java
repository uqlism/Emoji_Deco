package com.uqlism.emoji_deco.text.registry;

import com.uqlism.emoji_deco.text.hydrate.HydrateCache;
import com.uqlism.emoji_deco.text.hydrate.HydrateContext;
import com.uqlism.emoji_deco.text.hydrate.NodeHydrator;
import com.uqlism.emoji_deco.text.ir.ParsedNode;
import com.uqlism.emoji_deco.text.ir.RichNode;
import com.uqlism.emoji_deco.text.parse.EmojiDecoComponentParser;
import com.uqlism.emoji_deco.text.parse.ParsedNodeParser;

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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;
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

    /** ParsedNode per decorator — parsed at load time. */
    private static final Map<String, ParsedNode>  PARSED_REGISTRY = new ConcurrentHashMap<>();
    /** Per-decorator hydration result cache. */
    private static final Map<String, HydrateCache> HYDRATE_CACHES  = new ConcurrentHashMap<>();
    /** Full JSON per decorator — for label / preview / suggestion lookup. */
    private static final Map<String, JsonObject>  JSON_REGISTRY   = new ConcurrentHashMap<>();

    private record LoadResult(
            Map<String, ParsedNode> parsedRegistry,
            Map<String, JsonObject> jsonRegistry) {}

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
                    PARSED_REGISTRY.clear();
                    PARSED_REGISTRY.putAll(loaded.parsedRegistry());
                    JSON_REGISTRY.clear();
                    JSON_REGISTRY.putAll(loaded.jsonRegistry());
                    HYDRATE_CACHES.clear();
                    LOGGER.info("[EmojiDeco] Loaded {} style tag(s)", PARSED_REGISTRY.size());
                }, gameExecutor);
    }

    private static LoadResult loadAll(ResourceManager resourceManager) {
        Map<String, ParsedNode> parsed = new HashMap<>();
        Map<String, JsonObject> json   = new HashMap<>();
        Map<ResourceLocation, Resource> resources = resourceManager.listResources(
                "decorators", path -> path.getPath().endsWith(".json"));

        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            ResourceLocation location = entry.getKey();
            try (InputStreamReader reader = new InputStreamReader(
                    entry.getValue().open(), StandardCharsets.UTF_8)) {

                JsonObject j = GsonHelper.parse(reader);
                if (j.has("enable") && !j.get("enable").getAsBoolean()) continue;
                if (!j.has("display")) continue;

                String path = location.getPath();
                String name = path.substring("decorators/".length(), path.length() - ".json".length());

                JsonArray topArgSpecs = j.has("args") ? j.getAsJsonArray("args") : null;
                parsed.put(name, ParsedNodeParser.parse(j.get("display"), topArgSpecs));
                json.put(name, j);

            } catch (Exception e) {
                LOGGER.error("[EmojiDeco] Failed to load decorator {}: {}", location, e.getMessage());
            }
        }
        return new LoadResult(parsed, json);
    }

    // ── Hydration ─────────────────────────────────────────────────────────────

    @Nullable
    public static RichNode hydrateWith(String name, RichNode slotNode, String[] args) {
        ParsedNode node = PARSED_REGISTRY.get(name);
        if (node == null) return null;

        JsonObject json = JSON_REGISTRY.get(name);
        JsonArray  topArgSpecs = (json != null && json.has("args")) ? json.getAsJsonArray("args") : null;
        HydrateContext ctx   = new HydrateContext(args, topArgSpecs, slotNode);
        HydrateCache   cache = HYDRATE_CACHES.computeIfAbsent(name, k -> new HydrateCache());
        long tick = NodeHydrator.currentTick();

        RichNode cached = cache.lookup(ctx, slotNode, tick);
        if (cached != null) return cached;

        HydrateContext.Tracked tracked = ctx.track();
        RichNode result = NodeHydrator.hydrateTracked(node, tracked);
        cache.store(tracked.extractPattern(), result, tick);
        return result;
    }

    @Nullable
    public static RichNode resolve(String name, RichNode slotNode, String[] args) {
        return hydrateWith(name, slotNode, args);
    }

    @Nullable
    public static RichNode resolve(String name, RichNode slotNode) {
        return hydrateWith(name, slotNode, new String[0]);
    }

    public static void gcCaches(long currentTick) {
        HYDRATE_CACHES.values().forEach(c -> c.gc(currentTick));
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    public static boolean has(String name)       { return PARSED_REGISTRY.containsKey(name); }
    public static boolean hasParsed(String name) { return PARSED_REGISTRY.containsKey(name); }

    public static List<String> getSuggestions(String prefix) {
        return PARSED_REGISTRY.keySet().stream()
                .filter(k -> k.startsWith(prefix))
                .sorted()
                .collect(Collectors.toList());
    }

    public static List<SuggestionEngine.SearchResult> searchSuggestions(String query, int maxResults) {
        return SuggestionEngine.search(PARSED_REGISTRY.keySet(), java.util.Map.of(), query, maxResults);
    }

    // ── Label / Preview (autocomplete) ────────────────────────────────────────

    public static com.google.gson.JsonElement getArgSuggestionsSpec(String tagName, int argIndex) {
        JsonObject json = JSON_REGISTRY.get(tagName);
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

    public static String getLabel(String name) {
        JsonObject json = JSON_REGISTRY.get(name);
        if (json == null) return "#" + name + "[]";

        if (json.has("label") && json.get("label").isJsonPrimitive())
            return json.get("label").getAsString();

        var args = effectiveArgSpecs(json);
        if (!args.isEmpty()) {
            int lastVisible = -1;
            for (var e : args.entrySet()) {
                JsonObject spec = e.getValue();
                if (!(spec.has("hidden") && spec.get("hidden").isJsonPrimitive()
                        && spec.get("hidden").getAsBoolean())) lastVisible = e.getKey();
            }
            if (lastVisible >= 0) {
                StringBuilder sb = new StringBuilder("#").append(name).append(".");
                boolean first = true;
                for (var e : args.entrySet()) {
                    if (e.getKey() > lastVisible) break;
                    if (!first) sb.append(",");
                    first = false;
                    JsonObject spec = e.getValue();
                    boolean hidden = spec.has("hidden") && spec.get("hidden").isJsonPrimitive()
                            && spec.get("hidden").getAsBoolean();
                    if (!hidden && spec.has("label") && spec.get("label").isJsonPrimitive())
                        sb.append(spec.get("label").getAsString());
                    else sb.append("<arg").append(e.getKey() + 1).append(">");
                }
                sb.append("[]");
                return sb.toString();
            }
        }
        return "#" + name + "[]";
    }

    public static net.minecraft.network.chat.Component getPreview(String name) {
        JsonObject json = JSON_REGISTRY.get(name);
        if (json != null && json.has("preview")) {
            try {
                RichNode node = EmojiDecoComponentParser.parse(json.get("preview"), null);
                if (node != null) return node.toComponent();
            } catch (Exception e) {
                LOGGER.warn("[EmojiDeco] Failed to parse preview for decorator '{}': {}", name, e.getMessage());
            }
        }
        RichNode slot     = new RichNode.Text(name, net.minecraft.network.chat.Style.EMPTY, List.of());
        RichNode fallback = resolve(name, slot);
        return fallback != null ? fallback.toComponent()
                : net.minecraft.network.chat.Component.literal(getLabel(name));
    }

    public static boolean isValidTagName(String name) {
        if (name.isEmpty()) return false;
        for (char c : name.toCharArray()) {
            if (!Character.isLetterOrDigit(c) && c != '_' && c != '-') return false;
        }
        return true;
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

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
}
