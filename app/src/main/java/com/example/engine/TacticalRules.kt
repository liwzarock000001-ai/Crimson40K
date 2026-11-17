package com.example.engine

import com.example.model.BattleUnit
import com.example.model.ChargePreview
import com.example.model.CoverTag
import com.example.model.Facing
import com.example.model.GridPos
import com.example.model.TargetPreview
import com.example.model.TerrainType
import com.example.model.UnitArchetype
import com.example.model.WeaponMode
import com.example.model.WorldPos
import com.example.model.MovePreview
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

data class MoveSearchResult(
    val costs: Map<GridPos, Int>,
    val parents: Map<GridPos, GridPos>
)

data class CombatOutcome(
    val updatedAttacker: BattleUnit,
    val updatedTargets: List<BattleUnit>,
    val hits: Int,
    val totalDamageDealt: Int,
    val killsCount: Int,
    val floatingTexts: List<Pair<GridPos, Pair<String, Long>>>,
    val isCritOrFlank: Boolean,
    val isMelee: Boolean = false
)

data class CounterAttackOutcome(
    val updatedDefender: BattleUnit,
    val updatedAttacker: BattleUnit,
    val hits: Int,
    val totalDamageDealt: Int,
    val modelsLost: Int,
    val floatingTexts: List<Pair<GridPos, Pair<String, Long>>>
)

object TacticalRules {
    private const val W = MapGenerator.GRID_W
    private const val H = MapGenerator.GRID_H
    private const val TOTAL_CELLS = W * H
    private val DX = intArrayOf(0, 1, 0, -1)
    private val DY = intArrayOf(-1, 0, 1, 0)

    const val CM_PER_TILE = 3.0f

    fun isWorldWalkable(
        wx: Float,
        wy: Float,
        movingUnitUid: String,
        allUnits: List<BattleUnit>,
        map: Array<Array<TerrainType>>
    ): Boolean {
        if (wx < 0f || wx > (W - 1).toFloat() || wy < 0f || wy > (H - 1).toFloat()) return false
        val gx = wx.roundToInt().coerceIn(0, W - 1)
        val gy = wy.roundToInt().coerceIn(0, H - 1)
        if (!MapGenerator.isWalkable(map[gy][gx])) return false

        // Keep unit base clear of nearby solid rock/tree obstacles
        for (oy in -1..1) {
            for (ox in -1..1) {
                val tx = gx + ox
                val ty = gy + oy
                if (tx in 0 until W && ty in 0 until H && !MapGenerator.isWalkable(map[ty][tx])) {
                    if (abs(wx - tx) < 0.66f && abs(wy - ty) < 0.66f) return false
                }
            }
        }

        // Prevent overlapping other alive squads
        for (i in allUnits.indices) {
            val u = allUnits[i]
            if (u.isAlive && u.uid != movingUnitUid) {
                if (hypot(wx - u.worldX, wy - u.worldY) < 0.78f) return false
            }
        }
        return true
    }

    fun canWalkStraightCm(
        fromX: Float,
        fromY: Float,
        toX: Float,
        toY: Float,
        movingUnitUid: String,
        allUnits: List<BattleUnit>,
        map: Array<Array<TerrainType>>
    ): Boolean {
        val dist = hypot(toX - fromX, toY - fromY)
        val steps = (dist / 0.18f).toInt().coerceAtLeast(1)
        for (s in 1..steps) {
            val t = s / steps.toFloat()
            val px = fromX + (toX - fromX) * t
            val py = fromY + (toY - fromY) * t
            if (!isWorldWalkable(px, py, movingUnitUid, allUnits, map)) return false
        }
        return true
    }

    fun computeCmMovePreview(
        unit: BattleUnit,
        destX: Float,
        destY: Float,
        allUnits: List<BattleUnit>,
        map: Array<Array<TerrainType>>
    ): MovePreview? {
        if (unit.moveCmLeft < 0.15f) return null
        val tx = destX.coerceIn(0f, (W - 1).toFloat())
        val ty = destY.coerceIn(0f, (H - 1).toFloat())
        if (!isWorldWalkable(tx, ty, unit.uid, allUnits, map)) return null
        val straightDistTiles = hypot(tx - unit.worldX, ty - unit.worldY)
        if (straightDistTiles < 0.05f) return null

        if (canWalkStraightCm(unit.worldX, unit.worldY, tx, ty, unit.uid, allUnits, map)) {
            val distCm = straightDistTiles * CM_PER_TILE
            if (distCm <= unit.moveCmLeft + 0.05f) {
                return MovePreview(tx, ty, distCm, listOf(WorldPos(tx, ty)))
            }
            return null
        }

        // Obstacle avoidance via BFS grid waypoints + final continuous endpoint
        val destTile = GridPos(tx.roundToInt().coerceIn(0, W - 1), ty.roundToInt().coerceIn(0, H - 1))
        val search = computeReachableTiles(unit, map, allUnits, allowExtraBuffer = true)
        val tilePath = reconstructPath(destTile, unit.pos, search.parents)
        if (tilePath.isEmpty() && destTile != unit.pos) return null

        val worldPath = ArrayList<WorldPos>(tilePath.size + 1)
        var prevX = unit.worldX
        var prevY = unit.worldY
        var totalTiles = 0f
        for (i in tilePath.indices) {
            val p = tilePath[i]
            val wx = if (i == tilePath.lastIndex) tx else p.x.toFloat()
            val wy = if (i == tilePath.lastIndex) ty else p.y.toFloat()
            totalTiles += hypot(wx - prevX, wy - prevY)
            worldPath.add(WorldPos(wx, wy))
            prevX = wx
            prevY = wy
        }
        if (worldPath.isEmpty()) {
            totalTiles = straightDistTiles
            worldPath.add(WorldPos(tx, ty))
        }
        val distCm = totalTiles * CM_PER_TILE
        return if (distCm <= unit.moveCmLeft + 0.05f) {
            MovePreview(tx, ty, distCm, worldPath)
        } else null
    }

    fun computeForwardCmPreview(
        unit: BattleUnit,
        requestedCm: Float,
        allUnits: List<BattleUnit>,
        map: Array<Array<TerrainType>>
    ): MovePreview? {
        val targetCm = minOf(requestedCm, unit.moveCmLeft)
        if (targetCm < 0.15f) return null
        val rad = Math.toRadians(unit.facingDeg.toDouble())
        val dirX = cos(rad).toFloat()
        val dirY = -sin(rad).toFloat()

        val stepCm = 0.25f
        val maxSteps = (targetCm / stepCm).toInt().coerceAtLeast(1)
        var lastValidX = unit.worldX
        var lastValidY = unit.worldY
        var movedCm = 0f

        for (s in 1..maxSteps) {
            val tryCm = minOf(targetCm, s * stepCm)
            val tryTiles = tryCm / CM_PER_TILE
            val nx = unit.worldX + dirX * tryTiles
            val ny = unit.worldY + dirY * tryTiles
            if (!isWorldWalkable(nx, ny, unit.uid, allUnits, map)) break
            lastValidX = nx
            lastValidY = ny
            movedCm = tryCm
        }
        if (movedCm < 0.15f) return null
        return MovePreview(lastValidX, lastValidY, movedCm, listOf(WorldPos(lastValidX, lastValidY)))
    }

    /**
     * Zero-GC-overhead flat primitive BFS for weighted pathfinding on 16x14 grid.
     */
    fun computeReachableTiles(
        unit: BattleUnit,
        map: Array<Array<TerrainType>>,
        units: List<BattleUnit>,
        allowExtraBuffer: Boolean = false,
        maxMpOverride: Int? = null
    ): MoveSearchResult {
        val maxMp = maxMpOverride ?: ((unit.moveCmLeft / CM_PER_TILE).toInt() + if (allowExtraBuffer) 2 else 0)
        if (maxMp <= 0) return MoveSearchResult(emptyMap(), emptyMap())

        val occupied = BooleanArray(TOTAL_CELLS)
        for (i in units.indices) {
            val u = units[i]
            if (u.isAlive && u.uid != unit.uid) {
                if (u.x in 0 until W && u.y in 0 until H) {
                    occupied[u.y * W + u.x] = true
                }
            }
        }

        val dist = IntArray(TOTAL_CELLS) { Int.MAX_VALUE }
        val parentIdx = IntArray(TOTAL_CELLS) { -1 }
        val queue = IntArray(TOTAL_CELLS * 2)
        var head = 0
        var tail = 0

        val startIdx = unit.y * W + unit.x
        dist[startIdx] = 0
        queue[tail++] = startIdx

        while (head < tail) {
            val currIdx = queue[head++]
            val cx = currIdx % W
            val cy = currIdx / W
            val currCost = dist[currIdx]

            for (dir in 0 until 4) {
                val nx = cx + DX[dir]
                val ny = cy + DY[dir]
                if (nx !in 0 until W || ny !in 0 until H) continue
                val nIdx = ny * W + nx
                if (occupied[nIdx]) continue
                val terrain = map[ny][nx]
                if (!MapGenerator.isWalkable(terrain)) continue

                val nextCost = currCost + MapGenerator.moveCost(terrain)
                if (nextCost <= maxMp && nextCost < dist[nIdx]) {
                    dist[nIdx] = nextCost
                    parentIdx[nIdx] = currIdx
                    if (tail < queue.size) {
                        queue[tail++] = nIdx
                    }
                }
            }
        }

        val costs = HashMap<GridPos, Int>(48)
        val parents = HashMap<GridPos, GridPos>(48)
        for (idx in 0 until TOTAL_CELLS) {
            val d = dist[idx]
            if (idx != startIdx && d <= maxMp) {
                val pos = GridPos(idx % W, idx / W)
                costs[pos] = d
                val pIdx = parentIdx[idx]
                if (pIdx >= 0) {
                    parents[pos] = GridPos(pIdx % W, pIdx / W)
                }
            }
        }
        return MoveSearchResult(costs, parents)
    }

    fun reconstructPath(target: GridPos, start: GridPos, parents: Map<GridPos, GridPos>): List<GridPos> {
        val path = mutableListOf<GridPos>()
        var curr: GridPos? = target
        while (curr != null && curr != start) {
            path.add(curr)
            curr = parents[curr]
        }
        return path.reversed()
    }

    fun hasLineOfSight(
        fromX: Int,
        fromY: Int,
        toX: Int,
        toY: Int,
        map: Array<Array<TerrainType>>
    ): Boolean {
        val dx = abs(toX - fromX)
        val dy = abs(toY - fromY)
        val sx = if (fromX < toX) 1 else -1
        val sy = if (fromY < toY) 1 else -1
        var err = dx - dy
        var cx = fromX
        var cy = fromY

        while (true) {
            if (cx == toX && cy == toY) return true
            if (!(cx == fromX && cy == fromY)) {
                if (MapGenerator.blocksLos(map[cy][cx])) return false
            }
            val e2 = 2 * err
            if (e2 > -dy) {
                err -= dy
                cx += sx
            }
            if (e2 < dx) {
                err += dx
                cy += sy
            }
        }
    }

    fun hasLineOfSight(from: GridPos, to: GridPos, map: Array<Array<TerrainType>>): Boolean =
        hasLineOfSight(from.x, from.y, to.x, to.y, map)

    fun hasCoverBetween(
        atkX: Int,
        atkY: Int,
        defX: Int,
        defY: Int,
        map: Array<Array<TerrainType>>
    ): Boolean {
        val defTerrain = map[defY][defX]
        if (defTerrain == TerrainType.CRATER || defTerrain == TerrainType.BARRICADE || defTerrain == TerrainType.OBJECTIVE) {
            return true
        }
        val dirX = (atkX - defX).coerceIn(-1, 1)
        val dirY = (atkY - defY).coerceIn(-1, 1)
        val cx1 = defX + dirX
        if (cx1 in 0 until W && !(cx1 == atkX && defY == atkY)) {
            val t = map[defY][cx1]
            if (t == TerrainType.BARRICADE || t == TerrainType.RUIN_WALL) return true
        }
        val cy2 = defY + dirY
        if (cy2 in 0 until H && !(defX == atkX && cy2 == atkY)) {
            val t = map[cy2][defX]
            if (t == TerrainType.BARRICADE || t == TerrainType.RUIN_WALL) return true
        }
        return false
    }

    fun evaluateCoverTag(
        atkX: Float,
        atkY: Float,
        defender: BattleUnit,
        map: Array<Array<TerrainType>>,
        ignoreCover: Boolean = false
    ): CoverTag {
        val vx = atkX - defender.worldX
        val vy = atkY - defender.worldY
        val dist = hypot(vx, vy)
        val rad = Math.toRadians(defender.facingDeg.toDouble())
        val fdx = cos(rad).toFloat()
        val fdy = -sin(rad).toFloat()
        val cosAngle = if (dist > 0.01f) (vx * fdx + vy * fdy) / dist else 1f

        val gx = atkX.roundToInt().coerceIn(0, W - 1)
        val gy = atkY.roundToInt().coerceIn(0, H - 1)
        return when {
            cosAngle < -0.35f -> CoverTag.REAR
            cosAngle < 0.35f && dist > 0.05f -> CoverTag.FLANK
            !ignoreCover && hasCoverBetween(gx, gy, defender.x, defender.y, map) -> CoverTag.COVER
            else -> CoverTag.OPEN
        }
    }

    fun evaluateCoverTag(
        atkX: Int,
        atkY: Int,
        defender: BattleUnit,
        map: Array<Array<TerrainType>>,
        ignoreCover: Boolean = false
    ): CoverTag = evaluateCoverTag(atkX.toFloat(), atkY.toFloat(), defender, map, ignoreCover)

    fun evaluateCoverTag(
        attacker: BattleUnit,
        defender: BattleUnit,
        map: Array<Array<TerrainType>>,
        ignoreCover: Boolean = false
    ): CoverTag = evaluateCoverTag(attacker.worldX, attacker.worldY, defender, map, ignoreCover)

    fun weaponRange(attacker: BattleUnit, mode: WeaponMode): Int = when (mode) {
        WeaponMode.MAIN -> attacker.template.range
        WeaponMode.PISTOL -> attacker.template.pistolRange
        WeaponMode.SKILL -> when (attacker.template.archetype) {
            UnitArchetype.CARNIFEX -> 4
            UnitArchetype.HIVE_TYRANT -> 4
            UnitArchetype.DREADNOUGHT -> 6
            else -> attacker.template.range
        }
    }

    fun modeUsesMelee(attacker: BattleUnit, mode: WeaponMode): Boolean =
        weaponRange(attacker, mode) <= 1 || (mode == WeaponMode.MAIN && attacker.hasCharged)

    fun effectiveIsMelee(attacker: BattleUnit, mode: WeaponMode, distTiles: Float): Boolean =
        weaponRange(attacker, mode) <= 1 ||
            (mode == WeaponMode.MAIN && attacker.hasCharged && distTiles <= 1.55f) ||
            (mode == WeaponMode.MAIN && distTiles <= 1.55f && (!attacker.template.mainWeaponPistol || attacker.hasShot)) ||
            (mode == WeaponMode.SKILL && modeUsesMelee(attacker, mode))

    fun isEngagedInMelee(unit: BattleUnit, allUnits: List<BattleUnit>?): Boolean {
        if (!unit.isAlive || allUnits == null) return false
        return allUnits.any { other ->
            other.isAlive && other.faction != unit.faction &&
                hypot(unit.worldX - other.worldX, unit.worldY - other.worldY) <= 1.55f
        }
    }

    fun isEngagedInMeleeAt(unit: BattleUnit, atX: Float, atY: Float, allUnits: List<BattleUnit>?): Boolean {
        if (!unit.isAlive || allUnits == null) return false
        return allUnits.any { other ->
            other.isAlive && other.faction != unit.faction &&
                hypot(atX - other.worldX, atY - other.worldY) <= 1.55f
        }
    }

    fun canUseMode(
        attacker: BattleUnit,
        mode: WeaponMode,
        allUnits: List<BattleUnit>? = null
    ): Boolean {
        val inMelee = isEngagedInMelee(attacker, allUnits)
        return when (mode) {
            WeaponMode.MAIN -> {
                val rangedMain = attacker.template.range > 1
                when {
                    attacker.hasCharged -> !attacker.hasFought
                    attacker.hasAdvanced -> rangedMain && attacker.template.mainWeaponAssault && !attacker.hasShot && (!inMelee || attacker.template.mainWeaponPistol)
                    inMelee -> if (attacker.template.mainWeaponPistol && !attacker.hasShot) true else !attacker.hasFought
                    else -> if (rangedMain) !attacker.hasShot else !attacker.hasFought
                }
            }
            WeaponMode.PISTOL ->
                attacker.template.hasPistol &&
                    attacker.pistolCooldownLeft == 0 &&
                    !attacker.hasShot &&
                    !attacker.hasAdvanced &&
                    !attacker.hasCharged
            WeaponMode.SKILL -> when (attacker.template.archetype) {
                UnitArchetype.TERMINATOR -> attacker.skillCooldownLeft == 0 && !attacker.hasAdvanced && !attacker.hasCharged && !inMelee
                UnitArchetype.HORMAGAUNT -> attacker.skillCooldownLeft == 0 && !attacker.hasCharged
                else -> {
                    val isMeleeSkill = weaponRange(attacker, mode) <= 1
                    if (isMeleeSkill) {
                        attacker.skillCooldownLeft == 0 && !attacker.hasFought && !attacker.hasAdvanced
                    } else {
                        if (inMelee && !attacker.template.mainWeaponPistol) false else {
                            attacker.skillCooldownLeft == 0 &&
                                !attacker.hasShot &&
                                !attacker.hasCharged &&
                                (!attacker.hasAdvanced || attacker.template.mainWeaponAssault)
                        }
                    }
                }
            }
        }
    }

    fun rollAdvanceBonus(rng: Random = Random.Default): Pair<Int, Float> {
        val roll = rng.nextInt(1, 7)
        return roll to (roll * CM_PER_TILE)
    }

    fun findChargeEngagementSpot(
        charger: BattleUnit,
        target: BattleUnit,
        allUnits: List<BattleUnit>,
        map: Array<Array<TerrainType>>
    ): ChargePreview? {
        if (!charger.canCharge || !target.isAlive || charger.faction == target.faction) return null
        val curDist = hypot(charger.worldX - target.worldX, charger.worldY - target.worldY)
        if (curDist <= 1.55f || curDist > 12.45f) return null

        // Check direct straight-line charge point at 1.05 tiles from target
        val dirX = (charger.worldX - target.worldX) / curDist
        val dirY = (charger.worldY - target.worldY) / curDist
        val directX = (target.worldX + dirX * 1.05f).coerceIn(0f, (W - 1).toFloat())
        val directY = (target.worldY + dirY * 1.05f).coerceIn(0f, (H - 1).toFloat())
        if (isWorldWalkable(directX, directY, charger.uid, allUnits, map) &&
            canWalkStraightCm(charger.worldX, charger.worldY, directX, directY, charger.uid, allUnits, map)
        ) {
            val dTiles = hypot(directX - charger.worldX, directY - charger.worldY)
            if (dTiles <= 12.0f) {
                val dCm = dTiles * CM_PER_TILE
                val reqRoll = ceil(dTiles.toDouble()).toInt().coerceIn(2, 12)
                return ChargePreview(
                    targetUid = target.uid,
                    targetName = target.template.name,
                    distanceTiles = dTiles,
                    distanceCm = dCm,
                    requiredRoll = reqRoll,
                    destinationX = directX,
                    destinationY = directY,
                    path = listOf(WorldPos(directX, directY))
                )
            }
        }

        // Fallback BFS around obstacles to find closest walkable engagement tile adjacent to target
        val search = computeReachableTiles(charger, map, allUnits, maxMpOverride = 12)
        var bestTile: GridPos? = null
        var bestWorldDist = Float.MAX_VALUE
        for (dy in -1..1) {
            for (dx in -1..1) {
                if (dx == 0 && dy == 0) continue
                val tx = target.x + dx
                val ty = target.y + dy
                if (tx !in 0 until W || ty !in 0 until H) continue
                val gp = GridPos(tx, ty)
                if (gp in search.costs && isWorldWalkable(tx.toFloat(), ty.toFloat(), charger.uid, allUnits, map)) {
                    val path = reconstructPath(gp, charger.pos, search.parents)
                    var pathLen = 0f
                    var px = charger.worldX
                    var py = charger.worldY
                    for (step in path) {
                        pathLen += hypot(step.x - px, step.y - py)
                        px = step.x.toFloat()
                        py = step.y.toFloat()
                    }
                    if (pathLen <= 12.0f && pathLen < bestWorldDist) {
                        bestWorldDist = pathLen
                        bestTile = gp
                    }
                }
            }
        }
        val dest = bestTile ?: return null
        val tilePath = reconstructPath(dest, charger.pos, search.parents)
        val worldPath = tilePath.map { WorldPos(it.x.toFloat(), it.y.toFloat()) }
        val reqRoll = ceil(bestWorldDist.toDouble()).toInt().coerceIn(2, 12)
        return ChargePreview(
            targetUid = target.uid,
            targetName = target.template.name,
            distanceTiles = bestWorldDist,
            distanceCm = bestWorldDist * CM_PER_TILE,
            requiredRoll = reqRoll,
            destinationX = dest.x.toFloat(),
            destinationY = dest.y.toFloat(),
            path = worldPath
        )
    }

    fun computeChargeableTargets(
        charger: BattleUnit,
        allUnits: List<BattleUnit>,
        map: Array<Array<TerrainType>>
    ): Set<String> {
        if (!charger.canCharge) return emptySet()
        val result = HashSet<String>(8)
        for (i in allUnits.indices) {
            val enemy = allUnits[i]
            if (enemy.isAlive && enemy.faction != charger.faction) {
                if (findChargeEngagementSpot(charger, enemy, allUnits, map) != null) {
                    result.add(enemy.uid)
                }
            }
        }
        return result
    }

    fun d6SuccessProbability(needed: Int): Float = when {
        needed <= 2 -> 5f / 6f // Natural 1 always fails
        needed <= 6 -> (7 - needed) / 6f
        else -> 0f
    }

    fun woundRollNeeded(strength: Int, toughness: Int): Int = when {
        strength >= toughness * 2 -> 2
        strength > toughness -> 3
        strength == toughness -> 4
        strength * 2 <= toughness -> 6
        else -> 5
    }

    fun weaponStrength(attacker: BattleUnit, mode: WeaponMode, isMeleeOverride: Boolean = false): Int {
        if (isMeleeOverride && attacker.template.range > 1 &&
            attacker.template.archetype != UnitArchetype.TERMINATOR &&
            attacker.template.archetype != UnitArchetype.DREADNOUGHT
        ) {
            return minOf(attacker.template.strength, 4)
        }
        return when (mode) {
            WeaponMode.MAIN -> attacker.template.strength
            WeaponMode.PISTOL -> attacker.template.pistolStrength
            WeaponMode.SKILL -> when (attacker.template.archetype) {
                UnitArchetype.HELLBLASTER -> attacker.template.strength + 1
                UnitArchetype.CARNIFEX -> 8
                UnitArchetype.HIVE_TYRANT -> 9
                else -> attacker.template.strength
            }
        }
    }

    fun weaponAp(attacker: BattleUnit, mode: WeaponMode, isMeleeOverride: Boolean = false): Int {
        if (isMeleeOverride && attacker.template.range > 1 &&
            attacker.template.archetype != UnitArchetype.TERMINATOR &&
            attacker.template.archetype != UnitArchetype.DREADNOUGHT
        ) {
            return 0
        }
        return when (mode) {
            WeaponMode.MAIN -> attacker.template.armorPen
            WeaponMode.PISTOL -> attacker.template.pistolArmorPen
            WeaponMode.SKILL -> when (attacker.template.archetype) {
                UnitArchetype.HELLBLASTER -> (attacker.template.armorPen + 1).coerceAtMost(5)
                UnitArchetype.CARNIFEX -> 3
                UnitArchetype.HIVE_TYRANT -> 3
                else -> attacker.template.armorPen
            }
        }
    }

    fun computeHitRollNeeded(
        attacker: BattleUnit,
        mode: WeaponMode,
        coverTag: CoverTag,
        isMelee: Boolean
    ): Int {
        var needed = if (isMelee) attacker.effectiveWeaponSkill else attacker.effectiveBallisticSkill
        if (!isMelee && attacker.template.archetype == UnitArchetype.INTERCESSOR && attacker.moveCmLeft >= attacker.template.moveCm - 0.1f) {
            needed -= 1
        }
        if (mode == WeaponMode.SKILL &&
            (attacker.template.archetype == UnitArchetype.INTERCESSOR ||
                attacker.template.archetype == UnitArchetype.WARRIOR)
        ) {
            needed -= 1
        }
        if (coverTag == CoverTag.REAR || coverTag == CoverTag.FLANK) {
            needed -= 1
        }
        return needed.coerceIn(2, 6)
    }

    fun computeWoundRollNeeded(
        attacker: BattleUnit,
        defender: BattleUnit,
        mode: WeaponMode,
        coverTag: CoverTag,
        isMeleeOverride: Boolean = false
    ): Int {
        val s = weaponStrength(attacker, mode, isMeleeOverride)
        var needed = woundRollNeeded(s, defender.template.toughness)
        if (coverTag == CoverTag.REAR) {
            needed -= 1
        }
        return needed.coerceIn(2, 6)
    }

    fun computeEffectiveSave(
        defender: BattleUnit,
        ap: Int,
        coverTag: CoverTag
    ): Pair<Int, Boolean> {
        var modifiedArmorSv = defender.template.save + ap
        if (coverTag == CoverTag.COVER) {
            val isTermagant = defender.template.archetype == UnitArchetype.TERMAGANT
            if (ap > 0 || defender.template.save > 3 || isTermagant) {
                val coverBonus = if (isTermagant) 2 else 1
                modifiedArmorSv = (modifiedArmorSv - coverBonus).coerceAtLeast(2)
            }
        }
        val armorClamped = modifiedArmorSv.coerceIn(2, 7)
        val inv = defender.template.invulnSave
        return if (inv != null && inv in 2..6 && inv < armorClamped) {
            inv to true
        } else {
            armorClamped to false
        }
    }

    fun canTargetFromWorld(
        attacker: BattleUnit,
        atkX: Float,
        atkY: Float,
        defender: BattleUnit,
        mode: WeaponMode,
        map: Array<Array<TerrainType>>,
        allUnits: List<BattleUnit>? = null
    ): TargetPreview? {
        if (!attacker.isAlive || !defender.isAlive || attacker.faction == defender.faction) return null
        if (mode == WeaponMode.SKILL && attacker.template.archetype == UnitArchetype.TERMINATOR) return null
        if (!canUseMode(attacker, mode, allUnits)) return null

        val distTiles = hypot(atkX - defender.worldX, atkY - defender.worldY)
        val isMelee = effectiveIsMelee(attacker, mode, distTiles)
        if (attacker.hasCharged && !isMelee) return null

        val isAttackerEngaged = isEngagedInMeleeAt(attacker, atkX, atkY, allUnits)
        val hasPistolPrivilege = mode == WeaponMode.PISTOL || attacker.template.mainWeaponPistol
        // Rule: If in close combat range, cannot use shooting weapons unless it is a pistol / has Pistol tag
        if (isAttackerEngaged && !isMelee && !hasPistolPrivilege) return null

        val range = if (isMelee) 1 else weaponRange(attacker, mode)
        val maxReachTiles = if (isMelee) 1.55f else range.toFloat() + 0.45f
        val inRange = distTiles <= maxReachTiles
        val needsLos = range > 1 && attacker.template.archetype != UnitArchetype.HIVE_TYRANT
        val los = if (!inRange) false else if (!needsLos) true else hasLineOfSight(
            atkX.roundToInt().coerceIn(0, W - 1),
            atkY.roundToInt().coerceIn(0, H - 1),
            defender.x,
            defender.y,
            map
        )

        val ignoreCover = mode == WeaponMode.SKILL && attacker.template.archetype == UnitArchetype.GENESTEALER
        val coverTag = evaluateCoverTag(atkX, atkY, defender, map, ignoreCover)

        val hitRollNeeded = computeHitRollNeeded(attacker, mode, coverTag, isMelee)
        val woundRollNeeded = computeWoundRollNeeded(attacker, defender, mode, coverTag, isMelee)
        val s = weaponStrength(attacker, mode, isMelee)
        val ap = weaponAp(attacker, mode, isMelee)
        val (saveRollNeeded, usingInvuln) = computeEffectiveSave(defender, ap, coverTag)

        val hitProb = d6SuccessProbability(hitRollNeeded)
        val woundProb = d6SuccessProbability(woundRollNeeded)
        val failSaveProb = 1f - d6SuccessProbability(saveRollNeeded)

        val perUnsavedDmg = damagePerUnsavedWound(attacker, defender, mode, isMelee)
        val effectiveWoundsPerHit = minOf(perUnsavedDmg.toFloat(), defender.template.woundsPerModel.toFloat())
        val attacks = attacksCount(attacker, mode)
        val expectedTotal = (attacks * hitProb * woundProb * failSaveProb * effectiveWoundsPerHit)
            .roundToInt()
            .coerceAtLeast(if (failSaveProb > 0f) 1 else 0)

        return TargetPreview(
            targetUid = defender.uid,
            targetName = defender.template.name,
            hitChance = (hitProb * 100f).roundToInt(),
            hitRollNeeded = hitRollNeeded,
            woundRollNeeded = woundRollNeeded,
            saveRollNeeded = saveRollNeeded,
            usingInvuln = usingInvuln,
            isMelee = isMelee,
            weaponStrength = s,
            weaponAp = ap,
            targetToughness = defender.template.toughness,
            expectedTotalDamage = expectedTotal,
            attacksCount = attacks,
            coverTag = coverTag,
            weaponMode = mode,
            inRange = inRange,
            hasLos = los
        )
    }

    fun canTargetFrom(
        attacker: BattleUnit,
        atkX: Int,
        atkY: Int,
        defender: BattleUnit,
        mode: WeaponMode,
        map: Array<Array<TerrainType>>,
        allUnits: List<BattleUnit>? = null
    ): TargetPreview? = canTargetFromWorld(attacker, atkX.toFloat(), atkY.toFloat(), defender, mode, map, allUnits)

    fun canTarget(
        attacker: BattleUnit,
        defender: BattleUnit,
        mode: WeaponMode,
        map: Array<Array<TerrainType>>,
        allUnits: List<BattleUnit>? = null
    ): TargetPreview? = canTargetFromWorld(attacker, attacker.worldX, attacker.worldY, defender, mode, map, allUnits)

    fun attacksCount(attacker: BattleUnit, mode: WeaponMode): Int {
        val base = when (mode) {
            WeaponMode.MAIN -> attacker.aliveModels * attacker.template.attacksPerModel
            WeaponMode.PISTOL -> attacker.aliveModels * attacker.template.pistolAttacks
            WeaponMode.SKILL -> when (attacker.template.archetype) {
                UnitArchetype.INTERCESSOR, UnitArchetype.TERMAGANT ->
                    attacker.aliveModels * (attacker.template.attacksPerModel + 1)
                else -> attacker.aliveModels * attacker.template.attacksPerModel
            }
        }
        return base.coerceAtLeast(1)
    }

    fun allocateWoundDamage(
        currentTotalWounds: Int,
        woundsPerModel: Int,
        rawAttackDamage: Int
    ): Int {
        if (currentTotalWounds <= 0 || rawAttackDamage <= 0) return 0
        val wPer = woundsPerModel.coerceAtLeast(1)
        val mod = currentTotalWounds % wPer
        val frontModelWounds = if (mod == 0) wPer else mod
        return minOf(rawAttackDamage, frontModelWounds)
    }

    private fun damagePerUnsavedWound(
        attacker: BattleUnit,
        defender: BattleUnit,
        mode: WeaponMode,
        isMeleeOverride: Boolean = false
    ): Int {
        var dmg = if (isMeleeOverride && attacker.template.range > 1 &&
            attacker.template.archetype != UnitArchetype.TERMINATOR &&
            attacker.template.archetype != UnitArchetype.DREADNOUGHT
        ) {
            1
        } else {
            when (mode) {
                WeaponMode.MAIN -> attacker.effectiveDamage
                WeaponMode.PISTOL -> attacker.template.pistolDamage
                WeaponMode.SKILL -> when (attacker.template.archetype) {
                    UnitArchetype.HELLBLASTER -> attacker.effectiveDamage + 1
                    UnitArchetype.CARNIFEX -> 2
                    UnitArchetype.HIVE_TYRANT -> 2
                    else -> attacker.effectiveDamage
                }
            }
        }

        if (defender.template.archetype == UnitArchetype.DREADNOUGHT) {
            dmg = (dmg - 1).coerceAtLeast(1)
        }
        if (defender.damageReductionTurns > 0) {
            dmg = (dmg / 2).coerceAtLeast(1)
        }
        return dmg.coerceAtLeast(1)
    }

    fun applyAuras(units: List<BattleUnit>): List<BattleUnit> {
        val commanders = units.filter {
            it.isAlive && (it.template.archetype == UnitArchetype.CAPTAIN ||
                it.template.archetype == UnitArchetype.WARRIOR ||
                it.template.archetype == UnitArchetype.HIVE_TYRANT)
        }
        return units.map { u ->
            if (!u.isAlive) u
            else {
                var hasAura = false
                for (i in commanders.indices) {
                    val cmd = commanders[i]
                    if (cmd.faction == u.faction && hypot(cmd.worldX - u.worldX, cmd.worldY - u.worldY) <= 3.5f) {
                        hasAura = true
                        break
                    }
                }
                if (u.auraBuffed == hasAura) u else u.copy(auraBuffed = hasAura)
            }
        }
    }

    fun angleDegToward(fromX: Float, fromY: Float, toX: Float, toY: Float): Float =
        (atan2(-(toY - fromY), toX - fromX) * 180.0 / Math.PI).toFloat()

    fun nearestFacingFromDeg(deg: Float): Facing {
        val norm = ((deg % 360f) + 360f) % 360f
        return when {
            norm >= 315f || norm < 45f -> Facing.EAST
            norm < 135f -> Facing.NORTH
            norm < 225f -> Facing.WEST
            else -> Facing.SOUTH
        }
    }

    fun facingToward(from: GridPos, to: GridPos): Facing = facingToward(from.x, from.y, to.x, to.y)

    fun facingToward(fromX: Int, fromY: Int, toX: Int, toY: Int): Facing {
        val dx = toX - fromX
        val dy = toY - fromY
        return if (abs(dx) >= abs(dy)) {
            if (dx >= 0) Facing.EAST else Facing.WEST
        } else {
            if (dy >= 0) Facing.SOUTH else Facing.NORTH
        }
    }

    fun resolveAttack(
        attacker: BattleUnit,
        primaryTarget: BattleUnit,
        allUnits: List<BattleUnit>,
        mode: WeaponMode,
        map: Array<Array<TerrainType>>,
        rng: Random = Random.Default
    ): CombatOutcome {
        val preview = canTarget(attacker, primaryTarget, mode, map, allUnits)
            ?: return CombatOutcome(attacker, emptyList(), 0, 0, 0, emptyList(), false, false)

        val targetsToHit = mutableListOf(primaryTarget)
        if (mode == WeaponMode.SKILL) {
            when (attacker.template.archetype) {
                UnitArchetype.ASSAULT_MARINE -> {
                    allUnits.filterTo(targetsToHit) {
                        it.isAlive && it.faction != attacker.faction &&
                            it.uid != primaryTarget.uid && maxOf(abs(attacker.x - it.x), abs(attacker.y - it.y)) <= 1
                    }
                }
                UnitArchetype.DREADNOUGHT, UnitArchetype.CARNIFEX, UnitArchetype.HIVE_TYRANT -> {
                    allUnits.filterTo(targetsToHit) {
                        it.isAlive && it.faction != attacker.faction &&
                            it.uid != primaryTarget.uid && maxOf(abs(primaryTarget.x - it.x), abs(primaryTarget.y - it.y)) <= 1
                    }
                }
                else -> Unit
            }
        }

        val updatedTargets = ArrayList<BattleUnit>(targetsToHit.size)
        val floats = ArrayList<Pair<GridPos, Pair<String, Long>>>(targetsToHit.size * 2)
        var totalHits = 0
        var totalDmg = 0
        var totalKills = 0
        var anyFlank = false

        targetsToHit.forEachIndexed { idx, tgt ->
            val tgtPreview = if (idx == 0) preview else canTarget(attacker, tgt, mode, map, allUnits) ?: preview
            val attacks = if (idx == 0) tgtPreview.attacksCount else (tgtPreview.attacksCount / 2).coerceAtLeast(1)
            val perUnsavedDmg = damagePerUnsavedWound(attacker, tgt, mode, tgtPreview.isMelee)

            var tgtHits = 0
            var tgtWounds = 0
            var tgtSaved = 0
            var tgtDmg = 0
            var runningWounds = tgt.totalWounds

            repeat(attacks) {
                if (runningWounds > 0) {
                    // Step 1: Hit Roll (d6 vs BS / WS; 1 always misses)
                    val hitRoll = rng.nextInt(1, 7)
                    if (hitRoll > 1 && hitRoll >= tgtPreview.hitRollNeeded) {
                        tgtHits++
                        // Step 2: Wound Roll (d6 comparing S vs T; 1 always fails)
                        val woundRoll = rng.nextInt(1, 7)
                        if (woundRoll > 1 && woundRoll >= tgtPreview.woundRollNeeded) {
                            tgtWounds++
                            // Step 3: Saving Throw (d6 vs Sv+AP or Invuln; 1 always fails; 7+ auto-fails)
                            val saveRoll = rng.nextInt(1, 7)
                            val isSaved = tgtPreview.saveRollNeeded <= 6 && saveRoll > 1 && saveRoll >= tgtPreview.saveRollNeeded
                            if (isSaved) {
                                tgtSaved++
                            } else {
                                // Step 4: Model-by-Model Wound Allocation (no excess damage spillover)
                                val applied = allocateWoundDamage(runningWounds, tgt.template.woundsPerModel, perUnsavedDmg)
                                runningWounds -= applied
                                tgtDmg += applied
                            }
                        }
                    }
                }
            }

            if (tgtPreview.coverTag == CoverTag.FLANK || tgtPreview.coverTag == CoverTag.REAR) {
                anyFlank = true
            }

            val modelsBefore = tgt.aliveModels
            val newWounds = runningWounds.coerceAtLeast(0)
            val drainedMoveCm = if (mode == WeaponMode.SKILL && attacker.template.archetype == UnitArchetype.HIVE_TYRANT && tgtHits > 0) {
                (tgt.moveCmLeft - 6f).coerceAtLeast(0f)
            } else tgt.moveCmLeft

            val updatedTgt = tgt.copy(
                totalWounds = newWounds,
                moveCmLeft = drainedMoveCm
            )
            updatedTargets.add(updatedTgt)

            val modelsLost = (modelsBefore - updatedTgt.aliveModels).coerceAtLeast(0)
            totalKills += if (newWounds == 0) 1 else 0
            totalHits += tgtHits
            totalDmg += tgtDmg

            val tagPrefix = when {
                tgtHits == 0 -> "MISS"
                tgtWounds == 0 -> "NO WOUND"
                tgtDmg == 0 && tgtSaved > 0 -> if (tgtPreview.usingInvuln) "INVULN SAVED!" else "ARMOR SAVED!"
                tgtDmg == 0 -> "MISS"
                tgtPreview.coverTag == CoverTag.REAR -> "REAR -${tgtDmg}W"
                tgtPreview.coverTag == CoverTag.FLANK -> "FLANK -${tgtDmg}W"
                else -> "-${tgtDmg}W"
            }
            val color = when {
                tgtHits == 0 || tgtWounds == 0 -> 0xFF9AA3B2L
                tgtDmg == 0 && tgtSaved > 0 -> 0xFF4CC9F0L
                tgtPreview.coverTag == CoverTag.REAR || tgtPreview.coverTag == CoverTag.FLANK -> 0xFFFFD166L
                else -> 0xFFFF4D58L
            }
            floats.add(tgt.pos to (tagPrefix to color))
            if (tgtDmg > 0 && tgtSaved > 0) {
                val saveTag = if (tgtPreview.usingInvuln) "${tgtSaved}x INV SAVED" else "${tgtSaved}x SAVED"
                floats.add(tgt.pos to (saveTag to 0xFF4CC9F0L))
            }
            if (modelsLost > 0 && newWounds > 0) {
                floats.add(tgt.pos to ("-$modelsLost MODEL" to 0xFFFF8866L))
            } else if (newWounds == 0) {
                floats.add(tgt.pos to ("ELIMINATED!" to 0xFFFF384AL))
            }
        }

        val isMelee = preview.isMelee

        val newSkillCd = if (mode == WeaponMode.SKILL) attacker.template.skillCooldown else attacker.skillCooldownLeft
        val newPistolCd = if (mode == WeaponMode.PISTOL) attacker.template.pistolCooldown else attacker.pistolCooldownLeft
        val newBuffTurns = if (mode == WeaponMode.SKILL &&
            (attacker.template.archetype == UnitArchetype.WARRIOR ||
                attacker.template.archetype == UnitArchetype.CAPTAIN)
        ) 2 else attacker.buffTurnsLeft
        val newBuffLabel = if (newBuffTurns > attacker.buffTurnsLeft) attacker.template.skillName else attacker.activeBuffLabel

        val nextFacingDeg = angleDegToward(attacker.worldX, attacker.worldY, primaryTarget.worldX, primaryTarget.worldY)
        val updatedAttacker = attacker.copy(
            facing = nearestFacingFromDeg(nextFacingDeg),
            facingDeg = nextFacingDeg,
            hasShot = attacker.hasShot || !isMelee,
            hasFought = attacker.hasFought || isMelee,
            skillCooldownLeft = newSkillCd,
            pistolCooldownLeft = newPistolCd,
            buffTurnsLeft = newBuffTurns,
            activeBuffLabel = newBuffLabel,
            kills = attacker.kills + totalKills,
            damageDealt = attacker.damageDealt + totalDmg
        )

        return CombatOutcome(
            updatedAttacker = updatedAttacker,
            updatedTargets = updatedTargets,
            hits = totalHits,
            totalDamageDealt = totalDmg,
            killsCount = totalKills,
            floatingTexts = floats,
            isCritOrFlank = anyFlank,
            isMelee = isMelee
        )
    }

    fun resolveMeleeCounterAttack(
        counterAttacker: BattleUnit,
        target: BattleUnit,
        map: Array<Array<TerrainType>>,
        rng: Random = Random.Default
    ): CounterAttackOutcome? {
        if (!counterAttacker.isAlive || counterAttacker.aliveModels <= 0) return null
        if (!target.isAlive || target.aliveModels <= 0) return null
        val dist = hypot(counterAttacker.worldX - target.worldX, counterAttacker.worldY - target.worldY)
        if (dist > 1.55f) return null

        val isNativeMelee = counterAttacker.template.range <= 1
        val ws = counterAttacker.effectiveWeaponSkill
        val s = if (isNativeMelee) counterAttacker.template.strength else weaponStrength(counterAttacker, WeaponMode.MAIN, isMeleeOverride = true)
        val ap = if (isNativeMelee) counterAttacker.template.armorPen else weaponAp(counterAttacker, WeaponMode.MAIN, isMeleeOverride = true)
        val d = if (isNativeMelee) {
            damagePerUnsavedWound(counterAttacker, target, WeaponMode.MAIN, isMeleeOverride = false)
        } else {
            damagePerUnsavedWound(counterAttacker, target, WeaponMode.MAIN, isMeleeOverride = true)
        }

        val hitRollNeeded = ws.coerceIn(2, 6)
        val woundRollNeeded = woundRollNeeded(s, target.template.toughness)
        val (saveRollNeeded, usingInvuln) = computeEffectiveSave(target, ap, CoverTag.OPEN)

        val attacks = (counterAttacker.aliveModels * counterAttacker.template.attacksPerModel).coerceAtLeast(1)

        var tgtHits = 0
        var tgtWounds = 0
        var tgtSaved = 0
        var tgtDmg = 0
        var runningWounds = target.totalWounds

        repeat(attacks) {
            if (runningWounds > 0) {
                val hitRoll = rng.nextInt(1, 7)
                if (hitRoll > 1 && hitRoll >= hitRollNeeded) {
                    tgtHits++
                    val woundRoll = rng.nextInt(1, 7)
                    if (woundRoll > 1 && woundRoll >= woundRollNeeded) {
                        tgtWounds++
                        val saveRoll = rng.nextInt(1, 7)
                        val isSaved = saveRollNeeded <= 6 && saveRoll > 1 && saveRoll >= saveRollNeeded
                        if (isSaved) {
                            tgtSaved++
                        } else {
                            val applied = allocateWoundDamage(runningWounds, target.template.woundsPerModel, d)
                            runningWounds -= applied
                            tgtDmg += applied
                        }
                    }
                }
            }
        }

        val modelsBefore = target.aliveModels
        val newWounds = runningWounds.coerceAtLeast(0)
        val updatedTarget = target.copy(
            totalWounds = newWounds
        )
        val modelsLost = (modelsBefore - updatedTarget.aliveModels).coerceAtLeast(0)

        val floats = ArrayList<Pair<GridPos, Pair<String, Long>>>(3)
        val tag = when {
            tgtHits == 0 -> "COUNTER MISS"
            tgtWounds == 0 -> "COUNTER NO WOUND"
            tgtDmg == 0 && tgtSaved > 0 -> if (usingInvuln) "COUNTER INV SAVED" else "COUNTER SAVED"
            tgtDmg == 0 -> "COUNTER MISS"
            else -> "⚔️ COUNTER -${tgtDmg}W"
        }
        val col = when {
            tgtHits == 0 || tgtWounds == 0 -> 0xFF9AA3B2L
            tgtDmg == 0 && tgtSaved > 0 -> 0xFF4CC9F0L
            else -> 0xFFFF8866L
        }
        floats.add(target.pos to (tag to col))
        if (modelsLost > 0 && newWounds > 0) {
            floats.add(target.pos to ("-$modelsLost MODEL" to 0xFFFF4D58L))
        } else if (newWounds == 0) {
            floats.add(target.pos to ("ELIMINATED!" to 0xFFFF384AL))
        }

        val updatedCounterAttacker = counterAttacker.copy(
            kills = counterAttacker.kills + (if (newWounds == 0) 1 else 0),
            damageDealt = counterAttacker.damageDealt + tgtDmg
        )

        return CounterAttackOutcome(
            updatedDefender = updatedCounterAttacker,
            updatedAttacker = updatedTarget,
            hits = tgtHits,
            totalDamageDealt = tgtDmg,
            modelsLost = modelsLost,
            floatingTexts = floats
        )
    }

    fun activateSelfSkillIfApplicable(unit: BattleUnit): BattleUnit? {
        if (unit.skillCooldownLeft > 0) return null
        return when (unit.template.archetype) {
            UnitArchetype.HORMAGAUNT -> unit.copy(
                moveCmLeft = unit.moveCmLeft + 9f,
                skillCooldownLeft = unit.template.skillCooldown,
                buffTurnsLeft = 1,
                activeBuffLabel = "LEAP +9 cm"
            )
            else -> null
        }
    }

    /**
     * Assault Terminator Teleport Strike:
     * - Warp range up to 9 tiles
     * - Must not be within 4 tiles of any alive enemy (chebyshev distance > 4)
     * - Destination must be walkable and unoccupied
     */
    fun computeTeleportTiles(
        unit: BattleUnit,
        map: Array<Array<TerrainType>>,
        allUnits: List<BattleUnit>,
        maxDist: Int = 9,
        minEnemyDist: Int = 5
    ): Set<GridPos> {
        if (!unit.isAlive || unit.skillCooldownLeft > 0) return emptySet()
        val valid = mutableSetOf<GridPos>()
        val enemies = allUnits.filter { it.isAlive && it.faction != unit.faction }
        val occupied = allUnits.filter { it.isAlive }.map { it.pos }.toSet()

        val h = map.size
        val w = map[0].size
        for (y in 0 until h) {
            for (x in 0 until w) {
                val pos = GridPos(x, y)
                if (pos == unit.pos) continue
                val dist = maxOf(abs(x - unit.x), abs(y - unit.y))
                if (dist > maxDist) continue
                if (!MapGenerator.isWalkable(map[y][x])) continue
                if (pos in occupied) continue

                var tooCloseToEnemy = false
                for (i in enemies.indices) {
                    val e = enemies[i]
                    val dToEnemy = maxOf(abs(x - e.x), abs(y - e.y))
                    if (dToEnemy < minEnemyDist) {
                        tooCloseToEnemy = true
                        break
                    }
                }
                if (!tooCloseToEnemy) {
                    valid.add(pos)
                }
            }
        }
        return valid
    }

    fun computeLinePath(x0: Int, y0: Int, x1: Int, y1: Int): List<GridPos> {
        val points = mutableListOf<GridPos>()
        var x = x0
        var y = y0
        val dx = abs(x1 - x0)
        val dy = abs(y1 - y0)
        val sx = if (x0 < x1) 1 else -1
        val sy = if (y0 < y1) 1 else -1
        var err = dx - dy
        while (true) {
            points.add(GridPos(x, y))
            if (x == x1 && y == y1) break
            val e2 = 2 * err
            if (e2 > -dy) {
                err -= dy
                x += sx
            }
            if (e2 < dx) {
                err += dx
                y += sy
            }
        }
        return points
    }
}
