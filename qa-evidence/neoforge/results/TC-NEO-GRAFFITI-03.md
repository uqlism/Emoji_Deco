---
test: TC-NEO-GRAFFITI-03
feature: GraffitiBlock 選択ハイライト位置（NeoForge）
result: FAIL
date: 2026-05-31
commit: 7061b8b
screenshot: screenshots/TC-NEO-GRAFFITI-03.png
---

SHAPE_WALL_E / SHAPE_WALL_W の入れ替え修正は適用済み。PostMessage ベース right-click（commit 7061b8b の新実装）も依然として NeoForge 1.21.1 のマウスキャプチャと干渉し、graffiti_ink を手に持った状態でのハイライト選択確認が実施できない。right-click コマンドは視点リセットを引き起こすだけでインタラクションとして機能しない。VoxelShape 修正（SHAPE_WALL_E: box(15.5,0,0,16,16,16)、SHAPE_WALL_W: box(0,0,0,0.5,16,16)）はコード審査で正しい内容を確認済み。テスト環境の right-click 問題が解決するまで FAIL を継続。
