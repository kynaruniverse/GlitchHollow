package com.glitchhollow.gl

import com.glitchhollow.core.Constants
import com.glitchhollow.core.GameEngine

/**
 * Draws the in-game heads-up display using SpriteBatch.
 * Uses the HUD projection matrix (no camera scroll).
 *
 * Elements:
 *   - Heart icons (lives remaining)
 *   - Shard counter (◆ X/Y)
 *   - Timer (MM:SS)
 *   - Thin accent line below HUD
 *
 * Fallback: if no atlas, draws coloured rects for hearts/shards.
 */
class HUD {

    fun draw(batch: SpriteBatch, assets: AssetManager,
             engine: GameEngine, screenW: Float, hudMatrix: FloatArray) {

        batch.begin(hudMatrix)

        val hudH = Constants.HUD_HEIGHT.toFloat()

        // HUD background — dark panel
        drawRect(batch, assets, 0f, 0f, screenW, hudH,
            r = 0.051f, g = 0.039f, b = 0.102f, a = 0.92f)

        // Accent line at HUD bottom — purple
        drawRect(batch, assets, 0f, hudH - 2f, screenW, 2f,
            r = 0.48f, g = 0.18f, b = 0.75f, a = 1f)

        // Hearts
        val heartSize = 24f
        val heartPad  = 6f
        val heartY    = (hudH - heartSize) / 2f

        for (i in 0 until engine.player.lives.coerceAtMost(5)) {
            val hx = 12f + i * (heartSize + heartPad)
            if (assets.hasAtlas && assets.atlas != null && assets.sprites != null) {
                val r = assets.sprites!!.uiHeart
                batch.draw(assets.atlas!!, hx, heartY,
                    heartSize, heartSize, r.u0, r.v0, r.u1, r.v1)
            } else {
                // Fallback — red circle approximated as rect
                drawRect(batch, assets, hx, heartY, heartSize, heartSize,
                    r = 1f, g = 0.18f, b = 0.33f, a = 1f)
            }
        }

        // Shard counter — centred
        val shardsGot   = engine.shardsGot()
        val shardsTotal = engine.shardCollected.size
        // In fallback mode: draw a small cyan rect to represent shard count
        // Real text rendering via bitmap font comes in Phase 5
        val counterW  = 80f; val counterH = 20f
        val counterX  = screenW / 2f - counterW / 2f
        val counterY  = (hudH - counterH) / 2f
        drawRect(batch, assets, counterX, counterY, counterW * (shardsGot.toFloat() / shardsTotal.coerceAtLeast(1)), counterH,
            r = 0f, g = 0.96f, b = 1f, a = 0.8f)
        drawRect(batch, assets, counterX, counterY, counterW, counterH,
            r = 0f, g = 0.96f, b = 1f, a = 0.15f)

        // Timer — right aligned
        val elapsed = (engine.elapsedMs / 1000).toInt()
        val mins    = elapsed / 60; val secs = elapsed % 60
        // Fallback: yellow bar representing time (real font in Phase 5)
        val timerW = 60f * (1f - (elapsed / 120f).coerceAtMost(1f))
        drawRect(batch, assets, screenW - 80f, (hudH - 16f) / 2f, timerW, 16f,
            r = 1f, g = 0.9f, b = 0f, a = 0.8f)

        batch.end()
    }

    private fun drawRect(batch: SpriteBatch, assets: AssetManager,
                         x: Float, y: Float, w: Float, h: Float,
                         r: Float, g: Float, b: Float, a: Float) {
        val tex = assets.atlas ?: return
        // Sample the very top-left pixel of atlas (should be a solid colour pixel)
        // In Phase 5 we'll add a dedicated 1×1 white pixel at atlas position (0,0)
        batch.draw(tex, x, y, w, h, 0f, 0f, 0.001f, 0.001f, r, g, b, a)
    }
}