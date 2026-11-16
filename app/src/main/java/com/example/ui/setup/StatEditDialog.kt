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
    var woundsPerModel by remember(template.id) { mutableStateOf(template.woundsPerModel.toString()) }
    var toughness by remember(template.id) { mutableStateOf(template.toughness.toString()) }
    var save by remember(template.id) { mutableStateOf(template.save.toString()) }
    var invulnSave by remember(template.id) { mutableStateOf((template.invulnSave ?: 0).toString()) }
    var armorPen by remember(template.id) { mutableStateOf(template.armorPen.toString()) }
    var moveCm by remember(template.id) { mutableStateOf(template.moveCm.toString()) }
    var strength by remember(template.id) { mutableStateOf(template.strength.toString()) }
    var damage by remember(template.id) { mutableStateOf(template.damage.toString()) }
    var attacksPerModel by remember(template.id) { mutableStateOf(template.attacksPerModel.toString()) }
    var range by remember(template.id) { mutableStateOf(template.range.toString()) }
    var ballisticSkill by remember(template.id) { mutableStateOf(template.ballisticSkill.toString()) }
    var weaponSkill by remember(template.id) { mutableStateOf(template.weaponSkill.toString()) }
    var skillCooldown by remember(template.id) { mutableStateOf(template.skillCooldown.toString()) }
    var weaponChoice by remember(template.id) {
        mutableStateOf(template.weaponChoice ?: WarriorWeaponChoice.BONESWORDS)
    }
    var mainWeaponAssault by remember(template.id) { mutableStateOf(template.mainWeaponAssault) }
    var mainWeaponName by remember(template.id) { mutableStateOf(template.mainWeaponName) }
    var weaponMenuExpanded by remember { mutableStateOf(false) }

    var pistolAttacks by remember(template.id) { mutableStateOf(template.pistolAttacks.toString()) }
    var pistolDamage by remember(template.id) { mutableStateOf(template.pistolDamage.toString()) }
    var pistolStrength by remember(template.id) { mutableStateOf(template.pistolStrength.toString()) }
    var pistolArmorPen by remember(template.id) { mutableStateOf(template.pistolArmorPen.toString()) }
    var pistolRange by remember(template.id) { mutableStateOf(template.pistolRange.toString()) }
    var pistolCooldown by remember(template.id) { mutableStateOf(template.pistolCooldown.toString()) }

    val isRangedNow = (range.toIntOrNull() ?: template.range) > 1
    val showBsField = isRangedNow || template.hasPistol ||
        template.archetype == UnitArchetype.CARNIFEX ||
        template.archetype == UnitArchetype.HIVE_TYRANT

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
                    Text(
                        text = "UNIT PROFILE",
                        style = MaterialTheme.typography.labelMedium,
                        color = pal.trimBorder
                    )
                    TwoFieldRow("Point Cost", points, { points = it }, "Squad Size (1-5)", squadSize, { squadSize = it })
                    TwoFieldRow("Wounds / Model (W)", woundsPerModel, { woundsPerModel = it }, "Toughness T (1-14)", toughness, { toughness = it })
                    TwoFieldRow("Armor Save Sv (2-6+)", save, { save = it }, "Invuln (0=None,2-6)", invulnSave, { invulnSave = it })
                    TwoFieldRow("Movement M (cm)", moveCm, { moveCm = it }, "Skill Cooldown", skillCooldown, { skillCooldown = it })

                    HorizontalDivider(color = DividerIron, modifier = Modifier.padding(top = 4.dp))
                    Text(
                        text = "MAIN WEAPON: ${mainWeaponName.uppercase()}",
                        style = MaterialTheme.typography.labelMedium,
                        color = ImperialGold
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
                                                if (choice == WarriorWeaponChoice.BONESWORDS) {
                                                    range = "1"
                                                    mainWeaponName = "Tyranid Boneswords"
                                                    mainWeaponAssault = false
                                                } else {
                                                    range = "4"
                                                    mainWeaponName = "Fleshborer"
                                                    mainWeaponAssault = true
                                                }
                                                weaponMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    TwoFieldRow("Attack Range", range, { range = it }, "Attacks / Model (A)", attacksPerModel, { attacksPerModel = it })
                    if (showBsField) {
                        TwoFieldRow("Ballistic BS (2-6+)", ballisticSkill, { ballisticSkill = it }, "Weapon WS (2-6+)", weaponSkill, { weaponSkill = it })
                    } else {
                        StatTextField("Weapon WS (2-6+)", weaponSkill, { weaponSkill = it })
                    }
                    TwoFieldRow("Strength S (1-20)", strength, { strength = it }, "Armor Pen (0-5)", armorPen, { armorPen = it })
                    StatTextField("Main Damage (D)", damage, { damage = it })

                    if (isRangedNow) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "ASSAULT WEAPON TAG",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Allows shooting after Advance (cannot Charge)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                            androidx.compose.material3.Switch(
                                checked = mainWeaponAssault,
                                onCheckedChange = { mainWeaponAssault = it },
                                modifier = Modifier.testTag("assault_tag_switch")
                            )
                        }
                    }

                    if (template.hasPistol) {
                        HorizontalDivider(color = DividerIron, modifier = Modifier.padding(top = 4.dp))
                        Text(
                            text = "PISTOL: ${template.pistolWeaponName.uppercase()}",
                            style = MaterialTheme.typography.labelMedium,
                            color = pal.trimBorder
                        )
                        TwoFieldRow("Pistol Attacks (A)", pistolAttacks, { pistolAttacks = it }, "Pistol Damage (D)", pistolDamage, { pistolDamage = it })
                        TwoFieldRow("Pistol Strength S", pistolStrength, { pistolStrength = it }, "Pistol Pen (0-5)", pistolArmorPen, { pistolArmorPen = it })
                        TwoFieldRow("Pistol Range", pistolRange, { pistolRange = it }, "Pistol Cooldown", pistolCooldown, { pistolCooldown = it })
                    }
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
                            val parsedInv = invulnSave.toIntOrNull()?.let { if (it in 2..6) it else null }
                            val finalRange = range.toIntOrNull()?.coerceIn(1, 12) ?: template.range
                            onSave(
                                template.copy(
                                    points = points.toIntOrNull()?.coerceIn(20, 2000) ?: template.points,
                                    squadSize = squadSize.toIntOrNull()?.coerceIn(1, 5) ?: template.squadSize,
                                    woundsPerModel = woundsPerModel.toIntOrNull()?.coerceIn(1, 30) ?: template.woundsPerModel,
                                    toughness = toughness.toIntOrNull()?.coerceIn(1, 14) ?: template.toughness,
                                    save = save.toIntOrNull()?.coerceIn(2, 6) ?: template.save,
                                    invulnSave = parsedInv,
                                    armorPen = armorPen.toIntOrNull()?.coerceIn(0, 5) ?: template.armorPen,
                                    moveCm = moveCm.toIntOrNull()?.coerceIn(3, 36) ?: template.moveCm,
                                    strength = strength.toIntOrNull()?.coerceIn(1, 20) ?: template.strength,
                                    damage = damage.toIntOrNull()?.coerceIn(1, 500) ?: template.damage,
                                    attacksPerModel = attacksPerModel.toIntOrNull()?.coerceIn(1, 10) ?: template.attacksPerModel,
                                    range = finalRange,
                                    ballisticSkill = ballisticSkill.toIntOrNull()?.coerceIn(2, 6) ?: template.ballisticSkill,
                                    weaponSkill = weaponSkill.toIntOrNull()?.coerceIn(2, 6) ?: template.weaponSkill,
                                    skillCooldown = skillCooldown.toIntOrNull()?.coerceIn(1, 8) ?: template.skillCooldown,
                                    weaponChoice = if (template.archetype == UnitArchetype.WARRIOR) weaponChoice else template.weaponChoice,
                                    mainWeaponName = mainWeaponName,
                                    mainWeaponAssault = if (finalRange > 1) mainWeaponAssault else false,
                                    pistolAttacks = pistolAttacks.toIntOrNull()?.coerceIn(1, 10) ?: template.pistolAttacks,
                                    pistolDamage = pistolDamage.toIntOrNull()?.coerceIn(0, 200) ?: template.pistolDamage,
                                    pistolStrength = pistolStrength.toIntOrNull()?.coerceIn(1, 20) ?: template.pistolStrength,
                                    pistolArmorPen = pistolArmorPen.toIntOrNull()?.coerceIn(0, 5) ?: template.pistolArmorPen,
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
