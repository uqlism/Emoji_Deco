---
test: TC-NEO-GRAFFITI-03
feature: GraffitiBlock 選択ハイライト位置（NeoForge）
result: FAIL
date: 2026-05-31
commit: 8d805b9
screenshot: screenshots/TC-NEO-GRAFFITI-03.png
---

graffiti_ink を手に持ったまま face=wall,facing=south の graffiti ブロック（石の南面に設置）を正面（南）から見たとき、黄色い選択ハイライト枠がブロックの実際の視覚面（石の z=1.0 側に貼り付いた薄いスラブ）と大きくずれて空中に浮いて表示された。ハイライト枠は "NeoGraffiti" ラベルの周囲（ブロック本体より明らかに上かつ遠い位置）に表示されており、FAIL 条件「ハイライトがブロックの反対側に表示される」に該当する。VoxelShape の定義（SHAPE_WALL_S = Block.box(0,0,0,16,16,0.5)）自体はコード上正しいが、実際のレンダリングでは選択判定と描画が一致していない。
