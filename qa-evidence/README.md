# QA Evidence

ローダー別にテスト証拠を管理するディレクトリ。

## 構成

```
qa-evidence/
├── forge/           ← Forge ビルドの QA 結果
│   ├── results/     ← TC-*.md 個別テスト結果
│   ├── screenshots/ ← 最新スクリーンショット
│   └── report.md    ← 自動生成サマリー
├── neoforge/        ← NeoForge ビルドの QA 結果
│   ├── results/
│   ├── screenshots/
│   └── report.md
└── coverage-matrix.md ← 表示位置×機能 カバレッジマトリックス（Forge 基準）
```

## report.md の生成

```bash
python scripts/qa/gen_report.py --loader forge
python scripts/qa/gen_report.py --loader neoforge
```

## Forge 実績（2026-05-17〜18）

PASS=80 / FAIL=1 / CONDITIONAL=9 / N/A=7 — 詳細: [forge/report.md](forge/report.md)
