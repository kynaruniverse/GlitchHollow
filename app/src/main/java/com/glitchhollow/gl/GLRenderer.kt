package com.glitchhollow.gl

import android.content.Context
import android.content.SharedPreferences
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import com.glitchhollow.screen.MainMenuScreen
import com.glitchhollow.screen.ScreenManager
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class GLRenderer(private val context: Context) : GLSurfaceView.Renderer {

    lateinit var batch: SpriteBatch
    lateinit var assets: AssetManager
    lateinit var audio: AudioManager

    private lateinit var shader: ShaderProgram
    private lateinit var scanlines: ScanlineRenderer

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.02f, 0.01f, 0.05f, 1f)

        try {
            shader = ShaderProgram(context, "shaders/sprite.vert", "shaders/sprite.frag")
            batch = SpriteBatch(shader)
            assets = AssetManager(context)
            audio = AudioManager(context)
            scanlines = ScanlineRenderer(context)
        } catch (e: Exception) {
            // LAST RESORT: prevent crash loop
            throw RuntimeException("GL init failed", e)
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)

        if (ScreenManager.current == null) {
            ScreenManager.set(
                MainMenuScreen(
                    context, assets, audio, batch,
                    width, height,
                    renderer = this
                )
            )
        }
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        ScreenManager.current?.update(1f / 60f)
        ScreenManager.current?.render()
    }
}
