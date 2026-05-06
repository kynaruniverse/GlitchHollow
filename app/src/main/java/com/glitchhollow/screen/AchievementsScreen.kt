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

class AchievementsScreen(
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
    private val bg = ParallaxBackground(sw, sh, 3)
    private val tx = ScreenTransition()
    private val save = SaveManager(context)

    data class Achievement(val title: String, val desc: String, val earned: Boolean)

    private val achievements: List<Achievement> by lazy {
        val total = save.totalStars()
        val max   = save.maxStars()
        listOf(
            Achievement("FIRST STEP",    "Complete W1-L1",
                save.isUnlocked(1, 2)),
            Achievement("STAR SEEKER",   "Earn 10 stars",
                total >= 10),
            Achievement("STAR HUNTER",   "Earn 36 stars",
                total >= 36),
            Achievement("PERFECT",       "Earn all $max stars",
                total >= max),
            Achievement("COIN HOARDER",  "Find 12 coins",
                (1..4).sumOf { w -> (1..6).count { l -> save.getCoin(w, l) } } >= 12),
            Achievement("COMPLETIONIST", "Find all 24 coins",
                (1..4).all { w -> (1..6).all { l -> save.getCoin(w, l) } }),
            Achievement("WORLD CLEAR",   "Complete World 1",
                save.isUnlocked(2, 1)),
            Achievement("VOID WALKER",   "Complete all 4 worlds",
                save.isUnlocked(4, 6))
        )
    }

    private val rowH  = sh * 0.085f
    private val rowW  = sw * 0.85f
    private val rowX  = (sw - rowW) / 2f
    private val firstY = sh * 0.20f
    private val gap   = sh * 0.01f

    private val backBtnY = sh * 0.92f
    private val backBtnW = sw * 0.8f
    private val backBtnX = sw * 0.1f

    override fun update(dt: Float) { bg.update(dt); tx.update(dt) }

    override fun render() {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        bg.draw(batch, assets, hm)

        val atlas = assets.atlas ?: return
        val font  = assets.font

        batch.begin(hm)

        UIHelpers.divider(batch, atlas, sh * 0.15f, sw)
        UIHelpers.header(batch, font, "ACHIEVEMENTS", sh * 0.09f, sw)
        UIHelpers.divider(batch, atlas, sh * 0.15f + 2f, sw)

        achievements.forEachIndexed { i, ach ->
            val ry     = firstY + i * (rowH + gap)
            val border = if (ach.earned) UIHelpers.CYAN else UIHelpers.PURPLE
            val alpha  = if (ach.earned) 1f else 0.45f
            UIHelpers.rectOutline(batch, atlas, rowX, ry, rowW, rowH, 2f,
                UIHelpers.MID, 0.85f, border)
            font.draw(batch, ach.title,
                rowX + 14f, ry + rowH * 0.18f,
                1f, border[0], border[1], border[2], alpha)
            font.draw(batch, ach.desc,
                rowX + 14f, ry + rowH * 0.56f,
                1f, 1f, 1f, 1f, alpha * 0.7f)
            if (ach.earned) {
                font.draw(batch, "OK",
                    rowX + rowW - 40f, ry + rowH * 0.30f,
                    1f, UIHelpers.YELLOW[0], UIHelpers.YELLOW[1], UIHelpers.YELLOW[2], 1f)
            }
        }

        UIHelpers.button(batch, atlas, font, backBtnX, backBtnY, backBtnW, 56f, "BACK")

        batch.end()
        tx.draw(batch, assets, sw, sh, hm)
    }

    override fun onTouch(x: Float, y: Float, action: Int) {
        if (action != MotionEvent.ACTION_DOWN || tx.isRunning) return
        if (UIHelpers.hits(x, y, backBtnX, backBtnY, backBtnW, 56f)) {
            audio.play(SoundEvent.MENU_BACK)
            tx.start {
                ScreenManager.set(
                    ProfileScreen(context, assets, audio, batch, screenW, screenH)
                )
            }
        }
    }

    override fun dispose() {}
}