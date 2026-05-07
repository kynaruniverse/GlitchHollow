package com.glitchhollow.gl

import com.glitchhollow.core.Constants
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class ParticleSystem {

    companion object {
        private const val MAX = Constants.PARTICLE_POOL_SIZE
    }

    private val px    = FloatArray(MAX)
    private val py    = FloatArray(MAX)
    private val pvx   = FloatArray(MAX)
    private val pvy   = FloatArray(MAX)
    private val pr    = FloatArray(MAX)
    private val pg    = FloatArray(MAX)
    private val pb    = FloatArray(MAX)
    private val pa    = FloatArray(MAX)
    private val pLife = FloatArray(MAX)
    private val pSize = FloatArray(MAX)
    private val pAlive = BooleanArray(MAX)

    /** Live particle count — read by DebugOverlay */
    val liveCount get() = pAlive.count { it }

    private var time = 0f

    // ── Update ────────────────────────────────────────────────────

    fun update(dt: Float) {
        time += dt
        for (i in 0 until MAX) {
            if (!pAlive[i]) continue
            pLife[i] -= dt * 2f
            if (pLife[i] <= 0f) { pAlive[i] = false; continue }
            px[i]  += pvx[i]
            py[i]  += pvy[i]
            pvy[i] += Constants.PARTICLE_GRAVITY
            pvx[i] *= Constants.PARTICLE_DRAG
            pvy[i] *= Constants.PARTICLE_DRAG
            pa[i]  = pLife[i].coerceIn(0f, 1f)
        }
    }

    // ── Render ────────────────────────────────────────────────────

    fun draw(batch: SpriteBatch, atlas: Texture) {
        for (i in 0 until MAX) {
            if (!pAlive[i] || pa[i] < 0.01f) continue
            val s = pSize[i] * pLife[i].coerceIn(0.3f, 1f)
            batch.draw(atlas,
                px[i] - s / 2f, py[i] - s / 2f,
                s, s,
                0f, 0f, 0.001f, 0.001f,
                pr[i], pg[i], pb[i], pa[i]
            )
        }
    }

    // ── Emitters ──────────────────────────────────────────────────

    fun shardPickup(x: Float, y: Float) {
        repeat(Constants.SHARD_BURST_COUNT) {
            val angle = Random.nextFloat() * Math.PI.toFloat() * 2f
            val speed = Random.nextFloat() * 3f + 1.5f
            val i = allocate() ?: return@repeat
            set(i, x, y,
                cos(angle) * speed, sin(angle) * speed,
                if (Random.nextBoolean()) 0f else 1f,
                0.85f + Random.nextFloat() * 0.15f,
                1f,
                Random.nextFloat() * 4f + 3f,
                Random.nextFloat() * 0.5f + 0.75f)
        }
    }

    fun coinPickup(x: Float, y: Float) {
        repeat(Constants.COIN_BURST_COUNT) {
            val angle = -Math.PI.toFloat() / 2f + (Random.nextFloat() - 0.5f) * 1.6f
            val speed = Random.nextFloat() * 3.5f + 1f
            val i = allocate() ?: return@repeat
            set(i, x, y,
                cos(angle) * speed, sin(angle) * speed,
                1f,
                0.75f + Random.nextFloat() * 0.25f,
                Random.nextFloat() * 0.3f,
                Random.nextFloat() * 5f + 3f,
                Random.nextFloat() * 0.4f + 0.7f)
        }
    }

    fun stompDust(x: Float, y: Float) {
        repeat(Constants.STOMP_DUST_COUNT) {
            val dir = if (Random.nextBoolean()) 1f else -1f
            val i = allocate() ?: return@repeat
            val v = Random.nextFloat() * 0.8f + 0.2f
            set(i, x, y,
                dir * (Random.nextFloat() * 2f + 0.5f), -Random.nextFloat() * 1.5f,
                v, v, v,
                Random.nextFloat() * 6f + 4f,
                Random.nextFloat() * 0.3f + 0.5f)
        }
    }

    fun playerDeath(x: Float, y: Float) {
        repeat(Constants.DEATH_BURST_COUNT) {
            val angle = Random.nextFloat() * Math.PI.toFloat() * 2f
            val speed = Random.nextFloat() * 5f + 2f
            val i = allocate() ?: return@repeat
            val isMagenta = Random.nextBoolean()
            set(i, x, y,
                cos(angle) * speed, sin(angle) * speed,
                if (isMagenta) 1f else 1f,
                if (isMagenta) 0.23f else 1f,
                if (isMagenta) 0.67f else 1f,
                Random.nextFloat() * 5f + 2f,
                Random.nextFloat() * 0.6f + 0.4f)
        }
    }

    fun exitPulse(x: Float, y: Float) {
        if (Random.nextFloat() > 0.15f) return
        val i = allocate() ?: return
        set(i, x + Random.nextFloat() * 48f, y,
            (Random.nextFloat() - 0.5f) * 0.6f, -Random.nextFloat() * 1.2f - 0.4f,
            0f, 0.96f, 1f,
            Random.nextFloat() * 3f + 2f,
            0.5f)
    }

    // ── Internal ──────────────────────────────────────────────────

    private fun allocate(): Int? {
        for (i in 0 until MAX) {
            if (!pAlive[i]) { pAlive[i] = true; return i }
        }
        return null
    }

    private fun set(
        i: Int,
        x: Float, y: Float,
        vx: Float, vy: Float,
        r: Float, g: Float, b: Float,
        size: Float, life: Float
    ) {
        px[i] = x;    py[i] = y
        pvx[i] = vx;  pvy[i] = vy
        pr[i] = r;    pg[i] = g;   pb[i] = b
        pa[i] = life; pSize[i] = size; pLife[i] = life
    }
}