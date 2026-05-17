---
test: TC-ACTIONBAR-GLOW
feature: アクションバー #glow（2D GUI）
result: CONDITIONAL
date: 2026-05-18
commit: 1a52ac8
location: actionbar
content: glow
---

テキスト自体は正常に描画される。`LightMode.GLOW` は `packedLight` を最大値に上書きする仕組みだが、2D GUI 描画では `packedLight` が使われないため発光エフェクトは視覚的に無効。ホットバー（TC-HOTBAR-05）と同様の制限。
