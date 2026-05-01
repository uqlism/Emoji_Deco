package com.uqlism.emoji_deco.render.sequence;

import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSink;
import net.minecraft.util.FormattedCharSequence;

/**
 * FormattedCharSequence backed by a live Component (typically one whose
 * ComponentContents re-evaluates on every visit() call, such as
 * DynamicComponentContents). Re-calls Language.getVisualOrder() on every
 * accept() so the rendered result reflects the current game state each frame.
 */
public final class DynamicFormattedCharSequence implements FormattedCharSequence {

    private final Component liveComponent;

    public DynamicFormattedCharSequence(Component liveComponent) {
        this.liveComponent = liveComponent;
    }

    @Override
    public boolean accept(FormattedCharSink sink) {
        return Language.getInstance().getVisualOrder(liveComponent).accept(sink);
    }
}
