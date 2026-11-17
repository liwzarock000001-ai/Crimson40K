package com.example.ui.menu

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AppScreen
import com.example.ui.theme.BorderSteel
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
import com.example.ui.theme.XenosViolet
import com.example.ui.theme.grimdarkBevelShape

@Composable
fun MainMenuScreen(
    onNavigate: (AppScreen) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF161C26), VoidObsidian),
                    radius = 1350f
                )
            )
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_setup_banner),
            contentDescription = "Main Menu Background",
            contentScale = ContentScale.Crop,
            alpha = 0.28f,
            modifier = Modifier.matchParentSize()
        )
        DriftingEmbersBackdrop(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .displayCutoutPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Grimdark Logo / Title Header
            GrimdarkBevelPanel(
                modifier = Modifier.widthIn(max = 520.dp),
                cut = 12.dp,
                borderColor = ImperialGold,
                topColor = VoidObsidian,
                bottomColor = Color(0xFF1B2230),
                accentBarColor = ImperialGold
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "CRIMSON SECTOR TACTICAL",
                        style = MaterialTheme.typography.titleLarge,
                        color = ImperialGold,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "GRAND STRATEGY & CO-OP TACTICS",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // 4 Main Menu Buttons
            Column(
                modifier = Modifier.widthIn(max = 440.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Skirmish
                MenuButton(
                    title = "1. SKIRMISH BATTLE",
                    subtitle = "Deploy custom rosters & tactical skirmish",
                    icon = Icons.Default.PlayArrow,
                    containerColor = EmeraldSurface,
                    contentColor = EmeraldBuff,
                    borderColor = EmeraldBorder,
                    testTag = "menu_skirmish",
                    onClick = { onNavigate(AppScreen.SKIRMISH_SETUP) }
                )

                // 2. Load Saved Game
                MenuButton(
                    title = "2. LOAD SAVED GAME",
                    subtitle = "Resume saved skirmish roster & battle progress",
                    icon = Icons.Default.FolderOpen,
                    containerColor = PanelIronTop,
                    contentColor = TacticalCyan,
                    borderColor = BorderSteel,
                    testTag = "menu_load_game",
                    onClick = { onNavigate(AppScreen.LOAD_GAME) }
                )

                // 3. Co-op Multiplayer (4 players, host authority, turn rotation)
                MenuButton(
                    title = "3. CO-OP ONLINE (4P)",
                    subtitle = "Play online over long distance with a 6-digit Room Code",
                    icon = Icons.Default.Groups,
                    containerColor = XenosViolet.copy(alpha = 0.85f),
                    contentColor = TextPrimary,
                    borderColor = XenosViolet,
                    testTag = "menu_coop",
                    onClick = { onNavigate(AppScreen.COOP_LOBBY) }
                )

                // 4. Settings & Graphics (4 levels, audio, display)
                MenuButton(
                    title = "4. SETTINGS & GRAPHICS",
                    subtitle = "Configure audio, lighting & 4 graphics levels",
                    icon = Icons.Default.Settings,
                    containerColor = PanelIronTop,
                    contentColor = TextPrimary,
                    borderColor = BorderSteel,
                    testTag = "menu_settings",
                    onClick = { onNavigate(AppScreen.SETTINGS) }
                )
            }
        }
    }
}

@Composable
private fun MenuButton(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    contentColor: Color,
    borderColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        shape = grimdarkBevelShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        border = BorderStroke(1.5.dp, borderColor),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .testTag(testTag)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = contentColor)
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = contentColor.copy(alpha = 0.8f))
        }
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
    }
}
