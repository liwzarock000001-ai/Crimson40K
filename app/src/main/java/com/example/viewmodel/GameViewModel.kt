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
import com.example.engine.BackgroundMusicPlayer
import com.example.engine.SoundEffects
import com.example.engine.SoundType
import com.example.engine.TacticalRules
import com.example.model.AppScreen
import com.example.model.BattleSummary
import com.example.model.BattleUnit
import com.example.model.ChargePreview
import com.example.model.CombatLogEntry
import com.example.model.CoopNetworkRole
import com.example.model.CoopPlayer
import com.example.model.CoopRosterUnit
import com.example.model.CoopSlotMode
import com.example.model.CoopState
import com.example.model.DarkPactChoice
import com.example.model.DiceStepDetail
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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import org.json.JSONObject
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.random.Random

@Immutable
data class SetupUiState(
    val playerFaction: Faction = Faction.MARINES,
    val enemyFaction: Faction = Faction.BLACK_LEGION,
    val pointLimit: Int = 2000,
    val templates: Map<String, UnitTemplate> = DefaultCatalog.defaultTemplates.associateBy { it.id },
    val marineRoster: List<RosterSlot> = DefaultCatalog.defaultMarineRoster(),
    val blackLegionRoster: List<RosterSlot> = DefaultCatalog.defaultBlackLegionRoster(),
    val xenosRoster: List<RosterSlot> = DefaultCatalog.defaultXenosRoster(),
    val editingTemplateId: String? = null,
    val statusMessage: String? = null
) {
    val marinePoints: Int
        get() = marineRoster.sumOf { templates[it.templateId]?.points ?: 0 }
    val blackLegionPoints: Int
        get() = blackLegionRoster.sumOf { templates[it.templateId]?.points ?: 0 }
    val xenosPoints: Int
        get() = xenosRoster.sumOf { templates[it.templateId]?.points ?: 0 }
    fun rosterFor(faction: Faction): List<RosterSlot> = when (faction) {
        Faction.MARINES -> marineRoster
        Faction.BLACK_LEGION -> blackLegionRoster
        Faction.XENOS -> xenosRoster
    }
    fun pointsFor(faction: Faction): Int = when (faction) {
        Faction.MARINES -> marinePoints
        Faction.BLACK_LEGION -> blackLegionPoints
        Faction.XENOS -> xenosPoints
    }
    val validationWarning: String?
        get() {
            val playerRoster = rosterFor(playerFaction)
            val enemyRoster = rosterFor(enemyFaction)
            val playerPts = pointsFor(playerFaction)
            val enemyPts = pointsFor(enemyFaction)
            return when {
                playerFaction == enemyFaction -> "Commander and Enemy must be different factions."
                playerRoster.isEmpty() -> "${playerFaction.displayName} require at least 1 squad."
                enemyRoster.isEmpty() -> "${enemyFaction.displayName} require at least 1 squad."
                playerPts > pointLimit -> "${playerFaction.displayName} exceed $pointLimit pt limit."
                enemyPts > pointLimit -> "${enemyFaction.displayName} exceed $pointLimit pt limit."
                else -> null
            }
        }
    val canDeploy: Boolean get() = validationWarning == null
}

@Immutable
data class BattleUiState(
    val active: Boolean = false,
    val round: Int = 1,
    val activeFaction: Faction = Faction.MARINES,
    val playerFaction: Faction = Faction.MARINES,
    val enemyFaction: Faction = Faction.XENOS,
    val map: SectorMap = SectorMap(1L, MapGenerator.generateSectorMap(1L)),
    val units: List<BattleUnit> = emptyList(),
    val selectedUnitUid: String? = null,
    val weaponMode: WeaponMode = WeaponMode.MAIN,
    val selectedDarkPact: DarkPactChoice = DarkPactChoice.NONE,
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
    val autoFocusCamera: Boolean = false,
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
    val saveBannerMessage: String? = null,
    val isDeploymentPhase: Boolean = false,
    val reserveUnits: List<BattleUnit> = emptyList(),
    val selectedReserveUnitUid: String? = null,
    val deploymentTurnPlayerId: String? = null,
    val deploymentTurnPlayerName: String = "",
    val deploymentTeamIndex: Int = 0,
    val combatLogs: List<CombatLogEntry> = emptyList()
) {
    val isPlayerTurn: Boolean
        get() = !isAiThinking && summary == null && if (isDeploymentPhase) {
            if (!isCoopMode) {
                deploymentTeamIndex == 0
            } else {
                val active = deploymentTurnPlayerId ?: return false
                val localId = localControlledPlayerId ?: return false
                active == localId
            }
        } else if (!isCoopMode) {
            activeFaction == playerFaction
        } else {
            val active = activeCoopPlayerId ?: return false
            val localId = localControlledPlayerId ?: return false
            active == localId
        }

    fun canAcceptInput(isRemoteClientAction: Boolean = false): Boolean =
        !isAnimating && !isAiThinking && summary == null && (isRemoteClientAction || isPlayerTurn)

    val selectedUnit: BattleUnit? get() = units.firstOrNull { it.uid == selectedUnitUid && it.isAlive }
    val marineAliveModels: Int get() = units.filter { it.faction == Faction.MARINES && it.isAlive }.sumOf { it.aliveModels }
    val blackLegionAliveModels: Int get() = units.filter { it.faction == Faction.BLACK_LEGION && it.isAlive }.sumOf { it.aliveModels }
    val xenosAliveModels: Int get() = units.filter { it.faction == Faction.XENOS && it.isAlive }.sumOf { it.aliveModels }
    val resolvedEnemyFaction: Faction
        get() = units.firstOrNull { it.faction != playerFaction }?.faction ?: enemyFaction
    val playerAliveModels: Int
        get() = units.filter {
            it.isAlive && if (isCoopMode) it.effectiveTeam == 0 else it.faction == playerFaction
        }.sumOf { it.aliveModels }
    val enemyAliveModels: Int
        get() = units.filter {
            it.isAlive && if (isCoopMode) it.effectiveTeam == 1 else it.faction != playerFaction
        }.sumOf { it.aliveModels }

    fun canCommandUnit(unit: BattleUnit, isRemoteClientAction: Boolean = false): Boolean {
        if (!unit.isAlive || isDeploymentPhase) return false
        if (!isRemoteClientAction && !isPlayerTurn) return false
        return if (!isCoopMode) {
            unit.faction == playerFaction
        } else {
            unit.ownerPlayerId == activeCoopPlayerId
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
            bgmVolume = prefs.getFloat("bgm_volume", 0.8f).coerceIn(0f, 1f),
            bgmEnabled = prefs.getBoolean("bgm_enabled", true),
            graphicsLevel = prefs.getInt("graphics_level", 3).coerceIn(1, 4),
            screenBrightness = prefs.getFloat("screen_brightness", 1.0f).coerceIn(0.5f, 1.5f)
        )
    )
    val settingsState: StateFlow<SettingsState> = _settingsState.asStateFlow()

    val savedConfig: StateFlow<SavedConfigEntity?>
    val savedSkirmishBattle: StateFlow<SavedSkirmishBattleEntity?>

    private var nextIdCounter = 100L
    private var lastDeployTeam0Idx = -1
    private var lastDeployTeam1Idx = -1
    private var aiJob: Job? = null
    private var actionJob: Job? = null

    // Cloud Online Co-op bridge callbacks (wired in MainActivity only after Google Sign-In)
    var onlineLobbySyncPublisher: ((CoopState, Map<String, UnitTemplate>) -> Unit)? = null
    var onlineHostStatePublisher: ((BattleUiState, Map<String, UnitTemplate>) -> Unit)? = null
    var onlineClientActionSender: ((actionType: String, wx: Float?, wy: Float?, cm: Float?, mode: WeaponMode?, left: Boolean?) -> Unit)? = null

    init {
        val dao = AppDatabase.getInstance(application).gameDao()
        repository = GameRepository(dao)
        savedConfig = repository.savedConfig
            .catch { emit(null) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
        savedSkirmishBattle = repository.savedSkirmishBattle
            .catch { emit(null) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

        val initialSettings = _settingsState.value
        sound.enabled = initialSettings.sfxEnabled
        sound.volume = initialSettings.soundVolume
        BackgroundMusicPlayer.init(application)
        BackgroundMusicPlayer.setVolume(initialSettings.bgmVolume)
        BackgroundMusicPlayer.setEnabled(initialSettings.bgmEnabled)
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
                val parsedEnemy = saved.enemyFaction.takeIf { it.isNotBlank() }
                    ?.let { runCatching { Faction.valueOf(it) }.getOrNull() }
                    ?.takeIf { it != faction }
                    ?: if (faction == Faction.MARINES) Faction.BLACK_LEGION else Faction.MARINES
                val templates = DefaultCatalog.deserializeTemplates(saved.customTemplatesJson)
                val marines = DefaultCatalog.deserializeRoster(saved.marineRosterJson, DefaultCatalog.defaultMarineRoster())
                val blackLegion = DefaultCatalog.deserializeRoster(saved.blackLegionRosterJson, DefaultCatalog.defaultBlackLegionRoster())
                val xenos = DefaultCatalog.deserializeRoster(saved.xenosRosterJson, DefaultCatalog.defaultXenosRoster())
                _setupState.update {
                    it.copy(
                        playerFaction = faction,
                        enemyFaction = parsedEnemy,
                        pointLimit = saved.pointLimit.coerceIn(1000, 5000),
                        templates = templates,
                        marineRoster = marines,
                        blackLegionRoster = blackLegion,
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
            val normalizedUnits = units.map { u ->
                if (u.ownerPlayerId == "PLAYER") {
                    u.copy(ownerPlayerId = if (u.faction == playerFaction) "player" else "AI")
                } else u
            }
            val enemyFaction = normalizedUnits.firstOrNull { it.faction != playerFaction }?.faction
                ?: if (playerFaction == Faction.MARINES) Faction.XENOS else Faction.MARINES
            val rawMap = MapGenerator.generateSectorMap(saved.mapId)
            val map = SectorMap(saved.mapId, rawMap)
            val firstUnit = normalizedUnits.firstOrNull { it.faction == playerFaction && it.isAlive }
            val settings = _settingsState.value
            val playerModels = normalizedUnits.filter { it.faction == playerFaction && it.isAlive }.sumOf { it.aliveModels }
            val enemyModels = normalizedUnits.filter { it.faction != playerFaction && it.isAlive }.sumOf { it.aliveModels }

            val loadedState = BattleUiState(
                active = true,
                round = saved.round.coerceAtLeast(1),
                activeFaction = activeFaction,
                playerFaction = playerFaction,
                enemyFaction = enemyFaction,
                map = map,
                units = TacticalRules.applyAuras(normalizedUnits),
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
                    marineModelsAlive = normalizedUnits.filter { it.faction == Faction.MARINES && it.isAlive }.sumOf { it.aliveModels },
                    xenosModelsAlive = normalizedUnits.filter { it.faction == Faction.XENOS && it.isAlive }.sumOf { it.aliveModels },
                    playerKills = saved.playerKills,
                    playerLosses = saved.enemyKills,
                    commanderLabel = "SAVED SKIRMISH RESUMED",
                    playerFaction = playerFaction,
                    enemyFaction = enemyFaction,
                    playerModelsAlive = playerModels,
                    enemyModelsAlive = enemyModels
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
                            slotMode = if (p.slotMode == CoopSlotMode.HOST) CoopSlotMode.LAN_CLIENT else p.slotMode,
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
        status: String,
        customTemplates: Map<String, UnitTemplate>? = null,
        pointLimit: Int? = null,
        localSlotId: String? = null,
        isHost: Boolean? = null
    ) {
        if (status == "FINISHED") return
        _coopState.update { current ->
            val resolvedIsHost = isHost ?: (current.networkRole == CoopNetworkRole.LOCAL_OR_HOST)
            val resolvedLocalId = localSlotId?.takeIf { it.isNotBlank() } ?: current.localPlayerId
            val resolvedPlayers = if (players.isNotEmpty()) players else current.players
            val joinedPeers = resolvedPlayers.count { !it.isHost && it.connectedClient }
            val nextStatusText = if (resolvedIsHost) {
                if (joinedPeers > 0) "Cloud Room #$roomCode • $joinedPeers online ally" else "Cloud Room #$roomCode Live"
            } else {
                "Cloud Room #$roomCode • $status"
            }
            current.copy(
                inRoom = true,
                roomCode = roomCode,
                networkRole = if (resolvedIsHost) CoopNetworkRole.LOCAL_OR_HOST else CoopNetworkRole.CLIENT,
                serverRunning = resolvedIsHost,
                clientConnected = !resolvedIsHost,
                localPlayerId = resolvedLocalId,
                hostCommanderName = hostName.ifBlank { current.hostCommanderName },
                players = resolvedPlayers,
                assignedUnits = assignedUnits,
                spawnAiEnemyIfSameFaction = spawnAi,
                pointLimit = (pointLimit ?: current.pointLimit).coerceIn(500, 5000),
                networkStatus = nextStatusText,
                remoteTemplates = customTemplates ?: current.remoteTemplates
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
                networkStatus = "Ready for long-distance Online Co-op",
                remoteTemplates = null
            )
        }
        if (_battleState.value.isCoopMode && _battleState.value.active) {
            aiJob?.cancel()
            actionJob?.cancel()
            _battleState.update { st -> st.copy(active = false, summary = null, isAiThinking = false, isAnimating = false) }
        }
    }

    private fun broadcastLobbyUpdate() {
        val st = _coopState.value
        if (st.inRoom) {
            onlineLobbySyncPublisher?.invoke(st, _setupState.value.templates)
        }
    }

    private fun canEditCoopSlotInLobby(coop: CoopState, playerId: String): Boolean {
        if (coop.networkRole == CoopNetworkRole.CLIENT) {
            return playerId == coop.localPlayerId
        }
        if (playerId == "p1") return true
        val slot = coop.players.firstOrNull { it.id == playerId } ?: return false
        return slot.slotMode == CoopSlotMode.AI_BOT
    }

    fun updateCoopPlayerFaction(playerId: String, faction: Faction) {
        val currentCoop = _coopState.value
        if (!canEditCoopSlotInLobby(currentCoop, playerId)) return
        sound.play(SoundType.CLICK)
        _coopState.update { state ->
            val currentPlayer = state.players.firstOrNull { it.id == playerId }
            val updated = state.players.map { p ->
                if (p.id == playerId) p.copy(faction = faction) else p
            }
            val updatedUnits = if (currentPlayer != null && currentPlayer.faction != faction) {
                val others = state.assignedUnits.filterNot { it.ownerPlayerId == playerId }
                val starterIds = when (faction) {
                    Faction.MARINES -> listOf("intercessors", "hellblasters")
                    Faction.BLACK_LEGION -> listOf("legionaries", "legionaries")
                    Faction.XENOS -> listOf("warriors", "termagants")
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
        broadcastLobbyUpdate()
    }

    fun cycleCoopPlayerSlotMode(playerId: String) {
        if (playerId == "p1") return // Host slot is always HOST
        if (_coopState.value.networkRole == CoopNetworkRole.CLIENT) return
        sound.play(SoundType.CLICK)
        _coopState.update { state ->
            val targetPlayer = state.players.firstOrNull { it.id == playerId } ?: return@update state
            if (targetPlayer.connectedClient) {
                return@update state.copy(lobbyError = "${targetPlayer.name} is currently connected to this slot.")
            }
            val nextMode = when (targetPlayer.slotMode) {
                CoopSlotMode.LAN_CLIENT -> CoopSlotMode.AI_BOT
                CoopSlotMode.AI_BOT -> CoopSlotMode.OFF
                CoopSlotMode.OFF -> CoopSlotMode.LAN_CLIENT
                CoopSlotMode.HOST -> CoopSlotMode.HOST
            }
            val defaultHumanName = when (playerId) {
                "p2" -> "Commander 2"
                "p3" -> "Commander 3"
                else -> "Commander 4"
            }
            val defaultAiName = when (playerId) {
                "p2" -> "AI Ally (P2)"
                "p3" -> "AI Vanguard (P3)"
                else -> "AI Ravager (P4)"
            }
            val nextName = when (nextMode) {
                CoopSlotMode.AI_BOT -> if (targetPlayer.name.startsWith("Commander") || targetPlayer.name.startsWith("AI")) defaultAiName else targetPlayer.name
                CoopSlotMode.LAN_CLIENT -> if (targetPlayer.name.startsWith("AI")) defaultHumanName else targetPlayer.name
                else -> targetPlayer.name
            }
            val updatedPlayers = state.players.map { p ->
                if (p.id != playerId) p
                else p.copy(slotMode = nextMode, name = nextName, connectedClient = false)
            }
            // Ensure an AI_BOT slot has starter squads if empty so it deploys right away
            val hasUnits = state.assignedUnits.any { it.ownerPlayerId == playerId }
            val updatedUnits = if (nextMode == CoopSlotMode.AI_BOT && !hasUnits) {
                val starterIds = when (targetPlayer.faction) {
                    Faction.MARINES -> listOf("intercessors", "hellblasters")
                    Faction.BLACK_LEGION -> listOf("legionaries", "legionaries")
                    Faction.XENOS -> listOf("warriors", "termagants")
                }
                state.assignedUnits + starterIds.map { tid ->
                    CoopRosterUnit("c_${nextIdCounter++}", playerId, tid)
                }
            } else {
                state.assignedUnits
            }
            state.copy(players = updatedPlayers, assignedUnits = updatedUnits, lobbyError = null)
        }
        broadcastLobbyUpdate()
    }

    fun updateCoopPlayerName(playerId: String, name: String) {
        val currentCoop = _coopState.value
        if (!canEditCoopSlotInLobby(currentCoop, playerId)) return
        _coopState.update { state ->
            val updated = state.players.map { p ->
                if (p.id == playerId) p.copy(name = name.take(22)) else p
            }
            state.copy(players = updated)
        }
        broadcastLobbyUpdate()
    }

    fun selectCoopConfigPlayer(playerId: String) {
        sound.play(SoundType.CLICK)
        _coopState.update { it.copy(selectedConfigPlayerId = playerId, lobbyError = null) }
    }

    fun setCoopPointLimit(points: Int) {
        if (_coopState.value.networkRole == CoopNetworkRole.CLIENT) return
        sound.play(SoundType.CLICK)
        _coopState.update {
            it.copy(pointLimit = points.coerceIn(500, 5000), lobbyError = null)
        }
        broadcastLobbyUpdate()
    }

    fun addCoopUnitForPlayer(playerId: String, templateId: String) {
        val current = _coopState.value
        if (!canEditCoopSlotInLobby(current, playerId)) return
        val templates = if (current.networkRole == CoopNetworkRole.CLIENT) {
            current.remoteTemplates ?: _setupState.value.templates
        } else {
            _setupState.value.templates
        }
        val unitTpl = templates[templateId]
        val unitCost = unitTpl?.points ?: 0
        val currentUsed = current.assignedUnits
            .filter { it.ownerPlayerId == playerId }
            .sumOf { templates[it.templateId]?.points ?: 0 }
        if (currentUsed + unitCost > current.pointLimit) {
            sound.play(SoundType.CLICK)
            val remaining = (current.pointLimit - currentUsed).coerceAtLeast(0)
            val name = unitTpl?.name ?: templateId
            _coopState.update {
                it.copy(lobbyError = "Cannot add $name ($unitCost PT): only $remaining / ${current.pointLimit} PT left")
            }
            return
        }
        sound.play(SoundType.CLICK)
        val entry = CoopRosterUnit("c_${nextIdCounter++}", playerId, templateId)
        _coopState.update { it.copy(assignedUnits = it.assignedUnits + entry, lobbyError = null) }
        broadcastLobbyUpdate()
    }

    fun removeCoopUnit(entryId: String) {
        val current = _coopState.value
        val targetUnit = current.assignedUnits.firstOrNull { it.entryId == entryId }
        if (targetUnit != null && !canEditCoopSlotInLobby(current, targetUnit.ownerPlayerId)) return
        sound.play(SoundType.CLICK)
        _coopState.update {
            it.copy(assignedUnits = it.assignedUnits.filterNot { u -> u.entryId == entryId }, lobbyError = null)
        }
        broadcastLobbyUpdate()
    }

    fun toggleCoopAiOpposition() {
        if (_coopState.value.networkRole == CoopNetworkRole.CLIENT) return
        sound.play(SoundType.CLICK)
        _coopState.update { it.copy(spawnAiEnemyIfSameFaction = !it.spawnAiEnemyIfSameFaction) }
        broadcastLobbyUpdate()
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
        if (coop.networkRole == CoopNetworkRole.CLIENT) return
        val activePlayers = coop.players.filter { p ->
            p.slotMode == CoopSlotMode.HOST ||
                p.slotMode == CoopSlotMode.AI_BOT ||
                (p.slotMode == CoopSlotMode.LAN_CLIENT && p.connectedClient)
        }
        if (activePlayers.isEmpty()) return
        val templates = _setupState.value.templates
        val overPlayer = activePlayers.firstOrNull { p ->
            val pPts = coop.assignedUnits
                .filter { it.ownerPlayerId == p.id }
                .sumOf { templates[it.templateId]?.points ?: 0 }
            pPts > coop.pointLimit
        }
        if (overPlayer != null) {
            _coopState.update {
                it.copy(lobbyError = "${overPlayer.name} exceeds ${coop.pointLimit} PT limit! Remove squads or raise limit.")
            }
            return
        }
        val team0HasUnits = activePlayers.any { it.id in listOf("p1", "p2") && coop.assignedUnits.any { u -> u.ownerPlayerId == it.id } }
        if (!team0HasUnits) {
            _coopState.update {
                it.copy(lobbyError = "Team 1 needs at least 1 squad before starting.")
            }
            return
        }
        val team1HasUnitsCheck = activePlayers.any { it.id in listOf("p3", "p4") && coop.assignedUnits.any { u -> u.ownerPlayerId == it.id } }
        if (!team1HasUnitsCheck && !coop.spawnAiEnemyIfSameFaction) {
            _coopState.update {
                it.copy(lobbyError = "Team 2 needs an AI Bot, joined player, or AI Opposition enabled.")
            }
            return
        }

        aiJob?.cancel()
        actionJob?.cancel()
        sound.play(SoundType.TURN_HORN)

        val mapId = System.currentTimeMillis()
        val rawMap = MapGenerator.generateSectorMap(mapId)
        val map = SectorMap(mapId, rawMap)
        val reserveUnits = mutableListOf<BattleUnit>()

        for (player in activePlayers) {
            val playerSlots = coop.assignedUnits.filter { it.ownerPlayerId == player.id }
            val teamIndex = if (player.id in listOf("p1", "p2")) 0 else 1
            val facing = if (teamIndex == 0) Facing.EAST else Facing.WEST
            for (slot in playerSlots) {
                val tpl = templates[slot.templateId] ?: continue
                reserveUnits.add(
                    BattleUnit(
                        uid = "COOP_${player.id}_${nextIdCounter++}",
                        template = tpl,
                        faction = player.faction,
                        x = -1,
                        y = -1,
                        facing = facing,
                        totalWounds = tpl.maxTotalWounds,
                        maxTotalWounds = tpl.maxTotalWounds,
                        moveCmLeft = tpl.moveCm.toFloat(),
                        worldX = -1f,
                        worldY = -1f,
                        facingDeg = facing.yawDegrees(),
                        ownerPlayerId = player.id,
                        ownerPlayerName = player.name
                    )
                )
            }
        }

        val team1HasUnits = activePlayers.any { it.id in listOf("p3", "p4") && coop.assignedUnits.any { u -> u.ownerPlayerId == it.id } }
        val team0PrimaryFaction = activePlayers.firstOrNull { p ->
            p.id in listOf("p1", "p2") && coop.assignedUnits.any { u -> u.ownerPlayerId == p.id }
        }?.faction ?: Faction.MARINES
        if (!team1HasUnits && coop.spawnAiEnemyIfSameFaction) {
            val aiFaction = when (team0PrimaryFaction) {
                Faction.MARINES -> Faction.XENOS
                Faction.BLACK_LEGION, Faction.XENOS -> Faction.MARINES
            }
            val aiTemplateIds = when (aiFaction) {
                Faction.XENOS -> listOf("carnifex", "warriors", "von_ryans_leaper", "termagants", "hormagaunts")
                Faction.BLACK_LEGION -> listOf("legionaries", "legionaries", "legionaries")
                Faction.MARINES -> listOf("captain", "dreadnought", "terminators", "bladeguard_veterans", "hellblasters", "intercessors", "assault_intercessors")
            }
            for (tid in aiTemplateIds) {
                val tpl = templates[tid] ?: continue
                reserveUnits.add(
                    BattleUnit(
                        uid = "COOP_AI_${nextIdCounter++}",
                        template = tpl,
                        faction = tpl.faction,
                        x = -1,
                        y = -1,
                        facing = Facing.WEST,
                        totalWounds = tpl.maxTotalWounds,
                        maxTotalWounds = tpl.maxTotalWounds,
                        moveCmLeft = tpl.moveCm.toFloat(),
                        worldX = -1f,
                        worldY = -1f,
                        facingDeg = Facing.WEST.yawDegrees(),
                        ownerPlayerId = "AI",
                        ownerPlayerName = "AI Command"
                    )
                )
            }
        }

        if (reserveUnits.isEmpty()) return

        val firstPlayer = activePlayers.firstOrNull { p ->
            reserveUnits.any { it.ownerPlayerId == p.id }
        } ?: activePlayers.first()
        val firstPlayerIndex = activePlayers.indexOf(firstPlayer).coerceAtLeast(0)
        val firstReserve = reserveUnits.firstOrNull { it.ownerPlayerId == firstPlayer.id }
        val settings = _settingsState.value
        val localControlId = if (coop.networkRole == CoopNetworkRole.CLIENT) {
            coop.localPlayerId
        } else {
            "p1"
        }

        val firstTeamIndex = if (firstPlayer.id in listOf("p1", "p2")) 0 else 1
        lastDeployTeam0Idx = if (firstTeamIndex == 0) 0 else -1
        lastDeployTeam1Idx = if (firstTeamIndex == 1) 0 else -1

        val initialState = BattleUiState(
            active = true,
            round = 1,
            activeFaction = firstPlayer.faction,
            playerFaction = firstPlayer.faction,
            map = map,
            units = emptyList(),
            reserveUnits = reserveUnits,
            selectedReserveUnitUid = firstReserve?.uid,
            isDeploymentPhase = true,
            deploymentTurnPlayerId = firstPlayer.id,
            deploymentTurnPlayerName = firstPlayer.name,
            deploymentTeamIndex = firstTeamIndex,
            selectedUnitUid = null,
            autoFocusCamera = true,
            focusGridPos = GridPos(if (firstTeamIndex == 0) 1 else MapGenerator.GRID_W - 2, MapGenerator.GRID_H / 2),
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
                round = 0,
                activeFaction = firstPlayer.faction,
                isPlayerTurn = firstPlayer.id == localControlId && !firstPlayer.isAi,
                marineModelsAlive = reserveUnits.filter { it.faction == Faction.MARINES }.sumOf { it.aliveModels },
                xenosModelsAlive = reserveUnits.filter { it.faction == Faction.XENOS }.sumOf { it.aliveModels },
                playerKills = 0,
                playerLosses = 0,
                commanderLabel = "DEPLOYMENT: ${firstPlayer.name.uppercase()}"
            )
        )
        val afterLeadingAi = resolveAiDeploymentTurns(initialState)
        val readyState = if (afterLeadingAi.reserveUnits.isEmpty()) {
            transitionDeploymentToCombat(afterLeadingAi, afterLeadingAi.units)
        } else {
            recomputeSelectionOverlays(afterLeadingAi)
        }
        _battleState.value = readyState
        _currentScreen.value = AppScreen.COOP_BATTLE
        scheduleSplashAutoDismiss()
        syncCoopHostIfNeeded(readyState)
        if (!readyState.isDeploymentPhase && readyState.isAiThinking && readyState.activeCoopPlayerId != null) {
            runAiTurnLoop(readyState.activeFaction, aiOwnerId = readyState.activeCoopPlayerId)
        }
    }

    private fun buildSpawnCandidates(cols: List<Int>, map: Array<Array<TerrainType>>): List<GridPos> {
        val spots = mutableListOf<GridPos>()
        val rows = listOf(10, 11, 8, 13, 6, 15, 4, 17, 2, 19, 9, 12, 7, 14, 5, 16, 3, 18, 1)
        for (col in cols) {
            for (row in rows) {
                if (row in 0 until MapGenerator.GRID_H && col in 0 until MapGenerator.GRID_W && MapGenerator.isWalkable(map[row][col])) {
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
        val localId = _coopState.value.localPlayerId
        val adjustedSummary = syncedBattle.summary?.let { sum ->
            if (syncedBattle.isCoopMode && sum.winnerLabel.isNotBlank()) {
                val localTeam = if (localId in listOf("p3", "p4")) 1 else 0
                val winnerTeam = if (sum.winnerLabel.contains("TEAM 2")) 1 else 0
                sum.copy(playerWon = localTeam == winnerTeam)
            } else sum
        }
        val configured = syncedBattle.copy(
            localControlledPlayerId = localId,
            summary = adjustedSummary,
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
        if (!state.active || !state.isCoopMode) return
        val senderSlot = state.coopTurnOrder.firstOrNull { it.id == senderPlayerId } ?: return
        if (senderSlot.slotMode != CoopSlotMode.LAN_CLIENT) return
        val expectedPlayerId = if (state.isDeploymentPhase) state.deploymentTurnPlayerId else state.activeCoopPlayerId
        if (expectedPlayerId != senderPlayerId) return

        when (actionType) {
            "DEPLOY_UNIT" -> if (wx != null && wy != null) onWorldTapped(wx, wy, fromLanClient = true, deployUnitIndex = cm?.toInt())
            "TAP_WORLD" -> if (wx != null && wy != null) onWorldTapped(wx, wy, fromLanClient = true)
            "TOGGLE_MOVE" -> toggleMoveMode(fromLanClient = true, desiredActive = left)
            "ADVANCE" -> advanceMove(fromLanClient = true)
            "SET_CM" -> if (cm != null) setCmStep(cm, fromLanClient = true)
            "WALK_CM" -> walkForwardCm(fromLanClient = true)
            "CONFIRM_MOVE" -> confirmMovePreview(fromLanClient = true)
            "CANCEL_MOVE" -> cancelMovePreview(fromLanClient = true)
            "TOGGLE_CHARGE" -> toggleChargeMode(fromLanClient = true, desiredActive = left)
            "CONFIRM_CHARGE" -> confirmChargeRoll(fromLanClient = true)
            "CANCEL_CHARGE" -> cancelChargePreview(fromLanClient = true)
            "SELECT_DARK_PACT" -> cm?.toInt()?.let { DarkPactChoice.entries.getOrNull(it) }?.let { selectDarkPact(it, fromLanClient = true) }
            "STRIKE" -> confirmStrike(fromLanClient = true, darkPactOverride = cm?.toInt()?.let { DarkPactChoice.entries.getOrNull(it) })
            "WEAPON_MODE" -> if (mode != null) selectWeaponMode(mode, fromLanClient = true)
            "ROTATE" -> if (left != null) rotateSelectedUnit(left, fromLanClient = true)
            "FIGHT_FIRST" -> triggerFightFirst(fromLanClient = true)
            "END_TURN" -> endPlayerTurn(fromLanClient = true)
        }
    }

    private fun resolveCommandableUnit(state: BattleUiState, fromLanClient: Boolean): BattleUnit? {
        val currentSel = state.selectedUnit
        if (currentSel != null && state.canCommandUnit(currentSel, fromLanClient)) return currentSel
        val fallback = state.units.firstOrNull { it.isAlive && state.canCommandUnit(it, fromLanClient) }
        if (fallback != null) {
            _battleState.update { st ->
                if (st.selectedUnitUid == null || st.selectedUnit?.let { !st.canCommandUnit(it, fromLanClient) } == true) {
                    st.copy(selectedUnitUid = fallback.uid)
                } else st
            }
        }
        return fallback
    }

    fun updateSettings(newSettings: SettingsState) {
        val clamped = newSettings.copy(
            soundVolume = newSettings.soundVolume.coerceIn(0f, 1f),
            bgmVolume = newSettings.bgmVolume.coerceIn(0f, 1f),
            graphicsLevel = newSettings.graphicsLevel.coerceIn(1, 4),
            screenBrightness = newSettings.screenBrightness.coerceIn(0.5f, 1.5f)
        )
        sound.enabled = clamped.sfxEnabled
        sound.volume = clamped.soundVolume
        BackgroundMusicPlayer.setEnabled(clamped.bgmEnabled)
        BackgroundMusicPlayer.setVolume(clamped.bgmVolume)
        sound.play(SoundType.CLICK)
        _settingsState.value = clamped
        prefs.edit {
            putFloat("sound_volume", clamped.soundVolume)
            putBoolean("sfx_enabled", clamped.sfxEnabled)
            putFloat("bgm_volume", clamped.bgmVolume)
            putBoolean("bgm_enabled", clamped.bgmEnabled)
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
            runCatching {
                val saved = repository.getSavedConfigOnce() ?: return@launch
                val faction = runCatching { Faction.valueOf(saved.playerFaction) }.getOrDefault(Faction.MARINES)
                val parsedEnemy = saved.enemyFaction.takeIf { it.isNotBlank() }
                    ?.let { runCatching { Faction.valueOf(it) }.getOrNull() }
                    ?.takeIf { it != faction }
                    ?: if (faction == Faction.MARINES) Faction.BLACK_LEGION else Faction.MARINES
                val templates = DefaultCatalog.deserializeTemplates(saved.customTemplatesJson)
                val marines = DefaultCatalog.deserializeRoster(saved.marineRosterJson, DefaultCatalog.defaultMarineRoster())
                val blackLegion = DefaultCatalog.deserializeRoster(saved.blackLegionRosterJson, DefaultCatalog.defaultBlackLegionRoster())
                val xenos = DefaultCatalog.deserializeRoster(saved.xenosRosterJson, DefaultCatalog.defaultXenosRoster())
                _setupState.update {
                    it.copy(
                        playerFaction = faction,
                        enemyFaction = parsedEnemy,
                        pointLimit = saved.pointLimit.coerceIn(1000, 5000),
                        templates = templates,
                        marineRoster = marines,
                        blackLegionRoster = blackLegion,
                        xenosRoster = xenos
                    )
                }
            }
        }
    }

    // --- SETUP ACTIONS ---
    fun togglePlayerFaction() {
        sound.play(SoundType.CLICK)
        _setupState.update { state ->
            val all = Faction.entries
            val next = all[(all.indexOf(state.playerFaction) + 1) % all.size]
            val nextEnemy = if (state.enemyFaction == next) state.playerFaction else state.enemyFaction
            state.copy(playerFaction = next, enemyFaction = nextEnemy, statusMessage = null)
        }
    }

    fun selectPlayerFaction(faction: Faction) {
        sound.play(SoundType.CLICK)
        _setupState.update { state ->
            val nextEnemy = if (state.enemyFaction == faction) state.playerFaction else state.enemyFaction
            state.copy(playerFaction = faction, enemyFaction = nextEnemy, statusMessage = null)
        }
    }

    fun selectEnemyFaction(faction: Faction) {
        sound.play(SoundType.CLICK)
        _setupState.update { state ->
            val nextPlayer = if (state.playerFaction == faction) state.enemyFaction else state.playerFaction
            state.copy(playerFaction = nextPlayer, enemyFaction = faction, statusMessage = null)
        }
    }

    fun setPointLimit(points: Int) {
        _setupState.update {
            it.copy(pointLimit = points.coerceIn(1000, 5000), statusMessage = null)
        }
    }

    fun addRosterUnit(faction: Faction, templateId: String) {
        sound.play(SoundType.CLICK)
        _setupState.update { state ->
            val tpl = state.templates[templateId]
            val cost = tpl?.points ?: 0
            val used = state.pointsFor(faction)
            if (used + cost > state.pointLimit) {
                val left = (state.pointLimit - used).coerceAtLeast(0)
                return@update state.copy(
                    statusMessage = "Cannot add ${tpl?.name ?: templateId} ($cost PT): only $left / ${state.pointLimit} PT left!"
                )
            }
            val slot = RosterSlot("slot_${nextIdCounter++}", templateId)
            when (faction) {
                Faction.MARINES -> state.copy(marineRoster = state.marineRoster + slot, statusMessage = null)
                Faction.BLACK_LEGION -> state.copy(blackLegionRoster = state.blackLegionRoster + slot, statusMessage = null)
                Faction.XENOS -> state.copy(xenosRoster = state.xenosRoster + slot, statusMessage = null)
            }
        }
    }

    fun removeRosterSlot(faction: Faction, slotId: String) {
        sound.play(SoundType.CLICK)
        _setupState.update { state ->
            when (faction) {
                Faction.MARINES -> state.copy(marineRoster = state.marineRoster.filterNot { it.slotId == slotId }, statusMessage = null)
                Faction.BLACK_LEGION -> state.copy(blackLegionRoster = state.blackLegionRoster.filterNot { it.slotId == slotId }, statusMessage = null)
                Faction.XENOS -> state.copy(xenosRoster = state.xenosRoster.filterNot { it.slotId == slotId }, statusMessage = null)
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
            when (faction) {
                Faction.MARINES -> state.copy(marineRoster = chosen, statusMessage = "Randomized Ultramarines ($spent pts)")
                Faction.BLACK_LEGION -> state.copy(blackLegionRoster = chosen, statusMessage = "Randomized Black Legion ($spent pts)")
                Faction.XENOS -> state.copy(xenosRoster = chosen, statusMessage = "Randomized Tyranids ($spent pts)")
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
        _setupState.update { state ->
            state.copy(
                templates = state.templates + (updated.id to updated),
                editingTemplateId = null,
                statusMessage = "Saved ${updated.name} stats for room & next game!"
            )
        }
        val s = _setupState.value
        viewModelScope.launch {
            repository.saveConfig(
                SavedConfigEntity(
                    id = 1,
                    playerFaction = s.playerFaction.name,
                    enemyFaction = s.enemyFaction.name,
                    pointLimit = s.pointLimit,
                    marineRosterJson = DefaultCatalog.serializeRoster(s.marineRoster),
                    blackLegionRosterJson = DefaultCatalog.serializeRoster(s.blackLegionRoster),
                    xenosRosterJson = DefaultCatalog.serializeRoster(s.xenosRoster),
                    customTemplatesJson = DefaultCatalog.serializeTemplates(s.templates)
                )
            )
        }
        broadcastLobbyUpdate()
    }

    fun saveSetupToDatabase() {
        sound.play(SoundType.CLICK)
        val s = _setupState.value
        viewModelScope.launch {
            repository.saveConfig(
                SavedConfigEntity(
                    id = 1,
                    playerFaction = s.playerFaction.name,
                    enemyFaction = s.enemyFaction.name,
                    pointLimit = s.pointLimit,
                    marineRosterJson = DefaultCatalog.serializeRoster(s.marineRoster),
                    blackLegionRosterJson = DefaultCatalog.serializeRoster(s.blackLegionRoster),
                    xenosRosterJson = DefaultCatalog.serializeRoster(s.xenosRoster),
                    customTemplatesJson = DefaultCatalog.serializeTemplates(s.templates)
                )
            )
            _setupState.update { it.copy(statusMessage = "Saved all unit stats for next game & synced to room!") }
            broadcastLobbyUpdate()
        }
    }

    fun resetSetupToDefaults() {
        sound.play(SoundType.CLICK)
        viewModelScope.launch {
            repository.resetConfig()
            _setupState.value = SetupUiState(statusMessage = "Reset to Crimson Sector defaults")
            broadcastLobbyUpdate()
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

        // Spawn Player faction on West edge (x = 1..2), Enemy faction on East edge (x = W-2..W-3)
        spawnArmy(
            roster = setup.rosterFor(setup.playerFaction),
            templates = setup.templates,
            faction = setup.playerFaction,
            ownerPlayerId = "player",
            ownerPlayerName = "Commander",
            startCols = listOf(1, 2),
            facing = Facing.EAST,
            map = rawMap,
            out = spawned
        )
        spawnArmy(
            roster = setup.rosterFor(setup.enemyFaction),
            templates = setup.templates,
            faction = setup.enemyFaction,
            ownerPlayerId = "AI",
            ownerPlayerName = "AI Command",
            startCols = listOf(MapGenerator.GRID_W - 2, MapGenerator.GRID_W - 3),
            facing = Facing.WEST,
            map = rawMap,
            out = spawned
        )

        val unitsWithAuras = TacticalRules.applyAuras(spawned)
        val firstPlayerUnit = unitsWithAuras.firstOrNull { it.faction == setup.playerFaction }
        val settings = _settingsState.value
        val playerModels = unitsWithAuras.filter { it.faction == setup.playerFaction }.sumOf { it.aliveModels }
        val enemyModels = unitsWithAuras.filter { it.faction == setup.enemyFaction }.sumOf { it.aliveModels }

        val initialState = BattleUiState(
            active = true,
            round = 1,
            activeFaction = setup.playerFaction,
            playerFaction = setup.playerFaction,
            enemyFaction = setup.enemyFaction,
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
                playerLosses = 0,
                playerFaction = setup.playerFaction,
                enemyFaction = setup.enemyFaction,
                playerModelsAlive = playerModels,
                enemyModelsAlive = enemyModels
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
        ownerPlayerId: String,
        ownerPlayerName: String,
        startCols: List<Int>,
        facing: Facing,
        map: Array<Array<TerrainType>>,
        out: MutableList<BattleUnit>
    ) {
        val candidateSpots = buildSpawnCandidates(startCols, map)
        roster.forEachIndexed { index, slot ->
            val tpl = templates[slot.templateId] ?: return@forEachIndexed
            val pos = candidateSpots.getOrElse(index) { GridPos(startCols.first(), (index + 1).coerceIn(1, MapGenerator.GRID_H - 2)) }
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
                    facingDeg = facing.yawDegrees(),
                    ownerPlayerId = ownerPlayerId,
                    ownerPlayerName = ownerPlayerName
                )
            )
        }
    }

    fun returnToSetup() {
        val wasCoop = _battleState.value.isCoopMode
        aiJob?.cancel()
        actionJob?.cancel()
        sound.play(SoundType.CLICK)
        val inactive = _battleState.value.copy(
            active = false,
            summary = null,
            isAiThinking = false,
            isAnimating = false,
            combatLogs = emptyList()
        )
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

    fun selectReserveUnit(unitUid: String) {
        val state = _battleState.value
        if (!state.isDeploymentPhase || !state.canAcceptInput(false)) return
        val unit = state.reserveUnits.firstOrNull { it.uid == unitUid } ?: return
        if (state.isCoopMode && unit.ownerPlayerId != state.deploymentTurnPlayerId) return
        sound.play(SoundType.CLICK)
        _battleState.update { it.copy(selectedReserveUnitUid = unitUid) }
    }

    private fun pickNextDeployer(
        currentTeam: Int,
        reserves: List<BattleUnit>,
        turnOrder: List<CoopPlayer>
    ): Pair<String, Int>? {
        val team0Players = turnOrder.filter { it.isActive && it.id in listOf("p1", "p2") && reserves.any { u -> u.ownerPlayerId == it.id } }
        val team1Players = turnOrder.filter { it.isActive && it.id in listOf("p3", "p4") && reserves.any { u -> u.ownerPlayerId == it.id } }
        val aiHasReserves = reserves.any { it.ownerPlayerId == "AI" }

        fun nextTeam0(): Pair<String, Int>? {
            if (team0Players.isEmpty()) return null
            lastDeployTeam0Idx = (lastDeployTeam0Idx + 1) % team0Players.size
            return team0Players[lastDeployTeam0Idx].id to 0
        }

        fun nextTeam1(): Pair<String, Int>? {
            if (team1Players.isNotEmpty()) {
                lastDeployTeam1Idx = (lastDeployTeam1Idx + 1) % team1Players.size
                return team1Players[lastDeployTeam1Idx].id to 1
            }
            return if (aiHasReserves) "AI" to 1 else null
        }

        return if (currentTeam == 0) {
            nextTeam1() ?: nextTeam0()
        } else {
            nextTeam0() ?: nextTeam1()
        }
    }

    private fun isAiDeployOwner(ownerId: String?, turnOrder: List<CoopPlayer>): Boolean {
        if (ownerId == null) return false
        if (ownerId == "AI") return true
        return turnOrder.firstOrNull { it.id == ownerId }?.isAi == true
    }

    private fun resolveAiDeploymentTurns(initial: BattleUiState): BattleUiState {
        var currentUnits = initial.units
        var currentReserves = initial.reserveUnits
        var lastTeam = initial.deploymentTeamIndex
        var nextPid = initial.deploymentTurnPlayerId
        var nextTeam = initial.deploymentTeamIndex
        var safety = 0

        while (safety < 40 && nextPid != null && currentReserves.isNotEmpty() && isAiDeployOwner(nextPid, initial.coopTurnOrder)) {
            safety++
            val aiUnit = currentReserves.firstOrNull { it.ownerPlayerId == nextPid }
            if (aiUnit != null) {
                val aiTeam = if (nextPid in listOf("p1", "p2")) 0 else 1
                val cols = if (aiTeam == 0) {
                    listOf(1, 2, 0)
                } else {
                    listOf(MapGenerator.GRID_W - 2, MapGenerator.GRID_W - 3, MapGenerator.GRID_W - 1)
                }
                val candidates = buildSpawnCandidates(cols, initial.map.rows)
                val spot = candidates.firstOrNull { cand ->
                    currentUnits.none { it.isAlive && it.x == cand.x && it.y == cand.y }
                } ?: break
                val facing = if (aiTeam == 0) Facing.EAST else Facing.WEST
                val placedAi = aiUnit.copy(
                    x = spot.x,
                    y = spot.y,
                    worldX = spot.x.toFloat(),
                    worldY = spot.y.toFloat(),
                    facing = facing,
                    facingDeg = facing.yawDegrees()
                )
                currentUnits = TacticalRules.applyAuras(currentUnits + placedAi)
                currentReserves = currentReserves.filterNot { it.uid == aiUnit.uid }
                lastTeam = aiTeam
            }
            val nextPick = pickNextDeployer(lastTeam, currentReserves, initial.coopTurnOrder)
            nextPid = nextPick?.first
            nextTeam = nextPick?.second ?: lastTeam
        }

        if (nextPid == null || currentReserves.isEmpty()) {
            return initial.copy(
                units = currentUnits,
                reserveUnits = emptyList(),
                selectedReserveUnitUid = null,
                deploymentTurnPlayerId = null,
                deploymentTurnPlayerName = ""
            )
        }
        val nextPlayer = initial.coopTurnOrder.firstOrNull { it.id == nextPid }
        val nextReserve = currentReserves.firstOrNull { it.ownerPlayerId == nextPid }
        return initial.copy(
            units = currentUnits,
            reserveUnits = currentReserves,
            selectedReserveUnitUid = nextReserve?.uid,
            deploymentTurnPlayerId = nextPid,
            deploymentTurnPlayerName = nextPlayer?.name ?: nextPid,
            deploymentTeamIndex = nextTeam,
            activeFaction = nextPlayer?.faction ?: initial.activeFaction,
            playerFaction = nextPlayer?.faction ?: initial.playerFaction,
            activeCoopPlayerId = nextPid,
            activeCoopPlayerName = nextPlayer?.name ?: nextPid,
            focusGridPos = if (initial.autoFocusCamera && nextTeam != initial.deploymentTeamIndex) {
                GridPos(if (nextTeam == 0) 1 else MapGenerator.GRID_W - 2, MapGenerator.GRID_H / 2)
            } else initial.focusGridPos
        )
    }

    private fun transitionDeploymentToCombat(state: BattleUiState, deployedUnits: List<BattleUnit>): BattleUiState {
        val firstIdx = state.coopTurnOrder.indexOfFirst { p ->
            p.isActive && deployedUnits.any { it.ownerPlayerId == p.id && it.isAlive }
        }.takeIf { it >= 0 } ?: 0
        val firstPlayer = state.coopTurnOrder.getOrNull(firstIdx)
        val firstUnit = firstPlayer?.let { p -> deployedUnits.firstOrNull { it.ownerPlayerId == p.id && it.isAlive } }
        val isAiFirst = firstPlayer?.isAi == true
        return recomputeSelectionOverlays(
            state.copy(
                units = deployedUnits,
                reserveUnits = emptyList(),
                selectedReserveUnitUid = null,
                isDeploymentPhase = false,
                deploymentTurnPlayerId = null,
                deploymentTurnPlayerName = "",
                round = 1,
                activeFaction = firstPlayer?.faction ?: state.activeFaction,
                playerFaction = firstPlayer?.faction ?: state.playerFaction,
                selectedUnitUid = firstUnit?.uid,
                coopTurnIndex = firstIdx,
                activeCoopPlayerId = firstPlayer?.id,
                activeCoopPlayerName = firstPlayer?.name ?: "",
                isAiThinking = isAiFirst,
                focusGridPos = if (state.autoFocusCamera) firstUnit?.pos else state.focusGridPos,
                turnSplash = TurnSplashData(
                    round = 1,
                    activeFaction = firstPlayer?.faction ?: state.activeFaction,
                    isPlayerTurn = !isAiFirst && firstPlayer?.id == state.localControlledPlayerId,
                    marineModelsAlive = deployedUnits.filter { it.faction == Faction.MARINES && it.isAlive }.sumOf { it.aliveModels },
                    xenosModelsAlive = deployedUnits.filter { it.faction == Faction.XENOS && it.isAlive }.sumOf { it.aliveModels },
                    playerKills = 0,
                    playerLosses = 0,
                    commanderLabel = "BATTLE START • ${firstPlayer?.name?.uppercase() ?: "ROUND 1"}"
                )
            )
        )
    }

    private fun handleDeploymentTap(
        state: BattleUiState,
        wx: Float,
        wy: Float,
        fromLanClient: Boolean,
        deployUnitIndex: Int?
    ) {
        val pos = GridPos(
            wx.roundToInt().coerceIn(0, MapGenerator.GRID_W - 1),
            wy.roundToInt().coerceIn(0, MapGenerator.GRID_H - 1)
        )
        val expectedOwner = state.deploymentTurnPlayerId
        val unitToDeploy = if (deployUnitIndex != null) {
            state.reserveUnits.getOrNull(deployUnitIndex)?.takeIf { expectedOwner == null || it.ownerPlayerId == expectedOwner }
        } else {
            state.reserveUnits.firstOrNull { it.uid == state.selectedReserveUnitUid && (expectedOwner == null || it.ownerPlayerId == expectedOwner) }
                ?: state.reserveUnits.firstOrNull { expectedOwner == null || it.ownerPlayerId == expectedOwner }
        } ?: return

        val teamIndex = state.deploymentTeamIndex
        if (!TacticalRules.isWithinDeploymentZone(pos.x, pos.y, teamIndex)) {
            val sideName = if (teamIndex == 0) "WEST" else "EAST"
            spawnFloatingText(pos, "DEPLOY IN $sideName 3-TILE ZONE", 0xFFFF4D58L)
            return
        }
        if (!MapGenerator.isWalkable(state.map.rows[pos.y][pos.x])) {
            spawnFloatingText(pos, "BLOCKED TILE", 0xFFFF4D58L)
            return
        }
        if (state.units.any { it.isAlive && it.x == pos.x && it.y == pos.y }) {
            spawnFloatingText(pos, "TILE OCCUPIED", 0xFFFF4D58L)
            return
        }

        if (shouldForwardToLanHost(fromLanClient)) {
            val idx = state.reserveUnits.indexOfFirst { it.uid == unitToDeploy.uid }.coerceAtLeast(0)
            forwardClientAction("DEPLOY_UNIT") {
                it.put("wx", wx.toDouble())
                it.put("wy", wy.toDouble())
                it.put("cm", idx.toDouble())
            }
            return
        }

        sound.play(SoundType.CLICK)
        val facing = if (teamIndex == 0) Facing.EAST else Facing.WEST
        val placed = unitToDeploy.copy(
            x = pos.x,
            y = pos.y,
            worldX = pos.x.toFloat(),
            worldY = pos.y.toFloat(),
            facing = facing,
            facingDeg = facing.yawDegrees()
        )
        val currentUnits = TacticalRules.applyAuras(state.units + placed)
        val currentReserves = state.reserveUnits.filterNot { it.uid == unitToDeploy.uid }
        spawnFloatingText(pos, "DEPLOYED!", 0xFF38D96CL)

        val nextPick = pickNextDeployer(teamIndex, currentReserves, state.coopTurnOrder)
        val afterPlayerDeploy = state.copy(
            units = currentUnits,
            reserveUnits = currentReserves,
            deploymentTurnPlayerId = nextPick?.first,
            deploymentTeamIndex = nextPick?.second ?: teamIndex
        )
        val afterAiDeploy = resolveAiDeploymentTurns(afterPlayerDeploy)

        if (afterAiDeploy.reserveUnits.isEmpty()) {
            sound.play(SoundType.TURN_HORN)
            val readyBattle = transitionDeploymentToCombat(afterAiDeploy, afterAiDeploy.units)
            _battleState.value = readyBattle
            syncCoopHostIfNeeded(readyBattle)
            scheduleSplashAutoDismiss()
            if (readyBattle.isAiThinking && readyBattle.activeCoopPlayerId != null) {
                runAiTurnLoop(readyBattle.activeFaction, aiOwnerId = readyBattle.activeCoopPlayerId)
            }
        } else {
            _battleState.value = afterAiDeploy
            syncCoopHostIfNeeded(afterAiDeploy)
        }
    }

    // --- BATTLE INTERACTION ---
    fun onTileTapped(pos: GridPos) {
        onWorldTapped(pos.x.toFloat(), pos.y.toFloat())
    }

    fun onWorldTapped(wx: Float, wy: Float, fromLanClient: Boolean = false, deployUnitIndex: Int? = null) {
        val state = _battleState.value
        if (!state.canAcceptInput(fromLanClient)) return

        if (state.isDeploymentPhase) {
            handleDeploymentTap(state, wx, wy, fromLanClient, deployUnitIndex)
            return
        }

        if (shouldForwardToLanHost(fromLanClient)) {
            forwardClientAction("TAP_WORLD") {
                it.put("wx", wx.toDouble())
                it.put("wy", wy.toDouble())
            }
            return
        }

        val pos = GridPos(
            wx.roundToInt().coerceIn(0, MapGenerator.GRID_W - 1),
            wy.roundToInt().coerceIn(0, MapGenerator.GRID_H - 1)
        )

        // 0. Check if Assault Terminator is in Teleport Strike mode
        val selected = state.selectedUnit
        if (selected != null && state.canCommandUnit(selected, fromLanClient) &&
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
                val enemies = state.units.filter { it.isAlive && selected.isEnemyOf(it) }
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

        // 0b. Check if Biovore is in Seed Spore Mine mode
        if (selected != null && state.canCommandUnit(selected, fromLanClient) &&
            selected.template.archetype == UnitArchetype.BIOVORE &&
            state.weaponMode == WeaponMode.SKILL
        ) {
            if (pos in state.reachableTiles.keys) {
                actionJob?.cancel()
                actionJob = viewModelScope.launch {
                    performSeedSporeMine(selected.uid, pos)
                }
            } else {
                val dist = maxOf(abs(pos.x - selected.x), abs(pos.y - selected.y))
                val enemies = state.units.filter { it.isAlive && selected.isEnemyOf(it) }
                val minEnemyCm = enemies.minOfOrNull {
                    hypot(pos.x - it.worldX, pos.y - it.worldY) * TacticalRules.CM_PER_TILE
                } ?: 999f
                when {
                    dist > 8 -> spawnFloatingText(pos, "MAX RANGE 8", 0xFFFF4D58L)
                    minEnemyCm < 7.95f -> spawnFloatingText(pos, "TOO CLOSE TO ENEMY (MIN 8CM)", 0xFFFF4D58L)
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
            val activeTeam = if (state.isCoopMode) {
                if (state.activeCoopPlayerId in listOf("p1", "p2", null)) 0 else 1
            } else null
            val isFriendlyUnit = if (state.isCoopMode) {
                unitHit.effectiveTeam == activeTeam
            } else {
                unitHit.faction == state.activeFaction
            }
            if (state.canCommandUnit(unitHit, fromLanClient)) {
                sound.play(SoundType.CLICK)
                val updated = state.copy(
                    selectedUnitUid = unitHit.uid,
                    weaponMode = WeaponMode.MAIN,
                    selectedDarkPact = DarkPactChoice.NONE,
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
            } else if (isFriendlyUnit) {
                spawnFloatingText(pos, "ALLY: ${unitHit.ownerPlayerName.uppercase()}", 0xFF64DFDFL)
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
                        unitHit.isCloaked -> "CHAMELEONIC CLOAK: UNTARGETABLE"
                        !sel.canCharge -> if (sel.hasAdvanced) "NO CHARGE AFTER ADVANCE" else "ALREADY CHARGED"
                        curDist <= TacticalRules.MELEE_MAX_CENTER_TILES -> "ALREADY IN MELEE"
                        curDist > 12.45f -> "OUT OF 12 CM CHARGE"
                        else -> "NO CHARGE PATH"
                    }
                    spawnFloatingText(pos, msg, 0xFFFF4D58L)
                }
                return
            } else if (!state.moveModeActive) {
                val sel = state.selectedUnit ?: return
                if (!state.canCommandUnit(sel, fromLanClient)) return
                if (unitHit.isCloaked) {
                    spawnFloatingText(pos, "CHAMELEONIC CLOAK: UNTARGETABLE", 0xFF4CC9F0L)
                    return
                }
                val pactChoice = if (sel.template.archetype == UnitArchetype.LEGIONARY) state.selectedDarkPact else DarkPactChoice.NONE
                val preview = TacticalRules.canTarget(sel, unitHit, state.weaponMode, state.map.rows, state.units, pactChoice)
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
        if (!state.canCommandUnit(sel, fromLanClient)) return

        if (state.chargeModeActive) {
            spawnFloatingText(pos, "TAP ENEMY WITHIN 12 CM", 0xFFFFB703L)
            return
        }

        if (TacticalRules.isEngagedInMelee(sel, state.units)) {
            spawnFloatingText(pos, "MELEE DUEL LOCK: KILL FOE FIRST!", 0xFFFF4D58L)
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

    fun toggleMoveMode(fromLanClient: Boolean = false, desiredActive: Boolean? = null) {
        val state = _battleState.value
        if (!state.canAcceptInput(fromLanClient) || state.isDeploymentPhase) return
        if (shouldForwardToLanHost(fromLanClient)) {
            forwardClientAction("TOGGLE_MOVE") { it.put("left", !state.moveModeActive) }
            return
        }
        val selected = resolveCommandableUnit(state, fromLanClient) ?: return
        if (TacticalRules.isEngagedInMelee(selected, state.units)) {
            spawnFloatingText(selected.pos, "LOCKED IN MELEE: KILL ENEMY FIRST!", 0xFFFF4D58L)
            return
        }
        sound.play(SoundType.CLICK)
        _battleState.update { st ->
            val next = desiredActive ?: !st.moveModeActive
            recomputeSelectionOverlays(
                st.copy(
                    selectedUnitUid = selected.uid,
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
        val state = _battleState.value
        if (!state.canAcceptInput(fromLanClient) || state.isDeploymentPhase) return
        if (shouldForwardToLanHost(fromLanClient)) {
            forwardClientAction("ADVANCE")
            return
        }
        val selected = resolveCommandableUnit(state, fromLanClient) ?: return
        if (!TacticalRules.canAdvance(selected, state.units)) {
            val msg = if (TacticalRules.isEngagedInMelee(selected, state.units)) {
                "LOCKED IN MELEE: CANNOT ADVANCE!"
            } else {
                "CANNOT ADVANCE"
            }
            spawnFloatingText(selected.pos, msg, 0xFFFF4D58L)
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
                    "ADVANCE +$roll cm (ASSAULT)"
                } else {
                    "ADVANCE +$roll cm (NO SHOOT/CHRG)"
                }
            )
        }
        spawnFloatingText(selected.pos, "ADVANCE +$roll cm (+${bonusCm.toInt()} cm)", 0xFF38D96CL)
        val advLog = CombatLogEntry(
            id = nextIdCounter++,
            round = state.round,
            faction = selected.faction,
            title = "${selected.template.name.uppercase()} • ADVANCE",
            subtitle = "MOVEMENT PHASE • 1D6",
            steps = listOf(
                DiceStepDetail(
                    phaseName = "ADVANCE",
                    stepLabel = "1D6 ADVANCE",
                    targetNeeded = 1,
                    rolled = 1,
                    passed = 1,
                    failed = 0,
                    diceValues = listOf(roll),
                    note = if (selected.template.mainWeaponAssault) "ASSAULT OK" else "NO SHOOT/CHRG"
                )
            ),
            resultSummary = "+$roll cm Move",
            isSuccess = true
        )
        val nextState = state.copy(
            units = updatedUnits,
            weaponMode = WeaponMode.MAIN,
            moveModeActive = true,
            chargeModeActive = false,
            chargePreview = null,
            movePreview = null,
            targetPreview = null,
            combatLogs = appendCombatLogs(state.combatLogs, listOf(advLog))
        )
        val ready = recomputeSelectionOverlays(nextState)
        _battleState.value = ready
        syncCoopHostIfNeeded(ready)
    }

    fun toggleChargeMode(fromLanClient: Boolean = false, desiredActive: Boolean? = null) {
        val state = _battleState.value
        if (!state.canAcceptInput(fromLanClient) || state.isDeploymentPhase) return
        if (shouldForwardToLanHost(fromLanClient)) {
            forwardClientAction("TOGGLE_CHARGE") { it.put("left", !state.chargeModeActive) }
            return
        }
        val selected = resolveCommandableUnit(state, fromLanClient) ?: return
        val inMelee = TacticalRules.isEngagedInMelee(selected, state.units)
        if (inMelee || !selected.canCharge) {
            val msg = when {
                inMelee -> "LOCKED IN MELEE: DUEL ACTIVE!"
                selected.hasAdvanced -> "NO CHARGE AFTER ADVANCE"
                selected.hasCharged -> "ALREADY CHARGED"
                else -> "ALREADY FOUGHT"
            }
            spawnFloatingText(selected.pos, msg, 0xFFFF4D58L)
            return
        }
        sound.play(SoundType.CLICK)
        val next = desiredActive ?: !state.chargeModeActive
        val nextState = state.copy(
            selectedUnitUid = selected.uid,
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
        val state = _battleState.value
        if (!state.canAcceptInput(fromLanClient)) return
        if (shouldForwardToLanHost(fromLanClient)) {
            forwardClientAction("CANCEL_CHARGE")
            return
        }
        sound.play(SoundType.CLICK)
        _battleState.update { it.copy(chargePreview = null) }
        syncCoopHostIfNeeded(_battleState.value)
    }

    fun confirmChargeRoll(fromLanClient: Boolean = false) {
        val state = _battleState.value
        if (!state.canAcceptInput(fromLanClient) || state.isDeploymentPhase) return
        if (shouldForwardToLanHost(fromLanClient)) {
            forwardClientAction("CONFIRM_CHARGE")
            return
        }
        val selected = state.selectedUnit ?: return
        if (!state.canCommandUnit(selected, fromLanClient)) return
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
        val chargeSuccess = total >= preview.requiredRoll
        val chargeLog = CombatLogEntry(
            id = nextIdCounter++,
            round = state.round,
            faction = charger.faction,
            title = "${charger.template.name.uppercase()} ▸ ${preview.targetName.uppercase()}",
            subtitle = "CHARGE • ${"%.1f".format(preview.distanceCm)} cm (NEED ${preview.requiredRoll}+)",
            steps = listOf(
                DiceStepDetail(
                    phaseName = "CHARGE",
                    stepLabel = "2D6 (${preview.requiredRoll}+)",
                    targetNeeded = preview.requiredRoll,
                    rolled = 2,
                    passed = if (chargeSuccess) 1 else 0,
                    failed = if (chargeSuccess) 0 else 1,
                    diceValues = listOf(d1, d2),
                    note = "TOTAL $total"
                )
            ),
            resultSummary = if (chargeSuccess) "CHARGE SUCCESS ($d1+$d2=$total)" else "CHARGE FAILED ($d1+$d2=$total)",
            isSuccess = chargeSuccess
        )

        if (chargeSuccess) {
            sound.play(SoundType.MELEE)
            spawnFloatingText(charger.pos, "CHARGE $d1+$d2=$total! (${preview.requiredRoll}+)", 0xFFFFD166L)
            _battleState.update { st -> st.copy(combatLogs = appendCombatLogs(st.combatLogs, listOf(chargeLog))) }
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
                    chargeableUids = emptySet(),
                    combatLogs = appendCombatLogs(state.combatLogs, listOf(chargeLog))
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
        val state = _battleState.value
        if (!state.canAcceptInput(fromLanClient)) return
        if (shouldForwardToLanHost(fromLanClient)) {
            forwardClientAction("SET_CM") { it.put("cm", stepCm.toDouble()) }
            return
        }
        sound.play(SoundType.CLICK)
        _battleState.update { it.copy(cmStepChoice = stepCm) }
        syncCoopHostIfNeeded(_battleState.value)
    }

    fun walkForwardCm(fromLanClient: Boolean = false) {
        val state = _battleState.value
        if (!state.canAcceptInput(fromLanClient) || state.isDeploymentPhase) return
        if (shouldForwardToLanHost(fromLanClient)) {
            forwardClientAction("WALK_CM")
            return
        }
        val selected = resolveCommandableUnit(state, fromLanClient) ?: return
        if (TacticalRules.isEngagedInMelee(selected, state.units)) {
            spawnFloatingText(selected.pos, "LOCKED IN MELEE: CANNOT MOVE!", 0xFFFF4D58L)
            return
        }
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
        val state = _battleState.value
        if (!state.canAcceptInput(fromLanClient) || state.isDeploymentPhase) return
        if (shouldForwardToLanHost(fromLanClient)) {
            forwardClientAction("CONFIRM_MOVE")
            return
        }
        val selected = state.selectedUnit ?: return
        if (!state.canCommandUnit(selected, fromLanClient)) return
        if (TacticalRules.isEngagedInMelee(selected, state.units)) {
            _battleState.update { it.copy(movePreview = null, moveModeActive = false) }
            spawnFloatingText(selected.pos, "LOCKED IN MELEE!", 0xFFFF4D58L)
            return
        }
        val preview = state.movePreview ?: return
        actionJob?.cancel()
        actionJob = viewModelScope.launch {
            performContinuousMove(selected.uid, preview)
        }
    }

    fun cancelMovePreview(fromLanClient: Boolean = false) {
        val state = _battleState.value
        if (!state.canAcceptInput(fromLanClient)) return
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

    private suspend fun performSeedSporeMine(unitUid: String, dest: GridPos) {
        val state = _battleState.value
        val biovore = state.units.firstOrNull { it.uid == unitUid && it.isAlive } ?: return
        sound.play(SoundType.PLASMA)

        val startPos = biovore.pos
        val vfx = ProjectileVfx(
            id = nextIdCounter++,
            fromX = biovore.worldX,
            fromY = biovore.worldY,
            toX = dest.x.toFloat(),
            toY = dest.y.toFloat(),
            colorArgb = 0xFF9D4EDDL,
            isBeam = false,
            isMelee = false,
            startTimeMs = System.currentTimeMillis()
        )

        val updatedUnits = state.units.map { u ->
            if (u.uid != unitUid) u
            else u.copy(
                skillCooldownLeft = u.template.skillCooldown,
                hasShot = true
            )
        }.toMutableList()

        _battleState.value = state.copy(
            units = updatedUnits,
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
        spawnFloatingText(startPos, "SEED SPORE", 0xFFE0AAFFL)

        try {
            delay(350L)
            sound.play(SoundType.SURGE)

            val sporeTemplate = DefaultCatalog.defaultTemplates.firstOrNull { it.id == "spore_mine" }
                ?: DefaultCatalog.defaultTemplates.first { it.archetype == UnitArchetype.SPORE_MINE }
            val sporeWounds = sporeTemplate.woundsPerModel * sporeTemplate.squadSize
            val newSporeMine = BattleUnit(
                uid = "spore_mine_${nextIdCounter++}",
                template = sporeTemplate,
                faction = biovore.faction,
                x = dest.x,
                y = dest.y,
                facing = biovore.facing,
                totalWounds = sporeWounds,
                maxTotalWounds = sporeWounds,
                moveCmLeft = sporeTemplate.moveCm.toFloat(),
                worldX = dest.x.toFloat(),
                worldY = dest.y.toFloat(),
                facingDeg = biovore.facingDeg,
                ownerPlayerId = biovore.ownerPlayerId,
                ownerPlayerName = biovore.ownerPlayerName
            )
            updatedUnits.add(newSporeMine)

            spawnFloatingText(dest, "SPORE MINE SEEDED!", 0xFFC77DFFL)

            val log = CombatLogEntry(
                id = nextIdCounter++,
                round = state.round,
                faction = biovore.faction,
                title = "${biovore.template.name.uppercase()} • SEED SPORE MINE",
                subtitle = "SPAWN POS (${dest.x}, ${dest.y}) [≥8cm FROM FOES]",
                steps = listOf(
                    DiceStepDetail(
                        phaseName = "SKILL",
                        stepLabel = "Seed Spore Mine",
                        targetNeeded = 1,
                        rolled = 1,
                        passed = 1,
                        failed = 0,
                        diceValues = listOf(6),
                        note = "M 4 cm, Detonate: 1D6/enemy model, <3 = 1W"
                    )
                ),
                resultSummary = "Living Spore Mine deployed and controllable",
                isSuccess = true
            )

            val finalUnits = TacticalRules.applyAuras(updatedUnits)
            _battleState.value = recomputeSelectionOverlays(
                _battleState.value.copy(
                    units = finalUnits,
                    isAnimating = false,
                    selectedUnitUid = newSporeMine.uid,
                    focusGridPos = if (_battleState.value.autoFocusCamera) dest else _battleState.value.focusGridPos,
                    combatLogs = appendCombatLogs(_battleState.value.combatLogs, listOf(log))
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

    fun selectDarkPact(choice: DarkPactChoice, fromLanClient: Boolean = false) {
        val state = _battleState.value
        if (!state.canAcceptInput(fromLanClient) || state.isDeploymentPhase) return
        if (shouldForwardToLanHost(fromLanClient)) {
            forwardClientAction("SELECT_DARK_PACT") { it.put("cm", choice.ordinal.toDouble()) }
            return
        }
        val attacker = state.selectedUnit ?: return
        if (attacker.template.archetype != UnitArchetype.LEGIONARY) return
        sound.play(SoundType.CLICK)
        val curPreview = state.targetPreview
        val updatedPreview = if (curPreview != null) {
            val target = state.units.firstOrNull { it.uid == curPreview.targetUid && it.isAlive }
            if (target != null) {
                TacticalRules.canTarget(attacker, target, curPreview.weaponMode, state.map.rows, state.units, choice)
            } else curPreview
        } else null
        val next = state.copy(selectedDarkPact = choice, targetPreview = updatedPreview)
        _battleState.value = next
        syncCoopHostIfNeeded(next)
    }

    fun confirmStrike(fromLanClient: Boolean = false, darkPactOverride: DarkPactChoice? = null) {
        val state = _battleState.value
        if (!state.canAcceptInput(fromLanClient) || state.isDeploymentPhase) return
        if (shouldForwardToLanHost(fromLanClient)) {
            forwardClientAction("STRIKE") { it.put("cm", state.selectedDarkPact.ordinal.toDouble()) }
            return
        }
        val attacker = state.selectedUnit ?: return
        if (!state.canCommandUnit(attacker, fromLanClient)) return
        val preview = state.targetPreview ?: return
        val target = state.units.firstOrNull { it.uid == preview.targetUid && it.isAlive } ?: return
        val pactChoice = if (attacker.template.archetype == UnitArchetype.LEGIONARY) {
            darkPactOverride ?: preview.darkPactChoice
        } else DarkPactChoice.NONE
        actionJob?.cancel()
        actionJob = viewModelScope.launch {
            executeAttack(attacker.uid, target.uid, preview.weaponMode, darkPactChoice = pactChoice)
        }
    }

    fun triggerFightFirst(fromLanClient: Boolean = false) {
        val state = _battleState.value
        if (!state.canAcceptInput(fromLanClient) || state.isDeploymentPhase) return
        if (shouldForwardToLanHost(fromLanClient)) {
            forwardClientAction("FIGHT_FIRST")
            return
        }
        val selected = state.selectedUnit ?: return
        if (!state.canCommandUnit(selected, fromLanClient)) return
        if (selected.template.archetype != UnitArchetype.VON_RYANS_LEAPER) return
        if (selected.hasFought) {
            spawnFloatingText(selected.pos, "ALREADY FOUGHT", 0xFF9AA3B2L)
            return
        }
        val adjEnemy = state.units
            .filter { it.isAlive && !it.isCloaked && selected.isEnemyOf(it) }
            .minByOrNull { hypot(it.worldX - selected.worldX, it.worldY - selected.worldY) }
            ?.takeIf { TacticalRules.isWithinMeleeReach(selected, it) }
        if (adjEnemy == null) {
            spawnFloatingText(selected.pos, "NO ADJACENT ENEMY", 0xFFFF4D58L)
            return
        }
        actionJob?.cancel()
        actionJob = viewModelScope.launch {
            spawnFloatingText(selected.pos, "⚡ FIGHT FIRST!", 0xFFFFD166L)
            executeAttack(selected.uid, adjEnemy.uid, WeaponMode.MAIN, skipCounterAttack = true)
        }
    }

    private suspend fun executeAttack(
        attackerUid: String,
        targetUid: String,
        mode: WeaponMode,
        skipCounterAttack: Boolean = false,
        darkPactChoice: DarkPactChoice = DarkPactChoice.NONE
    ) {
        var state = _battleState.value
        var attacker = state.units.firstOrNull { it.uid == attackerUid && it.isAlive } ?: return
        var target = state.units.firstOrNull { it.uid == targetUid && it.isAlive } ?: return

        val distTiles = hypot(attacker.worldX - target.worldX, attacker.worldY - target.worldY)
        val isMelee = TacticalRules.effectiveIsMelee(attacker, mode, distTiles)

        // Von Ryan's Leaper Tabletop Passive: Fight First pre-strike when engaged in melee
        var leaperPreStruck = false
        if (isMelee && !skipCounterAttack &&
            target.template.archetype == UnitArchetype.VON_RYANS_LEAPER &&
            target.isAlive && target.aliveModels > 0 && !target.hasFought && !target.isCloaked
        ) {
            leaperPreStruck = true
            _battleState.update { it.copy(isAnimating = true) }
            spawnFloatingText(target.pos, "⚡ FIGHT FIRST!", 0xFFFFD166L)
            sound.play(SoundType.MELEE)
            val preOutcome = TacticalRules.resolveMeleeCounterAttack(target, attacker, state.map.rows)
            if (preOutcome != null) {
                val preNow = System.currentTimeMillis()
                val preProj = ProjectileVfx(
                    id = nextIdCounter++,
                    fromX = target.worldX,
                    fromY = target.worldY,
                    toX = attacker.worldX,
                    toY = attacker.worldY,
                    colorArgb = 0xFFC77DFFL,
                    isBeam = false,
                    isMelee = true,
                    startTimeMs = preNow
                )
                val preFloats = preOutcome.floatingTexts.mapIndexed { idx, pair ->
                    FloatingTextVfx(
                        id = nextIdCounter++,
                        gridX = pair.first.x.toFloat(),
                        gridY = pair.first.y.toFloat() - idx * 0.35f,
                        text = pair.second.first,
                        colorArgb = pair.second.second,
                        startTimeMs = preNow + 200L + idx * 180L
                    )
                }
                val leaperWithFought = preOutcome.updatedDefender.copy(hasFought = true)
                val preUnits = TacticalRules.applyAuras(
                    state.units.map { u ->
                        when (u.uid) {
                            leaperWithFought.uid -> leaperWithFought
                            preOutcome.updatedAttacker.uid -> preOutcome.updatedAttacker
                            else -> u
                        }
                    }
                )
                val targetIsSide0 = if (state.isCoopMode) target.effectiveTeam == 0 else target.faction == state.playerFaction
                val preKillsPlayer = if (targetIsSide0 && preOutcome.updatedAttacker.totalWounds == 0) 1 else 0
                val preKillsEnemy = if (!targetIsSide0 && preOutcome.updatedAttacker.totalWounds == 0) 1 else 0
                val preLogs = preOutcome.logEntry?.let {
                    listOf(
                        it.copy(
                            id = nextIdCounter++,
                            round = state.round,
                            subtitle = "⚡ FIGHT FIRST • ${TacticalRules.weaponDisplayName(target, WeaponMode.MAIN, true).uppercase()}"
                        )
                    )
                } ?: emptyList()
                val preUpdated = state.copy(
                    units = preUnits,
                    projectiles = (state.projectiles + preProj).filter { preNow - it.startTimeMs < 1800 },
                    floatingTexts = (state.floatingTexts + preFloats).filter { preNow - it.startTimeMs < 3200 },
                    playerKills = state.playerKills + preKillsPlayer,
                    enemyKills = state.enemyKills + preKillsEnemy,
                    focusGridPos = if (state.autoFocusCamera) attacker.pos else state.focusGridPos,
                    combatLogs = appendCombatLogs(state.combatLogs, preLogs)
                )
                val afterPre = checkVictoryOrUpdate(recomputeSelectionOverlays(preUpdated))
                _battleState.value = afterPre
                syncCoopHostIfNeeded(afterPre)
                scheduleVfxCleanup()
                delay(900L)
            }
            state = _battleState.value
            val nextAttacker = state.units.firstOrNull { it.uid == attackerUid && it.isAlive }
            val nextTarget = state.units.firstOrNull { it.uid == targetUid && it.isAlive }
            if (!state.active || state.summary != null || nextAttacker == null || nextTarget == null) {
                _battleState.update { st ->
                    if (!st.active) st else recomputeSelectionOverlays(st.copy(isAnimating = false))
                }
                syncCoopHostIfNeeded(_battleState.value)
                return
            }
            attacker = nextAttacker
            target = nextTarget
        }

        if (attacker.template.archetype == UnitArchetype.LEGIONARY &&
            !attacker.legionaryWoundRerollBuffActive &&
            attacker.skillCooldownLeft == 0
        ) {
            spawnFloatingText(attacker.pos, "⛧ VETERANS: REROLL WOUND 1s (1T)", 0xFFFFB703L)
        }

        sound.play(
            when {
                isMelee -> SoundType.MELEE
                mode == WeaponMode.SKILL || attacker.template.archetype == UnitArchetype.HELLBLASTER -> SoundType.PLASMA
                else -> SoundType.BOLTER
            }
        )

        spawnFloatingText(target.pos, "🎲 TABLETOP ROLL (2s)...", 0xFFFFD166L)
        _battleState.update { it.copy(isAnimating = true) }
        delay(2000L)

        val outcome = TacticalRules.resolveAttack(
            attacker = attacker,
            primaryTarget = target,
            allUnits = state.units,
            mode = mode,
            map = state.map.rows,
            darkPactChoice = darkPactChoice
        )
        val now = System.currentTimeMillis()

        val projColor = when {
            attacker.faction == Faction.MARINES && (mode == WeaponMode.SKILL || attacker.template.archetype == UnitArchetype.HELLBLASTER) -> 0xFF32C5FFL
            attacker.faction == Faction.MARINES -> 0xFFFFD166L
            attacker.faction == Faction.BLACK_LEGION && mode == WeaponMode.SKILL -> 0xFFFF9E2CL
            attacker.faction == Faction.BLACK_LEGION -> 0xFFE6A832L
            else -> 0xFFC77DFFL
        }
        val proj = if (outcome.updatedTargets.isNotEmpty()) {
            ProjectileVfx(
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
        } else null
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

        val attackerIsSide0 = if (state.isCoopMode) attacker.effectiveTeam == 0 else attacker.faction == state.playerFaction
        val selfDied = attacker.isAlive && !outcome.updatedAttacker.isAlive
        val addedPlayerKills = (if (attackerIsSide0) outcome.killsCount else 0) + (if (!attackerIsSide0 && selfDied) 1 else 0)
        val addedEnemyKills = (if (!attackerIsSide0) outcome.killsCount else 0) + (if (attackerIsSide0 && selfDied) 1 else 0)

        val survivingAttacker = newUnits.firstOrNull { it.uid == attacker.uid }
        val counterDefenders = if (isMelee && !skipCounterAttack && !leaperPreStruck && survivingAttacker != null && survivingAttacker.isAlive) {
            outcome.updatedTargets.mapNotNull { t ->
                newUnits.firstOrNull { it.uid == t.uid && it.isAlive && it.aliveModels > 0 && !it.isCloaked }?.takeIf { def ->
                    TacticalRules.isWithinMeleeReach(def, survivingAttacker)
                }
            }
        } else emptyList()
        val willCounterAttack = counterDefenders.isNotEmpty()

        val attackLogs = outcome.logEntries.map { it.copy(id = nextIdCounter++, round = state.round) }
        val updated = state.copy(
            units = newUnits,
            weaponMode = WeaponMode.MAIN,
            selectedDarkPact = DarkPactChoice.NONE,
            moveModeActive = false,
            movePreview = null,
            chargeModeActive = false,
            chargePreview = null,
            chargeableUids = emptySet(),
            targetPreview = null,
            isAnimating = willCounterAttack,
            projectiles = (if (proj != null) state.projectiles + proj else state.projectiles).filter { now - it.startTimeMs < 1800 },
            floatingTexts = (state.floatingTexts + floats).filter { now - it.startTimeMs < 3200 },
            focusGridPos = if (state.autoFocusCamera) target.pos else state.focusGridPos,
            playerKills = state.playerKills + addedPlayerKills,
            enemyKills = state.enemyKills + addedEnemyKills,
            combatLogs = appendCombatLogs(state.combatLogs, attackLogs)
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
                    val facingDeg = TacticalRules.angleDegToward(liveDefender.worldX, liveDefender.worldY, liveAttacker.worldX, liveAttacker.worldY)
                    val facing = TacticalRules.nearestFacingFromDeg(facingDeg)
                    val rotatedDefender = liveDefender.copy(facing = facing, facingDeg = facingDeg)

                    sound.play(SoundType.MELEE)
                    val counterOutcome = TacticalRules.resolveMeleeCounterAttack(
                        rotatedDefender,
                        liveAttacker,
                        currentState.map.rows
                    )
                    if (counterOutcome != null) {
                        val counterNow = System.currentTimeMillis()
                        val counterProjColor = when (liveDefender.faction) {
                            Faction.MARINES -> 0xFFFFD166L
                            Faction.BLACK_LEGION -> 0xFFE6A832L
                            Faction.XENOS -> 0xFFC77DFFL
                        }
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

                        val defIsSide0 = if (currentState.isCoopMode) liveDefender.effectiveTeam == 0 else liveDefender.faction == currentState.playerFaction
                        val counterKillsPlayer = if (defIsSide0 && counterOutcome.updatedAttacker.totalWounds == 0) 1 else 0
                        val counterKillsEnemy = if (!defIsSide0 && counterOutcome.updatedAttacker.totalWounds == 0) 1 else 0

                        val counterLogs = counterOutcome.logEntry?.let {
                            listOf(it.copy(id = nextIdCounter++, round = currentState.round))
                        } ?: emptyList()
                        val stateAfterCounter = currentState.copy(
                            units = afterCounterUnits,
                            projectiles = (currentState.projectiles + counterProj).filter { counterNow - it.startTimeMs < 1800 },
                            floatingTexts = (currentState.floatingTexts + counterFloats).filter { counterNow - it.startTimeMs < 3200 },
                            playerKills = currentState.playerKills + counterKillsPlayer,
                            enemyKills = currentState.enemyKills + counterKillsEnemy,
                            combatLogs = appendCombatLogs(currentState.combatLogs, counterLogs)
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
        val state = _battleState.value
        if (!state.canAcceptInput(fromLanClient) || state.isDeploymentPhase) return
        if (shouldForwardToLanHost(fromLanClient)) {
            forwardClientAction("WEAPON_MODE") { it.put("mode", mode.name) }
            return
        }
        val selected = resolveCommandableUnit(state, fromLanClient) ?: return
        sound.play(SoundType.CLICK)

        if (mode == WeaponMode.SKILL) {
            if (selected.template.archetype == UnitArchetype.SPORE_MINE) {
                val adjEnemy = state.units.firstOrNull {
                    it.isAlive && selected.isEnemyOf(it) && TacticalRules.isWithinMeleeReach(selected, it)
                }
                if (adjEnemy != null) {
                    actionJob?.cancel()
                    actionJob = viewModelScope.launch {
                        executeAttack(selected.uid, adjEnemy.uid, WeaponMode.MAIN)
                    }
                } else {
                    spawnFloatingText(selected.pos, "NO ENEMY IN MELEE RANGE", 0xFFFF4D58L)
                }
                return
            }

            val selfUpdated = TacticalRules.activateSelfSkillIfApplicable(selected, state.units)
            if (selfUpdated != null) {
                val units = TacticalRules.applyAuras(state.units.map { if (it.uid == selfUpdated.uid) selfUpdated else it })
                when (selected.template.archetype) {
                    UnitArchetype.VON_RYANS_LEAPER -> spawnFloatingText(selected.pos, "CHAMELEONIC CLOAK! (1T INVISIBLE)", 0xFF4CC9F0L)
                    UnitArchetype.ASSAULT_MARINE -> spawnFloatingText(selected.pos, "⚡ SHOCK ASSAULT! +1S +1AP MELEE", 0xFFFFD166L)
                    UnitArchetype.BLADEGUARD -> spawnFloatingText(selected.pos, "⚔️ REROLL 1s (HIT & SAVE 1T)", 0xFF4CC9F0L)
                    UnitArchetype.LEGIONARY -> spawnFloatingText(selected.pos, "⛧ VETERANS: REROLL WOUND 1s (1T)", 0xFFFFB703L)
                    else -> spawnFloatingText(selected.pos, "BOUNDING LEAP!", 0xFF38D96CL)
                }
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
                selectedUnitUid = selected.uid,
                weaponMode = nextMode,
                selectedDarkPact = DarkPactChoice.NONE,
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
        val state = _battleState.value
        if (!state.canAcceptInput(fromLanClient) || state.isDeploymentPhase) return
        if (shouldForwardToLanHost(fromLanClient)) {
            forwardClientAction("ROTATE") { it.put("left", left) }
            return
        }
        val selected = resolveCommandableUnit(state, fromLanClient) ?: return
        sound.play(SoundType.CLICK)
        val delta = if (left) 45f else -45f
        val nextDeg = ((selected.facingDeg + delta) % 360f + 360f) % 360f
        val nextFacing = TacticalRules.nearestFacingFromDeg(nextDeg)
        val units = state.units.map {
            if (it.uid == selected.uid) it.copy(facing = nextFacing, facingDeg = nextDeg) else it
        }
        _battleState.value = state.copy(selectedUnitUid = selected.uid, units = units, movePreview = null)
        syncCoopHostIfNeeded(_battleState.value)
    }

    fun toggleAutoFocus() {
        sound.play(SoundType.CLICK)
        _battleState.update { it.copy(autoFocusCamera = !it.autoFocusCamera) }
    }

    fun centerCamera() {
        sound.play(SoundType.CLICK)
        _battleState.update { state ->
            val target = if (state.isDeploymentPhase) {
                GridPos(if (state.deploymentTeamIndex == 0) 1 else MapGenerator.GRID_W - 2, MapGenerator.GRID_H / 2)
            } else {
                state.selectedUnit?.pos ?: GridPos(MapGenerator.GRID_W / 2, MapGenerator.GRID_H / 2)
            }
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
        val state = _battleState.value
        if (!state.canAcceptInput(fromLanClient) || state.isDeploymentPhase) return
        if (shouldForwardToLanHost(fromLanClient)) {
            forwardClientAction("END_TURN")
            return
        }
        sound.play(SoundType.TURN_HORN)

        if (!state.isCoopMode) {
            val aiFaction = state.units.firstOrNull { it.isAlive && it.faction != state.playerFaction }?.faction ?: state.enemyFaction
            val refreshedUnits = refreshFactionForNewTurn(clearTurnScopedMeleeBuffs(state.units), aiFaction)
            val firstAi = refreshedUnits.firstOrNull { it.faction == aiFaction && it.isAlive }
            val playerModels = refreshedUnits.filter { it.faction == state.playerFaction && it.isAlive }.sumOf { it.aliveModels }
            val enemyModels = refreshedUnits.filter { it.faction == aiFaction && it.isAlive }.sumOf { it.aliveModels }

            val updated = state.copy(
                activeFaction = aiFaction,
                units = TacticalRules.applyAuras(refreshedUnits),
                selectedUnitUid = firstAi?.uid,
                weaponMode = WeaponMode.MAIN,
                selectedDarkPact = DarkPactChoice.NONE,
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
                    playerFaction = state.playerFaction,
                    enemyFaction = aiFaction,
                    playerModelsAlive = playerModels,
                    enemyModelsAlive = enemyModels
                )
            )
            _battleState.value = updated
            scheduleSplashAutoDismiss()
            runAiTurnLoop(aiFaction)
            return
        }

        // 4-Player Co-op Sequential Turn Rotation (P1 -> P2 -> P3 -> P4 -> optional AI -> Next Round)
        advanceCoopTurnAfter(state, state.activeCoopPlayerId)
    }

    private fun activateCoopTurnForPlayer(
        state: BattleUiState,
        playerIdx: Int,
        targetRound: Int
    ) {
        val player = state.coopTurnOrder.getOrNull(playerIdx) ?: return
        val isAiTurn = player.isAi
        val refreshedUnits = TacticalRules.applyAuras(refreshCoopPlayerForNewTurn(clearTurnScopedMeleeBuffs(state.units), player.id))
        val firstUnit = refreshedUnits.firstOrNull { it.ownerPlayerId == player.id && it.isAlive }
        val updated = recomputeSelectionOverlays(
            state.copy(
                round = targetRound,
                activeFaction = player.faction,
                playerFaction = player.faction,
                units = refreshedUnits,
                selectedUnitUid = firstUnit?.uid,
                coopTurnIndex = playerIdx,
                activeCoopPlayerId = player.id,
                activeCoopPlayerName = player.name,
                weaponMode = WeaponMode.MAIN,
                selectedDarkPact = DarkPactChoice.NONE,
                moveModeActive = false,
                movePreview = null,
                chargeModeActive = false,
                chargePreview = null,
                targetPreview = null,
                isAiThinking = isAiTurn,
                focusGridPos = if (state.autoFocusCamera) firstUnit?.pos else state.focusGridPos,
                turnSplash = TurnSplashData(
                    round = targetRound,
                    activeFaction = player.faction,
                    isPlayerTurn = !isAiTurn && player.id == state.localControlledPlayerId,
                    marineModelsAlive = refreshedUnits.filter { it.faction == Faction.MARINES && it.isAlive }.sumOf { it.aliveModels },
                    xenosModelsAlive = refreshedUnits.filter { it.faction == Faction.XENOS && it.isAlive }.sumOf { it.aliveModels },
                    playerKills = state.playerKills,
                    playerLosses = state.enemyKills,
                    commanderLabel = if (isAiTurn) "${player.name.uppercase()} (AI) TURN" else "${player.name.uppercase()} TURN"
                )
            )
        )
        _battleState.value = updated
        syncCoopHostIfNeeded(updated)
        scheduleSplashAutoDismiss()
        if (isAiTurn) {
            runAiTurnLoop(player.faction, aiOwnerId = player.id)
        }
    }

    private fun advanceCoopTurnAfter(state: BattleUiState, finishedOwnerId: String?) {
        if (!state.active || state.summary != null || !state.isCoopMode) return
        val turnOrder = state.coopTurnOrder
        if (finishedOwnerId != "AI") {
            val nextPlayerIdx = ((state.coopTurnIndex + 1) until turnOrder.size).firstOrNull { idx ->
                val p = turnOrder[idx]
                p.isActive && state.units.any { it.ownerPlayerId == p.id && it.isAlive }
            }
            if (nextPlayerIdx != null) {
                activateCoopTurnForPlayer(state, nextPlayerIdx, state.round)
                return
            }

            // End of player/bot turnOrder slots in this round: check fallback Team 2 "AI" units
            val aliveAiUnits = state.units.filter { it.ownerPlayerId == "AI" && it.isAlive }
            if (aliveAiUnits.isNotEmpty()) {
                val aiFaction = aliveAiUnits.first().faction
                val refreshedUnits = TacticalRules.applyAuras(refreshCoopPlayerForNewTurn(clearTurnScopedMeleeBuffs(state.units), "AI"))
                val firstAi = refreshedUnits.firstOrNull { it.ownerPlayerId == "AI" && it.isAlive }
                val updated = state.copy(
                    activeFaction = aiFaction,
                    units = refreshedUnits,
                    selectedUnitUid = firstAi?.uid,
                    activeCoopPlayerId = "AI",
                    activeCoopPlayerName = "AI Command",
                    weaponMode = WeaponMode.MAIN,
                    selectedDarkPact = DarkPactChoice.NONE,
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
                runAiTurnLoop(aiFaction, aiOwnerId = "AI")
                return
            }
        }

        // Advance to next Round and first alive player/bot in turnOrder
        val nextRound = state.round + 1
        val firstIdx = turnOrder.indexOfFirst { p ->
            p.isActive && state.units.any { it.ownerPlayerId == p.id && it.isAlive }
        }.takeIf { it >= 0 } ?: 0
        activateCoopTurnForPlayer(state, firstIdx, nextRound)
    }

    private fun runAiTurnLoop(aiFaction: Faction, aiOwnerId: String? = null) {
        aiJob?.cancel()
        aiJob = viewModelScope.launch {
            delay(1400)
            var guard = 0
            while (guard < 40) {
                guard++
                val current = _battleState.value
                if (!current.active || current.summary != null ||
                    (current.isCoopMode && current.activeCoopPlayerId != aiOwnerId) ||
                    (!current.isCoopMode && current.activeFaction != aiFaction)
                ) break

                val decision = withContext(Dispatchers.Default) {
                    AiController.decideNextAction(
                        aiFaction = aiFaction,
                        units = current.units,
                        map = current.map.rows,
                        aiOwnerId = aiOwnerId
                    )
                }
                when (decision) {
                    is AiDecision.SelfSkill -> {
                        val units = current.units.map {
                            if (it.uid == decision.unitUid) TacticalRules.activateSelfSkillIfApplicable(it, current.units) ?: it else it
                        }
                        val u = units.firstOrNull { it.uid == decision.unitUid }
                        if (u != null) {
                            when (u.template.archetype) {
                                UnitArchetype.VON_RYANS_LEAPER -> spawnFloatingText(u.pos, "CHAMELEONIC CLOAK!", 0xFF4CC9F0L)
                                UnitArchetype.ASSAULT_MARINE -> spawnFloatingText(u.pos, "⚡ SHOCK ASSAULT! +1S +1AP", 0xFFFFD166L)
                                UnitArchetype.BLADEGUARD -> spawnFloatingText(u.pos, "⚔️ REROLL 1s (HIT & SAVE)", 0xFF4CC9F0L)
                                else -> spawnFloatingText(u.pos, "LEAP!", 0xFFC77DFFL)
                            }
                        }
                        _battleState.update { it.copy(units = units, focusGridPos = if (it.autoFocusCamera) u?.pos else it.focusGridPos) }
                        syncCoopHostIfNeeded(_battleState.value)
                        delay(840)
                    }
                    is AiDecision.Teleport -> {
                        performTeleport(decision.unitUid, decision.targetPos)
                        delay(900)
                    }
                    is AiDecision.SeedSporeMine -> {
                        performSeedSporeMine(decision.unitUid, decision.targetPos)
                        delay(900)
                    }
                    is AiDecision.Advance -> {
                        val (roll, bonusCm) = TacticalRules.rollAdvanceBonus()
                        val units = current.units.map { u ->
                            if (u.uid != decision.unitUid) u
                            else u.copy(
                                moveCmLeft = u.moveCmLeft + bonusCm,
                                hasAdvanced = true,
                                activeBuffLabel = if (u.template.mainWeaponAssault) "ADVANCE +$roll cm (ASSAULT)" else "ADVANCE +$roll cm"
                            )
                        }
                        val u = units.firstOrNull { it.uid == decision.unitUid }
                        val advLog = u?.let { advUnit ->
                            CombatLogEntry(
                                id = nextIdCounter++,
                                round = current.round,
                                faction = advUnit.faction,
                                title = "${advUnit.template.name.uppercase()} • ADVANCE",
                                subtitle = "MOVEMENT PHASE • 1D6",
                                steps = listOf(
                                    DiceStepDetail(
                                        phaseName = "ADVANCE",
                                        stepLabel = "1D6 ADVANCE",
                                        targetNeeded = 1,
                                        rolled = 1,
                                        passed = 1,
                                        failed = 0,
                                        diceValues = listOf(roll),
                                        note = if (advUnit.template.mainWeaponAssault) "ASSAULT OK" else "NO SHOOT/CHRG"
                                    )
                                ),
                                resultSummary = "+$roll cm Move",
                                isSuccess = true
                            )
                        }
                        if (u != null) spawnFloatingText(u.pos, "ADVANCE +$roll cm", 0xFF38D96CL)
                        _battleState.update { st ->
                            st.copy(
                                units = units,
                                focusGridPos = if (st.autoFocusCamera) u?.pos else st.focusGridPos,
                                combatLogs = if (advLog != null) appendCombatLogs(st.combatLogs, listOf(advLog)) else st.combatLogs
                            )
                        }
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
                        executeAttack(
                            attackerUid = decision.attackerUid,
                            targetUid = decision.targetUid,
                            mode = decision.mode,
                            darkPactChoice = decision.darkPactChoice
                        )
                        delay(1300)
                    }
                    AiDecision.TurnComplete -> break
                }
            }

            // Transition to next Co-op player/bot or back to Skirmish player turn
            val endState = _battleState.value
            if (endState.active && endState.summary == null) {
                sound.play(SoundType.TURN_HORN)
                if (endState.isCoopMode) {
                    advanceCoopTurnAfter(endState, aiOwnerId)
                } else {
                    val nextRound = endState.round + 1
                    val refreshed = TacticalRules.applyAuras(refreshFactionForNewTurn(clearTurnScopedMeleeBuffs(endState.units), endState.playerFaction))
                    val firstPlayer = refreshed.firstOrNull { it.faction == endState.playerFaction && it.isAlive }
                    val resolvedEnemy = endState.units.firstOrNull { it.faction != endState.playerFaction }?.faction ?: endState.enemyFaction
                    val playerModels = refreshed.filter { it.faction == endState.playerFaction && it.isAlive }.sumOf { it.aliveModels }
                    val enemyModels = refreshed.filter { it.faction != endState.playerFaction && it.isAlive }.sumOf { it.aliveModels }
                    val nextState = endState.copy(
                        round = nextRound,
                        activeFaction = endState.playerFaction,
                        units = refreshed,
                        selectedUnitUid = firstPlayer?.uid,
                        weaponMode = WeaponMode.MAIN,
                        selectedDarkPact = DarkPactChoice.NONE,
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
                            playerLosses = endState.enemyKills,
                            playerFaction = endState.playerFaction,
                            enemyFaction = resolvedEnemy,
                            playerModelsAlive = playerModels,
                            enemyModelsAlive = enemyModels
                        )
                    )
                    _battleState.value = recomputeSelectionOverlays(nextState)
                    scheduleSplashAutoDismiss()
                }
            }
        }
    }

    private fun refreshCoopPlayerForNewTurn(units: List<BattleUnit>, ownerId: String): List<BattleUnit> {
        val incomingTeam = if (ownerId in listOf("p1", "p2")) 0 else 1
        return units.map { u ->
            if (!u.isAlive) u
            else {
                val clearedMark = if (u.markedByTeam != null && u.markedByTeam != incomingTeam) null else u.markedByTeam
                if (u.ownerPlayerId != ownerId) {
                    if (u.markedByTeam == clearedMark) u else u.copy(markedByTeam = clearedMark)
                } else {
                    val nextBuffTurns = (u.buffTurnsLeft - 1).coerceAtLeast(0)
                    val nextDamageReduction = (u.damageReductionTurns - 1).coerceAtLeast(0)
                    val nextStealthTurns = (u.stealthTurnsLeft - 1).coerceAtLeast(0)
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
                        stealthTurnsLeft = nextStealthTurns,
                        activeBuffLabel = if (nextBuffTurns > 0 || nextDamageReduction > 0 || nextStealthTurns > 0) u.activeBuffLabel else null,
                        markedByTeam = clearedMark,
                        meleeChargeBuffActive = false,
                        bladeguardRerollBuffActive = false,
                        legionaryWoundRerollBuffActive = false
                    )
                }
            }
        }
    }

    private fun clearTurnScopedMeleeBuffs(units: List<BattleUnit>): List<BattleUnit> =
        units.map { u ->
            if (!u.meleeChargeBuffActive && !u.bladeguardRerollBuffActive) u
            else u.copy(
                meleeChargeBuffActive = false,
                bladeguardRerollBuffActive = false,
                activeBuffLabel = if (u.activeBuffLabel == "+1S +1AP MELEE" || u.activeBuffLabel == "REROLL 1s (HIT & SAVE)") null else u.activeBuffLabel
            )
        }

    private fun refreshFactionForNewTurn(units: List<BattleUnit>, faction: Faction): List<BattleUnit> {
        val incomingTeam = units.firstOrNull { it.faction == faction && it.isAlive }?.effectiveTeam
            ?: if (faction == Faction.MARINES) 0 else 1
        return units.map { u ->
            if (!u.isAlive) u
            else {
                val clearedMark = if (u.markedByTeam != null && u.markedByTeam != incomingTeam) null else u.markedByTeam
                if (u.faction != faction) {
                    if (u.markedByTeam == clearedMark) u else u.copy(markedByTeam = clearedMark)
                } else {
                    val nextBuffTurns = (u.buffTurnsLeft - 1).coerceAtLeast(0)
                    val nextDamageReduction = (u.damageReductionTurns - 1).coerceAtLeast(0)
                    val nextStealthTurns = (u.stealthTurnsLeft - 1).coerceAtLeast(0)
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
                        stealthTurnsLeft = nextStealthTurns,
                        activeBuffLabel = if (nextBuffTurns > 0 || nextDamageReduction > 0 || nextStealthTurns > 0) u.activeBuffLabel else null,
                        markedByTeam = clearedMark,
                        meleeChargeBuffActive = false,
                        bladeguardRerollBuffActive = false,
                        legionaryWoundRerollBuffActive = false
                    )
                }
            }
        }
    }

    private fun recomputeSelectionOverlays(state: BattleUiState): BattleUiState {
        val selected = state.selectedUnit
        if (selected == null || !state.canCommandUnit(selected, isRemoteClientAction = true) || state.isAnimating) {
            return state.copy(
                reachableTiles = emptyMap(),
                moveParents = emptyMap(),
                attackableUids = emptySet(),
                chargeableUids = emptySet()
            )
        }

        val inMelee = TacticalRules.isEngagedInMelee(selected, state.units)

        if (selected.template.archetype == UnitArchetype.TERMINATOR && state.weaponMode == WeaponMode.SKILL) {
            val canTeleport = !inMelee && selected.skillCooldownLeft == 0 && !selected.hasAdvanced && !selected.hasCharged
            val teleportTiles = if (canTeleport) {
                TacticalRules.computeTeleportTiles(selected, state.map.rows, state.units, maxDist = 9, minEnemyDist = 5)
            } else emptySet()
            return state.copy(
                moveModeActive = false,
                chargeModeActive = false,
                reachableTiles = teleportTiles.associateWith { 0 },
                moveParents = emptyMap(),
                attackableUids = emptySet(),
                chargeableUids = emptySet()
            )
        }

        if (selected.template.archetype == UnitArchetype.BIOVORE && state.weaponMode == WeaponMode.SKILL) {
            val canSeed = !inMelee && selected.skillCooldownLeft == 0 && !selected.hasAdvanced && !selected.hasCharged
            val seedTiles = if (canSeed) {
                TacticalRules.computeSeedSporeMineTiles(selected, state.map.rows, state.units, maxDist = 8, minEnemyDistCm = 8.0f)
            } else emptySet()
            return state.copy(
                moveModeActive = false,
                chargeModeActive = false,
                reachableTiles = seedTiles.associateWith { 0 },
                moveParents = emptyMap(),
                attackableUids = emptySet(),
                chargeableUids = emptySet()
            )
        }

        val effectiveMoveMode = if (inMelee) false else state.moveModeActive
        val effectiveChargeMode = if (inMelee) false else state.chargeModeActive

        val chargeable = if (effectiveChargeMode) {
            TacticalRules.computeChargeableTargets(selected, state.units, state.map.rows)
        } else emptySet()

        val attackable = if (effectiveChargeMode) {
            emptySet()
        } else {
            state.units
                .filter { it.isAlive && selected.isEnemyOf(it) }
                .filter { enemy ->
                    val p = TacticalRules.canTarget(selected, enemy, state.weaponMode, state.map.rows, state.units)
                    p != null && p.inRange && p.hasLos
                }
                .map { it.uid }
                .toSet()
        }

        return state.copy(
            moveModeActive = effectiveMoveMode,
            chargeModeActive = effectiveChargeMode,
            movePreview = if (inMelee) null else state.movePreview,
            chargePreview = if (inMelee) null else state.chargePreview,
            reachableTiles = emptyMap(),
            moveParents = emptyMap(),
            attackableUids = attackable,
            chargeableUids = chargeable
        )
    }

    private fun checkVictoryOrUpdate(state: BattleUiState): BattleUiState {
        if (state.isCoopMode) {
            val team0Alive = state.units.any { it.effectiveTeam == 0 && it.isAlive }
            val team1Alive = state.units.any { it.effectiveTeam == 1 && it.isAlive }
            if (team0Alive && team1Alive) return state

            aiJob?.cancel()
            actionJob?.cancel()
            sound.play(SoundType.VICTORY)
            val winningTeam = if (team0Alive) 0 else 1
            val winnerFaction = state.units.firstOrNull { it.effectiveTeam == winningTeam && it.isAlive }?.faction
                ?: if (winningTeam == 0) Faction.MARINES else Faction.XENOS
            val winnerLabel = if (winningTeam == 0) "TEAM 1 (WEST)" else "TEAM 2 (EAST)"
            val localTeam = if (state.localControlledPlayerId in listOf("p3", "p4")) 1 else 0
            val playerWon = winningTeam == localTeam
            val totalDmg = state.units.filter { it.effectiveTeam == localTeam }.sumOf { it.damageDealt }

            return state.copy(
                isAiThinking = false,
                isAnimating = false,
                reachableTiles = emptyMap(),
                attackableUids = emptySet(),
                targetPreview = null,
                summary = BattleSummary(
                    winner = winnerFaction,
                    playerWon = playerWon,
                    rounds = state.round,
                    playerKills = state.playerKills,
                    enemyKills = state.enemyKills,
                    totalDamageDealt = totalDmg,
                    winnerLabel = winnerLabel
                )
            )
        }

        val playerAlive = state.units.any { it.faction == state.playerFaction && it.isAlive }
        val enemyAlive = state.units.any { it.faction != state.playerFaction && it.isAlive }
        if (playerAlive && enemyAlive) return state

        aiJob?.cancel()
        actionJob?.cancel()
        sound.play(SoundType.VICTORY)
        val winner = if (playerAlive) {
            state.playerFaction
        } else {
            state.units.firstOrNull { it.faction != state.playerFaction }?.faction ?: state.enemyFaction
        }
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

    private fun appendCombatLogs(
        existing: List<CombatLogEntry>,
        additions: List<CombatLogEntry>,
        maxEntries: Int = 40
    ): List<CombatLogEntry> {
        if (additions.isEmpty()) return existing
        return (additions.reversed() + existing).take(maxEntries)
    }

    private fun scheduleSplashAutoDismiss() {
        viewModelScope.launch {
            delay(2800)
            _battleState.update { it.copy(turnSplash = null) }
        }
    }
}
