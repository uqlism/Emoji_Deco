package com.uqlism.emoji_deco.render.sequence;

import com.uqlism.emoji_deco.text.ComponentTransformer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;

import java.util.List;

/**
 * チャット1行分の動的 FormattedCharSequence。
 * accept() が呼ばれるたびに ComponentTransformer.transform() + font.split() で
 * 再評価することで #rainbow 等の時刻依存デコレータがアニメーションする。
 *
 * lineIndex: この FCS が担当する行インデックス（0始まり）
 * width:     ワードラップ幅（ピクセル）
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

    @Override
    public boolean accept(FormattedCharSink sink) {
        Font font = Minecraft.getInstance().font;
        if (font == null) return true;
        Component transformed = ComponentTransformer.transform(original);
        List<FormattedCharSequence> lines = font.split(transformed, width);
        if (lineIndex >= lines.size()) return true;
        return lines.get(lineIndex).accept(sink);
    }
}
