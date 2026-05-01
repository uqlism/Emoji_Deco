package com.uqlism.emoji_deco.text;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;

import java.util.Optional;

/**
 * ComponentContents that re-applies ComponentTransformer on every visit() call.
 * Used for chat messages that contain time-dependent rich text (e.g. #rainbow),
 * where the chat system converts Component → FormattedCharSequence only once and
 * DynamicRichContents.visit() would never be called again after that point.
 */
public record DynamicComponentContents(Component original) implements ComponentContents {

    @Override
    public <T> Optional<T> visit(FormattedText.StyledContentConsumer<T> consumer, Style style) {
        return ComponentTransformer.transform(original).visit(consumer, style);
    }

    @Override
    public <T> Optional<T> visit(FormattedText.ContentConsumer<T> consumer) {
        return ComponentTransformer.transform(original).visit(consumer);
    }

    @Override
    public String toString() { return "dynamic_component(" + original + ")"; }
}
