package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.OnlineCoopRepository
import com.example.model.AppScreen
import com.example.model.CoopSlotMode
import com.example.ui.battle.BattleScreen
import com.example.ui.menu.CoopGoogleAuthScreen
import com.example.ui.menu.CoopLobbyScreen
import com.example.ui.menu.LoadGameScreen
import com.example.ui.menu.MainMenuScreen
import com.example.ui.menu.SettingsScreen
import com.example.ui.menu.authStateFlow
import com.example.ui.menu.signOutGoogleAccount
import com.example.ui.setup.SetupScreen
import com.example.ui.theme.CrimsonSectorTheme
import com.example.viewmodel.GameViewModel
import com.example.viewmodel.OnlineCoopViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            CrimsonSectorTheme {
                val vm: GameViewModel = viewModel()
                val setupState by vm.setupState.collectAsStateWithLifecycle()
                val battleState by vm.battleState.collectAsStateWithLifecycle()
                val currentScreen by vm.currentScreen.collectAsStateWithLifecycle()
                val coopState by vm.coopState.collectAsStateWithLifecycle()
                val settingsState by vm.settingsState.collectAsStateWithLifecycle()
                val savedConfig by vm.savedConfig.collectAsStateWithLifecycle()
                val savedSkirmishBattle by vm.savedSkirmishBattle.collectAsStateWithLifecycle()

                val context = LocalContext.current
                val authScope = rememberCoroutineScope()
                val credentialManager = remember(context) { CredentialManager.create(context) }
                val currentUser by Firebase.auth.authStateFlow()
                    .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

                // Apply screen brightness from Settings to window attributes as well as 3D renderer lighting
                LaunchedEffect(settingsState.screenBrightness) {
                    val lp = window.attributes
                    lp.screenBrightness = (settingsState.screenBrightness - 0.35f).coerceIn(0.15f, 1.0f)
                    window.attributes = lp
                }

                val isCoopScreen = currentScreen == AppScreen.COOP_LOBBY || currentScreen == AppScreen.COOP_BATTLE

                if (isCoopScreen) {
                    val user = currentUser
                    if (user == null) {
                        LaunchedEffect(currentScreen) {
                            vm.onlineLobbySyncPublisher = null
                            vm.onlineHostStatePublisher = null
                            vm.onlineClientActionSender = null
                            if (currentScreen == AppScreen.COOP_BATTLE) {
                                vm.setScreen(AppScreen.COOP_LOBBY)
                            }
                        }
                        CoopGoogleAuthScreen(
                            onBackToMenu = {
                                vm.resetOnlineCoopState()
                                vm.setScreen(AppScreen.MAIN_MENU)
                            },
                            onAuthSuccess = {}
                        )
                    } else {
                        key(user.uid) {
                            val onlineDisplayName = (user.displayName ?: user.email?.substringBefore('@') ?: "Commander")
                                .trim()
                                .ifBlank { "Commander" }
                                .take(20)

                            val onlineVm: OnlineCoopViewModel = viewModel(
                                key = "online_${user.uid}",
                                factory = viewModelFactory {
                                    initializer {
                                        OnlineCoopViewModel(
                                            repository = OnlineCoopRepository(context.applicationContext),
                                            currentUserId = user.uid,
                                            displayName = onlineDisplayName
                                        )
                                    }
                                }
                            )

                            val myRoomsState by onlineVm.myRoomsState.collectAsStateWithLifecycle()
                            val activeRoom by onlineVm.activeRoom.collectAsStateWithLifecycle()
                            val isOnlineBusy by onlineVm.isBusy.collectAsStateWithLifecycle()
                            val onlineError by onlineVm.errorMessage.collectAsStateWithLifecycle()

                            LaunchedEffect(user.uid, onlineDisplayName) {
                                if (coopState.hostCommanderName == "Commander 1") {
                                    vm.updateHostCommanderName(onlineDisplayName)
                                }
                                if (coopState.joinCommanderName == "Commander 2") {
                                    vm.updateJoinCommanderName(onlineDisplayName)
                                }
                            }

                            DisposableEffect(onlineVm, vm) {
                                vm.onlineLobbySyncPublisher = { st ->
                                    onlineVm.syncHostLobby(st)
                                }
                                vm.onlineHostStatePublisher = { st, templates ->
                                    onlineVm.publishBattleState(st, templates)
                                }
                                vm.onlineClientActionSender = { actionType, wx, wy, cm, mode, left ->
                                    onlineVm.submitClientAction(actionType, wx, wy, cm, mode, left)
                                }
                                onDispose {
                                    vm.onlineLobbySyncPublisher = null
                                    vm.onlineHostStatePublisher = null
                                    vm.onlineClientActionSender = null
                                }
                            }

                            LaunchedEffect(activeRoom) {
                                val room = activeRoom ?: return@LaunchedEffect
                                vm.applyOnlineRoomSnapshot(
                                    roomCode = room.roomCode,
                                    hostName = room.hostName,
                                    players = room.players,
                                    assignedUnits = room.assignedUnits,
                                    spawnAi = room.spawnAiEnemyIfSameFaction,
                                    status = room.status
                                )
                                if (room.hostUid != user.uid) {
                                    val bMap = room.battleMap
                                    if (bMap != null && (room.status == "ACTIVE" || room.status == "FINISHED")) {
                                        val synced = onlineVm.applyRemoteBattleSnapshot(bMap, vm.battleState.value)
                                        if (synced != null) {
                                            vm.applyOnlineBattleState(synced, room.status)
                                        }
                                    }
                                }
                            }

                            LaunchedEffect(onlineVm) {
                                onlineVm.incomingActions.collect { act ->
                                    vm.executeRemoteCoopAction(
                                        senderPlayerId = act.playerId,
                                        actionType = act.actionType,
                                        wx = act.wx,
                                        wy = act.wy,
                                        cm = act.cm,
                                        mode = act.mode,
                                        left = act.left
                                    )
                                }
                            }

                            Crossfade(targetState = currentScreen, label = "coop_navigation") { coopScreen ->
                                if (coopScreen == AppScreen.COOP_BATTLE) {
                                    BattleScreen(
                                        state = battleState,
                                        onWorldTap = vm::onWorldTapped,
                                        onToggleMoveMode = vm::toggleMoveMode,
                                        onAdvanceMove = vm::advanceMove,
                                        onSetCmStep = vm::setCmStep,
                                        onWalkForwardCm = vm::walkForwardCm,
                                        onConfirmMove = vm::confirmMovePreview,
                                        onCancelMove = vm::cancelMovePreview,
                                        onToggleChargeMode = vm::toggleChargeMode,
                                        onConfirmCharge = vm::confirmChargeRoll,
                                        onCancelCharge = vm::cancelChargePreview,
                                        onConfirmStrike = vm::confirmStrike,
                                        onSelectWeaponMode = vm::selectWeaponMode,
                                        onRotateUnit = vm::rotateSelectedUnit,
                                        onEndTurn = vm::endPlayerTurn,
                                        onToggleAutoFocus = vm::toggleAutoFocus,
                                        onCenterCamera = vm::centerCamera,
                                        onToggleSound = vm::toggleSound,
                                        onToggleUnitCard = vm::toggleUnitCardCollapsed,
                                        onDismissTurnSplash = vm::dismissTurnSplash,
                                        onSaveBattle = vm::saveSkirmishBattleToDatabase,
                                        onExitToSetup = {
                                            val inactiveBattle = battleState.copy(
                                                active = false,
                                                summary = null,
                                                isAiThinking = false,
                                                isAnimating = false
                                            )
                                            onlineVm.leaveActiveRoom(
                                                finalInactiveBattle = inactiveBattle,
                                                templates = setupState.templates
                                            )
                                            vm.returnToSetup()
                                        }
                                    )
                                } else {
                                    CoopLobbyScreen(
                                        state = coopState,
                                        templates = setupState.templates,
                                        myRoomsState = myRoomsState,
                                        isBusy = isOnlineBusy,
                                        signedInName = onlineDisplayName,
                                        onlineErrorMessage = onlineError,
                                        onBackToMenu = {
                                            onlineVm.leaveActiveRoom()
                                            vm.leaveCoopRoom()
                                            vm.setScreen(AppScreen.MAIN_MENU)
                                        },
                                        onSignOut = {
                                            signOutGoogleAccount(
                                                credentialManager = credentialManager,
                                                scope = authScope,
                                                onCleanupBeforeSignOut = {
                                                    onlineVm.leaveActiveRoom()
                                                    onlineVm.stopRoomListeners()
                                                    vm.resetOnlineCoopState()
                                                }
                                            )
                                        },
                                        onLeaveRoom = {
                                            onlineVm.leaveActiveRoom()
                                            vm.leaveCoopRoom()
                                        },
                                        onUpdateHostName = vm::updateHostCommanderName,
                                        onUpdateJoinName = vm::updateJoinCommanderName,
                                        onUpdateRoomCodeInput = { code ->
                                            onlineVm.clearError()
                                            vm.updateJoinRoomCode(code)
                                        },
                                        onCreateOnlineRoom = {
                                            onlineVm.clearError()
                                            val preparedPlayers = coopState.players.map { p ->
                                                when (p.id) {
                                                    "p1" -> p.copy(
                                                        name = coopState.hostCommanderName.trim().ifBlank { onlineDisplayName },
                                                        slotMode = CoopSlotMode.HOST,
                                                        connectedClient = true
                                                    )
                                                    else -> p.copy(
                                                        slotMode = if (p.slotMode == CoopSlotMode.OFF) CoopSlotMode.OFF else CoopSlotMode.LAN_CLIENT,
                                                        connectedClient = false
                                                    )
                                                }
                                            }
                                            onlineVm.createOnlineRoom(
                                                hostName = coopState.hostCommanderName.ifBlank { onlineDisplayName },
                                                initialPlayers = preparedPlayers,
                                                initialUnits = coopState.assignedUnits,
                                                spawnAiIfSameFaction = coopState.spawnAiEnemyIfSameFaction
                                            ) { roomCode, slot, isHost ->
                                                vm.enterOnlineRoom(roomCode, slot, isHost)
                                            }
                                        },
                                        onJoinByRoomCode = {
                                            onlineVm.clearError()
                                            val preferredSlot = if (coopState.localPlayerId == "p1") "p2" else coopState.localPlayerId
                                            onlineVm.joinRoomByCode(
                                                roomCode = coopState.joinRoomCodeInput,
                                                preferredSlotId = preferredSlot,
                                                joinerName = coopState.joinCommanderName.ifBlank { onlineDisplayName }
                                            ) { roomCode, slot, isHost ->
                                                vm.enterOnlineRoom(roomCode, slot, isHost)
                                            }
                                        },
                                        onRejoinRoom = { summary ->
                                            onlineVm.clearError()
                                            onlineVm.rejoinExistingRoom(summary) { roomCode, slot, isHost ->
                                                vm.enterOnlineRoom(roomCode, slot, isHost)
                                            }
                                        },
                                        onUpdatePlayerFaction = vm::updateCoopPlayerFaction,
                                        onCycleSlotMode = vm::cycleCoopPlayerSlotMode,
                                        onSelectConfigPlayer = vm::selectCoopConfigPlayer,
                                        onAddUnitForPlayer = vm::addCoopUnitForPlayer,
                                        onRemoveCoopUnit = vm::removeCoopUnit,
                                        onToggleAiOpposition = vm::toggleCoopAiOpposition,
                                        onSelectLocalClientSlot = vm::selectLocalClientPlayerId,
                                        onStartCoopMatch = vm::startCoopMatch
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Crossfade(targetState = currentScreen, label = "main_navigation") { screen ->
                        when (screen) {
                            AppScreen.MAIN_MENU -> MainMenuScreen(
                                onNavigate = vm::setScreen
                            )
                            AppScreen.LOAD_GAME -> LoadGameScreen(
                                savedBattle = savedSkirmishBattle,
                                savedConfig = savedConfig,
                                onBack = { vm.setScreen(AppScreen.MAIN_MENU) },
                                onLoadBattle = vm::loadSavedSkirmishBattle,
                                onLoadRoster = vm::loadSavedSetupAndOpen
                            )
                            AppScreen.SKIRMISH_SETUP -> SetupScreen(
                                state = setupState,
                                onBackToMenu = { vm.setScreen(AppScreen.MAIN_MENU) },
                                onToggleSide = vm::togglePlayerFaction,
                                onPointLimitChange = vm::setPointLimit,
                                onSave = vm::saveSetupToDatabase,
                                onReset = vm::resetSetupToDefaults,
                                onRandomize = vm::randomizeRoster,
                                onAddUnit = vm::addRosterUnit,
                                onRemoveSlot = vm::removeRosterSlot,
                                onEditTemplate = vm::openStatEditor,
                                onCloseEditor = vm::closeStatEditor,
                                onSaveTemplate = vm::saveEditedTemplate,
                                onDeploy = vm::deployToBattle
                            )
                            AppScreen.SKIRMISH_BATTLE -> BattleScreen(
                                state = battleState,
                                onWorldTap = vm::onWorldTapped,
                                onToggleMoveMode = vm::toggleMoveMode,
                                onAdvanceMove = vm::advanceMove,
                                onSetCmStep = vm::setCmStep,
                                onWalkForwardCm = vm::walkForwardCm,
                                onConfirmMove = vm::confirmMovePreview,
                                onCancelMove = vm::cancelMovePreview,
                                onToggleChargeMode = vm::toggleChargeMode,
                                onConfirmCharge = vm::confirmChargeRoll,
                                onCancelCharge = vm::cancelChargePreview,
                                onConfirmStrike = vm::confirmStrike,
                                onSelectWeaponMode = vm::selectWeaponMode,
                                onRotateUnit = vm::rotateSelectedUnit,
                                onEndTurn = vm::endPlayerTurn,
                                onToggleAutoFocus = vm::toggleAutoFocus,
                                onCenterCamera = vm::centerCamera,
                                onToggleSound = vm::toggleSound,
                                onToggleUnitCard = vm::toggleUnitCardCollapsed,
                                onDismissTurnSplash = vm::dismissTurnSplash,
                                onSaveBattle = vm::saveSkirmishBattleToDatabase,
                                onExitToSetup = vm::returnToSetup
                            )
                            AppScreen.SETTINGS -> SettingsScreen(
                                state = settingsState,
                                onBack = { vm.setScreen(AppScreen.MAIN_MENU) },
                                onUpdateSettings = vm::updateSettings
                            )
                            AppScreen.COOP_LOBBY, AppScreen.COOP_BATTLE -> Unit
                        }
                    }
                }
            }
        }
    }
}
