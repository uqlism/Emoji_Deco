# RunicInk 残タスク

## バグ修正

- [ ] **`MixinWrittenBookAccess` — 書籍リッチテキスト未対応**
  1.21.1 で `BookViewScreen$WrittenBookAccess` が削除されたため Mixin ターゲットが見つからない。
  1.21.1 での代替クラス（`BookViewScreen` 内の書籍ページ取得経路）を調査して差し替える。
  現状は `require=0` なのでクラッシュしないが書籍では一切変換が効かない。

- [ ] **`mc_qa.py` のウィンドウ選択** — `wscreenshot` が Minecraft Launcher を優先してしまう
  `QA_WINDOW_TITLE` を毎回指定しなくても "Forge" ウィンドウを優先するようデフォルト挙動を修正する。

## 動作未確認の機能

- [ ] **オートコンプリート** (`#` / `:` トリガー)
  `MixinChatScreen` は適用済みだが、ドロップダウン表示の目視確認ができていない。
  チャット欄に `#` を入力して候補が出るか確認する。

- [ ] **看板のリッチテキスト描画**
  `MixinSignText` は適用済み。実際に `#bold[Hello]` を書いた看板を設置して確認する（TC-SIGN-01）。

- [ ] **エンティティ名タグ**
  `MixinFont.runicink$transformDrawBatchComponent` が 1.21.1 で正しく動くか未確認。
  名前タグ付きのエンティティを召喚してリッチテキストが適用されるか確認する。

- [ ] **GUI（ホットバーアイテム名・ツールチップ）**
  `MixinGuiGraphics` は適用済み。アイテムの表示名に `#bold[...]` を設定して確認する。

## QA テストスイート

- [ ] **TC-CHAT-01〜06 全件実行** — shortcode / decorator / escape / player head
- [ ] **TC-AUTOCOMPLETE-01〜02** — `#` / `:` の候補表示
- [ ] **TC-SIGN-01** — 看板リッチテキスト
- [ ] **TC-GRAFFITI-01** — 落書きブロック（`#rainbow[Graffiti]`）

## 将来課題

- [ ] **書籍（Written Book）リッチテキスト対応** — WrittenBookAccess 修正後
- [ ] **`size` デコレータの Chat/Sign 対応確認** — TC-CHAT-04
- [ ] **player head shortcode の確認** — TC-CHAT-06（`:player.Dev:`）
