---
test: TC-NEO-GRAFFITI-02
feature: GraffitiEditScreen 入力枠表示品質（NeoForge）
result: FAIL
date: 2026-05-31
commit: 85906d1
screenshot: screenshots/TC-NEO-GRAFFITI-02.png
---

GraffitiEditScreen を開くと、入力枠（EditBox）の上方に黄色い選択ハイライト枠（VoxelShape 由来）が GUI 上に誤って重なって表示される。入力枠のパネルは 1 行のみで構造が不完全であり、タイトル "Edit Graffiti" もぼんやりとした表示になっている。テキスト入力自体（"Hello World"）は機能するが、GUI の描画品質に問題がある。FAIL 条件：入力枠の上に graffiti ブロックの選択ハイライトが誤表示されている。
