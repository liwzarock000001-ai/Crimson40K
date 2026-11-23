package com.example.ui.battle

import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.opengl.Matrix
import com.example.engine.MapGenerator
import com.example.engine.TacticalRules
import com.example.model.Faction
import com.example.model.GridPos
import com.example.model.TerrainType
import com.example.model.UnitArchetype
import com.example.model.WeaponMode
import com.example.viewmodel.BattleUiState
import java.util.IdentityHashMap
import java.util.concurrent.atomic.AtomicReference
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class GlesBattleRenderer(
    private val cameraRig: SharedCameraRig
) : GLSurfaceView.Renderer {

    val sceneRef = AtomicReference(BattleUiState())

    private class GpuVbo(
        val id: Int,
        val vertexCount: Int,
        val uvVboId: Int = 0,
        val texId: Int = 0
    )

    private val vboCache = IdentityHashMap<MeshGeometry, GpuVbo>()
    private var lastBoundVboId = -1

    private var bakedMapId = Long.MIN_VALUE
    private var floorBatchVbo: GpuVbo? = null
    private var terrainBatchVbo: GpuVbo? = null
    private val objectiveCoords = ArrayList<GridPos>(4)

    private var program = 0
    private var aPos = 0
    private var aNormal = 0
    private var aColor = 0
    private var aTexCoord = 0
    private var uMvp = 0
    private var uModel = 0
    private var uEyePos = 0
    private var uTint = 0
    private var uUnlit = 0
    private var uIsUnit = 0
    private var uTime = 0
    private var uGraphicsLevel = 0
    private var uBrightness = 0
    private var uTexture = 0
    private var uHasTexture = 0

    // Pre-allocated scratch matrices & vectors (0 allocations per frame)
    private val projMat = FloatArray(16)
    private val viewMat = FloatArray(16)
    private val vpMat = FloatArray(16)
    private val modelMat = FloatArray(16)
    private val mvpMat = FloatArray(16)
    private val eyePos = FloatArray(3)
    private val genBuf = IntArray(1)

    // Pre-allocated atmospheric embers & ash particles (0 allocations per frame)
    private val emberCount = 48
    private val emberX = FloatArray(emberCount)
    private val emberZ = FloatArray(emberCount)
    private val emberPhase = FloatArray(emberCount)
    private val emberSpeed = FloatArray(emberCount)
    private val emberSize = FloatArray(emberCount)
    private val emberIsHot = BooleanArray(emberCount)

    private var viewW = 1
    private var viewH = 1
    private var startTimeMs = System.currentTimeMillis()
    private var lastFrameTimeNs = System.nanoTime()
    private val unitVisPos = HashMap<String, FloatArray>(32)

    init {
        for (i in 0 until emberCount) {
            val h1 = ((i * 73 + 17) % 100) / 100f
            val h2 = ((i * 41 + 53) % 100) / 100f
            emberX[i] = h1 * MapGenerator.GRID_W - 0.5f
            emberZ[i] = h2 * MapGenerator.GRID_H - 0.5f
            emberPhase[i] = (i * 0.67f)
            emberSpeed[i] = 0.11f + (i % 5) * 0.04f
            emberSize[i] = 0.036f + (i % 4) * 0.014f
            emberIsHot[i] = i % 3 != 0
        }
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.48f, 0.72f, 0.92f, 1f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        vboCache.clear()
        bakedMapId = Long.MIN_VALUE
        floorBatchVbo = null
        terrainBatchVbo = null
        lastBoundVboId = -1

        program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        aPos = GLES20.glGetAttribLocation(program, "aPos")
        aNormal = GLES20.glGetAttribLocation(program, "aNormal")
        aColor = GLES20.glGetAttribLocation(program, "aColor")
        aTexCoord = GLES20.glGetAttribLocation(program, "aTexCoord")
        uMvp = GLES20.glGetUniformLocation(program, "uMvp")
        uModel = GLES20.glGetUniformLocation(program, "uModel")
        uEyePos = GLES20.glGetUniformLocation(program, "uEyePos")
        uTint = GLES20.glGetUniformLocation(program, "uTint")
        uUnlit = GLES20.glGetUniformLocation(program, "uUnlit")
        uIsUnit = GLES20.glGetUniformLocation(program, "uIsUnit")
        uTime = GLES20.glGetUniformLocation(program, "uTime")
        uGraphicsLevel = GLES20.glGetUniformLocation(program, "uGraphicsLevel")
        uBrightness = GLES20.glGetUniformLocation(program, "uBrightness")
        uTexture = GLES20.glGetUniformLocation(program, "uTexture")
        uHasTexture = GLES20.glGetUniformLocation(program, "uHasTexture")
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewW = width.coerceAtLeast(1)
        viewH = height.coerceAtLeast(1)
        GLES20.glViewport(0, 0, viewW, viewH)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        if (program == 0) return

        val state = sceneRef.get()
        val nowNs = System.nanoTime()
        val dtSec = ((nowNs - lastFrameTimeNs) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
        lastFrameTimeNs = nowNs

        cameraRig.step(0.08f)
        val cam = cameraRig.snapshot()

        val now = System.currentTimeMillis()
        val elapsedSec = ((now - startTimeMs) % 100000L) / 1000f
        val pulse = sin(elapsedSec * 1.8f) * 0.5f + 0.5f

        cam.buildVpMatrixInto(vpMat, projMat, viewMat, eyePos, viewW.toFloat(), viewH.toFloat())

        if (state.map.id != bakedMapId) {
            unitVisPos.clear()
            rebuildStaticMapVbos(state.map)
        }

        GLES20.glUseProgram(program)
        GLES20.glUniform3f(uEyePos, eyePos[0], eyePos[1], eyePos[2])
        GLES20.glUniform1f(uTime, elapsedSec)
        GLES20.glUniform1f(uGraphicsLevel, state.graphicsLevel.coerceIn(1, 4).toFloat())
        GLES20.glUniform1f(uBrightness, state.screenBrightness.coerceIn(0.5f, 1.5f))
        GLES20.glEnableVertexAttribArray(aPos)
        GLES20.glEnableVertexAttribArray(aNormal)
        GLES20.glEnableVertexAttribArray(aColor)
        lastBoundVboId = -1

        // 1. Batched Weathered Steel Deck & Slab (1 single draw call!)
        floorBatchVbo?.let { vbo ->
            Matrix.setIdentityM(modelMat, 0)
            drawVbo(vbo, 1f, 1f, 1f, 1f, unlit = false)
        }

        // 2. Batched Gothic Ruins, Craters & Grounded Obstacle Shadows (1 single draw call!)
        terrainBatchVbo?.let { vbo ->
            Matrix.setIdentityM(modelMat, 0)
            drawVbo(vbo, 1f, 1f, 1f, 1f, unlit = false)
        }

        // 3. Pulsing Grimdark Objective Beacon Rings
        val ringVbo = getOrUploadVbo(MeshCatalog3D.ringTile)
        for (i in objectiveCoords.indices) {
            val obj = objectiveCoords[i]
            Matrix.setIdentityM(modelMat, 0)
            Matrix.translateM(modelMat, 0, obj.x.toFloat(), 0.075f + pulse * 0.04f, obj.y.toFloat())
            Matrix.scaleM(modelMat, 0, 0.65f + pulse * 0.14f, 1f, 0.65f + pulse * 0.14f)
            drawVbo(ringVbo, 0.72f, 0.22f, 0.16f, 0.48f, unlit = true)
        }

        // 4. Special Skill Placement Tiles (Teleport / Seed Spore Mine) + Selected/Target Rings
        val tileVbo = getOrUploadVbo(MeshCatalog3D.tileQuad)
        val rangeRingVbo = getOrUploadVbo(MeshCatalog3D.rangeRadiusRing)
        val cubeVbo = getOrUploadVbo(MeshCatalog3D.unitCube)
        val isTeleportMode = state.selectedUnit?.template?.archetype == UnitArchetype.TERMINATOR && state.weaponMode == WeaponMode.SKILL
        val isSeedSporeMineMode = state.selectedUnit?.template?.archetype == UnitArchetype.BIOVORE && state.weaponMode == WeaponMode.SKILL
        if (state.isDeploymentPhase) {
            for (gy in 0 until MapGenerator.GRID_H) {
                for (gx in 0 until MapGenerator.GRID_W) {
                    val inWestZone = gx < 3
                    val inEastZone = gx >= MapGenerator.GRID_W - 3
                    if (!inWestZone && !inEastZone) continue
                    if (!MapGenerator.isWalkable(state.map.rows[gy][gx])) continue
                    if (state.units.any { it.isAlive && it.x == gx && it.y == gy }) continue
                    Matrix.setIdentityM(modelMat, 0)
                    Matrix.translateM(modelMat, 0, gx.toFloat(), 0.009f, gy.toFloat())
                    val isActiveSide = (inWestZone && state.deploymentTeamIndex == 0) || (inEastZone && state.deploymentTeamIndex == 1)
                    val alpha = if (isActiveSide) 0.28f + pulse * 0.16f else 0.09f
                    if (inWestZone) {
                        drawVbo(tileVbo, 0.16f, 0.72f, 0.98f, alpha, unlit = true)
                    } else {
                        drawVbo(tileVbo, 0.95f, 0.24f, 0.28f, alpha, unlit = true)
                    }
                }
            }
        }
        if (isTeleportMode || isSeedSporeMineMode) {
            for (pos in state.reachableTiles.keys) {
                Matrix.setIdentityM(modelMat, 0)
                Matrix.translateM(modelMat, 0, pos.x.toFloat(), 0.008f, pos.y.toFloat())
                if (isTeleportMode) {
                    drawVbo(tileVbo, 0.22f, 0.82f, 0.98f, 0.28f + pulse * 0.15f, unlit = true)
                    drawVbo(ringVbo, 0.40f, 0.92f, 1.0f, 0.75f + pulse * 0.20f, unlit = true)
                } else {
                    drawVbo(tileVbo, 0.78f, 0.28f, 0.92f, 0.32f + pulse * 0.18f, unlit = true)
                    drawVbo(ringVbo, 0.92f, 0.45f, 1.0f, 0.82f + pulse * 0.18f, unlit = true)
                }
            }
        }

        val selectedUnit = state.selectedUnit
        if (selectedUnit != null) {
            val selVis = unitVisPos[selectedUnit.uid]
            val sx = selVis?.get(0) ?: selectedUnit.worldX
            val sz = selVis?.get(1) ?: selectedUnit.worldY
            Matrix.setIdentityM(modelMat, 0)
            Matrix.translateM(modelMat, 0, sx, 0.012f, sz)
            when (selectedUnit.faction) {
                Faction.MARINES -> {
                    drawVbo(tileVbo, 0.72f, 0.78f, 0.86f, 0.20f, unlit = true)
                    drawVbo(ringVbo, 0.84f, 0.89f, 0.96f, 0.92f, unlit = true)
                }
                Faction.BLACK_LEGION -> {
                    drawVbo(tileVbo, 0.78f, 0.58f, 0.22f, 0.22f, unlit = true)
                    drawVbo(ringVbo, 0.94f, 0.74f, 0.30f, 0.92f, unlit = true)
                }
                Faction.XENOS -> {
                    drawVbo(tileVbo, 0.65f, 0.36f, 0.78f, 0.20f, unlit = true)
                    drawVbo(ringVbo, 0.78f, 0.56f, 0.86f, 0.92f, unlit = true)
                }
            }

            // Range Rings & Preview Paths
            if (!isTeleportMode && !isSeedSporeMineMode && state.canCommandUnit(selectedUnit)) {
                if (state.chargeModeActive) {
                    val chargeRadiusTiles = (12.0f / TacticalRules.CM_PER_TILE) + TacticalRules.MELEE_MAX_CENTER_TILES
                    Matrix.setIdentityM(modelMat, 0)
                    Matrix.translateM(modelMat, 0, sx, 0.020f, sz)
                    Matrix.scaleM(modelMat, 0, chargeRadiusTiles, 1f, chargeRadiusTiles)
                    drawVbo(rangeRingVbo, 1.0f, 0.45f, 0.12f, 0.72f + pulse * 0.20f, unlit = true)

                    val cPreview = state.chargePreview
                    if (cPreview != null && cPreview.path.isNotEmpty()) {
                        var prevX = sx
                        var prevZ = sz
                        for (i in cPreview.path.indices) {
                            val wp = cPreview.path[i]
                            val dx = wp.x - prevX
                            val dz = wp.y - prevZ
                            val segDist = sqrt(dx * dx + dz * dz)
                            if (segDist > 0.02f) {
                                val angleDeg = -(atan2(dz, dx) * 180.0 / PI).toFloat()
                                Matrix.setIdentityM(modelMat, 0)
                                Matrix.translateM(modelMat, 0, (prevX + wp.x) * 0.5f, 0.052f, (prevZ + wp.y) * 0.5f)
                                Matrix.rotateM(modelMat, 0, angleDeg, 0f, 1f, 0f)
                                Matrix.scaleM(modelMat, 0, segDist, 0.036f, 0.070f)
                                drawVbo(cubeVbo, 1.0f, 0.58f, 0.14f, 0.94f, unlit = true)
                            }
                            prevX = wp.x
                            prevZ = wp.y
                        }
                        Matrix.setIdentityM(modelMat, 0)
                        Matrix.translateM(modelMat, 0, cPreview.destinationX, 0.024f, cPreview.destinationY)
                        Matrix.scaleM(modelMat, 0, 0.65f + pulse * 0.15f, 1f, 0.65f + pulse * 0.15f)
                        drawVbo(ringVbo, 1.0f, 0.65f, 0.16f, 0.96f, unlit = true)
                    }
                } else {
                    // Blue Movement Range Circle (remaining move distance in cm)
                    val moveRadiusTiles = (selectedUnit.moveCmLeft / TacticalRules.CM_PER_TILE).coerceAtLeast(0.5f)
                    Matrix.setIdentityM(modelMat, 0)
                    Matrix.translateM(modelMat, 0, sx, 0.019f, sz)
                    Matrix.scaleM(modelMat, 0, moveRadiusTiles, 1f, moveRadiusTiles)
                    drawVbo(rangeRingVbo, 0.20f, 0.78f, 1.0f, 0.65f + pulse * 0.20f, unlit = true)

                    if (!state.moveModeActive) {
                        val activeMode = state.weaponMode
                        val isEngaged = TacticalRules.isEngagedInMelee(selectedUnit, state.units)
                        val atkRange = when {
                            selectedUnit.hasCharged && activeMode == WeaponMode.MAIN -> 1
                            isEngaged && activeMode == WeaponMode.MAIN && (!selectedUnit.template.mainWeaponPistol || selectedUnit.hasShot) -> 1
                            else -> TacticalRules.weaponRange(selectedUnit, activeMode)
                        }
                        val isMeleeRange = atkRange <= 1
                        val attackReady = TacticalRules.canUseMode(selectedUnit, activeMode, state.units)
                        val rangeRadiusTiles = if (isMeleeRange) TacticalRules.MELEE_MAX_CENTER_TILES else atkRange.toFloat() + 0.45f
                        val rangeAlpha = if (attackReady) 0.72f + pulse * 0.18f else 0.28f
                        Matrix.setIdentityM(modelMat, 0)
                        Matrix.translateM(modelMat, 0, sx, 0.020f, sz)
                        Matrix.scaleM(modelMat, 0, rangeRadiusTiles, 1f, rangeRadiusTiles)
                        drawVbo(rangeRingVbo, 0.96f, 0.16f, 0.18f, rangeAlpha, unlit = true)
                    }
                }
            }

            // CM Ruler Preview Path & Landing Ring (visible only to active controlling player)
            val preview = state.movePreview
            if (state.isPlayerTurn && state.canCommandUnit(selectedUnit) && preview != null && preview.path.isNotEmpty()) {
                var prevX = sx
                var prevZ = sz
                for (i in preview.path.indices) {
                    val wp = preview.path[i]
                    val dx = wp.x - prevX
                    val dz = wp.y - prevZ
                    val segDist = sqrt(dx * dx + dz * dz)
                    if (segDist > 0.02f) {
                        val angleDeg = -(atan2(dz, dx) * 180.0 / PI).toFloat()
                        Matrix.setIdentityM(modelMat, 0)
                        Matrix.translateM(modelMat, 0, (prevX + wp.x) * 0.5f, 0.045f, (prevZ + wp.y) * 0.5f)
                        Matrix.rotateM(modelMat, 0, angleDeg, 0f, 1f, 0f)
                        Matrix.scaleM(modelMat, 0, segDist, 0.032f, 0.055f)
                        drawVbo(cubeVbo, 0.30f, 0.94f, 1.0f, 0.88f, unlit = true)
                    }
                    prevX = wp.x
                    prevZ = wp.y
                }
                Matrix.setIdentityM(modelMat, 0)
                Matrix.translateM(modelMat, 0, preview.targetX, 0.022f, preview.targetY)
                Matrix.scaleM(modelMat, 0, 0.62f + pulse * 0.12f, 1f, 0.62f + pulse * 0.12f)
                drawVbo(ringVbo, 0.30f, 0.96f, 1.0f, 0.95f, unlit = true)
            }
        }

        val previewTargetUid = state.targetPreview?.targetUid
        if (previewTargetUid != null) {
            val targetUnit = state.units.firstOrNull { it.uid == previewTargetUid }
            if (targetUnit != null) {
                Matrix.setIdentityM(modelMat, 0)
                Matrix.translateM(modelMat, 0, targetUnit.worldX, 0.014f, targetUnit.worldY)
                drawVbo(tileVbo, 0.68f, 0.14f, 0.18f, 0.32f, unlit = true)
                drawVbo(ringVbo, 0.85f, 0.18f, 0.22f, 0.92f, unlit = true)
            }
        }

        // 5. 3D Squad Miniatures + Heavy Contact Shadows + Facing Chevrons
        val arrowVbo = getOrUploadVbo(MeshCatalog3D.facingArrow)
        val shadowVbo = getOrUploadVbo(MeshCatalog3D.shadowDisc)

        val moveLerp = (dtSec * 12.0f).coerceIn(0.06f, 0.55f)
        for (uIdx in state.units.indices) {
            val unit = state.units[uIdx]
            if (!unit.isAlive) continue
            var vis = unitVisPos[unit.uid]
            if (vis == null) {
                vis = floatArrayOf(unit.worldX, unit.worldY)
                unitVisPos[unit.uid] = vis
            } else {
                val targetX = unit.worldX
                val targetZ = unit.worldY
                val dx = targetX - vis[0]
                val dz = targetZ - vis[1]
                if (abs(dx) < 0.002f && abs(dz) < 0.002f) {
                    vis[0] = targetX
                    vis[1] = targetZ
                } else {
                    vis[0] += dx * moveLerp
                    vis[1] += dz * moveLerp
                }
            }
            val ux = vis[0]
            val uz = vis[1]

            val isSelected = unit.uid == state.selectedUnitUid
            val isAttackable = unit.uid in state.attackableUids
            val isChargeable = unit.uid in state.chargeableUids

            val rr: Float
            val rg: Float
            val rb: Float
            when {
                isSelected && (!state.isCoopMode && unit.faction == state.playerFaction || state.isCoopMode && unit.effectiveTeam == 0) -> {
                    if (unit.faction == Faction.BLACK_LEGION) {
                        rr = 0.92f; rg = 0.72f; rb = 0.28f
                    } else {
                        rr = 0.76f; rg = 0.82f; rb = 0.90f
                    }
                }
                isSelected -> { rr = 0.76f; rg = 0.56f; rb = 0.86f }
                isChargeable -> { rr = 1.00f; rg = 0.52f; rb = 0.14f }
                isAttackable -> { rr = 0.72f; rg = 0.16f; rb = 0.20f }
                unit.isCloaked -> { rr = 0.35f; rg = 0.92f; rb = 0.98f }
                state.isCoopMode && unit.effectiveTeam == 0 -> { rr = 0.18f; rg = 0.62f; rb = 0.88f }
                state.isCoopMode && unit.effectiveTeam == 1 -> { rr = 0.82f; rg = 0.24f; rb = 0.32f }
                unit.faction == Faction.MARINES -> { rr = 0.22f; rg = 0.38f; rb = 0.60f }
                unit.faction == Faction.BLACK_LEGION -> { rr = 0.78f; rg = 0.58f; rb = 0.20f }
                else -> { rr = 0.44f; rg = 0.21f; rb = 0.52f }
            }

            Matrix.setIdentityM(modelMat, 0)
            Matrix.translateM(modelMat, 0, ux, 0.01f, uz)
            drawVbo(ringVbo, rr, rg, rb, if (isSelected || isAttackable || isChargeable || unit.isCloaked) 0.78f + pulse * 0.20f else 0.58f, unlit = true)

            if (unit.isMarked) {
                Matrix.setIdentityM(modelMat, 0)
                Matrix.translateM(modelMat, 0, ux, 0.024f, uz)
                Matrix.scaleM(modelMat, 0, 1.18f + pulse * 0.06f, 1f, 1.18f + pulse * 0.06f)
                drawVbo(ringVbo, 1.00f, 0.42f, 0.12f, 0.72f + pulse * 0.24f, unlit = true)
            }

            Matrix.setIdentityM(modelMat, 0)
            Matrix.translateM(modelMat, 0, ux, 0.01f, uz)
            val yawDeg = unit.facingDeg
            Matrix.rotateM(modelMat, 0, yawDeg, 0f, 1f, 0f)
            val arrowR = when {
                isSelected && unit.faction == Faction.MARINES -> 0.86f
                isSelected && unit.faction == Faction.BLACK_LEGION -> 0.94f
                isSelected -> 0.80f
                else -> rr
            }
            val arrowG = when {
                isSelected && unit.faction == Faction.MARINES -> 0.90f
                isSelected && unit.faction == Faction.BLACK_LEGION -> 0.76f
                isSelected -> 0.68f
                else -> rg
            }
            val arrowB = when {
                isSelected && unit.faction == Faction.MARINES -> 0.96f
                isSelected && unit.faction == Faction.BLACK_LEGION -> 0.34f
                isSelected -> 0.86f
                else -> rb
            }
            drawVbo(arrowVbo, arrowR, arrowG, arrowB, 0.92f, unlit = true)

            val count = unit.aliveModels.coerceIn(1, 5)
            val isDread = unit.template.archetype == UnitArchetype.DREADNOUGHT
            val shadowScale = if (isDread) 1.72f else if (count == 1) 1.08f else 0.58f
            val useClaws = unit.template.archetype == UnitArchetype.TERMINATOR && (
                unit.template.terminatorWeapon == com.example.model.TerminatorWeaponChoice.LIGHTNING_CLAWS ||
                    (unit.template.terminatorWeapon == null &&
                        unit.template.mainWeaponName.contains("claw", ignoreCase = true) &&
                        !unit.template.mainWeaponName.contains("hammer", ignoreCase = true))
                )
            val meshVariant = if (useClaws) "CLAWS" else ""
            val unitVbo = getOrUploadVbo(MeshCatalog3D.getUnitMesh(unit.template.archetype, meshVariant))

            for (mIdx in 0 until count) {
                val ox = subOffsetX(count, mIdx)
                val oz = subOffsetZ(count, mIdx)
                val wx = ux + ox
                val wz = uz + oz

                // Offset directional cast shadow + grounded contact umbra
                Matrix.setIdentityM(modelMat, 0)
                Matrix.translateM(modelMat, 0, wx - (if (isDread) 0.12f else 0.06f), 0f, wz - (if (isDread) 0.10f else 0.05f))
                Matrix.scaleM(modelMat, 0, shadowScale, 1f, shadowScale)
                drawVbo(shadowVbo, 1f, 1f, 1f, 1f, unlit = true)

                val bob = if (isSelected) sin(elapsedSec * 2.25f + mIdx) * 0.02f else 0f
                Matrix.setIdentityM(modelMat, 0)
                Matrix.translateM(modelMat, 0, wx, bob, wz)
                Matrix.rotateM(modelMat, 0, yawDeg, 0f, 1f, 0f)
                if (isDread) {
                    Matrix.scaleM(modelMat, 0, 1.72f, 1.72f, 1.72f)
                }
                if (unit.isCloaked) {
                    drawVbo(unitVbo, 0.55f, 0.85f, 0.96f, 0.35f, unlit = false, isUnit = true)
                } else {
                    drawVbo(unitVbo, 1f, 1f, 1f, 1f, unlit = false, isUnit = true)
                }
            }
        }

        // 6. 3D Projectile Bolts, Plasma Beams & Melee Slashes (50% slower for clear visual tracking)
        if (state.projectiles.isNotEmpty()) {
            for (pIdx in state.projectiles.indices) {
                val proj = state.projectiles[pIdx]
                val elapsed = (now - proj.startTimeMs).coerceAtLeast(0L)
                if (elapsed < 1300L) {
                    val p = (elapsed / 840f).coerceIn(0f, 1f)
                    val fade = (elapsed / 1200f).coerceIn(0f, 1f)
                    val pr = ((proj.colorArgb shr 16) and 0xFF) / 255f
                    val pg = ((proj.colorArgb shr 8) and 0xFF) / 255f
                    val pb = (proj.colorArgb and 0xFF) / 255f

                    if (proj.isMelee) {
                        Matrix.setIdentityM(modelMat, 0)
                        Matrix.translateM(modelMat, 0, proj.toX, 0.28f, proj.toY)
                        Matrix.scaleM(modelMat, 0, 0.8f + fade * 1.1f, 1f, 0.8f + fade * 1.1f)
                        drawVbo(ringVbo, pr, pg, pb, (1f - fade) * 0.92f, unlit = true)
                    } else {
                        val dx = proj.toX - proj.fromX
                        val dz = proj.toY - proj.fromY
                        val dist = sqrt(dx * dx + dz * dz).coerceAtLeast(0.01f)
                        val angleDeg = -(atan2(dz, dx) * 180.0 / PI).toFloat()
                        if (proj.isBeam) {
                            Matrix.setIdentityM(modelMat, 0)
                            Matrix.translateM(modelMat, 0, (proj.fromX + proj.toX) * 0.5f, 0.45f, (proj.fromY + proj.toY) * 0.5f)
                            Matrix.rotateM(modelMat, 0, angleDeg, 0f, 1f, 0f)
                            Matrix.scaleM(modelMat, 0, dist, 0.085f * (1f - fade * 0.5f), 0.085f * (1f - fade * 0.5f))
                            drawVbo(cubeVbo, pr, pg, pb, (1f - fade) * 0.92f, unlit = true)
                        } else if (elapsed < 880L) {
                            Matrix.setIdentityM(modelMat, 0)
                            Matrix.translateM(modelMat, 0, proj.fromX + dx * p, 0.45f, proj.fromY + dz * p)
                            Matrix.rotateM(modelMat, 0, angleDeg, 0f, 1f, 0f)
                            Matrix.scaleM(modelMat, 0, 0.40f, 0.07f, 0.07f)
                            drawVbo(cubeVbo, pr, pg, pb, 1f, unlit = true)
                        }
                    }
                }
            }
        }

        // 7. Drifting Warzone Embers & Ash Particles (zero heap allocations, scaled by 4 graphics levels)
        val activeEmbers = when (state.graphicsLevel) {
            1 -> 0
            2 -> 16
            3 -> 32
            else -> emberCount
        }
        for (i in 0 until activeEmbers) {
            val cycle = (elapsedSec * emberSpeed[i] + emberPhase[i]) % 3.2f
            val hFrac = cycle / 3.2f
            val py = 0.06f + hFrac * 2.5f
            val px = emberX[i] + sin(elapsedSec * 0.45f + emberPhase[i]) * 0.28f
            val pz = emberZ[i] + cos(elapsedSec * 0.35f + emberPhase[i]) * 0.24f
            val fade = sin(hFrac * PI).toFloat().coerceIn(0f, 1f)
            val s = emberSize[i]

            Matrix.setIdentityM(modelMat, 0)
            Matrix.translateM(modelMat, 0, px, py, pz)
            Matrix.rotateM(modelMat, 0, elapsedSec * 22.5f + i * 25f, 0f, 1f, 0f)
            Matrix.scaleM(modelMat, 0, s, s, s)

            if (emberIsHot[i]) {
                drawVbo(cubeVbo, 0.96f, 0.36f + (i % 3) * 0.12f, 0.08f, fade * 0.78f, unlit = true)
            } else {
                drawVbo(cubeVbo, 0.45f, 0.46f, 0.50f, fade * 0.36f, unlit = true)
            }
        }

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
    }

    private fun subOffsetX(count: Int, idx: Int): Float = when (count) {
        1 -> 0f
        2 -> if (idx == 0) -0.34f else 0.34f
        3 -> if (idx == 0) 0.34f else -0.36f
        4 -> if (idx == 0 || idx == 2) -0.36f else 0.36f
        else -> when (idx) {
            0 -> 0f
            1, 3 -> -0.44f
            else -> 0.44f
        }
    }

    private fun subOffsetZ(count: Int, idx: Int): Float = when (count) {
        1 -> 0f
        2 -> if (idx == 0) -0.28f else 0.28f
        3 -> when (idx) {
            0 -> 0f
            1 -> -0.38f
            else -> 0.38f
        }
        4 -> if (idx < 2) -0.36f else 0.36f
        else -> when (idx) {
            0 -> 0f
            1, 2 -> -0.44f
            else -> 0.44f
        }
    }

    private fun rebuildStaticMapVbos(map: com.example.model.SectorMap) {
        floorBatchVbo?.let { deleteVbo(it) }
        terrainBatchVbo?.let { deleteVbo(it) }

        val (floorMesh, terrainMesh) = MeshCatalog3D.buildStaticMapBatches(map)
        floorBatchVbo = uploadVbo(floorMesh)
        terrainBatchVbo = uploadVbo(terrainMesh)

        objectiveCoords.clear()
        val rows = map.rows
        for (gy in 0 until MapGenerator.GRID_H) {
            for (gx in 0 until MapGenerator.GRID_W) {
                if (rows[gy][gx] == TerrainType.OBJECTIVE) {
                    objectiveCoords.add(GridPos(gx, gy))
                }
            }
        }
        bakedMapId = map.id
    }

    private fun getOrUploadVbo(mesh: MeshGeometry): GpuVbo {
        vboCache[mesh]?.let { return it }
        return uploadVbo(mesh).also { vboCache[mesh] = it }
    }

    private fun uploadVbo(mesh: MeshGeometry): GpuVbo {
        GLES20.glGenBuffers(1, genBuf, 0)
        val vboId = genBuf[0]
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId)
        mesh.buffer.position(0)
        GLES20.glBufferData(
            GLES20.GL_ARRAY_BUFFER,
            mesh.data.size * 4,
            mesh.buffer,
            GLES20.GL_STATIC_DRAW
        )
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)

        var uvVboId = 0
        var texId = 0
        val uvBuf = mesh.uvBuffer
        val uvs = mesh.texCoords
        val texBytes = mesh.textureBytes
        if (uvBuf != null && uvs != null && texBytes != null) {
            val bmp = BitmapFactory.decodeByteArray(texBytes, 0, texBytes.size)
            if (bmp != null) {
                GLES20.glGenBuffers(1, genBuf, 0)
                uvVboId = genBuf[0]
                GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, uvVboId)
                uvBuf.position(0)
                GLES20.glBufferData(
                    GLES20.GL_ARRAY_BUFFER,
                    uvs.size * 4,
                    uvBuf,
                    GLES20.GL_STATIC_DRAW
                )
                GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)

                GLES20.glGenTextures(1, genBuf, 0)
                texId = genBuf[0]
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId)
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR)
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0)
                GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
                bmp.recycle()
            }
        }
        return GpuVbo(vboId, mesh.vertexCount, uvVboId, texId)
    }

    private fun deleteVbo(vbo: GpuVbo) {
        genBuf[0] = vbo.id
        GLES20.glDeleteBuffers(1, genBuf, 0)
        if (vbo.uvVboId != 0) {
            genBuf[0] = vbo.uvVboId
            GLES20.glDeleteBuffers(1, genBuf, 0)
        }
        if (vbo.texId != 0) {
            genBuf[0] = vbo.texId
            GLES20.glDeleteTextures(1, genBuf, 0)
        }
    }

    private fun drawVbo(
        vbo: GpuVbo,
        tintR: Float,
        tintG: Float,
        tintB: Float,
        tintA: Float,
        unlit: Boolean,
        isUnit: Boolean = false
    ) {
        Matrix.multiplyMM(mvpMat, 0, vpMat, 0, modelMat, 0)
        GLES20.glUniformMatrix4fv(uMvp, 1, false, mvpMat, 0)
        GLES20.glUniformMatrix4fv(uModel, 1, false, modelMat, 0)
        GLES20.glUniform4f(uTint, tintR, tintG, tintB, tintA)
        GLES20.glUniform1f(uUnlit, if (unlit) 1f else 0f)
        GLES20.glUniform1f(uIsUnit, if (isUnit) 1f else 0f)

        val hasTex = vbo.uvVboId != 0 && vbo.texId != 0
        GLES20.glUniform1f(uHasTexture, if (hasTex) 1f else 0f)

        if (lastBoundVboId != vbo.id) {
            lastBoundVboId = vbo.id
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo.id)
            val stride = 10 * 4
            GLES20.glVertexAttribPointer(aPos, 3, GLES20.GL_FLOAT, false, stride, 0)
            GLES20.glVertexAttribPointer(aNormal, 3, GLES20.GL_FLOAT, false, stride, 3 * 4)
            GLES20.glVertexAttribPointer(aColor, 4, GLES20.GL_FLOAT, false, stride, 6 * 4)
            if (hasTex && aTexCoord >= 0) {
                GLES20.glEnableVertexAttribArray(aTexCoord)
                GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo.uvVboId)
                GLES20.glVertexAttribPointer(aTexCoord, 2, GLES20.GL_FLOAT, false, 2 * 4, 0)
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, vbo.texId)
                GLES20.glUniform1i(uTexture, 0)
            } else if (aTexCoord >= 0) {
                GLES20.glDisableVertexAttribArray(aTexCoord)
                GLES20.glVertexAttrib2f(aTexCoord, 0f, 0f)
            }
        }
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vbo.vertexCount)
    }

    private fun createProgram(vsCode: String, fsCode: String): Int {
        val vs = compileShader(GLES20.GL_VERTEX_SHADER, vsCode)
        val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, fsCode)
        val prog = GLES20.glCreateProgram()
        GLES20.glAttachShader(prog, vs)
        GLES20.glAttachShader(prog, fs)
        GLES20.glLinkProgram(prog)
        return prog
    }

    private fun compileShader(type: Int, code: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, code)
        GLES20.glCompileShader(shader)
        return shader
    }

    companion object {
        private const val VERTEX_SHADER = """
            uniform mat4 uMvp;
            uniform mat4 uModel;
            attribute vec3 aPos;
            attribute vec3 aNormal;
            attribute vec4 aColor;
            attribute vec2 aTexCoord;
            varying vec3 vWorldPos;
            varying vec3 vNormal;
            varying vec4 vColor;
            varying vec2 vTexCoord;
            void main() {
                vec4 worldPos = uModel * vec4(aPos, 1.0);
                vWorldPos = worldPos.xyz;
                vNormal = normalize(mat3(uModel) * aNormal);
                vColor = aColor;
                vTexCoord = aTexCoord;
                gl_Position = uMvp * vec4(aPos, 1.0);
            }
        """

        private const val FRAGMENT_SHADER = """
            precision mediump float;
            uniform vec3 uEyePos;
            uniform vec4 uTint;
            uniform float uUnlit;
            uniform float uIsUnit;
            uniform float uTime;
            uniform float uGraphicsLevel;
            uniform float uBrightness;
            uniform sampler2D uTexture;
            uniform float uHasTexture;
            varying vec3 vWorldPos;
            varying vec3 vNormal;
            varying vec4 vColor;
            varying vec2 vTexCoord;

            float gritNoise(vec2 p) {
                return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);
            }

            void main() {
                vec4 texSample = uHasTexture > 0.5 ? texture2D(uTexture, vTexCoord) : vec4(1.0);
                if (uUnlit > 0.5) {
                    vec4 unlitCol = vColor * uTint * texSample;
                    gl_FragColor = vec4(clamp(unlitCol.rgb * uBrightness, 0.0, 1.0), unlitCol.a);
                    return;
                }

                // Self-illuminated emissive optics, plasma coils, hammer energy & crater magma (alpha >= 1.5)
                if (vColor.a > 1.5) {
                    float pulse = 0.90 + 0.16 * sin(uTime * 3.8 + vWorldPos.x * 2.0);
                    float glowBoost = uGraphicsLevel >= 3.5 ? 1.28 : 1.15;
                    vec3 glow = clamp(vColor.rgb * uTint.rgb * pulse * glowBoost * uBrightness, 0.0, 1.0);
                    gl_FragColor = vec4(glow, uTint.a);
                    return;
                }

                vec4 baseCol = vColor * uTint * texSample;
                vec3 N = normalize(vNormal);
                vec3 V = normalize(uEyePos - vWorldPos);

                if (uIsUnit > 0.5) {
                    // Clean tabletop painted ceramite & metallic lighting for 3D miniatures
                    vec3 sunDir = normalize(vec3(0.50, 0.88, 0.45));
                    vec3 fillDir = normalize(vec3(-0.62, 0.48, -0.55));
                    float diffSun = max(dot(N, sunDir), 0.0);
                    float diffFill = max(dot(N, fillDir), 0.0);

                    float hemi = N.y * 0.5 + 0.5;
                    vec3 ambient = mix(vec3(0.14, 0.15, 0.20), vec3(0.28, 0.31, 0.38), hemi);

                    vec3 H = normalize(sunDir + V);
                    float specMult = uGraphicsLevel >= 2.5 ? (uGraphicsLevel >= 3.5 ? 0.46 : 0.36) : 0.0;
                    float rimMult = uGraphicsLevel >= 2.5 ? (uGraphicsLevel >= 3.5 ? 0.28 : 0.20) : 0.0;
                    float spec = pow(max(dot(N, H), 0.0), 28.0) * specMult;
                    float rim = pow(1.0 - max(dot(N, V), 0.0), 3.0) * rimMult;

                    vec3 lit = baseCol.rgb * (ambient + diffSun * vec3(0.98, 0.96, 0.93) * 0.92 + diffFill * vec3(0.32, 0.42, 0.62) * 0.42)
                             + mix(vec3(0.85, 0.90, 0.98), baseCol.rgb, 0.35) * spec
                             + vec3(0.38, 0.52, 0.78) * rim;

                    float dist = length(uEyePos - vWorldPos);
                    float distFog = uGraphicsLevel >= 1.5 ? clamp((dist - 24.0) / 34.0, 0.0, 0.55) : 0.0;
                    vec3 fogColor = vec3(0.032, 0.036, 0.046);
                    vec3 finalUnit = clamp(mix(lit, fogColor, distFog) * uBrightness, 0.0, 1.0);
                    gl_FragColor = vec4(finalUnit, baseCol.a);
                    return;
                }

                // Procedural industrial deck weathering, cracks & stone surface grit for terrain (L2+)
                if (uGraphicsLevel >= 1.5) {
                    float grain = gritNoise(floor(vWorldPos.xz * 26.0)) * 0.12 - 0.06;
                    float seamX = abs(fract(vWorldPos.x + 0.5) - 0.5);
                    float seamZ = abs(fract(vWorldPos.z + 0.5) - 0.5);
                    float crackLine = abs(sin(vWorldPos.x * 3.1 + sin(vWorldPos.z * 2.3) * 1.6));
                    if (vWorldPos.y < 0.04) {
                        float seam = smoothstep(0.47, 0.495, max(seamX, seamZ));
                        float crack = (1.0 - smoothstep(0.0, 0.045, crackLine)) * 0.22;
                        baseCol.rgb *= (1.0 + grain - seam * 0.35 - crack);
                    } else {
                        baseCol.rgb *= (1.0 + grain * 0.75);
                    }
                }

                // Bright Daytime Meadow Sunlight + Sky Ambient
                vec3 sunDir = normalize(vec3(0.55, 0.88, 0.42));
                vec3 fillDir = normalize(vec3(-0.65, 0.52, -0.58));
                float diffSun = max(dot(N, sunDir), 0.0);
                float diffFill = max(dot(N, fillDir), 0.0);

                float hemi = N.y * 0.5 + 0.5;
                vec3 ambient = mix(vec3(0.38, 0.46, 0.34), vec3(0.65, 0.78, 0.92), hemi);

                vec3 H = normalize(sunDir + V);
                float specTerrain = uGraphicsLevel >= 2.5 ? (uGraphicsLevel >= 3.5 ? 0.40 : 0.32) : 0.0;
                float rimTerrain = uGraphicsLevel >= 2.5 ? (uGraphicsLevel >= 3.5 ? 0.24 : 0.18) : 0.0;
                float spec = pow(max(dot(N, H), 0.0), 22.0) * specTerrain;
                float rim = pow(1.0 - max(dot(N, V), 0.0), 3.2) * rimTerrain;

                vec3 lit = baseCol.rgb * (ambient + diffSun * vec3(1.0, 0.96, 0.85) * 1.18 + diffFill * vec3(0.42, 0.56, 0.72) * 0.45)
                         + vec3(1.0, 0.96, 0.88) * spec
                         + vec3(0.45, 0.62, 0.82) * rim;

                // Light sunny atmosphere haze
                float dist = length(uEyePos - vWorldPos);
                float distFog = uGraphicsLevel >= 1.5 ? clamp((dist - 32.0) / 48.0, 0.0, 0.42) : 0.0;
                vec3 fogColor = vec3(0.58, 0.75, 0.90);
                vec3 finalRgb = clamp(mix(lit, fogColor, distFog) * uBrightness, 0.0, 1.0);

                gl_FragColor = vec4(finalRgb, baseCol.a);
            }
        """
    }
}
