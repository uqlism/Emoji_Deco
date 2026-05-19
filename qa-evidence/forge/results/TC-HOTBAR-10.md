---
test: TC-HOTBAR-10
feature: ホットバーアイテム名 エスケープ (\#bold)
result: PASS
date: 2026-05-18
commit: 4a3aeab
screenshot: screenshots/TC-HOTBAR-10.png
location: hotbar (chat fallback)
content: escape
---

チャット入力で `\#bold Literal` を送信したところ、チャットに `#bold Literal` が
書式なし（通常テキスト）で表示された。バックスラッシュエスケープが正しく機能している。

備考: アイテム名経由のテストは 1.21.1 の SNBT/JSON パーサーが `\#` を無効なエスケープとして
拒否するため実施不可。チャット経路で代替検証した。パーサー（RichTextParser）自体は
ホットバー経路と共通のため、機能は同等に確認できている。
