---
test: TC-CHAT-07
feature: チャット デコレータ #size
result: FAIL
date: 2026-05-18
commit: daffbb2
location: chat
content: size
---

`#size.2[Hi]` がチャットで通常サイズで描画される（スケール無効）。

**1.20.1 では動作していた**。1.21.1 でのリグレッション。

**原因**: `MixinChatComponent` が `toComponent()` を先に呼ぶため、`Scaled` ノードのスケール値がここで破棄される。その後 `computeNow()` → `toSequence()` を通るが、元の `#size.2[...]` 構文は Component に変換済みで失われているためスケールが復元されない。

**修正方針**: `MixinChatComponent` の変換経路を変更し、チャットでも `toComponent()` を経由せずに `toSequence()` / `DynamicFormattedCharSequence` 経路を使えるようにする。
