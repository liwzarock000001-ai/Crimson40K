package com.example.engine

import com.example.model.BattleSummary
import com.example.model.BattleUnit
import com.example.model.ChargePreview
import com.example.model.CombatLogEntry
import com.example.model.CoopPlayer
import com.example.model.CoopRosterUnit
import com.example.model.CoopSlotMode
import com.example.model.CoopState
import com.example.model.CoverTag
import com.example.model.DarkPactChoice
import com.example.model.DiceStepDetail
import com.example.model.Facing
import com.example.model.Faction
import com.example.model.GridPos
import com.example.model.MovePreview
import com.example.model.SectorMap
import com.example.model.TargetPreview
import com.example.model.UnitTemplate
import com.example.model.WeaponMode
import com.example.model.WorldPos
import com.example.viewmodel.BattleUiState
import org.json.JSONArray
import org.json.JSONObject

object BattleStateSerializer {

    fun serializeUnits(units: List<BattleUnit>): String {
        val arr = JSONArray()
        for (u in units) {
            arr.put(JSONObject().apply {
                put("uid", u.uid)
                put("templateId", u.template.id)
                put("faction", u.faction.name)
                put("x", u.x)
                put("y", u.y)
                put("facing", u.facing.name)
                put("totalWounds", u.totalWounds)
                put("maxTotalWounds", u.maxTotalWounds)
                put("moveCmLeft", u.moveCmLeft.toDouble())
                put("hasAdvanced", u.hasAdvanced)
                put("hasCharged", u.hasCharged)
                put("hasShot", u.hasShot)
                put("hasFought", u.hasFought)
                put("worldX", u.worldX.toDouble())
                put("worldY", u.worldY.toDouble())
                put("facingDeg", u.facingDeg.toDouble())
                put("skillCooldownLeft", u.skillCooldownLeft)
                put("pistolCooldownLeft", u.pistolCooldownLeft)
                put("buffTurnsLeft", u.buffTurnsLeft)
                put("activeBuffLabel", u.activeBuffLabel ?: "")
                put("damageReductionTurns", u.damageReductionTurns)
                put("stealthTurnsLeft", u.stealthTurnsLeft)
                put("auraBuffed", u.auraBuffed)
                put("kills", u.kills)
                put("damageDealt", u.damageDealt)
                put("ownerPlayerId", u.ownerPlayerId)
                put("ownerPlayerName", u.ownerPlayerName)
                put("markedByTeam", u.markedByTeam ?: -1)
                put("meleeChargeBuffActive", u.meleeChargeBuffActive)
                put("bladeguardRerollBuffActive", u.bladeguardRerollBuffActive)
                put("legionaryWoundRerollBuffActive", u.legionaryWoundRerollBuffActive)
                put("brutalisChargeBuffActive", u.brutalisChargeBuffActive)
                put("brutalisAttackStacks", u.brutalisAttackStacks)
                put("brutalisStackEarnedThisTurn", u.brutalisStackEarnedThisTurn)
                put("attachedLeaderUid", u.attachedLeaderUid ?: "")
                put("bodyguardUid", u.bodyguardUid ?: "")
                put("isHero", u.isHero)
                put("hasFeelNoPain", u.hasFeelNoPain)
                put("inheritedPassiveIds", JSONArray(u.inheritedPassiveIds))
                put("honourOfUltramarPendingRemoval", u.honourOfUltramarPendingRemoval)
            })
        }
        return arr.toString()
    }

    fun deserializeUnits(json: String, templates: Map<String, UnitTemplate>): List<BattleUnit> {
        if (json.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            val result = ArrayList<BattleUnit>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val tplId = o.getString("templateId")
                val tpl = templates[tplId] ?: DefaultCatalog.defaultTemplates.first()
                val faction = runCatching { Faction.valueOf(o.getString("faction")) }.getOrDefault(tpl.faction)
                val facing = runCatching { Facing.valueOf(o.getString("facing")) }.getOrDefault(Facing.EAST)
                val x = o.getInt("x")
                val y = o.getInt("y")
                val buffLabel = o.optString("activeBuffLabel")
                result.add(
                    BattleUnit(
                        uid = o.getString("uid"),
                        template = tpl,
                        faction = faction,
                        x = x,
                        y = y,
                        facing = facing,
                        totalWounds = o.getInt("totalWounds"),
                        maxTotalWounds = o.optInt("maxTotalWounds", tpl.maxTotalWounds),
                        moveCmLeft = o.optDouble("moveCmLeft", tpl.moveCm.toDouble()).toFloat(),
                        hasAdvanced = o.optBoolean("hasAdvanced", false),
                        hasCharged = o.optBoolean("hasCharged", false),
                        hasShot = o.optBoolean("hasShot", false),
                        hasFought = o.optBoolean("hasFought", false),
                        worldX = o.optDouble("worldX", x.toDouble()).toFloat(),
                        worldY = o.optDouble("worldY", y.toDouble()).toFloat(),
                        facingDeg = o.optDouble("facingDeg", facing.yawDegrees().toDouble()).toFloat(),
                        skillCooldownLeft = o.optInt("skillCooldownLeft", 0),
                        pistolCooldownLeft = o.optInt("pistolCooldownLeft", 0),
                        buffTurnsLeft = o.optInt("buffTurnsLeft", 0),
                        activeBuffLabel = buffLabel.ifBlank { null },
                        damageReductionTurns = o.optInt("damageReductionTurns", 0),
                        stealthTurnsLeft = o.optInt("stealthTurnsLeft", 0),
                        auraBuffed = o.optBoolean("auraBuffed", false),
                        kills = o.optInt("kills", 0),
                        damageDealt = o.optInt("damageDealt", 0),
                        ownerPlayerId = o.optString("ownerPlayerId", "PLAYER"),
                        ownerPlayerName = o.optString("ownerPlayerName", ""),
                        markedByTeam = o.optInt("markedByTeam", -1).let { if (it >= 0) it else null },
                        meleeChargeBuffActive = o.optBoolean("meleeChargeBuffActive", false),
                        bladeguardRerollBuffActive = o.optBoolean("bladeguardRerollBuffActive", false),
                        legionaryWoundRerollBuffActive = o.optBoolean("legionaryWoundRerollBuffActive", false),
                        brutalisChargeBuffActive = o.optBoolean("brutalisChargeBuffActive", false),
                        brutalisAttackStacks = o.optInt("brutalisAttackStacks", 0).coerceIn(0, 4),
                        brutalisStackEarnedThisTurn = o.optBoolean("brutalisStackEarnedThisTurn", false),
                        attachedLeaderUid = o.optString("attachedLeaderUid").ifBlank { null },
                        bodyguardUid = o.optString("bodyguardUid").ifBlank { null },
                        isHero = o.optBoolean("isHero", tpl.isHero),
                        hasFeelNoPain = o.optBoolean("hasFeelNoPain", tpl.isHero || tpl.archetype == com.example.model.UnitArchetype.CAPTAIN_TITUS || tpl.feelNoPainTarget != null),
                        inheritedPassiveIds = o.optJSONArray("inheritedPassiveIds")?.let { pArr ->
                            (0 until pArr.length()).mapNotNull { idx -> pArr.optString(idx).takeIf { it.isNotBlank() } }
                        } ?: emptyList(),
                        honourOfUltramarPendingRemoval = o.optBoolean("honourOfUltramarPendingRemoval", false)
                    )
                )
            }
            result
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun serializeCoopBattleSnapshot(state: BattleUiState, templates: Map<String, UnitTemplate>): String {
        val obj = JSONObject()
        obj.put("type", "STATE_SYNC")
        obj.put("active", state.active)
        obj.put("round", state.round)
        obj.put("activeFaction", state.activeFaction.name)
        obj.put("playerFaction", state.playerFaction.name)
        obj.put("enemyFaction", state.enemyFaction.name)
        obj.put("mapId", state.map.id)
        obj.put("unitsJson", serializeUnits(state.units))
        obj.put("templatesJson", DefaultCatalog.serializeTemplates(templates))
        obj.put("selectedUnitUid", state.selectedUnitUid ?: "")
        obj.put("weaponMode", state.weaponMode.name)
        obj.put("selectedDarkPact", state.selectedDarkPact.name)
        obj.put("cmStepChoice", state.cmStepChoice.toDouble())
        obj.put("moveModeActive", state.moveModeActive)
        obj.put("chargeModeActive", state.chargeModeActive)
        obj.put("isAnimating", state.isAnimating)
        obj.put("isAiThinking", state.isAiThinking)
        obj.put("isCoopMode", state.isCoopMode)
        obj.put("activeCoopPlayerId", state.activeCoopPlayerId ?: "")
        obj.put("activeCoopPlayerName", state.activeCoopPlayerName)
        obj.put("coopTurnIndex", state.coopTurnIndex)
        obj.put("playerKills", state.playerKills)
        obj.put("enemyKills", state.enemyKills)
        state.movePreview?.let { mp ->
            val pathArr = JSONArray()
            for (wp in mp.path) {
                pathArr.put(JSONObject().apply {
                    put("x", wp.x.toDouble())
                    put("y", wp.y.toDouble())
                })
            }
            obj.put("movePreview", JSONObject().apply {
                put("targetX", mp.targetX.toDouble())
                put("targetY", mp.targetY.toDouble())
                put("distanceCm", mp.distanceCm.toDouble())
                put("path", pathArr)
            })
        }
        state.chargePreview?.let { cp ->
            val pathArr = JSONArray()
            for (wp in cp.path) {
                pathArr.put(JSONObject().apply {
                    put("x", wp.x.toDouble())
                    put("y", wp.y.toDouble())
                })
            }
            obj.put("chargePreview", JSONObject().apply {
                put("targetUid", cp.targetUid)
                put("targetName", cp.targetName)
                put("distanceTiles", cp.distanceTiles.toDouble())
                put("distanceCm", cp.distanceCm.toDouble())
                put("requiredRoll", cp.requiredRoll)
                put("destinationX", cp.destinationX.toDouble())
                put("destinationY", cp.destinationY.toDouble())
                put("path", pathArr)
            })
        }
        state.targetPreview?.let { tp ->
            obj.put("targetPreview", JSONObject().apply {
                put("targetUid", tp.targetUid)
                put("targetName", tp.targetName)
                put("hitChance", tp.hitChance)
                put("hitRollNeeded", tp.hitRollNeeded)
                put("woundRollNeeded", tp.woundRollNeeded)
                put("saveRollNeeded", tp.saveRollNeeded)
                put("usingInvuln", tp.usingInvuln)
                put("isMelee", tp.isMelee)
                put("weaponStrength", tp.weaponStrength)
                put("weaponAp", tp.weaponAp)
                put("targetToughness", tp.targetToughness)
                put("expectedTotalDamage", tp.expectedTotalDamage)
                put("attacksCount", tp.attacksCount)
                put("coverTag", tp.coverTag.name)
                put("weaponMode", tp.weaponMode.name)
                put("weaponName", tp.weaponName)
                put("sustainedHits", tp.sustainedHits)
                put("lethalHits", tp.lethalHits)
                put("rerollWoundOnes", tp.rerollWoundOnes)
                put("twinLinked", tp.twinLinked)
                put("meltaBonus", tp.meltaBonus)
                put("rapidFireBonus", tp.rapidFireBonus)
                put("antiFlyTriggered", tp.antiFlyTriggered)
                put("antiNonMonsterVehicleTriggered", tp.antiNonMonsterVehicleTriggered)
                put("heavyApplied", tp.heavyApplied)
                put("cleaveBonus", tp.cleaveBonus)
                put("darkPactChoice", tp.darkPactChoice.name)
                put("markedBonusApplied", tp.markedBonusApplied)
                put("inRange", tp.inRange)
                put("hasLos", tp.hasLos)
            })
        }
        val orderArr = JSONArray()
        for (p in state.coopTurnOrder) {
            orderArr.put(JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("faction", p.faction.name)
                put("slotMode", p.slotMode.name)
            })
        }
        obj.put("coopTurnOrder", orderArr)
        state.summary?.let { sum ->
            obj.put("summary", JSONObject().apply {
                put("winner", sum.winner.name)
                put("winnerLabel", sum.winnerLabel)
                put("playerWon", sum.playerWon)
                put("rounds", sum.rounds)
                put("playerKills", sum.playerKills)
                put("enemyKills", sum.enemyKills)
                put("totalDamageDealt", sum.totalDamageDealt)
            })
        }
        obj.put("combatLogs", serializeCombatLogs(state.combatLogs.take(25)))
        return obj.toString()
    }

    private fun serializeCombatLogs(logs: List<CombatLogEntry>): JSONArray {
        val arr = JSONArray()
        for (entry in logs) {
            val stepsArr = JSONArray()
            for (s in entry.steps) {
                val diceArr = JSONArray()
                for (d in s.diceValues) diceArr.put(d)
                val rerollsArr = JSONArray()
                for (rr in s.rerolls) {
                    rerollsArr.put(JSONObject().apply {
                        put("dieIndex", rr.dieIndex)
                        put("originalValue", rr.originalValue)
                        put("newValue", rr.newValue)
                        put("passed", rr.passed)
                    })
                }
                stepsArr.put(JSONObject().apply {
                    put("phaseName", s.phaseName)
                    put("stepLabel", s.stepLabel)
                    put("targetNeeded", s.targetNeeded)
                    put("rolled", s.rolled)
                    put("passed", s.passed)
                    put("failed", s.failed)
                    put("diceValues", diceArr)
                    put("note", s.note)
                    if (rerollsArr.length() > 0) {
                        put("rerolls", rerollsArr)
                    }
                })
            }
            arr.put(JSONObject().apply {
                put("id", entry.id)
                put("round", entry.round)
                put("faction", entry.faction.name)
                put("title", entry.title)
                put("subtitle", entry.subtitle)
                put("steps", stepsArr)
                put("resultSummary", entry.resultSummary)
                put("isCounterOrFightFirst", entry.isCounterOrFightFirst)
                put("isSuccess", entry.isSuccess)
            })
        }
        return arr
    }

    private fun deserializeCombatLogs(arr: JSONArray?): List<CombatLogEntry> {
        if (arr == null) return emptyList()
        val out = ArrayList<CombatLogEntry>(arr.length())
        for (i in 0 until arr.length()) {
            val lo = arr.optJSONObject(i) ?: continue
            val stepsArr = lo.optJSONArray("steps") ?: JSONArray()
            val steps = ArrayList<DiceStepDetail>(stepsArr.length())
            for (j in 0 until stepsArr.length()) {
                val so = stepsArr.optJSONObject(j) ?: continue
                val diceArr = so.optJSONArray("diceValues") ?: JSONArray()
                val dice = ArrayList<Int>(diceArr.length())
                for (k in 0 until diceArr.length()) {
                    dice.add(diceArr.optInt(k, 1))
                }
                val rerollsArr = so.optJSONArray("rerolls")
                val rerolls = if (rerollsArr != null) {
                    List(rerollsArr.length()) { rIdx ->
                        val ro = rerollsArr.optJSONObject(rIdx)
                        com.example.model.DiceRerollDetail(
                            dieIndex = ro?.optInt("dieIndex", 0) ?: 0,
                            originalValue = ro?.optInt("originalValue", 1) ?: 1,
                            newValue = ro?.optInt("newValue", 1) ?: 1,
                            passed = ro?.optBoolean("passed", false) ?: false
                        )
                    }
                } else emptyList()
                steps.add(
                    DiceStepDetail(
                        phaseName = so.optString("phaseName", "ROLL"),
                        stepLabel = so.optString("stepLabel", ""),
                        targetNeeded = so.optInt("targetNeeded", 1),
                        rolled = so.optInt("rolled", 0),
                        passed = so.optInt("passed", 0),
                        failed = so.optInt("failed", 0),
                        diceValues = dice,
                        note = so.optString("note", ""),
                        rerolls = rerolls
                    )
                )
            }
            out.add(
                CombatLogEntry(
                    id = lo.optLong("id", 0L),
                    round = lo.optInt("round", 1),
                    faction = runCatching { Faction.valueOf(lo.optString("faction", Faction.MARINES.name)) }.getOrDefault(Faction.MARINES),
                    title = lo.optString("title", ""),
                    subtitle = lo.optString("subtitle", ""),
                    steps = steps,
                    resultSummary = lo.optString("resultSummary", ""),
                    isCounterOrFightFirst = lo.optBoolean("isCounterOrFightFirst", false),
                    isSuccess = lo.optBoolean("isSuccess", true)
                )
            )
        }
        return out
    }

    fun applyCoopBattleSnapshot(
        json: String,
        current: BattleUiState
    ): BattleUiState? {
        return try {
            val o = JSONObject(json)
            if (o.optString("type") != "STATE_SYNC") return null
            val templates = DefaultCatalog.deserializeTemplates(o.optString("templatesJson"))
            val mapId = o.getLong("mapId")
            val sectorMap = if (current.map.id == mapId) current.map else SectorMap(mapId, MapGenerator.generateSectorMap(mapId))
            val units = deserializeUnits(o.optString("unitsJson"), templates)
            val activeFaction = runCatching { Faction.valueOf(o.getString("activeFaction")) }.getOrDefault(Faction.MARINES)
            val playerFaction = runCatching { Faction.valueOf(o.getString("playerFaction")) }.getOrDefault(Faction.MARINES)
            val enemyFaction = runCatching { Faction.valueOf(o.optString("enemyFaction", Faction.XENOS.name)) }.getOrDefault(Faction.XENOS)
            val selUid = o.optString("selectedUnitUid").ifBlank { null }
            val activePid = o.optString("activeCoopPlayerId").ifBlank { null }
            val orderArr = o.optJSONArray("coopTurnOrder") ?: JSONArray()
            val turnOrder = ArrayList<CoopPlayer>(orderArr.length())
            for (i in 0 until orderArr.length()) {
                val po = orderArr.getJSONObject(i)
                turnOrder.add(
                    CoopPlayer(
                        id = po.getString("id"),
                        name = po.getString("name"),
                        faction = runCatching { Faction.valueOf(po.getString("faction")) }.getOrDefault(Faction.MARINES),
                        slotMode = runCatching { CoopSlotMode.valueOf(po.getString("slotMode")) }.getOrDefault(CoopSlotMode.LAN_CLIENT)
                    )
                )
            }
            val summaryObj = o.optJSONObject("summary")
            val parsedSummary = if (summaryObj != null) {
                BattleSummary(
                    winner = runCatching { Faction.valueOf(summaryObj.getString("winner")) }.getOrDefault(Faction.MARINES),
                    playerWon = summaryObj.optBoolean("playerWon", true),
                    rounds = summaryObj.optInt("rounds", 1),
                    playerKills = summaryObj.optInt("playerKills", 0),
                    enemyKills = summaryObj.optInt("enemyKills", 0),
                    totalDamageDealt = summaryObj.optInt("totalDamageDealt", 0),
                    winnerLabel = summaryObj.optString("winnerLabel", "")
                )
            } else null
            val parsedLogs = deserializeCombatLogs(o.optJSONArray("combatLogs"))
            val parsedWeaponMode = runCatching {
                WeaponMode.valueOf(o.optString("weaponMode", WeaponMode.MAIN.name))
            }.getOrDefault(WeaponMode.MAIN)
            val parsedDarkPact = runCatching {
                DarkPactChoice.valueOf(o.optString("selectedDarkPact", DarkPactChoice.NONE.name))
            }.getOrDefault(DarkPactChoice.NONE)
            val parsedCmStepChoice = o.optDouble("cmStepChoice", 3.0).toFloat()
            val parsedMoveMode = o.optBoolean("moveModeActive", false)
            val parsedChargeMode = o.optBoolean("chargeModeActive", false)
            val parsedIsAnimating = o.optBoolean("isAnimating", false)
            val parsedMovePreview = o.optJSONObject("movePreview")?.let { mp ->
                val pathArr = mp.optJSONArray("path") ?: JSONArray()
                val path = ArrayList<WorldPos>(pathArr.length())
                for (i in 0 until pathArr.length()) {
                    val wp = pathArr.optJSONObject(i) ?: continue
                    path.add(WorldPos(wp.optDouble("x", 0.0).toFloat(), wp.optDouble("y", 0.0).toFloat()))
                }
                MovePreview(
                    targetX = mp.optDouble("targetX", 0.0).toFloat(),
                    targetY = mp.optDouble("targetY", 0.0).toFloat(),
                    distanceCm = mp.optDouble("distanceCm", 0.0).toFloat(),
                    path = path
                )
            }
            val parsedChargePreview = o.optJSONObject("chargePreview")?.let { cp ->
                val uid = cp.optString("targetUid").takeIf { it.isNotBlank() } ?: return@let null
                val pathArr = cp.optJSONArray("path") ?: JSONArray()
                val path = ArrayList<WorldPos>(pathArr.length())
                for (i in 0 until pathArr.length()) {
                    val wp = pathArr.optJSONObject(i) ?: continue
                    path.add(WorldPos(wp.optDouble("x", 0.0).toFloat(), wp.optDouble("y", 0.0).toFloat()))
                }
                ChargePreview(
                    targetUid = uid,
                    targetName = cp.optString("targetName", ""),
                    distanceTiles = cp.optDouble("distanceTiles", 0.0).toFloat(),
                    distanceCm = cp.optDouble("distanceCm", 0.0).toFloat(),
                    requiredRoll = cp.optInt("requiredRoll", 2),
                    destinationX = cp.optDouble("destinationX", 0.0).toFloat(),
                    destinationY = cp.optDouble("destinationY", 0.0).toFloat(),
                    path = path
                )
            }
            val parsedTargetPreview = o.optJSONObject("targetPreview")?.let { tp ->
                val uid = tp.optString("targetUid").takeIf { it.isNotBlank() } ?: return@let null
                TargetPreview(
                    targetUid = uid,
                    targetName = tp.optString("targetName", ""),
                    hitChance = tp.optInt("hitChance", 0),
                    hitRollNeeded = tp.optInt("hitRollNeeded", 3),
                    woundRollNeeded = tp.optInt("woundRollNeeded", 4),
                    saveRollNeeded = tp.optInt("saveRollNeeded", 4),
                    usingInvuln = tp.optBoolean("usingInvuln", false),
                    isMelee = tp.optBoolean("isMelee", false),
                    weaponStrength = tp.optInt("weaponStrength", 4),
                    weaponAp = tp.optInt("weaponAp", 0),
                    targetToughness = tp.optInt("targetToughness", 4),
                    expectedTotalDamage = tp.optInt("expectedTotalDamage", 0),
                    attacksCount = tp.optInt("attacksCount", 1),
                    coverTag = runCatching { CoverTag.valueOf(tp.optString("coverTag", CoverTag.OPEN.name)) }.getOrDefault(CoverTag.OPEN),
                    weaponMode = runCatching { WeaponMode.valueOf(tp.optString("weaponMode", WeaponMode.MAIN.name)) }.getOrDefault(WeaponMode.MAIN),
                    weaponName = tp.optString("weaponName", ""),
                    sustainedHits = tp.optInt("sustainedHits", 0),
                    lethalHits = tp.optBoolean("lethalHits", false),
                    rerollWoundOnes = tp.optBoolean("rerollWoundOnes", false),
                    twinLinked = tp.optBoolean("twinLinked", false),
                    meltaBonus = tp.optInt("meltaBonus", 0),
                    rapidFireBonus = tp.optInt("rapidFireBonus", 0),
                    antiFlyTriggered = tp.optBoolean("antiFlyTriggered", false),
                    antiNonMonsterVehicleTriggered = tp.optBoolean("antiNonMonsterVehicleTriggered", false),
                    heavyApplied = tp.optBoolean("heavyApplied", false),
                    cleaveBonus = tp.optInt("cleaveBonus", 0),
                    darkPactChoice = runCatching {
                        DarkPactChoice.valueOf(tp.optString("darkPactChoice", DarkPactChoice.NONE.name))
                    }.getOrDefault(DarkPactChoice.NONE),
                    markedBonusApplied = tp.optBoolean("markedBonusApplied", false),
                    inRange = tp.optBoolean("inRange", true),
                    hasLos = tp.optBoolean("hasLos", true)
                )
            }
            val selPos = units.firstOrNull { it.uid == selUid }?.pos
            current.copy(
                active = o.optBoolean("active", true),
                round = o.optInt("round", 1),
                activeFaction = activeFaction,
                playerFaction = playerFaction,
                enemyFaction = enemyFaction,
                map = sectorMap,
                units = units,
                selectedUnitUid = selUid,
                weaponMode = parsedWeaponMode,
                selectedDarkPact = parsedDarkPact,
                cmStepChoice = parsedCmStepChoice,
                isAnimating = parsedIsAnimating,
                isAiThinking = o.optBoolean("isAiThinking", false),
                isCoopMode = o.optBoolean("isCoopMode", true),
                activeCoopPlayerId = activePid,
                activeCoopPlayerName = o.optString("activeCoopPlayerName", ""),
                coopTurnOrder = turnOrder,
                coopTurnIndex = o.optInt("coopTurnIndex", 0),
                playerKills = o.optInt("playerKills", 0),
                enemyKills = o.optInt("enemyKills", 0),
                summary = parsedSummary,
                combatLogs = parsedLogs,
                focusGridPos = if (current.autoFocusCamera && selPos != null) selPos else current.focusGridPos,
                moveModeActive = parsedMoveMode,
                movePreview = parsedMovePreview,
                chargeModeActive = parsedChargeMode,
                chargePreview = parsedChargePreview,
                targetPreview = parsedTargetPreview
            )
        } catch (_: Exception) {
            null
        }
    }

    fun serializeCoopLobbySnapshot(state: CoopState): String {
        val obj = JSONObject()
        obj.put("type", "LOBBY_SYNC")
        obj.put("roomCode", state.roomCode)
        obj.put("hostIp", state.hostIp)
        obj.put("port", state.port)
        obj.put("pointLimit", state.pointLimit)
        obj.put("spawnAiEnemyIfSameFaction", state.spawnAiEnemyIfSameFaction)
        obj.put("networkStatus", state.networkStatus)

        val playersArr = JSONArray()
        for (p in state.players) {
            playersArr.put(JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("faction", p.faction.name)
                put("slotMode", p.slotMode.name)
                put("ready", p.ready)
                put("connectedClient", p.connectedClient)
            })
        }
        obj.put("players", playersArr)

        val unitsArr = JSONArray()
        for (u in state.assignedUnits) {
            unitsArr.put(JSONObject().apply {
                put("entryId", u.entryId)
                put("ownerPlayerId", u.ownerPlayerId)
                put("templateId", u.templateId)
            })
        }
        obj.put("assignedUnits", unitsArr)
        return obj.toString()
    }

    fun applyCoopLobbySnapshot(json: String, current: CoopState): CoopState? {
        return try {
            val o = JSONObject(json)
            if (o.optString("type") != "LOBBY_SYNC") return null
            val playersArr = o.optJSONArray("players") ?: JSONArray()
            val players = ArrayList<CoopPlayer>(playersArr.length())
            for (i in 0 until playersArr.length()) {
                val po = playersArr.getJSONObject(i)
                players.add(
                    CoopPlayer(
                        id = po.getString("id"),
                        name = po.getString("name"),
                        faction = runCatching { Faction.valueOf(po.getString("faction")) }.getOrDefault(Faction.MARINES),
                        slotMode = runCatching { CoopSlotMode.valueOf(po.getString("slotMode")) }.getOrDefault(CoopSlotMode.LAN_CLIENT),
                        ready = po.optBoolean("ready", true),
                        connectedClient = po.optBoolean("connectedClient", false)
                    )
                )
            }
            val unitsArr = o.optJSONArray("assignedUnits") ?: JSONArray()
            val units = ArrayList<CoopRosterUnit>(unitsArr.length())
            for (i in 0 until unitsArr.length()) {
                val uo = unitsArr.getJSONObject(i)
                units.add(
                    CoopRosterUnit(
                        entryId = uo.getString("entryId"),
                        ownerPlayerId = uo.getString("ownerPlayerId"),
                        templateId = uo.getString("templateId")
                    )
                )
            }
            current.copy(
                inRoom = true,
                roomCode = o.optString("roomCode", current.roomCode),
                hostIp = o.optString("hostIp", current.hostIp),
                port = o.optInt("port", current.port),
                pointLimit = o.optInt("pointLimit", current.pointLimit).coerceIn(500, 5000),
                spawnAiEnemyIfSameFaction = o.optBoolean("spawnAiEnemyIfSameFaction", current.spawnAiEnemyIfSameFaction),
                networkStatus = o.optString("networkStatus", current.networkStatus),
                lobbyError = null,
                players = if (players.isNotEmpty()) players else current.players,
                assignedUnits = units
            )
        } catch (_: Exception) {
            null
        }
    }
}
