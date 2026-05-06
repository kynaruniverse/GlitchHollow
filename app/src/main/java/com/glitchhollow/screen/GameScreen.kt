package com.glitchhollow.screen

import android.content.Context
import android.content.SharedPreferences
import android.opengl.GLES20
import android.view.MotionEvent
import com.glitchhollow.core.Constants
import com.glitchhollow.core.GameEngine
import com.glitchhollow.core.Player
import com.glitchhollow.core.SaveManager
import com.glitchhollow.core.SoundEvent
import com.glitchhollow.gl.AssetManager
import com.glitchhollow.gl.AudioManager
import com.glitchhollow.gl.BackgroundArt
import com.glitchhollow.gl.Camera2D
import com.glitchhollow.gl.GlowRenderer
import com.glitchhollow.gl.HUD
import com.glitchhollow.gl.ParticleSystem
import com.glitchhollow.gl.ScreenTransition
import com.glitchhollow.gl.SpriteBatch
import com.glitchhollow.gl.SpriteAtlas
import com.glitchhollow.gl.UIHelpers
import com.glitchhollow.gl.VirtualControls

class GameScreen(
    private val context:  Context,
    private val assets:   AssetManager,
    private val audio:    AudioManager,
    private val batch:    SpriteBatch,
    val world:            Int,
    val level:            Int,
    private val screenW:  Int,
    private val screenH:  Int
) : Screen {

    val engine          = GameEngine(context, world, level)
    private val camera  = Camera2D(screenW, screenH)
    private val controls     = VirtualControls(engine.player)
    private val hud          = HUD()
    private val saveManager  = SaveManager(context)
    private val transition   = ScreenTransition()
    private val particles    = ParticleSystem()
    private val glowRenderer = GlowRenderer(context)
    private val bgArt        = BackgroundArt(screenW.toFloat(), screenH.toFloat(), world - 1)

    private val settings: SharedPreferences =
        context.getSharedPreferences("gh_settings", Context.MODE_PRIVATE)

    // Event tracking for particle + shake triggers
    private var prevShardCount  = 0
    private var prevCoinCount   = 0
    private var prevGameState   = Constants.STATE_PLAYING
    private var prevEnemyCount  = 0

    // Pause button region
    private val pauseBtnX = screenW * 0.82f
    private val pauseBtnY = 8f
    private val pauseBtnW = screenW * 0.15f
    private val pauseBtnH = Constants.HUD_HEIGHT - 16f

    init {
        camera.setLevelSize(engine.tileMap.cols, engine.tileMap.rows)
        controls.setScreenSize(screenW, screenH)
        prevEnemyCount = engine.enemies.size
        audio.playBgm("world$world")
    }

    fun restart() {
        engine.restart()
        prevShardCount = 0
        prevCoinCount  = 0
        prevEnemyCount = engine.enemies.size
        prevGameState  = Constants.STATE_PLAYING
    }

    // ── Update ────────────────────────────────────────────────────

    override fun update(dt: Float) {
        transition.update(dt)
        glowRenderer.update(dt)

        if (engine.gameState == Constants.STATE_PLAYING) {
            engine.update((dt * 1000).toLong())
            camera.update(
                engine.player.x + Constants.PLAYER_WIDTH  / 2f,
                engine.player.y + Constants.PLAYER_HEIGHT / 2f
            )
            bgArt.update(dt, camera.x, camera.y)
            particles.update(dt)
            fireParticleEvents()
            spawnExitParticles()
        }

        // Drain sound queue — always, even when paused for win SFX
        audio.drainQueue(engine.soundQueue)
    }

    private fun fireParticleEvents() {
        val shardsNow = engine.shardCollected.count { it }
        val coinsNow  = engine.coinCollected.count  { it }

        if (shardsNow > prevShardCount) {
            val idx = engine.shardCollected.indexOfLast { it }
            if (idx >= 0) {
                particles.shardPickup(engine.shardX[idx], engine.shardY[idx])
                camera.shake(Constants.SHAKE_SHARD, Constants.SHAKE_FRAMES_SHORT)
            }
            prevShardCount = shardsNow
        }

        if (coinsNow > prevCoinCount) {
            val idx = engine.coinCollected.indexOfLast { it }
            if (idx >= 0) particles.coinPickup(engine.coinX[idx], engine.coinY[idx])
            prevCoinCount = coinsNow
        }

        val aliveNow = engine.enemies.count { !it.dead }
        if (aliveNow < prevEnemyCount) {
            engine.enemies.filter { it.dead }.forEach { e ->
                particles.stompDust(e.x + e.width / 2f, e.y + e.height)
            }
            camera.shake(Constants.SHAKE_STOMP, Constants.SHAKE_FRAMES_SHORT)
            prevEnemyCount = aliveNow
        }

        if (engine.gameState == Constants.STATE_DEAD &&
            prevGameState   == Constants.STATE_PLAYING) {
            particles.playerDeath(
                engine.player.x + Constants.PLAYER_WIDTH  / 2f,
                engine.player.y + Constants.PLAYER_HEIGHT / 2f
            )
            camera.shake(Constants.SHAKE_DEATH, Constants.SHAKE_FRAMES_LONG)
        }

        prevGameState = engine.gameState
    }

    private fun spawnExitParticles() {
        if (!engine.allShardsCollected()) return
        val ts = Constants.TILE_SIZE.toFloat()
        for (row in 0 until engine.tileMap.rows) {
            for (col in 0 until engine.tileMap.cols) {
                if (engine.tileMap.isExit(col, row)) {
                    particles.exitPulse(col * ts, row * ts)
                }
            }
        }
    }

    // ── Render ────────────────────────────────────────────────────

    override fun render() {
        val hudMatrix = camera.buildHudMatrix()

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        bgArt.draw(batch, assets, hudMatrix)

        // Pass 1 — world (normal alpha blend)
        batch.begin(camera.matrix)
        drawTileMap()
        drawItems()
        drawEnemies()
        drawPlayer()
        assets.atlas?.let { particles.draw(batch, it) }
        batch.end()

        // Pass 2 — glow (additive blend)
        drawGlowPass()

        // HUD
        hud.draw(batch, assets, engine, screenW.toFloat(), hudMatrix)

        // Control indicators + pause button
        val atlas = assets.atlas
        if (atlas != null && engine.gameState == Constants.STATE_PLAYING) {
            batch.begin(hudMatrix)
            controls.drawIndicators(batch, assets, screenW.toFloat(), screenH.toFloat())
            UIHelpers.button(batch, atlas, assets.font,
                pauseBtnX, pauseBtnY, pauseBtnW, pauseBtnH, "||")
            batch.end()
        }

        // Glitch overlay
        if (engine.glitchFrames > 0 && settings.getBoolean("glitch", true)) {
            drawGlitchOverlay()
        }

        // State overlays
        when (engine.gameState) {
            Constants.STATE_DEAD,
            Constants.STATE_GAMEOVER -> drawOverlay(0.35f, 0f, 0f, 0.55f)
        }

        // Win — trigger transition to WinScreen
        if (engine.gameState == Constants.STATE_WIN && !transition.isRunning) {
            transition.start {
                ScreenManager.set(
                    WinScreen(context, assets, audio, batch, screenW, screenH, engine)
                )
            }
        }

        transition.draw(batch, assets, screenW.toFloat(), screenH.toFloat(), hudMatrix)
    }

    private fun drawGlowPass() {
        val atlas = assets.atlas ?: return
        val sp    = assets.sprites

        glowRenderer.beginPass(camera.matrix)

        // Shard glow
        engine.shardX.indices.forEach { i ->
            if (engine.shardCollected[i]) return@forEach
            if (!camera.isVisible(engine.shardX[i], engine.shardY[i], 32f, 40f)) return@forEach
            val bob = Math.sin(engine.tick * 0.08 + i.toDouble()).toFloat() * 4f
            val reg = sp?.shardSpin?.get((engine.tick / 8 + i) % 4)
            glowRenderer.drawGlow(atlas,
                engine.shardX[i], engine.shardY[i] + bob, 32f, 40f,
                Constants.GLOW_SHARD_INTENSITY, 0f, 0.96f, 1f,
                reg?.u0 ?: 0f, reg?.v0 ?: 0f,
                reg?.u1 ?: 0.001f, reg?.v1 ?: 0.001f)
        }

        // Coin glow
        engine.coinX.indices.forEach { i ->
            if (engine.coinCollected[i]) return@forEach
            if (!camera.isVisible(engine.coinX[i], engine.coinY[i], 28f, 28f)) return@forEach
            val bob = Math.sin(engine.tick * 0.1 + i * 1.5).toFloat() * 3f
            val reg = sp?.coinSpin?.get((engine.tick / 8 + i) % 4)
            glowRenderer.drawGlow(atlas,
                engine.coinX[i], engine.coinY[i] + bob, 28f, 28f,
                Constants.GLOW_COIN_INTENSITY, 1f, 0.9f, 0f,
                reg?.u0 ?: 0f, reg?.v0 ?: 0f,
                reg?.u1 ?: 0.001f, reg?.v1 ?: 0.001f)
        }

        // Exit glow — only when all shards collected
        if (engine.allShardsCollected()) {
            val ts = Constants.TILE_SIZE.toFloat()
            for (row in 0 until engine.tileMap.rows) {
                for (col in 0 until engine.tileMap.cols) {
                    if (!engine.tileMap.isExit(col, row)) continue
                    glowRenderer.drawGlow(atlas,
                        col * ts, row * ts, ts, ts,
                        Constants.GLOW_EXIT_INTENSITY, 0f, 0.96f, 1f)
                }
            }
        }

        // Player glow — subtle edge light
        if (engine.player.invincible == 0) {
            glowRenderer.drawGlow(atlas,
                engine.player.x, engine.player.y,
                Constants.PLAYER_WIDTH.toFloat(), Constants.PLAYER_HEIGHT.toFloat(),
                Constants.GLOW_PLAYER_INTENSITY, 0.48f, 0.18f, 0.75f)
        }

        glowRenderer.endPass()
    }

    // ── Touch ─────────────────────────────────────────────────────

    override fun onTouch(x: Float, y: Float, action: Int) {
        if (action == MotionEvent.ACTION_DOWN) {
            // Pause button
            if (UIHelpers.hits(x, y, pauseBtnX, pauseBtnY, pauseBtnW, pauseBtnH) &&
                engine.gameState == Constants.STATE_PLAYING) {
                audio.play(SoundEvent.MENU_SELECT)
                ScreenManager.set(
                    PauseScreen(context, assets, audio, batch, screenW, screenH, this)
                )
                return
            }
            // State taps
            when (engine.gameState) {
                Constants.STATE_DEAD     -> engine.respawnAfterDeath()
                Constants.STATE_GAMEOVER -> engine.restart()
            }
        }
        if (engine.gameState == Constants.STATE_PLAYING) {
            controls.onTouch(x, y, action)
        }
    }

    override fun dispose() {
        glowRenderer.dispose()
    }

    // ── Draw helpers ──────────────────────────────────────────────

    private fun drawTileMap() {
        val ts    = Constants.TILE_SIZE.toFloat()
        val atlas = assets.atlas ?: return
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
                if (sp != null) {
                    val reg = when (tile) {
                        Constants.TILE_FLOOR    -> sp.tileFloor
                        Constants.TILE_WALL     -> sp.tileWall
                        Constants.TILE_PLATFORM -> sp.tilePlatform
                        Constants.TILE_HAZARD   -> sp.tileHazard
                        Constants.TILE_EXIT     -> sp.tileExit[0]
                        else -> null
                    }
                    if (reg != null) {
                        batch.draw(atlas, sx, sy, reg.w, reg.h,
                            reg.u0, reg.v0, reg.u1, reg.v1)
                        continue
                    }
                }
                val c = tileColor(tile)
                batch.draw(atlas, sx, sy, ts, ts,
                    0f, 0f, 0.001f, 0.001f, c[0], c[1], c[2], 1f)
            }
        }
    }

    private fun drawItems() {
        val atlas = assets.atlas ?: return
        val sp    = assets.sprites
        val tick  = engine.tick

        engine.shardX.indices.forEach { i ->
            if (engine.shardCollected[i]) return@forEach
            val wx = engine.shardX[i]; val wy = engine.shardY[i]
            if (!camera.isVisible(wx, wy, 32f, 40f)) return@forEach
            val bob = Math.sin(tick * 0.08 + i.toDouble()).toFloat() * 4f
            if (sp != null) {
                val r = sp.shardSpin[(tick / 8 + i) % 4]
                batch.draw(atlas, wx, wy + bob, r.w, r.h,
                    r.u0, r.v0, r.u1, r.v1, 0f, 0.96f, 1f, 1f)
            } else {
                batch.draw(atlas, wx, wy + bob, 32f, 40f,
                    0f, 0f, 0.001f, 0.001f, 0f, 0.96f, 1f, 1f)
            }
        }

        engine.coinX.indices.forEach { i ->
            if (engine.coinCollected[i]) return@forEach
            val wx = engine.coinX[i]; val wy = engine.coinY[i]
            if (!camera.isVisible(wx, wy, 28f, 28f)) return@forEach
            val bob = Math.sin(tick * 0.1 + i * 1.5).toFloat() * 3f
            if (sp != null) {
                val r = sp.coinSpin[(tick / 8 + i) % 4]
                batch.draw(atlas, wx, wy + bob, r.w, r.h,
                    r.u0, r.v0, r.u1, r.v1, 1f, 0.9f, 0f, 1f)
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
                    reg.u0, reg.v0, reg.u1, reg.v1, flipX = e.movingLeft)
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
        if (p.invincible > 0 && (p.invincible / 4) % 2 == 0) return
        if (sp != null) {
            val frames = when (p.anim) {
                Player.Anim.IDLE -> sp.pipIdle
                Player.Anim.RUN  -> sp.pipRun
                Player.Anim.JUMP -> sp.pipJump
                Player.Anim.DEAD -> sp.pipDead
            }
            val r = frames[p.animFrame.coerceAtMost(frames.size - 1)]
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
        batch.begin(camera.buildHudMatrix())
        for (i in 0 until 6) {
            val sy  = screenH.toFloat() / 6f * i
            val off = if (frame % 2 == 0) 10f else -10f
            batch.draw(atlas, off, sy, screenW.toFloat(), screenH.toFloat() / 12f,
                0f, 0f, 0.001f, 0.001f, 1f, 0.23f, 0.67f, 0.12f)
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
}