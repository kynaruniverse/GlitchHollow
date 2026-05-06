package com.glitchhollow.screen

import android.content.Context
import android.opengl.GLES20
import android.view.MotionEvent
import com.glitchhollow.core.SaveManager
import com.glitchhollow.core.SoundEvent
import com.glitchhollow.gl.AssetManager
import com.glitchhollow.gl.AudioManager
import com.glitchhollow.gl.ParallaxBackground
import com.glitchhollow.gl.ScreenTransition
import com.glitchhollow.gl.SpriteBatch
import com.glitchhollow.gl.UIHelpers

class LevelSelectScreen(
    private val context: Context,
    private val assets:  AssetManager,
    private val audio:   AudioManager,
    private val batch:   SpriteBatch,
    private val screenW: Int,
    private val screenH: Int,
    private val world:   Int
) : Screen {

    private val sw = screenW.toFloat()
    private val sh = screenH.toFloat()
    private val hm = buildHudMatrix(sw, sh)
    private val bg = ParallaxBackground(sw, sh, world - 1)
    private val tx = ScreenTransition()
    private val save = SaveManager(context)

    private val LEVELS = SaveManager.LEVELS_PER_WORLD

    private val cardW  = sw * 0.85f
    private val cardH  = sh * 0.105f
    private val cardX  = (sw - cardW) / 2f
    private val firstY = sh * 0.22f
    private val cardGap = sh * 0.015f

    private val worldNames = arrayOf(
        "STATIC FIELDS", "VOID CIRCUIT",
        "CORRUPT COAST", "CORE BREACH"
    )

    private var time = 0f

    override fun update(dt: Float) {
        time += dt
        bg.update(dt)
        tx.update(dt)
    }

    override fun render() {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        bg.draw(batch, assets, hm)

        val atlas = assets.atlas ?: return
        val font  = assets.font

        batch.begin(hm)

        UIHelpers.divider(batch, atlas, sh * 0.15f, sw)
        UIHelpers.header(batch, font,
            "W$world  ${worldNames[(world - 1).coerceIn(0, 3)]}",
            sh * 0.09f, sw)
        UIHelpers.divider(batch, atlas, sh * 0.15f + 2f, sw)

        for (l in 1..LEVELS) {
            val ry     = firstY + (l - 1) * (cardH + cardGap)
            val locked = !save.isUnlocked(world, l)
            val stars  = save.getStars(world, l)
            val coin   = save.getCoin(world, l)

            val border = if (locked) UIHelpers.PURPLE else UIHelpers.CYAN
            UIHelpers.rectOutline(batch, atlas,
                cardX, ry, cardW, cardH, 2f, UIHelpers.MID, 0.8f, border)

            font.draw(batch, "L$l",
                cardX + 18f, ry + (cardH - font.lineHeight(2f)) / 2f,
                2f, border[0], border[1], border[2], if (locked) 0.4f else 1f)

            if (locked) {
                font.draw(batch, "LOCKED",
                    cardX + cardW / 2f + 20f,
                    ry + (cardH - font.lineHeight(1f)) / 2f,
                    1f, UIHelpers.PURPLE[0], UIHelpers.PURPLE[1], UIHelpers.PURPLE[2],
                    0.5f, align = 0)
            } else {
                UIHelpers.drawStars(batch, assets, stars,
                    cardX + cardW * 0.55f, ry + (cardH - 20f) / 2f, 20f)
                if (coin) {
                    UIHelpers.rect(batch, atlas,
                        cardX + cardW - 36f, ry + (cardH - 16f) / 2f,
                        16f, 16f, UIHelpers.YELLOW)
                }
            }
        }

        UIHelpers.button(batch, atlas, font,
            sw * 0.1f, sh * 0.91f, sw * 0.8f, 58f, "BACK")

        batch.end()
        tx.draw(batch, assets, sw, sh, hm)
    }

    override fun onTouch(x: Float, y: Float, action: Int) {
        if (action != MotionEvent.ACTION_DOWN || tx.isRunning) return

        for (l in 1..LEVELS) {
            val ry = firstY + (l - 1) * (cardH + cardGap)
            if (UIHelpers.hits(x, y, cardX, ry, cardW, cardH)) {
                if (!save.isUnlocked(world, l)) return
                audio.play(SoundEvent.MENU_SELECT)
                tx.start {
                    ScreenManager.set(
                        GameScreen(context, assets, audio, batch, world, l, screenW, screenH)
                    )
                }
                return
            }
        }

        if (UIHelpers.hits(x, y, sw * 0.1f, sh * 0.91f, sw * 0.8f, 58f)) {
            audio.play(SoundEvent.MENU_BACK)
            tx.start {
                ScreenManager.set(
                    WorldSelectScreen(context, assets, audio, batch, screenW, screenH)
                )
            }
        }
    }

    override fun dispose() {}
}