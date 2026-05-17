---
test: TC-GUI-02
feature: GUI タイトル画面 動的デコレータ
result: PASS
date: 2026-05-18
commit: afd7d25
screenshot: screenshots/TC-GUI-02.png
location: title
content: rainbow
---

`#rainbow[Title]` が画面中央に時間依存カラーアニメーションで表示されている。
MixinGuiGraphics を常に DynamicFormattedCharSequence でラップするよう修正後に PASS。
複数フレームでシアン→ピンクへの色変化を確認し、DynamicFCS による毎フレーム再評価が機能していることを証明。
