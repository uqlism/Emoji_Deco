package com.uqlism.emoji_deco.text;

import net.minecraft.network.chat.*;
import net.minecraft.network.chat.contents.LiteralContents;

import java.util.List;

/**
 * Walks an existing Minecraft Component tree and applies RichTextParser to every
 * LiteralContents leaf node. Used for incoming chat messages and GUI components
 * that arrive as pre-structured Components rather than raw strings.
 */
public class ComponentTransformer {

    public static Component transform(Component root) {
        return walk(root);
    }

    private static MutableComponent walk(Component component) {
        ComponentContents contents = component.getContents();
        Style style = component.getStyle();
        List<Component> siblings = component.getSiblings();

        if (contents instanceof LiteralContents lc) {
            MutableComponent base = RichTextParser.parseInline(lc.text(), style)
                    .toComponent().withStyle(style);
            for (Component sibling : siblings) base.append(walk(sibling));
            return base;
        }

        MutableComponent base = MutableComponent.create(contents).withStyle(style);
        for (Component sibling : siblings) base.append(walk(sibling));
        return base;
    }
}
