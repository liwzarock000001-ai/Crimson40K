package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Crimson Sector", appName)
  }

  @Test
  fun `template serialization preserves pistol attacks and attack count`() {
    val baseTpl = com.example.engine.DefaultCatalog.defaultTemplates.first { it.id == "hellblasters" }
    val customTpl = baseTpl.copy(attacksPerModel = 4, pistolAttacks = 3, mainWeaponPistol = true)
    val json = com.example.engine.DefaultCatalog.serializeTemplates(mapOf(customTpl.id to customTpl))
    val restored = com.example.engine.DefaultCatalog.deserializeTemplates(json)["hellblasters"]
    org.junit.Assert.assertNotNull(restored)
    assertEquals(4, restored!!.attacksPerModel)
    assertEquals(3, restored.pistolAttacks)
    org.junit.Assert.assertTrue(restored.mainWeaponPistol)
  }

  @Test
  fun `serializer and coop snapshot preserve unit owners and turn order`() {
    val map = com.example.engine.MapGenerator.generateSectorMap(99L)
    val sectorMap = com.example.model.SectorMap(99L, map)
    val templates = com.example.engine.DefaultCatalog.defaultTemplates.associateBy { it.id }
    val marineTpl = templates.getValue("intercessors")
    val termiTpl = templates.getValue("terminators")

    val u1 = com.example.model.BattleUnit(
      uid = "coop_p1_1",
      template = marineTpl,
      faction = com.example.model.Faction.MARINES,
      x = 1,
      y = 5,
      facing = com.example.model.Facing.EAST,
      totalWounds = marineTpl.maxTotalWounds,
      maxTotalWounds = marineTpl.maxTotalWounds,
      moveCmLeft = marineTpl.moveCm.toFloat(),
      ownerPlayerId = "p1",
      ownerPlayerName = "Commander 1 (Host)"
    )
    val u2 = com.example.model.BattleUnit(
      uid = "coop_p2_1",
      template = termiTpl,
      faction = com.example.model.Faction.MARINES,
      x = 2,
      y = 6,
      facing = com.example.model.Facing.EAST,
      totalWounds = termiTpl.maxTotalWounds,
      maxTotalWounds = termiTpl.maxTotalWounds,
      moveCmLeft = termiTpl.moveCm.toFloat(),
      ownerPlayerId = "p2",
      ownerPlayerName = "Commander 2"
    )

    val unitsJson = com.example.engine.BattleStateSerializer.serializeUnits(listOf(u1, u2))
    val restored = com.example.engine.BattleStateSerializer.deserializeUnits(unitsJson, templates)
    assertEquals(2, restored.size)
    assertEquals("p1", restored[0].ownerPlayerId)
    assertEquals("Commander 2", restored[1].ownerPlayerName)

    val p1 = com.example.model.CoopPlayer("p1", "Commander 1 (Host)", com.example.model.Faction.MARINES, com.example.model.CoopSlotMode.HOST)
    val p2 = com.example.model.CoopPlayer("p2", "Commander 2", com.example.model.Faction.MARINES, com.example.model.CoopSlotMode.LAN_CLIENT)
    val state = com.example.viewmodel.BattleUiState(
      active = true,
      round = 2,
      activeFaction = com.example.model.Faction.MARINES,
      playerFaction = com.example.model.Faction.MARINES,
      map = sectorMap,
      units = listOf(u1, u2),
      selectedUnitUid = u2.uid,
      isCoopMode = true,
      coopTurnOrder = listOf(p1, p2),
      coopTurnIndex = 1,
      activeCoopPlayerId = "p2",
      activeCoopPlayerName = "Commander 2",
      localControlledPlayerId = "p2"
    )
    org.junit.Assert.assertFalse(state.canCommandUnit(u1))
    org.junit.Assert.assertTrue(state.canCommandUnit(u2))

    val snap = com.example.engine.BattleStateSerializer.serializeCoopBattleSnapshot(state, templates)
    val applied = com.example.engine.BattleStateSerializer.applyCoopBattleSnapshot(snap, com.example.viewmodel.BattleUiState())
    org.junit.Assert.assertNotNull(applied)
    assertEquals("p2", applied!!.activeCoopPlayerId)
    assertEquals(2, applied.round)
    assertEquals(2, applied.coopTurnOrder.size)
    assertEquals("Commander 2", applied.activeCoopPlayerName)
  }

  @Test
  fun `coop lobby snapshot preserves room code players point limit and assigned units`() {
    val lobbyState = com.example.model.CoopState(
      inRoom = true,
      roomCode = "739204",
      hostIp = "192.168.1.44",
      pointLimit = 1500,
      spawnAiEnemyIfSameFaction = false
    )
    val json = com.example.engine.BattleStateSerializer.serializeCoopLobbySnapshot(lobbyState)
    val clientApplied = com.example.engine.BattleStateSerializer.applyCoopLobbySnapshot(
      json,
      com.example.model.CoopState(inRoom = false, roomCode = "000000")
    )
    org.junit.Assert.assertNotNull(clientApplied)
    org.junit.Assert.assertTrue(clientApplied!!.inRoom)
    assertEquals("739204", clientApplied.roomCode)
    assertEquals(1500, clientApplied.pointLimit)
    assertEquals(4, clientApplied.players.size)
    assertEquals(lobbyState.assignedUnits.size, clientApplied.assignedUnits.size)
    org.junit.Assert.assertFalse(clientApplied.spawnAiEnemyIfSameFaction)
  }
}
