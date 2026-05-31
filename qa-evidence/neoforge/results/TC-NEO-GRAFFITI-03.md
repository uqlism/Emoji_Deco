---
test: TC-NEO-GRAFFITI-03
feature: GraffitiBlock 選択ハイライト位置（NeoForge）
result: PASS
date: 2026-05-31
commit: 1fe0015
screenshot: screenshots/TC-NEO-GRAFFITI-03.png
---

face=wall,facing=north の graffiti ブロック (0,-62,-4) をプレイヤーが北側 (z=-1) から南向きに見た状態で、黄色い選択ハイライト枠がブロックのプレイヤー向き面（北面 Z=0~0.5）に正確に重なって表示された。ハイライトは石支持ブロック (z=-5) より手前（プレイヤー側）に位置しており、壁側（南面）ではなくプレイヤー向き面にあることを確認。SHAPE_WALL_N = Block.box(0,0,0,16,16,0.5) の修正が正しく反映されている。
