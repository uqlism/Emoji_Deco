---
test: TC-HOTBAR-05
feature: ホットバーアイテム名 #glow
result: N/A
date: 2026-05-18
commit: 3caacce
location: hotbar
content: glow
---

2D GUI 描画では `packedLight` の概念がないため `LightMode.GLOW` による発光エフェクトは効かない（設計上）。テキスト自体は正常表示。3D ワールド描画（看板・エンティティ名前タグ・Graffiti）でのみ有効。
