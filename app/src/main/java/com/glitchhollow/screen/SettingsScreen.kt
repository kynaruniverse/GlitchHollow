package com.glitchhollow.screen

import android.content.Context
import android.content.SharedPreferences
import android.opengl.GLES20
import android.view.MotionEvent
import com.glitchhollow.core.SaveManager
import com.glitchhollow.core.SoundEvent
import com.glitchhollow.gl.AssetManager
import com.glitchhollow.gl.AudioManager
import com.glitchhollow.gl.BitmapFont
import com.glitchhollow.gl.ParallaxBackground
import com.glitchhollow.gl.ScreenTransition
import com.glitchhollow.gl.SpriteBatch
import com.glitchhollow.gl.Texture
import com.glitchhollow.gl.UIHelpers

class SettingsScreen(
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
    private val bg = ParallaxBackground(sw, sh, 2)
    private val tx = ScreenTransition()

    private val prefs: SharedPreferences =
        context.getSharedPreferences("gh_settings", Context.MODE_PRIVATE)

    private var musicOn  = prefs.getBoolean("music",  true)
    private var sfxOn    = prefs.getBoolean("sfx",    true)
    private var glitchOn = prefs.getBoolean("glitch", true)

    private val rowW   = sw * 0.78f
    private val rowH   = 68f
    private val rowX   = (sw - rowW) / 2f
    private val row1Y  = sh * 0.28f
    private val row2Y  = row1Y + rowH + 20f
    private val row3Y  = row2Y + rowH + 20f
    private val row4Y  = sh * 0.72f
    private val backY  = sh * 0.86f

    private val toggleW = rowH
    private val toggleX = rowX + rowW - toggleW

    override fun update(dt: Float) {
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
        UIHelpers.header(batch, font, "SETTINGS", sh * 0.09f, sw)
        UIHelpers.divider(batch, atlas, sh * 0.15f + 2f, sw)

        drawToggleRow(batch, atlas, font, row1Y, "MUSIC",    musicOn)
        drawToggleRow(batch, atlas, font, row2Y, "SFX",      sfxOn)
        drawToggleRow(batch, atlas, font, row3Y, "GLITCH FX", glitchOn)

        UIHelpers.button(batch, atlas, font, rowX, row4Y, rowW, rowH, "RESET SAVE")
        UIHelpers.button(batch, atlas, font, rowX, backY,  rowW, 60f,  "BACK")

        batch.end()
        tx.draw(batch, assets, sw, sh, hm)
    }

    private fun drawToggleRow(
        batch: SpriteBatch, atlas: Texture, font: BitmapFont,
        y: Float, label: String, on: Boolean
    ) {
        UIHelpers.rectOutline(batch, atlas, rowX, y, rowW, rowH, 2f,
            UIHelpers.MID, 0.85f, UIHelpers.PURPLE)
        font.draw(batch, label,
            rowX + 18f, y + (rowH - font.lineHeight(2f)) / 2f,
            2f, 1f, 1f, 1f, 1f)
        val tc = if (on) UIHelpers.CYAN else UIHelpers.PURPLE
        font.draw(batch, if (on) "ON" else "OFF",
            toggleX + toggleW / 2f, y + (rowH - font.lineHeight(2f)) / 2f,
            2f, tc[0], tc[1], tc[2], 1f, align = 0)
    }

    override fun onTouch(x: Float, y: Float, action: Int) {
        if (action != MotionEvent.ACTION_DOWN || tx.isRunning) return

        when {
            UIHelpers.hits(x, y, rowX, row1Y, rowW, rowH) -> {
                musicOn = !musicOn
                prefs.edit().putBoolean("music", musicOn).apply()
                audio.onSettingsChanged()
                audio.play(SoundEvent.MENU_SELECT)
            }
            UIHelpers.hits(x, y, rowX, row2Y, rowW, rowH) -> {
                sfxOn = !sfxOn
                prefs.edit().putBoolean("sfx", sfxOn).apply()
                audio.play(SoundEvent.MENU_SELECT)
            }
            UIHelpers.hits(x, y, rowX, row3Y, rowW, rowH) -> {
                glitchOn = !glitchOn
                prefs.edit().putBoolean("glitch", glitchOn).apply()
                audio.play(SoundEvent.MENU_SELECT)
            }
            UIHelpers.hits(x, y, rowX, row4Y, rowW, rowH) -> {
                SaveManager(context).resetAll()
                audio.play(SoundEvent.MENU_SELECT)
            }
            UIHelpers.hits(x, y, rowX, backY, rowW, 60f) -> {
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