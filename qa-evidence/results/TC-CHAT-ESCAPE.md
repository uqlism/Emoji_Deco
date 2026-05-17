---
test: TC-CHAT-ESCAPE
feature: チャット エスケープ \#
result: PASS
date: 2026-05-18
commit: 81b2e92
screenshot: screenshots/TC-CHAT-ESCAPE.png
location: chat
content: escape
---

チャットに `\#bold text` と入力すると `#bold text` がリテラルテキスト（太字なし）で表示される。
MixinChatComponent → ComponentSequenceConverter.toSequence() 経路でエスケープシーケンスが正しく処理される。
