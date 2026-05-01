package com.uqlism.emoji_deco.render.sequence;

import com.uqlism.emoji_deco.text.parse.RichTextParser;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;

/**
 * FormattedCharSequence that re-evaluates rich-text on every accept() call.
 * Stored in chat's GuiMessage.Line instead of a static sequence so that
 * time-dependent decorators (e.g. #rainbow) animate each render frame.
 */
public final class DynamicFormattedCharSequence implements FormattedCharSequence {

    private final String raw;
    private final Style  baseStyle;

    public DynamicFormattedCharSequence(String raw, Style baseStyle) {
        this.raw       = raw;
        this.baseStyle = baseStyle;
    }

    @Override
    public boolean accept(FormattedCharSink sink) {
        Component fresh = RichTextParser.parse(raw).toComponent().withStyle(baseStyle);
        return Language.getInstance().getVisualOrder(fresh).accept(sink);
    }
}
