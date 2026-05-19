---
test: TC-NEO-CHAT-SIZE
feature: チャット #size（リグレッション）
result: FAIL
date: 2026-05-19
commit: 65c495c
location: chat
content: size
---

1.21.1 で MixinChatComponent が toComponent() を先に呼ぶためスケール情報が失われる。Forge 版と同一の既知バグ。
