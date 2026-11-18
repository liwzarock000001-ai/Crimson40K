package com.example.ui.setup

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.Faction
import com.example.model.RosterSlot
import com.example.model.UnitTemplate
import com.example.ui.theme.BorderSteel
import com.example.ui.theme.CardSlate
import com.example.ui.theme.ColorTokens
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
import com.example.ui.theme.PanelIronBottom
import com.example.ui.theme.PanelIronTop
import com.example.ui.theme.RowSlate
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.UltramarineSilver
import com.example.ui.theme.UltramarineSilverBright
import com.example.ui.theme.VoidObsidian
import com.example.ui.theme.WarmAmber
import com.example.ui.theme.XenosBorder
import com.example.ui.theme.XenosSurface
import com.example.ui.theme.XenosText
import com.example.ui.theme.XenosViolet
import com.example.ui.theme.factionPalette
import com.example.ui.theme.grimdarkBevelShape
import com.example.viewmodel.SetupUiState

@Composable
fun SetupScreen(
    state: SetupUiState,
    onBackToMenu: () -> Unit,
    onToggleSide: () -> Unit,
    onPointLimitChange: (Int) -> Unit,
    onSave: () -> Unit,
    onReset: () -> Unit,
    onRandomize: (Faction) -> Unit,
    onAddUnit: (Faction, String) -> Unit,
    onRemoveSlot: (Faction, String) -> Unit,
    onEditTemplate: (String) -> Unit,
    onCloseEditor: () -> Unit,
    onSaveTemplate: (UnitTemplate) -> Unit,
    onDeploy: () -> Unit
) {
    BackHandler { onBackToMenu() }
    val editingTemplate = state.editingTemplateId?.let { state.templates[it] }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF141820), VoidObsidian),
                    radius = 1250f
                )
            )
    ) {
        DriftingEmbersBackdrop(modifier = Modifier.fillMaxSize())

        Column(modifier = Modifier.fillMaxSize()) {
            SetupHeader(
                state = state,
                onBackToMenu = onBackToMenu,
                onToggleSide = onToggleSide,
                onPointLimitChange = onPointLimitChange,
                onSave = onSave,
                onReset = onReset,
                onDeploy = onDeploy
            )

            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                val isWide = maxWidth >= 700.dp
                val listState = rememberLazyListState()
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isWide) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                FactionSetupCard(
                                    faction = Faction.MARINES,
                                    isPlayer = state.playerFaction == Faction.MARINES,
                                    roster = state.marineRoster,
                                    templates = state.templates,
                                    totalPoints = state.marinePoints,
                                    pointLimit = state.pointLimit,
                                    onRandomize = { onRandomize(Faction.MARINES) },
                                    onAddUnit = { onAddUnit(Faction.MARINES, it) },
                                    onRemoveSlot = { onRemoveSlot(Faction.MARINES, it) },
                                    onEditTemplate = onEditTemplate,
                                    modifier = Modifier.weight(1f)
                                )
                                FactionSetupCard(
                                    faction = Faction.XENOS,
                                    isPlayer = state.playerFaction == Faction.XENOS,
                                    roster = state.xenosRoster,
                                    templates = state.templates,
                                    totalPoints = state.xenosPoints,
                                    pointLimit = state.pointLimit,
                                    onRandomize = { onRandomize(Faction.XENOS) },
                                    onAddUnit = { onAddUnit(Faction.XENOS, it) },
                                    onRemoveSlot = { onRemoveSlot(Faction.XENOS, it) },
                                    onEditTemplate = onEditTemplate,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    } else {
                        item {
                            FactionSetupCard(
                                faction = Faction.MARINES,
                                isPlayer = state.playerFaction == Faction.MARINES,
                                roster = state.marineRoster,
                                templates = state.templates,
                                totalPoints = state.marinePoints,
                                pointLimit = state.pointLimit,
                                onRandomize = { onRandomize(Faction.MARINES) },
                                onAddUnit = { onAddUnit(Faction.MARINES, it) },
                                onRemoveSlot = { onRemoveSlot(Faction.MARINES, it) },
                                onEditTemplate = onEditTemplate
                            )
                        }
                        item {
                            FactionSetupCard(
                                faction = Faction.XENOS,
                                isPlayer = state.playerFaction == Faction.XENOS,
                                roster = state.xenosRoster,
                                templates = state.templates,
                                totalPoints = state.xenosPoints,
                                pointLimit = state.pointLimit,
                                onRandomize = { onRandomize(Faction.XENOS) },
                                onAddUnit = { onAddUnit(Faction.XENOS, it) },
                                onRemoveSlot = { onRemoveSlot(Faction.XENOS, it) },
                                onEditTemplate = onEditTemplate
                            )
                        }
                    }
                }
            }

            SetupFooter(state = state)
        }
    }

    if (editingTemplate != null) {
        StatEditDialog(
            template = editingTemplate,
            onDismiss = onCloseEditor,
            onSave = onSaveTemplate
        )
    }
}

@Composable
private fun SetupHeader(
    state: SetupUiState,
    onBackToMenu: () -> Unit,
    onToggleSide: () -> Unit,
    onPointLimitChange: (Int) -> Unit,
    onSave: () -> Unit,
    onReset: () -> Unit,
    onDeploy: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(VoidObsidian)
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_setup_banner),
            contentDescription = "Crimson Sector Tactical Command Banner",
            contentScale = ContentScale.Crop,
            alpha = 0.22f,
            modifier = Modifier.matchParentSize()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            VoidObsidian.copy(alpha = 0.78f),
                            VoidObsidian.copy(alpha = 0.96f)
                        )
                    )
                )
        )
        val pal = factionPalette(state.playerFaction)
        val isMarines = state.playerFaction == Faction.MARINES
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .displayCutoutPadding()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onBackToMenu,
                    shape = grimdarkBevelShape(5.dp),
                    border = BorderStroke(1.dp, BorderSteel),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = PanelIronTop,
                        contentColor = TextPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("setup_back_menu_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Menu", modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("MENU", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
                }

                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = pal.trimBorder,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(R.string.setup_title).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = pal.headerText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onToggleSide,
                        shape = grimdarkBevelShape(5.dp),
                        border = BorderStroke(1.dp, pal.trimBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = pal.buttonSurface,
                            contentColor = pal.headerText
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("toggle_side_button")
                    ) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = "Switch Faction", modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "PLAY: ${state.playerFaction.displayName.uppercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    OutlinedButton(
                        onClick = onSave,
                        shape = grimdarkBevelShape(5.dp),
                        border = BorderStroke(1.dp, EmeraldBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = EmeraldSurface,
                            contentColor = EmeraldBuff
                        ),
                        contentPadding = PaddingValues(horizontal = 9.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("save_setup_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Save Stat", modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(stringResource(R.string.btn_save).uppercase(), style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
                    }

                    OutlinedButton(
                        onClick = onReset,
                        shape = grimdarkBevelShape(5.dp),
                        border = BorderStroke(1.dp, BorderSteel),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = PanelIronTop,
                            contentColor = TextMuted
                        ),
                        contentPadding = PaddingValues(horizontal = 9.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("reset_setup_button")
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = "Reset Roster", modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(stringResource(R.string.btn_reset).uppercase(), style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
                    }

                    Button(
                        onClick = onDeploy,
                        enabled = state.canDeploy,
                        shape = grimdarkBevelShape(5.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isMarines) MarineBright else XenosViolet,
                            contentColor = TextPrimary
                        ),
                        border = BorderStroke(1.dp, pal.trimBorder),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("deploy_top_button")
                    ) {
                        Text("DEPLOY", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
                        Spacer(Modifier.width(3.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                }
            }

            // Live Army Points Summary Display with Progress Bars at Top
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CutCornerShape(4.dp))
                    .background(InputDark.copy(alpha = 0.92f))
                    .border(1.dp, pal.trimMuted, CutCornerShape(4.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val marineOver = state.marinePoints > state.pointLimit
                val xenosOver = state.xenosPoints > state.pointLimit
                val marineLeft = (state.pointLimit - state.marinePoints).coerceAtLeast(0)
                val xenosLeft = (state.pointLimit - state.xenosPoints).coerceAtLeast(0)
                val marineRatio = (state.marinePoints.toFloat() / state.pointLimit.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                val xenosRatio = (state.xenosPoints.toFloat() / state.pointLimit.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("ULTRAMARINES: ${state.marinePoints} / ${state.pointLimit} PT", style = MaterialTheme.typography.labelSmall, color = MarineBright, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (marineOver) "OVER!" else "${marineLeft} PT LEFT",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (marineOver) CrimsonSoft else EmeraldBuff,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(CutCornerShape(2.dp))
                            .background(PanelIronBottom)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(marineRatio)
                                .background(if (marineOver) CrimsonSoft else MarineBright)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("TYRANIDS: ${state.xenosPoints} / ${state.pointLimit} PT", style = MaterialTheme.typography.labelSmall, color = XenosViolet, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (xenosOver) "OVER!" else "${xenosLeft} PT LEFT",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (xenosOver) CrimsonSoft else EmeraldBuff,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(CutCornerShape(2.dp))
                            .background(PanelIronBottom)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(xenosRatio)
                                .background(if (xenosOver) CrimsonSoft else XenosViolet)
                        )
                    }
                }
            }

            // Compact Beveled Point Limit Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .clip(CutCornerShape(5.dp))
                    .background(Brush.verticalGradient(listOf(pal.panelTop, pal.panelBottom)))
                    .border(1.dp, pal.trimMuted, CutCornerShape(5.dp))
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("REQUISITION LIMIT", style = MaterialTheme.typography.labelSmall, color = pal.badgeText, maxLines = 1, softWrap = false)
                Slider(
                    value = state.pointLimit.toFloat(),
                    onValueChange = { onPointLimitChange(((it / 100f).toInt()) * 100) },
                    valueRange = 1000f..5000f,
                    steps = 39,
                    colors = SliderDefaults.colors(
                        thumbColor = pal.trimBorder,
                        activeTrackColor = pal.trimBorder,
                        inactiveTrackColor = DividerIron
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                        .testTag("point_limit_slider")
                )
                Box(
                    modifier = Modifier
                        .clip(CutCornerShape(4.dp))
                        .background(InputDark)
                        .border(1.dp, pal.trimMuted, CutCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${state.pointLimit}",
                        style = MaterialTheme.typography.labelMedium,
                        color = pal.headerText,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
        HorizontalDivider(color = pal.trimBorder, thickness = 1.5.dp, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun FactionSetupCard(
    faction: Faction,
    isPlayer: Boolean,
    roster: List<RosterSlot>,
    templates: Map<String, UnitTemplate>,
    totalPoints: Int,
    pointLimit: Int,
    onRandomize: () -> Unit,
    onAddUnit: (String) -> Unit,
    onRemoveSlot: (String) -> Unit,
    onEditTemplate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isMarines = faction == Faction.MARINES
    val pal = factionPalette(faction)
    val overBudget = totalPoints > pointLimit
    val catalog = templates.values.filter { it.faction == faction }

    GrimdarkBevelPanel(
        modifier = modifier.fillMaxWidth(),
        cut = 9.dp,
        borderColor = pal.trimBorder,
        topColor = pal.panelTop,
        bottomColor = pal.panelBottom,
        accentBarColor = pal.primaryAccent
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Image(
                        painter = painterResource(
                            id = if (isMarines) R.drawable.img_marine_crest else R.drawable.img_xenos_crest
                        ),
                        contentDescription = "${faction.displayName} Crest",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CutCornerShape(6.dp))
                            .border(1.5.dp, pal.trimBorder, CutCornerShape(6.dp))
                    )
                    Column {
                        Text(
                            text = "${if (isPlayer) "COMMANDER" else "HOSTILE"}: ${faction.displayName.uppercase()}",
                            style = MaterialTheme.typography.titleMedium,
                            color = pal.headerText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = faction.badgeTag,
                            style = MaterialTheme.typography.labelSmall,
                            color = pal.badgeText
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = onRandomize,
                        shape = grimdarkBevelShape(5.dp),
                        border = BorderStroke(1.dp, pal.trimBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = pal.buttonSurface,
                            contentColor = pal.headerText
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("randomize_${faction.name.lowercase()}")
                    ) {
                        Icon(Icons.Default.Casino, contentDescription = "Randomize", modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("RND", style = MaterialTheme.typography.labelSmall)
                    }

                    Box(
                        modifier = Modifier
                            .clip(CutCornerShape(5.dp))
                            .background(InputDark)
                            .border(1.dp, if (overBudget) CrimsonSoft else pal.trimMuted, CutCornerShape(5.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$totalPoints/$pointLimit",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (overBudget) CrimsonSoft else pal.headerText
                        )
                    }
                }
            }

            HorizontalDivider(color = DividerIron)

            Text(
                text = if (isMarines) "DEPLOYED STRIKE FORCE (${roster.size})" else "DEPLOYED TYRANIDS (${roster.size})",
                style = MaterialTheme.typography.labelSmall,
                color = pal.badgeText
            )

            if (roster.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CutCornerShape(6.dp))
                        .background(RowSlate)
                        .border(1.dp, DividerIron, CutCornerShape(6.dp))
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No squads deployed. Add reinforcements below.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            } else {
                roster.forEach { slot ->
                    val tpl = templates[slot.templateId]
                    if (tpl != null) {
                        DeployedUnitRow(
                            template = tpl,
                            pointColor = pal.trimBorder,
                            rowBorderColor = pal.trimMuted.copy(alpha = 0.65f),
                            onEdit = { onEditTemplate(tpl.id) },
                            onRemove = { onRemoveSlot(slot.slotId) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(2.dp))
            Text(
                text = if (isMarines) "ARMORY REINFORCEMENTS" else "TYRANID BIO-STRAINS",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )

            catalog.forEach { tpl ->
                CatalogUnitRow(
                    template = tpl,
                    pointColor = pal.trimBorder,
                    canAfford = totalPoints + tpl.points <= pointLimit,
                    onEdit = { onEditTemplate(tpl.id) },
                    onAdd = { onAddUnit(tpl.id) }
                )
            }
        }
    }
}

@Composable
private fun DeployedUnitRow(
    template: UnitTemplate,
    pointColor: Color,
    rowBorderColor: Color,
    onEdit: () -> Unit,
    onRemove: () -> Unit
) {
    val shape = CutCornerShape(6.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.verticalGradient(listOf(CardSlate, RowSlate)))
            .border(1.dp, rowBorderColor, shape)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = template.name.uppercase(),
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "${template.points}PT",
                    style = MaterialTheme.typography.labelSmall,
                    color = pointColor
                )
            }
            val invSuffix = template.invulnSave?.let { "/${it}++" } ?: ""
            Text(
                text = "UNIT: ${template.squadSize}x ${template.woundsPerModel}W • M ${template.moveCm}cm • T${template.toughness} • SV ${template.save}+$invSuffix • WS ${template.weaponSkill}+",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val skillPart = if (template.isRangedMain) "R${template.range} BS${template.ballisticSkill}+" else "MELEE"
            val apPart = if (template.armorPen == 0) "AP0" else "AP-${template.armorPen}"
            val weaponTags = buildString {
                if (template.mainWeaponAssault) append(" [ASSAULT]")
                if (template.mainWeaponPistol) append(" [PISTOL]")
            }
            Text(
                text = "${template.mainWeaponName.uppercase()}$weaponTags: $skillPart • A${template.attacksPerModel} S${template.strength} $apPart D${template.damage}",
                style = MaterialTheme.typography.labelSmall,
                color = WarmAmber,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (template.hasPistol) {
                val pAp = if (template.pistolArmorPen == 0) "AP0" else "AP-${template.pistolArmorPen}"
                Text(
                    text = "PISTOL (${template.pistolWeaponName}): R${template.pistolRange} BS${template.ballisticSkill}+ • A${template.pistolAttacks} S${template.pistolStrength} $pAp D${template.pistolDamage}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TacticalCyan,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        OutlinedButton(
            onClick = onEdit,
            shape = grimdarkBevelShape(4.dp),
            border = BorderStroke(1.dp, BorderSteel),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = PanelIronTop,
                contentColor = TacticalCyan
            ),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            modifier = Modifier.height(34.dp)
        ) {
            Icon(Icons.Default.Edit, contentDescription = "Edit ${template.name}", modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(3.dp))
            Text("EDIT", style = MaterialTheme.typography.labelSmall)
        }

        OutlinedButton(
            onClick = onRemove,
            shape = grimdarkBevelShape(4.dp),
            border = BorderStroke(1.dp, CrimsonDeep),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = CrimsonSurface,
                contentColor = CrimsonSoft
            ),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            modifier = Modifier.height(34.dp)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Remove ${template.name}", modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun CatalogUnitRow(
    template: UnitTemplate,
    pointColor: Color,
    canAfford: Boolean,
    onEdit: () -> Unit,
    onAdd: () -> Unit
) {
    val shape = CutCornerShape(6.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(InputDark.copy(alpha = 0.85f))
            .border(1.dp, DividerIron, shape)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = template.name.uppercase(),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (canAfford) TextPrimary else TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "${template.points}PT",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (canAfford) pointColor else CrimsonSoft
                )
            }
            val wpnTag = if (template.mainWeaponAssault) " [ASSAULT]" else ""
            Text(
                text = "${template.role} • ${template.mainWeaponName}$wpnTag • Skill: ${template.skillName}",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        OutlinedButton(
            onClick = onEdit,
            shape = grimdarkBevelShape(4.dp),
            border = BorderStroke(1.dp, BorderSteel),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = PanelIronTop,
                contentColor = TacticalCyan
            ),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            modifier = Modifier.height(34.dp)
        ) {
            Text("STATS", style = MaterialTheme.typography.labelSmall)
        }

        OutlinedButton(
            onClick = onAdd,
            enabled = canAfford,
            shape = grimdarkBevelShape(4.dp),
            border = BorderStroke(1.dp, if (canAfford) EmeraldBorder else DividerIron),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (canAfford) EmeraldSurface else InputDark,
                contentColor = if (canAfford) EmeraldBuff else TextMuted,
                disabledContainerColor = InputDark,
                disabledContentColor = TextMuted
            ),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier = Modifier.height(34.dp)
        ) {
            if (canAfford) {
                Icon(Icons.Default.Add, contentDescription = "Add ${template.name}", modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(2.dp))
                Text("ADD", style = MaterialTheme.typography.labelSmall)
            } else {
                Text("LIMIT", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun SetupFooter(
    state: SetupUiState
) {
    val pal = factionPalette(state.playerFaction)
    val isMarines = state.playerFaction == Faction.MARINES
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            AnimatedVisibility(visible = state.validationWarning != null) {
                Text(
                    text = state.validationWarning.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = CrimsonSoft,
                    fontWeight = FontWeight.SemiBold
                )
            }
            AnimatedVisibility(visible = state.validationWarning == null && state.statusMessage != null) {
                Text(
                    text = state.statusMessage.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = EmeraldBuff,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (state.validationWarning == null && state.statusMessage == null) {
                Text(
                    text = if (isMarines) "ULTRAMARINES COGITATOR READY • DEPLOY STRIKE FORCE"
                    else "HIVE SYNAPSE LINK ARMED • UNLEASH THE TYRANIDS",
                    style = MaterialTheme.typography.bodySmall,
                    color = pal.badgeText
                )
            }
        }
    }
}
