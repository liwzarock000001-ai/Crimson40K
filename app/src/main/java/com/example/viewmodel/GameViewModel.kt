package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.BattleRecordEntity
import com.example.data.GameRepository
import com.example.data.SavedConfigEntity
import com.example.data.SavedSkirmishBattleEntity
import com.example.engine.AiController
import com.example.engine.AiDecision
import com.example.engine.BattleStateSerializer
import com.example.engine.DefaultCatalog
import com.example.engine.MapGenerator
import com.example.engine.SoundEffects
import com.example.engine.SoundType
import com.example.engine.TacticalRules
import com.example.model.AppScreen
import com.example.model.BattleSummary
import com.example.model.BattleUnit
import com.example.model.ChargePreview
import com.example.model.CoopNetworkRole
import com.example.model.CoopPlayer
import com.example.model.CoopRosterUnit
import com.example.model.CoopSlotMode
import com.example.model.CoopState
import com.example.model.Facing
import com.example.model.Faction
import com.example.model.FloatingTextVfx
import com.example.model.GridPos
import com.example.model.MovePreview
import com.example.model.ProjectileVfx
import com.example.model.RosterSlot
import com.example.model.SectorMap
import com.example.model.SettingsState
import com.example.model.TargetPreview
import com.example.model.TerrainType
import com.example.model.TurnSplashData
import com.example.model.UnitArchetype
import com.example.model.UnitTemplate
import com.example.model.WarriorWeaponChoice
import com.example.model.WeaponMode
import com.example.model.WorldPos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import org.json.JSONObject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.random.Random

@Immutable
data class SetupUiState(
    val playerFaction: Faction = Faction.MARINES,
    val pointLimit: Int = 2000,
    val templates: Map<String, UnitTemplate> = DefaultCatalog.defaultTemplates.associateBy { it.id },
    val marineRoster: List<RosterSlot> = DefaultCatalog.defaultMarineRoster(),
    val xenosRoster: List<RosterSlot> = DefaultCatalog.defaultXenosRoster(),
    val editingTemplateId: String? = null,
    val statusMessage: String? = null
) {
    val marinePoints: Int
        get() = marineRoster.sumOf { templates[it.templateId]?.points ?: 0 }
    val xenosPoints: Int
        get() = xenosRoster.sumOf { templates[it.templateId]?.points ?: 0 }
    val validationWarning: String?
        get() = when {
            marineRoster.isEmpty() -> "Ultramarines require at least 1 squad."
            xenosRoster.isEmpty() -> "Alien Swarm requires at least 1 squad."
            marinePoints > pointLimit -> "Ultramarines exceed $pointLimit pt limit."
            xenosPoints > pointLimit -> "Alien Swarm exceeds $pointLimit pt limit."
            else -> null
        }
    val canDeploy: Boolean get() = validationWarning == null
}

@Immutable
data class BattleUiState(
    val active: Boolean = false,
    val round: Int = 1,
    val activeFaction: Faction = Faction.MARINES,
    val playerFaction: Faction = Faction.MARINES,
    val map: SectorMap = SectorMap(1L, MapGenerator.generateSectorMap(1L)),
    val units: List<BattleUnit> = emptyList(),
    val selectedUnitUid: String? = null,
    val weaponMode: WeaponMode = WeaponMode.MAIN,
    val moveModeActive: Boolean = false,
    val cmStepChoice: Float = 3f,
    val movePreview: MovePreview? = null,
    val reachableTiles: Map<GridPos, Int> = emptyMap(),
    val moveParents: Map<GridPos, GridPos> = emptyMap(),
    val attackableUids: Set<String> = emptySet(),
    val targetPreview: TargetPreview? = null,
    val chargeModeActive: Boolean = false,
    val chargeableUids: Set<String> = emptySet(),
    val chargePreview: ChargePreview? = null,
    val projectiles: List<ProjectileVfx> = emptyList(),
    val floatingTexts: List<FloatingTextVfx> = emptyList(),
    val autoFocusCamera: Boolean = true,
    val focusGridPos: GridPos? = null,
    val centerTrigger: Int = 0,
    val soundEnabled: Boolean = true,
    val isAiThinking: Boolean = false,
    val isAnimating: Boolean = false,
    val turnSplash: TurnSplashData? = null,
    val summary: BattleSummary? = null,
    val unitCardCollapsed: Boolean = false,
    val playerKills: Int = 0,
    val enemyKills: Int = 0,
    val graphicsLevel: Int = 3,
    val screenBrightness: Float = 1.0f,
    val isCoopMode: Boolean = false,
    val coopTurnOrder: List<CoopPlayer> = emptyList(),
    val coopTurnIndex: Int = 0,
    val activeCoopPlayerId: String? = null,
    val activeCoopPlayerName: String = "",
    val localControlledPlayerId: String? = null,
    val saveBannerMessage: String? = null
) {
    val isPlayerTurn: Boolean
        get() = !isAiThinking && summary == null && if (!isCoopMode) {
            activeFaction == playerFaction
        } else {
            activeCoopPlayerId != null && (localControlledPlayerId == null || localControlledPlayerId == activeCoopPlayerId)
        }
    val selectedUnit: BattleUnit? get() = units.firstOrNull { it.uid == selectedUnitUid && it.isAlive }
    val marineAliveModels: Int get() = units.filter { it.faction == Faction.MARINES && it.isAlive }.sumOf { it.aliveModels }
    val xenosAliveModels: Int get() = units.filter { it.faction == Faction.XENOS && it.isAlive }.sumOf { it.aliveModels }

    fun canCommandUnit(unit: BattleUnit): Boolean {
        if (!unit.isAlive || !isPlayerTurn) return false
        return if (!isCoopMode) {
            unit.faction == playerFaction
        } else {
            unit.ownerPlayerId == activeCoopPlayerId && unit.faction == activeFaction
        }
    }
}

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("crimson_settings", Context.MODE_PRIVATE)
    private val repository: GameRepository
    private val sound = SoundEffects()

    private val _setupState = MutableStateFlow(SetupUiState())
    val setupState: StateFlow<SetupUiState> = _setupState.asStateFlow()

    private val _battleState = MutableStateFlow(BattleUiState())
    val battleState: StateFlow<BattleUiState> = _battleState.asStateFlow()

    private val _currentScreen = MutableStateFlow(AppScreen.MAIN_MENU)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _coopState = MutableStateFlow(CoopState())
    val coopState: StateFlow<CoopState> = _coopState.asStateFlow()

    private val _settingsState = MutableStateFlow(
        SettingsState(
            soundVolume = prefs.getFloat("sound_volume", 0.8f).coerceIn(0f, 1f),
            sfxEnabled = prefs.getBoolean("sfx_enabled", true),
            graphicsLevel = prefs.getInt("graphics_level", 3).coerceIn(1, 4),
            screenBrightness = prefs.getFloat("screen_brightness", 1.0f).coerceIn(0.5f, 1.5f)
        )
    )
    val settingsState: StateFlow<SettingsState> = _settingsState.asStateFlow()

    val savedConfig: StateFlow<SavedConfigEntity?>
    val savedSkirmishBattle: StateFlow<SavedSkirmishBattleEntity?>

    private var nextIdCounter = 100L
    private var aiJob: Job? = null
    private var actionJob: Job? = null

    // Cloud Online Co-op bridge callbacks (wired in MainActivity only after Google Sign-In)
    var onlineLobbySyncPublisher: ((CoopState) -> Unit)? = null
    var onlineHostStatePublisher: ((BattleUiState, Map<String, UnitTemplate>) -> Unit)? = null
    var onlineClientActionSender: ((actionType: String, wx: Float?, wy: Float?, cm: Float?, mode: WeaponMode?, left: Boolean?) -> Unit)? = null

    init {
        val dao = AppDatabase.getInstance(application).gameDao()
        repository = GameRepository(dao)
        savedConfig = repository.savedConfig
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
        savedSkirmishBattle = repository.savedSkirmishBattle
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

        val initialSettings = _settingsState.value
        sound.enabled = initialSettings.sfxEnabled
        sound.volume = initialSettings.soundVolume
        _battleState.update {
            it.copy(
                soundEnabled = initialSettings.sfxEnabled,
                graphicsLevel = initialSettings.graphicsLevel,
                screenBrightness = initialSettings.screenBrightness
            )
        }
        loadSavedSetup()
    }

    fun setScreen(screen: AppScreen) {
        sound.play(SoundType.CLICK)
        _currentScreen.value = screen
    }

    fun loadSavedSetupAndOpen() {
        sound.play(SoundType.CLICK)
        viewModelScope.launch {
            val saved = repository.getSavedConfigOnce()
            if (saved != null) {
                val faction = runCatching { Faction.valueOf(saved.playerFaction) }.getOrDefault(Faction.MARINES)
                val templates = DefaultCatalog.deserializeTemplates(saved.customTemplatesJson)
                val marines = DefaultCatalog.deserializeRoster(saved.marineRosterJson, DefaultCatalog.defaultMarineRoster())
                val xenos = DefaultCatalog.deserializeRoster(saved.xenosRosterJson, DefaultCatalog.defaultXenosRoster())
                _setupState.update {
                    it.copy(
                        playerFaction = faction,
                        pointLimit = saved.pointLimit.coerceIn(1000, 5000),
                        templates = templates,
                        marineRoster = marines,
                        xenosRoster = xenos,
                        statusMessage = "Loaded saved Skirmish roster."
                    )
                }
            }
            _currentScreen.value = AppScreen.SKIRMISH_SETUP
        }
    }

    fun saveSkirmishBattleToDatabase() {
        val state = _battleState.value
        if (!state.active || state.isCoopMode || state.summary != null) return
        sound.play(SoundType.CLICK)
        val templates = _setupState.value.templates
        viewModelScope.launch {
            repository.saveSkirmishBattle(
                SavedSkirmishBattleEntity(
                    round = state.round,
                    playerFaction = state.playerFaction.name,
                    activeFaction = state.activeFaction.name,
                    mapId = state.map.id,
                    unitsJson = BattleStateSerializer.serializeUnits(state.units),
                    templatesJson = DefaultCatalog.serializeTemplates(templates),
                    playerKills = state.playerKills,
                    enemyKills = state.enemyKills,
                    marineModelsAlive = state.marineAliveModels,
                    xenosModelsAlive = state.xenosAliveModels
                )
            )
            _battleState.update { it.copy(saveBannerMessage = "SKIRMISH SAVED (ROUND ${state.round})") }
            delay(2200)
            _battleState.update { it.copy(saveBannerMessage = null) }
        }
    }

    fun loadSavedSkirmishBattle() {
        sound.play(SoundType.CLICK)
        viewModelScope.launch {
            val saved = repository.getSavedBattleOnce() ?: return@launch
            aiJob?.cancel()
            actionJob?.cancel()
            val templates = DefaultCatalog.deserializeTemplates(saved.templatesJson)
            val units = BattleStateSerializer.deserializeUnits(saved.unitsJson, templates)
            if (units.isEmpty()) return@launch
            val playerFaction = runCatching { Faction.valueOf(saved.playerFaction) }.getOrDefault(Faction.MARINES)
            val activeFaction = runCatching { Faction.valueOf(saved.activeFaction) }.getOrDefault(playerFaction)
            val rawMap = MapGenerator.generateSectorMap(saved.mapId)
            val map = SectorMap(saved.mapId, rawMap)
            val firstUnit = units.firstOrNull { it.faction == playerFaction && it.isAlive }
            val settings = _settingsState.value

            val loadedState = BattleUiState(
                active = true,
                round = saved.round.coerceAtLeast(1),
                activeFaction = activeFaction,
                playerFaction = playerFaction,
                map = map,
                units = TacticalRules.applyAuras(units),
                selectedUnitUid = firstUnit?.uid,
                autoFocusCamera = true,
                focusGridPos = firstUnit?.pos,
                soundEnabled = settings.sfxEnabled,
                graphicsLevel = settings.graphicsLevel,
                screenBrightness = settings.screenBrightness,
                playerKills = saved.playerKills,
                enemyKills = saved.enemyKills,
                isCoopMode = false,
                turnSplash = TurnSplashData(
                    round = saved.round,
                    activeFaction = activeFaction,
                    isPlayerTurn = activeFaction == playerFaction,
                    marineModelsAlive = units.filter { it.faction == Faction.MARINES && it.isAlive }.sumOf { it.aliveModels },
                    xenosModelsAlive = units.filter { it.faction == Faction.XENOS && it.isAlive }.sumOf { it.aliveModels },
                    playerKills = saved.playerKills,
                    playerLosses = saved.enemyKills,
                    commanderLabel = "SAVED SKIRMISH RESUMED"
                )
            )
            _battleState.value = recomputeSelectionOverlays(loadedState)
            _currentScreen.value = AppScreen.SKIRMISH_BATTLE
            scheduleSplashAutoDismiss()
        }
    }

    // --- CO-OP ROOM GATEWAY, LOBBY & HOST AUTHORITY ACTIONS ---
    fun updateHostCommanderName(name: String) {
        _coopState.update { it.copy(hostCommanderName = name.take(20)) }
    }

    fun updateJoinCommanderName(name: String) {
        _coopState.update { it.copy(joinCommanderName = name.take(20)) }
    }

    fun updateJoinRoomCode(code: String) {
        _coopState.update { it.copy(joinRoomCodeInput = code.take(24), lobbyError = null) }
    }

    fun enterOnlineRoom(roomCode: String, assignedSlot: String, isHost: Boolean) {
        sound.play(SoundType.CLICK)
        _coopState.update { st ->
            val updatedPlayers = if (isHost) {
                val hostName = st.hostCommanderName.trim().ifBlank { "Commander 1" }
                st.players.map { p ->
                    when (p.id) {
                        "p1" -> p.copy(name = hostName, slotMode = CoopSlotMode.HOST, connectedClient = true)
                        else -> p.copy(
                            slotMode = if (p.slotMode == CoopSlotMode.OFF) CoopSlotMode.OFF else CoopSlotMode.LAN_CLIENT,
                            connectedClient = false
                        )
                    }
                }
            } else {
                st.players
            }
            st.copy(
                inRoom = true,
                roomCode = roomCode,
                networkRole = if (isHost) CoopNetworkRole.LOCAL_OR_HOST else CoopNetworkRole.CLIENT,
                serverRunning = isHost,
                clientConnected = !isHost,
                localPlayerId = assignedSlot,
                selectedConfigPlayerId = if (isHost) "p1" else assignedSlot,
                lobbyError = null,
                players = updatedPlayers,
                networkStatus = if (isHost) "Cloud Room #$roomCode Live" else "Connected to Cloud Room #$roomCode"
            )
        }
    }

    fun applyOnlineRoomSnapshot(
        roomCode: String,
        hostName: String,
        players: List<CoopPlayer>,
        assignedUnits: List<CoopRosterUnit>,
        spawnAi: Boolean,
        status: String
    ) {
        if (status == "FINISHED") return
        _coopState.update { current ->
            val isHost = current.networkRole == CoopNetworkRole.LOCAL_OR_HOST
            val resolvedPlayers = if (players.isNotEmpty()) players else current.players
            val joinedPeers = resolvedPlayers.count { !it.isHost && it.connectedClient }
            val nextStatusText = if (isHost) {
                if (joinedPeers > 0) "Cloud Room #$roomCode • $joinedPeers online ally" else "Cloud Room #$roomCode Live"
            } else {
                "Cloud Room #$roomCode • $status"
            }
            current.copy(
                inRoom = true,
                roomCode = roomCode,
                hostCommanderName = hostName.ifBlank { current.hostCommanderName },
                players = resolvedPlayers,
                assignedUnits = assignedUnits,
                spawnAiEnemyIfSameFaction = spawnAi,
                networkStatus = nextStatusText
            )
        }
    }

    fun leaveCoopRoom() {
        sound.play(SoundType.CLICK)
        resetOnlineCoopState()
    }

    fun resetOnlineCoopState(errorMessage: String? = null) {
        _coopState.update {
            it.copy(
                inRoom = false,
                networkRole = CoopNetworkRole.LOCAL_OR_HOST,
                serverRunning = false,
                clientConnected = false,
                localPlayerId = "p1",
                lobbyError = errorMessage,
                networkStatus = "Ready for long-distance Online Co-op"
            )
        }
        if (_battleState.value.isCoopMode && _battleState.value.active) {
            aiJob?.cancel()
            actionJob?.cancel()
            _battleState.update { st -> st.copy(active = false, summary = null, isAiThinking = false, isAnimating = false) }
        }
    }

    private fun broadcastLobbyIfHost() {
        val st = _coopState.value
        if (st.inRoom && st.networkRole == CoopNetworkRole.LOCAL_OR_HOST && st.serverRunning) {
            onlineLobbySyncPublisher?.invoke(st)
        }
    }

    fun updateCoopPlayerFaction(playerId: String, faction: Faction) {
        sound.play(SoundType.CLICK)
        _coopState.update { state ->
            val currentPlayer = state.players.firstOrNull { it.id == playerId }
            val updated = state.players.map { p ->
                if (p.id == playerId) p.copy(faction = faction) else p
            }
            val updatedUnits = if (currentPlayer != null && currentPlayer.faction != faction) {
                val others = state.assignedUnits.filterNot { it.ownerPlayerId == playerId }
                val starterIds = if (faction == Faction.MARINES) {
                    listOf("intercessors", "hellblasters")
                } else {
                    listOf("warriors", "termagants")
                }
                val replacements = starterIds.map { tid ->
                    CoopRosterUnit("c_${nextIdCounter++}", playerId, tid)
                }
                others + replacements
            } else {
                state.assignedUnits
            }
            state.copy(players = updated, assignedUnits = updatedUnits, selectedConfigPlayerId = playerId)
        }
        broadcastLobbyIfHost()
    }

    fun cycleCoopPlayerSlotMode(playerId: String) {
        if (playerId == "p1") return // Host slot is always HOST
        sound.play(SoundType.CLICK)
        _coopState.update { state ->
            val updated = state.players.map { p ->
                if (p.id != playerId) p
                else {
                    val nextMode = when (p.slotMode) {
                        CoopSlotMode.LOCAL_PLAYER -> CoopSlotMode.LAN_CLIENT
                        CoopSlotMode.LAN_CLIENT -> CoopSlotMode.OFF
                        CoopSlotMode.OFF -> CoopSlotMode.LOCAL_PLAYER
                        CoopSlotMode.HOST -> CoopSlotMode.HOST
                    }
                    p.copy(slotMode = nextMode, connectedClient = if (nextMode == CoopSlotMode.LAN_CLIENT) p.connectedClient else false)
                }
            }
            state.copy(players = updated)
        }
        broadcastLobbyIfHost()
    }

    fun updateCoopPlayerName(playerId: String, name: String) {
        _coopState.update { state ->
            val updated = state.players.map { p ->
                if (p.id == playerId) p.copy(name = name.take(22)) else p
            }
            state.copy(players = updated)
        }
        broadcastLobbyIfHost()
    }

    fun selectCoopConfigPlayer(playerId: String) {
        sound.play(SoundType.CLICK)
        _coopState.update { it.copy(selectedConfigPlayerId = playerId) }
    }

    fun addCoopUnitForPlayer(playerId: String, templateId: String) {
        sound.play(SoundType.CLICK)
        val entry = CoopRosterUnit("c_${nextIdCounter++}", playerId, templateId)
        _coopState.update { it.copy(assignedUnits = it.assignedUnits + entry) }
        broadcastLobbyIfHost()
    }

    fun removeCoopUnit(entryId: String) {
        sound.play(SoundType.CLICK)
        _coopState.update { it.copy(assignedUnits = it.assignedUnits.filterNot { u -> u.entryId == entryId }) }
        broadcastLobbyIfHost()
    }

    fun toggleCoopAiOpposition() {
        sound.play(SoundType.CLICK)
        _coopState.update { it.copy(spawnAiEnemyIfSameFaction = !it.spawnAiEnemyIfSameFaction) }
        broadcastLobbyIfHost()
    }

    fun updateCoopJoinIp(ip: String) {
        _coopState.update { it.copy(joinIpInput = ip, lobbyError = null) }
    }

    fun selectLocalClientPlayerId(playerId: String) {
        sound.play(SoundType.CLICK)
        _coopState.update { it.copy(localPlayerId = playerId) }
    }

    fun startCoopMatch() {
        val coop = _coopState.value
        val activePlayers = coop.players.filter { it.isActive }
        if (activePlayers.isEmpty()) return
        val templates = _setupState.value.templates

        aiJob?.cancel()
        actionJob?.cancel()
        sound.play(SoundType.TURN_HORN)

        val mapId = System.currentTimeMillis()
        val rawMap = MapGenerator.generateSectorMap(mapId)
        val map = SectorMap(mapId, rawMap)
        val spawned = mutableListOf<BattleUnit>()

        val hasMarines = activePlayers.any { p ->
            val pUnits = coop.assignedUnits.filter { it.ownerPlayerId == p.id }
            p.faction == Faction.MARINES && pUnits.isNotEmpty()
        }
        val hasXenos = activePlayers.any { p ->
            val pUnits = coop.assignedUnits.filter { it.ownerPlayerId == p.id }
            p.faction == Faction.XENOS && pUnits.isNotEmpty()
        }

        val westCandidates = buildSpawnCandidates(listOf(1, 2, 3), rawMap)
        val eastCandidates = buildSpawnCandidates(listOf(14, 13, 12), rawMap)
        var westIdx = 0
        var eastIdx = 0

        for (player in activePlayers) {
            val playerSlots = coop.assignedUnits.filter { it.ownerPlayerId == player.id }
            for (slot in playerSlots) {
                val tpl = templates[slot.templateId] ?: continue
                val spawnOnWest = tpl.faction == Faction.MARINES
                val pos = if (spawnOnWest) {
                    westCandidates.getOrElse(westIdx++) { GridPos(1, (westIdx % 11) + 1) }
                } else {
                    eastCandidates.getOrElse(eastIdx++) { GridPos(14, (eastIdx % 11) + 1) }
                }
                val facing = if (spawnOnWest) Facing.EAST else Facing.WEST
                spawned.add(
                    BattleUnit(
                        uid = "COOP_${player.id}_${nextIdCounter++}",
                        template = tpl,
                        faction = tpl.faction,
                        x = pos.x,
                        y = pos.y,
                        facing = facing,
                        totalWounds = tpl.maxTotalWounds,
                        maxTotalWounds = tpl.maxTotalWounds,
                        moveCmLeft = tpl.moveCm.toFloat(),
                        worldX = pos.x.toFloat(),
                        worldY = pos.y.toFloat(),
                        facingDeg = facing.yawDegrees(),
                        ownerPlayerId = player.id,
                        ownerPlayerName = player.name
                    )
                )
            }
        }

        // If all players picked the SAME faction (Co-op vs AI!) spawn an AI opposition wave
        val sameFactionCoop = !(hasMarines && hasXenos)
        if (sameFactionCoop && coop.spawnAiEnemyIfSameFaction) {
            val aiFaction = if (hasMarines) Faction.XENOS else Faction.MARINES
            val aiTemplateIds = if (aiFaction == Faction.XENOS) {
                listOf("hive_tyrant", "carnifex", "warriors", "genestealers", "termagants", "hormagaunts")
            } else {
                listOf("captain", "dreadnought", "terminators", "hellblasters", "intercessors", "assault_intercessors")
            }
            for (tid in aiTemplateIds) {
                val tpl = templates[tid] ?: continue
                val spawnOnWest = aiFaction == Faction.MARINES
                val pos = if (spawnOnWest) {
                    westCandidates.getOrElse(westIdx++) { GridPos(1, (westIdx % 11) + 1) }
                } else {
                    eastCandidates.getOrElse(eastIdx++) { GridPos(14, (eastIdx % 11) + 1) }
                }
                val facing = if (spawnOnWest) Facing.EAST else Facing.WEST
                spawned.add(
                    BattleUnit(
                        uid = "COOP_AI_${nextIdCounter++}",
                        template = tpl,
                        faction = tpl.faction,
                        x = pos.x,
                        y = pos.y,
                        facing = facing,
                        totalWounds = tpl.maxTotalWounds,
                        maxTotalWounds = tpl.maxTotalWounds,
                        moveCmLeft = tpl.moveCm.toFloat(),
                        worldX = pos.x.toFloat(),
                        worldY = pos.y.toFloat(),
                        facingDeg = facing.yawDegrees(),
                        ownerPlayerId = "AI",
                        ownerPlayerName = "AI Command"
                    )
                )
            }
        }

        if (spawned.isEmpty()) return

        val unitsWithAuras = TacticalRules.applyAuras(spawned)
        val firstPlayer = activePlayers.firstOrNull { p ->
            unitsWithAuras.any { it.ownerPlayerId == p.id && it.isAlive }
        } ?: activePlayers.first()
        val firstPlayerIndex = activePlayers.indexOf(firstPlayer).coerceAtLeast(0)
        val firstUnit = unitsWithAuras.firstOrNull { it.ownerPlayerId == firstPlayer.id && it.isAlive }
        val settings = _settingsState.value
        val hasLanClients = activePlayers.any { it.slotMode == CoopSlotMode.LAN_CLIENT }
        val localControlId = if (coop.networkRole == CoopNetworkRole.CLIENT) {
            coop.localPlayerId
        } else if (hasLanClients) {
            // Host allows local players on Host device, plus Host p1
            null
        } else {
            null
        }

        val initialState = BattleUiState(
            active = true,
            round = 1,
            activeFaction = firstPlayer.faction,
            playerFaction = firstPlayer.faction,
            map = map,
            units = unitsWithAuras,
            selectedUnitUid = firstUnit?.uid,
            autoFocusCamera = true,
            focusGridPos = firstUnit?.pos,
            soundEnabled = settings.sfxEnabled,
            graphicsLevel = settings.graphicsLevel,
            screenBrightness = settings.screenBrightness,
            isCoopMode = true,
            coopTurnOrder = activePlayers,
            coopTurnIndex = firstPlayerIndex,
            activeCoopPlayerId = firstPlayer.id,
            activeCoopPlayerName = firstPlayer.name,
            localControlledPlayerId = localControlId,
            turnSplash = TurnSplashData(
                round = 1,
                activeFaction = firstPlayer.faction,
                isPlayerTurn = true,
                marineModelsAlive = unitsWithAuras.filter { it.faction == Faction.MARINES }.sumOf { it.aliveModels },
                xenosModelsAlive = unitsWithAuras.filter { it.faction == Faction.XENOS }.sumOf { it.aliveModels },
                playerKills = 0,
                playerLosses = 0,
                commanderLabel = "${firstPlayer.name.uppercase()} TURN"
            )
        )
        val readyState = recomputeSelectionOverlays(initialState)
        _battleState.value = readyState
        _currentScreen.value = AppScreen.COOP_BATTLE
        scheduleSplashAutoDismiss()
        syncCoopHostIfNeeded(readyState)
    }

    private fun buildSpawnCandidates(cols: List<Int>, map: Array<Array<TerrainType>>): List<GridPos> {
        val spots = mutableListOf<GridPos>()
        val rows = listOf(6, 7, 4, 9, 2, 11, 5, 8, 3, 10, 1, 12)
        for (col in cols) {
            for (row in rows) {
                if (MapGenerator.isWalkable(map[row][col])) {
                    spots.add(GridPos(col, row))
                }
            }
        }
        return spots
    }

    private fun syncCoopHostIfNeeded(state: BattleUiState) {
        if (state.isCoopMode && _coopState.value.networkRole == CoopNetworkRole.LOCAL_OR_HOST && _coopState.value.serverRunning) {
            onlineHostStatePublisher?.invoke(state, _setupState.value.templates)
        }
    }

    fun applyOnlineBattleState(syncedBattle: BattleUiState, roomStatus: String) {
        if (_coopState.value.networkRole != CoopNetworkRole.CLIENT) return
        if (roomStatus == "FINISHED" && syncedBattle.summary == null && !syncedBattle.active) {
            aiJob?.cancel()
            actionJob?.cancel()
            _battleState.value = syncedBattle
            _coopState.update {
                it.copy(
                    inRoom = false,
                    networkRole = CoopNetworkRole.LOCAL_OR_HOST,
                    serverRunning = false,
                    clientConnected = false,
                    localPlayerId = "p1",
                    lobbyError = "Host ended the match."
                )
            }
            _currentScreen.value = AppScreen.COOP_LOBBY
            return
        }
        val configured = syncedBattle.copy(
            localControlledPlayerId = _coopState.value.localPlayerId,
            graphicsLevel = _settingsState.value.graphicsLevel,
            screenBrightness = _settingsState.value.screenBrightness,
            soundEnabled = _settingsState.value.sfxEnabled
        )
        _battleState.value = recomputeSelectionOverlays(configured)
        if ((configured.active || configured.summary != null) && _currentScreen.value != AppScreen.COOP_BATTLE) {
            _currentScreen.value = AppScreen.COOP_BATTLE
        }
    }

    fun executeRemoteCoopAction(
        senderPlayerId: String,
        actionType: String,
        wx: Float? = null,
        wy: Float? = null,
        cm: Float? = null,
        mode: WeaponMode? = null,
        left: Boolean? = null
    ) {
        if (_coopState.value.networkRole != CoopNetworkRole.LOCAL_OR_HOST) return
        val state = _battleState.value
        if (!state.active || !state.isCoopMode || state.activeCoopPlayerId != senderPlayerId) return

        when (actionType) {
            "TAP_WORLD" -> if (wx != null && wy != null) onWorldTapped(wx, wy, fromLanClient = true)
            "TOGGLE_MOVE" -> toggleMoveMode(fromLanClient = true)
            "ADVANCE" -> advanceMove(fromLanClient = true)
            "SET_CM" -> if (cm != null) setCmStep(cm, fromLanClient = true)
            "WALK_CM" -> walkForwardCm(fromLanClient = true)
            "CONFIRM_MOVE" -> confirmMovePreview(fromLanClient = true)
            "CANCEL_MOVE" -> cancelMovePreview(fromLanClient = true)
            "TOGGLE_CHARGE" -> toggleChargeMode(fromLanClient = true)
            "CONFIRM_CHARGE" -> confirmChargeRoll(fromLanClient = true)
            "CANCEL_CHARGE" -> cancelChargePreview(fromLanClient = true)
            "STRIKE" -> confirmStrike(fromLanClient = true)
            "WEAPON_MODE" -> if (mode != null) selectWeaponMode(mode, fromLanClient = true)
            "ROTATE" -> if (left != null) rotateSelectedUnit(left, fromLanClient = true)
            "END_TURN" -> endPlayerTurn(fromLanClient = true)
        }
    }

    fun updateSettings(newSettings: SettingsState) {
        val clamped = newSettings.copy(
            soundVolume = newSettings.soundVolume.coerceIn(0f, 1f),
            graphicsLevel = newSettings.graphicsLevel.coerceIn(1, 4),
            screenBrightness = newSettings.screenBrightness.coerceIn(0.5f, 1.5f)
        )
        sound.enabled = clamped.sfxEnabled
        sound.volume = clamped.soundVolume
        sound.play(SoundType.CLICK)
        _settingsState.value = clamped
        prefs.edit {
            putFloat("sound_volume", clamped.soundVolume)
            putBoolean("sfx_enabled", clamped.sfxEnabled)
            putInt("graphics_level", clamped.graphicsLevel)
            putFloat("screen_brightness", clamped.screenBrightness)
        }
        _battleState.update {
            it.copy(
                soundEnabled = clamped.sfxEnabled,
                graphicsLevel = clamped.graphicsLevel,
                screenBrightness = clamped.screenBrightness
            )
        }
    }

    private fun loadSavedSetup() {
        viewModelScope.launch {
            val saved = repository.getSavedConfigOnce() ?: return@launch
            val faction = runCatching { Faction.valueOf(saved.playerFaction) }.getOrDefault(Faction.MARINES)
            val templates = DefaultCatalog.deserializeTemplates(saved.customTemplatesJson)
            val marines = DefaultCatalog.deserializeRoster(saved.marineRosterJson, DefaultCatalog.defaultMarineRoster())
            val xenos = DefaultCatalog.deserializeRoster(saved.xenosRosterJson, DefaultCatalog.defaultXenosRoster())
            _setupState.update {
                it.copy(
                    playerFaction = faction,
                    pointLimit = saved.pointLimit.coerceIn(1000, 5000),
                    templates = templates,
                    marineRoster = marines,
                    xenosRoster = xenos
                )
            }
        }
    }

    // --- SETUP ACTIONS ---
    fun togglePlayerFaction() {
        sound.play(SoundType.CLICK)
        _setupState.update {
            val next = if (it.playerFaction == Faction.MARINES) Faction.XENOS else Faction.MARINES
            it.copy(playerFaction = next, statusMessage = null)
        }
    }

    fun setPointLimit(points: Int) {
        _setupState.update {
            it.copy(pointLimit = points.coerceIn(1000, 5000), statusMessage = null)
        }
    }

    fun addRosterUnit(faction: Faction, templateId: String) {
        sound.play(SoundType.CLICK)
        val slot = RosterSlot("slot_${nextIdCounter++}", templateId)
        _setupState.update { state ->
            if (faction == Faction.MARINES) {
                state.copy(marineRoster = state.marineRoster + slot, statusMessage = null)
            } else {
                state.copy(xenosRoster = state.xenosRoster + slot, statusMessage = null)
            }
        }
    }

    fun removeRosterSlot(faction: Faction, slotId: String) {
        sound.play(SoundType.CLICK)
        _setupState.update { state ->
            if (faction == Faction.MARINES) {
                state.copy(marineRoster = state.marineRoster.filterNot { it.slotId == slotId }, statusMessage = null)
            } else {
                state.copy(xenosRoster = state.xenosRoster.filterNot { it.slotId == slotId }, statusMessage = null)
            }
        }
    }

    fun randomizeRoster(faction: Faction) {
        sound.play(SoundType.CLICK)
        _setupState.update { state ->
            val pool = state.templates.values.filter { it.faction == faction }
            val limit = state.pointLimit
            val chosen = mutableListOf<RosterSlot>()
            var spent = 0
            val rng = Random(System.nanoTime())
            var guard = 0
            while (guard < 40) {
                guard++
                val affordable = pool.filter { spent + it.points <= limit }
                if (affordable.isEmpty()) break
                val pick = affordable[rng.nextInt(affordable.size)]
                chosen.add(RosterSlot("rnd_${nextIdCounter++}", pick.id))
                spent += pick.points
                if (chosen.size >= 8) break
            }
            if (chosen.isEmpty() && pool.isNotEmpty()) {
                val cheapest = pool.minBy { it.points }
                chosen.add(RosterSlot("rnd_${nextIdCounter++}", cheapest.id))
            }
            if (faction == Faction.MARINES) {
                state.copy(marineRoster = chosen, statusMessage = "Randomized Ultramarines ($spent pts)")
            } else {
                state.copy(xenosRoster = chosen, statusMessage = "Randomized Swarm ($spent pts)")
            }
        }
    }

    fun openStatEditor(templateId: String) {
        sound.play(SoundType.CLICK)
        _setupState.update { it.copy(editingTemplateId = templateId) }
    }

    fun closeStatEditor() {
        _setupState.update { it.copy(editingTemplateId = null) }
    }

    fun saveEditedTemplate(updated: UnitTemplate) {
        sound.play(SoundType.CLICK)
        val adjusted = if (updated.archetype == UnitArchetype.WARRIOR && updated.weaponChoice != null) {
            when (updated.weaponChoice) {
                WarriorWeaponChoice.BONESWORDS -> updated.copy(
                    range = 1,
                    mainWeaponName = "Tyranid Boneswords",
                    mainWeaponAssault = false,
                    strength = updated.strength.coerceAtLeast(5),
                    armorPen = updated.armorPen.coerceAtLeast(2),
                    damage = updated.damage.coerceAtLeast(2)
                )
                WarriorWeaponChoice.FLESHBORER -> updated.copy(
                    range = 4,
                    mainWeaponName = "Fleshborer",
                    mainWeaponAssault = true
                )
            }
        } else updated
        _setupState.update { state ->
            state.copy(
                templates = state.templates + (adjusted.id to adjusted),
                editingTemplateId = null,
                statusMessage = "Updated ${adjusted.name} stats"
            )
        }
    }

    fun saveSetupToDatabase() {
        sound.play(SoundType.CLICK)
        val s = _setupState.value
        viewModelScope.launch {
            repository.saveConfig(
                SavedConfigEntity(
                    id = 1,
                    playerFaction = s.playerFaction.name,
                    pointLimit = s.pointLimit,
                    marineRosterJson = DefaultCatalog.serializeRoster(s.marineRoster),
                    xenosRosterJson = DefaultCatalog.serializeRoster(s.xenosRoster),
                    customTemplatesJson = DefaultCatalog.serializeTemplates(s.templates)
                )
            )
            _setupState.update { it.copy(statusMessage = "Saved army configuration!") }
        }
    }

    fun resetSetupToDefaults() {
        sound.play(SoundType.CLICK)
        viewModelScope.launch {
            repository.resetConfig()
            _setupState.value = SetupUiState(statusMessage = "Reset to Crimson Sector defaults")
        }
    }

    fun clearStatusMessage() {
        _setupState.update { it.copy(statusMessage = null) }
    }

    // --- BATTLE LIFECYCLE ---
    fun deployToBattle() {
        val setup = _setupState.value
        if (!setup.canDeploy) return
        aiJob?.cancel()
        actionJob?.cancel()
        sound.play(SoundType.TURN_HORN)

        val mapId = System.currentTimeMillis()
        val rawMap = MapGenerator.generateSectorMap(mapId)
        val map = SectorMap(mapId, rawMap)
        val spawned = mutableListOf<BattleUnit>()

        // Spawn Marines on West edge (x = 1..2), Swarm on East edge (x = 13..14)
        spawnArmy(setup.marineRoster, setup.templates, Faction.MARINES, startCols = listOf(1, 2), Facing.EAST, rawMap, spawned)
        spawnArmy(setup.xenosRoster, setup.templates, Faction.XENOS, startCols = listOf(14, 13), Facing.WEST, rawMap, spawned)

        val unitsWithAuras = TacticalRules.applyAuras(spawned)
        val firstPlayerUnit = unitsWithAuras.firstOrNull { it.faction == setup.playerFaction }
        val settings = _settingsState.value

        val initialState = BattleUiState(
            active = true,
            round = 1,
            activeFaction = setup.playerFaction,
            playerFaction = setup.playerFaction,
            map = map,
            units = unitsWithAuras,
            selectedUnitUid = firstPlayerUnit?.uid,
            autoFocusCamera = true,
            focusGridPos = firstPlayerUnit?.pos,
            soundEnabled = settings.sfxEnabled,
            graphicsLevel = settings.graphicsLevel,
            screenBrightness = settings.screenBrightness,
            isCoopMode = false,
            coopTurnOrder = emptyList(),
            coopTurnIndex = 0,
            activeCoopPlayerId = null,
            activeCoopPlayerName = "",
            localControlledPlayerId = null,
            turnSplash = TurnSplashData(
                round = 1,
                activeFaction = setup.playerFaction,
                isPlayerTurn = true,
                marineModelsAlive = unitsWithAuras.filter { it.faction == Faction.MARINES }.sumOf { it.aliveModels },
                xenosModelsAlive = unitsWithAuras.filter { it.faction == Faction.XENOS }.sumOf { it.aliveModels },
                playerKills = 0,
                playerLosses = 0
            )
        )
        _battleState.value = recomputeSelectionOverlays(initialState)
        scheduleSplashAutoDismiss()
        _currentScreen.value = AppScreen.SKIRMISH_BATTLE
    }

    private fun spawnArmy(
        roster: List<RosterSlot>,
        templates: Map<String, UnitTemplate>,
        faction: Faction,
        startCols: List<Int>,
        facing: Facing,
        map: Array<Array<TerrainType>>,
        out: MutableList<BattleUnit>
    ) {
        val candidateSpots = mutableListOf<GridPos>()
        val rows = listOf(6, 7, 4, 9, 2, 11, 5, 8, 3, 10, 1, 12)
        for (col in startCols) {
            for (row in rows) {
                if (MapGenerator.isWalkable(map[row][col])) {
                    candidateSpots.add(GridPos(col, row))
                }
            }
        }
        roster.forEachIndexed { index, slot ->
            val tpl = templates[slot.templateId] ?: return@forEachIndexed
            val pos = candidateSpots.getOrElse(index) { GridPos(startCols.first(), (index + 1).coerceIn(1, 12)) }
            out.add(
                BattleUnit(
                    uid = "${faction.name}_${index}_${nextIdCounter++}",
                    template = tpl,
                    faction = faction,
                    x = pos.x,
                    y = pos.y,
                    facing = facing,
                    totalWounds = tpl.maxTotalWounds,
                    maxTotalWounds = tpl.maxTotalWounds,
                    moveCmLeft = tpl.moveCm.toFloat(),
                    worldX = pos.x.toFloat(),
                    worldY = pos.y.toFloat(),
                    facingDeg = facing.yawDegrees()
                )
            )
        }
    }

    fun returnToSetup() {
        val wasCoop = _battleState.value.isCoopMode
        aiJob?.cancel()
        actionJob?.cancel()
        sound.play(SoundType.CLICK)
        val inactive = _battleState.value.copy(active = false, summary = null, isAiThinking = false, isAnimating = false)
        _battleState.value = inactive
        if (wasCoop) {
            syncCoopHostIfNeeded(inactive)
            resetOnlineCoopState()
            _currentScreen.value = AppScreen.COOP_LOBBY
        } else {
            _currentScreen.value = AppScreen.SKIRMISH_SETUP
        }
    }

    private fun shouldForwardToLanHost(fromLanClient: Boolean): Boolean =
        !fromLanClient && _battleState.value.isCoopMode && _coopState.value.networkRole == CoopNetworkRole.CLIENT

    private fun forwardClientAction(action: String, fill: (JSONObject) -> Unit = {}) {
        val payload = JSONObject().apply(fill)
        val wx = if (payload.has("wx")) payload.optDouble("wx").toFloat() else null
        val wy = if (payload.has("wy")) payload.optDouble("wy").toFloat() else null
        val cm = if (payload.has("cm")) payload.optDouble("cm").toFloat() else null
        val mode = if (payload.has("mode")) runCatching { WeaponMode.valueOf(payload.optString("mode")) }.getOrNull() else null
        val left = if (payload.has("left")) payload.optBoolean("left") else null
        onlineClientActionSender?.invoke(action, wx, wy, cm, mode, left)
    }

    // --- BATTLE INTERACTION ---
    fun onTileTapped(pos: GridPos) {
        onWorldTapped(pos.x.toFloat(), pos.y.toFloat())
    }

    fun onWorldTapped(wx: Float, wy: Float, fromLanClient: Boolean = false) {
        if (shouldForwardToLanHost(fromLanClient)) {
            forwardClientAction("TAP_WORLD") {
                it.put("wx", wx.toDouble())
                it.put("wy", wy.toDouble())
            }
            return
        }
        val state = _battleState.value
        if (!state.isPlayerTurn || state.isAnimating) return

        val pos = GridPos(
            wx.roundToInt().coerceIn(0, MapGenerator.GRID_W - 1),
            wy.roundToInt().coerceIn(0, MapGenerator.GRID_H - 1)
        )

        // 0. Check if Assault Terminator is in Teleport Strike mode
        val selected = state.selectedUnit
        if (selected != null && state.canCommandUnit(selected) &&
            selected.template.archetype == UnitArchetype.TERMINATOR &&
            state.weaponMode == WeaponMode.SKILL
        ) {
            if (pos in state.reachableTiles.keys) {
                actionJob?.cancel()
                actionJob = viewModelScope.launch {
                    performTeleport(selected.uid, pos)
                }
            } else {
                val dist = maxOf(abs(pos.x - selected.x), abs(pos.y - selected.y))
                val enemies = state.units.filter { it.isAlive && it.faction != selected.faction }
                val minEnemy = enemies.minOfOrNull { maxOf(abs(pos.x - it.x), abs(pos.y - it.y)) } ?: 99
                when {
                    dist > 9 -> spawnFloatingText(pos, "MAX RANGE 9", 0xFFFF4D58L)
                    minEnemy <= 4 -> spawnFloatingText(pos, "TOO CLOSE TO ENEMY (MIN 5)", 0xFFFF4D58L)
                    !MapGenerator.isWalkable(state.map.rows[pos.y][pos.x]) -> spawnFloatingText(pos, "BLOCKED TILE", 0xFFFF4D58L)
                    else -> spawnFloatingText(pos, "OCCUPIED TILE", 0xFFFF4D58L)
                }
            }
            return
        }

        // 1. Check if tapped an alive unit (continuous hitbox or tile match)
        val unitHit = state.units
            .filter { it.isAlive }
            .minByOrNull { hypot(it.worldX - wx, it.worldY - wy) }
            ?.takeIf { hypot(it.worldX - wx, it.worldY - wy) < 0.65f || it.pos == pos }

        if (unitHit != null) {
            if (state.canCommandUnit(unitHit)) {
                sound.play(SoundType.CLICK)
                val updated = state.copy(
                    selectedUnitUid = unitHit.uid,
                    weaponMode = WeaponMode.MAIN,
                    moveModeActive = false,
                    movePreview = null,
                    targetPreview = null,
                    chargeModeActive = false,
                    chargePreview = null,
                    focusGridPos = if (state.autoFocusCamera) unitHit.pos else state.focusGridPos
                )
                val ready = recomputeSelectionOverlays(updated)
                _battleState.value = ready
                syncCoopHostIfNeeded(ready)
                return
            } else if (unitHit.faction == state.activeFaction) {
                spawnFloatingText(pos, "CMD: ${unitHit.ownerPlayerName.uppercase()}", 0xFF64DFDFL)
                return
            } else if (state.chargeModeActive) {
                val sel = state.selectedUnit ?: return
                val cPreview = TacticalRules.findChargeEngagementSpot(sel, unitHit, state.units, state.map.rows)
                if (cPreview != null) {
                    sound.play(SoundType.CLICK)
                    _battleState.update {
                        it.copy(
                            chargePreview = cPreview,
                            movePreview = null,
                            targetPreview = null
                        )
                    }
                    syncCoopHostIfNeeded(_battleState.value)
                } else {
                    val curDist = hypot(sel.worldX - unitHit.worldX, sel.worldY - unitHit.worldY)
                    val msg = when {
                        !sel.canCharge -> if (sel.hasAdvanced) "NO CHARGE AFTER ADVANCE" else "ALREADY CHARGED"
                        curDist <= 1.55f -> "ALREADY IN MELEE"
                        curDist > 12.45f -> "OUT OF 12\" CHARGE"
                        else -> "NO CHARGE PATH"
                    }
                    spawnFloatingText(pos, msg, 0xFFFF4D58L)
                }
                return
            } else if (!state.moveModeActive) {
                val sel = state.selectedUnit ?: return
                if (!state.canCommandUnit(sel)) return
                val preview = TacticalRules.canTarget(sel, unitHit, state.weaponMode, state.map.rows, state.units)
                if (preview != null && preview.inRange && preview.hasLos) {
                    sound.play(SoundType.CLICK)
                    _battleState.update { it.copy(targetPreview = preview, movePreview = null) }
                    syncCoopHostIfNeeded(_battleState.value)
                } else {
                    val isEngaged = TacticalRules.isEngagedInMelee(sel, state.units)
                    val distToHit = hypot(sel.worldX - unitHit.worldX, sel.worldY - unitHit.worldY)
                    val isMeleeStrike = TacticalRules.effectiveIsMelee(sel, state.weaponMode, distToHit)
                    val hasPistolPrivilege = state.weaponMode == WeaponMode.PISTOL || sel.template.mainWeaponPistol
                    val reason = when {
                        sel.skillCooldownLeft > 0 && state.weaponMode == WeaponMode.SKILL -> "ON COOLDOWN"
                        sel.pistolCooldownLeft > 0 && state.weaponMode == WeaponMode.PISTOL -> "ON COOLDOWN"
                        isEngaged && !isMeleeStrike && !hasPistolPrivilege -> "IN MELEE: PISTOL ONLY"
                        !TacticalRules.canUseMode(sel, state.weaponMode, state.units) -> when {
                            sel.hasAdvanced && state.weaponMode == WeaponMode.PISTOL -> "NO PISTOL AFTER ADVANCE"
                            sel.hasAdvanced && !sel.template.mainWeaponAssault -> "NO SHOOT AFTER ADVANCE"
                            sel.hasCharged -> "MELEE ONLY AFTER CHARGE"
                            TacticalRules.modeUsesMelee(sel, state.weaponMode) -> "ALREADY FOUGHT"
                            else -> "ALREADY SHOT"
                        }
                        preview == null -> "UNAVAILABLE"
                        !preview.inRange -> "OUT OF RANGE"
                        else -> "NO LOS"
                    }
                    spawnFloatingText(pos, reason, 0xFF9AA3B2L)
                }
                return
            }
        }

        // 2. Ground tap -> Charge mode guard or continuous cm movement preview
        val sel = state.selectedUnit ?: return
        if (!state.canCommandUnit(sel)) return

        if (state.chargeModeActive) {
            spawnFloatingText(pos, "TAP ENEMY WITHIN 12\"", 0xFFFFB703L)
            return
        }

        if (sel.moveCmLeft < 0.25f) {
            spawnFloatingText(pos, "0.0 CM LEFT", 0xFF9AA3B2L)
            return
        }

        val clampedX = wx.coerceIn(0f, (MapGenerator.GRID_W - 1).toFloat())
        val clampedY = wy.coerceIn(0f, (MapGenerator.GRID_H - 1).toFloat())
        val preview = TacticalRules.computeCmMovePreview(sel, clampedX, clampedY, state.units, state.map.rows)
        if (preview != null) {
            sound.play(SoundType.CLICK)
            _battleState.update {
                it.copy(
                    movePreview = preview,
                    targetPreview = null
                )
            }
            syncCoopHostIfNeeded(_battleState.value)
        } else {
            val rawDistCm = hypot(clampedX - sel.worldX, clampedY - sel.worldY) * TacticalRules.CM_PER_TILE
            val msg = if (rawDistCm > sel.moveCmLeft + 0.05f) {
                "OUT OF CM (${"%.1f".format(sel.moveCmLeft)} cm)"
            } else {
                "BLOCKED"
            }
            spawnFloatingText(pos, msg, 0xFFFF4D58L)
        }
    }

    fun toggleMoveMode(fromLanClient: Boolean = false) {
        if (shouldForwardToLanHost(fromLanClient)) {
            forwardClientAction("TOGGLE_MOVE")
            return
        }
        val state = _battleState.value
        if (!state.isPlayerTurn || state.isAnimating) return
        val selected = state.selectedUnit ?: return
        if (!state.canCommandUnit(selected)) return
        sound.play(SoundType.CLICK)
        _battleState.update { st ->
            val next = !st.moveModeActive
            recomputeSelectionOverlays(
                st.copy(
                    moveModeActive = next,
                    movePreview = if (next) st.movePreview else null,
                    chargeModeActive = false,
                    chargePreview = null,
                    targetPreview = null
                )
            )
        }
        syncCoopHostIfNeeded(_battleState.value)
    }

    fun advanceMove(fromLanClient: Boolean = false) {
        if (shouldForwardToLanHost(fromLanClient)) {
            forwardClientAction("ADVANCE")
            return
        }
        val state = _battleState.value
        if (!state.isPlayerTurn || state.isAnimating) return
        val selected = state.selectedUnit ?: return
        if (!state.canCommandUnit(selected) || !selected.canAdvance) {
            spawnFloatingText(selected.pos, "CANNOT ADVANCE", 0xFFFF4D58L)
            return
        }
        sound.play(SoundType.CLICK)
        val (roll, bonusCm) = TacticalRules.rollAdvanceBonus()
        val updatedUnits = state.units.map { u ->
            if (u.uid != selected.uid) u
            else u.copy(
                moveCmLeft = u.moveCmLeft + bonusCm,
                hasAdvanced = true,
                activeBuffLabel = if (u.template.mainWeaponAssault) {
                    "ADVANCE +$roll\" (ASSAULT)"
                } else {
                    "ADVANCE +$roll\" (NO SHOOT/CHRG)"
                }
            )
        }
        spawnFloatingText(selected.pos, "ADVANCE +$roll\" (+${bonusCm.toInt()} cm)", 0xFF38D96CL)
        val nextState = state.copy(
            units = updatedUnits,
            weaponMode = WeaponMode.MAIN,
            moveModeActive = true,
            chargeModeActive = false,
            chargePreview = null,
            movePreview = null,
            targetPreview = null
        )
        val ready = recomputeSelectionOverlays(nextState)
        _battleState.value = ready
        syncCoopHostIfNeeded(ready)
    }

    fun toggleChargeMode(fromLanClient: Boolean = false) {
        if (shouldForwardToLanHost(fromLanClient)) {
            forwardClientAction("TOGGLE_CHARGE")
            return
        }
        val state = _battleState.value
        if (!state.isPlayerTurn || state.isAnimating) return
        val selected = state.selectedUnit ?: return
        if (!state.canCommandUnit(selected)) return
        if (!selected.canCharge) {
            val msg = when {
                selected.hasAdvanced -> "NO CHARGE AFTER ADVANCE"
                selected.hasCharged -> "ALREADY CHARGED"
                else -> "ALREADY FOUGHT"
            }
            spawnFloatingText(selected.pos, msg, 0xFFFF4D58L)
            return
        }
        sound.play(SoundType.CLICK)
        val next = !state.chargeModeActive
        val nextState = state.copy(
            chargeModeActive = next,
            chargePreview = null,
            moveModeActive = false,
            movePreview = null,
            targetPreview = null,
            weaponMode = WeaponMode.MAIN
        )
        val ready = recomputeSelectionOverlays(nextState)
        _battleState.value = ready
        syncCoopHostIfNeeded(ready)
    }

    fun cancelChargePreview(fromLanClient: Boolean = false) {
        if (shouldForwardToLanHost(fromLanClient)) {
            forwardClientAction("CANCEL_CHARGE")
            return
        }
        sound.play(SoundType.CLICK)
        _battleState.update { it.copy(chargePreview = null) }
        syncCoopHostIfNeeded(_battleState.value)
    }

    fun confirmChargeRoll(fromLanClient: Boolean = false) {
        if (shouldForwardToLanHost(fromLanClient)) {
            forwardClientAction("CONFIRM_CHARGE")
            return
        }
        val state = _battleState.value
        if (!state.isPlayerTurn || state.isAnimating) return
        val selected = state.selectedUnit ?: return
        if (!state.canCommandUnit(selected)) return
        val preview = state.chargePreview ?: return
        actionJob?.cancel()
        actionJob = viewModelScope.launch {
            executeChargeAttempt(selected.uid, preview)
        }
    }

    private suspend fun executeChargeAttempt(unitUid: String, preview: ChargePreview) {
        val state = _battleState.value
        val charger = state.units.firstOrNull { it.uid == unitUid && it.isAlive } ?: return
        val d1 = Random.nextInt(1, 7)
        val d2 = Random.nextInt(1, 7)
        val total = d1 + d2

        if (total >= preview.requiredRoll) {
            sound.play(SoundType.MELEE)
            spawnFloatingText(charger.pos, "CHARGE $d1+$d2=$total! (${preview.requiredRoll}+)", 0xFFFFD166L)
            performChargeDash(unitUid, preview)
        } else {
            sound.play(SoundType.CLICK)
            spawnFloatingText(charger.pos, "CHARGE FAIL $d1+$d2=$total (${preview.requiredRoll}+)", 0xFFFF4D58L)
            val updatedUnits = state.units.map { u ->
                if (u.uid != unitUid) u else u.copy(hasCharged = true, moveCmLeft = 0f)
            }
            val next = recomputeSelectionOverlays(
                state.copy(
                    units = updatedUnits,
                    chargeModeActive = false,
                    chargePreview = null,
                    chargeableUids = emptySet()
                )
            )
            _battleState.value = next
            syncCoopHostIfNeeded(next)
        }
    }

    private suspend fun performChargeDash(unitUid: String, preview: ChargePreview) {
        val stateBefore = _battleState.value
        val unit = stateBefore.units.firstOrNull { it.uid == unitUid && it.isAlive } ?: return
        val target = stateBefore.units.firstOrNull { it.uid == preview.targetUid && it.isAlive }
        if (preview.path.isEmpty()) return

        val destGrid = GridPos(
            preview.destinationX.roundToInt().coerceIn(0, MapGenerator.GRID_W - 1),
            preview.destinationY.roundToInt().coerceIn(0, MapGenerator.GRID_H - 1)
        )

        _battleState.update { state ->
            val updatedUnits = state.units.map { u ->
                if (u.uid != unitUid) u
                else u.copy(
                    hasCharged = true,
                    moveCmLeft = 0f,
                    activeBuffLabel = "CHARGED"
                )
            }
            state.copy(
                units = updatedUnits,
                weaponMode = WeaponMode.MAIN,
                moveModeActive = false,
                movePreview = null,
                chargeModeActive = false,
                chargePreview = null,
                chargeableUids = emptySet(),
                reachableTiles = emptyMap(),
                moveParents = emptyMap(),
                attackableUids = emptySet(),
                targetPreview = null,
                isAnimating = true
            )
        }

        try {
            var curX = unit.worldX
            var curY = unit.worldY
            for (wp in preview.path) {
                val segLen = hypot(wp.x - curX, wp.y - curY)
                if (segLen < 0.01f) continue
                val subSteps = (segLen / 0.35f).roundToInt().coerceAtLeast(1)
                val segDeg = TacticalRules.angleDegToward(curX, curY, wp.x, wp.y)
                val segFacing = TacticalRules.nearestFacingFromDeg(segDeg)
                for (s in 1..subSteps) {
                    delay(48L)
                    val t = s / subSteps.toFloat()
                    val nx = curX + (wp.x - curX) * t
                    val ny = curY + (wp.y - curY) * t
                    val gx = nx.roundToInt().coerceIn(0, MapGenerator.GRID_W - 1)
                    val gy = ny.roundToInt().coerceIn(0, MapGenerator.GRID_H - 1)
                    _battleState.update { st ->
                        val updatedUnits = st.units.map { u ->
                            if (u.uid != unitUid) u
                            else u.copy(
                                worldX = nx,
                                worldY = ny,
                                x = gx,
                                y = gy,
                                facing = segFacing,
                                facingDeg = segDeg
                            )
                        }
                        st.copy(
                            units = updatedUnits,
                            focusGridPos = if (st.autoFocusCamera) GridPos(gx, gy) else st.focusGridPos
                        )
                    }
                }
                curX = wp.x
                curY = wp.y
            }

            val finalFacingDeg = if (target != null) {
                TacticalRules.angleDegToward(preview.destinationX, preview.destinationY, target.worldX, target.worldY)
            } else unit.facingDeg
            val finalFacing = TacticalRules.nearestFacingFromDeg(finalFacingDeg)

            _battleState.update { state ->
                val alignedUnits = state.units.map { u ->
                    if (u.uid != unitUid) u
                    else u.copy(
                        x = destGrid.x,
                        y = destGrid.y,
                        worldX = preview.destinationX,
                        worldY = preview.destinationY,
                        facing = finalFacing,
                        facingDeg = finalFacingDeg
                    )
                }
                val withAuras = TacticalRules.applyAuras(alignedUnits)
                val updatedUnit = withAuras.firstOrNull { it.uid == unitUid }
                val updatedTarget = target?.let { t -> withAuras.firstOrNull { it.uid == t.uid && it.isAlive } }
                val autoTargetPreview = if (state.isPlayerTurn && updatedUnit != null && updatedTarget != null) {
                    TacticalRules.canTarget(updatedUnit, updatedTarget, WeaponMode.MAIN, state.map.rows, withAuras)
                        ?.takeIf { it.inRange }
                } else null

                recomputeSelectionOverlays(
                    state.copy(
                        units = withAuras,
                        isAnimating = false,
                        targetPreview = autoTargetPreview,
                        focusGridPos = if (state.autoFocusCamera) destGrid else state.focusGridPos
                    )
                )
            }
            syncCoopHostIfNeeded(_battleState.value)
        } finally {
            _battleState.update { st ->
                if (st.isAnimating) recomputeSelectionOverlays(st.copy(isAnimating = false)) else st
            }
        }
    }

    fun setCmStep(stepCm: Float, fromLanClient: Boolean = false) {
        if (shouldForwardToLanHost(fromLanClient)) {
            forwardClientAction("SET_CM") { it.put("cm", stepCm.toDouble()) }
            return
        }
        sound.play(SoundType.CLICK)
        _battleState.update { it.copy(cmStepChoice = stepCm) }
        syncCoopHostIfNeeded(_battleState.value)
    }

    fun walkForwardCm(fromLanClient: Boolean = false) {
        if (shouldForwardToLanHost(fromLanClient)) {
            forwardClientAction("WALK_CM")
            return
        }
        val state = _battleState.value
        if (!state.isPlayerTurn || state.isAnimating) return
        val selected = state.selectedUnit ?: return
        if (!state.canCommandUnit(selected)) return
        val requested = if (state.cmStepChoice <= 0f) selected.moveCmLeft else state.cmStepChoice
        val preview = TacticalRules.computeForwardCmPreview(selected, requested, state.units, state.map.rows)
        if (preview == null) {
            spawnFloatingText(
                selected.pos,
                if (selected.moveCmLeft < 0.2f) "0.0 CM LEFT" else "PATH BLOCKED",
                0xFFFF4D58L
            )
            return
        }
        actionJob?.cancel()
        actionJob = viewModelScope.launch {
            performContinuousMove(selected.uid, preview)
        }
    }

    fun confirmMovePreview(fromLanClient: Boolean = false) {
        if (shouldForwardToLanHost(fromLanClient)) {
            forwardClientAction("CONFIRM_MOVE")
            return
        }
        val state = _battleState.value
        if (!state.isPlayerTurn || state.isAnimating) return
        val selected = state.selectedUnit ?: return
        if (!state.canCommandUnit(selected)) return
        val preview = state.movePreview ?: return
        actionJob?.cancel()
        actionJob = viewModelScope.launch {
            performContinuousMove(selected.uid, preview)
        }
    }

    fun cancelMovePreview(fromLanClient: Boolean = false) {
        if (shouldForwardToLanHost(fromLanClient)) {
            forwardClientAction("CANCEL_MOVE")
            return
        }
        sound.play(SoundType.CLICK)
        _battleState.update { it.copy(movePreview = null) }
        syncCoopHostIfNeeded(_battleState.value)
    }

    private suspend fun performTeleport(unitUid: String, dest: GridPos) {
        val state = _battleState.value
        val unit = state.units.firstOrNull { it.uid == unitUid && it.isAlive } ?: return
        sound.play(SoundType.PLASMA)

        val startPos = unit.pos
        val pathPoints = TacticalRules.computeLinePath(startPos.x, startPos.y, dest.x, dest.y)

        val initialUpdatedUnits = state.units.map { u ->
            if (u.uid != unitUid) u
            else u.copy(
                moveCmLeft = 0f,
                skillCooldownLeft = u.template.skillCooldown
            )
        }
        val vfx = ProjectileVfx(
            id = nextIdCounter++,
            fromX = unit.worldX,
            fromY = unit.worldY,
            toX = dest.x.toFloat(),
            toY = dest.y.toFloat(),
            colorArgb = 0xFF4CC9F0L,
            isBeam = true,
            isMelee = false,
            startTimeMs = System.currentTimeMillis()
        )

        _battleState.value = state.copy(
            units = initialUpdatedUnits,
            weaponMode = WeaponMode.MAIN,
            moveModeActive = false,
            movePreview = null,
            reachableTiles = emptyMap(),
            moveParents = emptyMap(),
            attackableUids = emptySet(),
            targetPreview = null,
            isAnimating = true,
            projectiles = state.projectiles + vfx
        )
        spawnFloatingText(startPos, "WARP OUT", 0xFF64DFDFL)

        try {
            for (i in 1 until pathPoints.size) {
                val pt = pathPoints[i]
                delay(120L)
                _battleState.update { st ->
                    val currentUnits = st.units.map { u ->
                        if (u.uid != unitUid) u
                        else {
                            val nextFacing = TacticalRules.facingToward(u.pos, pt)
                            val nextDeg = TacticalRules.angleDegToward(u.worldX, u.worldY, pt.x.toFloat(), pt.y.toFloat())
                            u.copy(
                                x = pt.x,
                                y = pt.y,
                                worldX = pt.x.toFloat(),
                                worldY = pt.y.toFloat(),
                                facing = nextFacing,
                                facingDeg = nextDeg
                            )
                        }
                    }
                    st.copy(
                        units = currentUnits,
                        focusGridPos = if (st.autoFocusCamera) pt else st.focusGridPos
                    )
                }
            }

            sound.play(SoundType.PLASMA)
            spawnFloatingText(dest, "TELEPORT STRIKE!", 0xFF48CAE4L)
            spawnFloatingText(dest, "SHIELD -50% DMG", 0xFFFFD166L)

            val finalUnits = _battleState.value.units.map { u ->
                if (u.uid != unitUid) u
                else u.copy(
                    x = dest.x,
                    y = dest.y,
                    worldX = dest.x.toFloat(),
                    worldY = dest.y.toFloat(),
                    damageReductionTurns = 1,
                    buffTurnsLeft = 1,
                    activeBuffLabel = "SHIELD -50% DMG"
                )
            }
            val withAuras = TacticalRules.applyAuras(finalUnits)
            _battleState.value = recomputeSelectionOverlays(
                _battleState.value.copy(
                    units = withAuras,
                    isAnimating = false,
                    focusGridPos = if (_battleState.value.autoFocusCamera) dest else _battleState.value.focusGridPos
                )
            )
            syncCoopHostIfNeeded(_battleState.value)
            scheduleVfxCleanup()
        } finally {
            _battleState.update { st ->
                if (st.isAnimating) recomputeSelectionOverlays(st.copy(isAnimating = false)) else st
            }
        }
    }

    private suspend fun performContinuousMove(unitUid: String, preview: MovePreview) {
        val stateBefore = _battleState.value
        val unit = stateBefore.units.firstOrNull { it.uid == unitUid && it.isAlive } ?: return
        if (preview.path.isEmpty()) return
        sound.play(SoundType.CLICK)

        val destGrid = GridPos(
            preview.targetX.roundToInt().coerceIn(0, MapGenerator.GRID_W - 1),
            preview.targetY.roundToInt().coerceIn(0, MapGenerator.GRID_H - 1)
        )

        _battleState.update { state ->
            val updatedUnits = state.units.map { u ->
                if (u.uid != unitUid) u
                else u.copy(moveCmLeft = (u.moveCmLeft - preview.distanceCm).coerceAtLeast(0f))
            }
            state.copy(
                units = updatedUnits,
                movePreview = null,
                reachableTiles = emptyMap(),
                moveParents = emptyMap(),
                attackableUids = emptySet(),
                targetPreview = null,
                isAnimating = true
            )
        }

        try {
            var curX = unit.worldX
            var curY = unit.worldY
            for (wp in preview.path) {
                val segLen = hypot(wp.x - curX, wp.y - curY)
                if (segLen < 0.01f) continue
                val subSteps = (segLen / 0.25f).roundToInt().coerceAtLeast(1)
                val segDeg = TacticalRules.angleDegToward(curX, curY, wp.x, wp.y)
                val segFacing = TacticalRules.nearestFacingFromDeg(segDeg)
                for (s in 1..subSteps) {
                    delay(75L)
                    val t = s / subSteps.toFloat()
                    val nx = curX + (wp.x - curX) * t
                    val ny = curY + (wp.y - curY) * t
                    val gx = nx.roundToInt().coerceIn(0, MapGenerator.GRID_W - 1)
                    val gy = ny.roundToInt().coerceIn(0, MapGenerator.GRID_H - 1)
                    _battleState.update { st ->
                        val updatedUnits = st.units.map { u ->
                            if (u.uid != unitUid) u
                            else u.copy(
                                worldX = nx,
                                worldY = ny,
                                x = gx,
                                y = gy,
                                facing = segFacing,
                                facingDeg = segDeg
                            )
                        }
                        st.copy(
                            units = updatedUnits,
                            focusGridPos = if (st.autoFocusCamera) GridPos(gx, gy) else st.focusGridPos
                        )
                    }
                }
                curX = wp.x
                curY = wp.y
            }

            _battleState.update { state ->
                val withAuras = TacticalRules.applyAuras(state.units)
                recomputeSelectionOverlays(
                    state.copy(
                        units = withAuras,
                        isAnimating = false,
                        focusGridPos = if (state.autoFocusCamera) destGrid else state.focusGridPos
                    )
                )
            }
            syncCoopHostIfNeeded(_battleState.value)
        } finally {
            _battleState.update { st ->
                if (st.isAnimating) recomputeSelectionOverlays(st.copy(isAnimating = false)) else st
            }
        }
    }

    private suspend fun performMoveAlongPath(unitUid: String, path: List<GridPos>, totalCost: Int) {
        if (path.isEmpty()) return
        sound.play(SoundType.CLICK)
        val dest = path.last()
        val cmCost = totalCost * TacticalRules.CM_PER_TILE

        _battleState.update { state ->
            val updatedUnits = state.units.map { u ->
                if (u.uid != unitUid) u
                else u.copy(moveCmLeft = (u.moveCmLeft - cmCost).coerceAtLeast(0f))
            }
            state.copy(
                units = updatedUnits,
                reachableTiles = emptyMap(),
                moveParents = emptyMap(),
                attackableUids = emptySet(),
                targetPreview = null,
                isAnimating = true
            )
        }

        try {
            for (step in path) {
                delay(220L)
                _battleState.update { state ->
                    val updatedUnits = state.units.map { u ->
                        if (u.uid != unitUid) u
                        else {
                            val nextDeg = TacticalRules.angleDegToward(u.worldX, u.worldY, step.x.toFloat(), step.y.toFloat())
                            val nextFacing = TacticalRules.nearestFacingFromDeg(nextDeg)
                            u.copy(
                                x = step.x,
                                y = step.y,
                                worldX = step.x.toFloat(),
                                worldY = step.y.toFloat(),
                                facing = nextFacing,
                                facingDeg = nextDeg
                            )
                        }
                    }
                    state.copy(
                        units = updatedUnits,
                        focusGridPos = if (state.autoFocusCamera) step else state.focusGridPos
                    )
                }
            }

            _battleState.update { state ->
                val withAuras = TacticalRules.applyAuras(state.units)
                recomputeSelectionOverlays(
                    state.copy(
                        units = withAuras,
                        isAnimating = false,
                        focusGridPos = if (state.autoFocusCamera) dest else state.focusGridPos
                    )
                )
            }
            syncCoopHostIfNeeded(_battleState.value)
        } finally {
            _battleState.update { st ->
                if (st.isAnimating) recomputeSelectionOverlays(st.copy(isAnimating = false)) else st
            }
        }
    }

    fun confirmStrike(fromLanClient: Boolean = false) {
        if (shouldForwardToLanHost(fromLanClient)) {
            forwardClientAction("STRIKE")
            return
        }
        val state = _battleState.value
        if (!state.isPlayerTurn || state.isAnimating) return
        val attacker = state.selectedUnit ?: return
        if (!state.canCommandUnit(attacker)) return
        val preview = state.targetPreview ?: return
        val target = state.units.firstOrNull { it.uid == preview.targetUid && it.isAlive } ?: return
        actionJob?.cancel()
        actionJob = viewModelScope.launch {
            executeAttack(attacker.uid, target.uid, preview.weaponMode)
        }
    }

    private suspend fun executeAttack(attackerUid: String, targetUid: String, mode: WeaponMode) {
        val state = _battleState.value
        val attacker = state.units.firstOrNull { it.uid == attackerUid && it.isAlive } ?: return
        val target = state.units.firstOrNull { it.uid == targetUid && it.isAlive } ?: return

        val distTiles = hypot(attacker.worldX - target.worldX, attacker.worldY - target.worldY)
        val isMelee = TacticalRules.effectiveIsMelee(attacker, mode, distTiles)
        sound.play(
            when {
                isMelee -> SoundType.MELEE
                mode == WeaponMode.SKILL || attacker.template.archetype == UnitArchetype.HELLBLASTER -> SoundType.PLASMA
                else -> SoundType.BOLTER
            }
        )

        val outcome = TacticalRules.resolveAttack(attacker, target, state.units, mode, state.map.rows)
        val now = System.currentTimeMillis()

        val projColor = when {
            attacker.faction == Faction.MARINES && (mode == WeaponMode.SKILL || attacker.template.archetype == UnitArchetype.HELLBLASTER) -> 0xFF32C5FFL
            attacker.faction == Faction.MARINES -> 0xFFFFD166L
            else -> 0xFFC77DFFL
        }
        val proj = ProjectileVfx(
            id = nextIdCounter++,
            fromX = attacker.worldX,
            fromY = attacker.worldY,
            toX = target.worldX,
            toY = target.worldY,
            colorArgb = projColor,
            isBeam = mode == WeaponMode.SKILL || attacker.template.archetype == UnitArchetype.HELLBLASTER,
            isMelee = isMelee,
            startTimeMs = now
        )
        val floats = outcome.floatingTexts.mapIndexed { idx, pair ->
            FloatingTextVfx(
                id = nextIdCounter++,
                gridX = pair.first.x.toFloat(),
                gridY = pair.first.y.toFloat() - idx * 0.35f,
                text = pair.second.first,
                colorArgb = pair.second.second,
                startTimeMs = now + 450L + idx * 180L
            )
        }

        val targetMap = outcome.updatedTargets.associateBy { it.uid }
        val newUnits = TacticalRules.applyAuras(
            state.units.map { u ->
                when (u.uid) {
                    outcome.updatedAttacker.uid -> outcome.updatedAttacker
                    in targetMap -> targetMap.getValue(u.uid)
                    else -> u
                }
            }
        )

        val addedPlayerKills = if (attacker.faction == state.playerFaction) outcome.killsCount else 0
        val addedEnemyKills = if (attacker.faction != state.playerFaction) outcome.killsCount else 0

        val survivingAttacker = newUnits.firstOrNull { it.uid == attacker.uid }
        val counterDefenders = if (isMelee && survivingAttacker != null && survivingAttacker.isAlive) {
            outcome.updatedTargets.mapNotNull { t ->
                newUnits.firstOrNull { it.uid == t.uid && it.isAlive && it.aliveModels > 0 }?.takeIf { def ->
                    hypot(def.worldX - survivingAttacker.worldX, def.worldY - survivingAttacker.worldY) <= 1.55f
                }
            }
        } else emptyList()
        val willCounterAttack = counterDefenders.isNotEmpty()

        val updated = state.copy(
            units = newUnits,
            weaponMode = WeaponMode.MAIN,
            moveModeActive = false,
            movePreview = null,
            chargeModeActive = false,
            chargePreview = null,
            chargeableUids = emptySet(),
            targetPreview = null,
            isAnimating = willCounterAttack,
            projectiles = (state.projectiles + proj).filter { now - it.startTimeMs < 1800 },
            floatingTexts = (state.floatingTexts + floats).filter { now - it.startTimeMs < 3200 },
            focusGridPos = if (state.autoFocusCamera) target.pos else state.focusGridPos,
            playerKills = state.playerKills + addedPlayerKills,
            enemyKills = state.enemyKills + addedEnemyKills
        )
        val afterHit = checkVictoryOrUpdate(recomputeSelectionOverlays(updated))
        _battleState.value = afterHit
        syncCoopHostIfNeeded(afterHit)
        scheduleVfxCleanup()

        if (willCounterAttack) {
            for (survivingDefender in counterDefenders) {
                val preCheck = _battleState.value
                if (!preCheck.active || preCheck.summary != null) return
                val liveAtkBefore = preCheck.units.firstOrNull { it.uid == attacker.uid && it.isAlive } ?: break
                val liveDefBefore = preCheck.units.firstOrNull { it.uid == survivingDefender.uid && it.isAlive } ?: continue

                spawnFloatingText(liveDefBefore.pos, "⚔️ COUNTER-ATTACK (2s)", 0xFFFFD166L)
                delay(2000L)

                val currentState = _battleState.value
                if (!currentState.active || currentState.summary != null) return
                val liveDefender = currentState.units.firstOrNull { it.uid == liveDefBefore.uid && it.isAlive }
                val liveAttacker = currentState.units.firstOrNull { it.uid == liveAtkBefore.uid && it.isAlive }

                if (liveDefender != null && liveAttacker != null && liveDefender.aliveModels > 0) {
                    sound.play(SoundType.MELEE)
                    val counterOutcome = TacticalRules.resolveMeleeCounterAttack(
                        liveDefender,
                        liveAttacker,
                        currentState.map.rows
                    )
                    if (counterOutcome != null) {
                        val counterNow = System.currentTimeMillis()
                        val counterProjColor = if (liveDefender.faction == Faction.MARINES) 0xFFFFD166L else 0xFFC77DFFL
                        val counterProj = ProjectileVfx(
                            id = nextIdCounter++,
                            fromX = liveDefender.worldX,
                            fromY = liveDefender.worldY,
                            toX = liveAttacker.worldX,
                            toY = liveAttacker.worldY,
                            colorArgb = counterProjColor,
                            isBeam = false,
                            isMelee = true,
                            startTimeMs = counterNow
                        )
                        val counterFloats = counterOutcome.floatingTexts.mapIndexed { idx, pair ->
                            FloatingTextVfx(
                                id = nextIdCounter++,
                                gridX = pair.first.x.toFloat(),
                                gridY = pair.first.y.toFloat() - idx * 0.35f,
                                text = pair.second.first,
                                colorArgb = pair.second.second,
                                startTimeMs = counterNow + 250L + idx * 180L
                            )
                        }

                        val afterCounterUnits = TacticalRules.applyAuras(
                            currentState.units.map { u ->
                                when (u.uid) {
                                    counterOutcome.updatedDefender.uid -> counterOutcome.updatedDefender
                                    counterOutcome.updatedAttacker.uid -> counterOutcome.updatedAttacker
                                    else -> u
                                }
                            }
                        )

                        val counterKillsPlayer = if (liveDefender.faction == currentState.playerFaction && counterOutcome.updatedAttacker.totalWounds == 0) 1 else 0
                        val counterKillsEnemy = if (liveDefender.faction != currentState.playerFaction && counterOutcome.updatedAttacker.totalWounds == 0) 1 else 0

                        val stateAfterCounter = currentState.copy(
                            units = afterCounterUnits,
                            projectiles = (currentState.projectiles + counterProj).filter { counterNow - it.startTimeMs < 1800 },
                            floatingTexts = (currentState.floatingTexts + counterFloats).filter { counterNow - it.startTimeMs < 3200 },
                            playerKills = currentState.playerKills + counterKillsPlayer,
                            enemyKills = currentState.enemyKills + counterKillsEnemy
                        )
                        val afterCounter = checkVictoryOrUpdate(recomputeSelectionOverlays(stateAfterCounter))
                        _battleState.value = afterCounter
                        syncCoopHostIfNeeded(afterCounter)
                        scheduleVfxCleanup()
                        delay(500L)
                    }
                }
            }
            _battleState.update { st ->
                if (!st.active) st else recomputeSelectionOverlays(st.copy(isAnimating = false))
            }
            syncCoopHostIfNeeded(_battleState.value)
        }
    }

    fun selectWeaponMode(mode: WeaponMode, fromLanClient: Boolean = false) {
        if (shouldForwardToLanHost(fromLanClient)) {
            forwardClientAction("WEAPON_MODE") { it.put("mode", mode.name) }
            return
        }
        val state = _battleState.value
        if (!state.isPlayerTurn || state.isAnimating) return
        val selected = state.selectedUnit ?: return
        if (!state.canCommandUnit(selected)) return
        sound.play(SoundType.CLICK)

        if (mode == WeaponMode.SKILL) {
            val selfUpdated = TacticalRules.activateSelfSkillIfApplicable(selected)
            if (selfUpdated != null) {
                val units = TacticalRules.applyAuras(state.units.map { if (it.uid == selfUpdated.uid) selfUpdated else it })
                spawnFloatingText(selected.pos, "BOUNDING LEAP!", 0xFF38D96CL)
                val next = recomputeSelectionOverlays(
                    state.copy(units = units, weaponMode = WeaponMode.MAIN, movePreview = null, targetPreview = null)
                )
                _battleState.value = next
                syncCoopHostIfNeeded(next)
                return
            }
        }

        val nextMode = if (state.weaponMode == mode) WeaponMode.MAIN else mode
        val next = recomputeSelectionOverlays(
            state.copy(
                weaponMode = nextMode,
                moveModeActive = false,
                movePreview = null,
                chargeModeActive = false,
                chargePreview = null,
                targetPreview = null
            )
        )
        _battleState.value = next
        syncCoopHostIfNeeded(next)
    }

    fun rotateSelectedUnit(left: Boolean, fromLanClient: Boolean = false) {
        if (shouldForwardToLanHost(fromLanClient)) {
            forwardClientAction("ROTATE") { it.put("left", left) }
            return
        }
        val state = _battleState.value
        if (!state.isPlayerTurn || state.isAnimating) return
        val selected = state.selectedUnit ?: return
        if (!state.canCommandUnit(selected)) return
        sound.play(SoundType.CLICK)
        val delta = if (left) 45f else -45f
        val nextDeg = ((selected.facingDeg + delta) % 360f + 360f) % 360f
        val nextFacing = TacticalRules.nearestFacingFromDeg(nextDeg)
        val units = state.units.map {
            if (it.uid == selected.uid) it.copy(facing = nextFacing, facingDeg = nextDeg) else it
        }
        _battleState.value = state.copy(units = units, movePreview = null)
        syncCoopHostIfNeeded(_battleState.value)
    }

    fun toggleAutoFocus() {
        sound.play(SoundType.CLICK)
        _battleState.update { it.copy(autoFocusCamera = !it.autoFocusCamera) }
    }

    fun centerCamera() {
        sound.play(SoundType.CLICK)
        _battleState.update { state ->
            val target = state.selectedUnit?.pos ?: GridPos(MapGenerator.GRID_W / 2, MapGenerator.GRID_H / 2)
            state.copy(focusGridPos = target, centerTrigger = state.centerTrigger + 1)
        }
    }

    fun toggleSound() {
        sound.enabled = !sound.enabled
        if (sound.enabled) sound.play(SoundType.CLICK)
        _battleState.update { it.copy(soundEnabled = sound.enabled) }
    }

    fun toggleUnitCardCollapsed() {
        _battleState.update { it.copy(unitCardCollapsed = !it.unitCardCollapsed) }
    }

    fun dismissTurnSplash() {
        _battleState.update { it.copy(turnSplash = null) }
    }

    fun endPlayerTurn(fromLanClient: Boolean = false) {
        if (shouldForwardToLanHost(fromLanClient)) {
            forwardClientAction("END_TURN")
            return
        }
        val state = _battleState.value
        if (!state.isPlayerTurn || state.isAnimating) return
        sound.play(SoundType.TURN_HORN)

        if (!state.isCoopMode) {
            val aiFaction = if (state.playerFaction == Faction.MARINES) Faction.XENOS else Faction.MARINES
            val refreshedUnits = refreshFactionForNewTurn(state.units, aiFaction)
            val firstAi = refreshedUnits.firstOrNull { it.faction == aiFaction && it.isAlive }

            val updated = state.copy(
                activeFaction = aiFaction,
                units = TacticalRules.applyAuras(refreshedUnits),
                selectedUnitUid = firstAi?.uid,
                weaponMode = WeaponMode.MAIN,
                moveModeActive = false,
                movePreview = null,
                chargeModeActive = false,
                chargePreview = null,
                chargeableUids = emptySet(),
                reachableTiles = emptyMap(),
                attackableUids = emptySet(),
                targetPreview = null,
                isAiThinking = true,
                focusGridPos = if (state.autoFocusCamera) firstAi?.pos else state.focusGridPos,
                turnSplash = TurnSplashData(
                    round = state.round,
                    activeFaction = aiFaction,
                    isPlayerTurn = false,
                    marineModelsAlive = refreshedUnits.filter { it.faction == Faction.MARINES && it.isAlive }.sumOf { it.aliveModels },
                    xenosModelsAlive = refreshedUnits.filter { it.faction == Faction.XENOS && it.isAlive }.sumOf { it.aliveModels },
                    playerKills = state.playerKills,
                    playerLosses = state.enemyKills
                )
            )
            _battleState.value = updated
            scheduleSplashAutoDismiss()
            runAiTurnLoop(aiFaction)
            return
        }

        // 4-Player Co-op Sequential Turn Rotation (P1 -> P2 -> P3 -> P4 -> optional AI -> Next Round)
        val turnOrder = state.coopTurnOrder
        val nextPlayerIdx = ((state.coopTurnIndex + 1) until turnOrder.size).firstOrNull { idx ->
            val p = turnOrder[idx]
            p.isActive && state.units.any { it.ownerPlayerId == p.id && it.isAlive }
        }

        if (nextPlayerIdx != null) {
            val nextPlayer = turnOrder[nextPlayerIdx]
            val refreshedUnits = TacticalRules.applyAuras(refreshCoopPlayerForNewTurn(state.units, nextPlayer.id))
            val firstUnit = refreshedUnits.firstOrNull { it.ownerPlayerId == nextPlayer.id && it.isAlive }
            val updated = recomputeSelectionOverlays(
                state.copy(
                    activeFaction = nextPlayer.faction,
                    playerFaction = nextPlayer.faction,
                    units = refreshedUnits,
                    selectedUnitUid = firstUnit?.uid,
                    coopTurnIndex = nextPlayerIdx,
                    activeCoopPlayerId = nextPlayer.id,
                    activeCoopPlayerName = nextPlayer.name,
                    weaponMode = WeaponMode.MAIN,
                    moveModeActive = false,
                    movePreview = null,
                    chargeModeActive = false,
                    chargePreview = null,
                    targetPreview = null,
                    isAiThinking = false,
                    focusGridPos = if (state.autoFocusCamera) firstUnit?.pos else state.focusGridPos,
                    turnSplash = TurnSplashData(
                        round = state.round,
                        activeFaction = nextPlayer.faction,
                        isPlayerTurn = true,
                        marineModelsAlive = refreshedUnits.filter { it.faction == Faction.MARINES && it.isAlive }.sumOf { it.aliveModels },
                        xenosModelsAlive = refreshedUnits.filter { it.faction == Faction.XENOS && it.isAlive }.sumOf { it.aliveModels },
                        playerKills = state.playerKills,
                        playerLosses = state.enemyKills,
                        commanderLabel = "${nextPlayer.name.uppercase()} TURN"
                    )
                )
            )
            _battleState.value = updated
            syncCoopHostIfNeeded(updated)
            scheduleSplashAutoDismiss()
            return
        }

        // End of Co-op players in this round: check if AI opposition units remain
        val aliveAiUnits = state.units.filter { it.ownerPlayerId == "AI" && it.isAlive }
        if (aliveAiUnits.isNotEmpty()) {
            val aiFaction = aliveAiUnits.first().faction
            val refreshedUnits = TacticalRules.applyAuras(refreshCoopPlayerForNewTurn(state.units, "AI"))
            val firstAi = refreshedUnits.firstOrNull { it.ownerPlayerId == "AI" && it.isAlive }
            val updated = state.copy(
                activeFaction = aiFaction,
                units = refreshedUnits,
                selectedUnitUid = firstAi?.uid,
                activeCoopPlayerId = "AI",
                activeCoopPlayerName = "AI Command",
                weaponMode = WeaponMode.MAIN,
                moveModeActive = false,
                movePreview = null,
                chargeModeActive = false,
                chargePreview = null,
                chargeableUids = emptySet(),
                reachableTiles = emptyMap(),
                attackableUids = emptySet(),
                targetPreview = null,
                isAiThinking = true,
                focusGridPos = if (state.autoFocusCamera) firstAi?.pos else state.focusGridPos,
                turnSplash = TurnSplashData(
                    round = state.round,
                    activeFaction = aiFaction,
                    isPlayerTurn = false,
                    marineModelsAlive = refreshedUnits.filter { it.faction == Faction.MARINES && it.isAlive }.sumOf { it.aliveModels },
                    xenosModelsAlive = refreshedUnits.filter { it.faction == Faction.XENOS && it.isAlive }.sumOf { it.aliveModels },
                    playerKills = state.playerKills,
                    playerLosses = state.enemyKills,
                    commanderLabel = "AI COMMAND TURN"
                )
            )
            _battleState.value = updated
            syncCoopHostIfNeeded(updated)
            scheduleSplashAutoDismiss()
            runAiTurnLoop(aiFaction)
        } else {
            // No AI units -> advance directly to next Round and first alive Co-op player
            val nextRound = state.round + 1
            val firstIdx = turnOrder.indexOfFirst { p ->
                p.isActive && state.units.any { it.ownerPlayerId == p.id && it.isAlive }
            }.takeIf { it >= 0 } ?: 0
            val firstPlayer = turnOrder.getOrNull(firstIdx) ?: return
            val refreshedUnits = TacticalRules.applyAuras(refreshCoopPlayerForNewTurn(state.units, firstPlayer.id))
            val firstUnit = refreshedUnits.firstOrNull { it.ownerPlayerId == firstPlayer.id && it.isAlive }
            val updated = recomputeSelectionOverlays(
                state.copy(
                    round = nextRound,
                    activeFaction = firstPlayer.faction,
                    playerFaction = firstPlayer.faction,
                    units = refreshedUnits,
                    selectedUnitUid = firstUnit?.uid,
                    coopTurnIndex = firstIdx,
                    activeCoopPlayerId = firstPlayer.id,
                    activeCoopPlayerName = firstPlayer.name,
                    weaponMode = WeaponMode.MAIN,
                    moveModeActive = false,
                    movePreview = null,
                    chargeModeActive = false,
                    chargePreview = null,
                    targetPreview = null,
                    isAiThinking = false,
                    focusGridPos = if (state.autoFocusCamera) firstUnit?.pos else state.focusGridPos,
                    turnSplash = TurnSplashData(
                        round = nextRound,
                        activeFaction = firstPlayer.faction,
                        isPlayerTurn = true,
                        marineModelsAlive = refreshedUnits.filter { it.faction == Faction.MARINES && it.isAlive }.sumOf { it.aliveModels },
                        xenosModelsAlive = refreshedUnits.filter { it.faction == Faction.XENOS && it.isAlive }.sumOf { it.aliveModels },
                        playerKills = state.playerKills,
                        playerLosses = state.enemyKills,
                        commanderLabel = "${firstPlayer.name.uppercase()} TURN"
                    )
                )
            )
            _battleState.value = updated
            syncCoopHostIfNeeded(updated)
            scheduleSplashAutoDismiss()
        }
    }

    private fun runAiTurnLoop(aiFaction: Faction) {
        aiJob?.cancel()
        aiJob = viewModelScope.launch {
            delay(1700)
            var guard = 0
            while (guard < 40) {
                guard++
                val current = _battleState.value
                if (!current.active || current.summary != null || current.activeFaction != aiFaction) break

                val decision = withContext(Dispatchers.Default) {
                    AiController.decideNextAction(aiFaction, current.units, current.map.rows)
                }
                when (decision) {
                    is AiDecision.SelfSkill -> {
                        val units = current.units.map {
                            if (it.uid == decision.unitUid) TacticalRules.activateSelfSkillIfApplicable(it) ?: it else it
                        }
                        val u = units.firstOrNull { it.uid == decision.unitUid }
                        if (u != null) spawnFloatingText(u.pos, "LEAP!", 0xFFC77DFFL)
                        _battleState.update { it.copy(units = units, focusGridPos = if (it.autoFocusCamera) u?.pos else it.focusGridPos) }
                        syncCoopHostIfNeeded(_battleState.value)
                        delay(840)
                    }
                    is AiDecision.Teleport -> {
                        performTeleport(decision.unitUid, decision.targetPos)
                        delay(900)
                    }
                    is AiDecision.Advance -> {
                        val (roll, bonusCm) = TacticalRules.rollAdvanceBonus()
                        val units = current.units.map { u ->
                            if (u.uid != decision.unitUid) u
                            else u.copy(
                                moveCmLeft = u.moveCmLeft + bonusCm,
                                hasAdvanced = true,
                                activeBuffLabel = if (u.template.mainWeaponAssault) "ADVANCE +$roll\" (ASSAULT)" else "ADVANCE +$roll\""
                            )
                        }
                        val u = units.firstOrNull { it.uid == decision.unitUid }
                        if (u != null) spawnFloatingText(u.pos, "ADVANCE +$roll\"", 0xFF38D96CL)
                        _battleState.update { it.copy(units = units, focusGridPos = if (it.autoFocusCamera) u?.pos else it.focusGridPos) }
                        syncCoopHostIfNeeded(_battleState.value)
                        delay(650)
                    }
                    is AiDecision.Charge -> {
                        executeChargeAttempt(decision.unitUid, decision.preview)
                        delay(850)
                    }
                    is AiDecision.Move -> {
                        performMoveAlongPath(decision.unitUid, decision.path, decision.cost)
                        delay(700)
                    }
                    is AiDecision.Attack -> {
                        executeAttack(decision.attackerUid, decision.targetUid, decision.mode)
                        delay(1300)
                    }
                    AiDecision.TurnComplete -> break
                }
            }

            // Transition back to Player Turn & increment Round
            val endState = _battleState.value
            if (endState.active && endState.summary == null) {
                sound.play(SoundType.TURN_HORN)
                val nextRound = endState.round + 1
                if (endState.isCoopMode) {
                    val turnOrder = endState.coopTurnOrder
                    val firstIdx = turnOrder.indexOfFirst { p ->
                        p.isActive && endState.units.any { it.ownerPlayerId == p.id && it.isAlive }
                    }.takeIf { it >= 0 } ?: 0
                    val firstCoop = turnOrder.getOrNull(firstIdx)
                    if (firstCoop != null) {
                        val refreshed = TacticalRules.applyAuras(refreshCoopPlayerForNewTurn(endState.units, firstCoop.id))
                        val firstUnit = refreshed.firstOrNull { it.ownerPlayerId == firstCoop.id && it.isAlive }
                        val nextState = endState.copy(
                            round = nextRound,
                            activeFaction = firstCoop.faction,
                            playerFaction = firstCoop.faction,
                            units = refreshed,
                            selectedUnitUid = firstUnit?.uid,
                            coopTurnIndex = firstIdx,
                            activeCoopPlayerId = firstCoop.id,
                            activeCoopPlayerName = firstCoop.name,
                            weaponMode = WeaponMode.MAIN,
                            moveModeActive = false,
                            movePreview = null,
                            chargeModeActive = false,
                            chargePreview = null,
                            chargeableUids = emptySet(),
                            isAiThinking = false,
                            focusGridPos = if (endState.autoFocusCamera) firstUnit?.pos else endState.focusGridPos,
                            turnSplash = TurnSplashData(
                                round = nextRound,
                                activeFaction = firstCoop.faction,
                                isPlayerTurn = true,
                                marineModelsAlive = refreshed.filter { it.faction == Faction.MARINES && it.isAlive }.sumOf { it.aliveModels },
                                xenosModelsAlive = refreshed.filter { it.faction == Faction.XENOS && it.isAlive }.sumOf { it.aliveModels },
                                playerKills = endState.playerKills,
                                playerLosses = endState.enemyKills,
                                commanderLabel = "${firstCoop.name.uppercase()} TURN"
                            )
                        )
                        val ready = recomputeSelectionOverlays(nextState)
                        _battleState.value = ready
                        syncCoopHostIfNeeded(ready)
                        scheduleSplashAutoDismiss()
                    }
                } else {
                    val refreshed = TacticalRules.applyAuras(refreshFactionForNewTurn(endState.units, endState.playerFaction))
                    val firstPlayer = refreshed.firstOrNull { it.faction == endState.playerFaction && it.isAlive }
                    val nextState = endState.copy(
                        round = nextRound,
                        activeFaction = endState.playerFaction,
                        units = refreshed,
                        selectedUnitUid = firstPlayer?.uid,
                        weaponMode = WeaponMode.MAIN,
                        moveModeActive = false,
                        movePreview = null,
                        chargeModeActive = false,
                        chargePreview = null,
                        chargeableUids = emptySet(),
                        isAiThinking = false,
                        focusGridPos = if (endState.autoFocusCamera) firstPlayer?.pos else endState.focusGridPos,
                        turnSplash = TurnSplashData(
                            round = nextRound,
                            activeFaction = endState.playerFaction,
                            isPlayerTurn = true,
                            marineModelsAlive = refreshed.filter { it.faction == Faction.MARINES && it.isAlive }.sumOf { it.aliveModels },
                            xenosModelsAlive = refreshed.filter { it.faction == Faction.XENOS && it.isAlive }.sumOf { it.aliveModels },
                            playerKills = endState.playerKills,
                            playerLosses = endState.enemyKills
                        )
                    )
                    _battleState.value = recomputeSelectionOverlays(nextState)
                    scheduleSplashAutoDismiss()
                }
            }
        }
    }

    private fun refreshCoopPlayerForNewTurn(units: List<BattleUnit>, ownerId: String): List<BattleUnit> =
        units.map { u ->
            if (!u.isAlive || u.ownerPlayerId != ownerId) u
            else {
                val nextBuffTurns = (u.buffTurnsLeft - 1).coerceAtLeast(0)
                val nextDamageReduction = (u.damageReductionTurns - 1).coerceAtLeast(0)
                u.copy(
                    moveCmLeft = u.template.moveCm.toFloat(),
                    hasAdvanced = false,
                    hasCharged = false,
                    hasShot = false,
                    hasFought = false,
                    skillCooldownLeft = (u.skillCooldownLeft - 1).coerceAtLeast(0),
                    pistolCooldownLeft = (u.pistolCooldownLeft - 1).coerceAtLeast(0),
                    buffTurnsLeft = nextBuffTurns,
                    damageReductionTurns = nextDamageReduction,
                    activeBuffLabel = if (nextBuffTurns > 0 || nextDamageReduction > 0) u.activeBuffLabel else null
                )
            }
        }

    private fun refreshFactionForNewTurn(units: List<BattleUnit>, faction: Faction): List<BattleUnit> =
        units.map { u ->
            if (!u.isAlive || u.faction != faction) u
            else {
                val nextBuffTurns = (u.buffTurnsLeft - 1).coerceAtLeast(0)
                val nextDamageReduction = (u.damageReductionTurns - 1).coerceAtLeast(0)
                u.copy(
                    moveCmLeft = u.template.moveCm.toFloat(),
                    hasAdvanced = false,
                    hasCharged = false,
                    hasShot = false,
                    hasFought = false,
                    skillCooldownLeft = (u.skillCooldownLeft - 1).coerceAtLeast(0),
                    pistolCooldownLeft = (u.pistolCooldownLeft - 1).coerceAtLeast(0),
                    buffTurnsLeft = nextBuffTurns,
                    damageReductionTurns = nextDamageReduction,
                    activeBuffLabel = if (nextBuffTurns > 0 || nextDamageReduction > 0) u.activeBuffLabel else null
                )
            }
        }

    private fun recomputeSelectionOverlays(state: BattleUiState): BattleUiState {
        val selected = state.selectedUnit
        if (selected == null || !state.canCommandUnit(selected) || state.isAnimating) {
            return state.copy(
                reachableTiles = emptyMap(),
                moveParents = emptyMap(),
                attackableUids = emptySet(),
                chargeableUids = emptySet()
            )
        }

        if (selected.template.archetype == UnitArchetype.TERMINATOR && state.weaponMode == WeaponMode.SKILL) {
            val canTeleport = selected.skillCooldownLeft == 0 && !selected.hasAdvanced && !selected.hasCharged
            val teleportTiles = if (canTeleport) {
                TacticalRules.computeTeleportTiles(selected, state.map.rows, state.units, maxDist = 9, minEnemyDist = 5)
            } else emptySet()
            return state.copy(
                reachableTiles = teleportTiles.associateWith { 0 },
                moveParents = emptyMap(),
                attackableUids = emptySet(),
                chargeableUids = emptySet()
            )
        }

        val moveResult = if (selected.moveCmLeft >= 1.5f && !state.chargeModeActive) {
            TacticalRules.computeReachableTiles(selected, state.map.rows, state.units)
        } else null

        val chargeable = if (state.chargeModeActive) {
            TacticalRules.computeChargeableTargets(selected, state.units, state.map.rows)
        } else emptySet()

        val attackable = if (state.chargeModeActive) {
            emptySet()
        } else {
            state.units
                .filter { it.isAlive && it.faction != selected.faction }
                .filter { enemy ->
                    val p = TacticalRules.canTarget(selected, enemy, state.weaponMode, state.map.rows, state.units)
                    p != null && p.inRange && p.hasLos
                }
                .map { it.uid }
                .toSet()
        }

        return state.copy(
            reachableTiles = moveResult?.costs ?: emptyMap(),
            moveParents = moveResult?.parents ?: emptyMap(),
            attackableUids = attackable,
            chargeableUids = chargeable
        )
    }

    private fun checkVictoryOrUpdate(state: BattleUiState): BattleUiState {
        val marinesAlive = state.units.any { it.faction == Faction.MARINES && it.isAlive }
        val xenosAlive = state.units.any { it.faction == Faction.XENOS && it.isAlive }
        if (marinesAlive && xenosAlive) return state

        aiJob?.cancel()
        actionJob?.cancel()
        sound.play(SoundType.VICTORY)
        val winner = if (marinesAlive) Faction.MARINES else Faction.XENOS
        val playerWon = winner == state.playerFaction
        val totalDmg = state.units.filter { it.faction == state.playerFaction }.sumOf { it.damageDealt }

        viewModelScope.launch {
            repository.recordBattle(
                BattleRecordEntity(
                    winnerFaction = winner.displayName,
                    playerFaction = state.playerFaction.displayName,
                    playerWon = playerWon,
                    rounds = state.round,
                    playerKills = state.playerKills,
                    enemyKills = state.enemyKills
                )
            )
        }

        return state.copy(
            isAiThinking = false,
            isAnimating = false,
            reachableTiles = emptyMap(),
            attackableUids = emptySet(),
            targetPreview = null,
            summary = BattleSummary(
                winner = winner,
                playerWon = playerWon,
                rounds = state.round,
                playerKills = state.playerKills,
                enemyKills = state.enemyKills,
                totalDamageDealt = totalDmg
            )
        )
    }

    private fun spawnFloatingText(pos: GridPos, text: String, colorArgb: Long) {
        val now = System.currentTimeMillis()
        val fx = FloatingTextVfx(nextIdCounter++, pos.x.toFloat(), pos.y.toFloat(), text, colorArgb, now)
        _battleState.update { state ->
            state.copy(floatingTexts = (state.floatingTexts + fx).filter { now - it.startTimeMs < 3200 })
        }
        scheduleVfxCleanup()
    }

    private fun scheduleVfxCleanup() {
        viewModelScope.launch {
            delay(3400)
            val now = System.currentTimeMillis()
            _battleState.update { state ->
                val nextProj = state.projectiles.filter { now - it.startTimeMs < 1800 }
                val nextFloats = state.floatingTexts.filter { now - it.startTimeMs < 3200 }
                if (nextProj.size == state.projectiles.size && nextFloats.size == state.floatingTexts.size) {
                    state
                } else {
                    state.copy(projectiles = nextProj, floatingTexts = nextFloats)
                }
            }
        }
    }

    private fun scheduleSplashAutoDismiss() {
        viewModelScope.launch {
            delay(2800)
            _battleState.update { it.copy(turnSplash = null) }
        }
    }
}
