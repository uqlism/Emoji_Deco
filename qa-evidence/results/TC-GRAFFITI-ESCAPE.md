---
test: TC-GRAFFITI-ESCAPE
feature: Graffiti ブロック エスケープ \#
result: CONDITIONAL
date: 2026-05-18
commit: 81b2e92
screenshot: screenshots/TC-CHAT-ESCAPE.png
location: graffiti
content: escape
---

Graffiti ブロックのエディタへの直接入力でのエスケープテストは実行していない。
RunicInk のエスケープパーサー自体はチャット経路（TC-CHAT-ESCAPE）で PASS 確認済み。
Graffiti 経路（GraffitiRenderer → RichNode.toSequence()）は同一 RichTextParser を使用する。
