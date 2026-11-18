package com.example.data

import android.content.Context
import com.example.R
import com.example.engine.DefaultCatalog
import com.example.engine.MapGenerator
import com.example.model.BattleSummary
import com.example.model.BattleUnit
import com.example.model.CoopPlayer
import com.example.model.CoopRosterUnit
import com.example.model.CoopSlotMode
import com.example.model.CoopState
import com.example.model.Facing
import com.example.model.Faction
import com.example.model.SectorMap
import com.example.model.UnitArchetype
import com.example.model.UnitTemplate
import com.example.model.WarriorWeaponChoice
import com.example.model.WeaponMode
import com.example.viewmodel.BattleUiState
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

data class OnlineRoomSummary(
    val roomCode: String = "",
    val hostUid: String = "",
    val hostName: String = "",
    val status: String = "LOBBY",
    val memberCount: Int = 1,
    val openForJoin: Boolean = true,
    val updatedAt: Timestamp? = null
)

data class OnlineClientAction(
    val actionId: String = "",
    val userId: String = "",
    val hostUid: String = "",
    val playerId: String = "",
    val actionType: String = "",
    val wx: Float? = null,
    val wy: Float? = null,
    val cm: Float? = null,
    val mode: WeaponMode? = null,
    val left: Boolean? = null
)

data class OnlineRoomSnapshot(
    val roomCode: String = "",
    val hostUid: String = "",
    val hostName: String = "",
    val status: String = "LOBBY",
    val openForJoin: Boolean = true,
    val memberUids: List<String> = emptyList(),
    val spawnAiEnemyIfSameFaction: Boolean = true,
    val players: List<CoopPlayer> = emptyList(),
    val playerUids: Map<String, String> = emptyMap(),
    val assignedUnits: List<CoopRosterUnit> = emptyList(),
    val battleMap: Map<String, Any?>? = null
)

class OnlineCoopRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        try {
            FirebaseFirestore.getInstance(
                context.applicationContext.getString(R.string.firestore_database_id)
            )
        } catch (_: Exception) {
            FirebaseFirestore.getInstance()
        }
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Must sign in with Google before using Online Co-op.")
    }

    fun observeMyRooms(userId: String): Flow<List<OnlineRoomSummary>> {
        val path = "coop_rooms"
        return db.collection(path)
            .whereArrayContains("memberUids", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc -> parseRoomSummary(doc) }
                    .sortedByDescending { it.updatedAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val firestoreError = extractFirestoreRootError(error)
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                }
                throw error
            }
    }

    fun observeRoom(roomCode: String): Flow<OnlineRoomSnapshot?> {
        val path = "coop_rooms/$roomCode"
        return db.collection("coop_rooms")
            .document(roomCode)
            .snapshots()
            .map { doc -> if (doc.exists()) parseRoomSnapshot(doc) else null }
            .catch { error ->
                val firestoreError = extractFirestoreRootError(error)
                if (firestoreError == null && error is CancellationException) throw error
                val uid = auth.currentUser?.uid
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && !uid.isNullOrBlank()) {
                    handleFirestoreError(exceptionToLog, OperationType.GET, path)
                }
                throw error
            }
    }

    fun observeRoomActions(roomCode: String, hostUid: String): Flow<List<OnlineClientAction>> {
        val path = "coop_rooms/$roomCode/actions"
        return db.collection("coop_rooms")
            .document(roomCode)
            .collection("actions")
            .whereEqualTo("hostUid", hostUid)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc -> parseClientAction(doc) }
            }
            .catch { error ->
                val firestoreError = extractFirestoreRootError(error)
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == hostUid) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                }
                throw error
            }
    }

    suspend fun createOnlineRoom(
        hostName: String,
        initialPlayers: List<CoopPlayer>,
        initialUnits: List<CoopRosterUnit>,
        spawnAiIfSameFaction: Boolean
    ): Result<String> {
        var docPath = "coop_rooms"
        return try {
            val uid = requireUserId()
            val cleanHostName = hostName.trim().ifBlank { "Commander 1" }.take(24)
            var roomCode = Random.nextInt(100000, 999999).toString()

            // Try to avoid collision if the random 6-digit code already exists
            for (attempt in 0 until 3) {
                val existing = db.collection("coop_rooms").document(roomCode).get().await()
                if (!existing.exists()) break
                roomCode = Random.nextInt(100000, 999999).toString()
            }

            val docRef = db.collection("coop_rooms").document(roomCode)
            docPath = docRef.path

            val playersList = initialPlayers.take(4).map { p ->
                val pUid = if (p.id == "p1") uid else ""
                val pName = if (p.id == "p1") cleanHostName else p.name.take(24)
                playerToMap(p.copy(name = pName), pUid)
            }
            val unitsList = initialUnits.take(40).map { rosterUnitToMap(it) }

            val payload = mapOf(
                "roomCode" to roomCode,
                "hostUid" to uid,
                "hostName" to cleanHostName,
                "status" to "LOBBY",
                "openForJoin" to true,
                "memberUids" to listOf(uid),
                "spawnAiEnemyIfSameFaction" to spawnAiIfSameFaction,
                "players" to playersList,
                "assignedUnits" to unitsList,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(roomCode)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun joinOnlineRoomByCode(
        roomCode: String,
        preferredSlotId: String,
        joinerName: String
    ): Result<String> {
        val cleanCode = roomCode.trim()
        val docPath = "coop_rooms/$cleanCode"
        return try {
            val uid = requireUserId()
            require(cleanCode.length in 4..12 && cleanCode.all { it.isLetterOrDigit() || it == '_' || it == '-' }) {
                "Invalid room code format. Use 4-12 digits/letters."
            }
            val cleanName = joinerName.trim().ifBlank { "Commander" }.take(24)
            val docRef = db.collection("coop_rooms").document(cleanCode)

            val assignedSlotId = db.runTransaction { tx ->
                val snap = tx.get(docRef)
                if (!snap.exists()) {
                    throw IllegalStateException("Room #$cleanCode not found. Check the room code.")
                }
                val data = snap.data ?: throw IllegalStateException("Room #$cleanCode data is empty.")
                val status = (data["status"] as? String) ?: "LOBBY"
                val openForJoin = (data["openForJoin"] as? Boolean) ?: false
                val members = (data["memberUids"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                val rawPlayers = (data["players"] as? List<*>)?.mapNotNull { it as? Map<*, *> } ?: emptyList()

                if (uid in members) {
                    val existingSlot = rawPlayers.firstOrNull { (it["uid"] as? String) == uid }
                    return@runTransaction (existingSlot?.get("id") as? String) ?: "p1"
                }

                if (status != "LOBBY" || !openForJoin) {
                    throw IllegalStateException("Room #$cleanCode has already started or is closed.")
                }
                if (members.size >= 4) {
                    throw IllegalStateException("Room #$cleanCode is full (4/4 players).")
                }

                // Pick preferred slot if open, else first open non-host slot
                val parsedMaps = rawPlayers.map { row ->
                    row.entries.associate { (k, v) -> k.toString() to v }.toMutableMap()
                }
                val candidate = parsedMaps.firstOrNull {
                    (it["id"] as? String) == preferredSlotId &&
                        ((it["uid"] as? String).isNullOrBlank()) &&
                        ((it["slotMode"] as? String) != CoopSlotMode.OFF.name)
                } ?: parsedMaps.firstOrNull {
                    val pid = it["id"] as? String
                    pid != "p1" &&
                        ((it["uid"] as? String).isNullOrBlank()) &&
                        ((it["slotMode"] as? String) != CoopSlotMode.OFF.name)
                } ?: parsedMaps.firstOrNull {
                    val pid = it["id"] as? String
                    pid != "p1" && ((it["uid"] as? String).isNullOrBlank())
                } ?: throw IllegalStateException("No open player slots left in Room #$cleanCode.")

                val claimedSlotId = (candidate["id"] as? String) ?: "p2"
                candidate["uid"] = uid
                candidate["name"] = cleanName
                candidate["slotMode"] = CoopSlotMode.LAN_CLIENT.name
                candidate["connectedClient"] = true
                candidate["ready"] = true

                tx.update(
                    docRef,
                    mapOf(
                        "memberUids" to (members + uid),
                        "players" to parsedMaps,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                claimedSlotId
            }.await()

            Result.success(assignedSlotId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun leaveOnlineRoom(roomCode: String): Result<Unit> {
        val docPath = "coop_rooms/$roomCode"
        return try {
            val uid = requireUserId()
            if (roomCode.isBlank()) return Result.success(Unit)
            val docRef = db.collection("coop_rooms").document(roomCode)

            db.runTransaction { tx ->
                val snap = tx.get(docRef)
                if (!snap.exists()) return@runTransaction null
                val data = snap.data ?: return@runTransaction null
                val hostUid = (data["hostUid"] as? String) ?: ""
                val status = (data["status"] as? String) ?: "LOBBY"

                if (hostUid == uid) {
                    if (status == "LOBBY") {
                        tx.delete(docRef)
                    } else if (status != "FINISHED") {
                        tx.update(
                            docRef,
                            mapOf(
                                "status" to "FINISHED",
                                "openForJoin" to false,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                } else if (status != "FINISHED") {
                    val members = (data["memberUids"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                    if (uid in members) {
                        val rawPlayers = (data["players"] as? List<*>)?.mapNotNull { it as? Map<*, *> } ?: emptyList()
                        val updatedPlayers = rawPlayers.map { row ->
                            val m = row.entries.associate { (k, v) -> k.toString() to v }.toMutableMap()
                            if ((m["uid"] as? String) == uid) {
                                m["uid"] = ""
                                m["connectedClient"] = false
                            }
                            m
                        }
                        tx.update(
                            docRef,
                            mapOf(
                                "memberUids" to members.filterNot { it == uid },
                                "players" to updatedPlayers,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                }
                null
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateHostLobby(
        roomCode: String,
        coopState: CoopState,
        playerUids: Map<String, String>
    ): Result<Unit> {
        val docPath = "coop_rooms/$roomCode"
        return try {
            requireUserId()
            require(roomCode.isNotBlank()) { "Room code missing." }
            val docRef = db.collection("coop_rooms").document(roomCode)
            val playersList = coopState.players.take(4).map { p ->
                playerToMap(p, playerUids[p.id] ?: "")
            }
            val unitsList = coopState.assignedUnits.take(40).map { rosterUnitToMap(it) }
            docRef.update(
                mapOf(
                    "players" to playersList,
                    "assignedUnits" to unitsList,
                    "spawnAiEnemyIfSameFaction" to coopState.spawnAiEnemyIfSameFaction,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun publishBattleSnapshot(
        roomCode: String,
        battleState: BattleUiState,
        templates: Map<String, UnitTemplate>,
        status: String
    ): Result<Unit> {
        val docPath = "coop_rooms/$roomCode"
        return try {
            requireUserId()
            require(roomCode.isNotBlank()) { "Room code missing." }
            val docRef = db.collection("coop_rooms").document(roomCode)
            val battleMap = serializeBattleToNativeMap(battleState, templates)
            docRef.update(
                mapOf(
                    "status" to status,
                    "openForJoin" to false,
                    "battleState" to battleMap,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun submitClientAction(
        roomCode: String,
        hostUid: String,
        playerId: String,
        actionType: String,
        wx: Float? = null,
        wy: Float? = null,
        cm: Float? = null,
        mode: WeaponMode? = null,
        left: Boolean? = null
    ): Result<Unit> {
        val colPath = "coop_rooms/$roomCode/actions"
        return try {
            val uid = requireUserId()
            require(roomCode.isNotBlank() && hostUid.isNotBlank()) { "Room metadata missing." }
            val docRef = db.collection("coop_rooms").document(roomCode).collection("actions").document()
            val payload = buildMap<String, Any> {
                put("userId", uid)
                put("hostUid", hostUid)
                put("playerId", playerId)
                put("actionType", actionType)
                wx?.let { put("wx", it.toDouble()) }
                wy?.let { put("wy", it.toDouble()) }
                cm?.let { put("cm", it.toDouble()) }
                mode?.let { put("mode", it.name) }
                left?.let { put("left", it) }
                put("createdAt", FieldValue.serverTimestamp())
            }
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, colPath)
            Result.failure(e)
        }
    }

    suspend fun consumeAction(roomCode: String, actionId: String): Result<Unit> {
        val docPath = "coop_rooms/$roomCode/actions/$actionId"
        return try {
            requireUserId()
            db.collection("coop_rooms").document(roomCode).collection("actions").document(actionId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    private fun parseRoomSummary(doc: DocumentSnapshot): OnlineRoomSummary? {
        val data = doc.data ?: return null
        val members = (data["memberUids"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
        return OnlineRoomSummary(
            roomCode = (data["roomCode"] as? String) ?: doc.id,
            hostUid = (data["hostUid"] as? String) ?: "",
            hostName = (data["hostName"] as? String) ?: "Host",
            status = (data["status"] as? String) ?: "LOBBY",
            memberCount = members.size,
            openForJoin = (data["openForJoin"] as? Boolean) ?: true,
            updatedAt = doc.getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseRoomSnapshot(doc: DocumentSnapshot): OnlineRoomSnapshot? {
        val data = doc.data ?: return null
        val members = (data["memberUids"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
        val rawPlayers = (data["players"] as? List<*>)?.mapNotNull { it as? Map<*, *> } ?: emptyList()
        val uidsByPlayer = mutableMapOf<String, String>()
        val parsedPlayers = rawPlayers.mapNotNull { row ->
            val id = row["id"] as? String ?: return@mapNotNull null
            val name = (row["name"] as? String) ?: "Commander"
            val factionRaw = (row["faction"] as? String) ?: Faction.MARINES.name
            val faction = enumValues<Faction>().firstOrNull { it.name == factionRaw } ?: Faction.MARINES
            val modeRaw = (row["slotMode"] as? String) ?: CoopSlotMode.LOCAL_PLAYER.name
            val slotMode = enumValues<CoopSlotMode>().firstOrNull { it.name == modeRaw } ?: CoopSlotMode.LOCAL_PLAYER
            val ready = (row["ready"] as? Boolean) ?: true
            val connected = (row["connectedClient"] as? Boolean) ?: false
            val pUid = (row["uid"] as? String) ?: ""
            if (pUid.isNotBlank()) uidsByPlayer[id] = pUid
            CoopPlayer(id = id, name = name, faction = faction, slotMode = slotMode, ready = ready, connectedClient = connected)
        }
        val rawUnits = (data["assignedUnits"] as? List<*>)?.mapNotNull { it as? Map<*, *> } ?: emptyList()
        val parsedUnits = rawUnits.mapNotNull { row ->
            val entryId = row["entryId"] as? String ?: return@mapNotNull null
            val ownerId = row["ownerPlayerId"] as? String ?: return@mapNotNull null
            val tplId = row["templateId"] as? String ?: return@mapNotNull null
            CoopRosterUnit(entryId = entryId, ownerPlayerId = ownerId, templateId = tplId)
        }
        val battleRaw = data["battleState"] as? Map<*, *>
        val battleMap = battleRaw?.entries?.associate { (k, v) -> k.toString() to v }

        return OnlineRoomSnapshot(
            roomCode = (data["roomCode"] as? String) ?: doc.id,
            hostUid = (data["hostUid"] as? String) ?: "",
            hostName = (data["hostName"] as? String) ?: "Host",
            status = (data["status"] as? String) ?: "LOBBY",
            openForJoin = (data["openForJoin"] as? Boolean) ?: true,
            memberUids = members,
            spawnAiEnemyIfSameFaction = (data["spawnAiEnemyIfSameFaction"] as? Boolean) ?: true,
            players = parsedPlayers,
            playerUids = uidsByPlayer,
            assignedUnits = parsedUnits,
            battleMap = battleMap
        )
    }

    private fun parseClientAction(doc: DocumentSnapshot): OnlineClientAction? {
        val data = doc.data ?: return null
        val userId = (data["userId"] as? String) ?: return null
        val hostUid = (data["hostUid"] as? String) ?: return null
        val playerId = (data["playerId"] as? String) ?: return null
        val actionType = (data["actionType"] as? String) ?: return null
        val modeStr = data["mode"] as? String
        val mode = modeStr?.let { m -> enumValues<WeaponMode>().firstOrNull { it.name == m } }
        return OnlineClientAction(
            actionId = doc.id,
            userId = userId,
            hostUid = hostUid,
            playerId = playerId,
            actionType = actionType,
            wx = (data["wx"] as? Number)?.toFloat(),
            wy = (data["wy"] as? Number)?.toFloat(),
            cm = (data["cm"] as? Number)?.toFloat(),
            mode = mode,
            left = data["left"] as? Boolean
        )
    }

    private fun playerToMap(p: CoopPlayer, uid: String): Map<String, Any> = mapOf(
        "id" to p.id,
        "name" to p.name.take(24),
        "faction" to p.faction.name,
        "slotMode" to p.slotMode.name,
        "ready" to p.ready,
        "connectedClient" to p.connectedClient,
        "uid" to uid
    )

    private fun rosterUnitToMap(u: CoopRosterUnit): Map<String, Any> = mapOf(
        "entryId" to u.entryId,
        "ownerPlayerId" to u.ownerPlayerId,
        "templateId" to u.templateId
    )

    private fun serializeBattleToNativeMap(
        state: BattleUiState,
        templates: Map<String, UnitTemplate>
    ): Map<String, Any> {
        val unitsList = state.units.map { u ->
            mapOf(
                "uid" to u.uid,
                "templateId" to u.template.id,
                "faction" to u.faction.name,
                "x" to u.x,
                "y" to u.y,
                "facing" to u.facing.name,
                "totalWounds" to u.totalWounds,
                "maxTotalWounds" to u.maxTotalWounds,
                "moveCmLeft" to u.moveCmLeft.toDouble(),
                "hasAdvanced" to u.hasAdvanced,
                "hasCharged" to u.hasCharged,
                "hasShot" to u.hasShot,
                "hasFought" to u.hasFought,
                "worldX" to u.worldX.toDouble(),
                "worldY" to u.worldY.toDouble(),
                "facingDeg" to u.facingDeg.toDouble(),
                "skillCooldownLeft" to u.skillCooldownLeft,
                "pistolCooldownLeft" to u.pistolCooldownLeft,
                "buffTurnsLeft" to u.buffTurnsLeft,
                "activeBuffLabel" to (u.activeBuffLabel ?: ""),
                "damageReductionTurns" to u.damageReductionTurns,
                "auraBuffed" to u.auraBuffed,
                "kills" to u.kills,
                "damageDealt" to u.damageDealt,
                "ownerPlayerId" to u.ownerPlayerId,
                "ownerPlayerName" to u.ownerPlayerName
            )
        }
        val orderList = state.coopTurnOrder.map { p ->
            mapOf(
                "id" to p.id,
                "name" to p.name,
                "faction" to p.faction.name,
                "slotMode" to p.slotMode.name
            )
        }
        val tplList = templates.values.map { t ->
            mapOf(
                "id" to t.id,
                "name" to t.name,
                "faction" to t.faction.name,
                "archetype" to t.archetype.name,
                "role" to t.role,
                "points" to t.points,
                "squadSize" to t.squadSize,
                "woundsPerModel" to t.woundsPerModel,
                "moveCm" to t.moveCm,
                "toughness" to t.toughness,
                "save" to t.save,
                "invulnSave" to (t.invulnSave ?: 0),
                "armorPen" to t.armorPen,
                "strength" to t.strength,
                "damage" to t.damage,
                "attacksPerModel" to t.attacksPerModel,
                "range" to t.range,
                "ballisticSkill" to t.ballisticSkill,
                "weaponSkill" to t.weaponSkill,
                "mainWeaponName" to t.mainWeaponName,
                "mainWeaponAssault" to t.mainWeaponAssault,
                "mainWeaponPistol" to t.mainWeaponPistol,
                "pistolWeaponName" to t.pistolWeaponName,
                "pistolAttacks" to t.pistolAttacks,
                "pistolDamage" to t.pistolDamage,
                "pistolStrength" to t.pistolStrength,
                "pistolArmorPen" to t.pistolArmorPen,
                "pistolRange" to t.pistolRange,
                "pistolCooldown" to t.pistolCooldown,
                "weaponChoice" to (t.weaponChoice?.name ?: "")
            )
        }
        val summaryMap = state.summary?.let { sum ->
            mapOf(
                "winner" to sum.winner.name,
                "playerWon" to sum.playerWon,
                "rounds" to sum.rounds,
                "playerKills" to sum.playerKills,
                "enemyKills" to sum.enemyKills,
                "totalDamageDealt" to sum.totalDamageDealt
            )
        }
        return buildMap {
            put("active", state.active)
            put("round", state.round)
            put("activeFaction", state.activeFaction.name)
            put("playerFaction", state.playerFaction.name)
            put("mapId", state.map.id)
            put("selectedUnitUid", state.selectedUnitUid ?: "")
            put("aiThinking", state.isAiThinking)
            put("coopMode", state.isCoopMode)
            put("activeCoopPlayerId", state.activeCoopPlayerId ?: "")
            put("activeCoopPlayerName", state.activeCoopPlayerName)
            put("coopTurnIndex", state.coopTurnIndex)
            put("playerKills", state.playerKills)
            put("enemyKills", state.enemyKills)
            put("units", unitsList)
            put("coopTurnOrder", orderList)
            put("templates", tplList)
            summaryMap?.let { put("summary", it) }
        }
    }

    fun applyBattleMapToState(battleMap: Map<String, Any?>, current: BattleUiState): BattleUiState? {
        return try {
            val baseTemplates = DefaultCatalog.defaultTemplates.associateBy { it.id }.toMutableMap()
            val tplRows = (battleMap["templates"] as? List<*>)?.mapNotNull { it as? Map<*, *> } ?: emptyList()
            for (row in tplRows) {
                val id = row["id"] as? String ?: continue
                val base = baseTemplates[id] ?: DefaultCatalog.defaultTemplates.first()
                val inv = (row["invulnSave"] as? Number)?.toInt() ?: 0
                val factionRaw = (row["faction"] as? String) ?: base.faction.name
                val archRaw = (row["archetype"] as? String) ?: base.archetype.name
                val wwcRaw = (row["weaponChoice"] as? String).orEmpty()
                val parsedChoice = if (wwcRaw.isBlank()) base.weaponChoice else {
                    enumValues<WarriorWeaponChoice>().firstOrNull { it.name == wwcRaw } ?: base.weaponChoice
                }
                baseTemplates[id] = base.copy(
                    id = id,
                    name = (row["name"] as? String) ?: base.name,
                    faction = enumValues<Faction>().firstOrNull { it.name == factionRaw } ?: base.faction,
                    archetype = enumValues<UnitArchetype>().firstOrNull { it.name == archRaw } ?: base.archetype,
                    role = (row["role"] as? String) ?: base.role,
                    points = (row["points"] as? Number)?.toInt() ?: base.points,
                    squadSize = (row["squadSize"] as? Number)?.toInt() ?: base.squadSize,
                    woundsPerModel = (row["woundsPerModel"] as? Number)?.toInt() ?: base.woundsPerModel,
                    moveCm = (row["moveCm"] as? Number)?.toInt() ?: base.moveCm,
                    toughness = (row["toughness"] as? Number)?.toInt() ?: base.toughness,
                    save = (row["save"] as? Number)?.toInt() ?: base.save,
                    invulnSave = if (inv > 0) inv else null,
                    armorPen = (row["armorPen"] as? Number)?.toInt() ?: base.armorPen,
                    strength = (row["strength"] as? Number)?.toInt() ?: base.strength,
                    damage = (row["damage"] as? Number)?.toInt() ?: base.damage,
                    attacksPerModel = (row["attacksPerModel"] as? Number)?.toInt() ?: base.attacksPerModel,
                    range = (row["range"] as? Number)?.toInt() ?: base.range,
                    ballisticSkill = (row["ballisticSkill"] as? Number)?.toInt() ?: base.ballisticSkill,
                    weaponSkill = (row["weaponSkill"] as? Number)?.toInt() ?: base.weaponSkill,
                    mainWeaponName = (row["mainWeaponName"] as? String) ?: base.mainWeaponName,
                    mainWeaponAssault = (row["mainWeaponAssault"] as? Boolean) ?: base.mainWeaponAssault,
                    mainWeaponPistol = (row["mainWeaponPistol"] as? Boolean) ?: base.mainWeaponPistol,
                    pistolWeaponName = (row["pistolWeaponName"] as? String) ?: base.pistolWeaponName,
                    pistolAttacks = (row["pistolAttacks"] as? Number)?.toInt() ?: base.pistolAttacks,
                    pistolDamage = (row["pistolDamage"] as? Number)?.toInt() ?: base.pistolDamage,
                    pistolStrength = (row["pistolStrength"] as? Number)?.toInt() ?: base.pistolStrength,
                    pistolArmorPen = (row["pistolArmorPen"] as? Number)?.toInt() ?: base.pistolArmorPen,
                    pistolRange = (row["pistolRange"] as? Number)?.toInt() ?: base.pistolRange,
                    pistolCooldown = (row["pistolCooldown"] as? Number)?.toInt() ?: base.pistolCooldown,
                    weaponChoice = parsedChoice
                )
            }

            val mapId = (battleMap["mapId"] as? Number)?.toLong() ?: 1L
            val sectorMap = if (current.map.id == mapId) current.map else SectorMap(mapId, MapGenerator.generateSectorMap(mapId))

            val unitRows = (battleMap["units"] as? List<*>)?.mapNotNull { it as? Map<*, *> } ?: emptyList()
            val units = unitRows.mapNotNull { row ->
                val uid = row["uid"] as? String ?: return@mapNotNull null
                val tplId = row["templateId"] as? String ?: return@mapNotNull null
                val tpl = baseTemplates[tplId] ?: DefaultCatalog.defaultTemplates.first()
                val fRaw = (row["faction"] as? String) ?: tpl.faction.name
                val fac = enumValues<Faction>().firstOrNull { it.name == fRaw } ?: tpl.faction
                val faceRaw = (row["facing"] as? String) ?: Facing.EAST.name
                val facing = enumValues<Facing>().firstOrNull { it.name == faceRaw } ?: Facing.EAST
                val x = (row["x"] as? Number)?.toInt() ?: 1
                val y = (row["y"] as? Number)?.toInt() ?: 1
                val buffLabel = (row["activeBuffLabel"] as? String).orEmpty()
                BattleUnit(
                    uid = uid,
                    template = tpl,
                    faction = fac,
                    x = x,
                    y = y,
                    facing = facing,
                    totalWounds = (row["totalWounds"] as? Number)?.toInt() ?: tpl.maxTotalWounds,
                    maxTotalWounds = (row["maxTotalWounds"] as? Number)?.toInt() ?: tpl.maxTotalWounds,
                    moveCmLeft = (row["moveCmLeft"] as? Number)?.toFloat() ?: tpl.moveCm.toFloat(),
                    hasAdvanced = (row["hasAdvanced"] as? Boolean) ?: false,
                    hasCharged = (row["hasCharged"] as? Boolean) ?: false,
                    hasShot = (row["hasShot"] as? Boolean) ?: false,
                    hasFought = (row["hasFought"] as? Boolean) ?: false,
                    worldX = (row["worldX"] as? Number)?.toFloat() ?: x.toFloat(),
                    worldY = (row["worldY"] as? Number)?.toFloat() ?: y.toFloat(),
                    facingDeg = (row["facingDeg"] as? Number)?.toFloat() ?: facing.yawDegrees(),
                    skillCooldownLeft = (row["skillCooldownLeft"] as? Number)?.toInt() ?: 0,
                    pistolCooldownLeft = (row["pistolCooldownLeft"] as? Number)?.toInt() ?: 0,
                    buffTurnsLeft = (row["buffTurnsLeft"] as? Number)?.toInt() ?: 0,
                    activeBuffLabel = buffLabel.ifBlank { null },
                    damageReductionTurns = (row["damageReductionTurns"] as? Number)?.toInt() ?: 0,
                    auraBuffed = (row["auraBuffed"] as? Boolean) ?: false,
                    kills = (row["kills"] as? Number)?.toInt() ?: 0,
                    damageDealt = (row["damageDealt"] as? Number)?.toInt() ?: 0,
                    ownerPlayerId = (row["ownerPlayerId"] as? String) ?: "PLAYER",
                    ownerPlayerName = (row["ownerPlayerName"] as? String) ?: ""
                )
            }

            val orderRows = (battleMap["coopTurnOrder"] as? List<*>)?.mapNotNull { it as? Map<*, *> } ?: emptyList()
            val turnOrder = orderRows.mapNotNull { row ->
                val id = row["id"] as? String ?: return@mapNotNull null
                val fRaw = (row["faction"] as? String) ?: Faction.MARINES.name
                val mRaw = (row["slotMode"] as? String) ?: CoopSlotMode.LOCAL_PLAYER.name
                CoopPlayer(
                    id = id,
                    name = (row["name"] as? String) ?: "Commander",
                    faction = enumValues<Faction>().firstOrNull { it.name == fRaw } ?: Faction.MARINES,
                    slotMode = enumValues<CoopSlotMode>().firstOrNull { it.name == mRaw } ?: CoopSlotMode.LOCAL_PLAYER
                )
            }

            val sumRaw = battleMap["summary"] as? Map<*, *>
            val parsedSummary = if (sumRaw != null) {
                val wRaw = (sumRaw["winner"] as? String) ?: Faction.MARINES.name
                BattleSummary(
                    winner = enumValues<Faction>().firstOrNull { it.name == wRaw } ?: Faction.MARINES,
                    playerWon = (sumRaw["playerWon"] as? Boolean) ?: true,
                    rounds = (sumRaw["rounds"] as? Number)?.toInt() ?: 1,
                    playerKills = (sumRaw["playerKills"] as? Number)?.toInt() ?: 0,
                    enemyKills = (sumRaw["enemyKills"] as? Number)?.toInt() ?: 0,
                    totalDamageDealt = (sumRaw["totalDamageDealt"] as? Number)?.toInt() ?: 0
                )
            } else null

            val activeFacRaw = (battleMap["activeFaction"] as? String) ?: Faction.MARINES.name
            val playerFacRaw = (battleMap["playerFaction"] as? String) ?: Faction.MARINES.name
            val selUid = (battleMap["selectedUnitUid"] as? String)?.ifBlank { null }
            val activePid = (battleMap["activeCoopPlayerId"] as? String)?.ifBlank { null }
            val selPos = units.firstOrNull { it.uid == selUid }?.pos

            current.copy(
                active = (battleMap["active"] as? Boolean) ?: true,
                round = (battleMap["round"] as? Number)?.toInt() ?: 1,
                activeFaction = enumValues<Faction>().firstOrNull { it.name == activeFacRaw } ?: Faction.MARINES,
                playerFaction = enumValues<Faction>().firstOrNull { it.name == playerFacRaw } ?: Faction.MARINES,
                map = sectorMap,
                units = units,
                selectedUnitUid = selUid,
                isAiThinking = (battleMap["aiThinking"] as? Boolean) ?: false,
                isCoopMode = (battleMap["coopMode"] as? Boolean) ?: true,
                activeCoopPlayerId = activePid,
                activeCoopPlayerName = (battleMap["activeCoopPlayerName"] as? String) ?: "",
                coopTurnIndex = (battleMap["coopTurnIndex"] as? Number)?.toInt() ?: 0,
                playerKills = (battleMap["playerKills"] as? Number)?.toInt() ?: 0,
                enemyKills = (battleMap["enemyKills"] as? Number)?.toInt() ?: 0,
                coopTurnOrder = turnOrder,
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
}
