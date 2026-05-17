---
test: TC-SIGN-ESCAPE
feature: 看板 エスケープ \#
result: CONDITIONAL
date: 2026-05-18
commit: 81b2e92
screenshot: screenshots/TC-CHAT-ESCAPE.png
location: sign
content: escape
---

MC 1.21.1 の `/setblock` コマンド内 JSON パーサーが `\#` を不正エスケープとして拒否するため、
コマンドベースでの看板エスケープテストは実行不可（環境制限）。
RunicInk のエスケープパーサー自体はチャット経路（TC-CHAT-ESCAPE）で PASS 確認済み。
看板経路（MixinSignText → ComponentSequenceConverter.toSequence()）は同一 RichTextParser を使用するため
動作するはずだが、直接確認できない。
