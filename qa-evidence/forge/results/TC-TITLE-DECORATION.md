---
test: TC-TITLE-DECORATION
feature: タイトル / サブタイトル #underline #strike
result: PASS
date: 2026-05-18
commit: 81b2e92
screenshot: screenshots/TC-TITLE-DECORATION.png
location: title
content: decoration
---

`#underline[Under] #strike[Strike]` がタイトルに下線付き「Under」と取り消し線付き「Strike」として大きく表示されている。
MixinGuiGraphics → ComponentSequenceConverter.toSequence() 経路で装飾スタイルが正しく適用される。
