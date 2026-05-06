#!/usr/bin/env python3
"""
generate_atlas.py — Glitch Hollow placeholder sprite atlas generator.

Creates assets/sprites/atlas.png (1024x1024) with:
  - White pixel at (0,0) — required for rect drawing
  - Coloured placeholder sprites in correct atlas positions
  - Correct frame sizes matching SpriteAtlas.kt exactly

When real pixel art is ready:
  - Replace this script's output with your actual atlas.png
  - Keep the same pixel positions and frame sizes
  - This script is then retired

Dependencies: Pillow (pip install Pillow)
"""

from PIL import Image, ImageDraw
import os

W, H = 1024, 1024
img  = Image.new("RGBA", (W, H), (0, 0, 0, 0))
draw = ImageDraw.Draw(img)

def rect(x, y, w, h, color, label_color=(255,255,255,180)):
    """Draw a labelled placeholder sprite."""
    r, g, b, a = color
    # Fill
    draw.rectangle([x, y, x+w-1, y+h-1], fill=(r, g, b, a))
    # Border
    draw.rectangle([x, y, x+w-1, y+h-1], outline=(255,255,255,100))

def frames(x_start, y, fw, fh, count, color, labels=None):
    for i in range(count):
        lbl = labels[i] if labels else str(i)
        rect(x_start + i * fw, y, fw, fh, color)

# ── Critical: white pixel at (0,0) ────────────────────────────────
img.putpixel((0, 0), (255, 255, 255, 255))

# ── Row 0: Player Pip (y=0, h=64, fw=48) ─────────────────────────
PIP = (120, 60, 200, 255)      # purple
frames(0,   0, 48, 64, 4,  PIP)                        # idle
frames(192, 0, 48, 64, 6,  (140, 80, 220, 255))        # run
frames(480, 0, 48, 64, 2,  (160, 100, 240, 255))       # jump
frames(576, 0, 48, 64, 4,  (80,  30, 120, 255))        # dead

# ── Row 1: Wobble enemy (y=64, h=48, fw=48) ───────────────────────
WOB = (200, 60, 60, 255)
frames(0,   64, 48, 48, 4, WOB)                        # patrol
frames(192, 64, 48, 48, 3, (120, 30, 30, 255))         # dead

# ── Row 2: Stitchy enemy (y=128, h=56, fw=48) ────────────────────
STI = (200, 120, 60, 255)
frames(0,   128, 48, 56, 4, STI)                       # patrol
frames(192, 128, 48, 56, 3, (120, 60, 20, 255))        # dead

# ── Row 3: Glitch enemy (y=192, h=56, fw=48) ─────────────────────
GLI = (60, 200, 180, 255)
frames(0,   192, 48, 56, 4, GLI)                       # patrol
frames(192, 192, 48, 56, 2, (30, 140, 120, 255))       # alert
frames(288, 192, 48, 56, 3, (20, 80,  70, 255))        # dead

# ── Row 4: Director enemy (y=256, h=64, fw=64) ───────────────────
DIR = (200, 180, 60, 255)
frames(0,   256, 64, 64, 4, DIR)                       # patrol
frames(256, 256, 64, 64, 4, (120, 100, 20, 255))       # dead

# ── Row 5: Tiles (y=320) ──────────────────────────────────────────
rect(0,   320, 48,  48, (60,  20,  100, 255))          # floor
rect(48,  320, 48,  48, (30,  30,  80,  255))          # wall
rect(96,  320, 96,  24, (120, 50,  180, 255))          # platform (wide, half height)
rect(192, 320, 48,  48, (200, 40,  60,  255))          # hazard
frames(240, 320, 48, 64, 4, (0, 220, 240, 255))        # exit animated

# ── Row 6: Items (y=368) ──────────────────────────────────────────
SHARD = (0, 220, 255, 255)
COIN  = (255, 210, 0,   255)
frames(0,   368, 32, 40, 4, SHARD)                     # shard spin
frames(128, 368, 28, 28, 4, COIN)                      # coin spin

# ── Row 7: UI (y=408) ─────────────────────────────────────────────
rect(0,  408, 28, 28, (220, 50,  80,  255))            # heart
rect(28, 408, 32, 32, (255, 210, 0,   255))            # star ON
rect(60, 408, 32, 32, (80,  50,  120, 255))            # star OFF

# ── Output ────────────────────────────────────────────────────────
out = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "app", "src", "main", "assets", "sprites", "atlas.png"
)
os.makedirs(os.path.dirname(out), exist_ok=True)
img.save(out, "PNG")
print(f"Atlas generated: {out}  ({W}×{H})")