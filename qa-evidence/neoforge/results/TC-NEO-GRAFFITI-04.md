---
test: TC-NEO-GRAFFITI-04
feature: Graffiti Ink クラフトレシピ（NeoForge）
result: FAIL
date: 2026-05-31
commit: 8e18fcf
screenshot: screenshots/TC-NEO-GRAFFITI-04.png
---

修正後（c:dyes + paper）も依然 FAIL。`/recipe give @p emoji_deco:graffiti_ink` で "Unknown recipe: emoji_deco:graffiti_ink" エラーが表示される。jar には `data/emoji_deco/recipes/graffiti_ink.json` が含まれているが、MC 1.21.1 では レシピパスが `data/<namespace>/recipe/`（単数形）に変更されており、`recipes/`（複数形）は認識されない。また result フィールドも `{"id": "...", "count": N}` 形式が必要だが `{"item": "...", "count": N}` のままである。2つの互換性問題が残存している。
