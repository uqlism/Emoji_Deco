# RunicInk QA カバレッジマトリックス

> 自動生成: 2026-05-18 01:13 — `python scripts/qa/gen_report.py`

**凡例**: ✅ PASS / ❌ FAIL / ⚠ CONDITIONAL / — 非対応（設計上） / ? 未確認

| 表示位置 | sprite<br>:item: :block: | player<br>head | #bold<br>#italic | #color | #size | #glow | #rainbow<br>（動的） | #underline<br>#strike | ネスト<br>複合 | エスケープ<br>\# |
|---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| **チャット** | ✅ | ⚠ | ✅ | ✅ | ⚠ | ✅ | ✅ | ✅ | ✅ | ✅ |
| **看板** | ✅ | ? | ? | ✅ | ? | ? | ? | ? | ? | ? |
| **エンティティ名タグ** | ? | ? | ✅ | ✅ | ? | ? | ? | ? | ? | ? |
| **本 (Written Book)** | ? | ? | ✅ | ? | — | — | — | ? | ? | ? |
| **Graffiti ブロック** | ✅ | ? | ? | ? | ? | ✅ | ✅ | ? | ? | ? |
| **アクションバー** | ? | ? | ? | ✅ | ? | ? | ? | ? | ? | ? |
| **タイトル / サブタイトル** | ? | ? | ? | ? | ? | ? | ✅ | ? | ? | ? |
| **ホットバーアイテム名** | ? | ? | ? | ? | ? | ? | ? | ? | ? | ? |
| **ホバーツールチップ** | ? | ? | ✅ | ✅ | ? | ? | ? | ? | ? | ? |

## 備考

- **#size**: `toComponent()` 経路（チャット・本・GUI）では `Sized` ノードが無効。看板・Graffiti の `toSequence()` 経路のみ有効。
- **本**: `Sized` / `Glowing` / 動的デコレータは `font.split()` 経路のため非対応（設計上）。
- **player head オフライン**: グリフ確保は動作、スキンテクスチャはネット接続が必要。
- **?**: 未テスト。次の QA 優先候補。

## 優先確認候補 (?)

- 看板 × player head
- 看板 × #bold #italic
- 看板 × #size
- 看板 × #glow
- 看板 × #rainbow （動的）
- 看板 × #underline #strike
- 看板 × ネスト 複合
- 看板 × エスケープ \#
- エンティティ名タグ × sprite :item: :block:
- エンティティ名タグ × player head
- エンティティ名タグ × #size
- エンティティ名タグ × #glow
- エンティティ名タグ × #rainbow （動的）
- エンティティ名タグ × #underline #strike
- エンティティ名タグ × ネスト 複合
- エンティティ名タグ × エスケープ \#
- 本 (Written Book) × sprite :item: :block:
- 本 (Written Book) × player head
- 本 (Written Book) × #color
- 本 (Written Book) × #underline #strike
- 本 (Written Book) × ネスト 複合
- 本 (Written Book) × エスケープ \#
- Graffiti ブロック × player head
- Graffiti ブロック × #bold #italic
- Graffiti ブロック × #color
- Graffiti ブロック × #size
- Graffiti ブロック × #underline #strike
- Graffiti ブロック × ネスト 複合
- Graffiti ブロック × エスケープ \#
- アクションバー × sprite :item: :block:
- アクションバー × player head
- アクションバー × #bold #italic
- アクションバー × #size
- アクションバー × #glow
- アクションバー × #rainbow （動的）
- アクションバー × #underline #strike
- アクションバー × ネスト 複合
- アクションバー × エスケープ \#
- タイトル / サブタイトル × sprite :item: :block:
- タイトル / サブタイトル × player head
- タイトル / サブタイトル × #bold #italic
- タイトル / サブタイトル × #color
- タイトル / サブタイトル × #size
- タイトル / サブタイトル × #glow
- タイトル / サブタイトル × #underline #strike
- タイトル / サブタイトル × ネスト 複合
- タイトル / サブタイトル × エスケープ \#
- ホットバーアイテム名 × sprite :item: :block:
- ホットバーアイテム名 × player head
- ホットバーアイテム名 × #bold #italic
- ホットバーアイテム名 × #color
- ホットバーアイテム名 × #size
- ホットバーアイテム名 × #glow
- ホットバーアイテム名 × #rainbow （動的）
- ホットバーアイテム名 × #underline #strike
- ホットバーアイテム名 × ネスト 複合
- ホットバーアイテム名 × エスケープ \#
- ホバーツールチップ × sprite :item: :block:
- ホバーツールチップ × player head
- ホバーツールチップ × #size
- ホバーツールチップ × #glow
- ホバーツールチップ × #rainbow （動的）
- ホバーツールチップ × #underline #strike
- ホバーツールチップ × ネスト 複合
- ホバーツールチップ × エスケープ \#
