---
test: TC-HOTBAR-01
feature: ホットバーアイテム名 #bold
result: PASS
date: 2026-05-18
commit: 4a3aeab
screenshot: screenshots/TC-HOTBAR-01.png
location: hotbar
content: bold_italic
---

ホットバーアイテム名に `#bold[Bold Diamond]` が太字で表示されている。
custom_name コンポーネント構文: `minecraft:diamond[minecraft:custom_name='"#bold[Bold Diamond]"']`
MixinGuiGraphics 経由で正しく変換・描画されることを確認。
