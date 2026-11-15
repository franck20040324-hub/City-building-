package com.example.engine3d

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-performance vertex accumulator for Adreno 610 (OpenGL ES 2.0).
 * Vertex layout: [x, y, z, nx, ny, nz, r, g, b, emissive] = 10 floats (40 bytes).
 */
class MeshBuilder(initialCapacityFloats: Int = 131072) {
    private var data = FloatArray(initialCapacityFloats)
    var floatCount = 0
        private set

    fun clear() {
        floatCount = 0
    }

    private fun ensureCapacity(extraFloats: Int) {
        if (floatCount + extraFloats <= data.size) return
        var newCap = data.size * 2
        while (newCap < floatCount + extraFloats) newCap *= 2
        data = data.copyOf(newCap)
    }

    fun vertexCount(): Int = floatCount / 10

    fun toFloatBuffer(): FloatBuffer {
        val bb = ByteBuffer.allocateDirect(floatCount * 4).order(ByteOrder.nativeOrder())
        val fb = bb.asFloatBuffer()
        fb.put(data, 0, floatCount)
        fb.position(0)
        return fb
    }

    fun rotateVerticesY(startFloat: Int, pivotX: Float, pivotZ: Float, yawRad: Float) {
        if (yawRad == 0f || startFloat >= floatCount) return
        val c = cos(yawRad)
        val s = sin(yawRad)
        var i = startFloat
        while (i < floatCount) {
            val dx = data[i] - pivotX
            val dz = data[i + 2] - pivotZ
            data[i] = pivotX + dx * c - dz * s
            data[i + 2] = pivotZ + dx * s + dz * c

            val nx = data[i + 3]
            val nz = data[i + 5]
            data[i + 3] = nx * c - nz * s
            data[i + 5] = nx * s + nz * c
            i += 10
        }
    }

    private fun pushVertex(
        x: Float, y: Float, z: Float,
        nx: Float, ny: Float, nz: Float,
        r: Float, g: Float, b: Float,
        emissive: Float
    ) {
        ensureCapacity(10)
        val i = floatCount
        data[i] = x
        data[i + 1] = y
        data[i + 2] = z
        data[i + 3] = nx
        data[i + 4] = ny
        data[i + 5] = nz
        data[i + 6] = r
        data[i + 7] = g
        data[i + 8] = b
        data[i + 9] = emissive
        floatCount = i + 10
    }

    fun addTri(
        x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float,
        x3: Float, y3: Float, z3: Float,
        nx: Float, ny: Float, nz: Float,
        r: Float, g: Float, b: Float,
        emissive: Float = 0f
    ) {
        pushVertex(x1, y1, z1, nx, ny, nz, r, g, b, emissive)
        pushVertex(x2, y2, z2, nx, ny, nz, r, g, b, emissive)
        pushVertex(x3, y3, z3, nx, ny, nz, r, g, b, emissive)
    }

    fun addQuad(
        x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float,
        x3: Float, y3: Float, z3: Float,
        x4: Float, y4: Float, z4: Float,
        nx: Float, ny: Float, nz: Float,
        r: Float, g: Float, b: Float,
        emissive: Float = 0f
    ) {
        addTri(x1, y1, z1, x2, y2, z2, x3, y3, z3, nx, ny, nz, r, g, b, emissive)
        addTri(x1, y1, z1, x3, y3, z3, x4, y4, z4, nx, ny, nz, r, g, b, emissive)
    }

    fun addTopPlane(
        cx: Float, y: Float, cz: Float,
        sx: Float, sz: Float,
        r: Float, g: Float, b: Float,
        emissive: Float = 0f
    ) {
        val hx = sx * 0.5f
        val hz = sz * 0.5f
        addQuad(
            cx - hx, y, cz - hz,
            cx - hx, y, cz + hz,
            cx + hx, y, cz + hz,
            cx + hx, y, cz - hz,
            0f, 1f, 0f,
            r, g, b, emissive
        )
    }

    fun addBox(
        cx: Float, cy: Float, cz: Float,
        sx: Float, sy: Float, sz: Float,
        r: Float, g: Float, b: Float,
        emissive: Float = 0f,
        topR: Float = r, topG: Float = g, topB: Float = b
    ) {
        val hx = sx * 0.5f
        val hy = sy * 0.5f
        val hz = sz * 0.5f
        val x0 = cx - hx; val x1 = cx + hx
        val y0 = cy - hy; val y1 = cy + hy
        val z0 = cz - hz; val z1 = cz + hz

        addQuad(x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, 0f, 1f, 0f, topR, topG, topB, emissive)
        addQuad(x0, y1, z1, x0, y0, z1, x1, y0, z1, x1, y1, z1, 0f, 0f, 1f, r, g, b, emissive)
        addQuad(x1, y1, z0, x1, y0, z0, x0, y0, z0, x0, y1, z0, 0f, 0f, -1f, r * 0.92f, g * 0.92f, b * 0.92f, emissive)
        addQuad(x1, y1, z1, x1, y0, z1, x1, y0, z0, x1, y1, z0, 1f, 0f, 0f, r * 0.95f, g * 0.95f, b * 0.95f, emissive)
        addQuad(x0, y1, z0, x0, y0, z0, x0, y0, z1, x0, y1, z1, -1f, 0f, 0f, r * 0.88f, g * 0.88f, b * 0.88f, emissive)
    }

    fun addRotatedBox(
        cx: Float, cy: Float, cz: Float,
        sx: Float, sy: Float, sz: Float,
        yawRad: Float,
        r: Float, g: Float, b: Float,
        emissive: Float = 0f
    ) {
        val c = cos(yawRad)
        val s = sin(yawRad)
        val hx = sx * 0.5f
        val hy = sy * 0.5f
        val hz = sz * 0.5f

        fun tx(lx: Float, lz: Float) = cx + lx * c - lz * s
        fun tz(lx: Float, lz: Float) = cz + lx * s + lz * c

        val y0 = cy - hy
        val y1 = cy + hy

        val x00 = tx(-hx, -hz); val z00 = tz(-hx, -hz)
        val x01 = tx(-hx, hz); val z01 = tz(-hx, hz)
        val x11 = tx(hx, hz); val z11 = tz(hx, hz)
        val x10 = tx(hx, -hz); val z10 = tz(hx, -hz)

        addQuad(x00, y1, z00, x01, y1, z01, x11, y1, z11, x10, y1, z10, 0f, 1f, 0f, r * 1.05f, g * 1.05f, b * 1.05f, emissive)
        addQuad(x01, y1, z01, x01, y0, z01, x11, y0, z11, x11, y1, z11, -s, 0f, c, r, g, b, emissive)
        addQuad(x10, y1, z10, x10, y0, z10, x00, y0, z00, x00, y1, z00, s, 0f, -c, r * 0.9f, g * 0.9f, b * 0.9f, emissive)
        addQuad(x11, y1, z11, x11, y0, z11, x10, y0, z10, x10, y1, z10, c, 0f, s, r * 0.95f, g * 0.95f, b * 0.95f, emissive)
        addQuad(x00, y1, z00, x00, y0, z00, x01, y0, z01, x01, y1, z01, -c, 0f, -s, r * 0.88f, g * 0.88f, b * 0.88f, emissive)
    }

    /**
     * Sloped/tapered box (great for car cabins, boat hulls, aircraft noses, sloped roofs).
     */
    fun addRotatedTrapezoid(
        cx: Float, baseY: Float, cz: Float,
        botLenX: Float, topLenX: Float,
        height: Float,
        botWidZ: Float, topWidZ: Float,
        yawRad: Float,
        r: Float, g: Float, b: Float,
        emissive: Float = 0f
    ) {
        val c = cos(yawRad)
        val s = sin(yawRad)
        fun tx(lx: Float, lz: Float) = cx + lx * c - lz * s
        fun tz(lx: Float, lz: Float) = cz + lx * s + lz * c

        val y0 = baseY
        val y1 = baseY + height
        val bx = botLenX * 0.5f; val bz = botWidZ * 0.5f
        val ux = topLenX * 0.5f; val uz = topWidZ * 0.5f

        val b00x = tx(-bx, -bz); val b00z = tz(-bx, -bz)
        val b01x = tx(-bx, bz); val b01z = tz(-bx, bz)
        val b11x = tx(bx, bz); val b11z = tz(bx, bz)
        val b10x = tx(bx, -bz); val b10z = tz(bx, -bz)

        val u00x = tx(-ux, -uz); val u00z = tz(-ux, -uz)
        val u01x = tx(-ux, uz); val u01z = tz(-ux, uz)
        val u11x = tx(ux, uz); val u11z = tz(ux, uz)
        val u10x = tx(ux, -uz); val u10z = tz(ux, -uz)

        // Top
        addQuad(u00x, y1, u00z, u01x, y1, u01z, u11x, y1, u11z, u10x, y1, u10z, 0f, 1f, 0f, r * 1.05f, g * 1.05f, b * 1.05f, emissive)
        // Sides
        addQuad(u01x, y1, u01z, b01x, y0, b01z, b11x, y0, b11z, u11x, y1, u11z, -s, 0.35f, c, r, g, b, emissive)
        addQuad(u10x, y1, u10z, b10x, y0, b10z, b00x, y0, b00z, u00x, y1, u00z, s, 0.35f, -c, r * 0.9f, g * 0.9f, b * 0.9f, emissive)
        addQuad(u11x, y1, u11z, b11x, y0, b11z, b10x, y0, b10z, u10x, y1, u10z, c, 0.35f, s, r * 0.95f, g * 0.95f, b * 0.95f, emissive)
        addQuad(u00x, y1, u00z, b00x, y0, b00z, b01x, y0, b01z, u01x, y1, u01z, -c, 0.35f, -s, r * 0.88f, g * 0.88f, b * 0.88f, emissive)
    }

    /**
     * Pitched house gable roof along X-axis or Z-axis.
     */
    fun addGableRoof(
        cx: Float, baseY: Float, cz: Float,
        spanX: Float, height: Float, spanZ: Float,
        r: Float, g: Float, b: Float
    ) {
        val hx = spanX * 0.5f
        val hz = spanZ * 0.5f
        val ridgeY = baseY + height
        // Left & right roof slopes
        addQuad(
            cx - hx, ridgeY, cz,
            cx - hx, baseY, cz + hz,
            cx + hx, baseY, cz + hz,
            cx + hx, ridgeY, cz,
            0f, 0.65f, 0.75f, r, g, b
        )
        addQuad(
            cx + hx, ridgeY, cz,
            cx + hx, baseY, cz - hz,
            cx - hx, baseY, cz - hz,
            cx - hx, ridgeY, cz,
            0f, 0.65f, -0.75f, r * 0.88f, g * 0.88f, b * 0.88f
        )
        // Gable end triangles
        addTri(cx + hx, baseY, cz + hz, cx + hx, baseY, cz - hz, cx + hx, ridgeY, cz, 1f, 0f, 0f, r * 0.92f, g * 0.92f, b * 0.92f)
        addTri(cx - hx, baseY, cz - hz, cx - hx, baseY, cz + hz, cx - hx, ridgeY, cz, -1f, 0f, 0f, r * 0.92f, g * 0.92f, b * 0.92f)
    }

    fun addPyramid(
        cx: Float, baseY: Float, cz: Float,
        baseSizeX: Float, height: Float, baseSizeZ: Float,
        r: Float, g: Float, b: Float,
        emissive: Float = 0f
    ) {
        val hx = baseSizeX * 0.5f
        val hz = baseSizeZ * 0.5f
        val ax = cx; val ay = baseY + height; val az = cz
        addTri(cx - hx, baseY, cz + hz, cx + hx, baseY, cz + hz, ax, ay, az, 0f, 0.55f, 0.83f, r, g, b, emissive)
        addTri(cx + hx, baseY, cz + hz, cx + hx, baseY, cz - hz, ax, ay, az, 0.83f, 0.55f, 0f, r * 0.93f, g * 0.93f, b * 0.93f, emissive)
        addTri(cx + hx, baseY, cz - hz, cx - hx, baseY, cz - hz, ax, ay, az, 0f, 0.55f, -0.83f, r * 0.86f, g * 0.86f, b * 0.86f, emissive)
        addTri(cx - hx, baseY, cz - hz, cx - hx, baseY, cz + hz, ax, ay, az, -0.83f, 0.55f, 0f, r * 0.9f, g * 0.9f, b * 0.9f, emissive)
    }

    fun addCylinder(
        cx: Float, baseY: Float, cz: Float,
        radius: Float, height: Float,
        segments: Int,
        r: Float, g: Float, b: Float,
        emissive: Float = 0f
    ) {
        val topY = baseY + height
        val step = (2.0 * PI / segments).toFloat()
        for (i in 0 until segments) {
            val a0 = i * step
            val a1 = (i + 1) * step
            val c0 = cos(a0); val s0 = sin(a0)
            val c1 = cos(a1); val s1 = sin(a1)
            val x0 = cx + radius * c0; val z0 = cz + radius * s0
            val x1 = cx + radius * c1; val z1 = cz + radius * s1
            val nx = (c0 + c1) * 0.5f
            val nz = (s0 + s1) * 0.5f
            addQuad(x0, topY, z0, x0, baseY, z0, x1, baseY, z1, x1, topY, z1, nx, 0f, nz, r, g, b, emissive)
            addTri(cx, topY, cz, x1, topY, z1, x0, topY, z0, 0f, 1f, 0f, r * 1.05f, g * 1.05f, b * 1.05f, emissive)
        }
    }

    /**
     * Horizontal cylinder along vehicle's local forward (+X rotated by yawRad) — great for airplane/blimp fuselages!
     */
    fun addHorizontalFuselage(
        cx: Float, cy: Float, cz: Float,
        radiusY: Float, radiusZ: Float, length: Float,
        yawRad: Float, segments: Int,
        r: Float, g: Float, b: Float,
        emissive: Float = 0f
    ) {
        val c = cos(yawRad)
        val s = sin(yawRad)
        val hl = length * 0.5f
        val step = (2.0 * PI / segments).toFloat()
        for (i in 0 until segments) {
            val a0 = i * step
            val a1 = (i + 1) * step
            val y0 = cy + radiusY * sin(a0); val z0Local = radiusZ * cos(a0)
            val y1 = cy + radiusY * sin(a1); val z1Local = radiusZ * cos(a1)

            // Front ring (+hl) and Back ring (-hl)
            val fx0 = cx + hl * c - z0Local * s; val fz0 = cz + hl * s + z0Local * c
            val fx1 = cx + hl * c - z1Local * s; val fz1 = cz + hl * s + z1Local * c
            val bx0 = cx - hl * c - z0Local * s; val bz0 = cz - hl * s + z0Local * c
            val bx1 = cx - hl * c - z1Local * s; val bz1 = cz - hl * s + z1Local * c

            val ny = (sin(a0) + sin(a1)) * 0.5f
            val nzL = (cos(a0) + cos(a1)) * 0.5f
            addQuad(fx0, y0, fz0, bx0, y0, bz0, bx1, y1, bz1, fx1, y1, fz1, -nzL * s, ny, nzL * c, r, g, b, emissive)
            // Pointed nose cone (+hl * 1.45) and tail cone (-hl * 1.35)
            val noseX = cx + (hl * 1.45f) * c; val noseZ = cz + (hl * 1.45f) * s
            val tailX = cx - (hl * 1.35f) * c; val tailZ = cz - (hl * 1.35f) * s
            addTri(fx0, y0, fz0, fx1, y1, fz1, noseX, cy, noseZ, c, ny, s, r * 1.05f, g * 1.05f, b * 1.05f, emissive)
            addTri(bx1, y1, bz1, bx0, y0, bz0, tailX, cy, tailZ, -c, ny, -s, r * 0.9f, g * 0.9f, b * 0.9f, emissive)
        }
    }

    /**
     * Vertical ring of blocks (great for Colosseum walls or Ferris Wheel rims).
     */
    fun addHorizontalRing(
        cx: Float, baseY: Float, cz: Float,
        radius: Float, thickness: Float, height: Float,
        segments: Int,
        r: Float, g: Float, b: Float
    ) {
        val step = (2.0 * PI / segments).toFloat()
        val segLen = (2.0 * PI * radius / segments).toFloat() * 1.06f
        for (i in 0 until segments) {
            val a = i * step
            val px = cx + cos(a) * radius
            val pz = cz + sin(a) * radius
            addRotatedBox(px, baseY + height * 0.5f, pz, thickness, height, segLen, a, r, g, b)
        }
    }

    fun addCone(
        cx: Float, baseY: Float, cz: Float,
        radius: Float, height: Float,
        segments: Int,
        r: Float, g: Float, b: Float,
        emissive: Float = 0f
    ) {
        val topY = baseY + height
        val step = (2.0 * PI / segments).toFloat()
        for (i in 0 until segments) {
            val a0 = i * step
            val a1 = (i + 1) * step
            val c0 = cos(a0); val s0 = sin(a0)
            val c1 = cos(a1); val s1 = sin(a1)
            val x0 = cx + radius * c0; val z0 = cz + radius * s0
            val x1 = cx + radius * c1; val z1 = cz + radius * s1
            val nx = (c0 + c1) * 0.45f
            val nz = (s0 + s1) * 0.45f
            addTri(x0, baseY, z0, x1, baseY, z1, cx, topY, cz, nx, 0.5f, nz, r, g, b, emissive)
        }
    }
}
