---
test: TC-TOOLTIP-04
feature: ホバーツールチップ #glow
result: PASS
date: 2026-05-18
commit: 4a52b56
screenshot: screenshots/TC-TOOLTIP-04.png
location: tooltip
content: glow
---
`#glow[Glow Item]` をカスタム名に持つグロウストーンダストのツールチップに "Glow Item" が正しく表示されている。ツールチップは toComponent() 経路のため LightMode による光度変化は非対応（設計上）。テキスト自体の描画は正常。
