# RunicInk QA カバレッジマトリックス

> 自動生成: 2026-05-19 09:58 — `python scripts/qa/gen_report.py`

**凡例**: ✅ PASS / ❌ FAIL / ⚠ CONDITIONAL / — 非対応（設計上） / ? 未確認

| 表示位置 | sprite<br>:item: :block: | player<br>head | #bold<br>#italic | #color | #size | #glow | #rainbow<br>（動的） | #underline<br>#strike | ネスト<br>複合 | エスケープ<br>\# |
|---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| **チャット** | ✅ | ? | ? | ? | ❌ | ? | ? | ? | ? | ? |
| **看板** | ? | ⚠ | ? | ? | ? | ? | ? | ? | ? | ? |
| **エンティティ名タグ** | ? | ⚠ | ? | ? | ? | ? | ? | ? | ? | ? |
| **本 (Written Book)** | ? | ⚠ | ? | ? | — | — | — | ? | ? | ? |
| **Graffiti ブロック** | ✅ | ⚠ | ? | ? | ? | ? | ✅ | ? | ? | ? |
| **アクションバー** | ? | ⚠ | ? | ? | ? | — | ? | ? | ? | ? |
| **タイトル / サブタイトル** | ? | ⚠ | ? | ? | ? | — | ? | ? | ? | ? |
| **ホットバーアイテム名** | ? | ⚠ | ? | ? | ? | — | ? | ? | ? | ? |
| **ホバーツールチップ** | ? | ⚠ | ? | ? | ? | ? | ? | ? | ? | ? |

## 備考

- **#size**: チャットと**ホバーツールチップ**のみ無効（チャット: `toComponent()` を先に呼ぶため構文消失。ツールチップ: アイテム名が別経路で描画され DynamicFCS をバイパス）。ホットバー・GUI・看板・Graffiti・エンティティ名タグでは有効。本は `font.split()` 経路のため非対応。
- **#glow（2D GUI）**: ホットバー・アクションバー・タイトルでは `packedLight` の概念がないため非対応（設計上、N/A）。看板・エンティティ名前タグ・Graffiti の 3D ワールド描画コンテキストでのみ有効。
- **エスケープ `\#`**: RunicInk パーサーは正常動作（チャットで PASS 確認済み）。MC 1.21.1 コマンドの JSON パーサーが `\#` を拒否するため、コマンドベースの自動テストが不可。手入力（チャット・サインエディタ等）では正常動作。
- **本**: `Sized` / `Glowing` / 動的デコレータは `font.split()` 経路のため非対応（設計上）。
- **player head オフライン**: グリフ確保は動作、スキンテクスチャはネット接続が必要。

## 優先確認候補 (?)

- チャット × player head
- チャット × #bold #italic
- チャット × #color
- チャット × #glow
- チャット × #rainbow （動的）
- チャット × #underline #strike
- チャット × ネスト 複合
- チャット × エスケープ \#
- 看板 × sprite :item: :block:
- 看板 × #bold #italic
- 看板 × #color
- 看板 × #size
- 看板 × #glow
- 看板 × #rainbow （動的）
- 看板 × #underline #strike
- 看板 × ネスト 複合
- 看板 × エスケープ \#
- エンティティ名タグ × sprite :item: :block:
- エンティティ名タグ × #bold #italic
- エンティティ名タグ × #color
- エンティティ名タグ × #size
- エンティティ名タグ × #glow
- エンティティ名タグ × #rainbow （動的）
- エンティティ名タグ × #underline #strike
- エンティティ名タグ × ネスト 複合
- エンティティ名タグ × エスケープ \#
- 本 (Written Book) × sprite :item: :block:
- 本 (Written Book) × #bold #italic
- 本 (Written Book) × #color
- 本 (Written Book) × #underline #strike
- 本 (Written Book) × ネスト 複合
- 本 (Written Book) × エスケープ \#
- Graffiti ブロック × #bold #italic
- Graffiti ブロック × #color
- Graffiti ブロック × #size
- Graffiti ブロック × #glow
- Graffiti ブロック × #underline #strike
- Graffiti ブロック × ネスト 複合
- Graffiti ブロック × エスケープ \#
- アクションバー × sprite :item: :block:
- アクションバー × #bold #italic
- アクションバー × #color
- アクションバー × #size
- アクションバー × #rainbow （動的）
- アクションバー × #underline #strike
- アクションバー × ネスト 複合
- アクションバー × エスケープ \#
- タイトル / サブタイトル × sprite :item: :block:
- タイトル / サブタイトル × #bold #italic
- タイトル / サブタイトル × #color
- タイトル / サブタイトル × #size
- タイトル / サブタイトル × #rainbow （動的）
- タイトル / サブタイトル × #underline #strike
- タイトル / サブタイトル × ネスト 複合
- タイトル / サブタイトル × エスケープ \#
- ホットバーアイテム名 × sprite :item: :block:
- ホットバーアイテム名 × #bold #italic
- ホットバーアイテム名 × #color
- ホットバーアイテム名 × #size
- ホットバーアイテム名 × #rainbow （動的）
- ホットバーアイテム名 × #underline #strike
- ホットバーアイテム名 × ネスト 複合
- ホットバーアイテム名 × エスケープ \#
- ホバーツールチップ × sprite :item: :block:
- ホバーツールチップ × #bold #italic
- ホバーツールチップ × #color
- ホバーツールチップ × #size
- ホバーツールチップ × #glow
- ホバーツールチップ × #rainbow （動的）
- ホバーツールチップ × #underline #strike
- ホバーツールチップ × ネスト 複合
- ホバーツールチップ × エスケープ \#
