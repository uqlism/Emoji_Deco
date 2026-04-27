package com.uqlism.emoji_deco.text;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.contents.LiteralContents;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.util.List;

public class SizeRegistry {

    public static final String KEY = "emoji_deco:size";

    /**
     * Returns the scale factor if {@code c} represents a size-decorated component
     * (i.e. its effective root is a {@code emoji_deco:size} translate), or {@code 1.0f}.
     */
    public static float extractScale(Component c) {
        TranslatableContents tc = findTranslate(c);
        if (tc == null) return 1.0f;
        Object[] args = tc.getArgs();
        if (args.length < 1) return 1.0f;
        try {
            return Float.parseFloat(args[0].toString());
        } catch (NumberFormatException e) {
            return 1.0f;
        }
    }

    /**
     * Returns the inner content component from a size-decorated component.
     * If {@code c} is not a size-decorated component, returns {@code c} unchanged.
     */
    public static Component extractContent(Component c) {
        TranslatableContents tc = findTranslate(c);
        if (tc == null) return c;
        Object[] args = tc.getArgs();
        if (args.length < 2) return Component.empty();
        return args[1] instanceof Component comp ? comp : Component.literal(args[1].toString());
    }

    /**
     * Finds the {@code emoji_deco:size} TranslatableContents in {@code c}, handling two cases:
     * 1. {@code c} itself is the size translate.
     * 2. {@code c} is an empty literal wrapper with a single size-translate sibling
     *    (the typical output of {@link RichTextParser#parseInline} for a lone {@code #tag[...]} token).
     */
    private static TranslatableContents findTranslate(Component c) {
        if (c.getContents() instanceof TranslatableContents tc && KEY.equals(tc.getKey()))
            return tc;

        List<Component> siblings = c.getSiblings();
        if (siblings.size() == 1 && isTransparentContent(c.getContents())) {
            Component child = siblings.get(0);
            if (child.getContents() instanceof TranslatableContents tc && KEY.equals(tc.getKey()))
                return tc;
        }
        return null;
    }

    /**
     * Returns true for content that contributes no visible text of its own:
     * {@code ComponentContents.EMPTY} (from {@link Component#empty()}) or
     * {@code LiteralContents("")} (from {@link Component#literal(String)}).
     */
    private static boolean isTransparentContent(ComponentContents contents) {
        return contents == ComponentContents.EMPTY
            || (contents instanceof LiteralContents lc && lc.text().isEmpty());
    }
}
