#!/usr/bin/env python3
"""
gen_report.py — qa-evidence/{loader}/results/TC-*.md から report.md と coverage-matrix.md を自動生成する。

Usage:
  python scripts/qa/gen_report.py                  # デフォルト: forge
  python scripts/qa/gen_report.py --loader forge
  python scripts/qa/gen_report.py --loader neoforge
"""

import argparse
import re
import sys
from pathlib import Path
from datetime import datetime

_parser = argparse.ArgumentParser()
_parser.add_argument("--loader", default="forge", choices=["forge", "neoforge"])
_args, _ = _parser.parse_known_args()

LOADER         = _args.loader
RESULTS_DIR    = Path(f"qa-evidence/{LOADER}/results")
REPORT_PATH    = Path(f"qa-evidence/{LOADER}/report.md")
MATRIX_PATH    = Path("qa-evidence/coverage-matrix.md")  # 共有

ICONS = {"PASS": "✅", "FAIL": "❌", "CONDITIONAL": "⚠", "N/A": "—"}

# マトリックスの軸定義
LOCATIONS = [
    ("chat",       "チャット"),
    ("sign",       "看板"),
    ("entity",     "エンティティ名タグ"),
    ("book",       "本 (Written Book)"),
    ("graffiti",   "Graffiti ブロック"),
    ("actionbar",  "アクションバー"),
    ("title",      "タイトル / サブタイトル"),
    ("hotbar",     "ホットバーアイテム名"),
    ("tooltip",    "ホバーツールチップ"),
]
CONTENT_TYPES = [
    ("sprite",      "sprite\n:item: :block:"),
    ("player_head", "player\nhead"),
    ("bold_italic", "#bold\n#italic"),
    ("color",       "#color"),
    ("size",        "#size"),
    ("glow",        "#glow"),
    ("rainbow",     "#rainbow\n（動的）"),
    ("decoration",  "#underline\n#strike"),
    ("nest",        "ネスト\n複合"),
    ("escape",      "エスケープ\n\\#"),
]

RESULT_PRIORITY = {"PASS": 3, "FAIL": 2, "CONDITIONAL": 1, "N/A": 0}


def parse_frontmatter(text):
    m = re.match(r"^---\n(.*?)\n---\n?", text, re.DOTALL)
    if not m:
        return {}, text
    fm = {}
    for line in m.group(1).splitlines():
        if ":" in line:
            k, _, v = line.partition(":")
            fm[k.strip()] = v.strip()
    return fm, text[m.end():].strip()


def merge_result(current, new):
    """優先度の高い結果を採用する（PASS > FAIL > CONDITIONAL > N/A > ?）"""
    if current == "?":
        return new
    cp = RESULT_PRIORITY.get(current, -1)
    np = RESULT_PRIORITY.get(new, -1)
    return current if cp >= np else new


def main():
    if not RESULTS_DIR.exists():
        print(f"ERROR: {RESULTS_DIR} not found", file=sys.stderr)
        sys.exit(1)

    records = []
    for path in sorted(RESULTS_DIR.glob("TC-*.md")):
        text = path.read_text(encoding="utf-8")
        fm, body = parse_frontmatter(text)
        if not fm:
            continue
        fm["_body"] = body
        fm["_file"] = path.stem
        records.append(fm)

    # ── report.md ────────────────────────────────────────────────────────────
    counts = {k: 0 for k in ICONS}
    for r in records:
        res = r.get("result", "").upper()
        if res in counts:
            counts[res] += 1

    now = datetime.now().strftime("%Y-%m-%d %H:%M")
    total = len(records)

    lines = [
        "# QA Report",
        "",
        f"> 自動生成: {now} — `python scripts/qa/gen_report.py`",
        "",
        f"**合計**: {total}件 / "
        + " / ".join(f"{ICONS[k]} {k}: {counts[k]}件" for k in ICONS if counts[k] > 0),
        "",
        "| テストケース | 機能 | 結果 | 最終実行 | コミット |",
        "|---|---|---|---|---|",
    ]

    for r in records:
        tc = r.get("test", r["_file"])
        feature = r.get("feature", "")
        result = r.get("result", "?").upper()
        icon = ICONS.get(result, "?")
        date = r.get("date", "?")
        commit = r.get("commit", "")[:7] or "?"
        lines.append(f"| [{tc}](results/{r['_file']}.md) | {feature} | {icon} {result} | {date} | `{commit}` |")

    with_ss = [r for r in records if r.get("screenshot")]
    if with_ss:
        lines += ["", "---", "", "## スクリーンショット", ""]
        for r in with_ss:
            tc = r.get("test", r["_file"])
            feature = r.get("feature", "")
            result = r.get("result", "?").upper()
            icon = ICONS.get(result, "?")
            ss = r["screenshot"]
            lines += [
                f"### {icon} {tc} — {feature}",
                f"![{tc}]({ss})",
                f"> {r['_body']}",
                "",
            ]

    fails = [r for r in records if r.get("result", "").upper() == "FAIL"]
    if fails:
        lines += ["---", "", "## FAIL 未解決", ""]
        for r in fails:
            tc = r.get("test", r["_file"])
            lines += [f"### {tc}", "", r["_body"], ""]

    REPORT_PATH.write_text("\n".join(lines) + "\n", encoding="utf-8")

    # ── coverage-matrix.md ────────────────────────────────────────────────────
    # (location, content) → result のマップを構築
    cell = {}
    for r in records:
        loc = r.get("location", "").strip()
        contents_raw = r.get("content", "").strip()
        result = r.get("result", "?").upper()
        if not loc or not contents_raw:
            continue
        for content in [c.strip() for c in contents_raw.split(",")]:
            key = (loc, content)
            cell[key] = merge_result(cell.get(key, "?"), result)

    content_keys = [c for c, _ in CONTENT_TYPES]
    header_row = "| 表示位置 | " + " | ".join(
        label.replace("\n", "<br>") for _, label in CONTENT_TYPES
    ) + " |"
    sep_row = "|---" + "|:---:" * len(CONTENT_TYPES) + "|"

    mx_lines = [
        "# RunicInk QA カバレッジマトリックス",
        "",
        f"> 自動生成: {now} — `python scripts/qa/gen_report.py`",
        "",
        "**凡例**: ✅ PASS / ❌ FAIL / ⚠ CONDITIONAL / — 非対応（設計上） / ? 未確認",
        "",
        header_row,
        sep_row,
    ]

    for loc_key, loc_label in LOCATIONS:
        cells = []
        for c_key in content_keys:
            key = (loc_key, c_key)
            val = cell.get(key, "?")
            if val == "N/A":
                cells.append("—")
            else:
                cells.append(ICONS.get(val, "?"))
        mx_lines.append(f"| **{loc_label}** | " + " | ".join(cells) + " |")

    # 備考
    mx_lines += [
        "",
        "## 備考",
        "",
        "- **#size**: チャットと**ホバーツールチップ**のみ無効（チャット: `toComponent()` を先に呼ぶため構文消失。ツールチップ: アイテム名が別経路で描画され DynamicFCS をバイパス）。ホットバー・GUI・看板・Graffiti・エンティティ名タグでは有効。本は `font.split()` 経路のため非対応。",
        "- **#glow（2D GUI）**: ホットバー・アクションバー・タイトルでは `packedLight` の概念がないため非対応（設計上、N/A）。看板・エンティティ名前タグ・Graffiti の 3D ワールド描画コンテキストでのみ有効。",
        "- **エスケープ `\\#`**: RunicInk パーサーは正常動作（チャットで PASS 確認済み）。MC 1.21.1 コマンドの JSON パーサーが `\\#` を拒否するため、コマンドベースの自動テストが不可。手入力（チャット・サインエディタ等）では正常動作。",
        "- **本**: `Sized` / `Glowing` / 動的デコレータは `font.split()` 経路のため非対応（設計上）。",
        "- **player head オフライン**: グリフ確保は動作、スキンテクスチャはネット接続が必要。",
    ]

    # 未テストの優先候補を列挙
    untested = [(loc, c) for (loc, c), v in cell.items() if v == "?" ]
    not_in_cell = [
        (loc, c) for loc, _ in LOCATIONS for c in content_keys
        if cell.get((loc, c), "?") == "?"
    ]
    if not_in_cell:
        mx_lines += ["", "## 優先確認候補 (?)", ""]
        for loc, c in not_in_cell:
            loc_label = next((l for k, l in LOCATIONS if k == loc), loc)
            c_label = next((l.replace("\n", " ") for k, l in CONTENT_TYPES if k == c), c)
            mx_lines.append(f"- {loc_label} × {c_label}")

    MATRIX_PATH.write_text("\n".join(mx_lines) + "\n", encoding="utf-8")

    print(f"Generated {REPORT_PATH}  ({total} tests: "
          + ", ".join(f"{k}={counts[k]}" for k in ICONS if counts[k] > 0) + ")")
    tested = sum(1 for v in cell.values() if v != "?")
    total_cells = len(LOCATIONS) * len(content_keys)
    print(f"Generated {MATRIX_PATH}  ({tested}/{total_cells} cells tested)")


if __name__ == "__main__":
    main()
