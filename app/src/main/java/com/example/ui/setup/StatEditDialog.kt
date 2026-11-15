package com.example.ui.setup

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.model.UnitArchetype
import com.example.model.UnitTemplate
import com.example.model.WarriorWeaponChoice
import com.example.ui.theme.BorderSteel
import com.example.ui.theme.CardSlate
import com.example.ui.theme.DividerIron
import com.example.ui.theme.EmeraldBorder
import com.example.ui.theme.EmeraldSurface
import com.example.ui.theme.GrimdarkBevelPanel
import com.example.ui.theme.ImperialGold
import com.example.ui.theme.InputDark
import com.example.ui.theme.PanelIronBottom
import com.example.ui.theme.PanelIronTop
import com.example.ui.theme.TarnishedBrass
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.factionPalette
import com.example.ui.theme.grimdarkBevelShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatEditDialog(
    template: UnitTemplate,
    onDismiss: () -> Unit,
    onSave: (UnitTemplate) -> Unit
) {
    val pal = factionPalette(template.faction)
    var points by remember(template.id) { mutableStateOf(template.points.toString()) }
    var squadSize by remember(template.id) { mutableStateOf(template.squadSize.toString()) }
    var hpPerModel by remember(template.id) { mutableStateOf(template.hpPerModel.toString()) }
    var armor by remember(template.id) { mutableStateOf(template.armor.toString()) }
    var armorPen by remember(template.id) { mutableStateOf(template.armorPen.toString()) }
    var mp by remember(template.id) { mutableStateOf(template.mp.toString()) }
    var ap by remember(template.id) { mutableStateOf(template.ap.toString()) }
    var damage by remember(template.id) { mutableStateOf(template.damage.toString()) }
    var attacksPerModel by remember(template.id) { mutableStateOf(template.attacksPerModel.toString()) }
    var range by remember(template.id) { mutableStateOf(template.range.toString()) }
    var accuracy by remember(template.id) { mutableStateOf(template.accuracy.toString()) }
    var skillCooldown by remember(template.id) { mutableStateOf(template.skillCooldown.toString()) }
    var skillApCost by remember(template.id) { mutableStateOf(template.skillApCost.toString()) }
    var weaponChoice by remember(template.id) {
        mutableStateOf(template.weaponChoice ?: WarriorWeaponChoice.BONESWORDS)
    }
    var weaponMenuExpanded by remember { mutableStateOf(false) }

    var pistolDamage by remember(template.id) { mutableStateOf(template.pistolDamage.toString()) }
    var pistolApCost by remember(template.id) { mutableStateOf(template.pistolApCost.toString()) }
    var pistolRange by remember(template.id) { mutableStateOf(template.pistolRange.toString()) }
    var pistolCooldown by remember(template.id) { mutableStateOf(template.pistolCooldown.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        GrimdarkBevelPanel(
            cut = 10.dp,
            borderColor = pal.trimBorder,
            topColor = pal.panelTop,
            bottomColor = pal.panelBottom,
            accentBarColor = pal.primaryAccent,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DATASHEET: ${template.name.uppercase()}",
                        style = MaterialTheme.typography.titleMedium,
                        color = pal.headerText
                    )
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = grimdarkBevelShape(4.dp),
                        border = BorderStroke(1.dp, BorderSteel),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("CLOSE", style = MaterialTheme.typography.labelSmall, color = TextPrimary)
                    }
                }
                HorizontalDivider(color = DividerIron, modifier = Modifier.padding(vertical = 8.dp))

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TwoFieldRow("Point Cost", points, { points = it }, "Squad Size (1-5)", squadSize, { squadSize = it })
                    TwoFieldRow("HP per Model", hpPerModel, { hpPerModel = it }, "Armor (0-10)", armor, { armor = it })
                    TwoFieldRow("Armor Pen (0-10)", armorPen, { armorPen = it }, "Movement (MP)", mp, { mp = it })
                    TwoFieldRow("Action Points (AP)", ap, { ap = it }, "Main Damage", damage, { damage = it })
                    TwoFieldRow("Attacks / Model", attacksPerModel, { attacksPerModel = it }, "Attack Range", range, { range = it })
                    TwoFieldRow("Accuracy (%)", accuracy, { accuracy = it }, "Skill Cooldown", skillCooldown, { skillCooldown = it })
                    StatTextField("Skill AP Cost", skillApCost, { skillApCost = it })

                    HorizontalDivider(color = DividerIron, modifier = Modifier.padding(top = 4.dp))
                    Text(
                        text = "AUXILIARY WARGEAR",
                        style = MaterialTheme.typography.labelMedium,
                        color = pal.trimBorder
                    )

                    if (template.archetype == UnitArchetype.WARRIOR) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("TYRANID WARRIOR WEAPON", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            ExposedDropdownMenuBox(
                                expanded = weaponMenuExpanded,
                                onExpandedChange = { weaponMenuExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = weaponChoice.label,
                                    onValueChange = {},
                                    readOnly = true,
                                    shape = grimdarkBevelShape(4.dp),
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = weaponMenuExpanded) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = InputDark,
                                        unfocusedContainerColor = InputDark,
                                        focusedBorderColor = pal.trimBorder,
                                        unfocusedBorderColor = BorderSteel,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    ),
                                    modifier = Modifier
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                        .fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = weaponMenuExpanded,
                                    onDismissRequest = { weaponMenuExpanded = false },
                                    modifier = Modifier.background(CardSlate)
                                ) {
                                    WarriorWeaponChoice.entries.forEach { choice ->
                                        DropdownMenuItem(
                                            text = { Text(choice.label, color = TextPrimary) },
                                            onClick = {
                                                weaponChoice = choice
                                                range = if (choice == WarriorWeaponChoice.BONESWORDS) "1" else "4"
                                                weaponMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    TwoFieldRow("Pistol Damage", pistolDamage, { pistolDamage = it }, "Pistol AP Cost", pistolApCost, { pistolApCost = it })
                    TwoFieldRow("Pistol Range", pistolRange, { pistolRange = it }, "Pistol Cooldown", pistolCooldown, { pistolCooldown = it })
                }

                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = grimdarkBevelShape(5.dp),
                        border = BorderStroke(1.dp, BorderSteel),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("CANCEL", color = TextPrimary)
                    }
                    Button(
                        onClick = {
                            onSave(
                                template.copy(
                                    points = points.toIntOrNull()?.coerceIn(20, 2000) ?: template.points,
                                    squadSize = squadSize.toIntOrNull()?.coerceIn(1, 5) ?: template.squadSize,
                                    hpPerModel = hpPerModel.toIntOrNull()?.coerceIn(1, 999) ?: template.hpPerModel,
                                    armor = armor.toIntOrNull()?.coerceIn(0, 10) ?: template.armor,
                                    armorPen = armorPen.toIntOrNull()?.coerceIn(0, 10) ?: template.armorPen,
                                    mp = mp.toIntOrNull()?.coerceIn(1, 12) ?: template.mp,
                                    ap = ap.toIntOrNull()?.coerceIn(1, 6) ?: template.ap,
                                    damage = damage.toIntOrNull()?.coerceIn(1, 500) ?: template.damage,
                                    attacksPerModel = attacksPerModel.toIntOrNull()?.coerceIn(1, 6) ?: template.attacksPerModel,
                                    range = range.toIntOrNull()?.coerceIn(1, 12) ?: template.range,
                                    accuracy = accuracy.toIntOrNull()?.coerceIn(5, 100) ?: template.accuracy,
                                    skillCooldown = skillCooldown.toIntOrNull()?.coerceIn(1, 8) ?: template.skillCooldown,
                                    skillApCost = skillApCost.toIntOrNull()?.coerceIn(1, 4) ?: template.skillApCost,
                                    weaponChoice = if (template.archetype == UnitArchetype.WARRIOR) weaponChoice else template.weaponChoice,
                                    pistolDamage = pistolDamage.toIntOrNull()?.coerceIn(0, 200) ?: template.pistolDamage,
                                    pistolApCost = pistolApCost.toIntOrNull()?.coerceIn(1, 4) ?: template.pistolApCost,
                                    pistolRange = pistolRange.toIntOrNull()?.coerceIn(1, 8) ?: template.pistolRange,
                                    pistolCooldown = pistolCooldown.toIntOrNull()?.coerceIn(1, 6) ?: template.pistolCooldown
                                )
                            )
                        },
                        shape = grimdarkBevelShape(5.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldSurface,
                            contentColor = TextPrimary
                        ),
                        border = BorderStroke(1.dp, EmeraldBorder),
                        modifier = Modifier.testTag("stat_save_button")
                    ) {
                        Text("SAVE")
                    }
                }
            }
        }
    }
}

@Composable
private fun TwoFieldRow(
    labelA: String,
    valueA: String,
    onChangeA: (String) -> Unit,
    labelB: String,
    valueB: String,
    onChangeB: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatTextField(labelA, valueA, onChangeA, modifier = Modifier.weight(1f))
        StatTextField(labelB, valueB, onChangeB, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StatTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
        )
        OutlinedTextField(
            value = value,
            onValueChange = { input -> onValueChange(input.filter { it.isDigit() }) },
            singleLine = true,
            shape = grimdarkBevelShape(4.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = MaterialTheme.typography.labelMedium.copy(color = TextPrimary),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = InputDark,
                unfocusedContainerColor = InputDark,
                focusedBorderColor = ImperialGold,
                unfocusedBorderColor = BorderSteel
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
