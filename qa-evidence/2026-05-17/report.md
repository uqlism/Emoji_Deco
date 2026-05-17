# QA Evidence — 2026-05-17

**ブランチ**: 1.21.1  
**コミット**: 836952c  
**Minecraft / Forge**: 1.21.1

## 結果サマリー

合計 2件 / ✅ PASS: 2件 / ❌ FAIL: 0件

| # | テストケース | 機能 | 結果 |
|---|---|---|---|
| 1 | TC-SIGN-01 | 看板リッチテキスト (`MixinSignText`) | ✅ PASS |
| 2 | TC-ENTITY-01 | エンティティ名前タグ (`MixinFont`) | ✅ PASS |

## スクリーンショット

### TC-SIGN-01 — 看板リッチテキスト
![TC-SIGN-01](TC-SIGN-01.png)
> `#color.gold[Hello Sign]` が金色で描画されている。raw テキストではなくリッチテキストとして処理されていることを確認。

### TC-ENTITY-01 — エンティティ名前タグ
![TC-ENTITY-01](TC-ENTITY-01.png)
> 左 Cow: `#bold[Bold]`（太字）、右 Cow: `Normal`（通常）。文字の太さの違いが明確で `MixinFont.runicink$transformDrawBatchComponent` が正常に機能していることを確認。
