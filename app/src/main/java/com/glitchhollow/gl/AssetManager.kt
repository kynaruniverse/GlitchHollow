package com.glitchhollow.gl

import android.content.Context
import android.util.Log

class AssetManager(private val context: Context) {

    var atlas:    Texture?     = null
    var sprites:  SpriteAtlas? = null
    var font:     BitmapFont   = BitmapFont(null)  // starts as no-op
    var hasAtlas  = false

    init { loadAll() }

    private fun loadAll() {
        // Sprite atlas
        try {
            atlas   = Texture(context, "sprites/atlas.png")
            sprites = SpriteAtlas(atlas!!.width, atlas!!.height)
            hasAtlas = true
            Log.d("AssetManager", "Atlas loaded: ${atlas!!.width}×${atlas!!.height}")
        } catch (e: Exception) {
            Log.w("AssetManager", "No atlas — fallback colour mode.")
        }

        // Bitmap font
        try {
            val fontTex = Texture(context, "sprites/font.png")
            font = BitmapFont(fontTex)
            Log.d("AssetManager", "Font loaded")
        } catch (e: Exception) {
            Log.w("AssetManager", "No font.png — text rendering disabled.")
        }
    }

    fun dispose() {
        atlas?.dispose()
        atlas = null; sprites = null
    }
}