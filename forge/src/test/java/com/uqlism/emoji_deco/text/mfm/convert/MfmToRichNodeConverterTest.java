package com.uqlism.emoji_deco.text.mfm.convert;

import com.uqlism.emoji_deco.MinecraftTestBase;
import com.uqlism.emoji_deco.text.hydrate.HydrateContext;
import com.uqlism.emoji_deco.text.ir.RichNode;
import com.uqlism.emoji_deco.text.mfm.Mfm;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end tests for {@code Mfm.parse(input) -> MfmToRichNodeConverter.convert(...) ->
 * RichNode.toComponent()}. Mirrors the style of {@code RichTextParserTest} /
 * {@code ParsingIntegrationTest} in the parent package.
 */
class MfmToRichNodeConverterTest extends MinecraftTestBase {

    private static Component component(String input) {
        return MfmToRichNodeConverter.convert(Mfm.parse(input), HydrateContext.EMPTY.track()).toComponent();
    }

    private static String str(String input) {
        return component(input).getString();
    }

    /** Checks whether any component in the tree has the given style flag set. */
    private static boolean hasStyle(Component comp, Predicate<Style> check) {
        if (check.test(comp.getStyle())) return true;
        return comp.getSiblings().stream().anyMatch(c -> hasStyle(c, check));
    }

    // ── plain text ────────────────────────────────────────────────────────────

    @Test void plainText() { assertEquals("hello world", str("hello world")); }

    // ── spaces after decorated nodes ──────────────────────────────────────────

    @Test void spaceAfterBold() { assertEquals("bold A", str("**bold** A")); }
    @Test void spaceAfterItalic() { assertEquals("italic B", str("*italic* B")); }
    @Test void spaceAfterStrike() { assertEquals("strike C", str("~~strike~~ C")); }
    @Test void spaceAfterFn() { assertEquals("big D", str("$[x2 big] D")); }
    @Test void spaceAroundFn() { assertEquals("a big D", str("a $[x2 big] D")); }

    // ── bold / italic / strike / small ───────────────────────────────────────

    @Test void bold() {
        assertEquals("abc", str("**abc**"));
        assertTrue(hasStyle(component("**abc**"), s -> Boolean.TRUE.equals(s.isBold())));
    }

    @Test void italic() {
        assertEquals("abc", str("*abc*"));
        assertTrue(hasStyle(component("*abc*"), s -> Boolean.TRUE.equals(s.isItalic())));
    }

    @Test void strike() {
        assertEquals("abc", str("~~abc~~"));
        assertTrue(hasStyle(component("~~abc~~"), s -> Boolean.TRUE.equals(s.isStrikethrough())));
    }

    @Test void small() {
        // Scaled has no toComponent() rendering of the scale itself, but content must survive.
        assertEquals("abc", str("<small>abc</small>"));
    }

    // ── url / link ────────────────────────────────────────────────────────────

    @Test void bareUrl() {
        assertEquals("https://example.com", str("https://example.com"));
        assertTrue(hasStyle(component("https://example.com"),
                s -> s.getClickEvent() != null && s.getClickEvent().getAction() == ClickEvent.Action.OPEN_URL));
    }

    @Test void link() {
        assertEquals("official instance", str("[official instance](https://example.com/@ai)"));
        assertTrue(hasStyle(component("[official instance](https://example.com/@ai)"),
                s -> s.getClickEvent() != null && s.getClickEvent().getAction() == ClickEvent.Action.OPEN_URL));
    }

    // ── mention / hashtag ─────────────────────────────────────────────────────

    @Test void hashtag() { assertEquals("#foo", str("#foo")); }

    // ── emoji ─────────────────────────────────────────────────────────────────

    @Test void unicodeEmoji() { assertEquals("👍", str("👍")); }

    @Test void unresolvedEmojiCodeFallsBackToLiteral() {
        // No shortcode named this is registered in the test environment, so it must fall back
        // to the ":name:" literal rather than disappearing.
        assertEquals(":totally_unregistered_emoji:", str(":totally_unregistered_emoji:"));
    }

    // ── $[fn ...] ─────────────────────────────────────────────────────────────

    @Test void x2Scale() {
        RichNode node = MfmToRichNodeConverter.convert(Mfm.parse("$[x2 abc]"), HydrateContext.EMPTY.track());
        assertEquals("abc", node.toComponent().getString());
        assertTrue(containsScaled(node, 2f, 2f), "Expected a Scaled(2,2,...) node in the tree");
    }

    @Test void rotateDefaultDeg() {
        RichNode node = MfmToRichNodeConverter.convert(Mfm.parse("$[rotate abc]"), HydrateContext.EMPTY.track());
        assertEquals("abc", node.toComponent().getString());
        assertTrue(containsRotated(node, 90f), "Expected default rotate of 90 degrees");
    }

    @Test void rotateExplicitDeg() {
        RichNode node = MfmToRichNodeConverter.convert(Mfm.parse("$[rotate.deg=45 abc]"), HydrateContext.EMPTY.track());
        assertTrue(containsRotated(node, 45f));
    }

    @Test void position() {
        RichNode node = MfmToRichNodeConverter.convert(
                Mfm.parse("$[position.x=-3,y=4 abc]"), HydrateContext.EMPTY.track());
        assertEquals("abc", node.toComponent().getString());
        assertTrue(node.toString().contains("Offset"), "Expected an Offset node");
    }

    @Test void flipDefaultsHorizontal() {
        RichNode node = MfmToRichNodeConverter.convert(Mfm.parse("$[flip abc]"), HydrateContext.EMPTY.track());
        assertTrue(containsScaled(node, -1f, 1f));
    }

    @Test void fgColor() {
        Component comp = component("$[fg.color=f00 abc]");
        assertEquals("abc", comp.getString());
        assertTrue(hasStyle(comp, s -> s.getColor() != null), "Expected a color to be applied");
    }

    @Test void rainbowDoesNotThrowAndAppliesColor() {
        assertDoesNotThrow(() -> {
            Component comp = component("$[rainbow abc]");
            assertEquals("abc", comp.getString());
            assertTrue(hasStyle(comp, s -> s.getColor() != null), "Expected rainbow to apply some color");
        });
    }

    @Test void sparkleKeepsContentAndDoesNotThrow() {
        assertDoesNotThrow(() -> {
            assertEquals("abc", str("$[sparkle abc]"));
            assertEquals("hello world", str("$[sparkle hello world]"));
            assertEquals("a b", str("$[sparkle $[x2 a] **b**]"));
            assertEquals("abc", component("$[sparkle abc]").getString());
        });
    }

    @Test void sparkleStarsAreOverlayOffsets() {
        RichNode node = MfmToRichNodeConverter.convert(Mfm.parse("$[sparkle abc]"), HydrateContext.EMPTY.track());
        assertTrue(allOffsets(node).stream().allMatch(RichNode.Offset::isOverlay));
    }

    // ── unsupported constructs: fallback must not drop content or throw ─────────

    @Test void tadaFallsBackToPlainChildren() {
        // Big syntax ***abc*** parses to Fn("tada", ...); v1 doesn't implement the tada effect.
        assertEquals("abc", str("***abc***"));
        assertEquals("abc", str("$[tada abc]"));
    }

    @Test void otherUnsupportedFnFallsBackToPlainChildren() {
        assertDoesNotThrow(() -> {
            assertEquals("a", str("$[shake a]"));
            assertEquals("a", str("$[jelly a]"));
            assertEquals("a", str("$[blur a]"));
        });
    }

    @Test void unknownFnNameDoesNotCrash() {
        assertDoesNotThrow(() -> str("$[totallyMadeUpFn abc]"));
    }

    @Test void mathFallsBackToRawFormula() {
        assertDoesNotThrow(() -> str("\\(x^2\\)"));
    }

    @Test void centerPassesThroughChildren() {
        assertEquals("abc", str("<center>abc</center>"));
    }

    @Test void quotePrependsMarker() {
        assertEquals("> abc", str("> abc"));
    }

    @Test void blockCodeRendersRawCode() {
        assertEquals("abc", str("```\nabc\n```"));
    }

    @Test void inlineCodeRendersRawCode() {
        assertEquals("abc", str("`abc`"));
    }

    @Test void plainStripsNestedDecoration() {
        assertEquals("**abc**", str("<plain>\n**abc**\n</plain>"));
    }

    // ── deeply nested / mixed input never throws ─────────────────────────────

    @Test void mixedInputDoesNotThrow() {
        assertDoesNotThrow(() -> str(
                "hello **bold** *italic* ~~strike~~ $[x2 $[rotate.deg=30 $[rainbow abc]]] "
                        + "[link](https://example.com) #tag :unregistered: 👍 $[tada big] $[shake x]"));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static List<RichNode.Offset> allOffsets(RichNode node) {
        List<RichNode.Offset> out = new java.util.ArrayList<>();
        if (node instanceof RichNode.Offset o) out.add(o);
        for (RichNode c : childrenOf(node)) out.addAll(allOffsets(c));
        return out;
    }

    private static boolean containsScaled(RichNode node, float sx, float sy) {
        if (node instanceof RichNode.Scaled s && s.scaleX() == sx && s.scaleY() == sy) return true;
        return childrenOf(node).stream().anyMatch(c -> containsScaled(c, sx, sy));
    }

    private static boolean containsRotated(RichNode node, float deg) {
        if (node instanceof RichNode.Rotated r && r.angle() == deg) return true;
        return childrenOf(node).stream().anyMatch(c -> containsRotated(c, deg));
    }

    private static java.util.List<RichNode> childrenOf(RichNode node) {
        return switch (node) {
            case RichNode.Text t -> t.children();
            case RichNode.Scaled s -> s.children();
            case RichNode.Rotated r -> r.children();
            case RichNode.Offset o -> o.children();
            case RichNode.Click c -> c.children();
            case RichNode.Hover h -> h.children();
            case RichNode.Insertion i -> i.children();
            case RichNode.Glowing g -> g.children();
            default -> java.util.List.of();
        };
    }
}
