package com.uqlism.emoji_deco.render.sequence;

import com.google.gson.JsonElement;
import com.uqlism.emoji_deco.text.hydrate.HydrateContext;
import com.uqlism.emoji_deco.text.hydrate.Hydrators;
import com.uqlism.emoji_deco.text.ir.RichNode;
import com.uqlism.emoji_deco.text.parse.RichTextParser;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSink;
import net.minecraft.util.FormattedCharSequence;

/**
 * FormattedCharSequence that re-hydrates on every accept() call.
 * Stored in chat's GuiMessage.Line instead of a static sequence so that
 * time-dependent decorators (e.g. #rainbow) animate each render frame.
 *
 * The raw string is parsed to JsonElement once at construction time (the result
 * is stable via RichTextParser.PARSE_CACHE). Only the hydration step is repeated
 * per frame to pick up the current game time / color.
 */
public final class DynamicFormattedCharSequence implements FormattedCharSequence {

    private final JsonElement json;
    private final Style       baseStyle;

    public DynamicFormattedCharSequence(String raw, Style baseStyle) {
        this.json      = RichTextParser.parseToJson(raw);
        this.baseStyle = baseStyle;
    }

    @Override
    public boolean accept(FormattedCharSink sink) {
        RichNode node  = Hydrators.CACHED_NODE.hydrate(json, HydrateContext.EMPTY.track());
        Component fresh = (node != null ? node : RichNode.empty()).toComponent().withStyle(baseStyle);
        return Language.getInstance().getVisualOrder(fresh).accept(sink);
    }
}
