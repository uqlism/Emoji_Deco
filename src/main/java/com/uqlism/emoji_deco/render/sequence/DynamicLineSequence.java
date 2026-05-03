package com.uqlism.emoji_deco.render.sequence;

import com.uqlism.emoji_deco.text.ComponentConverter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;

import java.util.List;

/**
 * チャット1行分の動的 FormattedCharSequence。
 * MixinFont.drawInBatch が毎フレーム ComponentConverter.toLines() で解決して
 * drawInBatch(FCS) 経路に通すことで #rainbow 等がアニメーションし scale/glow も保持される。
 * accept(FormattedCharSink) は AffineSequence の変換を失うため、描画には必ず drawInBatch 経路を使う。
 */
public final class DynamicLineSequence implements FormattedCharSequence {

    private final Component original;
    private final int lineIndex;
    private final int width;

    public DynamicLineSequence(Component original, int lineIndex, int width) {
        this.original = original;
        this.lineIndex = lineIndex;
        this.width = width;
    }

    public Component original()  { return original; }
    public int       lineIndex() { return lineIndex; }
    public int       width()     { return width; }

    @Override
    public boolean accept(FormattedCharSink sink) {
        Font font = Minecraft.getInstance().font;
        if (font == null) return true;
        List<FormattedCharSequence> lines = ComponentConverter.toLines(font, original, width);
        if (lineIndex >= lines.size()) return true;
        return lines.get(lineIndex).accept(sink);
    }
}
