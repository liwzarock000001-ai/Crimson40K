package com.example.ui.menu

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.SettingsState
import com.example.ui.theme.BorderSteel
import com.example.ui.theme.CardSlate
import com.example.ui.theme.DividerIron
import com.example.ui.theme.DriftingEmbersBackdrop
import com.example.ui.theme.EmeraldBorder
import com.example.ui.theme.EmeraldBuff
import com.example.ui.theme.EmeraldSurface
import com.example.ui.theme.GrimdarkBevelPanel
import com.example.ui.theme.ImperialGold
import com.example.ui.theme.PanelIronTop
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.VoidObsidian
import com.example.ui.theme.grimdarkBevelShape

@Composable
fun SettingsScreen(
    state: SettingsState,
    onBack: () -> Unit,
    onUpdateSettings: (SettingsState) -> Unit
) {
    BackHandler { onBack() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(VoidObsidian, Color(0xFF10141D)),
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
                    shape = grimdarkBevelShape(5.dp),
                    border = BorderStroke(1.dp, BorderSteel),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = PanelIronTop,
                        contentColor = TextPrimary
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("MENU", style = MaterialTheme.typography.labelSmall)
                }

                Text(
                    text = "SETTINGS & GRAPHICS (4 LEVELS)",
                    style = MaterialTheme.typography.titleMedium,
                    color = ImperialGold,
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider(color = DividerIron)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Audio Settings
                GrimdarkBevelPanel(
                    modifier = Modifier.fillMaxWidth(),
                    cut = 8.dp,
                    borderColor = BorderSteel,
                    topColor = CardSlate,
                    bottomColor = VoidObsidian,
                    accentBarColor = TacticalCyan
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = TacticalCyan)
                            Text("AUDIO & SOUND FX", style = MaterialTheme.typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Enable Sound Effects (SFX)", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                            Switch(
                                checked = state.sfxEnabled,
                                onCheckedChange = { onUpdateSettings(state.copy(sfxEnabled = it)) },
                                colors = SwitchDefaults.colors(checkedThumbColor = TacticalCyan)
                            )
                        }

                        Text("Master Volume: ${(state.soundVolume * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Slider(
                            value = state.soundVolume,
                            onValueChange = { onUpdateSettings(state.copy(soundVolume = it)) },
                            valueRange = 0f..1f
                        )
                    }
                }

                // 2. Graphics Quality (4 Levels)
                GrimdarkBevelPanel(
                    modifier = Modifier.fillMaxWidth(),
                    cut = 8.dp,
                    borderColor = EmeraldBorder,
                    topColor = CardSlate,
                    bottomColor = VoidObsidian,
                    accentBarColor = EmeraldBuff
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.HighQuality, contentDescription = null, tint = EmeraldBuff)
                            Text("GRAPHICS QUALITY (4 LEVELS)", style = MaterialTheme.typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
                        }

                        Text("Select rendering quality profile for tactical 3D battlefield:", style = MaterialTheme.typography.bodySmall, color = TextMuted)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val levelsList = listOf(1 to "LOW", 2 to "MED", 3 to "HIGH", 4 to "ULTRA")
                            levelsList.forEach { (lvl, label) ->
                                val selected = state.graphicsLevel == lvl
                                OutlinedButton(
                                    onClick = { onUpdateSettings(state.copy(graphicsLevel = lvl)) },
                                    shape = grimdarkBevelShape(5.dp),
                                    border = BorderStroke(1.5.dp, if (selected) EmeraldBorder else BorderSteel),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (selected) EmeraldSurface else PanelIronTop,
                                        contentColor = if (selected) EmeraldBuff else TextPrimary
                                    ),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(vertical = 8.dp)
                                ) {
                                    Text("L$lvl: $label", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        val levelDesc = when (state.graphicsLevel) {
                            1 -> "Level 1 (Low): Clean flat terrain shading, no embers (Max FPS)"
                            2 -> "Level 2 (Medium): Standard directional lighting & 16 ambient embers"
                            3 -> "Level 3 (High): Rich metallic specular shaders, fog & 32 embers"
                            else -> "Level 4 (Ultra): Enhanced atmospheric bloom, rim lighting & 48 embers"
                        }
                        Text(levelDesc, style = MaterialTheme.typography.labelSmall, color = EmeraldBuff)
                    }
                }

                // 3. Screen Lighting & Brightness
                GrimdarkBevelPanel(
                    modifier = Modifier.fillMaxWidth(),
                    cut = 8.dp,
                    borderColor = BorderSteel,
                    topColor = CardSlate,
                    bottomColor = VoidObsidian,
                    accentBarColor = ImperialGold
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("BATTLEFIELD LIGHTING & BRIGHTNESS", style = MaterialTheme.typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
                        Text("Brightness: ${(state.screenBrightness * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Slider(
                            value = state.screenBrightness,
                            onValueChange = { onUpdateSettings(state.copy(screenBrightness = it)) },
                            valueRange = 0.5f..1.5f
                        )
                    }
                }
            }
        }
    }
}
