---
test: TC-ENTITY-DECORATION
feature: エンティティ名前タグ #underline #strike
result: PASS
date: 2026-05-18
commit: 81b2e92
screenshot: screenshots/TC-ENTITY-DECORATION.png
location: entity
content: decoration
---

`#underline[U] #strike[S]` がエンティティ名前タグに下線付き「U」と取り消し線付き「S」として表示されている。
Font.drawInBatch(Component) → computeNow() → toSequence() 経路。
