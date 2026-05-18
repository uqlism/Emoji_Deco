---
test: TC-CHAT-01
feature: チャット ショートコード（アイテムスプライト）
result: PASS
date: 2026-05-18
commit: afd7d25
screenshot: screenshots/TC-CHAT-01.png
location: chat
content: sprite
---

`:item.diamond:` がダイヤモンドのスプライトアイコンとして描画されている。ImageGlyphPool.bake() up=0, down=h 修正後に "Hello" テキストと同じ高さに正しく揃っていることを確認。
