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

class WorldSelectScreen(
    private val context: Context,
    private val assets:  AssetManager,
    private val audio:   AudioManager,
    private val batch:   SpriteBatch,
    private val screenW: Int,
    private val screenH: Int
) : Screen {

    private val sw = screenW.toFloat()
    private val sh = screenH.toFloat()
    private val hm = buildHudMatrix(sw, sh)
    private val bg = ParallaxBackground(sw, sh, 0)
    private val tx = ScreenTransition()
    private val save = SaveManager(context)

    private val WORLDS = 4

    private val cardW  = sw * 0.42f
    private val cardH  = sh * 0.22f
    private val colGap = sw * 0.04f
    private val rowGap = sh * 0.04f
    private val gridX  = (sw - cardW * 2 - colGap) / 2f
    private val gridY  = sh * 0.20f

    private val worldNames = arrayOf(
        "STATIC FIELDS", "VOID CIRCUIT",
        "CORRUPT COAST", "CORE BREACH"
    )

    private var time = 0f

    private fun cardRect(w: Int): FloatArray {
        val col = (w - 1) % 2
        val row = (w - 1) / 2
        return floatArrayOf(
            gridX + col * (cardW + colGap),
            gridY + row * (cardH + rowGap),
            cardW, cardH
        )
    }

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
        UIHelpers.header(batch, font, "SELECT WORLD", sh * 0.09f, sw)
        UIHelpers.divider(batch, atlas, sh * 0.15f + 2f, sw)

        for (w in 1..WORLDS) {
            val r      = cardRect(w)
            val locked = !save.isUnlocked(w, 1) && w != 1
            val stars  = (1..SaveManager.LEVELS_PER_WORLD).sumOf { l ->
                save.getStars(w, l).count { it }
            }
            val maxS  = SaveManager.LEVELS_PER_WORLD * 3
            val pulse = if (!locked) (Math.sin(time * 2.0 + w) * 0.1f + 0.9f).toFloat() else 1f

            val fill   = if (locked) UIHelpers.DARK else UIHelpers.MID
            val border = if (locked) UIHelpers.PURPLE else UIHelpers.CYAN
            UIHelpers.rectOutline(batch, atlas,
                r[0], r[1], r[2], r[3], 3f, fill, 0.85f * pulse, border, pulse)

            font.draw(batch, "W$w", r[0] + 16f, r[1] + 14f,
                3f, border[0], border[1], border[2], if (locked) 0.4f else 1f)

            font.draw(batch, worldNames[w - 1],
                r[0] + r[2] / 2f, r[1] + r[3] * 0.42f,
                1f, 1f, 1f, 1f, if (locked) 0.3f else 0.9f, align = 0)

            if (!locked) {
                font.draw(batch, "$stars/$maxS STARS",
                    r[0] + r[2] / 2f, r[1] + r[3] * 0.68f,
                    1f, UIHelpers.YELLOW[0], UIHelpers.YELLOW[1], UIHelpers.YELLOW[2],
                    0.85f, align = 0)
            } else {
                font.draw(batch, "LOCKED",
                    r[0] + r[2] / 2f, r[1] + r[3] * 0.65f,
                    1f, UIHelpers.PURPLE[0], UIHelpers.PURPLE[1], UIHelpers.PURPLE[2],
                    0.6f, align = 0)
            }
        }

        UIHelpers.button(batch, atlas, font,
            sw * 0.1f, sh * 0.90f, sw * 0.8f, 60f, "BACK")

        batch.end()
        tx.draw(batch, assets, sw, sh, hm)
    }

    override fun onTouch(x: Float, y: Float, action: Int) {
        if (action != MotionEvent.ACTION_DOWN || tx.isRunning) return

        for (w in 1..WORLDS) {
            val r = cardRect(w)
            if (UIHelpers.hits(x, y, r[0], r[1], r[2], r[3])) {
                if (!save.isUnlocked(w, 1) && w != 1) return
                audio.play(SoundEvent.MENU_SELECT)
                tx.start {
                    ScreenManager.set(
                        LevelSelectScreen(context, assets, audio, batch, screenW, screenH, w)
                    )
                }
                return
            }
        }

        if (UIHelpers.hits(x, y, sw * 0.1f, sh * 0.90f, sw * 0.8f, 60f)) {
            audio.play(SoundEvent.MENU_BACK)
            tx.start {
                ScreenManager.set(
                    MainMenuScreen(context, assets, audio, batch, screenW, screenH)
                )
            }
        }
    }

    override fun dispose() {}
}