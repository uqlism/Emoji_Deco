---
test: TC-NEO-GRAFFITI-02
feature: GraffitiEditScreen 入力枠表示品質（NeoForge）
result: FAIL
date: 2026-05-31
commit: 8e18fcf
screenshot: screenshots/TC-NEO-GRAFFITI-02.png
---

GraffitiEditScreen を開くための right-click（SendInput 版 mc_qa.py コマンド）を実行するたびにゲームが予期しない状態（スポーン地点にリセット）になるため、エディタ画面を開くことができなかった。mc_qa.py の right-click 実装（SendInput、_focus() 経由）が Minecraft NeoForge 1.21.1 のマウスキャプチャと干渉している可能性がある。テスト環境の問題により FAIL で記録（GraffitiEditScreen 自体の描画品質は未評価）。
