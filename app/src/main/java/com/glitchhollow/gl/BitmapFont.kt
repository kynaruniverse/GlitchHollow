package com.glitchhollow.gl

/**
 * Pixel art bitmap font renderer.
 *
 * Reads from a glyph PNG atlas where each character is a fixed-width cell.
 * The font sheet lives at assets/sprites/font.png
 *
 * Layout: 16 columns × 6 rows of glyphs, each cell 16×16px (256×96px total)
 * ASCII 32 (space) → glyph 0, ASCII 126 (~) → glyph 94
 * Row-major order left→right, top→bottom
 *
 * Usage:
 *   font.draw(batch, "SCORE: 42", x, y, scale, r, g, b, a)
 *
 * Fallback: if font texture not loaded, draw() is a no-op.
 * Add assets/sprites/font.png to enable text.
 */
class BitmapFont(private val fontTexture: Texture?) {

    companion object {
        const val GLYPH_W    = 16    // px per glyph cell
        const val GLYPH_H    = 16
        const val COLS       = 16    // glyphs per row in the sheet
        const val SHEET_W    = 256f
        const val SHEET_H    = 96f
        const val ASCII_START = 32   // space
    }

    /**
     * Draw a string at world/screen position (x, y).
     *
     * @param scale  1f = native 16px, 2f = 32px, etc.
     * @param align  -1 = left (default), 0 = centre, 1 = right
     */
    fun draw(
        batch:  SpriteBatch,
        text:   String,
        x:      Float,
        y:      Float,
        scale:  Float = 1f,
        r:      Float = 1f,
        g:      Float = 1f,
        b:      Float = 1f,
        a:      Float = 1f,
        align:  Int = -1
    ) {
        val tex = fontTexture ?: return
        val cw  = GLYPH_W * scale
        val ch  = GLYPH_H * scale

        val startX = when (align) {
            0    -> x - (text.length * cw) / 2f   // centre
            1    -> x - text.length * cw            // right
            else -> x                               // left
        }

        text.forEachIndexed { i, char ->
            val code = char.code - ASCII_START
            if (code < 0 || code >= 16 * 6) return@forEachIndexed

            val col = code % COLS
            val row = code / COLS

            val u0 = col * GLYPH_W / SHEET_W
            val v0 = row * GLYPH_H / SHEET_H
            val u1 = u0 + GLYPH_W / SHEET_W
            val v1 = v0 + GLYPH_H / SHEET_H

            batch.draw(tex,
                startX + i * cw, y,
                cw, ch,
                u0, v0, u1, v1,
                r, g, b, a
            )
        }
    }

    /** Width of a string in pixels at given scale */
    fun measureWidth(text: String, scale: Float = 1f) =
        text.length * GLYPH_W * scale

    /** Height of one line in pixels at given scale */
    fun lineHeight(scale: Float = 1f) = GLYPH_H * scale
}