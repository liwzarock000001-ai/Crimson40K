package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.model.Faction

// Grimdark Abyssal Void & Weathered Metal Surfaces
val VoidObsidian = Color(0xFF07080B)
val PanelCharcoal = Color(0xFF111318)
val PanelIronTop = Color(0xFF1D2129)
val PanelIronBottom = Color(0xFF0B0D11)
val CardSlate = Color(0xFF151820)
val RowSlate = Color(0xFF12151C)
val InputDark = Color(0xFF090B0E)
val BorderSteel = Color(0xFF424754)
val BorderSteelLight = Color(0xFF5A6170)
val DividerIron = Color(0xFF262A33)

// Tarnished Antique Brass / Forge Ember (Relics & Warnings)
val ImperialGold = Color(0xFFB38E46)
val TarnishedBrass = Color(0xFF7D6230)
val WarmAmber = Color(0xFFCBB07A)

// Ultramarines: Macragge Ultramarine Blue + Brushed Silver Insignia & Trim
val MarineBlue = Color(0xFF163B7A)
val MarineBright = Color(0xFF2255AA)
val MarineText = Color(0xFF93AFD4)
val MarineSurface = Color(0xFF0D1B33)
val MarinePanelTop = Color(0xFF13223D)
val MarinePanelBottom = Color(0xFF09101D)
val UltramarineSilver = Color(0xFFB8C4D4)
val UltramarineSilverMuted = Color(0xFF7A8799)
val UltramarineSilverBright = Color(0xFFD9E2EE)

// Alien Swarm: Grimdark Bruised Chitin Violet + Aged Bone
val XenosViolet = Color(0xFF6B3680)
val XenosBorder = Color(0xFF844A9C)
val XenosText = Color(0xFFB88ECA)
val XenosSurface = Color(0xFF1C1024)
val XenosPanelTop = Color(0xFF21162B)
val XenosPanelBottom = Color(0xFF0E0913)
val XenosBone = Color(0xFFC7B2AC)

// Dried Blood / Inquisition Crimson
val CrimsonDeep = Color(0xFF69151B)
val CrimsonBright = Color(0xFF942028)
val CrimsonSoft = Color(0xFFC9676D)
val CrimsonSurface = Color(0xFF230E12)

// Muted Cogitator Telemetry & Tactical Accents
val TacticalCyan = Color(0xFF4B9EB8)
val EmeraldBuff = Color(0xFF528C65)
val EmeraldSurface = Color(0xFF122118)
val EmeraldBorder = Color(0xFF31573E)

// Aged Parchment Bone & Ash Grey Typography
val TextPrimary = Color(0xFFD8D5CC)
val TextMuted = Color(0xFF838894)

data class FactionThemePalette(
    val primaryAccent: Color,
    val trimBorder: Color,
    val trimMuted: Color,
    val headerText: Color,
    val badgeText: Color,
    val panelTop: Color,
    val panelBottom: Color,
    val buttonSurface: Color
)

fun factionPalette(faction: Faction): FactionThemePalette = when (faction) {
    Faction.MARINES -> FactionThemePalette(
        primaryAccent = MarineBright,
        trimBorder = UltramarineSilver,
        trimMuted = UltramarineSilverMuted,
        headerText = UltramarineSilverBright,
        badgeText = MarineText,
        panelTop = MarinePanelTop,
        panelBottom = MarinePanelBottom,
        buttonSurface = MarineSurface
    )
    Faction.BLACK_LEGION -> FactionThemePalette(
        primaryAccent = ImperialGold,
        trimBorder = ImperialGold,
        trimMuted = BorderSteel,
        headerText = ImperialGold,
        badgeText = EmeraldBuff,
        panelTop = Color(0xFF1B2230),
        panelBottom = VoidObsidian,
        buttonSurface = Color(0xFF161C26)
    )
    Faction.XENOS -> FactionThemePalette(
        primaryAccent = XenosViolet,
        trimBorder = XenosBorder,
        trimMuted = XenosViolet,
        headerText = XenosText,
        badgeText = XenosBone,
        panelTop = XenosPanelTop,
        panelBottom = XenosPanelBottom,
        buttonSurface = XenosSurface
    )
}
