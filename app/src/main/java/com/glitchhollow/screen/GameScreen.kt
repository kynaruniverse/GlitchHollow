package com.glitchhollow.screen

import android.content.Context
import android.opengl.GLES20
import android.view.MotionEvent
import com.glitchhollow.core.Constants
import com.glitchhollow.core.GameEngine
import com.glitchhollow.core.SaveManager
import com.glitchhollow.gl.*

class GameScreen(
    private val context:  Context,
    private val assets:   AssetManager,
    private val batch:    SpriteBatch,
    private val world:    Int,
    private val level:    Int,
    private val screenW:  Int,
    private val screenH:  Int
) : Screen {

    private val engine      = GameEngine(context, world, level)
    private val camera      = Camera2D(screenW, screenH)
    private val controls    = VirtualControls(engine.player)
    private val hud         = HUD()
    private val saveManager = SaveManager(context)

    // Fallback colours per world for background and tiles
    private val bgColors = arrayOf(
        floatArrayOf(0.051f, 0.039f, 0.102f),  // W1 — deep purple-black
        floatArrayOf(0.102f, 0f,     0.051f),  // W2 — deep magenta-black
        floatArrayOf(0f,     0.051f, 0.102f),  // W3 — deep cyan-black
        floatArrayOf(0.039f, 0.039f, 0f)       // W4 — deep yellow-black
    )

    private var lastUpdateMs = System.currentTimeMillis()

    init {
        camera.setLevelSize(engine.tileMap.cols, engine.tileMap.rows)
        controls.setScreenSize(screenW, screenH)
    }

    // ── Screen interface ──────────────────────────────────────────

    override fun update(dt: Float) {
        val now   = System.currentTimeMillis()
        val delta = now - lastUpdateMs
        lastUpdateMs = now

        if (engine.gameState == Constants.STATE_PLAYING) {
            engine.update(delta)
            camera.update(
                engine.player.x + Constants.PLAYER_WIDTH  / 2f,
                engine.player.y + Constants.PLAYER_HEIGHT / 2f
            )
        }
    }

    override fun render() {
        clearBackground()

        // ── World rendering (camera-scrolled) ────────────────────
        batch.begin(camera.matrix)
        drawTileMap()
        drawItems()
        drawEnemies()
        drawPlayer()
        batch.end()

        // ── HUD (screen-space, no scroll) ─────────────────────────
        hud.draw(batch, assets, engine, screenW.toFloat(), camera.buildHudMatrix())

        // ── Control zone indicators ───────────────────────────────
        if (engine.gameState == Constants.STATE_PLAYING) {
            batch.begin(camera.buildHudMatrix())
            controls.drawIndicators(batch, assets, screenW.toFloat(), screenH.toFloat())
            batch.end()
        }

        // ── Glitch overlay ────────────────────────────────────────
        if (engine.glitchFrames > 0) drawGlitchOverlay()

        // ── State overlays ────────────────────────────────────────
        when (engine.gameState) {
            Constants.STATE_PAUSED   -> drawOverlay(0f, 0f, 0f, 0.7f)
            Constants.STATE_DEAD,
            Constants.STATE_GAMEOVER -> drawOverlay(0.4f, 0f, 0f, 0.6f)
            Constants.STATE_WIN      -> drawOverlay(0f, 0.4f, 0f, 0.6f)
        }
    }

    override fun onTouch(x: Float, y: Float, action: Int) {
        // State-change taps (full screen)
        if (action == MotionEvent.ACTION_DOWN) {
            when (engine.gameState) {
                Constants.STATE_PAUSED   -> engine.gameState = Constants.STATE_PLAYING
                Constants.STATE_DEAD     -> engine.respawnAfterDeath()
                Constants.STATE_GAMEOVER -> engine.restart()
                Constants.STATE_WIN      -> handleWin()
            }
        }
        controls.onTouch(x, y, action)
    }

    override fun dispose() { /* assets owned by AssetManager */ }

    // ── Draw helpers ──────────────────────────────────────────────

    private fun clearBackground() {
        val bg = bgColors[(engine.world - 1).coerceIn(0, bgColors.size - 1)]
        GLES20.glClearColor(bg[0], bg[1], bg[2], 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
    }

    private fun drawTileMap() {
        val ts    = Constants.TILE_SIZE.toFloat()
        val atlas = assets.atlas
        val sp    = assets.sprites

        val startCol = ((camera.x / ts) - 1).toInt().coerceAtLeast(0)
        val endCol   = (startCol + screenW / ts + 3).toInt().coerceAtMost(engine.tileMap.cols)
        val startRow = ((camera.y / ts) - 1).toInt().coerceAtLeast(0)
        val endRow   = (startRow + screenH / ts + 3).toInt().coerceAtMost(engine.tileMap.rows)

        for (row in startRow until endRow) {
            for (col in startCol until endCol) {
                val tile = engine.tileMap.getTile(col, row)
                if (tile == Constants.TILE_AIR) continue

                val sx = col * ts
                val sy = row * ts

                if (atlas != null && sp != null) {
                    val reg = when (tile) {
                        Constants.TILE_FLOOR    -> sp.tileFloor
                        Constants.TILE_WALL     -> sp.tileWall
                        Constants.TILE_PLATFORM -> sp.tilePlatform
                        Constants.TILE_HAZARD   -> sp.tileHazard
                        Constants.TILE_EXIT     -> sp.tileExit[0]
                        else -> null
                    }
                    if (reg != null) {
                        batch.draw(atlas, sx, sy, reg.w, reg.h, reg.u0, reg.v0, reg.u1, reg.v1)
                        continue
                    }
                }

                // Fallback coloured rect
                val c = tileColor(tile)
                if (atlas != null) {
                    batch.draw(atlas, sx, sy, ts, ts,
                        0f, 0f, 0.001f, 0.001f, c[0], c[1], c[2], 1f)
                }
            }
        }
    }

    private fun drawItems() {
        val atlas = assets.atlas ?: return
        val sp    = assets.sprites
        val tick  = engine.tick

        // Shards
        engine.shardX.indices.forEach { i ->
            if (engine.shardCollected[i]) return@forEach
            val wx = engine.shardX[i]; val wy = engine.shardY[i]
            if (!camera.isVisible(wx, wy, 32f, 40f)) return@forEach
            val bob = Math.sin(tick * 0.08 + i.toDouble()).toFloat() * 4f
            if (sp != null) {
                val frame = (tick / 8 + i) % 4
                val r = sp.shardSpin[frame]
                batch.draw(atlas, wx, wy + bob, r.w, r.h, r.u0, r.v0, r.u1, r.v1,
                    0f, 0.96f, 1f, 1f)
            } else {
                batch.draw(atlas, wx, wy + bob, 32f, 40f,
                    0f, 0f, 0.001f, 0.001f, 0f, 0.96f, 1f, 1f)
            }
            // Glow pulse — additive blend would be Phase 5; use alpha overlay now
            batch.draw(atlas, wx - 8f, wy + bob - 8f, 48f, 56f,
                0f, 0f, 0.001f, 0.001f, 0f, 0.96f, 1f, 0.08f)
        }

        // Coins
        engine.coinX.indices.forEach { i ->
            if (engine.coinCollected[i]) return@forEach
            val wx = engine.coinX[i]; val wy = engine.coinY[i]
            if (!camera.isVisible(wx, wy, 28f, 28f)) return@forEach
            val bob = Math.sin(tick * 0.1 + i * 1.5).toFloat() * 3f
            if (sp != null) {
                val frame = (tick / 8 + i) % 4
                val r = sp.coinSpin[frame]
                batch.draw(atlas, wx, wy + bob, r.w, r.h, r.u0, r.v0, r.u1, r.v1,
                    1f, 0.9f, 0f, 1f)
            } else {
                batch.draw(atlas, wx, wy + bob, 28f, 28f,
                    0f, 0f, 0.001f, 0.001f, 1f, 0.9f, 0f, 1f)
            }
        }
    }

    private fun drawEnemies() {
        val atlas = assets.atlas ?: return
        val sp    = assets.sprites
        engine.enemies.forEach { e ->
            if (e.dead) return@forEach
            if (!camera.isVisible(e.x, e.y, e.width, e.height)) return@forEach

            if (sp != null) {
                val reg = enemyRegion(sp, e.type, e.animFrame)
                batch.draw(atlas, e.x, e.y, reg.w, reg.h,
                    reg.u0, reg.v0, reg.u1, reg.v1,
                    flipX = e.movingLeft)
            } else {
                batch.draw(atlas, e.x, e.y, e.width, e.height,
                    0f, 0f, 0.001f, 0.001f, 1f, 0.23f, 0.67f, 1f)
            }
        }
    }

    private fun drawPlayer() {
        val atlas = assets.atlas ?: return
        val sp    = assets.sprites
        val p     = engine.player

        // Invincibility flicker — skip every other 4-frame block
        if (p.invincible > 0 && (p.invincible / 4) % 2 == 0) return

        if (sp != null) {
            val frames = when (p.anim) {
                com.glitchhollow.core.Player.Anim.IDLE -> sp.pipIdle
                com.glitchhollow.core.Player.Anim.RUN  -> sp.pipRun
                com.glitchhollow.core.Player.Anim.JUMP -> sp.pipJump
                com.glitchhollow.core.Player.Anim.DEAD -> sp.pipDead
            }
            val frame = p.animFrame.coerceAtMost(frames.size - 1)
            val r = frames[frame]
            batch.draw(atlas, p.x, p.y, r.w, r.h,
                r.u0, r.v0, r.u1, r.v1, flipX = p.facingLeft)
        } else {
            batch.draw(atlas, p.x, p.y,
                Constants.PLAYER_WIDTH.toFloat(), Constants.PLAYER_HEIGHT.toFloat(),
                0f, 0f, 0.001f, 0.001f, 1f, 0.9f, 0f, 1f)
        }
    }

    private fun drawGlitchOverlay() {
        val atlas = assets.atlas ?: return
        val frame = engine.glitchFrames
        val strips = 6
        batch.begin(camera.buildHudMatrix())
        for (i in 0 until strips) {
            val sy     = screenH.toFloat() / strips * i
            val offset = if (frame % 2 == 0) 8f else -8f
            batch.draw(atlas, offset, sy,
                screenW.toFloat(), screenH.toFloat() / strips / 2f,
                0f, 0f, 0.001f, 0.001f, 1f, 0.23f, 0.67f, 0.1f)
        }
        batch.end()
    }

    private fun drawOverlay(r: Float, g: Float, b: Float, a: Float) {
        val atlas = assets.atlas ?: return
        batch.begin(camera.buildHudMatrix())
        batch.draw(atlas, 0f, 0f, screenW.toFloat(), screenH.toFloat(),
            0f, 0f, 0.001f, 0.001f, r, g, b, a)
        batch.end()
    }

    // ── Helpers ───────────────────────────────────────────────────

    private fun tileColor(tile: Int) = when (tile) {
        Constants.TILE_FLOOR    -> floatArrayOf(0.118f, 0f,     0.208f)
        Constants.TILE_WALL     -> floatArrayOf(0.051f, 0.051f, 0.102f)
        Constants.TILE_PLATFORM -> floatArrayOf(0.482f, 0.184f, 0.745f)
        Constants.TILE_HAZARD   -> floatArrayOf(1f,     0.176f, 0.333f)
        Constants.TILE_EXIT     -> floatArrayOf(0f,     0.957f, 1f)
        else                    -> floatArrayOf(0.2f,   0.2f,   0.2f)
    }

    private fun enemyRegion(sp: SpriteAtlas, type: Int, frame: Int) = when (type) {
        Constants.ENEMY_WOBBLE   -> sp.wobblePatrol[frame % sp.wobblePatrol.size]
        Constants.ENEMY_STITCHY  -> sp.stitchyPatrol[frame % sp.stitchyPatrol.size]
        Constants.ENEMY_GLITCH   -> sp.glitchPatrol[frame % sp.glitchPatrol.size]
        Constants.ENEMY_DIRECTOR -> sp.directorPatrol[frame % sp.directorPatrol.size]
        else                     -> sp.wobblePatrol[0]
    }

    private fun handleWin() {
        saveManager.saveStars(world, level, engine.starsEarned)
        saveManager.saveCoin(world, level, engine.coinGotThisRun)
        saveManager.onLevelComplete(world, level)

        var nextWorld = world; var nextLevel = level + 1
        if (nextLevel > SaveManager.LEVELS_PER_WORLD) { nextWorld++; nextLevel = 1 }

        if (nextWorld > SaveManager.TOTAL_WORLDS) {
            ScreenManager.set(MainMenuScreen(context, assets, batch, screenW, screenH))
            return
        }
        ScreenManager.set(GameScreen(context, assets, batch, nextWorld, nextLevel, screenW, screenH))
    }
}