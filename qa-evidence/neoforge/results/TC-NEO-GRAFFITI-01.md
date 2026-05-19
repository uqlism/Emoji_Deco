---
test: TC-NEO-GRAFFITI-01
feature: Graffiti ブロック リッチテキスト描画（NeoForge）
result: PASS
date: 2026-05-19
commit: 6cb9c51
screenshot: screenshots/TC-NEO-GRAFFITI-01.png
location: graffiti
content: rainbow, sprite
---
NeoForge 版で Graffiti ブロックに #rainbow[NeoGraffiti] と :item.diamond: が正しく描画されている。
/setblock と /data merge でブロックを設置し、テキストが "NeoGraffiti"（多色）と
ダイヤアイコン（水色の点）として描画されることを確認した。

GraffitiRenderer が NeoForge 版でも正常に動作している。
