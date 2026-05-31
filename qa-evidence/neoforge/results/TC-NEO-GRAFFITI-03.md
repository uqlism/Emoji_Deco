---
test: TC-NEO-GRAFFITI-03
feature: GraffitiBlock 選択ハイライト位置（NeoForge）
result: FAIL
date: 2026-05-31
commit: 85906d1
screenshot: screenshots/TC-NEO-GRAFFITI-03.png
---

graffiti ブロックを正面から見ると、黄色い選択ハイライト枠がブロック本体の上半分より上に空中に浮いた状態で表示される。ブロックの描画面（薄いスラブ状の南面）とハイライト位置が大きくずれており、VoxelShape の定義が実際の描画形状と一致していないことが確認された。FAIL 条件：ハイライトがブロックの描画面（0.5px 厚の薄い面）ではなく、ブロック上部の空中に表示されている。
