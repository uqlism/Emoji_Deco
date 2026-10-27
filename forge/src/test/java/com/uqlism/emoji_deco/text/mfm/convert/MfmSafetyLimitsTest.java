package com.uqlism.emoji_deco.text.mfm.convert;

import com.uqlism.emoji_deco.MinecraftTestBase;
import com.uqlism.emoji_deco.text.hydrate.HydrateContext;
import com.uqlism.emoji_deco.text.ir.RichNode;
import com.uqlism.emoji_deco.text.mfm.Mfm;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Scale/position clamps, cumulative scale cap and link styling of the MFM converter. */
class MfmSafetyLimitsTest extends MinecraftTestBase {

    private static RichNode node(String in) {
        return MfmToRichNodeConverter.convert(Mfm.parse(in), HydrateContext.EMPTY.track());
    }

    private static List<RichNode> children(RichNode n) {
        return switch (n) {
            case RichNode.Text t -> t.children();
            case RichNode.Scaled s -> s.children();
            case RichNode.Rotated r -> r.children();
            case RichNode.Offset o -> o.children();
            case RichNode.Click c -> c.children();
            default -> List.of();
        };
    }

    private static void collect(RichNode n, List<RichNode.Scaled> out) {
        if (n instanceof RichNode.Scaled s) out.add(s);
        for (RichNode c : children(n)) collect(c, out);
    }

    private static List<RichNode.Scaled> scaled(String in) {
        List<RichNode.Scaled> out = new ArrayList<>();
        collect(node(in), out);
        return out;
    }

    private static boolean anyStyle(Component c, Predicate<Style> p) {
        return p.test(c.getStyle()) || c.getSiblings().stream().anyMatch(s -> anyStyle(s, p));
    }

    @Test void scaleArgClampedToFive() {
        RichNode.Scaled s = scaled("$[scale.x=999,y=-999 a]").get(0);
        assertEquals(5f, s.scaleX());
        assertEquals(-5f, s.scaleY());
    }

    @Test void nonFiniteArgsFallBackToDefault() {
        assertEquals(1f, scaled("$[scale.x=NaN,y=Infinity a]").get(0).scaleX());
        assertEquals(1f, scaled("$[scale.x=NaN,y=Infinity a]").get(0).scaleY());
    }

    @Test void nestedX2IsCappedCumulatively() {
        float product = 1f;
        for (RichNode.Scaled s : scaled("$[x2 $[x2 $[x2 $[x2 a]]]]"))
            product *= Math.max(Math.abs(s.scaleX()), Math.abs(s.scaleY()));
        assertTrue(product <= MfmToRichNodeConverter.MAX_TOTAL_SCALE + 1e-4f, "product=" + product);
        assertEquals(8f, product, 1e-4f);
    }

    @Test void singleX4IsUnchanged() {
        assertEquals(4f, scaled("$[x4 a]").get(0).scaleX());
    }

    @Test void siblingScalesDoNotAccumulate() {
        List<RichNode.Scaled> l = scaled("$[x4 a] $[x4 b]");
        assertEquals(2, l.size());
        assertEquals(4f, l.get(1).scaleX());
    }

    @Test void clampTotalScalePure() {
        float[] f = MfmToRichNodeConverter.clampTotalScale(5f, 5f, 4f);
        assertEquals(2f, f[0], 1e-5f);
        float[] g = MfmToRichNodeConverter.clampTotalScale(-2f, 1f, 1f);
        assertEquals(-2f, g[0]);
    }

    @Test void positionClamped() {
        RichNode.Offset o = (RichNode.Offset) children(node("$[position.x=99999,y=-99999 a]")).get(0);
        assertEquals(64f, o.x());
        assertEquals(-64f, o.y());
    }

    @Test void linksAreUnderlinedAndColored() {
        for (String in : new String[]{"https://example.com", "[label](https://example.com)", "<https://example.com>"}) {
            assertTrue(anyStyle(node(in).toComponent(), s -> s.isUnderlined() && s.getColor() != null), in);
        }
    }

    @Test void linkLabelKeepsDecoration() {
        assertTrue(anyEffective(node("[**bold**](https://example.com)").toComponent(), Style.EMPTY,
                s -> s.isBold() && s.isUnderlined()));
    }

    /** Checks the effective style (child delta applied over the inherited one), as rendering does. */
    private static boolean anyEffective(Component c, Style inherited, Predicate<Style> p) {
        Style eff = c.getStyle().applyTo(inherited);
        return p.test(eff) || c.getSiblings().stream().anyMatch(s -> anyEffective(s, eff, p));
    }
}
