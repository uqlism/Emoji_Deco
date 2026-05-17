---
test: TC-GUI-05
feature: アクションバー #size
result: PASS
date: 2026-05-18
commit: 1a52ac8
screenshot: screenshots/TC-GUI-05.png
location: actionbar
content: size
---

`#size.2[BIG]` がアクションバーに通常より大きなテキストとして表示されている。MixinGuiGraphics → DynamicFCS → toSequence() → ScaledSequence 経路でスケールが適用される。
