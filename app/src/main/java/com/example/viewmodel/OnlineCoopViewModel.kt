package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.OnlineClientAction
import com.example.data.OnlineCoopRepository
import com.example.data.OnlineRoomSnapshot
import com.example.data.OnlineRoomSummary
import com.example.data.extractFirestoreRootError
import com.example.model.CoopPlayer
import com.example.model.CoopRosterUnit
import com.example.model.CoopState
import com.example.model.UnitTemplate
import com.example.model.WeaponMode
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
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

    val myRoomsState: StateFlow<UiState<List<OnlineRoomSummary>>> = repository.observeMyRooms(currentUserId)
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

    private val _incomingActions = MutableSharedFlow<OnlineClientAction>(extraBufferCapacity = 32)
    val incomingActions = _incomingActions.asSharedFlow()

    private var roomObserverJob: Job? = null
    private var actionsObserverJob: Job? = null
    private var currentRoomCode: String? = null
    private var currentPlayerSlotId: String = "p1"
    private val processedActionIds = HashSet<String>()

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

    private fun startObservingRoom(roomCode: String, asHost: Boolean) {
        stopRoomListeners()
        currentRoomCode = roomCode
        processedActionIds.clear()

        roomObserverJob = viewModelScope.launch {
            repository.observeRoom(roomCode)
                .catch { error ->
                    val firestoreError = extractFirestoreRootError(error)
                    if (firestoreError == null && error is CancellationException) throw error
                    _errorMessage.value = (firestoreError ?: error).message ?: "Connection lost to Room #$roomCode"
                }
                .collect { snap ->
                    _activeRoom.value = snap
                    if (snap != null && snap.hostUid == currentUserId && actionsObserverJob == null) {
                        startObservingHostActions(roomCode)
                    }
                }
        }
        if (asHost) {
            startObservingHostActions(roomCode)
        }
    }

    private fun startObservingHostActions(roomCode: String) {
        actionsObserverJob?.cancel()
        actionsObserverJob = viewModelScope.launch {
            repository.observeRoomActions(roomCode, currentUserId)
                .catch { error ->
                    val firestoreError = extractFirestoreRootError(error)
                    if (firestoreError == null && error is CancellationException) throw error
                    Log.w(TAG, "Error observing client actions", firestoreError ?: error)
                }
                .collect { actions ->
                    for (action in actions) {
                        if (processedActionIds.add(action.actionId)) {
                            _incomingActions.tryEmit(action)
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
        if (code.isBlank() || (snap != null && snap.hostUid != currentUserId)) return
        val uidMap = snap?.playerUids ?: mapOf("p1" to currentUserId)
        viewModelScope.launch {
            repository.updateHostLobby(code, coopState, uidMap, templates).onFailure { err ->
                _errorMessage.value = err.message ?: "Failed to sync room changes."
            }
        }
    }

    fun publishBattleState(state: BattleUiState, templates: Map<String, UnitTemplate>) {
        val code = currentRoomCode ?: return
        val snap = _activeRoom.value
        if (snap != null && (snap.hostUid != currentUserId || snap.status == "FINISHED")) return
        val nextStatus = if (state.summary != null || !state.active) "FINISHED" else "ACTIVE"
        viewModelScope.launch {
            repository.publishBattleSnapshot(code, state, templates, nextStatus).onFailure { err ->
                Log.w(TAG, "Failed to sync battle state to cloud", err)
            }
        }
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
            repository.submitClientAction(
                roomCode = code,
                hostUid = snap.hostUid,
                playerId = currentPlayerSlotId,
                actionType = actionType,
                wx = wx,
                wy = wy,
                cm = cm,
                mode = mode,
                left = left
            ).onFailure { err ->
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
        stopRoomListeners()
        if (!code.isNullOrBlank() && !alreadyFinished) {
            viewModelScope.launch {
                if (isHost && finalInactiveBattle != null) {
                    repository.publishBattleSnapshot(code, finalInactiveBattle, templates, "FINISHED")
                } else {
                    repository.leaveOnlineRoom(code)
                }
            }
        }
    }

    fun applyRemoteBattleSnapshot(
        battleMap: Map<String, Any?>,
        current: BattleUiState
    ): BattleUiState? = repository.applyBattleMapToState(battleMap, current)

    fun stopRoomListeners() {
        roomObserverJob?.cancel()
        roomObserverJob = null
        actionsObserverJob?.cancel()
        actionsObserverJob = null
        currentRoomCode = null
        _activeRoom.value = null
        processedActionIds.clear()
    }

    override fun onCleared() {
        super.onCleared()
        stopRoomListeners()
    }
}
