package com.example.ui.menu

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.OnlineRoomSummary
import com.example.model.CoopNetworkRole
import com.example.model.CoopPlayer
import com.example.model.CoopRosterUnit
import com.example.model.CoopSlotMode
import com.example.model.CoopState
import com.example.model.Faction
import com.example.model.UnitTemplate
import com.example.ui.setup.StatEditDialog
import com.example.ui.theme.BorderSteel
import com.example.ui.theme.CardSlate
import com.example.ui.theme.CrimsonDeep
import com.example.ui.theme.CrimsonSoft
import com.example.ui.theme.CrimsonSurface
import com.example.ui.theme.DividerIron
import com.example.ui.theme.DriftingEmbersBackdrop
import com.example.ui.theme.EmeraldBorder
import com.example.ui.theme.EmeraldBuff
import com.example.ui.theme.EmeraldSurface
import com.example.ui.theme.GrimdarkBevelPanel
import com.example.ui.theme.ImperialGold
import com.example.ui.theme.InputDark
import com.example.ui.theme.MarineBright
import com.example.ui.theme.MarineSurface
import com.example.ui.theme.MarineText
import com.example.ui.theme.PanelCharcoal
import com.example.ui.theme.PanelIronBottom
import com.example.ui.theme.PanelIronTop
import com.example.ui.theme.RowSlate
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.VoidObsidian
import com.example.ui.theme.WarmAmber
import com.example.ui.theme.XenosSurface
import com.example.ui.theme.XenosText
import com.example.ui.theme.XenosViolet
import com.example.ui.theme.grimdarkBevelShape
import com.example.viewmodel.UiState
import kotlinx.coroutines.delay

@Composable
fun CoopLobbyScreen(
    state: CoopState,
    templates: Map<String, UnitTemplate>,
    allTemplates: Map<String, UnitTemplate> = templates,
    editingTemplateId: String? = null,
    statStatusMessage: String? = null,
    myRoomsState: UiState<List<OnlineRoomSummary>>,
    isBusy: Boolean,
    signedInName: String,
    onlineErrorMessage: String? = null,
    createErrorMessage: String? = null,
    joinErrorMessage: String? = null,
    onBackToMenu: () -> Unit,
    onSignOut: () -> Unit,
    onLeaveRoom: () -> Unit,
    onUpdateHostName: (String) -> Unit,
    onUpdateJoinName: (String) -> Unit,
    onUpdateRoomCodeInput: (String) -> Unit,
    onCreateOnlineRoom: () -> Unit,
    onJoinByRoomCode: () -> Unit,
    onRejoinRoom: (OnlineRoomSummary) -> Unit,
    onUpdatePlayerFaction: (String, Faction) -> Unit,
    onCycleSlotMode: (String) -> Unit,
    onSelectConfigPlayer: (String) -> Unit,
    onSetPointLimit: (Int) -> Unit = {},
    onAddUnitForPlayer: (String, String) -> Unit,
    onRemoveCoopUnit: (String) -> Unit,
    onToggleAiOpposition: () -> Unit,
    onSelectLocalClientSlot: (String) -> Unit,
    onOpenStatEditor: (String) -> Unit = {},
    onCloseStatEditor: () -> Unit = {},
    onSaveEditedTemplate: (UnitTemplate) -> Unit = {},
    onSaveSetup: () -> Unit = {},
    onResetSetup: () -> Unit = {},
    onStartCoopMatch: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF171D28), VoidObsidian),
                    radius = 1400f
                )
            )
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_setup_banner),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = 0.14f,
            modifier = Modifier.matchParentSize()
        )
        DriftingEmbersBackdrop(modifier = Modifier.fillMaxSize())

        if (!state.inRoom) {
            BackHandler { onBackToMenu() }
            CoopRoomGateway(
                state = state,
                myRoomsState = myRoomsState,
                isBusy = isBusy,
                signedInName = signedInName,
                errorMessage = onlineErrorMessage,
                createErrorMessage = createErrorMessage,
                joinErrorMessage = joinErrorMessage,
                onBackToMenu = onBackToMenu,
                onSignOut = onSignOut,
                onUpdateHostName = onUpdateHostName,
                onUpdateJoinName = onUpdateJoinName,
                onUpdateRoomCodeInput = onUpdateRoomCodeInput,
                onSelectClientSlot = onSelectLocalClientSlot,
                onCreateOnlineRoom = onCreateOnlineRoom,
                onJoinByRoomCode = onJoinByRoomCode,
                onRejoinRoom = onRejoinRoom
            )
            return
        }

        BackHandler { onLeaveRoom() }

        var showInviteModal by remember { mutableStateOf(false) }
        var showStatsManager by remember { mutableStateOf(false) }
        var copiedRecently by remember { mutableStateOf(false) }
        val clipboard = LocalClipboardManager.current

        LaunchedEffect(copiedRecently) {
            if (copiedRecently) {
                delay(1800)
                copiedRecently = false
            }
        }

        val isClient = state.networkRole == CoopNetworkRole.CLIENT
        val activePlayers = state.players.filter { it.isActive }
        val selectedPlayer = state.players.firstOrNull { it.id == state.selectedConfigPlayerId } ?: state.players.first()
        val assignedForSelected = state.assignedUnits.filter { it.ownerPlayerId == selectedPlayer.id }
        val selectedUsedPts = assignedForSelected.sumOf { templates[it.templateId]?.points ?: 0 }
        val availableTemplatesForSelected = templates.values.filter { it.faction == selectedPlayer.faction }
        val sameFactionCheck = activePlayers.isNotEmpty() && activePlayers.map { it.faction }.distinct().size <= 1
        val overLimitPlayer = activePlayers.firstOrNull { p ->
            val pPts = state.assignedUnits
                .filter { it.ownerPlayerId == p.id }
                .sumOf { templates[it.templateId]?.points ?: 0 }
            pPts > state.pointLimit
        }
        val canStart = !isClient && overLimitPlayer == null && activePlayers.isNotEmpty() && activePlayers.any { p ->
            state.assignedUnits.any { it.ownerPlayerId == p.id }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .displayCutoutPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Dow3LobbyHeader(
                state = state,
                isClient = isClient,
                canStart = canStart,
                copiedRecently = copiedRecently,
                onLeaveRoom = onLeaveRoom,
                onCopyRoomCode = {
                    clipboard.setText(AnnotatedString(state.roomCode))
                    copiedRecently = true
                },
                onOpenRoomModal = { showInviteModal = true },
                onOpenStatsManager = { showStatsManager = true },
                onSaveStats = onSaveSetup,
                onStartCoopMatch = onStartCoopMatch
            )

            CoopPointsControlBar(
                pointLimit = state.pointLimit,
                selectedPlayer = selectedPlayer,
                selectedUsedPts = selectedUsedPts,
                isClient = isClient,
                onSetPointLimit = onSetPointLimit
            )

            val budgetWarning = state.lobbyError ?: overLimitPlayer?.let {
                "${it.name} exceeds ${state.pointLimit} PT limit — remove squads or raise Point Limit"
            }
            if (!budgetWarning.isNullOrBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CutCornerShape(4.dp))
                        .background(CrimsonSurface.copy(alpha = 0.85f))
                        .border(1.dp, CrimsonSoft, CutCornerShape(4.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = budgetWarning,
                        style = MaterialTheme.typography.labelSmall,
                        color = CrimsonSoft,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else if (!isClient && !statStatusMessage.isNullOrBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CutCornerShape(4.dp))
                        .background(EmeraldSurface.copy(alpha = 0.85f))
                        .border(1.dp, EmeraldBorder, CutCornerShape(4.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldBuff, modifier = Modifier.size(14.dp))
                    Text(
                        text = statStatusMessage,
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldBuff,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                val isWide = maxWidth >= 620.dp
                if (isWide) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PlayerSlotsPane(
                            state = state,
                            templates = templates,
                            isReadOnly = isClient,
                            sameFactionCheck = sameFactionCheck,
                            onSelectPlayer = onSelectConfigPlayer,
                            onToggleFaction = { pid, next -> if (!isClient) onUpdatePlayerFaction(pid, next) },
                            onCycleSlotMode = { pid -> if (!isClient) onCycleSlotMode(pid) },
                            onToggleAiOpposition = { if (!isClient) onToggleAiOpposition() },
                            modifier = Modifier
                                .weight(0.52f)
                                .fillMaxHeight()
                        )

                        CommanderLoadoutPane(
                            player = selectedPlayer,
                            pointLimit = state.pointLimit,
                            isReadOnly = isClient,
                            assignedUnits = assignedForSelected,
                            availableTemplates = availableTemplatesForSelected,
                            templates = templates,
                            onAddUnit = { tplId -> if (!isClient) onAddUnitForPlayer(selectedPlayer.id, tplId) },
                            onRemoveUnit = { entryId -> if (!isClient) onRemoveCoopUnit(entryId) },
                            onEditTemplate = { tplId -> if (!isClient) onOpenStatEditor(tplId) },
                            modifier = Modifier
                                .weight(0.48f)
                                .fillMaxHeight()
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PlayerSlotsPane(
                            state = state,
                            templates = templates,
                            isReadOnly = isClient,
                            sameFactionCheck = sameFactionCheck,
                            onSelectPlayer = onSelectConfigPlayer,
                            onToggleFaction = { pid, next -> if (!isClient) onUpdatePlayerFaction(pid, next) },
                            onCycleSlotMode = { pid -> if (!isClient) onCycleSlotMode(pid) },
                            onToggleAiOpposition = { if (!isClient) onToggleAiOpposition() },
                            modifier = Modifier
                                .weight(0.48f)
                                .fillMaxWidth()
                        )
                        CommanderLoadoutPane(
                            player = selectedPlayer,
                            pointLimit = state.pointLimit,
                            isReadOnly = isClient,
                            assignedUnits = assignedForSelected,
                            availableTemplates = availableTemplatesForSelected,
                            templates = templates,
                            onAddUnit = { tplId -> if (!isClient) onAddUnitForPlayer(selectedPlayer.id, tplId) },
                            onRemoveUnit = { entryId -> if (!isClient) onRemoveCoopUnit(entryId) },
                            onEditTemplate = { tplId -> if (!isClient) onOpenStatEditor(tplId) },
                            modifier = Modifier
                                .weight(0.52f)
                                .fillMaxWidth()
                        )
                    }
                }
            }
        }

        if (showInviteModal) {
            RoomInviteModal(
                state = state,
                copiedRecently = copiedRecently,
                onCopyCode = {
                    clipboard.setText(AnnotatedString(state.roomCode))
                    copiedRecently = true
                },
                onLeaveRoom = {
                    showInviteModal = false
                    onLeaveRoom()
                },
                onDismiss = { showInviteModal = false }
            )
        }

        if (showStatsManager && !isClient) {
            AllFactionStatsDialog(
                templates = allTemplates,
                statusMessage = statStatusMessage,
                onEditTemplate = onOpenStatEditor,
                onSaveForFuture = onSaveSetup,
                onResetDefaults = onResetSetup,
                onDismiss = { showStatsManager = false }
            )
        }

        if (!isClient) {
            editingTemplateId?.let { editId ->
                allTemplates[editId]?.let { tpl ->
                    StatEditDialog(
                        template = tpl,
                        onDismiss = onCloseStatEditor,
                        onSave = onSaveEditedTemplate
                    )
                }
            }
        }
    }
}

@Composable
private fun CoopPointsControlBar(
    pointLimit: Int,
    selectedPlayer: CoopPlayer,
    selectedUsedPts: Int,
    isClient: Boolean,
    onSetPointLimit: (Int) -> Unit
) {
    val overBudget = selectedUsedPts > pointLimit
    val remaining = (pointLimit - selectedUsedPts).coerceAtLeast(0)
    val fillRatio = (selectedUsedPts.toFloat() / pointLimit.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val meterColor = when {
        overBudget -> CrimsonSoft
        fillRatio >= 0.85f -> WarmAmber
        else -> EmeraldBuff
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CutCornerShape(6.dp))
            .background(Brush.horizontalGradient(listOf(PanelIronTop, VoidObsidian)))
            .border(1.dp, if (overBudget) CrimsonSoft else ImperialGold.copy(alpha = 0.65f), CutCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Left: Host Point Limit Control (or Synced Limit pill for Client)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = "POINT LIMIT:",
                style = MaterialTheme.typography.labelSmall,
                color = ImperialGold,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
            if (!isClient) {
                Box(
                    modifier = Modifier
                        .height(28.dp)
                        .clip(CutCornerShape(4.dp))
                        .background(InputDark)
                        .border(1.dp, if (pointLimit > 500) BorderSteel else DividerIron, CutCornerShape(4.dp))
                        .clickable(enabled = pointLimit > 500) { onSetPointLimit((pointLimit - 100).coerceAtLeast(500)) }
                        .padding(horizontal = 8.dp)
                        .testTag("coop_point_minus"),
                    contentAlignment = Alignment.Center
                ) {
                    Text("-100", style = MaterialTheme.typography.labelSmall, color = if (pointLimit > 500) TextPrimary else TextMuted, fontWeight = FontWeight.Bold)
                }
            }
            Box(
                modifier = Modifier
                    .height(28.dp)
                    .clip(CutCornerShape(4.dp))
                    .background(InputDark)
                    .border(1.dp, ImperialGold, CutCornerShape(4.dp))
                    .padding(horizontal = 8.dp)
                    .testTag("coop_point_limit_value"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$pointLimit PT",
                    style = MaterialTheme.typography.labelSmall,
                    color = ImperialGold,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
            if (!isClient) {
                Box(
                    modifier = Modifier
                        .height(28.dp)
                        .clip(CutCornerShape(4.dp))
                        .background(InputDark)
                        .border(1.dp, if (pointLimit < 5000) BorderSteel else DividerIron, CutCornerShape(4.dp))
                        .clickable(enabled = pointLimit < 5000) { onSetPointLimit((pointLimit + 100).coerceAtMost(5000)) }
                        .padding(horizontal = 8.dp)
                        .testTag("coop_point_plus"),
                    contentAlignment = Alignment.Center
                ) {
                    Text("+100", style = MaterialTheme.typography.labelSmall, color = if (pointLimit < 5000) TextPrimary else TextMuted, fontWeight = FontWeight.Bold)
                }
                val presets = listOf(500, 1000, 1500, 2000, 3000)
                presets.forEach { preset ->
                    val active = pointLimit == preset
                    val label = if (preset >= 1000 && preset % 1000 == 0) "${preset / 1000}K" else "$preset"
                    Box(
                        modifier = Modifier
                            .height(26.dp)
                            .clip(CutCornerShape(4.dp))
                            .background(if (active) ImperialGold.copy(alpha = 0.22f) else InputDark)
                            .border(1.dp, if (active) ImperialGold else DividerIron, CutCornerShape(4.dp))
                            .clickable { onSetPointLimit(preset) }
                            .padding(horizontal = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (active) ImperialGold else TextMuted,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        // Right: Active Commander's Current Points Progress Bar
        Column(
            modifier = Modifier
                .weight(1f)
                .testTag("coop_points_progress_bar"),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${selectedPlayer.name.uppercase()}: $selectedUsedPts / $pointLimit PT",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (overBudget) CrimsonSoft else TextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (overBudget) "OVER ${selectedUsedPts - pointLimit} PT" else "$remaining PT LEFT",
                    style = MaterialTheme.typography.labelSmall,
                    color = meterColor,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CutCornerShape(2.dp))
                    .background(InputDark)
                    .border(0.8.dp, BorderSteel, CutCornerShape(2.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fillRatio)
                        .background(meterColor)
                )
            }
        }
    }
}

@Composable
private fun Dow3LobbyHeader(
    state: CoopState,
    isClient: Boolean,
    canStart: Boolean,
    copiedRecently: Boolean,
    onLeaveRoom: () -> Unit,
    onCopyRoomCode: () -> Unit,
    onOpenRoomModal: () -> Unit,
    onOpenStatsManager: () -> Unit,
    onSaveStats: () -> Unit,
    onStartCoopMatch: () -> Unit
) {
    GrimdarkBevelPanel(
        cut = 7.dp,
        borderColor = ImperialGold.copy(alpha = 0.7f),
        topColor = PanelIronTop,
        bottomColor = VoidObsidian,
        accentBarColor = ImperialGold,
        showRivets = false,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onLeaveRoom,
                shape = grimdarkBevelShape(4.dp),
                border = BorderStroke(1.dp, BorderSteel),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = InputDark,
                    contentColor = TextPrimary
                ),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier
                    .height(38.dp)
                    .testTag("coop_leave_room")
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Leave Room", modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Text("LEAVE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "CO-OP LOBBY",
                        style = MaterialTheme.typography.titleMedium,
                        color = ImperialGold,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    // Prominent Copyable Room Code Chip
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(CutCornerShape(4.dp))
                            .background(InputDark)
                            .border(1.dp, if (copiedRecently) EmeraldBorder else TacticalCyan.copy(alpha = 0.8f), CutCornerShape(4.dp))
                            .clickable(onClick = onCopyRoomCode)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                            .testTag("coop_room_code_badge")
                    ) {
                        Text(
                            text = if (copiedRecently) "COPIED #${state.roomCode}" else "ROOM #${state.roomCode}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (copiedRecently) EmeraldBuff else TacticalCyan,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                        Icon(
                            imageVector = if (copiedRecently) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = "Copy Room Code",
                            tint = if (copiedRecently) EmeraldBuff else TacticalCyan,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    if (isClient && state.remoteTemplates != null) {
                        SlotStatusPill(text = "HOST STATS", bg = TacticalCyan, fg = VoidObsidian)
                    }
                }
                Text(
                    text = if (isClient) {
                        "Joined as ${state.localPlayerId.uppercase()} • ${state.networkStatus}"
                    } else {
                        "Share #${state.roomCode} • Tap UNIT STATS to edit or SAVE STAT to keep"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isClient) {
                    OutlinedButton(
                        onClick = onOpenStatsManager,
                        shape = grimdarkBevelShape(4.dp),
                        border = BorderStroke(1.dp, ImperialGold.copy(alpha = 0.85f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = PanelIronBottom,
                            contentColor = ImperialGold
                        ),
                        contentPadding = PaddingValues(horizontal = 9.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("coop_unit_stats_button")
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = "Edit All Unit Stats", modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "UNIT STATS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    OutlinedButton(
                        onClick = onSaveStats,
                        shape = grimdarkBevelShape(4.dp),
                        border = BorderStroke(1.dp, EmeraldBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = EmeraldSurface,
                            contentColor = EmeraldBuff
                        ),
                        contentPadding = PaddingValues(horizontal = 9.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("coop_header_save_stat_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Save Stat", modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "SAVE STAT",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // Room Status & Invite Modal Button
                val netActive = state.serverRunning || state.clientConnected
                OutlinedButton(
                    onClick = onOpenRoomModal,
                    shape = grimdarkBevelShape(4.dp),
                    border = BorderStroke(1.dp, if (netActive) EmeraldBorder else TacticalCyan.copy(alpha = 0.7f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (netActive) EmeraldSurface else InputDark,
                        contentColor = if (netActive) EmeraldBuff else TacticalCyan
                    ),
                    contentPadding = PaddingValues(horizontal = 9.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(38.dp)
                        .testTag("coop_room_info_button")
                ) {
                    Icon(Icons.Default.Hub, contentDescription = "Room Info", modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (state.serverRunning) "ROOM" else "CLOUD",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                if (isClient) {
                    Box(
                        modifier = Modifier
                            .height(38.dp)
                            .clip(CutCornerShape(5.dp))
                            .background(PanelIronTop)
                            .border(1.dp, TacticalCyan.copy(alpha = 0.6f), CutCornerShape(5.dp))
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "WAIT HOST",
                            style = MaterialTheme.typography.labelSmall,
                            color = TacticalCyan,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                } else {
                    Button(
                        onClick = onStartCoopMatch,
                        enabled = canStart,
                        shape = grimdarkBevelShape(5.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldSurface,
                            contentColor = EmeraldBuff
                        ),
                        border = BorderStroke(1.5.dp, if (canStart) EmeraldBorder else BorderSteel),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("start_coop_match_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("START", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerSlotsPane(
    state: CoopState,
    templates: Map<String, UnitTemplate>,
    isReadOnly: Boolean,
    sameFactionCheck: Boolean,
    onSelectPlayer: (String) -> Unit,
    onToggleFaction: (String, Faction) -> Unit,
    onCycleSlotMode: (String) -> Unit,
    onToggleAiOpposition: () -> Unit,
    modifier: Modifier = Modifier
) {
    GrimdarkBevelPanel(
        cut = 8.dp,
        borderColor = BorderSteel,
        topColor = CardSlate,
        bottomColor = VoidObsidian,
        accentBarColor = TacticalCyan,
        showRivets = false,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "COMMANDER SLOTS (TURN ORDER 1–4)",
                    style = MaterialTheme.typography.labelMedium,
                    color = TacticalCyan,
                    fontWeight = FontWeight.Bold
                )
                val activeCount = state.players.count { it.isActive }
                Text(
                    text = "$activeCount / 4 READY",
                    style = MaterialTheme.typography.labelSmall,
                    color = ImperialGold
                )
            }

            HorizontalDivider(color = DividerIron)

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                items(state.players.size) { idx ->
                    val player = state.players[idx]
                    val playerUnits = state.assignedUnits.filter { it.ownerPlayerId == player.id }
                    Dow3PlayerSlotCard(
                        turnIndex = idx + 1,
                        player = player,
                        pointLimit = state.pointLimit,
                        isYou = state.localPlayerId == player.id,
                        isReadOnly = isReadOnly,
                        assignedUnits = playerUnits,
                        templates = templates,
                        isSelected = state.selectedConfigPlayerId == player.id,
                        onSelect = { onSelectPlayer(player.id) },
                        onToggleFaction = {
                            val next = if (player.faction == Faction.MARINES) Faction.XENOS else Faction.MARINES
                            onToggleFaction(player.id, next)
                        },
                        onCycleMode = { onCycleSlotMode(player.id) }
                    )
                }

                item {
                    val activePlayers = state.players.filter { it.isActive }
                    val playerFaction = activePlayers.firstOrNull()?.faction ?: Faction.MARINES
                    val aiFaction = if (playerFaction == Faction.MARINES) Faction.XENOS else Faction.MARINES
                    if (sameFactionCheck) {
                        val aiCrestRes = if (aiFaction == Faction.MARINES) R.drawable.img_marine_crest else R.drawable.img_xenos_crest
                        val aiAccent = if (aiFaction == Faction.MARINES) MarineBright else XenosViolet
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(CutCornerShape(6.dp))
                                .background(
                                    if (state.spawnAiEnemyIfSameFaction) CrimsonSurface.copy(alpha = 0.55f) else InputDark
                                )
                                .border(
                                    1.dp,
                                    if (state.spawnAiEnemyIfSameFaction) CrimsonSoft.copy(alpha = 0.8f) else BorderSteel,
                                    CutCornerShape(6.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Image(
                                painter = painterResource(id = aiCrestRes),
                                contentDescription = "AI Faction Crest",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CutCornerShape(4.dp))
                                    .border(1.dp, aiAccent, CutCornerShape(4.dp))
                                    .alpha(if (state.spawnAiEnemyIfSameFaction) 1f else 0.4f)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.SmartToy, contentDescription = null, tint = CrimsonSoft, modifier = Modifier.size(14.dp))
                                    Text(
                                        text = "AI OPPOSITION WAVE (${aiFaction.displayName.uppercase()})",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = if (state.spawnAiEnemyIfSameFaction) {
                                        "Auto-spawns 6 enemy squads for same-faction Co-op"
                                    } else {
                                        "AI Opposition disabled"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }
                            Switch(
                                checked = state.spawnAiEnemyIfSameFaction,
                                enabled = !isReadOnly,
                                onCheckedChange = { onToggleAiOpposition() },
                                colors = SwitchDefaults.colors(checkedThumbColor = EmeraldBuff)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(CutCornerShape(5.dp))
                                .background(InputDark)
                                .border(1.dp, BorderSteel, CutCornerShape(5.dp))
                                .padding(horizontal = 10.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = "TEAM VS TEAM ACTIVE: Marines commanders face Tyranids commanders directly.",
                                style = MaterialTheme.typography.labelSmall,
                                color = WarmAmber
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Dow3PlayerSlotCard(
    turnIndex: Int,
    player: CoopPlayer,
    pointLimit: Int,
    isYou: Boolean,
    isReadOnly: Boolean,
    assignedUnits: List<CoopRosterUnit>,
    templates: Map<String, UnitTemplate>,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onToggleFaction: () -> Unit,
    onCycleMode: () -> Unit
) {
    val isMarines = player.faction == Faction.MARINES
    val factionColor = if (isMarines) MarineBright else XenosViolet
    val factionBg = if (isMarines) MarineSurface else XenosSurface
    val factionText = if (isMarines) MarineText else XenosText
    val crestRes = if (isMarines) R.drawable.img_marine_crest else R.drawable.img_xenos_crest
    val totalPts = assignedUnits.sumOf { templates[it.templateId]?.points ?: 0 }
    val overLimit = totalPts > pointLimit
    val ptRatio = (totalPts.toFloat() / pointLimit.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val ptBarColor = when {
        overLimit -> CrimsonSoft
        ptRatio >= 0.85f -> WarmAmber
        else -> factionColor
    }

    val cardBorder = when {
        overLimit && player.isActive -> CrimsonSoft
        isSelected -> ImperialGold
        !player.isActive -> DividerIron
        else -> factionColor.copy(alpha = 0.65f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CutCornerShape(6.dp))
            .background(
                Brush.horizontalGradient(
                    colors = if (!player.isActive) {
                        listOf(InputDark, VoidObsidian)
                    } else if (isSelected) {
                        listOf(factionBg.copy(alpha = 0.85f), PanelIronTop)
                    } else {
                        listOf(RowSlate, PanelIronBottom)
                    }
                )
            )
            .border(if (isSelected) 1.8.dp else 1.dp, cardBorder, CutCornerShape(6.dp))
            .clickable(onClick = onSelect)
            .padding(horizontal = 9.dp, vertical = 8.dp)
            .alpha(if (player.isActive) 1f else 0.55f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Image(
                painter = painterResource(id = crestRes),
                contentDescription = player.faction.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CutCornerShape(5.dp))
                    .border(1.2.dp, factionColor, CutCornerShape(5.dp))
            )
            Box(
                modifier = Modifier
                    .background(VoidObsidian.copy(alpha = 0.9f), CutCornerShape(2.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text("P$turnIndex", style = MaterialTheme.typography.labelSmall, color = ImperialGold, fontWeight = FontWeight.Bold)
            }
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = player.name.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) ImperialGold else TextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (player.isHost) {
                    SlotStatusPill(text = "HOST", bg = ImperialGold, fg = VoidObsidian)
                } else if (isYou) {
                    SlotStatusPill(text = "YOU", bg = TacticalCyan, fg = VoidObsidian)
                } else if (player.connectedClient && player.slotMode == CoopSlotMode.LAN_CLIENT) {
                    SlotStatusPill(text = "JOINED", bg = EmeraldBuff, fg = VoidObsidian)
                }
            }

            val unitNames = assignedUnits.mapNotNull { templates[it.templateId]?.name }
            val summaryText = when {
                !player.isActive -> "Slot Closed"
                unitNames.isEmpty() -> "No squads assigned (Tap to add)"
                else -> "${unitNames.size} Squads: ${unitNames.joinToString(", ")}"
            }
            Text(
                text = summaryText,
                style = MaterialTheme.typography.labelSmall,
                color = if (unitNames.isEmpty() && player.isActive) WarmAmber else TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (player.isActive) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(CutCornerShape(2.dp))
                            .background(InputDark)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(ptRatio)
                                .background(ptBarColor)
                        )
                    }
                    Text(
                        text = "$totalPts/$pointLimit PT",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (overLimit) CrimsonSoft else ptBarColor,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .height(28.dp)
                    .clip(CutCornerShape(4.dp))
                    .background(factionBg)
                    .border(1.dp, factionColor, CutCornerShape(4.dp))
                    .clickable(enabled = !isReadOnly, onClick = onToggleFaction)
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isMarines) "MARINES" else "TYRANIDS",
                    style = MaterialTheme.typography.labelSmall,
                    color = factionText,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }

            val modeColor = when (player.slotMode) {
                CoopSlotMode.HOST -> ImperialGold
                CoopSlotMode.LOCAL_PLAYER -> TacticalCyan
                CoopSlotMode.LAN_CLIENT -> EmeraldBuff
                CoopSlotMode.OFF -> TextMuted
            }
            Box(
                modifier = Modifier
                    .height(26.dp)
                    .clip(CutCornerShape(4.dp))
                    .background(InputDark)
                    .border(1.dp, modeColor.copy(alpha = 0.7f), CutCornerShape(4.dp))
                    .clickable(enabled = !isReadOnly && player.id != "p1", onClick = onCycleMode)
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = player.slotMode.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = modeColor,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
private fun SlotStatusPill(text: String, bg: Color, fg: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = fg,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(CutCornerShape(3.dp))
            .background(bg)
            .padding(horizontal = 4.dp, vertical = 1.dp)
    )
}

@Composable
private fun CommanderLoadoutPane(
    player: CoopPlayer,
    pointLimit: Int,
    isReadOnly: Boolean,
    assignedUnits: List<CoopRosterUnit>,
    availableTemplates: List<UnitTemplate>,
    templates: Map<String, UnitTemplate>,
    onAddUnit: (String) -> Unit,
    onRemoveUnit: (String) -> Unit,
    onEditTemplate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isMarines = player.faction == Faction.MARINES
    val accent = if (isMarines) MarineBright else XenosViolet
    val crestRes = if (isMarines) R.drawable.img_marine_crest else R.drawable.img_xenos_crest
    val totalPts = assignedUnits.sumOf { templates[it.templateId]?.points ?: 0 }
    val overLimit = totalPts > pointLimit
    val remainingPts = (pointLimit - totalPts).coerceAtLeast(0)
    val budgetRatio = (totalPts.toFloat() / pointLimit.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val budgetColor = when {
        overLimit -> CrimsonSoft
        budgetRatio >= 0.85f -> WarmAmber
        else -> EmeraldBuff
    }

    GrimdarkBevelPanel(
        cut = 8.dp,
        borderColor = accent,
        topColor = PanelCharcoal,
        bottomColor = VoidObsidian,
        accentBarColor = accent,
        showRivets = false,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Image(
                    painter = painterResource(id = crestRes),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CutCornerShape(4.dp))
                        .border(1.dp, accent, CutCornerShape(4.dp))
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${player.name.uppercase()} • LOADOUT",
                        style = MaterialTheme.typography.labelMedium,
                        color = ImperialGold,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${player.faction.displayName} • ${assignedUnits.size} Squads${if (isReadOnly) " (Host Controlled)" else ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(CutCornerShape(4.dp))
                        .background(InputDark)
                        .border(1.dp, if (overLimit) CrimsonSoft else accent, CutCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$totalPts / $pointLimit PT",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (overLimit) CrimsonSoft else accent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Visible Commander Point Budget Progress Strip
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ARMY BUDGET: $totalPts / $pointLimit PT",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (overLimit) "EXCEEDS LIMIT BY ${totalPts - pointLimit} PT" else "$remainingPts PT AVAILABLE",
                        style = MaterialTheme.typography.labelSmall,
                        color = budgetColor,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CutCornerShape(2.dp))
                        .background(InputDark)
                        .border(0.8.dp, BorderSteel, CutCornerShape(2.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(budgetRatio)
                            .background(budgetColor)
                    )
                }
            }

            HorizontalDivider(color = DividerIron)

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    Text(
                        text = "DEPLOYED SQUADS (${assignedUnits.size})",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldBuff,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (assignedUnits.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(CutCornerShape(5.dp))
                                .background(InputDark)
                                .border(1.dp, WarmAmber.copy(alpha = 0.6f), CutCornerShape(5.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = if (isReadOnly) "Host has not assigned squads to this slot yet." else "No squads assigned yet. Tap + ADD below.",
                                style = MaterialTheme.typography.bodySmall,
                                color = WarmAmber
                            )
                        }
                    }
                } else {
                    items(assignedUnits, key = { it.entryId }) { entry ->
                        val tpl = templates[entry.templateId]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(CutCornerShape(5.dp))
                                .background(PanelIronTop)
                                .border(1.dp, accent.copy(alpha = 0.6f), CutCornerShape(5.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tpl?.name ?: entry.templateId,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                if (tpl != null) {
                                    Text(
                                        text = "${tpl.mainWeaponName} • ${tpl.squadSize} models • ${tpl.points} pt",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )
                                }
                            }
                            if (!isReadOnly) {
                                if (tpl != null) {
                                    OutlinedButton(
                                        onClick = { onEditTemplate(tpl.id) },
                                        shape = grimdarkBevelShape(4.dp),
                                        border = BorderStroke(1.dp, BorderSteel),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = PanelIronBottom,
                                            contentColor = TacticalCyan
                                        ),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit ${tpl.name}", modifier = Modifier.size(13.dp))
                                    }
                                }
                                OutlinedButton(
                                    onClick = { onRemoveUnit(entry.entryId) },
                                    shape = grimdarkBevelShape(4.dp),
                                    border = BorderStroke(1.dp, CrimsonSoft.copy(alpha = 0.7f)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = CrimsonDeep.copy(alpha = 0.6f),
                                        contentColor = CrimsonSoft
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }

                if (!isReadOnly) {
                    item {
                        Spacer(Modifier.height(4.dp))
                        HorizontalDivider(color = DividerIron)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "REINFORCEMENT POOL (${player.faction.displayName.uppercase()})",
                            style = MaterialTheme.typography.labelSmall,
                            color = TacticalCyan,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(availableTemplates, key = { it.id }) { tpl ->
                        val canAfford = totalPts + tpl.points <= pointLimit
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(CutCornerShape(5.dp))
                                .background(RowSlate)
                                .border(1.dp, if (canAfford) BorderSteel else DividerIron, CutCornerShape(5.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = tpl.name,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (canAfford) TextPrimary else TextMuted,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${tpl.points} PT",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (canAfford) ImperialGold else CrimsonSoft,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "${tpl.mainWeaponName} • M${tpl.moveCm}cm • T${tpl.toughness} • ${tpl.save}+",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            OutlinedButton(
                                onClick = { onEditTemplate(tpl.id) },
                                shape = grimdarkBevelShape(4.dp),
                                border = BorderStroke(1.dp, BorderSteel),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = PanelIronTop,
                                    contentColor = TacticalCyan
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit ${tpl.name}", modifier = Modifier.size(13.dp))
                                Spacer(Modifier.width(3.dp))
                                Text("EDIT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { onAddUnit(tpl.id) },
                                enabled = canAfford,
                                shape = grimdarkBevelShape(4.dp),
                                border = BorderStroke(1.dp, if (canAfford) EmeraldBorder else DividerIron),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (canAfford) EmeraldSurface else InputDark,
                                    contentColor = if (canAfford) EmeraldBuff else TextMuted,
                                    disabledContainerColor = InputDark,
                                    disabledContentColor = TextMuted
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                if (canAfford) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(3.dp))
                                    Text("ADD", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                } else {
                                    Text("LIMIT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoomInviteModal(
    state: CoopState,
    copiedRecently: Boolean,
    onCopyCode: () -> Unit,
    onLeaveRoom: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = grimdarkBevelShape(8.dp),
        containerColor = CardSlate,
        titleContentColor = ImperialGold,
        textContentColor = TextPrimary,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Hub, contentDescription = null, tint = TacticalCyan)
                Text("ONLINE ROOM INVITE", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CutCornerShape(6.dp))
                        .background(InputDark)
                        .border(1.dp, ImperialGold, CutCornerShape(6.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("ROOM CODE", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text(
                            text = state.roomCode,
                            style = MaterialTheme.typography.headlineSmall,
                            color = ImperialGold,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Cloud Multiplayer • Share this code with friends anywhere",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }

                Text(
                    text = "Status: ${state.networkStatus}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (state.serverRunning || state.clientConnected) EmeraldBuff else WarmAmber
                )

                Button(
                    onClick = onCopyCode,
                    shape = grimdarkBevelShape(5.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PanelIronTop, contentColor = TacticalCyan),
                    border = BorderStroke(1.dp, TacticalCyan),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(if (copiedRecently) Icons.Default.Check else Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (copiedRecently) "COPIED ROOM CODE!" else "COPY ROOM CODE", style = MaterialTheme.typography.labelSmall)
                }

                Button(
                    onClick = onLeaveRoom,
                    shape = grimdarkBevelShape(5.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonDeep, contentColor = TextPrimary),
                    border = BorderStroke(1.dp, CrimsonSoft),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("LEAVE ONLINE ROOM", style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = grimdarkBevelShape(4.dp),
                border = BorderStroke(1.dp, BorderSteel)
            ) {
                Text("CLOSE", style = MaterialTheme.typography.labelSmall, color = TextPrimary)
            }
        }
    )
}

@Composable
private fun AllFactionStatsDialog(
    templates: Map<String, UnitTemplate>,
    statusMessage: String?,
    onEditTemplate: (String) -> Unit,
    onSaveForFuture: () -> Unit,
    onResetDefaults: () -> Unit,
    onDismiss: () -> Unit
) {
    var filterFaction by remember { mutableStateOf<Faction?>(null) }
    val filteredUnits = remember(templates, filterFaction) {
        templates.values.filter { filterFaction == null || it.faction == filterFaction }
            .sortedWith(compareBy({ it.faction.ordinal }, { it.points }))
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        GrimdarkBevelPanel(
            cut = 10.dp,
            borderColor = ImperialGold,
            topColor = PanelIronTop,
            bottomColor = VoidObsidian,
            accentBarColor = ImperialGold,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .fillMaxWidth()
                .widthIn(max = 680.dp)
                .fillMaxHeight(0.92f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "HOST UNIT STATS & SAVE STAT",
                            style = MaterialTheme.typography.titleMedium,
                            color = ImperialGold,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Edits apply to all players in this room & save for next game",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = grimdarkBevelShape(4.dp),
                        border = BorderStroke(1.dp, BorderSteel),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("CLOSE", style = MaterialTheme.typography.labelSmall, color = TextPrimary, maxLines = 1, softWrap = false)
                    }
                }

                // Faction Filter Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatsFilterChip("ALL", filterFaction == null, ImperialGold, { filterFaction = null }, Modifier.weight(1f))
                    StatsFilterChip("ULTRAMARINES", filterFaction == Faction.MARINES, MarineBright, { filterFaction = Faction.MARINES }, Modifier.weight(1f))
                    StatsFilterChip("TYRANIDS", filterFaction == Faction.XENOS, XenosViolet, { filterFaction = Faction.XENOS }, Modifier.weight(1f))
                }

                if (!statusMessage.isNullOrBlank()) {
                    Text(
                        text = statusMessage,
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldBuff,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredUnits, key = { it.id }) { tpl ->
                        val facColor = if (tpl.faction == Faction.MARINES) MarineBright else XenosViolet
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(CutCornerShape(5.dp))
                                .background(RowSlate)
                                .border(1.dp, facColor.copy(alpha = 0.55f), CutCornerShape(5.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = tpl.name.uppercase(),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${tpl.points} PT",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ImperialGold
                                    )
                                }
                                Text(
                                    text = "${tpl.faction.displayName} • ${tpl.squadSize}x ${tpl.woundsPerModel}W • M${tpl.moveCm} T${tpl.toughness} SV${tpl.save}+ • S${tpl.strength} AP-${tpl.armorPen} D${tpl.damage}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            OutlinedButton(
                                onClick = { onEditTemplate(tpl.id) },
                                shape = grimdarkBevelShape(4.dp),
                                border = BorderStroke(1.dp, TacticalCyan.copy(alpha = 0.8f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = PanelIronBottom,
                                    contentColor = TacticalCyan
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .height(32.dp)
                                    .testTag("coop_edit_stat_${tpl.id}")
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit ${tpl.name}", modifier = Modifier.size(13.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("EDIT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                            }
                        }
                    }
                }

                HorizontalDivider(color = DividerIron)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onResetDefaults,
                        shape = grimdarkBevelShape(5.dp),
                        border = BorderStroke(1.dp, BorderSteel),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = InputDark,
                            contentColor = CrimsonSoft
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .testTag("coop_stats_reset_button")
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("RESET DEFAULTS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }
                    Button(
                        onClick = onSaveForFuture,
                        shape = grimdarkBevelShape(5.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldSurface,
                            contentColor = EmeraldBuff
                        ),
                        border = BorderStroke(1.2.dp, EmeraldBorder),
                        modifier = Modifier
                            .weight(1.4f)
                            .height(40.dp)
                            .testTag("coop_stats_save_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("SAVE STAT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsFilterChip(
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(30.dp)
            .clip(CutCornerShape(4.dp))
            .background(if (selected) accent.copy(alpha = 0.22f) else InputDark)
            .border(1.dp, if (selected) accent else BorderSteel, CutCornerShape(4.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) accent else TextMuted,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false
        )
    }
}
