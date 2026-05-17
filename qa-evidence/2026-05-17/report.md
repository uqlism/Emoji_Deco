# QA Evidence — 2026-05-17

**ブランチ**: 1.21.1  
**コミット**: 836952c  
**Minecraft / Forge**: 1.21.1

## 結果サマリー

合計 10件 / ✅ PASS: 8件 / ❌ FAIL: 0件 / ⚠ CONDITIONAL: 2件

| # | テストケース | 機能 | 結果 |
|---|---|---|---|
| 1 | TC-SIGN-01 | 看板リッチテキスト (`MixinSignText`) | ✅ PASS |
| 2 | TC-ENTITY-01 | エンティティ名前タグ (`MixinFont`) | ✅ PASS |
| 3 | TC-CHAT-01 | ショートコード—アイテムスプライト | ✅ PASS |
| 4 | TC-CHAT-02 | デコレータ `#bold` | ✅ PASS |
| 5 | TC-CHAT-03 | デコレータ `#color.red` | ✅ PASS |
| 6 | TC-CHAT-04 | デコレータ `#size.2`（Chat では無効） | ⚠ 仕様通り（size は Chat 非対応） |
| 7 | TC-CHAT-05 | エスケープシーケンス `\#bold` | ✅ PASS |
| 8 | TC-CHAT-06 | プレイヤーヘッド `:player.Steve:` | ⚠ グリフ描画は動作、スキンはオフライン時に取得不可（白い四角） |
| 9 | TC-AUTOCOMPLETE-01 | デコレータ補完（`#b` プレフィックス） | ✅ PASS |
| 10 | TC-AUTOCOMPLETE-02 | ショートコード補完（`:di` プレフィックス） | ✅ PASS |

## スクリーンショット

### TC-SIGN-01 — 看板リッチテキスト
![TC-SIGN-01](TC-SIGN-01.png)
> `#color.gold[Hello Sign]` が金色で描画されている。raw テキストではなくリッチテキストとして処理されていることを確認。

### TC-ENTITY-01 — エンティティ名前タグ
![TC-ENTITY-01](TC-ENTITY-01.png)
> 左 Cow: `#bold[Bold]`（太字）、右 Cow: `Normal`（通常）。文字の太さの違いが明確で `MixinFont.runicink$transformDrawBatchComponent` が正常に機能していることを確認。

### TC-CHAT-01 — ショートコード（アイテムスプライト）
![TC-CHAT-01](TC-CHAT-01.png)
> `:item.diamond:` がダイヤモンドのスプライトアイコン（青い点）として描画されている。

### TC-CHAT-02 — デコレータ（bold）
![TC-CHAT-02](TC-CHAT-02.png)
> `#bold[Hello]` が太字で描画されている。

### TC-CHAT-03 — デコレータ（color）
![TC-CHAT-03](TC-CHAT-03.png)
> `#color.red[Hello]` が赤色で描画されている。

### TC-CHAT-04 — デコレータ（size）
![TC-CHAT-04](TC-CHAT-04.png)
> `#size.2[Hi]` は Chat では通常サイズで描画される（`Sized` ノードは `toComponent()` 経路では無効 — 設計通り）。

### TC-CHAT-05 — エスケープシーケンス
![TC-CHAT-05](TC-CHAT-05.png)
> `\#bold` が `#bold` というリテラルテキストとして（書式なしで）描画されている。

### TC-CHAT-06 — プレイヤーヘッド
![TC-CHAT-06](TC-CHAT-06.png)
> `:player.Steve:` でグリフのスペース（白い四角）が確保されている。スキンテクスチャはオフラインプレイ時にはダウンロードできないため空白表示となるが、グリフシステム自体は正常に動作している。

### TC-AUTOCOMPLETE-01 — デコレータ補完
![TC-AUTOCOMPLETE-01](TC-AUTOCOMPLETE-01.png)
> `#b` 入力後に `bold #bold[]` のドロップダウンが表示されている。`#` のみの入力では使用履歴が空の場合に候補が出ない（仕様通り）。

### TC-AUTOCOMPLETE-02 — ショートコード補完
![TC-AUTOCOMPLETE-02](TC-AUTOCOMPLETE-02.png)
> `:di` 入力後に `:disappointed:` `:disguised_face:` 等の候補が表示されている。
