package com.glitchhollow.gl

/**
 * Animated parallax background with 3 layers.
 *
 * In fallback mode (no atlas): draws geometric shapes using coloured
 * rects — drifting rectangles at different speeds to give depth.
 *
 * With atlas: layers map to horizontal strips of a background sheet.
 * (Phase 5 adds the real background art.)
 *
 * Each layer scrolls at a different speed and wraps seamlessly.
 */
class ParallaxBackground(
    private val screenW: Float,
    private val screenH: Float,
    private val worldIndex: Int = 0  // 0–3, affects colour theme
) {
    private var time = 0f

    // World colour themes [bg, layer1, layer2, layer3]
    private val themes = arrayOf(
        arrayOf(                                            // W1 — neon purple
            floatArrayOf(0.051f, 0.039f, 0.102f),
            floatArrayOf(0.118f, 0f,     0.208f),
            floatArrayOf(0.208f, 0.051f, 0.318f),
            floatArrayOf(0.482f, 0.184f, 0.745f)
        ),
        arrayOf(                                            // W2 — magenta
            floatArrayOf(0.102f, 0f,     0.051f),
            floatArrayOf(0.208f, 0f,     0.102f),
            floatArrayOf(0.318f, 0.051f, 0.208f),
            floatArrayOf(0.745f, 0.184f, 0.482f)
        ),
        arrayOf(                                            // W3 — cyan
            floatArrayOf(0f,     0.051f, 0.102f),
            floatArrayOf(0f,     0.102f, 0.208f),
            floatArrayOf(0.051f, 0.208f, 0.318f),
            floatArrayOf(0.184f, 0.745f, 0.482f)
        ),
        arrayOf(                                            // W4 — amber
            floatArrayOf(0.051f, 0.039f, 0f),
            floatArrayOf(0.102f, 0.078f, 0f),
            floatArrayOf(0.208f, 0.157f, 0f),
            floatArrayOf(0.745f, 0.576f, 0.184f)
        )
    )

    fun update(dt: Float) { time += dt }

    fun draw(batch: SpriteBatch, assets: AssetManager, hudMatrix: FloatArray) {
        val atlas = assets.atlas ?: return
        val theme = themes[worldIndex.coerceIn(0, themes.size - 1)]

        batch.begin(hudMatrix)

        // Layer 0 — solid background
        val bg = theme[0]
        batch.draw(atlas, 0f, 0f, screenW, screenH,
            0f, 0f, 0.001f, 0.001f, bg[0], bg[1], bg[2], 1f)

        // Layer 1 — large slow drifting rects (stars/boulders)
        drawLayer(batch, atlas, theme[1], speed = 12f,  count = 6,
            minW = 80f, maxW = 160f, minH = 2f, maxH = 4f, alpha = 0.3f)

        // Layer 2 — medium rects
        drawLayer(batch, atlas, theme[2], speed = 28f, count = 10,
            minW = 20f, maxW = 60f,  minH = 2f, maxH = 3f, alpha = 0.4f)

        // Layer 3 — fast small sparkles
        drawLayer(batch, atlas, theme[3], speed = 55f, count = 18,
            minW = 3f,  maxW = 8f,   minH = 3f, maxH = 8f, alpha = 0.7f)

        batch.end()
    }

    private fun drawLayer(
        batch: SpriteBatch, atlas: Texture,
        color: FloatArray, speed: Float, count: Int,
        minW: Float, maxW: Float, minH: Float, maxH: Float, alpha: Float
    ) {
        for (i in 0 until count) {
            // Pseudo-random but stable positions using index as seed
            val seed  = i * 137.508f + worldIndex * 31f
            val xBase = (seed * 0.618f) % screenW
            val yBase = (seed * 0.381f) % screenH
            val w     = minW + (seed % (maxW - minW))
            val h     = minH + (seed % (maxH - minH))

            // Scroll left, wrap around
            val x = ((xBase - time * speed) % (screenW + maxW) + screenW + maxW) % (screenW + maxW) - maxW
            val y = yBase

            batch.draw(atlas, x, y, w, h,
                0f, 0f, 0.001f, 0.001f,
                color[0], color[1], color[2], alpha)
        }
    }
}