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

    /** ParsedNode for every shortcode — parsed at load time, hydrated on demand. */
    private static final Map<String, ParsedNode>  PARSED_REGISTRY = new ConcurrentHashMap<>();
    /** Per-shortcode hydration result cache (pattern → RichNode). */
    private static final Map<String, HydrateCache> HYDRATE_CACHES  = new ConcurrentHashMap<>();
    /** Full JSON for all shortcodes — used for label / preview / suggestion lookup. */
    private static final Map<String, JsonObject>  JSON_REGISTRY   = new ConcurrentHashMap<>();
    /** alias text → canonical shortcode name */
    private static final Map<String, String>      ALIASES         = new ConcurrentHashMap<>();

    private record LoadResult(
            Map<String, ParsedNode> parsedRegistry,
            Map<String, JsonObject> jsonRegistry,
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
                    PARSED_REGISTRY.clear();
                    PARSED_REGISTRY.putAll(result.parsedRegistry());
                    JSON_REGISTRY.clear();
                    JSON_REGISTRY.putAll(result.jsonRegistry());
                    ALIASES.clear();
                    ALIASES.putAll(result.aliases());
                    HYDRATE_CACHES.clear();
                    LOGGER.info("[EmojiDeco] Loaded {} shortcode(s), {} alias(es)",
                            PARSED_REGISTRY.size(), ALIASES.size());
                }, gameExecutor);
    }

    private static LoadResult loadAll(ResourceManager resourceManager) {
        Map<String, ParsedNode> parsed  = new HashMap<>();
        Map<String, JsonObject> json    = new HashMap<>();
        Map<String, String>     aliases = new HashMap<>();
        Map<ResourceLocation, Resource> resources = resourceManager.listResources(
                "shortcodes", path -> path.getPath().endsWith(".json"));

        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            ResourceLocation location = entry.getKey();
            try (InputStreamReader reader = new InputStreamReader(
                    entry.getValue().open(), StandardCharsets.UTF_8)) {

                JsonObject j = GsonHelper.parse(reader);
                if (j.has("enable") && !j.get("enable").getAsBoolean()) continue;
                if (!j.has("display")) continue;

                String path = location.getPath();
                String name = path.substring("shortcodes/".length(), path.length() - ".json".length());

                JsonArray topArgSpecs = j.has("args") ? j.getAsJsonArray("args") : null;
                parsed.put(name, ParsedNodeParser.parse(j.get("display"), topArgSpecs));
                json.put(name, j);

                if (j.has("aliases") && j.get("aliases").isJsonArray()) {
                    for (JsonElement alias : j.getAsJsonArray("aliases")) {
                        String a = alias.getAsString().trim();
                        if (!a.isEmpty()) aliases.put(a, name);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("[EmojiDeco] Failed to load shortcode {}: {}", location, e.getMessage());
            }
        }
        return new LoadResult(parsed, json, aliases);
    }

    // ── Hydration ─────────────────────────────────────────────────────────────

    public static RichNode hydrateWith(String code, String[] args, @Nullable RichNode slot) {
        // ALIASES は使わない — 正規名のみ描画対象（alias はサジェスト専用）
        ParsedNode node  = PARSED_REGISTRY.get(code);
        if (node == null) return new RichNode.Text(":" + code + ":", Style.EMPTY, List.of());

        JsonObject json = JSON_REGISTRY.get(code);
        JsonArray  topArgSpecs = (json != null && json.has("args")) ? json.getAsJsonArray("args") : null;
        HydrateContext ctx  = new HydrateContext(args, topArgSpecs, slot);
        HydrateCache   cache = HYDRATE_CACHES.computeIfAbsent(code, k -> new HydrateCache());
        long tick = NodeHydrator.currentTick();

        RichNode cached = cache.lookup(ctx, slot, tick);
        if (cached != null) return cached;

        HydrateContext.Tracked tracked = ctx.track();
        RichNode result = NodeHydrator.hydrateTracked(node, tracked);
        cache.store(tracked.extractPattern(), result, tick);
        return result;
    }

    public static RichNode resolve(String code, String[] args) { return hydrateWith(code, args, null); }
    public static RichNode resolve(String code)               { return hydrateWith(code, new String[0], null); }

    public static void gcCaches(long currentTick) {
        HYDRATE_CACHES.values().forEach(c -> c.gc(currentTick));
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    /** Returns true if code is a canonical shortcode name (aliases are excluded intentionally). */
    public static boolean has(String code) {
        return PARSED_REGISTRY.containsKey(code);
    }

    /** Used by NodeHydrator to check before resolving apply_shortcode. */
    public static boolean hasParsed(String code) {
        return PARSED_REGISTRY.containsKey(code);
    }

    public static List<String> getSuggestions(String prefix) {
        return PARSED_REGISTRY.keySet().stream()
                .filter(k -> k.startsWith(prefix))
                .sorted()
                .collect(Collectors.toList());
    }

    public static List<Map.Entry<String, String>> getAliasSuggestions(String prefix) {
        return ALIASES.entrySet().stream()
                .filter(e -> e.getKey().startsWith(prefix))
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toList());
    }

    // ── Label / Preview / Suggestions (autocomplete) ─────────────────────────

    public static String getLabel(String code) {
        JsonObject json = JSON_REGISTRY.get(code);
        if (json == null) return ":" + code + ":";

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
                StringBuilder sb = new StringBuilder(":").append(code).append(".");
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
                sb.append(":");
                return sb.toString();
            }
        }
        return ":" + code + ":";
    }

    public static net.minecraft.network.chat.Component getPreview(String code) {
        JsonObject json = JSON_REGISTRY.get(code);
        if (json != null && json.has("preview")) {
            try {
                RichNode node = EmojiDecoComponentParser.parse(json.get("preview"), null);
                if (node != null) return node.toComponent();
            } catch (Exception e) {
                LOGGER.warn("[EmojiDeco] Failed to parse preview for shortcode '{}': {}", code, e.getMessage());
            }
        }
        return resolve(code).toComponent();
    }

    public static JsonElement getArgSuggestionsSpec(String code, int argIndex) {
        JsonObject json = JSON_REGISTRY.get(code);
        if (json == null) return null;
        JsonObject spec = topLevelArgSpec(json, argIndex);
        if (spec != null && spec.has("suggestions")) return spec.get("suggestions");
        JsonElement display = json.get("display");
        if (display == null) return null;
        JsonObject argSpec = EmojiDecoComponentParser.findArgSpec(display, argIndex);
        if (argSpec == null || !argSpec.has("suggestions")) return null;
        return argSpec.get("suggestions");
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private static java.util.Map<Integer, JsonObject> effectiveArgSpecs(JsonObject json) {
        JsonElement display = json.get("display");
        java.util.Map<Integer, JsonObject> specs = display != null
                ? new java.util.TreeMap<>(EmojiDecoComponentParser.findAllArgSpecs(display))
                : new java.util.TreeMap<>();
        if (json.has("args") && json.get("args").isJsonArray()) {
            JsonArray topArgs = json.getAsJsonArray("args");
            for (int i = 0; i < topArgs.size(); i++) {
                if (topArgs.get(i).isJsonObject()) specs.put(i, topArgs.get(i).getAsJsonObject());
            }
        }
        return specs;
    }

    @Nullable
    private static JsonObject topLevelArgSpec(JsonObject json, int argIndex) {
        if (!json.has("args") || !json.get("args").isJsonArray()) return null;
        JsonArray topArgs = json.getAsJsonArray("args");
        if (argIndex < 0 || argIndex >= topArgs.size()) return null;
        JsonElement el = topArgs.get(argIndex);
        return el.isJsonObject() ? el.getAsJsonObject() : null;
    }
}
