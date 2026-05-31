---
test: TC-NEO-GRAFFITI-02
feature: GraffitiEditScreen 入力枠表示品質（NeoForge）
result: PASS
date: 2026-05-31
commit: 8d805b9
screenshot: screenshots/TC-NEO-GRAFFITI-02.png
---

right-click コマンド（lParam=0 修正後、WM_RBUTTONDOWN/UP）が NeoForge 1.21.1 で正常に機能し、GraffitiEditScreen が開いた。入力枠（黒いパネル内）に "Hello World" を入力したところ、テキストがシャープに表示され枠外へのはみ出しなし。カーソル（_）も正しい位置に描画。入力枠の境界線は 1px のシャープな線として描画されており、ぼやけ・にじみなし。PASS 条件をすべて満たす。
