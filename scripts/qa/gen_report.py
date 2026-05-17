#!/usr/bin/env python3
"""
gen_report.py — qa-evidence/results/TC-*.md から qa-evidence/report.md を自動生成する。

Usage: python scripts/qa/gen_report.py
"""

import re
import sys
from pathlib import Path
from datetime import datetime

RESULTS_DIR = Path("qa-evidence/results")
SCREENSHOTS_DIR = Path("qa-evidence/screenshots")
REPORT_PATH = Path("qa-evidence/report.md")

ICONS = {"PASS": "✅", "FAIL": "❌", "CONDITIONAL": "⚠", "N/A": "—"}


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
        ss = r.get("screenshot", "")
        ss_link = f" [📷](../{ss})" if ss else ""
        lines.append(f"| [{tc}](results/{r['_file']}.md) | {feature} | {icon} {result}{ss_link} | {date} | `{commit}` |")

    fails = [r for r in records if r.get("result", "").upper() == "FAIL"]
    if fails:
        lines += ["", "---", "", "## ❌ 未解決の FAIL", ""]
        for r in fails:
            tc = r.get("test", r["_file"])
            lines += [f"### {tc}", "", r["_body"], ""]

    REPORT_PATH.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"Generated {REPORT_PATH}  ({total} tests: "
          + ", ".join(f"{k}={counts[k]}" for k in ICONS if counts[k] > 0) + ")")


if __name__ == "__main__":
    main()
