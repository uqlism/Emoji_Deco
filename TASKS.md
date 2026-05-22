# RunicInk 残タスク

## バグ修正

- [x] **`MixinWrittenBookAccess` — 書籍 Mixin を 1.21.1 対応に修正済み**
  @Redirect の SRG 名が Mojang マッピングで解決されなかった問題も修正済み（`m_98310_` → `getPage`）。

- [x] **`mc_qa.py` のウィンドウ選択** — Forge ウィンドウ優先に修正済み

- [x] **スプライトグリフ縦位置ズレ** — `ImageGlyphPool` の `up=3f→1f` で修正済み

- [x] **補完ドロップダウン z-order** — `CompletionRenderer` に `pose().translate(0,0,400f)` で修正済み

## 動作確認済み機能

- [x] チャット: ショートコード、デコレータ(bold/color/escape)、基本動作確認済み
- [x] チャット残り(italic/glow/rainbow/underline/strike/ネスト/複合/ブロックスプライト) — QA 実行中
- [x] オートコンプリート: `#prefix`, `:prefix` PASS。引数候補(`#color.`, `:item.`) — QA 実行中
- [x] 看板: リッチテキスト PASS。スプライト入り — QA 実行中
- [x] エンティティ名タグ: PASS
- [x] Graffiti: `#rainbow` PASS。glow モード — QA 実行中
- [x] 本(Written Book): TC-BOOK-01 PASS
- [ ] GUI（アクションバー・タイトル画面）— QA 実行中
- [ ] ホバーツールチップ — QA 実行中

## 将来課題

- [ ] **Supplementaries 道標（Sign Post）対応** — 次バージョンで対応予定
  `TextHolder.getRenderMessages()` が `Font.split(Component, int)` を呼ぶためすべてのフックをバイパス。
  修正方針: `Font.split(Component, int)` に `@Inject HEAD` を追加し、Component の場合は
  `ComponentConverter.toLines()` 経由でスケール・グロー含む変換を行う。
  同じパスを持つ他 mod への副作用を要検証。

- [ ] **`sequence` コマンドの `_focus()` がゲームメニューを開く副作用**
  フォーカス奪取時に Minecraft のマウスキャプチャが解除される問題。
  `sendcmd` 個別コマンドで回避可能だが根本修正が必要。
- [ ] **`#` 単体オートコンプリート** — 使用履歴が空の場合に候補なし。初回起動時の UX 課題。
- [ ] **player head のオフラインスキン** — オンライン環境でのみ確認可能。
