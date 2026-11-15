package com.example.ui.theme

import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val CrimsonDarkColorScheme = darkColorScheme(
    primary = MarineBright,
    onPrimary = TextPrimary,
    primaryContainer = MarineSurface,
    onPrimaryContainer = MarineText,
    secondary = ImperialGold,
    onSecondary = VoidObsidian,
    secondaryContainer = ColorTokens.GoldSurface,
    onSecondaryContainer = WarmAmber,
    tertiary = XenosViolet,
    onTertiary = TextPrimary,
    tertiaryContainer = XenosSurface,
    onTertiaryContainer = XenosText,
    background = VoidObsidian,
    onBackground = TextPrimary,
    surface = PanelCharcoal,
    onSurface = TextPrimary,
    surfaceVariant = CardSlate,
    onSurfaceVariant = TextMuted,
    outline = BorderSteel,
    outlineVariant = DividerIron,
    error = CrimsonBright,
    onError = TextPrimary,
    errorContainer = CrimsonSurface,
    onErrorContainer = CrimsonSoft
)

private val GrimdarkShapes = Shapes(
    extraSmall = CutCornerShape(4.dp),
    small = CutCornerShape(6.dp),
    medium = CutCornerShape(8.dp),
    large = CutCornerShape(10.dp),
    extraLarge = CutCornerShape(12.dp)
)

object ColorTokens {
    val GoldSurface = Color(0xFF282012)
    val SkillSurface = Color(0xFF231D13)
    val SkillBorder = Color(0xFF70582B)
    val SurgeSurface = Color(0xFF2E1D0C)
    val CyanSurface = Color(0xFF122733)
    val PistolSurface = Color(0xFF171B24)
}

@Composable
fun CrimsonSectorTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CrimsonDarkColorScheme,
        typography = CrimsonTypography,
        shapes = GrimdarkShapes,
        content = content
    )
}
