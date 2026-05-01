#!/usr/bin/env python3
"""
Generate Twemoji shortcode JSON files for the emoji_deco_starter resource pack.
Fetches emoji-data from iamcal/emoji-data for Discord/Slack-compatible shortcode names.
Validates against the actual Twemoji 14.0.2 file list to avoid 404s caused by
FE0F / leading-zero differences between iamcal and Twemoji naming conventions.

Usage:
    python scripts/generate_twemoji_shortcodes.py
"""

import json
import re
import urllib.request
from pathlib import Path

EMOJI_DATA_URL   = "https://raw.githubusercontent.com/iamcal/emoji-data/master/emoji.json"
TWEMOJI_TREE_URL = "https://api.github.com/repos/twitter/twemoji/git/trees/v14.0.2?recursive=1"
TWEMOJI_CDN_BASE = "https://cdn.jsdelivr.net/gh/twitter/twemoji@14.0.2/assets/72x72"
OUTPUT_DIR       = Path("src/main/resources/resourcepacks/emoji_deco_starter/assets/emoji_deco/shortcodes")

# ResourceLocation パスに使用できる文字: [a-z0-9._-]
_RL_VALID = re.compile(r"^[a-z0-9._-]+$")


def normalize(codepoint_str: str) -> str:
    """FE0F 除去 + 各パートの先頭ゼロ除去で両者を統一表現に変換する。"""
    parts = [p for p in codepoint_str.lower().split("-") if p != "fe0f"]
    return "-".join(p.lstrip("0") or "0" for p in parts)


def fetch_twemoji_map() -> dict:
    """Twemoji 14.0.2 の 72x72 ファイル一覧を取得し {正規化名: 実ファイル名} を返す。"""
    print("Fetching Twemoji 14.0.2 file list from GitHub ...")
    req = urllib.request.Request(TWEMOJI_TREE_URL, headers={"User-Agent": "Mozilla/5.0"})
    with urllib.request.urlopen(req, timeout=30) as r:
        tree = json.loads(r.read())
    files = {
        item["path"].replace("assets/72x72/", "")
        for item in tree["tree"]
        if item["path"].startswith("assets/72x72/") and item["path"].endswith(".png")
    }
    return {normalize(f[:-4]): f for f in files}


def make_shortcode(url: str, aliases: list) -> dict:
    entry = {
        "enable": True,
        "display": {
            "type": "emoji_deco:image_to_glyph",
            "width": 8,
            "height": 8,
            "image": {
                "type": "emoji_deco:decode_image",
                "source": {
                    "type": "emoji_deco:fetch_url",
                    "disk_cache": True,
                    "url": url,
                }
            }
        }
    }
    if aliases:
        entry["aliases"] = aliases
    return entry


def main():
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

    twemoji_map = fetch_twemoji_map()
    print(f"Twemoji 14.0.2: {len(twemoji_map)} files")

    print("Fetching emoji-data from iamcal/emoji-data ...")
    with urllib.request.urlopen(EMOJI_DATA_URL, timeout=30) as r:
        emoji_data = json.loads(r.read().decode("utf-8"))

    # 既存ファイルを削除してから再生成（stale なファイルを残さない）
    for f in OUTPUT_DIR.glob("*.json"):
        f.unlink()

    print(f"Processing {len(emoji_data)} entries ...")
    generated = skipped = 0

    for emoji in emoji_data:
        if not emoji.get("has_img_twitter", False):
            skipped += 1
            continue

        # Twemoji 14.0.2 に実在するファイル名を正規化照合で取得
        actual_file = twemoji_map.get(normalize(emoji["unified"]))
        if actual_file is None:
            skipped += 1
            continue

        url = f"{TWEMOJI_CDN_BASE}/{actual_file}"

        # ResourceLocation に使用できないプライマリ名は有効なエイリアスに昇格
        all_names = list(dict.fromkeys([emoji["short_name"]] + emoji.get("short_names", [])))
        primary = next((n for n in all_names if _RL_VALID.match(n)), None)
        if primary is None:
            skipped += 1
            continue
        aliases = [n for n in all_names if n != primary]

        output_path = OUTPUT_DIR / f"{primary}.json"
        with open(output_path, "w", encoding="utf-8") as f:
            json.dump(make_shortcode(url, aliases), f, ensure_ascii=False, indent=2)
        generated += 1

    print(f"Done. Generated {generated}, skipped {skipped}.")
    print(f"Output: {OUTPUT_DIR.resolve()}")


if __name__ == "__main__":
    main()
