package com.uqlism.emoji_deco.text.hydrate;

import com.uqlism.emoji_deco.text.ir.ParsedNode;
import com.uqlism.emoji_deco.text.ir.RichNode;
import com.uqlism.emoji_deco.text.registry.DecoratorManager;
import com.uqlism.emoji_deco.text.registry.ShortcodeManager;

import com.mojang.logging.LogUtils;
import com.uqlism.emoji_deco.render.image.BinarySource;
import com.uqlism.emoji_deco.render.image.ImageGlyphPool;
import com.uqlism.emoji_deco.render.image.ImageSpec;
import com.uqlism.emoji_deco.render.sequence.LightMode;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Converts a ParsedNode to a RichNode using a HydrateContext.
 *
 * - apply_shortcode / apply_decorator are resolved via ShortcodeManager / DecoratorManager
 *   which maintain per-entry HydrateCaches keyed by AccessPattern.
 * - Cycle detection uses a per-thread stack (same as before).
 */
public final class NodeHydrator {

    private static final Logger    LOGGER = LogUtils.getLogger();
    private static final AtomicLong TICK  = new AtomicLong(0);

    private static final ThreadLocal<Set<String>> RESOLVING = ThreadLocal.withInitial(HashSet::new);

    private NodeHydrator() {}

    public static void tick() { TICK.incrementAndGet(); }
    public static long currentTick() { return TICK.get(); }

    // ── Public API ────────────────────────────────────────────────────────────

    public static RichNode hydrate(ParsedNode node, HydrateContext ctx) {
        return hydrateTracked(node, ctx.track());
    }

    // ── Core dispatch ─────────────────────────────────────────────────────────

    public static RichNode hydrateTracked(ParsedNode node, HydrateContext.Tracked ctx) {
        if (node instanceof ParsedNode.Text t)          return hydrateText(t, ctx);
        if (node instanceof ParsedNode.Many m)          return hydrateMany(m, ctx);
        if (node instanceof ParsedNode.Image img)       return hydrateImage(img, ctx);
        if (node instanceof ParsedNode.Glow g)          return hydrateGlow(g, ctx);
        if (node instanceof ParsedNode.Offset o)        return hydrateOffset(o, ctx);
        if (node instanceof ParsedNode.Scale s)         return hydrateScale(s, ctx);
        if (node instanceof ParsedNode.Rotate r)        return hydrateRotate(r, ctx);
        if (node instanceof ParsedNode.SlotRef) {
            RichNode s = ctx.getSlot();
            return s != null ? s : RichNode.empty();
        }
        if (node instanceof ParsedNode.ArgAsText a) {
            String v = ctx.getArg(a.index());
            return new RichNode.Text(v.isEmpty() ? a.defaultVal() : v, Style.EMPTY, List.of());
        }
        if (node instanceof ParsedNode.ApplyShortcode sc) return hydrateApplyShortcode(sc, ctx);
        if (node instanceof ParsedNode.ApplyDecorator d)  return hydrateApplyDecorator(d, ctx);
        return RichNode.empty();
    }

    // ── Node hydrators ────────────────────────────────────────────────────────

    private static RichNode hydrateText(ParsedNode.Text t, HydrateContext.Tracked ctx) {
        Style style = Style.EMPTY;
        if (!t.style().isEmpty()) {
            ParsedNode.ParsedStyle ps = t.style();
            if (ps.color() != null) {
                TextColor color = TextColor.parseColor(evalString(ps.color(), ctx));
                if (color != null) style = style.withColor(color);
            }
            if (ps.bold()          != null) style = style.withBold(evalBool(ps.bold(), ctx));
            if (ps.italic()        != null) style = style.withItalic(evalBool(ps.italic(), ctx));
            if (ps.strikethrough() != null) style = style.withStrikethrough(evalBool(ps.strikethrough(), ctx));
            if (ps.underlined()    != null) style = style.withUnderlined(evalBool(ps.underlined(), ctx));
            if (ps.obfuscated()    != null) style = style.withObfuscated(evalBool(ps.obfuscated(), ctx));
            if (ps.font()          != null) {
                ResourceLocation rl = ResourceLocation.tryParse(ps.font());
                if (rl != null) style = style.withFont(rl);
            }
        }
        List<RichNode> children = new ArrayList<>();
        for (ParsedNode child : t.children()) children.add(hydrateTracked(child, ctx));
        return new RichNode.Text(t.literal(), style, children);
    }

    private static RichNode hydrateMany(ParsedNode.Many m, HydrateContext.Tracked ctx) {
        List<RichNode> children = new ArrayList<>();
        for (ParsedNode item : m.items()) children.add(hydrateTracked(item, ctx));
        return new RichNode.Text("", Style.EMPTY, children);
    }

    private static RichNode hydrateImage(ParsedNode.Image img, HydrateContext.Tracked ctx) {
        ImageSpec spec = evalImageSpec(img.imageSpec(), ctx);
        if (spec == null) return RichNode.empty();
        int w = Math.round(evalFloat(img.w(), ctx));
        int h = Math.round(evalFloat(img.h(), ctx));
        float advance = evalFloat(img.advance(), ctx);
        return new RichNode.Image(spec, img.crop(), w, h, advance);
    }

    private static RichNode hydrateGlow(ParsedNode.Glow g, HydrateContext.Tracked ctx) {
        LightMode mode = evalBool(g.glow(), ctx) ? LightMode.GLOW : LightMode.AMBIENT;
        return new RichNode.Glowing(mode, List.of(hydrateTracked(g.contents(), ctx)));
    }

    private static RichNode hydrateOffset(ParsedNode.Offset o, HydrateContext.Tracked ctx) {
        return new RichNode.Offset(evalFloat(o.x(), ctx), evalFloat(o.y(), ctx), evalFloat(o.z(), ctx),
                List.of(hydrateTracked(o.contents(), ctx)));
    }

    private static RichNode hydrateScale(ParsedNode.Scale s, HydrateContext.Tracked ctx) {
        return new RichNode.Scaled(evalFloat(s.x(), ctx), evalFloat(s.y(), ctx),
                List.of(hydrateTracked(s.contents(), ctx)));
    }

    private static RichNode hydrateRotate(ParsedNode.Rotate r, HydrateContext.Tracked ctx) {
        return new RichNode.Rotated(evalFloat(r.angle(), ctx),
                List.of(hydrateTracked(r.contents(), ctx)));
    }

    private static RichNode hydrateApplyShortcode(ParsedNode.ApplyShortcode sc, HydrateContext.Tracked ctx) {
        if (!ShortcodeManager.hasParsed(sc.shortcode())) return RichNode.empty();
        String[] callArgs = sc.callArgs().stream().map(a -> evalString(a, ctx)).toArray(String[]::new);
        String key = "s:" + sc.shortcode();
        if (!RESOLVING.get().add(key)) {
            LOGGER.warn("[EmojiDeco] Cyclic apply_shortcode: '{}'", sc.shortcode());
            return RichNode.empty();
        }
        try { return ShortcodeManager.hydrateWith(sc.shortcode(), callArgs, null); }
        finally { RESOLVING.get().remove(key); }
    }

    private static RichNode hydrateApplyDecorator(ParsedNode.ApplyDecorator d, HydrateContext.Tracked ctx) {
        if (!DecoratorManager.hasParsed(d.decorator())) return RichNode.empty();
        RichNode slot     = hydrateTracked(d.slot(), ctx);
        String[] callArgs = d.callArgs().stream().map(a -> evalString(a, ctx)).toArray(String[]::new);
        String key = "d:" + d.decorator();
        if (!RESOLVING.get().add(key)) {
            LOGGER.warn("[EmojiDeco] Cyclic apply_decorator: '{}'", d.decorator());
            return slot;
        }
        try {
            RichNode result = DecoratorManager.hydrateWith(d.decorator(), slot, callArgs);
            return result != null ? result : slot;
        } finally { RESOLVING.get().remove(key); }
    }

    // ── Expression evaluators ─────────────────────────────────────────────────

    public static String evalString(ParsedNode.StringVal val, HydrateContext.Tracked ctx) {
        if (val instanceof ParsedNode.StringVal.Literal l) return l.value();
        if (val instanceof ParsedNode.StringVal.Arg a) {
            String v = ctx.getArg(a.index());
            return v.isEmpty() ? a.defaultVal() : v;
        }
        if (val instanceof ParsedNode.StringVal.Join j) {
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (ParsedNode.StringVal part : j.parts()) {
                if (!first) sb.append(j.separator());
                first = false;
                sb.append(evalString(part, ctx));
            }
            return sb.toString();
        }
        if (val instanceof ParsedNode.StringVal.PlayerNames) {
            ctx.markPlayerNamesAccessed();
            return "";  // player names are for suggestions, not display strings
        }
        return "";
    }

    public static float evalFloat(ParsedNode.FloatVal val, HydrateContext.Tracked ctx) {
        if (val instanceof ParsedNode.FloatVal.Literal l) return l.value();
        if (val instanceof ParsedNode.FloatVal.Arg a) {
            String v = ctx.getArg(a.index());
            try { return v.isEmpty() ? a.defaultVal() : Float.parseFloat(v); }
            catch (NumberFormatException e) { return a.defaultVal(); }
        }
        if (val instanceof ParsedNode.FloatVal.Time t) {
            ctx.markTimeAccessed();
            Minecraft mc = Minecraft.getInstance();
            long gameTime = (mc != null && mc.level != null) ? mc.level.getGameTime() : 0L;
            long dayTime  = (mc != null && mc.level != null) ? mc.level.getDayTime()  : 0L;
            float partial = (mc != null) ? mc.getPartialTick() : 0f;
            float raw;
            switch (t.timeType()) {
                case "daytime": raw = (float)(dayTime % 24000L) + partial; break;
                case "day":     raw = (float)(dayTime / 24000L); break;
                default:        raw = (float)gameTime + partial; break;
            }
            return raw * t.scale() + t.offset();
        }
        return 0f;
    }

    public static boolean evalBool(ParsedNode.BoolVal val, HydrateContext.Tracked ctx) {
        if (val instanceof ParsedNode.BoolVal.Literal l) return l.value();
        if (val instanceof ParsedNode.BoolVal.Arg a) {
            String v = ctx.getArg(a.index());
            return v.isEmpty() ? a.defaultVal()
                    : (v.equalsIgnoreCase("true") || v.equals("1") || v.equalsIgnoreCase("yes"));
        }
        return false;
    }

    // ── Image spec evaluators ─────────────────────────────────────────────────

    @Nullable
    private static ImageSpec evalImageSpec(ParsedNode.ParsedImageSpec spec, HydrateContext.Tracked ctx) {
        if (spec instanceof ParsedNode.ParsedImageSpec.Decoded d) {
            String format = d.format() != null ? evalString(d.format(), ctx) : null;
            if (format != null && format.isEmpty()) format = null;
            BinarySource src = evalBinarySource(d.source(), ctx);
            return src != null ? new ImageSpec.Decoded(format, src) : null;
        }
        if (spec instanceof ParsedNode.ParsedImageSpec.Atlas a) {
            String atlas  = evalString(a.atlas(),  ctx);
            String sprite = evalString(a.sprite(), ctx);
            return (atlas.isEmpty() || sprite.isEmpty()) ? null : new ImageSpec.Atlas(atlas, sprite);
        }
        if (spec instanceof ParsedNode.ParsedImageSpec.Skin s) {
            String player = evalString(s.player(), ctx);
            return player.isEmpty() ? null : new ImageSpec.Skin(player);
        }
        return null;
    }

    @Nullable
    private static BinarySource evalBinarySource(ParsedNode.ParsedBinarySource src, HydrateContext.Tracked ctx) {
        if (src instanceof ParsedNode.ParsedBinarySource.Url u) {
            String url = evalString(u.url(), ctx);
            return url.isEmpty() ? null : new BinarySource.Url(url, u.diskCache(), u.ttlSeconds());
        }
        if (src instanceof ParsedNode.ParsedBinarySource.Resource r) {
            String path = evalString(r.path(), ctx);
            return path.isEmpty() ? null : new BinarySource.Resource(path);
        }
        return null;
    }

    // ── GC ────────────────────────────────────────────────────────────────────

    /** Run periodically from ClientEvents.onClientTick() to evict stale cache entries. */
    public static void gcCaches(long currentTick) {
        ShortcodeManager.gcCaches(currentTick);
        DecoratorManager.gcCaches(currentTick);
    }
}
