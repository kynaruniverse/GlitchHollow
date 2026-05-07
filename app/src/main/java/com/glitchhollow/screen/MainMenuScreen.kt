package com.glitchhollow.screen

import android.content.Context
import android.opengl.GLES20
import android.view.MotionEvent
import com.glitchhollow.core.SaveManager
import com.glitchhollow.core.SoundEvent
import com.glitchhollow.gl.AssetManager
import com.glitchhollow.gl.AudioManager
import com.glitchhollow.gl.GLRenderer
import com.glitchhollow.gl.ParallaxBackground
import com.glitchhollow.gl.ScreenTransition
import com.glitchhollow.gl.SpriteBatch
import com.glitchhollow.gl.UIHelpers
import com.glitchhollow.gl.Camera2D

class MainMenuScreen(
    private val context:  Context,
    private val assets:   AssetManager,
    private val audio:    AudioManager,
    private val batch:    SpriteBatch,
    private val screenW:  Int,
    private val screenH:  Int,
    private val renderer: GLRenderer? = null
) : Screen {

    private val sw = screenW.toFloat()
    private val sh = screenH.toFloat()

    private val camera = Camera2D(screenW, screenH)
    private val hm = camera.buildHudMatrix()

    private val bg         = ParallaxBackground(sw, sh, worldIndex = 0)
    private val transition = ScreenTransition()
    private val save       = SaveManager(context)

    private val btnW   = sw * 0.7f
    private val btnH   = 72f
    private val btnX   = (sw - btnW) / 2f
    private val titleY = sh * 0.22f
    private val btn1Y  = sh * 0.50f
    private val btn2Y  = btn1Y + btnH + 20f
    private val btn3Y  = btn2Y + btnH + 20f
    private val btn4Y  = btn3Y + btnH + 20f

    private var time = 0f

    init {
        audio.playBgm("menu")
    }

    override fun update(dt: Float) {
        time += dt
        bg.update(dt)
        transition.update(dt)
    }

    override fun render() {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        bg.draw(batch, assets, hm)

        val atlas = assets.atlas ?: return
        val font  = assets.font

        batch.begin(hm)

        val glitchX = Math.sin(time * 3.0).toFloat() * 3f

        // Title shadow
        font.draw(batch, "GLITCH", sw / 2f + glitchX + 3f, titleY + 3f,
            5f, 0f, 0f, 0f, 0.6f, align = 0)
        font.draw(batch, "HOLLOW", sw / 2f - glitchX + 3f,
            titleY + font.lineHeight(5f) + 8f + 3f,
            5f, 0f, 0f, 0f, 0.6f, align = 0)

        // Cyan layer
        font.draw(batch, "GLITCH", sw / 2f + glitchX - 2f, titleY,
            5f, 0f, 0.96f, 1f, 0.5f, align = 0)
        font.draw(batch, "HOLLOW", sw / 2f - glitchX - 2f,
            titleY + font.lineHeight(5f) + 8f,
            5f, 0f, 0.96f, 1f, 0.5f, align = 0)

        // White layer
        font.draw(batch, "GLITCH", sw / 2f + glitchX, titleY,
            5f, 1f, 1f, 1f, 1f, align = 0)
        font.draw(batch, "HOLLOW", sw / 2f - glitchX,
            titleY + font.lineHeight(5f) + 8f,
            5f, 1f, 1f, 1f, 1f, align = 0)

        // Subtitle
        font.draw(batch, "A GLITCH IN THE DARK",
            sw / 2f, titleY + font.lineHeight(5f) * 2f + 20f,
            1f, UIHelpers.PURPLE[0], UIHelpers.PURPLE[1], UIHelpers.PURPLE[2],
            0.9f, align = 0)

        UIHelpers.button(batch, atlas, font, btnX, btn1Y, btnW, btnH, "PLAY")
        UIHelpers.button(batch, atlas, font, btnX, btn2Y, btnW, btnH, "WORLDS")
        UIHelpers.button(batch, atlas, font, btnX, btn3Y, btnW, btnH, "PROFILE")
        UIHelpers.button(batch, atlas, font, btnX, btn4Y, btnW, btnH, "SETTINGS")

        val total = save.totalStars()
        val max   = save.maxStars()
        font.draw(batch, "STARS  $total / $max",
            sw / 2f, sh - 60f,
            1f, UIHelpers.YELLOW[0], UIHelpers.YELLOW[1], UIHelpers.YELLOW[2],
            0.8f, align = 0)

        font.draw(batch, "V2.0", sw - 60f, sh - 36f,
            1f, UIHelpers.PURPLE[0], UIHelpers.PURPLE[1], UIHelpers.PURPLE[2], 0.5f)

        batch.end()

        transition.draw(batch, assets, sw, sh, hm)
    }

    override fun onTouch(x: Float, y: Float, action: Int) {
        if (action != MotionEvent.ACTION_DOWN || transition.isRunning) return

        when {
            UIHelpers.hits(x, y, btnX, btn1Y, btnW, btnH) -> {
                audio.play(SoundEvent.MENU_SELECT)
                val pt = save.getResumePoint()
                transition.start {
                    ScreenManager.set(
                        GameScreen(
                            context, assets, audio, batch,
                            pt[0], pt[1], screenW, screenH
                        )
                    )
                }
            }
            UIHelpers.hits(x, y, btnX, btn2Y, btnW, btnH) -> {
                audio.play(SoundEvent.MENU_SELECT)
                transition.start {
                    ScreenManager.set(
                        WorldSelectScreen(
                            context, assets, audio, batch,
                            screenW, screenH
                        )
                    )
                }
            }
            UIHelpers.hits(x, y, btnX, btn3Y, btnW, btnH) -> {
                audio.play(SoundEvent.MENU_SELECT)
                transition.start {
                    ScreenManager.set(
                        ProfileScreen(
                            context, assets, audio, batch,
                            screenW, screenH
                        )
                    )
                }
            }
            UIHelpers.hits(x, y, btnX, btn4Y, btnW, btnH) -> {
                audio.play(SoundEvent.MENU_SELECT)
                transition.start {
                    ScreenManager.set(
                        SettingsScreen(
                            context, assets, audio, batch,
                            screenW, screenH
                        )
                    )
                }
            }
        }
    }

    override fun dispose() {}
}