---
test: TC-ENTITY-SIZE
feature: エンティティ名前タグ #size
result: PASS
date: 2026-05-18
commit: 81b2e92
screenshot: screenshots/TC-ENTITY-SIZE.png
location: entity
content: size
---

`#size.2[BigCow]` が2倍サイズでエンティティ名前タグに表示されている。
Font.drawInBatch(Component) → computeNow() → toSequence() 経路で ScaledSequence が有効。
