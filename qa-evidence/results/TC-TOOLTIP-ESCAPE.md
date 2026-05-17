---
test: TC-TOOLTIP-ESCAPE
feature: ホバーツールチップ エスケープ \#
result: CONDITIONAL
date: 2026-05-18
commit: 81b2e92
screenshot: screenshots/TC-CHAT-ESCAPE.png
location: tooltip
content: escape
---

MC 1.21.1 の `/give` コマンド内コンポーネント JSON パーサーが `\#` を不正エスケープとして拒否するため、
コマンドベースでのツールチップエスケープテストは実行不可（環境制限）。
RunicInk のエスケープパーサー自体はチャット経路（TC-CHAT-ESCAPE）で PASS 確認済み。
