package com.uqlism.emoji_deco.text;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Converts the emoji_deco JSON component format into Minecraft Component trees.
 *
 * Supported node types:
 *   String literal          → LiteralComponent
 *   Array                   → empty parent with each element appended
 *   {"emoji_deco:slot":{}}  → injected slot content (decorators only)
 *   {"emoji_deco:size":{size,contents}}  → size-scaled component
 *   {"emoji_deco:glow":{contents}}       → glow-wrapped component
 *   {"emoji_deco:sprite":{atlas,sprite}} → sprite glyph
 *   {"emoji_deco:head":{username}}       → player-head glyph pair
 *   Standard MC fields (text/color/bold/italic/strikethrough/underlined/obfuscated/font/extra)
 */
public final class EmojiDecoComponentParser {

    private EmojiDecoComponentParser() {}

    /**
     * @param el   JSON element to parse
     * @param slot component injected at emoji_deco:slot positions; null for shortcodes
     */
    public static Component parse(JsonElement el, @Nullable Component slot) {
        if (el == null || el.isJsonNull()) return Component.empty();
        if (el.isJsonPrimitive())          return Component.literal(el.getAsString());
        if (el.isJsonArray()) {
            MutableComponent result = Component.empty();
            for (JsonElement child : el.getAsJsonArray()) result.append(parse(child, slot));
            return result;
        }
        if (el.isJsonObject()) return parseObject(el.getAsJsonObject(), slot);
        return Component.empty();
    }

    private static Component parseObject(JsonObject obj, @Nullable Component slot) {
        if (obj.has("emoji_deco:slot")) {
            return slot != null ? slot : Component.empty();
        }
        if (obj.has("emoji_deco:size")) {
            JsonObject spec = obj.getAsJsonObject("emoji_deco:size");
            float size = spec.has("size") ? spec.get("size").getAsFloat() : 1.0f;
            Component contents = spec.has("contents") ? parse(spec.get("contents"), slot) : Component.empty();
            return MutableComponent.create(
                    new TranslatableContents(SizeRegistry.SIZE_KEY, null,
                            new Object[]{String.valueOf(size), contents}));
        }
        if (obj.has("emoji_deco:glow")) {
            JsonObject spec = obj.getAsJsonObject("emoji_deco:glow");
            Component contents = spec.has("contents") ? parse(spec.get("contents"), slot) : Component.empty();
            return MutableComponent.create(
                    new TranslatableContents(SizeRegistry.GLOW_KEY, null, new Object[]{contents}));
        }
        if (obj.has("emoji_deco:sprite")) {
            JsonObject spec = obj.getAsJsonObject("emoji_deco:sprite");
            String atlas  = spec.has("atlas")  ? spec.get("atlas").getAsString()  : "";
            String sprite = spec.has("sprite") ? spec.get("sprite").getAsString() : "";
            return SpriteRegistry.createComponent(atlas, sprite);
        }
        if (obj.has("emoji_deco:head")) {
            JsonObject spec = obj.getAsJsonObject("emoji_deco:head");
            String username = spec.has("username") ? spec.get("username").getAsString() : "";
            return SpriteRegistry.createHeadComponent(username);
        }
        return parseStandard(obj, slot);
    }

    private static Component parseStandard(JsonObject obj, @Nullable Component slot) {
        MutableComponent comp = obj.has("text")
                ? Component.literal(obj.get("text").getAsString())
                : Component.empty();

        Style style = Style.EMPTY;
        if (obj.has("color")) {
            TextColor color = TextColor.parseColor(obj.get("color").getAsString());
            if (color != null) style = style.withColor(color);
        }
        if (obj.has("bold"))          style = style.withBold(obj.get("bold").getAsBoolean());
        if (obj.has("italic"))        style = style.withItalic(obj.get("italic").getAsBoolean());
        if (obj.has("strikethrough")) style = style.withStrikethrough(obj.get("strikethrough").getAsBoolean());
        if (obj.has("underlined"))    style = style.withUnderlined(obj.get("underlined").getAsBoolean());
        if (obj.has("obfuscated"))    style = style.withObfuscated(obj.get("obfuscated").getAsBoolean());
        if (obj.has("font")) {
            ResourceLocation fontLoc = ResourceLocation.tryParse(obj.get("font").getAsString());
            if (fontLoc != null) style = style.withFont(fontLoc);
        }
        if (!style.isEmpty()) comp = comp.withStyle(style);

        if (obj.has("extra") && obj.get("extra").isJsonArray()) {
            for (JsonElement child : obj.getAsJsonArray("extra")) comp.append(parse(child, slot));
        }
        return comp;
    }
}
