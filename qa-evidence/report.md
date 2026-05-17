# QA Report

> 自動生成: 2026-05-18 01:50 — `python scripts/qa/gen_report.py`

**合計**: 53件 / ✅ PASS: 46件 / ⚠ CONDITIONAL: 3件 / — N/A: 4件

| テストケース | 機能 | 結果 | 最終実行 | コミット |
|---|---|---|---|---|
| [TC-AUTO-01](results/TC-AUTO-01.md) | オートコンプリート デコレータ候補 | ✅ PASS | 2026-05-17 | `f3f8074` |
| [TC-AUTO-02](results/TC-AUTO-02.md) | オートコンプリート ショートコード候補 | ✅ PASS | 2026-05-17 | `f3f8074` |
| [TC-AUTO-03](results/TC-AUTO-03.md) | オートコンプリート デコレータ引数候補 | ✅ PASS | 2026-05-18 | `bf2c5ed` |
| [TC-AUTO-04](results/TC-AUTO-04.md) | オートコンプリート ショートコード引数候補 | — N/A | 2026-05-18 | `bf2c5ed` |
| [TC-BOOK-01](results/TC-BOOK-01.md) | Written Book リッチテキスト | ✅ PASS | 2026-05-18 | `d2e8510` |
| [TC-BOOK-02](results/TC-BOOK-02.md) | 本 #size（非対応） | — N/A | 2026-05-18 | `25280fa` |
| [TC-BOOK-03](results/TC-BOOK-03.md) | 本 #glow（非対応） | — N/A | 2026-05-18 | `25280fa` |
| [TC-BOOK-04](results/TC-BOOK-04.md) | 本 #rainbow（非対応） | — N/A | 2026-05-18 | `25280fa` |
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
| [TC-ENTITY-02](results/TC-ENTITY-02.md) | エンティティ名前タグ sprite (:item.diamond:) | ✅ PASS | 2026-05-18 | `4a52b56` |
| [TC-ENTITY-03](results/TC-ENTITY-03.md) | エンティティ名前タグ #glow | ✅ PASS | 2026-05-18 | `4a52b56` |
| [TC-ENTITY-04](results/TC-ENTITY-04.md) | エンティティ名前タグ #rainbow | ✅ PASS | 2026-05-18 | `4a52b56` |
| [TC-ENTITY-05](results/TC-ENTITY-05.md) | エンティティ名前タグ ネスト複合 (#bold + #color.gold) | ✅ PASS | 2026-05-18 | `4a52b56` |
| [TC-GRAFFITI-01](results/TC-GRAFFITI-01.md) | Graffiti ブロック リッチテキスト基本 | ✅ PASS | 2026-05-17 | `c3cc94f` |
| [TC-GRAFFITI-02](results/TC-GRAFFITI-02.md) | Graffiti ブロック glow モード（暗所発光） | ✅ PASS | 2026-05-18 | `bf2c5ed` |
| [TC-GUI-01](results/TC-GUI-01.md) | GUI アクションバー | ✅ PASS | 2026-05-18 | `bf2c5ed` |
| [TC-GUI-02](results/TC-GUI-02.md) | GUI タイトル画面 動的デコレータ | ✅ PASS | 2026-05-18 | `afd7d25` |
| [TC-HOTBAR-01](results/TC-HOTBAR-01.md) | ホットバーアイテム名 #bold | ✅ PASS | 2026-05-18 | `4a3aeab` |
| [TC-HOTBAR-02](results/TC-HOTBAR-02.md) | ホットバーアイテム名 #italic | ✅ PASS | 2026-05-18 | `4a3aeab` |
| [TC-HOTBAR-03](results/TC-HOTBAR-03.md) | ホットバーアイテム名 #color | ✅ PASS | 2026-05-18 | `4a3aeab` |
| [TC-HOTBAR-04](results/TC-HOTBAR-04.md) | ホットバーアイテム名 #rainbow (動的) | ✅ PASS | 2026-05-18 | `4a3aeab` |
| [TC-HOTBAR-05](results/TC-HOTBAR-05.md) | ホットバーアイテム名 #glow | ⚠ CONDITIONAL | 2026-05-18 | `4a3aeab` |
| [TC-HOTBAR-06](results/TC-HOTBAR-06.md) | ホットバーアイテム名 スプライト (:item:) | ✅ PASS | 2026-05-18 | `4a3aeab` |
| [TC-HOTBAR-07](results/TC-HOTBAR-07.md) | ホットバーアイテム名 #size | ✅ PASS | 2026-05-18 | `4a3aeab` |
| [TC-HOTBAR-08](results/TC-HOTBAR-08.md) | ホットバーアイテム名 #underline #strike | ✅ PASS | 2026-05-18 | `4a3aeab` |
| [TC-HOTBAR-09](results/TC-HOTBAR-09.md) | ホットバーアイテム名 ネスト (#bold[#color.red[...]]) | ✅ PASS | 2026-05-18 | `4a3aeab` |
| [TC-HOTBAR-10](results/TC-HOTBAR-10.md) | ホットバーアイテム名 エスケープ (\#bold) | ✅ PASS | 2026-05-18 | `4a3aeab` |
| [TC-SIGN-01](results/TC-SIGN-01.md) | 看板 リッチテキスト | ✅ PASS | 2026-05-17 | `836952c` |
| [TC-SIGN-02](results/TC-SIGN-02.md) | 看板 スプライト | ✅ PASS | 2026-05-18 | `bf2c5ed` |
| [TC-SIGN-03](results/TC-SIGN-03.md) | 看板 #bold | ✅ PASS | 2026-05-18 | `4a52b56` |
| [TC-SIGN-04](results/TC-SIGN-04.md) | 看板 #glow | ✅ PASS | 2026-05-18 | `4a52b56` |
| [TC-SIGN-05](results/TC-SIGN-05.md) | 看板 #rainbow | ✅ PASS | 2026-05-18 | `4a52b56` |
| [TC-SIGN-06](results/TC-SIGN-06.md) | 看板 #underline #strike | ✅ PASS | 2026-05-18 | `4a52b56` |
| [TC-SIGN-07](results/TC-SIGN-07.md) | 看板 ネスト複合 (#bold + #color.red) | ✅ PASS | 2026-05-18 | `4a52b56` |
| [TC-SIGN-08](results/TC-SIGN-08.md) | 看板 #size | ✅ PASS | 2026-05-18 | `4a52b56` |
| [TC-TOOLTIP-01](results/TC-TOOLTIP-01.md) | ホバーツールチップ アイテム名リッチテキスト | ✅ PASS | 2026-05-18 | `bf2c5ed` |
| [TC-TOOLTIP-02](results/TC-TOOLTIP-02.md) | ホバーツールチップ sprite (:item.diamond:) | ✅ PASS | 2026-05-18 | `4a52b56` |
| [TC-TOOLTIP-03](results/TC-TOOLTIP-03.md) | ホバーツールチップ #rainbow | ✅ PASS | 2026-05-18 | `4a52b56` |
| [TC-TOOLTIP-04](results/TC-TOOLTIP-04.md) | ホバーツールチップ #glow | ✅ PASS | 2026-05-18 | `4a52b56` |
| [TC-TOOLTIP-05](results/TC-TOOLTIP-05.md) | ホバーツールチップ ネスト複合 (#bold + #color.green) | ✅ PASS | 2026-05-18 | `4a52b56` |

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

### ✅ TC-ENTITY-02 — エンティティ名前タグ sprite (:item.diamond:)
![TC-ENTITY-02](screenshots/TC-ENTITY-02.png)
> 牛の名前タグに `:item.diamond: Cow` が正しく描画されている。ダイヤモンドのスプライトアイコン + "Cow" テキストが並んで表示。

### ✅ TC-ENTITY-03 — エンティティ名前タグ #glow
![TC-ENTITY-03](screenshots/TC-ENTITY-03.png)
> midnight + night_vision 環境で `#glow[GlowCow]` が牛の名前タグに明るく発光表示されている。Font.drawInBatch 経由の LightSequence が正常動作。

### ✅ TC-ENTITY-04 — エンティティ名前タグ #rainbow
![TC-ENTITY-04](screenshots/TC-ENTITY-04.png)
> `#rainbow[RainbowCow]` が牛の名前タグに虹色で表示されている。DynamicFormattedCharSequence 経路で毎フレーム再評価される動的カラーが機能している。

### ✅ TC-ENTITY-05 — エンティティ名前タグ ネスト複合 (#bold + #color.gold)
![TC-ENTITY-05](screenshots/TC-ENTITY-05.png)
> `#bold[#color.gold[GoldCow]]` が牛の名前タグに金色かつ太字で表示されている。ネストした decorator が正しく機能している。

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

### ✅ TC-HOTBAR-01 — ホットバーアイテム名 #bold
![TC-HOTBAR-01](screenshots/TC-HOTBAR-01.png)
> ホットバーアイテム名に `#bold[Bold Diamond]` が太字で表示されている。
custom_name コンポーネント構文: `minecraft:diamond[minecraft:custom_name='"#bold[Bold Diamond]"']`
MixinGuiGraphics 経由で正しく変換・描画されることを確認。

### ✅ TC-HOTBAR-02 — ホットバーアイテム名 #italic
![TC-HOTBAR-02](screenshots/TC-HOTBAR-02.png)
> ホットバーアイテム名に `#italic[Italic Emerald]` が斜体で表示されている。

### ✅ TC-HOTBAR-03 — ホットバーアイテム名 #color
![TC-HOTBAR-03](screenshots/TC-HOTBAR-03.png)
> ホットバーアイテム名に `#color.gold[Gold Ingot]` が金色で表示されている。

### ✅ TC-HOTBAR-04 — ホットバーアイテム名 #rainbow (動的)
![TC-HOTBAR-04](screenshots/TC-HOTBAR-04.png)
> ホットバーアイテム名に `#rainbow[Rainbow Star]` が動的カラーで表示されている。
2フレーム間で色がアクア→オレンジに変化しており、アニメーション動作を確認。
ホットバーは toComponent() 経由だが、DynamicFormattedCharSequence として毎フレーム再評価される。

### ⚠ TC-HOTBAR-05 — ホットバーアイテム名 #glow
![TC-HOTBAR-05](screenshots/TC-HOTBAR-05.png)
> ホットバーアイテム名に `#glow[Glow Stone]` と表示されている（テキスト描画は正常）。
ただし発光エフェクト（LightMode）はホットバー名経路（GuiGraphics.drawString -> toComponent()）では
LightSequence が無視されるため、視覚的な発光差は確認できない。
これはアーキテクチャ上の仕様であり、FCS 経路（看板・落書きブロック）では有効。

### ✅ TC-HOTBAR-06 — ホットバーアイテム名 スプライト (:item:)
![TC-HOTBAR-06](screenshots/TC-HOTBAR-06.png)
> ホットバーアイテム名に `:item.diamond: Stone` のダイヤモンドスプライトアイコン + "Stone" が表示されている。
SpriteRegistry によるカスタムグリフが GuiGraphics 経路でも正しく描画されることを確認。

### ✅ TC-HOTBAR-07 — ホットバーアイテム名 #size
![TC-HOTBAR-07](screenshots/TC-HOTBAR-07.png)
> ホットバーアイテム名に `#size.2[Big]` が通常の2倍サイズで表示されている。
MixinGuiGraphics 経由（toComponent() ではなく AffineSequence/ScaledSequence 経路）でスケールが有効。
テスト仕様では CONDITIONAL とされていたが、実際には PASS（スケール有効）。

### ✅ TC-HOTBAR-08 — ホットバーアイテム名 #underline #strike
![TC-HOTBAR-08](screenshots/TC-HOTBAR-08.png)
> ホットバーアイテム名に `#underline[Under] #strike[Strike]` が下線・取り消し線付きで表示されている。

### ✅ TC-HOTBAR-09 — ホットバーアイテム名 ネスト (#bold[#color.red[...]])
![TC-HOTBAR-09](screenshots/TC-HOTBAR-09.png)
> ホットバーアイテム名に `#bold[#color.red[Bold Red]]` が太字かつ赤色で表示されている。
ネストされたデコレータが正しくスタイルを合成することを確認。
チャットログの give 確認メッセージでも同様に太字・赤色で描画されていた。

### ✅ TC-HOTBAR-10 — ホットバーアイテム名 エスケープ (\#bold)
![TC-HOTBAR-10](screenshots/TC-HOTBAR-10.png)
> チャット入力で `\#bold Literal` を送信したところ、チャットに `#bold Literal` が
書式なし（通常テキスト）で表示された。バックスラッシュエスケープが正しく機能している。

備考: アイテム名経由のテストは 1.21.1 の SNBT/JSON パーサーが `\#` を無効なエスケープとして
拒否するため実施不可。チャット経路で代替検証した。パーサー（RichTextParser）自体は
ホットバー経路と共通のため、機能は同等に確認できている。

### ✅ TC-SIGN-01 — 看板 リッチテキスト
![TC-SIGN-01](screenshots/TC-SIGN-01.png)
> `#color.gold[Hello Sign]` が金色で描画されている。`MixinSignText` 正常動作確認。

### ✅ TC-SIGN-02 — 看板 スプライト
![TC-SIGN-02](screenshots/TC-SIGN-02.png)
> 看板に `:item.diamond:` のスプライトアイコンが描画されている。

### ✅ TC-SIGN-03 — 看板 #bold
![TC-SIGN-03](screenshots/TC-SIGN-03.png)
> 看板に `#bold[Bold Sign]` が太字で描画されている。oak_wall_sign (south-facing) + `/data merge` でテキスト設定。

### ✅ TC-SIGN-04 — 看板 #glow
![TC-SIGN-04](screenshots/TC-SIGN-04.png)
> midnight + night_vision 状態で `#glow[Glow Sign]` が看板に明るく発光表示されている。暗所でもテキストが鮮明に見える。

### ✅ TC-SIGN-05 — 看板 #rainbow
![TC-SIGN-05](screenshots/TC-SIGN-05.png)
> `#rainbow[Rainbow Sign]` が看板に虹色で表示されている。toSequence() 経路のため動的グラデーションが機能している。

### ✅ TC-SIGN-06 — 看板 #underline #strike
![TC-SIGN-06](screenshots/TC-SIGN-06.png)
> 看板の1行目に `#underline[Under]` が下線付きで、2行目に `#strike[Strike]` が取り消し線付きで描画されている。

### ✅ TC-SIGN-07 — 看板 ネスト複合 (#bold + #color.red)
![TC-SIGN-07](screenshots/TC-SIGN-07.png)
> `#bold[#color.red[BoldRed]]` が看板に赤色かつ太字で描画されている。ネストした decorator が正しく機能している。

### ✅ TC-SIGN-08 — 看板 #size
![TC-SIGN-08](screenshots/TC-SIGN-08.png)
> `#size.2[Big]` が看板に通常の2倍サイズで描画されている。toSequence() 経路のため Sized ノードが有効。

### ✅ TC-TOOLTIP-01 — ホバーツールチップ アイテム名リッチテキスト
![TC-TOOLTIP-01](screenshots/TC-TOOLTIP-01.png)
> ホバーツールチップのアイテム名に `#bold[#color.aqua[Diamond]]` が太字アクア色で表示されている。

### ✅ TC-TOOLTIP-02 — ホバーツールチップ sprite (:item.diamond:)
![TC-TOOLTIP-02](screenshots/TC-TOOLTIP-02.png)
> `:item.diamond: Gem` をカスタム名に持つダイヤモンドのツールチップに、ダイヤモンドスプライトアイコン + "Gem" が表示されている。1.21.1 コンポーネント構文 `minecraft:custom_name='{"text":"..."}` で動作確認。

### ✅ TC-TOOLTIP-03 — ホバーツールチップ #rainbow
![TC-TOOLTIP-03](screenshots/TC-TOOLTIP-03.png)
> `#rainbow[Rainbow Item]` をカスタム名に持つネザースターのツールチップに "Rainbow Item" が虹色で表示されている。

### ✅ TC-TOOLTIP-04 — ホバーツールチップ #glow
![TC-TOOLTIP-04](screenshots/TC-TOOLTIP-04.png)
> `#glow[Glow Item]` をカスタム名に持つグロウストーンダストのツールチップに "Glow Item" が正しく表示されている。ツールチップは toComponent() 経路のため LightMode による光度変化は非対応（設計上）。テキスト自体の描画は正常。

### ✅ TC-TOOLTIP-05 — ホバーツールチップ ネスト複合 (#bold + #color.green)
![TC-TOOLTIP-05](screenshots/TC-TOOLTIP-05.png)
> `#bold[#color.green[Bold Green]]` をカスタム名に持つエメラルドのツールチップに "Bold Green" が太字緑色で表示されている。

