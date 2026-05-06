package com.glitchhollow.gl

import android.content.Context
import com.glitchhollow.core.Constants

/**
 * CRT scanline + vignette overlay.
 *
 * Drawn as the absolute last pass — on top of everything including the HUD.
 *
 * Scanlines: alternating dark horizontal strips 1–2px tall, very low alpha.
 *   Gives the illusion of a CRT monitor without a custom shader.
 *   Uses the SpriteBatch with dark tinted quads.
 *
 * Vignette: a dark radial gradient at screen edges.
 *   Faked with 4 edge rectangles with alpha gradient (outer dark, inner clear).
 *   Real radial vignette requires a custom shader — this approximation
 *   is cheap and looks good enough at play distance.
 *
 * Both effects are toggleable via Settings (gh_settings prefs).
 * Intensity tuned via Constants.SCANLINE_ALPHA and VIGNETTE_ALPHA.
 */
class ScanlineRenderer(context: Context) {

    fun draw(batch: SpriteBatch, assets: AssetManager,
             screenW: Float, screenH: Float, hudMatrix: FloatArray,
             glitchEnabled: Boolean) {

        val atlas = assets.atlas ?: return

        batch.begin(hudMatrix)

        // ── Scanlines ─────────────────────────────────────────────
        // Draw every other 2px strip dark — creates the scan-line pattern
        val lineH    = 2f
        val lineStep = 4f
        var y = 0f
        while (y < screenH) {
            batch.draw(atlas, 0f, y, screenW, lineH,
                0f, 0f, 0.001f, 0.001f,
                0f, 0f, 0f, Constants.SCANLINE_ALPHA)
            y += lineStep
        }

        // ── Vignette — 4 edge gradients ───────────────────────────
        val edgeW = screenW * 0.22f
        val edgeH = screenH * 0.28f
        val va    = Constants.VIGNETTE_ALPHA

        // Left edge
        batch.draw(atlas, 0f, 0f, edgeW, screenH,
            0f, 0f, 0.001f, 0.001f, 0f, 0f, 0f, va)
        // Right edge
        batch.draw(atlas, screenW - edgeW, 0f, edgeW, screenH,
            0f, 0f, 0.001f, 0.001f, 0f, 0f, 0f, va)
        // Top edge
        batch.draw(atlas, 0f, 0f, screenW, edgeH,
            0f, 0f, 0.001f, 0.001f, 0f, 0f, 0f, va * 0.6f)
        // Bottom edge
        batch.draw(atlas, 0f, screenH - edgeH, screenW, edgeH,
            0f, 0f, 0.001f, 0.001f, 0f, 0f, 0f, va * 0.6f)

        // ── Glitch chromatic aberration ───────────────────────────
        // A very faint persistent RGB fringe — looks like a slightly
        // misaligned CRT beam. Only if glitch FX enabled in settings.
        if (glitchEnabled) {
            batch.draw(atlas, -1.5f, 0f, screenW, screenH,
                0f, 0f, 0.001f, 0.001f, 1f, 0f, 0f, 0.018f)
            batch.draw(atlas, 1.5f, 0f, screenW, screenH,
                0f, 0f, 0.001f, 0.001f, 0f, 0f, 1f, 0.018f)
        }

        batch.end()
    }
}