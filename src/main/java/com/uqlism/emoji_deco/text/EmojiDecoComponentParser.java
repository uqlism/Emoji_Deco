package com.uqlism.emoji_deco.text;

import com.google.gson.JsonArray;
import com.uqlism.emoji_deco.render.image.BinarySource;
import com.uqlism.emoji_deco.render.image.ImageSpec;
import com.uqlism.emoji_deco.render.sequence.LightMode;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Set;

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

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String[] EMPTY_ARGS = new String[0];

    /**
     * Per-thread set of shortcode/decorator names currently being resolved.
     * Keys: "s:<name>" for shortcodes, "d:<name>" for decorators.
     * Prevents infinite recursion from apply_shortcode / apply_decorator cycles.
     */
    private static final ThreadLocal<Set<String>> RESOLVING = ThreadLocal.withInitial(HashSet::new);

    private EmojiDecoComponentParser() {}

    // ── public API ────────────────────────────────────────────────────────────

    public static RichNode parse(JsonElement el, @Nullable RichNode slot) {
        return parse(el, slot, EMPTY_ARGS);
    }

    public static RichNode parse(JsonElement el, @Nullable RichNode slot, String[] args) {
        return parse(el, slot, args, null);
    }

    public static RichNode parse(JsonElement el, @Nullable RichNode slot, String[] args,
                                 @Nullable JsonArray topLevelArgSpecs) {
        if (el == null || el.isJsonNull()) return RichNode.empty();
        return parseExpanded(expandDynamicProviders(el, args, topLevelArgSpecs), slot);
    }

    /**
     * Resolves a JsonElement to a flat list of strings.
     * Dynamic providers are expanded first; each primitive in the resulting array is collected.
     */
    public static List<String> resolveStringList(JsonElement el) {
        if (el == null) return Collections.emptyList();
        JsonElement expanded = expandDynamicProviders(el, EMPTY_ARGS, null);
        if (!expanded.isJsonArray()) return Collections.emptyList();
        List<String> result = new ArrayList<>();
        for (JsonElement item : expanded.getAsJsonArray()) {
            if (item.isJsonPrimitive()) result.add(item.getAsString());
        }
        return result;
    }

    // ── dynamic expansion ─────────────────────────────────────────────────────

    private static JsonElement expandDynamicProviders(JsonElement el, String[] args,
                                                      @Nullable JsonArray topLevelArgSpecs) {
        if (el == null || el.isJsonNull() || el.isJsonPrimitive()) return el;

        if (el.isJsonObject()) {
            JsonObject obj = el.getAsJsonObject();

            String dynType = obj.has("type") ? obj.get("type").getAsString() : null;

            if ("emoji_deco:arg".equals(dynType)) {
                int index = obj.has("index") ? obj.get("index").getAsInt() : 0;
                JsonObject top = topArgSpec(topLevelArgSpecs, index);
                String vtype = "string";
                if      (obj.has("value_type"))                    vtype = obj.get("value_type").getAsString();
                else if (top != null && top.has("value_type"))     vtype = top.get("value_type").getAsString();
                if (index < args.length) return coerceArg(args[index], vtype);
                if (obj.has("default"))                            return obj.get("default");
                if (top != null && top.has("default"))             return top.get("default");
                return defaultForType(vtype);
            }

            if ("emoji_deco:player_names".equals(dynType)) {
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

            if ("emoji_deco:time".equals(dynType)) {
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                long gameTime   = (mc != null && mc.level != null) ? mc.level.getGameTime()          : 0L;
                long dayTime    = (mc != null && mc.level != null) ? mc.level.getDayTime()            : 0L;
                float partial   = (mc != null)                     ? mc.getPartialTick()              : 0f;
                String timeType = obj.has("time") && obj.get("time").isJsonPrimitive()
                        ? obj.get("time").getAsString() : "gametime";
                float raw = switch (timeType) {
                    case "daytime" -> (float)(dayTime % 24000L) + partial;
                    case "day"     -> (float)(dayTime / 24000L);
                    default        -> (float)gameTime + partial;   // "gametime"
                };
                float scale = 1f, offset = 0f;
                try { if (obj.has("scale")  && obj.get("scale") .isJsonPrimitive()) scale  = obj.get("scale") .getAsFloat(); } catch (Exception ignored) {}
                try { if (obj.has("offset") && obj.get("offset").isJsonPrimitive()) offset = obj.get("offset").getAsFloat(); } catch (Exception ignored) {}
                return new JsonPrimitive(raw * scale + offset);
            }

            if ("emoji_deco:join".equals(dynType)) {
                String separator = obj.has("separator") && obj.get("separator").isJsonPrimitive()
                        ? obj.get("separator").getAsString() : "";
                if (obj.has("parts") && obj.get("parts").isJsonArray()) {
                    java.util.List<String> parts = new java.util.ArrayList<>();
                    for (JsonElement part : obj.getAsJsonArray("parts")) {
                        JsonElement expanded = expandDynamicProviders(part, args, topLevelArgSpecs);
                        if (expanded != null && expanded.isJsonPrimitive())
                            parts.add(expanded.getAsString());
                    }
                    return new JsonPrimitive(String.join(separator, parts));
                }
                return new JsonPrimitive("");
            }

            JsonObject result = new JsonObject();
            for (var entry : obj.entrySet())
                result.add(entry.getKey(), expandDynamicProviders(entry.getValue(), args, topLevelArgSpecs));
            return result;
        }

        if (el.isJsonArray()) {
            JsonArray result = new JsonArray();
            for (JsonElement item : el.getAsJsonArray())
                result.add(expandDynamicProviders(item, args, topLevelArgSpecs));
            return result;
        }

        return el;
    }

    @Nullable
    private static JsonObject topArgSpec(@Nullable JsonArray specs, int index) {
        if (specs == null || index < 0 || index >= specs.size()) return null;
        JsonElement el = specs.get(index);
        return el.isJsonObject() ? el.getAsJsonObject() : null;
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
        if (obj.has("type")) {
            return switch (stringOf(obj.get("type"), "")) {
                case "emoji_deco:slot" -> slot != null ? slot : RichNode.empty();
                case "emoji_deco:glow" -> {
                    LightMode mode = (!obj.has("glow") || obj.get("glow").getAsBoolean())
                            ? LightMode.GLOW : LightMode.AMBIENT;
                    yield new RichNode.Glowing(mode, List.of(
                            obj.has("contents") ? parseExpanded(obj.get("contents"), slot) : RichNode.empty()));
                }
                case "emoji_deco:image_to_glyph" -> parseImageNode(obj);
                case "emoji_deco:rotate" -> {
                    float angle = 0f;
                    try { if (obj.has("angle") && obj.get("angle").isJsonPrimitive()) angle = obj.get("angle").getAsFloat(); } catch (NumberFormatException ignored) {}
                    yield new RichNode.Rotated(angle, List.of(
                            obj.has("contents") ? parseExpanded(obj.get("contents"), slot) : RichNode.empty()));
                }
                case "emoji_deco:scale" -> {
                    float x = 1f, y = 1f;
                    try { if (obj.has("x") && obj.get("x").isJsonPrimitive()) x = obj.get("x").getAsFloat(); } catch (NumberFormatException ignored) {}
                    try { if (obj.has("y") && obj.get("y").isJsonPrimitive()) y = obj.get("y").getAsFloat(); } catch (NumberFormatException ignored) {}
                    yield new RichNode.Scaled(x, y, List.of(
                            obj.has("contents") ? parseExpanded(obj.get("contents"), slot) : RichNode.empty()));
                }
                case "emoji_deco:offset" -> {
                    float x = 0f, y = 0f, z = 0f;
                    try { if (obj.has("x") && obj.get("x").isJsonPrimitive()) x = obj.get("x").getAsFloat(); } catch (NumberFormatException ignored) {}
                    try { if (obj.has("y") && obj.get("y").isJsonPrimitive()) y = obj.get("y").getAsFloat(); } catch (NumberFormatException ignored) {}
                    try { if (obj.has("z") && obj.get("z").isJsonPrimitive()) z = obj.get("z").getAsFloat(); } catch (NumberFormatException ignored) {}
                    yield new RichNode.Offset(x, y, z, List.of(
                            obj.has("contents") ? parseExpanded(obj.get("contents"), slot) : RichNode.empty()));
                }
                case "emoji_deco:apply_shortcode" -> parseApplyShortcode(obj);
                case "emoji_deco:apply_decorator" -> parseApplyDecorator(obj, slot);
                default -> RichNode.empty();
            };
        }
        return parseStandard(obj, slot);
    }

    private static RichNode parseImageNode(JsonObject obj) {
        int w = intOf(obj, "width", 8);
        int h = intOf(obj, "height", 8);
        float advance = advanceOf(obj);
        JsonElement imageEl = obj.get("image");
        if (imageEl == null || !imageEl.isJsonObject()) return RichNode.empty();
        JsonObject imageObj = imageEl.getAsJsonObject();
        int[] crop = imageObj.has("uv") ? parseCrop(imageObj.get("uv")) : null;
        ImageSpec imageSpec = parseImageSpec(imageObj);
        if (imageSpec == null) return RichNode.empty();
        return new RichNode.Image(imageSpec, crop, w, h, advance);
    }

    @Nullable
    private static ImageSpec parseImageSpec(JsonObject obj) {
        return switch (stringOf(obj.get("type"), "")) {
            case "emoji_deco:decode_image" -> {
                String format = obj.has("format") && obj.get("format").isJsonPrimitive()
                        ? obj.get("format").getAsString() : null;
                if (!obj.has("source") || !obj.get("source").isJsonObject()) yield null;
                BinarySource source = parseBinarySource(obj.getAsJsonObject("source"));
                yield source != null ? new ImageSpec.Decoded(format, source) : null;
            }
case "emoji_deco:fetch_atlas" -> {
                String atlas  = stringOf(obj.get("atlas"),  "");
                String sprite = stringOf(obj.get("sprite"), "");
                yield atlas.isEmpty() || sprite.isEmpty() ? null : new ImageSpec.Atlas(atlas, sprite);
            }
            case "emoji_deco:fetch_skin" -> {
                String player = stringOf(obj.get("player"), "");
                yield player.isEmpty() ? null : new ImageSpec.Skin(player);
            }
            default -> null;
        };
    }

    @Nullable
    private static BinarySource parseBinarySource(JsonObject obj) {
        return switch (stringOf(obj.get("type"), "")) {
            case "emoji_deco:fetch_url" -> {
                String url = stringOf(obj.get("url"), "");
                if (url.isEmpty()) yield null;
                boolean diskCache = obj.has("disk_cache") && obj.get("disk_cache").getAsBoolean();
                int ttl = obj.has("ttl") && obj.get("ttl").isJsonPrimitive()
                        ? obj.get("ttl").getAsInt() : 0;
                yield new BinarySource.Url(url, diskCache, ttl);
            }
            case "emoji_deco:fetch_resource" -> {
                String path = stringOf(obj.get("path"), "");
                yield path.isEmpty() ? null : new BinarySource.Resource(path);
            }
            default -> null;
        };
    }

    private static RichNode parseApplyShortcode(JsonObject obj) {
        String shortcode = stringOf(obj.get("shortcode"), "");
        if (shortcode.isEmpty() || !ShortcodeManager.has(shortcode)) return RichNode.empty();
        String[] callArgs = extractArgsArray(obj);
        String key = "s:" + shortcode;
        Set<String> stack = RESOLVING.get();
        if (stack.contains(key)) {
            LOGGER.warn("[EmojiDeco] Cyclic apply_shortcode detected: '{}'", shortcode);
            return RichNode.empty();
        }
        stack.add(key);
        try {
            return ShortcodeManager.resolve(shortcode, callArgs);
        } finally {
            stack.remove(key);
        }
    }

    private static RichNode parseApplyDecorator(JsonObject obj, @Nullable RichNode slot) {
        String decorator = stringOf(obj.get("decorator"), "");
        if (decorator.isEmpty() || !DecoratorManager.has(decorator)) return RichNode.empty();
        RichNode slotNode = obj.has("slot") ? parseExpanded(obj.get("slot"), slot) : RichNode.empty();
        String[] callArgs = extractArgsArray(obj);
        String key = "d:" + decorator;
        Set<String> stack = RESOLVING.get();
        if (stack.contains(key)) {
            LOGGER.warn("[EmojiDeco] Cyclic apply_decorator detected: '{}'", decorator);
            return slotNode;
        }
        stack.add(key);
        try {
            RichNode result = DecoratorManager.resolve(decorator, slotNode, callArgs);
            return result != null ? result : slotNode;
        } finally {
            stack.remove(key);
        }
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

    private static String[] extractArgsArray(JsonObject spec) {
        if (!spec.has("args") || !spec.get("args").isJsonArray()) return EMPTY_ARGS;
        JsonArray arr = spec.getAsJsonArray("args");
        String[] result = new String[arr.size()];
        for (int i = 0; i < arr.size(); i++)
            result[i] = arr.get(i).isJsonPrimitive() ? arr.get(i).getAsString() : "";
        return result;
    }

    // ── image pipeline ─────────────────────────────────────────────────────────

    private static float advanceOf(JsonObject spec) {
        return (spec.has("advance") && spec.get("advance").isJsonPrimitive())
                ? spec.get("advance").getAsFloat() : Float.NaN;
    }

    /** emoji_deco:crop の "uv": [x0,y0,x1,y1] を int[4] に変換。不正な場合は null。 */
    @Nullable
    private static int[] parseCrop(@Nullable JsonElement el) {
        if (el == null || !el.isJsonArray()) return null;
        JsonArray arr = el.getAsJsonArray();
        if (arr.size() != 4) return null;
        try {
            return new int[]{
                    arr.get(0).getAsInt(), arr.get(1).getAsInt(),
                    arr.get(2).getAsInt(), arr.get(3).getAsInt()
            };
        } catch (Exception e) { return null; }
    }

    private static int intOf(JsonObject obj, String key, int fallback) {
        return obj.has(key) && obj.get(key).isJsonPrimitive() ? obj.get(key).getAsInt() : fallback;
    }

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
            if ("emoji_deco:arg".equals(obj.has("type") ? obj.get("type").getAsString() : null)) {
                if ((obj.has("index") ? obj.get("index").getAsInt() : 0) == index) return obj;
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

    /**
     * Walks the JSON tree and collects all emoji_deco:arg specs keyed by their index.
     * First occurrence wins if the same index appears multiple times.
     */
    public static java.util.Map<Integer, JsonObject> findAllArgSpecs(JsonElement el) {
        java.util.Map<Integer, JsonObject> result = new java.util.TreeMap<>();
        collectArgSpecs(el, result);
        return result;
    }

    private static void collectArgSpecs(JsonElement el, java.util.Map<Integer, JsonObject> result) {
        if (el == null) return;
        if (el.isJsonObject()) {
            JsonObject obj = el.getAsJsonObject();
            if ("emoji_deco:arg".equals(obj.has("type") ? obj.get("type").getAsString() : null)) {
                int idx = obj.has("index") ? obj.get("index").getAsInt() : 0;
                result.putIfAbsent(idx, obj);
            }
            for (var entry : obj.entrySet()) collectArgSpecs(entry.getValue(), result);
        } else if (el.isJsonArray()) {
            for (JsonElement child : el.getAsJsonArray()) collectArgSpecs(child, result);
        }
    }
}
