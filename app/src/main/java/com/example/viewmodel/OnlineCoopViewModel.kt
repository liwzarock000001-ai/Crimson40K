package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.OnlineClientAction
import com.example.data.OnlineCoopRepository
import com.example.data.OnlineRoomSnapshot
import com.example.data.OnlineRoomSummary
import com.example.data.RoomPresencePolicy
import com.example.data.extractFirestoreRootError
import com.example.data.isAbandonedEmptyRoom
import com.example.model.CoopPlayer
import com.example.model.CoopRosterUnit
import com.example.model.CoopState
import com.example.model.UnitTemplate
import com.example.model.WeaponMode
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class OnlineCoopViewModel(
    private val repository: OnlineCoopRepository,
    val currentUserId: String,
    val displayName: String
) : ViewModel() {

    private companion object {
        const val TAG = "OnlineCoopVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }

    private var currentRoomCode: String? = null
    private val deletingRoomCodes = HashSet<String>()

    val myRoomsState: StateFlow<UiState<List<OnlineRoomSummary>>> = repository.observeMyRooms(currentUserId)
        .onEach { rooms ->
            if (currentRoomCode == null) {
                sweepAbandonedRooms(rooms)
            }
        }
        .map<List<OnlineRoomSummary>, UiState<List<OnlineRoomSummary>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = extractFirestoreRootError(error)
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing online rooms", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load online rooms"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    private val _activeRoom = MutableStateFlow<OnlineRoomSnapshot?>(null)
    val activeRoom: StateFlow<OnlineRoomSnapshot?> = _activeRoom.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _createError = MutableStateFlow<String?>(null)
    val createError: StateFlow<String?> = _createError.asStateFlow()

    private val _joinError = MutableStateFlow<String?>(null)
    val joinError: StateFlow<String?> = _joinError.asStateFlow()

    private val incomingActionsChannel = Channel<OnlineClientAction>(Channel.UNLIMITED)
    val incomingActions = incomingActionsChannel.receiveAsFlow()

    private val roomClosedChannel = Channel<String>(Channel.CONFLATED)
    val roomClosedEvents = roomClosedChannel.receiveAsFlow()

    private data class BattlePublishRequest(
        val roomCode: String,
        val state: BattleUiState,
        val templates: Map<String, UnitTemplate>,
        val status: String
    )

    private var roomObserverJob: Job? = null
    private var actionsObserverJob: Job? = null
    private var heartbeatJob: Job? = null
    private var currentPlayerSlotId: String = "p1"
    private val processedActionIds = HashSet<String>()
    private val battlePublishChannel = Channel<BattlePublishRequest>(Channel.CONFLATED)
    private var publishedTemplatesForRoom: String? = null
    private var publishedTemplatesHash: Int = 0

    init {
        viewModelScope.launch {
            for (initialReq in battlePublishChannel) {
                var currentReq = initialReq
                var attempt = 0
                var showedSyncWarning = false
                while (currentRoomCode == currentReq.roomCode || currentReq.status == "FINISHED") {
                    // Coalesce to newest pending snapshot if one arrived while waiting/retrying
                    while (true) {
                        val newer = battlePublishChannel.tryReceive().getOrNull() ?: break
                        currentReq = newer
                        attempt = 0
                    }
                    val needTemplates = currentReq.roomCode != publishedTemplatesForRoom ||
                        currentReq.templates.hashCode() != publishedTemplatesHash
                    val result = repository.publishBattleSnapshot(
                        roomCode = currentReq.roomCode,
                        battleState = currentReq.state,
                        templates = currentReq.templates,
                        status = currentReq.status,
                        includeTemplates = needTemplates
                    )
                    if (result.isSuccess) {
                        publishedTemplatesForRoom = currentReq.roomCode
                        publishedTemplatesHash = currentReq.templates.hashCode()
                        if (showedSyncWarning && _errorMessage.value?.startsWith("Cloud sync delayed") == true) {
                            _errorMessage.value = null
                        }
                        break
                    }
                    val err = result.exceptionOrNull()
                    if (err is CancellationException) throw err
                    Log.w(TAG, "Failed to sync battle state to cloud (attempt ${attempt + 1})", err)
                    val fsErr = err?.let { extractFirestoreRootError(it) }
                    if (fsErr?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        _errorMessage.value = fsErr.message ?: "Cloud sync rejected by room rules."
                        break
                    }
                    attempt++
                    val delayMs = when (attempt) {
                        1 -> 350L
                        2 -> 800L
                        3 -> 1500L
                        else -> {
                            showedSyncWarning = true
                            _errorMessage.value = "Cloud sync delayed — retrying..."
                            3000L
                        }
                    }
                    delay(delayMs)
                    if (currentReq.status == "FINISHED" && attempt >= 3) break
                }
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
        _createError.value = null
        _joinError.value = null
    }

    fun clearCreateError() {
        _createError.value = null
    }

    fun clearJoinError() {
        _joinError.value = null
    }

    fun createOnlineRoom(
        hostName: String,
        initialPlayers: List<CoopPlayer>,
        initialUnits: List<CoopRosterUnit>,
        spawnAiIfSameFaction: Boolean,
        pointLimit: Int = 1000,
        templates: Map<String, UnitTemplate> = emptyMap(),
        onRoomReady: (roomCode: String, assignedSlot: String, isHost: Boolean) -> Unit
    ) {
        if (_isBusy.value) return
        viewModelScope.launch {
            _isBusy.value = true
            _createError.value = null
            _errorMessage.value = null
            val res = repository.createOnlineRoom(
                hostName = hostName.ifBlank { displayName },
                initialPlayers = initialPlayers,
                initialUnits = initialUnits,
                spawnAiIfSameFaction = spawnAiIfSameFaction,
                pointLimit = pointLimit,
                templates = templates
            )
            _isBusy.value = false
            res.onSuccess { code ->
                currentPlayerSlotId = "p1"
                startObservingRoom(code, asHost = true)
                onRoomReady(code, "p1", true)
            }.onFailure { err ->
                val msg = err.message ?: "Unable to create online room."
                _createError.value = msg
                _errorMessage.value = msg
            }
        }
    }

    fun joinRoomByCode(
        roomCode: String,
        preferredSlotId: String,
        joinerName: String,
        onRoomReady: (roomCode: String, assignedSlot: String, isHost: Boolean) -> Unit
    ) {
        if (_isBusy.value) return
        val cleanCode = roomCode.trim()
        if (cleanCode.isBlank()) {
            _joinError.value = "Please enter the 6-digit Room Code."
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            _joinError.value = null
            _errorMessage.value = null
            val res = repository.joinOnlineRoomByCode(
                roomCode = cleanCode,
                preferredSlotId = preferredSlotId,
                joinerName = joinerName.ifBlank { displayName }
            )
            _isBusy.value = false
            res.onSuccess { assignedSlot ->
                currentPlayerSlotId = assignedSlot
                val isHost = assignedSlot == "p1"
                startObservingRoom(cleanCode, asHost = isHost)
                onRoomReady(cleanCode, assignedSlot, isHost)
            }.onFailure { err ->
                val msg = err.message ?: "Could not join Room #$cleanCode."
                _joinError.value = msg
                _errorMessage.value = msg
            }
        }
    }

    fun rejoinExistingRoom(
        summary: OnlineRoomSummary,
        onRoomReady: (roomCode: String, assignedSlot: String, isHost: Boolean) -> Unit
    ) {
        val isHost = summary.hostUid == currentUserId
        joinRoomByCode(
            roomCode = summary.roomCode,
            preferredSlotId = if (isHost) "p1" else "p2",
            joinerName = displayName,
            onRoomReady = onRoomReady
        )
    }

    fun sweepAbandonedRoomsIfNeeded() {
        val rooms = (myRoomsState.value as? UiState.Success)?.data ?: return
        sweepAbandonedRooms(rooms)
    }

    private fun sweepAbandonedRooms(rooms: List<OnlineRoomSummary>) {
        if (currentRoomCode != null) return
        val nowSec = System.currentTimeMillis() / 1000L
        val targets = rooms.filter { summary ->
            summary.roomCode !in deletingRoomCodes &&
                isAbandonedEmptyRoom(
                    summary = summary,
                    currentUid = currentUserId,
                    activeRoomCode = currentRoomCode,
                    nowSeconds = nowSec
                )
        }
        if (targets.isEmpty()) return
        deleteRoomSummaries(targets)
    }

    fun cleanupUnusedRooms() {
        if (_isBusy.value || currentRoomCode != null) return
        val rooms = (myRoomsState.value as? UiState.Success)?.data ?: return
        val nowSec = System.currentTimeMillis() / 1000L
        val targets = rooms.filter { room ->
            room.roomCode !in deletingRoomCodes && (
                room.status == "FINISHED" ||
                    (room.hostUid == currentUserId && room.otherAliveMembersCount(currentUserId, nowSec) == 0) ||
                    (room.hostUid != currentUserId && room.aliveMembersCount(nowSec) == 0)
                )
        }
        if (targets.isEmpty()) return
        viewModelScope.launch {
            _isBusy.value = true
            try {
                for (room in targets) {
                    if (!deletingRoomCodes.add(room.roomCode)) continue
                    try {
                        repository.deleteOnlineRoom(
                            roomCode = room.roomCode,
                            isHostOwned = room.hostUid == currentUserId
                        )
                    } finally {
                        deletingRoomCodes.remove(room.roomCode)
                    }
                }
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun deleteRoom(summary: OnlineRoomSummary) {
        if (_isBusy.value || summary.roomCode.isBlank()) return
        if (currentRoomCode == summary.roomCode) return
        viewModelScope.launch {
            _isBusy.value = true
            _joinError.value = null
            _errorMessage.value = null
            val res = if (deletingRoomCodes.add(summary.roomCode)) {
                try {
                    repository.deleteOnlineRoom(
                        roomCode = summary.roomCode,
                        isHostOwned = summary.hostUid == currentUserId
                    )
                } finally {
                    deletingRoomCodes.remove(summary.roomCode)
                }
            } else {
                Result.success(Unit)
            }
            _isBusy.value = false
            res.onFailure { err ->
                val msg = err.message ?: "Could not delete Room #${summary.roomCode}."
                _joinError.value = msg
                _errorMessage.value = msg
            }
        }
    }

    private fun deleteRoomSummaries(targets: List<OnlineRoomSummary>) {
        viewModelScope.launch {
            for (room in targets) {
                if (!deletingRoomCodes.add(room.roomCode)) continue
                try {
                    repository.deleteOnlineRoom(
                        roomCode = room.roomCode,
                        isHostOwned = room.hostUid == currentUserId
                    )
                } finally {
                    deletingRoomCodes.remove(room.roomCode)
                }
            }
        }
    }

    private fun startObservingRoom(roomCode: String, asHost: Boolean) {
        stopRoomListeners()
        currentRoomCode = roomCode
        processedActionIds.clear()

        roomObserverJob = viewModelScope.launch {
            var sawNonNullSnapshot = false
            repository.observeRoom(roomCode)
                .retryWhen { cause, attempt ->
                    if (cause is CancellationException) return@retryWhen false
                    val fsErr = extractFirestoreRootError(cause)
                    if (fsErr?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) return@retryWhen false
                    Log.w(TAG, "Room listener error (retrying attempt ${attempt + 1})", fsErr ?: cause)
                    delay(((attempt + 1) * 750L).coerceAtMost(5000L))
                    currentRoomCode == roomCode
                }
                .catch { error ->
                    val firestoreError = extractFirestoreRootError(error)
                    if (firestoreError == null && error is CancellationException) throw error
                    _errorMessage.value = (firestoreError ?: error).message ?: "Connection lost to Room #$roomCode"
                }
                .collect { snap ->
                    _activeRoom.value = snap
                    if (snap != null) {
                        sawNonNullSnapshot = true
                        val resolvedSlot = snap.playerUids.entries.firstOrNull { it.value == currentUserId }?.key
                        if (resolvedSlot != null) {
                            currentPlayerSlotId = resolvedSlot
                        }
                        if (snap.hostUid == currentUserId && actionsObserverJob?.isActive != true) {
                            startObservingHostActions(roomCode)
                        }
                    } else if (sawNonNullSnapshot && currentRoomCode == roomCode) {
                        stopRoomListeners()
                        roomClosedChannel.trySend("Room #$roomCode was closed.")
                    }
                }
        }
        if (asHost) {
            startObservingHostActions(roomCode)
        }
        startRoomHeartbeat(roomCode, asHost)
    }

    private fun startRoomHeartbeat(roomCode: String, asHost: Boolean) {
        heartbeatJob?.cancel()
        heartbeatJob = viewModelScope.launch {
            while (currentRoomCode == roomCode) {
                val snap = _activeRoom.value
                if (snap?.status == "FINISHED") break
                repository.sendRoomHeartbeat(roomCode)
                val currentSnap = _activeRoom.value
                if ((asHost || currentSnap?.hostUid == currentUserId) &&
                    currentSnap != null &&
                    currentSnap.status == "LOBBY" &&
                    currentSnap.hostUid == currentUserId
                ) {
                    val nowSec = System.currentTimeMillis() / 1000L
                    val hasStaleGuest = currentSnap.memberUids.any { memberUid ->
                        memberUid != currentUserId &&
                            (currentSnap.heartbeatsSeconds[memberUid]?.let { (nowSec - it) > RoomPresencePolicy.STALE_GUEST_EVICT_SEC } == true)
                    }
                    if (hasStaleGuest) {
                        repository.evictStaleGuestsInLobby(roomCode)
                    }
                }
                delay(RoomPresencePolicy.HEARTBEAT_INTERVAL_MS)
            }
        }
    }

    private fun startObservingHostActions(roomCode: String) {
        actionsObserverJob?.cancel()
        actionsObserverJob = viewModelScope.launch {
            repository.observeRoomActions(roomCode, currentUserId)
                .retryWhen { cause, attempt ->
                    if (cause is CancellationException) return@retryWhen false
                    val fsErr = extractFirestoreRootError(cause)
                    if (fsErr?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) return@retryWhen false
                    Log.w(TAG, "Actions listener error (retrying attempt ${attempt + 1})", fsErr ?: cause)
                    delay(((attempt + 1) * 750L).coerceAtMost(5000L))
                    currentRoomCode == roomCode
                }
                .catch { error ->
                    val firestoreError = extractFirestoreRootError(error)
                    if (firestoreError == null && error is CancellationException) throw error
                    Log.w(TAG, "Error observing client actions", firestoreError ?: error)
                }
                .onCompletion {
                    actionsObserverJob = null
                }
                .collect { actions ->
                    for (action in actions) {
                        if (processedActionIds.add(action.actionId)) {
                            incomingActionsChannel.trySend(action)
                            launch {
                                repository.consumeAction(roomCode, action.actionId)
                            }
                        }
                    }
                }
        }
    }

    fun syncHostLobby(coopState: CoopState, templates: Map<String, UnitTemplate> = emptyMap()) {
        val code = currentRoomCode ?: coopState.roomCode
        val snap = _activeRoom.value
        if (code.isBlank()) return
        val uidMap = snap?.playerUids ?: mapOf("p1" to currentUserId)
        viewModelScope.launch {
            if (snap != null && snap.hostUid != currentUserId) {
                repository.updateMemberLobby(code, coopState).onFailure { err ->
                    _errorMessage.value = err.message ?: "Failed to sync roster changes."
                }
            } else {
                repository.updateHostLobby(code, coopState, uidMap, templates).onFailure { err ->
                    _errorMessage.value = err.message ?: "Failed to sync room changes."
                }
            }
        }
    }

    fun publishBattleState(state: BattleUiState, templates: Map<String, UnitTemplate>) {
        val code = currentRoomCode ?: return
        val snap = _activeRoom.value
        if (snap != null && (snap.hostUid != currentUserId || snap.status == "FINISHED")) return
        val nextStatus = if (state.summary != null || !state.active) "FINISHED" else "ACTIVE"
        battlePublishChannel.trySend(BattlePublishRequest(code, state, templates, nextStatus))
    }

    fun submitClientAction(
        actionType: String,
        wx: Float? = null,
        wy: Float? = null,
        cm: Float? = null,
        mode: WeaponMode? = null,
        left: Boolean? = null
    ) {
        val snap = _activeRoom.value ?: return
        val code = snap.roomCode.ifBlank { currentRoomCode ?: return }
        if (snap.hostUid.isBlank() || snap.status != "ACTIVE") return
        viewModelScope.launch {
            var res = repository.submitClientAction(
                roomCode = code,
                hostUid = snap.hostUid,
                playerId = currentPlayerSlotId,
                actionType = actionType,
                wx = wx,
                wy = wy,
                cm = cm,
                mode = mode,
                left = left
            )
            if (res.isFailure) {
                val fsErr = res.exceptionOrNull()?.let { extractFirestoreRootError(it) }
                if (fsErr?.code != FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    delay(350L)
                    res = repository.submitClientAction(
                        roomCode = code,
                        hostUid = snap.hostUid,
                        playerId = currentPlayerSlotId,
                        actionType = actionType,
                        wx = wx,
                        wy = wy,
                        cm = cm,
                        mode = mode,
                        left = left
                    )
                }
            }
            res.onFailure { err ->
                _errorMessage.value = err.message ?: "Failed to send command to Host."
            }
        }
    }

    fun leaveActiveRoom(
        finalInactiveBattle: BattleUiState? = null,
        templates: Map<String, UnitTemplate> = emptyMap()
    ) {
        val code = currentRoomCode
        val snap = _activeRoom.value
        val isHost = snap != null && snap.hostUid == currentUserId
        val alreadyFinished = snap?.status == "FINISHED"
        val nowSec = System.currentTimeMillis() / 1000L
        val otherAlivePeers = if (snap != null) {
            snap.memberUids.count { memberUid ->
                memberUid != currentUserId &&
                    (snap.heartbeatsSeconds[memberUid]?.let { (nowSec - it) <= RoomPresencePolicy.MEMBER_ALIVE_WINDOW_SEC } == true)
            }
        } else 0

        stopRoomListeners()
        if (!code.isNullOrBlank()) {
            viewModelScope.launch {
                when {
                    isHost && (alreadyFinished || finalInactiveBattle == null || otherAlivePeers == 0) -> {
                        repository.deleteOnlineRoom(code, isHostOwned = true)
                    }
                    isHost && finalInactiveBattle != null && otherAlivePeers > 0 -> {
                        repository.publishBattleSnapshot(code, finalInactiveBattle, templates, "FINISHED")
                    }
                    else -> {
                        repository.leaveOnlineRoom(code)
                    }
                }
            }
        }
    }

    fun applyRemoteBattleSnapshot(
        battleMap: Map<String, Any?>,
        current: BattleUiState,
        fallbackTemplates: Map<String, UnitTemplate>? = null
    ): BattleUiState? = repository.applyBattleMapToState(battleMap, current, fallbackTemplates)

    fun stopRoomListeners() {
        heartbeatJob?.cancel()
        heartbeatJob = null
        roomObserverJob?.cancel()
        roomObserverJob = null
        actionsObserverJob?.cancel()
        actionsObserverJob = null
        currentRoomCode = null
        publishedTemplatesForRoom = null
        publishedTemplatesHash = 0
        _activeRoom.value = null
        processedActionIds.clear()
    }

    override fun onCleared() {
        super.onCleared()
        stopRoomListeners()
    }
}
