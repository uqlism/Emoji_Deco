package com.uqlism.emoji_deco.text.mfm.convert;

import com.uqlism.emoji_deco.text.hydrate.HydrateContext;
import com.uqlism.emoji_deco.text.hydrate.Hydrators;
import com.uqlism.emoji_deco.text.ir.RichNode;
import com.uqlism.emoji_deco.text.mfm.node.IMfmNode;
import com.uqlism.emoji_deco.text.mfm.node.IMfmNodeChildrenHolder;
import com.uqlism.emoji_deco.text.mfm.node.MfmBlockCode;
import com.uqlism.emoji_deco.text.mfm.node.MfmBold;
import com.uqlism.emoji_deco.text.mfm.node.MfmCenter;
import com.uqlism.emoji_deco.text.mfm.node.MfmEmojiCode;
import com.uqlism.emoji_deco.text.mfm.node.MfmFn;
import com.uqlism.emoji_deco.text.mfm.node.MfmHashtag;
import com.uqlism.emoji_deco.text.mfm.node.MfmInlineCode;
import com.uqlism.emoji_deco.text.mfm.node.MfmItalic;
import com.uqlism.emoji_deco.text.mfm.node.MfmLink;
import com.uqlism.emoji_deco.text.mfm.node.MfmMathBlock;
import com.uqlism.emoji_deco.text.mfm.node.MfmMathInline;
import com.uqlism.emoji_deco.text.mfm.node.MfmMention;
import com.uqlism.emoji_deco.text.mfm.node.MfmNest;
import com.uqlism.emoji_deco.text.mfm.node.MfmPlain;
import com.uqlism.emoji_deco.text.mfm.node.MfmQuote;
import com.uqlism.emoji_deco.text.mfm.node.MfmSearch;
import com.uqlism.emoji_deco.text.mfm.node.MfmSmall;
import com.uqlism.emoji_deco.text.mfm.node.MfmStrike;
import com.uqlism.emoji_deco.text.mfm.node.MfmText;
import com.uqlism.emoji_deco.text.mfm.node.MfmUnicodeEmoji;
import com.uqlism.emoji_deco.text.mfm.node.MfmUrl;
import com.uqlism.emoji_deco.text.registry.CustomEmojiResolver;
import com.uqlism.emoji_deco.render.image.BinarySource;
import com.uqlism.emoji_deco.render.image.ImageSpec;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts an MFM AST ({@link IMfmNode} tree, from {@link com.uqlism.emoji_deco.text.mfm.Mfm})
 * into this mod's {@link RichNode} intermediate representation.
 *
 * <p>Unlike the existing {@code decorator} JSON DSL / {@code Hydrators} pipeline, this does not go
 * through JSON hydration — it walks the AST directly with a {@code switch} pattern match. Any MFM
 * construct this mod doesn't render (see the implementation plan's "v1 unsupported" list — tada,
 * jelly, twitch, shake, jump, bounce, blur, ruby, unixtime, clickable, bg, font, math,
 * search, and any node type not explicitly handled below) falls back to rendering its children
 * with no extra decoration, so unsupported syntax degrades to plain text instead of disappearing
 * or throwing.
 */
public final class MfmToRichNodeConverter {
    private MfmToRichNodeConverter() {}

    /** Hue advances by this fraction of a full cycle per game tick, absent an explicit {@code speed} arg. */
    private static final float DEFAULT_RAINBOW_SPEED = 1f / 100f;
    /** Default rotation (degrees) for {@code $[rotate]} when no {@code deg} arg is given. */
    private static final float DEFAULT_ROTATE_DEG = 90f;

    public static RichNode convert(List<IMfmNode> nodes, HydrateContext.Tracked ctx) {
        return convert(nodes, ctx, Style.EMPTY);
    }

    public static RichNode convert(List<IMfmNode> nodes, HydrateContext.Tracked ctx, Style base) {
        return new RichNode.Text("", base, convertList(nodes, ctx));
    }

    private static List<RichNode> convertList(List<IMfmNode> nodes, HydrateContext.Tracked ctx) {
        List<RichNode> out = new ArrayList<>(nodes.size());
        for (IMfmNode n : nodes) out.add(convertNode(n, ctx));
        return out;
    }

    private static RichNode convertNode(IMfmNode node, HydrateContext.Tracked ctx) {
        return switch (node) {
            case MfmText t -> new RichNode.Text(t.text(), Style.EMPTY, List.of());
            case MfmUnicodeEmoji e -> new RichNode.Text(e.emoji(), Style.EMPTY, List.of());
            case MfmEmojiCode ec -> emojiCodeNode(ec);

            case MfmBold b -> new RichNode.Text("", Style.EMPTY.withBold(true), convertList(b.children(), ctx));
            case MfmItalic i -> new RichNode.Text("", Style.EMPTY.withItalic(true), convertList(i.children(), ctx));
            case MfmStrike s -> new RichNode.Text("", Style.EMPTY.withStrikethrough(true), convertList(s.children(), ctx));
            case MfmSmall s -> new RichNode.Scaled(0.5f, 0.5f, convertList(s.children(), ctx));

            case MfmQuote q -> quoteNode(q, ctx);
            case MfmCenter c -> new RichNode.Text("", Style.EMPTY, convertList(c.children(), ctx));

            // No monospace font ships with this mod / vanilla resources; approximate "code" with a
            // muted color instead of leaving it visually identical to surrounding prose.
            case MfmInlineCode c -> new RichNode.Text(c.code(), Style.EMPTY.withColor(TextColor.fromRgb(0xAAAAAA)), List.of());
            case MfmBlockCode c -> new RichNode.Text(c.code(), Style.EMPTY.withColor(TextColor.fromRgb(0xAAAAAA)), List.of());

            case MfmUrl u -> new RichNode.Click(new ClickEvent(ClickEvent.Action.OPEN_URL, u.url()),
                    List.of(new RichNode.Text(u.url(), Style.EMPTY, List.of())));
            case MfmLink l -> new RichNode.Click(new ClickEvent(ClickEvent.Action.OPEN_URL, l.url()),
                    convertList(l.children(), ctx));

            case MfmMention m -> new RichNode.Text(m.acct(), Style.EMPTY.withColor(TextColor.fromRgb(0x8899FF)), List.of());
            case MfmHashtag h -> new RichNode.Text("#" + h.hashtag(), Style.EMPTY.withColor(TextColor.fromRgb(0x8899FF)), List.of());

            case MfmFn fn -> fnNode(fn, ctx);

            // v1-unsupported block/inline constructs: fall back to raw source text rather than
            // attempting real rendering (math typesetting, search UI, plain-text decoration stripping).
            case MfmMathBlock mb -> new RichNode.Text(mb.formula(), Style.EMPTY, List.of());
            case MfmMathInline mi -> new RichNode.Text(mi.formula(), Style.EMPTY, List.of());
            case MfmSearch se -> new RichNode.Text(se.content(), Style.EMPTY, List.of());
            case MfmPlain p -> plainNode(p);

            case MfmNest n -> new RichNode.Text("", Style.EMPTY, convertList(n.children(), ctx));

            // Safety net: any node type not explicitly handled above (including future additions to
            // the mfm.kt port) still renders its children instead of vanishing or throwing.
            case IMfmNodeChildrenHolder holder -> new RichNode.Text("", Style.EMPTY, convertList(holder.children(), ctx));
            default -> new RichNode.Text(node.stringify(), Style.EMPTY, List.of());
        };
    }

    private static RichNode emojiCodeNode(MfmEmojiCode ec) {
        RichNode resolved = CustomEmojiResolver.resolve(ec.name());
        return resolved != null ? resolved : new RichNode.Text(ec.stringify(), Style.EMPTY, List.of());
    }

    private static RichNode quoteNode(MfmQuote q, HydrateContext.Tracked ctx) {
        List<RichNode> children = new ArrayList<>();
        children.add(new RichNode.Text("> ", Style.EMPTY.withColor(TextColor.fromRgb(0xAAAAAA)), List.of()));
        children.addAll(convertList(q.children(), ctx));
        return new RichNode.Text("", Style.EMPTY, children);
    }

    /** {@code <plain>} disables nested decoration parsing; children are rendered as raw text. */
    private static RichNode plainNode(MfmPlain p) {
        StringBuilder sb = new StringBuilder();
        collectPlainText(p.children(), sb);
        return new RichNode.Text(sb.toString(), Style.EMPTY, List.of());
    }

    private static void collectPlainText(List<IMfmNode> nodes, StringBuilder sb) {
        for (IMfmNode n : nodes) {
            if (n instanceof MfmText t) sb.append(t.text());
            else if (n instanceof IMfmNodeChildrenHolder h) collectPlainText(h.children(), sb);
            else sb.append(n.stringify());
        }
    }

    // ── $[fnName ...] ─────────────────────────────────────────────────────────

    private static RichNode fnNode(MfmFn fn, HydrateContext.Tracked ctx) {
        List<IMfmNode> rawChildren = fn.children();
        return switch (fn.name()) {
            case "x2" -> new RichNode.Scaled(2f, 2f, convertList(rawChildren, ctx));
            case "x3" -> new RichNode.Scaled(3f, 3f, convertList(rawChildren, ctx));
            case "x4" -> new RichNode.Scaled(4f, 4f, convertList(rawChildren, ctx));

            case "rotate" -> new RichNode.Rotated(
                    argFloat(fn, "deg", DEFAULT_ROTATE_DEG), convertList(rawChildren, ctx));

            case "position" -> new RichNode.Offset(
                    argFloat(fn, "x", 0f), argFloat(fn, "y", 0f), 0f, convertList(rawChildren, ctx));

            case "scale" -> new RichNode.Scaled(
                    argFloat(fn, "x", 1f), argFloat(fn, "y", 1f), convertList(rawChildren, ctx));

            case "flip" -> flipNode(fn, ctx);

            case "fg" -> fgNode(fn, ctx);

            case "rainbow" -> rainbowNode(fn, ctx);

            case "sparkle" -> sparkleNode(fn, ctx);

            // tada (incl. Big syntax ***abc***) and the rest of the v1-unsupported effect list
            // (jelly/twitch/shake/jump/bounce/blur/ruby/unixtime/clickable/bg/font):
            // pass children through undecorated rather than crashing or dropping text.
            default -> new RichNode.Text("", Style.EMPTY, convertList(rawChildren, ctx));
        };
    }

    private static RichNode flipNode(MfmFn fn, HydrateContext.Tracked ctx) {
        boolean h = fn.args().containsKey("h");
        boolean v = fn.args().containsKey("v");
        if (!h && !v) h = true; // bare $[flip] flips horizontally, matching Misskey's default
        return new RichNode.Scaled(h ? -1f : 1f, v ? -1f : 1f, convertList(fn.children(), ctx));
    }

    private static RichNode fgNode(MfmFn fn, HydrateContext.Tracked ctx) {
        List<RichNode> children = convertList(fn.children(), ctx);
        Object colorArg = fn.args().get("color");
        if (colorArg instanceof String colorStr) {
            TextColor tc = parseHexColor(colorStr);
            if (tc != null) return new RichNode.Text("", Style.EMPTY.withColor(tc), children);
        }
        return new RichNode.Text("", Style.EMPTY, children);
    }

    private static RichNode rainbowNode(MfmFn fn, HydrateContext.Tracked ctx) {
        ctx.markTimeAccessed();
        float speed = argFloat(fn, "speed", DEFAULT_RAINBOW_SPEED);

        Minecraft mc = Minecraft.getInstance();
        long gameTime = (mc != null && mc.level != null) ? mc.level.getGameTime() : 0L;
        float partial = mc != null ? mc.getTimer().getGameTimeDeltaPartialTick(true) : 0f;
        float hue = (gameTime + partial) * speed;
        hue = ((hue % 1f) + 1f) % 1f;

        TextColor tc = TextColor.parseColor(Hydrators.hsvToHex(hue, 1f, 1f)).result().orElse(null);
        Style style = tc != null ? Style.EMPTY.withColor(tc) : Style.EMPTY;
        return new RichNode.Text("", style, convertList(fn.children(), ctx));
    }

    // ── $[sparkle] ────────────────────────────────────────────────────────────
    // Star outline/colours/timings follow Misskey's MkSparkle.vue (AGPL-3.0); see THIRD_PARTY_NOTICES.md.

    /** Misskey: a new star every 500 + rand*500 ms. Modelled as one star per 750 ms slot, jittered by 250 ms. */
    private static final double SPARKLE_SLOT_MS = 750.0;
    private static final double SPARKLE_JITTER_MS = 250.0;
    /** Longest star lifetime (1000 + 1000 ms), used to know how many past slots can still be alive. */
    private static final double SPARKLE_MAX_LIFE_MS = 2000.0;
    /** Star image is drawn this many MC px wide/high before the per-star scale is applied. */
    private static final int STAR_BASE_PX = 16;
    /** 1 CSS px ~ 0.5 MC px; the 64px star box therefore peaks at 64 * size * 0.5 MC px. */
    private static final float STAR_MC_PER_CSS = 0.5f;
    // Must match Font.lineHeight and the rotate/scale origin used by RichNode.collectSegments.
    private static final float LINE_H = 9f;
    private static final float SCALE_BASELINE = LINE_H - 2f;
    private static final double MS_PER_TICK = 50.0;

    private static final String[] STAR_FILES = {"sparkle_pink", "sparkle_cyan", "sparkle_yellow"};
    /** Misskey picks uniformly from [pink, cyan, yellow, yellow, yellow]. */
    private static final int[] STAR_COLOR_PICK = {0, 1, 2, 2, 2};

    private static RichNode sparkleNode(MfmFn fn, HydrateContext.Tracked ctx) {
        ctx.markTimeAccessed();
        List<RichNode> out = new ArrayList<>(convertList(fn.children(), ctx));

        Minecraft mc = Minecraft.getInstance();
        long gameTime = (mc != null && mc.level != null) ? mc.level.getGameTime() : 0L;
        float partial = mc != null ? mc.getTimer().getGameTimeDeltaPartialTick(true) : 0f;
        double t = (gameTime + partial) * MS_PER_TICK;

        StringBuilder sb = new StringBuilder();
        collectPlainText(fn.children(), sb);
        String text = sb.toString();
        float width = (mc != null && mc.font != null) ? mc.font.width(text) : text.length() * 6f;
        width = Math.max(width, 6f);
        long seed = fn.stringify().hashCode() * 0x9E3779B97F4A7C15L;

        long firstSlot = Math.max(0L, (long) Math.floor((t - SPARKLE_MAX_LIFE_MS) / SPARKLE_SLOT_MS));
        long lastSlot = (long) Math.floor(t / SPARKLE_SLOT_MS);
        for (long slot = firstSlot; slot <= lastSlot; slot++) {
            long h = seed + slot * 0xD1B54A32D192ED03L;
            double spawn = slot * SPARKLE_SLOT_MS + mix01(h = next(h)) * SPARKLE_JITTER_MS;
            double sizeFactor = mix01(h = next(h));
            double dur = 1000.0 + sizeFactor * 1000.0;
            double age = t - spawn;
            // Misskey removes the element 100 ms before the animation ends (scale is ~0 there anyway).
            if (age < 0 || age >= dur - 100.0) continue;
            int color = STAR_COLOR_PICK[(int) (mix01(h = next(h)) * STAR_COLOR_PICK.length)];
            double px = mix01(h = next(h)) * width;
            double py = mix01(h = next(h)) * LINE_H;

            double phase = age / dur;
            double tri = phase < 0.5 ? phase * 2 : (1 - phase) * 2;
            double peakCss = 64.0 * (0.2 + 0.3 * sizeFactor);
            float scale = (float) (peakCss * tri * STAR_MC_PER_CSS / STAR_BASE_PX);
            if (scale < 0.03f) continue;
            float angle = (float) (360.0 * phase);

            // Nested Offset(Scaled(Rotated(star))) composes as  T + (0, base*(1-s)) + s * (Rc + R(p - Rc)),
            // where Rc = (0, lineHeight/2) is the rotate pivot for a zero-width child. Solve T so the
            // star's centre lands on (px - width, py): the stars sit after the text, hence the -width.
            double rad = Math.toRadians(angle);
            double cos = Math.cos(rad), sin = Math.sin(rad);
            double half = STAR_BASE_PX / 2.0;
            double rcY = LINE_H / 2.0;
            double vx = half, vy = half - rcY;
            double qx = vx * cos - vy * sin;
            double qy = rcY + vx * sin + vy * cos;
            float tx = (float) (px - width - scale * qx);
            float ty = (float) (py - SCALE_BASELINE * (1f - scale) - scale * qy);

            RichNode star = new RichNode.Image(starSpec(STAR_FILES[color]), null,
                    STAR_BASE_PX, STAR_BASE_PX, 0f);
            out.add(new RichNode.Offset(tx, ty, RichNode.Offset.OVERLAY_Z, List.of(
                    new RichNode.Scaled(scale, scale, List.of(
                            new RichNode.Rotated(angle, List.of(star)))))));
        }
        return new RichNode.Text("", Style.EMPTY, out);
    }

    private static ImageSpec starSpec(String name) {
        return new ImageSpec.Decoded("png",
                new BinarySource.Resource("emoji_deco:textures/mfm/" + name + ".png"));
    }

    /** SplitMix64 step. */
    private static long next(long x) {
        x += 0x9E3779B97F4A7C15L;
        x = (x ^ (x >>> 30)) * 0xBF58476D1CE4E5B9L;
        x = (x ^ (x >>> 27)) * 0x94D049BB133111EBL;
        return x ^ (x >>> 31);
    }

    private static double mix01(long x) {
        return (x >>> 11) * (1.0 / (1L << 53));
    }

    /** Reads a numeric MfmFn arg (stored as String, or {@link MfmFn#NO_VALUE} for bare flags). */
    private static float argFloat(MfmFn fn, String key, float def) {
        Object v = fn.args().get(key);
        if (v instanceof String s) {
            try { return Float.parseFloat(s); } catch (NumberFormatException ignored) { }
        }
        return def;
    }

    /** Accepts "#rgb"/"#rrggbb"/"rgb"/"rrggbb" (Misskey's fg.color omits the leading '#'). */
    private static TextColor parseHexColor(String raw) {
        TextColor direct = TextColor.parseColor(raw).result().orElse(null);
        if (direct != null) return direct;

        String hex = raw.startsWith("#") ? raw.substring(1) : raw;
        if (hex.length() == 3) {
            StringBuilder sb = new StringBuilder();
            for (char c : hex.toCharArray()) sb.append(c).append(c);
            hex = sb.toString();
        }
        return TextColor.parseColor("#" + hex).result().orElse(null);
    }
}
