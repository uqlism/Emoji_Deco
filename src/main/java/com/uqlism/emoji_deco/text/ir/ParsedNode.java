package com.uqlism.emoji_deco.text.ir;

import org.jetbrains.annotations.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Dehydrated display node — produced at load time from JSON (pure parse, no runtime context).
 * Converted to RichNode at use time via NodeHydrator.hydrate().
 *
 * Primitive values that can come from args are represented as StringVal / FloatVal / BoolVal
 * expressions. Structural positions that can be filled by args use ArgAsText.
 */
public sealed interface ParsedNode permits
        ParsedNode.Text,
        ParsedNode.Many,
        ParsedNode.Image,
        ParsedNode.Glow,
        ParsedNode.Offset,
        ParsedNode.Scale,
        ParsedNode.Rotate,
        ParsedNode.SlotRef,
        ParsedNode.ArgAsText,
        ParsedNode.ApplyShortcode,
        ParsedNode.ApplyDecorator {

    record Text(String literal, ParsedStyle style, List<ParsedNode> children) implements ParsedNode {}

    /** Array of nodes (from a JSON array). */
    record Many(List<ParsedNode> items) implements ParsedNode {}

    /** Inline image glyph. w/h/advance are always literals; imageSpec may contain expressions. */
    record Image(ParsedImageSpec imageSpec, @Nullable int[] crop, int w, int h, float advance)
            implements ParsedNode {
        @Override public boolean equals(Object o) {
            if (!(o instanceof Image i)) return false;
            return Objects.equals(imageSpec, i.imageSpec) && Arrays.equals(crop, i.crop)
                    && w == i.w && h == i.h && Float.compare(advance, i.advance) == 0;
        }
        @Override public int hashCode() {
            return Objects.hash(imageSpec, Arrays.hashCode(crop), w, h, advance);
        }
    }

    record Glow(BoolVal glow, ParsedNode contents)                          implements ParsedNode {}
    record Offset(FloatVal x, FloatVal y, FloatVal z, ParsedNode contents)  implements ParsedNode {}
    record Scale(FloatVal x, FloatVal y, ParsedNode contents)               implements ParsedNode {}
    record Rotate(FloatVal angle, ParsedNode contents)                      implements ParsedNode {}

    /** Replaced by the slot RichNode from HydrateContext at hydrate time. */
    record SlotRef() implements ParsedNode {}

    /** An arg reference in a structural position — hydrates to RichNode.Text(argValue). */
    record ArgAsText(int index, String defaultVal) implements ParsedNode {}

    record ApplyShortcode(String shortcode, List<StringVal> callArgs) implements ParsedNode {}
    record ApplyDecorator(String decorator, ParsedNode slot, List<StringVal> callArgs) implements ParsedNode {}

    static ParsedNode empty() { return new Many(List.of()); }

    // ── Primitive expression types ─────────────────────────────────────────────

    sealed interface StringVal permits StringVal.Literal, StringVal.Arg, StringVal.Join, StringVal.PlayerNames {
        record Literal(String value)                               implements StringVal {}
        record Arg(int index, String defaultVal)                   implements StringVal {}
        record Join(String separator, List<StringVal> parts)       implements StringVal {}
        record PlayerNames()                                       implements StringVal {}
    }

    sealed interface FloatVal permits FloatVal.Literal, FloatVal.Arg, FloatVal.Time {
        record Literal(float value)                                    implements FloatVal {}
        record Arg(int index, float defaultVal)                        implements FloatVal {}
        record Time(String timeType, float scale, float offset)        implements FloatVal {}
    }

    sealed interface BoolVal permits BoolVal.Literal, BoolVal.Arg {
        record Literal(boolean value)          implements BoolVal {}
        record Arg(int index, boolean defaultVal) implements BoolVal {}
    }

    // ── Style (all fields may be expressions) ─────────────────────────────────

    record ParsedStyle(
            @Nullable StringVal color,
            @Nullable BoolVal   bold,
            @Nullable BoolVal   italic,
            @Nullable BoolVal   strikethrough,
            @Nullable BoolVal   underlined,
            @Nullable BoolVal   obfuscated,
            @Nullable String    font) {
        public static final ParsedStyle EMPTY = new ParsedStyle(null, null, null, null, null, null, null);
        public boolean isEmpty() {
            return color == null && bold == null && italic == null && strikethrough == null
                    && underlined == null && obfuscated == null && font == null;
        }
    }

    // ── Image spec ─────────────────────────────────────────────────────────────

    sealed interface ParsedImageSpec permits ParsedImageSpec.Decoded, ParsedImageSpec.Atlas, ParsedImageSpec.Skin {
        record Decoded(@Nullable StringVal format, ParsedBinarySource source) implements ParsedImageSpec {}
        record Atlas(StringVal atlas, StringVal sprite)                       implements ParsedImageSpec {}
        record Skin(StringVal player)                                         implements ParsedImageSpec {}
    }

    sealed interface ParsedBinarySource permits ParsedBinarySource.Url, ParsedBinarySource.Resource {
        record Url(StringVal url, boolean diskCache, int ttlSeconds) implements ParsedBinarySource {}
        record Resource(StringVal path)                              implements ParsedBinarySource {}
    }
}
