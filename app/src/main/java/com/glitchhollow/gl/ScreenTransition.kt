package com.glitchhollow.gl

/**
 * Glitch-wipe transition between screens.
 *
 * Effect: horizontal strips slide off screen in alternating directions,
 * with a cyan/magenta chromatic flicker during the midpoint.
 *
 * Usage:
 *   val tx = ScreenTransition()
 *   tx.start { ScreenManager.set(nextScreen) }
 *   // in render(): tx.draw(batch, assets, screenW, screenH, hudMatrix)
 *   // in update(): tx.update(dt)
 */
class ScreenTransition {

    enum class Phase { IDLE, OUT, IN }

    var phase        = Phase.IDLE
    var progress     = 0f          // 0.0 → 1.0
    private var onMid: (() -> Unit)? = null
    private var midFired = false

    private val SPEED = 2.5f       // full transition in ~0.4s
    private val STRIPS = 8

    fun start(onMidpoint: () -> Unit) {
        phase     = Phase.OUT
        progress  = 0f
        midFired  = false
        onMid     = onMidpoint
    }

    fun update(dt: Float) {
        if (phase == Phase.IDLE) return

        progress += SPEED * dt

        // Fire midpoint callback (screen swap happens here)
        if (progress >= 0.5f && !midFired) {
            midFired = true
            onMid?.invoke()
            phase = Phase.IN
        }

        if (progress >= 1f) {
            progress = 1f
            phase    = Phase.IDLE
        }
    }

    val isRunning get() = phase != Phase.IDLE

    fun draw(batch: SpriteBatch, assets: AssetManager,
             screenW: Float, screenH: Float, hudMatrix: FloatArray) {
        val atlas = assets.atlas ?: return
        if (phase == Phase.IDLE) return

        val stripH = screenH / STRIPS
        val t = if (phase == Phase.OUT) progress * 2f else (1f - (progress - 0.5f) * 2f)

        batch.begin(hudMatrix)
        for (i in 0 until STRIPS) {
            val dir    = if (i % 2 == 0) 1f else -1f
            val offset = dir * screenW * t
            val sy     = i * stripH
            // Black strip sweeping across
            batch.draw(atlas, offset, sy, screenW, stripH,
                0f, 0f, 0.001f, 0.001f, 0f, 0f, 0f, 1f)
        }

        // Chromatic flicker at midpoint
        if (progress in 0.4f..0.6f) {
            val alpha = (1f - Math.abs(progress - 0.5f) * 10f).coerceIn(0f, 0.4f)
            batch.draw(atlas, -4f, 0f, screenW, screenH,
                0f, 0f, 0.001f, 0.001f, 0f, 0.96f, 1f, alpha)
            batch.draw(atlas, 4f, 0f, screenW, screenH,
                0f, 0f, 0.001f, 0.001f, 1f, 0.23f, 0.67f, alpha)
        }
        batch.end()
    }
}