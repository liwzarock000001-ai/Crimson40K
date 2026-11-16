package com.example.ui.battle

import android.opengl.Matrix
import com.example.engine.MapGenerator
import com.example.model.GridPos
import com.example.model.WorldPos
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

data class Camera3DParams(
    val targetX: Float = MapGenerator.GRID_W / 2f - 0.5f,
    val targetZ: Float = MapGenerator.GRID_H / 2f - 0.5f,
    val distance: Float = DEFAULT_DISTANCE,
    val pitchDeg: Float = 46f,
    val yawDeg: Float = 26f
) {
    /**
     * Smoothly lowers camera pitch and raises focus height when zooming in close (< 6.5 units)
     * so players can inspect 3D miniatures nearly eye-to-eye rather than looking straight down.
     */
    private fun closeUpBlend(): Float {
        val t = ((6.5f - distance) / (6.5f - MIN_DISTANCE)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    val effectivePitchDeg: Float
        get() {
            val blend = closeUpBlend()
            val closeTarget = minOf(pitchDeg, 18f)
            return pitchDeg + (closeTarget - pitchDeg) * blend
        }

    val effectiveTargetY: Float
        get() {
            val blend = closeUpBlend()
            return 0.25f + (0.48f - 0.25f) * blend
        }

    fun buildVpMatrixInto(
        outVp: FloatArray,
        projScratch: FloatArray,
        viewScratch: FloatArray,
        eyeOut: FloatArray,
        width: Float,
        height: Float
    ) {
        if (width <= 1f || height <= 1f) {
            Matrix.setIdentityM(outVp, 0)
            return
        }
        val aspect = width / height
        Matrix.perspectiveM(projScratch, 0, 42f, aspect, 0.12f, 140f)

        val pitchRad = Math.toRadians(effectivePitchDeg.toDouble())
        val yawRad = Math.toRadians(yawDeg.toDouble())
        val horizDist = (distance * cos(pitchRad)).toFloat()
        val lookY = effectiveTargetY
        val eyeY = lookY + (distance * sin(pitchRad)).toFloat()
        val eyeX = targetX + (horizDist * sin(yawRad)).toFloat()
        val eyeZ = targetZ + (horizDist * cos(yawRad)).toFloat()

        eyeOut[0] = eyeX
        eyeOut[1] = eyeY
        eyeOut[2] = eyeZ

        Matrix.setLookAtM(
            viewScratch, 0,
            eyeX, eyeY, eyeZ,
            targetX, lookY, targetZ,
            0f, 1f, 0f
        )
        Matrix.multiplyMM(outVp, 0, projScratch, 0, viewScratch, 0)
    }

    fun projectWorldToOut(
        wx: Float,
        wy: Float,
        wz: Float,
        vp: FloatArray,
        width: Float,
        height: Float,
        vecScratch: FloatArray,
        clipScratch: FloatArray,
        outXY: FloatArray
    ): Boolean {
        vecScratch[0] = wx
        vecScratch[1] = wy
        vecScratch[2] = wz
        vecScratch[3] = 1f
        Matrix.multiplyMV(clipScratch, 0, vp, 0, vecScratch, 0)
        val w = clipScratch[3]
        if (w <= 0.05f) return false
        val ndcX = clipScratch[0] / w
        val ndcY = clipScratch[1] / w
        outXY[0] = (ndcX * 0.5f + 0.5f) * width
        outXY[1] = (0.5f - ndcY * 0.5f) * height
        return true
    }

    fun screenToWorld(
        sx: Float,
        sy: Float,
        width: Float,
        height: Float
    ): WorldPos? {
        if (width <= 1f || height <= 1f) return null
        val vp = FloatArray(16)
        val proj = FloatArray(16)
        val view = FloatArray(16)
        val eye = FloatArray(3)
        buildVpMatrixInto(vp, proj, view, eye, width, height)

        val invVp = FloatArray(16)
        if (!Matrix.invertM(invVp, 0, vp, 0)) return null

        val ndcX = (sx / width) * 2f - 1f
        val ndcY = 1f - (sy / height) * 2f

        val nearClip = floatArrayOf(ndcX, ndcY, -1f, 1f)
        val farClip = floatArrayOf(ndcX, ndcY, 1f, 1f)
        val nearWorld = FloatArray(4)
        val farWorld = FloatArray(4)

        Matrix.multiplyMV(nearWorld, 0, invVp, 0, nearClip, 0)
        Matrix.multiplyMV(farWorld, 0, invVp, 0, farClip, 0)

        if (abs(nearWorld[3]) < 1e-5f || abs(farWorld[3]) < 1e-5f) return null
        val nx = nearWorld[0] / nearWorld[3]
        val ny = nearWorld[1] / nearWorld[3]
        val nz = nearWorld[2] / nearWorld[3]

        val fx = farWorld[0] / farWorld[3]
        val fy = farWorld[1] / farWorld[3]
        val fz = farWorld[2] / farWorld[3]

        val dy = fy - ny
        if (abs(dy) < 1e-5f) return null
        val t = -ny / dy
        if (t < 0f) return null

        val hitX = nx + t * (fx - nx)
        val hitZ = nz + t * (fz - nz)

        return if (hitX in -0.45f..(MapGenerator.GRID_W - 0.55f) && hitZ in -0.45f..(MapGenerator.GRID_H - 0.55f)) {
            WorldPos(hitX, hitZ)
        } else null
    }

    fun screenToGrid(
        sx: Float,
        sy: Float,
        width: Float,
        height: Float
    ): GridPos? {
        val wp = screenToWorld(sx, sy, width, height) ?: return null
        val gx = wp.x.roundToInt()
        val gy = wp.y.roundToInt()
        return if (gx in 0 until MapGenerator.GRID_W && gy in 0 until MapGenerator.GRID_H) {
            GridPos(gx, gy)
        } else null
    }

    companion object {
        const val MIN_DISTANCE = 0.90f
        const val MAX_DISTANCE = 26f
        const val DEFAULT_DISTANCE = 15.0f
    }
}

/**
 * Lock-free shared camera target & interpolated state between UI gestures and GL thread.
 */
class SharedCameraRig {
    @Volatile var desiredX: Float = MapGenerator.GRID_W / 2f - 0.5f
    @Volatile var desiredZ: Float = MapGenerator.GRID_H / 2f - 0.5f
    @Volatile var desiredDistance: Float = Camera3DParams.DEFAULT_DISTANCE
    @Volatile var desiredYawDeg: Float = 26f
    @Volatile var desiredPitchDeg: Float = 46f

    @Volatile var currentX: Float = desiredX
    @Volatile var currentZ: Float = desiredZ
    @Volatile var currentDistance: Float = desiredDistance
    @Volatile var currentYawDeg: Float = desiredYawDeg
    @Volatile var currentPitchDeg: Float = desiredPitchDeg

    fun step(alpha: Float = 0.08f): Boolean {
        val dx = desiredX - currentX
        val dz = desiredZ - currentZ
        val dd = desiredDistance - currentDistance
        val dpitch = desiredPitchDeg - currentPitchDeg
        var dyaw = (desiredYawDeg - currentYawDeg) % 360f
        if (dyaw > 180f) dyaw -= 360f
        if (dyaw < -180f) dyaw += 360f

        val moving = abs(dx) > 0.002f || abs(dz) > 0.002f || abs(dd) > 0.004f || abs(dyaw) > 0.05f || abs(dpitch) > 0.05f
        if (moving) {
            currentX += dx * alpha
            currentZ += dz * alpha
            currentDistance += dd * alpha
            currentYawDeg += dyaw * alpha
            currentPitchDeg += dpitch * alpha
        } else {
            currentX = desiredX
            currentZ = desiredZ
            currentDistance = desiredDistance
            currentYawDeg = desiredYawDeg
            currentPitchDeg = desiredPitchDeg
        }
        return moving
    }

    fun isSettling(): Boolean {
        val dx = desiredX - currentX
        val dz = desiredZ - currentZ
        val dd = desiredDistance - currentDistance
        val dpitch = desiredPitchDeg - currentPitchDeg
        var dyaw = (desiredYawDeg - currentYawDeg) % 360f
        if (dyaw > 180f) dyaw -= 360f
        if (dyaw < -180f) dyaw += 360f
        return abs(dx) > 0.002f || abs(dz) > 0.002f || abs(dd) > 0.004f || abs(dyaw) > 0.05f || abs(dpitch) > 0.05f
    }

    fun snapshot(): Camera3DParams = Camera3DParams(
        targetX = currentX,
        targetZ = currentZ,
        distance = currentDistance,
        pitchDeg = currentPitchDeg,
        yawDeg = currentYawDeg
    )
}
