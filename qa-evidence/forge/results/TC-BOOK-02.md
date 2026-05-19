---
test: TC-BOOK-02
feature: 本 #size（非対応）
result: N/A
date: 2026-05-18
commit: 25280fa
location: book
content: size
---

本（Written Book）は `font.split()` → `font.drawInBatch(FormattedCharSequence)` 経路を使うため
`Sized` ノードが無効。`toComponent()` 変換後に scale 情報が失われる（設計上）。
