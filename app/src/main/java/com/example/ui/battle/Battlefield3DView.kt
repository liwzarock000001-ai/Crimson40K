package com.example.ui.battle

import android.graphics.PixelFormat
import android.opengl.GLSurfaceView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.engine.MapGenerator
import com.example.model.Faction
import com.example.model.GridPos
import com.example.ui.theme.MarineBright
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.XenosViolet
import com.example.viewmodel.BattleUiState
import kotlinx.coroutines.channels.Channel
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun Battlefield3DView(
    state: BattleUiState,
    cameraYawQuarter: Int,
    zoomStepTrigger: Int = 0,
    zoomStepDirection: Int = 0,
    onTileTap: (GridPos) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current.density
    val textMeasurer = rememberTextMeasurer()

    val cameraRig = remember { SharedCameraRig() }
    val renderer = remember { GlesBattleRenderer(cameraRig) }
    val glView = remember {
        GLSurfaceView(context).apply {
            setEGLContextClientVersion(2)
            setEGLConfigChooser(8, 8, 8, 8, 16, 0)
            holder.setFormat(PixelFormat.TRANSLUCENT)
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
    val latestOnTileTap by rememberUpdatedState(onTileTap)
    val latestState by rememberUpdatedState(state)

    LaunchedEffect(state.focusGridPos, state.centerTrigger) {
        state.focusGridPos?.let { pos ->
            cameraRig.desiredX = pos.x.toFloat()
            cameraRig.desiredZ = pos.y.toFloat()
            overlayWakeChannel.trySend(Unit)
        }
    }

    LaunchedEffect(cameraYawQuarter) {
        cameraRig.desiredYawDeg = 26f + cameraYawQuarter * 90f
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

    LaunchedEffect(state.floatingTexts.isNotEmpty()) {
        if (state.floatingTexts.isNotEmpty()) {
            overlayWakeChannel.trySend(Unit)
        }
    }

    // Tick overlay ONLY while camera is settling or floating combat text is animating (0% CPU when idle!)
    LaunchedEffect(Unit) {
        while (true) {
            overlayWakeChannel.receive()
            while (cameraRig.isSettling() || latestState.floatingTexts.isNotEmpty()) {
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
                    detectTransformGestures { _, pan, zoomChange, _ ->
                        cameraRig.desiredDistance = (cameraRig.desiredDistance / zoomChange)
                            .coerceIn(Camera3DParams.MIN_DISTANCE, Camera3DParams.MAX_DISTANCE)
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
                            .screenToGrid(tapOffset.x, tapOffset.y, viewW, viewH)
                            ?.let { latestOnTileTap(it) }
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
                    unit.x.toFloat(), 1.18f, unit.y.toFloat(),
                    vpScratch, size.width, size.height,
                    vecScratch, clipScratch, outXY
                )
                if (!visible) continue
                val sx = outXY[0]
                val sy = outXY[1]
                if (sx < -60f || sx > size.width + 60f || sy < -40f || sy > size.height + 40f) continue

                val isSelected = unit.uid == st.selectedUnitUid
                val borderCol = when {
                    isSelected && unit.faction == Faction.MARINES -> Color(0xFFD9E2EE)
                    isSelected -> Color(0xFFC7B2AC)
                    unit.faction == Faction.MARINES -> Color(0xFF345B91)
                    else -> Color(0xFF6B3680)
                }
                val fillCol = if (unit.faction == Faction.MARINES) Color(0xFF3B669E) else Color(0xFF7D4394)
                val hpFrac = (unit.totalHp.toFloat() / unit.maxTotalHp.coerceAtLeast(1)).coerceIn(0f, 1f)

                val plateAlpha = ((cam.distance - 1.35f) / 1.4f).coerceIn(0.28f, 1f)
                val left = sx - plateW * 0.5f
                val top = sy - plateH - 4f * density

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

                val label = "${unit.aliveModels}x ${unit.totalHp}"
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
                    size = Size(barW * hpFrac, barH)
                )
            }

            // Floating Combat Text
            if (st.floatingTexts.isNotEmpty()) {
                val now = System.currentTimeMillis()
                if (floatTextCache.size > 32) floatTextCache.clear()
                for (i in st.floatingTexts.indices) {
                    val ft = st.floatingTexts[i]
                    val elapsed = (now - ft.startTimeMs).coerceAtLeast(0L)
                    if (elapsed in 0L..1350L) {
                        val t = elapsed / 1350f
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
    }
}
