package com.example.engine3d

import com.example.engine3d.CityMeshGenerators.TILE_SIZE
import com.example.model.BuildingType
import com.example.model.GameState
import com.example.model.GridCell
import com.example.model.TerrainType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class AnimatedEntitiesRenderer {

    private data class PlaneLivery(
        val bodyR: Float, val bodyG: Float, val bodyB: Float,
        val wingR: Float, val wingG: Float, val wingB: Float,
        val tailR: Float, val tailG: Float, val tailB: Float
    )

    private val randomizedLiveries: List<PlaneLivery> = listOf(
        PlaneLivery(0.95f, 0.96f, 0.99f, 0.16f, 0.66f, 0.96f, 0.94f, 0.24f, 0.26f), // Pearl + Sky Blue + Red
        PlaneLivery(0.92f, 0.24f, 0.28f, 0.96f, 0.96f, 0.98f, 1.00f, 0.80f, 0.18f), // Crimson + White + Gold
        PlaneLivery(0.18f, 0.56f, 0.92f, 0.96f, 0.82f, 0.22f, 0.96f, 0.97f, 0.99f), // Cobalt + Amber + White
        PlaneLivery(0.16f, 0.74f, 0.52f, 0.92f, 0.96f, 0.94f, 0.10f, 0.42f, 0.34f), // Emerald + Silver + Forest
        PlaneLivery(0.96f, 0.52f, 0.16f, 0.22f, 0.26f, 0.34f, 0.98f, 0.86f, 0.24f), // Sunset Orange + Slate + Gold
        PlaneLivery(0.58f, 0.32f, 0.90f, 0.94f, 0.92f, 0.98f, 0.98f, 0.42f, 0.72f), // Royal Purple + Pearl + Rose
        PlaneLivery(0.98f, 0.82f, 0.20f, 0.14f, 0.28f, 0.58f, 0.90f, 0.24f, 0.24f), // Golden Yellow + Navy + Red
        PlaneLivery(0.12f, 0.72f, 0.78f, 0.96f, 0.96f, 0.98f, 0.98f, 0.46f, 0.24f), // Teal + White + Coral
        PlaneLivery(0.94f, 0.36f, 0.62f, 0.22f, 0.76f, 0.94f, 0.98f, 0.94f, 0.96f), // Magenta Rose + Cyan + White
        PlaneLivery(0.30f, 0.80f, 0.38f, 0.98f, 0.94f, 0.32f, 0.18f, 0.46f, 0.86f)  // Lime + Sun Yellow + Blue
    ).shuffled(Random(System.nanoTime()))

    fun buildDynamicFrame(
        state: GameState,
        elapsedSec: Float,
        builder: MeshBuilder
    ) {
        builder.clear()
        val n = state.gridSize
        val halfSpan = (n * TILE_SIZE) * 0.5f

        // 1. Selection highlight ring if a tile is selected
        state.selectedCellCoords?.let { (sx, sz) ->
            val wx = (sx + 0.5f) * TILE_SIZE - halfSpan
            val wz = (sz + 0.5f) * TILE_SIZE - halfSpan
            val pulse = 0.88f + 0.08f * sin(elapsedSec * 5f)
            val s = TILE_SIZE * 0.5f * pulse
            val y = 0.08f
            builder.addBox(wx, y, wz - s, TILE_SIZE * pulse, 0.06f, 0.10f, 0.22f, 0.80f, 1.0f, emissive = 2.0f)
            builder.addBox(wx, y, wz + s, TILE_SIZE * pulse, 0.06f, 0.10f, 0.22f, 0.80f, 1.0f, emissive = 2.0f)
            builder.addBox(wx - s, y, wz, 0.10f, 0.06f, TILE_SIZE * pulse, 0.22f, 0.80f, 1.0f, emissive = 2.0f)
            builder.addBox(wx + s, y, wz, 0.10f, 0.06f, TILE_SIZE * pulse, 0.22f, 0.80f, 1.0f, emissive = 2.0f)
        }

        // 2. Sky Airplanes (multi-colored & randomized; no blimps)
        emitSkyTraffic(state, elapsedSec, halfSpan, builder)

        // 3. Sun & Moon orbs in sky
        emitCelestialOrbs(state.timeOfDay, halfSpan, builder)

        // 4. Outer Highway 3-lane Traffic (Trucks strictly in right-most lane!)
        emitOuterHighwayTraffic(state, elapsedSec, halfSpan, builder)

        // 5. City Road Vehicles, Traffic Lights, Ships, Trains, Trams, and Grazing Cows & Sheep
        emitCityLife(state, elapsedSec, halfSpan, builder)
    }

    private fun emitCelestialOrbs(timeOfDay: Float, halfSpan: Float, builder: MeshBuilder) {
        val angle = (timeOfDay - 0.25f) * (2f * PI.toFloat())
        val dist = halfSpan + 36f
        val sunX = cos(angle) * dist
        val sunY = sin(angle) * dist
        if (sunY > -4f) {
            builder.addCylinder(sunX, sunY, -halfSpan - 12f, 2.1f, 0.6f, 10, 1.0f, 0.92f, 0.55f, emissive = 2.5f)
        }
        val moonY = -sunY
        if (moonY > -4f) {
            builder.addCylinder(-sunX, moonY, -halfSpan - 12f, 1.5f, 0.5f, 10, 0.82f, 0.88f, 1.0f, emissive = 2.0f)
        }
    }

    private fun emitSkyTraffic(state: GameState, t: Float, halfSpan: Float, builder: MeshBuilder) {
        val airportCount = state.cells.count { it.building?.type == BuildingType.AIRPORT && !it.building.isConstructing }
        val basePlanes = 3 + airportCount.coerceAtMost(3)
        val planeCount = (basePlanes * state.settings.trafficDensity.scale).toInt().coerceAtLeast(2)
        val radius = halfSpan + 10f

        for (i in 0 until planeCount) {
            val livery = randomizedLiveries[i % randomizedLiveries.size]
            val phase = t * (0.14f + i * 0.025f) + i * 2.1f
            val px = cos(phase) * (radius - i * 3.1f)
            val pz = sin(phase) * (radius - i * 3.1f)
            val py = 12.2f + i * 1.3f + sin(t * 0.9f + i) * 0.4f
            // Tangent direction along circular flight path: dx = -sin(phase), dz = cos(phase)
            val yaw = phase + PI.toFloat() * 0.5f
            val c = cos(yaw); val s = sin(yaw)

            // Tapered cylindrical commercial jet fuselage with nose & tail cones
            builder.addHorizontalFuselage(px, py, pz, 0.24f, 0.24f, 1.75f, yaw, 8, livery.bodyR, livery.bodyG, livery.bodyB)
            // Cockpit dark glass
            builder.addRotatedBox(px + c * 0.72f, py + 0.12f, pz + s * 0.72f, 0.34f, 0.12f, 0.24f, yaw, 0.15f, 0.25f, 0.38f)
            // Swept main wings (left & right)
            builder.addRotatedBox(px + c * 0.05f - s * 0.65f, py - 0.02f, pz + s * 0.05f + c * 0.65f, 0.54f, 0.06f, 1.15f, yaw - 0.22f, livery.wingR, livery.wingG, livery.wingB)
            builder.addRotatedBox(px + c * 0.05f + s * 0.65f, py - 0.02f, pz + s * 0.05f - c * 0.65f, 0.54f, 0.06f, 1.15f, yaw + 0.22f, livery.wingR, livery.wingG, livery.wingB)
            // Twin under-wing jet engine nacelles
            builder.addHorizontalFuselage(px - s * 0.55f, py - 0.16f, pz + c * 0.55f, 0.10f, 0.10f, 0.42f, yaw, 6, livery.tailR, livery.tailG, livery.tailB)
            builder.addHorizontalFuselage(px + s * 0.55f, py - 0.16f, pz - c * 0.55f, 0.10f, 0.10f, 0.42f, yaw, 6, livery.tailR, livery.tailG, livery.tailB)
            // Horizontal tailplane + vertical rudder fin
            builder.addRotatedBox(px - c * 0.85f, py + 0.06f, pz - s * 0.85f, 0.28f, 0.05f, 0.85f, yaw, livery.wingR, livery.wingG, livery.wingB)
            builder.addRotatedTrapezoid(px - c * 0.85f, py + 0.10f, pz - s * 0.85f, 0.42f, 0.22f, 0.48f, 0.07f, 0.05f, yaw, livery.tailR, livery.tailG, livery.tailB)
            // Red wingtip / tail beacon
            builder.addBox(px - c * 0.85f, py + 0.60f, pz - s * 0.85f, 0.10f, 0.10f, 0.10f, 1.0f, 0.25f, 0.25f, emissive = 2.0f)
        }
    }

    private fun emitOuterHighwayTraffic(state: GameState, t: Float, halfSpan: Float, builder: MeshBuilder) {
        val hwDist = halfSpan + 3.2f
        val loopSpan = hwDist * 2f
        val density = state.settings.trafficDensity.scale
        val truckCount = (8 * density).toInt().coerceIn(3, 12)
        val carCount = (12 * density).toInt().coerceIn(4, 18)
        val revCount = (10 * density).toInt().coerceIn(4, 14)

        // Freight Trucks -> STRICTLY locked to FIRST LANE ON THE RIGHT (1.22f on outer 3-lane highway!)
        for (i in 0 until truckCount) {
            val pos = ((t * 3.8f + i * (loopSpan * 0.5f)) % (loopSpan * 4f))
            val seg = (pos / loopSpan).toInt()
            val d = pos % loopSpan
            val (vx, vz, yaw) = when (seg) {
                0 -> Triple(-hwDist + d, hwDist + 1.22f, 0f)
                1 -> Triple(hwDist + 1.22f, hwDist - d, -PI.toFloat() * 0.5f)
                2 -> Triple(hwDist - d, -hwDist - 1.22f, PI.toFloat())
                else -> Triple(-hwDist - 1.22f, -hwDist + d, PI.toFloat() * 0.5f)
            }
            val colorVariant = i % 3
            val tr = if (colorVariant == 0) 0.94f else if (colorVariant == 1) 0.22f else 0.86f
            val tg = if (colorVariant == 0) 0.95f else if (colorVariant == 1) 0.56f else 0.32f
            val tb = if (colorVariant == 0) 0.96f else if (colorVariant == 1) 0.86f else 0.22f
            emitFreightTruck(vx, vz, yaw, tr, tg, tb, builder)
        }

        // Cars & Taxis in fast & middle lanes (0.34f & 0.78f) in going direction
        for (i in 0 until carCount) {
            val speed = 6.2f + (i % 3) * 1.1f
            val pos = ((t * speed + i * 10.5f) % (loopSpan * 4f))
            val seg = (pos / loopSpan).toInt()
            val d = pos % loopSpan
            val lane = if (i % 2 == 0) 0.34f else 0.78f
            val (vx, vz, yaw) = when (seg) {
                0 -> Triple(-hwDist + d, hwDist + lane, 0f)
                1 -> Triple(hwDist + lane, hwDist - d, -PI.toFloat() * 0.5f)
                2 -> Triple(hwDist - d, -hwDist - lane, PI.toFloat())
                else -> Triple(-hwDist - lane, -hwDist + d, PI.toFloat() * 0.5f)
            }
            val (cr, cg, cb) = when (i % 4) {
                0 -> Triple(0.98f, 0.82f, 0.14f)
                1 -> Triple(0.20f, 0.72f, 0.94f)
                2 -> Triple(0.92f, 0.28f, 0.30f)
                else -> Triple(0.94f, 0.95f, 0.97f)
            }
            emitSedan(vx, vz, yaw, cr, cg, cb, isTaxi = (i % 4 == 0), builder = builder)
        }

        // Returning 3 lanes (clockwise): Trucks in right-most lane (1.22f), cars in 0.34f / 0.78f
        for (i in 0 until revCount) {
            val isTruck = i < 4
            val speed = if (isTruck) 3.6f else 6.4f + (i % 2) * 1.2f
            val pos = ((t * speed + i * 12.0f) % (loopSpan * 4f))
            val seg = (pos / loopSpan).toInt()
            val d = pos % loopSpan
            val lane = if (isTruck) 1.22f else if (i % 2 == 0) 0.34f else 0.78f
            val (vx, vz, yaw) = when (seg) {
                0 -> Triple(hwDist - d, hwDist - lane, PI.toFloat())
                1 -> Triple(-hwDist + lane, hwDist - d, -PI.toFloat() * 0.5f)
                2 -> Triple(-hwDist + d, -hwDist + lane, 0f)
                else -> Triple(hwDist - lane, -hwDist + d, PI.toFloat() * 0.5f)
            }
            if (isTruck) {
                emitFreightTruck(vx, vz, yaw, 0.90f, 0.92f, 0.95f, builder)
            } else {
                emitSedan(vx, vz, yaw, 0.24f, 0.76f, 0.92f, isTaxi = (i % 3 == 0), builder = builder)
            }
        }
    }

    private fun emitCityLife(state: GameState, t: Float, halfSpan: Float, builder: MeshBuilder) {
        val roadCells = mutableListOf<GridCell>()
        val waterCells = mutableListOf<GridCell>()
        val pastureCells = mutableListOf<GridCell>()
        var hasBus = false
        var hasTaxi = false
        var hasPolice = false
        var hasHospital = false
        var hasFire = false
        var hasTrain = false
        var hasTram = false
        var hasSeaport = false

        val cellMap = HashMap<Long, GridCell>(state.cells.size * 2)
        for (c in state.cells) {
            cellMap[(c.x.toLong() shl 16) or (c.z.toLong() and 0xFFFF)] = c
            if (c.terrain == TerrainType.WATER) waterCells.add(c)
            val b = c.building ?: continue
            if (b.type.isRoad) {
                roadCells.add(c)
            } else if (!b.isConstructing) {
                when (b.type) {
                    BuildingType.LIVESTOCK_PASTURE -> pastureCells.add(c)
                    BuildingType.BUS_STATION -> hasBus = true
                    BuildingType.TAXI_STATION -> hasTaxi = true
                    BuildingType.POLICE_STATION -> hasPolice = true
                    BuildingType.HOSPITAL -> hasHospital = true
                    BuildingType.FIRE_STATION -> hasFire = true
                    BuildingType.TRAIN_STATION -> hasTrain = true
                    BuildingType.TRAM_STATION -> hasTram = true
                    BuildingType.SEAPORT -> hasSeaport = true
                    else -> {}
                }
            }
        }

        fun hasRoad(gx: Int, gz: Int) = cellMap[(gx.toLong() shl 16) or (gz.toLong() and 0xFFFF)]?.building?.type?.isRoad == true

        val cycle = (t % 12f)
        val nsGreen = cycle < 5f
        val nsYellow = cycle in 5f..6f
        val ewGreen = cycle in 6f..11f

        for (idx in roadCells.indices) {
            val rc = roadCells[idx]
            val wx = (rc.x + 0.5f) * TILE_SIZE - halfSpan
            val wz = (rc.z + 0.5f) * TILE_SIZE - halfSpan
            val horiz = hasRoad(rc.x - 1, rc.z) || hasRoad(rc.x + 1, rc.z)
            val vert = hasRoad(rc.x, rc.z - 1) || hasRoad(rc.x, rc.z + 1)

            if (rc.building?.type != BuildingType.ROAD_HIGHWAY && horiz && vert) {
                val (nr, ng, nb) = when {
                    nsGreen -> Triple(0.12f, 0.95f, 0.30f)
                    nsYellow -> Triple(0.98f, 0.80f, 0.12f)
                    else -> Triple(0.98f, 0.18f, 0.18f)
                }
                val (er, eg, eb) = when {
                    ewGreen -> Triple(0.12f, 0.95f, 0.30f)
                    !nsGreen && !nsYellow && cycle >= 11f -> Triple(0.98f, 0.80f, 0.12f)
                    else -> Triple(0.98f, 0.18f, 0.18f)
                }
                builder.addBox(wx - 0.78f, 0.84f, wz - 0.78f, 0.14f, 0.14f, 0.14f, nr, ng, nb, emissive = 2.0f)
                builder.addBox(wx + 0.78f, 0.84f, wz + 0.78f, 0.14f, 0.14f, 0.14f, er, eg, eb, emissive = 2.0f)
            }

            if (idx % 3 == 0) {
                val roadType = rc.building?.type
                val isHw = roadType == BuildingType.ROAD_HIGHWAY
                val isDouble = roadType == BuildingType.ROAD_FOUR_LANE
                val reverseDir = (idx % 2 == 1)
                val rawOffset = (((t * 1.4f + idx * 0.37f) % 1f) - 0.5f) * TILE_SIZE
                val localOffset = if (reverseDir) -rawOffset else rawOffset
                val kind = idx % 6
                val mag = when {
                    isHw && kind == 5 -> 0.58f
                    isHw -> if (idx % 2 == 0) 0.20f else 0.38f
                    isDouble -> if (kind % 2 == 0) 0.28f else 0.58f
                    else -> 0.24f
                }
                val laneShift = if (reverseDir) -mag else mag
                val vx = if (horiz || !vert) wx + localOffset else wx + laneShift
                val vz = if (horiz || !vert) wz + laneShift else wz + localOffset
                val baseYaw = if (horiz || !vert) 0f else PI.toFloat() * 0.5f
                val yaw = if (reverseDir) baseYaw + PI.toFloat() else baseYaw

                when (kind) {
                    0 -> if (hasBus) emitBus(vx, vz, yaw, builder) else emitSedan(vx, vz, yaw, 0.25f, 0.70f, 0.95f, false, builder)
                    1 -> if (hasTaxi) emitSedan(vx, vz, yaw, 0.98f, 0.82f, 0.12f, true, builder) else emitSedan(vx, vz, yaw, 0.90f, 0.92f, 0.96f, false, builder)
                    2 -> if (hasPolice) emitEmergency(vx, vz, yaw, t, isPolice = true, isFire = false, builder) else emitSedan(vx, vz, yaw, 0.35f, 0.78f, 0.45f, false, builder)
                    3 -> if (hasHospital) emitEmergency(vx, vz, yaw, t, isPolice = false, isFire = false, builder) else emitSedan(vx, vz, yaw, 0.92f, 0.36f, 0.32f, false, builder)
                    4 -> if (hasFire) emitEmergency(vx, vz, yaw, t, isPolice = false, isFire = true, builder) else emitSedan(vx, vz, yaw, 0.65f, 0.45f, 0.88f, false, builder)
                    else -> emitFreightTruck(vx, vz, yaw, 0.92f, 0.94f, 0.96f, builder)
                }
            }
        }

        // Animated Tram (sleek double-ended carriage with overhead pantograph)
        if (hasTram) {
            val tramSpan = halfSpan * 1.4f
            val tx = ((t * 3.5f) % tramSpan) - tramSpan * 0.5f
            builder.addRotatedTrapezoid(tx, 0.08f, 0f, 1.48f, 1.32f, 0.32f, 0.44f, 0.40f, 0f, 0.16f, 0.76f, 0.58f)
            builder.addBox(tx, 0.28f, 0f, 1.15f, 0.11f, 0.43f, 0.96f, 0.94f, 0.72f, emissive = 1.0f)
            builder.addBox(tx, 0.46f, 0f, 0.28f, 0.14f, 0.06f, 0.75f, 0.78f, 0.82f)
            builder.addBox(tx - 0.45f, 0.06f, 0f, 0.22f, 0.08f, 0.46f, 0.12f, 0.12f, 0.14f)
            builder.addBox(tx + 0.45f, 0.06f, 0f, 0.22f, 0.08f, 0.46f, 0.12f, 0.12f, 0.14f)
        }

        // Animated Train with sloped locomotive nose, chimney & coaches
        if (hasTrain) {
            val railDist = halfSpan + 1.05f
            val railLen = railDist * 2f
            val pos = (t * 5.2f) % (railLen * 4f)
            val seg = (pos / railLen).toInt()
            val d = pos % railLen
            val (rx, rz, yaw) = when (seg) {
                0 -> Triple(-railDist + d, railDist, 0f)
                1 -> Triple(railDist, railDist - d, -PI.toFloat() * 0.5f)
                2 -> Triple(railDist - d, -railDist, PI.toFloat())
                else -> Triple(-railDist, -railDist + d, PI.toFloat() * 0.5f)
            }
            for (c in 0..2) {
                val off = c * 1.38f
                val cx = rx - cos(yaw) * off
                val cz = rz - sin(yaw) * off
                val isLoco = c == 0
                if (isLoco) {
                    builder.addRotatedTrapezoid(cx, 0.08f, cz, 1.18f, 0.92f, 0.36f, 0.46f, 0.40f, yaw, 0.86f, 0.22f, 0.22f)
                    builder.addRotatedBox(cx + cos(yaw) * 0.20f, 0.44f, cz + sin(yaw) * 0.20f, 0.22f, 0.14f, 0.34f, yaw, 0.96f, 0.92f, 0.65f, emissive = 1.2f)
                    builder.addRotatedBox(cx + cos(yaw) * 0.58f, 0.18f, cz + sin(yaw) * 0.58f, 0.06f, 0.10f, 0.28f, yaw, 1.0f, 0.96f, 0.72f, emissive = 1.8f)
                } else {
                    builder.addRotatedBox(cx, 0.25f, cz, 1.16f, 0.34f, 0.44f, yaw, 0.20f, 0.50f, 0.84f)
                    builder.addRotatedBox(cx, 0.28f, cz, 0.98f, 0.11f, 0.46f, yaw, 0.96f, 0.94f, 0.75f, emissive = 1.0f)
                }
            }
        }

        // Water ripples + Detailed Ships with V-hull, tiered white bridge & cargo stacks
        for (i in waterCells.indices) {
            val wc = waterCells[i]
            val wx = (wc.x + 0.5f) * TILE_SIZE - halfSpan
            val wz = (wc.z + 0.5f) * TILE_SIZE - halfSpan
            val ripple = sin(t * 2.5f + wc.x + wc.z) * 0.02f
            builder.addTopPlane(wx + ripple * 4f, -0.07f + ripple, wz, 0.72f, 0.14f, 0.52f, 0.86f, 1.0f)
        }
        if (waterCells.isNotEmpty()) {
            val shipCount = if (hasSeaport) minOf(3, waterCells.size) else 1
            for (s in 0 until shipCount) {
                val wc = waterCells[(s * 3) % waterCells.size]
                val wx = (wc.x + 0.5f) * TILE_SIZE - halfSpan + sin(t * 0.9f + s) * 0.40f
                val wz = (wc.z + 0.5f) * TILE_SIZE - halfSpan + cos(t * 0.7f + s) * 0.26f
                val bob = sin(t * 3.0f + s) * 0.03f
                // Tapered ship hull + flared bow
                builder.addRotatedTrapezoid(wx, -0.05f + bob, wz, 0.95f, 1.24f, 0.24f, 0.36f, 0.48f, 0f, 0.20f, 0.28f, 0.40f)
                // Red hull waterline stripe
                builder.addBox(wx, -0.02f + bob, wz, 1.02f, 0.08f, 0.42f, 0.82f, 0.22f, 0.20f)
                // Multi-tier white bridge superstructure + funnel stack
                builder.addBox(wx - 0.32f, 0.28f + bob, wz, 0.32f, 0.22f, 0.36f, 0.95f, 0.96f, 0.98f)
                builder.addBox(wx - 0.32f, 0.44f + bob, wz, 0.22f, 0.12f, 0.28f, 0.95f, 0.96f, 0.98f)
                builder.addBox(wx - 0.32f, 0.42f + bob, wz, 0.24f, 0.06f, 0.30f, 0.18f, 0.30f, 0.44f, emissive = 0.8f)
                builder.addCylinder(wx - 0.42f, 0.38f + bob, wz, 0.06f, 0.22f, 6, 0.90f, 0.30f, 0.20f)
                // Colorful cargo containers on deck
                builder.addBox(wx + 0.12f, 0.26f + bob, wz - 0.08f, 0.38f, 0.16f, 0.14f, 0.92f, 0.34f, 0.22f)
                builder.addBox(wx + 0.12f, 0.26f + bob, wz + 0.08f, 0.38f, 0.16f, 0.14f, 0.20f, 0.65f, 0.88f)
                builder.addBox(wx + 0.12f, 0.42f + bob, wz, 0.34f, 0.14f, 0.14f, 0.96f, 0.78f, 0.22f)
            }
        }

        // Animated Grazing Cows & Sheep with distinct legs, bodies, heads & ears!
        for (p in pastureCells) {
            val wx = (p.x + 0.5f) * TILE_SIZE - halfSpan
            val wz = (p.z + 0.5f) * TILE_SIZE - halfSpan
            for (s in 0..1) {
                val sx = wx + 0.25f + sin(t * 0.9f + s * 2f + p.x) * 0.32f
                val sz = wz - 0.20f + cos(t * 0.8f + s * 2f + p.z) * 0.32f
                // Wool body + legs + dark head
                builder.addBox(sx, 0.17f, sz, 0.24f, 0.15f, 0.16f, 0.97f, 0.97f, 0.95f)
                builder.addBox(sx + 0.14f, 0.20f, sz, 0.09f, 0.09f, 0.09f, 0.22f, 0.20f, 0.20f)
                builder.addBox(sx - 0.07f, 0.08f, sz - 0.05f, 0.04f, 0.08f, 0.04f, 0.22f, 0.20f, 0.20f)
                builder.addBox(sx + 0.07f, 0.08f, sz + 0.05f, 0.04f, 0.08f, 0.04f, 0.22f, 0.20f, 0.20f)
            }
            for (c in 0..1) {
                val cx = wx - 0.15f + cos(t * 0.7f + c * 2.5f + p.z) * 0.35f
                val cz = wz + 0.35f + sin(t * 0.6f + c * 2.5f + p.x) * 0.28f
                // Cow body + black spot + head + horns
                builder.addBox(cx, 0.20f, cz, 0.34f, 0.18f, 0.17f, 0.95f, 0.93f, 0.90f)
                builder.addBox(cx - 0.04f, 0.22f, cz, 0.15f, 0.15f, 0.18f, 0.20f, 0.16f, 0.14f)
                builder.addBox(cx + 0.20f, 0.24f, cz, 0.12f, 0.12f, 0.12f, 0.92f, 0.88f, 0.82f)
                builder.addBox(cx - 0.11f, 0.09f, cz - 0.06f, 0.05f, 0.10f, 0.05f, 0.24f, 0.20f, 0.18f)
                builder.addBox(cx + 0.11f, 0.09f, cz + 0.06f, 0.05f, 0.10f, 0.05f, 0.24f, 0.20f, 0.18f)
            }
        }
    }

    private fun emitSedan(
        x: Float, z: Float, yaw: Float,
        r: Float, g: Float, b: Float,
        isTaxi: Boolean,
        builder: MeshBuilder
    ) {
        val c = cos(yaw); val s = sin(yaw)
        // Lower sculpted chassis + 4 dark wheels
        builder.addRotatedBox(x, 0.12f, z, 0.48f, 0.11f, 0.25f, yaw, r, g, b)
        // Sloped windshield/roof cabin
        builder.addRotatedTrapezoid(x - c * 0.03f, 0.17f, z - s * 0.03f, 0.28f, 0.18f, 0.10f, 0.22f, 0.19f, yaw, 0.72f, 0.86f, 0.96f)
        // 4 wheels
        for (lx in listOf(-0.14f, 0.14f)) {
            for (lz in listOf(-0.12f, 0.12f)) {
                builder.addRotatedBox(x + lx * c - lz * s, 0.065f, z + lx * s + lz * c, 0.10f, 0.09f, 0.04f, yaw, 0.12f, 0.12f, 0.14f)
            }
        }
        // Taxi roof sign
        if (isTaxi) {
            builder.addRotatedBox(x - c * 0.02f, 0.29f, z - s * 0.02f, 0.09f, 0.05f, 0.12f, yaw, 1.0f, 0.94f, 0.42f, emissive = 1.5f)
        }
        // Headlights (+X) & Taillights (-X)
        builder.addRotatedBox(x + c * 0.24f, 0.12f, z + s * 0.24f, 0.03f, 0.05f, 0.20f, yaw, 1.0f, 0.96f, 0.72f, emissive = 1.2f)
        builder.addRotatedBox(x - c * 0.24f, 0.12f, z - s * 0.24f, 0.03f, 0.04f, 0.18f, yaw, 0.98f, 0.18f, 0.18f, emissive = 1.2f)
    }

    private fun emitBus(x: Float, z: Float, yaw: Float, builder: MeshBuilder) {
        val c = cos(yaw); val s = sin(yaw)
        builder.addRotatedTrapezoid(x, 0.07f, z, 0.80f, 0.74f, 0.26f, 0.30f, 0.28f, yaw, 0.96f, 0.66f, 0.16f)
        // Panoramic side windows
        builder.addRotatedBox(x, 0.24f, z, 0.68f, 0.09f, 0.31f, yaw, 0.82f, 0.92f, 0.98f, emissive = 1.0f)
        // Roof AC pod + wheels
        builder.addRotatedBox(x - c * 0.12f, 0.35f, z - s * 0.12f, 0.26f, 0.05f, 0.18f, yaw, 0.88f, 0.90f, 0.92f)
        for (lx in listOf(-0.24f, 0.24f)) {
            for (lz in listOf(-0.14f, 0.14f)) {
                builder.addRotatedBox(x + lx * c - lz * s, 0.065f, z + lx * s + lz * c, 0.12f, 0.10f, 0.05f, yaw, 0.12f, 0.12f, 0.14f)
            }
        }
    }

    private fun emitFreightTruck(
        x: Float, z: Float, yaw: Float,
        r: Float, g: Float, b: Float,
        builder: MeshBuilder
    ) {
        val c = cos(yaw); val s = sin(yaw)
        // Rear Cargo Trailer
        builder.addRotatedBox(x - c * 0.16f, 0.23f, z - s * 0.16f, 0.66f, 0.30f, 0.34f, yaw, r, g, b)
        // Front Tractor Cab with sloped windshield
        builder.addRotatedTrapezoid(x + c * 0.32f, 0.08f, z + s * 0.32f, 0.30f, 0.22f, 0.26f, 0.32f, 0.28f, yaw, 0.18f, 0.44f, 0.82f)
        builder.addRotatedBox(x + c * 0.38f, 0.24f, z + s * 0.38f, 0.12f, 0.09f, 0.26f, yaw, 0.78f, 0.90f, 0.98f)
        // 6 Truck Wheels
        for (lx in listOf(-0.38f, -0.14f, 0.32f)) {
            for (lz in listOf(-0.16f, 0.16f)) {
                builder.addRotatedBox(x + lx * c - lz * s, 0.07f, z + lx * s + lz * c, 0.12f, 0.10f, 0.05f, yaw, 0.12f, 0.12f, 0.14f)
            }
        }
        // Headlights
        builder.addRotatedBox(x + c * 0.48f, 0.13f, z + s * 0.48f, 0.04f, 0.06f, 0.24f, yaw, 1.0f, 0.96f, 0.72f, emissive = 1.2f)
    }

    private fun emitEmergency(
        x: Float, z: Float, yaw: Float, t: Float,
        isPolice: Boolean, isFire: Boolean, builder: MeshBuilder
    ) {
        val c = cos(yaw); val s = sin(yaw)
        val (r, g, b) = when {
            isPolice -> Triple(0.14f, 0.30f, 0.68f)
            isFire -> Triple(0.90f, 0.18f, 0.18f)
            else -> Triple(0.96f, 0.97f, 0.99f)
        }
        if (isFire) {
            builder.addRotatedBox(x, 0.18f, z, 0.76f, 0.22f, 0.30f, yaw, r, g, b)
            // White extendable ladder on roof
            builder.addRotatedBox(x - c * 0.08f, 0.32f, z - s * 0.08f, 0.56f, 0.06f, 0.14f, yaw, 0.92f, 0.94f, 0.96f)
        } else {
            builder.addRotatedBox(x, 0.14f, z, 0.54f, 0.14f, 0.27f, yaw, r, g, b)
            builder.addRotatedTrapezoid(x - c * 0.02f, 0.20f, z - s * 0.02f, 0.32f, 0.22f, 0.11f, 0.24f, 0.21f, yaw, 0.92f, 0.94f, 0.98f)
        }
        for (lx in listOf(-0.18f, 0.18f)) {
            for (lz in listOf(-0.13f, 0.13f)) {
                builder.addRotatedBox(x + lx * c - lz * s, 0.065f, z + lx * s + lz * c, 0.10f, 0.09f, 0.04f, yaw, 0.12f, 0.12f, 0.14f)
            }
        }
        val flash = ((t * 6f).toInt() and 1) == 0
        builder.addRotatedBox(
            x, 0.33f, z, 0.08f, 0.06f, 0.18f, yaw,
            if (flash || isFire) 1.0f else 0.15f,
            0.20f,
            if (!flash && !isFire) 1.0f else 0.18f,
            emissive = 2.0f
        )
    }
}
