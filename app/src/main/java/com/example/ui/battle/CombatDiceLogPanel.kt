package com.example.ui.battle

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.R
import com.example.model.CombatLogEntry
import com.example.model.DiceStepDetail
import com.example.model.Faction
import com.example.ui.theme.BorderSteel
import com.example.ui.theme.CardSlate
import com.example.ui.theme.ColorTokens
import com.example.ui.theme.CrimsonBright
import com.example.ui.theme.CrimsonSoft
import com.example.ui.theme.CrimsonSurface
import com.example.ui.theme.DividerIron
import com.example.ui.theme.EmeraldBorder
import com.example.ui.theme.EmeraldBuff
import com.example.ui.theme.EmeraldSurface
import com.example.ui.theme.GrimdarkBevelPanel
import com.example.ui.theme.ImperialGold
import com.example.ui.theme.InputDark
import com.example.ui.theme.PanelIronBottom
import com.example.ui.theme.PanelIronTop
import com.example.ui.theme.RowSlate
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.TarnishedBrass
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.WarmAmber
import com.example.ui.theme.factionPalette

@Composable
fun CombatDiceLogPanel(
    logs: List<CombatLogEntry>,
    activeFaction: Faction,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    maxBodyHeight: Dp = 180.dp,
    modifier: Modifier = Modifier
) {
    val pal = factionPalette(activeFaction)
    val listState = rememberLazyListState()
    val latestId = logs.firstOrNull()?.id

    LaunchedEffect(latestId, expanded) {
        if (expanded && logs.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    GrimdarkBevelPanel(
        cut = 7.dp,
        borderColor = if (logs.isNotEmpty()) ImperialGold.copy(alpha = 0.85f) else pal.trimMuted,
        topColor = PanelIronTop.copy(alpha = 0.94f),
        bottomColor = PanelIronBottom.copy(alpha = 0.94f),
        accentBarColor = ImperialGold,
        showRivets = false,
        modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 38.dp)
                    .clickable(onClick = onToggleExpanded)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("dice_log_toggle_button"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Casino,
                        contentDescription = stringResource(R.string.dice_log_title),
                        tint = ImperialGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${stringResource(R.string.dice_log_title)} (${logs.size})",
                        style = MaterialTheme.typography.labelMedium,
                        color = ImperialGold,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                    if (!expanded && logs.isNotEmpty()) {
                        val latest = logs.first()
                        Text(
                            text = "• ${latest.resultSummary}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (latest.isSuccess) EmeraldBuff else TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) {
                        stringResource(R.string.dice_log_collapse)
                    } else {
                        stringResource(R.string.dice_log_expand)
                    },
                    tint = WarmAmber,
                    modifier = Modifier.size(18.dp)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(color = DividerIron)
                    if (logs.isEmpty()) {
                        Text(
                            text = stringResource(R.string.dice_log_empty),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)
                        )
                    } else {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 78.dp, max = maxBodyHeight)
                                .testTag("dice_log_list")
                        ) {
                            items(
                                items = logs,
                                key = { it.id }
                            ) { entry ->
                                CombatDiceLogEntryCard(entry = entry)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CombatDiceLogEntryCard(entry: CombatLogEntry) {
    val entryPal = factionPalette(entry.faction)
    val cardShape = remember { CutCornerShape(5.dp) }
    val borderCol = when {
        entry.isCounterOrFightFirst -> WarmAmber.copy(alpha = 0.7f)
        else -> entryPal.trimMuted.copy(alpha = 0.75f)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(CardSlate.copy(alpha = 0.92f))
            .border(1.dp, borderCol, cardShape)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag("dice_log_entry_${entry.id}"),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .clip(CutCornerShape(3.dp))
                        .background(entryPal.buttonSurface)
                        .border(0.8.dp, entryPal.trimMuted, CutCornerShape(3.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "R${entry.round}",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = entryPal.headerText,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .clip(CutCornerShape(3.dp))
                    .background(if (entry.isSuccess) EmeraldSurface else CrimsonSurface)
                    .border(
                        0.8.dp,
                        if (entry.isSuccess) EmeraldBorder else CrimsonBright.copy(alpha = 0.7f),
                        CutCornerShape(3.dp)
                    )
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            ) {
                Text(
                    text = entry.resultSummary,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = if (entry.isSuccess) EmeraldBuff else CrimsonSoft,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        Text(
            text = entry.subtitle,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            color = if (entry.isCounterOrFightFirst) WarmAmber else TextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        entry.steps.forEach { step ->
            DiceStepDetailBlock(step = step, entryId = entry.id)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DiceStepDetailBlock(
    step: DiceStepDetail,
    entryId: Long = 0L
) {
    var rollTrigger by remember { mutableIntStateOf(0) }
    val blockShape = remember { CutCornerShape(4.dp) }
    val phaseColor = when (step.phaseName) {
        "HIT" -> TacticalCyan
        "WOUND" -> WarmAmber
        "SAVE" -> ImperialGold
        "CHARGE" -> CrimsonSoft
        else -> EmeraldBuff
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(blockShape)
            .background(RowSlate)
            .border(0.8.dp, DividerIron, blockShape)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = step.phaseName,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = phaseColor,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = step.stepLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = "Roll ${step.rolled}",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = TextMuted,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = "✓${step.passed}",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = EmeraldBuff,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = "✗${step.failed}",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = if (step.failed > 0) CrimsonSoft else TextMuted,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        if (step.note.isNotBlank()) {
            Text(
                text = step.note,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                color = WarmAmber,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (step.diceValues.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { rollTrigger++ }
                    .padding(vertical = 2.dp)
                    .testTag("step_dice_row_${step.phaseName}")
            ) {
                step.diceValues.forEachIndexed { index, die ->
                    Tumbling3DDice(
                        die = die,
                        step = step,
                        index = index,
                        triggerKey = entryId to rollTrigger
                    )
                }
            }
        }
    }
}

@Composable
private fun Tumbling3DDice(
    die: Int,
    step: DiceStepDetail,
    index: Int,
    triggerKey: Pair<Long, Int>
) {
    var individualTrigger by remember { mutableIntStateOf(0) }
    val animProgress = remember { Animatable(0f) }
    val staggerDelayMs = remember(index) { ((index % 8) * 36L).coerceAtMost(220L) }
    var displayedValue by remember(triggerKey, individualTrigger) { mutableIntStateOf(die) }

    LaunchedEffect(triggerKey, individualTrigger) {
        animProgress.snapTo(0f)
        if (staggerDelayMs > 0L) {
            delay(staggerDelayMs)
        }
        // Rapid face value tumbling cycling in flight
        launch {
            var stepCount = 0
            while (animProgress.value < 0.78f) {
                delay(38L)
                stepCount++
                displayedValue = ((die + stepCount + index) % 6) + 1
            }
            displayedValue = die // Firmly lock to true result
        }
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 580,
                easing = FastOutSlowInEasing
            )
        )
    }

    val t = animProgress.value.coerceIn(0f, 1f)
    val heightDp: Float
    val squashX: Float
    val squashY: Float

    if (t >= 1f) {
        heightDp = 0f
        squashX = 1f
        squashY = 1f
    } else if (t < 0.55f) {
        // Phase 1: Free fall drop under tabletop gravity from 14dp height
        val p = t / 0.55f
        heightDp = 14f * (1f - p * p)
        if (t > 0.48f) {
            val f = (t - 0.48f) / 0.07f
            squashY = 1f - 0.18f * f
            squashX = 1f + 0.15f * f
        } else {
            squashY = 1.06f
            squashX = 0.95f
        }
    } else if (t < 0.82f) {
        // Phase 2: First rebound bounce (apex 4.2dp)
        val p = (t - 0.55f) / 0.27f
        heightDp = 4f * 4.2f * p * (1f - p)
        if (t < 0.62f) {
            val f = 1f - (t - 0.55f) / 0.07f
            squashY = 1f - 0.18f * f
            squashX = 1f + 0.15f * f
        } else if (t > 0.76f) {
            val f = (t - 0.76f) / 0.06f
            squashY = 1f - 0.08f * f
            squashX = 1f + 0.07f * f
        } else {
            squashY = 1f
            squashX = 1f
        }
    } else {
        // Phase 3: Settle micro-hop (apex 1.2dp)
        val p = (t - 0.82f) / 0.18f
        heightDp = 4f * 1.2f * p * (1f - p)
        if (t < 0.88f) {
            val f = 1f - (t - 0.82f) / 0.06f
            squashY = 1f - 0.08f * f
            squashX = 1f + 0.07f * f
        } else {
            squashY = 1f
            squashX = 1f
        }
    }

    // Rotations along X, Y, and Z with quadratic decay towards 0
    val decay = if (t >= 1f) 0f else (1f - t) * (1f - t)
    val spinSignX = if (index % 2 == 0) 1f else -1f
    val spinSignY = if ((index / 2) % 2 == 0) 1f else -1f
    val spinSignZ = if (index % 3 == 0) 1f else -1f

    val rotX = (540f + (index % 3) * 180f) * spinSignX * decay
    val rotY = (360f + ((index + 1) % 3) * 120f) * spinSignY * decay
    val rotZ = (140f + (index % 2) * 80f) * spinSignZ * decay

    // Dynamic contact shadow beneath the die
    val normHeight = (heightDp / 14f).coerceIn(0f, 1f)
    val shadowAlpha = if (t >= 1f) 0.50f else (0.50f - normHeight * 0.35f).coerceIn(0.12f, 0.50f)
    val shadowScaleX = 1.0f + normHeight * 0.35f
    val shadowScaleY = 0.50f + normHeight * 0.20f

    val (finalBg, finalFg, finalBorder) = dieColors(step = step, die = die)
    val isTumbling = animProgress.value < 0.78f
    val bg = if (isTumbling) InputDark else finalBg
    val fg = if (isTumbling) WarmAmber else finalFg
    val border = if (isTumbling) BorderSteel.copy(alpha = 0.85f) else finalBorder

    Box(
        modifier = Modifier
            .size(width = 22.dp, height = 24.dp)
            .clickable { individualTrigger++ }
            .testTag("die_item_${step.phaseName}_$index"),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Tabletop contact shadow
        Box(
            modifier = Modifier
                .padding(bottom = 1.dp)
                .size(width = 16.dp, height = 6.dp)
                .align(Alignment.BottomCenter)
                .graphicsLayer {
                    this.scaleX = shadowScaleX
                    this.scaleY = shadowScaleY
                    this.alpha = shadowAlpha
                }
                .background(Color.Black.copy(alpha = 0.75f), CutCornerShape(3.dp))
        )

        // 3D physics-tumbling dice cube
        Box(
            modifier = Modifier
                .padding(bottom = 2.dp)
                .size(width = 19.dp, height = 19.dp)
                .align(Alignment.BottomCenter)
                .graphicsLayer {
                    cameraDistance = 14f * density
                    this.translationY = -heightDp * density
                    this.rotationX = rotX
                    this.rotationY = rotY
                    this.rotationZ = rotZ
                    this.scaleX = squashX
                    this.scaleY = squashY
                }
                .clip(CutCornerShape(2.5.dp))
                .background(bg)
                .border(0.9.dp, border, CutCornerShape(2.5.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Subtle 3D top bevel edge highlight
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.5.dp)
                    .align(Alignment.TopCenter)
                    .background(Color.White.copy(alpha = 0.20f))
            )

            Text(
                text = displayedValue.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.5.sp,
                color = fg,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

private fun dieColors(step: DiceStepDetail, die: Int): Triple<Color, Color, Color> {
    return when (step.phaseName) {
        "ADVANCE" -> Triple(EmeraldSurface, EmeraldBuff, EmeraldBorder)
        "CHARGE" -> if (step.passed > 0) {
            Triple(EmeraldSurface, EmeraldBuff, EmeraldBorder)
        } else {
            Triple(CrimsonSurface, CrimsonSoft, CrimsonBright.copy(alpha = 0.7f))
        }
        else -> when {
            die <= 1 -> Triple(CrimsonSurface, CrimsonSoft, CrimsonBright.copy(alpha = 0.65f))
            die == 6 && step.targetNeeded <= 6 -> Triple(ColorTokens.GoldSurface, WarmAmber, TarnishedBrass)
            die >= step.targetNeeded -> Triple(EmeraldSurface, EmeraldBuff, EmeraldBorder)
            else -> Triple(InputDark, TextMuted, BorderSteel.copy(alpha = 0.7f))
        }
    }
}
