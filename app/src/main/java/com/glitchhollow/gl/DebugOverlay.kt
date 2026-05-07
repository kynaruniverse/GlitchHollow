package com.glitchhollow.gl

import android.content.Context
import com.glitchhollow.core.Constants
import com.glitchhollow.core.GameEngine

class DebugOverlay(private val context: Context) {

    var visible = false

    fun toggle() { visible = !visible }

    fun draw(
        batch:         SpriteBatch,
        assets:        AssetManager,
        renderer:      GLRenderer,
        engine:        GameEngine,
        particleCount: Int,
        camX:          Float,
        camY:          Float,
        screenW:       Float,
        screenH:       Float,
        hudMatrix:     FloatArray
    ) {
        if (!visible) return
        val atlas = assets.atlas ?: return
        val font  = assets.font

        batch.begin(hudMatrix)

        // Panel background
        val panelW = screenW * 0.52f
        val panelH = 148f
        UIHelpers.rect(batch, atlas, 0f, 0f, panelW, panelH, UIHelpers.DARK, 0.82f)
        UIHelpers.rect(batch, atlas, 0f, panelH, panelW, 2f, UIHelpers.CYAN, 0.7f)
        UIHelpers.rect(batch, atlas, panelW, 0f, 2f, panelH, UIHelpers.CYAN, 0.7f)

        val x     = 10f
        val scale = 1f
        val lh    = font.lineHeight(scale) + 5f

        // FPS — green > 55, yellow > 40, red otherwise
        val fps  = renderer.actualFps
        val fpsR = if (fps > 55f) 0f   else 1f
        val fpsG = if (fps > 55f) 1f   else if (fps > 40f) 0.8f else 0.2f
        val fpsB = if (fps > 55f) 0.4f else 0f
        font.draw(batch, "FPS    ${fps.toInt()}",
            x, 10f, scale, fpsR, fpsG, fpsB, 1f)

        // Physics ticks — green if 1, yellow if 2, red if more
        val ticks  = renderer.physicsTicksLastFrame
        val ticksR = if (ticks <= 1) 0f else 1f
        val ticksG = if (ticks <= 1) 1f else if (ticks <= 2) 0.8f else 0.2f
        font.draw(batch, "TICKS  $ticks / ${GLRenderer.MAX_STEPS}",
            x, 10f + lh, scale, ticksR, ticksG, 0f, 1f)

        // Game state
        val stateStr = when (engine.gameState) {
            Constants.STATE_PLAYING  -> "PLAYING"
            Constants.STATE_PAUSED   -> "PAUSED"
            Constants.STATE_WIN      -> "WIN"
            Constants.STATE_DEAD     -> "DEAD"
            Constants.STATE_GAMEOVER -> "GAMEOVER"
            else                     -> "UNKNOWN(${engine.gameState})"
        }
        font.draw(batch, "STATE  $stateStr",
            x, 10f + lh * 2, scale, 1f, 1f, 1f, 0.85f)

        // Shards
        val got   = engine.shardsGot()
        val total = engine.shardCollected.size
        font.draw(batch, "SHARDS $got / $total",
            x, 10f + lh * 3, scale,
            UIHelpers.CYAN[0], UIHelpers.CYAN[1], UIHelpers.CYAN[2], 0.85f)

        // Particles
        val partR = if (particleCount < Constants.PARTICLE_POOL_SIZE * 0.8f) 0f else 1f
        val partG = if (particleCount < Constants.PARTICLE_POOL_SIZE * 0.8f) 1f else 0.4f
        font.draw(batch, "PARTS  $particleCount / ${Constants.PARTICLE_POOL_SIZE}",
            x, 10f + lh * 4, scale, partR, partG, 0f, 0.85f)

        // Camera position
        font.draw(batch, "CAM    ${camX.toInt()},${camY.toInt()}",
            x, 10f + lh * 5, scale, 1f, 1f, 1f, 0.6f)

        // World/Level indicator — top right of panel
        font.draw(batch, "W${engine.world}-L${engine.level}",
            panelW - 58f, 10f, scale,
            UIHelpers.YELLOW[0], UIHelpers.YELLOW[1], UIHelpers.YELLOW[2], 0.9f)

        // Touch hint
        font.draw(batch, "3-TAP TOGGLE",
            panelW - 100f, panelH - lh - 6f, scale,
            UIHelpers.PURPLE[0], UIHelpers.PURPLE[1], UIHelpers.PURPLE[2], 0.5f)

        batch.end()
    }
}