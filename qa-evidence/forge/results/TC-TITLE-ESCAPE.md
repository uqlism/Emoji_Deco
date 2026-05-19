---
test: TC-TITLE-ESCAPE
feature: タイトル エスケープシーケンス
result: PASS
date: 2026-05-18
commit: ce22356
location: title
content: escape
---

`/title` は JSON 直接渡しのため bash 4バックスラッシュで動作。`\\#bold` → `\#bold` (wtype) → `\#bold` (JSON) → RunicInk `\#` → `#bold` リテラル。TC-ACTIONBAR-ESCAPE と同一経路で確認済み。
