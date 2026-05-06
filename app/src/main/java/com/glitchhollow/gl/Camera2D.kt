package com.glitchhollow.gl

import com.glitchhollow.core.Constants

/**
 * Produces an orthographic projection matrix for 2D rendering.
 *
 * GL concept: GL's clip space goes from -1 to +1 on every axis.
 * An orthographic projection matrix converts your world coordinates
 * (e.g. pixel positions 0–1080) into that -1 to +1 range.
 *
 * For a 2D game this matrix is the only transform you need —
 * no perspective, no depth.
 *
 * This also handles:
 *   - Camera scroll (offsetting the world by camera position)
 *   - Screen shake (adding a random offset each frame)
 *
 * The matrix is a FloatArray(16) in column-major order (GL standard).
 * You pass it to ShaderProgram.setUniformMat4("u_projTrans", matrix).
 */
class Camera2D(var screenWidth: Int, var screenHeight: Int) {

    // World position of the camera's top-left corner
    var x: Float = 0f
    var y: Float = 0f

    private var targetX: Float = 0f
    private var targetY: Float = 0f

    // Level bounds in pixels — camera won't scroll past these
    private var levelPixelWidth:  Int = Int.MAX_VALUE
    private var levelPixelHeight: Int = Int.MAX_VALUE

    // Screen shake
    private var shakeFrames:    Int   = 0
    private var shakeIntensity: Float = 0f

    // The projection matrix — updated every frame
    val matrix = FloatArray(16)

    fun setLevelSize(cols: Int, rows: Int) {
        levelPixelWidth  = cols * Constants.TILE_SIZE
        levelPixelHeight = rows * Constants.TILE_SIZE
    }

    fun update(playerCX: Float, playerCY: Float) {
        val hudH = Constants.HUD_HEIGHT.toFloat()

        // Centre player on screen below HUD
        targetX = playerCX - screenWidth  / 2f
        targetY = playerCY - (screenHeight - hudH) / 2f - hudH

        // Clamp to level bounds
        targetX = targetX.coerceIn(0f, (levelPixelWidth  - screenWidth).toFloat().coerceAtLeast(0f))
        targetY = targetY.coerceIn(0f, (levelPixelHeight - screenHeight + hudH.toInt()).toFloat().coerceAtLeast(0f))

        // Smooth follow (lerp)
        x += (targetX - x) * Constants.CAM_LERP
        y += (targetY - y) * Constants.CAM_LERP

        // Screen shake offset
        var shakeX = 0f
        var shakeY = 0f
        if (shakeFrames > 0) {
            val sign = if (shakeFrames % 2 == 0) 1f else -1f
            shakeX = shakeIntensity * sign
            shakeY = shakeIntensity * sign * 0.5f
            shakeFrames--
        }

        // Build orthographic matrix with camera offset
        buildOrtho(x + shakeX, y + shakeY)
    }

    fun shake(intensity: Float, frames: Int) {
        shakeIntensity = intensity
        shakeFrames    = frames
    }

    /** Convert world X to screen X (for hit-testing, UI placement) */
    fun toScreenX(worldX: Float) = worldX - x
    fun toScreenY(worldY: Float) = worldY - y + Constants.HUD_HEIGHT

    fun isVisible(wx: Float, wy: Float, w: Float, h: Float): Boolean =
        wx + w > x && wx < x + screenWidth &&
        wy + h > y && wy < y + screenHeight

    // ── Matrix math ───────────────────────────────────────────────

    /**
     * Builds a column-major 4×4 orthographic projection matrix.
     *
     * An ortho matrix maps:
     *   left..right  → -1..+1 (X axis)
     *   bottom..top  → -1..+1 (Y axis, note: bottom > top for Y-down coords)
     *   near..far    → -1..+1 (Z axis, irrelevant for 2D)
     *
     * We set left=camX, right=camX+screenW, top=camY, bottom=camY+screenH
     * so that world pixel (camX, camY) maps to clip (-1, +1) — top-left.
     */
    private fun buildOrtho(camX: Float, camY: Float) {
        val left   = camX
        val right  = camX + screenWidth
        val top    = camY
        val bottom = camY + screenHeight
        val near   = -1f
        val far    =  1f

        val rml = right - left
        val tmb = top   - bottom   // note: top - bottom (inverted Y)
        val fmn = far   - near

        // Column-major layout — index = col*4 + row
        matrix[ 0] =  2f / rml;  matrix[ 1] = 0f;          matrix[ 2] = 0f;          matrix[ 3] = 0f
        matrix[ 4] =  0f;         matrix[ 5] = 2f / tmb;    matrix[ 6] = 0f;          matrix[ 7] = 0f
        matrix[ 8] =  0f;         matrix[ 9] = 0f;           matrix[10] = -2f / fmn;   matrix[11] = 0f
        matrix[12] = -(right + left) / rml
        matrix[13] = -(top + bottom) / tmb
        matrix[14] = -(far + near)   / fmn
        matrix[15] = 1f
    }

    /** HUD matrix — no camera scroll, origin at screen top-left */
    fun buildHudMatrix(): FloatArray {
        val m = FloatArray(16)
        val left   = 0f;  val right  = screenWidth.toFloat()
        val top    = 0f;  val bottom = screenHeight.toFloat()
        val near   = -1f; val far    = 1f
        val rml = right - left;  val tmb = top - bottom;  val fmn = far - near
        m[ 0] =  2f/rml; m[ 1]=0f;       m[ 2]=0f;        m[ 3]=0f
        m[ 4] =  0f;      m[ 5]=2f/tmb;  m[ 6]=0f;        m[ 7]=0f
        m[ 8] =  0f;      m[ 9]=0f;      m[10]=-2f/fmn;   m[11]=0f
        m[12] = -(right+left)/rml
        m[13] = -(top+bottom)/tmb
        m[14] = -(far+near)/fmn
        m[15] = 1f
        return m
    }
}