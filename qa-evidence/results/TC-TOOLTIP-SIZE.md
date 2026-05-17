---
test: TC-TOOLTIP-SIZE
feature: ホバーツールチップ #size
result: FAIL
date: 2026-05-18
commit: 81b2e92
screenshot: screenshots/TC-TOOLTIP-SIZE.png
location: tooltip
content: size
---

`#size.2[BIG GEM]` をカスタム名に持つアイテムのツールチップに "BIG GEM" が通常サイズで表示される。

**原因**: ツールチップは `GuiGraphics.renderTooltip()` → `Language.getVisualOrder()` → `font.drawInBatch(FormattedCharSequence)` の経路を通るため、`MixinGuiGraphics.runicink$drawStringComponent` をバイパスする。`DynamicFormattedCharSequence` でラップされないため `computeNow()` → `toSequence()` が呼ばれず `Scaled` ノードが無効。

修正対象: `renderTooltip` の経路に `DynamicFormattedCharSequence` を通す仕組みを追加。
