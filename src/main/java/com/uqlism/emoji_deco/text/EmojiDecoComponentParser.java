package com.uqlism.emoji_deco.text;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Converts the emoji_deco JSON component format into Minecraft Component trees.
 *
 * Dynamic nodes are resolved by expandDynamicProviders() before any structural
 * parsing. This is the sole location that maps provider/arg keys to runtime values;
 * all downstream code is unaware of them.
 *
 *   emoji_deco:arg          → JsonPrimitive (resolved arg string)
 *   emoji_deco:player_names → JsonArray of player name strings
 *
 * Structural component nodes handled after expansion:
 *   emoji_deco:slot, emoji_deco:size, emoji_deco:glow,
 *   emoji_deco:sprite, emoji_deco:player_head,
 *   standard MC fields (text/color/bold/italic/…/extra)
 */
public final class EmojiDecoComponentParser {

    private static final String[] EMPTY_ARGS = new String[0];

    private EmojiDecoComponentParser() {}

    // ── public API ────────────────────────────────────────────────────────────

    public static Component parse(JsonElement el, @Nullable Component slot) {
        return parse(el, slot, EMPTY_ARGS);
    }

    /** Expands dynamic nodes with args, then delegates to structural parsing. */
    public static Component parse(JsonElement el, @Nullable Component slot, String[] args) {
        if (el == null || el.isJsonNull()) return Component.empty();
        return parseExpanded(expandDynamicProviders(el, args), slot);
    }

    /**
     * Resolves a JsonElement to a flat list of strings.
     * Dynamic providers are expanded first; each primitive in the resulting array is collected.
     */
    public static List<String> resolveStringList(JsonElement el) {
        if (el == null) return Collections.emptyList();
        JsonElement expanded = expandDynamicProviders(el, EMPTY_ARGS);
        if (!expanded.isJsonArray()) return Collections.emptyList();
        List<String> result = new ArrayList<>();
        for (JsonElement item : expanded.getAsJsonArray()) {
            if (item.isJsonPrimitive()) result.add(item.getAsString());
        }
        return result;
    }

    // ── dynamic expansion (single source of truth for provider/arg keys) ──────

    /**
     * Recursively replaces dynamic nodes with resolved JSON values.
     * Only this method knows about emoji_deco:arg and emoji_deco:player_names.
     */
    private static JsonElement expandDynamicProviders(JsonElement el, String[] args) {
        if (el == null || el.isJsonNull() || el.isJsonPrimitive()) return el;

        if (el.isJsonObject()) {
            JsonObject obj = el.getAsJsonObject();

            if (obj.has("emoji_deco:arg")) {
                JsonObject spec = obj.getAsJsonObject("emoji_deco:arg");
                int    index = spec.has("index") ? spec.get("index").getAsInt()      : 0;
                String type  = spec.has("type")  ? spec.get("type").getAsString()    : "string";
                if (index < args.length) {
                    // arg is always a user-supplied string — coerce to the declared type
                    return coerceArg(args[index], type);
                } else {
                    // no arg supplied: use the default value as-is, preserving its JSON type
                    return spec.has("default") ? spec.get("default") : defaultForType(type);
                }
            }

            if (obj.has("emoji_deco:player_names")) {
                JsonArray arr = new JsonArray();
                Minecraft mc = Minecraft.getInstance();
                if (mc != null && mc.getConnection() != null) {
                    mc.getConnection().getOnlinePlayers().stream()
                            .map(pi -> pi.getProfile().getName())
                            .sorted(String.CASE_INSENSITIVE_ORDER)
                            .forEach(arr::add);
                }
                return arr;
            }

            // Recurse into all fields
            JsonObject result = new JsonObject();
            for (var entry : obj.entrySet()) {
                result.add(entry.getKey(), expandDynamicProviders(entry.getValue(), args));
            }
            return result;
        }

        if (el.isJsonArray()) {
            JsonArray result = new JsonArray();
            for (JsonElement item : el.getAsJsonArray()) {
                result.add(expandDynamicProviders(item, args));
            }
            return result;
        }

        return el;
    }

    // ── structural parsing (no knowledge of args or dynamic providers) ────────

    private static Component parseExpanded(JsonElement el, @Nullable Component slot) {
        if (el == null || el.isJsonNull()) return Component.empty();
        if (el.isJsonPrimitive()) return Component.literal(el.getAsString());
        if (el.isJsonArray()) {
            MutableComponent result = Component.empty();
            for (JsonElement child : el.getAsJsonArray()) result.append(parseExpanded(child, slot));
            return result;
        }
        if (el.isJsonObject()) return parseObject(el.getAsJsonObject(), slot);
        return Component.empty();
    }

    private static Component parseObject(JsonObject obj, @Nullable Component slot) {
        if (obj.has("emoji_deco:slot")) {
            return slot != null ? slot : Component.empty();
        }
        if (obj.has("emoji_deco:size")) {
            JsonObject spec = obj.getAsJsonObject("emoji_deco:size");
            float size = 1.0f;
            if (spec.has("size") && spec.get("size").isJsonPrimitive()) {
                try { size = spec.get("size").getAsFloat(); } catch (NumberFormatException ignored) {}
            }
            Component contents = spec.has("contents")
                    ? parseExpanded(spec.get("contents"), slot) : Component.empty();
            return MutableComponent.create(new TranslatableContents(
                    SizeRegistry.SIZE_KEY, null, new Object[]{String.valueOf(size), contents}));
        }
        if (obj.has("emoji_deco:glow")) {
            JsonObject spec = obj.getAsJsonObject("emoji_deco:glow");
            boolean applyGlow = !spec.has("glow") || spec.get("glow").getAsBoolean();
            Component contents = spec.has("contents")
                    ? parseExpanded(spec.get("contents"), slot) : Component.empty();
            return applyGlow
                    ? MutableComponent.create(new TranslatableContents(
                            SizeRegistry.GLOW_KEY, null, new Object[]{contents}))
                    : contents;
        }
        if (obj.has("emoji_deco:sprite")) {
            JsonObject spec     = obj.getAsJsonObject("emoji_deco:sprite");
            String atlas  = stringOf(spec.get("atlas"),  "");
            String sprite = stringOf(spec.get("sprite"), "");
            return SpriteRegistry.createComponent(atlas, sprite);
        }
        if (obj.has("emoji_deco:player_head")) {
            JsonObject spec = obj.getAsJsonObject("emoji_deco:player_head");
            String username = stringOf(spec.get("player"), "");
            return SpriteRegistry.createHeadComponent(username);
        }
        return parseStandard(obj, slot);
    }

    private static Component parseStandard(JsonObject obj, @Nullable Component slot) {
        MutableComponent comp = Component.literal(stringOf(obj.get("text"), ""));

        Style style = Style.EMPTY;
        if (obj.has("color")) {
            TextColor color = TextColor.parseColor(stringOf(obj.get("color"), ""));
            if (color != null) style = style.withColor(color);
        }
        if (obj.has("bold"))          style = style.withBold(obj.get("bold").getAsBoolean());
        if (obj.has("italic"))        style = style.withItalic(obj.get("italic").getAsBoolean());
        if (obj.has("strikethrough")) style = style.withStrikethrough(obj.get("strikethrough").getAsBoolean());
        if (obj.has("underlined"))    style = style.withUnderlined(obj.get("underlined").getAsBoolean());
        if (obj.has("obfuscated"))    style = style.withObfuscated(obj.get("obfuscated").getAsBoolean());
        if (obj.has("font")) {
            ResourceLocation fontLoc = ResourceLocation.tryParse(stringOf(obj.get("font"), ""));
            if (fontLoc != null) style = style.withFont(fontLoc);
        }
        if (!style.isEmpty()) comp = comp.withStyle(style);

        if (obj.has("extra") && obj.get("extra").isJsonArray()) {
            for (JsonElement child : obj.getAsJsonArray("extra")) comp.append(parseExpanded(child, slot));
        }
        return comp;
    }

    // ── utilities ─────────────────────────────────────────────────────────────

    /** Coerces a user-supplied arg string to the JSON primitive type declared in the arg spec. */
    private static JsonPrimitive coerceArg(String value, String type) {
        return switch (type) {
            case "boolean" -> new JsonPrimitive(
                    value.equalsIgnoreCase("true") || value.equals("1") || value.equalsIgnoreCase("yes"));
            case "integer" -> {
                try { yield new JsonPrimitive(Integer.parseInt(value)); }
                catch (NumberFormatException e) { yield new JsonPrimitive(0); }
            }
            case "float", "number" -> {
                try { yield new JsonPrimitive(Float.parseFloat(value)); }
                catch (NumberFormatException e) { yield new JsonPrimitive(0.0f); }
            }
            default -> new JsonPrimitive(value);
        };
    }

    /** Returns a sensible zero-value primitive for each type, used when no default is declared. */
    private static JsonPrimitive defaultForType(String type) {
        return switch (type) {
            case "boolean"        -> new JsonPrimitive(false);
            case "integer"        -> new JsonPrimitive(0);
            case "float", "number"-> new JsonPrimitive(0.0f);
            default               -> new JsonPrimitive("");
        };
    }

    /** Returns the string value of el, or fallback if el is null or not a primitive. */
    private static String stringOf(@Nullable JsonElement el, String fallback) {
        return (el != null && el.isJsonPrimitive()) ? el.getAsString() : fallback;
    }

    /**
     * Walks the JSON tree and returns the first emoji_deco:arg spec whose "index" equals
     * the given value, or null if not found. Used by managers to locate suggestion specs.
     */
    @Nullable
    public static JsonObject findArgSpec(JsonElement el, int index) {
        if (el == null) return null;
        if (el.isJsonObject()) {
            JsonObject obj = el.getAsJsonObject();
            if (obj.has("emoji_deco:arg")) {
                JsonObject spec = obj.getAsJsonObject("emoji_deco:arg");
                if ((spec.has("index") ? spec.get("index").getAsInt() : 0) == index) return spec;
            }
            for (var entry : obj.entrySet()) {
                JsonObject found = findArgSpec(entry.getValue(), index);
                if (found != null) return found;
            }
        } else if (el.isJsonArray()) {
            for (JsonElement child : el.getAsJsonArray()) {
                JsonObject found = findArgSpec(child, index);
                if (found != null) return found;
            }
        }
        return null;
    }
}
