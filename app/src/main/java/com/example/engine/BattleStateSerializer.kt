package com.example.engine

import com.example.model.BattleSummary
import com.example.model.BattleUnit
import com.example.model.CoopPlayer
import com.example.model.CoopRosterUnit
import com.example.model.CoopSlotMode
import com.example.model.CoopState
import com.example.model.Facing
import com.example.model.Faction
import com.example.model.GridPos
import com.example.model.SectorMap
import com.example.model.UnitTemplate
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
                put("auraBuffed", u.auraBuffed)
                put("kills", u.kills)
                put("damageDealt", u.damageDealt)
                put("ownerPlayerId", u.ownerPlayerId)
                put("ownerPlayerName", u.ownerPlayerName)
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
                        auraBuffed = o.optBoolean("auraBuffed", false),
                        kills = o.optInt("kills", 0),
                        damageDealt = o.optInt("damageDealt", 0),
                        ownerPlayerId = o.optString("ownerPlayerId", "PLAYER"),
                        ownerPlayerName = o.optString("ownerPlayerName", "")
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
        obj.put("mapId", state.map.id)
        obj.put("unitsJson", serializeUnits(state.units))
        obj.put("templatesJson", DefaultCatalog.serializeTemplates(templates))
        obj.put("selectedUnitUid", state.selectedUnitUid ?: "")
        obj.put("isAiThinking", state.isAiThinking)
        obj.put("isCoopMode", state.isCoopMode)
        obj.put("activeCoopPlayerId", state.activeCoopPlayerId ?: "")
        obj.put("activeCoopPlayerName", state.activeCoopPlayerName)
        obj.put("coopTurnIndex", state.coopTurnIndex)
        obj.put("playerKills", state.playerKills)
        obj.put("enemyKills", state.enemyKills)
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
                put("playerWon", sum.playerWon)
                put("rounds", sum.rounds)
                put("playerKills", sum.playerKills)
                put("enemyKills", sum.enemyKills)
                put("totalDamageDealt", sum.totalDamageDealt)
            })
        }
        return obj.toString()
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
                        slotMode = runCatching { CoopSlotMode.valueOf(po.getString("slotMode")) }.getOrDefault(CoopSlotMode.LOCAL_PLAYER)
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
                    totalDamageDealt = summaryObj.optInt("totalDamageDealt", 0)
                )
            } else null
            val selPos = units.firstOrNull { it.uid == selUid }?.pos
            current.copy(
                active = o.optBoolean("active", true),
                round = o.optInt("round", 1),
                activeFaction = activeFaction,
                playerFaction = playerFaction,
                map = sectorMap,
                units = units,
                selectedUnitUid = selUid,
                isAiThinking = o.optBoolean("isAiThinking", false),
                isCoopMode = o.optBoolean("isCoopMode", true),
                activeCoopPlayerId = activePid,
                activeCoopPlayerName = o.optString("activeCoopPlayerName", ""),
                coopTurnOrder = turnOrder,
                coopTurnIndex = o.optInt("coopTurnIndex", 0),
                playerKills = o.optInt("playerKills", 0),
                enemyKills = o.optInt("enemyKills", 0),
                summary = parsedSummary,
                focusGridPos = if (current.autoFocusCamera && selPos != null) selPos else current.focusGridPos,
                moveModeActive = false,
                movePreview = null,
                chargeModeActive = false,
                chargePreview = null,
                targetPreview = null
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
                        slotMode = runCatching { CoopSlotMode.valueOf(po.getString("slotMode")) }.getOrDefault(CoopSlotMode.LOCAL_PLAYER),
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
