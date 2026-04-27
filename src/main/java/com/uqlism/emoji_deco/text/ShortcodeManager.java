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
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
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
        // Custom format: {"type":"text","text":"...","font":"..."}
        if (displayElement.isJsonObject()) {
            JsonObject display = displayElement.getAsJsonObject();
            if (display.has("type") && "text".equals(display.get("type").getAsString())) {
                String text = display.has("text") ? display.get("text").getAsString() : "";
                MutableComponent comp = Component.literal(text);
                Style style = Style.EMPTY;

                if (display.has("font")) {
                    ResourceLocation fontLoc = ResourceLocation.tryParse(display.get("font").getAsString());
                    if (fontLoc != null) style = style.withFont(fontLoc);
                }
                if (display.has("bold")) {
                    style = style.withBold(display.get("bold").getAsBoolean());
                }
                if (display.has("italic")) {
                    style = style.withItalic(display.get("italic").getAsBoolean());
                }
                if (display.has("underlined")) {
                    style = style.withUnderlined(display.get("underlined").getAsBoolean());
                }
                if (display.has("color")) {
                    TextColor color = TextColor.parseColor(display.get("color").getAsString());
                    if (color != null) style = style.withColor(color);
                }
                return comp.withStyle(style);
            }
        }

        // 標準Minecraft Component JSON（文字列・オブジェクト・配列に対応）
        // ComponentTransformer.transform() で runicink:sprite 等を解決する
        try {
            Component parsed = Component.Serializer.fromJson(displayElement);
            if (parsed != null) {
                return ComponentTransformer.transform(parsed);
            }
        } catch (Exception e) {
            LOGGER.error("[EmojiDeco] Failed to parse shortcode display: {}", e.getMessage());
        }
        return Component.literal(displayElement.toString());
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
