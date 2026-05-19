---
test: TC-NEO-AUTOCOMPLETE-SHORTCODE
feature: ショートコード補完
result: PASS
date: 2026-05-19
commit: bf4c476
location: chat
content: sprite
---

**PASS** (2026-05-19 再確認): `emoji_deco_starter` リソースパックが正常にロードされ、ショートコード補完が動作することを確認。

- `:di` 入力時に `:dizzy:` `:disappointed:` `:diamonds:` 等のドロップダウンサジェストが表示される（各エントリに絵文字スプライト付き）
- ログ: `Loaded 1858 shortcode(s), 61 alias(es)`
- ResourceManager に `builtin/emoji_deco_starter` が含まれている
- "no longer compatible" のエラーなし

**根本原因と修正**: `emoji_deco_starter/pack.mcmeta` の `pack_format` が 15 (MC 1.20.1) のまま、前回 18 (MC 1.20.2) に変更したが MC 1.21.1 では 34 が必要。
今回 `pack_format: 34, supported_formats: [15, 34]` に修正し解決。
