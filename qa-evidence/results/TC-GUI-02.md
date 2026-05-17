---
test: TC-GUI-02
feature: GUI タイトル画面 動的デコレータ
result: FAIL
date: 2026-05-18
commit: bf2c5ed
screenshot: screenshots/TC-GUI-02.png
---

`#rainbow` が緑単色になる。`toComponent()` 経路では動的（時間ベース）デコレータが静的 Component に変換時に固定色となる。修正対応中（DynamicFormattedCharSequence 経路への変更）。
