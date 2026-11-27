package com.example.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.model.UnitRoleCategory
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.sin

fun grimdarkBevelShape(cut: Dp = 7.dp): CutCornerShape = CutCornerShape(cut)

/**
 * Beveled relic metal panel with brushed gunmetal gradient, inner top highlight,
 * tarnished brass or steel outer bevel, and corner rivets.
 */
@Composable
fun GrimdarkBevelPanel(
    modifier: Modifier = Modifier,
    cut: Dp = 8.dp,
    borderColor: Color = ImperialGold,
    topColor: Color = PanelIronTop,
    bottomColor: Color = PanelIronBottom,
    accentBarColor: Color? = null,
    showRivets: Boolean = true,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = remember(cut) { CutCornerShape(cut) }
    val brush = remember(topColor, bottomColor) {
        Brush.verticalGradient(colors = listOf(topColor, bottomColor))
    }

    var mod = modifier
        .clip(shape)
        .background(brush)
        .border(1.2.dp, borderColor.copy(alpha = 0.85f), shape)
        .drawWithCache {
            val cutPx = cut.toPx()
            val w = size.width
            val h = size.height
            val inset = 2.5.dp.toPx()
            val innerCut = (cutPx - inset * 0.45f).coerceAtLeast(2f)
            val path = Path().apply {
                moveTo(inset + innerCut, inset)
                lineTo(w - inset - innerCut, inset)
                lineTo(w - inset, inset + innerCut)
                lineTo(w - inset, h - inset - innerCut)
                lineTo(w - inset - innerCut, h - inset)
                lineTo(inset + innerCut, h - inset)
                lineTo(inset, h - inset - innerCut)
                lineTo(inset, inset + innerCut)
                close()
            }
            val rOffset = cutPx * 0.65f + 4.dp.toPx()
            val rivetRadius = 1.6.dp.toPx()
            val rivetYPad = 5.dp.toPx()
            val rivetColor = borderColor.copy(alpha = 0.78f)
            val darkRivet = Color(0xAA050608)
            val accentStroke = 2.5.dp.toPx()

            onDrawBehind {
                drawPath(path, color = Color(0x28FFFFFF), style = Stroke(width = 1f))
                if (accentBarColor != null) {
                    drawLine(
                        color = accentBarColor,
                        start = Offset(cutPx + 2f, 2f),
                        end = Offset(w - cutPx - 2f, 2f),
                        strokeWidth = accentStroke
                    )
                }
                if (showRivets && w > 48f && h > 36f) {
                    drawCircle(darkRivet, radius = rivetRadius + 1f, center = Offset(rOffset, rivetYPad))
                    drawCircle(rivetColor, radius = rivetRadius, center = Offset(rOffset, rivetYPad))
                    drawCircle(darkRivet, radius = rivetRadius + 1f, center = Offset(w - rOffset, rivetYPad))
                    drawCircle(rivetColor, radius = rivetRadius, center = Offset(w - rOffset, rivetYPad))
                    drawCircle(darkRivet, radius = rivetRadius + 1f, center = Offset(rOffset, h - rivetYPad))
                    drawCircle(rivetColor, radius = rivetRadius, center = Offset(rOffset, h - rivetYPad))
                    drawCircle(darkRivet, radius = rivetRadius + 1f, center = Offset(w - rOffset, h - rivetYPad))
                    drawCircle(rivetColor, radius = rivetRadius, center = Offset(w - rOffset, h - rivetYPad))
                }
            }
        }

    if (onClick != null) {
        mod = mod.clickable(onClick = onClick)
    }

    Box(modifier = mod, content = content)
}

/**
 * Lightweight drifting warzone embers & ash particles overlay for Compose screens (Setup).
 */
@Composable
fun DriftingEmbersBackdrop(modifier: Modifier = Modifier) {
    var timeSec by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = System.currentTimeMillis()
        while (true) {
            withFrameMillis {
                timeSec = ((System.currentTimeMillis() - start) % 120000L) / 1000f
            }
        }
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 1f || h <= 1f) return@Canvas

        val count = 28
        for (i in 0 until count) {
            val seed = (i * 73 + 19) % 100
            val baseX = ((seed * 37) % 100) / 100f
            val speed = 0.035f + (i % 5) * 0.012f
            val yFrac = 1f - ((timeSec * speed + (i * 0.13f)) % 1.1f)
            val drift = sin(timeSec * 1.4f + i * 1.9f) * (12f + (i % 4) * 6f)
            val px = baseX * w + drift
            val py = yFrac * h
            val isEmber = i % 3 != 0
            val radius = if (isEmber) 1.8f + (i % 3) * 0.9f else 1.4f
            val alpha = (sin((1f - yFrac.coerceIn(0f, 1f)) * Math.PI).toFloat() * 0.55f).coerceIn(0f, 0.6f)
            val color = if (isEmber) {
                if (i % 2 == 0) Color(0xFFD96B27).copy(alpha = alpha)
                else Color(0xFFC94024).copy(alpha = alpha)
            } else {
                Color(0xFF7D828C).copy(alpha = alpha * 0.65f)
            }
            drawCircle(color = color, radius = radius, center = Offset(px, py))
        }
    }
}

/**
 * Visual badge for Tabletop 10th/11th Edition unit categories:
 * - HERO / CHARACTER (Gold)
 * - BATTLELINE (Cyan)
 * - INFANTRY (Emerald/Slate)
 * - VEHICLE / MONSTER (Amber)
 */
@Composable
fun TabletopCategoryBadge(
    category: UnitRoleCategory,
    modifier: Modifier = Modifier
) {
    if (category == UnitRoleCategory.ALL) return
    val (textColor, bgColor, borderColor) = when (category) {
        UnitRoleCategory.HERO_CHARACTER -> Triple(ImperialGold, Color(0x33E5B338), ImperialGold.copy(alpha = 0.85f))
        UnitRoleCategory.BATTLELINE -> Triple(TacticalCyan, Color(0x2838BDF8), TacticalCyan.copy(alpha = 0.8f))
        UnitRoleCategory.INFANTRY -> Triple(EmeraldBuff, Color(0x2434D399), EmeraldBuff.copy(alpha = 0.7f))
        UnitRoleCategory.VEHICLE_MONSTER -> Triple(WarmAmber, Color(0x2ECA8A04), WarmAmber.copy(alpha = 0.8f))
        UnitRoleCategory.ALLIANCE_IMPERIUM -> Triple(CrimsonBright, Color(0x33DC2626), CrimsonBright.copy(alpha = 0.85f))
        UnitRoleCategory.ALL -> Triple(TextMuted, InputDark, DividerIron)
    }

    Surface(
        shape = CutCornerShape(3.dp),
        color = bgColor,
        border = BorderStroke(0.8.dp, borderColor),
        modifier = modifier
    ) {
        Text(
            text = category.badgeLabel,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
            color = textColor,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
        )
    }
}

/**
 * Tabletop 10th/11th Edition battlefield category filter tabs:
 * [ALL (9)] [⭐ HERO (1)] [⚔️ BATTLELINE (2)] [🛡️ INFANTRY (4)] [⚡ VEHICLE / MONST (2)]
 */
@Composable
fun TabletopCategoryFilterTabs(
    selectedCategory: UnitRoleCategory,
    onSelectCategory: (UnitRoleCategory) -> Unit,
    categoryCounts: Map<UnitRoleCategory, Int>,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UnitRoleCategory.entries.forEach { cat ->
            val count = categoryCounts[cat] ?: 0
            if (count > 0 || cat == UnitRoleCategory.ALL || cat == selectedCategory) {
                val isSelected = selectedCategory == cat
                val tabBorderColor = if (isSelected) accentColor else DividerIron
                val tabBgColor = if (isSelected) accentColor.copy(alpha = 0.22f) else InputDark.copy(alpha = 0.75f)
                val tabTextColor = if (isSelected) accentColor else if (count > 0) TextPrimary else TextMuted

                OutlinedButton(
                    onClick = { onSelectCategory(cat) },
                    shape = CutCornerShape(4.dp),
                    border = BorderStroke(if (isSelected) 1.2.dp else 0.8.dp, tabBorderColor),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = tabBgColor,
                        contentColor = tabTextColor
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier
                        .height(30.dp)
                        .testTag("tab_cat_${cat.id}")
                ) {
                    val label = if (cat == UnitRoleCategory.ALL) "ALL ($count)" else "${cat.emoji} ${cat.shortLabel} ($count)"
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 10.sp
                        )
                    )
                }
            }
        }
    }
}

