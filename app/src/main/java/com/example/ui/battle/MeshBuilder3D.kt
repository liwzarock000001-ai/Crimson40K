package com.example.ui.battle

import com.example.engine.MapGenerator
import com.example.model.SectorMap
import com.example.model.TerrainType
import com.example.model.UnitArchetype
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Interleaved vertex layout: [x, y, z, nx, ny, nz, r, g, b, a] (10 floats = 40 bytes per vertex).
 * Convention: a >= 1.5f marks self-illuminated emissive surfaces (eye lenses, plasma, hammer arcs, magma).
 */
class MeshGeometry(val data: FloatArray) {
    val vertexCount: Int = data.size / 10
    val buffer: FloatBuffer = ByteBuffer.allocateDirect(data.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply {
            put(data)
            position(0)
        }
}

class MeshAccumulator {
    private val floats = ArrayList<Float>(4096)

    fun addTriangle(
        ax: Float, ay: Float, az: Float,
        bx: Float, by: Float, bz: Float,
        cx: Float, cy: Float, cz: Float,
        r: Float, g: Float, b: Float, a: Float = 1f
    ) {
        val ux = bx - ax
        val uy = by - ay
        val uz = bz - az
        val vx = cx - ax
        val vy = cy - ay
        val vz = cz - az
        var nx = uy * vz - uz * vy
        var ny = uz * vx - ux * vz
        var nz = ux * vy - uy * vx
        val len = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(1e-5f)
        nx /= len
        ny /= len
        nz /= len

        pushVertex(ax, ay, az, nx, ny, nz, r, g, b, a)
        pushVertex(bx, by, bz, nx, ny, nz, r, g, b, a)
        pushVertex(cx, cy, cz, nx, ny, nz, r, g, b, a)
    }

    fun addTriangleSmooth(
        ax: Float, ay: Float, az: Float, nax: Float, nay: Float, naz: Float,
        bx: Float, by: Float, bz: Float, nbx: Float, nby: Float, nbz: Float,
        cx: Float, cy: Float, cz: Float, ncx: Float, ncy: Float, ncz: Float,
        r: Float, g: Float, b: Float, a: Float = 1f
    ) {
        pushVertex(ax, ay, az, nax, nay, naz, r, g, b, a)
        pushVertex(bx, by, bz, nbx, nby, nbz, r, g, b, a)
        pushVertex(cx, cy, cz, ncx, ncy, ncz, r, g, b, a)
    }

    private fun pushVertex(
        x: Float, y: Float, z: Float,
        nx: Float, ny: Float, nz: Float,
        r: Float, g: Float, b: Float, a: Float
    ) {
        floats.add(x)
        floats.add(y)
        floats.add(z)
        floats.add(nx)
        floats.add(ny)
        floats.add(nz)
        floats.add(r)
        floats.add(g)
        floats.add(b)
        floats.add(a)
    }

    fun addQuad(
        ax: Float, ay: Float, az: Float,
        bx: Float, by: Float, bz: Float,
        cx: Float, cy: Float, cz: Float,
        dx: Float, dy: Float, dz: Float,
        r: Float, g: Float, b: Float, a: Float = 1f
    ) {
        addTriangle(ax, ay, az, bx, by, bz, cx, cy, cz, r, g, b, a)
        addTriangle(ax, ay, az, cx, cy, cz, dx, dy, dz, r, g, b, a)
    }

    fun addBox(
        cx: Float, cy: Float, cz: Float,
        sx: Float, sy: Float, sz: Float,
        r: Float, g: Float, b: Float, a: Float = 1f
    ) {
        val hx = sx * 0.5f
        val hy = sy * 0.5f
        val hz = sz * 0.5f
        val x0 = cx - hx; val x1 = cx + hx
        val y0 = cy - hy; val y1 = cy + hy
        val z0 = cz - hz; val z1 = cz + hz

        addQuad(x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, r * 1.06f, g * 1.06f, b * 1.06f, a)
        addQuad(x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, r * 0.65f, g * 0.65f, b * 0.65f, a)
        addQuad(x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, r * 0.96f, g * 0.96f, b * 0.96f, a)
        addQuad(x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, r * 0.86f, g * 0.86f, b * 0.86f, a)
        addQuad(x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, r * 0.92f, g * 0.92f, b * 0.92f, a)
        addQuad(x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, r * 0.80f, g * 0.80f, b * 0.80f, a)
    }

    fun addOrientedBox(
        cx: Float, cy: Float, cz: Float,
        sx: Float, sy: Float, sz: Float,
        rotXDeg: Float = 0f, rotYDeg: Float = 0f, rotZDeg: Float = 0f,
        r: Float, g: Float, b: Float, a: Float = 1f
    ) {
        val hx = sx * 0.5f
        val hy = sy * 0.5f
        val hz = sz * 0.5f
        val corners = arrayOf(
            floatArrayOf(-hx, -hy, -hz),
            floatArrayOf(hx, -hy, -hz),
            floatArrayOf(hx, -hy, hz),
            floatArrayOf(-hx, -hy, hz),
            floatArrayOf(-hx, hy, -hz),
            floatArrayOf(hx, hy, -hz),
            floatArrayOf(hx, hy, hz),
            floatArrayOf(-hx, hy, hz)
        )
        for (c in corners) {
            rotatePointInPlace(c, rotXDeg, rotYDeg, rotZDeg)
            c[0] += cx; c[1] += cy; c[2] += cz
        }
        fun q(i0: Int, i1: Int, i2: Int, i3: Int, mul: Float) {
            addQuad(
                corners[i0][0], corners[i0][1], corners[i0][2],
                corners[i1][0], corners[i1][1], corners[i1][2],
                corners[i2][0], corners[i2][1], corners[i2][2],
                corners[i3][0], corners[i3][1], corners[i3][2],
                r * mul, g * mul, b * mul, a
            )
        }
        q(7, 6, 5, 4, 1.06f) // +Y
        q(0, 1, 2, 3, 0.65f) // -Y
        q(3, 2, 6, 7, 0.96f) // +Z
        q(1, 0, 4, 5, 0.86f) // -Z
        q(2, 1, 5, 6, 0.92f) // +X
        q(0, 3, 7, 4, 0.80f) // -X
    }

    fun addCylinder(
        cx: Float, yBottom: Float, cz: Float,
        radiusBottom: Float, radiusTop: Float, height: Float,
        segments: Int,
        r: Float, g: Float, b: Float, a: Float = 1f
    ) {
        val yTop = yBottom + height
        for (i in 0 until segments) {
            val a0 = (2.0 * PI * i / segments).toFloat()
            val a1 = (2.0 * PI * (i + 1) / segments).toFloat()
            val c0 = cos(a0); val s0 = sin(a0)
            val c1 = cos(a1); val s1 = sin(a1)

            val bx0 = cx + c0 * radiusBottom; val bz0 = cz + s0 * radiusBottom
            val bx1 = cx + c1 * radiusBottom; val bz1 = cz + s1 * radiusBottom
            val tx0 = cx + c0 * radiusTop; val tz0 = cz + s0 * radiusTop
            val tx1 = cx + c1 * radiusTop; val tz1 = cz + s1 * radiusTop

            addQuad(bx0, yBottom, bz0, bx1, yBottom, bz1, tx1, yTop, tz1, tx0, yTop, tz0, r, g, b, a)
            if (radiusTop > 0.001f) {
                addTriangle(cx, yTop, cz, tx0, yTop, tz0, tx1, yTop, tz1, r * 1.08f, g * 1.08f, b * 1.08f, a)
            }
        }
    }

    /**
     * Smooth tapered limb/barrel/greave cylinder directly connecting two 3D points p0 -> p1.
     */
    fun addOrientedCylinder(
        p0x: Float, p0y: Float, p0z: Float,
        p1x: Float, p1y: Float, p1z: Float,
        r0: Float, r1: Float,
        segments: Int = 10,
        capStart: Boolean = true,
        capEnd: Boolean = true,
        r: Float, g: Float, b: Float, a: Float = 1f
    ) {
        var ax = p1x - p0x
        var ay = p1y - p0y
        var az = p1z - p0z
        val len = sqrt(ax * ax + ay * ay + az * az).coerceAtLeast(1e-5f)
        ax /= len; ay /= len; az /= len

        // Orthonormal basis (u, v) perpendicular to axis a
        val refX = if (kotlin.math.abs(ay) < 0.9f) 0f else 1f
        val refY = if (kotlin.math.abs(ay) < 0.9f) 1f else 0f
        val refZ = 0f
        var ux = ay * refZ - az * refY
        var uy = az * refX - ax * refZ
        var uz = ax * refY - ay * refX
        val uLen = sqrt(ux * ux + uy * uy + uz * uz).coerceAtLeast(1e-5f)
        ux /= uLen; uy /= uLen; uz /= uLen

        val vx = ay * uz - az * uy
        val vy = az * ux - ax * uz
        val vz = ax * uy - ay * ux

        val slope = (r0 - r1) / len
        for (i in 0 until segments) {
            val t0 = (2.0 * PI * i / segments).toFloat()
            val t1 = (2.0 * PI * (i + 1) / segments).toFloat()
            val c0 = cos(t0); val s0 = sin(t0)
            val c1 = cos(t1); val s1 = sin(t1)

            val d0x = ux * c0 + vx * s0; val d0y = uy * c0 + vy * s0; val d0z = uz * c0 + vz * s0
            val d1x = ux * c1 + vx * s1; val d1y = uy * c1 + vy * s1; val d1z = uz * c1 + vz * s1

            // Smooth surface normals tilted by taper slope
            var n0x = d0x + ax * slope; var n0y = d0y + ay * slope; var n0z = d0z + az * slope
            val nl0 = sqrt(n0x * n0x + n0y * n0y + n0z * n0z).coerceAtLeast(1e-5f)
            n0x /= nl0; n0y /= nl0; n0z /= nl0

            var n1x = d1x + ax * slope; var n1y = d1y + ay * slope; var n1z = d1z + az * slope
            val nl1 = sqrt(n1x * n1x + n1y * n1y + n1z * n1z).coerceAtLeast(1e-5f)
            n1x /= nl1; n1y /= nl1; n1z /= nl1

            val bx0 = p0x + d0x * r0; val by0 = p0y + d0y * r0; val bz0 = p0z + d0z * r0
            val bx1 = p0x + d1x * r0; val by1 = p0y + d1y * r0; val bz1 = p0z + d1z * r0
            val tx0 = p1x + d0x * r1; val ty0 = p1y + d0y * r1; val tz0 = p1z + d0z * r1
            val tx1 = p1x + d1x * r1; val ty1 = p1y + d1y * r1; val tz1 = p1z + d1z * r1

            addTriangleSmooth(
                bx0, by0, bz0, n0x, n0y, n0z,
                bx1, by1, bz1, n1x, n1y, n1z,
                tx1, ty1, tz1, n1x, n1y, n1z,
                r, g, b, a
            )
            addTriangleSmooth(
                bx0, by0, bz0, n0x, n0y, n0z,
                tx1, ty1, tz1, n1x, n1y, n1z,
                tx0, ty0, tz0, n0x, n0y, n0z,
                r, g, b, a
            )
            if (capStart && r0 > 0.001f) {
                addTriangleSmooth(
                    p0x, p0y, p0z, -ax, -ay, -az,
                    bx1, by1, bz1, -ax, -ay, -az,
                    bx0, by0, bz0, -ax, -ay, -az,
                    r * 0.78f, g * 0.78f, b * 0.78f, a
                )
            }
            if (capEnd && r1 > 0.001f) {
                addTriangleSmooth(
                    p1x, p1y, p1z, ax, ay, az,
                    tx0, ty0, tz0, ax, ay, az,
                    tx1, ty1, tz1, ax, ay, az,
                    r * 1.06f, g * 1.06f, b * 1.06f, a
                )
            }
        }
    }

    /**
     * Smooth oriented ellipsoid/dome/pauldron shell with analytical smooth vertex normals.
     * phi: polar angle from +Y pole (0 .. PI). theta: azimuth around Y (0 .. 2*PI).
     */
    fun addOrientedEllipsoid(
        cx: Float, cy: Float, cz: Float,
        rx: Float, ry: Float, rz: Float,
        rotXDeg: Float = 0f, rotYDeg: Float = 0f, rotZDeg: Float = 0f,
        latSegs: Int = 6,
        lonSegs: Int = 10,
        phiStartDeg: Float = 0f,
        phiEndDeg: Float = 180f,
        thetaStartDeg: Float = 0f,
        thetaEndDeg: Float = 360f,
        r: Float, g: Float, b: Float, a: Float = 1f
    ) {
        val phi0 = (phiStartDeg * PI / 180.0).toFloat()
        val phi1 = (phiEndDeg * PI / 180.0).toFloat()
        val th0 = (thetaStartDeg * PI / 180.0).toFloat()
        val th1 = (thetaEndDeg * PI / 180.0).toFloat()

        val pos = FloatArray(3)
        val nrm = FloatArray(3)

        fun eval(phi: Float, th: Float, outP: FloatArray, outN: FloatArray) {
            val sp = sin(phi); val cp = cos(phi)
            val ct = cos(th); val st = sin(th)
            val lx = sp * ct
            val ly = cp
            val lz = sp * st

            outP[0] = lx * rx
            outP[1] = ly * ry
            outP[2] = lz * rz
            rotatePointInPlace(outP, rotXDeg, rotYDeg, rotZDeg)
            outP[0] += cx; outP[1] += cy; outP[2] += cz

            outN[0] = lx / rx.coerceAtLeast(1e-4f)
            outN[1] = ly / ry.coerceAtLeast(1e-4f)
            outN[2] = lz / rz.coerceAtLeast(1e-4f)
            rotatePointInPlace(outN, rotXDeg, rotYDeg, rotZDeg)
            val nLen = sqrt(outN[0] * outN[0] + outN[1] * outN[1] + outN[2] * outN[2]).coerceAtLeast(1e-5f)
            outN[0] /= nLen; outN[1] /= nLen; outN[2] /= nLen
        }

        val p00 = FloatArray(3); val n00 = FloatArray(3)
        val p10 = FloatArray(3); val n10 = FloatArray(3)
        val p11 = FloatArray(3); val n11 = FloatArray(3)
        val p01 = FloatArray(3); val n01 = FloatArray(3)

        for (i in 0 until latSegs) {
            val va = phi0 + (phi1 - phi0) * (i.toFloat() / latSegs)
            val vb = phi0 + (phi1 - phi0) * ((i + 1).toFloat() / latSegs)
            for (j in 0 until lonSegs) {
                val ua = th0 + (th1 - th0) * (j.toFloat() / lonSegs)
                val ub = th0 + (th1 - th0) * ((j + 1).toFloat() / lonSegs)

                eval(va, ua, p00, n00)
                eval(vb, ua, p10, n10)
                eval(vb, ub, p11, n11)
                eval(va, ub, p01, n01)

                addTriangleSmooth(
                    p00[0], p00[1], p00[2], n00[0], n00[1], n00[2],
                    p10[0], p10[1], p10[2], n10[0], n10[1], n10[2],
                    p11[0], p11[1], p11[2], n11[0], n11[1], n11[2],
                    r, g, b, a
                )
                addTriangleSmooth(
                    p00[0], p00[1], p00[2], n00[0], n00[1], n00[2],
                    p11[0], p11[1], p11[2], n11[0], n11[1], n11[2],
                    p01[0], p01[1], p01[2], n01[0], n01[1], n01[2],
                    r, g, b, a
                )
            }
        }
        // Dummy read so pos/nrm keep scope neat if refactored
        pos[0] = nrm[0]
    }

    /**
     * Smooth curved arc/torus segment — ideal for Gold Pauldron Trim Rims, Knee Arches, Omega emblem, and Halos.
     */
    fun addArcTube(
        cx: Float, cy: Float, cz: Float,
        majorRx: Float, majorRz: Float, tubeR: Float,
        rotXDeg: Float = 0f, rotYDeg: Float = 0f, rotZDeg: Float = 0f,
        startDeg: Float = 0f, endDeg: Float = 360f,
        arcSegs: Int = 12, tubeSegs: Int = 6,
        r: Float, g: Float, b: Float, a: Float = 1f
    ) {
        val a0 = (startDeg * PI / 180.0).toFloat()
        val a1 = (endDeg * PI / 180.0).toFloat()

        val p00 = FloatArray(3); val n00 = FloatArray(3)
        val p10 = FloatArray(3); val n10 = FloatArray(3)
        val p11 = FloatArray(3); val n11 = FloatArray(3)
        val p01 = FloatArray(3); val n01 = FloatArray(3)

        fun eval(u: Float, v: Float, outP: FloatArray, outN: FloatArray) {
            val cu = cos(u); val su = sin(u)
            val cv = cos(v); val sv = sin(v)
            val ringX = majorRx * cu
            val ringZ = majorRz * su
            // Outward radial in XZ plane
            var radX = cu / majorRx.coerceAtLeast(1e-4f)
            var radZ = su / majorRz.coerceAtLeast(1e-4f)
            val rl = sqrt(radX * radX + radZ * radZ).coerceAtLeast(1e-5f)
            radX /= rl; radZ /= rl

            val nxLocal = radX * cv
            val nyLocal = sv
            val nzLocal = radZ * cv

            outP[0] = ringX + nxLocal * tubeR
            outP[1] = nyLocal * tubeR
            outP[2] = ringZ + nzLocal * tubeR
            rotatePointInPlace(outP, rotXDeg, rotYDeg, rotZDeg)
            outP[0] += cx; outP[1] += cy; outP[2] += cz

            outN[0] = nxLocal; outN[1] = nyLocal; outN[2] = nzLocal
            rotatePointInPlace(outN, rotXDeg, rotYDeg, rotZDeg)
        }

        for (i in 0 until arcSegs) {
            val ua = a0 + (a1 - a0) * (i.toFloat() / arcSegs)
            val ub = a0 + (a1 - a0) * ((i + 1).toFloat() / arcSegs)
            for (j in 0 until tubeSegs) {
                val va = (2.0 * PI * j / tubeSegs).toFloat()
                val vb = (2.0 * PI * (j + 1) / tubeSegs).toFloat()

                eval(ua, va, p00, n00)
                eval(ub, va, p10, n10)
                eval(ub, vb, p11, n11)
                eval(ua, vb, p01, n01)

                addTriangleSmooth(
                    p00[0], p00[1], p00[2], n00[0], n00[1], n00[2],
                    p10[0], p10[1], p10[2], n10[0], n10[1], n10[2],
                    p11[0], p11[1], p11[2], n11[0], n11[1], n11[2],
                    r, g, b, a
                )
                addTriangleSmooth(
                    p00[0], p00[1], p00[2], n00[0], n00[1], n00[2],
                    p11[0], p11[1], p11[2], n11[0], n11[1], n11[2],
                    p01[0], p01[1], p01[2], n01[0], n01[1], n01[2],
                    r, g, b, a
                )
            }
        }
    }

    private fun rotatePointInPlace(p: FloatArray, rotXDeg: Float, rotYDeg: Float, rotZDeg: Float) {
        if (rotZDeg != 0f) {
            val rad = (rotZDeg * PI / 180.0).toFloat()
            val c = cos(rad); val s = sin(rad)
            val x = p[0] * c - p[1] * s
            val y = p[0] * s + p[1] * c
            p[0] = x; p[1] = y
        }
        if (rotXDeg != 0f) {
            val rad = (rotXDeg * PI / 180.0).toFloat()
            val c = cos(rad); val s = sin(rad)
            val y = p[1] * c - p[2] * s
            val z = p[1] * s + p[2] * c
            p[1] = y; p[2] = z
        }
        if (rotYDeg != 0f) {
            val rad = (rotYDeg * PI / 180.0).toFloat()
            val c = cos(rad); val s = sin(rad)
            val x = p[0] * c + p[2] * s
            val z = -p[0] * s + p[2] * c
            p[0] = x; p[2] = z
        }
    }

    fun addDisc(
        cx: Float, y: Float, cz: Float,
        radius: Float,
        segments: Int,
        r: Float, g: Float, b: Float, a: Float = 1f
    ) {
        for (i in 0 until segments) {
            val a0 = (2.0 * PI * i / segments).toFloat()
            val a1 = (2.0 * PI * (i + 1) / segments).toFloat()
            val x0 = cx + cos(a0) * radius; val z0 = cz + sin(a0) * radius
            val x1 = cx + cos(a1) * radius; val z1 = cz + sin(a1) * radius
            addTriangle(cx, y, cz, x1, y, z1, x0, y, z0, r, g, b, a)
        }
    }

    fun addRing(
        cx: Float, y: Float, cz: Float,
        innerR: Float, outerR: Float,
        segments: Int,
        r: Float, g: Float, b: Float, a: Float = 1f
    ) {
        for (i in 0 until segments) {
            val a0 = (2.0 * PI * i / segments).toFloat()
            val a1 = (2.0 * PI * (i + 1) / segments).toFloat()
            val c0 = cos(a0); val s0 = sin(a0)
            val c1 = cos(a1); val s1 = sin(a1)
            addQuad(
                cx + c0 * innerR, y, cz + s0 * innerR,
                cx + c0 * outerR, y, cz + s0 * outerR,
                cx + c1 * outerR, y, cz + s1 * outerR,
                cx + c1 * innerR, y, cz + s1 * innerR,
                r, g, b, a
            )
        }
    }

    fun addMeshTranslated(
        src: MeshGeometry,
        tx: Float,
        ty: Float,
        tz: Float,
        sx: Float = 1f,
        sy: Float = 1f,
        sz: Float = 1f,
        tintR: Float = 1f,
        tintG: Float = 1f,
        tintB: Float = 1f,
        tintA: Float = 1f
    ) {
        val d = src.data
        var i = 0
        while (i < d.size) {
            floats.add(d[i] * sx + tx)
            floats.add(d[i + 1] * sy + ty)
            floats.add(d[i + 2] * sz + tz)
            floats.add(d[i + 3])
            floats.add(d[i + 4])
            floats.add(d[i + 5])
            floats.add(d[i + 6] * tintR)
            floats.add(d[i + 7] * tintG)
            floats.add(d[i + 8] * tintB)
            floats.add(d[i + 9] * tintA)
            i += 10
        }
    }

    fun build(): MeshGeometry = MeshGeometry(floats.toFloatArray())
}

object MeshCatalog3D {

    const val EMISSIVE_ALPHA = 2.0f

    val unitCube: MeshGeometry by lazy {
        MeshAccumulator().apply { addBox(0f, 0f, 0f, 1f, 1f, 1f, 1f, 1f, 1f) }.build()
    }

    val shadowDisc: MeshGeometry by lazy {
        MeshAccumulator().apply {
            addDisc(0f, 0.009f, 0f, 0.44f, 16, 0.01f, 0.01f, 0.015f, 0.36f)
            addDisc(0f, 0.012f, 0f, 0.30f, 16, 0.005f, 0.005f, 0.01f, 0.68f)
        }.build()
    }

    val tileQuad: MeshGeometry by lazy {
        MeshAccumulator().apply {
            addQuad(
                -0.485f, 0.003f, -0.485f,
                -0.485f, 0.003f, 0.485f,
                0.485f, 0.003f, 0.485f,
                0.485f, 0.003f, -0.485f,
                1f, 1f, 1f, 1f
            )
        }.build()
    }

    val ringTile: MeshGeometry by lazy {
        MeshAccumulator().apply {
            addRing(0f, 0.018f, 0f, 0.37f, 0.45f, 20, 1f, 1f, 1f, 0.9f)
        }.build()
    }

    val cmRadiusRing: MeshGeometry by lazy {
        MeshAccumulator().apply {
            addRing(0f, 0.015f, 0f, 0.975f, 1.0f, 48, 1f, 1f, 1f, 0.92f)
        }.build()
    }

    val rangeRadiusRing: MeshGeometry by lazy {
        MeshAccumulator().apply {
            addRing(0f, 0.019f, 0f, 0.972f, 1.0f, 64, 1f, 1f, 1f, 0.96f)
        }.build()
    }

    val facingArrow: MeshGeometry by lazy {
        MeshAccumulator().apply {
            addTriangle(
                0.54f, 0.028f, 0f,
                0.36f, 0.028f, -0.14f,
                0.36f, 0.028f, 0.14f,
                1f, 1f, 1f, 1f
            )
            addBox(0.38f, 0.022f, 0f, 0.12f, 0.02f, 0.15f, 0.85f, 0.85f, 0.85f, 1f)
        }.build()
    }

    val rockBoulder: MeshGeometry by lazy {
        MeshAccumulator().apply {
            val stoneR = 0.52f; val stoneG = 0.56f; val stoneB = 0.60f
            addBox(0f, 0.22f, 0f, 0.82f, 0.44f, 0.76f, stoneR, stoneG, stoneB)
            addOrientedEllipsoid(0.15f, 0.38f, 0.12f, 0.34f, 0.32f, 0.32f, rotXDeg = 15f, rotYDeg = 24f, r = stoneR * 1.12f, g = stoneG * 1.12f, b = stoneB * 1.12f)
            addOrientedEllipsoid(-0.2f, 0.28f, -0.15f, 0.28f, 0.26f, 0.28f, rotXDeg = -20f, rotYDeg = -35f, r = stoneR * 0.85f, g = stoneG * 0.85f, b = stoneB * 0.85f)
            addBox(0.05f, 0.10f, -0.2f, 0.50f, 0.22f, 0.44f, stoneR * 0.9f, stoneG * 0.9f, stoneB * 0.9f)
        }.build()
    }

    val treePine: MeshGeometry by lazy {
        MeshAccumulator().apply {
            // Sturdy trunk
            addCylinder(0f, 0f, 0f, 0.18f, 0.12f, 0.85f, 8, 0.45f, 0.32f, 0.20f)
            // Layered lush green foliage canopy
            addOrientedEllipsoid(0f, 0.85f, 0f, 0.55f, 0.65f, 0.55f, latSegs = 5, lonSegs = 10, r = 0.18f, g = 0.62f, b = 0.24f)
            addOrientedEllipsoid(0f, 1.25f, 0f, 0.42f, 0.52f, 0.42f, latSegs = 4, lonSegs = 8, r = 0.22f, g = 0.72f, b = 0.28f)
            addOrientedEllipsoid(0f, 1.55f, 0f, 0.25f, 0.38f, 0.25f, latSegs = 4, lonSegs = 8, r = 0.28f, g = 0.80f, b = 0.34f)
        }.build()
    }

    val barricade: MeshGeometry by lazy {
        MeshAccumulator().apply {
            addBox(0f, 0.06f, 0f, 0.90f, 0.12f, 0.52f, 0.14f, 0.14f, 0.15f)
            addBox(0f, 0.24f, 0f, 0.82f, 0.28f, 0.28f, 0.24f, 0.25f, 0.27f)
            addBox(-0.28f, 0.25f, 0f, 0.11f, 0.33f, 0.34f, 0.32f, 0.32f, 0.34f)
            addBox(0f, 0.25f, 0f, 0.10f, 0.31f, 0.32f, 0.30f, 0.30f, 0.32f)
            addBox(0.28f, 0.25f, 0f, 0.11f, 0.33f, 0.34f, 0.32f, 0.32f, 0.34f)
            addBox(0f, 0.39f, 0f, 0.84f, 0.05f, 0.22f, 0.54f, 0.42f, 0.18f)
        }.build()
    }

    val crater: MeshGeometry by lazy {
        MeshAccumulator().apply {
            addRing(0f, 0.024f, 0f, 0.27f, 0.47f, 12, 0.16f, 0.14f, 0.13f)
            addCylinder(0f, 0.01f, 0f, 0.46f, 0.37f, 0.065f, 12, 0.12f, 0.11f, 0.11f)
            addDisc(0f, 0.016f, 0f, 0.28f, 12, 0.07f, 0.05f, 0.05f)
            addBox(0f, 0.020f, 0f, 0.26f, 0.014f, 0.045f, 0.95f, 0.34f, 0.06f, EMISSIVE_ALPHA)
            addBox(0f, 0.020f, 0f, 0.045f, 0.014f, 0.24f, 0.85f, 0.25f, 0.05f, EMISSIVE_ALPHA)
        }.build()
    }

    val objectiveTile: MeshGeometry by lazy {
        MeshAccumulator().apply {
            addCylinder(0f, 0.005f, 0f, 0.47f, 0.43f, 0.06f, 8, 0.14f, 0.12f, 0.14f)
            addRing(0f, 0.07f, 0f, 0.26f, 0.39f, 16, 0.62f, 0.49f, 0.21f)
            addDisc(0f, 0.068f, 0f, 0.25f, 16, 0.72f, 0.16f, 0.14f, EMISSIVE_ALPHA)
        }.build()
    }

    private val archetypeMeshes = mutableMapOf<Pair<UnitArchetype, String>, MeshGeometry>()

    fun getUnitMesh(archetype: UnitArchetype, variant: String = ""): MeshGeometry =
        archetypeMeshes.getOrPut(archetype to variant) { UnitMeshSculptor3D.build(archetype, variant) }

    fun buildStaticMapBatches(map: SectorMap): Pair<MeshGeometry, MeshGeometry> {
        val floor = MeshAccumulator()
        val w = MapGenerator.GRID_W
        val h = MapGenerator.GRID_H

        floor.addBox(
            cx = w / 2f - 0.5f,
            cy = -0.24f,
            cz = h / 2f - 0.5f,
            sx = w + 0.65f,
            sy = 0.48f,
            sz = h + 0.65f,
            r = 0.28f,
            g = 0.42f,
            b = 0.24f
        )
        floor.addBox(w / 2f - 0.5f, 0.004f, -0.76f, w + 0.55f, 0.025f, 0.06f, 0.46f, 0.40f, 0.28f)
        floor.addBox(w / 2f - 0.5f, 0.004f, h - 0.24f, w + 0.55f, 0.025f, 0.06f, 0.46f, 0.40f, 0.28f)
        floor.addBox(-0.76f, 0.004f, h / 2f - 0.5f, 0.06f, 0.025f, h + 0.55f, 0.46f, 0.40f, 0.28f)
        floor.addBox(w - 0.24f, 0.004f, h / 2f - 0.5f, 0.06f, 0.025f, h + 0.55f, 0.46f, 0.40f, 0.28f)

        val obstacles = MeshAccumulator()
        val rows = map.rows
        for (gy in 0 until h) {
            for (gx in 0 until w) {
                val terrain = rows[gy][gx]
                val checker = (gx + gy) % 2 == 0
                val grassHash = ((gx * 37 + gy * 73) % 11) * 0.004f
                val r: Float
                val g: Float
                val b: Float
                if (terrain == TerrainType.OBJECTIVE) {
                    r = 0.22f; g = 0.50f; b = 0.24f
                } else {
                    r = if (checker) 0.28f + grassHash else 0.23f + grassHash
                    g = if (checker) 0.65f + grassHash else 0.57f + grassHash
                    b = if (checker) 0.24f else 0.20f
                }
                floor.addMeshTranslated(tileQuad, gx.toFloat(), 0f, gy.toFloat(), tintR = r, tintG = g, tintB = b)

                when (terrain) {
                    TerrainType.RUIN_WALL -> {
                        obstacles.addMeshTranslated(shadowDisc, gx - 0.10f, 0f, gy - 0.08f, sx = 1.48f, sz = 1.48f)
                        obstacles.addMeshTranslated(rockBoulder, gx.toFloat(), 0f, gy.toFloat())
                    }
                    TerrainType.PILLAR -> {
                        obstacles.addMeshTranslated(shadowDisc, gx - 0.12f, 0f, gy - 0.09f, sx = 1.32f, sz = 1.32f)
                        obstacles.addMeshTranslated(treePine, gx.toFloat(), 0f, gy.toFloat())
                    }
                    TerrainType.BARRICADE -> {
                        obstacles.addMeshTranslated(shadowDisc, gx - 0.08f, 0f, gy - 0.06f, sx = 1.16f, sz = 1.05f)
                        obstacles.addMeshTranslated(barricade, gx.toFloat(), 0f, gy.toFloat())
                    }
                    TerrainType.CRATER -> {
                        obstacles.addMeshTranslated(crater, gx.toFloat(), 0f, gy.toFloat())
                    }
                    TerrainType.OBJECTIVE -> {
                        obstacles.addMeshTranslated(objectiveTile, gx.toFloat(), 0f, gy.toFloat())
                    }
                    TerrainType.OPEN -> Unit
                }
            }
        }
        return floor.build() to obstacles.build()
    }
}
