package com.uqlism.emoji_deco.text;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;

import java.util.Optional;

/**
 * ComponentContents that re-applies ComponentTransformer on every visit() call.
 * Used for chat messages that contain time-dependent rich text (e.g. #rainbow).
 *
 * ChatComponent.m_240465_ uses ComponentRenderUtils.wrapComponents() to convert
 * the message Component into FormattedCharSequence lines (one per line after word-
 * wrapping). MixinChatComponent intercepts that call and returns a
 * DynamicFormattedCharSequence instead, which re-evaluates this visit() on every
 * render frame so that time-dependent colors animate correctly.
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
