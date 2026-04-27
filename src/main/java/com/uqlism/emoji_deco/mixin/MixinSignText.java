package com.uqlism.emoji_deco.mixin;

import com.uqlism.emoji_deco.Config;
import com.uqlism.emoji_deco.text.CompositeScaledSequence;
import com.uqlism.emoji_deco.text.RichTextParser;
import com.uqlism.emoji_deco.text.ScaledSequence;
import com.uqlism.emoji_deco.text.SizeRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.entity.SignText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Mixin(SignText.class)
public class MixinSignText {

    @Inject(method = "m_277130_", at = @At("HEAD"), cancellable = true, remap = false)
    private void runicink$parseRichText(boolean filtered,
            Function<Component, FormattedCharSequence> lineConverter,
            CallbackInfoReturnable<FormattedCharSequence[]> cir) {
        if (!Config.enableSigns) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.font == null) return;
        Font font = mc.font;
        SignText self = (SignText)(Object)this;

        FormattedCharSequence[] result = new FormattedCharSequence[4];
        for (int i = 0; i < 4; i++) {
            Component original = self.getMessage(i, filtered);
            String raw = original.getString();
            if (raw.isBlank()) {
                result[i] = FormattedCharSequence.EMPTY;
                continue;
            }
            Component parsed = RichTextParser.parse(raw);
            result[i] = buildScaledLine(font, parsed);
        }
        cir.setReturnValue(result);
    }

    private static FormattedCharSequence buildScaledLine(Font font, Component parsed) {
        // Case 1: entire line is a single size-decorated component
        float sizeScale = SizeRegistry.extractScale(parsed);
        if (sizeScale != 1.0f) {
            Component content = SizeRegistry.extractContent(parsed);
            List<FormattedCharSequence> lines = font.split(content, Integer.MAX_VALUE / 2);
            FormattedCharSequence fcs = lines.isEmpty() ? FormattedCharSequence.EMPTY : lines.get(0);
            return new ScaledSequence(fcs, sizeScale);
        }

        // Case 2: mixed siblings — check if any sibling carries a size scale
        List<Component> siblings = parsed.getSiblings();
        boolean anyScaled = false;
        for (Component sib : siblings) {
            if (SizeRegistry.extractScale(sib) != 1.0f) { anyScaled = true; break; }
        }
        if (!anyScaled || siblings.isEmpty()) {
            List<FormattedCharSequence> lines = font.split(parsed, Integer.MAX_VALUE / 2);
            return lines.isEmpty() ? FormattedCharSequence.EMPTY : lines.get(0);
        }

        // Build one segment per sibling, each with its own scale
        List<CompositeScaledSequence.Segment> segments = new ArrayList<>();
        for (Component sib : siblings) {
            float s = SizeRegistry.extractScale(sib);
            if (s != 1.0f) {
                Component content = SizeRegistry.extractContent(sib);
                List<FormattedCharSequence> lines = font.split(content, Integer.MAX_VALUE / 2);
                segments.add(new CompositeScaledSequence.Segment(
                        lines.isEmpty() ? FormattedCharSequence.EMPTY : lines.get(0), s));
            } else {
                List<FormattedCharSequence> lines = font.split(sib, Integer.MAX_VALUE / 2);
                segments.add(new CompositeScaledSequence.Segment(
                        lines.isEmpty() ? FormattedCharSequence.EMPTY : lines.get(0), 1.0f));
            }
        }
        return new CompositeScaledSequence(segments);
    }
}
