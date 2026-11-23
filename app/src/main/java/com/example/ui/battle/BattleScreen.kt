package com.example.ui.battle

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoubleArrow
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Rotate90DegreesCcw
import androidx.compose.material.icons.filled.Save
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
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.engine.TacticalRules
import com.example.model.BattleSummary
import com.example.model.BattleUnit
import com.example.model.ChargePreview
import com.example.model.CoverTag
import com.example.model.DarkPactChoice
import com.example.model.Faction
import com.example.model.MovePreview
import com.example.model.TargetPreview
import com.example.model.TurnSplashData
import com.example.model.UnitArchetype
import com.example.model.UnitTemplate
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
import kotlin.math.hypot

@Composable
fun BattleScreen(
    state: BattleUiState,
    onWorldTap: (Float, Float) -> Unit,
    onToggleMoveMode: () -> Unit,
    onAdvanceMove: () -> Unit,
    onSetCmStep: (Float) -> Unit,
    onWalkForwardCm: () -> Unit,
    onConfirmMove: () -> Unit,
    onCancelMove: () -> Unit,
    onToggleChargeMode: () -> Unit,
    onConfirmCharge: () -> Unit,
    onCancelCharge: () -> Unit,
    onConfirmStrike: () -> Unit,
    onSelectDarkPact: (DarkPactChoice) -> Unit = {},
    onSelectWeaponMode: (WeaponMode) -> Unit,
    onRotateUnit: (Boolean) -> Unit,
    onFightFirst: () -> Unit = {},
    onEndTurn: () -> Unit,
    onToggleAutoFocus: () -> Unit,
    onCenterCamera: () -> Unit,
    onToggleSound: () -> Unit,
    onToggleUnitCard: () -> Unit,
    onDismissTurnSplash: () -> Unit,
    onSelectReserveUnit: (String) -> Unit = {},
    onSaveBattle: () -> Unit = {},
    onExitToSetup: () -> Unit
) {
    var showConfirmExit by remember { mutableStateOf(false) }
    var cameraYawDeltaTrigger by remember { mutableIntStateOf(0) }
    var cameraYawDeltaDeg by remember { mutableFloatStateOf(0f) }
    var cameraPitchMode by remember { mutableIntStateOf(0) } // 0 = Top (46f), 1 = Angled (30f), 2 = Side (16f)
    var zoomStepTrigger by remember { mutableIntStateOf(0) }
    var zoomStepDirection by remember { mutableIntStateOf(0) }
    var viewingSkillInfoTemplate by remember { mutableStateOf<UnitTemplate?>(null) }
    var diceLogExpanded by remember { mutableStateOf(true) }
    var diceLogOffset by remember { mutableStateOf(IntOffset(16, 110)) }

    val targetCameraPitch = when (cameraPitchMode) {
        1 -> 30f
        2 -> 16f
        else -> 46f
    }

    BackHandler {
        showConfirmExit = true
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidObsidian)
    ) {
        val maxLogBodyHeight = (maxHeight * 0.35f).coerceIn(100.dp, 220.dp)
        val logPanelWidth = (maxWidth * 0.38f).coerceIn(250.dp, 316.dp)
        viewingSkillInfoTemplate?.let { template ->
            SkillInfoDialog(
                template = template,
                onDismiss = { viewingSkillInfoTemplate = null }
            )
        }
        // Full-bleed OpenGL ES 2.0 Grimdark 3D Battlefield
        Battlefield3DView(
            state = state,
            cameraYawDeltaTrigger = cameraYawDeltaTrigger,
            cameraYawDeltaDeg = cameraYawDeltaDeg,
            cameraPitchDeg = targetCameraPitch,
            zoomStepTrigger = zoomStepTrigger,
            zoomStepDirection = zoomStepDirection,
            onWorldTap = onWorldTap,
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

        // Top HUD Stack: Slim Deployment Banner (topmost edge) + Top-Left Command Header & Top-Right Camera Controls (zero overlap)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .displayCutoutPadding()
                .padding(horizontal = 8.dp, vertical = if (state.isDeploymentPhase) 2.dp else 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (state.isDeploymentPhase) {
                DeploymentPhaseBanner(
                    turnPlayerName = state.deploymentTurnPlayerName,
                    teamIndex = state.deploymentTeamIndex,
                    isLocalTurn = state.isPlayerTurn,
                    remainingReserves = state.reserveUnits.size,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .testTag("deployment_phase_banner")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Top-Left Relic Command Header & Army Counters (themed by active faction)
                TopHudOverlay(
                    round = state.round,
                    activeFaction = state.activeFaction,
                    isPlayerTurn = state.isPlayerTurn,
                    isCoopMode = state.isCoopMode,
                    isDeploymentPhase = state.isDeploymentPhase,
                    activeCoopPlayerName = if (state.isDeploymentPhase) state.deploymentTurnPlayerName else state.activeCoopPlayerName,
                    autoFocusCamera = state.autoFocusCamera,
                    soundEnabled = state.soundEnabled,
                    playerFaction = state.playerFaction,
                    enemyFaction = state.resolvedEnemyFaction,
                    playerAliveModels = state.playerAliveModels,
                    enemyAliveModels = state.enemyAliveModels,
                    onToggleAutoFocus = onToggleAutoFocus,
                    onCenterCamera = onCenterCamera,
                    onRotateCamera = remember {
                        {
                            cameraYawDeltaDeg = -45f
                            cameraYawDeltaTrigger++
                        }
                    },
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
                    onSaveBattle = onSaveBattle,
                    onRequestSetup = remember { { showConfirmExit = true } },
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(end = 6.dp)
                )

                // Top-Right Camera Orbit & Side-Angle Controls + Turn Splash Banner
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val camPal = factionPalette(state.activeFaction)
                    GrimdarkBevelPanel(
                        cut = 6.dp,
                        borderColor = camPal.trimBorder,
                        topColor = camPal.panelTop,
                        bottomColor = camPal.panelBottom,
                        showRivets = false
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            HudIconButton(
                                icon = Icons.AutoMirrored.Filled.RotateLeft,
                                contentDesc = "Orbit Camera Left",
                                onClick = {
                                    cameraYawDeltaDeg = -45f
                                    cameraYawDeltaTrigger++
                                },
                                testTag = "cam_orbit_left_button"
                            )
                            HudChipButton(
                                label = when (cameraPitchMode) {
                                    1 -> "3/4"
                                    2 -> "Side"
                                    else -> "Top"
                                },
                                icon = Icons.Default.Videocam,
                                tint = if (cameraPitchMode != 0) TacticalCyan else camPal.headerText,
                                borderColor = if (cameraPitchMode != 0) TacticalCyan.copy(alpha = 0.75f) else BorderSteel,
                                onClick = { cameraPitchMode = (cameraPitchMode + 1) % 3 },
                                testTag = "cam_pitch_toggle_button"
                            )
                            HudIconButton(
                                icon = Icons.AutoMirrored.Filled.RotateRight,
                                contentDesc = "Orbit Camera Right",
                                onClick = {
                                    cameraYawDeltaDeg = 45f
                                    cameraYawDeltaTrigger++
                                },
                                testTag = "cam_orbit_right_button"
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = state.saveBannerMessage != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        state.saveBannerMessage?.let { msg ->
                            GrimdarkBevelPanel(
                                cut = 6.dp,
                                borderColor = EmeraldBorder,
                                topColor = EmeraldSurface,
                                bottomColor = VoidObsidian,
                                showRivets = false
                            ) {
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldBuff,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        AnimatedVisibility(
                            visible = state.turnSplash != null,
                            enter = fadeIn() + scaleIn(initialScale = 0.94f),
                            exit = fadeOut() + scaleOut(targetScale = 0.96f)
                        ) {
                            state.turnSplash?.let { splash ->
                                TurnSplashCard(splash = splash, onClick = onDismissTurnSplash)
                            }
                        }

                        if (!state.isDeploymentPhase) {
                            CombatDiceLogPanel(
                                logs = state.combatLogs,
                                activeFaction = state.activeFaction,
                                expanded = diceLogExpanded,
                                onToggleExpanded = { diceLogExpanded = !diceLogExpanded },
                                maxBodyHeight = maxLogBodyHeight,
                                modifier = Modifier
                                    .width(logPanelWidth)
                                    .offset { diceLogOffset }
                                    .pointerInput(Unit) {
                                        detectDragGestures { _, dragAmount ->
                                            diceLogOffset = IntOffset(
                                                (diceLogOffset.x + dragAmount.x).toInt(),
                                                (diceLogOffset.y + dragAmount.y).toInt()
                                            )
                                        }
                                    }
                            )
                        }
                    }
                }
            }
        }

        // Bottom HUD: Deployment Tray OR Target Preview cleanly docked above Unit Dossier + Command Dock
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
            if (state.isDeploymentPhase) {
                DeploymentTrayBar(
                    state = state,
                    onSelectReserveUnit = onSelectReserveUnit
                )
            } else {
            AnimatedVisibility(visible = state.targetPreview != null) {
                state.targetPreview?.let { preview ->
                    TargetPreviewBanner(
                        preview = preview,
                        canChooseDarkPact = selectedUnit?.template?.archetype == UnitArchetype.LEGIONARY,
                        leadershipNeeded = selectedUnit?.template?.leadership ?: 6,
                        selectedDarkPact = state.selectedDarkPact,
                        onSelectDarkPact = onSelectDarkPact,
                        onStrike = onConfirmStrike
                    )
                }
            }

            val canMoveControls = selectedUnit != null &&
                state.canCommandUnit(selectedUnit) &&
                !state.isAnimating

            AnimatedVisibility(visible = canMoveControls && state.chargePreview != null) {
                val cPreview = state.chargePreview
                if (cPreview != null) {
                    ChargePreviewBanner(
                        preview = cPreview,
                        onConfirmCharge = onConfirmCharge,
                        onCancelCharge = onCancelCharge
                    )
                }
            }

            AnimatedVisibility(visible = canMoveControls && state.chargeModeActive && state.chargePreview == null) {
                GrimdarkBevelPanel(
                    cut = 6.dp,
                    borderColor = WarmAmber,
                    topColor = Color(0xF0251208),
                    bottomColor = Color(0xF0120804),
                    accentBarColor = WarmAmber,
                    showRivets = false
                ) {
                    Text(
                        text = "CHARGE DECLARATION: TAP ENEMY WITHIN 12 CM (ROLL 2D6)",
                        style = MaterialTheme.typography.labelMedium,
                        color = WarmAmber,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            AnimatedVisibility(visible = canMoveControls && state.movePreview != null) {
                val preview = state.movePreview
                val unit = selectedUnit
                if (preview != null && unit != null) {
                    MovePreviewBanner(
                        preview = preview,
                        remainingAfterCm = (unit.moveCmLeft - preview.distanceCm).coerceAtLeast(0f),
                        onConfirmMove = onConfirmMove,
                        onCancelMove = onCancelMove
                    )
                }
            }

            val isSelectedEngaged = selectedUnit != null &&
                TacticalRules.isEngagedInMelee(selectedUnit, state.units)

            AnimatedVisibility(visible = canMoveControls && isSelectedEngaged) {
                GrimdarkBevelPanel(
                    cut = 6.dp,
                    borderColor = CrimsonBright,
                    topColor = Color(0xF0250D12),
                    bottomColor = Color(0xF014070A),
                    accentBarColor = CrimsonBright,
                    showRivets = false
                ) {
                    Text(
                        text = "MELEE DUEL LOCK • ELIMINATE ENGAGED ENEMY TO MOVE",
                        style = MaterialTheme.typography.labelSmall,
                        color = CrimsonSoft,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
                    )
                }
            }

            AnimatedVisibility(visible = canMoveControls && state.moveModeActive && !isSelectedEngaged) {
                selectedUnit?.let { unit ->
                    CmMovementControlBar(
                        unit = unit,
                        cmStepChoice = state.cmStepChoice,
                        onSetCmStep = onSetCmStep,
                        onWalkForwardCm = onWalkForwardCm
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
                        isEngagedInMelee = isSelectedEngaged,
                        collapsed = state.unitCardCollapsed,
                        onToggleCollapse = onToggleUnitCard,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .widthIn(min = 240.dp, max = 355.dp)
                    )
                } ?: Spacer(Modifier.width(1.dp))

                Spacer(Modifier.width(10.dp))

                ActionDock(
                    selected = selectedUnit,
                    canCommandSelected = selectedUnit != null && state.canCommandUnit(selectedUnit),
                    allUnits = state.units,
                    activeFaction = state.activeFaction,
                    isPlayerTurn = state.isPlayerTurn && !state.isAnimating,
                    weaponMode = state.weaponMode,
                    moveModeActive = state.moveModeActive,
                    chargeModeActive = state.chargeModeActive,
                    onToggleMoveMode = onToggleMoveMode,
                    onAdvanceMove = onAdvanceMove,
                    onToggleChargeMode = onToggleChargeMode,
                    onRotateUnit = onRotateUnit,
                    onFightFirst = onFightFirst,
                    onSelectWeaponMode = onSelectWeaponMode,
                    onEndTurn = onEndTurn,
                    onInspectSkill = { template -> viewingSkillInfoTemplate = template }
                )
            }
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
    isCoopMode: Boolean,
    isDeploymentPhase: Boolean,
    activeCoopPlayerName: String,
    autoFocusCamera: Boolean,
    soundEnabled: Boolean,
    playerFaction: Faction,
    enemyFaction: Faction,
    playerAliveModels: Int,
    enemyAliveModels: Int,
    onToggleAutoFocus: () -> Unit,
    onCenterCamera: () -> Unit,
    onRotateCamera: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onToggleSound: () -> Unit,
    onSaveBattle: () -> Unit,
    onRequestSetup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pal = factionPalette(activeFaction)
    var collapsed by remember { mutableStateOf(false) }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        GrimdarkBevelPanel(
            cut = 6.dp,
            borderColor = pal.trimBorder,
            topColor = pal.panelTop,
            bottomColor = pal.panelBottom,
            accentBarColor = pal.primaryAccent
        ) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(if (collapsed) 44.dp else 74.dp)
                    .clickable { collapsed = !collapsed },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (collapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                        contentDescription = if (collapsed) "Expand HUD" else "Collapse HUD",
                        tint = pal.headerText,
                        modifier = Modifier.size(18.dp)
                    )
                    if (collapsed) {
                        Text(
                            text = "HUD",
                            style = MaterialTheme.typography.labelSmall,
                            color = pal.trimBorder
                        )
                    }
                }
            }
        }

        if (!collapsed) {
            Column(
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
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 9.dp, vertical = 6.dp),
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
                                text = when {
                                    isDeploymentPhase -> "DEPLOY • CO-OP"
                                    isCoopMode -> "R$round • CO-OP"
                                    else -> "ROUND $round"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = pal.trimBorder
                            )
                            Text(
                                text = when {
                                    isCoopMode && activeCoopPlayerName.isNotBlank() -> activeCoopPlayerName.uppercase()
                                    isPlayerTurn -> "YOUR TURN"
                                    else -> "ENEMY TURN..."
                                },
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
                        if (!isCoopMode) {
                            HudChipButton(
                                label = "Save",
                                icon = Icons.Default.Save,
                                tint = EmeraldBuff,
                                borderColor = EmeraldBorder,
                                onClick = onSaveBattle,
                                testTag = "save_battle_button"
                            )
                        }
                        HudChipButton(
                            label = if (isCoopMode) "Lobby" else "Setup",
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
                        val pPal = factionPalette(playerFaction)
                        val ePal = factionPalette(enemyFaction)
                        Text(
                            text = "${playerFaction.displayName.uppercase()}: $playerAliveModels",
                            style = MaterialTheme.typography.labelSmall,
                            color = pPal.headerText
                        )
                        Text("|", style = MaterialTheme.typography.labelSmall, color = pal.trimMuted)
                        Text(
                            text = "${enemyFaction.displayName.uppercase()}: $enemyAliveModels",
                            style = MaterialTheme.typography.labelSmall,
                            color = ePal.headerText
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TargetPreviewBanner(
    preview: TargetPreview,
    canChooseDarkPact: Boolean,
    leadershipNeeded: Int,
    selectedDarkPact: DarkPactChoice,
    onSelectDarkPact: (DarkPactChoice) -> Unit,
    onStrike: () -> Unit
) {
    val tagBg = when (preview.coverTag) {
        CoverTag.OPEN -> Color(0xFF1F232B)
        CoverTag.COVER -> Color(0xFF3E3012)
    }
    val tagFg = when (preview.coverTag) {
        CoverTag.OPEN -> TextMuted
        CoverTag.COVER -> WarmAmber
    }
    val isSporeDetonate = preview.weaponName.contains("Destruct") || preview.saveRollNeeded > 6

    GrimdarkBevelPanel(
        cut = 8.dp,
        borderColor = CrimsonBright,
        topColor = Color(0xF5211014),
        bottomColor = Color(0xF511090C),
        accentBarColor = ImperialGold,
        modifier = Modifier.widthIn(max = 460.dp)
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
                val hitSkillLabel = if (preview.isMelee) "WS" else "BS"
                val apText = if (preview.weaponAp == 0) "AP 0" else "AP -${preview.weaponAp}"
                val saveText = when {
                    preview.saveRollNeeded > 6 -> "NO SV"
                    preview.usingInvuln -> "INV ${preview.saveRollNeeded}++"
                    else -> "SV ${preview.saveRollNeeded}+"
                }
                Text(
                    text = "WEAPON: ${preview.weaponName.uppercase()} (${if (preview.isMelee) "MELEE" else "RANGED"})",
                    style = MaterialTheme.typography.labelSmall,
                    color = ImperialGold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (preview.markedBonusApplied || preview.sustainedHits > 0 || preview.lethalHits || preview.rerollWoundOnes) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        if (preview.markedBonusApplied) {
                            Text(
                                text = "◎ MARKED (+1 HIT)",
                                style = MaterialTheme.typography.labelSmall,
                                color = WarmAmber,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (preview.rerollWoundOnes) {
                            Text(
                                text = "[REROLL WOUND 1s]",
                                style = MaterialTheme.typography.labelSmall,
                                color = ImperialGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (preview.lethalHits) {
                            Text(
                                text = "[LETHAL HITS]",
                                style = MaterialTheme.typography.labelSmall,
                                color = WarmAmber,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (preview.sustainedHits > 0) {
                            Text(
                                text = "[SUSTAINED HITS ${preview.sustainedHits}]",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldBuff,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Text(
                    text = "$hitSkillLabel ${preview.hitRollNeeded}+ (${preview.hitChance}%) • S${preview.weaponStrength} vs T${preview.targetToughness} (${preview.woundRollNeeded}+) • $apText • $saveText",
                    style = MaterialTheme.typography.labelSmall,
                    color = TacticalCyan,
                    maxLines = 2
                )
                Text(
                    text = if (isSporeDetonate) {
                        "💥 1D6/MODEL • <3 = 1W • EST: ${preview.expectedTotalDamage}W"
                    } else {
                        "${preview.attacksCount}x ATK • EST DMG: ${preview.expectedTotalDamage}W"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = WarmAmber
                )
                if (canChooseDarkPact) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "DARK PACT (OPTIONAL) • LD $leadershipNeeded+ (FAIL: D3 MW)",
                        style = MaterialTheme.typography.labelSmall,
                        color = ImperialGold
                    )
                    Spacer(Modifier.height(3.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        DarkPactChoice.entries.forEach { choice ->
                            val isSelected = selectedDarkPact == choice
                            val chipColor = when (choice) {
                                DarkPactChoice.NONE -> TextMuted
                                DarkPactChoice.LETHAL_HITS -> WarmAmber
                                DarkPactChoice.SUSTAINED_HITS_1 -> EmeraldBuff
                            }
                            val chipBg = if (isSelected) {
                                when (choice) {
                                    DarkPactChoice.NONE -> PanelIronTop
                                    DarkPactChoice.LETHAL_HITS -> Color(0xFF3A1A0AL)
                                    DarkPactChoice.SUSTAINED_HITS_1 -> EmeraldSurface
                                }
                            } else InputDark
                            val chipShape = CutCornerShape(4.dp)
                            Box(
                                modifier = Modifier
                                    .height(32.dp)
                                    .clip(chipShape)
                                    .background(chipBg)
                                    .border(
                                        width = if (isSelected) 1.3.dp else 0.8.dp,
                                        color = if (isSelected) chipColor else BorderSteel,
                                        shape = chipShape
                                    )
                                    .clickable { onSelectDarkPact(choice) }
                                    .padding(horizontal = 7.dp)
                                    .testTag("dark_pact_${choice.name.lowercase()}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = choice.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) chipColor else TextMuted,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
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
                Text(
                    text = if (isSporeDetonate) "DETONATE" else stringResource(R.string.btn_strike),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UnitStatusCard(
    unit: BattleUnit,
    isEngagedInMelee: Boolean,
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
                    if (unit.ownerPlayerName.isNotBlank()) {
                        Text(
                            text = "[${unit.ownerPlayerName.uppercase()}]",
                            style = MaterialTheme.typography.labelSmall,
                            color = TacticalCyan,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
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
                ProgressStatBar(
                    label = "WOUNDS (W)",
                    valueText = "${unit.totalWounds}/${unit.maxTotalWounds}",
                    progress = (unit.totalWounds.toFloat() / unit.maxTotalWounds.coerceAtLeast(1)).coerceIn(0f, 1f),
                    barColor = CrimsonBright,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(6.dp))
                Text(
                    text = "UNIT PROFILE",
                    style = MaterialTheme.typography.labelSmall,
                    color = pal.trimBorder
                )
                Spacer(Modifier.height(3.dp))
                val svLabel = "${unit.template.save}+" + (unit.template.invulnSave?.let { "/${it}++" } ?: "")
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    StatPill("M", "${"%.1f".format(unit.moveCmLeft)}cm", TacticalCyan)
                    StatPill("T", "${unit.template.toughness}", TextPrimary)
                    StatPill("SV", svLabel, if (unit.template.invulnSave != null) TacticalCyan else TextPrimary)
                    StatPill("W", "${unit.template.woundsPerModel}", TextPrimary)
                    StatPill("LD", "${unit.template.leadership}+", TextPrimary)
                    StatPill("OC", "${unit.template.objectiveControl}", TextPrimary)
                    StatPill("FACE", unit.facing.label, pal.trimBorder)
                    if (unit.hasRangedProfile) {
                        val shootLabel = when {
                            unit.hasCharged -> "LOCKED"
                            unit.hasAdvanced && !unit.template.mainWeaponAssault -> "NO ADV"
                            unit.hasShot -> "DONE"
                            else -> "READY"
                        }
                        val shootCol = if (shootLabel == "READY") EmeraldBuff else TextMuted
                        StatPill("SHOOT", shootLabel, shootCol)
                    }
                    if (unit.hasMeleeProfile) {
                        StatPill("MELEE", if (unit.hasFought) "DONE" else "READY", if (unit.hasFought) TextMuted else CrimsonSoft)
                    }
                    if (unit.hasAdvanced) {
                        StatPill("ADV", "ON", EmeraldBuff)
                    }
                    if (unit.hasCharged) {
                        StatPill("CHRG", "ON", WarmAmber)
                    }
                    if (isEngagedInMelee) {
                        StatPill("DUEL", "MELEE LOCK", CrimsonBright)
                    }
                    if (unit.isCloaked) {
                        StatPill("STEALTH", "CLOAKED 1T", TacticalCyan)
                    }
                    if (unit.isMarked) {
                        StatPill("MARKED", "+1 RANGED HIT", WarmAmber)
                    }
                    if (unit.legionaryWoundRerollBuffActive) {
                        StatPill("VETERANS", "REROLL W1 (1T)", WarmAmber)
                    }
                }

                HorizontalDivider(color = DividerIron, modifier = Modifier.padding(vertical = 5.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = unit.template.mainWeaponName.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = WarmAmber,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (unit.template.mainWeaponAssault) {
                        val tagShape = remember { CutCornerShape(3.dp) }
                        Text(
                            text = "ASSAULT",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldBuff,
                            modifier = Modifier
                                .clip(tagShape)
                                .background(EmeraldSurface)
                                .border(0.8.dp, EmeraldBorder, tagShape)
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                    if (unit.template.mainWeaponPistol) {
                        val tagShape = remember { CutCornerShape(3.dp) }
                        Text(
                            text = "PISTOL",
                            style = MaterialTheme.typography.labelSmall,
                            color = TacticalCyan,
                            modifier = Modifier
                                .clip(tagShape)
                                .background(TacticalCyan.copy(alpha = 0.16f))
                                .border(0.8.dp, TacticalCyan.copy(alpha = 0.55f), tagShape)
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                    if (unit.template.mainSustainedHits > 0) {
                        val tagShape = remember { CutCornerShape(3.dp) }
                        Text(
                            text = "SUSTAINED ${unit.template.mainSustainedHits}",
                            style = MaterialTheme.typography.labelSmall,
                            color = WarmAmber,
                            modifier = Modifier
                                .clip(tagShape)
                                .background(InputDark)
                                .border(0.8.dp, WarmAmber.copy(alpha = 0.65f), tagShape)
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }
                Spacer(Modifier.height(3.dp))
                val effectiveAp = unit.effectiveMainArmorPen
                val apLabel = if (effectiveAp == 0) "0" else "-$effectiveAp"
                val meleeChargeGlow = unit.meleeChargeBuffActive && !unit.template.isRangedMain
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    StatPill("RNG", if (unit.template.isRangedMain) "${unit.template.range}" else "2 cm", TextPrimary)
                    StatPill("A", "${unit.template.attacksPerModel}", TextPrimary)
                    if (unit.template.isRangedMain) {
                        StatPill("BS", "${unit.effectiveBallisticSkill}+", TextPrimary)
                    } else {
                        StatPill("WS", "${unit.effectiveWeaponSkill}+", TextPrimary)
                    }
                    StatPill("S", "${unit.effectiveMainStrength}", if (meleeChargeGlow) WarmAmber else TextPrimary)
                    StatPill("AP", apLabel, if (meleeChargeGlow) WarmAmber else TextPrimary)
                    StatPill("D", "${unit.effectiveDamage}", TextPrimary)
                }

                if (unit.template.isRangedMain) {
                    Spacer(Modifier.height(5.dp))
                    val ccAp = unit.template.effectiveMeleeArmorPen
                    val ccApLabel = if (ccAp == 0) "0" else "-$ccAp"
                    val ccDmg = unit.template.effectiveMeleeDamage + if (unit.buffTurnsLeft > 0) 1 else 0
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${unit.template.effectiveMeleeWeaponName.uppercase()} [MELEE]",
                            style = MaterialTheme.typography.labelSmall,
                            color = CrimsonSoft,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (unit.template.effectiveMeleeSustainedHits > 0) {
                            val tagShape = remember { CutCornerShape(3.dp) }
                            Text(
                                text = "SUSTAINED ${unit.template.effectiveMeleeSustainedHits}",
                                style = MaterialTheme.typography.labelSmall,
                                color = WarmAmber,
                                modifier = Modifier
                                    .clip(tagShape)
                                    .background(InputDark)
                                    .border(0.8.dp, WarmAmber.copy(alpha = 0.65f), tagShape)
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        StatPill("RNG", "2 cm", TextPrimary)
                        StatPill("A", "${unit.template.effectiveMeleeAttacks}", TextPrimary)
                        StatPill("WS", "${unit.effectiveMeleeWeaponSkill}+", TextPrimary)
                        StatPill("S", "${unit.template.effectiveMeleeStrength}", TextPrimary)
                        StatPill("AP", ccApLabel, TextPrimary)
                        StatPill("D", "$ccDmg", TextPrimary)
                    }
                }

                if (unit.template.hasPistol) {
                    Spacer(Modifier.height(5.dp))
                    val pistolAp = if (unit.template.pistolArmorPen == 0) "0" else "-${unit.template.pistolArmorPen}"
                    val pSusText = if (unit.template.pistolSustainedHits > 0) " [SUSTAINED ${unit.template.pistolSustainedHits}]" else ""
                    Text(
                        text = "${unit.template.pistolWeaponName.uppercase()} [PISTOL]$pSusText",
                        style = MaterialTheme.typography.labelSmall,
                        color = TacticalCyan,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        StatPill("RNG", "${unit.template.pistolRange}", TextPrimary)
                        StatPill("A", "${unit.template.pistolAttacks}", TextPrimary)
                        StatPill("BS", "${unit.effectiveBallisticSkill}+", TextPrimary)
                        StatPill("S", "${unit.template.pistolStrength}", TextPrimary)
                        StatPill("AP", pistolAp, TextPrimary)
                        StatPill("D", "${unit.template.pistolDamage}", TextPrimary)
                    }
                }

                if (unit.activeBuffLabel != null || unit.auraBuffed) {
                    Spacer(Modifier.height(4.dp))
                    val buffText = listOfNotNull(
                        unit.activeBuffLabel,
                        if (unit.auraBuffed) "AURA +1 HIT" else null
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
private fun ChargePreviewBanner(
    preview: ChargePreview,
    onConfirmCharge: () -> Unit,
    onCancelCharge: () -> Unit
) {
    GrimdarkBevelPanel(
        cut = 8.dp,
        borderColor = WarmAmber,
        topColor = Color(0xF5261309),
        bottomColor = Color(0xF5140A05),
        accentBarColor = CrimsonBright,
        modifier = Modifier.widthIn(max = 400.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = "CHARGE: ${preview.targetName.uppercase()}",
                    style = MaterialTheme.typography.titleSmall,
                    color = WarmAmber,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "DIST ${"%.1f".format(preview.distanceCm)} cm • NEED ${preview.requiredRoll}+ ON 2D6",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextPrimary,
                    maxLines = 1
                )
            }

            Button(
                onClick = onConfirmCharge,
                shape = grimdarkBevelShape(6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CrimsonDeep,
                    contentColor = TextPrimary
                ),
                border = BorderStroke(1.2.dp, WarmAmber),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier
                    .height(44.dp)
                    .testTag("confirm_charge_button")
            ) {
                Icon(Icons.Default.DoubleArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("CHARGE (2D6)", style = MaterialTheme.typography.labelMedium, maxLines = 1, softWrap = false)
            }

            OutlinedButton(
                onClick = onCancelCharge,
                shape = grimdarkBevelShape(6.dp),
                border = BorderStroke(1.dp, BorderSteel),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier
                    .height(44.dp)
                    .testTag("cancel_charge_button")
            ) {
                Icon(Icons.Default.Close, contentDescription = "Cancel Charge", tint = TextPrimary, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun MovePreviewBanner(
    preview: MovePreview,
    remainingAfterCm: Float,
    onConfirmMove: () -> Unit,
    onCancelMove: () -> Unit
) {
    GrimdarkBevelPanel(
        cut = 8.dp,
        borderColor = TacticalCyan,
        topColor = Color(0xF50B1822),
        bottomColor = Color(0xF5070E14),
        accentBarColor = TacticalCyan,
        modifier = Modifier.widthIn(max = 380.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = "MOVE ${"%.1f".format(preview.distanceCm)} cm",
                    style = MaterialTheme.typography.titleSmall,
                    color = TacticalCyan,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = "Remaining: ${"%.1f".format(remainingAfterCm)} cm",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    maxLines = 1,
                    softWrap = false
                )
            }

            Button(
                onClick = onConfirmMove,
                shape = grimdarkBevelShape(6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldSurface,
                    contentColor = TextPrimary
                ),
                border = BorderStroke(1.2.dp, EmeraldBorder),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier
                    .height(44.dp)
                    .testTag("confirm_move_cm_button")
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("CONFIRM", style = MaterialTheme.typography.labelMedium, maxLines = 1, softWrap = false)
            }

            OutlinedButton(
                onClick = onCancelMove,
                shape = grimdarkBevelShape(6.dp),
                border = BorderStroke(1.dp, BorderSteel),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier
                    .height(44.dp)
                    .testTag("cancel_move_cm_button")
            ) {
                Icon(Icons.Default.Close, contentDescription = "Cancel Move", tint = TextPrimary, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun CmMovementControlBar(
    unit: BattleUnit,
    cmStepChoice: Float,
    onSetCmStep: (Float) -> Unit,
    onWalkForwardCm: () -> Unit
) {
    val pal = factionPalette(unit.faction)
    val canStep = unit.moveCmLeft >= 0.25f
    val stepLabel = if (cmStepChoice <= 0f) "MAX" else "${cmStepChoice.toInt()}cm"

    GrimdarkBevelPanel(
        cut = 7.dp,
        borderColor = TacticalCyan.copy(alpha = 0.85f),
        topColor = pal.panelTop,
        bottomColor = pal.panelBottom,
        accentBarColor = TacticalCyan,
        showRivets = false
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = "REM ${"%.1f".format(unit.moveCmLeft)} cm",
                style = MaterialTheme.typography.labelMedium,
                color = TacticalCyan,
                maxLines = 1,
                softWrap = false
            )

            CmStepChip("1cm", cmStepChoice == 1f, { onSetCmStep(1f) }, "cm_step_1")
            CmStepChip("3cm", cmStepChoice == 3f, { onSetCmStep(3f) }, "cm_step_3")
            CmStepChip("5cm", cmStepChoice == 5f, { onSetCmStep(5f) }, "cm_step_5")
            CmStepChip("MAX", cmStepChoice <= 0f, { onSetCmStep(0f) }, "cm_step_max")

            Button(
                onClick = onWalkForwardCm,
                enabled = canStep,
                shape = grimdarkBevelShape(4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ColorTokens.CyanSurface,
                    contentColor = TextPrimary
                ),
                border = BorderStroke(1.dp, TacticalCyan),
                contentPadding = PaddingValues(horizontal = 9.dp, vertical = 4.dp),
                modifier = Modifier
                    .height(34.dp)
                    .testTag("walk_forward_cm_button")
            ) {
                Text("STEP +$stepLabel", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
            }
        }
    }
}

@Composable
private fun CmStepChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val shape = remember { CutCornerShape(4.dp) }
    Box(
        modifier = Modifier
            .height(32.dp)
            .clip(shape)
            .background(if (selected) ColorTokens.CyanSurface else InputDark)
            .border(1.dp, if (selected) TacticalCyan else BorderSteel, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 7.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) TacticalCyan else TextMuted,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun ActionDock(
    selected: BattleUnit?,
    canCommandSelected: Boolean,
    allUnits: List<BattleUnit>,
    activeFaction: Faction,
    isPlayerTurn: Boolean,
    weaponMode: WeaponMode,
    moveModeActive: Boolean,
    chargeModeActive: Boolean,
    onToggleMoveMode: () -> Unit,
    onAdvanceMove: () -> Unit,
    onToggleChargeMode: () -> Unit,
    onRotateUnit: (Boolean) -> Unit,
    onFightFirst: () -> Unit,
    onSelectWeaponMode: (WeaponMode) -> Unit,
    onEndTurn: () -> Unit,
    onInspectSkill: (UnitTemplate) -> Unit
) {
    val enabled = isPlayerTurn && canCommandSelected && selected != null
    val isEngaged = selected != null && TacticalRules.isEngagedInMelee(selected, allUnits)
    val pal = factionPalette(selected?.faction ?: activeFaction)
    var dockExpanded by remember { mutableStateOf(true) }

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        GrimdarkBevelPanel(
            cut = 5.dp,
            borderColor = pal.trimBorder,
            topColor = pal.panelTop,
            bottomColor = pal.panelBottom,
            showRivets = false,
            modifier = Modifier
                .widthIn(max = 315.dp)
                .clickable { dockExpanded = !dockExpanded }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selected != null) "⚡ ${selected.template.name.uppercase()} ACTIONS" else "⚡ COMMAND DOCK",
                    style = MaterialTheme.typography.labelSmall,
                    color = pal.headerText,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = if (dockExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (dockExpanded) "Collapse" else "Expand",
                    tint = pal.headerText,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        AnimatedVisibility(visible = dockExpanded) {
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
                            text = "💡 ${selected.template.passiveName}: ${selected.template.passiveDesc}",
                            style = MaterialTheme.typography.labelSmall,
                            color = pal.headerText,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

        // Row 1: Manoeuvre & Phase Actions (Turn, Move, Advance 1D6, Charge 2D6)
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ActionDockButton(
                title = "Turn L",
                subtitle = "45°",
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
                subtitle = "45°",
                icon = Icons.AutoMirrored.Filled.RotateRight,
                containerTop = PanelIronTop,
                containerBottom = PanelIronBottom,
                borderColor = BorderSteel,
                textColor = TextPrimary,
                enabled = enabled,
                onClick = { onRotateUnit(false) },
                testTag = "rotate_right_button"
            )

            if (selected != null) {
                val canMove = enabled && !isEngaged && selected.moveCmLeft >= 0.25f
                ActionDockButton(
                    title = "Move",
                    subtitle = if (isEngaged) "DUEL LOCK" else "${"%.1f".format(selected.moveCmLeft)}cm",
                    icon = Icons.Default.OpenWith,
                    containerTop = if (moveModeActive && !isEngaged) ColorTokens.CyanSurface else PanelIronTop,
                    containerBottom = PanelIronBottom,
                    borderColor = if (isEngaged) CrimsonDeep else if (moveModeActive) TacticalCyan else BorderSteel,
                    textColor = if (isEngaged) CrimsonSoft else if (moveModeActive) TacticalCyan else TextPrimary,
                    enabled = canMove,
                    onClick = onToggleMoveMode,
                    testTag = "move_mode_button"
                )

                val advanceSub = when {
                    isEngaged -> "DUEL LOCK"
                    selected.hasAdvanced -> "DONE"
                    !selected.canAdvance -> "LOCKED"
                    else -> "1D6 CM"
                }
                ActionDockButton(
                    title = "Advance",
                    subtitle = advanceSub,
                    icon = Icons.AutoMirrored.Filled.DirectionsRun,
                    containerTop = if (selected.hasAdvanced) EmeraldSurface else PanelIronTop,
                    containerBottom = PanelIronBottom,
                    borderColor = if (isEngaged) CrimsonDeep else if (selected.hasAdvanced) EmeraldBorder else BorderSteel,
                    textColor = if (isEngaged) CrimsonSoft else if (selected.hasAdvanced) EmeraldBuff else TextPrimary,
                    enabled = enabled && !isEngaged && selected.canAdvance,
                    onClick = onAdvanceMove,
                    testTag = "advance_button"
                )

                val chargeSub = when {
                    isEngaged -> "IN DUEL"
                    selected.hasAdvanced -> "NO ADV"
                    selected.hasCharged -> "CHARGED"
                    selected.hasFought -> "FOUGHT"
                    chargeModeActive -> "TAP FOE"
                    else -> "2D6"
                }
                ActionDockButton(
                    title = "Charge",
                    subtitle = chargeSub,
                    icon = Icons.Default.DoubleArrow,
                    containerTop = if (chargeModeActive && !isEngaged) CrimsonDeep else PanelIronTop,
                    containerBottom = PanelIronBottom,
                    borderColor = if (isEngaged) CrimsonDeep else if (chargeModeActive) WarmAmber else BorderSteel,
                    textColor = if (isEngaged) CrimsonSoft else if (chargeModeActive) WarmAmber else TextPrimary,
                    enabled = enabled && !isEngaged && selected.canCharge,
                    onClick = onToggleChargeMode,
                    testTag = "charge_button"
                )
            }
        }

        // Row 2: Combat Modes & End Turn (Pistol shown ONLY when unit has a pistol)
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selected != null && selected.template.hasPistol) {
                val pistolReady = enabled && TacticalRules.canUseMode(selected, WeaponMode.PISTOL, allUnits)
                val isOn = weaponMode == WeaponMode.PISTOL
                val pistolSubtitle = when {
                    selected.hasAdvanced -> "NO ADV"
                    selected.hasCharged -> "MELEE"
                    selected.pistolCooldownLeft > 0 -> "CD ${selected.pistolCooldownLeft}"
                    selected.hasShot -> "SHOT"
                    else -> "SHOOT"
                }
                ActionDockButton(
                    title = "Pistol",
                    subtitle = pistolSubtitle,
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

            if (selected != null && selected.template.archetype == UnitArchetype.VON_RYANS_LEAPER) {
                val hasAdjEnemy = allUnits.any {
                    it.isAlive && !it.isCloaked && it.faction != selected.faction &&
                        TacticalRules.isWithinMeleeReach(selected, it)
                }
                ActionDockButton(
                    title = "Fight 1st",
                    subtitle = when {
                        selected.hasFought -> "FOUGHT"
                        !hasAdjEnemy -> "NO FOE"
                        else -> "FIRST"
                    },
                    icon = null,
                    containerTop = CrimsonDeep,
                    containerBottom = PanelIronBottom,
                    borderColor = ImperialGold,
                    textColor = WarmAmber,
                    enabled = enabled && !selected.hasFought && hasAdjEnemy,
                    onClick = onFightFirst,
                    testTag = "fight_first_button"
                )
            }

            if (selected != null) {
                val isTerminator = selected.template.archetype == UnitArchetype.TERMINATOR
                val isBiovore = selected.template.archetype == UnitArchetype.BIOVORE
                val isSporeMine = selected.template.archetype == UnitArchetype.SPORE_MINE
                val isLeaper = selected.template.archetype == UnitArchetype.VON_RYANS_LEAPER
                val isAssaultMarine = selected.template.archetype == UnitArchetype.ASSAULT_MARINE
                val isBladeguard = selected.template.archetype == UnitArchetype.BLADEGUARD
                val isLegionary = selected.template.archetype == UnitArchetype.LEGIONARY
                val isMeleeSkill = TacticalRules.modeUsesMelee(selected, WeaponMode.SKILL)
                val isUtilitySkill = isTerminator || isBiovore || isSporeMine || selected.template.archetype == UnitArchetype.HORMAGAUNT || isLeaper || isAssaultMarine || isBladeguard || isLegionary
                val skillReady = enabled && TacticalRules.canUseMode(selected, WeaponMode.SKILL, allUnits)
                val isSkillOn = weaponMode == WeaponMode.SKILL
                val isBuffActive = selected.isCloaked || selected.meleeChargeBuffActive || selected.bladeguardRerollBuffActive || selected.legionaryWoundRerollBuffActive
                val skillSubtitle = when {
                    isBuffActive -> "ACTIVE 1T"
                    selected.skillCooldownLeft > 0 -> "CD ${selected.skillCooldownLeft}"
                    isBiovore && selected.hasShot -> "SEEDED"
                    isBiovore && isEngaged -> "IN MELEE"
                    isBiovore && selected.hasAdvanced -> "NO ADV"
                    isBiovore -> "SEED MINE"
                    isSporeMine -> "DETONATE"
                    isAssaultMarine && selected.hasFought -> "FOUGHT"
                    isAssaultMarine && selected.hasAdvanced -> "NO ADV"
                    isAssaultMarine && (!selected.hasCharged || !isEngaged) -> "NEED CHG"
                    isAssaultMarine -> "+1S +1AP"
                    isBladeguard -> "REROLL 1s"
                    isLegionary -> "REROLL W1"
                    selected.hasCharged && !isMeleeSkill -> "MELEE"
                    isEngaged && !isMeleeSkill && !isLeaper && (isTerminator || !selected.template.mainWeaponPistol) -> "IN MELEE"
                    selected.hasAdvanced && !selected.template.mainWeaponAssault && !isUtilitySkill && !isMeleeSkill -> "NO ADV"
                    selected.hasAdvanced && (isTerminator || isMeleeSkill) -> "NO ADV"
                    isTerminator -> "DEEP STRIKE"
                    isLeaper -> "CLOAK 1T"
                    !skillReady && !isUtilitySkill -> if (isMeleeSkill) "FOUGHT" else "SHOT"
                    else -> "READY"
                }
                ActionDockButton(
                    title = if (isTerminator) "Teleport" else if (isBiovore) "Seed Mine" else if (isSporeMine) "Detonate" else "Skill",
                    subtitle = skillSubtitle,
                    icon = null,
                    containerTop = if (isSkillOn || isBuffActive) ColorTokens.CyanSurface else ColorTokens.SkillSurface,
                    containerBottom = PanelIronBottom,
                    borderColor = if (isSkillOn || isBuffActive) TacticalCyan else ColorTokens.SkillBorder,
                    textColor = if (isBuffActive) TacticalCyan else WarmAmber,
                    enabled = true,
                    onClick = {
                        if (skillReady) {
                            onSelectWeaponMode(WeaponMode.SKILL)
                        } else {
                            onInspectSkill(selected.template)
                        }
                    },
                    onLongClick = {
                        onInspectSkill(selected.template)
                    },
                    testTag = "skill_button"
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
    onLongClick: (() -> Unit)? = null,
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
            .pointerInput(enabled, onLongClick) {
                detectTapGestures(
                    onTap = { if (enabled) onClick() },
                    onLongPress = { onLongClick?.invoke() }
                )
            }
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
                text = if (splash.round <= 0) {
                    "DEPLOYMENT PHASE • ${splash.activeFaction.badgeTag}"
                } else {
                    "ROUND ${splash.round} • ${splash.activeFaction.badgeTag}"
                },
                style = MaterialTheme.typography.labelSmall,
                color = pal.trimBorder
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = splash.commanderLabel.ifEmpty {
                    if (splash.isPlayerTurn) "COMMANDER TURN" else "${splash.activeFaction.displayName.uppercase()} ACTIVATION"
                },
                style = MaterialTheme.typography.titleMedium,
                color = pal.headerText
            )
            HorizontalDivider(color = DividerIron, modifier = Modifier.padding(vertical = 6.dp))
            val pShort = when (splash.playerFaction) {
                Faction.MARINES -> "MARINES"
                Faction.BLACK_LEGION -> "LEGION"
                Faction.XENOS -> "TYRANIDS"
            }
            val eShort = when (splash.enemyFaction) {
                Faction.MARINES -> "MARINES"
                Faction.BLACK_LEGION -> "LEGION"
                Faction.XENOS -> "TYRANIDS"
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SplashStatBox(pShort, "${splash.playerModelsAlive}", factionPalette(splash.playerFaction).headerText, Modifier.weight(1f))
                SplashStatBox(eShort, "${splash.enemyModelsAlive}", factionPalette(splash.enemyFaction).headerText, Modifier.weight(1f))
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
                    text = "${summary.winnerLabel.ifBlank { summary.winner.displayName }} prevailed in ${summary.rounds} rounds.\n" +
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

@Composable
private fun SkillInfoDialog(
    template: UnitTemplate,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PanelIronTop,
        titleContentColor = TextPrimary,
        textContentColor = TextMuted,
        title = {
            Column {
                Text(
                    text = template.skillName.uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = WarmAmber
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${template.name} • Cooldown: ${template.skillCooldown} Turns",
                    style = MaterialTheme.typography.labelSmall,
                    color = TacticalCyan
                )
            }
        },
        text = {
            Column {
                HorizontalDivider(color = DividerIron, modifier = Modifier.padding(vertical = 4.dp))
                Spacer(Modifier.height(4.dp))
                Text(
                    text = template.skillDesc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = grimdarkBevelShape(6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ColorTokens.CyanSurface, contentColor = TextPrimary),
                border = BorderStroke(1.dp, TacticalCyan),
                modifier = Modifier.testTag("close_skill_info_button")
            ) {
                Text("CLOSE")
            }
        }
    )
}

@Composable
private fun DeploymentPhaseBanner(
    turnPlayerName: String,
    teamIndex: Int,
    isLocalTurn: Boolean,
    remainingReserves: Int,
    modifier: Modifier = Modifier
) {
    val teamAccent = if (teamIndex == 0) TacticalCyan else CrimsonSoft
    val zoneLabel = if (teamIndex == 0) "TEAM 1 • WEST 3-TILE" else "TEAM 2 • EAST 3-TILE"
    GrimdarkBevelPanel(
        cut = 6.dp,
        borderColor = if (isLocalTurn) ImperialGold else teamAccent,
        topColor = PanelIronTop,
        bottomColor = VoidObsidian,
        accentBarColor = teamAccent,
        showRivets = false,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "DEPLOY • $zoneLabel",
                style = MaterialTheme.typography.labelSmall,
                color = teamAccent,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = "•",
                style = MaterialTheme.typography.labelSmall,
                color = DividerIron
            )
            Text(
                text = if (isLocalTurn) {
                    "YOUR TURN (${turnPlayerName.uppercase()}) — TAP HIGHLIGHTED BORDER TO PLACE ($remainingReserves LEFT)"
                } else {
                    "WAITING FOR ${turnPlayerName.uppercase()} TO DEPLOY ($remainingReserves LEFT)"
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (isLocalTurn) ImperialGold else TextMuted,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DeploymentTrayBar(
    state: BattleUiState,
    onSelectReserveUnit: (String) -> Unit
) {
    val activeOwnerId = state.deploymentTurnPlayerId
    val isLocalTurn = state.isPlayerTurn
    val trayUnits = remember(state.reserveUnits, activeOwnerId, state.localControlledPlayerId, isLocalTurn) {
        if (isLocalTurn && activeOwnerId != null) {
            state.reserveUnits.filter { it.ownerPlayerId == activeOwnerId }
        } else {
            val localId = state.localControlledPlayerId
            val mine = if (localId != null) state.reserveUnits.filter { it.ownerPlayerId == localId } else emptyList()
            mine.ifEmpty { state.reserveUnits.filter { it.ownerPlayerId == activeOwnerId } }
        }
    }
    val teamAccent = if (state.deploymentTeamIndex == 0) TacticalCyan else CrimsonSoft

    GrimdarkBevelPanel(
        cut = 8.dp,
        borderColor = if (isLocalTurn) ImperialGold else BorderSteel,
        topColor = PanelIronTop,
        bottomColor = VoidObsidian,
        accentBarColor = teamAccent,
        showRivets = false,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("deployment_tray_bar")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isLocalTurn) {
                        "RESERVE TRAY — SELECT SQUAD & TAP ${if (state.deploymentTeamIndex == 0) "WEST" else "EAST"} 3-TILE BORDER"
                    } else {
                        "RESERVE TRAY — ${state.deploymentTurnPlayerName.uppercase()} IS DEPLOYING..."
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isLocalTurn) ImperialGold else TextMuted,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${state.reserveUnits.size} UNPLACED",
                    style = MaterialTheme.typography.labelSmall,
                    color = teamAccent,
                    fontWeight = FontWeight.Bold
                )
            }

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(trayUnits, key = { it.uid }) { unit ->
                    val isSelected = unit.uid == state.selectedReserveUnitUid && isLocalTurn
                    val pal = factionPalette(unit.faction)
                    Column(
                        modifier = Modifier
                            .widthIn(min = 148.dp, max = 188.dp)
                            .clip(CutCornerShape(6.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = if (isSelected) {
                                        listOf(pal.panelTop, PanelIronBottom)
                                    } else {
                                        listOf(InputDark, VoidObsidian)
                                    }
                                )
                            )
                            .border(
                                width = if (isSelected) 1.8.dp else 1.dp,
                                color = if (isSelected) ImperialGold else pal.trimBorder.copy(alpha = 0.6f),
                                shape = CutCornerShape(6.dp)
                            )
                            .clickable(enabled = isLocalTurn) { onSelectReserveUnit(unit.uid) }
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                            .testTag("reserve_card_${unit.uid}"),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = unit.template.name.uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) ImperialGold else pal.headerText,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "${unit.template.points}PT",
                                style = MaterialTheme.typography.labelSmall,
                                color = ImperialGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "${unit.template.squadSize}x ${unit.template.woundsPerModel}W • M${unit.template.moveCm} • RNG ${unit.template.range}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isSelected) "READY TO PLACE" else unit.ownerPlayerName.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) EmeraldBuff else teamAccent,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                }
            }
        }
    }
}
}
