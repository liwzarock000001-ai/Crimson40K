package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.battle.BattleScreen
import com.example.ui.setup.SetupScreen
import com.example.ui.theme.CrimsonSectorTheme
import com.example.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            CrimsonSectorTheme {
                val vm: GameViewModel = viewModel()
                val setupState by vm.setupState.collectAsStateWithLifecycle()
                val battleState by vm.battleState.collectAsStateWithLifecycle()

                Crossfade(targetState = battleState.active, label = "screen_switch") { inBattle ->
                    if (inBattle) {
                        BattleScreen(
                            state = battleState,
                            onTileTap = vm::onTileTapped,
                            onConfirmStrike = vm::confirmStrike,
                            onSelectWeaponMode = vm::selectWeaponMode,
                            onTriggerSurge = vm::triggerSurge,
                            onRotateUnit = vm::rotateSelectedUnit,
                            onEndTurn = vm::endPlayerTurn,
                            onToggleAutoFocus = vm::toggleAutoFocus,
                            onCenterCamera = vm::centerCamera,
                            onToggleSound = vm::toggleSound,
                            onToggleUnitCard = vm::toggleUnitCardCollapsed,
                            onDismissTurnSplash = vm::dismissTurnSplash,
                            onExitToSetup = vm::returnToSetup
                        )
                    } else {
                        SetupScreen(
                            state = setupState,
                            onToggleSide = vm::togglePlayerFaction,
                            onPointLimitChange = vm::setPointLimit,
                            onSave = vm::saveSetupToDatabase,
                            onReset = vm::resetSetupToDefaults,
                            onRandomize = vm::randomizeRoster,
                            onAddUnit = vm::addRosterUnit,
                            onRemoveSlot = vm::removeRosterSlot,
                            onEditTemplate = vm::openStatEditor,
                            onCloseEditor = vm::closeStatEditor,
                            onSaveTemplate = vm::saveEditedTemplate,
                            onDeploy = vm::deployToBattle
                        )
                    }
                }
            }
        }
    }
}
