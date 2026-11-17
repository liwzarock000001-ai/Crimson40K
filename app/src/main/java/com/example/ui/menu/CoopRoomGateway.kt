package com.example.ui.menu

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.OnlineRoomSummary
import com.example.model.CoopState
import com.example.ui.theme.BorderSteel
import com.example.ui.theme.CardSlate
import com.example.ui.theme.CrimsonDeep
import com.example.ui.theme.CrimsonSoft
import com.example.ui.theme.DividerIron
import com.example.ui.theme.EmeraldBorder
import com.example.ui.theme.EmeraldBuff
import com.example.ui.theme.EmeraldSurface
import com.example.ui.theme.GrimdarkBevelPanel
import com.example.ui.theme.ImperialGold
import com.example.ui.theme.InputDark
import com.example.ui.theme.PanelCharcoal
import com.example.ui.theme.PanelIronTop
import com.example.ui.theme.RowSlate
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.VoidObsidian
import com.example.ui.theme.WarmAmber
import com.example.ui.theme.grimdarkBevelShape
import com.example.viewmodel.UiState

@Composable
fun CoopRoomGateway(
    state: CoopState,
    myRoomsState: UiState<List<OnlineRoomSummary>>,
    isBusy: Boolean,
    signedInName: String,
    errorMessage: String?,
    onBackToMenu: () -> Unit,
    onSignOut: () -> Unit,
    onUpdateHostName: (String) -> Unit,
    onUpdateJoinName: (String) -> Unit,
    onUpdateRoomCodeInput: (String) -> Unit,
    onSelectClientSlot: (String) -> Unit,
    onCreateOnlineRoom: () -> Unit,
    onJoinByRoomCode: () -> Unit,
    onRejoinRoom: (OnlineRoomSummary) -> Unit
) {
    val activeError = errorMessage?.takeIf { it.isNotBlank() } ?: state.lobbyError

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .displayCutoutPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Cloud Online Gateway Top Bar
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
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onBackToMenu,
                    shape = grimdarkBevelShape(4.dp),
                    border = BorderStroke(1.dp, BorderSteel),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = InputDark,
                        contentColor = TextPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(40.dp)
                        .testTag("coop_gateway_back_menu")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("MENU", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ONLINE CO-OP ROOMS (4P CLOUD)",
                        style = MaterialTheme.typography.titleMedium,
                        color = ImperialGold,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Play long-distance over the internet using a 6-digit Room Code",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(CutCornerShape(4.dp))
                        .background(InputDark)
                        .border(1.dp, EmeraldBorder.copy(alpha = 0.75f), CutCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Icon(Icons.Default.Public, contentDescription = null, tint = EmeraldBuff, modifier = Modifier.size(14.dp))
                    Text(
                        text = signedInName.ifBlank { "COMMANDER" }.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldBuff,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = onSignOut,
                    shape = grimdarkBevelShape(4.dp),
                    border = BorderStroke(1.dp, BorderSteel),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = InputDark,
                        contentColor = TextPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(40.dp)
                        .testTag("coop_sign_out_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign Out", modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("SIGN OUT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                }
            }
        }

        // Main Two-Pane Gateway: CREATE ONLINE ROOM | JOIN BY ROOM CODE
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val isWide = maxWidth >= 600.dp
            if (isWide) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CreateOnlineRoomPanel(
                        commanderName = state.hostCommanderName,
                        isBusy = isBusy,
                        onUpdateName = onUpdateHostName,
                        onCreateRoom = onCreateOnlineRoom,
                        modifier = Modifier
                            .weight(0.45f)
                            .fillMaxHeight()
                    )

                    JoinOnlineRoomPanel(
                        state = state,
                        myRoomsState = myRoomsState,
                        isBusy = isBusy,
                        activeError = activeError,
                        onUpdateJoinName = onUpdateJoinName,
                        onUpdateRoomCodeInput = onUpdateRoomCodeInput,
                        onSelectClientSlot = onSelectClientSlot,
                        onJoinByRoomCode = onJoinByRoomCode,
                        onRejoinRoom = onRejoinRoom,
                        modifier = Modifier
                            .weight(0.55f)
                            .fillMaxHeight()
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CreateOnlineRoomPanel(
                        commanderName = state.hostCommanderName,
                        isBusy = isBusy,
                        onUpdateName = onUpdateHostName,
                        onCreateRoom = onCreateOnlineRoom,
                        modifier = Modifier.fillMaxWidth()
                    )
                    JoinOnlineRoomPanel(
                        state = state,
                        myRoomsState = myRoomsState,
                        isBusy = isBusy,
                        activeError = activeError,
                        onUpdateJoinName = onUpdateJoinName,
                        onUpdateRoomCodeInput = onUpdateRoomCodeInput,
                        onSelectClientSlot = onSelectClientSlot,
                        onJoinByRoomCode = onJoinByRoomCode,
                        onRejoinRoom = onRejoinRoom,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(380.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CreateOnlineRoomPanel(
    commanderName: String,
    isBusy: Boolean,
    onUpdateName: (String) -> Unit,
    onCreateRoom: () -> Unit,
    modifier: Modifier = Modifier
) {
    GrimdarkBevelPanel(
        cut = 9.dp,
        borderColor = ImperialGold.copy(alpha = 0.85f),
        topColor = CardSlate,
        bottomColor = VoidObsidian,
        accentBarColor = ImperialGold,
        showRivets = false,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_marine_crest),
                    contentDescription = "Host Crest",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CutCornerShape(6.dp))
                        .border(1.2.dp, ImperialGold, CutCornerShape(6.dp))
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "CREATE ONLINE ROOM",
                        style = MaterialTheme.typography.titleMedium,
                        color = ImperialGold,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Host central command & share a 6-digit Room Code worldwide",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }

            HorizontalDivider(color = DividerIron)

            OutlinedTextField(
                value = commanderName,
                onValueChange = onUpdateName,
                label = { Text("Host Commander Name", style = MaterialTheme.typography.labelSmall) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ImperialGold,
                    unfocusedBorderColor = BorderSteel,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = InputDark,
                    unfocusedContainerColor = InputDark
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CutCornerShape(6.dp))
                    .background(InputDark)
                    .border(1.dp, TacticalCyan.copy(alpha = 0.6f), CutCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.CloudQueue, contentDescription = null, tint = TacticalCyan, modifier = Modifier.size(18.dp))
                Text(
                    text = "Friends can join from any network or mobile data using your Room Code — no shared Wi-Fi required.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }

            Spacer(Modifier.weight(1f, fill = true))

            Button(
                onClick = onCreateRoom,
                enabled = !isBusy,
                shape = grimdarkBevelShape(6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldSurface,
                    contentColor = EmeraldBuff
                ),
                border = BorderStroke(1.5.dp, EmeraldBorder),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("coop_create_room_button")
            ) {
                if (isBusy) {
                    CircularProgressIndicator(
                        color = EmeraldBuff,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("CREATING ROOM...", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("CREATE ONLINE ROOM", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun JoinOnlineRoomPanel(
    state: CoopState,
    myRoomsState: UiState<List<OnlineRoomSummary>>,
    isBusy: Boolean,
    activeError: String?,
    onUpdateJoinName: (String) -> Unit,
    onUpdateRoomCodeInput: (String) -> Unit,
    onSelectClientSlot: (String) -> Unit,
    onJoinByRoomCode: () -> Unit,
    onRejoinRoom: (OnlineRoomSummary) -> Unit,
    modifier: Modifier = Modifier
) {
    GrimdarkBevelPanel(
        cut = 9.dp,
        borderColor = TacticalCyan.copy(alpha = 0.8f),
        topColor = PanelCharcoal,
        bottomColor = VoidObsidian,
        accentBarColor = TacticalCyan,
        showRivets = false,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Key, contentDescription = null, tint = TacticalCyan, modifier = Modifier.size(22.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "JOIN BY ROOM CODE",
                        style = MaterialTheme.typography.titleMedium,
                        color = TacticalCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Enter a friend's 6-digit Room Code or rejoin your recent rooms",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }

            HorizontalDivider(color = DividerIron)

            // Room Code Input + Primary JOIN Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = state.joinRoomCodeInput,
                    onValueChange = onUpdateRoomCodeInput,
                    label = { Text("Room Code (เลขห้อง)", style = MaterialTheme.typography.labelSmall) },
                    placeholder = { Text("e.g. 482915", color = TextMuted) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TacticalCyan,
                        unfocusedBorderColor = BorderSteel,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = InputDark,
                        unfocusedContainerColor = InputDark
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("coop_room_code_input")
                )

                Button(
                    onClick = onJoinByRoomCode,
                    enabled = !isBusy,
                    shape = grimdarkBevelShape(6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PanelIronTop,
                        contentColor = TacticalCyan
                    ),
                    border = BorderStroke(1.5.dp, TacticalCyan),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    modifier = Modifier
                        .height(54.dp)
                        .testTag("coop_join_room_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("JOIN", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }

            // Callsign + Player Slot Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = state.joinCommanderName,
                    onValueChange = onUpdateJoinName,
                    label = { Text("Your Callsign", style = MaterialTheme.typography.labelSmall) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TacticalCyan,
                        unfocusedBorderColor = BorderSteel,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = InputDark,
                        unfocusedContainerColor = InputDark
                    ),
                    modifier = Modifier.weight(1f)
                )

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("PREFERRED SLOT", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("p2", "p3", "p4").forEach { pid ->
                            val selected = state.localPlayerId == pid || (state.localPlayerId == "p1" && pid == "p2")
                            Box(
                                modifier = Modifier
                                    .height(34.dp)
                                    .clip(CutCornerShape(4.dp))
                                    .background(if (selected) TacticalCyan.copy(alpha = 0.22f) else InputDark)
                                    .border(1.dp, if (selected) TacticalCyan else BorderSteel, CutCornerShape(4.dp))
                                    .clickable { onSelectClientSlot(pid) }
                                    .padding(horizontal = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = pid.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) TacticalCyan else TextMuted,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Inline Error Banner if Join Failed or Room Not Found
            activeError?.let { err ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CutCornerShape(5.dp))
                        .background(CrimsonDeep.copy(alpha = 0.7f))
                        .border(1.dp, CrimsonSoft, CutCornerShape(5.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = CrimsonSoft, modifier = Modifier.size(16.dp))
                    Text(
                        text = err,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextPrimary
                    )
                }
            }

            val roomCount = (myRoomsState as? UiState.Success)?.data?.size ?: 0
            Text(
                text = "YOUR ONLINE ROOMS ($roomCount)",
                style = MaterialTheme.typography.labelSmall,
                color = ImperialGold,
                fontWeight = FontWeight.Bold
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                when (myRoomsState) {
                    is UiState.Loading -> {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(CutCornerShape(5.dp))
                                    .background(InputDark)
                                    .border(1.dp, DividerIron, CutCornerShape(5.dp))
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(color = TacticalCyan, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                                Text("Syncing online rooms...", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            }
                        }
                    }
                    is UiState.Error -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(CutCornerShape(5.dp))
                                    .background(CrimsonDeep.copy(alpha = 0.55f))
                                    .border(1.dp, CrimsonSoft, CutCornerShape(5.dp))
                                    .padding(10.dp)
                            ) {
                                Text(myRoomsState.message, style = MaterialTheme.typography.labelSmall, color = TextPrimary)
                            }
                        }
                    }
                    is UiState.Success -> {
                        val rooms = myRoomsState.data.filter { it.status != "FINISHED" }
                        if (rooms.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(CutCornerShape(5.dp))
                                        .background(InputDark)
                                        .border(1.dp, DividerIron, CutCornerShape(5.dp))
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = "No active cloud rooms yet. Tap CREATE ONLINE ROOM or enter your friend's 6-digit Room Code above.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )
                                }
                            }
                        } else {
                            items(rooms, key = { it.roomCode }) { room ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(CutCornerShape(5.dp))
                                        .background(RowSlate)
                                        .border(1.dp, TacticalCyan.copy(alpha = 0.55f), CutCornerShape(5.dp))
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Groups, contentDescription = null, tint = ImperialGold, modifier = Modifier.size(18.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(
                                                text = "ROOM #${room.roomCode}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = ImperialGold,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${room.memberCount}/4 • ${room.status}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = EmeraldBuff
                                            )
                                        }
                                        Text(
                                            text = "Host: ${room.hostName}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = WarmAmber,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = { onRejoinRoom(room) },
                                        enabled = !isBusy,
                                        shape = grimdarkBevelShape(4.dp),
                                        border = BorderStroke(1.dp, EmeraldBorder),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = EmeraldSurface,
                                            contentColor = EmeraldBuff
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("OPEN", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
