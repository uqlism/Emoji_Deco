---
test: TC-HOTBAR-05
feature: ホットバーアイテム名 #glow
result: CONDITIONAL
date: 2026-05-18
commit: 4a3aeab
screenshot: screenshots/TC-HOTBAR-05.png
location: hotbar
content: glow
---

ホットバーアイテム名に `#glow[Glow Stone]` と表示されている（テキスト描画は正常）。
ただし発光エフェクト（LightMode）はホットバー名経路（GuiGraphics.drawString -> toComponent()）では
LightSequence が無視されるため、視覚的な発光差は確認できない。
これはアーキテクチャ上の仕様であり、FCS 経路（看板・落書きブロック）では有効。
