package com.example.model

import androidx.compose.runtime.Immutable

enum class Faction(val displayName: String, val badgeTag: String) {
    MARINES("Ultramarines", "XIII: ULTRAMARINES"),
    XENOS("Alien Swarm", "HIVE FLEET")
}

enum class Facing(val dx: Int, val dy: Int, val label: String) {
    NORTH(0, -1, "N"),
    EAST(1, 0, "E"),
    SOUTH(0, 1, "S"),
    WEST(-1, 0, "W");

    fun turnLeft(): Facing = when (this) {
        NORTH -> WEST
        WEST -> SOUTH
        SOUTH -> EAST
        EAST -> NORTH
    }

    fun turnRight(): Facing = when (this) {
        NORTH -> EAST
        EAST -> SOUTH
        SOUTH -> WEST
        WEST -> NORTH
    }

    fun yawDegrees(): Float = when (this) {
        NORTH -> -90f
        EAST -> 0f
        SOUTH -> 90f
        WEST -> 180f
    }
}

enum class WarriorWeaponChoice(val label: String) {
    BONESWORDS("Dual Boneswords (Melee Rng 1)"),
    FLESHBORER("Fleshborer (Ranged Rng 4)")
}

enum class UnitArchetype {
    INTERCESSOR,
    HELLBLASTER,
    ASSAULT_MARINE,
    TERMINATOR,
    DREADNOUGHT,
    CAPTAIN,
    TERMAGANT,
    HORMAGAUNT,
    WARRIOR,
    GENESTEALER,
    CARNIFEX,
    HIVE_TYRANT
}

@Immutable
data class UnitTemplate(
    val id: String,
    val name: String,
    val faction: Faction,
    val archetype: UnitArchetype,
    val role: String,
    val points: Int,
    val squadSize: Int,
    val hpPerModel: Int,
    val armor: Int,
    val armorPen: Int,
    val mp: Int,
    val ap: Int,
    val damage: Int,
    val attacksPerModel: Int,
    val range: Int,
    val accuracy: Int,
    val skillName: String,
    val skillDesc: String,
    val skillCooldown: Int,
    val skillApCost: Int,
    val passiveName: String,
    val passiveDesc: String,
    val weaponChoice: WarriorWeaponChoice? = null,
    val pistolDamage: Int = 0,
    val pistolApCost: Int = 1,
    val pistolRange: Int = 3,
    val pistolCooldown: Int = 1
) {
    val maxTotalHp: Int get() = squadSize * hpPerModel
    val hasPistol: Boolean get() = pistolDamage > 0 && pistolRange > 0
}

@Immutable
data class RosterSlot(
    val slotId: String,
    val templateId: String
)

enum class TerrainType {
    OPEN,
    RUIN_WALL,
    PILLAR,
    BARRICADE,
    CRATER,
    OBJECTIVE
}

@Immutable
data class GridPos(val x: Int, val y: Int) {
    fun manhattan(other: GridPos): Int = kotlin.math.abs(x - other.x) + kotlin.math.abs(y - other.y)
    fun chebyshev(other: GridPos): Int = maxOf(kotlin.math.abs(x - other.x), kotlin.math.abs(y - other.y))
}

@Immutable
class SectorMap(val id: Long, val rows: Array<Array<TerrainType>>) {
    operator fun get(y: Int): Array<TerrainType> = rows[y]
    fun at(x: Int, y: Int): TerrainType = rows[y][x]
    override fun equals(other: Any?): Boolean = other is SectorMap && other.id == id && other.rows === rows
    override fun hashCode(): Int = id.hashCode()
}

@Immutable
data class BattleUnit(
    val uid: String,
    val template: UnitTemplate,
    val faction: Faction,
    val x: Int,
    val y: Int,
    val facing: Facing,
    val totalHp: Int,
    val maxTotalHp: Int,
    val mpLeft: Int,
    val apLeft: Int,
    val momentum: Int = 0,
    val skillCooldownLeft: Int = 0,
    val pistolCooldownLeft: Int = 0,
    val buffTurnsLeft: Int = 0,
    val activeBuffLabel: String? = null,
    val surgeActive: Boolean = false,
    val damageReductionTurns: Int = 0,
    val auraBuffed: Boolean = false,
    val kills: Int = 0,
    val damageDealt: Int = 0
) {
    val pos: GridPos get() = GridPos(x, y)
    val isAlive: Boolean get() = totalHp > 0
    val aliveModels: Int
        get() {
            if (totalHp <= 0) return 0
            val hpPer = template.hpPerModel.coerceAtLeast(1)
            return ((totalHp + hpPer - 1) / hpPer).coerceIn(1, template.squadSize)
        }
    val effectiveDamage: Int
        get() {
            var d = template.damage
            if (surgeActive) d = (d * 1.25f).toInt().coerceAtLeast(d + 2)
            if (buffTurnsLeft > 0) d = (d * 1.15f).toInt().coerceAtLeast(d + 1)
            return d
        }
    val effectiveAccuracy: Int
        get() {
            var a = template.accuracy
            if (auraBuffed) a += 10
            if (buffTurnsLeft > 0) a += 10
            if (surgeActive) a += 10
            return a.coerceIn(5, 99)
        }
}

enum class WeaponMode {
    MAIN,
    PISTOL,
    SKILL
}

enum class CoverTag(val label: String) {
    OPEN("OPEN"),
    COVER("COVER"),
    FLANK("FLANK"),
    REAR("REAR")
}

@Immutable
data class TargetPreview(
    val targetUid: String,
    val targetName: String,
    val hitChance: Int,
    val expectedTotalDamage: Int,
    val attacksCount: Int,
    val coverTag: CoverTag,
    val weaponMode: WeaponMode,
    val apCost: Int,
    val inRange: Boolean,
    val hasLos: Boolean
)

@Immutable
data class ProjectileVfx(
    val id: Long,
    val fromX: Float,
    val fromY: Float,
    val toX: Float,
    val toY: Float,
    val colorArgb: Long,
    val isBeam: Boolean = false,
    val isMelee: Boolean = false,
    val startTimeMs: Long
)

@Immutable
data class FloatingTextVfx(
    val id: Long,
    val gridX: Float,
    val gridY: Float,
    val text: String,
    val colorArgb: Long,
    val startTimeMs: Long
)

@Immutable
data class TurnSplashData(
    val round: Int,
    val activeFaction: Faction,
    val isPlayerTurn: Boolean,
    val marineModelsAlive: Int,
    val xenosModelsAlive: Int,
    val playerKills: Int,
    val playerLosses: Int
)

@Immutable
data class BattleSummary(
    val winner: Faction,
    val playerWon: Boolean,
    val rounds: Int,
    val playerKills: Int,
    val enemyKills: Int,
    val totalDamageDealt: Int
)
