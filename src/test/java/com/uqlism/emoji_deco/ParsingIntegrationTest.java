package com.uqlism.emoji_deco;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.uqlism.emoji_deco.text.DecoratorManager;
import com.uqlism.emoji_deco.text.RichNode;
import com.uqlism.emoji_deco.text.RichTextParser;
import com.uqlism.emoji_deco.text.ShortcodeManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.LiteralContents;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the full parsing pipeline with registered decorators/shortcodes.
 * Decorator and shortcode registries are populated via reflection to avoid requiring
 * a live resource-pack reload.
 */
class ParsingIntegrationTest extends MinecraftTestBase {

    // ── test data setup ───────────────────────────────────────────────────────

    @BeforeAll
    static void registerTestData() {
        // Decorators
        putDecorator("bold",
                """
                {"text":"","bold":true,"extra":[{"type":"emoji_deco:slot"}]}
                """);
        putDecorator("italic",
                """
                {"text":"","italic":true,"extra":[{"type":"emoji_deco:slot"}]}
                """);
        putDecorator("color",
                """
                {"text":"","color":{"type":"emoji_deco:arg","index":0,"default":"white"},"extra":[{"type":"emoji_deco:slot"}]}
                """);
        putDecorator("size",
                """
                {"type":"emoji_deco:scale","x":{"type":"emoji_deco:arg","index":0,"default":1.0,"value_type":"float"},"y":{"type":"emoji_deco:arg","index":0,"default":1.0,"value_type":"float"},"contents":{"type":"emoji_deco:slot"}}
                """);

        // Shortcodes
        putShortcode("heart",
                new RichNode.Text("❤", Style.EMPTY.withColor(0xFF5555), java.util.List.of()));
        putShortcode("star",
                new RichNode.Text("★", Style.EMPTY, java.util.List.of()));
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private static String str(String input) {
        return RichTextParser.parse(input).toComponent().getString();
    }

    /** Checks whether any component in the tree has the given style flag set. */
    private static boolean hasStyle(Component comp, java.util.function.Predicate<Style> check) {
        if (check.test(comp.getStyle())) return true;
        return comp.getSiblings().stream().anyMatch(c -> hasStyle(c, check));
    }

    // ── decorator: content ────────────────────────────────────────────────────

    @Test void boldContent()   { assertEquals("hello", str("#bold[hello]"));   }
    @Test void italicContent() { assertEquals("hello", str("#italic[hello]")); }
    @Test void colorContent()  { assertEquals("hello", str("#color.red[hello]")); }

    // ── decorator: style applied ──────────────────────────────────────────────

    @Test
    void boldStyleApplied() {
        Component comp = RichTextParser.parse("#bold[hello]").toComponent();
        assertTrue(hasStyle(comp, s -> Boolean.TRUE.equals(s.isBold())),
                "Expected bold=true somewhere in component tree");
    }

    @Test
    void italicStyleApplied() {
        Component comp = RichTextParser.parse("#italic[hello]").toComponent();
        assertTrue(hasStyle(comp, s -> Boolean.TRUE.equals(s.isItalic())));
    }

    @Test
    void colorArgApplied() {
        Component comp = RichTextParser.parse("#color.red[hello]").toComponent();
        assertTrue(hasStyle(comp, s -> s.getColor() != null),
                "Expected a color somewhere in component tree");
    }

    // ── decorator: surrounding text ───────────────────────────────────────────

    @Test
    void decoratorWithSurroundingText() {
        assertEquals("hello world end", str("hello #bold[world] end"));
    }

    @Test
    void multipleDecoratorsInLine() {
        assertEquals("ab", str("#bold[a]#italic[b]"));
    }

    // ── nested decorators ─────────────────────────────────────────────────────

    @Test
    void nestedDecorators() {
        assertEquals("hello", str("#bold[#italic[hello]]"));
    }

    @Test
    void nestedDecoratorsBothStylesApplied() {
        Component comp = RichTextParser.parse("#bold[#italic[hello]]").toComponent();
        assertTrue(hasStyle(comp, s -> Boolean.TRUE.equals(s.isBold())));
        assertTrue(hasStyle(comp, s -> Boolean.TRUE.equals(s.isItalic())));
    }

    // ── shortcode ─────────────────────────────────────────────────────────────

    @Test void shortcodeResolved()       { assertEquals("❤",        str(":heart:"));        }
    @Test void shortcodeInText()         { assertEquals("I love ❤",  str("I love :heart:")); }
    @Test void multipleShortcodes()      { assertEquals("❤★",        str(":heart::star:"));  }
    @Test void shortcodeBetweenText()    { assertEquals("a❤b",       str("a:heart:b"));      }

    // ── shortcode with alias lookup (arg) ─────────────────────────────────────

    @Test
    void unknownShortcodePassthrough() {
        assertEquals(":nope:", str(":nope:"));
    }

    // ── decorator + shortcode mixed ───────────────────────────────────────────

    @Test
    void decoratorContainingShortcode() {
        assertEquals("❤", str("#bold[:heart:]"));
    }

    @Test
    void shortcodeInsideDecoratorHasParentStyle() {
        Component comp = RichTextParser.parse("#bold[:heart:]").toComponent();
        assertTrue(hasStyle(comp, s -> Boolean.TRUE.equals(s.isBold())));
    }

    // ── decorator args ────────────────────────────────────────────────────────

    @Test
    void decoratorArgDefaultUsedWhenAbsent() {
        // #color[text] without arg → default "white" is used, text passes through
        assertEquals("hello", str("#color[hello]"));
    }

    @Test
    void sizeArgFloat() {
        // #size.2.5[text] → scale=2.5; content passes through in toComponent()
        assertEquals("hello", str("#size.2.5[hello]"));
    }

    @Test
    void sizeNodeType() {
        // Verify the RichNode tree contains a Scaled node
        RichNode result = RichTextParser.parse("#size.2[hello]");
        assertTrue(containsNodeType(result, RichNode.Scaled.class),
                "Expected a Scaled node in the IR tree");
    }

    // ── registry helpers ──────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private static void putDecorator(String name, String displayJson) {
        try {
            JsonObject json = JsonParser.parseString(
                    "{\"display\":" + displayJson.strip() + "}").getAsJsonObject();
            Field f = DecoratorManager.class.getDeclaredField("REGISTRY");
            f.setAccessible(true);
            ((Map<String, JsonObject>) f.get(null)).put(name, json);
        } catch (Exception e) {
            throw new RuntimeException("Failed to register test decorator: " + name, e);
        }
    }

    @SuppressWarnings("unchecked")
    private static void putShortcode(String name, RichNode node) {
        try {
            Field f = ShortcodeManager.class.getDeclaredField("REGISTRY");
            f.setAccessible(true);
            ((Map<String, RichNode>) f.get(null)).put(name, node);
        } catch (Exception e) {
            throw new RuntimeException("Failed to register test shortcode: " + name, e);
        }
    }

    private static boolean containsNodeType(RichNode node, Class<? extends RichNode> type) {
        if (type.isInstance(node)) return true;
        if (node instanceof RichNode.Text t)
            return t.children().stream().anyMatch(c -> containsNodeType(c, type));
        if (node instanceof RichNode.Glowing g)
            return g.children().stream().anyMatch(c -> containsNodeType(c, type));
        if (node instanceof RichNode.Scaled s)
            return s.children().stream().anyMatch(c -> containsNodeType(c, type));
        if (node instanceof RichNode.Offset o)
            return o.children().stream().anyMatch(c -> containsNodeType(c, type));
        if (node instanceof RichNode.Rotated r)
            return r.children().stream().anyMatch(c -> containsNodeType(c, type));
        return false;
    }
}
