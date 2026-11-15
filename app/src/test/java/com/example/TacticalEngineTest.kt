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
            totalHp = tpl.maxTotalHp,
            maxTotalHp = tpl.maxTotalHp,
            mpLeft = tpl.mp,
            apLeft = tpl.ap
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
            totalHp = marineTpl.maxTotalHp,
            maxTotalHp = marineTpl.maxTotalHp,
            mpLeft = 4,
            apLeft = 2
        )
        val defenderFacingEast = BattleUnit(
            uid = "x1",
            template = xenosTpl,
            faction = Faction.XENOS,
            x = 5,
            y = 6,
            facing = Facing.EAST,
            totalHp = xenosTpl.maxTotalHp,
            maxTotalHp = xenosTpl.maxTotalHp,
            mpLeft = 4,
            apLeft = 2
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
        assertTrue(outcome.updatedAttacker.momentum > 0)
    }

    @Test
    fun aiSelectsValidActionAgainstEnemy() {
        val map = MapGenerator.generateSectorMap(1L)
        val marineTpl = DefaultCatalog.defaultTemplates.first { it.id == "intercessors" }
        val xenosTpl = DefaultCatalog.defaultTemplates.first { it.id == "termagants" }
        val marine = BattleUnit("m1", marineTpl, Faction.MARINES, 2, 6, Facing.EAST, 100, 100, 4, 2)
        val xenos = BattleUnit("x1", xenosTpl, Faction.XENOS, 12, 6, Facing.WEST, 80, 80, 5, 2)

        val decision = AiController.decideNextAction(Faction.XENOS, listOf(marine, xenos), map)
        assertNotNull(decision)
        assertTrue(decision is AiDecision.Move || decision is AiDecision.Attack)
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
}
