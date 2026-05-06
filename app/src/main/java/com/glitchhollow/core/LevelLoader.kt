package com.glitchhollow.core

import android.content.Context
import org.json.JSONObject

class LevelLoader(private val context: Context) {

    fun load(world: Int, level: Int): LevelData {
        val filename = "levels/w${world}_l${level}.json"
        val json = context.assets.open(filename)
            .bufferedReader()
            .use { it.readText() }
        return parse(json)
    }

    private fun parse(json: String): LevelData {
        val obj     = JSONObject(json)
        val rows    = obj.getInt("rows")
        val cols    = obj.getInt("cols")
        val name    = obj.getString("name")

        val start   = obj.getJSONObject("player_start")
        val startX  = start.getInt("x")
        val startY  = start.getInt("y")

        // Parse 2D tile array
        val tilesJson = obj.getJSONArray("tiles")
        val tiles = Array(rows) { r ->
            val row = tilesJson.getJSONArray(r)
            IntArray(cols) { c -> row.getInt(c) }
        }

        // Shards
        val shardsJson = obj.getJSONArray("shards")
        val shards = (0 until shardsJson.length()).map { i ->
            val s = shardsJson.getJSONObject(i)
            intArrayOf(s.getInt("x"), s.getInt("y"))
        }

        // Coins
        val coinsJson = obj.getJSONArray("coins")
        val coins = (0 until coinsJson.length()).map { i ->
            val c = coinsJson.getJSONObject(i)
            intArrayOf(c.getInt("x"), c.getInt("y"))
        }

        // Enemies
        val enemiesJson = obj.getJSONArray("enemies")
        val enemies = (0 until enemiesJson.length()).map { i ->
            val e = enemiesJson.getJSONObject(i)
            LevelData.EnemyData(
                type       = e.getInt("type"),
                startCol   = e.getInt("startCol"),
                startRow   = e.getInt("startRow"),
                patrolCols = e.getInt("patrolCols")
            )
        }

        return LevelData(name, rows, cols, tiles, startX, startY, shards, coins, enemies)
    }
}