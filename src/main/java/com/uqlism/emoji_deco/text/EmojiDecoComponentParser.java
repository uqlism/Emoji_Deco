package com.uqlism.emoji_deco.text;

import com.google.gson.JsonArray;
import com.uqlism.emoji_deco.render.sequence.LightMode;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Converts the emoji_deco JSON component format into RichNode trees.
 *
 * Dynamic nodes are resolved by expandDynamicProviders() before structural parsing.
 * This is the sole location that maps provider/arg keys to runtime values.
 *
 *   emoji_deco:arg          → JsonPrimitive (resolved arg string, typed per "type" field)
 *   emoji_deco:player_names → JsonArray of player name strings
 *
 * Structural node types handled after expansion:
 *   emoji_deco:slot, emoji_deco:size, emoji_deco:glow,
 *   emoji_deco:sprite, emoji_deco:player_head,
 *   standard MC fields (text/color/bold/italic/…/extra)
 */
public final class EmojiDecoComponentParser {

    private static final String[] EMPTY_ARGS = new String[0];

    private EmojiDecoComponentParser() {}

    // ── public API ────────────────────────────────────────────────────────────

    public static RichNode parse(JsonElement el, @Nullable RichNode slot) {
        return parse(el, slot, EMPTY_ARGS);
    }

    public static RichNode parse(JsonElement el, @Nullable RichNode slot, String[] args) {
        if (el == null || el.isJsonNull()) return RichNode.empty();
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

    // ── dynamic expansion ─────────────────────────────────────────────────────

    private static JsonElement expandDynamicProviders(JsonElement el, String[] args) {
        if (el == null || el.isJsonNull() || el.isJsonPrimitive()) return el;

        if (el.isJsonObject()) {
            JsonObject obj = el.getAsJsonObject();

            if (obj.has("emoji_deco:arg")) {
                JsonObject spec = obj.getAsJsonObject("emoji_deco:arg");
                int    index = spec.has("index") ? spec.get("index").getAsInt()      : 0;
                String type  = spec.has("type")  ? spec.get("type").getAsString()    : "string";
                if (index < args.length) return coerceArg(args[index], type);
                return spec.has("default") ? spec.get("default") : defaultForType(type);
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

            JsonObject result = new JsonObject();
            for (var entry : obj.entrySet())
                result.add(entry.getKey(), expandDynamicProviders(entry.getValue(), args));
            return result;
        }

        if (el.isJsonArray()) {
            JsonArray result = new JsonArray();
            for (JsonElement item : el.getAsJsonArray())
                result.add(expandDynamicProviders(item, args));
            return result;
        }

        return el;
    }

    // ── structural parsing ────────────────────────────────────────────────────

    private static RichNode parseExpanded(JsonElement el, @Nullable RichNode slot) {
        if (el == null || el.isJsonNull()) return RichNode.empty();
        if (el.isJsonPrimitive()) return new RichNode.Text(el.getAsString(), Style.EMPTY, List.of());
        if (el.isJsonArray()) {
            List<RichNode> children = new ArrayList<>();
            for (JsonElement child : el.getAsJsonArray()) children.add(parseExpanded(child, slot));
            return new RichNode.Text("", Style.EMPTY, children);
        }
        if (el.isJsonObject()) return parseObject(el.getAsJsonObject(), slot);
        return RichNode.empty();
    }

    private static RichNode parseObject(JsonObject obj, @Nullable RichNode slot) {
        if (obj.has("emoji_deco:slot")) {
            return slot != null ? slot : RichNode.empty();
        }
        if (obj.has("emoji_deco:size")) {
            JsonObject spec = obj.getAsJsonObject("emoji_deco:size");
            float size = 1.0f;
            if (spec.has("size") && spec.get("size").isJsonPrimitive()) {
                try { size = spec.get("size").getAsFloat(); } catch (NumberFormatException ignored) {}
            }
            RichNode contents = spec.has("contents") ? parseExpanded(spec.get("contents"), slot) : RichNode.empty();
            return new RichNode.Sized(size, List.of(contents));
        }
        if (obj.has("emoji_deco:glow")) {
            JsonObject spec = obj.getAsJsonObject("emoji_deco:glow");
            LightMode lightMode = (!spec.has("glow") || spec.get("glow").getAsBoolean())
                    ? LightMode.GLOW : LightMode.AMBIENT;
            RichNode contents = spec.has("contents") ? parseExpanded(spec.get("contents"), slot) : RichNode.empty();
            return new RichNode.Glowing(lightMode, List.of(contents));
        }
        if (obj.has("emoji_deco:sprite")) {
            JsonObject spec   = obj.getAsJsonObject("emoji_deco:sprite");
            String atlas  = stringOf(spec.get("atlas"),  "");
            String sprite = stringOf(spec.get("sprite"), "");
            int width  = spec.has("width")  && spec.get("width").isJsonPrimitive()  ? spec.get("width").getAsInt()  : 8;
            int height = spec.has("height") && spec.get("height").isJsonPrimitive() ? spec.get("height").getAsInt() : 8;
            return new RichNode.Sprite(atlas, sprite, width, height);
        }
        if (obj.has("emoji_deco:player_head")) {
            JsonObject spec = obj.getAsJsonObject("emoji_deco:player_head");
            String username = stringOf(spec.get("player"), "");
            return new RichNode.Head(username);
        }
        if (obj.has("emoji_deco:texture")) {
            JsonObject spec = obj.getAsJsonObject("emoji_deco:texture");
            ResourceLocation texRL = ResourceLocation.tryParse(stringOf(spec.get("texture"), ""));
            int width  = spec.has("width")  && spec.get("width").isJsonPrimitive()  ? spec.get("width").getAsInt()  : 8;
            int height = spec.has("height") && spec.get("height").isJsonPrimitive() ? spec.get("height").getAsInt() : 8;
            return texRL != null ? new RichNode.Texture(texRL, width, height) : RichNode.empty();
        }
        return parseStandard(obj, slot);
    }

    private static RichNode parseStandard(JsonObject obj, @Nullable RichNode slot) {
        String textVal = stringOf(obj.get("text"), "");

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

        List<RichNode> children = new ArrayList<>();
        if (obj.has("extra") && obj.get("extra").isJsonArray()) {
            for (JsonElement child : obj.getAsJsonArray("extra"))
                children.add(parseExpanded(child, slot));
        }
        return new RichNode.Text(textVal, style, children);
    }

    // ── utilities ─────────────────────────────────────────────────────────────

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

    private static JsonPrimitive defaultForType(String type) {
        return switch (type) {
            case "boolean"         -> new JsonPrimitive(false);
            case "integer"         -> new JsonPrimitive(0);
            case "float", "number" -> new JsonPrimitive(0.0f);
            default                -> new JsonPrimitive("");
        };
    }

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
