package com.example.viewmodel

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.BattleRecordEntity
import com.example.data.GameRepository
import com.example.data.SavedConfigEntity
import com.example.engine.AiController
import com.example.engine.AiDecision
import com.example.engine.DefaultCatalog
import com.example.engine.MapGenerator
import com.example.engine.SoundEffects
import com.example.engine.SoundType
import com.example.engine.TacticalRules
import com.example.model.BattleSummary
import com.example.model.BattleUnit
import com.example.model.ChargePreview
import com.example.model.Facing
import com.example.model.Faction
import com.example.model.FloatingTextVfx
import com.example.model.GridPos
import com.example.model.MovePreview
import com.example.model.ProjectileVfx
import com.example.model.RosterSlot
import com.example.model.SectorMap
import com.example.model.TargetPreview
import com.example.model.TerrainType
import com.example.model.TurnSplashData
import com.example.model.UnitArchetype
import com.example.model.UnitTemplate
import com.example.model.WarriorWeaponChoice
import com.example.model.WeaponMode
import com.example.model.WorldPos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.random.Random

@Immutable
data class SetupUiState(
    val playerFaction: Faction = Faction.MARINES,
    val pointLimit: Int = 2000,
    val templates: Map<String, UnitTemplate> = DefaultCatalog.defaultTemplates.associateBy { it.id },
    val marineRoster: List<RosterSlot> = DefaultCatalog.defaultMarineRoster(),
    val xenosRoster: List<RosterSlot> = DefaultCatalog.defaultXenosRoster(),
    val editingTemplateId: String? = null,
    val statusMessage: String? = null
) {
    val marinePoints: Int
        get() = marineRoster.sumOf { templates[it.templateId]?.points ?: 0 }
    val xenosPoints: Int
        get() = xenosRoster.sumOf { templates[it.templateId]?.points ?: 0 }
    val validationWarning: String?
        get() = when {
            marineRoster.isEmpty() -> "Ultramarines require at least 1 squad."
            xenosRoster.isEmpty() -> "Alien Swarm requires at least 1 squad."
            marinePoints > pointLimit -> "Ultramarines exceed $pointLimit pt limit."
            xenosPoints > pointLimit -> "Alien Swarm exceeds $pointLimit pt limit."
            else -> null
        }
    val canDeploy: Boolean get() = validationWarning == null
}

@Immutable
data class BattleUiState(
    val active: Boolean = false,
    val round: Int = 1,
    val activeFaction: Faction = Faction.MARINES,
    val playerFaction: Faction = Faction.MARINES,
    val map: SectorMap = SectorMap(1L, MapGenerator.generateSectorMap(1L)),
    val units: List<BattleUnit> = emptyList(),
    val selectedUnitUid: String? = null,
    val weaponMode: WeaponMode = WeaponMode.MAIN,
    val moveModeActive: Boolean = false,
    val cmStepChoice: Float = 3f,
    val movePreview: MovePreview? = null,
    val reachableTiles: Map<GridPos, Int> = emptyMap(),
    val moveParents: Map<GridPos, GridPos> = emptyMap(),
    val attackableUids: Set<String> = emptySet(),
    val targetPreview: TargetPreview? = null,
    val chargeModeActive: Boolean = false,
    val chargeableUids: Set<String> = emptySet(),
    val chargePreview: ChargePreview? = null,
    val projectiles: List<ProjectileVfx> = emptyList(),
    val floatingTexts: List<FloatingTextVfx> = emptyList(),
    val autoFocusCamera: Boolean = true,
    val focusGridPos: GridPos? = null,
    val centerTrigger: Int = 0,
    val soundEnabled: Boolean = true,
    val isAiThinking: Boolean = false,
    val isAnimating: Boolean = false,
    val turnSplash: TurnSplashData? = null,
    val summary: BattleSummary? = null,
    val unitCardCollapsed: Boolean = false,
    val playerKills: Int = 0,
    val enemyKills: Int = 0
) {
    val isPlayerTurn: Boolean get() = activeFaction == playerFaction && !isAiThinking && summary == null
    val selectedUnit: BattleUnit? get() = units.firstOrNull { it.uid == selectedUnitUid && it.isAlive }
    val marineAliveModels: Int get() = units.filter { it.faction == Faction.MARINES && it.isAlive }.sumOf { it.aliveModels }
    val xenosAliveModels: Int get() = units.filter { it.faction == Faction.XENOS && it.isAlive }.sumOf { it.aliveModels }
}

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: GameRepository
    private val sound = SoundEffects()

    private val _setupState = MutableStateFlow(SetupUiState())
    val setupState: StateFlow<SetupUiState> = _setupState.asStateFlow()

    private val _battleState = MutableStateFlow(BattleUiState())
    val battleState: StateFlow<BattleUiState> = _battleState.asStateFlow()

    private var nextIdCounter = 100L
    private var aiJob: Job? = null
    private var actionJob: Job? = null

    init {
        val dao = AppDatabase.getInstance(application).gameDao()
        repository = GameRepository(dao)
        loadSavedSetup()
    }

    private fun loadSavedSetup() {
        viewModelScope.launch {
            val saved = repository.getSavedConfigOnce() ?: return@launch
            val faction = runCatching { Faction.valueOf(saved.playerFaction) }.getOrDefault(Faction.MARINES)
            val templates = DefaultCatalog.deserializeTemplates(saved.customTemplatesJson)
            val marines = DefaultCatalog.deserializeRoster(saved.marineRosterJson, DefaultCatalog.defaultMarineRoster())
            val xenos = DefaultCatalog.deserializeRoster(saved.xenosRosterJson, DefaultCatalog.defaultXenosRoster())
            _setupState.update {
                it.copy(
                    playerFaction = faction,
                    pointLimit = saved.pointLimit.coerceIn(1000, 5000),
                    templates = templates,
                    marineRoster = marines,
                    xenosRoster = xenos
                )
            }
        }
    }

    // --- SETUP ACTIONS ---
    fun togglePlayerFaction() {
        sound.play(SoundType.CLICK)
        _setupState.update {
            val next = if (it.playerFaction == Faction.MARINES) Faction.XENOS else Faction.MARINES
            it.copy(playerFaction = next, statusMessage = null)
        }
    }

    fun setPointLimit(points: Int) {
        _setupState.update {
            it.copy(pointLimit = points.coerceIn(1000, 5000), statusMessage = null)
        }
    }

    fun addRosterUnit(faction: Faction, templateId: String) {
        sound.play(SoundType.CLICK)
        val slot = RosterSlot("slot_${nextIdCounter++}", templateId)
        _setupState.update { state ->
            if (faction == Faction.MARINES) {
                state.copy(marineRoster = state.marineRoster + slot, statusMessage = null)
            } else {
                state.copy(xenosRoster = state.xenosRoster + slot, statusMessage = null)
            }
        }
    }

    fun removeRosterSlot(faction: Faction, slotId: String) {
        sound.play(SoundType.CLICK)
        _setupState.update { state ->
            if (faction == Faction.MARINES) {
                state.copy(marineRoster = state.marineRoster.filterNot { it.slotId == slotId }, statusMessage = null)
            } else {
                state.copy(xenosRoster = state.xenosRoster.filterNot { it.slotId == slotId }, statusMessage = null)
            }
        }
    }

    fun randomizeRoster(faction: Faction) {
        sound.play(SoundType.CLICK)
        _setupState.update { state ->
            val pool = state.templates.values.filter { it.faction == faction }
            val limit = state.pointLimit
            val chosen = mutableListOf<RosterSlot>()
            var spent = 0
            val rng = Random(System.nanoTime())
            var guard = 0
            while (guard < 40) {
                guard++
                val affordable = pool.filter { spent + it.points <= limit }
                if (affordable.isEmpty()) break
                val pick = affordable[rng.nextInt(affordable.size)]
                chosen.add(RosterSlot("rnd_${nextIdCounter++}", pick.id))
                spent += pick.points
                if (chosen.size >= 8) break
            }
            if (chosen.isEmpty() && pool.isNotEmpty()) {
                val cheapest = pool.minBy { it.points }
                chosen.add(RosterSlot("rnd_${nextIdCounter++}", cheapest.id))
            }
            if (faction == Faction.MARINES) {
                state.copy(marineRoster = chosen, statusMessage = "Randomized Ultramarines ($spent pts)")
            } else {
                state.copy(xenosRoster = chosen, statusMessage = "Randomized Swarm ($spent pts)")
            }
        }
    }

    fun openStatEditor(templateId: String) {
        sound.play(SoundType.CLICK)
        _setupState.update { it.copy(editingTemplateId = templateId) }
    }

    fun closeStatEditor() {
        _setupState.update { it.copy(editingTemplateId = null) }
    }

    fun saveEditedTemplate(updated: UnitTemplate) {
        sound.play(SoundType.CLICK)
        val adjusted = if (updated.archetype == UnitArchetype.WARRIOR && updated.weaponChoice != null) {
            when (updated.weaponChoice) {
                WarriorWeaponChoice.BONESWORDS -> updated.copy(
                    range = 1,
                    mainWeaponName = "Tyranid Boneswords",
                    mainWeaponAssault = false,
                    strength = updated.strength.coerceAtLeast(5),
                    armorPen = updated.armorPen.coerceAtLeast(2),
                    damage = updated.damage.coerceAtLeast(2)
                )
                WarriorWeaponChoice.FLESHBORER -> updated.copy(
                    range = 4,
                    mainWeaponName = "Fleshborer",
                    mainWeaponAssault = true
                )
            }
        } else updated
        _setupState.update { state ->
            state.copy(
                templates = state.templates + (adjusted.id to adjusted),
                editingTemplateId = null,
                statusMessage = "Updated ${adjusted.name} stats"
            )
        }
    }

    fun saveSetupToDatabase() {
        sound.play(SoundType.CLICK)
        val s = _setupState.value
        viewModelScope.launch {
            repository.saveConfig(
                SavedConfigEntity(
                    id = 1,
                    playerFaction = s.playerFaction.name,
                    pointLimit = s.pointLimit,
                    marineRosterJson = DefaultCatalog.serializeRoster(s.marineRoster),
                    xenosRosterJson = DefaultCatalog.serializeRoster(s.xenosRoster),
                    customTemplatesJson = DefaultCatalog.serializeTemplates(s.templates)
                )
            )
            _setupState.update { it.copy(statusMessage = "Saved army configuration!") }
        }
    }

    fun resetSetupToDefaults() {
        sound.play(SoundType.CLICK)
        viewModelScope.launch {
            repository.resetConfig()
            _setupState.value = SetupUiState(statusMessage = "Reset to Crimson Sector defaults")
        }
    }

    fun clearStatusMessage() {
        _setupState.update { it.copy(statusMessage = null) }
    }

    // --- BATTLE LIFECYCLE ---
    fun deployToBattle() {
        val setup = _setupState.value
        if (!setup.canDeploy) return
        aiJob?.cancel()
        actionJob?.cancel()
        sound.play(SoundType.TURN_HORN)

        val mapId = System.currentTimeMillis()
        val rawMap = MapGenerator.generateSectorMap(mapId)
        val map = SectorMap(mapId, rawMap)
        val spawned = mutableListOf<BattleUnit>()

        // Spawn Marines on West edge (x = 1..2), Swarm on East edge (x = 13..14)
        spawnArmy(setup.marineRoster, setup.templates, Faction.MARINES, startCols = listOf(1, 2), Facing.EAST, rawMap, spawned)
        spawnArmy(setup.xenosRoster, setup.templates, Faction.XENOS, startCols = listOf(14, 13), Facing.WEST, rawMap, spawned)

        val unitsWithAuras = TacticalRules.applyAuras(spawned)
        val firstPlayerUnit = unitsWithAuras.firstOrNull { it.faction == setup.playerFaction }

        val initialState = BattleUiState(
            active = true,
            round = 1,
            activeFaction = setup.playerFaction,
            playerFaction = setup.playerFaction,
            map = map,
            units = unitsWithAuras,
            selectedUnitUid = firstPlayerUnit?.uid,
            autoFocusCamera = true,
            focusGridPos = firstPlayerUnit?.pos,
            soundEnabled = sound.enabled,
            turnSplash = TurnSplashData(
                round = 1,
                activeFaction = setup.playerFaction,
                isPlayerTurn = true,
                marineModelsAlive = unitsWithAuras.filter { it.faction == Faction.MARINES }.sumOf { it.aliveModels },
                xenosModelsAlive = unitsWithAuras.filter { it.faction == Faction.XENOS }.sumOf { it.aliveModels },
                playerKills = 0,
                playerLosses = 0
            )
        )
        _battleState.value = recomputeSelectionOverlays(initialState)
        scheduleSplashAutoDismiss()
    }

    private fun spawnArmy(
        roster: List<RosterSlot>,
        templates: Map<String, UnitTemplate>,
        faction: Faction,
        startCols: List<Int>,
        facing: Facing,
        map: Array<Array<TerrainType>>,
        out: MutableList<BattleUnit>
    ) {
        val candidateSpots = mutableListOf<GridPos>()
        val rows = listOf(6, 7, 4, 9, 2, 11, 5, 8, 3, 10, 1, 12)
        for (col in startCols) {
            for (row in rows) {
                if (MapGenerator.isWalkable(map[row][col])) {
                    candidateSpots.add(GridPos(col, row))
                }
            }
        }
        roster.forEachIndexed { index, slot ->
            val tpl = templates[slot.templateId] ?: return@forEachIndexed
            val pos = candidateSpots.getOrElse(index) { GridPos(startCols.first(), (index + 1).coerceIn(1, 12)) }
            out.add(
                BattleUnit(
                    uid = "${faction.name}_${index}_${nextIdCounter++}",
                    template = tpl,
                    faction = faction,
                    x = pos.x,
                    y = pos.y,
                    facing = facing,
                    totalWounds = tpl.maxTotalWounds,
                    maxTotalWounds = tpl.maxTotalWounds,
                    moveCmLeft = tpl.moveCm.toFloat(),
                    worldX = pos.x.toFloat(),
                    worldY = pos.y.toFloat(),
                    facingDeg = facing.yawDegrees()
                )
            )
        }
    }

    fun returnToSetup() {
        aiJob?.cancel()
        actionJob?.cancel()
        sound.play(SoundType.CLICK)
        _battleState.update { it.copy(active = false, summary = null, isAiThinking = false, isAnimating = false) }
    }

    // --- BATTLE INTERACTION ---
    fun onTileTapped(pos: GridPos) {
        onWorldTapped(pos.x.toFloat(), pos.y.toFloat())
    }

    fun onWorldTapped(wx: Float, wy: Float) {
        val state = _battleState.value
        if (!state.isPlayerTurn || state.isAnimating) return

        val pos = GridPos(
            wx.roundToInt().coerceIn(0, MapGenerator.GRID_W - 1),
            wy.roundToInt().coerceIn(0, MapGenerator.GRID_H - 1)
        )

        // 0. Check if Assault Terminator is in Teleport Strike mode
        val selected = state.selectedUnit
        if (selected != null && selected.faction == state.playerFaction &&
            selected.template.archetype == UnitArchetype.TERMINATOR &&
            state.weaponMode == WeaponMode.SKILL
        ) {
            if (pos in state.reachableTiles.keys) {
                actionJob?.cancel()
                actionJob = viewModelScope.launch {
                    performTeleport(selected.uid, pos)
                }
            } else {
                val dist = maxOf(abs(pos.x - selected.x), abs(pos.y - selected.y))
                val enemies = state.units.filter { it.isAlive && it.faction != selected.faction }
                val minEnemy = enemies.minOfOrNull { maxOf(abs(pos.x - it.x), abs(pos.y - it.y)) } ?: 99
                when {
                    dist > 9 -> spawnFloatingText(pos, "MAX RANGE 9", 0xFFFF4D58L)
                    minEnemy <= 4 -> spawnFloatingText(pos, "TOO CLOSE TO ENEMY (MIN 5)", 0xFFFF4D58L)
                    !MapGenerator.isWalkable(state.map.rows[pos.y][pos.x]) -> spawnFloatingText(pos, "BLOCKED TILE", 0xFFFF4D58L)
                    else -> spawnFloatingText(pos, "OCCUPIED TILE", 0xFFFF4D58L)
                }
            }
            return
        }

        // 1. Check if tapped an alive unit (continuous hitbox or tile match)
        val unitHit = state.units
            .filter { it.isAlive }
            .minByOrNull { hypot(it.worldX - wx, it.worldY - wy) }
            ?.takeIf { hypot(it.worldX - wx, it.worldY - wy) < 0.65f || it.pos == pos }

        if (unitHit != null) {
            if (unitHit.faction == state.playerFaction) {
                sound.play(SoundType.CLICK)
                val updated = state.copy(
                    selectedUnitUid = unitHit.uid,
                    weaponMode = WeaponMode.MAIN,
                    moveModeActive = false,
                    movePreview = null,
                    targetPreview = null,
                    chargeModeActive = false,
                    chargePreview = null,
                    focusGridPos = if (state.autoFocusCamera) unitHit.pos else state.focusGridPos
                )
                _battleState.value = recomputeSelectionOverlays(updated)
                return
            } else if (state.chargeModeActive) {
                val sel = state.selectedUnit ?: return
                val cPreview = TacticalRules.findChargeEngagementSpot(sel, unitHit, state.units, state.map.rows)
                if (cPreview != null) {
                    sound.play(SoundType.CLICK)
                    _battleState.update {
                        it.copy(
                            chargePreview = cPreview,
                            movePreview = null,
                            targetPreview = null
                        )
                    }
                } else {
                    val curDist = hypot(sel.worldX - unitHit.worldX, sel.worldY - unitHit.worldY)
                    val msg = when {
                        !sel.canCharge -> if (sel.hasAdvanced) "NO CHARGE AFTER ADVANCE" else "ALREADY CHARGED"
                        curDist <= 1.55f -> "ALREADY IN MELEE"
                        curDist > 12.45f -> "OUT OF 12\" CHARGE"
                        else -> "NO CHARGE PATH"
                    }
                    spawnFloatingText(pos, msg, 0xFFFF4D58L)
                }
                return
            } else if (!state.moveModeActive) {
                val sel = state.selectedUnit ?: return
                val preview = TacticalRules.canTarget(sel, unitHit, state.weaponMode, state.map.rows, state.units)
                if (preview != null && preview.inRange && preview.hasLos) {
                    sound.play(SoundType.CLICK)
                    _battleState.update { it.copy(targetPreview = preview, movePreview = null) }
                } else {
                    val isEngaged = TacticalRules.isEngagedInMelee(sel, state.units)
                    val distToHit = hypot(sel.worldX - unitHit.worldX, sel.worldY - unitHit.worldY)
                    val isMeleeStrike = TacticalRules.effectiveIsMelee(sel, state.weaponMode, distToHit)
                    val reason = when {
                        sel.skillCooldownLeft > 0 && state.weaponMode == WeaponMode.SKILL -> "ON COOLDOWN"
                        sel.pistolCooldownLeft > 0 && state.weaponMode == WeaponMode.PISTOL -> "ON COOLDOWN"
                        isEngaged && !isMeleeStrike && state.weaponMode != WeaponMode.PISTOL -> "IN MELEE: PISTOL ONLY"
                        !TacticalRules.canUseMode(sel, state.weaponMode, state.units) -> when {
                            sel.hasAdvanced && state.weaponMode == WeaponMode.PISTOL -> "NO PISTOL AFTER ADVANCE"
                            sel.hasAdvanced && !sel.template.mainWeaponAssault -> "NO SHOOT AFTER ADVANCE"
                            sel.hasCharged -> "MELEE ONLY AFTER CHARGE"
                            TacticalRules.modeUsesMelee(sel, state.weaponMode) -> "ALREADY FOUGHT"
                            else -> "ALREADY SHOT"
                        }
                        preview == null -> "UNAVAILABLE"
                        !preview.inRange -> "OUT OF RANGE"
                        else -> "NO LOS"
                    }
                    spawnFloatingText(pos, reason, 0xFF9AA3B2L)
                }
                return
            }
        }

        // 2. Ground tap -> Charge mode guard or continuous cm movement preview
        val sel = state.selectedUnit ?: return
        if (sel.faction != state.playerFaction) return

        if (state.chargeModeActive) {
            spawnFloatingText(pos, "TAP ENEMY WITHIN 12\"", 0xFFFFB703L)
            return
        }

        if (sel.moveCmLeft < 0.25f) {
            spawnFloatingText(pos, "0.0 CM LEFT", 0xFF9AA3B2L)
            return
        }

        val clampedX = wx.coerceIn(0f, (MapGenerator.GRID_W - 1).toFloat())
        val clampedY = wy.coerceIn(0f, (MapGenerator.GRID_H - 1).toFloat())
        val preview = TacticalRules.computeCmMovePreview(sel, clampedX, clampedY, state.units, state.map.rows)
        if (preview != null) {
            sound.play(SoundType.CLICK)
            _battleState.update {
                it.copy(
                    movePreview = preview,
                    targetPreview = null
                )
            }
        } else {
            val rawDistCm = hypot(clampedX - sel.worldX, clampedY - sel.worldY) * TacticalRules.CM_PER_TILE
            val msg = if (rawDistCm > sel.moveCmLeft + 0.05f) {
                "OUT OF CM (${"%.1f".format(sel.moveCmLeft)} cm)"
            } else {
                "BLOCKED"
            }
            spawnFloatingText(pos, msg, 0xFFFF4D58L)
        }
    }

    fun toggleMoveMode() {
        val state = _battleState.value
        if (!state.isPlayerTurn || state.isAnimating) return
        if (state.selectedUnit == null) return
        sound.play(SoundType.CLICK)
        _battleState.update { st ->
            val next = !st.moveModeActive
            recomputeSelectionOverlays(
                st.copy(
                    moveModeActive = next,
                    movePreview = if (next) st.movePreview else null,
                    chargeModeActive = false,
                    chargePreview = null,
                    targetPreview = null
                )
            )
        }
    }

    fun advanceMove() {
        val state = _battleState.value
        if (!state.isPlayerTurn || state.isAnimating) return
        val selected = state.selectedUnit ?: return
        if (selected.faction != state.playerFaction || !selected.canAdvance) {
            spawnFloatingText(selected.pos, "CANNOT ADVANCE", 0xFFFF4D58L)
            return
        }
        sound.play(SoundType.CLICK)
        val (roll, bonusCm) = TacticalRules.rollAdvanceBonus()
        val updatedUnits = state.units.map { u ->
            if (u.uid != selected.uid) u
            else u.copy(
                moveCmLeft = u.moveCmLeft + bonusCm,
                hasAdvanced = true,
                activeBuffLabel = if (u.template.mainWeaponAssault) {
                    "ADVANCE +$roll\" (ASSAULT)"
                } else {
                    "ADVANCE +$roll\" (NO SHOOT/CHRG)"
                }
            )
        }
        spawnFloatingText(selected.pos, "ADVANCE +$roll\" (+${bonusCm.toInt()} cm)", 0xFF38D96CL)
        val nextState = state.copy(
            units = updatedUnits,
            weaponMode = WeaponMode.MAIN,
            moveModeActive = true,
            chargeModeActive = false,
            chargePreview = null,
            movePreview = null,
            targetPreview = null
        )
        _battleState.value = recomputeSelectionOverlays(nextState)
    }

    fun toggleChargeMode() {
        val state = _battleState.value
        if (!state.isPlayerTurn || state.isAnimating) return
        val selected = state.selectedUnit ?: return
        if (!selected.canCharge) {
            val msg = when {
                selected.hasAdvanced -> "NO CHARGE AFTER ADVANCE"
                selected.hasCharged -> "ALREADY CHARGED"
                else -> "ALREADY FOUGHT"
            }
            spawnFloatingText(selected.pos, msg, 0xFFFF4D58L)
            return
        }
        sound.play(SoundType.CLICK)
        val next = !state.chargeModeActive
        val nextState = state.copy(
            chargeModeActive = next,
            chargePreview = null,
            moveModeActive = false,
            movePreview = null,
            targetPreview = null,
            weaponMode = WeaponMode.MAIN
        )
        _battleState.value = recomputeSelectionOverlays(nextState)
    }

    fun cancelChargePreview() {
        sound.play(SoundType.CLICK)
        _battleState.update { it.copy(chargePreview = null) }
    }

    fun confirmChargeRoll() {
        val state = _battleState.value
        if (!state.isPlayerTurn || state.isAnimating) return
        val selected = state.selectedUnit ?: return
        val preview = state.chargePreview ?: return
        actionJob?.cancel()
        actionJob = viewModelScope.launch {
            executeChargeAttempt(selected.uid, preview)
        }
    }

    private suspend fun executeChargeAttempt(unitUid: String, preview: ChargePreview) {
        val state = _battleState.value
        val charger = state.units.firstOrNull { it.uid == unitUid && it.isAlive } ?: return
        val d1 = Random.nextInt(1, 7)
        val d2 = Random.nextInt(1, 7)
        val total = d1 + d2

        if (total >= preview.requiredRoll) {
            sound.play(SoundType.MELEE)
            spawnFloatingText(charger.pos, "CHARGE $d1+$d2=$total! (${preview.requiredRoll}+)", 0xFFFFD166L)
            performChargeDash(unitUid, preview)
        } else {
            sound.play(SoundType.CLICK)
            spawnFloatingText(charger.pos, "CHARGE FAIL $d1+$d2=$total (${preview.requiredRoll}+)", 0xFFFF4D58L)
            val updatedUnits = state.units.map { u ->
                if (u.uid != unitUid) u else u.copy(hasCharged = true, moveCmLeft = 0f)
            }
            _battleState.value = recomputeSelectionOverlays(
                state.copy(
                    units = updatedUnits,
                    chargeModeActive = false,
                    chargePreview = null,
                    chargeableUids = emptySet()
                )
            )
        }
    }

    private suspend fun performChargeDash(unitUid: String, preview: ChargePreview) {
        val stateBefore = _battleState.value
        val unit = stateBefore.units.firstOrNull { it.uid == unitUid && it.isAlive } ?: return
        val target = stateBefore.units.firstOrNull { it.uid == preview.targetUid && it.isAlive }
        if (preview.path.isEmpty()) return

        val destGrid = GridPos(
            preview.destinationX.roundToInt().coerceIn(0, MapGenerator.GRID_W - 1),
            preview.destinationY.roundToInt().coerceIn(0, MapGenerator.GRID_H - 1)
        )

        _battleState.update { state ->
            val updatedUnits = state.units.map { u ->
                if (u.uid != unitUid) u
                else u.copy(
                    hasCharged = true,
                    moveCmLeft = 0f,
                    activeBuffLabel = "CHARGED"
                )
            }
            state.copy(
                units = updatedUnits,
                weaponMode = WeaponMode.MAIN,
                moveModeActive = false,
                movePreview = null,
                chargeModeActive = false,
                chargePreview = null,
                chargeableUids = emptySet(),
                reachableTiles = emptyMap(),
                moveParents = emptyMap(),
                attackableUids = emptySet(),
                targetPreview = null,
                isAnimating = true
            )
        }

        try {
            var curX = unit.worldX
            var curY = unit.worldY
            for (wp in preview.path) {
                val segLen = hypot(wp.x - curX, wp.y - curY)
                if (segLen < 0.01f) continue
                val subSteps = (segLen / 0.35f).roundToInt().coerceAtLeast(1)
                val segDeg = TacticalRules.angleDegToward(curX, curY, wp.x, wp.y)
                val segFacing = TacticalRules.nearestFacingFromDeg(segDeg)
                for (s in 1..subSteps) {
                    delay(48L)
                    val t = s / subSteps.toFloat()
                    val nx = curX + (wp.x - curX) * t
                    val ny = curY + (wp.y - curY) * t
                    val gx = nx.roundToInt().coerceIn(0, MapGenerator.GRID_W - 1)
                    val gy = ny.roundToInt().coerceIn(0, MapGenerator.GRID_H - 1)
                    _battleState.update { st ->
                        val updatedUnits = st.units.map { u ->
                            if (u.uid != unitUid) u
                            else u.copy(
                                worldX = nx,
                                worldY = ny,
                                x = gx,
                                y = gy,
                                facing = segFacing,
                                facingDeg = segDeg
                            )
                        }
                        st.copy(
                            units = updatedUnits,
                            focusGridPos = if (st.autoFocusCamera) GridPos(gx, gy) else st.focusGridPos
                        )
                    }
                }
                curX = wp.x
                curY = wp.y
            }

            val finalFacingDeg = if (target != null) {
                TacticalRules.angleDegToward(preview.destinationX, preview.destinationY, target.worldX, target.worldY)
            } else unit.facingDeg
            val finalFacing = TacticalRules.nearestFacingFromDeg(finalFacingDeg)

            _battleState.update { state ->
                val alignedUnits = state.units.map { u ->
                    if (u.uid != unitUid) u
                    else u.copy(
                        x = destGrid.x,
                        y = destGrid.y,
                        worldX = preview.destinationX,
                        worldY = preview.destinationY,
                        facing = finalFacing,
                        facingDeg = finalFacingDeg
                    )
                }
                val withAuras = TacticalRules.applyAuras(alignedUnits)
                val updatedUnit = withAuras.firstOrNull { it.uid == unitUid }
                val updatedTarget = target?.let { t -> withAuras.firstOrNull { it.uid == t.uid && it.isAlive } }
                val autoTargetPreview = if (state.isPlayerTurn && updatedUnit != null && updatedTarget != null) {
                    TacticalRules.canTarget(updatedUnit, updatedTarget, WeaponMode.MAIN, state.map.rows, withAuras)
                        ?.takeIf { it.inRange }
                } else null

                recomputeSelectionOverlays(
                    state.copy(
                        units = withAuras,
                        isAnimating = false,
                        targetPreview = autoTargetPreview,
                        focusGridPos = if (state.autoFocusCamera) destGrid else state.focusGridPos
                    )
                )
            }
        } finally {
            _battleState.update { st ->
                if (st.isAnimating) recomputeSelectionOverlays(st.copy(isAnimating = false)) else st
            }
        }
    }

    fun setCmStep(stepCm: Float) {
        sound.play(SoundType.CLICK)
        _battleState.update { it.copy(cmStepChoice = stepCm) }
    }

    fun walkForwardCm() {
        val state = _battleState.value
        if (!state.isPlayerTurn || state.isAnimating) return
        val selected = state.selectedUnit ?: return
        if (selected.faction != state.playerFaction) return
        val requested = if (state.cmStepChoice <= 0f) selected.moveCmLeft else state.cmStepChoice
        val preview = TacticalRules.computeForwardCmPreview(selected, requested, state.units, state.map.rows)
        if (preview == null) {
            spawnFloatingText(
                selected.pos,
                if (selected.moveCmLeft < 0.2f) "0.0 CM LEFT" else "PATH BLOCKED",
                0xFFFF4D58L
            )
            return
        }
        actionJob?.cancel()
        actionJob = viewModelScope.launch {
            performContinuousMove(selected.uid, preview)
        }
    }

    fun confirmMovePreview() {
        val state = _battleState.value
        if (!state.isPlayerTurn || state.isAnimating) return
        val selected = state.selectedUnit ?: return
        val preview = state.movePreview ?: return
        actionJob?.cancel()
        actionJob = viewModelScope.launch {
            performContinuousMove(selected.uid, preview)
        }
    }

    fun cancelMovePreview() {
        sound.play(SoundType.CLICK)
        _battleState.update { it.copy(movePreview = null) }
    }

    private suspend fun performTeleport(unitUid: String, dest: GridPos) {
        val state = _battleState.value
        val unit = state.units.firstOrNull { it.uid == unitUid && it.isAlive } ?: return
        sound.play(SoundType.PLASMA)

        val startPos = unit.pos
        val pathPoints = TacticalRules.computeLinePath(startPos.x, startPos.y, dest.x, dest.y)

        val initialUpdatedUnits = state.units.map { u ->
            if (u.uid != unitUid) u
            else u.copy(
                moveCmLeft = 0f,
                skillCooldownLeft = u.template.skillCooldown
            )
        }
        val vfx = ProjectileVfx(
            id = nextIdCounter++,
            fromX = unit.worldX,
            fromY = unit.worldY,
            toX = dest.x.toFloat(),
            toY = dest.y.toFloat(),
            colorArgb = 0xFF4CC9F0L,
            isBeam = true,
            isMelee = false,
            startTimeMs = System.currentTimeMillis()
        )

        _battleState.value = state.copy(
            units = initialUpdatedUnits,
            weaponMode = WeaponMode.MAIN,
            moveModeActive = false,
            movePreview = null,
            reachableTiles = emptyMap(),
            moveParents = emptyMap(),
            attackableUids = emptySet(),
            targetPreview = null,
            isAnimating = true,
            projectiles = state.projectiles + vfx
        )
        spawnFloatingText(startPos, "WARP OUT", 0xFF64DFDFL)

        try {
            for (i in 1 until pathPoints.size) {
                val pt = pathPoints[i]
                delay(120L)
                _battleState.update { st ->
                    val currentUnits = st.units.map { u ->
                        if (u.uid != unitUid) u
                        else {
                            val nextFacing = TacticalRules.facingToward(u.pos, pt)
                            val nextDeg = TacticalRules.angleDegToward(u.worldX, u.worldY, pt.x.toFloat(), pt.y.toFloat())
                            u.copy(
                                x = pt.x,
                                y = pt.y,
                                worldX = pt.x.toFloat(),
                                worldY = pt.y.toFloat(),
                                facing = nextFacing,
                                facingDeg = nextDeg
                            )
                        }
                    }
                    st.copy(
                        units = currentUnits,
                        focusGridPos = if (st.autoFocusCamera) pt else st.focusGridPos
                    )
                }
            }

            sound.play(SoundType.PLASMA)
            spawnFloatingText(dest, "TELEPORT STRIKE!", 0xFF48CAE4L)
            spawnFloatingText(dest, "SHIELD -50% DMG", 0xFFFFD166L)

            val finalUnits = _battleState.value.units.map { u ->
                if (u.uid != unitUid) u
                else u.copy(
                    x = dest.x,
                    y = dest.y,
                    worldX = dest.x.toFloat(),
                    worldY = dest.y.toFloat(),
                    damageReductionTurns = 1,
                    buffTurnsLeft = 1,
                    activeBuffLabel = "SHIELD -50% DMG"
                )
            }
            val withAuras = TacticalRules.applyAuras(finalUnits)
            _battleState.value = recomputeSelectionOverlays(
                _battleState.value.copy(
                    units = withAuras,
                    isAnimating = false,
                    focusGridPos = if (_battleState.value.autoFocusCamera) dest else _battleState.value.focusGridPos
                )
            )
            scheduleVfxCleanup()
        } finally {
            _battleState.update { st ->
                if (st.isAnimating) recomputeSelectionOverlays(st.copy(isAnimating = false)) else st
            }
        }
    }

    private suspend fun performContinuousMove(unitUid: String, preview: MovePreview) {
        val stateBefore = _battleState.value
        val unit = stateBefore.units.firstOrNull { it.uid == unitUid && it.isAlive } ?: return
        if (preview.path.isEmpty()) return
        sound.play(SoundType.CLICK)

        val destGrid = GridPos(
            preview.targetX.roundToInt().coerceIn(0, MapGenerator.GRID_W - 1),
            preview.targetY.roundToInt().coerceIn(0, MapGenerator.GRID_H - 1)
        )

        _battleState.update { state ->
            val updatedUnits = state.units.map { u ->
                if (u.uid != unitUid) u
                else u.copy(moveCmLeft = (u.moveCmLeft - preview.distanceCm).coerceAtLeast(0f))
            }
            state.copy(
                units = updatedUnits,
                movePreview = null,
                reachableTiles = emptyMap(),
                moveParents = emptyMap(),
                attackableUids = emptySet(),
                targetPreview = null,
                isAnimating = true
            )
        }

        try {
            var curX = unit.worldX
            var curY = unit.worldY
            for (wp in preview.path) {
                val segLen = hypot(wp.x - curX, wp.y - curY)
                if (segLen < 0.01f) continue
                val subSteps = (segLen / 0.25f).roundToInt().coerceAtLeast(1)
                val segDeg = TacticalRules.angleDegToward(curX, curY, wp.x, wp.y)
                val segFacing = TacticalRules.nearestFacingFromDeg(segDeg)
                for (s in 1..subSteps) {
                    delay(75L)
                    val t = s / subSteps.toFloat()
                    val nx = curX + (wp.x - curX) * t
                    val ny = curY + (wp.y - curY) * t
                    val gx = nx.roundToInt().coerceIn(0, MapGenerator.GRID_W - 1)
                    val gy = ny.roundToInt().coerceIn(0, MapGenerator.GRID_H - 1)
                    _battleState.update { st ->
                        val updatedUnits = st.units.map { u ->
                            if (u.uid != unitUid) u
                            else u.copy(
                                worldX = nx,
                                worldY = ny,
                                x = gx,
                                y = gy,
                                facing = segFacing,
                                facingDeg = segDeg
                            )
                        }
                        st.copy(
                            units = updatedUnits,
                            focusGridPos = if (st.autoFocusCamera) GridPos(gx, gy) else st.focusGridPos
                        )
                    }
                }
                curX = wp.x
                curY = wp.y
            }

            _battleState.update { state ->
                val withAuras = TacticalRules.applyAuras(state.units)
                recomputeSelectionOverlays(
                    state.copy(
                        units = withAuras,
                        isAnimating = false,
                        focusGridPos = if (state.autoFocusCamera) destGrid else state.focusGridPos
                    )
                )
            }
        } finally {
            _battleState.update { st ->
                if (st.isAnimating) recomputeSelectionOverlays(st.copy(isAnimating = false)) else st
            }
        }
    }

    private suspend fun performMoveAlongPath(unitUid: String, path: List<GridPos>, totalCost: Int) {
        if (path.isEmpty()) return
        sound.play(SoundType.CLICK)
        val dest = path.last()
        val cmCost = totalCost * TacticalRules.CM_PER_TILE

        _battleState.update { state ->
            val updatedUnits = state.units.map { u ->
                if (u.uid != unitUid) u
                else u.copy(moveCmLeft = (u.moveCmLeft - cmCost).coerceAtLeast(0f))
            }
            state.copy(
                units = updatedUnits,
                reachableTiles = emptyMap(),
                moveParents = emptyMap(),
                attackableUids = emptySet(),
                targetPreview = null,
                isAnimating = true
            )
        }

        try {
            for (step in path) {
                delay(220L)
                _battleState.update { state ->
                    val updatedUnits = state.units.map { u ->
                        if (u.uid != unitUid) u
                        else {
                            val nextDeg = TacticalRules.angleDegToward(u.worldX, u.worldY, step.x.toFloat(), step.y.toFloat())
                            val nextFacing = TacticalRules.nearestFacingFromDeg(nextDeg)
                            u.copy(
                                x = step.x,
                                y = step.y,
                                worldX = step.x.toFloat(),
                                worldY = step.y.toFloat(),
                                facing = nextFacing,
                                facingDeg = nextDeg
                            )
                        }
                    }
                    state.copy(
                        units = updatedUnits,
                        focusGridPos = if (state.autoFocusCamera) step else state.focusGridPos
                    )
                }
            }

            _battleState.update { state ->
                val withAuras = TacticalRules.applyAuras(state.units)
                recomputeSelectionOverlays(
                    state.copy(
                        units = withAuras,
                        isAnimating = false,
                        focusGridPos = if (state.autoFocusCamera) dest else state.focusGridPos
                    )
                )
            }
        } finally {
            _battleState.update { st ->
                if (st.isAnimating) recomputeSelectionOverlays(st.copy(isAnimating = false)) else st
            }
        }
    }

    fun confirmStrike() {
        val state = _battleState.value
        if (!state.isPlayerTurn || state.isAnimating) return
        val attacker = state.selectedUnit ?: return
        val preview = state.targetPreview ?: return
        val target = state.units.firstOrNull { it.uid == preview.targetUid && it.isAlive } ?: return
        viewModelScope.launch {
            executeAttack(attacker.uid, target.uid, preview.weaponMode)
        }
    }

    private suspend fun executeAttack(attackerUid: String, targetUid: String, mode: WeaponMode) {
        val state = _battleState.value
        val attacker = state.units.firstOrNull { it.uid == attackerUid && it.isAlive } ?: return
        val target = state.units.firstOrNull { it.uid == targetUid && it.isAlive } ?: return

        val distTiles = hypot(attacker.worldX - target.worldX, attacker.worldY - target.worldY)
        val isMelee = TacticalRules.effectiveIsMelee(attacker, mode, distTiles)
        sound.play(
            when {
                isMelee -> SoundType.MELEE
                mode == WeaponMode.SKILL || attacker.template.archetype == UnitArchetype.HELLBLASTER -> SoundType.PLASMA
                else -> SoundType.BOLTER
            }
        )

        val outcome = TacticalRules.resolveAttack(attacker, target, state.units, mode, state.map.rows)
        val now = System.currentTimeMillis()

        val projColor = when {
            attacker.faction == Faction.MARINES && (mode == WeaponMode.SKILL || attacker.template.archetype == UnitArchetype.HELLBLASTER) -> 0xFF32C5FFL
            attacker.faction == Faction.MARINES -> 0xFFFFD166L
            else -> 0xFFC77DFFL
        }
        val proj = ProjectileVfx(
            id = nextIdCounter++,
            fromX = attacker.worldX,
            fromY = attacker.worldY,
            toX = target.worldX,
            toY = target.worldY,
            colorArgb = projColor,
            isBeam = mode == WeaponMode.SKILL || attacker.template.archetype == UnitArchetype.HELLBLASTER,
            isMelee = isMelee,
            startTimeMs = now
        )
        val floats = outcome.floatingTexts.mapIndexed { idx, pair ->
            FloatingTextVfx(
                id = nextIdCounter++,
                gridX = pair.first.x.toFloat(),
                gridY = pair.first.y.toFloat() - idx * 0.35f,
                text = pair.second.first,
                colorArgb = pair.second.second,
                startTimeMs = now + 450L + idx * 180L
            )
        }

        val targetMap = outcome.updatedTargets.associateBy { it.uid }
        val newUnits = TacticalRules.applyAuras(
            state.units.map { u ->
                when (u.uid) {
                    outcome.updatedAttacker.uid -> outcome.updatedAttacker
                    in targetMap -> targetMap.getValue(u.uid)
                    else -> u
                }
            }
        )

        val addedPlayerKills = if (attacker.faction == state.playerFaction) outcome.killsCount else 0
        val addedEnemyKills = if (attacker.faction != state.playerFaction) outcome.killsCount else 0

        val survivingDefender = newUnits.firstOrNull { it.uid == target.uid }
        val survivingAttacker = newUnits.firstOrNull { it.uid == attacker.uid }
        val willCounterAttack = isMelee &&
            survivingDefender != null && survivingDefender.isAlive && survivingDefender.aliveModels > 0 &&
            survivingAttacker != null && survivingAttacker.isAlive &&
            hypot(survivingDefender.worldX - survivingAttacker.worldX, survivingDefender.worldY - survivingAttacker.worldY) <= 1.55f

        val updated = state.copy(
            units = newUnits,
            weaponMode = WeaponMode.MAIN,
            moveModeActive = false,
            movePreview = null,
            chargeModeActive = false,
            chargePreview = null,
            chargeableUids = emptySet(),
            targetPreview = null,
            isAnimating = willCounterAttack,
            projectiles = (state.projectiles + proj).filter { now - it.startTimeMs < 1800 },
            floatingTexts = (state.floatingTexts + floats).filter { now - it.startTimeMs < 3200 },
            focusGridPos = if (state.autoFocusCamera) target.pos else state.focusGridPos,
            playerKills = state.playerKills + addedPlayerKills,
            enemyKills = state.enemyKills + addedEnemyKills
        )
        _battleState.value = checkVictoryOrUpdate(recomputeSelectionOverlays(updated))
        scheduleVfxCleanup()

        if (willCounterAttack && survivingDefender != null && survivingAttacker != null) {
            spawnFloatingText(survivingDefender.pos, "⚔️ COUNTER-ATTACK (2s)", 0xFFFFD166L)
            delay(2000L)

            val currentState = _battleState.value
            val liveDefender = currentState.units.firstOrNull { it.uid == survivingDefender.uid && it.isAlive }
            val liveAttacker = currentState.units.firstOrNull { it.uid == survivingAttacker.uid && it.isAlive }

            if (liveDefender != null && liveAttacker != null && liveDefender.aliveModels > 0) {
                sound.play(SoundType.MELEE)
                val counterOutcome = TacticalRules.resolveMeleeCounterAttack(
                    liveDefender,
                    liveAttacker,
                    currentState.map.rows
                )
                if (counterOutcome != null) {
                    val counterNow = System.currentTimeMillis()
                    val counterProjColor = if (liveDefender.faction == Faction.MARINES) 0xFFFFD166L else 0xFFC77DFFL
                    val counterProj = ProjectileVfx(
                        id = nextIdCounter++,
                        fromX = liveDefender.worldX,
                        fromY = liveDefender.worldY,
                        toX = liveAttacker.worldX,
                        toY = liveAttacker.worldY,
                        colorArgb = counterProjColor,
                        isBeam = false,
                        isMelee = true,
                        startTimeMs = counterNow
                    )
                    val counterFloats = counterOutcome.floatingTexts.mapIndexed { idx, pair ->
                        FloatingTextVfx(
                            id = nextIdCounter++,
                            gridX = pair.first.x.toFloat(),
                            gridY = pair.first.y.toFloat() - idx * 0.35f,
                            text = pair.second.first,
                            colorArgb = pair.second.second,
                            startTimeMs = counterNow + 250L + idx * 180L
                        )
                    }

                    val afterCounterUnits = TacticalRules.applyAuras(
                        currentState.units.map { u ->
                            when (u.uid) {
                                counterOutcome.updatedDefender.uid -> counterOutcome.updatedDefender
                                counterOutcome.updatedAttacker.uid -> counterOutcome.updatedAttacker
                                else -> u
                            }
                        }
                    )

                    val counterKillsPlayer = if (liveDefender.faction == currentState.playerFaction && counterOutcome.updatedAttacker.totalWounds == 0) 1 else 0
                    val counterKillsEnemy = if (liveDefender.faction != currentState.playerFaction && counterOutcome.updatedAttacker.totalWounds == 0) 1 else 0

                    val stateAfterCounter = currentState.copy(
                        units = afterCounterUnits,
                        isAnimating = false,
                        projectiles = (currentState.projectiles + counterProj).filter { counterNow - it.startTimeMs < 1800 },
                        floatingTexts = (currentState.floatingTexts + counterFloats).filter { counterNow - it.startTimeMs < 3200 },
                        playerKills = currentState.playerKills + counterKillsPlayer,
                        enemyKills = currentState.enemyKills + counterKillsEnemy
                    )
                    _battleState.value = checkVictoryOrUpdate(recomputeSelectionOverlays(stateAfterCounter))
                    scheduleVfxCleanup()
                    delay(500L)
                } else {
                    _battleState.update { it.copy(isAnimating = false) }
                }
            } else {
                _battleState.update { it.copy(isAnimating = false) }
            }
        }
    }

    fun selectWeaponMode(mode: WeaponMode) {
        val state = _battleState.value
        if (!state.isPlayerTurn || state.isAnimating) return
        val selected = state.selectedUnit ?: return
        sound.play(SoundType.CLICK)

        if (mode == WeaponMode.SKILL) {
            val selfUpdated = TacticalRules.activateSelfSkillIfApplicable(selected)
            if (selfUpdated != null) {
                val units = TacticalRules.applyAuras(state.units.map { if (it.uid == selfUpdated.uid) selfUpdated else it })
                spawnFloatingText(selected.pos, "BOUNDING LEAP!", 0xFF38D96CL)
                _battleState.value = recomputeSelectionOverlays(
                    state.copy(units = units, weaponMode = WeaponMode.MAIN, movePreview = null, targetPreview = null)
                )
                return
            }
        }

        val nextMode = if (state.weaponMode == mode) WeaponMode.MAIN else mode
        _battleState.value = recomputeSelectionOverlays(
            state.copy(
                weaponMode = nextMode,
                moveModeActive = false,
                movePreview = null,
                chargeModeActive = false,
                chargePreview = null,
                targetPreview = null
            )
        )
    }

    fun rotateSelectedUnit(left: Boolean) {
        val state = _battleState.value
        if (!state.isPlayerTurn || state.isAnimating) return
        val selected = state.selectedUnit ?: return
        sound.play(SoundType.CLICK)
        val delta = if (left) 45f else -45f
        val nextDeg = ((selected.facingDeg + delta) % 360f + 360f) % 360f
        val nextFacing = TacticalRules.nearestFacingFromDeg(nextDeg)
        val units = state.units.map {
            if (it.uid == selected.uid) it.copy(facing = nextFacing, facingDeg = nextDeg) else it
        }
        _battleState.value = state.copy(units = units, movePreview = null)
    }

    fun toggleAutoFocus() {
        sound.play(SoundType.CLICK)
        _battleState.update { it.copy(autoFocusCamera = !it.autoFocusCamera) }
    }

    fun centerCamera() {
        sound.play(SoundType.CLICK)
        _battleState.update { state ->
            val target = state.selectedUnit?.pos ?: GridPos(MapGenerator.GRID_W / 2, MapGenerator.GRID_H / 2)
            state.copy(focusGridPos = target, centerTrigger = state.centerTrigger + 1)
        }
    }

    fun toggleSound() {
        sound.enabled = !sound.enabled
        if (sound.enabled) sound.play(SoundType.CLICK)
        _battleState.update { it.copy(soundEnabled = sound.enabled) }
    }

    fun toggleUnitCardCollapsed() {
        _battleState.update { it.copy(unitCardCollapsed = !it.unitCardCollapsed) }
    }

    fun dismissTurnSplash() {
        _battleState.update { it.copy(turnSplash = null) }
    }

    fun endPlayerTurn() {
        val state = _battleState.value
        if (!state.isPlayerTurn || state.isAnimating) return
        sound.play(SoundType.TURN_HORN)

        val aiFaction = if (state.playerFaction == Faction.MARINES) Faction.XENOS else Faction.MARINES
        val refreshedUnits = refreshFactionForNewTurn(state.units, aiFaction)
        val firstAi = refreshedUnits.firstOrNull { it.faction == aiFaction && it.isAlive }

        val updated = state.copy(
            activeFaction = aiFaction,
            units = TacticalRules.applyAuras(refreshedUnits),
            selectedUnitUid = firstAi?.uid,
            weaponMode = WeaponMode.MAIN,
            moveModeActive = false,
            movePreview = null,
            chargeModeActive = false,
            chargePreview = null,
            chargeableUids = emptySet(),
            reachableTiles = emptyMap(),
            attackableUids = emptySet(),
            targetPreview = null,
            isAiThinking = true,
            focusGridPos = if (state.autoFocusCamera) firstAi?.pos else state.focusGridPos,
            turnSplash = TurnSplashData(
                round = state.round,
                activeFaction = aiFaction,
                isPlayerTurn = false,
                marineModelsAlive = refreshedUnits.filter { it.faction == Faction.MARINES && it.isAlive }.sumOf { it.aliveModels },
                xenosModelsAlive = refreshedUnits.filter { it.faction == Faction.XENOS && it.isAlive }.sumOf { it.aliveModels },
                playerKills = state.playerKills,
                playerLosses = state.enemyKills
            )
        )
        _battleState.value = updated
        scheduleSplashAutoDismiss()
        runAiTurnLoop(aiFaction)
    }

    private fun runAiTurnLoop(aiFaction: Faction) {
        aiJob?.cancel()
        aiJob = viewModelScope.launch {
            delay(1700)
            var guard = 0
            while (guard < 40) {
                guard++
                val current = _battleState.value
                if (!current.active || current.summary != null || current.activeFaction != aiFaction) break

                val decision = withContext(Dispatchers.Default) {
                    AiController.decideNextAction(aiFaction, current.units, current.map.rows)
                }
                when (decision) {
                    is AiDecision.SelfSkill -> {
                        val units = current.units.map {
                            if (it.uid == decision.unitUid) TacticalRules.activateSelfSkillIfApplicable(it) ?: it else it
                        }
                        val u = units.firstOrNull { it.uid == decision.unitUid }
                        if (u != null) spawnFloatingText(u.pos, "LEAP!", 0xFFC77DFFL)
                        _battleState.update { it.copy(units = units, focusGridPos = if (it.autoFocusCamera) u?.pos else it.focusGridPos) }
                        delay(840)
                    }
                    is AiDecision.Teleport -> {
                        performTeleport(decision.unitUid, decision.targetPos)
                        delay(900)
                    }
                    is AiDecision.Advance -> {
                        val (roll, bonusCm) = TacticalRules.rollAdvanceBonus()
                        val units = current.units.map { u ->
                            if (u.uid != decision.unitUid) u
                            else u.copy(
                                moveCmLeft = u.moveCmLeft + bonusCm,
                                hasAdvanced = true,
                                activeBuffLabel = if (u.template.mainWeaponAssault) "ADVANCE +$roll\" (ASSAULT)" else "ADVANCE +$roll\""
                            )
                        }
                        val u = units.firstOrNull { it.uid == decision.unitUid }
                        if (u != null) spawnFloatingText(u.pos, "ADVANCE +$roll\"", 0xFF38D96CL)
                        _battleState.update { it.copy(units = units, focusGridPos = if (it.autoFocusCamera) u?.pos else it.focusGridPos) }
                        delay(650)
                    }
                    is AiDecision.Charge -> {
                        executeChargeAttempt(decision.unitUid, decision.preview)
                        delay(850)
                    }
                    is AiDecision.Move -> {
                        performMoveAlongPath(decision.unitUid, decision.path, decision.cost)
                        delay(700)
                    }
                    is AiDecision.Attack -> {
                        executeAttack(decision.attackerUid, decision.targetUid, decision.mode)
                        delay(1300)
                    }
                    AiDecision.TurnComplete -> break
                }
            }

            // Transition back to Player Turn & increment Round
            val endState = _battleState.value
            if (endState.active && endState.summary == null) {
                sound.play(SoundType.TURN_HORN)
                val nextRound = endState.round + 1
                val refreshed = TacticalRules.applyAuras(refreshFactionForNewTurn(endState.units, endState.playerFaction))
                val firstPlayer = refreshed.firstOrNull { it.faction == endState.playerFaction && it.isAlive }
                val nextState = endState.copy(
                    round = nextRound,
                    activeFaction = endState.playerFaction,
                    units = refreshed,
                    selectedUnitUid = firstPlayer?.uid,
                    weaponMode = WeaponMode.MAIN,
                    moveModeActive = false,
                    movePreview = null,
                    chargeModeActive = false,
                    chargePreview = null,
                    chargeableUids = emptySet(),
                    isAiThinking = false,
                    focusGridPos = if (endState.autoFocusCamera) firstPlayer?.pos else endState.focusGridPos,
                    turnSplash = TurnSplashData(
                        round = nextRound,
                        activeFaction = endState.playerFaction,
                        isPlayerTurn = true,
                        marineModelsAlive = refreshed.filter { it.faction == Faction.MARINES && it.isAlive }.sumOf { it.aliveModels },
                        xenosModelsAlive = refreshed.filter { it.faction == Faction.XENOS && it.isAlive }.sumOf { it.aliveModels },
                        playerKills = endState.playerKills,
                        playerLosses = endState.enemyKills
                    )
                )
                _battleState.value = recomputeSelectionOverlays(nextState)
                scheduleSplashAutoDismiss()
            }
        }
    }

    private fun refreshFactionForNewTurn(units: List<BattleUnit>, faction: Faction): List<BattleUnit> =
        units.map { u ->
            if (!u.isAlive || u.faction != faction) u
            else {
                val nextBuffTurns = (u.buffTurnsLeft - 1).coerceAtLeast(0)
                val nextDamageReduction = (u.damageReductionTurns - 1).coerceAtLeast(0)
                u.copy(
                    moveCmLeft = u.template.moveCm.toFloat(),
                    hasAdvanced = false,
                    hasCharged = false,
                    hasShot = false,
                    hasFought = false,
                    skillCooldownLeft = (u.skillCooldownLeft - 1).coerceAtLeast(0),
                    pistolCooldownLeft = (u.pistolCooldownLeft - 1).coerceAtLeast(0),
                    buffTurnsLeft = nextBuffTurns,
                    damageReductionTurns = nextDamageReduction,
                    activeBuffLabel = if (nextBuffTurns > 0 || nextDamageReduction > 0) u.activeBuffLabel else null
                )
            }
        }

    private fun recomputeSelectionOverlays(state: BattleUiState): BattleUiState {
        val selected = state.selectedUnit
        if (selected == null || !state.isPlayerTurn || state.isAnimating || selected.faction != state.playerFaction) {
            return state.copy(
                reachableTiles = emptyMap(),
                moveParents = emptyMap(),
                attackableUids = emptySet(),
                chargeableUids = emptySet()
            )
        }

        if (selected.template.archetype == UnitArchetype.TERMINATOR && state.weaponMode == WeaponMode.SKILL) {
            val canTeleport = selected.skillCooldownLeft == 0 && !selected.hasAdvanced && !selected.hasCharged
            val teleportTiles = if (canTeleport) {
                TacticalRules.computeTeleportTiles(selected, state.map.rows, state.units, maxDist = 9, minEnemyDist = 5)
            } else emptySet()
            return state.copy(
                reachableTiles = teleportTiles.associateWith { 0 },
                moveParents = emptyMap(),
                attackableUids = emptySet(),
                chargeableUids = emptySet()
            )
        }

        val moveResult = if (selected.moveCmLeft >= 1.5f && !state.chargeModeActive) {
            TacticalRules.computeReachableTiles(selected, state.map.rows, state.units)
        } else null

        val chargeable = if (state.chargeModeActive) {
            TacticalRules.computeChargeableTargets(selected, state.units, state.map.rows)
        } else emptySet()

        val attackable = if (state.chargeModeActive) {
            emptySet()
        } else {
            state.units
                .filter { it.isAlive && it.faction != selected.faction }
                .filter { enemy ->
                    val p = TacticalRules.canTarget(selected, enemy, state.weaponMode, state.map.rows)
                    p != null && p.inRange && p.hasLos
                }
                .map { it.uid }
                .toSet()
        }

        return state.copy(
            reachableTiles = moveResult?.costs ?: emptyMap(),
            moveParents = moveResult?.parents ?: emptyMap(),
            attackableUids = attackable,
            chargeableUids = chargeable
        )
    }

    private fun checkVictoryOrUpdate(state: BattleUiState): BattleUiState {
        val marinesAlive = state.units.any { it.faction == Faction.MARINES && it.isAlive }
        val xenosAlive = state.units.any { it.faction == Faction.XENOS && it.isAlive }
        if (marinesAlive && xenosAlive) return state

        aiJob?.cancel()
        actionJob?.cancel()
        sound.play(SoundType.VICTORY)
        val winner = if (marinesAlive) Faction.MARINES else Faction.XENOS
        val playerWon = winner == state.playerFaction
        val totalDmg = state.units.filter { it.faction == state.playerFaction }.sumOf { it.damageDealt }

        viewModelScope.launch {
            repository.recordBattle(
                BattleRecordEntity(
                    winnerFaction = winner.displayName,
                    playerFaction = state.playerFaction.displayName,
                    playerWon = playerWon,
                    rounds = state.round,
                    playerKills = state.playerKills,
                    enemyKills = state.enemyKills
                )
            )
        }

        return state.copy(
            isAiThinking = false,
            isAnimating = false,
            reachableTiles = emptyMap(),
            attackableUids = emptySet(),
            targetPreview = null,
            summary = BattleSummary(
                winner = winner,
                playerWon = playerWon,
                rounds = state.round,
                playerKills = state.playerKills,
                enemyKills = state.enemyKills,
                totalDamageDealt = totalDmg
            )
        )
    }

    private fun spawnFloatingText(pos: GridPos, text: String, colorArgb: Long) {
        val now = System.currentTimeMillis()
        val fx = FloatingTextVfx(nextIdCounter++, pos.x.toFloat(), pos.y.toFloat(), text, colorArgb, now)
        _battleState.update { state ->
            state.copy(floatingTexts = (state.floatingTexts + fx).filter { now - it.startTimeMs < 3200 })
        }
        scheduleVfxCleanup()
    }

    private fun scheduleVfxCleanup() {
        viewModelScope.launch {
            delay(3400)
            val now = System.currentTimeMillis()
            _battleState.update { state ->
                val nextProj = state.projectiles.filter { now - it.startTimeMs < 1800 }
                val nextFloats = state.floatingTexts.filter { now - it.startTimeMs < 3200 }
                if (nextProj.size == state.projectiles.size && nextFloats.size == state.floatingTexts.size) {
                    state
                } else {
                    state.copy(projectiles = nextProj, floatingTexts = nextFloats)
                }
            }
        }
    }

    private fun scheduleSplashAutoDismiss() {
        viewModelScope.launch {
            delay(2800)
            _battleState.update { it.copy(turnSplash = null) }
        }
    }
}
