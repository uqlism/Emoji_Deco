---
test: TC-CHAT-07
feature: チャット デコレータ #size
result: CONDITIONAL
date: 2026-05-17
commit: f3f8074

location: chat
content: size
---

`#size` はチャットでは通常サイズで描画される（スケール無効）。

**根本原因**: チャット経路は `MixinChatComponent` で `toComponent()` を先に呼ぶため、`Scaled` ノードのスケール値がここで破棄される。その後 `computeNow()` → `toSequence()` を通るが、元の `#size.2[...]` 構文は Component に変換済みで失われているためスケールが復元されない。

ホットバー・GUI・看板・Graffiti は `toComponent()` を経由しないため `#size` が正常動作する。チャットのスケール対応は `MixinChatComponent` のアーキテクチャ変更が必要（将来課題）。
