package com.glitchhollow.gl

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import com.glitchhollow.screen.MainMenuScreen
import com.glitchhollow.screen.ScreenManager
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class GLRenderer(private val context: Context) : GLSurfaceView.Renderer {

    lateinit var batch:  SpriteBatch
        private set
    lateinit var assets: AssetManager
        private set

    private lateinit var shader: ShaderProgram
    private var screenWidth  = 0
    private var screenHeight = 0
    private var lastFrameTime = 0L

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.051f, 0.039f, 0.102f, 1.0f)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        shader = ShaderProgram(context, "shaders/sprite.vert", "shaders/sprite.frag")
        batch  = SpriteBatch(shader)
        assets = AssetManager(context)

        lastFrameTime = System.nanoTime()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        screenWidth  = width
        screenHeight = height
        GLES20.glViewport(0, 0, width, height)

        // Launch main menu once we know the screen size
        if (ScreenManager.current == null) {
            ScreenManager.set(
                MainMenuScreen(context, assets, batch, width, height)
            )
        }
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = System.nanoTime()
        val dt  = ((now - lastFrameTime) / 1_000_000_000f).coerceAtMost(0.1f)
        lastFrameTime = now

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        val screen = ScreenManager.current ?: return
        screen.update(dt)
        screen.render()
    }
}