package com.glitchhollow.gl

import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLUtils

/**
 * Loads a PNG from assets into a GL texture on the GPU.
 *
 * GL concept: The CPU can't draw directly to the screen — it has to upload
 * image data to the GPU's memory first. A "texture" is that uploaded image.
 * Once uploaded, you reference it by an integer handle and GL reads pixels
 * from it at draw time.
 *
 * Pixel art critical setting:
 *   GL_NEAREST filtering — no smoothing, no bilinear interpolation.
 *   This is what makes pixel art look crisp instead of blurry.
 *   If you ever see blurry sprites, this is the first thing to check.
 *
 * Usage:
 *   val tex = Texture(context, "sprites/player.png")
 *   tex.bind()   // before drawing sprites that use this texture
 */
class Texture(context: Context, assetPath: String) {

    val handle: Int
    val width: Int
    val height: Int

    init {
        // Load PNG into a CPU-side Bitmap
        val bitmap = context.assets.open(assetPath).use { stream ->
            BitmapFactory.decodeStream(stream)
                ?: error("Failed to decode texture: $assetPath")
        }
        width  = bitmap.width
        height = bitmap.height

        // Generate a GL texture handle (just an integer ID)
        val handles = IntArray(1)
        GLES20.glGenTextures(1, handles, 0)
        handle = handles[0]
        check(handle != 0) { "glGenTextures failed for $assetPath" }

        // Bind the texture so subsequent GL texture calls affect this one
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, handle)

        // NEAREST = no smoothing — essential for pixel art crispness
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_NEAREST
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_NEAREST
        )

        // CLAMP_TO_EDGE — pixels at the edge of a sprite don't bleed into
        // neighbouring sprites on the atlas (important in Phase 3)
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE
        )

        // Upload the Bitmap pixels to the GPU
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)

        // Bitmap is now on the GPU — recycle the CPU copy to free RAM
        bitmap.recycle()

        // Unbind
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }

    /** Bind to texture unit 0 before a draw call */
    fun bind(unit: Int = 0) {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + unit)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, handle)
    }

    fun unbind() {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }

    /** Release GPU memory — call when the texture is no longer needed */
    fun dispose() {
        GLES20.glDeleteTextures(1, intArrayOf(handle), 0)
    }
}