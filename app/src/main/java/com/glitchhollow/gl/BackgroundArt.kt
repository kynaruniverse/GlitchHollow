package com.glitchhollow.gl

import kotlin.math.sin

/**
 * Per-world parallax background with 3 depth layers.
 *
 * Replaces ParallaxBackground's rect-based system with a richer
 * geometric approach that works beautifully even before real art exists.
 *
 * Layer structure per world:
 *   Layer 0 — sky gradient (solid fill, world colour)
 *   Layer 1 — distant silhouettes (large slow shapes)
 *   Layer 2 — midground elements (medium speed)
 *   Layer 3 — foreground details (fast, near camera)
 *
 * With real art: swap layer draws to use atlas regions instead of rects.
 * Without real art: procedural geometry gives a clean stylised look.
 *
 * Camera scroll drives layers at fractional speeds (parallax factor):
 *   Layer 1 scrolls at 0.1× camera speed  (far away)
 *   Layer 2 scrolls at 0.3× camera speed
 *   Layer 3 scrolls at 0.6× camera speed
 */
class BackgroundArt(
    private val screenW: Float,
    private val screenH: Float,
    private val worldIndex: Int
) {
    private var time    = 0f
    private var camX    = 0f
    private var camY    = 0f

    // World themes: [sky, distant, mid, near, accent]
    private val themes = arrayOf(
        arrayOf(  // W1 — Deep purple / neon cyan
            floatArrayOf(0.024f, 0.016f, 0.063f),
            floatArrayOf(0.063f, 0f,     0.157f),
            floatArrayOf(0.157f, 0.031f, 0.251f),
            floatArrayOf(0.282f, 0.094f, 0.471f),
            floatArrayOf(0f,     0.957f, 1f)
        ),
        arrayOf(  // W2 — Magenta / hot pink
            floatArrayOf(0.063f, 0f,     0.031f),
            floatArrayOf(0.157f, 0f,     0.094f),
            floatArrayOf(0.251f, 0.031f, 0.157f),
            floatArrayOf(0.471f, 0.094f, 0.314f),
            floatArrayOf(1f,     0.23f,  0.67f)
        ),
        arrayOf(  // W3 — Teal / electric blue
            floatArrayOf(0f,     0.024f, 0.063f),
            floatArrayOf(0f,     0.063f, 0.157f),
            floatArrayOf(0.031f, 0.157f, 0.251f),
            floatArrayOf(0.094f, 0.314f, 0.471f),
            floatArrayOf(0.18f,  0.74f,  1f)
        ),
        arrayOf(  // W4 — Amber / lava
            floatArrayOf(0.047f, 0.024f, 0f),
            floatArrayOf(0.118f, 0.059f, 0f),
            floatArrayOf(0.235f, 0.118f, 0f),
            floatArrayOf(0.471f, 0.235f, 0.031f),
            floatArrayOf(1f,     0.6f,   0f)
        )
    )

    fun update(dt: Float, cameraX: Float, cameraY: Float) {
        time = time + dt
        camX = cameraX
        camY = cameraY
    }

    fun draw(batch: SpriteBatch, assets: AssetManager, hudMatrix: FloatArray) {
        val atlas  = assets.atlas ?: return
        val t      = themes[worldIndex.coerceIn(0, themes.size - 1)]
        val sky    = t[0]; val dist = t[1]; val mid = t[2]
        val near   = t[3]; val acc  = t[4]

        batch.begin(hudMatrix)

        // ── Layer 0 — Sky ─────────────────────────────────────────
        batch.draw(atlas, 0f, 0f, screenW, screenH,
            0f, 0f, 0.001f, 0.001f, sky[0], sky[1], sky[2], 1f)

        // ── Layer 1 — Distant silhouettes (0.08× parallax) ────────
        val l1X = -(camX * 0.08f) % (screenW * 2)
        drawSilhouetteLayer(batch, atlas, dist, acc, l1X, screenH, 1)

        // ── Layer 2 — Midground (0.22× parallax) ──────────────────
        val l2X = -(camX * 0.22f) % (screenW * 2)
        drawSilhouetteLayer(batch, atlas, mid, acc, l2X, screenH * 0.7f, 2)

        // ── Layer 3 — Near foreground (0.5× parallax) ─────────────
        val l3X = -(camX * 0.50f) % (screenW * 2)
        drawSilhouetteLayer(batch, atlas, near, acc, l3X, screenH * 0.45f, 3)

        // ── Stars / particles in sky (stationary, twinkle) ────────
        drawStars(batch, atlas, acc)

        batch.end()
    }

    private fun drawSilhouetteLayer(
        batch: SpriteBatch, atlas: Texture,
        color: FloatArray, accent: FloatArray,
        offsetX: Float, maxH: Float, layer: Int
    ) {
        // Draw a row of abstract buildings/spires using rects
        // Until real art exists this gives clean geometric skylines
        val count  = 8
        val blockW = screenW / count

        for (i in 0 until count * 2) {   // ×2 for seamless wrap
            val seed  = (i % count) * 1337f + layer * 7919f
            val h     = maxH * (0.3f + (seed % 0.55f))
            val w     = blockW * (0.4f + (seed * 0.001f % 0.5f))
            val bx    = (i % count) * blockW + offsetX + (seed % blockW)
            val by    = screenH - h

            // Main block
            batch.draw(atlas, bx, by, w, h,
                0f, 0f, 0.001f, 0.001f, color[0], color[1], color[2], 0.9f)

            // Window lights — accent colour dots
            val winCount = (h / 24f).toInt().coerceAtMost(6)
            for (wi in 0 until winCount) {
                val pulse = sin(time * 1.5f + seed + wi).toFloat()
                if (pulse > 0.3f) {
                    batch.draw(atlas,
                        bx + w * 0.2f + (wi % 2) * w * 0.4f,
                        by + wi * (h / winCount) + 6f,
                        6f, 6f,
                        0f, 0f, 0.001f, 0.001f,
                        accent[0], accent[1], accent[2],
                        (pulse - 0.3f) * 0.7f)
                }
            }

            // Antenna / spire on top
            batch.draw(atlas, bx + w * 0.45f, by - h * 0.12f,
                4f, h * 0.12f,
                0f, 0f, 0.001f, 0.001f,
                accent[0], accent[1], accent[2], 0.4f)
        }
    }

    private fun drawStars(batch: SpriteBatch, atlas: Texture, accent: FloatArray) {
        val count = 40
        for (i in 0 until count) {
            val seed   = i * 91273f
            val sx     = (seed * 0.00137f) % screenW
            val sy     = (seed * 0.00271f) % (screenH * 0.6f)
            val twinkle = (sin(time * (1f + (seed % 2f)) + seed) * 0.5f + 0.5f).toFloat()
            val sz      = 2f + (seed % 3f)
            batch.draw(atlas, sx, sy, sz, sz,
                0f, 0f, 0.001f, 0.001f,
                accent[0] * 0.8f + 0.2f,
                accent[1] * 0.8f + 0.2f,
                accent[2] * 0.8f + 0.2f,
                twinkle * 0.7f)
        }
    }
}