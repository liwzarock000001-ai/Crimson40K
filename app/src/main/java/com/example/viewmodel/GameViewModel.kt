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
import com.example.model.Facing
import com.example.model.Faction
import com.example.model.FloatingTextVfx
import com.example.model.GridPos
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
    val reachableTiles: Map<GridPos, Int> = emptyMap(),
    val moveParents: Map<GridPos, GridPos> = emptyMap(),
    val attackableUids: Set<String> = emptySet(),
    val targetPreview: TargetPreview? = null,
    val projectiles: List<ProjectileVfx> = emptyList(),
    val floatingTexts: List<FloatingTextVfx> = emptyList(),
    val autoFocusCamera: Boolean = true,
    val focusGridPos: GridPos? = null,
    val centerTrigger: Int = 0,
    val soundEnabled: Boolean = true,
    val isAiThinking: Boolean = false,
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
                WarriorWeaponChoice.BONESWORDS -> updated.copy(range = 1, damage = updated.damage.coerceAtLeast(18))
                WarriorWeaponChoice.FLESHBORER -> updated.copy(range = 4)
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
                    totalHp = tpl.maxTotalHp,
                    maxTotalHp = tpl.maxTotalHp,
                    mpLeft = tpl.mp,
                    apLeft = tpl.ap
                )
            )
        }
    }

    fun returnToSetup() {
        aiJob?.cancel()
        sound.play(SoundType.CLICK)
        _battleState.update { it.copy(active = false, summary = null, isAiThinking = false) }
    }

    // --- BATTLE INTERACTION ---
    fun onTileTapped(pos: GridPos) {
        val state = _battleState.value
        if (!state.isPlayerTurn) return

        // 0. Check if Assault Terminator is in Teleport Strike mode
        val selected = state.selectedUnit
        if (selected != null && selected.faction == state.playerFaction &&
            selected.template.archetype == UnitArchetype.TERMINATOR &&
            state.weaponMode == WeaponMode.SKILL
        ) {
            if (pos in state.reachableTiles.keys) {
                executeTeleport(selected.uid, pos)
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

        // Check if tapped an alive unit on this tile
        val unitOnTile = state.units.firstOrNull { it.isAlive && it.pos == pos }
        if (unitOnTile != null) {
            if (unitOnTile.faction == state.playerFaction) {
                sound.play(SoundType.CLICK)
                val updated = state.copy(
                    selectedUnitUid = unitOnTile.uid,
                    weaponMode = WeaponMode.MAIN,
                    targetPreview = null,
                    focusGridPos = if (state.autoFocusCamera) unitOnTile.pos else state.focusGridPos
                )
                _battleState.value = recomputeSelectionOverlays(updated)
                return
            } else {
                // Enemy tapped: show Target Preview if selected unit can target!
                val sel = state.selectedUnit ?: return
                val preview = TacticalRules.canTarget(sel, unitOnTile, state.weaponMode, state.map.rows)
                if (preview != null && preview.inRange && preview.hasLos) {
                    sound.play(SoundType.CLICK)
                    _battleState.update { it.copy(targetPreview = preview) }
                } else {
                    spawnFloatingText(pos, if (preview == null) "NO AP / CD" else if (!preview.inRange) "OUT OF RANGE" else "NO LOS", 0xFF9AA3B2L)
                }
                return
            }
        }

        // Empty walkable reachable tile -> Move selected friendly unit!
        val sel = state.selectedUnit ?: return
        if (sel.faction != state.playerFaction) return
        val moveCost = state.reachableTiles[pos] ?: return
        val path = TacticalRules.reconstructPath(pos, sel.pos, state.moveParents)
        if (path.isNotEmpty()) {
            moveUnitAlongPath(sel.uid, path, moveCost)
        }
    }

    private fun executeTeleport(unitUid: String, dest: GridPos) {
        val state = _battleState.value
        val unit = state.units.firstOrNull { it.uid == unitUid && it.isAlive } ?: return
        sound.play(SoundType.PLASMA)

        val vfx = ProjectileVfx(
            id = nextIdCounter++,
            fromX = unit.x.toFloat(),
            fromY = unit.y.toFloat(),
            toX = dest.x.toFloat(),
            toY = dest.y.toFloat(),
            colorArgb = 0xFF4CC9F0L,
            isBeam = true,
            isMelee = false,
            startTimeMs = System.currentTimeMillis()
        )
        spawnFloatingText(unit.pos, "WARP OUT", 0xFF64DFDFL)
        spawnFloatingText(dest, "TELEPORT STRIKE!", 0xFF48CAE4L)
        spawnFloatingText(GridPos(dest.x, dest.y), "SHIELD -50% DMG", 0xFFFFD166L)

        val updatedUnits = state.units.map { u ->
            if (u.uid != unitUid) u
            else u.copy(
                x = dest.x,
                y = dest.y,
                facing = TacticalRules.facingToward(u.pos, dest),
                apLeft = (u.apLeft - u.template.skillApCost).coerceAtLeast(0),
                skillCooldownLeft = u.template.skillCooldown,
                damageReductionTurns = 1,
                buffTurnsLeft = 1,
                activeBuffLabel = "SHIELD -50% DMG"
            )
        }
        val withAuras = TacticalRules.applyAuras(updatedUnits)
        _battleState.value = recomputeSelectionOverlays(
            state.copy(
                units = withAuras,
                weaponMode = WeaponMode.MAIN,
                targetPreview = null,
                projectiles = state.projectiles + vfx,
                focusGridPos = if (state.autoFocusCamera) dest else state.focusGridPos
            )
        )
    }

    private fun moveUnitAlongPath(unitUid: String, path: List<GridPos>, totalCost: Int) {
        sound.play(SoundType.CLICK)
        val dest = path.last()
        _battleState.update { state ->
            val updatedUnits = state.units.map { u ->
                if (u.uid != unitUid) u
                else u.copy(
                    x = dest.x,
                    y = dest.y,
                    facing = TacticalRules.facingToward(u.pos, dest),
                    mpLeft = (u.mpLeft - totalCost).coerceAtLeast(0)
                )
            }
            val withAuras = TacticalRules.applyAuras(updatedUnits)
            recomputeSelectionOverlays(
                state.copy(
                    units = withAuras,
                    targetPreview = null,
                    focusGridPos = if (state.autoFocusCamera) dest else state.focusGridPos
                )
            )
        }
    }

    fun confirmStrike() {
        val state = _battleState.value
        if (!state.isPlayerTurn) return
        val attacker = state.selectedUnit ?: return
        val preview = state.targetPreview ?: return
        val target = state.units.firstOrNull { it.uid == preview.targetUid && it.isAlive } ?: return
        executeAttack(attacker.uid, target.uid, preview.weaponMode)
    }

    private fun executeAttack(attackerUid: String, targetUid: String, mode: WeaponMode) {
        val state = _battleState.value
        val attacker = state.units.firstOrNull { it.uid == attackerUid && it.isAlive } ?: return
        val target = state.units.firstOrNull { it.uid == targetUid && it.isAlive } ?: return

        val isMelee = TacticalRules.weaponRange(attacker, mode) <= 1
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
            fromX = attacker.x.toFloat(),
            fromY = attacker.y.toFloat(),
            toX = target.x.toFloat(),
            toY = target.y.toFloat(),
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
                startTimeMs = now + idx * 90L
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

        val updated = state.copy(
            units = newUnits,
            weaponMode = WeaponMode.MAIN,
            targetPreview = null,
            projectiles = (state.projectiles + proj).filter { now - it.startTimeMs < 900 },
            floatingTexts = (state.floatingTexts + floats).filter { now - it.startTimeMs < 1500 },
            focusGridPos = if (state.autoFocusCamera) target.pos else state.focusGridPos,
            playerKills = state.playerKills + addedPlayerKills,
            enemyKills = state.enemyKills + addedEnemyKills
        )
        _battleState.value = checkVictoryOrUpdate(recomputeSelectionOverlays(updated))
        scheduleVfxCleanup()
    }

    fun selectWeaponMode(mode: WeaponMode) {
        val state = _battleState.value
        if (!state.isPlayerTurn) return
        val selected = state.selectedUnit ?: return
        sound.play(SoundType.CLICK)

        if (mode == WeaponMode.SKILL) {
            val selfUpdated = TacticalRules.activateSelfSkillIfApplicable(selected)
            if (selfUpdated != null) {
                val units = TacticalRules.applyAuras(state.units.map { if (it.uid == selfUpdated.uid) selfUpdated else it })
                spawnFloatingText(selected.pos, "BOUNDING LEAP!", 0xFF38D96CL)
                _battleState.value = recomputeSelectionOverlays(state.copy(units = units, weaponMode = WeaponMode.MAIN, targetPreview = null))
                return
            }
        }

        val nextMode = if (state.weaponMode == mode) WeaponMode.MAIN else mode
        _battleState.value = recomputeSelectionOverlays(state.copy(weaponMode = nextMode, targetPreview = null))
    }

    fun triggerSurge() {
        val state = _battleState.value
        if (!state.isPlayerTurn) return
        val selected = state.selectedUnit ?: return
        if (selected.momentum < 100 || selected.surgeActive) return
        sound.play(SoundType.SURGE)
        val surged = TacticalRules.activateSurge(selected)
        val units = state.units.map { if (it.uid == surged.uid) surged else it }
        spawnFloatingText(selected.pos, "BATTLE SURGE!", 0xFFFFB703L)
        _battleState.value = recomputeSelectionOverlays(state.copy(units = units))
    }

    fun rotateSelectedUnit(left: Boolean) {
        val state = _battleState.value
        if (!state.isPlayerTurn) return
        val selected = state.selectedUnit ?: return
        sound.play(SoundType.CLICK)
        val nextFacing = if (left) selected.facing.turnLeft() else selected.facing.turnRight()
        val units = state.units.map { if (it.uid == selected.uid) it.copy(facing = nextFacing) else it }
        _battleState.value = state.copy(units = units)
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
        if (!state.isPlayerTurn) return
        sound.play(SoundType.TURN_HORN)

        val aiFaction = if (state.playerFaction == Faction.MARINES) Faction.XENOS else Faction.MARINES
        val refreshedUnits = refreshFactionForNewTurn(state.units, aiFaction)
        val firstAi = refreshedUnits.firstOrNull { it.faction == aiFaction && it.isAlive }

        val updated = state.copy(
            activeFaction = aiFaction,
            units = TacticalRules.applyAuras(refreshedUnits),
            selectedUnitUid = firstAi?.uid,
            weaponMode = WeaponMode.MAIN,
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
            delay(850)
            var guard = 0
            while (guard < 32) {
                guard++
                val current = _battleState.value
                if (!current.active || current.summary != null || current.activeFaction != aiFaction) break

                val decision = withContext(Dispatchers.Default) {
                    AiController.decideNextAction(aiFaction, current.units, current.map.rows)
                }
                when (decision) {
                    is AiDecision.Surge -> {
                        sound.play(SoundType.SURGE)
                        val units = current.units.map {
                            if (it.uid == decision.unitUid) TacticalRules.activateSurge(it) else it
                        }
                        val u = units.firstOrNull { it.uid == decision.unitUid }
                        if (u != null) spawnFloatingText(u.pos, "AI SURGE!", 0xFFFFB703L)
                        _battleState.update { it.copy(units = units, focusGridPos = if (it.autoFocusCamera) u?.pos else it.focusGridPos) }
                        delay(450)
                    }
                    is AiDecision.SelfSkill -> {
                        val units = current.units.map {
                            if (it.uid == decision.unitUid) TacticalRules.activateSelfSkillIfApplicable(it) ?: it else it
                        }
                        val u = units.firstOrNull { it.uid == decision.unitUid }
                        if (u != null) spawnFloatingText(u.pos, "LEAP!", 0xFFC77DFFL)
                        _battleState.update { it.copy(units = units, focusGridPos = if (it.autoFocusCamera) u?.pos else it.focusGridPos) }
                        delay(420)
                    }
                    is AiDecision.Teleport -> {
                        executeTeleport(decision.unitUid, decision.targetPos)
                        delay(650)
                    }
                    is AiDecision.Move -> {
                        moveUnitAlongPath(decision.unitUid, decision.path, decision.cost)
                        delay(460)
                    }
                    is AiDecision.Attack -> {
                        executeAttack(decision.attackerUid, decision.targetUid, decision.mode)
                        delay(650)
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
                    mpLeft = u.template.mp,
                    apLeft = u.template.ap,
                    skillCooldownLeft = (u.skillCooldownLeft - 1).coerceAtLeast(0),
                    pistolCooldownLeft = (u.pistolCooldownLeft - 1).coerceAtLeast(0),
                    surgeActive = false,
                    buffTurnsLeft = nextBuffTurns,
                    damageReductionTurns = nextDamageReduction,
                    activeBuffLabel = if (nextBuffTurns > 0 || nextDamageReduction > 0) u.activeBuffLabel else null
                )
            }
        }

    private fun recomputeSelectionOverlays(state: BattleUiState): BattleUiState {
        val selected = state.selectedUnit
        if (selected == null || !state.isPlayerTurn || selected.faction != state.playerFaction) {
            return state.copy(reachableTiles = emptyMap(), moveParents = emptyMap(), attackableUids = emptySet())
        }

        if (selected.template.archetype == UnitArchetype.TERMINATOR && state.weaponMode == WeaponMode.SKILL) {
            val canTeleport = selected.skillCooldownLeft == 0 && selected.apLeft >= selected.template.skillApCost
            val teleportTiles = if (canTeleport) {
                TacticalRules.computeTeleportTiles(selected, state.map.rows, state.units, maxDist = 9, minEnemyDist = 5)
            } else emptySet()
            return state.copy(
                reachableTiles = teleportTiles.associateWith { 0 },
                moveParents = emptyMap(),
                attackableUids = emptySet()
            )
        }

        val moveResult = if (selected.mpLeft > 0) {
            TacticalRules.computeReachableTiles(selected, state.map.rows, state.units)
        } else null

        val attackable = state.units
            .filter { it.isAlive && it.faction != selected.faction }
            .filter { enemy ->
                val p = TacticalRules.canTarget(selected, enemy, state.weaponMode, state.map.rows)
                p != null && p.inRange && p.hasLos
            }
            .map { it.uid }
            .toSet()

        return state.copy(
            reachableTiles = moveResult?.costs ?: emptyMap(),
            moveParents = moveResult?.parents ?: emptyMap(),
            attackableUids = attackable
        )
    }

    private fun checkVictoryOrUpdate(state: BattleUiState): BattleUiState {
        val marinesAlive = state.units.any { it.faction == Faction.MARINES && it.isAlive }
        val xenosAlive = state.units.any { it.faction == Faction.XENOS && it.isAlive }
        if (marinesAlive && xenosAlive) return state

        aiJob?.cancel()
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
            state.copy(floatingTexts = (state.floatingTexts + fx).filter { now - it.startTimeMs < 1500 })
        }
        scheduleVfxCleanup()
    }

    private fun scheduleVfxCleanup() {
        viewModelScope.launch {
            delay(1600)
            val now = System.currentTimeMillis()
            _battleState.update { state ->
                val nextProj = state.projectiles.filter { now - it.startTimeMs < 900 }
                val nextFloats = state.floatingTexts.filter { now - it.startTimeMs < 1500 }
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
            delay(1700)
            _battleState.update { it.copy(turnSplash = null) }
        }
    }
}
