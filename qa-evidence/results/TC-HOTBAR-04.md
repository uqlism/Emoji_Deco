---
test: TC-HOTBAR-04
feature: ホットバーアイテム名 #rainbow (動的)
result: PASS
date: 2026-05-18
commit: 4a3aeab
screenshot: screenshots/TC-HOTBAR-04.png
location: hotbar
content: rainbow
---

ホットバーアイテム名に `#rainbow[Rainbow Star]` が動的カラーで表示されている。
2フレーム間で色がアクア→オレンジに変化しており、アニメーション動作を確認。
ホットバーは toComponent() 経由だが、DynamicFormattedCharSequence として毎フレーム再評価される。
