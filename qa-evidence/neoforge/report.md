# QA Report

> 自動生成: 2026-05-19 09:58 — `python scripts/qa/gen_report.py`

**合計**: 24件 / ✅ PASS: 9件 / ❌ FAIL: 1件 / ⚠ CONDITIONAL: 8件 / — N/A: 6件

| テストケース | 機能 | 結果 | 最終実行 | コミット |
|---|---|---|---|---|
| [TC-NEO-ACTIONBAR-GLOW-NA](results/TC-NEO-ACTIONBAR-GLOW-NA.md) | アクションバー #glow（非対応） | — N/A | 2026-05-19 | `65c495c` |
| [TC-NEO-ACTIONBAR-PLAYER-HEAD](results/TC-NEO-ACTIONBAR-PLAYER-HEAD.md) | アクションバー player head（オフライン） | ⚠ CONDITIONAL | 2026-05-19 | `65c495c` |
| [TC-NEO-BOOK-GLOW-NA](results/TC-NEO-BOOK-GLOW-NA.md) | 本 #glow（非対応） | — N/A | 2026-05-19 | `65c495c` |
| [TC-NEO-BOOK-PLAYER-HEAD](results/TC-NEO-BOOK-PLAYER-HEAD.md) | 本 player head（オフライン） | ⚠ CONDITIONAL | 2026-05-19 | `65c495c` |
| [TC-NEO-BOOK-RAINBOW-NA](results/TC-NEO-BOOK-RAINBOW-NA.md) | 本 #rainbow（非対応） | — N/A | 2026-05-19 | `65c495c` |
| [TC-NEO-BOOK-SIZE-NA](results/TC-NEO-BOOK-SIZE-NA.md) | 本 #size（非対応） | — N/A | 2026-05-19 | `65c495c` |
| [TC-NEO-CHAT-01](results/TC-NEO-CHAT-01.md) | チャット ショートコード（アイテムスプライト） | ✅ PASS | 2026-05-19 | `11e4c0e` |
| [TC-NEO-CHAT-02](results/TC-NEO-CHAT-02.md) | チャット デコレータ #bold | ✅ PASS | 2026-05-19 | `11e4c0e` |
| [TC-NEO-CHAT-03](results/TC-NEO-CHAT-03.md) | チャット デコレータ #rainbow | ✅ PASS | 2026-05-19 | `11e4c0e` |
| [TC-NEO-CHAT-SIZE](results/TC-NEO-CHAT-SIZE.md) | チャット #size（リグレッション） | ❌ FAIL | 2026-05-19 | `65c495c` |
| [TC-NEO-ENTITY-01](results/TC-NEO-ENTITY-01.md) | エンティティ名前タグ | ✅ PASS | 2026-05-19 | `11e4c0e` |
| [TC-NEO-ENTITY-PLAYER-HEAD](results/TC-NEO-ENTITY-PLAYER-HEAD.md) | エンティティ名タグ player head（オフライン） | ⚠ CONDITIONAL | 2026-05-19 | `65c495c` |
| [TC-NEO-GRAFFITI-01](results/TC-NEO-GRAFFITI-01.md) | Graffiti ブロック リッチテキスト描画（NeoForge） | ✅ PASS | 2026-05-19 | `6cb9c51` |
| [TC-NEO-GRAFFITI-02](results/TC-NEO-GRAFFITI-02.md) | GraffitiEditScreen（エディタ）表示確認（NeoForge） | ✅ PASS | 2026-05-19 | `6cb9c51` |
| [TC-NEO-GRAFFITI-PLAYER-HEAD](results/TC-NEO-GRAFFITI-PLAYER-HEAD.md) | Graffiti ブロック player head（オフライン） | ⚠ CONDITIONAL | 2026-05-19 | `65c495c` |
| [TC-NEO-GUI-01](results/TC-NEO-GUI-01.md) | アクションバー GUI テキスト | ✅ PASS | 2026-05-19 | `11e4c0e` |
| [TC-NEO-HOTBAR-GLOW-NA](results/TC-NEO-HOTBAR-GLOW-NA.md) | ホットバー #glow（非対応） | — N/A | 2026-05-19 | `65c495c` |
| [TC-NEO-HOTBAR-PLAYER-HEAD](results/TC-NEO-HOTBAR-PLAYER-HEAD.md) | ホットバーアイテム名 player head（オフライン） | ⚠ CONDITIONAL | 2026-05-19 | `65c495c` |
| [TC-NEO-SIGN-01](results/TC-NEO-SIGN-01.md) | 看板 リッチテキスト | ✅ PASS | 2026-05-19 | `11e4c0e` |
| [TC-NEO-SIGN-PLAYER-HEAD](results/TC-NEO-SIGN-PLAYER-HEAD.md) | 看板 player head（オフライン） | ⚠ CONDITIONAL | 2026-05-19 | `65c495c` |
| [TC-NEO-TITLE-GLOW-NA](results/TC-NEO-TITLE-GLOW-NA.md) | タイトル #glow（非対応） | — N/A | 2026-05-19 | `65c495c` |
| [TC-NEO-TITLE-PLAYER-HEAD](results/TC-NEO-TITLE-PLAYER-HEAD.md) | タイトル player head（オフライン） | ⚠ CONDITIONAL | 2026-05-19 | `65c495c` |
| [TC-NEO-TOOLTIP-01](results/TC-NEO-TOOLTIP-01.md) | アイテム名表示（ホットバー GUI） | ✅ PASS | 2026-05-19 | `11e4c0e` |
| [TC-NEO-TOOLTIP-PLAYER-HEAD](results/TC-NEO-TOOLTIP-PLAYER-HEAD.md) | ホバーツールチップ player head（オフライン） | ⚠ CONDITIONAL | 2026-05-19 | `65c495c` |

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

### ✅ TC-NEO-GRAFFITI-01 — Graffiti ブロック リッチテキスト描画（NeoForge）
![TC-NEO-GRAFFITI-01](screenshots/TC-NEO-GRAFFITI-01.png)
> NeoForge 版で Graffiti ブロックに #rainbow[NeoGraffiti] と :item.diamond: が正しく描画されている。
/setblock と /data merge でブロックを設置し、テキストが "NeoGraffiti"（多色）と
ダイヤアイコン（水色の点）として描画されることを確認した。

GraffitiRenderer が NeoForge 版でも正常に動作している。

### ✅ TC-NEO-GRAFFITI-02 — GraffitiEditScreen（エディタ）表示確認（NeoForge）
![TC-NEO-GRAFFITI-02](screenshots/TC-NEO-GRAFFITI-02.png)
> NeoForge 版で既存 Graffiti ブロックを右クリックすると GraffitiEditScreen が正常に開く。
エディタには "Edit Graffiti" タイトル、テキストフィールド（#rainbow[NeoGraffiti] / #bold[:item.diamond:]）、
"Align: Left" と "Done" ボタンが表示されている。

GraffitiUpdatePacket の NeoForge 実装コードレビュー:
- neoforge/src/main/java/com/uqlism/emoji_deco/EmojiDeco.java:
  RegisterPayloadHandlersEvent で GraffitiUpdatePacket.TYPE が playToServer として登録されている
- neoforge/src/main/java/com/uqlism/emoji_deco/platform/NeoForgeNetworkBridge.java:
  PacketDistributor.sendToServer(packet) でクライアント→サーバー送信が実装されている
- neoforge/src/main/java/com/uqlism/emoji_deco/network/GraffitiUpdatePacket.java:
  handle() でプレイヤー距離チェック（64ブロック以内）と applyUpdate() 呼び出しが実装されている

Done ボタンのクリックでエディタが閉じること（save() メソッド呼び出し）も確認済み。
クラッシュは発生していない。

### ✅ TC-NEO-GUI-01 — アクションバー GUI テキスト
![TC-NEO-GUI-01](screenshots/TC-NEO-GUI-01.png)
> NeoForge 版で `#rainbow[NeoForge Action]` がアクションバーに緑色のレインボーテキストとして表示されている。/title actionbar コマンドで表示したテキストに #rainbow デコレータが正しく適用されている。

### ✅ TC-NEO-SIGN-01 — 看板 リッチテキスト
![TC-NEO-SIGN-01](screenshots/TC-NEO-SIGN-01.png)
> NeoForge 版で `#color.gold[NeoSign]` が看板に金色のテキストとして描画されている。setblock コマンドで設置した看板の表面に金色の "NeoSign" が正しく表示されている。

### ✅ TC-NEO-TOOLTIP-01 — アイテム名表示（ホットバー GUI）
![TC-NEO-TOOLTIP-01](screenshots/TC-NEO-TOOLTIP-01.png)
> NeoForge 版で `#bold[NeoGem]` がホットバーのアイテム名として太字で表示されている。custom_name コンポーネントで名前を付けたダイヤモンドをホットバーで選択すると、"NeoGen" が太字テキストとして画面中央下部に表示されている。

---

## FAIL 未解決

### TC-NEO-CHAT-SIZE

1.21.1 で MixinChatComponent が toComponent() を先に呼ぶためスケール情報が失われる。Forge 版と同一の既知バグ。

