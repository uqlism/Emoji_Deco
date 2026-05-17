---
test: TC-ACTIONBAR-DECORATION
feature: アクションバー #underline #strike
result: PASS
date: 2026-05-18
commit: 81b2e92
screenshot: screenshots/TC-ACTIONBAR-DECORATION.png
location: actionbar
content: decoration
---

`#underline[Under] #strike[Strike]` がアクションバーに下線付き「Under」と取り消し線付き「Strike」として表示されている。
MixinGuiGraphics → ComponentSequenceConverter.toSequence() 経路で装飾スタイルが正しく適用される。
