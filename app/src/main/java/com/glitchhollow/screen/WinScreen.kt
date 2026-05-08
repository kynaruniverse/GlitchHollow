package com.glitchhollow.screen

import android.content.Context
import android.view.MotionEvent
import com.glitchhollow.core.GameEngine
import com.glitchhollow.core.SaveManager
import com.glitchhollow.core.SoundEvent
import com.glitchhollow.gl.AssetManager
import com.glitchhollow.gl.AudioManager
import com.glitchhollow.gl.GLRenderer
import com.glitchhollow.gl.ScreenTransition
import com.glitchhollow.gl.SpriteBatch
import com.glitchhollow.gl.UIHelpers
import com.glitchhollow.gl.Camera2D

class WinScreen(
    private val context:  Context,
    private val assets:   AssetManager,
    private val audio:    AudioManager,
    private val batch:    SpriteBatch,
    private val screenW:  Int,
    private val screenH:  Int,
    private val engine:   GameEngine,
    private val renderer: GLRenderer? = null
) : Screen {

    private val sw = screenW.toFloat()
    private val sh = screenH.toFloat()

    private val camera = Camera2D(screenW, screenH)
    private val hm: FloatArray = camera.buildHudMatrix()
    private val tx = ScreenTransition()
    private val save = SaveManager(context)

    private var time       = 0f
    private val STAR_DELAY = 0.4f
    private val stars      = engine.starsEarned.copyOf()

    private val panelW  = sw * 0.82f
    private val panelH  = sh * 0.52f
    private val panelX  = (sw - panelW) / 2f
    private val panelY  = sh * 0.18f

    private val nextBtnW = panelW * 0.9f
    private val nextBtnX = (sw - nextBtnW) / 2f
    private val nextBtnY = panelY + panelH + 24f

    override fun update(dt: Float) {
        time += dt
        tx.update(dt)
    }

    override fun render() {
        val atlas = assets.atlas ?: return
        val font  = assets.font

        batch.begin(hm)
        UIHelpers.rect(batch, atlas, 0f, 0f, sw, sh, UIHelpers.DARK, 1f)
        batch.end()

        batch.begin(hm)

        UIHelpers.rectOutline(batch, atlas,
            panelX, panelY, panelW, panelH, 3f,
            UIHelpers.MID, 0.95f, UIHelpers.CYAN)

        UIHelpers.header(batch, font, "LEVEL CLEAR",
            panelY + panelH * 0.10f, sw, UIHelpers.CYAN)

        val elapsed = engine.elapsedMs / 1000
        font.draw(batch,
            "TIME  ${elapsed / 60}:${"%02d".format(elapsed % 60)}",
            sw / 2f, panelY + panelH * 0.28f,
            1f, 1f, 1f, 1f, 0.85f, align = 0)

        val starY    = panelY + panelH * 0.46f
        val starSize = 48f
        val starGap  = starSize + 16f
        val starsX   = sw / 2f - starGap

        for (i in 0..2) {
            val revealed = time > (i + 1) * STAR_DELAY
            val earned   = stars[i] && revealed
            val scale    = if (revealed) {
                val pop = ((time - (i + 1) * STAR_DELAY) * 6f).coerceAtMost(1f)
                0.4f + pop * 0.6f
            } else 0.4f

            val sx     = starsX + i * starGap
            val sz     = starSize * scale
            val offset = (starSize - sz) / 2f

            if (assets.sprites != null) {
                val reg = if (earned) assets.sprites!!.uiStarOn else assets.sprites!!.uiStarOff
                batch.draw(atlas, sx + offset, starY + offset, sz, sz,
                    reg.u0, reg.v0, reg.u1, reg.v1)
            } else {
                val c = if (earned) UIHelpers.YELLOW else UIHelpers.PURPLE
                UIHelpers.rect(batch, atlas, sx + offset, starY + offset, sz, sz, c,
                    if (earned) 1f else 0.3f)
            }
        }

        if (engine.coinGotThisRun && time > STAR_DELAY * 4) {
            font.draw(batch, "COIN EARNED!",
                sw / 2f, panelY + panelH * 0.76f,
                1f, UIHelpers.YELLOW[0], UIHelpers.YELLOW[1], UIHelpers.YELLOW[2],
                0.9f, align = 0)
        }

        if (time > STAR_DELAY * 4) {
            UIHelpers.button(batch, atlas, font,
                nextBtnX, nextBtnY, nextBtnW, 68f, "CONTINUE")
        }

        batch.end()
        tx.draw(batch, assets, sw, sh, hm)
    }

    override fun onTouch(x: Float, y: Float, action: Int) {
        if (action != MotionEvent.ACTION_DOWN || tx.isRunning) return

        // Skip animation on early tap
        if (time < STAR_DELAY * 4) {
            time = STAR_DELAY * 5f
            return
        }

        if (UIHelpers.hits(x, y, nextBtnX, nextBtnY, nextBtnW, 68f)) {
            audio.play(SoundEvent.MENU_SELECT)

            save.saveStars(engine.world, engine.level, stars)
            save.saveCoin(engine.world, engine.level, engine.coinGotThisRun)
            save.onLevelComplete(engine.world, engine.level)

            var nw = engine.world
            var nl = engine.level + 1
            if (nl > SaveManager.LEVELS_PER_WORLD) { nw++; nl = 1 }

            tx.start {
                if (nw > SaveManager.TOTAL_WORLDS) {
                    ScreenManager.set(
                        MainMenuScreen(
                            context, assets, audio, batch,
                            screenW, screenH,
                            renderer = renderer
                        )
                    )
                } else {
                    ScreenManager.set(
                        GameScreen(
                            context, assets, audio, batch,
                            nw, nl, screenW, screenH,
                            renderer = renderer
                        )
                    )
                }
            }
        }
    }

    override fun dispose() {}
}