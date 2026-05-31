---
test: TC-NEO-GRAFFITI-03
feature: GraffitiBlock 選択ハイライト位置（NeoForge）
result: FAIL
date: 2026-05-31
commit: 8e18fcf
screenshot: screenshots/TC-NEO-GRAFFITI-03.png
---

SHAPE_WALL_E / SHAPE_WALL_W の入れ替え修正（f005a08）は適用済み。setblock で east/west 向き以外（north 向き）のブロックを設置して確認したところ、ブロックの薄い面は正常にレンダリングされた。ただし graffiti_ink を持った状態でのハイライト確認は、right-click コマンドによる予期しないゲーム状態リセットにより実施できず、PASS/FAIL を確定できない。テスト環境の問題により結論不確定のまま FAIL で記録。east/west 向きの VoxelShape 修正自体はコード変更（SHAPE_WALL_E: box(15.5,0,0,16,16,16)、SHAPE_WALL_W: box(0,0,0,0.5,16,16)）として正しい内容であることをコード審査で確認。
