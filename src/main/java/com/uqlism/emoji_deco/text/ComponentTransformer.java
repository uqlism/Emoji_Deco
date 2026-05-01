package com.uqlism.emoji_deco.text;

import com.uqlism.emoji_deco.text.hydrate.HydrateContext;
import com.uqlism.emoji_deco.text.parse.RichTextParser;

import net.minecraft.network.chat.*;
import net.minecraft.network.chat.contents.LiteralContents;
import net.minecraft.network.chat.contents.TranslatableContents;

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
            String text = lc.text();
            HydrateContext.Tracked ctx = HydrateContext.EMPTY.track();
            RichTextParser.parseTracked(text, ctx);
            MutableComponent base;
            if (ctx.extractPattern().usesTime() || ctx.extractPattern().usesPlayerNames()) {
                base = MutableComponent.create(new DynamicRichContents(text, style));
            } else {
                base = RichTextParser.parseInline(text, style).toComponent().withStyle(style);
            }
            for (Component sibling : siblings) base.append(walk(sibling));
            return base;
        }

        if (contents instanceof TranslatableContents tc) {
            Object[] args = tc.getArgs();
            Object[] newArgs = new Object[args.length];
            for (int i = 0; i < args.length; i++) {
                newArgs[i] = (args[i] instanceof Component c) ? walk(c) : args[i];
            }
            MutableComponent base = MutableComponent
                    .create(new TranslatableContents(tc.getKey(), tc.getFallback(), newArgs))
                    .withStyle(style);
            for (Component sibling : siblings) base.append(walk(sibling));
            return base;
        }

        MutableComponent base = MutableComponent.create(contents).withStyle(style);
        for (Component sibling : siblings) base.append(walk(sibling));
        return base;
    }
}
