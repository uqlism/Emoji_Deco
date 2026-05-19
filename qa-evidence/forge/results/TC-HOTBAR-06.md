---
test: TC-HOTBAR-06
feature: ホットバーアイテム名 スプライト (:item:)
result: PASS
date: 2026-05-18
commit: 4a3aeab
screenshot: screenshots/TC-HOTBAR-06.png
location: hotbar
content: sprite
---

ホットバーアイテム名に `:item.diamond: Stone` のダイヤモンドスプライトアイコン + "Stone" が表示されている。
SpriteRegistry によるカスタムグリフが GuiGraphics 経路でも正しく描画されることを確認。
