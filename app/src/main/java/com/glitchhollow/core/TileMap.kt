package com.glitchhollow.core

class TileMap(
    val tiles: Array<IntArray>,
    val rows:  Int,
    val cols:  Int,
    val world: Int
) {
    fun getTile(col: Int, row: Int): Int {
        if (col < 0 || col >= cols || row < 0 || row >= rows) return Constants.TILE_WALL
        return tiles[row][col]
    }

    fun isSolid(col: Int, row: Int): Boolean {
        val t = getTile(col, row)
        return t == Constants.TILE_FLOOR ||
               t == Constants.TILE_WALL  ||
               t == Constants.TILE_HAZARD
    }

    fun isPlatform(col: Int, row: Int) = getTile(col, row) == Constants.TILE_PLATFORM
    fun isHazard(col: Int, row: Int)   = getTile(col, row) == Constants.TILE_HAZARD
    fun isExit(col: Int, row: Int)     = getTile(col, row) == Constants.TILE_EXIT

    fun pixelWidth()  = cols * Constants.TILE_SIZE.toFloat()
    fun pixelHeight() = rows * Constants.TILE_SIZE.toFloat()
}