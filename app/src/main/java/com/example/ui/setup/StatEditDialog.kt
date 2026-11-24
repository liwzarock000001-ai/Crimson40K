package com.example.ui.setup

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.TerminatorWeaponChoice
import com.example.model.UnitArchetype
import com.example.model.UnitTemplate
import com.example.model.WarriorWeaponChoice
import com.example.model.WeaponProfile
import com.example.ui.theme.BorderSteel
import com.example.ui.theme.CardSlate
import com.example.ui.theme.DividerIron
import com.example.ui.theme.EmeraldBuff
import com.example.ui.theme.EmeraldBorder
import com.example.ui.theme.EmeraldSurface
import com.example.ui.theme.GrimdarkBevelPanel
import com.example.ui.theme.ImperialGold
import com.example.ui.theme.InputDark
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.WarmAmber
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
    val isWarrior = template.archetype == UnitArchetype.WARRIOR
    val isTerminator = template.archetype == UnitArchetype.TERMINATOR

    // Core unit profile
    var points by remember(template.id) { mutableStateOf(template.points.toString()) }
    var squadSize by remember(template.id) { mutableStateOf(template.squadSize.toString()) }
    var woundsPerModel by remember(template.id) { mutableStateOf(template.woundsPerModel.toString()) }
    var toughness by remember(template.id) { mutableStateOf(template.toughness.toString()) }
    var save by remember(template.id) { mutableStateOf(template.save.toString()) }
    var invulnSave by remember(template.id) { mutableStateOf((template.invulnSave ?: 0).toString()) }
    var moveCm by remember(template.id) { mutableStateOf(template.moveCm.toString()) }
    var leadership by remember(template.id) { mutableStateOf(template.leadership.toString()) }
    var objectiveControl by remember(template.id) { mutableStateOf(template.objectiveControl.toString()) }
    var skillCooldown by remember(template.id) { mutableStateOf(template.skillCooldown.toString()) }

    // Standard primary weapon
    var armorPen by remember(template.id) { mutableStateOf(template.armorPen.toString()) }
    var strength by remember(template.id) { mutableStateOf(template.strength.toString()) }
    var damage by remember(template.id) { mutableStateOf(template.damage.toString()) }
    var attacksPerModel by remember(template.id) { mutableStateOf(template.attacksPerModel.toString()) }
    var range by remember(template.id) { mutableStateOf(template.range.toString()) }
    var ballisticSkill by remember(template.id) { mutableStateOf(template.ballisticSkill.toString()) }
    var weaponSkill by remember(template.id) { mutableStateOf(template.weaponSkill.toString()) }
    var mainSustainedHits by remember(template.id) { mutableStateOf(template.mainSustainedHits.toString()) }
    var mainWeaponAssault by remember(template.id) { mutableStateOf(template.mainWeaponAssault) }
    var mainWeaponPistol by remember(template.id) { mutableStateOf(template.mainWeaponPistol) }

    // Independent Melee weapon for ranged-main units
    var meleeAttacks by remember(template.id) { mutableStateOf(template.effectiveMeleeAttacks.toString()) }
    var meleeWeaponSkill by remember(template.id) { mutableStateOf(template.effectiveMeleeWeaponSkill.toString()) }
    var meleeStrength by remember(template.id) { mutableStateOf(template.effectiveMeleeStrength.toString()) }
    var meleeArmorPen by remember(template.id) { mutableStateOf(template.effectiveMeleeArmorPen.toString()) }
    var meleeDamage by remember(template.id) { mutableStateOf(template.effectiveMeleeDamage.toString()) }
    var meleeSustainedHits by remember(template.id) { mutableStateOf(template.effectiveMeleeSustainedHits.toString()) }

    // Independent Warrior Weapon 1 (Claws & Talons Melee) & Weapon 2 (Deathspitter Ranged)
    val initWarriorMelee = remember(template.id) {
        template.warriorMeleeProfile ?: WeaponProfile(
            name = "Tyranid Warrior Claws & Talons",
            range = 1,
            attacksPerModel = if (template.range <= 1) template.attacksPerModel else template.effectiveMeleeAttacks,
            skill = if (template.range <= 1) template.weaponSkill else template.effectiveMeleeWeaponSkill,
            strength = if (template.range <= 1) template.strength else template.effectiveMeleeStrength,
            armorPen = if (template.range <= 1) template.armorPen else template.effectiveMeleeArmorPen,
            damage = if (template.range <= 1) template.damage else template.effectiveMeleeDamage,
            sustainedHits = if (template.range <= 1) template.mainSustainedHits else template.meleeSustainedHits
        )
    }
    val initWarriorRanged = remember(template.id) {
        template.warriorRangedProfile ?: WeaponProfile(
            name = "Deathspitter",
            range = if (template.range > 1) template.range else 5,
            attacksPerModel = if (template.range > 1) template.attacksPerModel else 3,
            skill = template.ballisticSkill,
            strength = if (template.range > 1) template.strength else 5,
            armorPen = if (template.range > 1) template.armorPen else 1,
            damage = if (template.range > 1) template.damage else 1,
            sustainedHits = if (template.range > 1) template.mainSustainedHits else 0,
            assault = if (template.range > 1) template.mainWeaponAssault else false,
            pistol = if (template.range > 1) template.mainWeaponPistol else false
        )
    }
    var weaponChoice by remember(template.id) { mutableStateOf(template.weaponChoice ?: WarriorWeaponChoice.BONESWORDS) }
    var weaponMenuExpanded by remember { mutableStateOf(false) }
    var wMeleeA by remember(template.id) { mutableStateOf(initWarriorMelee.attacksPerModel.toString()) }
    var wMeleeWs by remember(template.id) { mutableStateOf(initWarriorMelee.skill.toString()) }
    var wMeleeS by remember(template.id) { mutableStateOf(initWarriorMelee.strength.toString()) }
    var wMeleeAp by remember(template.id) { mutableStateOf(initWarriorMelee.armorPen.toString()) }
    var wMeleeD by remember(template.id) { mutableStateOf(initWarriorMelee.damage.toString()) }
    var wMeleeSus by remember(template.id) { mutableStateOf(initWarriorMelee.sustainedHits.toString()) }

    var wRngRange by remember(template.id) { mutableStateOf(initWarriorRanged.range.toString()) }
    var wRngA by remember(template.id) { mutableStateOf(initWarriorRanged.attacksPerModel.toString()) }
    var wRngBs by remember(template.id) { mutableStateOf(initWarriorRanged.skill.toString()) }
    var wRngS by remember(template.id) { mutableStateOf(initWarriorRanged.strength.toString()) }
    var wRngAp by remember(template.id) { mutableStateOf(initWarriorRanged.armorPen.toString()) }
    var wRngD by remember(template.id) { mutableStateOf(initWarriorRanged.damage.toString()) }
    var wRngSus by remember(template.id) { mutableStateOf(initWarriorRanged.sustainedHits.toString()) }
    var wRngAssault by remember(template.id) { mutableStateOf(initWarriorRanged.assault) }
    var wRngPistol by remember(template.id) { mutableStateOf(initWarriorRanged.pistol) }

    // Independent Terminator Weapon 1 (Hammer & Shield) & Weapon 2 (Twin Lightning Claws)
    val initTermHammer = remember(template.id) {
        template.terminatorHammerProfile ?: WeaponProfile(
            name = "Thunder Hammer & Storm Shield",
            range = 1,
            attacksPerModel = if (template.terminatorWeapon != TerminatorWeaponChoice.LIGHTNING_CLAWS) template.attacksPerModel else 3,
            skill = if (template.terminatorWeapon != TerminatorWeaponChoice.LIGHTNING_CLAWS) template.weaponSkill else 3,
            strength = if (template.terminatorWeapon != TerminatorWeaponChoice.LIGHTNING_CLAWS) template.strength else 8,
            armorPen = if (template.terminatorWeapon != TerminatorWeaponChoice.LIGHTNING_CLAWS) template.armorPen else 2,
            damage = if (template.terminatorWeapon != TerminatorWeaponChoice.LIGHTNING_CLAWS) template.damage else 3,
            sustainedHits = if (template.terminatorWeapon != TerminatorWeaponChoice.LIGHTNING_CLAWS) template.mainSustainedHits else 0
        )
    }
    val initTermClaws = remember(template.id) {
        template.terminatorClawsProfile ?: WeaponProfile(
            name = "Twin Lightning Claws",
            range = 1,
            attacksPerModel = if (template.terminatorWeapon == TerminatorWeaponChoice.LIGHTNING_CLAWS) template.attacksPerModel else 6,
            skill = if (template.terminatorWeapon == TerminatorWeaponChoice.LIGHTNING_CLAWS) template.weaponSkill else 3,
            strength = if (template.terminatorWeapon == TerminatorWeaponChoice.LIGHTNING_CLAWS) template.strength else 5,
            armorPen = if (template.terminatorWeapon == TerminatorWeaponChoice.LIGHTNING_CLAWS) template.armorPen else 2,
            damage = if (template.terminatorWeapon == TerminatorWeaponChoice.LIGHTNING_CLAWS) template.damage else 1,
            sustainedHits = if (template.terminatorWeapon == TerminatorWeaponChoice.LIGHTNING_CLAWS) template.mainSustainedHits else 0
        )
    }
    var terminatorWeapon by remember(template.id) {
        mutableStateOf(template.terminatorWeapon ?: TerminatorWeaponChoice.THUNDER_HAMMER_SHIELD)
    }
    var termWeaponMenuExpanded by remember { mutableStateOf(false) }
    var tHamA by remember(template.id) { mutableStateOf(initTermHammer.attacksPerModel.toString()) }
    var tHamWs by remember(template.id) { mutableStateOf(initTermHammer.skill.toString()) }
    var tHamS by remember(template.id) { mutableStateOf(initTermHammer.strength.toString()) }
    var tHamAp by remember(template.id) { mutableStateOf(initTermHammer.armorPen.toString()) }
    var tHamD by remember(template.id) { mutableStateOf(initTermHammer.damage.toString()) }
    var tHamSus by remember(template.id) { mutableStateOf(initTermHammer.sustainedHits.toString()) }

    var tClawA by remember(template.id) { mutableStateOf(initTermClaws.attacksPerModel.toString()) }
    var tClawWs by remember(template.id) { mutableStateOf(initTermClaws.skill.toString()) }
    var tClawS by remember(template.id) { mutableStateOf(initTermClaws.strength.toString()) }
    var tClawAp by remember(template.id) { mutableStateOf(initTermClaws.armorPen.toString()) }
    var tClawD by remember(template.id) { mutableStateOf(initTermClaws.damage.toString()) }
    var tClawSus by remember(template.id) { mutableStateOf(initTermClaws.sustainedHits.toString()) }

    // Pistol stats
    var pistolAttacks by remember(template.id) { mutableStateOf(template.pistolAttacks.toString()) }
    var pistolDamage by remember(template.id) { mutableStateOf(template.pistolDamage.toString()) }
    var pistolStrength by remember(template.id) { mutableStateOf(template.pistolStrength.toString()) }
    var pistolArmorPen by remember(template.id) { mutableStateOf(template.pistolArmorPen.toString()) }
    var pistolRange by remember(template.id) { mutableStateOf(template.pistolRange.toString()) }
    var pistolCooldown by remember(template.id) { mutableStateOf(template.pistolCooldown.toString()) }
    var pistolSustainedHits by remember(template.id) { mutableStateOf(template.pistolSustainedHits.toString()) }

    // Auxiliary Ranged Weapon (e.g. Twin Icarus ironhail heavy stubber)
    val initAux = template.auxRangedProfile
    var auxRange by remember(template.id) { mutableStateOf((initAux?.range ?: 6).toString()) }
    var auxAttacks by remember(template.id) { mutableStateOf((initAux?.attacksPerModel ?: 3).toString()) }
    var auxSkill by remember(template.id) { mutableStateOf((initAux?.skill ?: 3).toString()) }
    var auxStrength by remember(template.id) { mutableStateOf((initAux?.strength ?: 4).toString()) }
    var auxArmorPen by remember(template.id) { mutableStateOf((initAux?.armorPen ?: 0).toString()) }
    var auxDamage by remember(template.id) { mutableStateOf((initAux?.damage ?: 1).toString()) }

    val isRangedNow = (range.toIntOrNull() ?: template.range) > 1
    val showBsOnNativeMelee = template.hasPistol ||
        template.archetype == UnitArchetype.CARNIFEX ||
        template.archetype == UnitArchetype.HIVE_TYRANT

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            GrimdarkBevelPanel(
                cut = 10.dp,
                borderColor = pal.trimBorder,
                topColor = pal.panelTop,
                bottomColor = pal.panelBottom,
                accentBarColor = pal.primaryAccent,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 620.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
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
                            text = if (template.baseDiameterMm > 32) "UNIT PROFILE • BASE ${template.baseDiameterMm}MM" else "UNIT PROFILE",
                            style = MaterialTheme.typography.labelMedium,
                            color = pal.trimBorder
                        )
                        TwoFieldRow("Point Cost", points, { points = it }, "Squad Size (1-10)", squadSize, { squadSize = it })
                        TwoFieldRow("Wounds / Model (W)", woundsPerModel, { woundsPerModel = it }, "Toughness T (1-14)", toughness, { toughness = it })
                        TwoFieldRow("Armor Save Sv (2-6+)", save, { save = it }, "Invuln (0=None,2-6)", invulnSave, { invulnSave = it })
                        TwoFieldRow("Movement M (cm)", moveCm, { moveCm = it }, "Skill Cooldown", skillCooldown, { skillCooldown = it })
                        TwoFieldRow("Leadership Ld (4-10+)", leadership, { leadership = it }, "Objective Control OC (0-8)", objectiveControl, { objectiveControl = it })

                        when {
                            isWarrior -> {
                                HorizontalDivider(color = DividerIron, modifier = Modifier.padding(top = 4.dp))
                                Text("ACTIVE LOADOUT (WEAPONS SAVED SEPARATELY)", style = MaterialTheme.typography.labelSmall, color = TextMuted)
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
                                                    weaponMenuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                HorizontalDivider(color = DividerIron, modifier = Modifier.padding(top = 4.dp))
                                Text("WEAPON 1: ${initWarriorMelee.name.uppercase()} (MELEE 2CM)", style = MaterialTheme.typography.labelMedium, color = ImperialGold)
                                TwoFieldRow("Melee Attacks (A)", wMeleeA, { wMeleeA = it }, "Weapon WS (2-6+)", wMeleeWs, { wMeleeWs = it })
                                TwoFieldRow("Strength S (1-20)", wMeleeS, { wMeleeS = it }, "Armor Pen (0-5)", wMeleeAp, { wMeleeAp = it })
                                TwoFieldRow("Melee Damage (D)", wMeleeD, { wMeleeD = it }, "Sustained Hits X (0-6)", wMeleeSus, { wMeleeSus = it })

                                HorizontalDivider(color = DividerIron, modifier = Modifier.padding(top = 4.dp))
                                Text("WEAPON 2: ${initWarriorRanged.name.uppercase()} (RANGED)", style = MaterialTheme.typography.labelMedium, color = TacticalCyan)
                                TwoFieldRow("Attack Range (Tiles)", wRngRange, { wRngRange = it }, "Ranged Attacks (A)", wRngA, { wRngA = it })
                                TwoFieldRow("Ballistic BS (2-6+)", wRngBs, { wRngBs = it }, "Strength S (1-20)", wRngS, { wRngS = it })
                                TwoFieldRow("Armor Pen (0-5)", wRngAp, { wRngAp = it }, "Ranged Damage (D)", wRngD, { wRngD = it })
                                StatTextField("Sustained Hits X (0-6)", wRngSus, { wRngSus = it })
                                WeaponTagSwitches(
                                    assaultChecked = wRngAssault,
                                    onAssaultChanged = { wRngAssault = it },
                                    pistolChecked = wRngPistol,
                                    onPistolChanged = { wRngPistol = it }
                                )
                            }

                            isTerminator -> {
                                HorizontalDivider(color = DividerIron, modifier = Modifier.padding(top = 4.dp))
                                Text("ACTIVE LOADOUT (3D MODEL • SAVED SEPARATELY)", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                ExposedDropdownMenuBox(
                                    expanded = termWeaponMenuExpanded,
                                    onExpandedChange = { termWeaponMenuExpanded = it }
                                ) {
                                    OutlinedTextField(
                                        value = terminatorWeapon.label,
                                        onValueChange = {},
                                        readOnly = true,
                                        shape = grimdarkBevelShape(4.dp),
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = termWeaponMenuExpanded) },
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
                                            .testTag("terminator_weapon_dropdown")
                                    )
                                    ExposedDropdownMenu(
                                        expanded = termWeaponMenuExpanded,
                                        onDismissRequest = { termWeaponMenuExpanded = false },
                                        modifier = Modifier.background(CardSlate)
                                    ) {
                                        TerminatorWeaponChoice.entries.forEach { choice ->
                                            DropdownMenuItem(
                                                text = { Text(choice.label, color = TextPrimary) },
                                                onClick = {
                                                    terminatorWeapon = choice
                                                    woundsPerModel = if (choice == TerminatorWeaponChoice.THUNDER_HAMMER_SHIELD) "4" else "3"
                                                    termWeaponMenuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                HorizontalDivider(color = DividerIron, modifier = Modifier.padding(top = 4.dp))
                                Text("WEAPON 1: THUNDER HAMMER & STORM SHIELD", style = MaterialTheme.typography.labelMedium, color = ImperialGold)
                                TwoFieldRow("Melee Attacks (A)", tHamA, { tHamA = it }, "Weapon WS (2-6+)", tHamWs, { tHamWs = it })
                                TwoFieldRow("Strength S (1-20)", tHamS, { tHamS = it }, "Armor Pen (0-5)", tHamAp, { tHamAp = it })
                                TwoFieldRow("Melee Damage (D)", tHamD, { tHamD = it }, "Sustained Hits X (0-6)", tHamSus, { tHamSus = it })

                                HorizontalDivider(color = DividerIron, modifier = Modifier.padding(top = 4.dp))
                                Text("WEAPON 2: TWIN LIGHTNING CLAWS", style = MaterialTheme.typography.labelMedium, color = TacticalCyan)
                                TwoFieldRow("Melee Attacks (A)", tClawA, { tClawA = it }, "Weapon WS (2-6+)", tClawWs, { tClawWs = it })
                                TwoFieldRow("Strength S (1-20)", tClawS, { tClawS = it }, "Armor Pen (0-5)", tClawAp, { tClawAp = it })
                                TwoFieldRow("Melee Damage (D)", tClawD, { tClawD = it }, "Sustained Hits X (0-6)", tClawSus, { tClawSus = it })
                            }

                            isRangedNow -> {
                                HorizontalDivider(color = DividerIron, modifier = Modifier.padding(top = 4.dp))
                                Text(
                                    text = "RANGED WEAPON: ${template.mainWeaponName.uppercase()}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ImperialGold
                                )
                                TwoFieldRow("Attack Range (Tiles)", range, { range = it }, "Attacks / Model (A)", attacksPerModel, { attacksPerModel = it })
                                TwoFieldRow("Ballistic BS (2-6+)", ballisticSkill, { ballisticSkill = it }, "Strength S (1-20)", strength, { strength = it })
                                TwoFieldRow("Armor Pen (0-5)", armorPen, { armorPen = it }, "Ranged Damage (D)", damage, { damage = it })
                                StatTextField("Sustained Hits X (0-6)", mainSustainedHits, { mainSustainedHits = it })
                                WeaponTagSwitches(
                                    assaultChecked = mainWeaponAssault,
                                    onAssaultChanged = { mainWeaponAssault = it },
                                    pistolChecked = mainWeaponPistol,
                                    onPistolChanged = { mainWeaponPistol = it }
                                )

                                HorizontalDivider(color = DividerIron, modifier = Modifier.padding(top = 4.dp))
                                Text(
                                    text = "MELEE WEAPON: ${template.effectiveMeleeWeaponName.uppercase()} (2CM)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = WarmAmber
                                )
                                TwoFieldRow("Melee Attacks (A)", meleeAttacks, { meleeAttacks = it }, "Melee WS (2-6+)", meleeWeaponSkill, { meleeWeaponSkill = it })
                                TwoFieldRow("Melee Strength S", meleeStrength, { meleeStrength = it }, "Melee Pen (0-5)", meleeArmorPen, { meleeArmorPen = it })
                                TwoFieldRow("Melee Damage (D)", meleeDamage, { meleeDamage = it }, "Sustained Hits X (0-6)", meleeSustainedHits, { meleeSustainedHits = it })
                            }

                            else -> {
                                HorizontalDivider(color = DividerIron, modifier = Modifier.padding(top = 4.dp))
                                Text(
                                    text = "MELEE WEAPON: ${template.mainWeaponName.uppercase()}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ImperialGold
                                )
                                TwoFieldRow("Range (1 = Melee 2cm)", range, { range = it }, "Attacks / Model (A)", attacksPerModel, { attacksPerModel = it })
                                if (showBsOnNativeMelee) {
                                    TwoFieldRow("Weapon WS (2-6+)", weaponSkill, { weaponSkill = it }, "Ballistic BS (2-6+)", ballisticSkill, { ballisticSkill = it })
                                } else {
                                    StatTextField("Weapon WS (2-6+)", weaponSkill, { weaponSkill = it })
                                }
                                TwoFieldRow("Strength S (1-20)", strength, { strength = it }, "Armor Pen (0-5)", armorPen, { armorPen = it })
                                TwoFieldRow("Main Damage (D)", damage, { damage = it }, "Sustained Hits X (0-6)", mainSustainedHits, { mainSustainedHits = it })
                            }
                        }

                        if (template.hasPistol) {
                            HorizontalDivider(color = DividerIron, modifier = Modifier.padding(top = 4.dp))
                            val secHeader = if (template.pistolIsPistol) "PISTOL" else "RANGED WEAPON 1"
                            Text(
                                text = "$secHeader: ${template.pistolWeaponName.uppercase()}",
                                style = MaterialTheme.typography.labelMedium,
                                color = pal.trimBorder
                            )
                            TwoFieldRow("Attacks (A)", pistolAttacks, { pistolAttacks = it }, "Damage (D)", pistolDamage, { pistolDamage = it })
                            TwoFieldRow("Strength S", pistolStrength, { pistolStrength = it }, "Armor Pen (0-5)", pistolArmorPen, { pistolArmorPen = it })
                            TwoFieldRow("Range (Tiles)", pistolRange, { pistolRange = it }, "Cooldown", pistolCooldown, { pistolCooldown = it })
                            StatTextField("Sustained Hits X (0-6)", pistolSustainedHits, { pistolSustainedHits = it })
                        }

                        if (initAux != null) {
                            HorizontalDivider(color = DividerIron, modifier = Modifier.padding(top = 4.dp))
                            Text(
                                text = "RANGED WEAPON 2: ${initAux.name.uppercase()}",
                                style = MaterialTheme.typography.labelMedium,
                                color = TacticalCyan
                            )
                            TwoFieldRow("Range (Tiles)", auxRange, { auxRange = it }, "Attacks (A)", auxAttacks, { auxAttacks = it })
                            TwoFieldRow("Ballistic BS (2-6+)", auxSkill, { auxSkill = it }, "Strength S (1-20)", auxStrength, { auxStrength = it })
                            TwoFieldRow("Armor Pen (0-5)", auxArmorPen, { auxArmorPen = it }, "Damage (D)", auxDamage, { auxDamage = it })
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
                                val commonPoints = points.toIntOrNull()?.coerceIn(20, 2000) ?: template.points
                                val commonSquad = squadSize.toIntOrNull()?.coerceIn(1, 10) ?: template.squadSize
                                val commonWounds = woundsPerModel.toIntOrNull()?.coerceIn(1, 30) ?: template.woundsPerModel
                                val commonTough = toughness.toIntOrNull()?.coerceIn(1, 14) ?: template.toughness
                                val commonSave = save.toIntOrNull()?.coerceIn(2, 6) ?: template.save
                                val commonMove = moveCm.toIntOrNull()?.coerceIn(3, 45) ?: template.moveCm
                                val commonLd = leadership.toIntOrNull()?.coerceIn(4, 10) ?: template.leadership
                                val commonOc = objectiveControl.toIntOrNull()?.coerceIn(0, 8) ?: template.objectiveControl
                                val commonCd = skillCooldown.toIntOrNull()?.coerceIn(1, 8) ?: template.skillCooldown
                                val savedPistolA = pistolAttacks.toIntOrNull()?.coerceIn(1, 10) ?: template.pistolAttacks
                                val savedPistolD = pistolDamage.toIntOrNull()?.coerceIn(0, 200) ?: template.pistolDamage
                                val savedPistolS = pistolStrength.toIntOrNull()?.coerceIn(1, 20) ?: template.pistolStrength
                                val savedPistolAp = pistolArmorPen.toIntOrNull()?.coerceIn(0, 5) ?: template.pistolArmorPen
                                val savedPistolR = pistolRange.toIntOrNull()?.coerceIn(1, 8) ?: template.pistolRange
                                val savedPistolCd = pistolCooldown.toIntOrNull()?.coerceIn(1, 6) ?: template.pistolCooldown
                                val savedPistolSus = pistolSustainedHits.toIntOrNull()?.coerceIn(0, 6) ?: template.pistolSustainedHits

                                val savedAux = initAux?.copy(
                                    range = auxRange.toIntOrNull()?.coerceIn(2, 12) ?: initAux.range,
                                    attacksPerModel = auxAttacks.toIntOrNull()?.coerceIn(1, 12) ?: initAux.attacksPerModel,
                                    skill = auxSkill.toIntOrNull()?.coerceIn(2, 6) ?: initAux.skill,
                                    strength = auxStrength.toIntOrNull()?.coerceIn(1, 20) ?: initAux.strength,
                                    armorPen = auxArmorPen.toIntOrNull()?.coerceIn(0, 5) ?: initAux.armorPen,
                                    damage = auxDamage.toIntOrNull()?.coerceIn(1, 500) ?: initAux.damage
                                )

                                val baseSaved = template.copy(
                                    points = commonPoints,
                                    squadSize = commonSquad,
                                    woundsPerModel = commonWounds,
                                    toughness = commonTough,
                                    save = commonSave,
                                    invulnSave = parsedInv,
                                    moveCm = commonMove,
                                    leadership = commonLd,
                                    objectiveControl = commonOc,
                                    skillCooldown = commonCd,
                                    pistolAttacks = savedPistolA,
                                    pistolDamage = savedPistolD,
                                    pistolStrength = savedPistolS,
                                    pistolArmorPen = savedPistolAp,
                                    pistolRange = savedPistolR,
                                    pistolCooldown = savedPistolCd,
                                    pistolSustainedHits = savedPistolSus,
                                    auxRangedProfile = savedAux
                                )

                                val savedTemplate = when {
                                    isWarrior -> {
                                        val savedMelee = WeaponProfile(
                                            name = initWarriorMelee.name,
                                            range = 1,
                                            attacksPerModel = wMeleeA.toIntOrNull()?.coerceIn(1, 10) ?: initWarriorMelee.attacksPerModel,
                                            skill = wMeleeWs.toIntOrNull()?.coerceIn(2, 6) ?: initWarriorMelee.skill,
                                            strength = wMeleeS.toIntOrNull()?.coerceIn(1, 20) ?: initWarriorMelee.strength,
                                            armorPen = wMeleeAp.toIntOrNull()?.coerceIn(0, 5) ?: initWarriorMelee.armorPen,
                                            damage = wMeleeD.toIntOrNull()?.coerceIn(1, 500) ?: initWarriorMelee.damage,
                                            sustainedHits = wMeleeSus.toIntOrNull()?.coerceIn(0, 6) ?: initWarriorMelee.sustainedHits
                                        )
                                        val savedRanged = WeaponProfile(
                                            name = initWarriorRanged.name,
                                            range = wRngRange.toIntOrNull()?.coerceIn(2, 12) ?: initWarriorRanged.range,
                                            attacksPerModel = wRngA.toIntOrNull()?.coerceIn(1, 10) ?: initWarriorRanged.attacksPerModel,
                                            skill = wRngBs.toIntOrNull()?.coerceIn(2, 6) ?: initWarriorRanged.skill,
                                            strength = wRngS.toIntOrNull()?.coerceIn(1, 20) ?: initWarriorRanged.strength,
                                            armorPen = wRngAp.toIntOrNull()?.coerceIn(0, 5) ?: initWarriorRanged.armorPen,
                                            damage = wRngD.toIntOrNull()?.coerceIn(1, 500) ?: initWarriorRanged.damage,
                                            sustainedHits = wRngSus.toIntOrNull()?.coerceIn(0, 6) ?: initWarriorRanged.sustainedHits,
                                            assault = wRngAssault,
                                            pistol = wRngPistol
                                        )
                                        val active = if (weaponChoice == WarriorWeaponChoice.BONESWORDS) savedMelee else savedRanged
                                        baseSaved.copy(
                                            weaponChoice = weaponChoice,
                                            range = active.range,
                                            attacksPerModel = active.attacksPerModel,
                                            ballisticSkill = savedRanged.skill,
                                            weaponSkill = savedMelee.skill,
                                            strength = active.strength,
                                            armorPen = active.armorPen,
                                            damage = active.damage,
                                            mainWeaponName = active.name,
                                            mainWeaponAssault = active.assault,
                                            mainWeaponPistol = active.pistol,
                                            mainSustainedHits = active.sustainedHits,
                                            meleeWeaponName = savedMelee.name,
                                            meleeAttacks = savedMelee.attacksPerModel,
                                            meleeWeaponSkill = savedMelee.skill,
                                            meleeStrength = savedMelee.strength,
                                            meleeArmorPen = savedMelee.armorPen,
                                            meleeDamage = savedMelee.damage,
                                            meleeSustainedHits = savedMelee.sustainedHits,
                                            warriorMeleeProfile = savedMelee,
                                            warriorRangedProfile = savedRanged
                                        )
                                    }

                                    isTerminator -> {
                                        val savedHammer = WeaponProfile(
                                            name = "Thunder Hammer & Storm Shield",
                                            range = 1,
                                            attacksPerModel = tHamA.toIntOrNull()?.coerceIn(1, 10) ?: initTermHammer.attacksPerModel,
                                            skill = tHamWs.toIntOrNull()?.coerceIn(2, 6) ?: initTermHammer.skill,
                                            strength = tHamS.toIntOrNull()?.coerceIn(1, 20) ?: initTermHammer.strength,
                                            armorPen = tHamAp.toIntOrNull()?.coerceIn(0, 5) ?: initTermHammer.armorPen,
                                            damage = tHamD.toIntOrNull()?.coerceIn(1, 500) ?: initTermHammer.damage,
                                            sustainedHits = tHamSus.toIntOrNull()?.coerceIn(0, 6) ?: initTermHammer.sustainedHits
                                        )
                                        val savedClaws = WeaponProfile(
                                            name = "Twin Lightning Claws",
                                            range = 1,
                                            attacksPerModel = tClawA.toIntOrNull()?.coerceIn(1, 10) ?: initTermClaws.attacksPerModel,
                                            skill = tClawWs.toIntOrNull()?.coerceIn(2, 6) ?: initTermClaws.skill,
                                            strength = tClawS.toIntOrNull()?.coerceIn(1, 20) ?: initTermClaws.strength,
                                            armorPen = tClawAp.toIntOrNull()?.coerceIn(0, 5) ?: initTermClaws.armorPen,
                                            damage = tClawD.toIntOrNull()?.coerceIn(1, 500) ?: initTermClaws.damage,
                                            sustainedHits = tClawSus.toIntOrNull()?.coerceIn(0, 6) ?: initTermClaws.sustainedHits
                                        )
                                        val active = if (terminatorWeapon == TerminatorWeaponChoice.THUNDER_HAMMER_SHIELD) savedHammer else savedClaws
                                        baseSaved.copy(
                                            terminatorWeapon = terminatorWeapon,
                                            range = 1,
                                            attacksPerModel = active.attacksPerModel,
                                            weaponSkill = active.skill,
                                            strength = active.strength,
                                            armorPen = active.armorPen,
                                            damage = active.damage,
                                            mainWeaponName = active.name,
                                            mainWeaponAssault = false,
                                            mainWeaponPistol = false,
                                            mainSustainedHits = active.sustainedHits,
                                            terminatorHammerProfile = savedHammer,
                                            terminatorClawsProfile = savedClaws
                                        )
                                    }

                                    else -> {
                                        val finalRange = range.toIntOrNull()?.coerceIn(1, 12) ?: template.range
                                        val savedBs = ballisticSkill.toIntOrNull()?.coerceIn(2, 6) ?: template.ballisticSkill
                                        val savedWs = weaponSkill.toIntOrNull()?.coerceIn(2, 6) ?: template.weaponSkill
                                        val savedMainA = attacksPerModel.toIntOrNull()?.coerceIn(1, 10) ?: template.attacksPerModel
                                        val savedMainS = strength.toIntOrNull()?.coerceIn(1, 20) ?: template.strength
                                        val savedMainAp = armorPen.toIntOrNull()?.coerceIn(0, 5) ?: template.armorPen
                                        val savedMainD = damage.toIntOrNull()?.coerceIn(1, 500) ?: template.damage
                                        val savedMainSus = mainSustainedHits.toIntOrNull()?.coerceIn(0, 6) ?: template.mainSustainedHits
                                        val savedMeleeA = meleeAttacks.toIntOrNull()?.coerceIn(1, 10) ?: template.effectiveMeleeAttacks
                                        val savedMeleeWs = meleeWeaponSkill.toIntOrNull()?.coerceIn(2, 6) ?: template.effectiveMeleeWeaponSkill
                                        val savedMeleeS = meleeStrength.toIntOrNull()?.coerceIn(1, 20) ?: template.effectiveMeleeStrength
                                        val savedMeleeAp = meleeArmorPen.toIntOrNull()?.coerceIn(0, 5) ?: template.effectiveMeleeArmorPen
                                        val savedMeleeD = meleeDamage.toIntOrNull()?.coerceIn(1, 500) ?: template.effectiveMeleeDamage
                                        val savedMeleeSus = meleeSustainedHits.toIntOrNull()?.coerceIn(0, 6) ?: template.effectiveMeleeSustainedHits

                                        baseSaved.copy(
                                            range = finalRange,
                                            attacksPerModel = savedMainA,
                                            ballisticSkill = savedBs,
                                            weaponSkill = if (finalRange > 1) savedMeleeWs else savedWs,
                                            strength = savedMainS,
                                            armorPen = savedMainAp,
                                            damage = savedMainD,
                                            mainWeaponAssault = if (finalRange > 1) mainWeaponAssault else false,
                                            mainWeaponPistol = if (finalRange > 1) mainWeaponPistol else false,
                                            mainSustainedHits = savedMainSus,
                                            meleeWeaponName = template.effectiveMeleeWeaponName,
                                            meleeAttacks = if (finalRange > 1) savedMeleeA else savedMainA,
                                            meleeWeaponSkill = if (finalRange > 1) savedMeleeWs else savedWs,
                                            meleeStrength = if (finalRange > 1) savedMeleeS else savedMainS,
                                            meleeArmorPen = if (finalRange > 1) savedMeleeAp else savedMainAp,
                                            meleeDamage = if (finalRange > 1) savedMeleeD else savedMainD,
                                            meleeSustainedHits = if (finalRange > 1) savedMeleeSus else savedMainSus
                                        )
                                    }
                                }
                                onSave(savedTemplate)
                            },
                            shape = grimdarkBevelShape(5.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldSurface,
                                contentColor = EmeraldBuff
                            ),
                            border = BorderStroke(1.2.dp, EmeraldBorder),
                            modifier = Modifier.testTag("stat_save_button")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("SAVE STAT", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeaponTagSwitches(
    assaultChecked: Boolean,
    onAssaultChanged: (Boolean) -> Unit,
    pistolChecked: Boolean,
    onPistolChanged: (Boolean) -> Unit
) {
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
        Switch(
            checked = assaultChecked,
            onCheckedChange = onAssaultChanged,
            modifier = Modifier.testTag("assault_tag_switch")
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "PISTOL WEAPON TAG",
                style = MaterialTheme.typography.labelSmall,
                color = TextPrimary
            )
            Text(
                text = "Allows shooting even while engaged in Melee",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }
        Switch(
            checked = pistolChecked,
            onCheckedChange = onPistolChanged,
            modifier = Modifier.testTag("pistol_tag_switch")
        )
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
