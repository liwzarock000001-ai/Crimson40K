package com.example

import com.example.engine.AiController
import com.example.engine.AiDecision
import com.example.engine.DefaultCatalog
import com.example.engine.MapGenerator
import com.example.engine.TacticalRules
import com.example.model.BattleUnit
import com.example.model.CoverTag
import com.example.model.Facing
import com.example.model.Faction
import com.example.model.GridPos
import com.example.model.RosterSlot
import com.example.model.TerrainType
import com.example.model.TerminatorWeaponChoice
import com.example.model.UnitArchetype
import com.example.model.UnitTemplate
import com.example.model.WarriorWeaponChoice
import com.example.model.WeaponMode
import com.example.ui.battle.UnitMeshSculptor3D
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class TacticalEngineTest {

    @Test
    fun pathfindingAvoidsRuinWallsAndOccupiedTiles() {
        val map = MapGenerator.generateSectorMap(42L)
        val tpl = DefaultCatalog.defaultTemplates.first { it.id == "intercessors" }
        val unit = BattleUnit(
            uid = "u1",
            template = tpl,
            faction = Faction.MARINES,
            x = 2,
            y = 6,
            facing = Facing.EAST,
            totalWounds = tpl.maxTotalWounds,
            maxTotalWounds = tpl.maxTotalWounds,
            moveCmLeft = tpl.moveCm.toFloat()
        )
        val search = TacticalRules.computeReachableTiles(unit, map, listOf(unit))
        assertTrue(search.costs.isNotEmpty())
        assertFalse(GridPos(4, 4) in search.costs) // PILLAR
        assertFalse(GridPos(2, 6) in search.costs) // Start tile excluded
    }

    @Test
    fun flankingAndRearCoverTagDetection() {
        val map = MapGenerator.generateSectorMap(1L)
        val marineTpl = DefaultCatalog.defaultTemplates.first { it.id == "intercessors" }
        val xenosTpl = DefaultCatalog.defaultTemplates.first { it.id == "warriors" }

        val attacker = BattleUnit(
            uid = "m1",
            template = marineTpl,
            faction = Faction.MARINES,
            x = 2,
            y = 6,
            facing = Facing.EAST,
            totalWounds = marineTpl.maxTotalWounds,
            maxTotalWounds = marineTpl.maxTotalWounds,
            moveCmLeft = 12f
        )
        val defenderFacingEast = BattleUnit(
            uid = "x1",
            template = xenosTpl,
            faction = Faction.XENOS,
            x = 5,
            y = 6,
            facing = Facing.EAST,
            totalWounds = xenosTpl.maxTotalWounds,
            maxTotalWounds = xenosTpl.maxTotalWounds,
            moveCmLeft = 15f
        )
        assertEquals(CoverTag.OPEN, TacticalRules.evaluateCoverTag(attacker, defenderFacingEast, map))

        val outcome = TacticalRules.resolveAttack(
            attacker = attacker,
            primaryTarget = defenderFacingEast,
            allUnits = listOf(attacker, defenderFacingEast),
            mode = WeaponMode.MAIN,
            map = map,
            rng = Random(7)
        )
        assertTrue(outcome.totalDamageDealt > 0)
    }

    @Test
    fun aiSelectsValidActionAgainstEnemy() {
        val map = MapGenerator.generateSectorMap(1L)
        val marineTpl = DefaultCatalog.defaultTemplates.first { it.id == "intercessors" }
        val xenosTpl = DefaultCatalog.defaultTemplates.first { it.id == "termagants" }
        val marine = BattleUnit("m1", marineTpl, Faction.MARINES, 2, 6, Facing.EAST, marineTpl.maxTotalWounds, marineTpl.maxTotalWounds, 15f)
        val xenos = BattleUnit("x1", xenosTpl, Faction.XENOS, 12, 6, Facing.WEST, xenosTpl.maxTotalWounds, xenosTpl.maxTotalWounds, 15f)

        val decision = AiController.decideNextAction(Faction.XENOS, listOf(marine, xenos), map)
        assertNotNull(decision)
        assertTrue(decision is AiDecision.Move || decision is AiDecision.Attack || decision is AiDecision.Advance || decision is AiDecision.Charge)
    }

    @Test
    fun allSculptedUnitMeshesBuildWithValidFiniteGeometry() {
        for (archetype in com.example.model.UnitArchetype.entries) {
            val mesh = com.example.ui.battle.MeshCatalog3D.getUnitMesh(archetype)
            assertTrue("Expected vertices for $archetype", mesh.vertexCount > 50)
            assertEquals("Strided 10-float layout for $archetype", 0, mesh.data.size % 10)
            for (v in mesh.data) {
                assertTrue("Non-finite vertex value in $archetype", v.isFinite())
            }
        }
    }

    @Test
    fun tabletopWoundChartAndSavesWorkAuthentically() {
        assertEquals(2, TacticalRules.woundRollNeeded(8, 4))
        assertEquals(3, TacticalRules.woundRollNeeded(5, 4))
        assertEquals(4, TacticalRules.woundRollNeeded(4, 4))
        assertEquals(5, TacticalRules.woundRollNeeded(4, 5))
        assertEquals(6, TacticalRules.woundRollNeeded(4, 8))

        val marineTpl = DefaultCatalog.defaultTemplates.first { it.id == "intercessors" }
        val termiTpl = DefaultCatalog.defaultTemplates.first { it.id == "terminators" }
        val gantTpl = DefaultCatalog.defaultTemplates.first { it.id == "termagants" }

        val marine = BattleUnit("m", marineTpl, Faction.MARINES, 2, 2, Facing.EAST, marineTpl.maxTotalWounds, marineTpl.maxTotalWounds, 15f)
        val termi = BattleUnit("t", termiTpl, Faction.MARINES, 3, 2, Facing.EAST, termiTpl.maxTotalWounds, termiTpl.maxTotalWounds, 12f)
        val gant = BattleUnit("g", gantTpl, Faction.XENOS, 4, 2, Facing.WEST, gantTpl.maxTotalWounds, gantTpl.maxTotalWounds, 15f)

        // Marine 3+ Sv vs AP-2 -> 5+
        assertEquals(5 to false, TacticalRules.computeEffectiveSave(marine, 2, CoverTag.OPEN))
        // Marine 3+ Sv in Cover vs AP0 stays 3+, vs AP-1 improves back to 3+
        assertEquals(3 to false, TacticalRules.computeEffectiveSave(marine, 0, CoverTag.COVER))
        assertEquals(3 to false, TacticalRules.computeEffectiveSave(marine, 1, CoverTag.COVER))
        // Terminator 2+/4++ vs AP-3 uses 4++ Invulnerable Save
        assertEquals(4 to true, TacticalRules.computeEffectiveSave(termi, 3, CoverTag.OPEN))
        // Termagant 5+ vs AP-2 -> 7 (No Save); in Cover vs AP0 -> 3+ (+2 bonus)
        assertEquals(7 to false, TacticalRules.computeEffectiveSave(gant, 2, CoverTag.OPEN))
        assertEquals(3 to false, TacticalRules.computeEffectiveSave(gant, 0, CoverTag.COVER))
    }

    @Test
    fun tabletopTargetPreviewDistinguishesBsAndWs() {
        val map = MapGenerator.generateSectorMap(1L)
        val intercessorTpl = DefaultCatalog.defaultTemplates.first { it.id == "intercessors" }
        val warriorTpl = DefaultCatalog.defaultTemplates.first { it.id == "warriors" }

        val rangedMarine = BattleUnit("m", intercessorTpl, Faction.MARINES, 1, 6, Facing.EAST, intercessorTpl.maxTotalWounds, intercessorTpl.maxTotalWounds, 15f)
        val meleeMarine = BattleUnit("m_adj", intercessorTpl, Faction.MARINES, 2, 6, Facing.EAST, intercessorTpl.maxTotalWounds, intercessorTpl.maxTotalWounds, 15f)
        val warrior = BattleUnit("w", warriorTpl, Faction.XENOS, 3, 6, Facing.WEST, warriorTpl.maxTotalWounds, warriorTpl.maxTotalWounds, 15f)

        val rangedPreview = TacticalRules.canTarget(rangedMarine, warrior, WeaponMode.MAIN, map, listOf(rangedMarine, warrior))
        assertNotNull(rangedPreview)
        assertFalse(rangedPreview!!.isMelee)
        // Intercessor BS 3+ with stationary (+1) => 2+
        assertEquals(2, rangedPreview.hitRollNeeded)
        assertEquals(5, rangedPreview.targetToughness)

        val meleePreview = TacticalRules.canTarget(warrior, meleeMarine, WeaponMode.MAIN, map, listOf(warrior, meleeMarine))
        assertNotNull(meleePreview)
        assertTrue(meleePreview!!.isMelee)
        assertEquals(3, meleePreview.hitRollNeeded) // Warrior WS 3+
        assertEquals(3, meleePreview.woundRollNeeded) // S5 vs T4 => 3+
    }

    @Test
    fun woundAllocationPreventsExcessDamageSpilloverBetweenModels() {
        // Squad of 5x 1W Termagants (total 5W) hit by a D3 attack -> only 1W kills front model (no spillover!)
        assertEquals(1, TacticalRules.allocateWoundDamage(5, woundsPerModel = 1, rawAttackDamage = 3))
        // Squad of 5x 2W Intercessors (total 10W) hit by D3 -> 2W kills front model (no 3rd point spills over)
        assertEquals(2, TacticalRules.allocateWoundDamage(10, woundsPerModel = 2, rawAttackDamage = 3))
        // Wounded 2W Intercessor with 1W left (total 9W) hit by D2 -> only 1W finishes wounded model
        assertEquals(1, TacticalRules.allocateWoundDamage(9, woundsPerModel = 2, rawAttackDamage = 2))
        // Single 12W Dreadnought takes full D3 damage cleanly
        assertEquals(3, TacticalRules.allocateWoundDamage(12, woundsPerModel = 12, rawAttackDamage = 3))
    }

    @Test
    fun centimeterMovementPreviewSupportsFractionalDistancesAndForwardSteps() {
        val map = MapGenerator.generateSectorMap(1L)
        val marineTpl = DefaultCatalog.defaultTemplates.first { it.id == "intercessors" }
        val marine = BattleUnit(
            uid = "m1",
            template = marineTpl,
            faction = Faction.MARINES,
            x = 2,
            y = 5,
            facing = Facing.EAST,
            totalWounds = marineTpl.maxTotalWounds,
            maxTotalWounds = marineTpl.maxTotalWounds,
            moveCmLeft = 15f
        )

        // Move forward 3.0 cm -> +1.0 tile in +X direction
        val forward3 = TacticalRules.computeForwardCmPreview(marine, 3f, listOf(marine), map)
        assertNotNull(forward3)
        assertEquals(3.0f, forward3!!.distanceCm, 0.05f)
        assertEquals(3.0f, forward3.targetX, 0.05f)
        assertEquals(5.0f, forward3.targetY, 0.05f)

        // Tap world point 1.5 tiles away in open lane -> 4.5 cm
        val tapPreview = TacticalRules.computeCmMovePreview(marine, 3.5f, 5.0f, listOf(marine), map)
        assertNotNull(tapPreview)
        assertEquals(4.5f, tapPreview!!.distanceCm, 0.05f)

        // Tapping directly inside/against PILLAR at (4, 4) should be blocked
        val blockedWall = TacticalRules.computeCmMovePreview(marine, 4.0f, 4.0f, listOf(marine), map)
        assertEquals(null, blockedWall)
    }

    @Test
    fun tabletopPhaseRulesAllowShootAndMeleeWithoutActionPoints() {
        val map = MapGenerator.generateSectorMap(1L)
        val intercessorTpl = DefaultCatalog.defaultTemplates.first { it.id == "intercessors" }
        val assaultTpl = DefaultCatalog.defaultTemplates.first { it.id == "assault_intercessors" }
        val warriorTpl = DefaultCatalog.defaultTemplates.first { it.id == "warriors" }

        val shooter = BattleUnit("m1", intercessorTpl, Faction.MARINES, 1, 5, Facing.EAST, intercessorTpl.maxTotalWounds, intercessorTpl.maxTotalWounds, 15f)
        val brawler = BattleUnit("m2", assaultTpl, Faction.MARINES, 2, 6, Facing.EAST, assaultTpl.maxTotalWounds, assaultTpl.maxTotalWounds, 18f)
        val enemy = BattleUnit("x1", warriorTpl, Faction.XENOS, 3, 6, Facing.WEST, warriorTpl.maxTotalWounds, warriorTpl.maxTotalWounds, 15f)

        // Ranged shooter fires once -> hasShot becomes true and blocks further ranged shots this turn
        val shootOutcome = TacticalRules.resolveAttack(shooter, enemy, listOf(shooter, enemy), WeaponMode.MAIN, map, Random(1))
        assertTrue(shootOutcome.updatedAttacker.hasShot)
        assertFalse(TacticalRules.canUseMode(shootOutcome.updatedAttacker, WeaponMode.MAIN))
        assertFalse(TacticalRules.canUseMode(shootOutcome.updatedAttacker, WeaponMode.PISTOL))
        assertFalse(TacticalRules.canUseMode(shootOutcome.updatedAttacker, WeaponMode.SKILL))

        // Assault Intercessor fires Pistol (Shooting phase) and can STILL Fight in Melee (Fight phase)
        val pistolOutcome = TacticalRules.resolveAttack(brawler, enemy, listOf(brawler, enemy), WeaponMode.PISTOL, map, Random(2))
        assertTrue(pistolOutcome.updatedAttacker.hasShot)
        assertFalse(pistolOutcome.updatedAttacker.hasFought)
        assertFalse(TacticalRules.canUseMode(pistolOutcome.updatedAttacker, WeaponMode.PISTOL))
        assertTrue(TacticalRules.canUseMode(pistolOutcome.updatedAttacker, WeaponMode.MAIN))

        val meleeOutcome = TacticalRules.resolveAttack(pistolOutcome.updatedAttacker, enemy, listOf(pistolOutcome.updatedAttacker, enemy), WeaponMode.MAIN, map, Random(3))
        assertTrue(meleeOutcome.updatedAttacker.hasFought)
        assertFalse(TacticalRules.canUseMode(meleeOutcome.updatedAttacker, WeaponMode.MAIN))
        assertFalse(TacticalRules.canUseMode(meleeOutcome.updatedAttacker, WeaponMode.SKILL))
    }

    @Test
    fun advanceRollsD6AndBlocksChargeAndNonAssaultWeapons() {
        val (roll, bonusCm) = TacticalRules.rollAdvanceBonus(Random(42))
        assertTrue(roll in 1..6)
        assertEquals(roll.toFloat(), bonusCm, 0.01f)

        val intercessorTpl = DefaultCatalog.defaultTemplates.first { it.id == "intercessors" } // Assault = true
        val hellblasterTpl = DefaultCatalog.defaultTemplates.first { it.id == "hellblasters" } // 11e Assault = true
        val dreadTpl = DefaultCatalog.defaultTemplates.first { it.id == "dreadnought" } // Macro Plasma Assault = false

        val advancedIntercessor = BattleUnit(
            uid = "m_adv1",
            template = intercessorTpl,
            faction = Faction.MARINES,
            x = 2,
            y = 5,
            facing = Facing.EAST,
            totalWounds = intercessorTpl.maxTotalWounds,
            maxTotalWounds = intercessorTpl.maxTotalWounds,
            moveCmLeft = intercessorTpl.moveCm + bonusCm,
            hasAdvanced = true
        )
        // Advanced unit cannot Charge, cannot fire Pistol, CAN shoot Assault Bolt Rifle
        assertFalse(advancedIntercessor.canAdvance)
        assertFalse(advancedIntercessor.canCharge)
        assertTrue(advancedIntercessor.canShoot)
        assertTrue(TacticalRules.canUseMode(advancedIntercessor, WeaponMode.MAIN))
        assertFalse(TacticalRules.canUseMode(advancedIntercessor, WeaponMode.PISTOL))

        // In 11th Edition, Hellblasters' Plasma Incinerator has [ASSAULT]
        assertTrue(hellblasterTpl.mainWeaponAssault)
        val advancedHellblaster = BattleUnit(
            uid = "m_adv2",
            template = hellblasterTpl,
            faction = Faction.MARINES,
            x = 2,
            y = 6,
            facing = Facing.EAST,
            totalWounds = hellblasterTpl.maxTotalWounds,
            maxTotalWounds = hellblasterTpl.maxTotalWounds,
            moveCmLeft = hellblasterTpl.moveCm + bonusCm,
            hasAdvanced = true
        )
        assertFalse(advancedHellblaster.canCharge)
        assertTrue(advancedHellblaster.canShoot)
        assertTrue(TacticalRules.canUseMode(advancedHellblaster, WeaponMode.MAIN))
        assertFalse(TacticalRules.canUseMode(advancedHellblaster, WeaponMode.PISTOL))

        // Non-Assault Macro Plasma Incinerator cannot shoot or charge after Advancing
        val advancedDread = BattleUnit(
            uid = "m_adv3",
            template = dreadTpl,
            faction = Faction.MARINES,
            x = 2,
            y = 7,
            facing = Facing.EAST,
            totalWounds = dreadTpl.maxTotalWounds,
            maxTotalWounds = dreadTpl.maxTotalWounds,
            moveCmLeft = dreadTpl.moveCm + bonusCm,
            hasAdvanced = true
        )
        assertFalse(advancedDread.canCharge)
        assertFalse(advancedDread.canShoot)
        assertFalse(TacticalRules.canUseMode(advancedDread, WeaponMode.MAIN))
        assertFalse(TacticalRules.canUseMode(advancedDread, WeaponMode.SKILL))
    }

    @Test
    fun chargeComputesEngagementSpotAndEnablesMeleeForChargedUnits() {
        val map = MapGenerator.generateSectorMap(1L)
        val intercessorTpl = DefaultCatalog.defaultTemplates.first { it.id == "intercessors" }
        val warriorTpl = DefaultCatalog.defaultTemplates.first { it.id == "warriors" }

        val charger = BattleUnit("m1", intercessorTpl, Faction.MARINES, 5, 8, Facing.EAST, intercessorTpl.maxTotalWounds, intercessorTpl.maxTotalWounds, 0f)
        val enemy = BattleUnit("x1", warriorTpl, Faction.XENOS, 10, 8, Facing.WEST, warriorTpl.maxTotalWounds, warriorTpl.maxTotalWounds, 15f)

        val preview = TacticalRules.findChargeEngagementSpot(charger, enemy, listOf(charger, enemy), map)
        assertNotNull(preview)
        assertTrue(preview!!.requiredRoll in 2..12)
        assertTrue(preview.path.isNotEmpty())

        // After Charge lands adjacent to enemy, shooting is blocked and Melee WS fight is unlocked
        val chargedUnit = charger.copy(
            x = 9,
            y = 8,
            worldX = preview.destinationX,
            worldY = preview.destinationY,
            hasCharged = true
        )
        assertFalse(chargedUnit.canShoot)
        assertTrue(chargedUnit.canFight)
        assertFalse(TacticalRules.canUseMode(chargedUnit, WeaponMode.PISTOL))

        val meleePreview = TacticalRules.canTarget(chargedUnit, enemy, WeaponMode.MAIN, map)
        assertNotNull(meleePreview)
        assertTrue(meleePreview!!.isMelee)
        assertTrue(meleePreview.inRange)
        assertEquals(3, meleePreview.hitRollNeeded) // Intercessor WS 3+
    }

    @Test
    fun baseSquadSizesAndWeaponAttacksAreConfigurable() {
        val termis = DefaultCatalog.defaultTemplates.first { it.id == "terminators" }
        val hellblasters = DefaultCatalog.defaultTemplates.first { it.id == "hellblasters" }

        // Verify base squad sizes are 5
        assertEquals(5, termis.squadSize)
        assertEquals(20, termis.maxTotalWounds) // 5 models * 4W = 20W
        assertEquals(5, hellblasters.squadSize)
        assertEquals(10, hellblasters.maxTotalWounds) // 5 models * 2W = 10W

        // Unit attacks count scales with aliveModels * attacksPerModel (main) or pistolAttacks (pistol)
        val fullHellblaster = BattleUnit(
            uid = "h1",
            template = hellblasters,
            faction = Faction.MARINES,
            x = 2,
            y = 2,
            facing = Facing.EAST,
            totalWounds = 10,
            maxTotalWounds = 10,
            moveCmLeft = 15f
        )
        assertEquals(5, fullHellblaster.aliveModels)
        assertEquals(10, TacticalRules.attacksCount(fullHellblaster, WeaponMode.MAIN)) // 5 * 2
        assertEquals(5, TacticalRules.attacksCount(fullHellblaster, WeaponMode.PISTOL)) // 5 * 1

        // Custom template with adjusted attacks: Main A=4, Pistol A=3
        val customTpl = hellblasters.copy(attacksPerModel = 4, pistolAttacks = 3)
        val customUnit = fullHellblaster.copy(
            template = customTpl,
            totalWounds = 6 // 3 models alive (3 * 2W = 6W)
        )
        assertEquals(3, customUnit.aliveModels)
        assertEquals(12, TacticalRules.attacksCount(customUnit, WeaponMode.MAIN)) // 3 models * 4 attacks = 12
        assertEquals(9, TacticalRules.attacksCount(customUnit, WeaponMode.PISTOL)) // 3 models * 3 pistol attacks = 9
    }

    @Test
    fun engagedMeleeBlocksNonPistolRangedAndAllowsPistols() {
        val map = MapGenerator.generateSectorMap(1L)
        val intercessorTpl = DefaultCatalog.defaultTemplates.first { it.id == "intercessors" } // has Bolt Pistol
        val hellblasterTpl = DefaultCatalog.defaultTemplates.first { it.id == "hellblasters" } // has Plasma Pistol
        val warriorTpl = DefaultCatalog.defaultTemplates.first { it.id == "warriors" }

        // Intercessor engaged with adjacent Warrior at (3,6), plus distant Warrior at (6,6)
        val intercessor = BattleUnit("m1", intercessorTpl, Faction.MARINES, 2, 6, Facing.EAST, intercessorTpl.maxTotalWounds, intercessorTpl.maxTotalWounds, 15f)
        val adjacentFoe = BattleUnit("x1", warriorTpl, Faction.XENOS, 3, 6, Facing.WEST, warriorTpl.maxTotalWounds, warriorTpl.maxTotalWounds, 15f)
        val distantFoe = BattleUnit("x2", warriorTpl, Faction.XENOS, 6, 6, Facing.WEST, warriorTpl.maxTotalWounds, warriorTpl.maxTotalWounds, 15f)
        val allUnits = listOf(intercessor, adjacentFoe, distantFoe)

        assertTrue(TacticalRules.isEngagedInMelee(intercessor, allUnits))

        // Non-pistol main weapon (Bolt Rifle) CANNOT shoot distant enemy while engaged
        assertEquals(null, TacticalRules.canTarget(intercessor, distantFoe, WeaponMode.MAIN, map, allUnits))
        // Non-pistol ranged SKILL is also blocked while engaged
        assertFalse(TacticalRules.canUseMode(intercessor, WeaponMode.SKILL, allUnits))
        assertEquals(null, TacticalRules.canTarget(intercessor, distantFoe, WeaponMode.SKILL, map, allUnits))

        // Targeting the adjacent foe in MAIN mode switches to MELEE (WS)
        val meleeVsAdjacent = TacticalRules.canTarget(intercessor, adjacentFoe, WeaponMode.MAIN, map, allUnits)
        assertNotNull(meleeVsAdjacent)
        assertTrue(meleeVsAdjacent!!.isMelee)

        // Bolt Pistol CAN shoot even while engaged in melee!
        assertTrue(TacticalRules.canUseMode(intercessor, WeaponMode.PISTOL, allUnits))
        val pistolVsAdjacent = TacticalRules.canTarget(intercessor, adjacentFoe, WeaponMode.PISTOL, map, allUnits)
        assertNotNull(pistolVsAdjacent)
        assertFalse(pistolVsAdjacent!!.isMelee)
        assertTrue(pistolVsAdjacent.inRange)

        // Main weapon WITH Pistol tag CAN shoot while engaged in melee!
        val pistolTaggedHellblaster = BattleUnit(
            "m2",
            hellblasterTpl.copy(mainWeaponPistol = true),
            Faction.MARINES,
            2,
            6,
            Facing.EAST,
            hellblasterTpl.maxTotalWounds,
            hellblasterTpl.maxTotalWounds,
            15f
        )
        val shootWithPistolTag = TacticalRules.canTarget(pistolTaggedHellblaster, distantFoe, WeaponMode.MAIN, map, listOf(pistolTaggedHellblaster, adjacentFoe, distantFoe))
        assertNotNull(shootWithPistolTag)
        assertFalse(shootWithPistolTag!!.isMelee)
    }

    @Test
    fun meleeCounterAttackUsesSurvivingModelsWeaponStats() {
        val map = MapGenerator.generateSectorMap(1L)
        val termiTpl = DefaultCatalog.defaultTemplates.first { it.id == "terminators" }
        val warriorTpl = DefaultCatalog.defaultTemplates.first { it.id == "warriors" }

        // Terminator squad (5x 4W = 20W, reduced to 12W -> 3 surviving models) attacks adjacent Warrior
        val attacker = BattleUnit("m1", termiTpl, Faction.MARINES, 2, 6, Facing.EAST, 12, termiTpl.maxTotalWounds, 12f)
        val survivor = BattleUnit("x1", warriorTpl, Faction.XENOS, 3, 6, Facing.WEST, warriorTpl.maxTotalWounds, warriorTpl.maxTotalWounds, 15f)

        val counter = TacticalRules.resolveMeleeCounterAttack(survivor, attacker, map, Random(7))
        assertNotNull(counter)
        assertTrue(counter!!.hits > 0 || counter.floatingTexts.isNotEmpty())

        // Eliminated target (0 wounds left) cannot counter-attack
        val deadDefender = survivor.copy(totalWounds = 0)
        assertEquals(null, TacticalRules.resolveMeleeCounterAttack(deadDefender, attacker, map, Random(7)))
    }

    @Test
    fun vonRyansLeaperPassivesCloakAndTargetWeaponNameWork() {
        val map = MapGenerator.generateSectorMap(1L)
        val intercessorTpl = DefaultCatalog.defaultTemplates.first { it.id == "intercessors" }
        val leaperTpl = DefaultCatalog.defaultTemplates.first { it.id == "von_ryans_leaper" }

        val shooter = BattleUnit("m1", intercessorTpl, Faction.MARINES, 2, 6, Facing.EAST, intercessorTpl.maxTotalWounds, intercessorTpl.maxTotalWounds, 15f)
        val leaper = BattleUnit("x_vr", leaperTpl, Faction.XENOS, 4, 6, Facing.WEST, leaperTpl.maxTotalWounds, leaperTpl.maxTotalWounds, 20f)

        // Ranged shot at Von Ryan's Leaper has -1 BS (+1 to hit roll needed: stationary 2+ becomes 3+)
        val rangedPreview = TacticalRules.canTarget(shooter, leaper, WeaponMode.MAIN, map, listOf(shooter, leaper))
        assertNotNull(rangedPreview)
        assertFalse(rangedPreview!!.isMelee)
        assertEquals(3, rangedPreview.hitRollNeeded)
        assertEquals(intercessorTpl.mainWeaponName, rangedPreview.weaponName)

        // Activate Chameleonic Cloak (1 turn invisible, 3 turn cooldown)
        val cloakedLeaper = TacticalRules.activateSelfSkillIfApplicable(leaper)
        assertNotNull(cloakedLeaper)
        assertEquals(1, cloakedLeaper!!.stealthTurnsLeft)
        assertEquals(3, cloakedLeaper.skillCooldownLeft)
        assertTrue(cloakedLeaper.isCloaked)

        // Cloaked Leaper cannot be targeted in ranged, melee, charge, or counter-attack
        assertEquals(null, TacticalRules.canTarget(shooter, cloakedLeaper, WeaponMode.MAIN, map, listOf(shooter, cloakedLeaper)))
        val adjMarine = shooter.copy(x = 3, y = 6, worldX = 3f, worldY = 6f)
        assertEquals(null, TacticalRules.canTarget(adjMarine, cloakedLeaper, WeaponMode.MAIN, map, listOf(adjMarine, cloakedLeaper)))
        assertEquals(null, TacticalRules.findChargeEngagementSpot(shooter, cloakedLeaper, listOf(shooter, cloakedLeaper), map))
        assertEquals(null, TacticalRules.resolveMeleeCounterAttack(adjMarine, cloakedLeaper, map, Random(1)))
    }

    @Test
    fun chargeRespectsMax12CmPerPipDistance() {
        val map = MapGenerator.generateSectorMap(1L)
        val intercessorTpl = DefaultCatalog.defaultTemplates.first { it.id == "intercessors" }
        val warriorTpl = DefaultCatalog.defaultTemplates.first { it.id == "warriors" }

        // 4 tiles = 12.0 cm center-to-center; adjacent engagement spot sits ~1.1 tiles from enemy (<= 12 cm) -> valid
        val chargerNear = BattleUnit("m_near", intercessorTpl, Faction.MARINES, 5, 8, Facing.EAST, intercessorTpl.maxTotalWounds, intercessorTpl.maxTotalWounds, 0f)
        val enemy = BattleUnit("x1", warriorTpl, Faction.XENOS, 9, 8, Facing.WEST, warriorTpl.maxTotalWounds, warriorTpl.maxTotalWounds, 15f)
        val nearPreview = TacticalRules.findChargeEngagementSpot(chargerNear, enemy, listOf(chargerNear, enemy), map)
        assertNotNull(nearPreview)
        assertTrue(nearPreview!!.distanceCm <= 12.0f)

        // 6 tiles away = ~15+ cm needed even to reach adjacent engagement tile -> > 12 cm, denied!
        val chargerFar = BattleUnit("m_far", intercessorTpl, Faction.MARINES, 2, 8, Facing.EAST, intercessorTpl.maxTotalWounds, intercessorTpl.maxTotalWounds, 0f)
        val farPreview = TacticalRules.findChargeEngagementSpot(chargerFar, enemy, listOf(chargerFar, enemy), map)
        assertEquals(null, farPreview)
    }

    @Test
    fun allMeleeAttacksEnforce2CmReach() {
        val map = MapGenerator.generateSectorMap(1L)
        val assaultTpl = DefaultCatalog.defaultTemplates.first { it.id == "assault_intercessors" }
        val termiTpl = DefaultCatalog.defaultTemplates.first { it.id == "terminators" }
        val warriorTpl = DefaultCatalog.defaultTemplates.first { it.id == "warriors" }

        // Edge gap = 1.0 - 0.78 = 0.22 tiles (~0.66cm) <= 2.0cm -> WITHIN melee reach
        val closeMarine = BattleUnit("m1", assaultTpl, Faction.MARINES, 2, 6, Facing.EAST, assaultTpl.maxTotalWounds, assaultTpl.maxTotalWounds, 18f)
        val closeTermi = BattleUnit("m2", termiTpl, Faction.MARINES, 2, 5, Facing.EAST, termiTpl.maxTotalWounds, termiTpl.maxTotalWounds, 12f)
        val enemy = BattleUnit("x1", warriorTpl, Faction.XENOS, 3, 6, Facing.WEST, warriorTpl.maxTotalWounds, warriorTpl.maxTotalWounds, 15f)

        val closeMain = TacticalRules.canTarget(closeMarine, enemy, WeaponMode.MAIN, map, listOf(closeMarine, enemy))
        assertNotNull(closeMain)
        assertTrue(closeMain!!.inRange)
        assertTrue(closeMain.isMelee)

        val closeSkill = TacticalRules.canTarget(closeMarine, enemy, WeaponMode.SKILL, map, listOf(closeMarine, enemy))
        assertNull(closeSkill) // Requires Charge to use Skill

        val chargedMarine = closeMarine.copy(hasCharged = true)
        val chargedSkill = TacticalRules.canTarget(chargedMarine, enemy, WeaponMode.SKILL, map, listOf(chargedMarine, enemy))
        assertNotNull(chargedSkill)
        assertTrue(chargedSkill!!.inRange)
        assertTrue(chargedSkill.isMelee)

        val closeTermiHammer = TacticalRules.canTarget(closeTermi, enemy, WeaponMode.MAIN, map, listOf(closeTermi, enemy))
        assertNotNull(closeTermiHammer)
        assertTrue(closeTermiHammer!!.inRange)
        assertTrue(closeTermiHammer.isMelee)

        // 1.48 tiles center-to-center => 1.48 - 0.78 = 0.70 tiles (~2.1cm > 2.0cm) -> OUT OF 2CM MELEE REACH
        val outOf2CmEnemy = enemy.copy(x = 3, y = 6, worldX = 3.48f, worldY = 6.0f)
        val tooFarMain = TacticalRules.canTarget(closeMarine, outOf2CmEnemy, WeaponMode.MAIN, map, listOf(closeMarine, outOf2CmEnemy))
        assertEquals(false, tooFarMain?.inRange ?: false)

        val tooFarSkill = TacticalRules.canTarget(closeMarine, outOf2CmEnemy, WeaponMode.SKILL, map, listOf(closeMarine, outOf2CmEnemy))
        assertEquals(false, tooFarSkill?.inRange ?: false)

        val tooFarTermi = TacticalRules.canTarget(closeTermi, outOf2CmEnemy, WeaponMode.MAIN, map, listOf(closeTermi, outOf2CmEnemy))
        assertEquals(false, tooFarTermi?.inRange ?: false)
    }

    @Test
    fun assaultTerminatorDefaultsToHammerAndShieldAndSwitchesModelWithLightningClaws() {
        val termiTpl = DefaultCatalog.defaultTemplates.first { it.id == "terminators" }
        assertEquals(com.example.model.TerminatorWeaponChoice.THUNDER_HAMMER_SHIELD, termiTpl.terminatorWeapon)
        assertEquals(1, termiTpl.range)
        assertEquals("Thunder Hammer & Storm Shield", termiTpl.mainWeaponName)
        assertEquals(4, termiTpl.woundsPerModel)
        assertEquals(8, termiTpl.strength)
        assertEquals(3, termiTpl.damage)

        val clawsTpl = termiTpl.copy(
            terminatorWeapon = com.example.model.TerminatorWeaponChoice.LIGHTNING_CLAWS,
            mainWeaponName = "Twin Lightning Claws",
            woundsPerModel = 3,
            attacksPerModel = 6,
            strength = 5,
            armorPen = 2,
            damage = 1,
            range = 1
        )
        assertEquals(com.example.model.TerminatorWeaponChoice.LIGHTNING_CLAWS, clawsTpl.terminatorWeapon)
        assertEquals(1, clawsTpl.range)

        val hammerMesh = com.example.ui.battle.MeshCatalog3D.getUnitMesh(com.example.model.UnitArchetype.TERMINATOR, "")
        val clawsMesh = com.example.ui.battle.MeshCatalog3D.getUnitMesh(com.example.model.UnitArchetype.TERMINATOR, "CLAWS")
        assertTrue(hammerMesh.vertexCount > 50)
        assertTrue(clawsMesh.vertexCount > 50)
        assertTrue("Expected distinct geometry between Hammer+Shield and Twin Lightning Claws", hammerMesh.vertexCount != clawsMesh.vertexCount || !hammerMesh.data.contentEquals(clawsMesh.data))
        for (v in clawsMesh.data) assertTrue(v.isFinite())
    }

    @Test
    fun terminatorGlbReplacesDefaultModelWithMatchingHeightAndCenteredBase() {
        val glbMesh = com.example.ui.battle.MeshCatalog3D.getUnitMesh(com.example.model.UnitArchetype.TERMINATOR, "")
        val legacyMesh = com.example.ui.battle.UnitMeshSculptor3D.build(com.example.model.UnitArchetype.TERMINATOR, "")

        // Verify the bundled assault_terminatorH_S.glb (26,613 tris = 79,839 vertices) with UV texture is loaded
        assertEquals(79_839, glbMesh.vertexCount)
        assertEquals(0, glbMesh.data.size % 10)
        assertEquals(glbMesh.vertexCount * 2, glbMesh.texCoords?.size)
        assertTrue((glbMesh.textureBytes?.size ?: 0) > 10_000)

        fun computeBounds(data: FloatArray): FloatArray {
            var minX = Float.POSITIVE_INFINITY
            var maxX = Float.NEGATIVE_INFINITY
            var minY = Float.POSITIVE_INFINITY
            var maxY = Float.NEGATIVE_INFINITY
            var minZ = Float.POSITIVE_INFINITY
            var maxZ = Float.NEGATIVE_INFINITY
            var i = 0
            while (i < data.size) {
                val x = data[i]
                val y = data[i + 1]
                val z = data[i + 2]
                assertTrue(x.isFinite() && y.isFinite() && z.isFinite())
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                if (y < minY) minY = y
                if (y > maxY) maxY = y
                if (z < minZ) minZ = z
                if (z > maxZ) maxZ = z
                i += 10
            }
            return floatArrayOf(minX, maxX, minY, maxY, minZ, maxZ)
        }

        val glbBounds = computeBounds(glbMesh.data)
        val legacyBounds = computeBounds(legacyMesh.data)
        val glbHeight = glbBounds[3] - glbBounds[2]
        val legacyHeight = legacyBounds[3] - legacyBounds[2]

        assertEquals(0f, glbBounds[2], 0.005f)
        assertEquals(legacyHeight, glbHeight, 0.02f)

        // Base plinth (bottom 4.5%) must be centered near (0, 0) in XZ
        var bMinX = Float.POSITIVE_INFINITY
        var bMaxX = Float.NEGATIVE_INFINITY
        var bMinZ = Float.POSITIVE_INFINITY
        var bMaxZ = Float.NEGATIVE_INFINITY
        var i = 0
        while (i < glbMesh.data.size) {
            val y = glbMesh.data[i + 1]
            if (y <= glbHeight * 0.045f) {
                val x = glbMesh.data[i]
                val z = glbMesh.data[i + 2]
                if (x < bMinX) bMinX = x
                if (x > bMaxX) bMaxX = x
                if (z < bMinZ) bMinZ = z
                if (z > bMaxZ) bMaxZ = z
            }
            i += 10
        }
        assertEquals(0f, (bMinX + bMaxX) * 0.5f, 0.02f)
        assertEquals(0f, (bMinZ + bMaxZ) * 0.5f, 0.02f)
    }

    @Test
    fun bladeguardGlbLoadsWithMatchingHeightAndCenteredBase() {
        val glbMesh = com.example.ui.battle.MeshCatalog3D.getUnitMesh(com.example.model.UnitArchetype.BLADEGUARD, "")
        val legacyMesh = com.example.ui.battle.UnitMeshSculptor3D.build(com.example.model.UnitArchetype.BLADEGUARD, "")

        assertTrue("Bladeguard GLB must have vertices", glbMesh.vertexCount > 1000)
        assertEquals(0, glbMesh.data.size % 10)

        fun computeBounds(data: FloatArray): FloatArray {
            var minX = Float.POSITIVE_INFINITY
            var maxX = Float.NEGATIVE_INFINITY
            var minY = Float.POSITIVE_INFINITY
            var maxY = Float.NEGATIVE_INFINITY
            var minZ = Float.POSITIVE_INFINITY
            var maxZ = Float.NEGATIVE_INFINITY
            var i = 0
            while (i < data.size) {
                val x = data[i]
                val y = data[i + 1]
                val z = data[i + 2]
                assertTrue(x.isFinite() && y.isFinite() && z.isFinite())
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                if (y < minY) minY = y
                if (y > maxY) maxY = y
                if (z < minZ) minZ = z
                if (z > maxZ) maxZ = z
                i += 10
            }
            return floatArrayOf(minX, maxX, minY, maxY, minZ, maxZ)
        }

        val glbBounds = computeBounds(glbMesh.data)
        val legacyBounds = computeBounds(legacyMesh.data)
        val glbHeight = glbBounds[3] - glbBounds[2]
        val legacyHeight = legacyBounds[3] - legacyBounds[2]

        assertEquals(0f, glbBounds[2], 0.005f)
        assertEquals(legacyHeight, glbHeight, 0.03f)

        // Base plinth (bottom 4.5%) must be centered near (0, 0) in XZ
        var bMinX = Float.POSITIVE_INFINITY
        var bMaxX = Float.NEGATIVE_INFINITY
        var bMinZ = Float.POSITIVE_INFINITY
        var bMaxZ = Float.NEGATIVE_INFINITY
        var i = 0
        while (i < glbMesh.data.size) {
            val y = glbMesh.data[i + 1]
            if (y <= glbHeight * 0.045f) {
                val x = glbMesh.data[i]
                val z = glbMesh.data[i + 2]
                if (x < bMinX) bMinX = x
                if (x > bMaxX) bMaxX = x
                if (z < bMinZ) bMinZ = z
                if (z > bMaxZ) bMaxZ = z
            }
            i += 10
        }
        assertEquals(0f, (bMinX + bMaxX) * 0.5f, 0.03f)
        // Unit must face +X (forward-most points in X > 0.25f, backpack in -X < -0.20f)
        assertTrue("Bladeguard front weapon/shield extends along +X", glbBounds[1] > 0.25f)
        assertTrue("Bladeguard backpack extends along -X", glbBounds[0] < -0.20f)
    }

    @Test
    fun incursorSquadGlbLoadsWithMatchingHeightAndCenteredBase() {
        val glbMesh = com.example.ui.battle.MeshCatalog3D.getUnitMesh(com.example.model.UnitArchetype.INCURSOR, "")
        val legacyMesh = com.example.ui.battle.UnitMeshSculptor3D.build(com.example.model.UnitArchetype.INCURSOR, "")

        assertTrue("Incursor GLB must have vertices", glbMesh.vertexCount > 1000)
        assertEquals(0, glbMesh.data.size % 10)

        fun computeBounds(data: FloatArray): FloatArray {
            var minX = Float.POSITIVE_INFINITY
            var maxX = Float.NEGATIVE_INFINITY
            var minY = Float.POSITIVE_INFINITY
            var maxY = Float.NEGATIVE_INFINITY
            var minZ = Float.POSITIVE_INFINITY
            var maxZ = Float.NEGATIVE_INFINITY
            var i = 0
            while (i < data.size) {
                val x = data[i]
                val y = data[i + 1]
                val z = data[i + 2]
                assertTrue(x.isFinite() && y.isFinite() && z.isFinite())
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                if (y < minY) minY = y
                if (y > maxY) maxY = y
                if (z < minZ) minZ = z
                if (z > maxZ) maxZ = z
                i += 10
            }
            return floatArrayOf(minX, maxX, minY, maxY, minZ, maxZ)
        }

        val glbBounds = computeBounds(glbMesh.data)
        val legacyBounds = computeBounds(legacyMesh.data)
        val glbHeight = glbBounds[3] - glbBounds[2]
        val legacyHeight = legacyBounds[3] - legacyBounds[2]

        assertEquals(0f, glbBounds[2], 0.005f)
        assertEquals(legacyHeight, glbHeight, 0.02f)

        // Base plinth (bottom 4.5%) must be centered near (0, 0) in XZ
        var bMinX = Float.POSITIVE_INFINITY
        var bMaxX = Float.NEGATIVE_INFINITY
        var bMinZ = Float.POSITIVE_INFINITY
        var bMaxZ = Float.NEGATIVE_INFINITY
        var i = 0
        while (i < glbMesh.data.size) {
            val y = glbMesh.data[i + 1]
            if (y <= glbHeight * 0.045f) {
                val x = glbMesh.data[i]
                val z = glbMesh.data[i + 2]
                if (x < bMinX) bMinX = x
                if (x > bMaxX) bMaxX = x
                if (z < bMinZ) bMinZ = z
                if (z > bMaxZ) bMaxZ = z
            }
            i += 10
        }
        assertEquals(0f, (bMinX + bMaxX) * 0.5f, 0.03f)
        // Unit must face +X (bolt carbine extends along +X > 0.20f, backpack extends along -X < -0.15f)
        assertTrue("Incursor bolt carbine extends along +X", glbBounds[1] > 0.20f)
        assertTrue("Incursor backpack extends along -X", glbBounds[0] < -0.15f)
    }

    @Test
    fun meleeEngagementLocksMovementAdvanceChargeTeleportAndAiUntilEnemyDies() {
        val map = MapGenerator.generateSectorMap(1L)
        val termiTpl = DefaultCatalog.defaultTemplates.first { it.id == "terminators" }
        val warriorTpl = DefaultCatalog.defaultTemplates.first { it.id == "warriors" }

        val engagedTermi = BattleUnit("m1", termiTpl, Faction.MARINES, 2, 6, Facing.EAST, termiTpl.maxTotalWounds, termiTpl.maxTotalWounds, 12f)
        val engagedFoe = BattleUnit("x1", warriorTpl, Faction.XENOS, 3, 6, Facing.WEST, warriorTpl.maxTotalWounds, warriorTpl.maxTotalWounds, 15f)
        val distantFoe = BattleUnit("x2", warriorTpl, Faction.XENOS, 7, 6, Facing.WEST, warriorTpl.maxTotalWounds, warriorTpl.maxTotalWounds, 15f)
        val engagedUnits = listOf(engagedTermi, engagedFoe, distantFoe)

        assertTrue(TacticalRules.isEngagedInMelee(engagedTermi, engagedUnits))
        assertTrue(TacticalRules.computeReachableTiles(engagedTermi, map, engagedUnits).costs.isEmpty())
        assertEquals(null, TacticalRules.computeCmMovePreview(engagedTermi, 1.0f, 6.0f, engagedUnits, map))
        assertEquals(null, TacticalRules.computeForwardCmPreview(engagedTermi, 3.0f, engagedUnits, map))
        assertFalse(TacticalRules.canAdvance(engagedTermi, engagedUnits))
        assertEquals(null, TacticalRules.findChargeEngagementSpot(engagedTermi, distantFoe, engagedUnits, map))
        assertTrue(TacticalRules.computeTeleportTiles(engagedTermi, map, engagedUnits).isEmpty())

        // AI in melee duel must NEVER Move/Advance/Charge away while engaged foe is alive
        val aiDecision = AiController.decideNextAction(Faction.XENOS, engagedUnits, map)
        assertTrue(aiDecision is AiDecision.Attack || aiDecision is AiDecision.TurnComplete)

        // Once the engaged enemy is eliminated (totalWounds = 0), movement & teleport unlock immediately!
        val deadFoe = engagedFoe.copy(totalWounds = 0)
        val freedUnits = listOf(engagedTermi, deadFoe, distantFoe)
        assertFalse(TacticalRules.isEngagedInMelee(engagedTermi, freedUnits))
        assertTrue(TacticalRules.computeReachableTiles(engagedTermi, map, freedUnits).costs.isNotEmpty())
        assertTrue(TacticalRules.canAdvance(engagedTermi, freedUnits))
        assertNotNull(TacticalRules.computeCmMovePreview(engagedTermi, 1.0f, 6.0f, freedUnits, map))
    }

    @Test
    fun sectorMapExpandedBy50PercentTo24x21AndDeploymentZonesCover3BorderColumns() {
        assertEquals(24, MapGenerator.GRID_W)
        assertEquals(21, MapGenerator.GRID_H)
        val map = MapGenerator.generateSectorMap(99L)
        assertEquals(21, map.size)
        assertEquals(24, map[0].size)

        // Team 0 (West) occupies columns 0..2
        assertTrue(TacticalRules.isWithinDeploymentZone(0, 10, 0))
        assertTrue(TacticalRules.isWithinDeploymentZone(2, 10, 0))
        assertFalse(TacticalRules.isWithinDeploymentZone(3, 10, 0))
        assertFalse(TacticalRules.isWithinDeploymentZone(22, 10, 0))
        assertFalse(TacticalRules.isWithinDeploymentZone(-1, 10, 0))

        // Team 1 (East) occupies columns 21..23
        assertTrue(TacticalRules.isWithinDeploymentZone(21, 10, 1))
        assertTrue(TacticalRules.isWithinDeploymentZone(23, 10, 1))
        assertFalse(TacticalRules.isWithinDeploymentZone(20, 10, 1))
        assertFalse(TacticalRules.isWithinDeploymentZone(1, 10, 1))
        assertFalse(TacticalRules.isWithinDeploymentZone(24, 10, 1))
    }

    @Test
    fun twoVsTwoTeamsSupportSameFactionAndCoverDropsFlankRear() {
        assertEquals(listOf(CoverTag.OPEN, CoverTag.COVER), CoverTag.entries.toList())

        val map = MapGenerator.generateSectorMap(1L)
        val marineTpl = DefaultCatalog.defaultTemplates.first { it.id == "intercessors" }

        val p1Marine = BattleUnit(
            uid = "u_p1",
            template = marineTpl,
            faction = Faction.MARINES,
            x = 1,
            y = 8,
            facing = Facing.EAST,
            totalWounds = marineTpl.maxTotalWounds,
            maxTotalWounds = marineTpl.maxTotalWounds,
            moveCmLeft = 15f,
            ownerPlayerId = "p1"
        )
        val p2Marine = p1Marine.copy(uid = "u_p2", x = 2, y = 9, ownerPlayerId = "p2")
        val p3Marine = p1Marine.copy(uid = "u_p3", x = 4, y = 8, facing = Facing.WEST, ownerPlayerId = "p3")
        val aiMarine = p1Marine.copy(uid = "u_ai", x = 6, y = 8, facing = Facing.WEST, ownerPlayerId = "AI")

        assertTrue(p1Marine.isAllyOf(p2Marine))
        assertFalse(p1Marine.isEnemyOf(p2Marine))
        assertTrue(p1Marine.isEnemyOf(p3Marine))
        assertTrue(p1Marine.isEnemyOf(aiMarine))

        val units = listOf(p1Marine, p2Marine, p3Marine, aiMarine)
        val preview = TacticalRules.canTarget(p1Marine, p3Marine, WeaponMode.MAIN, map, units)
        assertNotNull(preview)
        assertEquals(null, TacticalRules.canTarget(p1Marine, p2Marine, WeaponMode.MAIN, map, units))

        // AI controller scoped to aiOwnerId = "AI" only moves/attacks with AI-owned unit against Team 0
        val aiDecision = AiController.decideNextAction(Faction.MARINES, units, map, aiOwnerId = "AI")
        assertNotNull(aiDecision)
        assertTrue(aiDecision !is AiDecision.TurnComplete)

        // Team 1 AI Ally (p2) targets Team 2 (p3/AI) and ignores Team 1 (p1) even in Marine mirror
        val p2AiDecision = AiController.decideNextAction(Faction.MARINES, units, map, aiOwnerId = "p2")
        assertNotNull(p2AiDecision)
        assertTrue(p2AiDecision !is AiDecision.TurnComplete)
        if (p2AiDecision is AiDecision.Attack) {
            assertEquals("u_p2", p2AiDecision.attackerUid)
            assertTrue(p2AiDecision.targetUid in listOf("u_p3", "u_ai"))
        }

        // Team 2 AI (p3) targets Team 1 (p1/p2) and ignores Team 2 (AI)
        val p3AiDecision = AiController.decideNextAction(Faction.MARINES, units, map, aiOwnerId = "p3")
        assertNotNull(p3AiDecision)
        assertTrue(p3AiDecision !is AiDecision.TurnComplete)
        if (p3AiDecision is AiDecision.Attack) {
            assertEquals("u_p3", p3AiDecision.attackerUid)
            assertTrue(p3AiDecision.targetUid in listOf("u_p1", "u_p2"))
        }
    }

    @Test
    fun incursorSkillFiresGunStatsAndMarksTargetForAlliedRangedOnly() {
        val openMap = Array(MapGenerator.GRID_H) { Array(MapGenerator.GRID_W) { TerrainType.OPEN } }
        val incursorTpl = DefaultCatalog.defaultTemplates.first { it.id == "incursors" }
        assertEquals(UnitArchetype.INCURSOR, incursorTpl.archetype)
        assertEquals(1, incursorTpl.skillCooldown)
        assertEquals(1, incursorTpl.meleeSustainedHits)
        assertEquals("Paired Combat Blades", incursorTpl.meleeWeaponName)
        assertTrue(UnitMeshSculptor3D.build(UnitArchetype.INCURSOR).vertexCount > 100)

        val allyTpl = DefaultCatalog.defaultTemplates.first { it.id == "intercessors" }
        val meleeAllyTpl = DefaultCatalog.defaultTemplates.first { it.id == "assault_intercessors" }
        val enemyTpl = DefaultCatalog.defaultTemplates.first { it.id == "carnifex" }

        val incursor = BattleUnit(
            uid = "u_incursor",
            template = incursorTpl,
            faction = Faction.MARINES,
            x = 2,
            y = 10,
            facing = Facing.EAST,
            totalWounds = incursorTpl.maxTotalWounds,
            maxTotalWounds = incursorTpl.maxTotalWounds,
            moveCmLeft = incursorTpl.moveCm.toFloat(),
            ownerPlayerId = "p1"
        )
        val rangedAlly = BattleUnit(
            uid = "u_ally_ranged",
            template = allyTpl,
            faction = Faction.MARINES,
            x = 2,
            y = 12,
            facing = Facing.EAST,
            totalWounds = allyTpl.maxTotalWounds,
            maxTotalWounds = allyTpl.maxTotalWounds,
            moveCmLeft = 0f, // spent move so stationary +1 bonus doesn't obscure mark delta
            ownerPlayerId = "p2"
        )
        val meleeAlly = BattleUnit(
            uid = "u_ally_melee",
            template = meleeAllyTpl,
            faction = Faction.MARINES,
            x = 5,
            y = 10,
            facing = Facing.EAST,
            totalWounds = meleeAllyTpl.maxTotalWounds,
            maxTotalWounds = meleeAllyTpl.maxTotalWounds,
            moveCmLeft = 0f,
            ownerPlayerId = "p2"
        )
        val enemy = BattleUnit(
            uid = "u_enemy",
            template = enemyTpl,
            faction = Faction.XENOS,
            x = 6,
            y = 10,
            facing = Facing.WEST,
            totalWounds = enemyTpl.maxTotalWounds,
            maxTotalWounds = enemyTpl.maxTotalWounds,
            moveCmLeft = enemyTpl.moveCm.toFloat(),
            ownerPlayerId = "p3"
        )

        val units = listOf(incursor, rangedAlly, meleeAlly, enemy)
        val skillPreview = TacticalRules.canTarget(incursor, enemy, WeaponMode.SKILL, openMap, units)
        val mainPreview = TacticalRules.canTarget(incursor, enemy, WeaponMode.MAIN, openMap, units)
        assertNotNull(skillPreview)
        assertNotNull(mainPreview)
        // Skill uses full main gun stats
        assertEquals(mainPreview!!.attacksCount, skillPreview!!.attacksCount)
        assertEquals(mainPreview.weaponStrength, skillPreview.weaponStrength)
        assertEquals(mainPreview.weaponAp, skillPreview.weaponAp)
        assertEquals(mainPreview.hitRollNeeded, skillPreview.hitRollNeeded)

        val beforeMarkRanged = TacticalRules.canTarget(rangedAlly, enemy, WeaponMode.MAIN, openMap, units)!!
        val beforeMarkMelee = TacticalRules.canTarget(meleeAlly, enemy, WeaponMode.MAIN, openMap, units)!!
        assertFalse(beforeMarkRanged.markedBonusApplied)
        assertEquals(3, beforeMarkRanged.hitRollNeeded)
        assertEquals(3, beforeMarkMelee.hitRollNeeded)

        val outcome = TacticalRules.resolveAttack(incursor, enemy, units, WeaponMode.SKILL, openMap, Random(7))
        assertEquals(1, outcome.updatedAttacker.skillCooldownLeft)
        val markedEnemy = outcome.updatedTargets.first()
        assertTrue(markedEnemy.isAlive)
        assertEquals(incursor.effectiveTeam, markedEnemy.markedByTeam)
        assertTrue(outcome.floatingTexts.any { it.second.first.contains("MARKED") })

        val unitsAfterMark = listOf(outcome.updatedAttacker, rangedAlly, meleeAlly, markedEnemy)
        val afterMarkRanged = TacticalRules.canTarget(rangedAlly, markedEnemy, WeaponMode.MAIN, openMap, unitsAfterMark)!!
        val afterMarkPistol = TacticalRules.canTarget(rangedAlly, markedEnemy, WeaponMode.PISTOL, openMap, unitsAfterMark)!!
        val afterMarkMelee = TacticalRules.canTarget(meleeAlly, markedEnemy, WeaponMode.MAIN, openMap, unitsAfterMark)!!

        // Allied gun attacks get +1 to Hit roll (3+ -> 2+), allied melee does NOT
        assertTrue(afterMarkRanged.markedBonusApplied)
        assertEquals(2, afterMarkRanged.hitRollNeeded)
        assertTrue(afterMarkPistol.markedBonusApplied)
        assertEquals(2, afterMarkPistol.hitRollNeeded)
        assertFalse(afterMarkMelee.markedBonusApplied)
        assertEquals(3, afterMarkMelee.hitRollNeeded)
    }

    @Test
    fun sustainedHitsGeneratesExtraHitsOnUnmodifiedSixAndSupportsCustomX() {
        val openMap = Array(MapGenerator.GRID_H) { Array(MapGenerator.GRID_W) { TerrainType.OPEN } }
        val baseIncursor = DefaultCatalog.defaultTemplates.first { it.id == "incursors" }
        val customTemplate = baseIncursor.copy(meleeSustainedHits = 3)
        val enemyTpl = DefaultCatalog.defaultTemplates.first { it.id == "carnifex" }

        val attacker = BattleUnit(
            uid = "u_inc_melee",
            template = customTemplate,
            faction = Faction.MARINES,
            x = 5,
            y = 10,
            facing = Facing.EAST,
            totalWounds = customTemplate.maxTotalWounds,
            maxTotalWounds = customTemplate.maxTotalWounds,
            moveCmLeft = 0f
        )
        val target = BattleUnit(
            uid = "u_target",
            template = enemyTpl,
            faction = Faction.XENOS,
            x = 6,
            y = 10,
            facing = Facing.WEST,
            totalWounds = 50,
            maxTotalWounds = 50,
            moveCmLeft = 0f
        )
        val units = listOf(attacker, target)
        val preview = TacticalRules.canTarget(attacker, target, WeaponMode.MAIN, openMap, units)!!
        assertTrue(preview.isMelee)
        assertEquals(3, preview.sustainedHits)

        // With 15 melee attacks (5 models x 3A), deterministic RNG should roll 6s -> hits > attacksCount
        val outcome = TacticalRules.resolveAttack(attacker, target, units, WeaponMode.MAIN, openMap, Random(1234))
        assertTrue(outcome.hits > preview.attacksCount)
        assertTrue(outcome.floatingTexts.any { it.second.first.startsWith("SUSTAINED +") })
    }

    @Test
    fun weaponProfilesAreCompletelyIndependentPerWeapon() {
        val openMap = Array(MapGenerator.GRID_H) { Array(MapGenerator.GRID_W) { TerrainType.OPEN } }
        val baseWarrior = DefaultCatalog.defaultTemplates.first { it.id == "warriors" }
        val editedMelee = baseWarrior.warriorMeleeProfile!!.copy(attacksPerModel = 6, strength = 7, sustainedHits = 2)
        val editedRanged = baseWarrior.warriorRangedProfile!!.copy(attacksPerModel = 4, strength = 6, range = 6)

        val warriorSaved = baseWarrior.copy(
            weaponChoice = WarriorWeaponChoice.FLESHBORER,
            range = editedRanged.range,
            attacksPerModel = editedRanged.attacksPerModel,
            ballisticSkill = editedRanged.skill,
            weaponSkill = editedMelee.skill,
            strength = editedRanged.strength,
            armorPen = editedRanged.armorPen,
            damage = editedRanged.damage,
            mainWeaponName = editedRanged.name,
            mainSustainedHits = editedRanged.sustainedHits,
            meleeWeaponName = editedMelee.name,
            meleeAttacks = editedMelee.attacksPerModel,
            meleeWeaponSkill = editedMelee.skill,
            meleeStrength = editedMelee.strength,
            meleeArmorPen = editedMelee.armorPen,
            meleeDamage = editedMelee.damage,
            meleeSustainedHits = editedMelee.sustainedHits,
            warriorMeleeProfile = editedMelee,
            warriorRangedProfile = editedRanged
        )

        // Ranged Fleshborer active while Boneswords melee stats remain completely independent
        assertEquals(6, warriorSaved.warriorMeleeProfile?.attacksPerModel)
        assertEquals(7, warriorSaved.warriorMeleeProfile?.strength)
        assertEquals(2, warriorSaved.warriorMeleeProfile?.sustainedHits)
        assertEquals(4, warriorSaved.warriorRangedProfile?.attacksPerModel)
        assertEquals(6, warriorSaved.warriorRangedProfile?.strength)
        assertEquals(6, warriorSaved.warriorRangedProfile?.range)
        assertEquals(0, warriorSaved.warriorRangedProfile?.sustainedHits)

        val warriorUnit = BattleUnit(
            uid = "u_war",
            template = warriorSaved,
            faction = Faction.XENOS,
            x = 5,
            y = 10,
            facing = Facing.EAST,
            totalWounds = warriorSaved.maxTotalWounds,
            maxTotalWounds = warriorSaved.maxTotalWounds,
            moveCmLeft = 0f
        )
        val marineTarget = BattleUnit(
            uid = "u_mar_near",
            template = DefaultCatalog.defaultTemplates.first { it.id == "intercessors" },
            faction = Faction.MARINES,
            x = 6,
            y = 10,
            facing = Facing.WEST,
            totalWounds = 10,
            maxTotalWounds = 10,
            moveCmLeft = 0f
        )
        val marineFar = marineTarget.copy(uid = "u_mar_far", x = 10, y = 10, worldX = 10f, worldY = 10f)
        val warriorMeleePreview = TacticalRules.canTarget(warriorUnit, marineTarget, WeaponMode.MAIN, openMap, listOf(warriorUnit, marineTarget))!!
        val warriorRangedPreview = TacticalRules.canTarget(warriorUnit, marineFar, WeaponMode.MAIN, openMap, listOf(warriorUnit, marineFar))!!
        assertTrue(warriorMeleePreview.isMelee)
        assertEquals(18, warriorMeleePreview.attacksCount) // 3 models * 6A Boneswords
        assertEquals(7, warriorMeleePreview.weaponStrength)
        assertEquals(2, warriorMeleePreview.sustainedHits)
        assertFalse(warriorRangedPreview.isMelee)
        assertEquals(12, warriorRangedPreview.attacksCount) // 3 models * 4A Fleshborer
        assertEquals(6, warriorRangedPreview.weaponStrength)
        assertEquals(0, warriorRangedPreview.sustainedHits)

        // Ranged-main unit (Incursor) uses independent melee stats in close combat vs gun stats at range
        val customIncursorTpl = DefaultCatalog.defaultTemplates.first { it.id == "incursors" }.copy(
            attacksPerModel = 2,
            strength = 4,
            armorPen = 0,
            mainSustainedHits = 0,
            meleeAttacks = 5,
            meleeWeaponSkill = 2,
            meleeStrength = 6,
            meleeArmorPen = 2,
            meleeDamage = 2,
            meleeSustainedHits = 2
        )
        val incursor = BattleUnit(
            uid = "u_inc_indep",
            template = customIncursorTpl,
            faction = Faction.MARINES,
            x = 5,
            y = 10,
            facing = Facing.EAST,
            totalWounds = 10,
            maxTotalWounds = 10,
            moveCmLeft = 0f
        )
        val nearEnemy = BattleUnit(
            uid = "u_near",
            template = baseWarrior,
            faction = Faction.XENOS,
            x = 6,
            y = 10,
            facing = Facing.WEST,
            totalWounds = 10,
            maxTotalWounds = 10,
            moveCmLeft = 0f
        )
        val farEnemy = nearEnemy.copy(uid = "u_far", x = 10, y = 10, worldX = 10f, worldY = 10f)
        val meleePreview = TacticalRules.canTarget(incursor, nearEnemy, WeaponMode.MAIN, openMap, listOf(incursor, nearEnemy))!!
        val rangedPreview = TacticalRules.canTarget(incursor, farEnemy, WeaponMode.MAIN, openMap, listOf(incursor, farEnemy))!!
        assertTrue(meleePreview.isMelee)
        assertFalse(rangedPreview.isMelee)
        assertEquals(25, meleePreview.attacksCount)
        assertEquals(6, meleePreview.weaponStrength)
        assertEquals(2, meleePreview.weaponAp)
        assertEquals(2, meleePreview.sustainedHits)
        assertEquals(2, meleePreview.hitRollNeeded)

        assertEquals(10, rangedPreview.attacksCount)
        assertEquals(4, rangedPreview.weaponStrength)
        assertEquals(0, rangedPreview.weaponAp)
        assertEquals(0, rangedPreview.sustainedHits)
    }

    @Test
    fun assaultIntercessorSkillRequiresChargeAndGrantsPlusOneStrengthAndAp() {
        val openMap = Array(MapGenerator.GRID_H) { Array(MapGenerator.GRID_W) { TerrainType.OPEN } }
        val assaultTpl = DefaultCatalog.defaultTemplates.first { it.id == "assault_intercessors" }
        val enemyTpl = DefaultCatalog.defaultTemplates.first { it.id == "carnifex" }

        val enemy = BattleUnit(
            uid = "u_car_target",
            template = enemyTpl,
            faction = Faction.XENOS,
            x = 6,
            y = 10,
            facing = Facing.WEST,
            totalWounds = enemyTpl.maxTotalWounds,
            maxTotalWounds = enemyTpl.maxTotalWounds,
            moveCmLeft = 0f
        )

        // 1. Not engaged & not charged -> Skill blocked
        val unchargedFar = BattleUnit(
            uid = "u_ai_1",
            template = assaultTpl,
            faction = Faction.MARINES,
            x = 3,
            y = 10,
            facing = Facing.EAST,
            totalWounds = assaultTpl.maxTotalWounds,
            maxTotalWounds = assaultTpl.maxTotalWounds,
            moveCmLeft = 18f,
            hasCharged = false
        )
        assertFalse(TacticalRules.canUseMode(unchargedFar, WeaponMode.SKILL, listOf(unchargedFar, enemy)))
        assertNull(TacticalRules.activateSelfSkillIfApplicable(unchargedFar, listOf(unchargedFar, enemy)))

        // 2. In melee range WITHOUT charging (e.g., walked or still engaged at start of new turn with hasCharged = false) -> Skill blocked!
        val engagedNoCharge = unchargedFar.copy(x = 5, worldX = 5f, hasCharged = false, skillCooldownLeft = 0)
        assertFalse(TacticalRules.canUseMode(engagedNoCharge, WeaponMode.SKILL, listOf(engagedNoCharge, enemy)))
        assertNull(TacticalRules.activateSelfSkillIfApplicable(engagedNoCharge, listOf(engagedNoCharge, enemy)))

        // 3. Charged but failed to reach melee (hasCharged = true, not engaged) -> Skill blocked!
        val failedChargeNotEngaged = unchargedFar.copy(x = 3, worldX = 3f, hasCharged = true)
        assertFalse(TacticalRules.canUseMode(failedChargeNotEngaged, WeaponMode.SKILL, listOf(failedChargeNotEngaged, enemy)))
        assertNull(TacticalRules.activateSelfSkillIfApplicable(failedChargeNotEngaged, listOf(failedChargeNotEngaged, enemy)))

        // 4. Charged and reached melee target -> Skill ready & activates +1S +1AP melee buff within turn
        val chargedInMelee = engagedNoCharge.copy(hasCharged = true)
        assertTrue(TacticalRules.canUseMode(chargedInMelee, WeaponMode.SKILL, listOf(chargedInMelee, enemy)))

        val basePreview = TacticalRules.canTarget(chargedInMelee, enemy, WeaponMode.MAIN, openMap, listOf(chargedInMelee, enemy))!!
        assertEquals(assaultTpl.strength, basePreview.weaponStrength)
        assertEquals(assaultTpl.armorPen, basePreview.weaponAp)

        val buffed = TacticalRules.activateSelfSkillIfApplicable(chargedInMelee, listOf(chargedInMelee, enemy))
        assertNotNull(buffed)
        assertTrue(buffed!!.meleeChargeBuffActive)
        assertEquals(assaultTpl.skillCooldown, buffed.skillCooldownLeft)
        // Cannot activate twice in the same turn
        assertFalse(TacticalRules.canUseMode(buffed, WeaponMode.SKILL, listOf(buffed, enemy)))

        val buffedPreview = TacticalRules.canTarget(buffed, enemy, WeaponMode.MAIN, openMap, listOf(buffed, enemy))!!
        assertTrue(buffedPreview.isMelee)
        assertEquals(assaultTpl.strength + 1, buffedPreview.weaponStrength)
        assertEquals(assaultTpl.armorPen + 1, buffedPreview.weaponAp)
        assertEquals(assaultTpl.effectiveMeleeWeaponSkill, buffedPreview.hitRollNeeded)
        assertEquals(assaultTpl.strength + 1, buffed.effectiveMainStrength)
        assertEquals(assaultTpl.armorPen + 1, buffed.effectiveMainArmorPen)
        // Pistol stats are unaffected by melee Charge buff
        assertEquals(assaultTpl.pistolStrength, TacticalRules.weaponStrength(buffed, WeaponMode.PISTOL))
        assertEquals(assaultTpl.pistolArmorPen, TacticalRules.weaponAp(buffed, WeaponMode.PISTOL))
    }

    @Test
    fun allTwelveUnitsMatchWarhammer40k11thEditionStatsPointsAndMigrateOldConfigs() {
        val byId = DefaultCatalog.defaultTemplates.associateBy { it.id }
        assertEquals(18, byId.size)

        // --- Ultramarines (11e) ---
        val titus = byId.getValue("captain_titus")
        assertTrue(titus.isHero)
        assertEquals(105, titus.points)
        assertEquals(1, titus.squadSize)
        assertEquals(5, titus.toughness)
        assertEquals(2, titus.save)
        assertEquals(4, titus.invulnSave)
        assertEquals(5, titus.woundsPerModel)
        assertEquals(0, titus.skillCooldown)
        assertTrue(titus.mainWeaponAssault)
        assertTrue(titus.mainWeaponHeavy)
        assertEquals(1, titus.mainRapidFire)
        assertEquals(8, titus.meleeAttacks)
        assertEquals(2, titus.meleeWeaponSkill)
        assertEquals(0, titus.meleeSustainedHits)
        assertEquals(2, titus.meleeAntiNonMonsterVehicle)
        assertEquals(
            setOf("assault_intercessors", "intercessors", "hellblasters", "bladeguard_veterans"),
            titus.leadableIds.toSet()
        )

        val intercessors = byId.getValue("intercessors")
        assertEquals(85, intercessors.points)
        assertEquals(5, intercessors.squadSize)
        assertEquals(15, intercessors.moveCm)
        assertEquals(4, intercessors.toughness)
        assertEquals(3, intercessors.save)
        assertEquals(2, intercessors.woundsPerModel)
        assertEquals(6, intercessors.leadership)
        assertEquals(2, intercessors.objectiveControl)
        assertEquals(5, intercessors.strength) // Bolt Rifle S5 AP-1 D1 Assault
        assertEquals(1, intercessors.armorPen)
        assertTrue(intercessors.mainWeaponAssault)
        assertEquals(5, intercessors.meleeStrength) // Knives and Fists S5

        val incursors = byId.getValue("incursors")
        assertEquals(90, incursors.points)
        assertEquals(20, incursors.moveCm) // 8"
        assertEquals(6, incursors.leadership)
        assertEquals(1, incursors.objectiveControl)
        assertEquals(5, incursors.strength) // Occulus Bolt Carbine S5 AP-1
        assertEquals(1, incursors.armorPen)
        assertEquals(5, incursors.meleeStrength) // Paired Combat Blades S5 AP-1 Sustained 1
        assertEquals(1, incursors.meleeArmorPen)
        assertEquals(1, incursors.meleeSustainedHits)
        assertEquals(5, incursors.pistolStrength)
        assertEquals(1, incursors.pistolArmorPen)

        val hellblasters = byId.getValue("hellblasters")
        assertEquals(115, hellblasters.points)
        assertEquals(5, hellblasters.toughness) // T5
        assertEquals(6, hellblasters.leadership)
        assertEquals(1, hellblasters.objectiveControl)
        assertEquals(7, hellblasters.strength)
        assertEquals(2, hellblasters.armorPen)
        assertEquals(2, hellblasters.damage) // D2 standard & overcharge
        assertTrue(hellblasters.mainWeaponAssault)
        assertEquals(3, hellblasters.meleeWeaponSkill)
        assertEquals(5, hellblasters.meleeStrength)

        val assaultMarines = byId.getValue("assault_intercessors")
        assertEquals(85, assaultMarines.points)
        assertEquals(15, assaultMarines.moveCm) // 6"
        assertEquals(4, assaultMarines.attacksPerModel)
        assertEquals(4, assaultMarines.strength)
        assertEquals(1, assaultMarines.armorPen)
        assertEquals(4, assaultMarines.pistolRange) // Heavy Bolt Pistol 18"
        assertEquals(2, assaultMarines.objectiveControl)

        val bladeguard = byId.getValue("bladeguard_veterans")
        assertEquals(90, bladeguard.points)
        assertEquals(3, bladeguard.squadSize)
        assertEquals(3, bladeguard.woundsPerModel)
        assertEquals(4, bladeguard.toughness)
        assertEquals(3, bladeguard.save)
        assertEquals(4, bladeguard.invulnSave)
        assertEquals(15, bladeguard.moveCm) // 6"
        assertEquals(4, bladeguard.attacksPerModel)
        assertEquals(3, bladeguard.weaponSkill)
        assertEquals(5, bladeguard.strength)
        assertEquals(2, bladeguard.armorPen)
        assertEquals(2, bladeguard.damage)
        assertEquals(1, bladeguard.skillCooldown)
        assertEquals(4, bladeguard.pistolRange) // Heavy Bolt Pistol 18"

        val termis = byId.getValue("terminators")
        assertEquals(180, termis.points)
        assertEquals(12, termis.moveCm) // 5"
        assertEquals(5, termis.toughness)
        assertEquals(2, termis.save)
        assertEquals(4, termis.invulnSave)
        assertEquals(4, termis.woundsPerModel)
        assertEquals(3, termis.damage) // Thunder Hammer D3
        assertEquals(6, termis.terminatorClawsProfile?.attacksPerModel)

        val dread = byId.getValue("dreadnought")
        assertEquals(180, dread.points) // 170 + 10 Macro Plasma
        assertEquals(20, dread.moveCm) // 8"
        assertEquals(10, dread.toughness)
        assertEquals(2, dread.save)
        assertEquals(12, dread.woundsPerModel)
        assertEquals(4, dread.objectiveControl)
        assertEquals(2, dread.ballisticSkill) // BS 2+
        assertEquals(4, dread.attacksPerModel) // D6+1 avg
        assertEquals(5, dread.meleeDamage) // Redemptor Fist D3+3 avg
        assertEquals(3, dread.meleeArmorPen)

        // --- Tyranids (11e) ---
        val termagants = byId.getValue("termagants")
        assertEquals(55, termagants.points)
        assertEquals(10, termagants.squadSize)
        assertEquals(15, termagants.moveCm) // 6"
        assertEquals(8, termagants.leadership)
        assertEquals(2, termagants.objectiveControl)
        assertEquals(1, termagants.attacksPerModel) // Fleshborer 1A S5 AP0
        assertEquals(5, termagants.strength)

        val hormagaunts = byId.getValue("hormagaunts")
        assertEquals(70, hormagaunts.points)
        assertEquals(10, hormagaunts.squadSize)
        assertEquals(25, hormagaunts.moveCm) // 10"
        assertEquals(8, hormagaunts.leadership)
        assertEquals(2, hormagaunts.objectiveControl)
        assertEquals(3, hormagaunts.attacksPerModel)

        val warriors = byId.getValue("warriors")
        assertEquals(75, warriors.points)
        assertEquals(3, warriors.squadSize)
        assertEquals(6, warriors.attacksPerModel) // 6A S5 AP-2 D1
        assertEquals(1, warriors.damage)
        assertEquals(7, warriors.leadership)
        assertEquals(2, warriors.objectiveControl)
        assertEquals("Deathspitter", warriors.warriorRangedProfile?.name)
        assertEquals(5, warriors.warriorRangedProfile?.range)
        assertEquals(3, warriors.warriorRangedProfile?.attacksPerModel)
        assertEquals(1, warriors.warriorRangedProfile?.armorPen)
        assertEquals(false, warriors.warriorRangedProfile?.assault)

        val carnifex = byId.getValue("carnifex")
        assertEquals(90, carnifex.points)
        assertEquals(20, carnifex.moveCm) // 8"
        assertEquals(8, carnifex.woundsPerModel) // 8W
        assertEquals(9, carnifex.toughness)
        assertEquals(2, carnifex.save)
        assertEquals(3, carnifex.objectiveControl)
        assertEquals(4, carnifex.weaponSkill) // Crushing Claws WS 4+ 4A D4
        assertEquals(4, carnifex.attacksPerModel)
        assertEquals(4, carnifex.damage)
        assertEquals(5, carnifex.pistolAttacks) // Spine Banks 5A S5 AP0 R2
        assertEquals(5, carnifex.pistolStrength)
        assertEquals(0, carnifex.pistolArmorPen)
        assertEquals(2, carnifex.pistolRange)

        val genestealers = byId.getValue("genestealers")
        assertEquals(75, genestealers.points)
        assertEquals(5, genestealers.squadSize)
        assertEquals(20, genestealers.moveCm) // 8"
        assertEquals(5, genestealers.invulnSave)
        assertEquals(2, genestealers.weaponSkill)

        val leapers = byId.getValue("von_ryans_leaper")
        assertEquals(55, leapers.points)
        assertEquals(3, leapers.squadSize)
        assertEquals(25, leapers.moveCm) // 10"
        assertEquals(6, leapers.invulnSave) // 6++
        assertEquals(6, leapers.attacksPerModel) // 6A S5 AP-1 D1
        assertEquals(1, leapers.armorPen)
        assertEquals(1, leapers.damage)

        // --- Black Legion ---
        val legionaries = byId.getValue("legionaries")
        assertEquals(Faction.BLACK_LEGION, legionaries.faction)
        assertEquals(UnitArchetype.LEGIONARY, legionaries.archetype)
        assertEquals("Veterans of the Long War", legionaries.skillName)
        assertEquals(1, legionaries.skillCooldown)
        assertEquals("Dark Pacts", legionaries.passiveName)
        assertEquals(5, legionaries.squadSize)
        assertEquals(3, legionaries.save)
    }

    @Test
    fun blackLegionLegionariesAutoActivateRerollWoundOnesAndDarkPactsWork() {
        val openMap = Array(MapGenerator.GRID_H) { Array(MapGenerator.GRID_W) { TerrainType.OPEN } }
        val legTpl = DefaultCatalog.defaultTemplates.first { it.id == "legionaries" }
        val marineTpl = DefaultCatalog.defaultTemplates.first { it.id == "intercessors" }

        val legionaries = BattleUnit(
            uid = "bl_1",
            template = legTpl,
            faction = Faction.BLACK_LEGION,
            x = 2,
            y = 10,
            facing = Facing.EAST,
            totalWounds = legTpl.maxTotalWounds,
            maxTotalWounds = legTpl.maxTotalWounds,
            moveCmLeft = legTpl.moveCm.toFloat(),
            ownerPlayerId = "player"
        )
        val rangedTarget = BattleUnit(
            uid = "m_far",
            template = marineTpl,
            faction = Faction.MARINES,
            x = 6,
            y = 10,
            facing = Facing.WEST,
            totalWounds = 30,
            maxTotalWounds = 30,
            moveCmLeft = 0f,
            ownerPlayerId = "AI"
        )

        // 1. Previews show Reroll Wound 1s ready + Dark Pact options
        val basePreview = TacticalRules.canTarget(legionaries, rangedTarget, WeaponMode.MAIN, openMap, listOf(legionaries, rangedTarget))!!
        assertTrue(basePreview.rerollWoundOnes)
        assertFalse(basePreview.lethalHits)
        assertEquals(0, basePreview.sustainedHits)

        val lethalPreview = TacticalRules.canTarget(
            legionaries, rangedTarget, WeaponMode.MAIN, openMap, listOf(legionaries, rangedTarget),
            darkPactChoice = com.example.model.DarkPactChoice.LETHAL_HITS
        )!!
        assertTrue(lethalPreview.lethalHits)
        assertEquals(com.example.model.DarkPactChoice.LETHAL_HITS, lethalPreview.darkPactChoice)

        val sustainedPreview = TacticalRules.canTarget(
            legionaries, rangedTarget, WeaponMode.MAIN, openMap, listOf(legionaries, rangedTarget),
            darkPactChoice = com.example.model.DarkPactChoice.SUSTAINED_HITS_1
        )!!
        assertEquals(1, sustainedPreview.sustainedHits)

        // 2. Deterministic RNG: Fail 2D6 Ld test (2 + 2 = 4 < 6), suffer D3=2 mortal wounds (4 models remain -> 8 boltgun attacks),
        //    then roll Hit=6 (Lethal Hit -> auto-wound without Wound roll, Save=1 fail),
        //    and remaining Hit=3, Wound=1 -> reroll Wound=6!
        val deterministicRolls = intArrayOf(
            2, 2, // 2D6 Ld = 4 (fail 6+)
            2,    // D3 mortal wounds = 2 (Legionaries 10W -> 8W = 4 alive models = 8 attacks)
            6, 1, // Atk 1: Hit=6 (Lethal auto-wound, NO wound roll), Save=1 (fail -> -1W)
            3, 1, 6, 6 // Atk 2: Hit=3, Wound=1 (re-rolled!) -> 6 (pass!), Save=6 (saved)
        )
        var rollIdx = 0
        val testRng = object : Random() {
            override fun nextBits(bitCount: Int): Int = 0
            override fun nextInt(from: Int, until: Int): Int {
                // Remaining attacks miss cleanly on Hit=2
                val v = if (rollIdx < deterministicRolls.size) deterministicRolls[rollIdx] else 2
                rollIdx++
                return v.coerceIn(from, until - 1)
            }
        }

        val outcome = TacticalRules.resolveAttack(
            attacker = legionaries,
            primaryTarget = rangedTarget,
            allUnits = listOf(legionaries, rangedTarget),
            mode = WeaponMode.MAIN,
            map = openMap,
            rng = testRng,
            darkPactChoice = com.example.model.DarkPactChoice.LETHAL_HITS
        )

        // Auto-activated Veterans of the Long War (Reroll Wound 1s) with 1 turn cooldown & took 2 MW from failed Dark Pact
        assertTrue(outcome.updatedAttacker.legionaryWoundRerollBuffActive)
        assertEquals(1, outcome.updatedAttacker.skillCooldownLeft)
        assertEquals(8, outcome.updatedAttacker.totalWounds)
        assertEquals(4, outcome.updatedAttacker.aliveModels)

        val log = outcome.logEntries.first()
        val pactStep = log.steps.first { it.phaseName == "DARK PACT" }
        assertEquals(0, pactStep.passed)
        assertEquals(1, pactStep.failed)
        assertEquals(listOf(2, 2), pactStep.diceValues)

        val hitStep = log.steps.first { it.phaseName == "HIT" }
        assertEquals(8, hitStep.rolled) // 4 surviving models x 2 attacks each!
        assertTrue(hitStep.note.contains("LETHAL HITS"))

        val woundStep = log.steps.first { it.phaseName == "WOUND" }
        assertEquals(1, woundStep.rolled) // Atk 1 auto-wounded via Lethal Hits; only Atk 2 rolled wound!
        assertTrue(woundStep.note.contains("RE-ROLL 1s"))

        // 3. Sustained Hits 1 with passed 2D6 Leadership test (4 + 3 = 7 >= 6) -> 0 self-damage, Hit 6 adds +1 hit
        val susRolls = intArrayOf(
            4, 3, // 2D6 Ld = 7 (pass 6+)
            6, 4, 1, 4, 1 // Atk 1: Hit=6 (+1 Sustained -> 2 hits): Wound 4/Save 1 (-1W), Wound 4/Save 1 (-1W)
        )
        var susIdx = 0
        val susRng = object : Random() {
            override fun nextBits(bitCount: Int): Int = 0
            override fun nextInt(from: Int, until: Int): Int {
                val v = if (susIdx < susRolls.size) susRolls[susIdx] else 2
                susIdx++
                return v.coerceIn(from, until - 1)
            }
        }
        val susOutcome = TacticalRules.resolveAttack(
            attacker = legionaries,
            primaryTarget = rangedTarget,
            allUnits = listOf(legionaries, rangedTarget),
            mode = WeaponMode.MAIN,
            map = openMap,
            rng = susRng,
            darkPactChoice = com.example.model.DarkPactChoice.SUSTAINED_HITS_1
        )
        assertEquals(10, susOutcome.updatedAttacker.totalWounds)
        assertEquals(2, susOutcome.totalDamageDealt)
        assertEquals(2, susOutcome.hits)

        // 4. Veterans buff persists for melee counter-attacks and re-rolls Wound rolls of 1
        val adjMarine = rangedTarget.copy(x = 3, worldX = 3f)
        val counterRolls = intArrayOf(
            3, 1, 6, 1 // Atk 1: Hit=3, Wound=1 (re-rolled!) -> 6 (pass!), Save=1 (fail -> -1W)
        )
        var cIdx = 0
        val counterRng = object : Random() {
            override fun nextBits(bitCount: Int): Int = 0
            override fun nextInt(from: Int, until: Int): Int {
                val v = if (cIdx < counterRolls.size) counterRolls[cIdx] else 2
                cIdx++
                return v.coerceIn(from, until - 1)
            }
        }
        val counter = TacticalRules.resolveMeleeCounterAttack(
            counterAttacker = outcome.updatedAttacker,
            target = adjMarine,
            map = openMap,
            rng = counterRng
        )!!
        assertEquals(1, counter.totalDamageDealt)
        val cWoundStep = counter.logEntry!!.steps.first { it.phaseName == "WOUND" }
        assertTrue(cWoundStep.note.contains("RE-ROLL 1s"))
    }

    @Test
    fun diceLogCapturesStepByStepRollsPassesAndFailures() {
        val openMap = Array(MapGenerator.GRID_H) { Array(MapGenerator.GRID_W) { TerrainType.OPEN } }
        val intercessorTpl = DefaultCatalog.defaultTemplates.first { it.id == "intercessors" }
        val warriorTpl = DefaultCatalog.defaultTemplates.first { it.id == "warriors" }

        val shooter = BattleUnit(
            uid = "m_log",
            template = intercessorTpl,
            faction = Faction.MARINES,
            x = 2,
            y = 10,
            facing = Facing.EAST,
            totalWounds = intercessorTpl.maxTotalWounds,
            maxTotalWounds = intercessorTpl.maxTotalWounds,
            moveCmLeft = intercessorTpl.moveCm.toFloat()
        )
        val warrior = BattleUnit(
            uid = "w_log",
            template = warriorTpl,
            faction = Faction.XENOS,
            x = 5,
            y = 10,
            facing = Facing.WEST,
            totalWounds = 30,
            maxTotalWounds = 30,
            moveCmLeft = warriorTpl.moveCm.toFloat()
        )

        val rangedOutcome = TacticalRules.resolveAttack(
            attacker = shooter,
            primaryTarget = warrior,
            allUnits = listOf(shooter, warrior),
            mode = WeaponMode.MAIN,
            map = openMap,
            rng = Random(99)
        )
        assertEquals(1, rangedOutcome.logEntries.size)
        val log = rangedOutcome.logEntries.first()
        assertTrue(log.steps.isNotEmpty())
        val hitStep = log.steps.first { it.phaseName == "HIT" }
        assertTrue(hitStep.stepLabel.startsWith("BS "))
        assertEquals(10, hitStep.rolled)
        assertEquals(hitStep.rolled, hitStep.passed + hitStep.failed)
        assertEquals(hitStep.rolled, hitStep.diceValues.size)

        val woundStep = log.steps.firstOrNull { it.phaseName == "WOUND" }
        assertNotNull(woundStep)
        assertEquals(hitStep.passed, woundStep!!.rolled)
        assertEquals(woundStep.rolled, woundStep.passed + woundStep.failed)
        assertTrue(woundStep.stepLabel.contains("S5 vs T5"))

        val saveStep = log.steps.firstOrNull { it.phaseName == "SAVE" }
        if (woundStep.passed > 0) {
            assertNotNull(saveStep)
            assertEquals(woundStep.passed, saveStep!!.rolled)
            assertEquals(saveStep.rolled, saveStep.passed + saveStep.failed)
        }

        // Melee counter-attack also records step-by-step WS -> S vs T -> Save log
        val adjMarine = shooter.copy(x = 4, worldX = 4f)
        val counter = TacticalRules.resolveMeleeCounterAttack(warrior, adjMarine, openMap, Random(7))
        assertNotNull(counter)
        val counterLog = counter!!.logEntry
        assertNotNull(counterLog)
        assertTrue(counterLog!!.isCounterOrFightFirst)
        assertTrue(counterLog.steps.first().stepLabel.startsWith("WS "))
        counterLog.steps.forEach { step ->
            assertEquals(step.rolled, step.passed + step.failed)
        }
    }

    @Test
    fun bladeguardVeteransStormShieldInvulnAndTurnScopedRerollOnesWorkInAttackAndCounter() {
        val openMap = Array(MapGenerator.GRID_H) { Array(MapGenerator.GRID_W) { TerrainType.OPEN } }
        val bgTpl = DefaultCatalog.defaultTemplates.first { it.id == "bladeguard_veterans" }
        val carniTpl = DefaultCatalog.defaultTemplates.first { it.id == "carnifex" }

        // 1. 3D Mesh builds cleanly with rich geometry
        val mesh = UnitMeshSculptor3D.build(UnitArchetype.BLADEGUARD)
        assertTrue(mesh.vertexCount > 300)
        assertEquals(0, mesh.data.size % 10)
        assertTrue(mesh.data.all { it.isFinite() })

        val bladeguard = BattleUnit(
            uid = "u_bg",
            template = bgTpl,
            faction = Faction.MARINES,
            x = 5,
            y = 10,
            facing = Facing.EAST,
            totalWounds = bgTpl.maxTotalWounds,
            maxTotalWounds = bgTpl.maxTotalWounds,
            moveCmLeft = bgTpl.moveCm.toFloat()
        )
        val carnifex = BattleUnit(
            uid = "u_carni_bg",
            template = carniTpl,
            faction = Faction.XENOS,
            x = 6,
            y = 10,
            facing = Facing.WEST,
            totalWounds = 30,
            maxTotalWounds = 30,
            moveCmLeft = 0f
        )

        // 2. Passive 4++ Invulnerable Save kicks in against AP-3 Crushing Claws (Sv 3+ + AP3 = 6+, so 4++ is used)
        val carniVsBg = TacticalRules.canTarget(carnifex, bladeguard, WeaponMode.MAIN, openMap, listOf(carnifex, bladeguard))!!
        assertEquals(4, carniVsBg.saveRollNeeded)
        assertTrue(carniVsBg.usingInvuln)

        // 3. Activate Bladeguard Skill -> cooldown 1, buff active, cannot double activate same turn
        assertTrue(TacticalRules.canUseMode(bladeguard, WeaponMode.SKILL, listOf(bladeguard, carnifex)))
        val buffedBg = TacticalRules.activateSelfSkillIfApplicable(bladeguard, listOf(bladeguard, carnifex))!!
        assertTrue(buffedBg.bladeguardRerollBuffActive)
        assertEquals(1, buffedBg.skillCooldownLeft)
        assertFalse(TacticalRules.canUseMode(buffedBg, WeaponMode.SKILL, listOf(buffedBg, carnifex)))

        // 4. Deterministic seed check: Hit rolls of 1 are re-rolled on Bladeguard attack
        var foundHitReroll = false
        for (seed in 1..200) {
            val out = TacticalRules.resolveAttack(buffedBg, carnifex, listOf(buffedBg, carnifex), WeaponMode.MAIN, openMap, Random(seed))
            val hitNote = out.logEntries.firstOrNull()?.steps?.firstOrNull { it.phaseName == "HIT" }?.note.orEmpty()
            if (hitNote.contains("RE-ROLL 1s")) {
                foundHitReroll = true
                break
            }
        }
        assertTrue("Expected Bladeguard attack to re-roll Hit rolls of 1", foundHitReroll)

        // 5. Deterministic seed check: Save rolls of 1 are re-rolled on Enemy Counter-Attack vs buffed Bladeguard
        var foundSaveReroll = false
        for (seed in 1..400) {
            val counter = TacticalRules.resolveMeleeCounterAttack(carnifex, buffedBg, openMap, Random(seed)) ?: continue
            val svStep = counter.logEntry?.steps?.firstOrNull { it.phaseName == "SAVE" } ?: continue
            if (svStep.note.contains("RE-ROLL 1s")) {
                assertTrue(svStep.stepLabel.contains("INV 4++"))
                foundSaveReroll = true
                break
            }
        }
        assertTrue("Expected Enemy Counter-Attack against buffed Bladeguard to re-roll Save rolls of 1", foundSaveReroll)
    }

    @Test
    fun testBiovoresAndSporeMine11thEditionStats() {
        val biovore = DefaultCatalog.defaultTemplates.first { it.id == "biovores" }
        assertEquals(UnitArchetype.BIOVORE, biovore.archetype)
        assertEquals(50, biovore.points)
        assertEquals(1, biovore.squadSize)
        assertEquals(5, biovore.woundsPerModel)
        assertEquals(6, biovore.toughness)
        assertEquals(4, biovore.save)
        assertNull(biovore.invulnSave)
        assertEquals(12, biovore.moveCm)
        assertEquals(8, biovore.range) // 24"
        assertEquals(1, biovore.attacksPerModel)
        assertEquals(4, biovore.ballisticSkill)
        assertEquals(6, biovore.strength)
        assertEquals(1, biovore.armorPen)
        assertEquals(2, biovore.damage)
        assertEquals("Seed Spore Mine", biovore.skillName)
        assertEquals(1, biovore.skillCooldown)

        val sporeMine = DefaultCatalog.defaultTemplates.first { it.id == "spore_mine" }
        assertEquals(UnitArchetype.SPORE_MINE, sporeMine.archetype)
        assertEquals(0, sporeMine.points)
        assertEquals(1, sporeMine.squadSize)
        assertEquals(1, sporeMine.woundsPerModel)
        assertEquals(1, sporeMine.toughness)
        assertEquals(6, sporeMine.save)
        assertEquals(4, sporeMine.moveCm) // 4 cm move
        assertEquals(1, sporeMine.range)
        assertEquals("Self-Destruct", sporeMine.skillName)

        val xenosRoster = DefaultCatalog.defaultXenosRoster()
        assertTrue("Xenos roster must include biovores", xenosRoster.any { it.templateId == "biovores" })
    }

    @Test
    fun testComputeSeedSporeMineTilesRequiresAtLeast8CmFromEnemies() {
        val biovoreTemplate = DefaultCatalog.defaultTemplates.first { it.id == "biovores" }
        val enemyTemplate = DefaultCatalog.defaultTemplates.first { it.id == "intercessors" }

        val biovore = BattleUnit(
            uid = "bio1",
            template = biovoreTemplate,
            faction = Faction.XENOS,
            x = 4,
            y = 4,
            facing = Facing.EAST,
            totalWounds = biovoreTemplate.maxTotalWounds,
            maxTotalWounds = biovoreTemplate.maxTotalWounds,
            moveCmLeft = biovoreTemplate.moveCm.toFloat(),
            worldX = 4f,
            worldY = 4f
        )
        val enemy = BattleUnit(
            uid = "marine1",
            template = enemyTemplate,
            faction = Faction.MARINES,
            x = 8,
            y = 4,
            facing = Facing.WEST,
            totalWounds = enemyTemplate.maxTotalWounds,
            maxTotalWounds = enemyTemplate.maxTotalWounds,
            moveCmLeft = enemyTemplate.moveCm.toFloat(),
            worldX = 8f,
            worldY = 4f
        )

        val map = Array(14) { Array(16) { TerrainType.OPEN } }
        val tiles = TacticalRules.computeSeedSporeMineTiles(
            biovore = biovore,
            map = map,
            allUnits = listOf(biovore, enemy),
            maxDist = 8,
            minEnemyDistCm = 8.0f
        )

        assertTrue("Should have valid spawn tiles", tiles.isNotEmpty())
        for (tile in tiles) {
            val distCm = kotlin.math.hypot(tile.x - enemy.worldX, tile.y - enemy.worldY) * TacticalRules.CM_PER_TILE
            assertTrue("Every seed tile must be >= 8 cm from enemy ($distCm cm at tile $tile)", distCm >= 8.0f - 0.05f)
        }
        // Enemy is at (8, 4), so adjacent tile (7, 4) is 1 tile away = 3 cm < 8 cm -> must not be allowed
        assertFalse("Tile (7,4) is 3 cm from enemy, must not be valid", tiles.contains(GridPos(7, 4)))
    }

    @Test
    fun testSporeMineDetonationMechanic() {
        val sporeMineTemplate = DefaultCatalog.defaultTemplates.first { it.id == "spore_mine" }
        val enemyTemplate = DefaultCatalog.defaultTemplates.first { it.id == "intercessors" }

        val sporeMine = BattleUnit(
            uid = "mine1",
            template = sporeMineTemplate,
            faction = Faction.XENOS,
            x = 5,
            y = 5,
            facing = Facing.EAST,
            totalWounds = 1,
            maxTotalWounds = 1,
            moveCmLeft = 4f,
            worldX = 5f,
            worldY = 5f
        )
        // Squad of 5 Intercessors (10 total wounds, 2W each)
        val enemy = BattleUnit(
            uid = "marine1",
            template = enemyTemplate,
            faction = Faction.MARINES,
            x = 5,
            y = 6,
            facing = Facing.NORTH,
            totalWounds = 10,
            maxTotalWounds = 10,
            moveCmLeft = enemyTemplate.moveCm.toFloat(),
            worldX = 5f,
            worldY = 6f
        )

        val map = Array(14) { Array(16) { TerrainType.OPEN } }
        // Verify canTarget acknowledges melee reach detonation
        val preview = TacticalRules.canTarget(sporeMine, enemy, WeaponMode.MAIN, map, listOf(sporeMine, enemy))
        assertNotNull("Spore mine adjacent to enemy must be able to target for detonation", preview)
        assertEquals(5, preview!!.attacksCount) // 1 D6 per enemy model

        // Deterministic RNG test:
        // Mock RNG returning rolls [1, 2, 3, 4, 1]
        // 4 of 5 rolls are < 4 (rolls of 1, 2, 3, 1), so exactly 4 Mortal Wounds must be inflicted
        val customRng = object : Random() {
            private val rolls = intArrayOf(1, 2, 3, 4, 1)
            private var idx = 0
            override fun nextBits(bitCount: Int): Int = 0
            override fun nextInt(from: Int, until: Int): Int {
                val r = rolls[idx % rolls.size]
                idx++
                return r
            }
        }

        val outcome = TacticalRules.resolveAttack(
            attacker = sporeMine,
            primaryTarget = enemy,
            allUnits = listOf(sporeMine, enemy),
            mode = WeaponMode.MAIN,
            map = map,
            rng = customRng
        )

        // Spore mine must be dead (0 wounds)
        assertEquals(0, outcome.updatedAttacker.totalWounds)
        assertFalse(outcome.updatedAttacker.isAlive)

        // Target must have suffered 4 mortal wounds (10 - 4 = 6W)
        val updatedEnemy = outcome.updatedTargets.first()
        assertEquals(6, updatedEnemy.totalWounds)
        assertEquals(3, updatedEnemy.aliveModels) // 6W with 2W per model = 3 models alive
        assertEquals(4, outcome.totalDamageDealt)

        // Verify combat log
        val log = outcome.logEntries.first()
        val detonateStep = log.steps.first()
        assertEquals("DETONATE", detonateStep.phaseName)
        assertEquals(5, detonateStep.rolled)
        assertEquals(4, detonateStep.passed)
        assertEquals(1, detonateStep.failed)
        assertTrue(log.title.contains("Detonates"))
    }

    @Test
    fun testBiovoreAndSporeMine3DMeshes() {
        val biovoreMesh = UnitMeshSculptor3D.build(UnitArchetype.BIOVORE)
        assertTrue("Biovore mesh vertex count must be > 100", biovoreMesh.vertexCount > 100)
        assertEquals("Biovore float buffer size must be multiple of 10", 0, biovoreMesh.data.size % 10)
        for (v in biovoreMesh.data) {
            assertFalse("Biovore float must not be NaN", v.isNaN())
            assertFalse("Biovore float must not be Infinite", v.isInfinite())
        }

        val sporeMineMesh = UnitMeshSculptor3D.build(UnitArchetype.SPORE_MINE)
        assertTrue("Spore Mine mesh vertex count must be > 100", sporeMineMesh.vertexCount > 100)
        assertEquals("Spore Mine float buffer size must be multiple of 10", 0, sporeMineMesh.data.size % 10)
        for (v in sporeMineMesh.data) {
            assertFalse("Spore Mine float must not be NaN", v.isNaN())
            assertFalse("Spore Mine float must not be Infinite", v.isInfinite())
        }
    }

    @Test
    fun coopRemoteClientCanCommandOwnUnitAndMoveModeState() {
        val mapRows = MapGenerator.generateSectorMap(1L)
        val map = com.example.model.SectorMap(1L, mapRows)
        val tpl = DefaultCatalog.defaultTemplates.first { it.id == "intercessors" }
        val p1Unit = BattleUnit(
            uid = "u_p1",
            template = tpl,
            faction = Faction.MARINES,
            x = 2,
            y = 4,
            facing = Facing.EAST,
            totalWounds = tpl.maxTotalWounds,
            maxTotalWounds = tpl.maxTotalWounds,
            moveCmLeft = 15f,
            ownerPlayerId = "p1",
            ownerPlayerName = "Host"
        )
        val p2Unit = BattleUnit(
            uid = "u_p2",
            template = tpl,
            faction = Faction.MARINES,
            x = 2,
            y = 8,
            facing = Facing.EAST,
            totalWounds = tpl.maxTotalWounds,
            maxTotalWounds = tpl.maxTotalWounds,
            moveCmLeft = 15f,
            ownerPlayerId = "p2",
            ownerPlayerName = "Friend"
        )
        val movePreview = TacticalRules.computeForwardCmPreview(p2Unit, 5f, listOf(p1Unit, p2Unit), mapRows)
        assertNotNull(movePreview)

        val clientState = com.example.viewmodel.BattleUiState(
            active = true,
            round = 1,
            activeFaction = Faction.MARINES,
            playerFaction = Faction.MARINES,
            map = map,
            units = listOf(p1Unit, p2Unit),
            selectedUnitUid = p2Unit.uid,
            moveModeActive = true,
            cmStepChoice = 5f,
            movePreview = movePreview,
            isCoopMode = true,
            activeCoopPlayerId = "p2",
            activeCoopPlayerName = "Friend",
            localControlledPlayerId = "p2",
            isAnimating = false
        )

        assertTrue(clientState.isPlayerTurn)
        assertTrue(clientState.canAcceptInput(false))
        assertTrue(clientState.canCommandUnit(p2Unit))
        assertFalse(clientState.canCommandUnit(p1Unit))
        assertTrue(clientState.moveModeActive)
        assertEquals(5f, clientState.cmStepChoice, 0.01f)
        assertNotNull(clientState.movePreview)
    }

    @Test
    fun brutalisDreadnoughtCatalogSerializationAndMesh() {
        val brutalis = DefaultCatalog.defaultTemplates.first { it.id == "brutalis_dreadnought" }
        assertEquals(UnitArchetype.BRUTALIS_DREADNOUGHT, brutalis.archetype)
        assertEquals(90, brutalis.baseDiameterMm)
        assertEquals("Brutalis Talons", brutalis.mainWeaponName)
        assertEquals(8, brutalis.attacksPerModel)
        assertEquals(3, brutalis.weaponSkill)
        assertEquals(10, brutalis.strength)
        assertEquals(2, brutalis.armorPen)
        assertEquals(2, brutalis.damage)
        assertEquals(2, brutalis.mainCleave)
        assertEquals(1, brutalis.mainSustainedNonMonsterVehicle)
        assertTrue(brutalis.mainTwinLinked)
        assertEquals("Twin multi-melta", brutalis.pistolWeaponName)
        assertFalse(brutalis.pistolIsPistol)
        assertEquals(3, brutalis.pistolMelta)
        assertTrue(brutalis.pistolTwinLinked)
        val aux = brutalis.auxRangedProfile
        assertNotNull(aux)
        assertEquals("Twin Icarus ironhail heavy stubber", aux!!.name)
        assertEquals(3, aux.antiFly)
        assertEquals(3, aux.rapidFire)
        assertTrue(aux.twinLinked)
        assertTrue(DefaultCatalog.defaultMarineRoster().any { it.templateId == "brutalis_dreadnought" })

        val mesh = com.example.ui.battle.MeshCatalog3D.getUnitMesh(UnitArchetype.BRUTALIS_DREADNOUGHT)
        assertTrue("Brutalis GLB mesh should load > 1000 vertices", mesh.vertexCount > 1000)
        assertNotNull("Brutalis GLB texture should be present", mesh.textureBytes)
        for (v in mesh.data) {
            assertFalse(v.isNaN())
            assertFalse(v.isInfinite())
        }
    }

    @Test
    fun brutalisDreadnoughtTwinMultiMeltaAndIcarusRules() {
        val brutalisTpl = DefaultCatalog.defaultTemplates.first { it.id == "brutalis_dreadnought" }
        val sporeTpl = DefaultCatalog.defaultTemplates.first { it.id == "spore_mine" }
        val marineTpl = DefaultCatalog.defaultTemplates.first { it.id == "legionaries" }
        val map = Array(14) { Array(16) { TerrainType.OPEN } }

        val brutalis = BattleUnit(
            uid = "brut1",
            template = brutalisTpl,
            faction = Faction.MARINES,
            x = 2,
            y = 6,
            facing = Facing.EAST,
            totalWounds = brutalisTpl.maxTotalWounds,
            maxTotalWounds = brutalisTpl.maxTotalWounds,
            moveCmLeft = 20f
        )
        // Flying enemy at distance 3 (within half range of Icarus range 6, outside half range of Multi-melta range 4)
        val flyTarget = BattleUnit(
            uid = "fly1",
            template = sporeTpl.copy(isFly = true, toughness = 6),
            faction = Faction.XENOS,
            x = 5,
            y = 6,
            facing = Facing.WEST,
            totalWounds = 6,
            maxTotalWounds = 6,
            moveCmLeft = 10f
        )
        // Close enemy at distance 2 (within half range 2 of Multi-melta range 4, NOT adjacent melee)
        val meltaTarget = BattleUnit(
            uid = "m2",
            template = marineTpl.copy(woundsPerModel = 10, toughness = 9),
            faction = Faction.BLACK_LEGION,
            x = 4,
            y = 6,
            facing = Facing.WEST,
            totalWounds = 10,
            maxTotalWounds = 10,
            moveCmLeft = 15f
        )

        val all = listOf(brutalis, flyTarget, meltaTarget)
        assertFalse("Brutalis SKILL is auto passive, not manual", TacticalRules.canUseMode(brutalis, WeaponMode.SKILL, all))
        assertTrue(TacticalRules.canUseMode(brutalis, WeaponMode.PISTOL, all))
        assertTrue(TacticalRules.canUseMode(brutalis, WeaponMode.AUX_RANGED, all))

        // Anti-Fly 3+ & Rapid Fire 3 at half-range 3 on Twin Icarus
        val icarusPreview = TacticalRules.canTarget(brutalis, flyTarget, WeaponMode.AUX_RANGED, map, all)!!
        assertTrue(icarusPreview.inRange)
        assertEquals(6, icarusPreview.attacksCount) // 3 base + 3 Rapid Fire
        assertEquals(3, icarusPreview.rapidFireBonus)
        assertTrue(icarusPreview.antiFlyTriggered)
        assertTrue(icarusPreview.twinLinked)
        assertEquals(3, icarusPreview.woundRollNeeded) // S4 vs T6 normally 5+, Anti-Fly 3+ overrides to 3+

        // Melta 3 at half-range 2 on Twin multi-melta (beyond half range gives 0)
        val meltaClosePreview = TacticalRules.canTarget(brutalis, meltaTarget, WeaponMode.PISTOL, map, all)!!
        assertEquals(3, meltaClosePreview.meltaBonus)
        assertTrue(meltaClosePreview.twinLinked)
        val meltaFarPreview = TacticalRules.canTarget(brutalis, flyTarget, WeaponMode.PISTOL, map, all)!!
        assertEquals(0, meltaFarPreview.meltaBonus)

        // Engaged in melee: Multi-melta (non-pistol) and Icarus are locked out
        val engagedFoe = flyTarget.copy(uid = "eng1", x = 3, y = 6, worldX = 3f, worldY = 6f)
        val engagedList = listOf(brutalis, engagedFoe)
        assertFalse("Non-pistol Twin multi-melta cannot fire in melee", TacticalRules.canUseMode(brutalis, WeaponMode.PISTOL, engagedList))
        assertFalse("Twin Icarus cannot fire in melee", TacticalRules.canUseMode(brutalis, WeaponMode.AUX_RANGED, engagedList))
        assertTrue("Brutalis Talons melee works in melee", TacticalRules.canUseMode(brutalis, WeaponMode.MAIN, engagedList))

        // Verify Melta +3D and Twin-Linked wound reroll in resolveAttack (per-attack loop):
        // Atk1: Hit 4, Wound 1 -> reroll 4 (7 dmg); Atk2: Hit 4, Wound 4 (7 dmg) -> kills 10W (Save is 7+ auto-fail)
        val detRng = object : Random() {
            private val seq = intArrayOf(4, 1, 4, 4, 4)
            private var i = 0
            override fun nextBits(bitCount: Int): Int = 0
            override fun nextInt(from: Int, until: Int): Int = seq[i++ % seq.size]
        }
        val meltaOutcome = TacticalRules.resolveAttack(brutalis, meltaTarget, all, WeaponMode.PISTOL, map, rng = detRng)
        assertEquals(0, meltaOutcome.updatedTargets.first().totalWounds)
        assertTrue(meltaOutcome.logEntries.first().steps.any { it.note.contains("TWIN-LINKED") })
    }

    @Test
    fun brutalisDreadnoughtChargeBuffAndPermanentKillStack() {
        val brutalisTpl = DefaultCatalog.defaultTemplates.first { it.id == "brutalis_dreadnought" }
        val gauntsTpl = DefaultCatalog.defaultTemplates.first { it.id == "hormagaunts" }
        val map = Array(14) { Array(16) { TerrainType.OPEN } }

        val enemy = BattleUnit(
            uid = "e1",
            template = gauntsTpl,
            faction = Faction.XENOS,
            x = 6,
            y = 6,
            facing = Facing.WEST,
            totalWounds = 10,
            maxTotalWounds = 10,
            moveCmLeft = 20f
        )

        // 1. Without Charge buff: S10 AP-2 D2 8A, killing enemy does NOT grant stack
        val unchargedBrutalis = BattleUnit(
            uid = "b1",
            template = brutalisTpl,
            faction = Faction.MARINES,
            x = 5,
            y = 6,
            facing = Facing.EAST,
            totalWounds = 12,
            maxTotalWounds = 12,
            moveCmLeft = 20f,
            brutalisChargeBuffActive = false
        )
        assertEquals(10, unchargedBrutalis.effectiveMainStrength)
        assertEquals(2, unchargedBrutalis.effectiveMainArmorPen)
        assertEquals(8, unchargedBrutalis.effectiveMainAttacksPerModel)

        val alwaysSixRng = object : Random() {
            override fun nextBits(bitCount: Int): Int = 0
            override fun nextInt(from: Int, until: Int): Int = 6
        }
        val noChargeOutcome = TacticalRules.resolveAttack(
            unchargedBrutalis,
            enemy,
            listOf(unchargedBrutalis, enemy),
            WeaponMode.MAIN,
            map,
            rng = alwaysSixRng
        )
        assertEquals(0, noChargeOutcome.updatedAttacker.brutalisAttackStacks)
        assertFalse(noChargeOutcome.updatedAttacker.brutalisStackEarnedThisTurn)

        // 2. With Charge buff active: S12 (+2) AP-3 (+1), killing >=1 model adds +1 stack
        val chargedBrutalis = unchargedBrutalis.copy(
            hasCharged = true,
            brutalisChargeBuffActive = true
        )
        assertEquals(12, chargedBrutalis.effectiveMainStrength)
        assertEquals(3, chargedBrutalis.effectiveMainArmorPen)
        val preview = TacticalRules.canTarget(chargedBrutalis, enemy, WeaponMode.MAIN, map, listOf(chargedBrutalis, enemy))!!
        assertEquals(12, preview.weaponStrength)
        assertEquals(3, preview.weaponAp)

        val chargeKillOutcome = TacticalRules.resolveAttack(
            chargedBrutalis,
            enemy,
            listOf(chargedBrutalis, enemy),
            WeaponMode.MAIN,
            map,
            rng = alwaysSixRng
        )
        val stackedOnce = chargeKillOutcome.updatedAttacker
        assertEquals(1, stackedOnce.brutalisAttackStacks)
        assertTrue(stackedOnce.brutalisStackEarnedThisTurn)
        assertEquals(9, stackedOnce.effectiveMainAttacksPerModel)

        // 3. Another kill in the SAME turn cannot add a second stack
        val secondEnemy = enemy.copy(uid = "e2")
        val sameTurnSecondKill = TacticalRules.resolveAttack(
            stackedOnce.copy(hasFought = false),
            secondEnemy,
            listOf(stackedOnce, secondEnemy),
            WeaponMode.MAIN,
            map,
            rng = alwaysSixRng
        )
        assertEquals(1, sameTurnSecondKill.updatedAttacker.brutalisAttackStacks)

        // 4. Subsequent turns cap permanent stack at 4 (total 12A)
        var carrier = stackedOnce.copy(
            hasFought = false,
            hasCharged = true,
            brutalisChargeBuffActive = true,
            brutalisStackEarnedThisTurn = false
        )
        for (turn in 2..5) {
            val freshEnemy = enemy.copy(uid = "et_$turn")
            val out = TacticalRules.resolveAttack(
                carrier,
                freshEnemy,
                listOf(carrier, freshEnemy),
                WeaponMode.MAIN,
                map,
                rng = alwaysSixRng
            )
            carrier = out.updatedAttacker.copy(
                hasFought = false,
                hasCharged = true,
                brutalisChargeBuffActive = true,
                brutalisStackEarnedThisTurn = false
            )
        }
        assertEquals(4, carrier.brutalisAttackStacks)
        assertEquals(12, carrier.effectiveMainAttacksPerModel)
    }

    @Test
    fun brutalisTalonsCleaveSustainedNonMonsterAndTwinLinked() {
        val brutalisTpl = DefaultCatalog.defaultTemplates.first { it.id == "brutalis_dreadnought" }
        val gauntsTpl = DefaultCatalog.defaultTemplates.first { it.id == "termagants" } // 10 models
        val legionaryTpl = DefaultCatalog.defaultTemplates.first { it.id == "legionaries" } // 5 models, 2W
        val carnifexTpl = DefaultCatalog.defaultTemplates.first { it.id == "carnifex" } // Monster
        val map = Array(14) { Array(16) { TerrainType.OPEN } }

        val brutalis = BattleUnit(
            uid = "b1",
            template = brutalisTpl,
            faction = Faction.MARINES,
            x = 5,
            y = 6,
            facing = Facing.EAST,
            totalWounds = 12,
            maxTotalWounds = 12,
            moveCmLeft = 20f
        )

        // 10-model squad => Cleave 2 adds (10/5)*2 = +4A (8 + 4 = 12A) + Sustained Hits 1 (non-Monster/Vehicle)
        val horde10 = BattleUnit(
            uid = "g10",
            template = gauntsTpl,
            faction = Faction.XENOS,
            x = 6,
            y = 6,
            facing = Facing.WEST,
            totalWounds = 10,
            maxTotalWounds = 10,
            moveCmLeft = 15f
        )
        val hordePreview = TacticalRules.canTarget(brutalis, horde10, WeaponMode.MAIN, map, listOf(brutalis, horde10))!!
        assertEquals(4, hordePreview.cleaveBonus)
        assertEquals(12, hordePreview.attacksCount)
        assertEquals(1, hordePreview.sustainedHits)
        assertTrue(hordePreview.twinLinked)

        // 5-model squad => Cleave 2 adds (5/5)*2 = +2A (8 + 2 = 10A) + Sustained Hits 1
        val squad5 = BattleUnit(
            uid = "l5",
            template = legionaryTpl,
            faction = Faction.BLACK_LEGION,
            x = 6,
            y = 6,
            facing = Facing.WEST,
            totalWounds = 10,
            maxTotalWounds = 10,
            moveCmLeft = 15f
        )
        val squad5Preview = TacticalRules.canTarget(brutalis, squad5, WeaponMode.MAIN, map, listOf(brutalis, squad5))!!
        assertEquals(2, squad5Preview.cleaveBonus)
        assertEquals(10, squad5Preview.attacksCount)
        assertEquals(1, squad5Preview.sustainedHits)

        // 4-model squad (under 5 models) => Cleave 2 adds 0A (remains 8A)
        val squad4 = squad5.copy(uid = "l4", totalWounds = 8)
        val squad4Preview = TacticalRules.canTarget(brutalis, squad4, WeaponMode.MAIN, map, listOf(brutalis, squad4))!!
        assertEquals(0, squad4Preview.cleaveBonus)
        assertEquals(8, squad4Preview.attacksCount)

        // Monster/Vehicle target => Cleave 0A AND Sustained Hits 0 (does NOT trigger Sustained Hits 1)
        val carnifex = BattleUnit(
            uid = "c1",
            template = carnifexTpl.copy(woundsPerModel = 20),
            faction = Faction.XENOS,
            x = 6,
            y = 6,
            facing = Facing.WEST,
            totalWounds = 20,
            maxTotalWounds = 20,
            moveCmLeft = 15f
        )
        val monsterPreview = TacticalRules.canTarget(brutalis, carnifex, WeaponMode.MAIN, map, listOf(brutalis, carnifex))!!
        assertEquals(0, monsterPreview.cleaveBonus)
        assertEquals(8, monsterPreview.attacksCount)
        assertEquals(0, monsterPreview.sustainedHits)
        assertTrue(monsterPreview.twinLinked)

        // Resolve attacks with all 6s: vs 5-model Legionaries -> 10 attacks, each crit adds +1 sustained hit = 20 hits
        val alwaysSixRng = object : Random() {
            override fun nextBits(bitCount: Int): Int = 0
            override fun nextInt(from: Int, until: Int): Int = 6
        }
        val vsInfantryOutcome = TacticalRules.resolveAttack(
            brutalis,
            squad5.copy(totalWounds = 200, maxTotalWounds = 200), // keep wounds high so all 10 attacks run if same model count; note totalWounds=10 is 5 models
            listOf(brutalis, squad5),
            WeaponMode.MAIN,
            map,
            rng = alwaysSixRng
        )
        assertTrue(vsInfantryOutcome.floatingTexts.any { it.second.first.contains("CLEAVE +2A") })
        assertTrue(vsInfantryOutcome.floatingTexts.any { it.second.first.contains("SUSTAINED") })

        // vs Carnifex (20W) with all 6s -> exactly 8 hits (no Sustained extra hits!)
        val vsMonsterOutcome = TacticalRules.resolveAttack(
            brutalis,
            carnifex,
            listOf(brutalis, carnifex),
            WeaponMode.MAIN,
            map,
            rng = alwaysSixRng
        )
        assertEquals(8, vsMonsterOutcome.hits)
        assertFalse(vsMonsterOutcome.floatingTexts.any { it.second.first.contains("SUSTAINED") })

        // Verify Twin-linked wound reroll on Brutalis Talons: Hit 4, Wound 1 -> reroll 4, Save 1 (all other attacks 4/4/1)
        val twinLinkedRng = object : Random() {
            private val seq = intArrayOf(4, 1, 4, 1, 4, 4, 1)
            private var i = 0
            override fun nextBits(bitCount: Int): Int = 0
            override fun nextInt(from: Int, until: Int): Int = seq[i++ % seq.size]
        }
        val twinOut = TacticalRules.resolveAttack(
            brutalis,
            carnifex,
            listOf(brutalis, carnifex),
            WeaponMode.MAIN,
            map,
            rng = twinLinkedRng
        )
        assertTrue(twinOut.logEntries.first().steps.any { it.note.contains("TWIN-LINKED") })
    }

    @Test
    fun coopEnemyFactionAndMirrorEngagementRulesMatchHudChecks() {
        val stateMirrorBl = com.example.viewmodel.BattleUiState(
            isCoopMode = true,
            playerFaction = Faction.BLACK_LEGION,
            enemyFaction = Faction.BLACK_LEGION
        )
        assertEquals(Faction.BLACK_LEGION, stateMirrorBl.resolvedEnemyFaction)

        val stateSkirmish = com.example.viewmodel.BattleUiState(
            isCoopMode = false,
            playerFaction = Faction.BLACK_LEGION,
            enemyFaction = Faction.MARINES
        )
        assertEquals(Faction.MARINES, stateSkirmish.resolvedEnemyFaction)

        val setupError = com.example.viewmodel.SetupUiState(
            statusMessage = "Cannot add Termagants: exceeds 2000 pt limit!",
            statusIsError = true
        )
        assertTrue(setupError.statusIsError)

        val leaperTpl = DefaultCatalog.defaultTemplates.first { it.id == "von_ryans_leaper" }
        val p1Leaper = BattleUnit("l1", leaperTpl, Faction.XENOS, 5, 5, Facing.EAST, leaperTpl.maxTotalWounds, leaperTpl.maxTotalWounds, 20f, ownerPlayerId = "p1")
        val p2AllyLeaper = p1Leaper.copy(uid = "l2", x = 5, y = 6, ownerPlayerId = "p2")
        val p3EnemyLeaper = p1Leaper.copy(uid = "l3", x = 6, y = 5, ownerPlayerId = "p3")
        assertFalse(p1Leaper.isEnemyOf(p2AllyLeaper))
        assertTrue(p1Leaper.isEnemyOf(p3EnemyLeaper))
    }

    @Test
    fun captainTitusJoinEligibilityModelReachAndHeroLimit() {
        val byId = DefaultCatalog.defaultTemplates.associateBy { it.id }
        val titusTpl = byId.getValue("captain_titus")
        val intercessorsTpl = byId.getValue("intercessors")
        val assaultTpl = byId.getValue("assault_intercessors")
        val hellblastersTpl = byId.getValue("hellblasters")
        val bladeguardTpl = byId.getValue("bladeguard_veterans")
        val dreadTpl = byId.getValue("dreadnought")
        val termisTpl = byId.getValue("terminators")
        val incursorsTpl = byId.getValue("incursors")

        fun spawn(uid: String, tpl: UnitTemplate, wx: Float, wy: Float): BattleUnit = BattleUnit(
            uid = uid,
            template = tpl,
            faction = tpl.faction,
            x = wx.toInt(),
            y = wy.toInt(),
            worldX = wx,
            worldY = wy,
            facing = Facing.EAST,
            totalWounds = tpl.maxTotalWounds,
            maxTotalWounds = tpl.maxTotalWounds,
            moveCmLeft = tpl.moveCm.toFloat(),
            isHero = tpl.isHero,
            hasFeelNoPain = tpl.isHero
        )

        val titus = spawn("t1", titusTpl, 5f, 5f)
        val intercessors = spawn("i1", intercessorsTpl, 5.4f, 5f)
        val assault = spawn("a1", assaultTpl, 5.4f, 5f)
        val hellblasters = spawn("h1", hellblastersTpl, 5.4f, 5f)
        val bladeguard = spawn("b1", bladeguardTpl, 5.4f, 5f)
        val dread = spawn("d1", dreadTpl, 5.4f, 5f)
        val termis = spawn("tm1", termisTpl, 5.4f, 5f)
        val incursors = spawn("inc1", incursorsTpl, 5.4f, 5f)

        assertTrue(TacticalRules.canLeadSquad(titus, intercessors))
        assertTrue(TacticalRules.canLeadSquad(titus, assault))
        assertTrue(TacticalRules.canLeadSquad(titus, hellblasters))
        assertTrue(TacticalRules.canLeadSquad(titus, bladeguard))
        assertFalse(TacticalRules.canLeadSquad(titus, dread))
        assertFalse(TacticalRules.canLeadSquad(titus, termis))
        assertFalse(TacticalRules.canLeadSquad(titus, incursors))

        // Far apart (> 2cm) vs within 2cm of a squad edge model
        val farIntercessors = spawn("i_far", intercessorsTpl, 9f, 5f)
        assertFalse(TacticalRules.isWithinJoiningReach(titus, farIntercessors))

        // Center-to-center is ~1.05 tile (~2.1cm), while edge sub-model of a 5-man squad is 0.42 tile closer -> within 2cm
        val edgeIntercessors = spawn("i_edge", intercessorsTpl, 6.0f, 5f)
        assertTrue(TacticalRules.isWithinJoiningReach(titus, edgeIntercessors))

        // Hero limit: max 1 Hero in Skirmish army validation
        val setupDoubleHero = com.example.viewmodel.SetupUiState(
            templates = byId,
            playerFaction = Faction.MARINES,
            enemyFaction = Faction.XENOS,
            marineRoster = listOf(RosterSlot("s1", "captain_titus"), RosterSlot("s2", "captain_titus"))
        )
        assertNotNull(setupDoubleHero.validationWarning)
        assertFalse(setupDoubleHero.canDeploy)
    }

    @Test
    fun captainTitusHeavyAntiNonMvPressTheAttackAndFeelNoPain() {
        val map = Array(MapGenerator.GRID_H) { Array(MapGenerator.GRID_W) { TerrainType.OPEN } }
        val byId = DefaultCatalog.defaultTemplates.associateBy { it.id }
        val titusTpl = byId.getValue("captain_titus")
        val gauntTpl = byId.getValue("termagants")
        val carnifexTpl = byId.getValue("carnifex")

        val titusStationary = BattleUnit(
            uid = "t1",
            template = titusTpl,
            faction = Faction.MARINES,
            x = 2,
            y = 6,
            facing = Facing.EAST,
            totalWounds = 5,
            maxTotalWounds = 5,
            moveCmLeft = titusTpl.moveCm.toFloat(),
            isHero = true,
            hasFeelNoPain = true
        )
        val enemyInfantry = BattleUnit(
            uid = "g1",
            template = gauntTpl,
            faction = Faction.XENOS,
            x = 5,
            y = 6,
            facing = Facing.WEST,
            totalWounds = 10,
            maxTotalWounds = 10,
            moveCmLeft = 15f
        )

        // 1. Heavy +1 Hit when stationary (heavyApplied flag)
        val stationaryShot = TacticalRules.canTarget(titusStationary, enemyInfantry, WeaponMode.MAIN, map, listOf(titusStationary, enemyInfantry))!!
        assertTrue(stationaryShot.heavyApplied)
        val movedTitus = titusStationary.copy(moveCmLeft = 10f)
        val movedShot = TacticalRules.canTarget(movedTitus, enemyInfantry, WeaponMode.MAIN, map, listOf(movedTitus, enemyInfantry))!!
        assertFalse(movedShot.heavyApplied)

        // 2. Press the Attack + Anti-Non-Monster/Vehicle 2+ in melee
        val adjGaunt = enemyInfantry.copy(x = 3, y = 6, worldX = 3f, worldY = 6f)
        val meleeVsInfantry = TacticalRules.canTarget(titusStationary, adjGaunt, WeaponMode.MAIN, map, listOf(titusStationary, adjGaunt))!!
        assertTrue(meleeVsInfantry.isMelee)
        assertTrue(meleeVsInfantry.antiNonMonsterVehicleTriggered)
        assertEquals(2, meleeVsInfantry.woundRollNeeded)
        assertEquals(1, meleeVsInfantry.sustainedHits)

        val adjCarnifex = BattleUnit(
            uid = "c1",
            template = carnifexTpl,
            faction = Faction.XENOS,
            x = 3,
            y = 6,
            facing = Facing.WEST,
            totalWounds = 8,
            maxTotalWounds = 8,
            moveCmLeft = 20f
        )
        val meleeVsMonsterNoCharge = TacticalRules.canTarget(titusStationary, adjCarnifex, WeaponMode.MAIN, map, listOf(titusStationary, adjCarnifex))!!
        assertFalse(meleeVsMonsterNoCharge.antiNonMonsterVehicleTriggered)
        assertEquals(0, meleeVsMonsterNoCharge.sustainedHits)

        val chargedTitus = titusStationary.copy(hasCharged = true)
        val meleeVsMonsterCharged = TacticalRules.canTarget(chargedTitus, adjCarnifex, WeaponMode.MAIN, map, listOf(chargedTitus, adjCarnifex))!!
        assertEquals(1, meleeVsMonsterCharged.sustainedHits)

        // 3. Feel No Pain 5+: 6s ignore wounds, so Titus takes 0 wounds!
        val alwaysSixRng = object : Random() {
            override fun nextBits(bitCount: Int): Int = 0
            override fun nextInt(from: Int, until: Int): Int = 6
        }
        val remainingAfterFnp = TacticalRules.applyFeelNoPain(titusStationary, incomingDamage = 4, rng = alwaysSixRng)
        assertEquals(0, remainingAfterFnp)
    }

    @Test
    fun leaderBodyguardTargetingSimultaneousCombatZeroOverkillAndHonourOfUltramar() {
        val map = Array(MapGenerator.GRID_H) { Array(MapGenerator.GRID_W) { TerrainType.OPEN } }
        val byId = DefaultCatalog.defaultTemplates.associateBy { it.id }
        val titusTpl = byId.getValue("captain_titus")
        val intercessorsTpl = byId.getValue("intercessors")
        val carnifexTpl = byId.getValue("carnifex")

        val titus = BattleUnit(
            uid = "leader_titus",
            template = titusTpl,
            faction = Faction.MARINES,
            x = 5,
            y = 6,
            facing = Facing.EAST,
            totalWounds = 5,
            maxTotalWounds = 5,
            moveCmLeft = 15f,
            isHero = true,
            hasFeelNoPain = true,
            bodyguardUid = "bg_intercessors"
        )
        val bodyguard = BattleUnit(
            uid = "bg_intercessors",
            template = intercessorsTpl,
            faction = Faction.MARINES,
            x = 5,
            y = 6,
            facing = Facing.EAST,
            totalWounds = 2, // last 2W model remaining!
            maxTotalWounds = intercessorsTpl.maxTotalWounds,
            moveCmLeft = 15f,
            attachedLeaderUid = "leader_titus"
        )
        val carnifex = BattleUnit(
            uid = "foe_carnifex",
            template = carnifexTpl.copy(attacksPerModel = 2, weaponSkill = 2, strength = 12, armorPen = 4, damage = 4),
            faction = Faction.XENOS,
            x = 6,
            y = 6,
            facing = Facing.WEST,
            totalWounds = 20,
            maxTotalWounds = 20,
            moveCmLeft = 20f
        )

        val all = listOf(titus, bodyguard, carnifex)

        // 1. Simultaneous combat: triggering melee from Bodyguard resolves BOTH Bodyguard and Titus weapons
        val alwaysHitFailSave = object : Random() {
            private val seq = intArrayOf(5, 5, 1)
            private var idx = 0
            override fun nextBits(bitCount: Int): Int = 0
            override fun nextInt(from: Int, until: Int): Int = seq[idx++ % seq.size]
        }
        val simulOutcome = TacticalRules.resolveAttack(
            attacker = bodyguard,
            primaryTarget = carnifex,
            allUnits = all,
            mode = WeaponMode.MAIN,
            map = map,
            rng = alwaysHitFailSave
        )
        assertTrue(simulOutcome.updatedTargets.first { it.uid == carnifex.uid }.totalWounds < 20)
        assertEquals(2, simulOutcome.logEntries.size) // Both Bodyguard and Titus logged

        // 2. Targeting Titus routes to Bodyguard first -> Bodyguard dies with huge overkill -> Titus takes 0 spillover and inherits Intercessors passive permanently
        val killBodyguardOutcome = TacticalRules.resolveAttack(
            attacker = carnifex,
            primaryTarget = titus,
            allUnits = all,
            mode = WeaponMode.MAIN,
            map = map,
            rng = object : Random() {
                private val seq = intArrayOf(6, 6, 1, 1) // Hit 6, Wound 6, Save 1, FNP 1
                private var idx = 0
                override fun nextBits(bitCount: Int): Int = 0
                override fun nextInt(from: Int, until: Int): Int = seq[idx++ % seq.size]
            }
        )
        val updatedBg = killBodyguardOutcome.updatedTargets.first { it.uid == bodyguard.uid }
        assertEquals(0, updatedBg.totalWounds)
        val detachedTitus = killBodyguardOutcome.updatedTargets.firstOrNull { it.uid == titus.uid }
        assertNotNull(detachedTitus)
        assertNull(detachedTitus!!.bodyguardUid)
        assertEquals(5, detachedTitus.totalWounds) // Zero spillover!
        assertTrue("intercessors" in detachedTitus.inheritedPassiveIds)

        // 3. Honour of Ultramar: Titus dying in Fight phase while !hasFought rolls 2+ to stay at 1W with honourOfUltramarPendingRemoval = true
        val soloTitus = detachedTitus.copy(hasFought = false, totalWounds = 1)
        val honourOut = TacticalRules.resolveAttack(
            attacker = carnifex,
            primaryTarget = soloTitus,
            allUnits = listOf(soloTitus, carnifex),
            mode = WeaponMode.MAIN,
            map = map,
            rng = object : Random() {
                // Hit 6, Wound 6, Save 1, FNP 1 (fails FNP 5+), Honour 5 (passes 2+!)
                private val seq = intArrayOf(6, 6, 1, 1, 5)
                private var idx = 0
                override fun nextBits(bitCount: Int): Int = 0
                override fun nextInt(from: Int, until: Int): Int = seq[idx++ % seq.size]
            }
        )
        val standingTitus = honourOut.updatedTargets.first { it.uid == soloTitus.uid }
        assertTrue(standingTitus.isAlive)
        assertEquals(1, standingTitus.totalWounds)
        assertTrue(standingTitus.honourOfUltramarPendingRemoval)
    }

    @Test
    fun coopTurnOwnershipSyncRevisionAndAnimationLockChecks() {
        val byId = DefaultCatalog.defaultTemplates.associateBy { it.id }
        val bladeguardTpl = byId.getValue("bladeguard_veterans")
        val gauntTpl = byId.getValue("termagants")

        val p1Unit = BattleUnit(
            uid = "u_p1",
            template = bladeguardTpl,
            faction = Faction.MARINES,
            x = 2,
            y = 5,
            facing = Facing.EAST,
            totalWounds = bladeguardTpl.maxTotalWounds,
            maxTotalWounds = bladeguardTpl.maxTotalWounds,
            moveCmLeft = bladeguardTpl.moveCm.toFloat(),
            ownerPlayerId = "p1",
            ownerPlayerName = "NEETMANTH"
        )
        val p3Unit = BattleUnit(
            uid = "u_p3",
            template = gauntTpl,
            faction = Faction.XENOS,
            x = 18,
            y = 5,
            facing = Facing.WEST,
            totalWounds = gauntTpl.maxTotalWounds,
            maxTotalWounds = gauntTpl.maxTotalWounds,
            moveCmLeft = gauntTpl.moveCm.toFloat(),
            ownerPlayerId = "p3",
            ownerPlayerName = "Guest"
        )

        val p1TurnOnP3Device = com.example.viewmodel.BattleUiState(
            active = true,
            round = 3,
            activeFaction = Faction.MARINES,
            playerFaction = Faction.MARINES,
            enemyFaction = Faction.XENOS,
            units = listOf(p1Unit, p3Unit),
            selectedUnitUid = p1Unit.uid,
            isCoopMode = true,
            coopTurnIndex = 0,
            activeCoopPlayerId = "p1",
            activeCoopPlayerName = "NEETMANTH",
            localControlledPlayerId = "p3",
            isAnimating = false,
            isAiThinking = false,
            syncRevision = 42L
        )
        assertFalse(p1TurnOnP3Device.isPlayerTurn)
        assertFalse(p1TurnOnP3Device.canAcceptInput(isRemoteClientAction = false))
        assertTrue(p1TurnOnP3Device.canAcceptInput(isRemoteClientAction = true))

        // After host clears animation and advances turn to p3 (Round 3), p3 controls their units immediately
        val p3TurnOnP3Device = p1TurnOnP3Device.copy(
            activeFaction = Faction.XENOS,
            selectedUnitUid = p3Unit.uid,
            coopTurnIndex = 1,
            activeCoopPlayerId = "p3",
            activeCoopPlayerName = "Guest",
            syncRevision = 43L
        )
        assertTrue(p3TurnOnP3Device.isPlayerTurn)
        assertTrue(p3TurnOnP3Device.canAcceptInput(isRemoteClientAction = false))
        assertTrue(p3TurnOnP3Device.canCommandUnit(p3Unit, isRemoteClientAction = false))
        assertFalse(p3TurnOnP3Device.canCommandUnit(p1Unit, isRemoteClientAction = false))

        // Stuck animation blocks both local and remote actions, reinforcing why finally/turn transitions must clear it
        val stuckAnimState = p3TurnOnP3Device.copy(isAnimating = true)
        assertFalse(stuckAnimState.canAcceptInput(isRemoteClientAction = false))
        assertFalse(stuckAnimState.canAcceptInput(isRemoteClientAction = true))
    }
}
