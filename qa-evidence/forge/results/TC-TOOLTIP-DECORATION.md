---
test: TC-TOOLTIP-DECORATION
feature: ホバーツールチップ #underline #strike
result: PASS
date: 2026-05-18
commit: 81b2e92
screenshot: screenshots/TC-TOOLTIP-DECORATION.png
location: tooltip
content: decoration
---

`#underline[U] #strike[S]` をカスタム名に持つエメラルドのツールチップに「U」（下線）と「S」（取り消し線）が表示されている。
GuiGraphics.renderComponentHoverEffect → toComponent() 経路でも装飾スタイルが正しく適用される。
