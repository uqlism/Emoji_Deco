# QA Report

> 自動生成: 2026-05-19 08:57 — `python scripts/qa/gen_report.py`

**合計**: 7件 / ✅ PASS: 7件

| テストケース | 機能 | 結果 | 最終実行 | コミット |
|---|---|---|---|---|
| [TC-NEO-CHAT-01](results/TC-NEO-CHAT-01.md) | チャット ショートコード（アイテムスプライト） | ✅ PASS | 2026-05-19 | `11e4c0e` |
| [TC-NEO-CHAT-02](results/TC-NEO-CHAT-02.md) | チャット デコレータ #bold | ✅ PASS | 2026-05-19 | `11e4c0e` |
| [TC-NEO-CHAT-03](results/TC-NEO-CHAT-03.md) | チャット デコレータ #rainbow | ✅ PASS | 2026-05-19 | `11e4c0e` |
| [TC-NEO-ENTITY-01](results/TC-NEO-ENTITY-01.md) | エンティティ名前タグ | ✅ PASS | 2026-05-19 | `11e4c0e` |
| [TC-NEO-GUI-01](results/TC-NEO-GUI-01.md) | アクションバー GUI テキスト | ✅ PASS | 2026-05-19 | `11e4c0e` |
| [TC-NEO-SIGN-01](results/TC-NEO-SIGN-01.md) | 看板 リッチテキスト | ✅ PASS | 2026-05-19 | `11e4c0e` |
| [TC-NEO-TOOLTIP-01](results/TC-NEO-TOOLTIP-01.md) | アイテム名表示（ホットバー GUI） | ✅ PASS | 2026-05-19 | `11e4c0e` |

---

## スクリーンショット

### ✅ TC-NEO-CHAT-01 — チャット ショートコード（アイテムスプライト）
![TC-NEO-CHAT-01](screenshots/TC-NEO-CHAT-01.png)
> NeoForge 版で `:item.diamond:` がスプライトアイコンとして描画されている。チャット欄に `<Dev>` の後にダイヤモンドのスプライトアイコンが表示され、`:item.diamond:` という文字列はそのまま表示されていない。

### ✅ TC-NEO-CHAT-02 — チャット デコレータ #bold
![TC-NEO-CHAT-02](screenshots/TC-NEO-CHAT-02.png)
> NeoForge 版で `#bold[NeoForge Test]` が太字の "NeoForge Test" としてチャットに表示されている。

### ✅ TC-NEO-CHAT-03 — チャット デコレータ #rainbow
![TC-NEO-CHAT-03](screenshots/TC-NEO-CHAT-03.png)
> NeoForge 版で `#rainbow[Rainbow NeoForge]` が色付きテキストとして表示されている。オレンジ/緑系の色でレインボーアニメーションが適用されている。

### ✅ TC-NEO-ENTITY-01 — エンティティ名前タグ
![TC-NEO-ENTITY-01](screenshots/TC-NEO-ENTITY-01.png)
> NeoForge 版で `#bold[#color.aqua[NeoCow]]` が牛エンティティの名前タグにシアン色の太字テキストとして描画されている。summon コマンドで召喚した牛の頭上に aqua 色の "NeoCow" が正しく表示されている。

### ✅ TC-NEO-GUI-01 — アクションバー GUI テキスト
![TC-NEO-GUI-01](screenshots/TC-NEO-GUI-01.png)
> NeoForge 版で `#rainbow[NeoForge Action]` がアクションバーに緑色のレインボーテキストとして表示されている。/title actionbar コマンドで表示したテキストに #rainbow デコレータが正しく適用されている。

### ✅ TC-NEO-SIGN-01 — 看板 リッチテキスト
![TC-NEO-SIGN-01](screenshots/TC-NEO-SIGN-01.png)
> NeoForge 版で `#color.gold[NeoSign]` が看板に金色のテキストとして描画されている。setblock コマンドで設置した看板の表面に金色の "NeoSign" が正しく表示されている。

### ✅ TC-NEO-TOOLTIP-01 — アイテム名表示（ホットバー GUI）
![TC-NEO-TOOLTIP-01](screenshots/TC-NEO-TOOLTIP-01.png)
> NeoForge 版で `#bold[NeoGem]` がホットバーのアイテム名として太字で表示されている。custom_name コンポーネントで名前を付けたダイヤモンドをホットバーで選択すると、"NeoGen" が太字テキストとして画面中央下部に表示されている。

