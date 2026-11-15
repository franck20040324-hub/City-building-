package com.example.engine3d

import com.example.model.BuildingType
import com.example.model.GameState
import com.example.model.GridCell
import com.example.model.PlacedBuilding
import com.example.model.Season
import com.example.model.TerrainType

object CityMeshGenerators {

    const val TILE_SIZE = 2.0f

    fun seasonalFoliageColor(season: Season): Triple<Float, Float, Float> = when (season) {
        Season.SPRING -> Triple(0.45f, 0.78f, 0.36f)
        Season.SUMMER -> Triple(0.16f, 0.56f, 0.24f)
        Season.AUTUMN -> Triple(0.86f, 0.44f, 0.14f)
        Season.WINTER -> Triple(0.84f, 0.91f, 0.96f)
    }

    fun seasonalAccentBlossom(season: Season): Triple<Float, Float, Float> = when (season) {
        Season.SPRING -> Triple(0.96f, 0.65f, 0.78f) // cherry blossom
        Season.SUMMER -> Triple(0.22f, 0.64f, 0.28f)
        Season.AUTUMN -> Triple(0.92f, 0.65f, 0.15f) // golden leaves
        Season.WINTER -> Triple(0.93f, 0.96f, 1.0f)  // snow cap
    }

    fun buildStaticWorld(state: GameState, builder: MeshBuilder) {
        builder.clear()
        val n = state.gridSize
        val halfSpan = (n * TILE_SIZE) * 0.5f

        // 1. Surrounding countryside apron + External Highway Ring outside city with Entry/Exit turns
        buildCountrysideAndOuterHighway(n, state.season, builder)

        // Build lookup map for road neighbors (intersections & orientation)
        val cellMap = HashMap<Long, GridCell>(state.cells.size * 2)
        for (c in state.cells) {
            cellMap[(c.x.toLong() shl 16) or (c.z.toLong() and 0xFFFF)] = c
        }
        fun cellAt(gx: Int, gz: Int): GridCell? = cellMap[(gx.toLong() shl 16) or (gz.toLong() and 0xFFFF)]

        // 2. Every tile inside open 22x22 city grid
        for (cell in state.cells) {
            val wx = (cell.x + 0.5f) * TILE_SIZE - halfSpan
            val wz = (cell.z + 0.5f) * TILE_SIZE - halfSpan

            // Base Terrain
            when (cell.terrain) {
                TerrainType.WATER -> {
                    // Lowered ground basin + water surface + bank edges
                    builder.addBox(wx, -0.35f, wz, TILE_SIZE, 0.12f, TILE_SIZE, 0.12f, 0.34f, 0.46f)
                    builder.addTopPlane(wx, -0.09f, wz, TILE_SIZE, TILE_SIZE, 0.08f, 0.52f, 0.78f)
                    // Raised bank walls where adjacent to land
                    val dirs = arrayOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)
                    for ((dx, dz) in dirs) {
                        val nb = cellAt(cell.x + dx, cell.z + dz)
                        if (nb != null && nb.terrain != TerrainType.WATER) {
                            val bx = wx + dx * (TILE_SIZE * 0.46f)
                            val bz = wz + dz * (TILE_SIZE * 0.46f)
                            val sx = if (dx != 0) 0.16f else TILE_SIZE
                            val sz = if (dz != 0) 0.16f else TILE_SIZE
                            builder.addBox(bx, -0.05f, bz, sx, 0.12f, sz, 0.45f, 0.38f, 0.28f)
                        }
                    }
                }
                TerrainType.FERTILE_PLAIN -> {
                    val c = if (state.season == Season.WINTER) Triple(0.75f, 0.82f, 0.80f) else Triple(0.36f, 0.62f, 0.25f)
                    builder.addTopPlane(wx, 0f, wz, TILE_SIZE, TILE_SIZE, c.first, c.second, c.third)
                }
                TerrainType.FOREST_LAND -> {
                    val c = if (state.season == Season.WINTER) Triple(0.76f, 0.83f, 0.86f) else Triple(0.24f, 0.48f, 0.22f)
                    builder.addTopPlane(wx, 0f, wz, TILE_SIZE, TILE_SIZE, c.first, c.second, c.third)
                    if (cell.building == null) {
                        addTree(wx - 0.45f, wz - 0.42f, 0.85f, state.season, builder)
                        addTree(wx + 0.42f, wz + 0.35f, 0.95f, state.season, builder)
                        addTree(wx - 0.15f, wz + 0.50f, 0.75f, state.season, builder)
                    }
                }
                else -> {
                    val checker = ((cell.x + cell.z) and 1) == 0
                    val g = if (state.season == Season.WINTER) {
                        if (checker) Triple(0.84f, 0.88f, 0.92f) else Triple(0.80f, 0.85f, 0.90f)
                    } else if (state.season == Season.AUTUMN) {
                        if (checker) Triple(0.42f, 0.58f, 0.26f) else Triple(0.39f, 0.54f, 0.24f)
                    } else {
                        if (checker) Triple(0.29f, 0.64f, 0.28f) else Triple(0.26f, 0.59f, 0.26f)
                    }
                    builder.addTopPlane(wx, 0f, wz, TILE_SIZE, TILE_SIZE, g.first, g.second, g.third)
                }
            }

            // Placed Building or Road
            val b = cell.building ?: continue
            if (b.type.isRoad) {
                val nUp = cellAt(cell.x, cell.z - 1)?.building?.type?.isRoad == true
                val nDown = cellAt(cell.x, cell.z + 1)?.building?.type?.isRoad == true
                val nLeft = cellAt(cell.x - 1, cell.z)?.building?.type?.isRoad == true
                val nRight = cellAt(cell.x + 1, cell.z)?.building?.type?.isRoad == true
                buildRoadTile(wx, wz, b.type, nUp, nDown, nLeft, nRight, state.season, builder)
            } else {
                val startFloat = builder.floatCount
                buildPlacedStructure(wx, wz, b, state.season, builder)
                if (!b.isConstructing && b.rotation % 4 != 0) {
                    builder.rotateVerticesY(startFloat, wx, wz, (b.rotation % 4) * (kotlin.math.PI.toFloat() * 0.5f))
                }
            }
        }
    }

    private fun buildCountrysideAndOuterHighway(gridSize: Int, season: Season, builder: MeshBuilder) {
        val half = (gridSize * TILE_SIZE) * 0.5f
        val outerR = half + 16f
        val bgCol = if (season == Season.WINTER) Triple(0.74f, 0.80f, 0.85f) else Triple(0.22f, 0.50f, 0.22f)
        // Outer terrain plane slightly below y=0 to avoid z-fighting
        builder.addTopPlane(0f, -0.02f, 0f, outerR * 2f, outerR * 2f, bgCol.first, bgCol.second, bgCol.third)

        // Outer 3-Lane-Per-Direction Highway Loop around the city (3 lanes going + 3 lanes returning + emergency shoulders)
        val hwDist = half + 3.2f
        val hwWidth = 3.60f
        val hwLength = hwDist * 2f + hwWidth
        // North & South outer highway asphalt
        builder.addBox(0f, 0.01f, -hwDist, hwLength, 0.04f, hwWidth, 0.15f, 0.17f, 0.20f)
        builder.addBox(0f, 0.01f, hwDist, hwLength, 0.04f, hwWidth, 0.15f, 0.17f, 0.20f)
        // East & West outer highway asphalt
        builder.addBox(-hwDist, 0.01f, 0f, hwWidth, 0.04f, hwLength, 0.15f, 0.17f, 0.20f)
        builder.addBox(hwDist, 0.01f, 0f, hwWidth, 0.04f, hwLength, 0.15f, 0.17f, 0.20f)

        // Outer emergency shoulder aprons + center barrier
        for (sign in listOf(-1f, 1f)) {
            val zMid = sign * hwDist
            // Concrete center jersey barrier + bright double yellow median lines (glow at night)
            builder.addBox(0f, 0.055f, zMid, hwLength - hwWidth, 0.06f, 0.10f, 0.62f, 0.64f, 0.68f)
            builder.addTopPlane(0f, 0.036f, zMid - 0.09f, hwLength - 1.2f, 0.05f, 1.0f, 0.86f, 0.18f, emissive = 1.32f)
            builder.addTopPlane(0f, 0.036f, zMid + 0.09f, hwLength - 1.2f, 0.05f, 1.0f, 0.86f, 0.18f, emissive = 1.32f)
            // Bright solid yellow emergency shoulder edge lines (at ±1.44f from center)
            builder.addTopPlane(0f, 0.036f, zMid - 1.44f, hwLength - 0.6f, 0.06f, 1.0f, 0.82f, 0.20f, emissive = 1.32f)
            builder.addTopPlane(0f, 0.036f, zMid + 1.44f, hwLength - 0.6f, 0.06f, 1.0f, 0.82f, 0.20f, emissive = 1.32f)
            // Emergency shoulder strip outside ±1.44f
            builder.addTopPlane(0f, 0.032f, zMid - 1.63f, hwLength - 0.2f, 0.30f, 0.24f, 0.26f, 0.30f)
            builder.addTopPlane(0f, 0.032f, zMid + 1.63f, hwLength - 0.2f, 0.30f, 0.24f, 0.26f, 0.30f)

            val xMid = sign * hwDist
            builder.addBox(xMid, 0.055f, 0f, 0.10f, 0.06f, hwLength - hwWidth, 0.62f, 0.64f, 0.68f)
            builder.addTopPlane(xMid - 0.09f, 0.036f, 0f, 0.05f, hwLength - 1.2f, 1.0f, 0.86f, 0.18f, emissive = 1.32f)
            builder.addTopPlane(xMid + 0.09f, 0.036f, 0f, 0.05f, hwLength - 1.2f, 1.0f, 0.86f, 0.18f, emissive = 1.32f)
            builder.addTopPlane(xMid - 1.44f, 0.036f, 0f, 0.06f, hwLength - 0.6f, 1.0f, 0.82f, 0.20f, emissive = 1.32f)
            builder.addTopPlane(xMid + 1.44f, 0.036f, 0f, 0.06f, hwLength - 0.6f, 1.0f, 0.82f, 0.20f, emissive = 1.32f)
            builder.addTopPlane(xMid - 1.63f, 0.032f, 0f, 0.30f, hwLength - 0.2f, 0.24f, 0.26f, 0.30f)
            builder.addTopPlane(xMid + 1.63f, 0.032f, 0f, 0.30f, hwLength - 0.2f, 0.24f, 0.26f, 0.30f)
        }

        // Bright dashed white lane lines separating all 3 lanes in each direction (±0.54f & ±0.99f) + reflective road studs
        val laneOffsets = listOf(-0.99f, -0.54f, 0.54f, 0.99f)
        var dashPos = -hwDist + 2.2f
        while (dashPos <= hwDist - 2.2f) {
            for (off in laneOffsets) {
                // North & South dashed white dividers
                builder.addTopPlane(dashPos, 0.037f, -hwDist + off, 0.95f, 0.05f, 0.98f, 0.99f, 1.0f, emissive = 1.32f)
                builder.addTopPlane(dashPos, 0.037f, hwDist + off, 0.95f, 0.05f, 0.98f, 0.99f, 1.0f, emissive = 1.32f)
                // East & West dashed white dividers
                builder.addTopPlane(-hwDist + off, 0.037f, dashPos, 0.05f, 0.95f, 0.98f, 0.99f, 1.0f, emissive = 1.32f)
                builder.addTopPlane(hwDist + off, 0.037f, dashPos, 0.05f, 0.95f, 0.98f, 0.99f, 1.0f, emissive = 1.32f)
            }
            dashPos += 1.9f
        }

        // Entry & Exit Ramps connecting outer 3-lane highway into city center avenues (x = 1.0f & z = 1.0f)
        val axisCenter = 1.0f
        // South & North Ramps at x = axisCenter
        for (dir in listOf(-1f, 1f)) {
            val rz = dir * (half + 0.85f)
            builder.addBox(axisCenter, 0.01f, rz, 1.85f, 0.04f, 2.2f, 0.17f, 0.19f, 0.22f)
            builder.addTopPlane(axisCenter, 0.037f, rz, 0.06f, 1.8f, 1.0f, 0.86f, 0.20f, emissive = 1.32f)
            builder.addRotatedBox(axisCenter - 1.3f, 0.01f, dir * (half + 1.4f), 2.1f, 0.04f, 0.85f, dir * -0.52f, 0.17f, 0.19f, 0.22f)
            builder.addRotatedBox(axisCenter + 1.3f, 0.01f, dir * (half + 1.4f), 2.1f, 0.04f, 0.85f, dir * 0.52f, 0.17f, 0.19f, 0.22f)
            // East & West Ramps at z = axisCenter
            val rx = dir * (half + 0.85f)
            builder.addBox(rx, 0.01f, axisCenter, 2.2f, 0.04f, 1.85f, 0.17f, 0.19f, 0.22f)
            builder.addTopPlane(rx, 0.037f, axisCenter, 1.8f, 0.06f, 1.0f, 0.86f, 0.20f, emissive = 1.32f)
        }

        // Dual-arm Highway Road Lights along all 4 sides of the outer 3-lane highway
        var lampCoord = -hwDist + 3.0f
        while (lampCoord <= hwDist - 3.0f) {
            addHighwayMedianLamp(lampCoord, -hwDist, horiz = true, builder = builder)
            addHighwayMedianLamp(lampCoord, hwDist, horiz = true, builder = builder)
            addHighwayMedianLamp(-hwDist, lampCoord, horiz = false, builder = builder)
            addHighwayMedianLamp(hwDist, lampCoord, horiz = false, builder = builder)
            lampCoord += 5.5f
        }

        // Decorative outer forest clusters
        val treeCoords = listOf(
            -half - 7f to -half - 6f, -half - 9f to 0f, -half - 7f to half + 7f,
            half + 7f to -half - 7f, half + 8f to 4f, half + 7f to half + 6f
        )
        for ((tx, tz) in treeCoords) {
            addTree(tx, tz, 1.25f, season, builder)
            addTree(tx + 1.6f, tz - 1.2f, 1.0f, season, builder)
        }
    }

    private fun buildRoadTile(
        wx: Float, wz: Float,
        type: BuildingType,
        nUp: Boolean, nDown: Boolean, nLeft: Boolean, nRight: Boolean,
        season: Season,
        builder: MeshBuilder
    ) {
        val roadWidth = when (type) {
            BuildingType.ROAD_HIGHWAY -> 1.92f   // 3 lanes + 3 lanes + 2 emergency shoulders
            BuildingType.ROAD_FOUR_LANE -> 1.66f // Double-way city road: 2 lanes + 2 lanes + median
            else -> 1.14f                        // 1+1 opposite lanes 2-way road
        }

        // Sidewalk or shoulder base
        if (type != BuildingType.ROAD_HIGHWAY) {
            builder.addBox(wx, 0.015f, wz, TILE_SIZE, 0.03f, TILE_SIZE, 0.64f, 0.66f, 0.69f)
        } else {
            builder.addBox(wx, 0.015f, wz, TILE_SIZE, 0.03f, TILE_SIZE, 0.34f, 0.36f, 0.39f)
        }

        val horiz = nLeft || nRight
        val vert = nUp || nDown
        val connections = listOf(nUp, nDown, nLeft, nRight).count { it }
        val isIntersection = horiz && vert

        // Central asphalt pad & directional branches
        builder.addBox(wx, 0.03f, wz, roadWidth, 0.04f, roadWidth, 0.16f, 0.18f, 0.21f)
        if (nUp) builder.addBox(wx, 0.03f, wz - 0.5f, roadWidth, 0.04f, 1.0f, 0.16f, 0.18f, 0.21f)
        if (nDown) builder.addBox(wx, 0.03f, wz + 0.5f, roadWidth, 0.04f, 1.0f, 0.16f, 0.18f, 0.21f)
        if (nLeft) builder.addBox(wx - 0.5f, 0.03f, wz, 1.0f, 0.04f, roadWidth, 0.16f, 0.18f, 0.21f)
        if (nRight) builder.addBox(wx + 0.5f, 0.03f, wz, 1.0f, 0.04f, roadWidth, 0.16f, 0.18f, 0.21f)
        if (!horiz && !vert) {
            builder.addBox(wx, 0.03f, wz, TILE_SIZE, 0.04f, roadWidth, 0.16f, 0.18f, 0.21f)
        }

        // Bright Reflective Lane Markings (glow visibly at night via emissive = 1.32f!)
        when (type) {
            BuildingType.ROAD_HIGHWAY -> {
                if (horiz || !vert) {
                    // Center yellow median + glowing edge lines
                    builder.addBox(wx, 0.056f, wz, TILE_SIZE, 0.04f, 0.07f, 0.60f, 0.62f, 0.66f)
                    builder.addTopPlane(wx, 0.056f, wz - 0.06f, TILE_SIZE, 0.035f, 1.0f, 0.86f, 0.18f, emissive = 1.32f)
                    builder.addTopPlane(wx, 0.056f, wz + 0.06f, TILE_SIZE, 0.035f, 1.0f, 0.86f, 0.18f, emissive = 1.32f)
                    // Emergency shoulder solid yellow edge lines
                    builder.addTopPlane(wx, 0.056f, wz - 0.72f, TILE_SIZE, 0.045f, 1.0f, 0.80f, 0.20f, emissive = 1.32f)
                    builder.addTopPlane(wx, 0.056f, wz + 0.72f, TILE_SIZE, 0.045f, 1.0f, 0.80f, 0.20f, emissive = 1.32f)
                    // Bright dashed white dividers for 3+3 lanes
                    for (off in listOf(-0.49f, -0.26f, 0.26f, 0.49f)) {
                        builder.addTopPlane(wx - 0.48f, 0.056f, wz + off, 0.48f, 0.035f, 0.98f, 0.99f, 1.0f, emissive = 1.32f)
                        builder.addTopPlane(wx + 0.48f, 0.056f, wz + off, 0.48f, 0.035f, 0.98f, 0.99f, 1.0f, emissive = 1.32f)
                    }
                }
                if (vert) {
                    builder.addBox(wx, 0.056f, wz, 0.07f, 0.04f, TILE_SIZE, 0.60f, 0.62f, 0.66f)
                    builder.addTopPlane(wx - 0.06f, 0.056f, wz, 0.035f, TILE_SIZE, 1.0f, 0.86f, 0.18f, emissive = 1.32f)
                    builder.addTopPlane(wx + 0.06f, 0.056f, wz, 0.035f, TILE_SIZE, 1.0f, 0.86f, 0.18f, emissive = 1.32f)
                    builder.addTopPlane(wx - 0.72f, 0.056f, wz, 0.045f, TILE_SIZE, 1.0f, 0.80f, 0.20f, emissive = 1.32f)
                    builder.addTopPlane(wx + 0.72f, 0.056f, wz, 0.045f, TILE_SIZE, 1.0f, 0.80f, 0.20f, emissive = 1.32f)
                    for (off in listOf(-0.49f, -0.26f, 0.26f, 0.49f)) {
                        builder.addTopPlane(wx + off, 0.056f, wz - 0.48f, 0.035f, 0.48f, 0.98f, 0.99f, 1.0f, emissive = 1.32f)
                        builder.addTopPlane(wx + off, 0.056f, wz + 0.48f, 0.035f, 0.48f, 0.98f, 0.99f, 1.0f, emissive = 1.32f)
                    }
                }
            }
            BuildingType.ROAD_FOUR_LANE -> {
                // Double-way city avenue: 2 lanes each side, raised median + bright yellow/white lines in both axes
                val lenX = if (isIntersection) 0.42f else 0.78f
                if (horiz || !vert) {
                    if (!isIntersection) {
                        builder.addBox(wx, 0.06f, wz, TILE_SIZE * 0.74f, 0.045f, 0.14f, 0.24f, 0.58f, 0.28f)
                    }
                    // Bright yellow center median edge lines
                    builder.addTopPlane(wx - 0.56f, 0.056f, wz - 0.10f, lenX, 0.035f, 1.0f, 0.86f, 0.18f, emissive = 1.32f)
                    builder.addTopPlane(wx + 0.56f, 0.056f, wz - 0.10f, lenX, 0.035f, 1.0f, 0.86f, 0.18f, emissive = 1.32f)
                    builder.addTopPlane(wx - 0.56f, 0.056f, wz + 0.10f, lenX, 0.035f, 1.0f, 0.86f, 0.18f, emissive = 1.32f)
                    builder.addTopPlane(wx + 0.56f, 0.056f, wz + 0.10f, lenX, 0.035f, 1.0f, 0.86f, 0.18f, emissive = 1.32f)
                    // Bright dashed white lane lines separating the 2 lanes per direction (±0.44f)
                    for (off in listOf(-0.44f, 0.44f)) {
                        builder.addTopPlane(wx - 0.48f, 0.056f, wz + off, 0.48f, 0.04f, 0.98f, 0.99f, 1.0f, emissive = 1.32f)
                        builder.addTopPlane(wx + 0.48f, 0.056f, wz + off, 0.48f, 0.04f, 0.98f, 0.99f, 1.0f, emissive = 1.32f)
                    }
                    // Bright outer curb white lines (±0.77f)
                    builder.addTopPlane(wx, 0.056f, wz - 0.77f, TILE_SIZE, 0.035f, 0.94f, 0.96f, 1.0f, emissive = 1.25f)
                    builder.addTopPlane(wx, 0.056f, wz + 0.77f, TILE_SIZE, 0.035f, 0.94f, 0.96f, 1.0f, emissive = 1.25f)
                }
                if (vert) {
                    if (!isIntersection) {
                        builder.addBox(wx, 0.06f, wz, 0.14f, 0.045f, TILE_SIZE * 0.74f, 0.24f, 0.58f, 0.28f)
                    }
                    builder.addTopPlane(wx - 0.10f, 0.056f, wz - 0.56f, 0.035f, lenX, 1.0f, 0.86f, 0.18f, emissive = 1.32f)
                    builder.addTopPlane(wx - 0.10f, 0.056f, wz + 0.56f, 0.035f, lenX, 1.0f, 0.86f, 0.18f, emissive = 1.32f)
                    builder.addTopPlane(wx + 0.10f, 0.056f, wz - 0.56f, 0.035f, lenX, 1.0f, 0.86f, 0.18f, emissive = 1.32f)
                    builder.addTopPlane(wx + 0.10f, 0.056f, wz + 0.56f, 0.035f, lenX, 1.0f, 0.86f, 0.18f, emissive = 1.32f)
                    for (off in listOf(-0.44f, 0.44f)) {
                        builder.addTopPlane(wx + off, 0.056f, wz - 0.48f, 0.04f, 0.48f, 0.98f, 0.99f, 1.0f, emissive = 1.32f)
                        builder.addTopPlane(wx + off, 0.056f, wz + 0.48f, 0.04f, 0.48f, 0.98f, 0.99f, 1.0f, emissive = 1.32f)
                    }
                    builder.addTopPlane(wx - 0.77f, 0.056f, wz, 0.035f, TILE_SIZE, 0.94f, 0.96f, 1.0f, emissive = 1.25f)
                    builder.addTopPlane(wx + 0.77f, 0.056f, wz, 0.035f, TILE_SIZE, 0.94f, 0.96f, 1.0f, emissive = 1.25f)
                }
            }
            else -> {
                // 1+1 Opposite Lanes 2-Way Street with bright center dashed yellow & crisp curb edge lines
                if (horiz || !vert) {
                    builder.addTopPlane(wx - 0.46f, 0.056f, wz, 0.48f, 0.06f, 1.0f, 0.88f, 0.22f, emissive = 1.32f)
                    builder.addTopPlane(wx + 0.46f, 0.056f, wz, 0.48f, 0.06f, 1.0f, 0.88f, 0.22f, emissive = 1.32f)
                    builder.addTopPlane(wx, 0.056f, wz - 0.51f, TILE_SIZE, 0.035f, 0.96f, 0.97f, 1.0f, emissive = 1.25f)
                    builder.addTopPlane(wx, 0.056f, wz + 0.51f, TILE_SIZE, 0.035f, 0.96f, 0.97f, 1.0f, emissive = 1.25f)
                }
                if (vert) {
                    builder.addTopPlane(wx, 0.056f, wz - 0.46f, 0.06f, 0.48f, 1.0f, 0.88f, 0.22f, emissive = 1.32f)
                    builder.addTopPlane(wx, 0.056f, wz + 0.46f, 0.06f, 0.48f, 1.0f, 0.88f, 0.22f, emissive = 1.32f)
                    builder.addTopPlane(wx - 0.51f, 0.056f, wz, 0.035f, TILE_SIZE, 0.96f, 0.97f, 1.0f, emissive = 1.25f)
                    builder.addTopPlane(wx + 0.51f, 0.056f, wz, 0.035f, TILE_SIZE, 0.96f, 0.97f, 1.0f, emissive = 1.25f)
                }
            }
        }

        // Bright crosswalk stripes at intersections
        if (isIntersection && type != BuildingType.ROAD_HIGHWAY) {
            for (stripe in listOf(-0.36f, -0.12f, 0.12f, 0.36f)) {
                if (nUp) builder.addTopPlane(wx + stripe, 0.057f, wz - 0.82f, 0.12f, 0.26f, 0.98f, 0.99f, 1.0f, emissive = 1.28f)
                if (nDown) builder.addTopPlane(wx + stripe, 0.057f, wz + 0.82f, 0.12f, 0.26f, 0.98f, 0.99f, 1.0f, emissive = 1.28f)
                if (nLeft) builder.addTopPlane(wx - 0.82f, 0.057f, wz + stripe, 0.26f, 0.12f, 0.98f, 0.99f, 1.0f, emissive = 1.28f)
                if (nRight) builder.addTopPlane(wx + 0.82f, 0.057f, wz + stripe, 0.26f, 0.12f, 0.98f, 0.99f, 1.0f, emissive = 1.28f)
            }
        }

        // Seasonal decorative trees, bright road lights & intersection traffic signals
        if (type != BuildingType.ROAD_HIGHWAY) {
            if (connections <= 2) {
                addTree(wx - 0.84f, wz - 0.84f, 0.50f, season, builder)
                val armDx = if (vert && !horiz) -0.26f else 0f
                val armDz = if (vert && !horiz) 0f else -0.26f
                addStreetLamp(wx + 0.84f, wz + 0.84f, armDx, armDz, builder)
                if (type == BuildingType.ROAD_FOUR_LANE) {
                    addStreetLamp(wx - 0.84f, wz + 0.84f, -armDx, armDz, builder)
                }
            } else {
                // Traffic light pole + streetlights at 3-way and 4-way city intersections
                addTrafficLightPole(wx - 0.80f, wz - 0.80f, builder)
                addTrafficLightPole(wx + 0.80f, wz + 0.80f, builder)
                addStreetLamp(wx - 0.84f, wz + 0.84f, 0.22f, -0.22f, builder)
                addStreetLamp(wx + 0.84f, wz - 0.84f, -0.22f, 0.22f, builder)
            }
        } else {
            addHighwayMedianLamp(wx, wz, horiz = (horiz || !vert), builder = builder)
        }
    }

    private fun addStreetLamp(
        x: Float, z: Float,
        armDx: Float = 0f, armDz: Float = 0f,
        builder: MeshBuilder
    ) {
        builder.addCylinder(x, 0.03f, z, 0.045f, 0.78f, 6, 0.30f, 0.34f, 0.38f)
        val lx = x + armDx
        val lz = z + armDz
        if (armDx != 0f || armDz != 0f) {
            builder.addBox((x + lx) * 0.5f, 0.80f, (z + lz) * 0.5f, kotlin.math.abs(armDx) + 0.06f, 0.04f, kotlin.math.abs(armDz) + 0.06f, 0.36f, 0.40f, 0.45f)
        }
        // Dark upper hood + bright night-glowing warm-white LED luminaire underneath
        builder.addBox(lx, 0.83f, lz, 0.18f, 0.04f, 0.14f, 0.28f, 0.32f, 0.36f)
        builder.addBox(lx, 0.78f, lz, 0.16f, 0.06f, 0.12f, 1.0f, 0.94f, 0.62f, emissive = 1.38f)
    }

    private fun addHighwayMedianLamp(x: Float, z: Float, horiz: Boolean, builder: MeshBuilder) {
        // Tall dual-arm highway light pole
        builder.addCylinder(x, 0.05f, z, 0.055f, 1.15f, 6, 0.46f, 0.50f, 0.56f)
        if (horiz) {
            builder.addBox(x, 1.18f, z, 0.07f, 0.05f, 1.15f, 0.50f, 0.54f, 0.60f)
            builder.addBox(x, 1.13f, z - 0.52f, 0.16f, 0.06f, 0.22f, 1.0f, 0.95f, 0.68f, emissive = 1.38f)
            builder.addBox(x, 1.13f, z + 0.52f, 0.16f, 0.06f, 0.22f, 1.0f, 0.95f, 0.68f, emissive = 1.38f)
        } else {
            builder.addBox(x, 1.18f, z, 1.15f, 0.05f, 0.07f, 0.50f, 0.54f, 0.60f)
            builder.addBox(x - 0.52f, 1.13f, z, 0.22f, 0.06f, 0.16f, 1.0f, 0.95f, 0.68f, emissive = 1.38f)
            builder.addBox(x + 0.52f, 1.13f, z, 0.22f, 0.06f, 0.16f, 1.0f, 0.95f, 0.68f, emissive = 1.38f)
        }
    }

    private fun addTrafficLightPole(x: Float, z: Float, builder: MeshBuilder) {
        builder.addBox(x, 0.38f, z, 0.07f, 0.76f, 0.07f, 0.22f, 0.24f, 0.26f)
        builder.addBox(x, 0.76f, z, 0.14f, 0.26f, 0.14f, 0.12f, 0.12f, 0.14f)
    }

    fun addTree(x: Float, z: Float, scale: Float, season: Season, builder: MeshBuilder) {
        val (r, g, b) = seasonalFoliageColor(season)
        val (ar, ag, ab) = seasonalAccentBlossom(season)
        builder.addCylinder(x, 0f, z, 0.07f * scale, 0.42f * scale, 6, 0.42f, 0.28f, 0.16f)
        builder.addCone(x, 0.34f * scale, z, 0.34f * scale, 0.62f * scale, 7, r, g, b)
        builder.addCone(x, 0.62f * scale, z, 0.24f * scale, 0.45f * scale, 7, ar, ag, ab)
    }

    private fun buildPlacedStructure(
        wx: Float, wz: Float,
        b: PlacedBuilding,
        season: Season,
        builder: MeshBuilder
    ) {
        // Architectural stone/concrete lot pad with subtle curb border
        builder.addBox(wx, 0.02f, wz, TILE_SIZE * 0.94f, 0.04f, TILE_SIZE * 0.94f, 0.56f, 0.58f, 0.62f)

        if (b.isConstructing) {
            val pct = if (b.constructionTotalSec > 0) {
                1f - (b.constructionRemainingSec.toFloat() / b.constructionTotalSec.toFloat())
            } else 0.5f
            val h = 0.45f + pct.coerceIn(0.1f, 1f) * 1.45f
            // Exposed structural steel/concrete frame + crane
            builder.addBox(wx, 0.04f + h * 0.5f, wz, 1.08f, h, 1.08f, 0.68f, 0.62f, 0.50f)
            for (cx in listOf(-0.56f, 0.56f)) {
                for (cz in listOf(-0.56f, 0.56f)) {
                    builder.addBox(wx + cx, 0.04f + h * 0.55f, wz + cz, 0.09f, h * 1.1f, 0.09f, 0.45f, 0.48f, 0.52f)
                }
            }
            builder.addBox(wx - 0.62f, 1.25f, wz - 0.62f, 0.12f, 2.4f, 0.12f, 0.96f, 0.74f, 0.12f)
            builder.addBox(wx + 0.05f, 2.38f, wz - 0.62f, 1.45f, 0.12f, 0.12f, 0.96f, 0.74f, 0.12f)
            return
        }

        val lvlScale = 1f + (b.level - 1) * 0.22f
        when (b.type) {
            BuildingType.GROUND_HOUSE -> {
                // Stone porch + main cottage + peaked gable roof + chimney + front door & framed windows
                builder.addBox(wx, 0.34f, wz - 0.05f, 1.12f, 0.56f, 0.96f, 0.95f, 0.91f, 0.84f)
                builder.addGableRoof(wx, 0.62f, wz - 0.05f, 1.28f, 0.44f, 1.14f, 0.78f, 0.28f, 0.22f)
                builder.addBox(wx + 0.32f, 0.86f, wz - 0.26f, 0.16f, 0.42f, 0.16f, 0.58f, 0.24f, 0.20f) // Chimney
                builder.addBox(wx, 0.10f, wz + 0.52f, 0.48f, 0.12f, 0.26f, 0.74f, 0.72f, 0.68f) // Porch steps
                builder.addBox(wx, 0.27f, wz + 0.44f, 0.20f, 0.36f, 0.04f, 0.42f, 0.24f, 0.14f) // Wood door
                // Framed glowing windows
                builder.addBox(wx - 0.32f, 0.38f, wz + 0.44f, 0.22f, 0.20f, 0.04f, 1.0f, 0.90f, 0.52f, emissive = 1.0f)
                builder.addBox(wx + 0.32f, 0.38f, wz + 0.44f, 0.22f, 0.20f, 0.04f, 1.0f, 0.90f, 0.52f, emissive = 1.0f)
                builder.addBox(wx, 0.38f, wz - 0.05f, 1.15f, 0.18f, 0.42f, 1.0f, 0.90f, 0.52f, emissive = 1.0f)
                addTree(wx + 0.70f, wz + 0.55f, 0.48f, season, builder)
                if (b.level >= 2) addTree(wx - 0.70f, wz + 0.55f, 0.45f, season, builder)
            }
            BuildingType.SMALL_APARTMENT -> {
                val h = 1.20f * lvlScale
                builder.addBox(wx, 0.04f + h * 0.5f, wz, 1.24f, h, 1.10f, 0.86f, 0.78f, 0.68f)
                // Entry lobby canopy + vertical balcony bays
                builder.addBox(wx, 0.26f, wz + 0.62f, 0.52f, 0.08f, 0.24f, 0.20f, 0.54f, 0.76f)
                addWindowBands(wx, 0.24f, wz, 1.27f, 1.13f, h - 0.1f, 3 + b.level, builder)
                // Exterior balcony slabs
                for (story in 1..(1 + b.level).coerceAtMost(4)) {
                    val by = 0.15f + story * (h / (2.5f + b.level))
                    builder.addBox(wx - 0.34f, by, wz + 0.60f, 0.38f, 0.06f, 0.18f, 0.92f, 0.94f, 0.96f)
                    builder.addBox(wx + 0.34f, by, wz + 0.60f, 0.38f, 0.06f, 0.18f, 0.92f, 0.94f, 0.96f)
                }
                builder.addGableRoof(wx, 0.04f + h, wz, 1.32f, 0.28f, 1.18f, 0.42f, 0.48f, 0.56f)
            }
            BuildingType.LARGE_APARTMENT -> {
                val h = 2.10f * lvlScale
                // Twin-wing residential tower with recessed glass core
                builder.addBox(wx - 0.34f, 0.04f + h * 0.5f, wz, 0.62f, h, 1.26f, 0.78f, 0.82f, 0.88f)
                builder.addBox(wx + 0.34f, 0.04f + h * 0.5f, wz, 0.62f, h, 1.26f, 0.78f, 0.82f, 0.88f)
                builder.addBox(wx, 0.04f + h * 0.46f, wz, 0.32f, h * 0.92f, 0.95f, 0.24f, 0.48f, 0.68f)
                addWindowBands(wx, 0.24f, wz, 1.33f, 1.29f, h - 0.15f, 5 + b.level, builder)
                // Rooftop mechanical house & water tank
                builder.addBox(wx, 0.12f + h, wz - 0.15f, 0.72f, 0.22f, 0.64f, 0.42f, 0.46f, 0.52f)
                builder.addCylinder(wx + 0.32f, 0.04f + h, wz + 0.28f, 0.14f, 0.28f, 8, 0.72f, 0.76f, 0.80f)
            }
            BuildingType.LUXURY_APARTMENT -> {
                val h = 2.90f * lvlScale
                // Stepped luxury residential tower with wraparound balconies & sky-pool
                builder.addBox(wx, 0.04f + h * 0.35f, wz, 1.46f, h * 0.7f, 1.40f, 0.94f, 0.90f, 0.82f)
                builder.addBox(wx, 0.04f + h * 0.82f, wz, 1.18f, h * 0.36f, 1.12f, 0.96f, 0.92f, 0.86f)
                addWindowBands(wx, 0.25f, wz, 1.49f, 1.43f, h * 0.66f, 5 + b.level, builder)
                addWindowBands(wx, 0.08f + h * 0.70f, wz, 1.21f, 1.15f, h * 0.30f, 3, builder)
                // Corner architectural columns
                for (cx in listOf(-0.68f, 0.68f)) {
                    for (cz in listOf(-0.65f, 0.65f)) {
                        builder.addBox(wx + cx, 0.04f + h * 0.35f, wz + cz, 0.12f, h * 0.72f, 0.12f, 0.88f, 0.74f, 0.36f)
                    }
                }
                // Gold penthouse crown & glowing rooftop pool
                builder.addBox(wx, 0.08f + h, wz, 0.96f, 0.18f, 0.92f, 0.96f, 0.78f, 0.26f)
                builder.addTopPlane(wx, 0.18f + h, wz, 0.66f, 0.58f, 0.16f, 0.78f, 0.98f, emissive = 0.8f)
            }
            BuildingType.SKYSCRAPER -> {
                val h = 4.10f * lvlScale
                // Grand lobby podium + tapered curtain-wall glass tower + vertical ribs + spire
                builder.addBox(wx, 0.32f, wz, 1.56f, 0.56f, 1.56f, 0.20f, 0.30f, 0.44f)
                builder.addBox(wx, 0.56f + h * 0.38f, wz, 1.32f, h * 0.76f, 1.32f, 0.22f, 0.60f, 0.88f)
                builder.addBox(wx, 0.56f + h * 0.82f, wz, 0.96f, h * 0.26f, 0.96f, 0.34f, 0.76f, 0.96f)
                // Vertical metallic mullions on all 4 facades
                for (off in listOf(-0.42f, 0f, 0.42f)) {
                    builder.addBox(wx + off, 0.56f + h * 0.38f, wz, 0.06f, h * 0.78f, 1.36f, 0.76f, 0.84f, 0.92f)
                    builder.addBox(wx, 0.56f + h * 0.38f, wz + off, 1.36f, h * 0.78f, 0.06f, 0.76f, 0.84f, 0.92f)
                }
                addWindowBands(wx, 0.65f, wz, 1.34f, 1.34f, h * 0.72f, 10 + b.level * 2, builder)
                // Slanted architectural crown & dual spires + red aviation beacon
                builder.addPyramid(wx, 0.56f + h * 0.95f, wz, 0.96f, 0.55f, 0.96f, 0.48f, 0.86f, 1.0f)
                builder.addCylinder(wx, 0.56f + h * 0.95f + 0.40f, wz, 0.05f, 0.95f, 6, 0.90f, 0.94f, 0.98f)
                builder.addBox(wx, 0.56f + h * 0.95f + 1.36f, wz, 0.12f, 0.12f, 0.12f, 1.0f, 0.2f, 0.2f, emissive = 2.0f)
            }
            BuildingType.SHOP -> {
                builder.addBox(wx, 0.42f, wz - 0.05f, 1.28f, 0.76f, 1.12f, 0.94f, 0.58f, 0.34f)
                // Striped storefront awning + display glass + parapet sign
                builder.addRotatedTrapezoid(wx, 0.46f, wz + 0.58f, 1.26f, 1.18f, 0.16f, 0.28f, 0.10f, 0f, 0.96f, 0.26f, 0.26f)
                builder.addBox(wx, 0.28f, wz + 0.52f, 1.05f, 0.36f, 0.05f, 1.0f, 0.94f, 0.68f, emissive = 1.0f)
                builder.addBox(wx, 0.88f, wz + 0.42f, 0.90f, 0.18f, 0.10f, 0.98f, 0.82f, 0.20f, emissive = 0.8f)
            }
            BuildingType.RESTAURANT -> {
                builder.addBox(wx - 0.10f, 0.44f, wz - 0.12f, 1.20f, 0.80f, 1.04f, 0.84f, 0.28f, 0.28f)
                builder.addGableRoof(wx - 0.10f, 0.84f, wz - 0.12f, 1.28f, 0.30f, 1.12f, 0.94f, 0.84f, 0.68f)
                builder.addBox(wx - 0.10f, 0.32f, wz + 0.42f, 0.96f, 0.32f, 0.05f, 1.0f, 0.88f, 0.48f, emissive = 1.0f)
                // Outdoor patio parasols
                for (px in listOf(-0.35f, 0.35f)) {
                    builder.addCylinder(wx + px, 0.04f, wz + 0.62f, 0.03f, 0.36f, 6, 0.35f, 0.35f, 0.38f)
                    builder.addCone(wx + px, 0.36f, wz + 0.62f, 0.24f, 0.14f, 8, 0.98f, 0.86f, 0.24f)
                }
            }
            BuildingType.EMPLOYMENT_OFFICE -> {
                builder.addBox(wx, 0.68f, wz, 1.30f, 1.28f, 1.16f, 0.34f, 0.48f, 0.68f)
                builder.addBox(wx, 0.24f, wz + 0.62f, 0.56f, 0.40f, 0.16f, 0.86f, 0.90f, 0.94f)
                addWindowBands(wx, 0.32f, wz, 1.33f, 1.19f, 1.12f, 3, builder)
            }
            BuildingType.COMPANY -> {
                val h = 2.40f * lvlScale
                builder.addBox(wx, 0.04f + h * 0.5f, wz, 1.36f, h, 1.24f, 0.22f, 0.34f, 0.50f)
                for (off in listOf(-0.42f, 0.42f)) {
                    builder.addBox(wx + off, 0.04f + h * 0.5f, wz, 0.12f, h + 0.08f, 1.30f, 0.68f, 0.74f, 0.82f)
                }
                addWindowBands(wx, 0.25f, wz, 1.38f, 1.27f, h - 0.1f, 6, builder)
                builder.addBox(wx, 0.12f + h, wz, 0.82f, 0.20f, 0.34f, 0.22f, 0.80f, 0.98f, emissive = 1.2f)
            }
            BuildingType.LUXURY_ADMIN_BUILDING -> {
                // Grand Neoclassical City Hall: stairs, 6 portico columns, pediment, ribbed golden dome
                builder.addBox(wx, 0.10f, wz + 0.12f, 1.78f, 0.14f, 1.62f, 0.84f, 0.82f, 0.78f)
                builder.addBox(wx, 0.62f, wz - 0.06f, 1.56f, 0.92f, 1.22f, 0.95f, 0.93f, 0.88f)
                // 6 Front Corinthian Columns
                for (i in 0..5) {
                    val cx = wx - 0.60f + i * 0.24f
                    builder.addCylinder(cx, 0.16f, wz + 0.68f, 0.06f, 0.88f, 8, 0.96f, 0.95f, 0.92f)
                }
                builder.addGableRoof(wx, 1.04f, wz + 0.58f, 1.48f, 0.28f, 0.32f, 0.88f, 0.84f, 0.76f)
                builder.addCylinder(wx, 1.08f, wz - 0.06f, 0.46f, 0.38f, 12, 0.92f, 0.90f, 0.84f)
                builder.addCylinder(wx, 1.46f, wz - 0.06f, 0.42f, 0.32f, 12, 0.96f, 0.78f, 0.22f)
                builder.addCone(wx, 1.78f, wz - 0.06f, 0.42f, 0.42f, 12, 0.98f, 0.82f, 0.26f)
                addWindowBands(wx, 0.32f, wz - 0.06f, 1.59f, 1.25f, 0.72f, 2, builder)
            }
            BuildingType.SCHOOL -> {
                // Winged brick schoolhouse + clock gable tower + courtyard flag
                builder.addBox(wx, 0.48f, wz - 0.10f, 1.60f, 0.88f, 0.98f, 0.86f, 0.42f, 0.24f)
                builder.addGableRoof(wx, 0.92f, wz - 0.10f, 1.68f, 0.28f, 1.08f, 0.34f, 0.40f, 0.48f)
                builder.addBox(wx, 1.18f, wz - 0.10f, 0.40f, 0.42f, 0.40f, 0.94f, 0.90f, 0.80f)
                builder.addPyramid(wx, 1.39f, wz - 0.10f, 0.46f, 0.38f, 0.46f, 0.20f, 0.44f, 0.68f)
                addWindowBands(wx, 0.28f, wz - 0.10f, 1.63f, 1.01f, 0.68f, 2, builder)
            }
            BuildingType.WATER_PLANT -> {
                // Filtration basins + elevated conical water tower on 4 legs
                builder.addCylinder(wx - 0.38f, 0.04f, wz + 0.22f, 0.42f, 0.46f, 12, 0.76f, 0.82f, 0.86f)
                builder.addTopPlane(wx - 0.38f, 0.51f, wz + 0.22f, 0.54f, 0.54f, 0.16f, 0.72f, 0.96f, emissive = 0.6f)
                for (lx in listOf(0.18f, 0.54f)) {
                    for (lz in listOf(-0.52f, -0.16f)) {
                        builder.addCylinder(wx + lx, 0.04f, wz + lz, 0.04f, 1.10f, 6, 0.45f, 0.50f, 0.56f)
                    }
                }
                builder.addCylinder(wx + 0.36f, 1.05f, wz - 0.34f, 0.34f, 0.42f, 12, 0.22f, 0.72f, 0.96f)
                builder.addCone(wx + 0.36f, 1.47f, wz - 0.34f, 0.36f, 0.24f, 12, 0.16f, 0.54f, 0.80f)
            }
            BuildingType.FIRE_STATION -> {
                builder.addBox(wx, 0.46f, wz, 1.46f, 0.84f, 1.24f, 0.86f, 0.20f, 0.20f)
                // Twin fire truck bay doors
                builder.addBox(wx - 0.34f, 0.32f, wz + 0.63f, 0.44f, 0.52f, 0.04f, 0.94f, 0.94f, 0.96f)
                builder.addBox(wx + 0.34f, 0.32f, wz + 0.63f, 0.44f, 0.52f, 0.04f, 0.94f, 0.94f, 0.96f)
                // Bell & hose drying tower
                builder.addBox(wx - 0.46f, 0.98f, wz - 0.36f, 0.36f, 1.05f, 0.36f, 0.90f, 0.28f, 0.24f)
                builder.addPyramid(wx - 0.46f, 1.50f, wz - 0.36f, 0.42f, 0.26f, 0.42f, 0.30f, 0.32f, 0.36f)
            }
            BuildingType.POLICE_STATION -> {
                builder.addBox(wx, 0.56f, wz, 1.48f, 1.04f, 1.24f, 0.18f, 0.34f, 0.66f)
                builder.addBox(wx, 0.28f, wz + 0.64f, 0.62f, 0.48f, 0.12f, 0.90f, 0.92f, 0.96f)
                addWindowBands(wx, 0.34f, wz, 1.51f, 1.27f, 0.72f, 2, builder)
                builder.addCylinder(wx, 1.08f, wz, 0.05f, 0.52f, 6, 0.82f, 0.86f, 0.90f)
                builder.addBox(wx, 1.62f, wz, 0.18f, 0.14f, 0.18f, 0.20f, 0.55f, 1.0f, emissive = 2.0f)
            }
            BuildingType.HOSPITAL -> {
                // H-wing medical complex + ambulance canopy + rooftop helipad
                builder.addBox(wx - 0.46f, 0.74f, wz, 0.54f, 1.40f, 1.38f, 0.95f, 0.96f, 0.98f)
                builder.addBox(wx + 0.46f, 0.74f, wz, 0.54f, 1.40f, 1.38f, 0.95f, 0.96f, 0.98f)
                builder.addBox(wx, 0.64f, wz, 0.60f, 1.20f, 0.72f, 0.88f, 0.94f, 0.98f)
                addWindowBands(wx - 0.46f, 0.26f, wz, 0.57f, 1.41f, 1.25f, 4, builder)
                addWindowBands(wx + 0.46f, 0.26f, wz, 0.57f, 1.41f, 1.25f, 4, builder)
                // Glowing red medical cross
                builder.addBox(wx, 1.28f, wz, 0.55f, 0.08f, 0.18f, 0.94f, 0.18f, 0.22f, emissive = 1.6f)
                builder.addBox(wx, 1.28f, wz, 0.18f, 0.08f, 0.55f, 0.94f, 0.18f, 0.22f, emissive = 1.6f)
            }
            BuildingType.PRISON -> {
                builder.addBox(wx, 0.48f, wz, 1.46f, 0.88f, 1.36f, 0.44f, 0.46f, 0.50f)
                for (dx in listOf(-0.74f, 0.74f)) {
                    for (dz in listOf(-0.70f, 0.70f)) {
                        builder.addBox(wx + dx, 0.76f, wz + dz, 0.24f, 1.44f, 0.24f, 0.34f, 0.36f, 0.40f)
                        builder.addPyramid(wx + dx, 1.48f, wz + dz, 0.30f, 0.22f, 0.30f, 0.22f, 0.24f, 0.28f)
                    }
                }
            }
            BuildingType.AMUSEMENT_PARK -> {
                // Big-top circus pavilion + spoked 10-gondola 3D Ferris Wheel
                builder.addCylinder(wx - 0.42f, 0.04f, wz + 0.35f, 0.44f, 0.48f, 10, 0.95f, 0.36f, 0.52f)
                builder.addCone(wx - 0.42f, 0.52f, wz + 0.35f, 0.48f, 0.46f, 10, 0.98f, 0.82f, 0.22f)
                // Ferris Wheel A-frame supports + circular rim + 8 colorful passenger cars
                val fwX = wx + 0.34f; val fwY = 1.15f; val fwZ = wz - 0.18f; val fwR = 0.68f
                builder.addBox(fwX, 0.62f, fwZ - 0.20f, 0.10f, 1.15f, 0.08f, 0.78f, 0.82f, 0.88f)
                builder.addBox(fwX, 0.62f, fwZ + 0.20f, 0.10f, 1.15f, 0.08f, 0.78f, 0.82f, 0.88f)
                for (i in 0 until 8) {
                    val a = i * (2.0 * Math.PI / 8.0).toFloat()
                    val cy = fwY + kotlin.math.sin(a) * fwR
                    val cz = fwZ + kotlin.math.cos(a) * fwR
                    // Spoke
                    builder.addBox(fwX, (fwY + cy) * 0.5f, (fwZ + cz) * 0.5f, 0.05f, 0.32f, 0.32f, 0.88f, 0.90f, 0.94f)
                    // Colorful gondola car
                    builder.addBox(fwX, cy, cz, 0.20f, 0.18f, 0.18f, if (i % 2 == 0) 0.98f else 0.22f, if (i % 2 == 0) 0.40f else 0.78f, 0.92f, emissive = 1.0f)
                }
            }
            BuildingType.WATER_AMUSEMENT_PARK -> {
                builder.addTopPlane(wx, 0.06f, wz, 1.68f, 1.68f, 0.10f, 0.78f, 0.98f, emissive = 0.6f)
                builder.addCylinder(wx - 0.38f, 0.06f, wz - 0.38f, 0.16f, 1.48f, 8, 0.96f, 0.84f, 0.24f)
                // Winding multi-tier slide chutes
                builder.addRotatedBox(wx + 0.08f, 1.15f, wz - 0.22f, 0.88f, 0.16f, 0.26f, 0.35f, 0.18f, 0.62f, 0.98f)
                builder.addRotatedBox(wx + 0.35f, 0.78f, wz + 0.18f, 0.82f, 0.16f, 0.26f, 1.25f, 0.96f, 0.32f, 0.62f)
                builder.addRotatedBox(wx - 0.10f, 0.40f, wz + 0.42f, 0.78f, 0.16f, 0.26f, -0.35f, 0.98f, 0.78f, 0.18f)
            }
            BuildingType.CROP_FIELD -> {
                builder.addTopPlane(wx, 0.05f, wz, 1.75f, 1.75f, 0.42f, 0.28f, 0.16f)
                for (row in -2..2) {
                    builder.addBox(wx, 0.15f, wz + row * 0.32f, 1.56f, 0.18f, 0.16f, 0.86f, 0.74f, 0.24f)
                }
            }
            BuildingType.LIVESTOCK_PASTURE -> {
                builder.addTopPlane(wx, 0.05f, wz, 1.75f, 1.75f, 0.34f, 0.68f, 0.28f)
                // Gambrel red barn + white trim + wooden fence posts
                builder.addBox(wx - 0.48f, 0.34f, wz - 0.48f, 0.62f, 0.56f, 0.64f, 0.80f, 0.22f, 0.20f)
                builder.addGableRoof(wx - 0.48f, 0.62f, wz - 0.48f, 0.70f, 0.32f, 0.72f, 0.46f, 0.26f, 0.18f)
                builder.addBox(wx - 0.48f, 0.30f, wz - 0.15f, 0.28f, 0.36f, 0.04f, 0.96f, 0.96f, 0.96f)
                builder.addBox(wx, 0.14f, wz + 0.82f, 1.72f, 0.12f, 0.06f, 0.58f, 0.40f, 0.22f)
                builder.addBox(wx + 0.82f, 0.14f, wz, 0.06f, 0.12f, 1.72f, 0.58f, 0.40f, 0.22f)
            }
            BuildingType.FOREST_GROVE -> {
                addTree(wx - 0.45f, wz - 0.40f, 0.95f, season, builder)
                addTree(wx + 0.45f, wz - 0.35f, 1.05f, season, builder)
                addTree(wx - 0.25f, wz + 0.45f, 0.90f, season, builder)
                addTree(wx + 0.35f, wz + 0.40f, 0.82f, season, builder)
            }
            BuildingType.FOOD_FACTORY,
            BuildingType.CLOTHING_FACTORY,
            BuildingType.EQUIPMENT_FACTORY,
            BuildingType.MEDICINE_FACTORY,
            BuildingType.RECYCLING_FACTORY,
            BuildingType.TRANSPORT_FACTORY -> {
                val (fr, fg, fb) = when (b.type) {
                    BuildingType.FOOD_FACTORY -> Triple(0.86f, 0.62f, 0.26f)
                    BuildingType.CLOTHING_FACTORY -> Triple(0.76f, 0.36f, 0.68f)
                    BuildingType.EQUIPMENT_FACTORY -> Triple(0.45f, 0.52f, 0.60f)
                    BuildingType.MEDICINE_FACTORY -> Triple(0.24f, 0.76f, 0.74f)
                    BuildingType.RECYCLING_FACTORY -> Triple(0.26f, 0.68f, 0.38f)
                    else -> Triple(0.28f, 0.48f, 0.82f)
                }
                builder.addBox(wx, 0.46f, wz, 1.48f, 0.84f, 1.32f, fr, fg, fb)
                // Twin pitched industrial sawtooth roofs + storage silo + chimneys
                builder.addGableRoof(wx - 0.36f, 0.88f, wz, 0.70f, 0.28f, 1.32f, fr * 0.85f, fg * 0.85f, fb * 0.85f)
                builder.addGableRoof(wx + 0.36f, 0.88f, wz, 0.70f, 0.28f, 1.32f, fr * 0.85f, fg * 0.85f, fb * 0.85f)
                builder.addCylinder(wx - 0.54f, 0.04f, wz + 0.42f, 0.22f, 1.05f, 10, 0.82f, 0.86f, 0.90f)
                builder.addCylinder(wx - 0.48f, 0.88f, wz - 0.46f, 0.11f, 0.80f, 8, 0.36f, 0.38f, 0.42f)
                builder.addCylinder(wx + 0.48f, 0.88f, wz - 0.46f, 0.11f, 0.66f, 8, 0.36f, 0.38f, 0.42f)
            }
            BuildingType.FUEL_STATION -> {
                builder.addBox(wx, 0.06f, wz, 1.72f, 0.08f, 1.72f, 0.24f, 0.26f, 0.30f)
                builder.addBox(wx, 0.72f, wz, 1.36f, 0.14f, 0.98f, 0.96f, 0.36f, 0.18f, emissive = 0.8f)
                // Red/white fuel dispensers + canopy pillars
                for (px in listOf(-0.42f, 0.42f)) {
                    builder.addBox(wx + px, 0.38f, wz, 0.14f, 0.60f, 0.14f, 0.92f, 0.94f, 0.96f)
                    builder.addBox(wx + px * 0.5f, 0.26f, wz + 0.18f, 0.20f, 0.36f, 0.16f, 0.92f, 0.24f, 0.22f)
                }
            }
            BuildingType.HIGHWAY_TOLL -> {
                builder.addBox(wx, 0.05f, wz, 1.85f, 0.06f, 1.85f, 0.22f, 0.24f, 0.28f)
                builder.addBox(wx, 0.78f, wz, 1.75f, 0.18f, 0.65f, 0.16f, 0.68f, 0.42f, emissive = 0.8f)
                for (bx in listOf(-0.55f, 0f, 0.55f)) {
                    builder.addBox(wx + bx, 0.38f, wz, 0.18f, 0.66f, 0.42f, 0.90f, 0.92f, 0.94f)
                    builder.addBox(wx + bx + 0.24f, 0.32f, wz + 0.18f, 0.30f, 0.04f, 0.04f, 0.96f, 0.24f, 0.22f) // Toll gate bar
                }
            }
            BuildingType.BUS_STATION -> {
                builder.addBox(wx, 0.38f, wz - 0.12f, 1.48f, 0.68f, 0.88f, 0.94f, 0.68f, 0.18f)
                builder.addRotatedTrapezoid(wx, 0.72f, wz + 0.05f, 1.56f, 1.40f, 0.14f, 1.18f, 1.05f, 0f, 0.22f, 0.34f, 0.52f)
                // Bus bay awnings
                builder.addBox(wx - 0.42f, 0.26f, wz + 0.48f, 0.42f, 0.24f, 0.24f, 0.96f, 0.72f, 0.18f)
                builder.addBox(wx + 0.42f, 0.26f, wz + 0.48f, 0.42f, 0.24f, 0.24f, 0.96f, 0.72f, 0.18f)
            }
            BuildingType.TAXI_STATION -> {
                builder.addBox(wx, 0.34f, wz, 1.32f, 0.60f, 1.05f, 0.98f, 0.84f, 0.16f)
                builder.addGableRoof(wx, 0.64f, wz, 1.40f, 0.22f, 1.12f, 0.18f, 0.20f, 0.24f)
                builder.addBox(wx, 0.92f, wz, 0.58f, 0.16f, 0.26f, 1.0f, 0.92f, 0.35f, emissive = 1.2f)
            }
            BuildingType.TRAM_STATION -> {
                builder.addBox(wx, 0.30f, wz, 1.56f, 0.52f, 0.76f, 0.26f, 0.74f, 0.58f)
                builder.addGableRoof(wx, 0.56f, wz, 1.66f, 0.20f, 0.94f, 0.88f, 0.92f, 0.96f)
            }
            BuildingType.TRAIN_STATION -> {
                builder.addBox(wx, 0.42f, wz - 0.16f, 1.68f, 0.76f, 0.88f, 0.66f, 0.38f, 0.28f)
                builder.addGableRoof(wx, 0.80f, wz - 0.16f, 1.76f, 0.34f, 0.96f, 0.26f, 0.40f, 0.56f)
                builder.addBox(wx, 1.05f, wz - 0.16f, 0.38f, 0.36f, 0.38f, 0.94f, 0.90f, 0.80f) // Station clock gable
                builder.addBox(wx, 0.06f, wz + 0.52f, 1.95f, 0.04f, 0.08f, 0.72f, 0.74f, 0.76f)
                builder.addBox(wx, 0.06f, wz + 0.74f, 1.95f, 0.04f, 0.08f, 0.72f, 0.74f, 0.76f)
            }
            BuildingType.METRO_STATION -> {
                builder.addRotatedTrapezoid(wx, 0.04f, wz, 1.42f, 1.18f, 0.44f, 1.38f, 1.14f, 0f, 0.20f, 0.48f, 0.88f)
                builder.addBox(wx, 0.62f, wz, 0.60f, 0.32f, 0.24f, 0.98f, 0.82f, 0.18f, emissive = 1.4f)
            }
            BuildingType.SEAPORT -> {
                builder.addBox(wx, 0.16f, wz, 1.85f, 0.26f, 1.85f, 0.58f, 0.52f, 0.42f)
                builder.addBox(wx - 0.42f, 0.88f, wz - 0.28f, 0.16f, 1.35f, 0.16f, 0.92f, 0.34f, 0.22f)
                builder.addBox(wx - 0.42f, 0.88f, wz + 0.28f, 0.16f, 1.35f, 0.16f, 0.92f, 0.34f, 0.22f)
                builder.addBox(wx + 0.16f, 1.52f, wz, 1.32f, 0.16f, 0.64f, 0.92f, 0.34f, 0.22f)
                builder.addBox(wx + 0.38f, 0.42f, wz + 0.45f, 0.48f, 0.28f, 0.26f, 0.22f, 0.58f, 0.85f)
                builder.addBox(wx + 0.38f, 0.42f, wz - 0.45f, 0.48f, 0.28f, 0.26f, 0.18f, 0.72f, 0.46f)
            }
            BuildingType.AIRPORT -> {
                // Runway with threshold markings + arched hangar terminal + tapered glass ATC tower
                builder.addBox(wx, 0.06f, wz + 0.44f, 1.92f, 0.06f, 0.80f, 0.18f, 0.20f, 0.24f)
                builder.addTopPlane(wx, 0.095f, wz + 0.44f, 1.65f, 0.05f, 0.96f, 0.96f, 0.96f)
                builder.addBox(wx - 0.34f, 0.34f, wz - 0.42f, 1.08f, 0.52f, 0.54f, 0.86f, 0.90f, 0.94f)
                builder.addGableRoof(wx - 0.34f, 0.60f, wz - 0.42f, 1.14f, 0.22f, 0.60f, 0.24f, 0.58f, 0.86f)
                builder.addCylinder(wx + 0.58f, 0.04f, wz - 0.45f, 0.14f, 1.60f, 10, 0.84f, 0.88f, 0.92f)
                builder.addCone(wx + 0.58f, 1.64f, wz - 0.45f, 0.30f, 0.26f, 10, 0.22f, 0.74f, 0.96f, emissive = 1.0f)
                builder.addCylinder(wx + 0.58f, 1.88f, wz - 0.45f, 0.26f, 0.12f, 10, 0.84f, 0.88f, 0.92f)
            }
            // World-Famous Landmarks
            BuildingType.LANDMARK_EIFFEL -> {
                // 4 Distinct corner arched legs + 1st & 2nd observation decks + lattice spire
                val legColorR = 0.64f; val legColorG = 0.48f; val legColorB = 0.34f
                for (dx in listOf(-0.48f, 0.48f)) {
                    for (dz in listOf(-0.48f, 0.48f)) {
                        builder.addRotatedTrapezoid(wx + dx * 0.75f, 0.04f, wz + dz * 0.75f, 0.34f, 0.18f, 1.32f, 0.34f, 0.18f, 0f, legColorR, legColorG, legColorB)
                    }
                }
                builder.addBox(wx, 1.36f, wz, 0.96f, 0.16f, 0.96f, 0.74f, 0.58f, 0.40f)
                builder.addBox(wx, 2.38f, wz, 0.58f, 0.14f, 0.58f, 0.74f, 0.58f, 0.40f)
                builder.addPyramid(wx, 1.44f, wz, 0.66f, 3.00f, 0.66f, legColorR, legColorG, legColorB)
                builder.addBox(wx, 4.46f, wz, 0.12f, 0.22f, 0.12f, 1.0f, 0.86f, 0.32f, emissive = 2.0f)
            }
            BuildingType.LANDMARK_BURJ_KHALIFA -> {
                // Authentic 3-wing Y-plan spiralling setbacks + towering needle spire
                for (tier in 0..4) {
                    val h = 1.15f - tier * 0.10f
                    val yBase = 0.04f + tier * 0.98f
                    val armLen = 0.68f - tier * 0.10f
                    val armWid = 0.34f - tier * 0.04f
                    builder.addCylinder(wx, yBase, wz, armWid * 0.85f, h, 8, 0.78f, 0.88f, 0.96f)
                    for (w in 0..2) {
                        val a = (w * 2.0 * Math.PI / 3.0).toFloat()
                        builder.addRotatedBox(wx + kotlin.math.cos(a) * armLen * 0.45f, yBase + h * 0.5f, wz + kotlin.math.sin(a) * armLen * 0.45f, armLen, h, armWid, a, 0.74f, 0.85f, 0.94f)
                    }
                }
                builder.addCylinder(wx, 4.95f, wz, 0.05f, 1.45f, 6, 0.94f, 0.96f, 1.0f, emissive = 1.4f)
            }
            BuildingType.LANDMARK_STATUE_LIBERTY -> {
                builder.addBox(wx, 0.28f, wz, 1.42f, 0.48f, 1.42f, 0.74f, 0.70f, 0.64f)
                builder.addBox(wx, 1.02f, wz, 0.88f, 1.00f, 0.88f, 0.84f, 0.80f, 0.72f)
                builder.addCylinder(wx, 1.52f, wz, 0.28f, 1.50f, 10, 0.32f, 0.78f, 0.68f)
                builder.addCylinder(wx, 3.02f, wz, 0.18f, 0.32f, 10, 0.34f, 0.82f, 0.72f)
                builder.addBox(wx + 0.26f, 3.36f, wz, 0.14f, 0.58f, 0.14f, 0.32f, 0.78f, 0.68f)
                builder.addCone(wx + 0.26f, 3.68f, wz, 0.12f, 0.28f, 8, 1.0f, 0.78f, 0.20f, emissive = 2.0f)
            }
            BuildingType.LANDMARK_BIG_BEN -> {
                builder.addBox(wx, 1.40f, wz, 0.88f, 2.72f, 0.88f, 0.84f, 0.76f, 0.58f)
                for (cx in listOf(-0.42f, 0.42f)) {
                    for (cz in listOf(-0.42f, 0.42f)) {
                        builder.addBox(wx + cx, 1.45f, wz + cz, 0.12f, 2.82f, 0.12f, 0.74f, 0.66f, 0.50f)
                    }
                }
                builder.addBox(wx, 2.94f, wz, 0.98f, 0.66f, 0.98f, 0.98f, 0.95f, 0.84f, emissive = 1.4f)
                builder.addPyramid(wx, 3.27f, wz, 0.94f, 1.18f, 0.94f, 0.26f, 0.34f, 0.42f)
            }
            BuildingType.LANDMARK_COLOSSEUM -> {
                // Hollow elliptical/circular outer colonnade + inner tiered stone bowl
                builder.addHorizontalRing(wx, 0.04f, wz, 0.76f, 0.18f, 1.05f, 16, 0.88f, 0.80f, 0.66f)
                builder.addHorizontalRing(wx, 0.04f, wz, 0.54f, 0.16f, 0.62f, 14, 0.80f, 0.72f, 0.58f)
                builder.addCylinder(wx, 0.04f, wz, 0.42f, 0.08f, 12, 0.74f, 0.62f, 0.46f)
            }
            BuildingType.LANDMARK_PYRAMID -> {
                // Stepped limestone pyramid + golden capstone
                builder.addPyramid(wx, 0.04f, wz, 1.80f, 1.85f, 1.80f, 0.92f, 0.78f, 0.48f)
                builder.addPyramid(wx, 1.56f, wz, 0.42f, 0.44f, 0.42f, 0.98f, 0.86f, 0.34f, emissive = 0.8f)
            }
            BuildingType.COSMETIC_TREE_GARDEN -> {
                addTree(wx - 0.42f, wz - 0.42f, 0.78f, season, builder)
                addTree(wx + 0.42f, wz + 0.42f, 0.78f, season, builder)
                builder.addCylinder(wx, 0.04f, wz, 0.28f, 0.10f, 8, 0.94f, 0.52f, 0.68f)
            }
            BuildingType.COSMETIC_PARK_BENCH -> {
                builder.addBox(wx - 0.35f, 0.18f, wz, 0.52f, 0.16f, 0.24f, 0.62f, 0.40f, 0.22f)
                builder.addBox(wx + 0.35f, 0.18f, wz, 0.52f, 0.16f, 0.24f, 0.62f, 0.40f, 0.22f)
                addStreetLamp(wx, wz - 0.45f, builder = builder)
            }
            BuildingType.COSMETIC_FOUNTAIN -> {
                builder.addCylinder(wx, 0.04f, wz, 0.74f, 0.24f, 14, 0.88f, 0.90f, 0.92f)
                builder.addCylinder(wx, 0.28f, wz, 0.38f, 0.22f, 12, 0.20f, 0.76f, 0.98f, emissive = 0.8f)
                builder.addCone(wx, 0.50f, wz, 0.16f, 0.52f, 10, 0.62f, 0.92f, 1.0f, emissive = 0.9f)
            }
            BuildingType.COSMETIC_ARCH -> {
                builder.addBox(wx - 0.48f, 0.62f, wz, 0.34f, 1.18f, 0.48f, 0.92f, 0.88f, 0.80f)
                builder.addBox(wx + 0.48f, 0.62f, wz, 0.34f, 1.18f, 0.48f, 0.92f, 0.88f, 0.80f)
                builder.addBox(wx, 1.32f, wz, 1.38f, 0.38f, 0.54f, 0.95f, 0.91f, 0.83f)
            }
            else -> {
                builder.addBox(wx, 0.45f, wz, 1.2f, 0.8f, 1.2f, 0.7f, 0.7f, 0.75f)
            }
        }
    }

    private fun addWindowBands(
        wx: Float, startY: Float, wz: Float,
        sx: Float, sz: Float, height: Float,
        bands: Int, builder: MeshBuilder
    ) {
        val count = bands.coerceIn(1, 12)
        val step = (height - 0.25f) / count
        for (i in 0 until count) {
            val y = startY + i * step
            builder.addBox(wx, y, wz, sx, 0.10f, sz, 1.0f, 0.90f, 0.52f, emissive = 1.0f)
        }
    }
}
