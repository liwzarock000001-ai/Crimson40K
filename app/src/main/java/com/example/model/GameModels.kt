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
        NORTH -> 90f
        EAST -> 0f
        SOUTH -> -90f
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
    val woundsPerModel: Int,
    val toughness: Int,
    val save: Int,
    val invulnSave: Int? = null,
    val armorPen: Int,
    val moveCm: Int,
    val strength: Int,
    val damage: Int,
    val attacksPerModel: Int,
    val range: Int,
    val ballisticSkill: Int,
    val weaponSkill: Int,
    val skillName: String,
    val skillDesc: String,
    val skillCooldown: Int,
    val passiveName: String,
    val passiveDesc: String,
    val weaponChoice: WarriorWeaponChoice? = null,
    val mainWeaponName: String = "Primary Weapon",
    val mainWeaponAssault: Boolean = false,
    val pistolWeaponName: String = "Bolt Pistol",
    val pistolAttacks: Int = 1,
    val pistolDamage: Int = 0,
    val pistolStrength: Int = 4,
    val pistolArmorPen: Int = 0,
    val pistolRange: Int = 3,
    val pistolCooldown: Int = 1
) {
    val maxTotalWounds: Int get() = squadSize * woundsPerModel
    val hasPistol: Boolean get() = pistolDamage > 0 && pistolRange > 0
    val isRangedMain: Boolean get() = range > 1
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
data class WorldPos(val x: Float, val y: Float)

@Immutable
data class MovePreview(
    val targetX: Float,
    val targetY: Float,
    val distanceCm: Float,
    val path: List<WorldPos>
)

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
    val totalWounds: Int,
    val maxTotalWounds: Int,
    val moveCmLeft: Float,
    val hasAdvanced: Boolean = false,
    val hasCharged: Boolean = false,
    val hasShot: Boolean = false,
    val hasFought: Boolean = false,
    val worldX: Float = x.toFloat(),
    val worldY: Float = y.toFloat(),
    val facingDeg: Float = facing.yawDegrees(),
    val skillCooldownLeft: Int = 0,
    val pistolCooldownLeft: Int = 0,
    val buffTurnsLeft: Int = 0,
    val activeBuffLabel: String? = null,
    val damageReductionTurns: Int = 0,
    val auraBuffed: Boolean = false,
    val kills: Int = 0,
    val damageDealt: Int = 0
) {
    val pos: GridPos get() = GridPos(x, y)
    val worldPos: WorldPos get() = WorldPos(worldX, worldY)
    val isAlive: Boolean get() = totalWounds > 0
    val hasRangedProfile: Boolean
        get() = template.range > 1 ||
            template.hasPistol ||
            template.archetype == UnitArchetype.CARNIFEX ||
            template.archetype == UnitArchetype.HIVE_TYRANT
    val hasMeleeProfile: Boolean get() = template.range <= 1 || hasCharged
    val canAdvance: Boolean get() = isAlive && !hasAdvanced && !hasShot && !hasCharged && !hasFought
    val canCharge: Boolean get() = isAlive && !hasAdvanced && !hasCharged && !hasFought
    val canShoot: Boolean
        get() = isAlive && !hasShot && !hasCharged && hasRangedProfile &&
            (!hasAdvanced || template.mainWeaponAssault)
    val canFight: Boolean get() = isAlive && !hasFought && hasMeleeProfile
    val aliveModels: Int
        get() {
            if (totalWounds <= 0) return 0
            val wPer = template.woundsPerModel.coerceAtLeast(1)
            return ((totalWounds + wPer - 1) / wPer).coerceIn(1, template.squadSize)
        }
    val effectiveDamage: Int
        get() {
            var d = template.damage
            if (buffTurnsLeft > 0) d += 1
            return d
        }
    val effectiveHitBonus: Int
        get() {
            var bonus = 0
            if (auraBuffed) bonus += 1
            if (buffTurnsLeft > 0) bonus += 1
            return bonus
        }
    val effectiveBallisticSkill: Int
        get() = (template.ballisticSkill - effectiveHitBonus).coerceIn(2, 6)
    val effectiveWeaponSkill: Int
        get() = (template.weaponSkill - effectiveHitBonus).coerceIn(2, 6)
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
    val hitRollNeeded: Int,
    val woundRollNeeded: Int,
    val saveRollNeeded: Int,
    val usingInvuln: Boolean,
    val isMelee: Boolean,
    val weaponStrength: Int,
    val weaponAp: Int,
    val targetToughness: Int,
    val expectedTotalDamage: Int,
    val attacksCount: Int,
    val coverTag: CoverTag,
    val weaponMode: WeaponMode,
    val inRange: Boolean,
    val hasLos: Boolean
)

@Immutable
data class ChargePreview(
    val targetUid: String,
    val targetName: String,
    val distanceTiles: Float,
    val distanceCm: Float,
    val requiredRoll: Int,
    val destinationX: Float,
    val destinationY: Float,
    val path: List<WorldPos>
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
