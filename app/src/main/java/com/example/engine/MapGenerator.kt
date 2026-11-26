package com.example.engine

import com.example.model.GridPos
import com.example.model.TerrainType
import kotlin.random.Random

object MapGenerator {
    const val GRID_W = 24
    const val GRID_H = 21

    fun generateSectorMap(seed: Long = System.currentTimeMillis()): Array<Array<TerrainType>> {
        val rng = Random(seed)
        val grid = Array(GRID_H) { Array(GRID_W) { TerrainType.OPEN } }

        // Clean Central Objective Arena (center of 24x21 grid)
        val objCoords = listOf(
            GridPos(11, 9), GridPos(12, 9),
            GridPos(11, 10), GridPos(12, 10),
            GridPos(11, 11), GridPos(12, 11)
        )
        objCoords.forEach { grid[it.y][it.x] = TerrainType.OBJECTIVE }

        // Subtle Corner Boundary Pillars framing the wide open arena
        val cornerPillars = listOf(
            GridPos(4, 4), GridPos(19, 4),
            GridPos(4, 16), GridPos(19, 16)
        )
        cornerPillars.forEach { grid[it.y][it.x] = TerrainType.PILLAR }

        return grid
    }

    fun isWalkable(terrain: TerrainType): Boolean =
        terrain != TerrainType.RUIN_WALL && terrain != TerrainType.PILLAR

    fun blocksLos(terrain: TerrainType): Boolean =
        terrain == TerrainType.RUIN_WALL || terrain == TerrainType.PILLAR

    fun moveCost(terrain: TerrainType): Int = when (terrain) {
        TerrainType.CRATER -> 2
        TerrainType.BARRICADE -> 2
        else -> 1
    }
}
