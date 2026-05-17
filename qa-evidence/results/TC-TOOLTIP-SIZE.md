---
test: TC-TOOLTIP-SIZE
feature: ホバーツールチップ #size
result: CONDITIONAL
date: 2026-05-18
commit: 81b2e92
screenshot: screenshots/TC-TOOLTIP-SIZE.png
location: tooltip
content: size
---

`#size.2[BIG GEM]` をカスタム名に持つダイヤモンドのツールチップに "BIG GEM" が表示されている。
ツールチップは toComponent() 経路のため Sized ノードが無視され、通常サイズで描画される（設計上）。
テキスト自体の描画は正常。
