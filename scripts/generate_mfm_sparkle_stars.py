#!/usr/bin/env python3
"""
Generate the star textures used by MFM $[sparkle].
Output: common/src/main/resources/assets/emoji_deco/textures/mfm/sparkle_{pink,cyan,yellow}.png

The star outline and the three fill colours are taken from Misskey's MkSparkle.vue
(packages/frontend/src/components/MkSparkle.vue, AGPL-3.0, (c) syuilo and misskey-project).
See THIRD_PARTY_NOTICES.md.
"""
import re
from pathlib import Path
from PIL import Image, ImageDraw

STAR_PATH = (
    "M29.427,2.011C29.721,0.83 30.782,0 32,0C33.218,0 34.279,0.83 34.573,2.011L39.455,21.646"
    "C39.629,22.347 39.991,22.987 40.502,23.498C41.013,24.009 41.653,24.371 42.354,24.545"
    "L61.989,29.427C63.17,29.721 64,30.782 64,32C64,33.218 63.17,34.279 61.989,34.573"
    "L42.354,39.455C41.653,39.629 41.013,39.991 40.502,40.502C39.991,41.013 39.629,41.653 39.455,42.354"
    "L34.573,61.989C34.279,63.17 33.218,64 32,64C30.782,64 29.721,63.17 29.427,61.989"
    "L24.545,42.354C24.371,41.653 24.009,41.013 23.498,40.502C22.987,39.991 22.347,39.629 21.646,39.455"
    "L2.011,34.573C0.83,34.279 0,33.218 0,32C0,30.782 0.83,29.721 2.011,29.427"
    "L21.646,24.545C22.347,24.371 22.987,24.009 23.498,23.498C24.009,22.987 24.371,22.347 24.545,21.646"
    "L29.427,2.011Z"
)
COLORS = {"pink": "#FF1493", "cyan": "#00FFFF", "yellow": "#FFE202"}
SIZE = 32          # output px (star box is 64 in the source path)
SS = 8             # supersampling factor

OUT = Path(__file__).resolve().parent.parent / "common/src/main/resources/assets/emoji_deco/textures/mfm"


def flatten(path: str, steps: int = 24):
    pts, cur = [], (0.0, 0.0)
    for cmd, args in re.findall(r"([MLCZ])([^MLCZ]*)", path):
        n = [float(v) for v in re.findall(r"-?\d+(?:\.\d+)?", args)]
        if cmd in "ML":
            cur = (n[0], n[1]); pts.append(cur)
        elif cmd == "C":
            p0 = cur
            p1, p2, p3 = (n[0], n[1]), (n[2], n[3]), (n[4], n[5])
            for i in range(1, steps + 1):
                t = i / steps; u = 1 - t
                pts.append((u**3*p0[0] + 3*u*u*t*p1[0] + 3*u*t*t*p2[0] + t**3*p3[0],
                            u**3*p0[1] + 3*u*u*t*p1[1] + 3*u*t*t*p2[1] + t**3*p3[1]))
            cur = p3
    return pts


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    big = SIZE * SS
    k = big / 64.0
    poly = [(x * k, y * k) for x, y in flatten(STAR_PATH)]
    mask = Image.new("L", (big, big), 0)
    ImageDraw.Draw(mask).polygon(poly, fill=255)
    mask = mask.resize((SIZE, SIZE), Image.LANCZOS)
    for name, hexcol in COLORS.items():
        rgb = tuple(int(hexcol[i:i + 2], 16) for i in (1, 3, 5))
        img = Image.new("RGBA", (SIZE, SIZE), rgb + (0,))
        img.putalpha(mask)
        img.save(OUT / f"sparkle_{name}.png", optimize=True)
        print("wrote", OUT / f"sparkle_{name}.png")


if __name__ == "__main__":
    main()
