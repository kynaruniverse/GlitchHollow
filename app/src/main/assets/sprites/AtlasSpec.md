# Glitch Hollow — Sprite Atlas Spec
## File: assets/sprites/atlas.png
## Size: 1024×1024px, PNG-32, transparent background

### CRITICAL: Pixel at (0,0)
The top-left pixel MUST be solid white (#FFFFFF, full alpha).
This is the "white pixel" used for all coloured rect drawing.
Without it, all UI and fallback rects will be invisible.

### Row layout

#### Row 0 — Player "Pip" (y=0, h=64)
Each frame: 48×64px
| x    | Content           |
|------|-------------------|
| 0    | Idle frame 0      |
| 48   | Idle frame 1      |
| 96   | Idle frame 2      |
| 144  | Idle frame 3      |
| 192  | Run frame 0       |
| 240  | Run frame 1       |
| 288  | Run frame 2       |
| 336  | Run frame 3       |
| 384  | Run frame 4       |
| 432  | Run frame 5       |
| 480  | Jump frame 0      |
| 528  | Jump frame 1      |
| 576  | Dead frame 0      |
| 624  | Dead frame 1      |
| 672  | Dead frame 2      |
| 720  | Dead frame 3      |

#### Row 1 — Enemy: Wobble (y=64, h=48)
Each frame: 48×48px
| x    | Content           |
|------|-------------------|
| 0–143| Patrol frames 0–3 |
| 144–287 | Dead frames 0–2 |

#### Row 2 — Enemy: Stitchy (y=128, h=56)
Each frame: 48×56px — same column layout as Wobble

#### Row 3 — Enemy: Glitch (y=192, h=56)
Each frame: 48×56px
| x    | Content           |
|------|-------------------|
| 0–143| Patrol frames 0–3 |
| 144–239 | Alert frames 0–1 |
| 240–383 | Dead frames 0–2 |

#### Row 4 — Enemy: Director (y=256, h=64)
Each frame: 64×64px
| x    | Content           |
|------|-------------------|
| 0–191| Patrol frames 0–3 |
| 192–447 | Dead frames 0–3 |

#### Row 5 — Tiles (y=320, h=48)
| x    | w    | Content        |
|------|------|----------------|
| 0    | 48   | Floor          |
| 48   | 48   | Wall           |
| 96   | 96   | Platform (wide)|
| 192  | 48   | Hazard (spike) |
| 240  | 48   | Exit frame 0   |
| 288  | 48   | Exit frame 1   |
| 336  | 48   | Exit frame 2   |
| 384  | 48   | Exit frame 3   |

#### Row 6 — Items (y=368)
| x    | w    | h  | Content         |
|------|------|----|-----------------|
| 0    | 32   | 40 | Shard spin 0    |
| 32   | 32   | 40 | Shard spin 1    |
| 64   | 32   | 40 | Shard spin 2    |
| 96   | 32   | 40 | Shard spin 3    |
| 128  | 28   | 28 | Coin spin 0     |
| 156  | 28   | 28 | Coin spin 1     |
| 184  | 28   | 28 | Coin spin 2     |
| 212  | 28   | 28 | Coin spin 3     |

#### Row 7 — UI (y=408)
| x    | w    | h  | Content         |
|------|------|----|-----------------|
| 0    | 28   | 28 | Heart           |
| 28   | 32   | 32 | Star ON         |
| 60   | 32   | 32 | Star OFF        |

### Style guidelines
- Palette limited to ~16 colours per world theme
- W1 dominant: #7B2FBE (purple), #00F4FF (cyan), #FF3AB5 (magenta)
- Black outlines: 1px
- No anti-aliasing — hard pixel edges only
- Character design: "corrupted digital" aesthetic — glitchy, angular