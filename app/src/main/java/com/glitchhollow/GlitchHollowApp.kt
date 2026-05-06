package com.glitchhollow

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import com.glitchhollow.gl.GLGameView
import com.glitchhollow.screen.ScreenManager

class GlitchHollowApp : Activity() {

    private lateinit var glView: GLGameView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemUI()
        glView = GLGameView(this)
        setContentView(glView)
    }

    override fun onResume()  { super.onResume();  glView.onResume();  hideSystemUI() }
    override fun onPause()   { super.onPause();   glView.onPause() }
    override fun onDestroy() { super.onDestroy(); ScreenManager.dispose() }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI()
    }

    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN
        )
    }
}