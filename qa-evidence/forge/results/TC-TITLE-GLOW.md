---
test: TC-TITLE-GLOW
feature: タイトル / サブタイトル #glow
result: N/A
date: 2026-05-18
commit: 3caacce
location: title
content: glow
---

2D GUI 描画では `packedLight` の概念がないため `LightMode.GLOW` による発光エフェクトは効かない（設計上）。3D ワールド描画コンテキストでのみ有効。
