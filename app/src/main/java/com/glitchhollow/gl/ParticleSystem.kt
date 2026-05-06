package com.glitchhollow.gl

import com.glitchhollow.core.Constants
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Object-pool particle system. No allocation after init.
 *
 * Pool concept: creating/destroying objects every frame causes GC pauses
 * on Android (stutters). Instead we pre-allocate a fixed array of
 * Particle objects at startup and reuse them. Dead particles wait in the
 * pool until emitted again.
 *
 * Each particle is a coloured quad drawn via SpriteBatch.
 * No texture needed — we tint the 1×1 white pixel (atlas top-left).
 *
 * Emitter types:
 *   shardPickup(x, y)   — cyan/white sparks explode outward
 *   coinPickup(x, y)    — yellow/gold arcs upward
 *   stompDust(x, y)     — white/grey puffs spread horizontally
 *   playerDeath(x, y)   — magenta/white fragments scatter
 *   exitPulse(x, y)     — continuous slow cyan drift (called every frame)
 */
class ParticleSystem {

    // ── Particle data (struct-of-arrays for cache efficiency) ─────

    private val MAX = Constants.PARTICLE_POOL_SIZE

    private val px    = FloatArray(MAX)   // position x
    private val py    = FloatArray(MAX)   // position y
    private val pvx   = FloatArray(MAX)   // velocity x
    private val pvy   = FloatArray(MAX)   // velocity y
    private val pr    = FloatArray(MAX)   // colour red
    private val pg    = FloatArray(MAX)   // colour green
    private val pb    = FloatArray(MAX)   // colour blue
    private val pa    = FloatArray(MAX)   // alpha (current)
    private val pLife = FloatArray(MAX)   // remaining lifetime (0..1)
    private val pSize = FloatArray(MAX)   // size in pixels
    private val pAlive = BooleanArray(MAX)

    private var time = 0f

    // ── Update ────────────────────────────────────────────────────

    fun update(dt: Float) {
        time += dt
        for (i in 0 until MAX) {
            if (!pAlive[i]) continue

            pLife[i] -= dt * 2f   // lifespan ~0.5s at default
            if (pLife[i] <= 0f) { pAlive[i] = false; continue }

            px[i]  += pvx[i]
            py[i]  += pvy[i]
            pvy[i] += Constants.PARTICLE_GRAVITY
            pvx[i] *= Constants.PARTICLE_DRAG
            pvy[i] *= Constants.PARTICLE_DRAG

            pa[i]  = (pLife[i]).coerceIn(0f, 1f)
        }
    }

    // ── Render ────────────────────────────────────────────────────

    /**
     * Draw all alive particles. Must be called between batch.begin/end.
     * Uses the atlas top-left pixel (0,0)→(0.001,0.001) as a white pixel.
     */
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

    /** Cyan/white sparks — shard collected */
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
                Random.nextFloat() * 0.5f + 0.75f
            )
        }
    }

    /** Yellow/gold arcs — coin collected */
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
                Random.nextFloat() * 0.4f + 0.7f
            )
        }
    }

    /** White/grey puffs — stomp */
    fun stompDust(x: Float, y: Float) {
        repeat(Constants.STOMP_DUST_COUNT) {
            val dir = if (Random.nextBoolean()) 1f else -1f
            val i = allocate() ?: return@repeat
            val v = Random.nextFloat() * 0.8f + 0.2f
            set(i, x, y,
                dir * (Random.nextFloat() * 2f + 0.5f), -Random.nextFloat() * 1.5f,
                v, v, v,
                Random.nextFloat() * 6f + 4f,
                Random.nextFloat() * 0.3f + 0.5f
            )
        }
    }

    /** Magenta/white fragments — player death */
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
                Random.nextFloat() * 0.6f + 0.4f
            )
        }
    }

    /** Slow upward cyan drift — exit tile ambient */
    fun exitPulse(x: Float, y: Float) {
        if (Random.nextFloat() > 0.15f) return  // only spawn 15% of frames
        val i = allocate() ?: return
        set(i, x + Random.nextFloat() * 48f, y,
            (Random.nextFloat() - 0.5f) * 0.6f, -Random.nextFloat() * 1.2f - 0.4f,
            0f, 0.96f, 1f,
            Random.nextFloat() * 3f + 2f,
            0.5f
        )
    }

    // ── Internal helpers ──────────────────────────────────────────

    private fun allocate(): Int? {
        for (i in 0 until MAX) {
            if (!pAlive[i]) { pAlive[i] = true; return i }
        }
        return null  // pool full — skip this particle
    }

    private fun set(i: Int, x: Float, y: Float,
                    vx: Float, vy: Float,
                    r: Float, g: Float, b: Float,
                    size: Float, life: Float) {
        px[i] = x; py[i] = y; pvx[i] = vx; pvy[i] = vy
        pr[i] = r; pg[i] = g; pb[i] = b;   pa[i] = life
        pSize[i] = size; pLife[i] = life
    }
}