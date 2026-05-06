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

class ProfileScreen(
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
    private val bg = ParallaxBackground(sw, sh, 1)
    private val tx = ScreenTransition()
    private val save = SaveManager(context)

    private val btnW  = sw * 0.78f
    private val btnX2 = (sw - btnW) / 2f
    private val achY  = sh * 0.76f
    private val backY = achY + 64f + 18f

    override fun update(dt: Float) {
        bg.update(dt)
        tx.update(dt)
    }

    override fun render() {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        bg.draw(batch, assets, hm)

        val atlas = assets.atlas ?: return
        val font  = assets.font

        val totalStars    = save.totalStars()
        val maxStars      = save.maxStars()
        val totalCoins    = (1..4).sumOf { w -> (1..6).count { l -> save.getCoin(w, l) } }
        val levelsCleared = (1..4).sumOf { w ->
            (1..6).count { l -> save.getStars(w, l).any { it } }
        }
        val pct = if (maxStars > 0) (totalStars * 100) / maxStars else 0

        batch.begin(hm)

        UIHelpers.divider(batch, atlas, sh * 0.15f, sw)
        UIHelpers.header(batch, font, "PROFILE", sh * 0.09f, sw)
        UIHelpers.divider(batch, atlas, sh * 0.15f + 2f, sw)

        val statX   = sw * 0.15f
        val valX    = sw * 0.85f
        var sy      = sh * 0.24f
        val rowStep = sh * 0.085f

        fun statRow(label: String, value: String) {
            font.draw(batch, label, statX, sy, 1f, 1f, 1f, 1f, 0.75f)
            font.draw(batch, value, valX, sy,
                1f, UIHelpers.CYAN[0], UIHelpers.CYAN[1], UIHelpers.CYAN[2],
                1f, align = 1)
            UIHelpers.divider(batch, atlas, sy + font.lineHeight(1f) + 6f, sw, UIHelpers.MID)
            sy += rowStep
        }

        statRow("TOTAL STARS",    "$totalStars / $maxStars")
        statRow("TOTAL COINS",    "$totalCoins / 24")
        statRow("LEVELS CLEARED", "$levelsCleared / 24")
        statRow("COMPLETION",     "$pct%")

        // Progress bar
        val barY = sy + 8f
        val barW = sw * 0.7f
        val barX = (sw - barW) / 2f
        UIHelpers.rect(batch, atlas, barX, barY, barW, 16f, UIHelpers.MID, 0.7f)
        UIHelpers.rect(batch, atlas, barX, barY, barW * (pct / 100f), 16f, UIHelpers.CYAN, 0.9f)
        UIHelpers.rectOutline(batch, atlas, barX, barY, barW, 16f, 1f,
            UIHelpers.DARK, 0f, UIHelpers.PURPLE, 0.5f)

        UIHelpers.button(batch, atlas, font, btnX2, achY,  btnW, 64f, "ACHIEVEMENTS")
        UIHelpers.button(batch, atlas, font, btnX2, backY, btnW, 58f, "BACK")

        batch.end()
        tx.draw(batch, assets, sw, sh, hm)
    }

    override fun onTouch(x: Float, y: Float, action: Int) {
        if (action != MotionEvent.ACTION_DOWN || tx.isRunning) return

        when {
            UIHelpers.hits(x, y, btnX2, achY, btnW, 64f) -> {
                audio.play(SoundEvent.MENU_SELECT)
                tx.start {
                    ScreenManager.set(
                        AchievementsScreen(context, assets, audio, batch, screenW, screenH)
                    )
                }
            }
            UIHelpers.hits(x, y, btnX2, backY, btnW, 58f) -> {
                audio.play(SoundEvent.MENU_BACK)
                tx.start {
                    ScreenManager.set(
                        MainMenuScreen(context, assets, audio, batch, screenW, screenH)
                    )
                }
            }
        }
    }

    override fun dispose() {}
}