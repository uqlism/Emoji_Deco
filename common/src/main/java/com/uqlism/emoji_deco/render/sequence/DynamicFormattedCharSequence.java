package com.uqlism.emoji_deco.render.sequence;

import com.uqlism.emoji_deco.text.ComponentConverter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSink;
import net.minecraft.util.FormattedCharSequence;

/**
 * FormattedCharSequence that re-transforms the original chat component on every
 * accept() call so that time-dependent decorators (e.g. #rainbow) animate each frame.
 *
 * "original" is the pre-transformation component from the ClientChatReceivedEvent.
 * accept() calls ComponentConverter.computeNow(original) to get a fresh component
 * with current-frame colors, then passes its visual-order text to the sink.
 * This does NOT go through DynamicComponentContents again, so there is no recursion.
 */
public final class DynamicFormattedCharSequence implements FormattedCharSequence {

    private final Component original;

    public DynamicFormattedCharSequence(Component original) {
        this.original = original;
    }

    public Component original() { return original; }

    @Override
    public boolean accept(FormattedCharSink sink) {
        Font font = Minecraft.getInstance().font;
        if (font == null) return true;
        return ComponentConverter.computeNow(font, original).accept(sink);
    }
}
