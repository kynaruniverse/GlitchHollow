package com.glitchhollow.gl

import android.content.Context
import android.util.Log

/**
 * Loads and caches all game textures.
 * Must be created on the GL thread (inside onSurfaceCreated).
 *
 * Graceful fallback: if atlas.png doesn't exist yet (no art),
 * hasAtlas = false and all renderers fall back to coloured rectangles.
 * This lets you play-test the game before pixel art is ready.
 */
class AssetManager(private val context: Context) {

    var atlas:    Texture?     = null
    var sprites:  SpriteAtlas? = null
    var hasAtlas  = false

    init { loadAll() }

    private fun loadAll() {
        try {
            atlas   = Texture(context, "sprites/atlas.png")
            sprites = SpriteAtlas(atlas!!.width, atlas!!.height)
            hasAtlas = true
            Log.d("AssetManager", "Atlas loaded: ${atlas!!.width}×${atlas!!.height}")
        } catch (e: Exception) {
            Log.w("AssetManager", "No atlas found — running in fallback colour mode. " +
                "Add assets/sprites/atlas.png to enable pixel art.")
            hasAtlas = false
        }
    }

    fun dispose() {
        atlas?.dispose()
        atlas   = null
        sprites = null
    }
}