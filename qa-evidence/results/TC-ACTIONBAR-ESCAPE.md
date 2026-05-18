---
test: TC-ACTIONBAR-ESCAPE
feature: アクションバー エスケープ \#
result: PASS
date: 2026-05-18
commit: 0662990
screenshot: screenshots/TC-ACTIONBAR-ESCAPE.png
location: actionbar
content: escape
---

アクションバーに `#bold Bar` がリテラルテキスト（太字なし）で表示される。
MixinGuiGraphics → ComponentConverter.toSequence() 経路でエスケープシーケンスが正しく処理される。

コマンド: `/title @s actionbar {"text":"\\#bold Bar"}`
bash sequence 引数内でのバックスラッシュ数: 4個（SNBT レイヤーなしのため）
wtype に渡る文字列: `\\#bold Bar` (2バックスラッシュ)
MC JSON パーサー変換後: `\#bold Bar` (実際の値)
RunicInk エスケープ処理後: `#bold Bar` (表示値)

注: `/title` コマンドは SNBT なしの JSON コンポーネントを直接受け取るため、
エスケープレイヤーが1段少なく（SNBT → JSON の変換がない）、4バックスラッシュで十分。
エンティティ/ホットバーの 8バックスラッシュと異なる点に注意。
