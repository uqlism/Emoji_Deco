package com.uqlism.emoji_deco.text;

import com.uqlism.emoji_deco.render.sequence.ConcatSequence;
import com.uqlism.emoji_deco.render.sequence.DynamicFormattedCharSequence;
import com.uqlism.emoji_deco.text.hydrate.HydrateContext;
import com.uqlism.emoji_deco.text.ir.RichNode;
import com.uqlism.emoji_deco.text.parse.RichTextParser;
import net.minecraft.client.gui.Font;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.LiteralContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/**
 * Component ツリーを歩きながら RichTextParser + RichNode.toSequence() で変換する統一変換器。
 *
 * - LiteralContents → RichTextParser.parse() → RichNode.toSequence(inherited style)
 *   scale/glow 対応、且つ Component 由来の ClickEvent / HoverEvent を Style で保持
 * - TranslatableContents / その他 → Language によるバニラ解決（フォールバック）
 * - 動的コンテンツ（#rainbow など）は DynamicFormattedCharSequence でラップして毎フレーム再評価
 *
 * 使用箇所: MixinSignText, MixinChatComponent, DynamicFormattedCharSequence
 */
public final class ComponentSequenceConverter {

    private ComponentSequenceConverter() {}

    /**
     * Component ツリーを変換して FormattedCharSequence を返す。
     * 動的コンテンツが含まれる場合は DynamicFormattedCharSequence でラップする。
     */
    public static FormattedCharSequence toSequence(Font font, Component component) {
        if (isDynamic(component)) {
            return new DynamicFormattedCharSequence(component);
        }
        return computeNow(font, component);
    }

    /**
     * 動的評価なしで即座に変換する。DynamicFormattedCharSequence.accept() から毎フレーム呼ばれる。
     */
    public static FormattedCharSequence computeNow(Font font, Component component) {
        List<FormattedCharSequence> parts = new ArrayList<>();
        walkComponent(font, component, Style.EMPTY, parts);
        if (parts.isEmpty()) return FormattedCharSequence.EMPTY;
        if (parts.size() == 1) return parts.get(0);
        return new ConcatSequence(parts);
    }

    /**
     * LiteralContents に時刻・プレイヤー名などの動的パターンが含まれるか検査する。
     */
    public static boolean isDynamic(Component component) {
        ComponentContents contents = component.getContents();
        if (contents instanceof LiteralContents lc) {
            HydrateContext.Tracked ctx = HydrateContext.EMPTY.track();
            RichTextParser.parseTracked(lc.text(), ctx);
            var pattern = ctx.extractPattern();
            if (pattern.usesTime() || pattern.usesPlayerNames()) return true;
        } else if (contents instanceof TranslatableContents tc) {
            for (Object arg : tc.getArgs()) {
                if (arg instanceof Component ac && isDynamic(ac)) return true;
            }
        }
        for (Component sibling : component.getSiblings()) {
            if (isDynamic(sibling)) return true;
        }
        return false;
    }

    private static void walkComponent(Font font, Component c, Style inherited,
                                      List<FormattedCharSequence> out) {
        // Component の style を inherited に重ねる（ClickEvent/HoverEvent も引き継ぐ）
        Style effective = c.getStyle().applyTo(inherited);
        ComponentContents contents = c.getContents();

        if (contents instanceof LiteralContents lc) {
            if (!lc.text().isEmpty()) {
                FormattedCharSequence seq = RichNode.toSequence(font, RichTextParser.parse(lc.text()), effective);
                if (seq != FormattedCharSequence.EMPTY) out.add(seq);
            }
        } else {
            // TranslatableContents・その他: バニラ描画にフォールバック
            // （TranslatableContents の %s 解決は Language に委ね、Component args の mod 処理は省略）
            FormattedCharSequence vanilla = Language.getInstance().getVisualOrder(
                MutableComponent.create(contents).withStyle(effective));
            out.add(vanilla);
        }

        // siblings は Minecraft の visit() と同様に effective style を引き継ぐ
        for (Component sibling : c.getSiblings()) {
            walkComponent(font, sibling, effective, out);
        }
    }
}
