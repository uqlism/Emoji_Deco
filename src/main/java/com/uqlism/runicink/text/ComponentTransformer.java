package com.uqlism.runicink.text;

import net.minecraft.network.chat.*;
import net.minecraft.network.chat.contents.LiteralContents;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.util.List;

public class ComponentTransformer {

    public static Component transform(Component root) {
        return walk(root);
    }

    private static MutableComponent walk(Component component) {
        ComponentContents contents = component.getContents();
        Style style = component.getStyle();
        List<Component> siblings = component.getSiblings();

        if (contents instanceof TranslatableContents tc && tc.getKey().equals("runicink:sprite")) {
            MutableComponent base = resolveRunicComponent(tc);
            for (Component sibling : siblings) base.append(walk(sibling));
            return base;
        }

        if (contents instanceof TranslatableContents tc && tc.getKey().equals("runicink:head")) {
            MutableComponent base = resolveHeadComponent(tc);
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

        if (contents instanceof LiteralContents lc) {
            MutableComponent base = RichTextParser.parseInline(lc.text(), style);
            for (Component sibling : siblings) base.append(walk(sibling));
            return base;
        }

        MutableComponent base = MutableComponent.create(contents).withStyle(style);
        for (Component sibling : siblings) base.append(walk(sibling));
        return base;
    }

    private static MutableComponent resolveRunicComponent(TranslatableContents tc) {
        Object[] args = tc.getArgs();
        if (args.length < 2) return Component.empty();
        
        String atlasName;
        if (args[0] instanceof String s) {
            atlasName = s;
        } else if (args[0] instanceof Component c) {
            atlasName = c.getString();
        } else {
            atlasName = args[0].toString();
        }

        String textureName;
        if (args[1] instanceof String s) {
            textureName = s;
        } else if (args[1] instanceof Component c) {
            textureName = c.getString();
        } else {
            textureName = args[1].toString();
        }

        return SpriteRegistry.createComponent(atlasName,textureName).copy();
    }

    private static MutableComponent resolveHeadComponent(TranslatableContents tc) {
    Object[] args = tc.getArgs();
    if (args.length == 0) return Component.empty();

    String username = args[0] instanceof String s ? s
                    : args[0] instanceof Component c ? c.getString()
                    : args[0].toString();

    return SpriteRegistry.createHeadComponent(username).copy();
}
}
