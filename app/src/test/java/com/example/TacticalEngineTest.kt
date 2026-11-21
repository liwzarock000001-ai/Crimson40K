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
import com.example.model.TerrainType
import com.example.model.UnitArchetype
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
        assertEquals(2, termiTpl.damage)

        val clawsTpl = termiTpl.copy(
            terminatorWeapon = com.example.model.TerminatorWeaponChoice.LIGHTNING_CLAWS,
            mainWeaponName = "Twin Lightning Claws",
            woundsPerModel = 3,
            attacksPerModel = 5,
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
}




