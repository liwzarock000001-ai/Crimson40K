package com.example.engine

import com.example.model.Faction
import com.example.model.RosterSlot
import com.example.model.TerminatorWeaponChoice
import com.example.model.UnitArchetype
import com.example.model.UnitTemplate
import com.example.model.WarriorWeaponChoice
import org.json.JSONArray
import org.json.JSONObject

object DefaultCatalog {

    const val CATALOG_EDITION = 15

    val defaultTemplates: List<UnitTemplate> = listOf(
        // --- ULTRAMARINES (Warhammer 40,000 11th Edition) ---
        UnitTemplate(
            id = "intercessors",
            name = "Intercessor Squad",
            faction = Faction.MARINES,
            archetype = UnitArchetype.INTERCESSOR,
            role = "Battleline Infantry",
            points = 85,
            squadSize = 5,
            woundsPerModel = 2,
            toughness = 4,
            save = 3,
            invulnSave = null,
            armorPen = 1,
            moveCm = 15, // 6"
            strength = 5,
            damage = 1,
            attacksPerModel = 2,
            range = 5, // 24"
            ballisticSkill = 3,
            weaponSkill = 3,
            leadership = 6,
            objectiveControl = 2,
            skillName = "Bolt Volley",
            skillDesc = "Rapid Fire 1 saturation (+1 attack per model, +1 to Hit roll).",
            skillCooldown = 2,
            passiveName = "Bolter Discipline",
            passiveDesc = "Bolt Rifle (S5 AP-1 D1 Assault) • +1 to Hit (BS) when stationary.",
            mainWeaponName = "Bolt Rifle",
            mainWeaponAssault = true,
            meleeWeaponName = "Knives and Fists",
            meleeAttacks = 3,
            meleeWeaponSkill = 3,
            meleeStrength = 5,
            meleeArmorPen = 0,
            meleeDamage = 1,
            pistolWeaponName = "Bolt Pistol",
            pistolAttacks = 1,
            pistolDamage = 1,
            pistolStrength = 4,
            pistolArmorPen = 0,
            pistolRange = 3, // 12"
            pistolCooldown = 1
        ),
        UnitTemplate(
            id = "incursors",
            name = "Incursor Squad",
            faction = Faction.MARINES,
            archetype = UnitArchetype.INCURSOR,
            role = "Phobos Recon",
            points = 90,
            squadSize = 5,
            woundsPerModel = 2,
            toughness = 4,
            save = 3,
            invulnSave = null,
            armorPen = 1,
            moveCm = 20, // 8"
            strength = 5,
            damage = 1,
            attacksPerModel = 2,
            range = 5, // 24"
            ballisticSkill = 3,
            weaponSkill = 3,
            leadership = 6,
            objectiveControl = 1,
            skillName = "Multi-Spectrum Array",
            skillDesc = "Fires Occulus Bolt Carbine (Ignores Cover) & Marks target enemy (+1 to Hit for allied ranged attacks). Cooldown: 1 Turn.",
            skillCooldown = 1,
            passiveName = "Occulus Targeter",
            passiveDesc = "M 8\" (20 cm), Ignores Cover S5 AP-1 Assault Carbine & Sustained Hits 1 S5 AP-1 Paired Combat Blades.",
            mainWeaponName = "Occulus Bolt Carbine",
            mainWeaponAssault = true,
            mainSustainedHits = 0,
            meleeWeaponName = "Paired Combat Blades",
            meleeAttacks = 3,
            meleeWeaponSkill = 3,
            meleeStrength = 5,
            meleeArmorPen = 1,
            meleeDamage = 1,
            meleeSustainedHits = 1,
            pistolWeaponName = "Bolt Pistol",
            pistolAttacks = 1,
            pistolDamage = 1,
            pistolStrength = 5,
            pistolArmorPen = 1,
            pistolRange = 3, // 12"
            pistolCooldown = 1
        ),
        UnitTemplate(
            id = "hellblasters",
            name = "Hellblaster Squad",
            faction = Faction.MARINES,
            archetype = UnitArchetype.HELLBLASTER,
            role = "Heavy Plasma",
            points = 115,
            squadSize = 5,
            woundsPerModel = 2,
            toughness = 5,
            save = 3,
            invulnSave = null,
            armorPen = 2,
            moveCm = 15, // 6"
            strength = 7,
            damage = 2,
            attacksPerModel = 2,
            range = 5, // 24"
            ballisticSkill = 3,
            weaponSkill = 3,
            leadership = 6,
            objectiveControl = 1,
            skillName = "Plasma Overcharge",
            skillDesc = "Supercharged plasma (+1 S, +1 AP -> S8 AP-3, D2).",
            skillCooldown = 2,
            passiveName = "Plasma Specialists",
            passiveDesc = "T5, Assault Plasma Incinerators (S7 AP-2 D2) & Ceramite Fists (S5 WS3+).",
            mainWeaponName = "Plasma Incinerator",
            mainWeaponAssault = true,
            meleeWeaponName = "Ceramite Fists",
            meleeAttacks = 3,
            meleeWeaponSkill = 3,
            meleeStrength = 5,
            meleeArmorPen = 0,
            meleeDamage = 1,
            pistolWeaponName = "Plasma Pistol",
            pistolAttacks = 1,
            pistolDamage = 1,
            pistolStrength = 7,
            pistolArmorPen = 2,
            pistolRange = 3, // 12"
            pistolCooldown = 1
        ),
        UnitTemplate(
            id = "assault_intercessors",
            name = "Assault Intercessors",
            faction = Faction.MARINES,
            archetype = UnitArchetype.ASSAULT_MARINE,
            role = "Battleline Melee",
            points = 85,
            squadSize = 5,
            woundsPerModel = 2,
            toughness = 4,
            save = 3,
            invulnSave = null,
            armorPen = 1,
            moveCm = 15, // 6"
            strength = 4,
            damage = 1,
            attacksPerModel = 4,
            range = 1,
            ballisticSkill = 3,
            weaponSkill = 3,
            leadership = 6,
            objectiveControl = 2,
            skillName = "Targeted Intercession",
            skillDesc = "Requires completing a Charge into melee this turn. Grants +1 Strength and +1 AP (S5 AP-2) to melee weapons until end of turn.",
            skillCooldown = 1,
            passiveName = "Close Support",
            passiveDesc = "Fires Heavy Bolt Pistol (18\") then charges with 4x S4 AP-1 Astartes Chainsword strikes.",
            mainWeaponName = "Astartes Chainsword",
            mainWeaponAssault = false,
            pistolWeaponName = "Heavy Bolt Pistol",
            pistolAttacks = 1,
            pistolDamage = 1,
            pistolStrength = 4,
            pistolArmorPen = 1,
            pistolRange = 4, // 18"
            pistolCooldown = 1
        ),
        UnitTemplate(
            id = "bladeguard_veterans",
            name = "Bladeguard Veterans",
            faction = Faction.MARINES,
            archetype = UnitArchetype.BLADEGUARD,
            role = "Veteran Shieldwall",
            points = 90,
            squadSize = 3,
            woundsPerModel = 3,
            toughness = 4,
            save = 3,
            invulnSave = 4,
            armorPen = 2,
            moveCm = 15, // 6"
            strength = 5,
            damage = 2,
            attacksPerModel = 4,
            range = 1,
            ballisticSkill = 3,
            weaponSkill = 3,
            leadership = 6,
            objectiveControl = 1,
            skillName = "Swords & Shields",
            skillDesc = "Active for this turn: Re-roll Hit rolls of 1 and Saving Throw rolls of 1 (including vs Enemy Counter-Attacks). Cooldown: 1 Turn.",
            skillCooldown = 1,
            passiveName = "Storm Shield • 4++ Invuln",
            passiveDesc = "Relic Storm Shield grants a 4++ Invulnerable Save. 3W, T4, 4A WS3+ S5 AP-2 D2 Master-crafted Power Weapons & Heavy Bolt Pistol.",
            mainWeaponName = "Master-crafted Power Weapon",
            mainWeaponAssault = false,
            pistolWeaponName = "Heavy Bolt Pistol",
            pistolAttacks = 1,
            pistolDamage = 1,
            pistolStrength = 4,
            pistolArmorPen = 1,
            pistolRange = 4, // 18"
            pistolCooldown = 1
        ),
        UnitTemplate(
            id = "captain_titus",
            name = "Captain Titus",
            faction = Faction.MARINES,
            archetype = UnitArchetype.CAPTAIN_TITUS,
            role = "HERO • Chapter Champion",
            points = 105,
            squadSize = 1,
            woundsPerModel = 5,
            toughness = 5,
            save = 2,
            invulnSave = 4,
            armorPen = 1,
            moveCm = 15,
            strength = 5,
            damage = 2,
            attacksPerModel = 2,
            range = 5, // 24"
            ballisticSkill = 2,
            weaponSkill = 2,
            leadership = 5,
            objectiveControl = 2,
            isHero = true,
            feelNoPainTarget = 5,
            skillName = "Honour of Ultramar (Auto)",
            skillDesc = "Auto Passive — Timing: Fight phase, this model dies. Condition: Its unit was not yet selected to fight in the phase. Result: Roll 1D6; on 2+, model stays on the battlefield and leaves as soon as its unit finishes fighting (or when the phase closes).",
            skillCooldown = 0,
            passiveName = "Press the Attack • Feel No Pain 5+",
            passiveDesc = "Press the Attack: Melee attacks carry [Sustained Hits 1: non-Monster/Vehicle] (or [Sustained Hits 1] vs any target on Charge/Assault Doctrine). Feel No Pain 5+: Roll 1D6 per allocated wound; 5+ ignores wound. Leader (Max 1 Hero): Joins Intercessors, Assault Intercessors, Hellblasters, or Bladeguard Veterans.",
            mainWeaponName = "Master-crafted Bolter",
            mainWeaponAssault = true,
            mainWeaponHeavy = true,
            mainRapidFire = 1,
            mainSustainedHits = 0,
            meleeWeaponName = "Master-crafted Chainsword",
            meleeAttacks = 8,
            meleeWeaponSkill = 2,
            meleeStrength = 5,
            meleeArmorPen = 1,
            meleeDamage = 2,
            meleeSustainedHits = 0,
            meleeAntiNonMonsterVehicle = 2,
            pistolWeaponName = "Bolt Pistol",
            pistolAttacks = 1,
            pistolDamage = 1,
            pistolStrength = 5,
            pistolArmorPen = 1,
            pistolRange = 3,
            pistolCooldown = 1,
            leadableIds = listOf("intercessors", "assault_intercessors", "hellblasters", "bladeguard_veterans")
        ),
        UnitTemplate(
            id = "terminators",
            name = "Assault Terminator",
            faction = Faction.MARINES,
            archetype = UnitArchetype.TERMINATOR,
            role = "Elite Heavy",
            points = 180,
            squadSize = 5,
            woundsPerModel = 4,
            toughness = 5,
            save = 2,
            invulnSave = 4,
            armorPen = 2,
            moveCm = 12, // 5"
            strength = 8,
            damage = 3,
            attacksPerModel = 3,
            range = 1,
            ballisticSkill = 3,
            weaponSkill = 3,
            leadership = 6,
            objectiveControl = 1,
            skillName = "Teleport Strike",
            skillDesc = "Teleports anywhere on the battlefield (> 8 cm from enemies) via Teleport Beacon & Warp Strike.",
            skillCooldown = 4,
            passiveName = "Terminatus Assault",
            passiveDesc = "Rest: Charged this turn. Result: Thunder Hammer gains [SUSTAINED HITS 1 vs MONSTER/VEHICLE]; Lightning Claws gain [SUSTAINED HITS 1 vs NON-MONSTER/VEHICLE].",
            terminatorWeapon = TerminatorWeaponChoice.THUNDER_HAMMER_SHIELD,
            mainWeaponName = "Thunder Hammer & Storm Shield",
            mainWeaponAssault = false,
            pistolDamage = 0,
            terminatorHammerProfile = com.example.model.WeaponProfile(
                name = "Thunder Hammer & Storm Shield",
                range = 1,
                attacksPerModel = 3,
                skill = 3,
                strength = 8,
                armorPen = 2,
                damage = 3,
                sustainedHits = 0
            ),
            terminatorClawsProfile = com.example.model.WeaponProfile(
                name = "Twin Lightning Claws",
                range = 1,
                attacksPerModel = 6,
                skill = 3,
                strength = 5,
                armorPen = 2,
                damage = 1,
                sustainedHits = 0
            )
        ),
        UnitTemplate(
            id = "dreadnought",
            name = "Redemptor Dreadnought",
            faction = Faction.MARINES,
            archetype = UnitArchetype.DREADNOUGHT,
            role = "Armored Walker",
            points = 180, // 170 base + 10 Macro Plasma Incinerator
            squadSize = 1,
            woundsPerModel = 12,
            toughness = 10,
            save = 2,
            invulnSave = null,
            armorPen = 3,
            moveCm = 20, // 8"
            strength = 9,
            damage = 3,
            attacksPerModel = 4, // D6+1 avg
            range = 6, // 36"
            ballisticSkill = 2,
            weaponSkill = 3,
            leadership = 6,
            objectiveControl = 4,
            baseDiameterMm = 90,
            skillName = "Sweeping Barrage",
            skillDesc = "Fires Macro Plasma splash hitting target + nearby foes.",
            skillCooldown = 3,
            passiveName = "Duty Eternal",
            passiveDesc = "M 8\" (20 cm), 12W, T10, 2+ Save, OC 4; reduces incoming Damage D by 1 (min 1). Redemptor Fist S12 AP-3 D5.",
            mainWeaponName = "Macro Plasma Incinerator",
            mainWeaponAssault = false,
            meleeWeaponName = "Redemptor Fist",
            meleeAttacks = 5,
            meleeWeaponSkill = 3,
            meleeStrength = 12,
            meleeArmorPen = 3,
            meleeDamage = 5, // D3+3 avg
            pistolDamage = 0
        ),
        UnitTemplate(
            id = "brutalis_dreadnought",
            name = "Brutalis Dreadnought",
            faction = Faction.MARINES,
            archetype = UnitArchetype.BRUTALIS_DREADNOUGHT,
            role = "Brutalis Melee Walker",
            points = 160,
            squadSize = 1,
            woundsPerModel = 12,
            toughness = 10,
            save = 2,
            invulnSave = null,
            armorPen = 2,
            moveCm = 20, // 8"
            strength = 10,
            damage = 2,
            attacksPerModel = 8,
            range = 1, // Melee 2cm
            ballisticSkill = 3,
            weaponSkill = 3,
            leadership = 6,
            objectiveControl = 4,
            baseDiameterMm = 90,
            skillName = "Brutalis Charge (Auto)",
            skillDesc = "Auto Passive (No manual use): After completing a Charge, Brutalis Talons gain +2S (S12) & +1AP (AP-3) for melee this turn. If it destroys ≥1 enemy model on the turn it Charged, permanently gain +1 Melee Attack (max 4 stacks, 1 stack/turn).",
            skillCooldown = 1,
            passiveName = "Brutalis Charge (Auto)",
            passiveDesc = "90mm Base • Talons: Cleave 2, Sustained Hits 1: non-Monster/Vehicle, Twin-linked • After Charge: +2S +1AP Melee. Kill ≥1 model on Charge turn: +1A permanent (Max 4, 1/turn).",
            mainWeaponName = "Brutalis Talons",
            mainWeaponAssault = false,
            mainTwinLinked = true,
            mainCleave = 2,
            mainSustainedNonMonsterVehicle = 1,
            pistolWeaponName = "Twin multi-melta",
            pistolAttacks = 2,
            pistolDamage = 4, // D6 avg; +3 Melta at half range
            pistolStrength = 9,
            pistolArmorPen = 4,
            pistolRange = 4, // 18"
            pistolCooldown = 1,
            pistolIsPistol = false,
            pistolMelta = 3,
            pistolTwinLinked = true,
            auxRangedProfile = com.example.model.WeaponProfile(
                name = "Twin Icarus ironhail heavy stubber",
                range = 6, // 36"
                attacksPerModel = 3,
                skill = 3,
                strength = 4,
                armorPen = 0,
                damage = 1,
                twinLinked = true,
                rapidFire = 3,
                antiFly = 3
            )
        ),

        // --- ALLIANCE IMPERIUM ---
        UnitTemplate(
            id = "death_company_assault_intercessor",
            name = "Death Company Assault Intercessor",
            faction = Faction.ALLIANCE_IMPERIUM,
            archetype = UnitArchetype.DEATH_COMPANY_ASSAULT_INTERCESSOR,
            role = "Death Company Infantry",
            points = 125,
            squadSize = 5,
            woundsPerModel = 2,
            toughness = 4,
            save = 3,
            invulnSave = null,
            armorPen = 1,
            moveCm = 15, // 6"
            strength = 5,
            damage = 1,
            attacksPerModel = 4,
            range = 1, // Melee
            ballisticSkill = 3,
            weaponSkill = 3,
            leadership = 6,
            objectiveControl = 1,
            baseDiameterMm = 32,
            feelNoPainTarget = 6,
            skillName = "An Honourable Death in Combat (Auto)",
            skillDesc = "Always-active ability: Each attack gains [SUSTAINED HITS 1] while under Starting Strength, or [SUSTAINED HITS 2] instead (not both) while Below Half-strength.",
            skillCooldown = 0,
            passiveName = "An Honourable Death • Feel No Pain 6+",
            passiveDesc = "32mm Base • Chainsword (4A WS3+ S5 AP-1 D1) & Close-quarters Bolt Pistol (12\" 1A BS3+ S5 AP-1 D1) • An Honourable Death in Combat: [SUSTAINED HITS 1] under Starting Strength, [SUSTAINED HITS 2] Below Half-strength • Auto Passive: Feel No Pain 6+.",
            mainWeaponName = "Chainsword",
            mainWeaponAssault = false,
            pistolWeaponName = "Bolt Pistol",
            pistolAttacks = 1,
            pistolDamage = 1,
            pistolStrength = 5,
            pistolArmorPen = 1,
            pistolRange = 3, // 12"
            pistolCooldown = 1,
            pistolIsPistol = true
        ),

        // --- BLACK LEGION (Chaos Space Marines) ---
        UnitTemplate(
            id = "legionaries",
            name = "Legionaries",
            faction = Faction.BLACK_LEGION,
            archetype = UnitArchetype.LEGIONARY,
            role = "Chaos Battleline Infantry",
            points = 90,
            squadSize = 5,
            woundsPerModel = 2,
            toughness = 4,
            save = 3,
            invulnSave = null,
            armorPen = 1,
            moveCm = 15, // 6"
            strength = 5,
            damage = 1,
            attacksPerModel = 2,
            range = 5, // 24"
            ballisticSkill = 3,
            weaponSkill = 3,
            leadership = 6,
            objectiveControl = 2,
            skillName = "Veterans of the Long War",
            skillDesc = "Auto-activates when starting to shoot or fight: Re-roll unmodified Wound rolls of 1 for 1 turn (shooting, melee & counter-attacks). Cooldown: 1 Turn.",
            skillCooldown = 1,
            passiveName = "Dark Pacts",
            passiveDesc = "When selected to shoot or fight, may make a Dark Pact: take a 2D6 Leadership test (fail: D3 mortal wounds), then gain [LETHAL HITS] or [SUSTAINED HITS 1] for that phase.",
            mainWeaponName = "Boltgun",
            mainWeaponAssault = true,
            meleeWeaponName = "Astartes Chainsword",
            meleeAttacks = 4,
            meleeWeaponSkill = 3,
            meleeStrength = 5,
            meleeArmorPen = 1,
            meleeDamage = 1,
            pistolWeaponName = "Bolt Pistol",
            pistolAttacks = 1,
            pistolDamage = 1,
            pistolStrength = 4,
            pistolArmorPen = 0,
            pistolRange = 3,
            pistolCooldown = 1
        ),
        UnitTemplate(
            id = "chaos_terminator",
            name = "Chaos Terminators",
            faction = Faction.BLACK_LEGION,
            archetype = UnitArchetype.CHAOS_TERMINATOR,
            role = "Elite Chaos Heavy",
            points = 190,
            squadSize = 5,
            woundsPerModel = 4,
            toughness = 5,
            save = 2,
            invulnSave = 4,
            armorPen = 2,
            moveCm = 12, // 5"
            strength = 8,
            damage = 3,
            attacksPerModel = 4,
            range = 5, // 24"
            ballisticSkill = 3,
            weaponSkill = 3,
            leadership = 6,
            objectiveControl = 1,
            baseDiameterMm = 40,
            skillName = "Teleport Strike",
            skillDesc = "Teleports anywhere on the battlefield (> 8 cm from enemies) via Teleport Beacon & Warp Strike.",
            skillCooldown = 4,
            passiveName = "Mark of Chaos • Dark Pacts",
            passiveDesc = "40mm Base, T5, 2+, 4++ Invuln, 4W. Combi-bolter & Accursed Weapons with Dark Pacts & Teleport Strike.",
            mainWeaponName = "Combi-bolter",
            mainWeaponAssault = true,
            mainRapidFire = 1,
            meleeWeaponName = "Accursed Weapons",
            meleeAttacks = 4,
            meleeWeaponSkill = 3,
            meleeStrength = 8,
            meleeArmorPen = 2,
            meleeDamage = 2,
            pistolWeaponName = "Bolt Pistol",
            pistolAttacks = 1,
            pistolDamage = 1,
            pistolStrength = 4,
            pistolArmorPen = 0,
            pistolRange = 3,
            pistolCooldown = 1
        ),

        // --- TYRANIDS (Warhammer 40,000 11th Edition) ---
        UnitTemplate(
            id = "termagants",
            name = "Termagants",
            faction = Faction.XENOS,
            archetype = UnitArchetype.TERMAGANT,
            role = "Endless Multitude",
            points = 55,
            squadSize = 10,
            woundsPerModel = 1,
            toughness = 3,
            save = 5,
            invulnSave = null,
            armorPen = 0,
            moveCm = 15, // 6"
            strength = 5,
            damage = 1,
            attacksPerModel = 1,
            range = 4, // 18"
            ballisticSkill = 4,
            weaponSkill = 4,
            leadership = 8,
            objectiveControl = 2,
            skillName = "Acidic Salvo",
            skillDesc = "High-volume fleshborer barrage (+1 attack per model).",
            skillCooldown = 2,
            passiveName = "Skulking Horrors",
            passiveDesc = "10 models, OC 2, Assault Fleshborer (S5). Gains +2 to Save rolls while in Cover.",
            mainWeaponName = "Fleshborer",
            mainWeaponAssault = true,
            meleeWeaponName = "Chitinous Claws & Teeth",
            meleeAttacks = 1,
            meleeWeaponSkill = 4,
            meleeStrength = 3,
            meleeArmorPen = 0,
            meleeDamage = 1,
            pistolDamage = 0
        ),
        UnitTemplate(
            id = "hormagaunts",
            name = "Hormagaunts",
            faction = Faction.XENOS,
            archetype = UnitArchetype.HORMAGAUNT,
            role = "Fast Swarm",
            points = 70,
            squadSize = 10,
            woundsPerModel = 1,
            toughness = 3,
            save = 5,
            invulnSave = null,
            armorPen = 1,
            moveCm = 25, // 10"
            strength = 3,
            damage = 1,
            attacksPerModel = 3,
            range = 1,
            ballisticSkill = 4,
            weaponSkill = 4,
            leadership = 8,
            objectiveControl = 2,
            skillName = "Bounding Leap",
            skillDesc = "Surges +9 cm Move & empowers Hormagaunt talons (+1 Hit & +1 Dmg).",
            skillCooldown = 2,
            passiveName = "Hyper-Adaptation",
            passiveDesc = "10 models, M 10\" (25 cm), OC 2, 3x S3 AP-1 Hormagaunt Talons each.",
            mainWeaponName = "Hormagaunt Talons",
            mainWeaponAssault = false,
            pistolDamage = 0
        ),
        UnitTemplate(
            id = "warriors",
            name = "Tyranid Warriors",
            faction = Faction.XENOS,
            archetype = UnitArchetype.WARRIOR,
            role = "Synapse Vanguard",
            points = 75,
            squadSize = 3,
            woundsPerModel = 3,
            toughness = 5,
            save = 4,
            invulnSave = null,
            armorPen = 2,
            moveCm = 15, // 6"
            strength = 5,
            damage = 1,
            attacksPerModel = 6,
            range = 1,
            ballisticSkill = 4,
            weaponSkill = 3,
            leadership = 7,
            objectiveControl = 2,
            skillName = "Adrenal Frenzy",
            skillDesc = "Synapse surge: +1 to Hit & +1 Dmg for 2 turns + immediate strike.",
            skillCooldown = 2,
            passiveName = "Synapse Conduit",
            passiveDesc = "Swarm units within 3 tiles gain +1 to Hit (BS/WS). Twin-linked Claws (6A S5 AP-2 D1) or Deathspitter.",
            weaponChoice = WarriorWeaponChoice.BONESWORDS,
            mainWeaponName = "Tyranid Warrior Claws & Talons",
            mainWeaponAssault = false,
            meleeWeaponName = "Tyranid Warrior Claws & Talons",
            meleeAttacks = 6,
            meleeWeaponSkill = 3,
            meleeStrength = 5,
            meleeArmorPen = 2,
            meleeDamage = 1,
            pistolDamage = 0,
            warriorMeleeProfile = com.example.model.WeaponProfile(
                name = "Tyranid Warrior Claws & Talons",
                range = 1,
                attacksPerModel = 6,
                skill = 3,
                strength = 5,
                armorPen = 2,
                damage = 1,
                sustainedHits = 0
            ),
            warriorRangedProfile = com.example.model.WeaponProfile(
                name = "Deathspitter",
                range = 5, // 24"
                attacksPerModel = 3,
                skill = 4,
                strength = 5,
                armorPen = 1,
                damage = 1,
                sustainedHits = 0,
                assault = false
            )
        ),
        UnitTemplate(
            id = "carnifex",
            name = "Carnifex",
            faction = Faction.XENOS,
            archetype = UnitArchetype.CARNIFEX,
            role = "Living Battering Ram",
            points = 90,
            squadSize = 1,
            woundsPerModel = 8,
            toughness = 9,
            save = 2,
            invulnSave = null,
            armorPen = 3,
            moveCm = 20, // 8"
            strength = 12,
            damage = 4, // D6+1 avg
            attacksPerModel = 4,
            range = 1,
            ballisticSkill = 4,
            weaponSkill = 4,
            leadership = 8,
            objectiveControl = 3,
            skillName = "Bio-Plasma Blast",
            skillDesc = "Spews S7 AP-2 D1 bio-plasma up to 12\" (Range 3) in an AoE splash.",
            skillCooldown = 3,
            passiveName = "Living Ram",
            passiveDesc = "M 8\" (20 cm), 8W, T9, 2+ Save, OC 3; Crushing Claws (4A WS4+ S12 AP-3 D4) & 5A Spine Banks.",
            mainWeaponName = "Carnifex Crushing Claws",
            mainWeaponAssault = false,
            pistolWeaponName = "Spine Banks",
            pistolAttacks = 5,
            pistolDamage = 1,
            pistolStrength = 5,
            pistolArmorPen = 0,
            pistolRange = 2, // 6"
            pistolCooldown = 1
        ),
        UnitTemplate(
            id = "genestealers",
            name = "Genestealers",
            faction = Faction.XENOS,
            archetype = UnitArchetype.GENESTEALER,
            role = "Vanguard Infestation",
            points = 75,
            squadSize = 5,
            woundsPerModel = 2,
            toughness = 4,
            save = 5,
            invulnSave = 5,
            armorPen = 2,
            moveCm = 20, // 8"
            strength = 4,
            damage = 1,
            attacksPerModel = 4,
            range = 1,
            ballisticSkill = 4,
            weaponSkill = 2,
            leadership = 7,
            objectiveControl = 1,
            skillName = "Vanguard Predator",
            skillDesc = "Passive: Re-rolls unmodified Hit rolls of 1 and Wound rolls of 1 in combat. Skill: Auto-activates on attack (1 Turn Cooldown).",
            skillCooldown = 1,
            passiveName = "Vanguard Predator • 5++ Invuln",
            passiveDesc = "M 8\" (20 cm), WS 2+, 4A S4 AP-2 D1 Claws & Talons with Hit and Wound re-rolls of 1.",
            mainWeaponName = "Genestealer Claws & Talons",
            mainWeaponAssault = false,
            pistolDamage = 0
        ),
        UnitTemplate(
            id = "von_ryans_leaper",
            name = "Von Ryan's Leapers",
            faction = Faction.XENOS,
            archetype = UnitArchetype.VON_RYANS_LEAPER,
            role = "Ambush Stalker",
            points = 55,
            squadSize = 3,
            woundsPerModel = 3,
            toughness = 5,
            save = 4,
            invulnSave = 6,
            armorPen = 1,
            moveCm = 25, // 10"
            strength = 5,
            damage = 1,
            attacksPerModel = 6,
            range = 1,
            ballisticSkill = 4,
            weaponSkill = 3,
            leadership = 8,
            objectiveControl = 1,
            skillName = "Chameleonic Cloak",
            skillDesc = "Cloaks the unit in stealth (invisible/untargetable for 1 turn).",
            skillCooldown = 3,
            passiveName = "Fights First • Stealth • 6++",
            passiveDesc = "M 10\" (25 cm), 6A S5 AP-1 D1 Leaper's Talons, 6++ Invuln, Fights First, and -1 to Hit on incoming ranged attacks.",
            mainWeaponName = "Leaper's Talons",
            mainWeaponAssault = false,
            pistolDamage = 0
        ),
        UnitTemplate(
            id = "biovores",
            name = "Biovores",
            faction = Faction.XENOS,
            archetype = UnitArchetype.BIOVORE,
            role = "Living Artillery",
            points = 50,
            squadSize = 1,
            woundsPerModel = 5,
            toughness = 6,
            save = 4,
            invulnSave = null,
            armorPen = 1,
            moveCm = 12, // 5"
            strength = 6,
            damage = 2,
            attacksPerModel = 1,
            range = 8, // 24"
            ballisticSkill = 4,
            weaponSkill = 5,
            leadership = 8,
            objectiveControl = 1,
            skillName = "Seed Spore Mine",
            skillDesc = "Launches a living Spore Mine onto a selected tile at least 8 cm away from all enemy models. Cooldown: 1 Turn.",
            skillCooldown = 1,
            passiveName = "Heavy Artillery",
            passiveDesc = "50 pts, 5W, T6, Sv 4+, Move 12 cm. Fires Spore Mine Launcher (24\" Rng 8, 1A BS4+ S6 AP-1 D2) or Seeds a living Spore Mine (≥ 8 cm from enemy).",
            mainWeaponName = "Spore Mine Launcher",
            mainWeaponAssault = false,
            meleeWeaponName = "Chitinous Limbs",
            meleeAttacks = 2,
            meleeWeaponSkill = 5,
            meleeStrength = 5,
            meleeArmorPen = 0,
            meleeDamage = 1,
            pistolDamage = 0
        ),
        UnitTemplate(
            id = "spore_mine",
            name = "Spore Mine",
            faction = Faction.XENOS,
            archetype = UnitArchetype.SPORE_MINE,
            role = "Living Biological Munition",
            points = 0,
            squadSize = 1,
            woundsPerModel = 1,
            toughness = 1,
            save = 6,
            invulnSave = null,
            armorPen = 0,
            moveCm = 4, // 4 cm move
            strength = 1,
            damage = 1,
            attacksPerModel = 0,
            range = 1,
            ballisticSkill = 6,
            weaponSkill = 6,
            leadership = 8,
            objectiveControl = 0,
            isFly = true,
            skillName = "Self-Destruct",
            skillDesc = "Detonates: Spore Mine is destroyed. Rolls 1 D6 for every model in the enemy unit; each roll < 4 (1-3) inflicts 1 Mortal Wound.",
            skillCooldown = 1,
            passiveName = "Volatile Bio-Gasbag",
            passiveDesc = "Controllable living mine. M 4 cm, 1W. Detonates in melee range, rolling 1 D6 per enemy model; rolls < 4 inflict 1 Mortal Wound.",
            mainWeaponName = "Bio-Self Destruct",
            mainWeaponAssault = false,
            meleeWeaponName = "Bio-Self Destruct",
            meleeAttacks = 0,
            meleeWeaponSkill = 6,
            meleeStrength = 1,
            meleeArmorPen = 0,
            meleeDamage = 1,
            pistolDamage = 0
        )
    )

    fun defaultMarineRoster(): List<RosterSlot> = listOf(
        RosterSlot("m1", "intercessors"),
        RosterSlot("m2", "hellblasters"),
        RosterSlot("m3", "assault_intercessors"),
        RosterSlot("m4", "terminators"),
        RosterSlot("m5", "dreadnought"),
        RosterSlot("m6", "bladeguard_veterans"),
        RosterSlot("m7", "brutalis_dreadnought"),
        RosterSlot("m8", "captain_titus")
    )

    fun defaultXenosRoster(): List<RosterSlot> = listOf(
        RosterSlot("x1", "termagants"),
        RosterSlot("x2", "hormagaunts"),
        RosterSlot("x3", "warriors"),
        RosterSlot("x4", "carnifex"),
        RosterSlot("x5", "genestealers"),
        RosterSlot("x6", "biovores")
    )

    fun defaultBlackLegionRoster(): List<RosterSlot> = listOf(
        RosterSlot("bl1", "legionaries"),
        RosterSlot("bl2", "chaos_terminator")
    )

    fun defaultAllianceImperiumRoster(): List<RosterSlot> = listOf(
        RosterSlot("ai1", "death_company_assault_intercessor")
    )

    private fun profileToJson(p: com.example.model.WeaponProfile): JSONObject = JSONObject().apply {
        put("name", p.name)
        put("range", p.range)
        put("attacksPerModel", p.attacksPerModel)
        put("skill", p.skill)
        put("strength", p.strength)
        put("armorPen", p.armorPen)
        put("damage", p.damage)
        put("sustainedHits", p.sustainedHits)
        put("assault", p.assault)
        put("pistol", p.pistol)
        put("melta", p.melta)
        put("twinLinked", p.twinLinked)
        put("rapidFire", p.rapidFire)
        put("antiFly", p.antiFly)
        put("cleave", p.cleave)
        put("sustainedNonMonsterVehicle", p.sustainedNonMonsterVehicle)
    }

    private fun jsonToProfile(obj: JSONObject?, fallback: com.example.model.WeaponProfile?): com.example.model.WeaponProfile? {
        if (obj == null) return fallback
        val fb = fallback ?: return null
        return com.example.model.WeaponProfile(
            name = obj.optString("name", fb.name).ifBlank { fb.name },
            range = obj.optInt("range", fb.range).coerceIn(1, 12),
            attacksPerModel = obj.optInt("attacksPerModel", fb.attacksPerModel).coerceIn(1, 10),
            skill = obj.optInt("skill", fb.skill).coerceIn(2, 6),
            strength = obj.optInt("strength", fb.strength).coerceIn(1, 20),
            armorPen = obj.optInt("armorPen", fb.armorPen).coerceIn(0, 5),
            damage = obj.optInt("damage", fb.damage).coerceIn(1, 500),
            sustainedHits = obj.optInt("sustainedHits", fb.sustainedHits).coerceIn(0, 6),
            assault = obj.optBoolean("assault", fb.assault),
            pistol = obj.optBoolean("pistol", fb.pistol),
            melta = obj.optInt("melta", fb.melta).coerceIn(0, 10),
            twinLinked = obj.optBoolean("twinLinked", fb.twinLinked),
            rapidFire = obj.optInt("rapidFire", fb.rapidFire).coerceIn(0, 10),
            antiFly = obj.optInt("antiFly", fb.antiFly).coerceIn(0, 6),
            cleave = obj.optInt("cleave", fb.cleave).coerceIn(0, 10),
            sustainedNonMonsterVehicle = obj.optInt("sustainedNonMonsterVehicle", fb.sustainedNonMonsterVehicle).coerceIn(0, 6)
        )
    }

    fun serializeTemplates(templates: Map<String, UnitTemplate>): String {
        val arr = JSONArray()
        templates.values.forEach { t ->
            arr.put(JSONObject().apply {
                put("catalogEdition", CATALOG_EDITION)
                put("id", t.id)
                put("points", t.points)
                put("squadSize", t.squadSize)
                put("woundsPerModel", t.woundsPerModel)
                put("toughness", t.toughness)
                put("save", t.save)
                put("invulnSave", t.invulnSave ?: 0)
                put("armorPen", t.armorPen)
                put("moveCm", t.moveCm)
                put("strength", t.strength)
                put("damage", t.damage)
                put("attacksPerModel", t.attacksPerModel)
                put("range", t.range)
                put("ballisticSkill", t.ballisticSkill)
                put("weaponSkill", t.weaponSkill)
                put("leadership", t.leadership)
                put("objectiveControl", t.objectiveControl)
                put("baseDiameterMm", t.baseDiameterMm)
                put("isFly", t.isFly)
                put("isHero", t.isHero)
                put("feelNoPainTarget", t.feelNoPainTarget ?: 0)
                put("skillCooldown", t.skillCooldown)
                put("weaponChoice", t.weaponChoice?.name ?: "")
                put("terminatorWeapon", t.terminatorWeapon?.name ?: "")
                put("mainWeaponName", t.mainWeaponName)
                put("mainWeaponAssault", t.mainWeaponAssault)
                put("mainWeaponHeavy", t.mainWeaponHeavy)
                put("mainWeaponPistol", t.mainWeaponPistol)
                put("mainSustainedHits", t.mainSustainedHits)
                put("mainMelta", t.mainMelta)
                put("mainTwinLinked", t.mainTwinLinked)
                put("mainRapidFire", t.mainRapidFire)
                put("mainAntiFly", t.mainAntiFly)
                put("mainCleave", t.mainCleave)
                put("mainSustainedNonMonsterVehicle", t.mainSustainedNonMonsterVehicle)
                put("meleeWeaponName", t.meleeWeaponName)
                put("meleeAttacks", t.meleeAttacks)
                put("meleeWeaponSkill", t.meleeWeaponSkill)
                put("meleeStrength", t.meleeStrength)
                put("meleeArmorPen", t.meleeArmorPen)
                put("meleeDamage", t.meleeDamage)
                put("meleeSustainedHits", t.meleeSustainedHits)
                put("meleeTwinLinked", t.meleeTwinLinked)
                put("meleeAntiNonMonsterVehicle", t.meleeAntiNonMonsterVehicle)
                put("leadableIds", JSONArray(t.leadableIds))
                put("pistolAttacks", t.pistolAttacks)
                put("pistolDamage", t.pistolDamage)
                put("pistolStrength", t.pistolStrength)
                put("pistolArmorPen", t.pistolArmorPen)
                put("pistolRange", t.pistolRange)
                put("pistolCooldown", t.pistolCooldown)
                put("pistolSustainedHits", t.pistolSustainedHits)
                put("pistolIsPistol", t.pistolIsPistol)
                put("pistolMelta", t.pistolMelta)
                put("pistolTwinLinked", t.pistolTwinLinked)
                put("pistolRapidFire", t.pistolRapidFire)
                put("pistolAntiFly", t.pistolAntiFly)
                t.auxRangedProfile?.let { put("auxRangedProfile", profileToJson(it)) }
                t.warriorMeleeProfile?.let { put("warriorMeleeProfile", profileToJson(it)) }
                t.warriorRangedProfile?.let { put("warriorRangedProfile", profileToJson(it)) }
                t.terminatorHammerProfile?.let { put("terminatorHammerProfile", profileToJson(it)) }
                t.terminatorClawsProfile?.let { put("terminatorClawsProfile", profileToJson(it)) }
            })
        }
        return arr.toString()
    }

    fun deserializeTemplates(json: String): Map<String, UnitTemplate> {
        val base = defaultTemplates.associateBy { it.id }.toMutableMap()
        if (json.isBlank()) return base
        return try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.optString("id")
                val existing = base[id] ?: continue
                val edition = obj.optInt("catalogEdition", 0)
                if (edition < CATALOG_EDITION) {
                    // Upgrade pre-11th-Edition saved configs cleanly to 11th Edition defaults
                    // while preserving valid weapon choice selections.
                    val isLegacyTerminator = id == "terminators" &&
                        !obj.has("terminatorWeapon") &&
                        (obj.optString("mainWeaponName") == "Lightning Claws & Thunder Hammer" || obj.optInt("range", 1) == 4)
                    val twStr = obj.optString("terminatorWeapon")
                    val tw = if (isLegacyTerminator) TerminatorWeaponChoice.THUNDER_HAMMER_SHIELD
                    else if (twStr.isNotBlank()) runCatching { TerminatorWeaponChoice.valueOf(twStr) }.getOrNull() ?: existing.terminatorWeapon
                    else existing.terminatorWeapon
                    val wcStr = obj.optString("weaponChoice")
                    val wc = if (wcStr.isNotBlank()) runCatching { WarriorWeaponChoice.valueOf(wcStr) }.getOrNull() ?: existing.weaponChoice else existing.weaponChoice
                    base[id] = if (id == "terminators" && tw == TerminatorWeaponChoice.LIGHTNING_CLAWS && existing.terminatorClawsProfile != null) {
                        val claws = existing.terminatorClawsProfile
                        existing.copy(
                            terminatorWeapon = tw,
                            woundsPerModel = 3,
                            attacksPerModel = claws.attacksPerModel,
                            weaponSkill = claws.skill,
                            strength = claws.strength,
                            armorPen = claws.armorPen,
                            damage = claws.damage,
                            mainWeaponName = claws.name,
                            mainSustainedHits = claws.sustainedHits
                        )
                    } else if (id == "warriors" && wc == WarriorWeaponChoice.FLESHBORER && existing.warriorRangedProfile != null) {
                        val rngProf = existing.warriorRangedProfile
                        existing.copy(
                            weaponChoice = wc,
                            range = rngProf.range,
                            attacksPerModel = rngProf.attacksPerModel,
                            ballisticSkill = rngProf.skill,
                            strength = rngProf.strength,
                            armorPen = rngProf.armorPen,
                            damage = rngProf.damage,
                            mainWeaponName = rngProf.name,
                            mainWeaponAssault = rngProf.assault,
                            mainWeaponPistol = rngProf.pistol,
                            mainSustainedHits = rngProf.sustainedHits
                        )
                    } else {
                        existing.copy(weaponChoice = wc, terminatorWeapon = tw)
                    }
                    continue
                }
                val wcStr = obj.optString("weaponChoice")
                val wc = if (wcStr.isNotBlank()) runCatching { WarriorWeaponChoice.valueOf(wcStr) }.getOrNull() else existing.weaponChoice
                val twStr = obj.optString("terminatorWeapon")
                val tw = if (twStr.isNotBlank()) runCatching { TerminatorWeaponChoice.valueOf(twStr) }.getOrNull() else existing.terminatorWeapon
                val isLegacyTerminator = id == "terminators" &&
                    !obj.has("terminatorWeapon") &&
                    (obj.optString("mainWeaponName") == "Lightning Claws & Thunder Hammer" || obj.optInt("range", 1) == 4)
                val rawInv = if (obj.has("invulnSave")) obj.optInt("invulnSave", 0) else (existing.invulnSave ?: 0)
                val parsedInv = if (rawInv in 2..6) rawInv else null
                base[id] = existing.copy(
                    points = obj.optInt("points", existing.points),
                    squadSize = obj.optInt("squadSize", existing.squadSize).coerceIn(1, 10),
                    woundsPerModel = if (isLegacyTerminator) existing.woundsPerModel else obj.optInt("woundsPerModel", existing.woundsPerModel).coerceIn(1, 30),
                    toughness = obj.optInt("toughness", existing.toughness).coerceIn(1, 14),
                    save = obj.optInt("save", existing.save).coerceIn(2, 6),
                    invulnSave = parsedInv,
                    armorPen = if (isLegacyTerminator) existing.armorPen else obj.optInt("armorPen", existing.armorPen).coerceIn(0, 5),
                    moveCm = obj.optInt("moveCm", existing.moveCm).coerceIn(3, 45),
                    strength = if (isLegacyTerminator) existing.strength else obj.optInt("strength", existing.strength).coerceIn(1, 20),
                    damage = if (isLegacyTerminator) existing.damage else obj.optInt("damage", existing.damage).coerceIn(1, 500),
                    attacksPerModel = if (isLegacyTerminator) existing.attacksPerModel else obj.optInt("attacksPerModel", existing.attacksPerModel).coerceIn(1, 10),
                    range = if (isLegacyTerminator) existing.range else obj.optInt("range", existing.range).coerceIn(1, 12),
                    ballisticSkill = obj.optInt("ballisticSkill", existing.ballisticSkill).coerceIn(2, 6),
                    weaponSkill = obj.optInt("weaponSkill", existing.weaponSkill).coerceIn(2, 6),
                    leadership = obj.optInt("leadership", existing.leadership).coerceIn(4, 10),
                    objectiveControl = obj.optInt("objectiveControl", existing.objectiveControl).coerceIn(0, 8),
                    baseDiameterMm = obj.optInt("baseDiameterMm", existing.baseDiameterMm).coerceIn(25, 130),
                    isFly = obj.optBoolean("isFly", existing.isFly),
                    isHero = obj.optBoolean("isHero", existing.isHero),
                    feelNoPainTarget = if (obj.has("feelNoPainTarget")) {
                        obj.optInt("feelNoPainTarget", 0).takeIf { it in 2..6 }
                    } else existing.feelNoPainTarget,
                    skillCooldown = if (id == "terminators" || id == "captain_titus" || id == "death_company_assault_intercessor") existing.skillCooldown else obj.optInt("skillCooldown", existing.skillCooldown).coerceIn(0, 8),
                    weaponChoice = wc,
                    terminatorWeapon = tw,
                    mainWeaponName = if (isLegacyTerminator) existing.mainWeaponName else obj.optString("mainWeaponName", existing.mainWeaponName).ifBlank { existing.mainWeaponName },
                    mainWeaponAssault = if (isLegacyTerminator) existing.mainWeaponAssault else if (obj.has("mainWeaponAssault")) obj.optBoolean("mainWeaponAssault", existing.mainWeaponAssault) else existing.mainWeaponAssault,
                    mainWeaponHeavy = obj.optBoolean("mainWeaponHeavy", existing.mainWeaponHeavy),
                    mainWeaponPistol = if (isLegacyTerminator) existing.mainWeaponPistol else if (obj.has("mainWeaponPistol")) obj.optBoolean("mainWeaponPistol", existing.mainWeaponPistol) else existing.mainWeaponPistol,
                    mainSustainedHits = obj.optInt("mainSustainedHits", existing.mainSustainedHits).coerceIn(0, 6),
                    mainMelta = obj.optInt("mainMelta", existing.mainMelta).coerceIn(0, 10),
                    mainTwinLinked = obj.optBoolean("mainTwinLinked", existing.mainTwinLinked),
                    mainRapidFire = obj.optInt("mainRapidFire", existing.mainRapidFire).coerceIn(0, 10),
                    mainAntiFly = obj.optInt("mainAntiFly", existing.mainAntiFly).coerceIn(0, 6),
                    mainCleave = obj.optInt("mainCleave", existing.mainCleave).coerceIn(0, 10),
                    mainSustainedNonMonsterVehicle = obj.optInt("mainSustainedNonMonsterVehicle", existing.mainSustainedNonMonsterVehicle).coerceIn(0, 6),
                    meleeWeaponName = obj.optString("meleeWeaponName", existing.meleeWeaponName).ifBlank { existing.meleeWeaponName },
                    meleeAttacks = obj.optInt("meleeAttacks", existing.meleeAttacks).coerceIn(0, 10),
                    meleeWeaponSkill = obj.optInt("meleeWeaponSkill", existing.meleeWeaponSkill).coerceIn(0, 6),
                    meleeStrength = obj.optInt("meleeStrength", existing.meleeStrength).coerceIn(1, 20),
                    meleeArmorPen = obj.optInt("meleeArmorPen", existing.meleeArmorPen).coerceIn(0, 5),
                    meleeDamage = obj.optInt("meleeDamage", existing.meleeDamage).coerceIn(1, 500),
                    meleeSustainedHits = obj.optInt("meleeSustainedHits", existing.meleeSustainedHits).coerceIn(0, 6),
                    meleeTwinLinked = obj.optBoolean("meleeTwinLinked", existing.meleeTwinLinked),
                    meleeAntiNonMonsterVehicle = obj.optInt("meleeAntiNonMonsterVehicle", existing.meleeAntiNonMonsterVehicle).coerceIn(0, 6),
                    leadableIds = obj.optJSONArray("leadableIds")?.let { lArr ->
                        (0 until lArr.length()).mapNotNull { idx -> lArr.optString(idx).takeIf { it.isNotBlank() } }
                    } ?: existing.leadableIds,
                    pistolAttacks = obj.optInt("pistolAttacks", existing.pistolAttacks).coerceIn(1, 10),
                    pistolDamage = obj.optInt("pistolDamage", existing.pistolDamage).coerceIn(0, 200),
                    pistolStrength = obj.optInt("pistolStrength", existing.pistolStrength).coerceIn(1, 20),
                    pistolArmorPen = obj.optInt("pistolArmorPen", existing.pistolArmorPen).coerceIn(0, 5),
                    pistolRange = obj.optInt("pistolRange", existing.pistolRange).coerceIn(1, 8),
                    pistolCooldown = obj.optInt("pistolCooldown", existing.pistolCooldown).coerceIn(1, 6),
                    pistolSustainedHits = obj.optInt("pistolSustainedHits", existing.pistolSustainedHits).coerceIn(0, 6),
                    pistolIsPistol = obj.optBoolean("pistolIsPistol", existing.pistolIsPistol),
                    pistolMelta = obj.optInt("pistolMelta", existing.pistolMelta).coerceIn(0, 10),
                    pistolTwinLinked = obj.optBoolean("pistolTwinLinked", existing.pistolTwinLinked),
                    pistolRapidFire = obj.optInt("pistolRapidFire", existing.pistolRapidFire).coerceIn(0, 10),
                    pistolAntiFly = obj.optInt("pistolAntiFly", existing.pistolAntiFly).coerceIn(0, 6),
                    auxRangedProfile = jsonToProfile(obj.optJSONObject("auxRangedProfile"), existing.auxRangedProfile),
                    warriorMeleeProfile = jsonToProfile(obj.optJSONObject("warriorMeleeProfile"), existing.warriorMeleeProfile),
                    warriorRangedProfile = jsonToProfile(obj.optJSONObject("warriorRangedProfile"), existing.warriorRangedProfile),
                    terminatorHammerProfile = jsonToProfile(obj.optJSONObject("terminatorHammerProfile"), existing.terminatorHammerProfile),
                    terminatorClawsProfile = jsonToProfile(obj.optJSONObject("terminatorClawsProfile"), existing.terminatorClawsProfile)
                )
            }
            base
        } catch (_: Exception) {
            base
        }
    }

    fun serializeRoster(roster: List<RosterSlot>): String {
        val arr = JSONArray()
        roster.forEach { slot ->
            arr.put(JSONObject().apply {
                put("slotId", slot.slotId)
                put("templateId", slot.templateId)
            })
        }
        return arr.toString()
    }

    fun deserializeRoster(json: String, fallback: List<RosterSlot>): List<RosterSlot> {
        if (json.isBlank()) return fallback
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<RosterSlot>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    RosterSlot(
                        slotId = obj.getString("slotId"),
                        templateId = obj.getString("templateId")
                    )
                )
            }
            list
        } catch (_: Exception) {
            fallback
        }
    }
}
