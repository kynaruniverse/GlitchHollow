#!/usr/bin/env python3
"""
generate_font.py — Glitch Hollow bitmap font generator.

Creates assets/sprites/font.png (256x96) using a system monospace font.
Each ASCII character 32–126 is rendered into a 16x16 cell.

When real pixel art font is ready:
  - Replace font.png with your hand-crafted version
  - Keep the same 256x96 size and 16x16 cell grid
  - This script is then retired

Dependencies: Pillow (pip install Pillow)
"""

from PIL import Image, ImageDraw, ImageFont
import os

CELL_W, CELL_H = 16, 16
COLS            = 16
ROWS            = 6
IMG_W           = CELL_W * COLS   # 256
IMG_H           = CELL_H * ROWS   # 96
ASCII_START     = 32              # space

img  = Image.new("RGBA", (IMG_W, IMG_H), (0, 0, 0, 0))
draw = ImageDraw.Draw(img)

# Try to load a pixel-friendly font at small size
# Falls back to PIL default if no truetype available
font = None
font_candidates = [
    "/usr/share/fonts/truetype/dejavu/DejaVuSansMono-Bold.ttf",
    "/usr/share/fonts/truetype/liberation/LiberationMono-Regular.ttf",
    "/usr/share/fonts/truetype/freefont/FreeMono.ttf",
    "/system/fonts/DroidSansMono.ttf",
]
for path in font_candidates:
    if os.path.exists(path):
        try:
            font = ImageFont.truetype(path, 11)
            print(f"Using font: {path}")
            break
        except Exception:
            pass

if font is None:
    font = ImageFont.load_default()
    print("Using PIL default font")

for code in range(ASCII_START, ASCII_START + COLS * ROWS):
    if code > 126:
        break
    idx  = code - ASCII_START
    col  = idx % COLS
    row  = idx // COLS
    cx   = col * CELL_W
    cy   = row * CELL_H
    char = chr(code)

    # Measure and centre glyph in cell
    try:
        bbox = draw.textbbox((0, 0), char, font=font)
        gw   = bbox[2] - bbox[0]
        gh   = bbox[3] - bbox[1]
    except Exception:
        gw, gh = 8, 11

    tx = cx + max(0, (CELL_W - gw) // 2)
    ty = cy + max(0, (CELL_H - gh) // 2) - 1

    # Draw white glyph on transparent background
    draw.text((tx, ty), char, fill=(255, 255, 255, 255), font=font)

out = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "app", "src", "main", "assets", "sprites", "font.png"
)
os.makedirs(os.path.dirname(out), exist_ok=True)
img.save(out, "PNG")
print(f"Font sheet generated: {out}  ({IMG_W}×{IMG_H})")