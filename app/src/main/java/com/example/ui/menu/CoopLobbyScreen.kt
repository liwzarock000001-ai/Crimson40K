package com.example.ui.menu

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SmartToy
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
import com.example.R
import com.example.data.OnlineRoomSummary
import com.example.model.CoopNetworkRole
import com.example.model.CoopPlayer
import com.example.model.CoopRosterUnit
import com.example.model.CoopSlotMode
import com.example.model.CoopState
import com.example.model.Faction
import com.example.model.UnitTemplate
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
    myRoomsState: UiState<List<OnlineRoomSummary>>,
    isBusy: Boolean,
    signedInName: String,
    onlineErrorMessage: String?,
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
    onAddUnitForPlayer: (String, String) -> Unit,
    onRemoveCoopUnit: (String) -> Unit,
    onToggleAiOpposition: () -> Unit,
    onSelectLocalClientSlot: (String) -> Unit,
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
        val availableTemplatesForSelected = templates.values.filter { it.faction == selectedPlayer.faction }
        val sameFactionCheck = activePlayers.isNotEmpty() && activePlayers.map { it.faction }.distinct().size <= 1
        val canStart = !isClient && activePlayers.isNotEmpty() && activePlayers.any { p ->
            state.assignedUnits.any { it.ownerPlayerId == p.id }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .displayCutoutPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                onStartCoopMatch = onStartCoopMatch
            )

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
                            isReadOnly = isClient,
                            assignedUnits = assignedForSelected,
                            availableTemplates = availableTemplatesForSelected,
                            templates = templates,
                            onAddUnit = { tplId -> if (!isClient) onAddUnitForPlayer(selectedPlayer.id, tplId) },
                            onRemoveUnit = { entryId -> if (!isClient) onRemoveCoopUnit(entryId) },
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
                            isReadOnly = isClient,
                            assignedUnits = assignedForSelected,
                            availableTemplates = availableTemplatesForSelected,
                            templates = templates,
                            onAddUnit = { tplId -> if (!isClient) onAddUnitForPlayer(selectedPlayer.id, tplId) },
                            onRemoveUnit = { entryId -> if (!isClient) onRemoveCoopUnit(entryId) },
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
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier
                    .height(38.dp)
                    .testTag("coop_leave_room")
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Leave Room", modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Text("LEAVE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "CO-OP MATCH LOBBY",
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
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = if (copiedRecently) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = "Copy Room Code",
                            tint = if (copiedRecently) EmeraldBuff else TacticalCyan,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
                Text(
                    text = if (isClient) {
                        "Joined as ${state.localPlayerId.uppercase()} • ${state.networkStatus}"
                    } else {
                        "Share Room #${state.roomCode} • Tap a Commander slot to assign squads"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier
                    .height(40.dp)
                    .testTag("coop_room_info_button")
            ) {
                Icon(Icons.Default.Hub, contentDescription = "Room Info", modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    text = if (state.serverRunning) "CLOUD HOST" else "CLOUD JOINED",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }

            if (isClient) {
                Box(
                    modifier = Modifier
                        .height(40.dp)
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
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(40.dp)
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
                                text = "TEAM VS TEAM ACTIVE: Marines commanders face Xenos commanders directly.",
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

    val cardBorder = when {
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
                else -> "${unitNames.size} Squads (${totalPts}pt): ${unitNames.joinToString(", ")}"
            }
            Text(
                text = summaryText,
                style = MaterialTheme.typography.labelSmall,
                color = if (unitNames.isEmpty() && player.isActive) WarmAmber else TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
                    text = if (isMarines) "MARINES" else "XENOS",
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
    isReadOnly: Boolean,
    assignedUnits: List<CoopRosterUnit>,
    availableTemplates: List<UnitTemplate>,
    templates: Map<String, UnitTemplate>,
    onAddUnit: (String) -> Unit,
    onRemoveUnit: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isMarines = player.faction == Faction.MARINES
    val accent = if (isMarines) MarineBright else XenosViolet
    val crestRes = if (isMarines) R.drawable.img_marine_crest else R.drawable.img_xenos_crest
    val totalPts = assignedUnits.sumOf { templates[it.templateId]?.points ?: 0 }

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
                        .border(1.dp, accent, CutCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("$totalPts PT", style = MaterialTheme.typography.labelMedium, color = accent, fontWeight = FontWeight.Bold)
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
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(CutCornerShape(5.dp))
                                .background(RowSlate)
                                .border(1.dp, BorderSteel, CutCornerShape(5.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = tpl.name,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${tpl.points} PT",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ImperialGold
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
                                onClick = { onAddUnit(tpl.id) },
                                shape = grimdarkBevelShape(4.dp),
                                border = BorderStroke(1.dp, EmeraldBorder),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = EmeraldSurface,
                                    contentColor = EmeraldBuff
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(3.dp))
                                Text("ADD", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
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
