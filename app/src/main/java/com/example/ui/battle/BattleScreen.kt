package com.example.ui.battle

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Rotate90DegreesCcw
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.model.BattleSummary
import com.example.model.BattleUnit
import com.example.model.CoverTag
import com.example.model.Faction
import com.example.model.GridPos
import com.example.model.TargetPreview
import com.example.model.TurnSplashData
import com.example.model.UnitArchetype
import com.example.model.WeaponMode
import com.example.ui.theme.BorderSteel
import com.example.ui.theme.CardSlate
import com.example.ui.theme.ColorTokens
import com.example.ui.theme.CrimsonBright
import com.example.ui.theme.CrimsonDeep
import com.example.ui.theme.CrimsonSoft
import com.example.ui.theme.CrimsonSurface
import com.example.ui.theme.DividerIron
import com.example.ui.theme.EmeraldBorder
import com.example.ui.theme.EmeraldBuff
import com.example.ui.theme.EmeraldSurface
import com.example.ui.theme.GrimdarkBevelPanel
import com.example.ui.theme.ImperialGold
import com.example.ui.theme.InputDark
import com.example.ui.theme.MarineBright
import com.example.ui.theme.MarineText
import com.example.ui.theme.PanelIronBottom
import com.example.ui.theme.PanelIronTop
import com.example.ui.theme.SurgeOrange
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.TarnishedBrass
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.VoidObsidian
import com.example.ui.theme.WarmAmber
import com.example.ui.theme.XenosText
import com.example.ui.theme.XenosViolet
import com.example.ui.theme.factionPalette
import com.example.ui.theme.grimdarkBevelShape
import com.example.viewmodel.BattleUiState

@Composable
fun BattleScreen(
    state: BattleUiState,
    onTileTap: (GridPos) -> Unit,
    onConfirmStrike: () -> Unit,
    onSelectWeaponMode: (WeaponMode) -> Unit,
    onTriggerSurge: () -> Unit,
    onRotateUnit: (Boolean) -> Unit,
    onEndTurn: () -> Unit,
    onToggleAutoFocus: () -> Unit,
    onCenterCamera: () -> Unit,
    onToggleSound: () -> Unit,
    onToggleUnitCard: () -> Unit,
    onDismissTurnSplash: () -> Unit,
    onExitToSetup: () -> Unit
) {
    var showConfirmExit by remember { mutableStateOf(false) }
    var cameraRotationQuarter by remember { mutableIntStateOf(0) }
    var zoomStepTrigger by remember { mutableIntStateOf(0) }
    var zoomStepDirection by remember { mutableIntStateOf(0) }

    BackHandler {
        showConfirmExit = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidObsidian)
    ) {
        // Full-bleed OpenGL ES 2.0 Grimdark 3D Battlefield
        Battlefield3DView(
            state = state,
            cameraYawQuarter = cameraRotationQuarter,
            zoomStepTrigger = zoomStepTrigger,
            zoomStepDirection = zoomStepDirection,
            onTileTap = onTileTap,
            modifier = Modifier.fillMaxSize()
        )

        // Subtle atmospheric vignette along screen perimeter
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color(0x55040507)),
                        radius = 1350f
                    )
                )
        )

        // Top-Left Relic Command Header & Army Counters (themed by active faction)
        TopHudOverlay(
            round = state.round,
            activeFaction = state.activeFaction,
            isPlayerTurn = state.isPlayerTurn,
            autoFocusCamera = state.autoFocusCamera,
            soundEnabled = state.soundEnabled,
            marineAliveModels = state.marineAliveModels,
            xenosAliveModels = state.xenosAliveModels,
            onToggleAutoFocus = onToggleAutoFocus,
            onCenterCamera = onCenterCamera,
            onRotateCamera = remember { { cameraRotationQuarter = (cameraRotationQuarter + 1) % 4 } },
            onZoomIn = remember {
                {
                    zoomStepDirection = 1
                    zoomStepTrigger++
                }
            },
            onZoomOut = remember {
                {
                    zoomStepDirection = -1
                    zoomStepTrigger++
                }
            },
            onToggleSound = onToggleSound,
            onRequestSetup = remember { { showConfirmExit = true } },
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .displayCutoutPadding()
                .padding(8.dp)
        )

        // Top-Right Turn Splash Banner
        AnimatedVisibility(
            visible = state.turnSplash != null,
            enter = fadeIn() + scaleIn(initialScale = 0.94f),
            exit = fadeOut() + scaleOut(targetScale = 0.96f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .displayCutoutPadding()
                .padding(8.dp)
        ) {
            state.turnSplash?.let { splash ->
                TurnSplashCard(splash = splash, onClick = onDismissTurnSplash)
            }
        }

        // Bottom HUD: Target Preview cleanly docked above Unit Dossier + Command Dock
        val selectedUnit = state.selectedUnit
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .displayCutoutPadding()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.End
        ) {
            AnimatedVisibility(visible = state.targetPreview != null) {
                state.targetPreview?.let { preview ->
                    TargetPreviewBanner(
                        preview = preview,
                        onStrike = onConfirmStrike
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                selectedUnit?.let { unit ->
                    UnitStatusCard(
                        unit = unit,
                        collapsed = state.unitCardCollapsed,
                        onToggleCollapse = onToggleUnitCard,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .widthIn(min = 240.dp, max = 335.dp)
                    )
                } ?: Spacer(Modifier.width(1.dp))

                Spacer(Modifier.width(10.dp))

                ActionDock(
                    selected = selectedUnit,
                    activeFaction = state.activeFaction,
                    isPlayerTurn = state.isPlayerTurn,
                    weaponMode = state.weaponMode,
                    onRotateUnit = onRotateUnit,
                    onSelectWeaponMode = onSelectWeaponMode,
                    onTriggerSurge = onTriggerSurge,
                    onEndTurn = onEndTurn
                )
            }
        }

        // Confirm Return to Setup Dialog
        if (showConfirmExit) {
            AlertDialog(
                onDismissRequest = { showConfirmExit = false },
                shape = grimdarkBevelShape(10.dp),
                containerColor = CardSlate,
                titleContentColor = ImperialGold,
                textContentColor = TextMuted,
                title = {
                    Text(stringResource(R.string.confirm_setup_title), style = MaterialTheme.typography.titleMedium)
                },
                text = {
                    Text(stringResource(R.string.confirm_setup_msg), style = MaterialTheme.typography.bodyMedium)
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showConfirmExit = false
                            onExitToSetup()
                        },
                        shape = grimdarkBevelShape(5.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSurface, contentColor = TextPrimary),
                        border = BorderStroke(1.dp, EmeraldBorder)
                    ) {
                        Text("Confirm")
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showConfirmExit = false },
                        shape = grimdarkBevelShape(5.dp),
                        border = BorderStroke(1.dp, BorderSteel)
                    ) {
                        Text("Cancel", color = TextPrimary)
                    }
                }
            )
        }

        // Victory / Defeat Modal
        state.summary?.let { summary ->
            BattleSummaryDialog(summary = summary, onReturnToSetup = onExitToSetup)
        }
    }
}

@Composable
private fun TopHudOverlay(
    round: Int,
    activeFaction: Faction,
    isPlayerTurn: Boolean,
    autoFocusCamera: Boolean,
    soundEnabled: Boolean,
    marineAliveModels: Int,
    xenosAliveModels: Int,
    onToggleAutoFocus: () -> Unit,
    onCenterCamera: () -> Unit,
    onRotateCamera: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onToggleSound: () -> Unit,
    onRequestSetup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pal = factionPalette(activeFaction)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        GrimdarkBevelPanel(
            cut = 7.dp,
            borderColor = pal.trimBorder,
            topColor = pal.panelTop,
            bottomColor = pal.panelBottom,
            accentBarColor = pal.primaryAccent
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(28.dp)
                        .background(pal.primaryAccent)
                )
                Column(modifier = Modifier.padding(end = 4.dp)) {
                    Text(
                        text = "ROUND $round",
                        style = MaterialTheme.typography.labelSmall,
                        color = pal.trimBorder
                    )
                    Text(
                        text = if (isPlayerTurn) "YOUR TURN" else "ENEMY TURN...",
                        style = MaterialTheme.typography.titleSmall,
                        color = pal.headerText
                    )
                }

                HudChipButton(
                    label = if (autoFocusCamera) "Focus" else "Free",
                    icon = Icons.Default.Videocam,
                    tint = if (autoFocusCamera) TacticalCyan else TextMuted,
                    borderColor = if (autoFocusCamera) TacticalCyan.copy(alpha = 0.7f) else BorderSteel,
                    onClick = onToggleAutoFocus,
                    testTag = "focus_toggle_button"
                )
                HudIconButton(
                    icon = Icons.Default.CenterFocusStrong,
                    contentDesc = "Center View",
                    onClick = onCenterCamera,
                    testTag = "center_view_button"
                )
                HudIconButton(
                    icon = Icons.Default.Rotate90DegreesCcw,
                    contentDesc = "Rotate 3D Camera",
                    onClick = onRotateCamera,
                    testTag = "rotate_cam_button"
                )
                HudIconButton(
                    icon = Icons.Default.ZoomIn,
                    contentDesc = "Zoom In Close",
                    onClick = onZoomIn,
                    testTag = "zoom_in_button"
                )
                HudIconButton(
                    icon = Icons.Default.ZoomOut,
                    contentDesc = "Zoom Out",
                    onClick = onZoomOut,
                    testTag = "zoom_out_button"
                )
                HudIconButton(
                    icon = if (soundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                    contentDesc = "Toggle Sound",
                    onClick = onToggleSound,
                    testTag = "sound_toggle_button"
                )
                HudChipButton(
                    label = "Setup",
                    icon = Icons.Default.Settings,
                    tint = pal.headerText,
                    borderColor = pal.trimMuted,
                    onClick = onRequestSetup,
                    testTag = "exit_setup_button"
                )
            }
        }

        // Slim Army Force Telemetry Bar
        GrimdarkBevelPanel(
            cut = 5.dp,
            borderColor = pal.trimMuted,
            topColor = pal.panelTop,
            bottomColor = pal.panelBottom,
            showRivets = false
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ULTRAMARINES: $marineAliveModels",
                    style = MaterialTheme.typography.labelSmall,
                    color = MarineText
                )
                Text("|", style = MaterialTheme.typography.labelSmall, color = pal.trimMuted)
                Text(
                    text = "SWARM: $xenosAliveModels",
                    style = MaterialTheme.typography.labelSmall,
                    color = XenosText
                )
                Text("|", style = MaterialTheme.typography.labelSmall, color = pal.trimMuted)
                Text(
                    text = activeFaction.badgeTag,
                    style = MaterialTheme.typography.labelSmall,
                    color = pal.headerText
                )
            }
        }
    }
}

@Composable
private fun HudChipButton(
    label: String,
    icon: ImageVector,
    tint: Color,
    borderColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    val shape = remember { CutCornerShape(5.dp) }
    Row(
        modifier = Modifier
            .height(34.dp)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(PanelIronTop, InputDark)))
            .border(1.dp, borderColor, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(14.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = tint, maxLines = 1, softWrap = false)
    }
}

@Composable
private fun HudIconButton(
    icon: ImageVector,
    contentDesc: String,
    onClick: () -> Unit,
    testTag: String
) {
    val shape = remember { CutCornerShape(5.dp) }
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(PanelIronTop, InputDark)))
            .border(1.dp, BorderSteel, shape)
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDesc, tint = TextPrimary, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun TargetPreviewBanner(
    preview: TargetPreview,
    onStrike: () -> Unit
) {
    val tagBg = when (preview.coverTag) {
        CoverTag.OPEN -> Color(0xFF1F232B)
        CoverTag.COVER -> Color(0xFF3E3012)
        CoverTag.FLANK, CoverTag.REAR -> CrimsonSurface
    }
    val tagFg = when (preview.coverTag) {
        CoverTag.OPEN -> TextMuted
        CoverTag.COVER -> WarmAmber
        CoverTag.FLANK, CoverTag.REAR -> CrimsonSoft
    }

    GrimdarkBevelPanel(
        cut = 8.dp,
        borderColor = CrimsonBright,
        topColor = Color(0xF5211014),
        bottomColor = Color(0xF511090C),
        accentBarColor = ImperialGold,
        modifier = Modifier.widthIn(max = 390.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = preview.targetName.uppercase(),
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(CutCornerShape(3.dp))
                            .background(tagBg)
                            .border(1.dp, tagFg.copy(alpha = 0.45f), CutCornerShape(3.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(preview.coverTag.label, style = MaterialTheme.typography.labelSmall, color = tagFg)
                    }
                }
                Text(
                    text = "HIT: ${preview.hitChance}% • EST DMG: ${preview.expectedTotalDamage} (${preview.attacksCount}x)",
                    style = MaterialTheme.typography.bodySmall,
                    color = WarmAmber
                )
            }

            Button(
                onClick = onStrike,
                shape = grimdarkBevelShape(6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonDeep, contentColor = TextPrimary),
                border = BorderStroke(1.2.dp, ImperialGold),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier
                    .height(44.dp)
                    .testTag("confirm_strike_button")
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.btn_strike), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UnitStatusCard(
    unit: BattleUnit,
    collapsed: Boolean,
    onToggleCollapse: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pal = factionPalette(unit.faction)

    GrimdarkBevelPanel(
        cut = 8.dp,
        borderColor = pal.trimBorder,
        topColor = pal.panelTop,
        bottomColor = pal.panelBottom,
        accentBarColor = pal.primaryAccent,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleCollapse),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = unit.template.name.uppercase(),
                        style = MaterialTheme.typography.titleSmall,
                        color = pal.headerText,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${unit.aliveModels}/${unit.template.squadSize}",
                        style = MaterialTheme.typography.labelSmall,
                        color = pal.trimBorder,
                        maxLines = 1,
                        softWrap = false
                    )
                }
                Icon(
                    imageVector = if (collapsed) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Toggle Unit Card",
                    tint = pal.trimBorder,
                    modifier = Modifier.size(20.dp)
                )
            }

            if (!collapsed) {
                Spacer(Modifier.height(5.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProgressStatBar(
                        label = "INTEGRITY",
                        valueText = "${unit.totalHp}/${unit.maxTotalHp}",
                        progress = (unit.totalHp.toFloat() / unit.maxTotalHp.coerceAtLeast(1)).coerceIn(0f, 1f),
                        barColor = CrimsonBright,
                        modifier = Modifier.weight(1f)
                    )
                    ProgressStatBar(
                        label = "ZEAL",
                        valueText = "${unit.momentum}%",
                        progress = (unit.momentum / 100f).coerceIn(0f, 1f),
                        barColor = SurgeOrange,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(6.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    StatPill("MP", "${unit.mpLeft}", TacticalCyan)
                    StatPill("AP", "${unit.apLeft}", CrimsonSoft)
                    StatPill("ARM", "${unit.template.armor}", TextPrimary)
                    StatPill("PEN", "${unit.template.armorPen}", TextPrimary)
                    StatPill("DMG", "${unit.effectiveDamage}", TextPrimary)
                    StatPill("RNG", "${unit.template.range}", TextPrimary)
                    StatPill("ACC", "${unit.effectiveAccuracy}%", TextPrimary)
                    StatPill("FACE", unit.facing.label, pal.trimBorder)
                }
                if (unit.activeBuffLabel != null || unit.auraBuffed) {
                    Spacer(Modifier.height(4.dp))
                    val buffText = listOfNotNull(
                        unit.activeBuffLabel,
                        if (unit.auraBuffed) "AURA +10% ACC" else null
                    ).joinToString(" • ")
                    Text(
                        text = "LITANY: $buffText",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldBuff,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressStatBar(
    label: String,
    valueText: String,
    progress: Float,
    barColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted, maxLines = 1, softWrap = false)
            Text(valueText, style = MaterialTheme.typography.labelSmall, color = TextPrimary, maxLines = 1, softWrap = false)
        }
        Spacer(Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CutCornerShape(2.dp))
                .background(InputDark)
                .border(0.8.dp, DividerIron, CutCornerShape(2.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(6.dp)
                    .background(barColor)
            )
        }
    }
}

@Composable
private fun StatPill(label: String, value: String, valueColor: Color) {
    val shape = remember { CutCornerShape(3.dp) }
    Row(
        modifier = Modifier
            .clip(shape)
            .background(InputDark)
            .border(1.dp, DividerIron, shape)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted, maxLines = 1, softWrap = false)
        Text(value, style = MaterialTheme.typography.labelSmall, color = valueColor, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
    }
}

@Composable
private fun ActionDock(
    selected: BattleUnit?,
    activeFaction: Faction,
    isPlayerTurn: Boolean,
    weaponMode: WeaponMode,
    onRotateUnit: (Boolean) -> Unit,
    onSelectWeaponMode: (WeaponMode) -> Unit,
    onTriggerSurge: () -> Unit,
    onEndTurn: () -> Unit
) {
    val enabled = isPlayerTurn && selected != null
    val pal = factionPalette(selected?.faction ?: activeFaction)

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (selected != null) {
            GrimdarkBevelPanel(
                cut = 5.dp,
                borderColor = pal.trimBorder,
                topColor = pal.panelTop,
                bottomColor = pal.panelBottom,
                showRivets = false,
                modifier = Modifier.widthIn(max = 315.dp)
            ) {
                Text(
                    text = "⚡ ${selected.template.passiveName}: ${selected.template.passiveDesc}",
                    style = MaterialTheme.typography.labelSmall,
                    color = pal.headerText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ActionDockButton(
                title = "Turn L",
                subtitle = "Free",
                icon = Icons.AutoMirrored.Filled.RotateLeft,
                containerTop = PanelIronTop,
                containerBottom = PanelIronBottom,
                borderColor = BorderSteel,
                textColor = TextPrimary,
                enabled = enabled,
                onClick = { onRotateUnit(true) },
                testTag = "rotate_left_button"
            )
            ActionDockButton(
                title = "Turn R",
                subtitle = "Free",
                icon = Icons.AutoMirrored.Filled.RotateRight,
                containerTop = PanelIronTop,
                containerBottom = PanelIronBottom,
                borderColor = BorderSteel,
                textColor = TextPrimary,
                enabled = enabled,
                onClick = { onRotateUnit(false) },
                testTag = "rotate_right_button"
            )

            if (selected != null && selected.template.hasPistol) {
                val pistolReady = enabled && selected.apLeft >= selected.template.pistolApCost && selected.pistolCooldownLeft == 0
                val isOn = weaponMode == WeaponMode.PISTOL
                ActionDockButton(
                    title = "Pistol",
                    subtitle = if (selected.pistolCooldownLeft > 0) "CD ${selected.pistolCooldownLeft}" else "${selected.template.pistolApCost} AP",
                    icon = null,
                    containerTop = if (isOn) ColorTokens.CyanSurface else ColorTokens.PistolSurface,
                    containerBottom = PanelIronBottom,
                    borderColor = if (isOn) TacticalCyan else BorderSteel,
                    textColor = TextPrimary,
                    enabled = pistolReady,
                    onClick = { onSelectWeaponMode(WeaponMode.PISTOL) },
                    testTag = "pistol_button"
                )
            }

            if (selected != null) {
                val isTerminator = selected.template.archetype == UnitArchetype.TERMINATOR
                val skillReady = enabled && selected.apLeft >= selected.template.skillApCost && selected.skillCooldownLeft == 0
                val isSkillOn = weaponMode == WeaponMode.SKILL
                ActionDockButton(
                    title = if (isTerminator) "Teleport" else "Skill",
                    subtitle = if (selected.skillCooldownLeft > 0) "CD ${selected.skillCooldownLeft}" else "${selected.template.skillApCost} AP",
                    icon = null,
                    containerTop = if (isSkillOn) ColorTokens.CyanSurface else ColorTokens.SkillSurface,
                    containerBottom = PanelIronBottom,
                    borderColor = if (isSkillOn) TacticalCyan else ColorTokens.SkillBorder,
                    textColor = WarmAmber,
                    enabled = skillReady,
                    onClick = { onSelectWeaponMode(WeaponMode.SKILL) },
                    testTag = "skill_button"
                )

                val surgeReady = enabled && selected.momentum >= 100 && !selected.surgeActive
                ActionDockButton(
                    title = "Surge",
                    subtitle = if (selected.surgeActive) "ACTIVE" else "${selected.momentum}%",
                    icon = Icons.Default.Bolt,
                    containerTop = ColorTokens.SurgeSurface,
                    containerBottom = PanelIronBottom,
                    borderColor = SurgeOrange,
                    textColor = WarmAmber,
                    enabled = surgeReady,
                    onClick = onTriggerSurge,
                    testTag = "surge_button"
                )
            }

            ActionDockButton(
                title = "End",
                subtitle = "AI Next",
                icon = null,
                containerTop = CrimsonDeep,
                containerBottom = CrimsonSurface,
                borderColor = ImperialGold,
                textColor = TextPrimary,
                enabled = isPlayerTurn,
                onClick = onEndTurn,
                testTag = "end_turn_button"
            )
        }
    }
}

@Composable
private fun ActionDockButton(
    title: String,
    subtitle: String,
    icon: ImageVector?,
    containerTop: Color,
    containerBottom: Color,
    borderColor: Color,
    textColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val shape = remember { CutCornerShape(6.dp) }
    val bgBrush = remember(containerTop, containerBottom, enabled) {
        if (enabled) {
            Brush.verticalGradient(listOf(containerTop, containerBottom))
        } else {
            Brush.verticalGradient(listOf(containerTop.copy(alpha = 0.35f), containerBottom.copy(alpha = 0.35f)))
        }
    }
    Column(
        modifier = Modifier
            .height(48.dp)
            .widthIn(min = 54.dp)
            .clip(shape)
            .background(bgBrush)
            .border(1.2.dp, if (enabled) borderColor else borderColor.copy(alpha = 0.3f), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 4.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = title,
                    tint = if (enabled) textColor else textColor.copy(alpha = 0.38f),
                    modifier = Modifier.size(13.dp)
                )
            }
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = if (enabled) textColor else textColor.copy(alpha = 0.38f),
                maxLines = 1,
                softWrap = false
            )
        }
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = if (enabled) TextMuted else TextMuted.copy(alpha = 0.38f),
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun TurnSplashCard(splash: TurnSplashData, onClick: () -> Unit) {
    val pal = factionPalette(splash.activeFaction)
    GrimdarkBevelPanel(
        cut = 9.dp,
        borderColor = pal.trimBorder,
        topColor = pal.panelTop,
        bottomColor = pal.panelBottom,
        accentBarColor = pal.primaryAccent,
        onClick = onClick,
        modifier = Modifier.widthIn(max = 285.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "ROUND ${splash.round} • ${splash.activeFaction.badgeTag}",
                style = MaterialTheme.typography.labelSmall,
                color = pal.trimBorder
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (splash.isPlayerTurn) "COMMANDER TURN" else "SWARM ACTIVATION",
                style = MaterialTheme.typography.titleMedium,
                color = pal.headerText
            )
            HorizontalDivider(color = DividerIron, modifier = Modifier.padding(vertical = 6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SplashStatBox("MARINES", "${splash.marineModelsAlive}", MarineText, Modifier.weight(1f))
                SplashStatBox("SWARM", "${splash.xenosModelsAlive}", XenosText, Modifier.weight(1f))
                SplashStatBox("KILLS", "${splash.playerKills}", EmeraldBuff, Modifier.weight(1f))
                SplashStatBox("LOSS", "${splash.playerLosses}", CrimsonSoft, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SplashStatBox(label: String, value: String, valueColor: Color, modifier: Modifier) {
    val shape = remember { CutCornerShape(4.dp) }
    Column(
        modifier = modifier
            .clip(shape)
            .background(InputDark)
            .border(1.dp, DividerIron, shape)
            .padding(vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Text(value, style = MaterialTheme.typography.titleSmall, color = valueColor)
    }
}

@Composable
private fun BattleSummaryDialog(
    summary: BattleSummary,
    onReturnToSetup: () -> Unit
) {
    val pal = factionPalette(summary.winner)
    Dialog(onDismissRequest = {}) {
        GrimdarkBevelPanel(
            cut = 11.dp,
            borderColor = pal.trimBorder,
            topColor = pal.panelTop,
            bottomColor = VoidObsidian,
            accentBarColor = pal.primaryAccent,
            modifier = Modifier.widthIn(max = 350.dp).fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (summary.playerWon) "VICTORY IN THE CRIMSON SECTOR" else "SECTOR FALLEN",
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (summary.playerWon) pal.headerText else CrimsonSoft,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "${summary.winner.displayName} prevailed in ${summary.rounds} rounds.\n" +
                        "Enemy Squads Eliminated: ${summary.playerKills}\n" +
                        "Squads Lost: ${summary.enemyKills}\n" +
                        "Total Damage Dealt: ${summary.totalDamageDealt}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onReturnToSetup,
                    shape = grimdarkBevelShape(6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = pal.buttonSurface, contentColor = pal.headerText),
                    border = BorderStroke(1.2.dp, pal.trimBorder),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("return_to_setup_button")
                ) {
                    Text("RETURN TO COMMAND DECK")
                }
            }
        }
    }
}
