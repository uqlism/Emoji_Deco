# QA Report

> 自動生成: 2026-05-18 01:10 — `python scripts/qa/gen_report.py`

**合計**: 26件 / ✅ PASS: 23件 / ⚠ CONDITIONAL: 2件 / — N/A: 1件

| テストケース | 機能 | 結果 | 最終実行 | コミット |
|---|---|---|---|---|
| [TC-AUTO-01](results/TC-AUTO-01.md) | オートコンプリート デコレータ候補 | ✅ PASS | 2026-05-17 | `f3f8074` |
| [TC-AUTO-02](results/TC-AUTO-02.md) | オートコンプリート ショートコード候補 | ✅ PASS | 2026-05-17 | `f3f8074` |
| [TC-AUTO-03](results/TC-AUTO-03.md) | オートコンプリート デコレータ引数候補 | ✅ PASS | 2026-05-18 | `bf2c5ed` |
| [TC-AUTO-04](results/TC-AUTO-04.md) | オートコンプリート ショートコード引数候補 | — N/A | 2026-05-18 | `bf2c5ed` |
| [TC-BOOK-01](results/TC-BOOK-01.md) | Written Book リッチテキスト | ✅ PASS | 2026-05-18 | `d2e8510` |
| [TC-CHAT-01](results/TC-CHAT-01.md) | チャット ショートコード（アイテムスプライト） | ✅ PASS | 2026-05-18 | `afd7d25` |
| [TC-CHAT-02](results/TC-CHAT-02.md) | チャット ショートコード（ブロックスプライト） | ✅ PASS | 2026-05-18 | `afd7d25` |
| [TC-CHAT-03](results/TC-CHAT-03.md) | チャット ショートコード（プレイヤーヘッド） | ⚠ CONDITIONAL | 2026-05-17 | `f3f8074` |
| [TC-CHAT-04](results/TC-CHAT-04.md) | チャット デコレータ #bold | ✅ PASS | 2026-05-17 | `f3f8074` |
| [TC-CHAT-05](results/TC-CHAT-05.md) | チャット デコレータ #italic | ✅ PASS | 2026-05-18 | `bf2c5ed` |
| [TC-CHAT-06](results/TC-CHAT-06.md) | チャット デコレータ #color | ✅ PASS | 2026-05-17 | `f3f8074` |
| [TC-CHAT-07](results/TC-CHAT-07.md) | チャット デコレータ #size | ⚠ CONDITIONAL | 2026-05-17 | `f3f8074` |
| [TC-CHAT-08](results/TC-CHAT-08.md) | チャット デコレータ #glow | ✅ PASS | 2026-05-18 | `bf2c5ed` |
| [TC-CHAT-09](results/TC-CHAT-09.md) | チャット デコレータ #rainbow | ✅ PASS | 2026-05-18 | `bf2c5ed` |
| [TC-CHAT-10](results/TC-CHAT-10.md) | チャット デコレータ #underline #strike | ✅ PASS | 2026-05-18 | `bf2c5ed` |
| [TC-CHAT-11](results/TC-CHAT-11.md) | チャット デコレータ ネスト | ✅ PASS | 2026-05-18 | `bf2c5ed` |
| [TC-CHAT-12](results/TC-CHAT-12.md) | チャット エスケープシーケンス | ✅ PASS | 2026-05-17 | `f3f8074` |
| [TC-CHAT-13](results/TC-CHAT-13.md) | チャット 複合表現 | ✅ PASS | 2026-05-18 | `bf2c5ed` |
| [TC-ENTITY-01](results/TC-ENTITY-01.md) | エンティティ名前タグ リッチテキスト | ✅ PASS | 2026-05-17 | `836952c` |
| [TC-GRAFFITI-01](results/TC-GRAFFITI-01.md) | Graffiti ブロック リッチテキスト基本 | ✅ PASS | 2026-05-17 | `c3cc94f` |
| [TC-GRAFFITI-02](results/TC-GRAFFITI-02.md) | Graffiti ブロック glow モード（暗所発光） | ✅ PASS | 2026-05-18 | `bf2c5ed` |
| [TC-GUI-01](results/TC-GUI-01.md) | GUI アクションバー | ✅ PASS | 2026-05-18 | `bf2c5ed` |
| [TC-GUI-02](results/TC-GUI-02.md) | GUI タイトル画面 動的デコレータ | ✅ PASS | 2026-05-18 | `afd7d25` |
| [TC-SIGN-01](results/TC-SIGN-01.md) | 看板 リッチテキスト | ✅ PASS | 2026-05-17 | `836952c` |
| [TC-SIGN-02](results/TC-SIGN-02.md) | 看板 スプライト | ✅ PASS | 2026-05-18 | `bf2c5ed` |
| [TC-TOOLTIP-01](results/TC-TOOLTIP-01.md) | ホバーツールチップ アイテム名リッチテキスト | ✅ PASS | 2026-05-18 | `bf2c5ed` |

---

## スクリーンショット

### ✅ TC-AUTO-01 — オートコンプリート デコレータ候補
![TC-AUTO-01](screenshots/TC-AUTO-01.png)
> `#b` 入力後に `#bold` 等のデコレータ候補ドロップダウンが表示されている。`#` 単体では使用履歴が空の場合に候補なし（将来改善候補）。

### ✅ TC-AUTO-02 — オートコンプリート ショートコード候補
![TC-AUTO-02](screenshots/TC-AUTO-02.png)
> `:di` 入力後に `:disappointed:` 等の候補が表示されている。

### ✅ TC-AUTO-03 — オートコンプリート デコレータ引数候補
![TC-AUTO-03](screenshots/TC-AUTO-03.png)
> `#color.` 入力後に `red`, `gold` 等の色名候補が各色プレビュー付きで表示されている。

### ✅ TC-BOOK-01 — Written Book リッチテキスト
![TC-BOOK-01](screenshots/TC-BOOK-01.png)
> `#bold[Chapter 1]` が太字で描画されている。`MixinWrittenBookAccess` を Mojang マッピング名に修正後に PASS。

### ✅ TC-CHAT-01 — チャット ショートコード（アイテムスプライト）
![TC-CHAT-01](screenshots/TC-CHAT-01.png)
> `:item.diamond:` がダイヤモンドのスプライトアイコンとして描画されている。ImageGlyphPool.bake() up=0, down=h 修正後に "Hello" テキストと同じ高さに正しく揃っていることを確認。

### ✅ TC-CHAT-02 — チャット ショートコード（ブロックスプライト）
![TC-CHAT-02](screenshots/TC-CHAT-02.png)
> `:block.stone:` が石ブロックのスプライトアイコンとして描画されている。ImageGlyphPool.bake() up=0, down=h 修正後に "Hello" テキストと同じ高さに正しく揃っていることを確認。

### ✅ TC-CHAT-04 — チャット デコレータ #bold
![TC-CHAT-04](screenshots/TC-CHAT-04.png)
> `#bold[Hello]` が太字で描画されている。

### ✅ TC-CHAT-05 — チャット デコレータ #italic
![TC-CHAT-05](screenshots/TC-CHAT-05.png)
> `#italic[Hello World]` が斜体で描画されている。

### ✅ TC-CHAT-06 — チャット デコレータ #color
![TC-CHAT-06](screenshots/TC-CHAT-06.png)
> `#color.red[Hello]` が赤色で描画されている。

### ✅ TC-CHAT-08 — チャット デコレータ #glow
![TC-CHAT-08](screenshots/TC-CHAT-08.png)
> `#glow[Glowing]` で発光エフェクト付きテキストが描画されている。

### ✅ TC-CHAT-09 — チャット デコレータ #rainbow
![TC-CHAT-09](screenshots/TC-CHAT-09.png)
> `#rainbow[Rainbow]` が虹色グラデーションで描画されている。

### ✅ TC-CHAT-10 — チャット デコレータ #underline #strike
![TC-CHAT-10](screenshots/TC-CHAT-10.png)
> `#underline[Under] #strike[Strike]` で下線・取り消し線付きテキストが描画されている。

### ✅ TC-CHAT-11 — チャット デコレータ ネスト
![TC-CHAT-11](screenshots/TC-CHAT-11.png)
> `#bold[#color.red[Bold Red]]` が太字かつ赤色で描画されている。

### ✅ TC-CHAT-12 — チャット エスケープシーケンス
![TC-CHAT-12](screenshots/TC-CHAT-12.png)
> `\#bold` が `#bold` というリテラルテキストとして（書式なしで）描画されている。

### ✅ TC-CHAT-13 — チャット 複合表現
![TC-CHAT-13](screenshots/TC-CHAT-13.png)
> `:item.diamond:` アイコン + 太字 Diamond + アクア x64 が混在表示されている。

### ✅ TC-ENTITY-01 — エンティティ名前タグ リッチテキスト
![TC-ENTITY-01](screenshots/TC-ENTITY-01.png)
> 左 Cow: `#bold[Bold]`（太字）、右 Cow: `Normal`（通常）。`MixinFont.runicink$transformDrawBatchComponent` 正常動作確認。

### ✅ TC-GRAFFITI-01 — Graffiti ブロック リッチテキスト基本
![TC-GRAFFITI-01](screenshots/TC-GRAFFITI-01.png)
> `#rainbow[Graffiti]` が虹色で、`:item.diamond:` がスプライトアイコンとして描画されている。`GraffitiRenderer` + `RichNode.toSequence()` 経路確認。

### ✅ TC-GRAFFITI-02 — Graffiti ブロック glow モード（暗所発光）
![TC-GRAFFITI-02](screenshots/TC-GRAFFITI-02.png)
> 深夜の暗所で `#glow[GLOW]` テキストが最大輝度で発光表示されている。

### ✅ TC-GUI-01 — GUI アクションバー
![TC-GUI-01](screenshots/TC-GUI-01.png)
> `#color.gold[Action Bar]` が画面下部のアクションバーに金色で表示されている。`MixinGuiGraphics` 正常動作確認。

### ✅ TC-GUI-02 — GUI タイトル画面 動的デコレータ
![TC-GUI-02](screenshots/TC-GUI-02.png)
> `#rainbow[Title]` が画面中央に時間依存カラーアニメーションで表示されている。
MixinGuiGraphics を常に DynamicFormattedCharSequence でラップするよう修正後に PASS。
複数フレームでシアン→ピンクへの色変化を確認し、DynamicFCS による毎フレーム再評価が機能していることを証明。

### ✅ TC-SIGN-01 — 看板 リッチテキスト
![TC-SIGN-01](screenshots/TC-SIGN-01.png)
> `#color.gold[Hello Sign]` が金色で描画されている。`MixinSignText` 正常動作確認。

### ✅ TC-SIGN-02 — 看板 スプライト
![TC-SIGN-02](screenshots/TC-SIGN-02.png)
> 看板に `:item.diamond:` のスプライトアイコンが描画されている。

### ✅ TC-TOOLTIP-01 — ホバーツールチップ アイテム名リッチテキスト
![TC-TOOLTIP-01](screenshots/TC-TOOLTIP-01.png)
> ホバーツールチップのアイテム名に `#bold[#color.aqua[Diamond]]` が太字アクア色で表示されている。

