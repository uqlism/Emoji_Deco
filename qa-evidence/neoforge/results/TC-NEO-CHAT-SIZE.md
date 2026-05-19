---
test: TC-NEO-CHAT-SIZE
feature: チャット #size（リグレッション）
result: PASS
date: 2026-05-19
commit: 52e7bc3
location: chat
content: size
---

runicink$modifyGuiMessage から toComponent() 呼び出しを削除し、DynamicLineSequence パスで Scaled ノードを保持することで修正。
`#size.2[Hi]` がチャットに通常の約2倍サイズで描画されることを確認。
