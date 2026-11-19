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

        // Central Crimson Sector Objective Shrine (24x21 center)
        val objCoords = listOf(
            GridPos(11, 9), GridPos(12, 9),
            GridPos(11, 10), GridPos(12, 10),
            GridPos(11, 11), GridPos(12, 11)
        )
        objCoords.forEach { grid[it.y][it.x] = TerrainType.OBJECTIVE }

        // Symmetrical Gothic Ruin Bastions framing tactical avenues
        val fixedRuins = listOf(
            GridPos(7, 4), GridPos(7, 5), GridPos(8, 5),
            GridPos(16, 4), GridPos(16, 5), GridPos(15, 5),
            GridPos(7, 15), GridPos(7, 16), GridPos(8, 15),
            GridPos(16, 15), GridPos(16, 16), GridPos(15, 15),
            GridPos(6, 9), GridPos(6, 10), GridPos(6, 11),
            GridPos(17, 9), GridPos(17, 10), GridPos(17, 11)
        )
        fixedRuins.forEach {
            if (rng.nextFloat() > 0.15f) {
                grid[it.y][it.x] = TerrainType.RUIN_WALL
            } else {
                grid[it.y][it.x] = TerrainType.PILLAR
            }
        }

        // Mid-field Gothic Sanctuary Pillars
        val pillars = listOf(
            GridPos(4, 4), GridPos(19, 4),
            GridPos(4, 16), GridPos(19, 16),
            GridPos(10, 3), GridPos(13, 3),
            GridPos(10, 17), GridPos(13, 17),
            GridPos(9, 10), GridPos(14, 10)
        )
        pillars.forEach { grid[it.y][it.x] = TerrainType.PILLAR }

        // Tactical Barricades (half-cover)
        val barricades = listOf(
            GridPos(9, 7), GridPos(14, 7),
            GridPos(9, 13), GridPos(14, 13),
            GridPos(5, 7), GridPos(18, 7),
            GridPos(5, 13), GridPos(18, 13),
            GridPos(10, 6), GridPos(13, 14),
            GridPos(11, 4), GridPos(12, 16)
        )
        barricades.forEach {
            if (grid[it.y][it.x] == TerrainType.OPEN) {
                grid[it.y][it.x] = TerrainType.BARRICADE
            }
        }

        // Heavy Artillery Craters (rough ground + light cover)
        val craters = listOf(
            GridPos(5, 3), GridPos(18, 3),
            GridPos(5, 17), GridPos(18, 17),
            GridPos(8, 2), GridPos(15, 18),
            GridPos(8, 18), GridPos(15, 2),
            GridPos(9, 9), GridPos(14, 11),
            GridPos(9, 11), GridPos(14, 9)
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
