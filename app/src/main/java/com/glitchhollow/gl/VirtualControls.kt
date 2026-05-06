package com.glitchhollow.gl

import android.view.MotionEvent
import com.glitchhollow.core.Constants
import com.glitchhollow.core.Player

/**
 * Gesture-based controls with subtle visual zone indicators.
 *
 * Layout (portrait screen):
 *   Left 25%  of bottom 35% → move left
 *   25–50%    of bottom 35% → move right
 *   Right 50% of bottom 35% → jump (tap) / held jump
 *
 * Visual: very faint arc/chevron indicators at screen edges.
 * No opaque buttons — just enough to guide new players.
 *
 * Swipe anywhere in bottom zone also works:
 *   Swipe left  → move left
 *   Swipe right → move right
 *   Swipe up    → jump
 *
 * Called from GameScreen.onTouch() on the GL thread.
 */
class VirtualControls(private val player: Player) {

    // Active pointer tracking for multi-touch held movement
    private val pointers = mutableMapOf<Int, FloatArray>() // id → [x, y]

    // Swipe detection
    private var swipeStartX  = 0f; private var swipeStartY  = 0f
    private var swipeStartMs = 0L; private var swipeActive  = false

    private var screenW = 1080f; private var screenH = 1920f

    fun setScreenSize(w: Int, h: Int) { screenW = w.toFloat(); screenH = h.toFloat() }

    fun onTouch(x: Float, y: Float, action: Int, pointerId: Int = 0) {
        when (action) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_POINTER_DOWN -> {
                pointers[pointerId] = floatArrayOf(x, y)
                applyZone(x, y, press = true)
                swipeStartX  = x; swipeStartY  = y
                swipeStartMs = System.currentTimeMillis()
                swipeActive  = true
            }
            MotionEvent.ACTION_MOVE -> {
                pointers[pointerId] = floatArrayOf(x, y)
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_POINTER_UP -> {
                pointers.remove(pointerId)
                releaseZone(x, y)
                if (swipeActive) {
                    val elapsed = System.currentTimeMillis() - swipeStartMs
                    if (elapsed < Constants.SWIPE_MAX_TIME) {
                        evaluateSwipe(x - swipeStartX, y - swipeStartY)
                    }
                    swipeActive = false
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                pointers.clear()
                player.wantsLeft  = false
                player.wantsRight = false
                swipeActive = false
            }
        }

        // Re-evaluate held directions from all active pointers
        player.wantsLeft  = false
        player.wantsRight = false
        pointers.values.forEach { pos -> applyHeld(pos[0], pos[1]) }
    }

    private fun inControlZone(y: Float) = y > screenH * 0.65f

    private fun applyZone(x: Float, y: Float, press: Boolean) {
        if (!inControlZone(y)) return
        when {
            x < screenW * 0.25f -> player.wantsLeft  = true
            x < screenW * 0.50f -> player.wantsRight = true
            press               -> player.wantsJump  = true
        }
    }

    private fun applyHeld(x: Float, y: Float) {
        if (!inControlZone(y)) return
        when {
            x < screenW * 0.25f -> player.wantsLeft  = true
            x < screenW * 0.50f -> player.wantsRight = true
        }
    }

    private fun releaseZone(x: Float, y: Float) {
        if (inControlZone(y) && x >= screenW * 0.5f) player.wantsJump = false
    }

    private fun evaluateSwipe(dx: Float, dy: Float) {
        val t = Constants.SWIPE_THRESHOLD
        if (Math.abs(dx) > Math.abs(dy) && Math.abs(dx) > t) {
            if (dx < 0) player.wantsLeft = true else player.wantsRight = true
        } else if (dy < -t) {
            player.wantsJump = true
        }
    }

    /**
     * Draw subtle zone indicators — very faint chevrons at screen bottom.
     * Called from GameScreen.render() with the HUD batch.
     *
     * If hasAtlas is false we skip — coloured rects are enough for testing.
     */
    fun drawIndicators(batch: SpriteBatch, assets: AssetManager,
                       screenW: Float, screenH: Float) {
        // Faint semi-transparent zone outlines drawn as thin coloured rects
        // Left arrow zone — cyan tint, very low alpha
        val zoneY  = screenH * 0.65f
        val zoneH  = screenH - zoneY
        val alpha  = 0.06f   // very subtle — just visible enough

        // Left move zone
        batch.draw(
            texture = assets.atlas ?: return,
            x = 0f, y = zoneY,
            w = screenW * 0.25f, h = zoneH,
            r = 0f, g = 0.96f, b = 1f, a = alpha
        )
        // Right move zone
        batch.draw(
            texture = assets.atlas ?: return,
            x = screenW * 0.25f, y = zoneY,
            w = screenW * 0.25f, h = zoneH,
            r = 0.48f, g = 0.18f, b = 0.75f, a = alpha
        )
        // Jump zone
        batch.draw(
            texture = assets.atlas ?: return,
            x = screenW * 0.5f, y = zoneY,
            w = screenW * 0.5f, h = zoneH,
            r = 1f, g = 0.9f, b = 0f, a = alpha
        )
    }
}