package com.uqlism.emoji_deco.text.parse;

import com.uqlism.emoji_deco.text.ir.ParsedNode;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts emoji_deco JSON display elements to ParsedNode (dehydrated form).
 * Pure structural parse — no arg evaluation, no runtime state access.
 * Called at load time; result is cached in ShortcodeManager / DecoratorManager.
 */
public final class ParsedNodeParser {

    private ParsedNodeParser() {}

    // ── Public entry point ────────────────────────────────────────────────────

    public static ParsedNode parse(JsonElement el, @Nullable JsonArray topArgSpecs) {
        if (el == null || el.isJsonNull()) return ParsedNode.empty();
        if (el.isJsonPrimitive()) {
            return new ParsedNode.Text(el.getAsString(), ParsedNode.ParsedStyle.EMPTY, List.of());
        }
        if (el.isJsonArray()) {
            List<ParsedNode> items = new ArrayList<>();
            for (JsonElement item : el.getAsJsonArray()) items.add(parse(item, topArgSpecs));
            return new ParsedNode.Many(items);
        }
        if (el.isJsonObject()) return parseObject(el.getAsJsonObject(), topArgSpecs);
        return ParsedNode.empty();
    }

    // ── Object dispatch ───────────────────────────────────────────────────────

    private static ParsedNode parseObject(JsonObject obj, @Nullable JsonArray topArgSpecs) {
        String type = str(obj.get("type"), null);
        if (type == null) return parseStandard(obj, topArgSpecs);

        return switch (type) {
            case "emoji_deco:arg" -> {
                int    index = obj.has("index") ? obj.get("index").getAsInt() : 0;
                String def   = argDefaultStr(obj, topArgSpecs, index);
                yield new ParsedNode.ArgAsText(index, def);
            }
            case "emoji_deco:slot" -> new ParsedNode.SlotRef();
            case "emoji_deco:glow" -> {
                ParsedNode.BoolVal glow = parseBool(obj.get("glow"), true, topArgSpecs);
                yield new ParsedNode.Glow(glow, obj.has("contents")
                        ? parse(obj.get("contents"), topArgSpecs) : ParsedNode.empty());
            }
            case "emoji_deco:image_to_glyph" -> parseImage(obj, topArgSpecs);
            case "emoji_deco:rotate" -> {
                ParsedNode.FloatVal angle = parseFloat(obj.get("angle"), 0f, topArgSpecs);
                yield new ParsedNode.Rotate(angle, obj.has("contents")
                        ? parse(obj.get("contents"), topArgSpecs) : ParsedNode.empty());
            }
            case "emoji_deco:scale" -> new ParsedNode.Scale(
                    parseFloat(obj.get("x"), 1f, topArgSpecs),
                    parseFloat(obj.get("y"), 1f, topArgSpecs),
                    obj.has("contents") ? parse(obj.get("contents"), topArgSpecs) : ParsedNode.empty());
            case "emoji_deco:offset" -> new ParsedNode.Offset(
                    parseFloat(obj.get("x"), 0f, topArgSpecs),
                    parseFloat(obj.get("y"), 0f, topArgSpecs),
                    parseFloat(obj.get("z"), 0f, topArgSpecs),
                    obj.has("contents") ? parse(obj.get("contents"), topArgSpecs) : ParsedNode.empty());
            case "emoji_deco:apply_shortcode" -> {
                String name = str(obj.get("shortcode"), "");
                yield new ParsedNode.ApplyShortcode(name, parseCallArgs(obj.get("args"), topArgSpecs));
            }
            case "emoji_deco:apply_decorator" -> {
                String name = str(obj.get("decorator"), "");
                ParsedNode slot = obj.has("slot")
                        ? parse(obj.get("slot"), topArgSpecs) : ParsedNode.empty();
                yield new ParsedNode.ApplyDecorator(name, slot, parseCallArgs(obj.get("args"), topArgSpecs));
            }
            default -> ParsedNode.empty();
        };
    }

    private static ParsedNode parseStandard(JsonObject obj, @Nullable JsonArray topArgSpecs) {
        String textVal = str(obj.get("text"), "");

        ParsedNode.ParsedStyle style = new ParsedNode.ParsedStyle(
                obj.has("color")         ? parseString(obj.get("color"),         topArgSpecs) : null,
                obj.has("bold")          ? parseBool(obj.get("bold"),   false,   topArgSpecs) : null,
                obj.has("italic")        ? parseBool(obj.get("italic"), false,   topArgSpecs) : null,
                obj.has("strikethrough") ? parseBool(obj.get("strikethrough"), false, topArgSpecs) : null,
                obj.has("underlined")    ? parseBool(obj.get("underlined"),    false, topArgSpecs) : null,
                obj.has("obfuscated")    ? parseBool(obj.get("obfuscated"),    false, topArgSpecs) : null,
                obj.has("font") && obj.get("font").isJsonPrimitive() ? obj.get("font").getAsString() : null
        );

        List<ParsedNode> children = new ArrayList<>();
        if (obj.has("extra") && obj.get("extra").isJsonArray()) {
            for (JsonElement child : obj.getAsJsonArray("extra"))
                children.add(parse(child, topArgSpecs));
        }
        return new ParsedNode.Text(textVal, style, children);
    }

    // ── Image ─────────────────────────────────────────────────────────────────

    private static ParsedNode parseImage(JsonObject obj, @Nullable JsonArray topArgSpecs) {
        ParsedNode.FloatVal w       = parseFloat(obj.get("width"),   8f,        topArgSpecs);
        ParsedNode.FloatVal h       = parseFloat(obj.get("height"),  8f,        topArgSpecs);
        ParsedNode.FloatVal advance = parseFloat(obj.get("advance"), Float.NaN, topArgSpecs);
        JsonElement imageEl = obj.get("image");
        if (imageEl == null || !imageEl.isJsonObject()) return ParsedNode.empty();
        JsonObject imageObj = imageEl.getAsJsonObject();
        int[] crop = imageObj.has("uv") ? parseCrop(imageObj.get("uv")) : null;
        ParsedNode.ParsedImageSpec spec = parseImageSpec(imageObj, topArgSpecs);
        if (spec == null) return ParsedNode.empty();
        return new ParsedNode.Image(spec, crop, w, h, advance);
    }

    @Nullable
    private static ParsedNode.ParsedImageSpec parseImageSpec(JsonObject obj, @Nullable JsonArray topArgSpecs) {
        return switch (str(obj.get("type"), "")) {
            case "emoji_deco:decode_image" -> {
                ParsedNode.StringVal format = obj.has("format") ? parseString(obj.get("format"), topArgSpecs) : null;
                if (!obj.has("source") || !obj.get("source").isJsonObject()) yield null;
                ParsedNode.ParsedBinarySource src = parseBinarySource(obj.getAsJsonObject("source"), topArgSpecs);
                yield src != null ? new ParsedNode.ParsedImageSpec.Decoded(format, src) : null;
            }
            case "emoji_deco:fetch_atlas" -> new ParsedNode.ParsedImageSpec.Atlas(
                    parseString(obj.get("atlas"),  topArgSpecs),
                    parseString(obj.get("sprite"), topArgSpecs));
            case "emoji_deco:fetch_skin" -> new ParsedNode.ParsedImageSpec.Skin(
                    parseString(obj.get("player"), topArgSpecs));
            default -> null;
        };
    }

    @Nullable
    private static ParsedNode.ParsedBinarySource parseBinarySource(JsonObject obj, @Nullable JsonArray topArgSpecs) {
        return switch (str(obj.get("type"), "")) {
            case "emoji_deco:fetch_url" -> {
                ParsedNode.StringVal url = parseString(obj.get("url"), topArgSpecs);
                boolean diskCache = obj.has("disk_cache") && obj.get("disk_cache").getAsBoolean();
                int ttl = obj.has("ttl") && obj.get("ttl").isJsonPrimitive() ? obj.get("ttl").getAsInt() : 0;
                yield new ParsedNode.ParsedBinarySource.Url(url, diskCache, ttl);
            }
            case "emoji_deco:fetch_resource" -> new ParsedNode.ParsedBinarySource.Resource(
                    parseString(obj.get("path"), topArgSpecs));
            default -> null;
        };
    }

    // ── Expression parsers ─────────────────────────────────────────────────────

    static ParsedNode.StringVal parseString(@Nullable JsonElement el, @Nullable JsonArray topArgSpecs) {
        if (el == null) return new ParsedNode.StringVal.Literal("");
        if (el.isJsonPrimitive()) return new ParsedNode.StringVal.Literal(el.getAsString());
        if (!el.isJsonObject()) return new ParsedNode.StringVal.Literal("");
        JsonObject obj = el.getAsJsonObject();
        String t = str(obj.get("type"), "");
        return switch (t) {
            case "emoji_deco:arg" -> {
                int    index = obj.has("index") ? obj.get("index").getAsInt() : 0;
                String def   = argDefaultStr(obj, topArgSpecs, index);
                yield new ParsedNode.StringVal.Arg(index, def);
            }
            case "emoji_deco:join" -> {
                String sep = obj.has("separator") && obj.get("separator").isJsonPrimitive()
                        ? obj.get("separator").getAsString() : "";
                List<ParsedNode.StringVal> parts = new ArrayList<>();
                if (obj.has("parts") && obj.get("parts").isJsonArray())
                    for (JsonElement p : obj.getAsJsonArray("parts"))
                        parts.add(parseString(p, topArgSpecs));
                yield new ParsedNode.StringVal.Join(sep, parts);
            }
            case "emoji_deco:player_names" -> new ParsedNode.StringVal.PlayerNames();
            default -> new ParsedNode.StringVal.Literal("");
        };
    }

    static ParsedNode.FloatVal parseFloat(@Nullable JsonElement el, float defaultLit, @Nullable JsonArray topArgSpecs) {
        if (el == null) return new ParsedNode.FloatVal.Literal(defaultLit);
        if (el.isJsonPrimitive()) {
            try { return new ParsedNode.FloatVal.Literal(el.getAsFloat()); }
            catch (Exception e) { return new ParsedNode.FloatVal.Literal(defaultLit); }
        }
        if (!el.isJsonObject()) return new ParsedNode.FloatVal.Literal(defaultLit);
        JsonObject obj = el.getAsJsonObject();
        String t = str(obj.get("type"), "");
        return switch (t) {
            case "emoji_deco:arg" -> {
                int   index = obj.has("index") ? obj.get("index").getAsInt() : 0;
                float def   = argDefaultFloat(obj, topArgSpecs, index, defaultLit);
                yield new ParsedNode.FloatVal.Arg(index, def);
            }
            case "emoji_deco:time" -> {
                String timeType = obj.has("time") && obj.get("time").isJsonPrimitive()
                        ? obj.get("time").getAsString() : "gametime";
                float scale  = safef(obj.get("scale"),  1f);
                float offset = safef(obj.get("offset"), 0f);
                yield new ParsedNode.FloatVal.Time(timeType, scale, offset);
            }
            default -> new ParsedNode.FloatVal.Literal(defaultLit);
        };
    }

    static ParsedNode.BoolVal parseBool(@Nullable JsonElement el, boolean defaultLit, @Nullable JsonArray topArgSpecs) {
        if (el == null) return new ParsedNode.BoolVal.Literal(defaultLit);
        if (el.isJsonPrimitive()) {
            try { return new ParsedNode.BoolVal.Literal(el.getAsBoolean()); }
            catch (Exception e) { return new ParsedNode.BoolVal.Literal(defaultLit); }
        }
        if (!el.isJsonObject()) return new ParsedNode.BoolVal.Literal(defaultLit);
        JsonObject obj = el.getAsJsonObject();
        if ("emoji_deco:arg".equals(str(obj.get("type"), ""))) {
            int     index = obj.has("index") ? obj.get("index").getAsInt() : 0;
            boolean def   = argDefaultBool(obj, topArgSpecs, index, defaultLit);
            return new ParsedNode.BoolVal.Arg(index, def);
        }
        return new ParsedNode.BoolVal.Literal(defaultLit);
    }

    private static List<ParsedNode.StringVal> parseCallArgs(@Nullable JsonElement el, @Nullable JsonArray topArgSpecs) {
        if (el == null || !el.isJsonArray()) return List.of();
        List<ParsedNode.StringVal> result = new ArrayList<>();
        for (JsonElement item : el.getAsJsonArray()) result.add(parseString(item, topArgSpecs));
        return result;
    }

    // ── Default value helpers ─────────────────────────────────────────────────

    private static String argDefaultStr(JsonObject argRef, @Nullable JsonArray topArgSpecs, int index) {
        if (argRef.has("default") && argRef.get("default").isJsonPrimitive())
            return argRef.get("default").getAsString();
        JsonObject top = topSpec(topArgSpecs, index);
        if (top != null && top.has("default") && top.get("default").isJsonPrimitive())
            return top.get("default").getAsString();
        return "";
    }

    private static float argDefaultFloat(JsonObject argRef, @Nullable JsonArray topArgSpecs, int index, float fallback) {
        try {
            if (argRef.has("default") && argRef.get("default").isJsonPrimitive())
                return argRef.get("default").getAsFloat();
            JsonObject top = topSpec(topArgSpecs, index);
            if (top != null && top.has("default") && top.get("default").isJsonPrimitive())
                return top.get("default").getAsFloat();
        } catch (Exception ignored) {}
        return fallback;
    }

    private static boolean argDefaultBool(JsonObject argRef, @Nullable JsonArray topArgSpecs, int index, boolean fallback) {
        try {
            if (argRef.has("default") && argRef.get("default").isJsonPrimitive())
                return argRef.get("default").getAsBoolean();
            JsonObject top = topSpec(topArgSpecs, index);
            if (top != null && top.has("default") && top.get("default").isJsonPrimitive())
                return top.get("default").getAsBoolean();
        } catch (Exception ignored) {}
        return fallback;
    }

    @Nullable
    private static JsonObject topSpec(@Nullable JsonArray specs, int index) {
        if (specs == null || index < 0 || index >= specs.size()) return null;
        JsonElement el = specs.get(index);
        return el.isJsonObject() ? el.getAsJsonObject() : null;
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private static int intOf(JsonObject obj, String key, int fallback) {
        return obj.has(key) && obj.get(key).isJsonPrimitive() ? obj.get(key).getAsInt() : fallback;
    }

    private static float safef(@Nullable JsonElement el, float fallback) {
        try { return (el != null && el.isJsonPrimitive()) ? el.getAsFloat() : fallback; }
        catch (Exception e) { return fallback; }
    }

    @Nullable
    private static String str(@Nullable JsonElement el, @Nullable String fallback) {
        return (el != null && el.isJsonPrimitive()) ? el.getAsString() : fallback;
    }

    @Nullable
    private static int[] parseCrop(@Nullable JsonElement el) {
        if (el == null || !el.isJsonArray()) return null;
        JsonArray arr = el.getAsJsonArray();
        if (arr.size() != 4) return null;
        try { return new int[]{arr.get(0).getAsInt(), arr.get(1).getAsInt(),
                                arr.get(2).getAsInt(), arr.get(3).getAsInt()}; }
        catch (Exception e) { return null; }
    }
}
