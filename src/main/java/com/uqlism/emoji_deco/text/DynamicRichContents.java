package com.uqlism.emoji_deco.text;

import com.uqlism.emoji_deco.text.parse.RichTextParser;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;

import java.util.Optional;

/**
 * ComponentContents that re-evaluates rich text on every visit() call.
 * Used for text containing time-dependent decorators (e.g. animated colors),
 * where a static baked Component would freeze at the moment of creation.
 */
public record DynamicRichContents(String raw, Style baseStyle) implements ComponentContents {

    @Override
    public <T> Optional<T> visit(FormattedText.StyledContentConsumer<T> consumer, Style style) {
        return RichTextParser.parse(raw).toComponent().withStyle(baseStyle).visit(consumer, style);
    }

    @Override
    public <T> Optional<T> visit(FormattedText.ContentConsumer<T> consumer) {
        return RichTextParser.parse(raw).toComponent().visit(consumer);
    }

    @Override
    public String toString() {
        return "dynamic(" + raw + ")";
    }
}
