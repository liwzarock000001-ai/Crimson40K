package com.example.engine

import com.example.model.BattleUnit
import com.example.model.ChargePreview
import com.example.model.CoverTag
import com.example.model.Faction
import com.example.model.GridPos
import com.example.model.TerrainType
import com.example.model.UnitArchetype
import com.example.model.WeaponMode
import kotlin.math.abs

sealed interface AiDecision {
    data class Move(val unitUid: String, val path: List<GridPos>, val cost: Int) : AiDecision
    data class Advance(val unitUid: String) : AiDecision
    data class Charge(val unitUid: String, val preview: ChargePreview) : AiDecision
    data class SelfSkill(val unitUid: String) : AiDecision
    data class Teleport(val unitUid: String, val targetPos: GridPos) : AiDecision
    data class Attack(val attackerUid: String, val targetUid: String, val mode: WeaponMode) : AiDecision
    data object TurnComplete : AiDecision
}

object AiController {
    private val MODES = arrayOf(WeaponMode.SKILL, WeaponMode.MAIN, WeaponMode.PISTOL)

    fun decideNextAction(
        aiFaction: Faction,
        units: List<BattleUnit>,
        map: Array<Array<TerrainType>>,
        aiOwnerId: String? = null
    ): AiDecision {
        val isAiUnit: (BattleUnit) -> Boolean = if (aiOwnerId != null) {
            { it.ownerPlayerId == aiOwnerId }
        } else {
            { it.faction == aiFaction }
        }
        val aiTeam = when (aiOwnerId) {
            "p1", "p2" -> 0
            "p3", "p4", "AI" -> 1
            else -> null
        }
        val enemies = units.filter {
            it.isAlive && if (aiTeam != null) it.effectiveTeam != aiTeam else it.faction != aiFaction
        }
        if (enemies.isEmpty()) return AiDecision.TurnComplete

        for (i in units.indices) {
            val unit = units[i]
            val isEngaged = TacticalRules.isEngagedInMelee(unit, units)
            val canAttackAny = unit.canShoot || unit.canFight || isEngaged
            if (!unit.isAlive || !isAiUnit(unit) || (!canAttackAny && unit.moveCmLeft < 3.0f)) continue

            // 1. Self-buff leap if melee far from enemy
            var distToNearest = Int.MAX_VALUE
            for (eIdx in enemies.indices) {
                val e = enemies[eIdx]
                val d = maxOf(abs(e.x - unit.x), abs(e.y - unit.y))
                if (d < distToNearest) distToNearest = d
            }
            if (unit.skillCooldownLeft == 0) {
                val selfSkill = TacticalRules.activateSelfSkillIfApplicable(unit, units)
                val shouldTriggerSelf = when (unit.template.archetype) {
                    UnitArchetype.VON_RYANS_LEAPER -> distToNearest <= 7 && !unit.isCloaked
                    UnitArchetype.ASSAULT_MARINE -> true
                    else -> distToNearest > 2
                }
                if (selfSkill != null && shouldTriggerSelf) {
                    return AiDecision.SelfSkill(unit.uid)
                }
                if (!isEngaged && unit.template.archetype == UnitArchetype.TERMINATOR && distToNearest > 4) {
                    val tTiles = TacticalRules.computeTeleportTiles(unit, map, units)
                    if (tTiles.isNotEmpty()) {
                        val bestTile = tTiles.minByOrNull { tPos ->
                            enemies.minOfOrNull { maxOf(abs(tPos.x - it.x), abs(tPos.y - it.y)) } ?: 99
                        }
                        if (bestTile != null) {
                            return AiDecision.Teleport(unit.uid, bestTile)
                        }
                    }
                }
            }

            // 2. Check best attack from current position
            val bestAttack = findBestAttackFrom(unit, unit.x, unit.y, enemies, units, map)
            if (bestAttack != null) {
                return AiDecision.Attack(unit.uid, bestAttack.first.uid, bestAttack.second)
            }

            // Forced Melee Duel Lock: if engaged with living enemy, cannot Charge elsewhere, Advance, or Move away
            if (isEngaged) continue

            // 3. Melee / Charger unit within Charge range -> Charge!
            if (unit.canCharge && (unit.template.range <= 1 || unit.hasShot || unit.moveCmLeft < 3.0f)) {
                var bestCharge: ChargePreview? = null
                var bestReq = Int.MAX_VALUE
                for (eIdx in enemies.indices) {
                    val preview = TacticalRules.findChargeEngagementSpot(unit, enemies[eIdx], units, map) ?: continue
                    if (preview.requiredRoll <= 8 && preview.requiredRoll < bestReq) {
                        bestReq = preview.requiredRoll
                        bestCharge = preview
                    }
                }
                if (bestCharge != null) {
                    return AiDecision.Charge(unit.uid, bestCharge)
                }
            }

            // 4. Advance if Assault shooter far from target
            if (unit.canAdvance && unit.template.mainWeaponAssault && distToNearest > unit.template.range + 2) {
                return AiDecision.Advance(unit.uid)
            }

            // 4. Move closer to best firing/melee position if cm movement left (zero BattleUnit.copy allocation!)
            if (unit.moveCmLeft >= 3.0f) {
                val search = TacticalRules.computeReachableTiles(unit, map, units)
                if (search.costs.isNotEmpty()) {
                    val idealRange = unit.template.range
                    var bestTile: GridPos? = null
                    var bestScore = Float.NEGATIVE_INFINITY
                    var bestHasAttack = false

                    for ((pos, cost) in search.costs) {
                        val attackFromHere = findBestAttackFrom(unit, pos.x, pos.y, enemies, units, map)
                        var score = 0f
                        var tileHasAttack = false
                        if (attackFromHere != null) {
                            tileHasAttack = true
                            val preview = TacticalRules.canTargetFrom(unit, pos.x, pos.y, attackFromHere.first, attackFromHere.second, map, units)
                            score += 1000f + (preview?.expectedTotalDamage ?: 0) * 5f
                            if (preview?.coverTag == CoverTag.OPEN) score += 60f
                        }
                        var minEnemyDist = Int.MAX_VALUE
                        for (eIdx in enemies.indices) {
                            val e = enemies[eIdx]
                            val d = maxOf(abs(e.x - pos.x), abs(e.y - pos.y))
                            if (d < minEnemyDist) minEnemyDist = d
                        }
                        score -= abs(minEnemyDist - idealRange) * 25f
                        score -= minEnemyDist * 10f
                        val terrain = map[pos.y][pos.x]
                        if (terrain == TerrainType.OBJECTIVE) score += 45f
                        if (terrain == TerrainType.BARRICADE || terrain == TerrainType.CRATER) score += 20f
                        score -= cost * 1.5f

                        if (score > bestScore) {
                            bestScore = score
                            bestTile = pos
                            bestHasAttack = tileHasAttack
                        }
                    }

                    if (bestTile != null) {
                        var newMinDist = Int.MAX_VALUE
                        for (eIdx in enemies.indices) {
                            val e = enemies[eIdx]
                            val d = maxOf(abs(e.x - bestTile.x), abs(e.y - bestTile.y))
                            if (d < newMinDist) newMinDist = d
                        }
                        if (bestHasAttack || newMinDist < distToNearest) {
                            val path = TacticalRules.reconstructPath(bestTile, unit.pos, search.parents)
                            val cost = search.costs[bestTile] ?: 1
                            if (path.isNotEmpty()) {
                                return AiDecision.Move(unit.uid, path, cost)
                            }
                        }
                    }
                }
            }
        }

        return AiDecision.TurnComplete
    }

    private fun findBestAttackFrom(
        unit: BattleUnit,
        atkX: Int,
        atkY: Int,
        enemies: List<BattleUnit>,
        allUnits: List<BattleUnit>,
        map: Array<Array<TerrainType>>
    ): Pair<BattleUnit, WeaponMode>? {
        val engagedAtPos = TacticalRules.isEngagedInMeleeAt(unit, atkX.toFloat(), atkY.toFloat(), allUnits)
        if (!unit.canShoot && !unit.canFight && !engagedAtPos) return null
        var best: Pair<BattleUnit, WeaponMode>? = null
        var bestScore = Float.NEGATIVE_INFINITY

        for (mIdx in MODES.indices) {
            val mode = MODES[mIdx]
            if (!TacticalRules.canUseMode(unit, mode, allUnits)) continue

            val range = if (engagedAtPos && mode == WeaponMode.MAIN && !unit.template.mainWeaponPistol) {
                1
            } else {
                TacticalRules.weaponRange(unit, mode)
            }
            for (eIdx in enemies.indices) {
                val enemy = enemies[eIdx]
                // Fast distance early-exit before LOS/cover math
                if (maxOf(abs(atkX - enemy.x), abs(atkY - enemy.y)) > range + 1) continue
                val preview = TacticalRules.canTargetFrom(unit, atkX, atkY, enemy, mode, map, allUnits) ?: continue
                if (!preview.inRange || !preview.hasLos) continue

                var score = preview.expectedTotalDamage.toFloat()
                if (preview.expectedTotalDamage >= enemy.totalWounds) score += 250f
                if (mode == WeaponMode.SKILL) score += 25f
                if (mode == WeaponMode.SKILL &&
                    unit.template.archetype == UnitArchetype.INCURSOR &&
                    enemy.markedByTeam != unit.effectiveTeam
                ) {
                    score += 40f
                }
                if (enemy.markedByTeam == unit.effectiveTeam && !preview.isMelee) {
                    score += 20f
                }
                if (score > bestScore) {
                    bestScore = score
                    best = enemy to mode
                }
            }
        }
        return best
    }
}
