#!/usr/bin/env python3
"""
Generate Twemoji shortcode JSON files for the emoji_deco_starter resource pack.
Fetches emoji-data from iamcal/emoji-data for Discord/Slack-compatible shortcode names.

Usage:
    python scripts/generate_twemoji_shortcodes.py
"""

import json
import os
import urllib.request
from pathlib import Path

EMOJI_DATA_URL = "https://raw.githubusercontent.com/iamcal/emoji-data/master/emoji.json"
TWEMOJI_CDN_BASE = "https://cdn.jsdelivr.net/gh/twitter/twemoji@14.0.2/assets/72x72"
OUTPUT_DIR = Path("src/main/resources/resourcepacks/emoji_deco_starter/assets/emoji_deco/shortcodes")


def unified_to_filename(unified):
    """Convert 'iamcal' unified string (e.g. '1F600' or '1F1E6-1F1E8') to Twemoji filename."""
    return unified.lower() + ".png"


def make_shortcode(url, aliases):
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
                    "url": url
                }
            }
        }
    }
    if aliases:
        entry["aliases"] = aliases
    return entry


def main():
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

    print("Fetching emoji-data from iamcal/emoji-data ...")
    with urllib.request.urlopen(EMOJI_DATA_URL, timeout=30) as response:
        emoji_data = json.loads(response.read().decode("utf-8"))

    print(f"Processing {len(emoji_data)} entries ...")
    generated = 0
    skipped = 0

    for emoji in emoji_data:
        if not emoji.get("has_img_twitter", False):
            skipped += 1
            continue

        short_name = emoji["short_name"]
        short_names = emoji.get("short_names", [short_name])
        unified = emoji["unified"]
        filename = unified_to_filename(unified)
        url = f"{TWEMOJI_CDN_BASE}/{filename}"

        aliases = [n for n in short_names if n != short_name]
        shortcode = make_shortcode(url, aliases)

        output_path = OUTPUT_DIR / f"{short_name}.json"
        with open(output_path, "w", encoding="utf-8") as f:
            json.dump(shortcode, f, ensure_ascii=False, indent=2)

        generated += 1

    print(f"Done. Generated {generated} shortcodes, skipped {skipped} (no Twemoji image).")
    print(f"Output: {OUTPUT_DIR.resolve()}")


if __name__ == "__main__":
    main()
