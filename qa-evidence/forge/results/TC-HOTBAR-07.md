---
test: TC-HOTBAR-07
feature: ホットバーアイテム名 #size
result: PASS
date: 2026-05-18
commit: 4a3aeab
screenshot: screenshots/TC-HOTBAR-07.png
location: hotbar
content: size
---

ホットバーアイテム名に `#size.2[Big]` が通常の2倍サイズで表示されている。
MixinGuiGraphics 経由（toComponent() ではなく AffineSequence/ScaledSequence 経路）でスケールが有効。
テスト仕様では CONDITIONAL とされていたが、実際には PASS（スケール有効）。
