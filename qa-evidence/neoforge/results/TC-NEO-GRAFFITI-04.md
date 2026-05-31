---
test: TC-NEO-GRAFFITI-04
feature: Graffiti Ink クラフトレシピ（NeoForge）
result: PASS
date: 2026-05-31
commit: 7061b8b
screenshot: screenshots/TC-NEO-GRAFFITI-04.png
---

`recipe/` パス修正（`recipes/` → `recipe/`）および result フィールドを `id` 形式に修正後、NeoForge 1.21.1 でレシピが正常に認識される。`/recipe give @p emoji_deco:graffiti_ink` で "Unlocked 1 recipes for Dev" が表示され、サバイバルモードのレシピブックに graffiti_ink のクラフトレシピ（paper + c:dyes の shapeless）が 1 件表示される。クラフトグリッドにレシピが展開されることも確認。
