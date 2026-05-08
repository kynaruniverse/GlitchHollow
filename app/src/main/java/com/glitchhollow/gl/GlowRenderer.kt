package com.glitchhollow.gl

import android.content.Context
import android.opengl.GLES20
import com.glitchhollow.core.Constants

/**
 * Second render pass for additive glow effects.
 *
 * How the two-pass glow works:
 *   Pass 1: normal SpriteBatch draws everything (standard alpha blend)
 *   Pass 2: GlowRenderer switches to additive blend, draws glow quads
 *           on top using glow.frag — larger, softer, colour-only quads
 *           that add light without overwriting the sprite beneath
 *
 * Additive blend mode:
 *   glBlendFunc(GL_SRC_ALPHA, GL_ONE)
 *   "take the source colour scaled by its alpha, ADD it to whatever
 *    is already in the framebuffer" — this is how neon looks
 *
 * GlowRenderer owns its own ShaderProgram (glow.frag) and SpriteBatch.
 * It does NOT share the main batch — different shader, different blend mode.
 */
class GlowRenderer(context: Context) {

    private val glowShader = ShaderProgram(context,
        "shaders/sprite.vert", "shaders/glow.frag")
    private val glowBatch  = SpriteBatch(glowShader)
    private var time       = 0f

    fun update(dt: Float) { time += dt }

    /**
     * Begin a glow pass.
     * Call between passes — after main batch.end(), before screen overlays.
     *
     * Switches blend mode to additive for the duration of this pass.
     * Caller must call endPass() when done.
     */
    fun beginPass(projMatrix: FloatArray) {
        // Switch to additive blending
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE)
        glowShader.bind()
        glowShader.setUniformf("u_time", time)
        glowBatch.begin(projMatrix)
    }

    /**
     * Draw a glow halo around a world-space sprite.
     *
     * @param texture same texture as the sprite
     * @param x sprite top-left x position
     * @param y sprite top-left y position
     * @param w sprite width in pixels
     * @param h sprite height in pixels
     * @param intensity glow brightness
     * @param r red glow component
     * @param g green glow component
     * @param b blue glow component
     * @param u0 left UV
     * @param v0 top UV
     * @param u1 right UV
     * @param v1 bottom UV
     */
 
    fun drawGlow(texture: Texture,
                 x: Float, y: Float, w: Float, h: Float,
                 intensity: Float,
                 r: Float, g: Float, b: Float,
                 u0: Float = 0f, v0: Float = 0f,
                 u1: Float = 1f, v1: Float = 1f) {
        val scale = Constants.GLOW_SCALE
        val padX  = w * (scale - 1f) / 2f
        val padY  = h * (scale - 1f) / 2f
        glowShader.setUniformf("u_intensity", intensity)
        glowBatch.draw(texture,
            x - padX, y - padY,
            w * scale, h * scale,
            u0, v0, u1, v1,
            r, g, b, 1f
        )
    }

    fun endPass() {
        glowBatch.end()
        // Restore standard alpha blending
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
    }

    fun dispose() {
        glowBatch.dispose()
        glowShader.dispose()
    }
}