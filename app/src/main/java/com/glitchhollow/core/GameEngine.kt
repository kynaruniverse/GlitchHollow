package com.glitchhollow.core

import android.content.Context
import java.util.LinkedList

class GameEngine(context: Context, val world: Int, val level: Int) {

    lateinit var player: Player
    lateinit var tileMap:   TileMap
    val enemies:   MutableList<Enemy> = mutableListOf()
    var levelName: String = ""

    var shardX = FloatArray(0); var shardY = FloatArray(0)
    var shardCollected = BooleanArray(0)
    var coinX  = FloatArray(0); var coinY  = FloatArray(0)
    var coinCollected  = BooleanArray(0)
    var coinGotThisRun = false

    var gameState    = Constants.STATE_PLAYING
    var elapsedMs    = 0L
    var tick         = 0
    var glitchFrames = 0
    var starsEarned  = BooleanArray(3)

    // ── Sound event queue ─────────────────────────────────────────
    // Drained each frame by GameScreen — no Android imports here
    val soundQueue: LinkedList<SoundEvent> = LinkedList()

    private var spawnX = 0f; private var spawnY = 0f
    private val loader = LevelLoader(context)
    private val collisionCache = Rect2D(0f, 0f, 0f, 0f)

    // Cached exit tile positions — built at load time, avoids full-map scan every frame
    val exitPositions: MutableList<Pair<Int, Int>> = mutableListOf()

    // Player state tracking for sound triggers
    private var wasOnGround = false

    init { loadLevel(world, level) }

    private fun loadLevel(w: Int, l: Int) {
        val data  = loader.load(w, l)
        levelName = data.name
        tileMap   = TileMap(data.tiles, data.rows, data.cols, w)

        spawnX = data.playerStartX * Constants.TILE_SIZE.toFloat()
        spawnY = data.playerStartY * Constants.TILE_SIZE.toFloat()
        player = Player(spawnX, spawnY)

        val sc = data.shards.size
        shardX = FloatArray(sc); shardY = FloatArray(sc)
        shardCollected = BooleanArray(sc)
        data.shards.forEachIndexed { i, s ->
            shardX[i] = s[0] * Constants.TILE_SIZE + (Constants.TILE_SIZE - 32) / 2f
            shardY[i] = s[1] * Constants.TILE_SIZE + (Constants.TILE_SIZE - 40) / 2f
        }

        val cc = data.coins.size
        coinX = FloatArray(cc); coinY = FloatArray(cc)
        coinCollected = BooleanArray(cc)
        data.coins.forEachIndexed { i, c ->
            coinX[i] = c[0] * Constants.TILE_SIZE + (Constants.TILE_SIZE - 28) / 2f
            coinY[i] = c[1] * Constants.TILE_SIZE + (Constants.TILE_SIZE - 28) / 2f
        }

        enemies.clear()
        enemies.addAll(data.enemies.map { ed ->
            Enemy(ed.type,
                ed.startCol * Constants.TILE_SIZE.toFloat(),
                ed.startRow * Constants.TILE_SIZE.toFloat(),
                ed.patrolCols * Constants.TILE_SIZE.toFloat())
        })

        // Cache exit tile positions so GameScreen never scans the full map
        exitPositions.clear()
        for (row in 0 until tileMap.rows)
            for (col in 0 until tileMap.cols)
                if (tileMap.isExit(col, row)) exitPositions.add(col to row)

        resetState()
    }

    private fun resetState() {
        gameState = Constants.STATE_PLAYING
        elapsedMs = 0L; tick = 0; glitchFrames = 0
        starsEarned = BooleanArray(3); coinGotThisRun = false
        soundQueue.clear()
        wasOnGround = false
    }

    fun update(deltaMs: Long) {
        if (gameState != Constants.STATE_PLAYING) return
        elapsedMs += deltaMs
        tick++
        if (glitchFrames > 0) glitchFrames--

        wasOnGround = player.onGround

        player.update(tileMap)
        enemies.forEach { it.update(tileMap) }

        // Land sound — wasn't on ground, now is
        if (!wasOnGround && player.onGround && player.velY == 0f) {
            soundQueue.add(SoundEvent.LAND)
        }

        // Jump sound — detected via velocity sign change upward
        if (player.velY == Constants.JUMP_FORCE) {
            soundQueue.add(SoundEvent.JUMP)
        }

        checkShardPickup()
        checkCoinPickup()
        checkEnemyCollision()
        checkHazardCollision()
        checkExit()

        if (player.y > tileMap.pixelHeight() + 100) killPlayer()
    }

    private fun checkShardPickup() {
        val p = player.getRect()
        shardX.indices.forEach { i ->
            if (shardCollected[i]) return@forEach
            collisionCache.set(shardX[i], shardY[i], 32f, 40f)
            if (p.intersects(collisionCache)) {
                shardCollected[i] = true
                glitchFrames = Constants.GLITCH_DURATION
                soundQueue.add(SoundEvent.SHARD_COLLECT)
                soundQueue.add(SoundEvent.GLITCH_PULSE)
            }
        }
    }

    private fun checkCoinPickup() {
        val p = player.getRect()
        coinX.indices.forEach { i ->
            if (coinCollected[i]) return@forEach
            collisionCache.set(coinX[i], coinY[i], 28f, 28f)
            if (p.intersects(collisionCache)) {
                coinCollected[i] = true
                coinGotThisRun = true
                soundQueue.add(SoundEvent.COIN_COLLECT)
            }
        }
    }

    private fun checkEnemyCollision() {
        if (player.invincible > 0) return
        val p = player.getRect()
        enemies.forEach { e ->
            if (e.dead) return@forEach
            collisionCache.set(e.x, e.y, e.width, e.height)
            if (!p.intersects(collisionCache)) return@forEach
            val stomp = player.velY > 2f &&
                (player.y + Constants.PLAYER_HEIGHT) < (e.y + e.height * 0.4f)
            if (stomp) {
                e.dead = true
                player.velY = Constants.JUMP_FORCE * 0.6f
                glitchFrames = Constants.GLITCH_DURATION
                soundQueue.add(SoundEvent.ENEMY_STOMP)
            } else {
                killPlayer()
            }
        }
    }

    private fun checkHazardCollision() {
        if (player.invincible > 0) return
        val col  = (player.x / Constants.TILE_SIZE).toInt()
        val row  = (player.y / Constants.TILE_SIZE).toInt()
        val col2 = ((player.x + Constants.PLAYER_WIDTH) / Constants.TILE_SIZE).toInt()
        if (tileMap.isHazard(col, row) || tileMap.isHazard(col2, row)) killPlayer()
    }

    private fun checkExit() {
        if (shardCollected.any { !it }) return
        val col  = (player.x / Constants.TILE_SIZE).toInt()
        val row  = (player.y / Constants.TILE_SIZE).toInt()
        val col2 = col + 1
        if (tileMap.isExit(col, row) || tileMap.isExit(col2, row)) winLevel()
    }

    private fun killPlayer() {
        player.lives--
        glitchFrames = Constants.GLITCH_DURATION * 2
        soundQueue.add(
            if (player.lives <= 0) SoundEvent.PLAYER_DEATH else SoundEvent.PLAYER_HURT
        )
        gameState = if (player.lives <= 0) Constants.STATE_GAMEOVER else Constants.STATE_DEAD
        player.respawn(spawnX, spawnY)
    }

    private fun winLevel() {
        gameState      = Constants.STATE_WIN
        starsEarned[0] = true
        starsEarned[1] = elapsedMs < Constants.STAR_TIME_BONUS * 1000L
        starsEarned[2] = coinGotThisRun
        soundQueue.add(SoundEvent.LEVEL_WIN)
    }

    fun respawnAfterDeath()  { player.respawn(spawnX, spawnY); gameState = Constants.STATE_PLAYING }
    fun restart()            { loadLevel(world, level) }
    fun allShardsCollected() = shardCollected.all { it }
    fun shardsGot()          = shardCollected.count { it }
}