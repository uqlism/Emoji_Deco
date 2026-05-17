# RunicInk 残タスク

## バグ修正

- [ ] **`MixinWrittenBookAccess` — 書籍リッチテキスト未対応**
  1.21.1 で `BookViewScreen$WrittenBookAccess` が削除されたため Mixin ターゲットが見つからない。
  1.21.1 での代替クラス（`BookViewScreen` 内の書籍ページ取得経路）を調査して差し替える。
  現状は `require=0` なのでクラッシュしないが書籍では一切変換が効かない。

- [x] **`mc_qa.py` のウィンドウ選択** — Forge ウィンドウ優先に修正済み

## 動作未確認の機能

- [x] **オートコンプリート** (`#` / `:` トリガー) — TC-AUTOCOMPLETE-01〜02 PASS
  ⚠ `#` 単体では使用履歴が空の場合に候補が出ない（`#b` 等プレフィックスが必要）。将来改善候補。

- [x] **看板のリッチテキスト描画** — TC-SIGN-01 PASS（`#color.gold[Hello Sign]` 描画確認済み）

- [x] **エンティティ名タグ** — TC-ENTITY-01 PASS（`#bold` / `#color.aqua` 描画確認済み）

- [ ] **GUI（ホットバーアイテム名・ツールチップ）**
  `MixinGuiGraphics` は適用済み。アイテムの表示名に `#bold[...]` を設定して確認する。
  ホットバー切替（`key:1` など）はフォーカス奪取が必要。

## QA テストスイート

- [x] **TC-CHAT-01〜06** — PASS（TC-04 size は Chat 非対応で仕様通り、TC-06 オフライン時スキン空白は想定内）
- [x] **TC-AUTOCOMPLETE-01〜02** — PASS
- [x] **TC-SIGN-01** — PASS
- [ ] **TC-GRAFFITI-01** — 落書きブロック（`#rainbow[Graffiti]`）

## 将来課題

- [ ] **書籍（Written Book）リッチテキスト対応** — MixinWrittenBookAccess 修正済み、QA 未実施
- [ ] **`size` デコレータの Chat/Sign 対応確認** — TC-CHAT-04
- [ ] **player head shortcode の確認** — TC-CHAT-06（`:player.Dev:`）
