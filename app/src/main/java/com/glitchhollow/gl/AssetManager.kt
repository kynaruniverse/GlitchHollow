package com.glitchhollow.gl

import android.content.Context
import android.util.Log

class AssetManager(private val context: Context) {

    var atlas: Texture? = null
    var sprites: SpriteAtlas? = null
    var font: BitmapFont = BitmapFont(null)
    var hasAtlas = false

    init {
        loadAll()
    }

    private fun loadAll() {

        // ── Atlas (SAFE LOAD) ─────────────────────────────
        atlas = try {
            Texture(context, "sprites/atlas.png")
        } catch (e: Exception) {
            Log.e("AssetManager", "Missing atlas.png", e)
            null
        }

        sprites = try {
            atlas?.let {
                SpriteAtlas(it.width, it.height)
            }
        } catch (e: Exception) {
            Log.e("AssetManager", "SpriteAtlas init failed", e)
            null
        }

        hasAtlas = atlas != null

        // ── Font (SAFE LOAD) ──────────────────────────────
        font = try {
            val fontTex = Texture(context, "sprites/font.png")
            BitmapFont(fontTex)
        } catch (e: Exception) {
            Log.e("AssetManager", "Missing font.png", e)
            BitmapFont(null)
        }
    }

    fun dispose() {
        atlas?.dispose()
        atlas = null
        sprites = null
    }
}
