package com.glitchhollow.core

data class LevelData(
    val name: String,
    val rows: Int,
    val cols: Int,
    val tiles: Array<IntArray>,
    val playerStartX: Int,
    val playerStartY: Int,
    val shards: List<IntArray>,   // each = [col, row]
    val coins: List<IntArray>,    // each = [col, row]
    val enemies: List<EnemyData>
) {
    data class EnemyData(
        val type: Int,
        val startCol: Int,
        val startRow: Int,
        val patrolCols: Int
    )
}