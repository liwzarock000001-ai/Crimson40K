package com.example.model

import androidx.compose.runtime.Immutable

enum class Faction(val displayName: String, val badgeTag: String) {
    MARINES("Ultramarines", "XIII: ULTRAMARINES"),
    BLACK_LEGION("Black Legion", "LEGION: BLACK LEGION"),
    XENOS("Tyranids", "HIVE FLEET: TYRANIDS")
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
    BONESWORDS("Claws & Talons (Melee 2cm)"),
    FLESHBORER("Deathspitter (Ranged Rng 5)")
}

enum class TerminatorWeaponChoice(val label: String) {
    THUNDER_HAMMER_SHIELD("Thunder Hammer & Storm Shield (Melee 2cm)"),
    LIGHTNING_CLAWS("Twin Lightning Claws (Melee 2cm)")
}

enum class UnitArchetype {
    INTERCESSOR,
    INCURSOR,
    HELLBLASTER,
    ASSAULT_MARINE,
    BLADEGUARD,
    TERMINATOR,
    DREADNOUGHT,
    BRUTALIS_DREADNOUGHT,
    CAPTAIN,
    LEGIONARY,
    TERMAGANT,
    HORMAGAUNT,
    WARRIOR,
    GENESTEALER,
    VON_RYANS_LEAPER,
    CARNIFEX,
    HIVE_TYRANT,
    BIOVORE,
    SPORE_MINE
}

@Immutable
data class WeaponProfile(
    val name: String,
    val range: Int,
    val attacksPerModel: Int,
    val skill: Int,
    val strength: Int,
    val armorPen: Int,
    val damage: Int,
    val sustainedHits: Int = 0,
    val assault: Boolean = false,
    val pistol: Boolean = false,
    val melta: Int = 0,
    val twinLinked: Boolean = false,
    val rapidFire: Int = 0,
    val antiFly: Int = 0,
    val cleave: Int = 0,
    val sustainedNonMonsterVehicle: Int = 0
)

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
    val leadership: Int = 6,
    val objectiveControl: Int = 1,
    val baseDiameterMm: Int = 32,
    val isFly: Boolean = false,
    val skillName: String,
    val skillDesc: String,
    val skillCooldown: Int,
    val passiveName: String,
    val passiveDesc: String,
    val weaponChoice: WarriorWeaponChoice? = null,
    val terminatorWeapon: TerminatorWeaponChoice? = null,
    val mainWeaponName: String = "Primary Weapon",
    val mainWeaponAssault: Boolean = false,
    val mainWeaponPistol: Boolean = false,
    val mainSustainedHits: Int = 0,
    val mainMelta: Int = 0,
    val mainTwinLinked: Boolean = false,
    val mainRapidFire: Int = 0,
    val mainAntiFly: Int = 0,
    val mainCleave: Int = 0,
    val mainSustainedNonMonsterVehicle: Int = 0,
    val meleeWeaponName: String = "Close Combat Weapon",
    val meleeAttacks: Int = 0,
    val meleeWeaponSkill: Int = 0,
    val meleeStrength: Int = 4,
    val meleeArmorPen: Int = 0,
    val meleeDamage: Int = 1,
    val meleeSustainedHits: Int = 0,
    val meleeTwinLinked: Boolean = false,
    val pistolWeaponName: String = "Bolt Pistol",
    val pistolAttacks: Int = 1,
    val pistolDamage: Int = 0,
    val pistolStrength: Int = 4,
    val pistolArmorPen: Int = 0,
    val pistolRange: Int = 3,
    val pistolCooldown: Int = 1,
    val pistolSustainedHits: Int = 0,
    val pistolIsPistol: Boolean = true,
    val pistolMelta: Int = 0,
    val pistolTwinLinked: Boolean = false,
    val pistolRapidFire: Int = 0,
    val pistolAntiFly: Int = 0,
    val auxRangedProfile: WeaponProfile? = null,
    val warriorMeleeProfile: WeaponProfile? = null,
    val warriorRangedProfile: WeaponProfile? = null,
    val terminatorHammerProfile: WeaponProfile? = null,
    val terminatorClawsProfile: WeaponProfile? = null
) {
    val maxTotalWounds: Int get() = squadSize * woundsPerModel
    val hasPistol: Boolean get() = pistolDamage > 0 && pistolRange > 0
    val hasAuxRanged: Boolean get() = auxRangedProfile != null && auxRangedProfile.damage > 0 && auxRangedProfile.range > 1
    val isRangedMain: Boolean get() = range > 1
    val effectiveMeleeWeaponName: String
        get() = if (isRangedMain) meleeWeaponName.ifBlank { "Close Combat Weapon" } else mainWeaponName
    val effectiveMeleeAttacks: Int
        get() = if (isRangedMain && meleeAttacks > 0) meleeAttacks else attacksPerModel
    val effectiveMeleeWeaponSkill: Int
        get() = if (isRangedMain && meleeWeaponSkill in 2..6) meleeWeaponSkill else weaponSkill
    val effectiveMeleeStrength: Int
        get() = if (isRangedMain) meleeStrength else strength
    val effectiveMeleeArmorPen: Int
        get() = if (isRangedMain) meleeArmorPen else armorPen
    val effectiveMeleeDamage: Int
        get() = if (isRangedMain) meleeDamage.coerceAtLeast(1) else damage
    val effectiveMeleeSustainedHits: Int
        get() = if (isRangedMain) meleeSustainedHits.coerceAtLeast(0) else mainSustainedHits.coerceAtLeast(0)
    val effectiveMeleeTwinLinked: Boolean
        get() = if (isRangedMain) meleeTwinLinked else mainTwinLinked
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
    val stealthTurnsLeft: Int = 0,
    val auraBuffed: Boolean = false,
    val kills: Int = 0,
    val damageDealt: Int = 0,
    val ownerPlayerId: String = "PLAYER",
    val ownerPlayerName: String = "",
    val markedByTeam: Int? = null,
    val meleeChargeBuffActive: Boolean = false,
    val bladeguardRerollBuffActive: Boolean = false,
    val legionaryWoundRerollBuffActive: Boolean = false,
    val brutalisChargeBuffActive: Boolean = false,
    val brutalisAttackStacks: Int = 0,
    val brutalisStackEarnedThisTurn: Boolean = false
) {
    val pos: GridPos get() = GridPos(x, y)
    val worldPos: WorldPos get() = WorldPos(worldX, worldY)
    val isAlive: Boolean get() = totalWounds > 0
    val isCloaked: Boolean get() = stealthTurnsLeft > 0
    val isMarked: Boolean get() = markedByTeam != null
    val effectiveTeam: Int
        get() = when (ownerPlayerId) {
            "p1", "p2", "player" -> 0
            "p3", "p4", "AI", "ai" -> 1
            else -> if (faction == Faction.MARINES) 0 else 1
        }
    fun isEnemyOf(other: BattleUnit): Boolean = effectiveTeam != other.effectiveTeam
    fun isAllyOf(other: BattleUnit): Boolean = effectiveTeam == other.effectiveTeam
    val hasRangedProfile: Boolean
        get() = template.range > 1 ||
            template.hasPistol ||
            template.hasAuxRanged ||
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
    val effectiveMeleeWeaponSkill: Int
        get() = (template.effectiveMeleeWeaponSkill - effectiveHitBonus).coerceIn(2, 6)
    val effectiveMainAttacksPerModel: Int
        get() = template.attacksPerModel + if (template.archetype == UnitArchetype.BRUTALIS_DREADNOUGHT) brutalisAttackStacks.coerceIn(0, 4) else 0
    val effectiveMainStrength: Int
        get() = template.strength +
            (if (meleeChargeBuffActive && !template.isRangedMain) 1 else 0) +
            (if (brutalisChargeBuffActive && !template.isRangedMain) 2 else 0)
    val effectiveMainArmorPen: Int
        get() = (template.armorPen +
            (if (meleeChargeBuffActive && !template.isRangedMain) 1 else 0) +
            (if (brutalisChargeBuffActive && !template.isRangedMain) 1 else 0)).coerceAtMost(6)
}

enum class WeaponMode {
    MAIN,
    PISTOL,
    AUX_RANGED,
    SKILL
}

enum class CoverTag(val label: String) {
    OPEN("OPEN"),
    COVER("COVER")
}

enum class DarkPactChoice(val label: String) {
    NONE("NO PACT"),
    LETHAL_HITS("LETHAL HITS"),
    SUSTAINED_HITS_1("SUSTAINED 1")
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
    val weaponName: String = "",
    val sustainedHits: Int = 0,
    val lethalHits: Boolean = false,
    val rerollWoundOnes: Boolean = false,
    val twinLinked: Boolean = false,
    val meltaBonus: Int = 0,
    val rapidFireBonus: Int = 0,
    val antiFlyTriggered: Boolean = false,
    val cleaveBonus: Int = 0,
    val darkPactChoice: DarkPactChoice = DarkPactChoice.NONE,
    val markedBonusApplied: Boolean = false,
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
    val isTeleportBeacon: Boolean = false,
    val isWarpLightning: Boolean = false,
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
    val playerLosses: Int,
    val commanderLabel: String = "",
    val playerFaction: Faction = Faction.MARINES,
    val enemyFaction: Faction = Faction.XENOS,
    val playerModelsAlive: Int = marineModelsAlive,
    val enemyModelsAlive: Int = xenosModelsAlive
)

@Immutable
data class BattleSummary(
    val winner: Faction,
    val playerWon: Boolean,
    val rounds: Int,
    val playerKills: Int,
    val enemyKills: Int,
    val totalDamageDealt: Int,
    val winnerLabel: String = ""
)

enum class AppScreen {
    MAIN_MENU,
    LOAD_GAME,
    SKIRMISH_SETUP,
    SKIRMISH_BATTLE,
    COOP_LOBBY,
    COOP_BATTLE,
    SETTINGS
}

enum class CoopSlotMode(val label: String) {
    HOST("HOST"),
    LAN_CLIENT("ONLINE"),
    AI_BOT("AI BOT"),
    OFF("CLOSED")
}

enum class CoopNetworkRole {
    LOCAL_OR_HOST,
    CLIENT
}

@Immutable
data class DiscoveredCoopRoom(
    val roomCode: String,
    val hostName: String,
    val hostIp: String,
    val port: Int,
    val activePlayers: Int,
    val lastSeenMs: Long
)

@Immutable
data class CoopRosterUnit(
    val entryId: String,
    val ownerPlayerId: String,
    val templateId: String
)

@Immutable
data class CoopPlayer(
    val id: String,
    val name: String,
    val faction: Faction,
    val slotMode: CoopSlotMode,
    val ready: Boolean = true,
    val connectedClient: Boolean = false
) {
    val isHost: Boolean get() = slotMode == CoopSlotMode.HOST
    val isAi: Boolean get() = slotMode == CoopSlotMode.AI_BOT
    val isActive: Boolean get() = slotMode != CoopSlotMode.OFF
}

@Immutable
data class CoopState(
    val inRoom: Boolean = false,
    val roomCode: String = "482915",
    val hostIp: String = "127.0.0.1",
    val port: Int = 40999,
    val networkRole: CoopNetworkRole = CoopNetworkRole.LOCAL_OR_HOST,
    val serverRunning: Boolean = false,
    val clientConnected: Boolean = false,
    val hostCommanderName: String = "Commander 1",
    val joinCommanderName: String = "Commander 2",
    val joinRoomCodeInput: String = "",
    val joinIpInput: String = "",
    val localPlayerId: String = "p1",
    val selectedConfigPlayerId: String = "p1",
    val pointLimit: Int = 1000,
    val networkStatus: String = "Ready to create or join a room",
    val lobbyError: String? = null,
    val discoveredRooms: List<DiscoveredCoopRoom> = emptyList(),
    val players: List<CoopPlayer> = listOf(
        CoopPlayer("p1", "Commander 1", Faction.MARINES, CoopSlotMode.HOST, true, true),
        CoopPlayer("p2", "Commander 2", Faction.MARINES, CoopSlotMode.LAN_CLIENT, true, false),
        CoopPlayer("p3", "AI Vanguard", Faction.XENOS, CoopSlotMode.AI_BOT, true, false),
        CoopPlayer("p4", "Commander 4", Faction.XENOS, CoopSlotMode.LAN_CLIENT, true, false)
    ),
    val assignedUnits: List<CoopRosterUnit> = listOf(
        CoopRosterUnit("c1", "p1", "intercessors"),
        CoopRosterUnit("c2", "p1", "assault_intercessors"),
        CoopRosterUnit("c3", "p2", "terminators"),
        CoopRosterUnit("c4", "p2", "hellblasters"),
        CoopRosterUnit("c5", "p3", "warriors"),
        CoopRosterUnit("c6", "p3", "von_ryans_leaper"),
        CoopRosterUnit("c7", "p4", "carnifex"),
        CoopRosterUnit("c8", "p4", "termagants")
    ),
    val spawnAiEnemyIfSameFaction: Boolean = true,
    val remoteTemplates: Map<String, UnitTemplate>? = null
)

@Immutable
data class SettingsState(
    val soundVolume: Float = 0.8f,
    val sfxEnabled: Boolean = true,
    val bgmVolume: Float = 0.8f,
    val bgmEnabled: Boolean = true,
    val graphicsLevel: Int = 3, // 1 = Low, 2 = Medium, 3 = High, 4 = Ultra
    val screenBrightness: Float = 1.0f
)

@Immutable
data class DiceStepDetail(
    val phaseName: String,
    val stepLabel: String,
    val targetNeeded: Int,
    val rolled: Int,
    val passed: Int,
    val failed: Int,
    val diceValues: List<Int> = emptyList(),
    val note: String = ""
)

@Immutable
data class CombatLogEntry(
    val id: Long = 0L,
    val round: Int = 1,
    val faction: Faction = Faction.MARINES,
    val title: String,
    val subtitle: String,
    val steps: List<DiceStepDetail>,
    val resultSummary: String,
    val isCounterOrFightFirst: Boolean = false,
    val isSuccess: Boolean = true
)
