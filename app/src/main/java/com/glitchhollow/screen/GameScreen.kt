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
import com.glitchhollow.gl.DebugOverlay
import com.glitchhollow.gl.GLRenderer
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
    private val screenH:  Int,
    private val renderer: GLRenderer? = null
) : Screen {

    val engine = GameEngine(context, world, level)
    private val camera = Camera2D(screenW, screenH)

    private val controls     = VirtualControls(engine.player)
    private val hud          = HUD()
    private val saveManager  = SaveManager(context)
    private val transition   = ScreenTransition()
    private val particles    = ParticleSystem()
    private val glowRenderer = GlowRenderer(context)
    private val bgArt        = BackgroundArt(screenW.toFloat(), screenH.toFloat(), world - 1)
    private val debugOverlay = DebugOverlay(context)

    private val settings: SharedPreferences =
        context.getSharedPreferences("gh_settings", Context.MODE_PRIVATE)

    private var prevShardCount = 0
    private var prevCoinCount  = 0
    private var prevGameState  = Constants.STATE_PLAYING
    private var prevEnemyCount = 0

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
        prevCoinCount = 0
        prevEnemyCount = engine.enemies.size
        prevGameState = Constants.STATE_PLAYING
    }

    // ─────────────────────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────────────────────

    override fun update(dt: Float) {
        transition.update(dt)
        glowRenderer.update(dt)

        if (engine.gameState == Constants.STATE_PLAYING) {
            engine.update((dt * 1000f).toLong())

            camera.update(
                engine.player.x + Constants.PLAYER_WIDTH / 2f,
                engine.player.y + Constants.PLAYER_HEIGHT / 2f
            )

            bgArt.update(dt, camera.x, camera.y)
            particles.update(dt)

            fireParticleEvents()
            spawnExitParticles()
        }

        audio.drainQueue(engine.soundQueue)
    }

    private fun fireParticleEvents() {
        val shardsNow = engine.shardCollected.count { it }
        val coinsNow  = engine.coinCollected.count { it }

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
            if (idx >= 0) {
                particles.coinPickup(engine.coinX[idx], engine.coinY[idx])
            }
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
            prevGameState == Constants.STATE_PLAYING) {

            particles.playerDeath(
                engine.player.x + Constants.PLAYER_WIDTH / 2f,
                engine.player.y + Constants.PLAYER_HEIGHT / 2f
            )

            camera.shake(Constants.SHAKE_DEATH, Constants.SHAKE_FRAMES_LONG)
        }

        prevGameState = engine.gameState
    }

    private fun spawnExitParticles() {
        if (!engine.allShardsCollected()) return
        val ts = Constants.TILE_SIZE.toFloat()
        engine.exitPositions.forEach { (col, row) ->
            particles.exitPulse(col * ts, row * ts)
        }
    }

    // ─────────────────────────────────────────────────────────────
    // RENDER
    // ─────────────────────────────────────────────────────────────

    override fun render() {

        val hudMatrix = camera.buildHudMatrix()

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        bgArt.draw(batch, assets, hudMatrix)

        // WORLD PASS
        batch.begin(camera.matrix)
        drawTileMap()
        drawItems()
        drawEnemies()
        drawPlayer()
        assets.atlas?.let { particles.draw(batch, it) }
        batch.end()

        // GLOW PASS
        drawGlowPass()

        // HUD PASS
        hud.draw(batch, assets, engine, screenW.toFloat(), hudMatrix)

        val atlas = assets.atlas
        if (atlas != null && engine.gameState == Constants.STATE_PLAYING) {
            batch.begin(hudMatrix)
            controls.drawIndicators(batch, assets, screenW.toFloat(), screenH.toFloat())

            UIHelpers.button(
                batch,
                atlas,
                assets.font,
                pauseBtnX,
                pauseBtnY,
                pauseBtnW,
                pauseBtnH,
                "||"
            )

            batch.end()
        }

        if (engine.glitchFrames > 0 && settings.getBoolean("glitch", true)) {
            drawGlitchOverlay(hudMatrix)
        }

        when (engine.gameState) {
            Constants.STATE_DEAD,
            Constants.STATE_GAMEOVER -> drawOverlay(hudMatrix, 0.35f, 0f, 0f, 0.55f)
        }

        if (engine.gameState == Constants.STATE_WIN && !transition.isRunning) {
            transition.start {
                ScreenManager.set(
                    WinScreen(
                        context, assets, audio, batch,
                        screenW, screenH, engine,
                        renderer = renderer
                    )
                )
            }
        }

        transition.draw(batch, assets, screenW.toFloat(), screenH.toFloat(), hudMatrix)

        // SCANLINE PASS — absolute last, on top of everything
        val glitchFxOn = settings.getBoolean("glitch", true)
        renderer?.scanlines?.draw(
            batch, assets,
            screenW.toFloat(), screenH.toFloat(),
            hudMatrix, glitchFxOn
        )

        if (renderer != null) {
            debugOverlay.draw(
                batch, assets, renderer, engine,
                particles.liveCount,
                camera.x, camera.y,
                screenW.toFloat(), screenH.toFloat(),
                hudMatrix
            )
        }
    }

    // ─────────────────────────────────────────────────────────────
    // GLOW PASS
    // ─────────────────────────────────────────────────────────────

    private fun drawGlowPass() {
        val atlas = assets.atlas ?: return
        val sp = assets.sprites

        glowRenderer.beginPass(camera.matrix)

        engine.shardX.indices.forEach { i ->
            if (engine.shardCollected[i]) return@forEach

            if (!camera.isVisible(engine.shardX[i], engine.shardY[i], 32f, 40f)) return@forEach

            val bob = Math.sin(engine.tick * 0.08 + i).toFloat() * 4f
            val reg = sp?.shardSpin?.get((engine.tick / 8 + i) % 4)

            glowRenderer.drawGlow(
                atlas,
                engine.shardX[i],
                engine.shardY[i] + bob,
                32f,
                40f,
                Constants.GLOW_SHARD_INTENSITY,
                0f, 0.96f, 1f,
                reg?.u0 ?: 0f,
                reg?.v0 ?: 0f,
                reg?.u1 ?: 0.001f,
                reg?.v1 ?: 0.001f
            )
        }

        engine.coinX.indices.forEach { i ->
            if (engine.coinCollected[i]) return@forEach

            if (!camera.isVisible(engine.coinX[i], engine.coinY[i], 28f, 28f)) return@forEach

            val bob = Math.sin(engine.tick * 0.1 + i * 1.5).toFloat() * 3f
            val reg = sp?.coinSpin?.get((engine.tick / 8 + i) % 4)

            glowRenderer.drawGlow(
                atlas,
                engine.coinX[i],
                engine.coinY[i] + bob,
                28f,
                28f,
                Constants.GLOW_COIN_INTENSITY,
                1f, 0.9f, 0f,
                reg?.u0 ?: 0f,
                reg?.v0 ?: 0f,
                reg?.u1 ?: 0.001f,
                reg?.v1 ?: 0.001f
            )
        }

        glowRenderer.endPass()
    }

    // ─────────────────────────────────────────────────────────────
    // TOUCH
    // ─────────────────────────────────────────────────────────────

    override fun onTouch(x: Float, y: Float, action: Int) {
        if (action == MotionEvent.ACTION_POINTER_DOWN) {
            debugOverlay.toggle()
            return
        }

        if (action == MotionEvent.ACTION_DOWN) {
            if (UIHelpers.hits(x, y, pauseBtnX, pauseBtnY, pauseBtnW, pauseBtnH)
                && engine.gameState == Constants.STATE_PLAYING
            ) {
                audio.play(SoundEvent.MENU_SELECT)
                ScreenManager.set(
                    PauseScreen(
                        context, assets, audio, batch,
                        screenW, screenH, this,
                        renderer = renderer
                    )
                )
                return
            }

            when (engine.gameState) {
                Constants.STATE_DEAD -> engine.respawnAfterDeath()
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

    // ─────────────────────────────────────────────────────────────
    // DRAW HELPERS
    // ─────────────────────────────────────────────────────────────

    private fun drawGlitchOverlay(hudMatrix: FloatArray) {
        val atlas = assets.atlas ?: return
        batch.begin(hudMatrix)

        for (i in 0 until 6) {
            val sy = screenH.toFloat() / 6f * i
            val off = if (engine.glitchFrames % 2 == 0) 10f else -10f

            batch.draw(
                atlas,
                off,
                sy,
                screenW.toFloat(),
                screenH.toFloat() / 12f,
                0f, 0f, 0.001f, 0.001f,
                1f, 0.23f, 0.67f, 0.12f
            )
        }

        batch.end()
    }

    private fun drawOverlay(
        hudMatrix: FloatArray,
        r: Float,
        g: Float,
        b: Float,
        a: Float
    ) {
        val atlas = assets.atlas ?: return
        batch.begin(hudMatrix)

        batch.draw(
            atlas,
            0f,
            0f,
            screenW.toFloat(),
            screenH.toFloat(),
            0f, 0f, 0.001f, 0.001f,
            r, g, b, a
        )

        batch.end()
    }

    // ─────────────────────────────────────────────────────────────
    // TILE / ENTITIES
    // ─────────────────────────────────────────────────────────────

    private fun drawTileMap() {
        val atlas = assets.atlas
        val sp    = assets.sprites
        val ts    = Constants.TILE_SIZE.toFloat()

        for (row in 0 until engine.tileMap.rows) {
            for (col in 0 until engine.tileMap.cols) {
                val tile = engine.tileMap.getTile(col, row)
                if (tile == Constants.TILE_AIR) continue

                val wx = col * ts
                val wy = row * ts

                // Frustum cull — skip tiles not visible to camera
                if (!camera.isVisible(wx, wy, ts, ts)) continue

                if (atlas != null && sp != null) {
                    when (tile) {
                        Constants.TILE_FLOOR -> {
                            val r = sp.tileFloor
                            batch.draw(atlas, wx, wy, ts, ts, r.u0, r.v0, r.u1, r.v1)
                        }
                        Constants.TILE_WALL -> {
                            val r = sp.tileWall
                            batch.draw(atlas, wx, wy, ts, ts, r.u0, r.v0, r.u1, r.v1)
                        }
                        Constants.TILE_PLATFORM -> {
                            val r = sp.tilePlatform
                            // Platform is a half-height strip — draw centred vertically
                            val ph = r.h
                            batch.draw(atlas, wx, wy + (ts - ph) / 2f, ts, ph,
                                r.u0, r.v0, r.u1, r.v1)
                        }
                        Constants.TILE_HAZARD -> {
                            val r = sp.tileHazard
                            batch.draw(atlas, wx, wy, ts, ts, r.u0, r.v0, r.u1, r.v1)
                        }
                        Constants.TILE_EXIT -> {
                            val frame = (engine.tick / 10) % sp.tileExit.size
                            val r = sp.tileExit[frame]
                            batch.draw(atlas, wx, wy - (r.h - ts), ts, r.h,
                                r.u0, r.v0, r.u1, r.v1)
                        }
                    }
                } else {
                    // Fallback — coloured rects when atlas is missing
                    val c = tileColor(tile)
                    val fallbackAtlas = assets.atlas ?: continue
                    batch.draw(fallbackAtlas, wx, wy, ts, ts,
                        0f, 0f, 0.001f, 0.001f, c[0], c[1], c[2], 1f)
                }
            }
        }
    }

    private fun drawItems() {
        val atlas = assets.atlas ?: return
        val sp    = assets.sprites
        val ts    = Constants.TILE_SIZE.toFloat()

        // Shards
        engine.shardX.indices.forEach { i ->
            if (engine.shardCollected[i]) return@forEach
            val sx = engine.shardX[i]
            val sy = engine.shardY[i]
            if (!camera.isVisible(sx, sy, 32f, 40f)) return@forEach

            val bob = Math.sin(engine.tick * 0.08 + i).toFloat() * 4f
            if (sp != null) {
                val frame = (engine.tick / 8 + i) % sp.shardSpin.size
                val r = sp.shardSpin[frame]
                batch.draw(atlas, sx, sy + bob, r.w, r.h, r.u0, r.v0, r.u1, r.v1,
                    0f, 0.96f, 1f, 1f)
            } else {
                batch.draw(atlas, sx, sy + bob, 32f, 40f,
                    0f, 0f, 0.001f, 0.001f, 0f, 0.96f, 1f, 1f)
            }
        }

        // Coins
        engine.coinX.indices.forEach { i ->
            if (engine.coinCollected[i]) return@forEach
            val cx = engine.coinX[i]
            val cy = engine.coinY[i]
            if (!camera.isVisible(cx, cy, 28f, 28f)) return@forEach

            val bob = Math.sin(engine.tick * 0.1 + i * 1.5).toFloat() * 3f
            if (sp != null) {
                val frame = (engine.tick / 8 + i) % sp.coinSpin.size
                val r = sp.coinSpin[frame]
                batch.draw(atlas, cx, cy + bob, r.w, r.h, r.u0, r.v0, r.u1, r.v1,
                    1f, 0.9f, 0f, 1f)
            } else {
                batch.draw(atlas, cx, cy + bob, 28f, 28f,
                    0f, 0f, 0.001f, 0.001f, 1f, 0.9f, 0f, 1f)
            }
        }
    }

    private fun drawEnemies() {
        val atlas = assets.atlas ?: return
        val sp    = assets.sprites

        engine.enemies.forEach { e ->
            if (!camera.isVisible(e.x, e.y, e.width, e.height)) return@forEach

            if (sp != null) {
                val reg = when (e.type) {
                    Constants.ENEMY_WOBBLE -> {
                        if (e.dead) sp.wobbleDead[e.animFrame.coerceAtMost(sp.wobbleDead.size - 1)]
                        else        sp.wobblePatrol[e.animFrame % sp.wobblePatrol.size]
                    }
                    Constants.ENEMY_STITCHY -> {
                        if (e.dead) sp.stitchyDead[e.animFrame.coerceAtMost(sp.stitchyDead.size - 1)]
                        else        sp.stitchyPatrol[e.animFrame % sp.stitchyPatrol.size]
                    }
                    Constants.ENEMY_GLITCH -> {
                        if (e.dead) sp.glitchDead[e.animFrame.coerceAtMost(sp.glitchDead.size - 1)]
                        else        sp.glitchPatrol[e.animFrame % sp.glitchPatrol.size]
                    }
                    Constants.ENEMY_DIRECTOR -> {
                        if (e.dead) sp.directorDead[e.animFrame.coerceAtMost(sp.directorDead.size - 1)]
                        else        sp.directorPatrol[e.animFrame % sp.directorPatrol.size]
                    }
                    else -> sp.wobblePatrol[0]
                }
                batch.draw(atlas, e.x, e.y, e.width, e.height,
                    reg.u0, reg.v0, reg.u1, reg.v1,
                    flipX = !e.movingLeft)
            } else {
                // Fallback — coloured rect per enemy type
                val (r, g, b) = when (e.type) {
                    Constants.ENEMY_WOBBLE   -> Triple(0.78f, 0.24f, 0.24f)
                    Constants.ENEMY_STITCHY  -> Triple(0.78f, 0.47f, 0.24f)
                    Constants.ENEMY_GLITCH   -> Triple(0.24f, 0.78f, 0.71f)
                    Constants.ENEMY_DIRECTOR -> Triple(0.78f, 0.71f, 0.24f)
                    else                     -> Triple(0.78f, 0.24f, 0.24f)
                }
                val alpha = if (e.dead) 0.3f else 1f
                batch.draw(atlas, e.x, e.y, e.width, e.height,
                    0f, 0f, 0.001f, 0.001f, r, g, b, alpha)
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
                Player.Anim.IDLE -> sp.pipIdle
                Player.Anim.RUN  -> sp.pipRun
                Player.Anim.JUMP -> sp.pipJump
                Player.Anim.DEAD -> sp.pipDead
            }
            val reg = frames[p.animFrame.coerceAtMost(frames.size - 1)]
            batch.draw(atlas, p.x, p.y,
                Constants.PLAYER_WIDTH.toFloat(), Constants.PLAYER_HEIGHT.toFloat(),
                reg.u0, reg.v0, reg.u1, reg.v1,
                flipX = p.facingLeft)
        } else {
            // Fallback — magenta rect
            val alpha = if (p.invincible > 0 && (p.invincible / 4) % 2 == 0) 0f else 1f
            batch.draw(atlas, p.x, p.y,
                Constants.PLAYER_WIDTH.toFloat(), Constants.PLAYER_HEIGHT.toFloat(),
                0f, 0f, 0.001f, 0.001f, 0.48f, 0.18f, 0.75f, alpha)
        }
    }

    private fun tileColor(tile: Int) = when (tile) {
        Constants.TILE_FLOOR -> floatArrayOf(0.118f, 0f, 0.208f)
        Constants.TILE_WALL -> floatArrayOf(0.051f, 0.051f, 0.102f)
        Constants.TILE_PLATFORM -> floatArrayOf(0.482f, 0.184f, 0.745f)
        Constants.TILE_HAZARD -> floatArrayOf(1f, 0.176f, 0.333f)
        Constants.TILE_EXIT -> floatArrayOf(0f, 0.957f, 1f)
        else -> floatArrayOf(0.2f, 0.2f, 0.2f)
    }
}