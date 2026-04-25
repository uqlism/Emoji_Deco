package com.uqlism.runicink.text;

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
import org.slf4j.Logger;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

public class ShortcodeManager implements PreparableReloadListener {

    public static final ShortcodeManager INSTANCE = new ShortcodeManager();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, Component> REGISTRY = new ConcurrentHashMap<>();

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
                    LOGGER.info("[RunicInk] Loaded {} shortcode(s)", REGISTRY.size());
                }, gameExecutor);
    }

    private static Map<String, Component> loadAll(ResourceManager resourceManager) {
        Map<String, Component> loaded = new HashMap<>();
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
            } catch (Exception e) {
                LOGGER.error("[RunicInk] Failed to load shortcode {}: {}", location, e.getMessage());
            }
        }
        return loaded;
    }

    private static Component parseDisplay(JsonElement displayElement) {
        if (!displayElement.isJsonObject()) {
            return Component.literal(displayElement.getAsString());
        }
        JsonObject display = displayElement.getAsJsonObject();

        // Custom format: {"type":"text","text":"...","font":"..."}
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

        // Fallback: standard Minecraft Component JSON
        try {
            Component parsed = Component.Serializer.fromJson(displayElement);
            return parsed != null ? parsed : Component.literal(displayElement.toString());
        } catch (Exception e) {
            return Component.literal(displayElement.toString());
        }
    }

    public static boolean has(String code) {
        return REGISTRY.containsKey(code);
    }

    public static Component resolve(String code) {
        return REGISTRY.getOrDefault(code, Component.literal(":" + code + ":"));
    }
}
