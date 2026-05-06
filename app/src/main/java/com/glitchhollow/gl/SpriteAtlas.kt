package com.glitchhollow.gl

/**
 * Maps logical sprite names to UV regions on a texture atlas PNG.
 *
 * GL concept: Instead of one texture per sprite, we pack all sprites
 * into one large PNG (the atlas). Each sprite is a rectangular region
 * of that PNG, identified by UV coordinates (0.0–1.0).
 *
 * Why: Switching textures between draw calls forces a flush of the
 * SpriteBatch. If everything is on one atlas, the entire scene draws
 * in 1–2 flush calls instead of one per sprite type.
 *
 * Atlas layout (1024×1024 PNG — `assets/sprites/atlas.png`):
 *
 * Row 0  (y=0,   h=64):  Player frames — IDLE×4, RUN×6, JUMP×2, DEAD×4
 * Row 1  (y=64,  h=64):  Enemy Wobble — PATROL×4, DEAD×3, (pad)
 * Row 2  (y=128, h=64):  Enemy Stitchy — PATROL×4, DEAD×3
 * Row 3  (y=192, h=64):  Enemy Glitch — PATROL×4, ALERT×2, DEAD×3
 * Row 4  (y=256, h=64):  Enemy Director — PATROL×4, DEAD×4
 * Row 5  (y=320, h=48):  Tiles — floor, wall, platform(96w), hazard, exit
 * Row 6  (y=368, h=40):  Items — shard×4 spin, coin×4 spin
 * Row 7  (y=408, h=32):  UI — heart, star-on, star-off
 *
 * Until real art exists, the game falls back to coloured rects via
 * AssetManager.hasAtlas = false.
 *
 * Each Region stores: u0, v0 (top-left UV), u1, v1 (bottom-right UV),
 * width and height in pixels.
 */
class SpriteAtlas(atlasWidth: Int, atlasHeight: Int) {

    data class Region(
        val u0: Float, val v0: Float,
        val u1: Float, val v1: Float,
        val w:  Float, val h: Float
    )

    private val w = atlasWidth.toFloat()
    private val h = atlasHeight.toFloat()

    /** Build a Region from pixel coordinates on the atlas */
    fun region(px: Int, py: Int, pw: Int, ph: Int) = Region(
        u0 = px / w,         v0 = py / h,
        u1 = (px + pw) / w,  v1 = (py + ph) / h,
        w  = pw.toFloat(),   h  = ph.toFloat()
    )

    // ── Player ────────────────────────────────────────────────────
    // Each frame = 48×64px, packed left to right in row 0

    val pipIdle  = Array(4) { i -> region(i * 48,          0, 48, 64) }
    val pipRun   = Array(6) { i -> region((4 + i) * 48,    0, 48, 64) }
    val pipJump  = Array(2) { i -> region((10 + i) * 48,   0, 48, 64) }
    val pipDead  = Array(4) { i -> region((12 + i) * 48,   0, 48, 64) }

    // ── Enemies ───────────────────────────────────────────────────

    val wobblePatrol  = Array(4) { i -> region(i * 48,  64, 48, 48) }
    val wobbleDead    = Array(3) { i -> region((4+i)*48, 64, 48, 48) }

    val stitchyPatrol = Array(4) { i -> region(i * 48, 128, 48, 56) }
    val stitchyDead   = Array(3) { i -> region((4+i)*48,128, 48, 56) }

    val glitchPatrol  = Array(4) { i -> region(i * 48, 192, 48, 56) }
    val glitchAlert   = Array(2) { i -> region((4+i)*48,192, 48, 56) }
    val glitchDead    = Array(3) { i -> region((6+i)*48,192, 48, 56) }

    val directorPatrol = Array(4) { i -> region(i * 64, 256, 64, 64) }
    val directorDead   = Array(4) { i -> region((4+i)*64,256, 64, 64) }

    // ── Tiles ─────────────────────────────────────────────────────

    val tileFloor    = region(0,   320, 48, 48)
    val tileWall     = region(48,  320, 48, 48)
    val tilePlatform = region(96,  320, 96, 24)  // wider sprite
    val tileHazard   = region(192, 320, 48, 48)
    val tileExit     = Array(4) { i -> region((240 + i*48), 320, 48, 64) }

    // ── Items ─────────────────────────────────────────────────────

    val shardSpin = Array(4) { i -> region(i * 32,        368, 32, 40) }
    val coinSpin  = Array(4) { i -> region((128 + i*28),  368, 28, 28) }

    // ── UI ────────────────────────────────────────────────────────

    val uiHeart   = region(0,   408, 28, 28)
    val uiStarOn  = region(28,  408, 32, 32)
    val uiStarOff = region(60,  408, 32, 32)
}