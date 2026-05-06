package com.glitchhollow.gl

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

/**
 * SpriteBatch — draws many sprites in as few GPU draw calls as possible.
 *
 * GL concept — why batching matters:
 * Each call to glDrawElements() has overhead: the CPU has to talk to the GPU
 * driver. On mobile, even 100 separate draw calls per frame can cause jank.
 * A sprite batch solves this by collecting ALL sprite data into one big
 * float buffer and sending it to the GPU in a SINGLE draw call.
 *
 * How it works:
 * 1. begin()       — reset the buffer, bind the shader
 * 2. draw(...)     — add a sprite's 4 corners to the buffer (no GPU call yet)
 * 3. flush()/end() — send the entire buffer to GPU in one glDrawElements()
 *
 * Buffer layout per vertex (9 floats):
 *   [x, y,  u, v,  r, g, b, a]
 *    pos    uv     colour
 *
 * Each sprite = 4 vertices = 36 floats
 * Each sprite = 2 triangles = 6 indices
 *
 * Max sprites: 1000 per batch (plenty for this game)
 */
class SpriteBatch(private val shader: ShaderProgram) {

    companion object {
        private const val MAX_SPRITES    = 1000
        private const val FLOATS_PER_VERTEX = 8   // x,y, u,v, r,g,b,a
        private const val VERTICES_PER_SPRITE = 4
        private const val INDICES_PER_SPRITE  = 6
        private const val BUFFER_SIZE = MAX_SPRITES * VERTICES_PER_SPRITE * FLOATS_PER_VERTEX
    }

    // CPU-side float buffer — filled by draw() calls
    private val vertices: FloatBuffer = ByteBuffer
        .allocateDirect(BUFFER_SIZE * 4) // 4 bytes per float
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()

    // Index buffer — tells GL how to form triangles from 4 vertices
    // Pattern per sprite: [0,1,2, 2,3,0]  (two triangles sharing an edge)
    // This never changes, so we build it once
    private val indices: ShortBuffer = run {
        val buf = ByteBuffer
            .allocateDirect(MAX_SPRITES * INDICES_PER_SPRITE * 2) // 2 bytes per short
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()
        for (i in 0 until MAX_SPRITES) {
            val base = (i * VERTICES_PER_SPRITE).toShort()
            buf.put(base);            buf.put((base + 1).toShort())
            buf.put((base + 2).toShort())
            buf.put((base + 2).toShort()); buf.put((base + 3).toShort())
            buf.put(base)
        }
        buf.flip()
        buf
    }

    // GL buffer object handles (VBO + IBO)
    private val vboHandle: Int
    private val iboHandle: Int

    // Attribute locations from the shader
    private val aPosition: Int
    private val aTexCoord: Int
    private val aColor:    Int

    // State
    private var spriteCount   = 0
    private var currentTexture: Int = -1
    private var drawing       = false
    private var projMatrix: FloatArray? = null

    init {
        // Get attribute locations from the compiled shader
        aPosition = shader.getAttribLocation("a_position")
        aTexCoord = shader.getAttribLocation("a_texCoord")
        aColor    = shader.getAttribLocation("a_color")

        // Generate VBO (vertex buffer object) and IBO (index buffer object)
        val handles = IntArray(2)
        GLES20.glGenBuffers(2, handles, 0)
        vboHandle = handles[0]
        iboHandle = handles[1]

        // Upload the index buffer to the GPU — it never changes
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, iboHandle)
        GLES20.glBufferData(
            GLES20.GL_ELEMENT_ARRAY_BUFFER,
            indices.capacity() * 2,
            indices,
            GLES20.GL_STATIC_DRAW   // STATIC = upload once, use many times
        )
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)
    }

    // ── Public API ────────────────────────────────────────────────

    /**
     * Call before any draw() calls.
     * @param projectionMatrix the Camera2D.matrix for world-space sprites,
     *                         or Camera2D.buildHudMatrix() for HUD elements
     */
    fun begin(projectionMatrix: FloatArray) {
        check(!drawing) { "SpriteBatch.end() must be called before begin()" }
        drawing     = true
        projMatrix  = projectionMatrix
        spriteCount = 0
        vertices.clear()
    }

    /**
     * Queue a sprite for drawing.
     *
     * @param texture  the Texture to sample from
     * @param x        world X of the sprite's top-left corner
     * @param y        world Y of the sprite's top-left corner
     * @param w        width in pixels
     * @param h        height in pixels
     * @param u0,v0    UV top-left     (0f..1f) — default full texture
     * @param u1,v1    UV bottom-right (0f..1f)
     * @param r,g,b,a  tint colour — (1,1,1,1) = no tint
     * @param flipX    mirror horizontally (for left-facing sprites)
     */
    fun draw(
        texture: Texture,
        x: Float, y: Float,
        w: Float, h: Float,
        u0: Float = 0f, v0: Float = 0f,
        u1: Float = 1f, v1: Float = 1f,
        r: Float = 1f, g: Float = 1f, b: Float = 1f, a: Float = 1f,
        flipX: Boolean = false
    ) {
        check(drawing) { "SpriteBatch.begin() must be called before draw()" }

        // If the texture changed, flush what we have first
        // (GL can only sample one texture per draw call in ES 2.0 without extensions)
        if (texture.handle != currentTexture && spriteCount > 0) {
            flush()
        }
        currentTexture = texture.handle

        // If the buffer is full, flush before adding more
        if (spriteCount >= MAX_SPRITES) flush()

        // UV flip for left-facing sprites
        val left  = if (flipX) u1 else u0
        val right = if (flipX) u0 else u1

        // Add 4 vertices (corners of the sprite quad):
        // Top-left
        vertices.put(x);     vertices.put(y)
        vertices.put(left);  vertices.put(v0)
        vertices.put(r);     vertices.put(g); vertices.put(b); vertices.put(a)

        // Top-right
        vertices.put(x + w); vertices.put(y)
        vertices.put(right); vertices.put(v0)
        vertices.put(r);     vertices.put(g); vertices.put(b); vertices.put(a)

        // Bottom-right
        vertices.put(x + w); vertices.put(y + h)
        vertices.put(right); vertices.put(v1)
        vertices.put(r);     vertices.put(g); vertices.put(b); vertices.put(a)

        // Bottom-left
        vertices.put(x);     vertices.put(y + h)
        vertices.put(left);  vertices.put(v1)
        vertices.put(r);     vertices.put(g); vertices.put(b); vertices.put(a)

        spriteCount++
    }

    /** Flush remaining sprites and end the batch */
    fun end() {
        check(drawing) { "SpriteBatch.begin() must be called before end()" }
        if (spriteCount > 0) flush()
        drawing = false
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)
    }

    // ── Internal flush ────────────────────────────────────────────

    private fun flush() {
        if (spriteCount == 0) return

        shader.bind()
        shader.setUniformMat4("u_projTrans", projMatrix!!)
        shader.setUniformi("u_texture", 0) // texture unit 0

        // Bind texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, currentTexture)

        // Upload vertex data to VBO
        vertices.flip()
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboHandle)
        GLES20.glBufferData(
            GLES20.GL_ARRAY_BUFFER,
            vertices.limit() * 4,
            vertices,
            GLES20.GL_DYNAMIC_DRAW  // DYNAMIC = changes every frame
        )

        // Tell GL how to read the vertex buffer
        // Stride = total bytes per vertex = 8 floats × 4 bytes = 32
        val stride = FLOATS_PER_VERTEX * 4

        GLES20.glEnableVertexAttribArray(aPosition)
        GLES20.glVertexAttribPointer(
            aPosition, 2, GLES20.GL_FLOAT, false, stride,
            0 * 4  // offset 0 bytes — position is first
        )

        GLES20.glEnableVertexAttribArray(aTexCoord)
        GLES20.glVertexAttribPointer(
            aTexCoord, 2, GLES20.GL_FLOAT, false, stride,
            2 * 4  // offset 8 bytes — after x,y
        )

        GLES20.glEnableVertexAttribArray(aColor)
        GLES20.glVertexAttribPointer(
            aColor, 4, GLES20.GL_FLOAT, false, stride,
            4 * 4  // offset 16 bytes — after x,y,u,v
        )

        // Draw using the pre-built index buffer
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, iboHandle)
        GLES20.glDrawElements(
            GLES20.GL_TRIANGLES,
            spriteCount * INDICES_PER_SPRITE,
            GLES20.GL_UNSIGNED_SHORT,
            0
        )

        // Reset for next batch
        spriteCount = 0
        vertices.clear()
    }

    // ── Cleanup ───────────────────────────────────────────────────

    fun dispose() {
        GLES20.glDeleteBuffers(2, intArrayOf(vboHandle, iboHandle), 0)
        shader.dispose()
    }
}