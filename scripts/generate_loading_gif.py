#!/usr/bin/env python3
"""
Generate loading.gif — skeleton shimmer placeholder for emoji_deco.
Output: src/main/resources/assets/emoji_deco/textures/loading.gif

4x4 uniform-color GIF; each frame is a single gray shade that pulses.
Being a uniform color, it stretches correctly to any aspect ratio (no spinner distortion).
Frame duration: 220ms  →  880ms total loop.
"""

from pathlib import Path
from PIL import Image

OUTPUT = Path("src/main/resources/assets/emoji_deco/textures/loading.gif")
SIZE   = 4    # uniform color → size doesn't matter, 4×4 is safe for all decoders
DUR    = 220  # ms per frame

# Smooth pulse: dark → mid-dark → mid-light → mid-dark (seamless loop)
SHADES = [0x3A, 0x52, 0x68, 0x52]

OUTPUT.parent.mkdir(parents=True, exist_ok=True)
frames = [Image.new("P", (SIZE, SIZE), s).putpalette(
              [s, s, s] + [0] * (256 * 3 - 3)) or Image.new("P", (SIZE, SIZE), 0)
          for s in SHADES]

# Rebuild cleanly (putpalette returns None)
frames = []
for shade in SHADES:
    # 均一色のパレット画像を直接生成（dither なし）
    img = Image.new("P", (SIZE, SIZE), 0)
    img.putpalette([shade, shade, shade] * 256)
    frames.append(img)

frames[0].save(
    OUTPUT,
    save_all=True,
    append_images=frames[1:],
    duration=DUR,
    loop=0,
    disposal=2,   # 各フレームを背景色でクリアしてから描画
)
print(f"Generated {OUTPUT.resolve()}  ({len(frames)} frames × {DUR}ms  shades={[hex(s) for s in SHADES]})")
