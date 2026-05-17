---
test: TC-HOTBAR-09
feature: ホットバーアイテム名 ネスト (#bold[#color.red[...]])
result: PASS
date: 2026-05-18
commit: 4a3aeab
screenshot: screenshots/TC-HOTBAR-09.png
location: hotbar
content: nest
---

ホットバーアイテム名に `#bold[#color.red[Bold Red]]` が太字かつ赤色で表示されている。
ネストされたデコレータが正しくスタイルを合成することを確認。
チャットログの give 確認メッセージでも同様に太字・赤色で描画されていた。
