package com.example.engine

import com.example.model.Faction
import com.example.model.RosterSlot
import com.example.model.UnitArchetype
import com.example.model.UnitTemplate
import com.example.model.WarriorWeaponChoice
import org.json.JSONArray
import org.json.JSONObject

object DefaultCatalog {

    val defaultTemplates: List<UnitTemplate> = listOf(
        // --- ULTRAMARINES ---
        UnitTemplate(
            id = "intercessors",
            name = "Intercessor Squad",
            faction = Faction.MARINES,
            archetype = UnitArchetype.INTERCESSOR,
            role = "Line Infantry",
            points = 180,
            squadSize = 5,
            woundsPerModel = 2,
            toughness = 4,
            save = 3,
            invulnSave = null,
            armorPen = 1,
            moveCm = 15,
            strength = 4,
            damage = 1,
            attacksPerModel = 2,
            range = 5,
            ballisticSkill = 3,
            weaponSkill = 3,
            skillName = "Bolt Volley",
            skillDesc = "Fires rapid disciplined salvos (+1 attack per model, +1 to Hit roll).",
            skillCooldown = 2,
            passiveName = "Tactical Discipline",
            passiveDesc = "+1 to Hit (BS) when stationary (full Move cm).",
            mainWeaponName = "Bolt Rifle",
            mainWeaponAssault = true,
            pistolWeaponName = "Bolt Pistol",
            pistolAttacks = 1,
            pistolDamage = 1,
            pistolStrength = 4,
            pistolArmorPen = 0,
            pistolRange = 3,
            pistolCooldown = 1
        ),
        UnitTemplate(
            id = "hellblasters",
            name = "Hellblaster Squad",
            faction = Faction.MARINES,
            archetype = UnitArchetype.HELLBLASTER,
            role = "Heavy Plasma",
            points = 320,
            squadSize = 5,
            woundsPerModel = 2,
            toughness = 4,
            save = 3,
            invulnSave = null,
            armorPen = 2,
            moveCm = 15,
            strength = 7,
            damage = 1,
            attacksPerModel = 2,
            range = 5,
            ballisticSkill = 3,
            weaponSkill = 4,
            skillName = "Plasma Overcharge",
            skillDesc = "Supercharged plasma beam (+1 S, +1 AP -> S8 AP-3, D2 Wounds).",
            skillCooldown = 2,
            passiveName = "Armor Melter",
            passiveDesc = "High Strength & AP plasma pierces heavy armor saves.",
            mainWeaponName = "Plasma Incinerator",
            mainWeaponAssault = false,
            pistolWeaponName = "Plasma Pistol",
            pistolAttacks = 1,
            pistolDamage = 1,
            pistolStrength = 7,
            pistolArmorPen = 2,
            pistolRange = 3,
            pistolCooldown = 1
        ),
        UnitTemplate(
            id = "assault_intercessors",
            name = "Assault Intercessors",
            faction = Faction.MARINES,
            archetype = UnitArchetype.ASSAULT_MARINE,
            role = "Shock Melee",
            points = 200,
            squadSize = 5,
            woundsPerModel = 2,
            toughness = 4,
            save = 3,
            invulnSave = null,
            armorPen = 1,
            moveCm = 18,
            strength = 4,
            damage = 1,
            attacksPerModel = 3,
            range = 1,
            ballisticSkill = 3,
            weaponSkill = 3,
            skillName = "Chainsword Whirlwind",
            skillDesc = "Cleaves all adjacent enemy squads in melee range.",
            skillCooldown = 2,
            passiveName = "Shock Assault",
            passiveDesc = "Fires Heavy Bolt Pistol then charges with 3x S4 AP-1 Chainsword strikes.",
            mainWeaponName = "Astartes Chainsword",
            mainWeaponAssault = false,
            pistolWeaponName = "Heavy Bolt Pistol",
            pistolAttacks = 1,
            pistolDamage = 1,
            pistolStrength = 4,
            pistolArmorPen = 1,
            pistolRange = 3,
            pistolCooldown = 1
        ),
        UnitTemplate(
            id = "terminators",
            name = "Assault Terminator",
            faction = Faction.MARINES,
            archetype = UnitArchetype.TERMINATOR,
            role = "Elite Heavy",
            points = 440,
            squadSize = 5,
            woundsPerModel = 4,
            toughness = 5,
            save = 2,
            invulnSave = 4,
            armorPen = 2,
            moveCm = 12,
            strength = 6,
            damage = 2,
            attacksPerModel = 2,
            range = 4,
            ballisticSkill = 3,
            weaponSkill = 3,
            skillName = "Teleport Strike",
            skillDesc = "Teleports up to 9 tiles away (must be > 4 tiles from enemies). Grants 50% damage reduction for 1 turn.",
            skillCooldown = 4,
            passiveName = "Crux Terminatus",
            passiveDesc = "4W each, T5, 2+ Armor Save, 4++ Storm Shield Invulnerable Save & Lightning Claws / Thunder Hammer.",
            mainWeaponName = "Lightning Claws & Thunder Hammer",
            mainWeaponAssault = false,
            pistolDamage = 0
        ),
        UnitTemplate(
            id = "dreadnought",
            name = "Redemptor Dreadnought",
            faction = Faction.MARINES,
            archetype = UnitArchetype.DREADNOUGHT,
            role = "Armored Walker",
            points = 440,
            squadSize = 1,
            woundsPerModel = 12,
            toughness = 10,
            save = 2,
            invulnSave = null,
            armorPen = 3,
            moveCm = 15,
            strength = 9,
            damage = 3,
            attacksPerModel = 3,
            range = 6,
            ballisticSkill = 3,
            weaponSkill = 3,
            skillName = "Sweeping Barrage",
            skillDesc = "Fires gatling & plasma splash hitting target + nearby foes.",
            skillCooldown = 3,
            passiveName = "Duty Eternal",
            passiveDesc = "12W, T10, 2+ Save and reduces incoming Damage D by 1 (min 1).",
            mainWeaponName = "Macro Plasma & Combat Fist",
            mainWeaponAssault = false,
            pistolDamage = 0
        ),

        // --- ALIEN SWARM ---
        UnitTemplate(
            id = "termagants",
            name = "Termagant Brood",
            faction = Faction.XENOS,
            archetype = UnitArchetype.TERMAGANT,
            role = "Skittering Swarm",
            points = 150,
            squadSize = 5,
            woundsPerModel = 1,
            toughness = 3,
            save = 5,
            invulnSave = null,
            armorPen = 0,
            moveCm = 18,
            strength = 5,
            damage = 1,
            attacksPerModel = 2,
            range = 4,
            ballisticSkill = 4,
            weaponSkill = 4,
            skillName = "Acidic Salvo",
            skillDesc = "High-volume fleshborer barrage (+1 attack per model).",
            skillCooldown = 2,
            passiveName = "Skulking Horrors",
            passiveDesc = "Gains +2 to Save rolls while in Cover (craters & barricades).",
            mainWeaponName = "Fleshborer",
            mainWeaponAssault = true,
            pistolDamage = 0
        ),
        UnitTemplate(
            id = "hormagaunts",
            name = "Hormagaunt Pack",
            faction = Faction.XENOS,
            archetype = UnitArchetype.HORMAGAUNT,
            role = "Fast Melee",
            points = 160,
            squadSize = 5,
            woundsPerModel = 1,
            toughness = 3,
            save = 5,
            invulnSave = null,
            armorPen = 1,
            moveCm = 21,
            strength = 3,
            damage = 1,
            attacksPerModel = 3,
            range = 1,
            ballisticSkill = 4,
            weaponSkill = 4,
            skillName = "Bounding Leap",
            skillDesc = "Surges +9 cm Move & empowers scything talons (+1 Hit & +1 Dmg).",
            skillCooldown = 2,
            passiveName = "Hyper-Adaptation",
            passiveDesc = "Highest base movement speed (21 cm) in the Crimson Sector.",
            mainWeaponName = "Scything Talons",
            mainWeaponAssault = false,
            pistolDamage = 0
        ),
        UnitTemplate(
            id = "warriors",
            name = "Tyranid Warriors",
            faction = Faction.XENOS,
            archetype = UnitArchetype.WARRIOR,
            role = "Synapse Vanguard",
            points = 260,
            squadSize = 3,
            woundsPerModel = 3,
            toughness = 5,
            save = 4,
            invulnSave = null,
            armorPen = 2,
            moveCm = 15,
            strength = 5,
            damage = 2,
            attacksPerModel = 3,
            range = 1,
            ballisticSkill = 4,
            weaponSkill = 3,
            skillName = "Adrenal Frenzy",
            skillDesc = "Synapse surge: +1 to Hit & +1 Dmg for 2 turns + immediate strike.",
            skillCooldown = 2,
            passiveName = "Synapse Conduit",
            passiveDesc = "Swarm units within 3 tiles gain +1 to Hit (BS/WS).",
            weaponChoice = WarriorWeaponChoice.BONESWORDS,
            mainWeaponName = "Tyranid Boneswords",
            mainWeaponAssault = false,
            pistolDamage = 0
        ),
        UnitTemplate(
            id = "carnifex",
            name = "Carnifex",
            faction = Faction.XENOS,
            archetype = UnitArchetype.CARNIFEX,
            role = "Living Battering Ram",
            points = 410,
            squadSize = 1,
            woundsPerModel = 10,
            toughness = 9,
            save = 2,
            invulnSave = null,
            armorPen = 3,
            moveCm = 18,
            strength = 12,
            damage = 3,
            attacksPerModel = 3,
            range = 1,
            ballisticSkill = 4,
            weaponSkill = 3,
            skillName = "Bio-Plasma Blast",
            skillDesc = "Spews S8 AP-3 D2 corrosive plasma up to Range 4 in an AoE splash.",
            skillCooldown = 3,
            passiveName = "Living Ram",
            passiveDesc = "10W, T9, 2+ Chitin Save and S12 AP-3 D3 crushing claws.",
            mainWeaponName = "Crushing Claws",
            mainWeaponAssault = false,
            pistolWeaponName = "Spine Banks",
            pistolAttacks = 1,
            pistolDamage = 1,
            pistolStrength = 6,
            pistolArmorPen = 1,
            pistolRange = 3,
            pistolCooldown = 1
        )
    )

    fun defaultMarineRoster(): List<RosterSlot> = listOf(
        RosterSlot("m1", "intercessors"),
        RosterSlot("m2", "hellblasters"),
        RosterSlot("m3", "assault_intercessors"),
        RosterSlot("m4", "terminators"),
        RosterSlot("m5", "dreadnought")
    )

    fun defaultXenosRoster(): List<RosterSlot> = listOf(
        RosterSlot("x1", "termagants"),
        RosterSlot("x2", "hormagaunts"),
        RosterSlot("x3", "warriors"),
        RosterSlot("x4", "carnifex")
    )

    fun serializeTemplates(templates: Map<String, UnitTemplate>): String {
        val arr = JSONArray()
        templates.values.forEach { t ->
            arr.put(JSONObject().apply {
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
                put("skillCooldown", t.skillCooldown)
                put("weaponChoice", t.weaponChoice?.name ?: "")
                put("mainWeaponName", t.mainWeaponName)
                put("mainWeaponAssault", t.mainWeaponAssault)
                put("mainWeaponPistol", t.mainWeaponPistol)
                put("pistolAttacks", t.pistolAttacks)
                put("pistolDamage", t.pistolDamage)
                put("pistolStrength", t.pistolStrength)
                put("pistolArmorPen", t.pistolArmorPen)
                put("pistolRange", t.pistolRange)
                put("pistolCooldown", t.pistolCooldown)
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
                val wcStr = obj.optString("weaponChoice")
                val wc = if (wcStr.isNotBlank()) runCatching { WarriorWeaponChoice.valueOf(wcStr) }.getOrNull() else existing.weaponChoice
                val rawInv = if (obj.has("invulnSave")) obj.optInt("invulnSave", 0) else (existing.invulnSave ?: 0)
                val parsedInv = if (rawInv in 2..6) rawInv else null
                base[id] = existing.copy(
                    points = obj.optInt("points", existing.points),
                    squadSize = obj.optInt("squadSize", existing.squadSize).coerceIn(1, 5),
                    woundsPerModel = obj.optInt("woundsPerModel", existing.woundsPerModel).coerceIn(1, 30),
                    toughness = obj.optInt("toughness", existing.toughness).coerceIn(1, 14),
                    save = obj.optInt("save", existing.save).coerceIn(2, 6),
                    invulnSave = parsedInv,
                    armorPen = obj.optInt("armorPen", existing.armorPen).coerceIn(0, 5),
                    moveCm = obj.optInt("moveCm", existing.moveCm).coerceIn(3, 45),
                    strength = obj.optInt("strength", existing.strength).coerceIn(1, 20),
                    damage = obj.optInt("damage", existing.damage).coerceIn(1, 20),
                    attacksPerModel = obj.optInt("attacksPerModel", existing.attacksPerModel).coerceIn(1, 10),
                    range = obj.optInt("range", existing.range).coerceIn(1, 12),
                    ballisticSkill = obj.optInt("ballisticSkill", existing.ballisticSkill).coerceIn(2, 6),
                    weaponSkill = obj.optInt("weaponSkill", existing.weaponSkill).coerceIn(2, 6),
                    skillCooldown = if (id == "terminators") existing.skillCooldown else obj.optInt("skillCooldown", existing.skillCooldown).coerceIn(1, 8),
                    weaponChoice = wc,
                    mainWeaponName = obj.optString("mainWeaponName", existing.mainWeaponName).ifBlank { existing.mainWeaponName },
                    mainWeaponAssault = if (obj.has("mainWeaponAssault")) obj.optBoolean("mainWeaponAssault", existing.mainWeaponAssault) else existing.mainWeaponAssault,
                    mainWeaponPistol = if (obj.has("mainWeaponPistol")) obj.optBoolean("mainWeaponPistol", existing.mainWeaponPistol) else existing.mainWeaponPistol,
                    pistolAttacks = obj.optInt("pistolAttacks", existing.pistolAttacks).coerceIn(1, 10),
                    pistolDamage = obj.optInt("pistolDamage", existing.pistolDamage).coerceIn(0, 12),
                    pistolStrength = obj.optInt("pistolStrength", existing.pistolStrength).coerceIn(1, 20),
                    pistolArmorPen = obj.optInt("pistolArmorPen", existing.pistolArmorPen).coerceIn(0, 5),
                    pistolRange = obj.optInt("pistolRange", existing.pistolRange).coerceIn(1, 8),
                    pistolCooldown = obj.optInt("pistolCooldown", existing.pistolCooldown).coerceIn(1, 6)
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
