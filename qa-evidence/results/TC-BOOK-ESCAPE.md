---
test: TC-BOOK-ESCAPE
feature: 本 (Written Book) エスケープ \#
result: CONDITIONAL
date: 2026-05-18
commit: 81b2e92
screenshot: screenshots/TC-CHAT-ESCAPE.png
location: book
content: escape
---

MC 1.21.1 の本コンポーネント内でのエスケープテストは直接実行していない。
RunicInk のエスケープパーサー自体はチャット経路（TC-CHAT-ESCAPE）で PASS 確認済み。
本経路（MixinWrittenBookAccess → ComponentTransformer → RichTextParser）は同一パーサーを使用する。
