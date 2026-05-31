---
test: TC-NEO-GRAFFITI-04
feature: Graffiti Ink クラフトレシピ（NeoForge）
result: FAIL
date: 2026-05-31
commit: 85906d1
screenshot: screenshots/TC-NEO-GRAFFITI-04.png
---

サバイバルモードのレシピブックで "graffiti" と検索しても結果が 0 件。graffiti_ink.json のレシピは材料に `minecraft:potion{nbt}` + `forge:dyes` タグを使用しているが、NeoForge 1.21.1 では `forge:dyes` タグが存在せず（`c:dyes` が正しい）、またポーションの NBT マッチングも 1.21.1 の Components 形式に未対応のため、レシピが認識されない。材料を揃えてもクラフトできないことを確認。
