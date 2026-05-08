package com.glitchhollow.gl

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.util.Log
import com.glitchhollow.core.SoundEvent

/**
 * Manages all game audio — SFX via SoundPool, BGM via MediaPlayer.
 *
 * SoundPool: designed for short, frequently-triggered sounds (< 1s).
 * Pre-loads all SFX into RAM so playback is instant with no I/O lag.
 *
 * MediaPlayer: for long-form looping BGM. One track at a time.
 * Crossfades between world tracks handled by fade coroutine alternative
 * (we use a simple volume ramp via Handler since no coroutines here).
 *
 * Audio file locations: app/src/main/assets/audio/
 *   sfx/jump.ogg, land.ogg, shard.ogg, coin.ogg,
 *       stomp.ogg, hurt.ogg, death.ogg, win.ogg,
 *       menu_select.ogg, menu_back.ogg, glitch.ogg
 *   bgm/world1.ogg, world2.ogg, world3.ogg, world4.ogg, menu.ogg
 *
 * All audio files are OGG Vorbis (best quality/size on Android).
 * Fallback: if a file doesn't exist, that sound is silently skipped.
 *
 * Settings integration: reads gh_settings prefs for music/sfx toggles.
 */
class AudioManager(private val context: Context) {

    companion object {
        private const val MAX_STREAMS = 8  // simultaneous SFX
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences("gh_settings", Context.MODE_PRIVATE)

    private val sfxEnabled get() = prefs.getBoolean("sfx",   true)
    private val bgmEnabled get() = prefs.getBoolean("music", true)

    // ── SoundPool ─────────────────────────────────────────────────

    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(MAX_STREAMS)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    // Sound ID map — 0 means not loaded / file missing
    private val sfxIds = mutableMapOf<SoundEvent, Int>()

    // ── MediaPlayer (BGM) ─────────────────────────────────────────

    private var bgmPlayer:    MediaPlayer? = null
    private var currentTrack: String       = ""
    private var bgmVolume = 0.7f
    private var sfxVolume = 0.85f

    // ── Init ──────────────────────────────────────────────────────

    init { loadSfx() }

    private fun loadSfx() {
        val map = mapOf(
            SoundEvent.JUMP          to "audio/sfx/jump.ogg",
            SoundEvent.LAND          to "audio/sfx/land.ogg",
            SoundEvent.SHARD_COLLECT to "audio/sfx/shard.ogg",
            SoundEvent.COIN_COLLECT  to "audio/sfx/coin.ogg",
            SoundEvent.ENEMY_STOMP   to "audio/sfx/stomp.ogg",
            SoundEvent.PLAYER_HURT   to "audio/sfx/hurt.ogg",
            SoundEvent.PLAYER_DEATH  to "audio/sfx/death.ogg",
            SoundEvent.LEVEL_WIN     to "audio/sfx/win.ogg",
            SoundEvent.MENU_SELECT   to "audio/sfx/menu_select.ogg",
            SoundEvent.MENU_BACK     to "audio/sfx/menu_back.ogg",
            SoundEvent.GLITCH_PULSE  to "audio/sfx/glitch.ogg"
        )

        map.forEach { (event, path) ->
            try {
                val afd = context.assets.openFd(path)
                val id  = pool.load(afd, 1)
                sfxIds[event] = id
                afd.close()
            } catch (e: Exception) {
                // File missing — silently skip, game runs without it
                Log.d("AudioManager", "SFX not found (skipped): $path")
            }
        }
    }

    // ── Public API ────────────────────────────────────────────────

    /** Play a sound effect immediately */
    fun play(event: SoundEvent) {
        if (!sfxEnabled) return
        val id = sfxIds[event] ?: return
        if (id == 0) return
        pool.play(id, sfxVolume, sfxVolume, 1, 0, 1f)
    }

    /** Play multiple events from the engine's queue */
    fun drainQueue(queue: java.util.LinkedList<SoundEvent>) {
        while (queue.isNotEmpty()) play(queue.poll())
    }

    /**
     * Switch BGM track. Does nothing if the same track is already playing.
     * @param trackName one of: "menu", "world1", "world2", "world3", "world4"
     */
    fun playBgm(trackName: String) {
        if (trackName == currentTrack && bgmPlayer?.isPlaying == true) return
        currentTrack = trackName
        stopBgm()
        if (!bgmEnabled) return

        try {
            val path = "audio/bgm/$trackName.ogg"
            val afd  = context.assets.openFd(path)
            bgmPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                isLooping = true
                setVolume(bgmVolume, bgmVolume)
                setOnPreparedListener { it.start() }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.d("AudioManager", "BGM not found (skipped): $trackName.ogg")
        }
    }

    fun stopBgm() {
        bgmPlayer?.apply { if (isPlaying) stop(); release() }
        bgmPlayer = null
    }

    fun pauseBgm()  { bgmPlayer?.takeIf { it.isPlaying }?.pause() }
    fun resumeBgm() { if (bgmEnabled) bgmPlayer?.takeIf { !it.isPlaying }?.start() }

    /** Call from SettingsScreen when music toggle changes */
    fun onSettingsChanged() {
        if (!bgmEnabled) stopBgm()
        else if (currentTrack.isNotEmpty()) playBgm(currentTrack)
    }

    /** Release all resources — call from onDestroy */
    fun dispose() {
        pool.release()
        stopBgm()
    }
}