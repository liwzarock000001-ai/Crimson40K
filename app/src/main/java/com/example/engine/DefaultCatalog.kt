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
            hpPerModel = 24,
            armor = 4,
            armorPen = 2,
            mp = 4,
            ap = 2,
            damage = 10,
            attacksPerModel = 2,
            range = 5,
            accuracy = 78,
            skillName = "Bolt Volley",
            skillDesc = "Fires rapid disciplined salvos (+1 attack per model, +10% accuracy).",
            skillCooldown = 2,
            skillApCost = 1,
            passiveName = "Tactical Discipline",
            passiveDesc = "+10% Accuracy when stationary (full MP).",
            pistolDamage = 8,
            pistolApCost = 1,
            pistolRange = 3,
            pistolCooldown = 1
        ),
        UnitTemplate(
            id = "hellblasters",
            name = "Hellblaster Squad",
            faction = Faction.MARINES,
            archetype = UnitArchetype.HELLBLASTER,
            role = "Heavy Plasma",
            points = 260,
            squadSize = 4,
            hpPerModel = 24,
            armor = 4,
            armorPen = 6,
            mp = 4,
            ap = 2,
            damage = 19,
            attacksPerModel = 1,
            range = 5,
            accuracy = 76,
            skillName = "Plasma Overcharge",
            skillDesc = "Supercharged plasma beam (+50% damage, +2 Armor Pen).",
            skillCooldown = 2,
            skillApCost = 1,
            passiveName = "Armor Melter",
            passiveDesc = "High Armor Pen ignores most chitin and plating.",
            pistolDamage = 11,
            pistolApCost = 1,
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
            hpPerModel = 26,
            armor = 4,
            armorPen = 3,
            mp = 5,
            ap = 2,
            damage = 14,
            attacksPerModel = 2,
            range = 1,
            accuracy = 84,
            skillName = "Chainsword Whirlwind",
            skillDesc = "Cleaves all adjacent enemy squads in melee range.",
            skillCooldown = 2,
            skillApCost = 1,
            passiveName = "Shock Assault",
            passiveDesc = "Melee strikes build +15 extra Momentum.",
            pistolDamage = 9,
            pistolApCost = 1,
            pistolRange = 3,
            pistolCooldown = 1
        ),
        UnitTemplate(
            id = "terminators",
            name = "Assault Terminator",
            faction = Faction.MARINES,
            archetype = UnitArchetype.TERMINATOR,
            role = "Elite Heavy",
            points = 340,
            squadSize = 3,
            hpPerModel = 42,
            armor = 7,
            armorPen = 4,
            mp = 3,
            ap = 2,
            damage = 18,
            attacksPerModel = 2,
            range = 4,
            accuracy = 80,
            skillName = "Teleport Strike",
            skillDesc = "Teleports up to 9 tiles away (must be > 4 tiles from enemies). Grants 50% damage reduction for 1 turn.",
            skillCooldown = 4,
            skillApCost = 1,
            passiveName = "Crux Terminatus",
            passiveDesc = "Heavy Assault Terminator plating and Storm Shield resist fire.",
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
            hpPerModel = 165,
            armor = 8,
            armorPen = 5,
            mp = 4,
            ap = 2,
            damage = 42,
            attacksPerModel = 2,
            range = 6,
            accuracy = 78,
            skillName = "Sweeping Barrage",
            skillDesc = "Fires gatling & plasma splash hitting target + nearby foes.",
            skillCooldown = 3,
            skillApCost = 1,
            passiveName = "Duty Eternal",
            passiveDesc = "Reduces incoming chip damage with reinforced chassis.",
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
            hpPerModel = 16,
            armor = 2,
            armorPen = 1,
            mp = 5,
            ap = 2,
            damage = 8,
            attacksPerModel = 2,
            range = 4,
            accuracy = 70,
            skillName = "Acidic Salvo",
            skillDesc = "High-volume fleshborer barrage (+1 attack per model).",
            skillCooldown = 2,
            skillApCost = 1,
            passiveName = "Skulking Horrors",
            passiveDesc = "Gains extra cover bonus from craters & barricades.",
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
            hpPerModel = 17,
            armor = 2,
            armorPen = 2,
            mp = 6,
            ap = 2,
            damage = 11,
            attacksPerModel = 2,
            range = 1,
            accuracy = 80,
            skillName = "Bounding Leap",
            skillDesc = "Surges +3 MP & empowers scything talons this turn.",
            skillCooldown = 2,
            skillApCost = 1,
            passiveName = "Hyper-Adaptation",
            passiveDesc = "Highest base movement speed in the Crimson Sector.",
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
            hpPerModel = 38,
            armor = 4,
            armorPen = 4,
            mp = 4,
            ap = 2,
            damage = 20,
            attacksPerModel = 2,
            range = 1,
            accuracy = 82,
            skillName = "Adrenal Frenzy",
            skillDesc = "Synapse surge: +15% DMG & Acc for 2 turns + immediate strike.",
            skillCooldown = 2,
            skillApCost = 1,
            passiveName = "Synapse Conduit",
            passiveDesc = "Swarm units within 3 tiles gain +10% Accuracy.",
            weaponChoice = WarriorWeaponChoice.BONESWORDS,
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
            hpPerModel = 170,
            armor = 7,
            armorPen = 5,
            mp = 4,
            ap = 2,
            damage = 46,
            attacksPerModel = 2,
            range = 1,
            accuracy = 82,
            skillName = "Bio-Plasma Blast",
            skillDesc = "Spews corrosive plasma up to Range 4 in an AoE splash.",
            skillCooldown = 3,
            skillApCost = 1,
            passiveName = "Living Ram",
            passiveDesc = "Monstrous chitin and crushing claws devastate armor.",
            pistolDamage = 18,
            pistolApCost = 1,
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
                put("hpPerModel", t.hpPerModel)
                put("armor", t.armor)
                put("armorPen", t.armorPen)
                put("mp", t.mp)
                put("ap", t.ap)
                put("damage", t.damage)
                put("attacksPerModel", t.attacksPerModel)
                put("range", t.range)
                put("accuracy", t.accuracy)
                put("skillCooldown", t.skillCooldown)
                put("skillApCost", t.skillApCost)
                put("weaponChoice", t.weaponChoice?.name ?: "")
                put("pistolDamage", t.pistolDamage)
                put("pistolApCost", t.pistolApCost)
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
                base[id] = existing.copy(
                    points = obj.optInt("points", existing.points),
                    squadSize = obj.optInt("squadSize", existing.squadSize).coerceIn(1, 5),
                    hpPerModel = obj.optInt("hpPerModel", existing.hpPerModel).coerceAtLeast(1),
                    armor = obj.optInt("armor", existing.armor).coerceIn(0, 10),
                    armorPen = obj.optInt("armorPen", existing.armorPen).coerceIn(0, 10),
                    mp = obj.optInt("mp", existing.mp).coerceIn(1, 12),
                    ap = obj.optInt("ap", existing.ap).coerceIn(1, 6),
                    damage = obj.optInt("damage", existing.damage).coerceAtLeast(1),
                    attacksPerModel = obj.optInt("attacksPerModel", existing.attacksPerModel).coerceIn(1, 6),
                    range = obj.optInt("range", existing.range).coerceIn(1, 12),
                    accuracy = obj.optInt("accuracy", existing.accuracy).coerceIn(5, 100),
                    skillCooldown = if (id == "terminators") existing.skillCooldown else obj.optInt("skillCooldown", existing.skillCooldown).coerceIn(1, 8),
                    skillApCost = obj.optInt("skillApCost", existing.skillApCost).coerceIn(1, 4),
                    weaponChoice = wc,
                    pistolDamage = obj.optInt("pistolDamage", existing.pistolDamage).coerceAtLeast(0),
                    pistolApCost = obj.optInt("pistolApCost", existing.pistolApCost).coerceIn(1, 4),
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
