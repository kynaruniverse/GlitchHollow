package com.glitchhollow.gl

import android.content.Context
import android.opengl.GLES20
import android.util.Log

/**
 * Compiles a GLSL vertex + fragment shader pair into a GPU program.
 *
 * GL concept: Before the GPU can draw anything, it needs a "program" —
 * two compiled shader files linked together. This class handles that
 * compile → link → validate pipeline and gives you a clean API to
 * set uniforms (data you send to the shader each frame).
 *
 * Usage:
 *   val shader = ShaderProgram(context, "shaders/sprite.vert", "shaders/sprite.frag")
 *   shader.bind()
 *   shader.setUniformMat4("u_projTrans", matrix)
 */
class ShaderProgram(context: Context, vertPath: String, fragPath: String) {

    val programHandle: Int
    private val uniformCache = mutableMapOf<String, Int>()

    init {
        val vertSrc = context.assets.open(vertPath).bufferedReader().readText()
        val fragSrc = context.assets.open(fragPath).bufferedReader().readText()
        programHandle = createProgram(vertSrc, fragSrc)
    }

    // ── Compile + Link ────────────────────────────────────────────

    private fun createProgram(vertSrc: String, fragSrc: String): Int {
        val vert = compileShader(GLES20.GL_VERTEX_SHADER, vertSrc)
        val frag = compileShader(GLES20.GL_FRAGMENT_SHADER, fragSrc)

        val program = GLES20.glCreateProgram()
        check(program != 0) { "glCreateProgram failed" }

        GLES20.glAttachShader(program, vert)
        GLES20.glAttachShader(program, frag)
        GLES20.glLinkProgram(program)

        // Check link status
        val status = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0)
        if (status[0] == 0) {
            val log = GLES20.glGetProgramInfoLog(program)
            GLES20.glDeleteProgram(program)
            error("Shader link failed: $log")
        }

        // Shaders are linked into the program — the individual ones can be deleted
        GLES20.glDeleteShader(vert)
        GLES20.glDeleteShader(frag)

        return program
    }

    private fun compileShader(type: Int, src: String): Int {
        val shader = GLES20.glCreateShader(type)
        check(shader != 0) { "glCreateShader failed for type $type" }

        GLES20.glShaderSource(shader, src)
        GLES20.glCompileShader(shader)

        val status = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            val log = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            error("Shader compile failed: $log")
        }

        return shader
    }

    // ── Bind / Unbind ─────────────────────────────────────────────

    /** Tell GL to use this shader program for subsequent draw calls */
    fun bind() = GLES20.glUseProgram(programHandle)

    fun unbind() = GLES20.glUseProgram(0)

    // ── Attribute locations ───────────────────────────────────────

    /** Get the location of a vertex attribute (e.g. "a_position") */
    fun getAttribLocation(name: String): Int =
        GLES20.glGetAttribLocation(programHandle, name)

    // ── Uniforms ──────────────────────────────────────────────────

    /**
     * Uniforms are values you set from Kotlin that stay constant across
     * all vertices in a single draw call. Examples: the projection matrix,
     * the texture unit number, a tint colour.
     */
    private fun uniformLocation(name: String): Int =
        uniformCache.getOrPut(name) {
            GLES20.glGetUniformLocation(programHandle, name)
        }

    /** Set a 4×4 matrix uniform (e.g. u_projTrans) */
    fun setUniformMat4(name: String, matrix: FloatArray) {
        // transpose=false because we build column-major matrices (GL standard)
        GLES20.glUniformMatrix4fv(uniformLocation(name), 1, false, matrix, 0)
    }

    /** Set a single integer uniform (e.g. texture unit: u_texture = 0) */
    fun setUniformi(name: String, value: Int) {
        GLES20.glUniform1i(uniformLocation(name), value)
    }

    /** Set a float uniform */
    fun setUniformf(name: String, value: Float) {
        GLES20.glUniform1f(uniformLocation(name), value)
    }

    // ── Cleanup ───────────────────────────────────────────────────

    fun dispose() = GLES20.glDeleteProgram(programHandle)
}