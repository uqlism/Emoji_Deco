---
test: TC-HOTBAR-ESCAPE
feature: ホットバーアイテム名 エスケープ \#
result: PASS
date: 2026-05-18
commit: 0662990
screenshot: screenshots/TC-HOTBAR-ESCAPE.png
location: hotbar
content: escape
---

ホットバーに `#bold Item` がリテラルテキスト（太字なし）で表示される。
MixinGuiGraphics → ComponentConverter.toSequence() 経路でエスケープシーケンスが正しく処理される。

コマンド: `/give @p minecraft:stick[custom_name='{"text":"\\\\#bold Item"}']`
bash sequence 引数内でのバックスラッシュ数: 8個
wtype に渡る文字列: `\\\\#bold Item` (4バックスラッシュ)
MC コンポーネントパーサー変換後: `\#bold Item` (実際の値)
RunicInk エスケープ処理後: `#bold Item` (表示値)

注: MC 1.21.1 では `/give` の NBT 形式が `[custom_name='...']` のコンポーネント形式に変更されている。
旧来の `{display:{Name:...}}` NBT 形式は構文エラーになる。
