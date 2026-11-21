package com.example.ui.battle

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

/**
 * Lightweight glTF 2.0 binary (.glb) loader that converts standard triangle meshes
 * into the engine's 10-float interleaved VBO format [x, y, z, nx, ny, nz, r, g, b, a].
 * Uses an embedded pure-Kotlin JSON reader so it runs identically on Android and JVM tests.
 */
object GlbLoader3D {

    private const val GLB_MAGIC = 0x46546C67 // "glTF"
    private const val CHUNK_JSON = 0x4E4F534A // "JSON"
    private const val CHUNK_BIN = 0x004E4942  // "BIN\0"

    private const val COMP_BYTE = 5120
    private const val COMP_UBYTE = 5121
    private const val COMP_SHORT = 5122
    private const val COMP_USHORT = 5123
    private const val COMP_UINT = 5125
    private const val COMP_FLOAT = 5126

    private data class PrimitiveSource(
        val positions: FloatArray,
        val normals: FloatArray?,
        val colors: FloatArray?,
        val colorComponents: Int,
        val indices: IntArray,
        val baseR: Float,
        val baseG: Float,
        val baseB: Float,
        val baseA: Float,
        val nodeMatrix: FloatArray?
    )

    fun loadMesh(
        stream: InputStream,
        targetHeight: Float = 0.923f,
        rotateYPlus90: Boolean = true
    ): MeshGeometry? {
        val bytes = stream.use { it.readBytes() }
        if (bytes.size < 20) return null

        val header = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val magic = header.int
        val version = header.int
        val totalLen = header.int
        if (magic != GLB_MAGIC || version != 2 || totalLen > bytes.size) return null

        var jsonText: String? = null
        var binSlice: ByteBuffer? = null

        var offset = 12
        while (offset + 8 <= totalLen) {
            header.position(offset)
            val chunkLen = header.int
            val chunkType = header.int
            val dataStart = offset + 8
            if (chunkLen < 0 || dataStart + chunkLen > totalLen) break
            when (chunkType) {
                CHUNK_JSON -> jsonText = String(bytes, dataStart, chunkLen, Charsets.UTF_8)
                CHUNK_BIN -> {
                    binSlice = ByteBuffer.wrap(bytes, dataStart, chunkLen)
                        .slice()
                        .order(ByteOrder.LITTLE_ENDIAN)
                }
            }
            offset = dataStart + chunkLen
        }

        val json = jsonText?.let { MiniJsonParser(it).parseObject() } ?: return null
        val bin = binSlice ?: return null

        val meshes = json.arr("meshes") ?: return null
        val accessors = json.arr("accessors") ?: return null
        val bufferViews = json.arr("bufferViews") ?: return null
        val materials = json.arr("materials")
        val nodes = json.arr("nodes")

        val nodeTransforms = collectMeshNodeTransforms(json, nodes)
        val primitives = ArrayList<PrimitiveSource>()
        var totalTriVertices = 0

        if (nodeTransforms.isNotEmpty()) {
            for ((meshIdx, mat4) in nodeTransforms) {
                val meshObj = meshes.getOrNull(meshIdx) as? Map<*, *> ?: continue
                extractMeshPrimitives(meshObj, mat4, accessors, bufferViews, materials, bin, primitives)
            }
        } else {
            for (item in meshes) {
                val meshObj = item as? Map<*, *> ?: continue
                extractMeshPrimitives(meshObj, null, accessors, bufferViews, materials, bin, primitives)
            }
        }

        if (primitives.isEmpty()) return null
        for (p in primitives) totalTriVertices += p.indices.size
        if (totalTriVertices == 0 || totalTriVertices % 3 != 0) return null

        // Pass 1: find overall Y bounds (after optional node transform)
        var minY = Float.POSITIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        for (p in primitives) {
            val pos = p.positions
            val vCount = pos.size / 3
            val nm = p.nodeMatrix
            for (i in 0 until vCount) {
                val lx = pos[i * 3]
                val ly = pos[i * 3 + 1]
                val lz = pos[i * 3 + 2]
                val wy = if (nm != null) nm[1] * lx + nm[5] * ly + nm[9] * lz + nm[13] else ly
                if (wy < minY) minY = wy
                if (wy > maxY) maxY = wy
            }
        }
        val rawHeight = (maxY - minY).coerceAtLeast(1e-4f)
        val baseSliceMaxY = minY + rawHeight * 0.045f

        var baseMinX = Float.POSITIVE_INFINITY
        var baseMaxX = Float.NEGATIVE_INFINITY
        var baseMinZ = Float.POSITIVE_INFINITY
        var baseMaxZ = Float.NEGATIVE_INFINITY
        var allMinX = Float.POSITIVE_INFINITY
        var allMaxX = Float.NEGATIVE_INFINITY
        var allMinZ = Float.POSITIVE_INFINITY
        var allMaxZ = Float.NEGATIVE_INFINITY
        var baseHits = 0

        for (p in primitives) {
            val pos = p.positions
            val vCount = pos.size / 3
            val nm = p.nodeMatrix
            for (i in 0 until vCount) {
                val lx = pos[i * 3]
                val ly = pos[i * 3 + 1]
                val lz = pos[i * 3 + 2]
                val wx = if (nm != null) nm[0] * lx + nm[4] * ly + nm[8] * lz + nm[12] else lx
                val wy = if (nm != null) nm[1] * lx + nm[5] * ly + nm[9] * lz + nm[13] else ly
                val wz = if (nm != null) nm[2] * lx + nm[6] * ly + nm[10] * lz + nm[14] else lz
                if (wx < allMinX) allMinX = wx
                if (wx > allMaxX) allMaxX = wx
                if (wz < allMinZ) allMinZ = wz
                if (wz > allMaxZ) allMaxZ = wz
                if (wy <= baseSliceMaxY) {
                    baseHits++
                    if (wx < baseMinX) baseMinX = wx
                    if (wx > baseMaxX) baseMaxX = wx
                    if (wz < baseMinZ) baseMinZ = wz
                    if (wz > baseMaxZ) baseMaxZ = wz
                }
            }
        }

        val centerX = if (baseHits > 0) (baseMinX + baseMaxX) * 0.5f else (allMinX + allMaxX) * 0.5f
        val centerZ = if (baseHits > 0) (baseMinZ + baseMaxZ) * 0.5f else (allMinZ + allMaxZ) * 0.5f
        val scale = targetHeight / rawHeight

        // Pass 2: write packed 10-float triangles directly without boxing
        val out = FloatArray(totalTriVertices * 10)
        var outIdx = 0

        for (p in primitives) {
            val pos = p.positions
            val nrm = p.normals
            val col = p.colors
            val cComp = p.colorComponents
            val idxs = p.indices
            val nm = p.nodeMatrix

            val baseR = p.baseR
            val baseG = p.baseG
            val baseB = p.baseB
            val baseA = p.baseA

            var t = 0
            while (t + 2 < idxs.size) {
                val i0 = idxs[t]
                val i1 = idxs[t + 1]
                val i2 = idxs[t + 2]

                val x0 = transformX(pos, i0, nm, centerX, scale)
                val y0 = transformY(pos, i0, nm, minY, scale)
                val z0 = transformZ(pos, i0, nm, centerZ, scale)

                val x1 = transformX(pos, i1, nm, centerX, scale)
                val y1 = transformY(pos, i1, nm, minY, scale)
                val z1 = transformZ(pos, i1, nm, centerZ, scale)

                val x2 = transformX(pos, i2, nm, centerX, scale)
                val y2 = transformY(pos, i2, nm, minY, scale)
                val z2 = transformZ(pos, i2, nm, centerZ, scale)

                // Flat fallback normal if NORMAL accessor omitted
                var fnx = 0f
                var fny = 1f
                var fnz = 0f
                if (nrm == null) {
                    val ux = x1 - x0; val uy = y1 - y0; val uz = z1 - z0
                    val vx = x2 - x0; val vy = y2 - y0; val vz = z2 - z0
                    fnx = uy * vz - uz * vy
                    fny = uz * vx - ux * vz
                    fnz = ux * vy - uy * vx
                    val len = sqrt(fnx * fnx + fny * fny + fnz * fnz).coerceAtLeast(1e-5f)
                    fnx /= len; fny /= len; fnz /= len
                }

                for (k in 0..2) {
                    val vi: Int
                    val sx: Float
                    val sy: Float
                    val sz: Float
                    when (k) {
                        0 -> { vi = i0; sx = x0; sy = y0; sz = z0 }
                        1 -> { vi = i1; sx = x1; sy = y1; sz = z1 }
                        else -> { vi = i2; sx = x2; sy = y2; sz = z2 }
                    }

                    var nx: Float
                    var ny: Float
                    var nz: Float
                    if (nrm != null) {
                        val lx = nrm[vi * 3]
                        val ly = nrm[vi * 3 + 1]
                        val lz = nrm[vi * 3 + 2]
                        if (nm != null) {
                            nx = nm[0] * lx + nm[4] * ly + nm[8] * lz
                            ny = nm[1] * lx + nm[5] * ly + nm[9] * lz
                            nz = nm[2] * lx + nm[6] * ly + nm[10] * lz
                        } else {
                            nx = lx; ny = ly; nz = lz
                        }
                        val nLen = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(1e-5f)
                        nx /= nLen; ny /= nLen; nz /= nLen
                    } else {
                        nx = fnx; ny = fny; nz = fnz
                    }

                    val px: Float
                    val py: Float
                    val pz: Float
                    val rnx: Float
                    val rny: Float
                    val rnz: Float
                    if (rotateYPlus90) {
                        px = sz
                        py = sy
                        pz = -sx
                        rnx = nz
                        rny = ny
                        rnz = -nx
                    } else {
                        px = sx
                        py = sy
                        pz = sz
                        rnx = nx
                        rny = ny
                        rnz = nz
                    }

                    val cr: Float
                    val cg: Float
                    val cb: Float
                    val ca: Float
                    if (col != null) {
                        cr = col[vi * cComp] * baseR
                        cg = col[vi * cComp + 1] * baseG
                        cb = col[vi * cComp + 2] * baseB
                        ca = if (cComp >= 4) col[vi * cComp + 3] * baseA else baseA
                    } else {
                        cr = baseR
                        cg = baseG
                        cb = baseB
                        ca = baseA
                    }

                    out[outIdx++] = px
                    out[outIdx++] = py
                    out[outIdx++] = pz
                    out[outIdx++] = rnx
                    out[outIdx++] = rny
                    out[outIdx++] = rnz
                    out[outIdx++] = cr
                    out[outIdx++] = cg
                    out[outIdx++] = cb
                    out[outIdx++] = ca
                }
                t += 3
            }
        }

        return MeshGeometry(out)
    }

    private fun transformX(pos: FloatArray, vi: Int, nm: FloatArray?, cx: Float, scale: Float): Float {
        val lx = pos[vi * 3]
        val ly = pos[vi * 3 + 1]
        val lz = pos[vi * 3 + 2]
        val wx = if (nm != null) nm[0] * lx + nm[4] * ly + nm[8] * lz + nm[12] else lx
        return (wx - cx) * scale
    }

    private fun transformY(pos: FloatArray, vi: Int, nm: FloatArray?, minY: Float, scale: Float): Float {
        val lx = pos[vi * 3]
        val ly = pos[vi * 3 + 1]
        val lz = pos[vi * 3 + 2]
        val wy = if (nm != null) nm[1] * lx + nm[5] * ly + nm[9] * lz + nm[13] else ly
        return (wy - minY) * scale
    }

    private fun transformZ(pos: FloatArray, vi: Int, nm: FloatArray?, cz: Float, scale: Float): Float {
        val lx = pos[vi * 3]
        val ly = pos[vi * 3 + 1]
        val lz = pos[vi * 3 + 2]
        val wz = if (nm != null) nm[2] * lx + nm[6] * ly + nm[10] * lz + nm[14] else lz
        return (wz - cz) * scale
    }

    private fun extractMeshPrimitives(
        meshObj: Map<*, *>,
        nodeMatrix: FloatArray?,
        accessors: List<*>,
        bufferViews: List<*>,
        materials: List<*>?,
        bin: ByteBuffer,
        out: MutableList<PrimitiveSource>
    ) {
        val prims = meshObj.arr("primitives") ?: return
        for (item in prims) {
            val prim = item as? Map<*, *> ?: continue
            val mode = prim.int("mode", 4)
            if (mode != 4) continue
            val attrs = prim.obj("attributes") ?: continue
            val posAccIdx = attrs.optInt("POSITION") ?: continue

            val pos = readVecFloatAccessor(posAccIdx, 3, accessors, bufferViews, bin) ?: continue
            val nrm = attrs.optInt("NORMAL")?.let {
                readVecFloatAccessor(it, 3, accessors, bufferViews, bin)
            }

            var colorComp = 0
            val colors = attrs.optInt("COLOR_0")?.let { accIdx ->
                val accObj = accessors.getOrNull(accIdx) as? Map<*, *>
                val accType = accObj?.str("type")
                colorComp = if (accType == "VEC4") 4 else 3
                readColorAccessor(accIdx, colorComp, accessors, bufferViews, bin)
            }

            val vCount = pos.size / 3
            val indices = prim.optInt("indices")?.let { idxAcc ->
                readIndexAccessor(idxAcc, accessors, bufferViews, bin)
            } ?: IntArray(vCount) { it }

            var br = 0.5f; var bg = 0.5f; var bb = 0.5f; var ba = 1f
            val matIdx = prim.optInt("material")
            if (matIdx != null && materials != null) {
                val mat = materials.getOrNull(matIdx) as? Map<*, *>
                val pbr = mat?.obj("pbrMetallicRoughness")
                val bcf = pbr?.arr("baseColorFactor")
                if (bcf != null && bcf.size >= 3) {
                    br = bcf.dbl(0, 0.5).toFloat().coerceIn(0f, 1f)
                    bg = bcf.dbl(1, 0.5).toFloat().coerceIn(0f, 1f)
                    bb = bcf.dbl(2, 0.5).toFloat().coerceIn(0f, 1f)
                    ba = if (bcf.size >= 4) bcf.dbl(3, 1.0).toFloat().coerceIn(0f, 1f) else 1f
                }
            }

            out.add(
                PrimitiveSource(
                    positions = pos,
                    normals = nrm,
                    colors = colors,
                    colorComponents = colorComp,
                    indices = indices,
                    baseR = br,
                    baseG = bg,
                    baseB = bb,
                    baseA = ba,
                    nodeMatrix = nodeMatrix
                )
            )
        }
    }

    private fun readVecFloatAccessor(
        accessorIndex: Int,
        components: Int,
        accessors: List<*>,
        bufferViews: List<*>,
        bin: ByteBuffer
    ): FloatArray? {
        val acc = accessors.getOrNull(accessorIndex) as? Map<*, *> ?: return null
        if (acc.int("componentType", -1) != COMP_FLOAT) return null
        val bvIdx = acc.int("bufferView", -1)
        if (bvIdx < 0) return null
        val bv = bufferViews.getOrNull(bvIdx) as? Map<*, *> ?: return null
        val count = acc.int("count", 0)
        if (count <= 0) return null

        val baseOffset = bv.int("byteOffset", 0) + acc.int("byteOffset", 0)
        val stride = bv.int("byteStride", components * 4).coerceAtLeast(components * 4)
        val out = FloatArray(count * components)
        for (i in 0 until count) {
            val addr = baseOffset + i * stride
            if (addr + components * 4 > bin.limit()) break
            for (c in 0 until components) {
                out[i * components + c] = bin.getFloat(addr + c * 4)
            }
        }
        return out
    }

    private fun readColorAccessor(
        accessorIndex: Int,
        components: Int,
        accessors: List<*>,
        bufferViews: List<*>,
        bin: ByteBuffer
    ): FloatArray? {
        val acc = accessors.getOrNull(accessorIndex) as? Map<*, *> ?: return null
        val compType = acc.int("componentType", -1)
        val bvIdx = acc.int("bufferView", -1)
        if (bvIdx < 0) return null
        val bv = bufferViews.getOrNull(bvIdx) as? Map<*, *> ?: return null
        val count = acc.int("count", 0)
        if (count <= 0) return null
        val baseOffset = bv.int("byteOffset", 0) + acc.int("byteOffset", 0)
        val elemSize = when (compType) {
            COMP_FLOAT -> 4
            COMP_USHORT -> 2
            COMP_UBYTE -> 1
            else -> return null
        }
        val stride = bv.int("byteStride", components * elemSize).coerceAtLeast(components * elemSize)
        val out = FloatArray(count * components)
        for (i in 0 until count) {
            val addr = baseOffset + i * stride
            if (addr + components * elemSize > bin.limit()) break
            for (c in 0 until components) {
                out[i * components + c] = when (compType) {
                    COMP_FLOAT -> bin.getFloat(addr + c * 4)
                    COMP_USHORT -> (bin.getShort(addr + c * 2).toInt() and 0xFFFF) / 65535f
                    else -> (bin.get(addr + c).toInt() and 0xFF) / 255f
                }
            }
        }
        return out
    }

    private fun readIndexAccessor(
        accessorIndex: Int,
        accessors: List<*>,
        bufferViews: List<*>,
        bin: ByteBuffer
    ): IntArray? {
        val acc = accessors.getOrNull(accessorIndex) as? Map<*, *> ?: return null
        val compType = acc.int("componentType", -1)
        val bvIdx = acc.int("bufferView", -1)
        if (bvIdx < 0) return null
        val bv = bufferViews.getOrNull(bvIdx) as? Map<*, *> ?: return null
        val count = acc.int("count", 0)
        if (count <= 0) return null
        val baseOffset = bv.int("byteOffset", 0) + acc.int("byteOffset", 0)
        val elemSize = when (compType) {
            COMP_UINT -> 4
            COMP_USHORT -> 2
            COMP_UBYTE -> 1
            else -> return null
        }
        val stride = bv.int("byteStride", elemSize).coerceAtLeast(elemSize)
        val out = IntArray(count)
        for (i in 0 until count) {
            val addr = baseOffset + i * stride
            if (addr + elemSize > bin.limit()) break
            out[i] = when (compType) {
                COMP_UINT -> bin.getInt(addr)
                COMP_USHORT -> bin.getShort(addr).toInt() and 0xFFFF
                else -> bin.get(addr).toInt() and 0xFF
            }
        }
        return out
    }

    private fun collectMeshNodeTransforms(json: Map<*, *>, nodes: List<*>?): List<Pair<Int, FloatArray?>> {
        if (nodes == null || nodes.isEmpty()) return emptyList()
        val scenes = json.arr("scenes") ?: return emptyList()
        val sceneIdx = json.int("scene", 0)
        val sceneObj = scenes.getOrNull(sceneIdx) as? Map<*, *> ?: return emptyList()
        val rootNodes = sceneObj.arr("nodes") ?: return emptyList()

        val result = ArrayList<Pair<Int, FloatArray?>>()
        fun visit(nodeIdx: Int, parentMat: FloatArray?) {
            val node = nodes.getOrNull(nodeIdx) as? Map<*, *> ?: return
            val localMat = computeNodeLocalMatrix(node)
            val worldMat = when {
                parentMat == null && localMat == null -> null
                parentMat == null -> localMat
                localMat == null -> parentMat
                else -> multiplyMat4(parentMat, localMat)
            }
            val meshIdx = node.optInt("mesh")
            if (meshIdx != null) {
                result.add(meshIdx to worldMat)
            }
            val children = node.arr("children")
            if (children != null) {
                for (child in children) {
                    val cIdx = (child as? Number)?.toInt() ?: continue
                    visit(cIdx, worldMat)
                }
            }
        }
        for (root in rootNodes) {
            val rIdx = (root as? Number)?.toInt() ?: continue
            visit(rIdx, null)
        }
        return result
    }

    private fun computeNodeLocalMatrix(node: Map<*, *>): FloatArray? {
        val matArr = node.arr("matrix")
        if (matArr != null && matArr.size == 16) {
            return FloatArray(16) { matArr.dbl(it, 0.0).toFloat() }
        }
        val t = node.arr("translation")
        val r = node.arr("rotation")
        val s = node.arr("scale")
        if (t == null && r == null && s == null) return null

        val tx = t?.dbl(0, 0.0)?.toFloat() ?: 0f
        val ty = t?.dbl(1, 0.0)?.toFloat() ?: 0f
        val tz = t?.dbl(2, 0.0)?.toFloat() ?: 0f

        val sx = s?.dbl(0, 1.0)?.toFloat() ?: 1f
        val sy = s?.dbl(1, 1.0)?.toFloat() ?: 1f
        val sz = s?.dbl(2, 1.0)?.toFloat() ?: 1f

        val qx = r?.dbl(0, 0.0)?.toFloat() ?: 0f
        val qy = r?.dbl(1, 0.0)?.toFloat() ?: 0f
        val qz = r?.dbl(2, 0.0)?.toFloat() ?: 0f
        val qw = r?.dbl(3, 1.0)?.toFloat() ?: 1f

        val xx = qx * qx; val yy = qy * qy; val zz = qz * qz
        val xy = qx * qy; val xz = qx * qz; val yz = qy * qz
        val wx = qw * qx; val wy = qw * qy; val wz = qw * qz

        return floatArrayOf(
            (1f - 2f * (yy + zz)) * sx, (2f * (xy + wz)) * sx, (2f * (xz - wy)) * sx, 0f,
            (2f * (xy - wz)) * sy, (1f - 2f * (xx + zz)) * sy, (2f * (yz + wx)) * sy, 0f,
            (2f * (xz + wy)) * sz, (2f * (yz - wx)) * sz, (1f - 2f * (xx + yy)) * sz, 0f,
            tx, ty, tz, 1f
        )
    }

    private fun multiplyMat4(a: FloatArray, b: FloatArray): FloatArray {
        val out = FloatArray(16)
        for (col in 0..3) {
            for (row in 0..3) {
                out[col * 4 + row] =
                    a[row] * b[col * 4] +
                    a[4 + row] * b[col * 4 + 1] +
                    a[8 + row] * b[col * 4 + 2] +
                    a[12 + row] * b[col * 4 + 3]
            }
        }
        return out
    }

    private fun Map<*, *>.obj(key: String): Map<*, *>? = this[key] as? Map<*, *>
    private fun Map<*, *>.arr(key: String): List<*>? = this[key] as? List<*>
    private fun Map<*, *>.str(key: String): String? = this[key] as? String
    private fun Map<*, *>.optInt(key: String): Int? = (this[key] as? Number)?.toInt()
    private fun Map<*, *>.int(key: String, default: Int): Int = (this[key] as? Number)?.toInt() ?: default
    private fun List<*>.dbl(idx: Int, default: Double): Double = (this.getOrNull(idx) as? Number)?.toDouble() ?: default

    private class MiniJsonParser(private val src: String) {
        private var pos = 0

        fun parseObject(): Map<String, Any?>? {
            skipWs()
            if (pos >= src.length || src[pos] != '{') return null
            return readObject()
        }

        private fun readValue(): Any? {
            skipWs()
            if (pos >= src.length) return null
            return when (src[pos]) {
                '{' -> readObject()
                '[' -> readArray()
                '"' -> readString()
                't' -> { pos += 4; true }
                'f' -> { pos += 5; false }
                'n' -> { pos += 4; null }
                else -> readNumber()
            }
        }

        private fun readObject(): Map<String, Any?> {
            pos++ // '{'
            val map = LinkedHashMap<String, Any?>()
            skipWs()
            if (pos < src.length && src[pos] == '}') {
                pos++
                return map
            }
            while (pos < src.length) {
                skipWs()
                val key = readString()
                skipWs()
                if (pos < src.length && src[pos] == ':') pos++
                map[key] = readValue()
                skipWs()
                if (pos < src.length && src[pos] == ',') {
                    pos++
                } else {
                    if (pos < src.length && src[pos] == '}') pos++
                    break
                }
            }
            return map
        }

        private fun readArray(): List<Any?> {
            pos++ // '['
            val list = ArrayList<Any?>()
            skipWs()
            if (pos < src.length && src[pos] == ']') {
                pos++
                return list
            }
            while (pos < src.length) {
                list.add(readValue())
                skipWs()
                if (pos < src.length && src[pos] == ',') {
                    pos++
                } else {
                    if (pos < src.length && src[pos] == ']') pos++
                    break
                }
            }
            return list
        }

        private fun readString(): String {
            pos++ // '"'
            val sb = StringBuilder()
            while (pos < src.length) {
                val ch = src[pos++]
                if (ch == '"') break
                if (ch == '\\' && pos < src.length) {
                    val esc = src[pos++]
                    when (esc) {
                        '"', '\\', '/' -> sb.append(esc)
                        'b' -> sb.append('\b')
                        'f' -> sb.append('\u000C')
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'u' -> if (pos + 4 <= src.length) {
                            sb.append(src.substring(pos, pos + 4).toInt(16).toChar())
                            pos += 4
                        }
                        else -> sb.append(esc)
                    }
                } else {
                    sb.append(ch)
                }
            }
            return sb.toString()
        }

        private fun readNumber(): Number {
            val start = pos
            var isFloat = false
            while (pos < src.length) {
                val c = src[pos]
                if (c in '0'..'9' || c == '-' || c == '+') {
                    pos++
                } else if (c == '.' || c == 'e' || c == 'E') {
                    isFloat = true
                    pos++
                } else {
                    break
                }
            }
            val token = src.substring(start, pos)
            return if (isFloat) token.toDouble() else token.toLong()
        }

        private fun skipWs() {
            while (pos < src.length && src[pos] <= ' ') pos++
        }
    }
}
