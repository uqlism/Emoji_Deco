---
test: TC-ENTITY-ESCAPE
feature: エンティティ名前タグ エスケープ \#
result: PASS
date: 2026-05-18
commit: 0662990
screenshot: screenshots/TC-ENTITY-ESCAPE.png
location: entity
content: escape
---

エンティティ（Cow）の名前タグに `#bold Cow` がリテラルテキスト（太字なし）で表示される。
MixinFont → ComponentTransformer → RichTextParser 経路でエスケープシーケンスが正しく処理される。

コマンド: `/summon minecraft:cow ... {CustomName:'{"text":"\\\\#bold Cow"}'}`
bash sequence 引数内でのバックスラッシュ数: 8個
wtype に渡る文字列: `\\\\#bold Cow` (4バックスラッシュ)
MC SNBT パーサー変換後: `\\#bold Cow` (JSON文字列内 2バックスラッシュ)
JSON パーサー変換後: `\#bold Cow` (実際の値)
RunicInk エスケープ処理後: `#bold Cow` (表示値)

注: `\#` を直接 SNBT の JSON 文字列に含めると MC 1.21.1 の GSON が MalformedJsonException を発生させる。
SNBT レイヤーで1段余分にエスケープ (`\\\\` → `\\`) することで回避。
