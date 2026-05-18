---
test: TC-SIGN-ESCAPE
feature: 看板 エスケープ \#
result: PASS
date: 2026-05-18
commit: 0662990
screenshot: screenshots/TC-SIGN-ESCAPE.png
location: sign
content: escape
---

看板エディタで `\#bold Sign` を入力すると、設置した看板に `#bold Sign` がリテラルテキスト（太字なし）で表示される。
MixinSignText → ComponentConverter.toSequence() 経路でエスケープシーケンスが正しく処理される。

入力方法: 看板エディタで直接 `\#bold Sign` と入力（`type` コマンドで `\\#bold Sign` を送信）。
コマンド経由の setblock での直接設定は MC 1.21.1 の GSON が `\#` を不正エスケープとして拒否するため不可。
看板エディタ経由では RunicInk パーサーが正しく `\#` → `#` リテラルに変換することを確認。

エディタプレビュー確認: 入力中のプレビュー領域に `#bold Sign` が表示されることを確認。
設置後の看板面に `#bold Sign` がリテラルテキストで表示されることを確認。
