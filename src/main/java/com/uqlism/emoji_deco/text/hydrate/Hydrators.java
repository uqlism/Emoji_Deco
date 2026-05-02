package com.uqlism.emoji_deco.text.hydrate;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.function.Function;
import com.mojang.logging.LogUtils;
import com.uqlism.emoji_deco.render.image.BinarySource;
import com.uqlism.emoji_deco.render.image.ImageSpec;
import com.uqlism.emoji_deco.render.sequence.LightMode;
import com.uqlism.emoji_deco.text.ir.RichNode;
import com.uqlism.emoji_deco.text.registry.DecoratorManager;
import com.uqlism.emoji_deco.text.registry.ShortcodeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.UUID;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Static Hydrator<T> constants for the emoji_deco JSON display format.
 *
 * Public fields are method-backed to allow self-referential dispatch tables.
 * Private *_IMPL fields hold the actual combinator chains, built once at class load.
 */
public final class Hydrators {

    private static final Logger        LOGGER    = LogUtils.getLogger();
    private static final ThreadLocal<Set<String>> RESOLVING = ThreadLocal.withInitial(HashSet::new);

    private Hydrators() {}

    // ── Scalar combinator chains ───────────────────────────────────────────────
    // Defined before the public fields so STRING/FLOAT/BOOL can be direct aliases.
    // Recursive calls (e.g. joinStr → STRING) work because method bodies are
    // evaluated at runtime, not at class-init time.

    public static final Hydrator<String> STRING = Hydrator.firstOf(
            (el, ctx) -> el != null && el.isJsonPrimitive() ? el.getAsString() : null,
            Hydrator.dispatch(Map.ofEntries(
                    Map.entry("emoji_deco:arg",          argOf(Function.identity(), Hydrator.lazy(() -> Hydrators.STRING))),
                    Map.entry("emoji_deco:join",         Hydrator.lazy(() -> Hydrators.JOIN_STR)),
                    Map.entry("emoji_deco:player_names", (el, ctx) -> { ctx.markPlayerNamesAccessed(); return ""; }),
                    // HSV → "#RRGGBB" 文字列。h/s/v はすべて 0.0〜1.0 の FLOAT 式。
                    Map.entry("emoji_deco:color/hsv",    Hydrator.zip(
                            Hydrator.field("h", Hydrator.lazy(() -> Hydrators.FLOAT).withDefault(0f)),
                            Hydrator.field("s", Hydrator.lazy(() -> Hydrators.FLOAT).withDefault(1f)),
                            Hydrator.field("v", Hydrator.lazy(() -> Hydrators.FLOAT).withDefault(1f)),
                            Hydrators::hsvToHex))
            ), null));

    public static final Hydrator<Float> FLOAT = Hydrator.firstOf(
            (el, ctx) -> {
                if (el == null || !el.isJsonPrimitive()) return null;
                try { return el.getAsFloat(); } catch (Exception e) { return null; }
            },
            Hydrator.dispatch(Map.ofEntries(
                    Map.entry("emoji_deco:arg",        argOf(Hydrators::tryParseFloat, Hydrator.lazy(() -> Hydrators.FLOAT))),
                    Map.entry("emoji_deco:time",       Hydrator.lazy(() -> Hydrators.TIME_VAL)),
                    // ── 算術演算子 ─────────────────────────────────────────────
                    Map.entry("emoji_deco:math/mod",   Hydrator.zip(
                            Hydrator.field("value", Hydrator.lazy(() -> Hydrators.FLOAT)),
                            Hydrator.field("mod",   Hydrator.lazy(() -> Hydrators.FLOAT).withDefault(1f)),
                            (v, m) -> m != 0f ? v % m : 0f)),
                    Map.entry("emoji_deco:math/add",   Hydrator.zip(
                            Hydrator.field("a", Hydrator.lazy(() -> Hydrators.FLOAT).withDefault(0f)),
                            Hydrator.field("b", Hydrator.lazy(() -> Hydrators.FLOAT).withDefault(0f)),
                            (a, b) -> a + b)),
                    Map.entry("emoji_deco:math/sub",   Hydrator.zip(
                            Hydrator.field("a", Hydrator.lazy(() -> Hydrators.FLOAT).withDefault(0f)),
                            Hydrator.field("b", Hydrator.lazy(() -> Hydrators.FLOAT).withDefault(0f)),
                            (a, b) -> a - b)),
                    Map.entry("emoji_deco:math/mul",   Hydrator.zip(
                            Hydrator.field("a", Hydrator.lazy(() -> Hydrators.FLOAT).withDefault(1f)),
                            Hydrator.field("b", Hydrator.lazy(() -> Hydrators.FLOAT).withDefault(1f)),
                            (a, b) -> a * b)),
                    Map.entry("emoji_deco:math/abs",   Hydrator.field("value",
                            Hydrator.lazy(() -> Hydrators.FLOAT)).map(f -> Math.abs(f))),
                    Map.entry("emoji_deco:math/clamp", Hydrator.zip(
                            Hydrator.field("value", Hydrator.lazy(() -> Hydrators.FLOAT)),
                            Hydrator.field("min",   Hydrator.lazy(() -> Hydrators.FLOAT).withDefault(0f)),
                            Hydrator.field("max",   Hydrator.lazy(() -> Hydrators.FLOAT).withDefault(1f)),
                            (v, mn, mx) -> Math.max(mn, Math.min(mx, v)))),
                    Map.entry("emoji_deco:math/sin",   Hydrator.field("value",
                            Hydrator.lazy(() -> Hydrators.FLOAT)).map(f -> (float) Math.sin(f))),
                    Map.entry("emoji_deco:math/cos",   Hydrator.field("value",
                            Hydrator.lazy(() -> Hydrators.FLOAT)).map(f -> (float) Math.cos(f)))
            ), null));

    public static final Hydrator<Boolean> BOOL = Hydrator.firstOf(
            (el, ctx) -> {
                if (el == null || !el.isJsonPrimitive()) return null;
                try { return el.getAsBoolean(); } catch (Exception e) { return null; }
            },
            Hydrator.dispatch(Map.of(
                    "emoji_deco:arg", argOf(Hydrators::parseBool, Hydrator.lazy(() -> Hydrators.BOOL))
            ), null));

    public static final Hydrator<List<String>> STRING_LIST = Hydrator.firstOf(
            Hydrator.list(STRING),
            Hydrator.dispatch(Map.of(
                    "emoji_deco:player_names",
                    Hydrator.withEffect(
                            HydrateContext.Tracked::markPlayerNamesAccessed,
                            (el, ctx) -> {
                                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                                if (mc == null || mc.getConnection() == null) return List.of();
                                return mc.getConnection().getOnlinePlayers().stream()
                                        .map(pi -> pi.getProfile().getName())
                                        .sorted(String.CASE_INSENSITIVE_ORDER)
                                        .collect(java.util.stream.Collectors.toList());
                            })
            ), null));

    public static final Hydrator<float[]> FLOAT_ARRAY =
            Hydrator.list(FLOAT).map(list -> {
                float[] a = new float[list.size()];
                for (int i = 0; i < list.size(); i++) a[i] = list.get(i);
                return a;
            });

    // Defined before NODE_DISPATCH so it can be referenced directly (no delay needed).
    private static final Hydrator<Float> TIME_VAL = Hydrator.withEffect(
            HydrateContext.Tracked::markTimeAccessed,
            Hydrator.zip(
                    Hydrator.field("time",   STRING.withDefault("gametime")),
                    Hydrator.field("scale",  FLOAT.withDefault(1f)),
                    Hydrator.field("offset", FLOAT.withDefault(0f)),
                    (timeType, scale, offset) -> {
                        Minecraft mc      = Minecraft.getInstance();
                        long gameTime     = (mc != null && mc.level != null) ? mc.level.getGameTime() : 0L;
                        long dayTime      = (mc != null && mc.level != null) ? mc.level.getDayTime()  : 0L;
                        float partial     = (mc != null) ? mc.getPartialTick() : 0f;
                        float raw = switch (timeType) {
                            case "daytime" -> (float)(dayTime % 24000L) + partial;
                            case "day"     -> (float)(dayTime / 24000L);
                            default        -> (float) gameTime + partial;
                        };
                        return raw * scale + offset;
                    }));

    // Structural node hydrators — defined before NODE; use lazy(NODE) for the contents field.
    private static final Hydrator<List<RichNode>> CONTENTS =
            Hydrator.field("contents", Hydrator.lazy(() -> Hydrators.NODE)).map(List::of);

    private static final Hydrator<RichNode> GLOW_NODE = cached(Hydrator.zip(
            Hydrator.field("glow", BOOL.withDefault(true))
                    .map(g -> g ? LightMode.GLOW : LightMode.AMBIENT),
            CONTENTS,
            RichNode.Glowing::new));

    private static final Hydrator<RichNode> SCALE_NODE = cached(Hydrator.zip(
            Hydrator.field("x", FLOAT.withDefault(1f)),
            Hydrator.field("y", FLOAT.withDefault(1f)),
            CONTENTS,
            RichNode.Scaled::new));

    private static final Hydrator<RichNode> OFFSET_NODE = cached(Hydrator.zip(
            Hydrator.field("x", FLOAT.withDefault(0f)),
            Hydrator.field("y", FLOAT.withDefault(0f)),
            Hydrator.field("z", FLOAT.withDefault(0f)),
            CONTENTS,
            RichNode.Offset::new));

    private static final Hydrator<RichNode> ROTATE_NODE = cached(Hydrator.zip(
            Hydrator.field("angle", FLOAT.withDefault(0f)),
            CONTENTS,
            RichNode.Rotated::new));

    private static final Hydrator<RichNode> HOVER_NODE = cached(Hydrator.zip(
            Hydrator.field("hover_contents", Hydrator.lazy(() -> Hydrators.NODE)).withDefault(RichNode.empty()),
            CONTENTS,
            (hoverText, children) -> new RichNode.HoverMC(
                new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    net.minecraft.network.chat.MutableComponent.create(
                        new com.uqlism.emoji_deco.text.ir.HoverRichContents(hoverText))),
                children)));

    private static final Hydrator<RichNode> HOVER_ITEM_NODE = cached(Hydrator.zip(
            Hydrator.field("id",    STRING.withDefault("minecraft:air")),
            Hydrator.field("count", FLOAT.withDefault(1f)).map(f -> Math.round(f)),
            Hydrator.field("nbt",   STRING.withDefault(null)),
            CONTENTS,
            (id, count, nbt, children) -> {
                var rl = ResourceLocation.tryParse(id);
                Item item = rl != null ? BuiltInRegistries.ITEM.get(rl) : Items.AIR;
                ItemStack stack = new ItemStack(item, count);
                if (nbt != null && !nbt.isEmpty()) {
                    try { stack.setTag(TagParser.parseTag(nbt)); } catch (Exception ignored) {}
                }
                return new RichNode.HoverMC(
                    new HoverEvent(HoverEvent.Action.SHOW_ITEM, new HoverEvent.ItemStackInfo(stack)),
                    children);
            }));

    private static final Hydrator<RichNode> HOVER_ENTITY_NODE = cached(Hydrator.zip(
            Hydrator.field("entity_type", STRING.withDefault("minecraft:pig")),
            Hydrator.field("name",        Hydrator.lazy(() -> Hydrators.NODE).withDefault(null)),
            Hydrator.field("id",          STRING.withDefault(null)),
            CONTENTS,
            (entityType, nameNode, idStr, children) -> {
                var rl = ResourceLocation.tryParse(entityType);
                EntityType<?> type = rl != null
                    ? BuiltInRegistries.ENTITY_TYPE.getOptional(rl).orElse(EntityType.PIG)
                    : EntityType.PIG;
                net.minecraft.network.chat.Component nameComp =
                    nameNode != null ? nameNode.toComponent() : null;
                UUID uuid;
                try { uuid = idStr != null ? UUID.fromString(idStr) : new UUID(0, 0); }
                catch (Exception e) { uuid = new UUID(0, 0); }
                return new RichNode.HoverMC(
                    new HoverEvent(HoverEvent.Action.SHOW_ENTITY,
                        new HoverEvent.EntityTooltipInfo(type, uuid, nameComp)),
                    children);
            }));

    private static Hydrator<RichNode> clickNode(ClickEvent.Action action, String valueField) {
        return cached(Hydrator.zip(
            Hydrator.field(valueField, STRING.withDefault("")),
            CONTENTS,
            (value, children) -> new RichNode.Click(new ClickEvent(action, value), children)));
    }

    private static final Hydrator<RichNode> INSERTION_NODE = cached(Hydrator.zip(
            Hydrator.field("text", STRING.withDefault("")),
            CONTENTS,
            (text, children) -> new RichNode.Insertion(text, children)));

    // ── Image hydrators — before NODE so NODE_DISPATCH can reference IMAGE_GLYPH_NODE directly ──

    private static final Hydrator<BinarySource> BINARY_SOURCE = Hydrator.dispatch(Map.of(
            "emoji_deco:fetch_url", Hydrator.zip(
                    Hydrator.field("url",        STRING.map(u -> u.isEmpty() ? null : u)),
                    Hydrator.field("disk_cache", BOOL.withDefault(false)),
                    Hydrator.field("ttl",        FLOAT.withDefault(0f)).map(f -> Math.round(f)),
                    BinarySource.Url::new),
            "emoji_deco:fetch_resource", Hydrator.field("path", STRING)
                    .map(p -> p.isEmpty() ? null : new BinarySource.Resource(p))
    ), null);

    private static final Hydrator<ImageSpec> IMAGE_SPEC = Hydrator.dispatch(Map.of(
            "emoji_deco:decode_image", Hydrators::decodedSpec,
            "emoji_deco:fetch_atlas",  Hydrator.zip(
                    Hydrator.field("atlas",  STRING.map(s -> s.isEmpty() ? null : s)),
                    Hydrator.field("sprite", STRING.map(s -> s.isEmpty() ? null : s)),
                    (a, s) -> new ImageSpec.Atlas(a, s)),
            "emoji_deco:fetch_skin",   Hydrator.field("player", STRING)
                    .map(p -> p.isEmpty() ? null : new ImageSpec.Skin(p))
    ), null);

    private static final Hydrator<int[]> CROP_H = Hydrator.list(FLOAT)
            .map(fs -> fs.size() == 4
                    ? new int[]{Math.round(fs.get(0)), Math.round(fs.get(1)),
                                Math.round(fs.get(2)), Math.round(fs.get(3))}
                    : null);

    private record ImageBundle(ImageSpec spec, @Nullable int[] crop) {}

    private static final Hydrator<ImageBundle> IMAGE_BUNDLE_H = (el, ctx) -> {
        ImageSpec spec = IMAGE_SPEC.hydrate(el, ctx);
        if (spec == null) return null;
        return new ImageBundle(spec, Hydrator.field("uv", CROP_H).hydrate(el, ctx));
    };

    private static final Hydrator<RichNode> IMAGE_GLYPH_NODE = cached(Hydrator.zip(
            Hydrator.field("width",   FLOAT.withDefault(8f)).map(f -> Math.round(f)),
            Hydrator.field("height",  FLOAT.withDefault(8f)).map(f -> Math.round(f)),
            Hydrator.field("advance", FLOAT.withDefault(Float.NaN)),
            Hydrator.field("image",   IMAGE_BUNDLE_H),
            (w, h, advance, bundle) -> new RichNode.Image(bundle.spec(), bundle.crop(), w, h, advance)));

    // STRING.map covers: primitives, emoji_deco:arg, emoji_deco:join, emoji_deco:player_names.
    // list(lazy(NODE)) covers arrays; lazy breaks the self-reference initialisation cycle.
    public static final Hydrator<RichNode> NODE = Hydrator.firstOf(
            STRING.map(str -> new RichNode.Text(str, Style.EMPTY, List.of())),
            Hydrator.list(Hydrator.lazy(() -> Hydrators.NODE))
                    .map(children -> new RichNode.Text("", Style.EMPTY, children)),
            Hydrator.dispatch(
                Map.ofEntries(
                        Map.entry("emoji_deco:slot",           (Hydrator<RichNode>) (el, ctx) -> {
                            RichNode s = ctx.getSlot(); return s != null ? s : RichNode.empty();
                        }),
                        Map.entry("emoji_deco:glow",           GLOW_NODE),
                        Map.entry("emoji_deco:scale",          SCALE_NODE),
                        Map.entry("emoji_deco:offset",         OFFSET_NODE),
                        Map.entry("emoji_deco:rotate",         ROTATE_NODE),
                        Map.entry("emoji_deco:image_to_glyph", IMAGE_GLYPH_NODE),
                        Map.entry("emoji_deco:apply_shortcode",Hydrators::applyShortcodeNode),
                        Map.entry("emoji_deco:apply_decorator",Hydrators::applyDecoratorNode),
                        Map.entry("emoji_deco:time",           TIME_VAL.map(f -> new RichNode.Text(String.valueOf(f), Style.EMPTY, List.of()))),
                        Map.entry("emoji_deco:hover/text",     HOVER_NODE),
                        Map.entry("emoji_deco:hover/item",     HOVER_ITEM_NODE),
                        Map.entry("emoji_deco:hover/entity",   HOVER_ENTITY_NODE),
                        Map.entry("emoji_deco:click/open_url",          clickNode(ClickEvent.Action.OPEN_URL,          "url")),
                        Map.entry("emoji_deco:click/run_command",       clickNode(ClickEvent.Action.RUN_COMMAND,       "command")),
                        Map.entry("emoji_deco:click/suggest_command",   clickNode(ClickEvent.Action.SUGGEST_COMMAND,   "command")),
                        Map.entry("emoji_deco:click/change_page",       clickNode(ClickEvent.Action.CHANGE_PAGE,       "page")),
                        Map.entry("emoji_deco:click/copy_to_clipboard", clickNode(ClickEvent.Action.COPY_TO_CLIPBOARD, "text")),
                        Map.entry("emoji_deco:insertion",      INSERTION_NODE),
                        Map.entry("emoji_deco:style",          (Hydrator<RichNode>) Hydrators::styleNode)
                ),
                Hydrators::standardTextNode)
    ).withDefault(RichNode.empty());

    /**
     * Root-level cached hydrator — used by managers instead of HydrateCache.
     * 64 entries per display JsonElement matches the old HydrateCache capacity.
     * WeakHashMap keys are auto-cleared on resource pack reload.
     */
    public static final Hydrator<RichNode> CACHED_NODE = NODE.cached(64);

    // ── String handlers ────────────────────────────────────────────────────────

    private static final Hydrator<String> JOIN_STR = Hydrator.zip(
            Hydrator.field("separator", STRING.withDefault("")),
            Hydrator.field("parts",     Hydrator.list(STRING)),
            (sep, parts) -> String.join(sep, parts));

    // ── Color helpers ──────────────────────────────────────────────────────────

    /** HSV (各 0.0〜1.0) → "#RRGGBB" 16進数カラー文字列。TextColor.parseColor() が解釈できる。 */
    private static String hsvToHex(float h, float s, float v) {
        h = ((h % 1f) + 1f) % 1f;  // [0,1) に正規化
        int r, g, b;
        if (s <= 0f) {
            int grey = Math.max(0, Math.min(255, Math.round(v * 255f)));
            r = g = b = grey;
        } else {
            float h6 = h * 6f;
            int   i  = (int) h6;
            float f  = h6 - i;
            float p  = v * (1f - s), q = v * (1f - s * f), t = v * (1f - s * (1f - f));
            float fr, fg, fb;
            switch (i % 6) {
                case 0: fr = v; fg = t; fb = p; break;
                case 1: fr = q; fg = v; fb = p; break;
                case 2: fr = p; fg = v; fb = t; break;
                case 3: fr = p; fg = q; fb = v; break;
                case 4: fr = t; fg = p; fb = v; break;
                default:fr = v; fg = p; fb = q; break;
            }
            r = Math.max(0, Math.min(255, Math.round(fr * 255f)));
            g = Math.max(0, Math.min(255, Math.round(fg * 255f)));
            b = Math.max(0, Math.min(255, Math.round(fb * 255f)));
        }
        return String.format("#%02X%02X%02X", r, g, b);
    }

    // ── Float handlers ─────────────────────────────────────────────────────────

    /** Applies cached() while widening the type from Hydrator<? extends T> to Hydrator<T>. */
    @SuppressWarnings("unchecked")
    private static <T> Hydrator<T> cached(Hydrator<? extends T> h) { return (Hydrator<T>) h.cached(); }

    private static @Nullable Float tryParseFloat(String s) {
        try { return Float.parseFloat(s); } catch (NumberFormatException e) { return null; }
    }

    // ── Bool handler ───────────────────────────────────────────────────────────

    private static Boolean parseBool(String s) {
        return s.equalsIgnoreCase("true") || s.equals("1") || s.equalsIgnoreCase("yes");
    }

    // ── Structural node handlers ───────────────────────────────────────────────

    private static RichNode standardTextNode(JsonElement el, HydrateContext.Tracked ctx) {
        JsonObject obj  = el.getAsJsonObject();
        String     text = fld(obj, "text", ctx);
        Style      style = Style.EMPTY;

        if (obj.has("color")) {
            TextColor color = TextColor.parseColor(fld(obj, "color", ctx));
            if (color != null) style = style.withColor(color);
        }
        if (obj.has("bold"))          style = style.withBold(fld(obj, "bold", false, ctx));
        if (obj.has("italic"))        style = style.withItalic(fld(obj, "italic", false, ctx));
        if (obj.has("strikethrough")) style = style.withStrikethrough(fld(obj, "strikethrough", false, ctx));
        if (obj.has("underlined"))    style = style.withUnderlined(fld(obj, "underlined", false, ctx));
        if (obj.has("obfuscated"))    style = style.withObfuscated(fld(obj, "obfuscated", false, ctx));
        if (obj.has("font")) {
            ResourceLocation rl = ResourceLocation.tryParse(fld(obj, "font", ctx));
            if (rl != null) style = style.withFont(rl);
        }
        List<RichNode> children = new ArrayList<>();
        if (obj.has("extra") && obj.get("extra").isJsonArray())
            for (JsonElement child : obj.getAsJsonArray("extra"))
                children.add(NODE.hydrate(child, ctx));
        return new RichNode.Text(text, style, children);
    }

    // ── Image ──────────────────────────────────────────────────────────────────

    // format is optional (null = no format) — prevents using zip directly
    private static @Nullable ImageSpec decodedSpec(JsonElement el, HydrateContext.Tracked ctx) {
        String       format = Hydrator.field("format", STRING).map(s -> s.isEmpty() ? null : s).hydrate(el, ctx);
        BinarySource src    = Hydrator.field("source", BINARY_SOURCE).hydrate(el, ctx);
        return src != null ? new ImageSpec.Decoded(format, src) : null;
    }

    // ── emoji_deco:style ──────────────────────────────────────────────────────

    /**
     * 各スタイルフィールドを直接 hydrate() して null チェックする。
     * null（フィールド不在・JSON null・動的式が null を返す）= inherit。
     * zip / withDefault は使わない（null を failure 扱いするため）。
     */
    private static RichNode styleNode(JsonElement el, HydrateContext.Tracked ctx) {
        JsonObject obj = el.getAsJsonObject();
        Style style = Style.EMPTY;

        String colorStr = STRING.hydrate(obj.get("color"), ctx);
        if (colorStr != null) {
            TextColor tc = TextColor.parseColor(colorStr);
            if (tc != null) style = style.withColor(tc);
        }
        Boolean bold          = BOOL.hydrate(obj.get("bold"),          ctx); if (bold          != null) style = style.withBold(bold);
        Boolean italic        = BOOL.hydrate(obj.get("italic"),        ctx); if (italic        != null) style = style.withItalic(italic);
        Boolean underlined    = BOOL.hydrate(obj.get("underlined"),    ctx); if (underlined    != null) style = style.withUnderlined(underlined);
        Boolean strikethrough = BOOL.hydrate(obj.get("strikethrough"), ctx); if (strikethrough != null) style = style.withStrikethrough(strikethrough);
        Boolean obfuscated    = BOOL.hydrate(obj.get("obfuscated"),    ctx); if (obfuscated    != null) style = style.withObfuscated(obfuscated);
        String fontStr = STRING.hydrate(obj.get("font"), ctx);
        if (fontStr != null) {
            ResourceLocation rl = ResourceLocation.tryParse(fontStr);
            if (rl != null) style = style.withFont(rl);
        }

        RichNode contents = NODE.hydrate(obj.get("contents"), ctx);
        return new RichNode.Text("", style, contents != null ? List.of(contents) : List.of());
    }

    // ── apply_shortcode / apply_decorator ──────────────────────────────────────

    private static RichNode applyShortcodeNode(JsonElement el, HydrateContext.Tracked ctx) {
        JsonObject obj  = el.getAsJsonObject();
        String     name = fld(obj, "shortcode", ctx);
        if (name.isEmpty() || !ShortcodeManager.has(name)) return RichNode.empty();
        String[] args = extractCallArgs(obj, ctx);
        String   key  = "s:" + name;
        if (!RESOLVING.get().add(key)) { LOGGER.warn("[EmojiDeco] Cyclic apply_shortcode: '{}'", name); return RichNode.empty(); }
        try { return ShortcodeManager.hydrateWith(name, args, null, ctx); }
        finally { RESOLVING.get().remove(key); }
    }

    private static RichNode applyDecoratorNode(JsonElement el, HydrateContext.Tracked ctx) {
        JsonObject obj  = el.getAsJsonObject();
        String     name = fld(obj, "decorator", ctx);
        if (name.isEmpty() || !DecoratorManager.has(name)) return RichNode.empty();
        RichNode slot = obj.has("slot") ? NODE.hydrate(obj.get("slot"), ctx) : RichNode.empty();
        String[] args = extractCallArgs(obj, ctx);
        String   key  = "d:" + name;
        if (!RESOLVING.get().add(key)) { LOGGER.warn("[EmojiDeco] Cyclic apply_decorator: '{}'", name); return slot; }
        try {
            RichNode result = DecoratorManager.hydrateWith(name, slot, args, ctx);
            return result != null ? result : slot;
        } finally { RESOLVING.get().remove(key); }
    }

    private static String[] extractCallArgs(JsonObject obj, HydrateContext.Tracked ctx) {
        return Hydrator.field("args", Hydrator.list(STRING.withDefault("")))
                .withDefault(List.of())
                .hydrate(obj, ctx)
                .toArray(String[]::new);
    }

    // ── Field accessors with defaults ──────────────────────────────────────────

    private static float   fld(JsonObject o, String k, float   def, HydrateContext.Tracked c) { Float   r = FLOAT .hydrate(o.get(k), c); return r != null ? r : def; }
    private static String  fld(JsonObject o, String k,              HydrateContext.Tracked c) { String  r = STRING.hydrate(o.get(k), c); return r != null ? r : "";  }
    private static String  fld(JsonObject o, String k, String  def, HydrateContext.Tracked c) { String  r = STRING.hydrate(o.get(k), c); return r != null ? r : def; }
    private static boolean fld(JsonObject o, String k, boolean def, HydrateContext.Tracked c) { Boolean r = BOOL  .hydrate(o.get(k), c); return r != null ? r : def; }

    // ── Arg helpers ────────────────────────────────────────────────────────────

    /** Shared logic for {type:"emoji_deco:arg"} handlers.
     *  Extracts the arg string, applies parse; on empty/failure falls back
     *  to the "default" field (evaluated via defaultH, so defaults can be expressions). */
    private static <T> Hydrator<T> argOf(
            Function<String, @Nullable T> parse, Hydrator<T> defaultH) {
        return (el, ctx) -> {
            JsonObject  obj = el.getAsJsonObject();
            int         idx = obj.has("index") ? obj.get("index").getAsInt() : 0;
            String      v   = ctx.getArg(idx);
            if (!v.isEmpty()) {
                T r = parse.apply(v);
                if (r != null) return r;
            }
            JsonElement def = argDefaultJson(obj, ctx, idx);
            return def != null ? defaultH.hydrate(def, ctx) : null;
        };
    }

    @Nullable
    private static JsonElement argDefaultJson(JsonObject ref, HydrateContext.Tracked ctx, int idx) {
        if (ref.has("default")) return ref.get("default");
        JsonObject top = topSpec(ctx.getTopArgSpecs(), idx);
        return (top != null && top.has("default")) ? top.get("default") : null;
    }

    @Nullable
    private static JsonObject topSpec(@Nullable JsonArray specs, int index) {
        if (specs == null || index < 0 || index >= specs.size()) return null;
        JsonElement el = specs.get(index);
        return el.isJsonObject() ? el.getAsJsonObject() : null;
    }
}
