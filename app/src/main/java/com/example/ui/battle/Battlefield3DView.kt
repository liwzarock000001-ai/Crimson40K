package com.example.ui.battle

import android.graphics.PixelFormat
import android.opengl.GLSurfaceView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.engine.MapGenerator
import com.example.engine.TacticalRules
import com.example.model.Faction
import com.example.model.GridPos
import com.example.model.UnitArchetype
import com.example.ui.theme.ColorTokens
import com.example.ui.theme.ImperialGold
import com.example.ui.theme.MarineBright
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.XenosViolet
import com.example.viewmodel.BattleUiState
import kotlinx.coroutines.channels.Channel
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun Battlefield3DView(
    state: BattleUiState,
    cameraYawDeltaTrigger: Int = 0,
    cameraYawDeltaDeg: Float = 0f,
    cameraPitchDeg: Float = 46f,
    zoomStepTrigger: Int = 0,
    zoomStepDirection: Int = 0,
    onWorldTap: (Float, Float) -> Unit,
    onJoinSquad: (String, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    remember(context) { MeshCatalog3D.init(context.applicationContext) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current.density
    val textMeasurer = rememberTextMeasurer()

    val cameraRig = remember { SharedCameraRig() }
    val renderer = remember { GlesBattleRenderer(cameraRig) }
    val glView = remember {
        GLSurfaceView(context).apply {
            setEGLContextClientVersion(2)
            setRenderer(renderer)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
    }

    DisposableEffect(lifecycleOwner, glView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> glView.onResume()
                Lifecycle.Event.ON_PAUSE -> glView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            glView.onPause()
        }
    }

    // Update GL scene snapshot without triggering extra work
    renderer.sceneRef.set(state)

    // Draw-phase-only tick counter (never triggers Composition or Layout!)
    var overlayDrawTick by remember { mutableLongStateOf(0L) }
    val overlayWakeChannel = remember { Channel<Unit>(Channel.CONFLATED) }
    val latestOnWorldTap by rememberUpdatedState(onWorldTap)
    val latestState by rememberUpdatedState(state)

    LaunchedEffect(state.focusGridPos, state.centerTrigger) {
        state.focusGridPos?.let { pos ->
            cameraRig.desiredX = pos.x.toFloat()
            cameraRig.desiredZ = pos.y.toFloat()
            overlayWakeChannel.trySend(Unit)
        }
    }

    LaunchedEffect(cameraYawDeltaTrigger) {
        if (cameraYawDeltaTrigger > 0 && abs(cameraYawDeltaDeg) > 0.01f) {
            cameraRig.desiredYawDeg += cameraYawDeltaDeg
            overlayWakeChannel.trySend(Unit)
        }
    }

    LaunchedEffect(cameraPitchDeg) {
        cameraRig.desiredPitchDeg = cameraPitchDeg.coerceIn(14f, 75f)
        overlayWakeChannel.trySend(Unit)
    }

    LaunchedEffect(zoomStepTrigger) {
        if (zoomStepTrigger > 0 && zoomStepDirection != 0) {
            if (zoomStepDirection > 0) {
                // When zooming in from wide tactical distance, snap onto selected unit if available
                if (cameraRig.desiredDistance > 8.5f) {
                    latestState.selectedUnit?.let { u ->
                        cameraRig.desiredX = u.x.toFloat()
                        cameraRig.desiredZ = u.y.toFloat()
                    }
                }
                cameraRig.desiredDistance = (cameraRig.desiredDistance * 0.62f)
                    .coerceIn(Camera3DParams.MIN_DISTANCE, Camera3DParams.MAX_DISTANCE)
            } else {
                cameraRig.desiredDistance = (cameraRig.desiredDistance * 1.55f)
                    .coerceIn(Camera3DParams.MIN_DISTANCE, Camera3DParams.MAX_DISTANCE)
            }
            overlayWakeChannel.trySend(Unit)
        }
    }

    LaunchedEffect(
        state.floatingTexts.isNotEmpty(),
        state.isAnimating,
        state.movePreview,
        state.units,
        state.selectedUnitUid,
        state.isPlayerTurn
    ) {
        if (state.floatingTexts.isNotEmpty() || state.isAnimating) {
            overlayWakeChannel.trySend(Unit)
        } else {
            overlayDrawTick = System.currentTimeMillis()
        }
    }

    // Tick overlay ONLY while camera is settling, unit is animating, or floating combat text is animating (0% CPU when idle!)
    LaunchedEffect(Unit) {
        while (true) {
            overlayWakeChannel.receive()
            while (cameraRig.isSettling() || latestState.floatingTexts.isNotEmpty() || latestState.isAnimating) {
                withFrameMillis {
                    overlayDrawTick = System.currentTimeMillis()
                }
            }
            // Final aligned draw after settling completes
            overlayDrawTick = System.currentTimeMillis()
        }
    }

    // Pre-allocated scratch buffers & text cache for DrawScope
    val vpScratch = remember { FloatArray(16) }
    val projScratch = remember { FloatArray(16) }
    val viewScratch = remember { FloatArray(16) }
    val eyeScratch = remember { FloatArray(3) }
    val vecScratch = remember { FloatArray(4) }
    val clipScratch = remember { FloatArray(4) }
    val outXY = remember { FloatArray(2) }
    val btnVpScratch = remember { FloatArray(16) }
    val btnProjScratch = remember { FloatArray(16) }
    val btnViewScratch = remember { FloatArray(16) }
    val btnEyeScratch = remember { FloatArray(3) }
    val btnVecScratch = remember { FloatArray(4) }
    val btnClipScratch = remember { FloatArray(4) }
    val btnOutXY = remember { FloatArray(2) }
    val plateLabelCache = remember { HashMap<String, TextLayoutResult>(32) }
    val floatTextCache = remember { HashMap<Long, Pair<Long, TextLayoutResult>>(16) }

    val plateTextStyle = remember {
        TextStyle(
            color = Color(0xFFD8D5CC),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val viewW = constraints.maxWidth.toFloat()
        val viewH = constraints.maxHeight.toFloat()

        // 1. Hardware-Accelerated OpenGL ES 2.0 3D Scene (never recomposes on camera/VFX)
        AndroidView(
            factory = { glView },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Stable Touch Gesture Layer (keyed ONLY on viewW/viewH -> never restarted during camera pan!)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(viewW, viewH) {
                    detectTransformGestures { _, pan, zoomChange, rotationChange ->
                        cameraRig.desiredDistance = (cameraRig.desiredDistance / zoomChange)
                            .coerceIn(Camera3DParams.MIN_DISTANCE, Camera3DParams.MAX_DISTANCE)
                        if (abs(rotationChange) > 0.01f) {
                            cameraRig.desiredYawDeg += rotationChange
                        }
                        val sensitivity = cameraRig.desiredDistance * 0.00145f
                        val yawRad = Math.toRadians(cameraRig.currentYawDeg.toDouble())
                        val cosY = cos(yawRad).toFloat()
                        val sinY = sin(yawRad).toFloat()
                        val dxScreen = -pan.x * sensitivity
                        val dzScreen = -pan.y * sensitivity
                        val worldDx = dxScreen * cosY + dzScreen * sinY
                        val worldDz = -dxScreen * sinY + dzScreen * cosY
                        cameraRig.desiredX = (cameraRig.desiredX + worldDx).coerceIn(-1f, MapGenerator.GRID_W.toFloat())
                        cameraRig.desiredZ = (cameraRig.desiredZ + worldDz).coerceIn(-1f, MapGenerator.GRID_H.toFloat())
                        overlayDrawTick = System.currentTimeMillis()
                        overlayWakeChannel.trySend(Unit)
                    }
                }
                .pointerInput(viewW, viewH) {
                    detectTapGestures { tapOffset ->
                        cameraRig.snapshot()
                            .screenToWorld(tapOffset.x, tapOffset.y, viewW, viewH)
                            ?.let { wp -> latestOnWorldTap(wp.x, wp.y) }
                    }
                }
        )

        // 3. Draw-Phase-Only Canvas for Overhead Unit HP Plates & Floating Combat Text
        Canvas(modifier = Modifier.fillMaxSize()) {
            @Suppress("UNUSED_VARIABLE")
            val tick = overlayDrawTick
            val cam = cameraRig.snapshot()
            cam.buildVpMatrixInto(vpScratch, projScratch, viewScratch, eyeScratch, size.width, size.height)

            val st = latestState
            val plateW = 50f * density
            val plateH = 17f * density
            val barPad = 3.5f * density
            val barH = 3.5f * density
            val barW = plateW - barPad * 2f
            val cornerR = CornerRadius(2f * density, 2f * density)

            for (i in st.units.indices) {
                val unit = st.units[i]
                if (!unit.isAlive) continue
                val visible = cam.projectWorldToOut(
                    unit.worldX, 1.18f, unit.worldY,
                    vpScratch, size.width, size.height,
                    vecScratch, clipScratch, outXY
                )
                if (!visible) continue
                val sx = outXY[0]
                val sy = outXY[1]
                if (sx < -60f || sx > size.width + 60f || sy < -40f || sy > size.height + 40f) continue

                val isSelected = unit.uid == st.selectedUnitUid
                val isTeam0 = if (st.isCoopMode) unit.effectiveTeam == 0 else unit.faction == st.playerFaction
                val borderCol = when {
                    isSelected && isTeam0 -> Color(0xFFD9E2EE)
                    isSelected -> Color(0xFFFFB5B5)
                    st.isCoopMode && isTeam0 -> Color(0xFF3A8FD6)
                    st.isCoopMode -> Color(0xFFC9444C)
                    unit.faction == Faction.MARINES -> Color(0xFF345B91)
                    unit.faction == Faction.ALLIANCE_IMPERIUM -> Color(0xFFB33942)
                    unit.faction == Faction.BLACK_LEGION -> Color(0xFFB38E46)
                    else -> Color(0xFF6B3680)
                }
                val fillCol = when {
                    st.isCoopMode && isTeam0 -> Color(0xFF3A8FD6)
                    st.isCoopMode -> Color(0xFFC9444C)
                    unit.faction == Faction.MARINES -> Color(0xFF3B669E)
                    unit.faction == Faction.ALLIANCE_IMPERIUM -> Color(0xFFC93D47)
                    unit.faction == Faction.BLACK_LEGION -> Color(0xFFC99E44)
                    else -> Color(0xFF7D4394)
                }
                val woundsFrac = (unit.totalWounds.toFloat() / unit.maxTotalWounds.coerceAtLeast(1)).coerceIn(0f, 1f)

                val plateAlpha = ((cam.distance - 1.35f) / 1.4f).coerceIn(0.28f, 1f)
                val leaderLift = if (unit.bodyguardUid != null) 26f * density else 0f
                val left = sx - plateW * 0.5f
                val top = sy - plateH - 4f * density - leaderLift

                drawRoundRect(
                    color = Color(0xE60B0D11).copy(alpha = 0.90f * plateAlpha),
                    topLeft = Offset(left, top),
                    size = Size(plateW, plateH),
                    cornerRadius = cornerR
                )
                drawRoundRect(
                    color = borderCol.copy(alpha = plateAlpha),
                    topLeft = Offset(left, top),
                    size = Size(plateW, plateH),
                    cornerRadius = cornerR,
                    style = Stroke(width = (if (isSelected) 1.5f else 1f) * density)
                )

                val label = if (st.isCoopMode) {
                    "${unit.ownerPlayerId.uppercase()} ${unit.aliveModels}x"
                } else {
                    "${unit.aliveModels}x ${unit.totalWounds}W"
                }
                if (plateLabelCache.size > 128) plateLabelCache.clear()
                val measured = plateLabelCache.getOrPut(label) {
                    textMeasurer.measure(label, plateTextStyle)
                }
                drawText(
                    textLayoutResult = measured,
                    topLeft = Offset(sx - measured.size.width * 0.5f, top + 1f * density),
                    alpha = plateAlpha
                )

                val barTop = top + plateH - barH - 2.5f * density
                drawRect(
                    color = Color(0xFF1A1D24).copy(alpha = plateAlpha),
                    topLeft = Offset(left + barPad, barTop),
                    size = Size(barW, barH)
                )
                drawRect(
                    color = fillCol.copy(alpha = plateAlpha),
                    topLeft = Offset(left + barPad, barTop),
                    size = Size(barW * woundsFrac, barH)
                )

                if (unit.isMarked) {
                    val markLabel = "◎ MARKED +1 HIT"
                    val markMeasured = plateLabelCache.getOrPut(markLabel) {
                        textMeasurer.measure(
                            markLabel,
                            TextStyle(
                                color = Color(0xFFFFD166),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Black
                            )
                        )
                    }
                    val mPadX = 6f * density
                    val mPadY = 2.5f * density
                    val mW = markMeasured.size.width + mPadX * 2f
                    val mH = markMeasured.size.height + mPadY * 2f
                    val mLeft = sx - mW * 0.5f
                    val mTop = top - mH - 3.5f * density
                    drawRoundRect(
                        color = Color(0xEE2A080C).copy(alpha = plateAlpha),
                        topLeft = Offset(mLeft, mTop),
                        size = Size(mW, mH),
                        cornerRadius = cornerR
                    )
                    drawRoundRect(
                        color = Color(0xFFFF6B35).copy(alpha = plateAlpha),
                        topLeft = Offset(mLeft, mTop),
                        size = Size(mW, mH),
                        cornerRadius = cornerR,
                        style = Stroke(width = 1.3f * density)
                    )
                    drawText(
                        textLayoutResult = markMeasured,
                        topLeft = Offset(mLeft + mPadX, mTop + mPadY),
                        alpha = plateAlpha
                    )
                }

                // In-world Leader badge when Hero is attached to a Bodyguard
                if (unit.bodyguardUid != null) {
                    val leaderBadge = "🛡️ LEADER"
                    val lMeasured = plateLabelCache.getOrPut(leaderBadge) {
                        textMeasurer.measure(
                            leaderBadge,
                            TextStyle(color = Color(0xFFFFD166), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                    val lPadX = 5f * density
                    val lPadY = 2f * density
                    val lW = lMeasured.size.width + lPadX * 2f
                    val lH = lMeasured.size.height + lPadY * 2f
                    val lLeft = sx - lW * 0.5f
                    val lTop = top - lH - (if (unit.isMarked) 22f else 3.5f) * density
                    drawRoundRect(
                        color = Color(0xEE2A1E0B).copy(alpha = plateAlpha),
                        topLeft = Offset(lLeft, lTop),
                        size = Size(lW, lH),
                        cornerRadius = cornerR
                    )
                    drawRoundRect(
                        color = Color(0xFFFFD166).copy(alpha = plateAlpha),
                        topLeft = Offset(lLeft, lTop),
                        size = Size(lW, lH),
                        cornerRadius = cornerR,
                        style = Stroke(width = 1f * density)
                    )
                    drawText(
                        textLayoutResult = lMeasured,
                        topLeft = Offset(lLeft + lPadX, lTop + lPadY),
                        alpha = plateAlpha
                    )
                }
            }

            // CM Ruler Preview Badge (active commanding player only)
            val sel = st.selectedUnit
            if (st.isPlayerTurn && sel != null && st.canCommandUnit(sel)) {
                st.movePreview?.let { preview ->
                val visible = cam.projectWorldToOut(
                    preview.targetX, 0.52f, preview.targetY,
                    vpScratch, size.width, size.height,
                    vecScratch, clipScratch, outXY
                )
                if (visible) {
                    val sx = outXY[0]
                    val sy = outXY[1]
                    val cmText = "%.1f cm".format(preview.distanceCm)
                    if (plateLabelCache.size > 128) plateLabelCache.clear()
                    val measured = plateLabelCache.getOrPut(cmText) {
                        textMeasurer.measure(
                            cmText,
                            TextStyle(
                                color = Color(0xFF4CC9F0),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        )
                    }
                    val padX = 7f * density
                    val padY = 3f * density
                    val boxW = measured.size.width + padX * 2f
                    val boxH = measured.size.height + padY * 2f
                    val bx = sx - boxW * 0.5f
                    val by = sy - boxH - 4f * density
                    drawRoundRect(
                        color = Color(0xEE081018),
                        topLeft = Offset(bx, by),
                        size = Size(boxW, boxH),
                        cornerRadius = cornerR
                    )
                    drawRoundRect(
                        color = Color(0xFF4CC9F0),
                        topLeft = Offset(bx, by),
                        size = Size(boxW, boxH),
                        cornerRadius = cornerR,
                        style = Stroke(width = 1.2f * density)
                    )
                    drawText(
                        textLayoutResult = measured,
                        topLeft = Offset(bx + padX, by + padY)
                    )
                }
            }
            }

            // Floating Combat Text
            if (st.floatingTexts.isNotEmpty()) {
                val now = System.currentTimeMillis()
                if (floatTextCache.size > 32) floatTextCache.clear()
                for (i in st.floatingTexts.indices) {
                    val ft = st.floatingTexts[i]
                    val elapsed = now - ft.startTimeMs
                    if (elapsed in 0L..2700L) {
                        val t = elapsed / 2700f
                        val visible = cam.projectWorldToOut(
                            ft.gridX, 1.45f + t * 1.15f, ft.gridY,
                            vpScratch, size.width, size.height,
                            vecScratch, clipScratch, outXY
                        )
                        if (visible) {
                            val alpha = (1f - t * t).coerceIn(0f, 1f)
                            val cached = floatTextCache.getOrPut(ft.id) {
                                ft.colorArgb to textMeasurer.measure(
                                    ft.text,
                                    TextStyle(
                                        color = Color(ft.colorArgb),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                )
                            }
                            drawText(
                                textLayoutResult = cached.second,
                                topLeft = Offset(
                                    outXY[0] - cached.second.size.width * 0.5f,
                                    outXY[1] - cached.second.size.height * 0.5f
                                ),
                                alpha = alpha
                            )
                        }
                    }
                }
            }
        }

        // 4. Interactive Overhead "JOIN" UI Button above Captain Titus when within 2cm of an eligible squad
        val joinCandidates = remember(
            state.units,
            state.isPlayerTurn,
            state.isAnimating,
            state.isDeploymentPhase,
            state.isCoopMode,
            state.activeCoopPlayerId
        ) {
            if (!state.isPlayerTurn || state.isAnimating || state.isDeploymentPhase) {
                emptyList()
            } else {
                state.units.mapNotNull { hero ->
                    val isHero = hero.isHeroLeader
                    if (!hero.isAlive || !isHero || hero.bodyguardUid != null) return@mapNotNull null
                    val squad = state.units.firstOrNull { other ->
                        other.isAlive && other.uid != hero.uid && other.isAllyOf(hero) &&
                            TacticalRules.canLeadSquad(hero, other) &&
                            TacticalRules.isWithinJoiningReach(hero, other) &&
                            (state.canCommandUnit(hero) || state.canCommandUnit(other))
                    } ?: return@mapNotNull null
                    hero to squad
                }
            }
        }

        joinCandidates.forEach { (hero, squad) ->
            OutlinedButton(
                onClick = { onJoinSquad(hero.uid, squad.uid) },
                shape = CutCornerShape(6.dp),
                border = BorderStroke(1.5.dp, ImperialGold),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = ColorTokens.CyanSurface.copy(alpha = 0.96f),
                    contentColor = TacticalCyan
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier
                    .offset {
                        @Suppress("UNUSED_VARIABLE")
                        val tick = overlayDrawTick
                        val cam = cameraRig.snapshot()
                        cam.buildVpMatrixInto(btnVpScratch, btnProjScratch, btnViewScratch, btnEyeScratch, viewW, viewH)
                        val visible = cam.projectWorldToOut(
                            hero.worldX, 1.38f, hero.worldY,
                            btnVpScratch, viewW, viewH,
                            btnVecScratch, btnClipScratch, btnOutXY
                        )
                        if (!visible) {
                            IntOffset(-5000, -5000)
                        } else {
                            val btnW = (76f * density).roundToInt()
                            val btnH = (48f * density).roundToInt()
                            val px = (btnOutXY[0] - btnW * 0.5f).roundToInt()
                            val py = (btnOutXY[1] - btnH - (if (hero.isMarked) 34f else 18f) * density).roundToInt()
                            if (px < -140 || px > viewW.roundToInt() + 140 || py < -140 || py > viewH.roundToInt() + 140) {
                                IntOffset(-5000, -5000)
                            } else {
                                IntOffset(px, py)
                            }
                        }
                    }
                    .minimumInteractiveComponentSize()
                    .testTag("join_overhead_button")
            ) {
                Text(
                    text = "JOIN",
                    color = ImperialGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}
