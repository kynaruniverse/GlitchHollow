package com.glitchhollow.screen

import android.content.Context
import android.view.MotionEvent
import com.glitchhollow.core.SoundEvent
import com.glitchhollow.gl.AssetManager
import com.glitchhollow.gl.AudioManager
import com.glitchhollow.gl.GLRenderer
import com.glitchhollow.gl.ScreenTransition
import com.glitchhollow.gl.SpriteBatch
import com.glitchhollow.gl.UIHelpers

class PauseScreen(
    private val context:    Context,
    private val assets:     AssetManager,
    private val audio:      AudioManager,
    private val batch:      SpriteBatch,
    private val screenW:    Int,
    private val screenH:    Int,
    private val gameScreen: GameScreen,
    private val renderer:   GLRenderer? = null
) : Screen {

    private val sw = screenW.toFloat()
    private val sh = screenH.toFloat()

    private val camera = Camera2D(screenW, screenH)
    private val hm = camera.buildHudMatrix()
    private val tx = ScreenTransition()

    private val panelW  = sw * 0.75f
    private val panelH  = sh * 0.44f
    private val panelX  = (sw - panelW) / 2f
    private val panelY  = (sh - panelH) / 2f

    private val btnW     = panelW * 0.8f
    private val btnH     = 64f
    private val btnX     = panelX + (panelW - btnW) / 2f
    private val resumeY  = panelY + panelH * 0.28f
    private val restartY = resumeY + btnH + 18f
    private val menuY    = restartY + btnH + 18f

    override fun update(dt: Float) {
        tx.update(dt)
    }

    override fun render() {
        // Render the paused game behind the panel
        gameScreen.render()

        val atlas = assets.atlas ?: return
        val font  = assets.font

        batch.begin(hm)

        UIHelpers.rect(batch, atlas, 0f, 0f, sw, sh, UIHelpers.DARK, 0.72f)

        UIHelpers.rectOutline(batch, atlas,
            panelX, panelY, panelW, panelH, 2f,
            UIHelpers.MID, 0.95f, UIHelpers.CYAN)

        UIHelpers.header(batch, font, "PAUSED", panelY + panelH * 0.10f, sw)

        UIHelpers.button(batch, atlas, font, btnX, resumeY,  btnW, btnH, "RESUME")
        UIHelpers.button(batch, atlas, font, btnX, restartY, btnW, btnH, "RESTART")
        UIHelpers.button(batch, atlas, font, btnX, menuY,    btnW, btnH, "MAIN MENU")

        batch.end()
        tx.draw(batch, assets, sw, sh, hm)
    }

    override fun onTouch(x: Float, y: Float, action: Int) {
        if (action != MotionEvent.ACTION_DOWN || tx.isRunning) return

        when {
            UIHelpers.hits(x, y, btnX, resumeY,  btnW, btnH) -> {
                audio.play(SoundEvent.MENU_SELECT)
                ScreenManager.set(gameScreen)
            }
            UIHelpers.hits(x, y, btnX, restartY, btnW, btnH) -> {
                audio.play(SoundEvent.MENU_SELECT)
                gameScreen.restart()
                ScreenManager.set(gameScreen)
            }
            UIHelpers.hits(x, y, btnX, menuY, btnW, btnH) -> {
                audio.play(SoundEvent.MENU_BACK)
                tx.start {
                    ScreenManager.set(
                        MainMenuScreen(
                            context, assets, audio, batch,
                            screenW, screenH,
                            renderer = renderer
                        )
                    )
                }
            }
        }
    }

    override fun dispose() {}
}