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

    /** Pre-parsed nodes for shortcodes without dynamic content */
    private static final Map<String, RichNode> REGISTRY = new ConcurrentHashMap<>();
    /** Raw display JSON for shortcodes that contain dynamic nodes — parsed per call */
    private static final Map<String, JsonElement> PARAM_REGISTRY = new ConcurrentHashMap<>();
    /** Full JSON for all shortcodes — used for label/preview lookup */
    private static final Map<String, JsonObject> JSON_REGISTRY = new ConcurrentHashMap<>();
    /** alias text → canonical shortcode name */
    private static final Map<String, String> ALIASES = new ConcurrentHashMap<>();

    private record LoadResult(
            Map<String, RichNode>    registry,
            Map<String, JsonElement> paramRegistry,
            Map<String, JsonObject>  jsonRegistry,
            Map<String, String>      aliases) {}

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
                    JSON_REGISTRY.clear();
                    JSON_REGISTRY.putAll(result.jsonRegistry());
                    ALIASES.clear();
                    ALIASES.putAll(result.aliases());
                    LOGGER.info("[EmojiDeco] Loaded {} shortcode(s) ({} parametric), {} alias(es)",
                            REGISTRY.size() + PARAM_REGISTRY.size(), PARAM_REGISTRY.size(), ALIASES.size());
                }, gameExecutor);
    }

    private static LoadResult loadAll(ResourceManager resourceManager) {
        Map<String, RichNode>    loaded        = new HashMap<>();
        Map<String, JsonElement> paramLoaded   = new HashMap<>();
        Map<String, JsonObject>  jsonLoaded    = new HashMap<>();
        Map<String, String>      loadedAliases = new HashMap<>();
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
                jsonLoaded.put(name, json);

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
        return new LoadResult(loaded, paramLoaded, jsonLoaded, loadedAliases);
    }

    private static boolean isDynamic(JsonElement el) {
        if (el.isJsonObject()) {
            JsonObject obj = el.getAsJsonObject();
            String t = obj.has("type") ? obj.get("type").getAsString() : "";
            if ("emoji_deco:arg".equals(t) || "emoji_deco:player_names".equals(t) || "emoji_deco:time".equals(t)) return true;
            for (var e : obj.entrySet()) if (isDynamic(e.getValue())) return true;
        } else if (el.isJsonArray()) {
            for (JsonElement child : el.getAsJsonArray()) if (isDynamic(child)) return true;
        }
        return false;
    }

    /**
     * Returns the label for autocomplete display.
     * Priority: explicit top-level "label" > auto-built from arg "label" fields > ":code:".
     */
    public static String getLabel(String code) {
        JsonObject json = JSON_REGISTRY.get(code);
        if (json == null) return ":" + code + ":";

        if (json.has("label") && json.get("label").isJsonPrimitive())
            return json.get("label").getAsString();

        // トップレベル args → display 内スキャン の順で effective なスペックを構築
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
                StringBuilder sb = new StringBuilder(":").append(code).append(".");
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
                sb.append(":");
                return sb.toString();
            }
        }

        return ":" + code + ":";
    }

    /** Returns a Component for autocomplete preview. Falls back to resolving with empty args. */
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
        // display が URL ソースを含む場合は toComponent() で大量フェッチが起きるため
        // オートコンプリート一覧ではテキストのみ表示する
        if (json != null && displayHasUrlSource(json.get("display"))) {
            return net.minecraft.network.chat.Component.literal(":" + code + ":");
        }
        return resolve(code).toComponent();
    }

    private static boolean displayHasUrlSource(@Nullable com.google.gson.JsonElement el) {
        if (el == null) return false;
        if (el.isJsonObject()) {
            com.google.gson.JsonObject obj = el.getAsJsonObject();
            if ("emoji_deco:fetch_url".equals(obj.has("type") ? obj.get("type").getAsString() : ""))
                return true;
            for (var e : obj.entrySet()) if (displayHasUrlSource(e.getValue())) return true;
        } else if (el.isJsonArray()) {
            for (com.google.gson.JsonElement item : el.getAsJsonArray())
                if (displayHasUrlSource(item)) return true;
        }
        return false;
    }

    public static JsonElement getArgSuggestionsSpec(String code, int argIndex) {
        // トップレベル args を優先
        JsonObject json = JSON_REGISTRY.get(code);
        if (json != null) {
            JsonObject spec = topLevelArgSpec(json, argIndex);
            if (spec != null && spec.has("suggestions")) return spec.get("suggestions");
        }
        // display 内のインライン emoji_deco:arg にフォールバック
        JsonElement display = PARAM_REGISTRY.get(code);
        if (display == null) return null;
        JsonObject argSpec = EmojiDecoComponentParser.findArgSpec(display, argIndex);
        if (argSpec == null || !argSpec.has("suggestions")) return null;
        return argSpec.get("suggestions");
    }

    /**
     * トップレベル "args" 配列と display 内スキャン結果をマージした有効スペックマップを返す。
     * トップレベルが優先される。
     */
    private static java.util.Map<Integer, JsonObject> effectiveArgSpecs(JsonObject json) {
        // display 内スキャンをベースにする
        JsonElement display = json.get("display");
        java.util.Map<Integer, JsonObject> specs = display != null
                ? EmojiDecoComponentParser.findAllArgSpecs(display)
                : new java.util.TreeMap<>();

        // トップレベル args で上書き
        if (json.has("args") && json.get("args").isJsonArray()) {
            JsonArray topArgs = json.getAsJsonArray("args");
            for (int i = 0; i < topArgs.size(); i++) {
                if (topArgs.get(i).isJsonObject())
                    specs.put(i, topArgs.get(i).getAsJsonObject());
            }
        }
        return specs;
    }

    /** トップレベル "args" 配列の指定インデックスのスペックを返す。なければ null。 */
    private static @Nullable JsonObject topLevelArgSpec(JsonObject json, int argIndex) {
        if (!json.has("args") || !json.get("args").isJsonArray()) return null;
        JsonArray topArgs = json.getAsJsonArray("args");
        if (argIndex < 0 || argIndex >= topArgs.size()) return null;
        JsonElement el = topArgs.get(argIndex);
        return el.isJsonObject() ? el.getAsJsonObject() : null;
    }

    public static boolean has(String code) {
        return REGISTRY.containsKey(code) || PARAM_REGISTRY.containsKey(code);
    }

    public static RichNode resolve(String code, String[] args) {
        JsonElement display = PARAM_REGISTRY.get(code);
        if (display != null) {
            JsonObject json = JSON_REGISTRY.get(code);
            JsonArray topArgSpecs = (json != null && json.has("args")) ? json.getAsJsonArray("args") : null;
            return EmojiDecoComponentParser.parse(display, null, args, topArgSpecs);
        }
        return REGISTRY.getOrDefault(code, new RichNode.Text(":" + code + ":", Style.EMPTY, List.of()));
    }

    public static RichNode resolve(String code) {
        return resolve(code, new String[0]);
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
