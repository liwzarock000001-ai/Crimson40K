package com.example.engine

import com.example.model.GridPos
import com.example.model.TerrainType
import kotlin.random.Random

object MapGenerator {
    const val GRID_W = 16
    const val GRID_H = 14

    fun generateSectorMap(seed: Long = System.currentTimeMillis()): Array<Array<TerrainType>> {
        val rng = Random(seed)
        val grid = Array(GRID_H) { Array(GRID_W) { TerrainType.OPEN } }

        // Central Crimson Sector Objective Shrine
        val objCoords = listOf(
            GridPos(7, 6), GridPos(8, 6),
            GridPos(7, 7), GridPos(8, 7)
        )
        objCoords.forEach { grid[it.y][it.x] = TerrainType.OBJECTIVE }

        // Symmetrical Gothic Ruin Bastions framing center lanes
        val fixedRuins = listOf(
            GridPos(5, 3), GridPos(5, 4), GridPos(10, 9), GridPos(10, 10),
            GridPos(10, 3), GridPos(10, 4), GridPos(5, 9), GridPos(5, 10),
            GridPos(4, 6), GridPos(4, 7), GridPos(11, 6), GridPos(11, 7)
        )
        fixedRuins.forEach {
            if (rng.nextFloat() > 0.15f) {
                grid[it.y][it.x] = TerrainType.RUIN_WALL
            } else {
                grid[it.y][it.x] = TerrainType.PILLAR
            }
        }

        // Pillars in mid-field flanking corridors
        val pillars = listOf(
            GridPos(3, 3), GridPos(12, 10),
            GridPos(3, 10), GridPos(12, 3),
            GridPos(7, 2), GridPos(8, 11)
        )
        pillars.forEach { grid[it.y][it.x] = TerrainType.PILLAR }

        // Tactical Barricades (half-cover)
        val barricades = listOf(
            GridPos(6, 5), GridPos(9, 8),
            GridPos(9, 5), GridPos(6, 8),
            GridPos(3, 6), GridPos(12, 7),
            GridPos(7, 4), GridPos(8, 9)
        )
        barricades.forEach {
            if (grid[it.y][it.x] == TerrainType.OPEN) {
                grid[it.y][it.x] = TerrainType.BARRICADE
            }
        }

        // Artillery Craters (rough ground + light cover)
        val craters = listOf(
            GridPos(4, 2), GridPos(11, 11),
            GridPos(6, 1), GridPos(9, 12),
            GridPos(4, 11), GridPos(11, 2),
            GridPos(6, 11), GridPos(9, 2)
        )
        craters.forEach {
            if (grid[it.y][it.x] == TerrainType.OPEN && rng.nextFloat() > 0.25f) {
                grid[it.y][it.x] = TerrainType.CRATER
            }
        }

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
