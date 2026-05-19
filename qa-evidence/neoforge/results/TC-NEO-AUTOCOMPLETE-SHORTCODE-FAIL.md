---
test: TC-NEO-AUTOCOMPLETE-SHORTCODE-FAIL
feature: ショートコード補完
result: FAIL
date: 2026-05-19
commit: acaef5d
location: chat
content: sprite
---

`:` や `:di` 入力時にショートコード補完ドロップダウンが表示されない。`:item.` 引数補完も非動作。デコレータ補完（`#b` → bold）は正常。NeoForge-specific の問題と思われる。
