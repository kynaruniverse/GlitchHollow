package com.glitchhollow.gl

/**
 * Shared UI drawing primitives for all screens.
 * All coordinates are in screen-space pixels (hudMatrix space).
 */
object UIHelpers {

    // ── Colours ───────────────────────────────────────────────────

    val CYAN    = floatArrayOf(0f,    0.96f, 1f)
    val PURPLE  = floatArrayOf(0.48f, 0.18f, 0.75f)
    val MAGENTA = floatArrayOf(1f,    0.23f, 0.67f)
    val YELLOW  = floatArrayOf(1f,    0.9f,  0f)
    val WHITE   = floatArrayOf(1f,    1f,    1f)
    val DARK    = floatArrayOf(0.051f,0.039f,0.102f)
    val MID     = floatArrayOf(0.118f,0f,    0.208f)

    // ── Primitives ────────────────────────────────────────────────

    fun rect(batch: SpriteBatch, atlas: Texture,
             x: Float, y: Float, w: Float, h: Float,
             c: FloatArray, a: Float = 1f) {
        batch.draw(atlas, x, y, w, h,
            0f, 0f, 0.001f, 0.001f, c[0], c[1], c[2], a)
    }

    /** Outlined rect — border width px */
    fun rectOutline(batch: SpriteBatch, atlas: Texture,
                    x: Float, y: Float, w: Float, h: Float,
                    border: Float, fillColor: FloatArray, fillAlpha: Float,
                    borderColor: FloatArray, borderAlpha: Float = 1f) {
        rect(batch, atlas, x, y, w, h, fillColor, fillAlpha)
        // Top
        rect(batch, atlas, x, y, w, border, borderColor, borderAlpha)
        // Bottom
        rect(batch, atlas, x, y + h - border, w, border, borderColor, borderAlpha)
        // Left
        rect(batch, atlas, x, y, border, h, borderColor, borderAlpha)
        // Right
        rect(batch, atlas, x + w - border, y, border, h, borderColor, borderAlpha)
    }

    /** Standard glitch-hollow button */
    fun button(batch: SpriteBatch, atlas: Texture, font: BitmapFont,
               x: Float, y: Float, w: Float, h: Float,
               label: String, active: Boolean = true) {
        val fill   = if (active) MID    else DARK
        val border = if (active) CYAN   else PURPLE
        val tc     = if (active) WHITE  else PURPLE
        val ta     = if (active) 1f     else 0.5f

        rectOutline(batch, atlas, x, y, w, h, 2f, fill, 0.9f, border)
        font.draw(batch, label,
            x + w / 2f, y + (h - font.lineHeight(2f)) / 2f,
            2f, tc[0], tc[1], tc[2], ta, align = 0)
    }

    /** Horizontal divider line */
    fun divider(batch: SpriteBatch, atlas: Texture,
                y: Float, screenW: Float, c: FloatArray = PURPLE) {
        rect(batch, atlas, 0f, y, screenW, 2f, c, 0.5f)
    }

    /** Section header label */
    fun header(batch: SpriteBatch, font: BitmapFont,
               text: String, y: Float, screenW: Float,
               c: FloatArray = CYAN) {
        font.draw(batch, text, screenW / 2f, y,
            2f, c[0], c[1], c[2], 1f, align = 0)
    }

    /** Star icons (3 in a row) — uses coloured rects in fallback */
    fun drawStars(batch: SpriteBatch, assets: AssetManager,
                  stars: BooleanArray, cx: Float, cy: Float, size: Float = 20f) {
        val atlas = assets.atlas ?: return
        val sp    = assets.sprites
        val gap   = size + 4f
        val startX = cx - gap

        stars.forEachIndexed { i, earned ->
            val sx = startX + i * gap
            if (sp != null) {
                val reg = if (earned) sp.uiStarOn else sp.uiStarOff
                batch.draw(atlas, sx, cy, size, size,
                    reg.u0, reg.v0, reg.u1, reg.v1)
            } else {
                val c = if (earned) YELLOW else PURPLE
                rect(batch, atlas, sx, cy, size, size, c, if (earned) 1f else 0.3f)
            }
        }
    }

    /** Hit test — is point (px,py) inside rect? */
    fun hits(px: Float, py: Float,
             rx: Float, ry: Float, rw: Float, rh: Float) =
        px >= rx && px <= rx + rw && py >= ry && py <= ry + rh
}