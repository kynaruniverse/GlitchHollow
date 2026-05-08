package com.glitchhollow.core

import android.content.Context
import org.json.JSONObject

class LevelLoader(private val context: Context) {

    fun load(world: Int, level: Int): LevelData {
        val filename = "levels/w${world}_l${level}.json"

        return try {
            val json = context.assets.open(filename)
                .bufferedReader()
                .use { it.readText() }

            parse(json)

        } catch (e: Exception) {
            // SAFE FALLBACK LEVEL (never crash game)
            LevelData(
                name = "Fallback Level",
                rows = 1,
                cols = 1,
                tiles = arrayOf(intArrayOf(0)),
                playerStartX = 0,
                playerStartY = 0,
                shards = emptyList(),
                coins = emptyList(),
                enemies = emptyList()
            )
        }
    }

    private fun parse(json: String): LevelData {
        val obj = JSONObject(json)

        val rows = obj.getInt("rows")
        val cols = obj.getInt("cols")
        val name = obj.getString("name")

        val start = obj.getJSONObject("player_start")
        val startX = start.getInt("x")
        val startY = start.getInt("y")

        val tilesJson = obj.getJSONArray("tiles")
        val tiles = Array(rows) { r ->
            val row = tilesJson.getJSONArray(r)
            IntArray(cols) { c -> row.getInt(c) }
        }

        val shards = obj.getJSONArray("shards").let { arr ->
            (0 until arr.length()).map {
                val s = arr.getJSONObject(it)
                intArrayOf(s.getInt("x"), s.getInt("y"))
            }
        }

        val coins = obj.getJSONArray("coins").let { arr ->
            (0 until arr.length()).map {
                val c = arr.getJSONObject(it)
                intArrayOf(c.getInt("x"), c.getInt("y"))
            }
        }

        val enemies = obj.getJSONArray("enemies").let { arr ->
            (0 until arr.length()).map {
                val e = arr.getJSONObject(it)
                LevelData.EnemyData(
                    type = e.getInt("type"),
                    startCol = e.getInt("startCol"),
                    startRow = e.getInt("startRow"),
                    patrolCols = e.getInt("patrolCols")
                )
            }
        }

        return LevelData(name, rows, cols, tiles, startX, startY, shards, coins, enemies)
    }
}
