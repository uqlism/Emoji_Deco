package com.uqlism.emoji_deco.render.sequence;

import com.uqlism.emoji_deco.text.ComponentTransformer;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSink;
import net.minecraft.util.FormattedCharSequence;

/**
 * FormattedCharSequence that re-transforms the original chat component on every
 * accept() call so that time-dependent decorators (e.g. #rainbow) animate each frame.
 *
 * "original" is the pre-transformation component from the ClientChatReceivedEvent.
 * accept() calls ComponentTransformer.transform(original) to get a fresh component
 * with current-frame colors, then passes its visual-order text to the sink.
 * This does NOT go through DynamicComponentContents again, so there is no recursion.
 */
public final class DynamicFormattedCharSequence implements FormattedCharSequence {

    private final Component original;

    public DynamicFormattedCharSequence(Component original) {
        this.original = original;
    }

    @Override
    public boolean accept(FormattedCharSink sink) {
        Component fresh = ComponentTransformer.transform(original);
        return Language.getInstance().getVisualOrder(fresh).accept(sink);
    }
}
