package com.example.engine

import com.example.model.BattleUnit
import com.example.model.CoverTag
import com.example.model.Facing
import com.example.model.GridPos
import com.example.model.TargetPreview
import com.example.model.TerrainType
import com.example.model.UnitArchetype
import com.example.model.WeaponMode
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
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
    val isCritOrFlank: Boolean
)

object TacticalRules {
    private const val W = MapGenerator.GRID_W
    private const val H = MapGenerator.GRID_H
    private const val TOTAL_CELLS = W * H
    private val DX = intArrayOf(0, 1, 0, -1)
    private val DY = intArrayOf(-1, 0, 1, 0)

    /**
     * Zero-GC-overhead flat primitive BFS for weighted pathfinding on 16x14 grid.
     */
    fun computeReachableTiles(
        unit: BattleUnit,
        map: Array<Array<TerrainType>>,
        units: List<BattleUnit>
    ): MoveSearchResult {
        val maxMp = unit.mpLeft
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
        atkX: Int,
        atkY: Int,
        defender: BattleUnit,
        map: Array<Array<TerrainType>>,
        ignoreCover: Boolean = false
    ): CoverTag {
        val vx = atkX - defender.x
        val vy = atkY - defender.y
        val dot = vx * defender.facing.dx + vy * defender.facing.dy

        return when {
            dot < 0 -> CoverTag.REAR
            dot == 0 && (vx != 0 || vy != 0) -> CoverTag.FLANK
            !ignoreCover && hasCoverBetween(atkX, atkY, defender.x, defender.y, map) -> CoverTag.COVER
            else -> CoverTag.OPEN
        }
    }

    fun evaluateCoverTag(
        attacker: BattleUnit,
        defender: BattleUnit,
        map: Array<Array<TerrainType>>,
        ignoreCover: Boolean = false
    ): CoverTag = evaluateCoverTag(attacker.x, attacker.y, defender, map, ignoreCover)

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

    fun weaponApCost(attacker: BattleUnit, mode: WeaponMode): Int = when (mode) {
        WeaponMode.MAIN -> 1
        WeaponMode.PISTOL -> attacker.template.pistolApCost
        WeaponMode.SKILL -> attacker.template.skillApCost
    }

    fun canTargetFrom(
        attacker: BattleUnit,
        atkX: Int,
        atkY: Int,
        defender: BattleUnit,
        mode: WeaponMode,
        map: Array<Array<TerrainType>>
    ): TargetPreview? {
        if (!attacker.isAlive || !defender.isAlive || attacker.faction == defender.faction) return null
        val apCost = weaponApCost(attacker, mode)
        if (attacker.apLeft < apCost) return null
        if (mode == WeaponMode.PISTOL && (!attacker.template.hasPistol || attacker.pistolCooldownLeft > 0)) return null
        if (mode == WeaponMode.SKILL && attacker.skillCooldownLeft > 0) return null
        if (mode == WeaponMode.SKILL && attacker.template.archetype == UnitArchetype.TERMINATOR) return null

        val dist = maxOf(abs(atkX - defender.x), abs(atkY - defender.y))
        val range = weaponRange(attacker, mode)
        val inRange = dist <= range
        val needsLos = range > 1 && attacker.template.archetype != UnitArchetype.HIVE_TYRANT
        val los = if (!inRange) false else if (!needsLos) true else hasLineOfSight(atkX, atkY, defender.x, defender.y, map)

        val ignoreCover = mode == WeaponMode.SKILL && attacker.template.archetype == UnitArchetype.GENESTEALER
        val coverTag = evaluateCoverTag(atkX, atkY, defender, map, ignoreCover)

        var hitChance = attacker.effectiveAccuracy
        if (attacker.template.archetype == UnitArchetype.INTERCESSOR && attacker.mpLeft == attacker.template.mp) {
            hitChance += 10
        }
        if (mode == WeaponMode.SKILL) {
            if (attacker.template.archetype == UnitArchetype.INTERCESSOR ||
                attacker.template.archetype == UnitArchetype.WARRIOR
            ) {
                hitChance += 10
            }
        }
        when (coverTag) {
            CoverTag.COVER -> {
                val penalty = if (defender.template.archetype == UnitArchetype.TERMAGANT) 22 else 15
                hitChance -= penalty
            }
            CoverTag.REAR -> hitChance += 15
            CoverTag.FLANK -> hitChance += 5
            CoverTag.OPEN -> Unit
        }
        hitChance = hitChance.coerceIn(10, 99)

        val perShot = expectedDamagePerHit(attacker, defender, mode, coverTag)
        val attacks = attacksCount(attacker, mode)
        val expectedTotal = (perShot * attacks * (hitChance / 100f)).roundToInt().coerceAtLeast(1)

        return TargetPreview(
            targetUid = defender.uid,
            targetName = defender.template.name,
            hitChance = hitChance,
            expectedTotalDamage = expectedTotal,
            attacksCount = attacks,
            coverTag = coverTag,
            weaponMode = mode,
            apCost = apCost,
            inRange = inRange,
            hasLos = los
        )
    }

    fun canTarget(
        attacker: BattleUnit,
        defender: BattleUnit,
        mode: WeaponMode,
        map: Array<Array<TerrainType>>
    ): TargetPreview? = canTargetFrom(attacker, attacker.x, attacker.y, defender, mode, map)

    private fun attacksCount(attacker: BattleUnit, mode: WeaponMode): Int {
        val base = when (mode) {
            WeaponMode.MAIN -> attacker.aliveModels * attacker.template.attacksPerModel
            WeaponMode.PISTOL -> attacker.aliveModels
            WeaponMode.SKILL -> when (attacker.template.archetype) {
                UnitArchetype.INTERCESSOR, UnitArchetype.TERMAGANT ->
                    attacker.aliveModels * (attacker.template.attacksPerModel + 1)
                else -> attacker.aliveModels * attacker.template.attacksPerModel
            }
        }
        return base.coerceAtLeast(1)
    }

    private fun expectedDamagePerHit(
        attacker: BattleUnit,
        defender: BattleUnit,
        mode: WeaponMode,
        coverTag: CoverTag
    ): Float {
        var rawDamage = when (mode) {
            WeaponMode.MAIN -> attacker.effectiveDamage.toFloat()
            WeaponMode.PISTOL -> attacker.template.pistolDamage.toFloat()
            WeaponMode.SKILL -> when (attacker.template.archetype) {
                UnitArchetype.HELLBLASTER -> attacker.effectiveDamage * 1.5f
                UnitArchetype.CARNIFEX -> attacker.effectiveDamage * 0.9f
                UnitArchetype.HIVE_TYRANT -> attacker.effectiveDamage * 0.85f
                else -> attacker.effectiveDamage * 1.2f
            }
        }

        val pen = attacker.template.armorPen +
            if (mode == WeaponMode.SKILL && attacker.template.archetype == UnitArchetype.HELLBLASTER) 2 else 0
        val effectiveArmor = max(0, defender.template.armor - pen)
        val armorFactor = (1f - (effectiveArmor * 0.07f)).coerceIn(0.45f, 1f)
        rawDamage *= armorFactor

        if (defender.template.archetype == UnitArchetype.DREADNOUGHT) {
            rawDamage = (rawDamage - 2f).coerceAtLeast(2f)
        }
        if (defender.damageReductionTurns > 0) {
            rawDamage *= 0.50f
        }
        if (coverTag == CoverTag.FLANK) rawDamage *= 1.25f
        if (coverTag == CoverTag.REAR) rawDamage *= 1.45f
        if (coverTag == CoverTag.COVER) rawDamage *= 0.85f
        return rawDamage.coerceAtLeast(1f)
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
                    if (cmd.faction == u.faction && maxOf(abs(cmd.x - u.x), abs(cmd.y - u.y)) <= 3) {
                        hasAura = true
                        break
                    }
                }
                if (u.auraBuffed == hasAura) u else u.copy(auraBuffed = hasAura)
            }
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
        val preview = canTarget(attacker, primaryTarget, mode, map)
            ?: return CombatOutcome(attacker, emptyList(), 0, 0, 0, emptyList(), false)

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
            val tgtPreview = if (idx == 0) preview else canTarget(attacker, tgt, mode, map) ?: preview
            val attacks = if (idx == 0) tgtPreview.attacksCount else (tgtPreview.attacksCount / 2).coerceAtLeast(1)
            val perHit = expectedDamagePerHit(attacker, tgt, mode, tgtPreview.coverTag)

            var tgtHits = 0
            var tgtDmg = 0
            repeat(attacks) {
                val roll = rng.nextInt(100)
                if (roll < tgtPreview.hitChance) {
                    tgtHits++
                    val variance = 0.85f + rng.nextFloat() * 0.30f
                    tgtDmg += (perHit * variance).roundToInt().coerceAtLeast(1)
                }
            }

            if (tgtPreview.coverTag == CoverTag.FLANK || tgtPreview.coverTag == CoverTag.REAR) {
                anyFlank = true
            }

            val modelsBefore = tgt.aliveModels
            val newHp = (tgt.totalHp - tgtDmg).coerceAtLeast(0)
            val momGain = if (tgtDmg > 0) 15 else 5
            val drainedAp = if (mode == WeaponMode.SKILL && attacker.template.archetype == UnitArchetype.HIVE_TYRANT && tgtHits > 0) {
                (tgt.apLeft - 1).coerceAtLeast(0)
            } else tgt.apLeft

            val updatedTgt = tgt.copy(
                totalHp = newHp,
                apLeft = drainedAp,
                momentum = (tgt.momentum + momGain).coerceAtMost(100)
            )
            updatedTargets.add(updatedTgt)

            val modelsLost = (modelsBefore - updatedTgt.aliveModels).coerceAtLeast(0)
            totalKills += if (newHp == 0) 1 else 0
            totalHits += tgtHits
            totalDmg += tgtDmg

            val tagPrefix = when {
                tgtDmg == 0 -> "MISS"
                tgtPreview.coverTag == CoverTag.REAR -> "REAR -$tgtDmg"
                tgtPreview.coverTag == CoverTag.FLANK -> "FLANK -$tgtDmg"
                else -> "-$tgtDmg"
            }
            val color = when {
                tgtDmg == 0 -> 0xFF9AA3B2L
                tgtPreview.coverTag == CoverTag.REAR || tgtPreview.coverTag == CoverTag.FLANK -> 0xFFFFD166L
                else -> 0xFFFF4D58L
            }
            floats.add(tgt.pos to (tagPrefix to color))
            if (modelsLost > 0 && newHp > 0) {
                floats.add(tgt.pos to ("-$modelsLost MODEL" to 0xFFFF8866L))
            } else if (newHp == 0) {
                floats.add(tgt.pos to ("ELIMINATED!" to 0xFFFF384AL))
            }
        }

        val isMelee = weaponRange(attacker, mode) <= 1
        val shockBonus = if (isMelee && attacker.template.archetype == UnitArchetype.ASSAULT_MARINE) 15 else 0
        val atkMomGain = (if (totalHits > 0) 20 else 8) + shockBonus

        val newSkillCd = if (mode == WeaponMode.SKILL) attacker.template.skillCooldown else attacker.skillCooldownLeft
        val newPistolCd = if (mode == WeaponMode.PISTOL) attacker.template.pistolCooldown else attacker.pistolCooldownLeft
        val newBuffTurns = if (mode == WeaponMode.SKILL &&
            (attacker.template.archetype == UnitArchetype.WARRIOR ||
                attacker.template.archetype == UnitArchetype.CAPTAIN)
        ) 2 else attacker.buffTurnsLeft
        val newBuffLabel = if (newBuffTurns > attacker.buffTurnsLeft) attacker.template.skillName else attacker.activeBuffLabel

        val updatedAttacker = attacker.copy(
            facing = facingToward(attacker.x, attacker.y, primaryTarget.x, primaryTarget.y),
            apLeft = (attacker.apLeft - preview.apCost).coerceAtLeast(0),
            momentum = (attacker.momentum + atkMomGain).coerceAtMost(100),
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
            isCritOrFlank = anyFlank
        )
    }

    fun activateSurge(unit: BattleUnit): BattleUnit {
        if (unit.momentum < 100 || unit.surgeActive) return unit
        return unit.copy(
            momentum = 0,
            surgeActive = true,
            apLeft = unit.apLeft + 1,
            mpLeft = unit.mpLeft + 2,
            activeBuffLabel = "SURGE +25%"
        )
    }

    fun activateSelfSkillIfApplicable(unit: BattleUnit): BattleUnit? {
        if (unit.skillCooldownLeft > 0 || unit.apLeft < unit.template.skillApCost) return null
        return when (unit.template.archetype) {
            UnitArchetype.HORMAGAUNT -> unit.copy(
                apLeft = unit.apLeft - unit.template.skillApCost,
                mpLeft = unit.mpLeft + 3,
                skillCooldownLeft = unit.template.skillCooldown,
                buffTurnsLeft = 1,
                activeBuffLabel = "LEAP +3 MP"
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
        if (!unit.isAlive || unit.apLeft < unit.template.skillApCost || unit.skillCooldownLeft > 0) return emptySet()
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
}
