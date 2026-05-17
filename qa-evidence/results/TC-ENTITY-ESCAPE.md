---
test: TC-ENTITY-ESCAPE
feature: エンティティ名前タグ エスケープ \#
result: CONDITIONAL
date: 2026-05-18
commit: 81b2e92
screenshot: screenshots/TC-CHAT-ESCAPE.png
location: entity
content: escape
---

MC 1.21.1 の `/summon` コマンド内 JSON パーサーが `\#` を不正エスケープとして拒否するため、
コマンドベースでのエンティティエスケープテストは実行不可（環境制限）。
RunicInk のエスケープパーサー自体はチャット経路（TC-CHAT-ESCAPE）で PASS 確認済み。
エンティティ経路（MixinFont → ComponentTransformer → RichTextParser）は同一パーサーを使用するため
動作するはずだが、直接確認できない。
