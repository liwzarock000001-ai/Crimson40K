package com.example.ui.menu

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.SavedConfigEntity
import com.example.data.SavedSkirmishBattleEntity
import com.example.ui.theme.BorderSteel
import com.example.ui.theme.CardSlate
import com.example.ui.theme.DividerIron
import com.example.ui.theme.DriftingEmbersBackdrop
import com.example.ui.theme.EmeraldBorder
import com.example.ui.theme.EmeraldBuff
import com.example.ui.theme.EmeraldSurface
import com.example.ui.theme.GrimdarkBevelPanel
import com.example.ui.theme.ImperialGold
import com.example.ui.theme.InputDark
import com.example.ui.theme.PanelIronTop
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.VoidObsidian
import com.example.ui.theme.WarmAmber
import com.example.ui.theme.grimdarkBevelShape
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class PendingLoadType {
    BATTLE,
    ROSTER
}

@Composable
fun LoadGameScreen(
    savedBattle: SavedSkirmishBattleEntity?,
    savedConfig: SavedConfigEntity?,
    onBack: () -> Unit,
    onLoadBattle: () -> Unit,
    onLoadRoster: () -> Unit
) {
    var pendingLoad by remember { mutableStateOf<PendingLoadType?>(null) }
    val progress by animateFloatAsState(
        targetValue = if (pendingLoad != null) 1f else 0f,
        animationSpec = tween(durationMillis = 650),
        label = "load_progress"
    )

    BackHandler {
        if (pendingLoad == null) onBack()
    }

    LaunchedEffect(pendingLoad) {
        val target = pendingLoad ?: return@LaunchedEffect
        delay(700L)
        pendingLoad = null
        when (target) {
            PendingLoadType.BATTLE -> onLoadBattle()
            PendingLoadType.ROSTER -> onLoadRoster()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(VoidObsidian, Color(0xFF0D1117)),
                    radius = 1350f
                )
            )
    ) {
        DriftingEmbersBackdrop(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .displayCutoutPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    enabled = pendingLoad == null,
                    shape = grimdarkBevelShape(5.dp),
                    border = BorderStroke(1.dp, BorderSteel),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = PanelIronTop,
                        contentColor = TextPrimary
                    ),
                    modifier = Modifier.testTag("load_back_menu")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("MENU", style = MaterialTheme.typography.labelSmall)
                }

                Column {
                    Text(
                        text = "LOADING & SAVED SKIRMISH ARCHIVE",
                        style = MaterialTheme.typography.titleMedium,
                        color = ImperialGold,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Restore saved Skirmish battle progress or saved army deployment rosters",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }

            HorizontalDivider(color = DividerIron)

            if (pendingLoad != null) {
                // Dedicated Loading Screen State while loading saved Skirmish
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    GrimdarkBevelPanel(
                        modifier = Modifier
                            .widthIn(max = 460.dp)
                            .fillMaxWidth(),
                        cut = 10.dp,
                        borderColor = ImperialGold,
                        topColor = CardSlate,
                        bottomColor = VoidObsidian,
                        accentBarColor = TacticalCyan
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = if (pendingLoad == PendingLoadType.BATTLE) {
                                    "DEPLOYING SAVED SKIRMISH SECTOR..."
                                } else {
                                    "LOADING SAVED ARMY ROSTER..."
                                },
                                style = MaterialTheme.typography.titleSmall,
                                color = ImperialGold,
                                fontWeight = FontWeight.Bold
                            )
                            LinearProgressIndicator(
                                progress = { progress },
                                color = TacticalCyan,
                                trackColor = InputDark,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                            )
                            Text(
                                text = "Synchronizing terrain grid, unit wounds & tactical state ${(progress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    // Slot 1: Saved Mid-Battle Skirmish Session
                    item {
                        GrimdarkBevelPanel(
                            modifier = Modifier.fillMaxWidth(),
                            cut = 8.dp,
                            borderColor = if (savedBattle != null) ImperialGold else BorderSteel,
                            topColor = CardSlate,
                            bottomColor = VoidObsidian,
                            accentBarColor = if (savedBattle != null) ImperialGold else BorderSteel
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Default.Save, contentDescription = null, tint = ImperialGold)
                                        Text(
                                            "SKIRMISH BATTLE SAVE (IN-PROGRESS)",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    val dateStr = savedBattle?.let {
                                        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(it.updatedAt))
                                    } ?: "Empty"
                                    Text(dateStr, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                }

                                if (savedBattle != null) {
                                    Text(
                                        text = "Round ${savedBattle.round} • Commander: ${savedBattle.playerFaction} • Kills ${savedBattle.playerKills} / Losses ${savedBattle.enemyKills}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = WarmAmber
                                    )
                                    Text(
                                        text = "Kills: ${savedBattle.playerKills} • Losses: ${savedBattle.enemyKills}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Button(
                                        onClick = { pendingLoad = PendingLoadType.BATTLE },
                                        shape = grimdarkBevelShape(6.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = EmeraldSurface,
                                            contentColor = EmeraldBuff
                                        ),
                                        border = BorderStroke(1.dp, EmeraldBorder),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("load_saved_battle_button")
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("LOAD & RESUME SKIRMISH BATTLE", style = MaterialTheme.typography.labelMedium)
                                    }
                                } else {
                                    Text(
                                        text = "No mid-battle Skirmish save found yet. Tap SAVE on the Skirmish battlefield HUD to save your battle.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted
                                    )
                                }
                            }
                        }
                    }

                    // Slot 2: Saved Skirmish Army Roster Configuration
                    item {
                        GrimdarkBevelPanel(
                            modifier = Modifier.fillMaxWidth(),
                            cut = 8.dp,
                            borderColor = if (savedConfig != null) TacticalCyan else BorderSteel,
                            topColor = CardSlate,
                            bottomColor = VoidObsidian,
                            accentBarColor = if (savedConfig != null) TacticalCyan else BorderSteel
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Default.FolderOpen, contentDescription = null, tint = TacticalCyan)
                                        Text(
                                            "SKIRMISH ARMY ROSTER SAVE",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    val dateStr = savedConfig?.let {
                                        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(it.updatedAt))
                                    } ?: "Empty"
                                    Text(dateStr, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                }

                                if (savedConfig != null) {
                                    Text(
                                        text = "Faction: ${savedConfig.playerFaction} • Requisition Limit: ${savedConfig.pointLimit} PT",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TacticalCyan
                                    )
                                    Text(
                                        text = "Saved unit roster & custom stat templates in local database.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Button(
                                        onClick = { pendingLoad = PendingLoadType.ROSTER },
                                        shape = grimdarkBevelShape(6.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = PanelIronTop,
                                            contentColor = TacticalCyan
                                        ),
                                        border = BorderStroke(1.dp, TacticalCyan),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("load_saved_roster_button")
                                    ) {
                                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("LOAD ROSTER INTO SKIRMISH SETUP", style = MaterialTheme.typography.labelMedium)
                                    }
                                } else {
                                    Text(
                                        text = "No saved army configuration found. Tap SAVE on the Skirmish Setup screen to store a roster.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
