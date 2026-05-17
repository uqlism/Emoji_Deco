---
test: TC-TOOLTIP-SIZE
feature: ホバーツールチップ #size
result: PASS
date: 2026-05-18
commit: 783e292
screenshot: screenshots/TC-TOOLTIP-SIZE.png
location: tooltip
content: size
---

`#size.2[BIG GEM]` がホバーツールチップで2倍サイズで表示されている。

`MixinGuiGraphics.runicink$renderTooltipComponents` の追加により `renderTooltip(Font, List<Component>, Optional<TooltipComponent>, int, int)` の経路で `DynamicFormattedCharSequence` が有効化された。これにより `Sized` ノードが `ScaledSequence` に変換され、`MixinFont` が正しくスケールを適用する。

通常ツールチップ（`/give @p minecraft:stick`）のリグレッションなし。
