---
test: TC-NEO-AUTOCOMPLETE-SHORTCODE-FAIL
feature: ショートコード補完
result: CONDITIONAL
date: 2026-05-19
commit: acaef5d
location: chat
content: sprite
---

**原因判明（コードバグではない）**: `neoforge/run/options.txt` の `resourcePacks:[]` で `emoji_deco_starter` が未ロード。
twemoji shortcode が未登録のため `:di` に一致するショートコードが存在しない。

修正: `options.txt` を `["builtin/emoji_deco_starter"]` に更新。次回起動から解消見込み。
- `:item.` 引数サジェスト: `item.json` に `suggestions` フィールドなし（設計上空、Forge も同様）
- `:` 単独: prefix 空のとき recent history のみ表示（初回は履歴なし）
