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
import com.example.model.WeaponMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
        assertFalse(GridPos(4, 6) in search.costs) // RUIN_WALL
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
        assertEquals(CoverTag.REAR, TacticalRules.evaluateCoverTag(attacker, defenderFacingEast, map))

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

        val marine = BattleUnit("m", intercessorTpl, Faction.MARINES, 2, 6, Facing.EAST, intercessorTpl.maxTotalWounds, intercessorTpl.maxTotalWounds, 15f)
        val warrior = BattleUnit("w", warriorTpl, Faction.XENOS, 3, 6, Facing.WEST, warriorTpl.maxTotalWounds, warriorTpl.maxTotalWounds, 15f)

        val rangedPreview = TacticalRules.canTarget(marine, warrior, WeaponMode.MAIN, map)
        assertNotNull(rangedPreview)
        assertFalse(rangedPreview!!.isMelee)
        // Intercessor BS 3+ with stationary (+1) => 2+
        assertEquals(2, rangedPreview.hitRollNeeded)
        assertEquals(5, rangedPreview.targetToughness)

        val meleePreview = TacticalRules.canTarget(warrior, marine, WeaponMode.MAIN, map)
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

        // Tapping directly inside/against RUIN_WALL at (4, 6) should be blocked
        val blockedWall = TacticalRules.computeCmMovePreview(marine, 4.0f, 6.0f, listOf(marine), map)
        assertEquals(null, blockedWall)
    }

    @Test
    fun tabletopPhaseRulesAllowShootAndMeleeWithoutActionPoints() {
        val map = MapGenerator.generateSectorMap(1L)
        val intercessorTpl = DefaultCatalog.defaultTemplates.first { it.id == "intercessors" }
        val assaultTpl = DefaultCatalog.defaultTemplates.first { it.id == "assault_intercessors" }
        val warriorTpl = DefaultCatalog.defaultTemplates.first { it.id == "warriors" }

        val shooter = BattleUnit("m1", intercessorTpl, Faction.MARINES, 2, 5, Facing.EAST, intercessorTpl.maxTotalWounds, intercessorTpl.maxTotalWounds, 15f)
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
        assertEquals(roll * TacticalRules.CM_PER_TILE, bonusCm, 0.01f)

        val intercessorTpl = DefaultCatalog.defaultTemplates.first { it.id == "intercessors" } // Assault = true
        val hellblasterTpl = DefaultCatalog.defaultTemplates.first { it.id == "hellblasters" } // Assault = false

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
        // Non-Assault Plasma Incinerator cannot shoot or charge after Advancing
        assertFalse(advancedHellblaster.canCharge)
        assertFalse(advancedHellblaster.canShoot)
        assertFalse(TacticalRules.canUseMode(advancedHellblaster, WeaponMode.MAIN))
        assertFalse(TacticalRules.canUseMode(advancedHellblaster, WeaponMode.SKILL))
        assertFalse(TacticalRules.canUseMode(advancedHellblaster, WeaponMode.PISTOL))
    }

    @Test
    fun chargeComputesEngagementSpotAndEnablesMeleeForChargedUnits() {
        val map = MapGenerator.generateSectorMap(1L)
        val intercessorTpl = DefaultCatalog.defaultTemplates.first { it.id == "intercessors" }
        val warriorTpl = DefaultCatalog.defaultTemplates.first { it.id == "warriors" }

        val charger = BattleUnit("m1", intercessorTpl, Faction.MARINES, 5, 5, Facing.EAST, intercessorTpl.maxTotalWounds, intercessorTpl.maxTotalWounds, 0f)
        val enemy = BattleUnit("x1", warriorTpl, Faction.XENOS, 10, 5, Facing.WEST, warriorTpl.maxTotalWounds, warriorTpl.maxTotalWounds, 15f)

        val preview = TacticalRules.findChargeEngagementSpot(charger, enemy, listOf(charger, enemy), map)
        assertNotNull(preview)
        assertTrue(preview!!.requiredRoll in 2..12)
        assertTrue(preview.path.isNotEmpty())

        // After Charge lands adjacent to enemy, shooting is blocked and Melee WS fight is unlocked
        val chargedUnit = charger.copy(
            x = 9,
            y = 5,
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
}

