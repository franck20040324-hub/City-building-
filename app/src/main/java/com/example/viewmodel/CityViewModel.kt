package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.MusicPlayerManager
import com.example.audio.SoundFxEngine
import com.example.model.AppLanguage
import com.example.model.AudioTrackItem
import com.example.model.BuildCategory
import com.example.model.BuildingType
import com.example.model.CitySettings
import com.example.model.CraftRecipe
import com.example.model.GameState
import com.example.model.GridCell
import com.example.model.MissionGoalType
import com.example.model.MissionItem
import com.example.model.MissionPeriod
import com.example.model.PlacedBuilding
import com.example.model.ResidentEvent
import com.example.model.Season
import com.example.model.TaxPolicy
import com.example.model.TerrainType
import com.example.model.ToolMode
import com.example.model.TrafficDensity
import com.example.model.WeatherCondition
import com.example.persistence.LocalSaveManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class CityViewModel(application: Application) : AndroidViewModel(application) {

    private val saveManager = LocalSaveManager(application)
    private val sfx = SoundFxEngine()
    private val musicPlayer = MusicPlayerManager(application) { idx, playing ->
        _uiState.update { it.copy(currentTrackIndex = idx, isMusicPlaying = playing) }
    }

    private val _uiState = MutableStateFlow(GameState())
    val uiState: StateFlow<GameState> = _uiState.asStateFlow()

    private val saveMutex = Mutex()
    private var tickCounter = 0

    init {
        initializeCityFromStorageOrStarter()
        startSimulationLoop()
    }

    private fun initializeCityFromStorageOrStarter() {
        val loaded = saveManager.searchAndLoadAutoSave()
        if (loaded != null) {
            val recomputed = recomputeCityStats(loaded.state)
            val isAr = recomputed.settings.language == AppLanguage.ARABIC
            _uiState.value = recomputed.copy(
                statusBanner = if (isAr) "تم العثور على ملف الحفظ واستئناف اللعب تلقائياً" else "Auto-save found — city progress resumed"
            )
            sfx.volume = if (recomputed.settings.sfxEnabled) recomputed.settings.soundVolume else 0f
            musicPlayer.setVolume(recomputed.settings.soundVolume)
            musicPlayer.setPlaylist(recomputed.playlist)
        } else {
            val savedPlaylist = saveManager.loadSavedPlaylist()
            val fresh = recomputeCityStats(createStarterCity(savedPlaylist))
            _uiState.value = fresh
            sfx.volume = if (fresh.settings.sfxEnabled) fresh.settings.soundVolume else 0f
            musicPlayer.setVolume(fresh.settings.soundVolume)
            musicPlayer.setPlaylist(fresh.playlist)
            saveNowAsync()
        }
    }

    private fun createStarterCity(savedPlaylist: List<AudioTrackItem>): GameState {
        val size = 22
        val cells = ArrayList<GridCell>(size * size)
        val center = size / 2

        for (x in 0 until size) {
            for (z in 0 until size) {
                val terrain = when {
                    // Scenic small river/pond on south-east corner
                    (x in 17..20 && z in 15..18) -> TerrainType.WATER
                    // Fertile agricultural plain on west side
                    (x in 1..6 && z in 4..14) -> TerrainType.FERTILE_PLAIN
                    // Natural forest groves on north-east
                    (x in 15..20 && z in 1..5) -> TerrainType.FOREST_LAND
                    else -> TerrainType.GRASS
                }

                // Starter avenue connecting outer highway north-south & east-west + starter buildings
                val placed: PlacedBuilding? = when {
                    x == center && z in 0 until size -> PlacedBuilding(
                        id = "road_${x}_$z",
                        type = BuildingType.ROAD_FOUR_LANE
                    )
                    z == center && x in 5..16 -> PlacedBuilding(
                        id = "road_${x}_$z",
                        type = BuildingType.ROAD_TWO_WAY
                    )
                    x == center - 1 && z == center - 1 -> PlacedBuilding(
                        id = "starter_house_1",
                        type = BuildingType.GROUND_HOUSE
                    )
                    x == center + 1 && z == center - 1 -> PlacedBuilding(
                        id = "starter_shop_1",
                        type = BuildingType.SHOP
                    )
                    x == center - 2 && z == center - 1 -> PlacedBuilding(
                        id = "starter_house_2",
                        type = BuildingType.SMALL_APARTMENT
                    )
                    x == 4 && z == 9 -> PlacedBuilding(
                        id = "starter_farm_1",
                        type = BuildingType.CROP_FIELD
                    )
                    x == 16 && z == 16 -> PlacedBuilding(
                        id = "starter_port_1",
                        type = BuildingType.WATER_PLANT
                    )
                    else -> null
                }

                cells.add(GridCell(x = x, z = z, terrain = terrain, building = placed))
            }
        }

        return GameState(
            cityName = "Skyline Harbor",
            gridSize = size,
            cells = cells,
            coins = 4500L,
            gems = 35,
            xp = 0L,
            level = 1,
            happiness = 94,
            missions = createDefaultMissions(),
            playlist = savedPlaylist,
            autoSavePathLabel = saveManager.getPrimarySavePath()
        )
    }

    private fun createDefaultMissions(): List<MissionItem> = listOf(
        MissionItem(
            id = "d_build_homes",
            period = MissionPeriod.DAILY,
            goalType = MissionGoalType.BUILD_RESIDENTIAL,
            titleEn = "Build 2 Residential Homes",
            titleAr = "بناء منزلين سكنيين",
            targetCount = 2,
            rewardCoins = 350,
            rewardGems = 5,
            rewardXp = 60
        ),
        MissionItem(
            id = "d_pave_roads",
            period = MissionPeriod.DAILY,
            goalType = MissionGoalType.BUILD_ROAD,
            titleEn = "Pave 4 Road Tiles",
            titleAr = "رصف ٤ قطع طرق",
            targetCount = 4,
            rewardCoins = 260,
            rewardGems = 4,
            rewardXp = 45
        ),
        MissionItem(
            id = "d_craft_goods",
            period = MissionPeriod.DAILY,
            goalType = MissionGoalType.CRAFT_GOODS,
            titleEn = "Craft 1 Factory Good",
            titleAr = "تصنيع منتج واحد في المصنع",
            targetCount = 1,
            rewardCoins = 320,
            rewardGems = 6,
            rewardXp = 55
        ),
        MissionItem(
            id = "w_upgrade",
            period = MissionPeriod.WEEKLY,
            goalType = MissionGoalType.UPGRADE_BUILDING,
            titleEn = "Upgrade 2 City Buildings",
            titleAr = "ترقية مبنيين في المدينة",
            targetCount = 2,
            rewardCoins = 950,
            rewardGems = 14,
            rewardXp = 160
        ),
        MissionItem(
            id = "w_taxes",
            period = MissionPeriod.WEEKLY,
            goalType = MissionGoalType.COLLECT_TAXES,
            titleEn = "Collect City Taxes 5 Times",
            titleAr = "تحصيل ضرائب المدينة ٥ مرات",
            targetCount = 5,
            rewardCoins = 800,
            rewardGems = 12,
            rewardXp = 140
        ),
        MissionItem(
            id = "w_terraform",
            period = MissionPeriod.WEEKLY,
            goalType = MissionGoalType.PLANT_OR_TERRAFORM,
            titleEn = "Shape Land, Water, or Trees (3x)",
            titleAr = "تعديل التضاريس أو زرع أشجار (٣ مرات)",
            targetCount = 3,
            rewardCoins = 700,
            rewardGems = 10,
            rewardXp = 120
        )
    )

    private fun startSimulationLoop() {
        viewModelScope.launch {
            var wallSeconds = 0
            while (true) {
                delay(1000L)
                wallSeconds++
                val speed = _uiState.value.settings.simulationSpeed.coerceIn(0, 3)
                repeat(speed) {
                    tickCounter++
                    processOneSecondTick()
                }
                // Auto-save every 5 seconds locally
                if (wallSeconds % 5 == 0) {
                    saveNowAsync()
                }
            }
        }
    }

    private fun processOneSecondTick() {
        var playChime = false
        var playCoin = false

        _uiState.update { state ->
            var changed = false
            var coins = state.coins
            var xp = state.xp
            var level = state.level
            val inv = state.storageInventory.toMutableMap()
            var missions = state.missions
            var craftedDoneCount = 0

            val updatedCells = state.cells.map { cell ->
                val b = cell.building ?: return@map cell
                var nb = b
                var cellChanged = false

                // 1. Construction Timer
                if (b.isConstructing && b.constructionRemainingSec > 0) {
                    val rem = b.constructionRemainingSec - 1
                    cellChanged = true
                    if (rem <= 0) {
                        nb = nb.copy(isConstructing = false, constructionRemainingSec = 0)
                        xp += b.type.xpReward
                        playChime = true
                    } else {
                        nb = nb.copy(constructionRemainingSec = rem)
                    }
                }

                // 2. Upgrade Timer
                if (nb.isUpgrading && nb.upgradeRemainingSec > 0) {
                    val rem = nb.upgradeRemainingSec - 1
                    cellChanged = true
                    if (rem <= 0) {
                        val nextLvl = (nb.level + 1).coerceAtMost(nb.type.maxLevel)
                        nb = nb.copy(
                            isUpgrading = false,
                            upgradeRemainingSec = 0,
                            upgradePaidCoins = 0,
                            level = nextLvl
                        )
                        xp += (nb.type.xpReward / 2).coerceAtLeast(15)
                        playChime = true
                    } else {
                        nb = nb.copy(upgradeRemainingSec = rem)
                    }
                }

                // 3. Factory Crafting Timer
                val recipe = nb.craftingRecipe
                if (recipe != null && nb.craftRemainingSec > 0) {
                    val rem = nb.craftRemainingSec - 1
                    cellChanged = true
                    if (rem <= 0) {
                        inv[recipe] = (inv[recipe] ?: 0) + 1
                        nb = nb.copy(craftingRecipe = null, craftRemainingSec = 0, craftTotalSec = 0)
                        xp += recipe.xpReward
                        craftedDoneCount++
                        playChime = true
                    } else {
                        nb = nb.copy(craftRemainingSec = rem)
                    }
                }

                if (cellChanged) {
                    changed = true
                    cell.copy(building = nb)
                } else cell
            }

            if (craftedDoneCount > 0) {
                missions = advanceMission(missions, MissionGoalType.CRAFT_GOODS, craftedDoneCount)
            }

            // Automatic Tax Collection every 15 simulation seconds!
            val taxTick = (tickCounter % 15 == 0)
            if (taxTick) {
                val cycleTax = computeTaxPerCycle(updatedCells, state.happiness, state.settings.taxPolicy)
                if (cycleTax > 0) {
                    coins += cycleTax
                    xp += 5L
                    playCoin = true
                    missions = advanceMission(missions, MissionGoalType.COLLECT_TAXES, 1)
                }
            }

            // Level Up check
            var lvlThreshold = (level * 150L) + 100L
            while (xp >= lvlThreshold) {
                xp -= lvlThreshold
                level++
                coins += level * 150L
                lvlThreshold = (level * 150L) + 100L
            }

            // Day / Night & Seasons progression
            val nextTod = if (state.settings.autoDayNightCycle) {
                (state.timeOfDay + 0.0042f) % 1.0f // ~4 minutes full day-night cycle
            } else state.timeOfDay

            val nextSeason = if (tickCounter % 180 == 0) state.season.next() else state.season

            val weatherUpdate = tickCounter % 45 == 0 || nextSeason != state.season
            val nextWeather = if (weatherUpdate) {
                pickWeatherForSeason(nextSeason, tickCounter)
            } else state.weather
            val nextTempC = if (weatherUpdate || tickCounter % 15 == 0) {
                computeWeatherTemp(nextSeason, nextWeather, nextTod)
            } else state.weatherTempC
            val nextHumidityPct = if (weatherUpdate) {
                computeWeatherHumidity(nextWeather, nextSeason, tickCounter)
            } else state.weatherHumidityPct

            // Periodic Resident Event every ~75 seconds if none active
            val nextEvent = if (state.activeResidentEvent == null && tickCounter % 75 == 25) {
                pickRandomResidentEvent()
            } else state.activeResidentEvent

            val nextState = state.copy(
                cells = if (changed) updatedCells else state.cells,
                coins = coins,
                xp = xp,
                level = level,
                storageInventory = inv,
                missions = missions,
                timeOfDay = nextTod,
                season = nextSeason,
                weather = nextWeather,
                weatherTempC = nextTempC,
                weatherHumidityPct = nextHumidityPct,
                activeResidentEvent = nextEvent
            )
            if (changed || taxTick) recomputeCityStats(nextState) else nextState
        }

        if (playChime) sfx.playBuildChime() else if (playCoin) sfx.playCoinChime()
    }

    private fun computeTaxPerCycle(cells: List<GridCell>, happiness: Int, taxPolicy: TaxPolicy): Int {
        val raw = cells.sumOf { it.building?.currentTaxIncome() ?: 0 }
        val bonus = 0.75f + (happiness / 200f)
        return (raw * bonus * 0.25f * taxPolicy.multiplier).toInt().coerceAtLeast(if (raw > 0) 8 else 0)
    }

    private fun recomputeCityStats(state: GameState): GameState {
        var pop = 0
        var civicBonus = 0
        var pollutionCount = 0

        for (c in state.cells) {
            val b = c.building ?: continue
            pop += b.currentPopulation()
            if (!b.isConstructing) {
                when (b.type) {
                    BuildingType.SCHOOL,
                    BuildingType.HOSPITAL,
                    BuildingType.POLICE_STATION,
                    BuildingType.FIRE_STATION,
                    BuildingType.WATER_PLANT,
                    BuildingType.AMUSEMENT_PARK,
                    BuildingType.WATER_AMUSEMENT_PARK,
                    BuildingType.FOREST_GROVE,
                    BuildingType.COSMETIC_FOUNTAIN,
                    BuildingType.COSMETIC_TREE_GARDEN,
                    BuildingType.LANDMARK_EIFFEL,
                    BuildingType.LANDMARK_BURJ_KHALIFA,
                    BuildingType.LANDMARK_STATUE_LIBERTY,
                    BuildingType.LANDMARK_BIG_BEN,
                    BuildingType.LANDMARK_COLOSSEUM,
                    BuildingType.LANDMARK_PYRAMID -> civicBonus += 3
                    BuildingType.FOOD_FACTORY,
                    BuildingType.EQUIPMENT_FACTORY,
                    BuildingType.TRANSPORT_FACTORY -> pollutionCount += 2
                    BuildingType.RECYCLING_FACTORY -> civicBonus += 2
                    else -> {}
                }
            }
        }
        val happiness = (85 + civicBonus - pollutionCount + state.settings.taxPolicy.happinessDelta).coerceIn(50, 100)
        val taxPerMin = computeTaxPerCycle(state.cells, happiness, state.settings.taxPolicy) * 4
        return state.copy(population = pop, happiness = happiness, taxPerMinute = taxPerMin)
    }

    fun selectCategory(category: BuildCategory) {
        _uiState.update { it.copy(activeCategory = category) }
    }

    fun startPlacingBuilding(type: BuildingType) {
        _uiState.update {
            it.copy(
                toolMode = ToolMode.BUILD,
                selectedBuildType = type,
                selectedCellCoords = null
            )
        }
    }

    fun setToolMode(mode: ToolMode) {
        _uiState.update {
            it.copy(
                toolMode = mode,
                selectedBuildType = if (mode == ToolMode.BUILD) it.selectedBuildType else null
            )
        }
    }

    fun cancelPlacementMode() {
        _uiState.update {
            it.copy(toolMode = ToolMode.INSPECT, selectedBuildType = null)
        }
    }

    fun onTileTapped(gx: Int, gz: Int) {
        val state = _uiState.value
        val idx = state.cells.indexOfFirst { it.x == gx && it.z == gz }
        if (idx < 0) return
        val cell = state.cells[idx]

        when (state.toolMode) {
            ToolMode.BUILD -> {
                val type = state.selectedBuildType ?: return
                placeBuildingAt(idx, cell, type)
            }
            ToolMode.UPGRADE_ROAD -> {
                upgradeRoadAt(cell)
            }
            ToolMode.LOWER_GROUND -> {
                lowerGroundToWater(idx, cell)
            }
            ToolMode.RAISE_GROUND -> {
                raiseWaterToLand(idx, cell)
            }
            ToolMode.CANCEL_REFUND -> {
                cancelOrDemolishAt(idx, cell)
            }
            ToolMode.INSPECT -> {
                _uiState.update { it.copy(selectedCellCoords = gx to gz) }
            }
        }
    }

    private fun upgradeRoadAt(cell: GridCell) {
        val state = _uiState.value
        val isAr = state.settings.language == AppLanguage.ARABIC
        val b = cell.building
        if (b == null || !b.canUpgradeRoad()) {
            setBanner(if (isAr) "اختر طريق اتجاهين (١+١) لترقيته إلى مزدوج" else "Tap a 2-Way (1+1) road tile to upgrade to Double-Way (2+2)")
            return
        }
        upgradeRoadTiles(listOf(cell))
    }

    fun upgradeAllCityTwoWayRoads() {
        val targets = _uiState.value.cells.filter { it.building?.canUpgradeRoad() == true }
        if (targets.isEmpty()) {
            val isAr = _uiState.value.settings.language == AppLanguage.ARABIC
            setBanner(if (isAr) "كل طرق المدينة مرقّاة بالفعل" else "All city 2-way roads are already upgraded")
            return
        }
        upgradeRoadTiles(targets)
    }

    fun rotateSelectedBuilding() {
        val state = _uiState.value
        val coords = state.selectedCellCoords ?: return
        val idx = state.cells.indexOfFirst { it.x == coords.first && it.z == coords.second }
        if (idx < 0) return
        val cell = state.cells[idx]
        val b = cell.building ?: return
        if (b.type.isRoad) return
        val updated = state.cells.toMutableList()
        updated[idx] = cell.copy(building = b.copy(rotation = (b.rotation + 1) % 4))
        _uiState.update { it.copy(cells = updated) }
        sfx.playBuildChime()
        saveNowAsync()
    }

    private fun placeBuildingAt(idx: Int, cell: GridCell, type: BuildingType) {
        val state = _uiState.value
        val isAr = state.settings.language == AppLanguage.ARABIC

        if (cell.building != null) {
            if (cell.building.type.upgradedRoadTarget() == type) {
                upgradeRoadTiles(listOf(cell))
                return
            }
            setBanner(if (isAr) "هذه القطعة مشغولة بالفعل" else "This tile already has a structure")
            return
        }
        if (type.isWaterRequired && cell.terrain != TerrainType.WATER) {
            setBanner(if (isAr) "يجب وضع الميناء على الماء" else "Seaport must be placed on water")
            return
        }
        if (!type.isWaterRequired && cell.terrain == TerrainType.WATER) {
            setBanner(if (isAr) "ارفع الأرض أولاً للبناء هنا" else "Raise ground to land before building here")
            return
        }
        if (state.coins < type.coinCost || state.gems < type.gemCost) {
            setBanner(if (isAr) "رصيدك غير كافٍ" else "Not enough coins or gems")
            return
        }

        val buildSec = type.buildTimeSec
        val newBuilding = PlacedBuilding(
            id = "b_${UUID.randomUUID().toString().take(8)}",
            type = type,
            level = 1,
            isConstructing = buildSec > 0,
            constructionRemainingSec = buildSec,
            constructionTotalSec = buildSec
        )

        val updatedCells = state.cells.toMutableList()
        updatedCells[idx] = cell.copy(building = newBuilding)

        var missions = advanceMission(state.missions, MissionGoalType.BUILD_ANY, 1)
        if (type.category == BuildCategory.RESIDENTIAL) {
            missions = advanceMission(missions, MissionGoalType.BUILD_RESIDENTIAL, 1)
        } else if (type.isRoad) {
            missions = advanceMission(missions, MissionGoalType.BUILD_ROAD, 1)
        } else if (type == BuildingType.FOREST_GROVE || type == BuildingType.COSMETIC_TREE_GARDEN) {
            missions = advanceMission(missions, MissionGoalType.PLANT_OR_TERRAFORM, 1)
        }

        val xpGain = if (buildSec == 0) type.xpReward else 5
        _uiState.update { s ->
            recomputeCityStats(
                s.copy(
                    cells = updatedCells,
                    coins = s.coins - type.coinCost,
                    gems = s.gems - type.gemCost,
                    xp = s.xp + xpGain,
                    missions = missions,
                    selectedCellCoords = cell.x to cell.z
                )
            )
        }
        sfx.playBuildChime()
        saveNowAsync()
    }

    private fun lowerGroundToWater(idx: Int, cell: GridCell) {
        val state = _uiState.value
        val isAr = state.settings.language == AppLanguage.ARABIC
        if (cell.building != null) {
            setBanner(if (isAr) "أزل المبنى أولاً قبل خفض الأرض" else "Remove structure before lowering ground")
            return
        }
        if (cell.terrain == TerrainType.WATER) return

        val updated = state.cells.toMutableList()
        updated[idx] = cell.copy(terrain = TerrainType.WATER, elevation = -0.35f)
        val missions = advanceMission(state.missions, MissionGoalType.PLANT_OR_TERRAFORM, 1)
        _uiState.update {
            recomputeCityStats(it.copy(cells = updated, xp = it.xp + 8, missions = missions))
        }
        sfx.playWaterSplash()
        saveNowAsync()
    }

    private fun raiseWaterToLand(idx: Int, cell: GridCell) {
        val state = _uiState.value
        if (cell.terrain != TerrainType.WATER) return

        val updated = state.cells.toMutableList()
        updated[idx] = cell.copy(
            terrain = TerrainType.GRASS,
            elevation = 0f,
            building = if (cell.building?.type?.isWaterRequired == true) null else cell.building
        )
        val missions = advanceMission(state.missions, MissionGoalType.PLANT_OR_TERRAFORM, 1)
        _uiState.update {
            recomputeCityStats(it.copy(cells = updated, xp = it.xp + 8, missions = missions))
        }
        sfx.playBuildChime()
        saveNowAsync()
    }

    /**
     * Cancel construction / remove building or road and recover 100% of the purchase money & gems!
     */
    fun cancelOrDemolishAt(idx: Int, cell: GridCell) {
        val b = cell.building ?: return
        val state = _uiState.value
        val isAr = state.settings.language == AppLanguage.ARABIC
        val refundCoins = b.type.coinCost + b.upgradePaidCoins
        val refundGems = b.type.gemCost

        val updated = state.cells.toMutableList()
        updated[idx] = cell.copy(building = null)
        _uiState.update {
            recomputeCityStats(
                it.copy(
                    cells = updated,
                    coins = it.coins + refundCoins,
                    gems = it.gems + refundGems,
                    selectedCellCoords = null,
                    statusBanner = if (isAr) "تم الإلغاء واسترداد $refundCoins عملة" else "Cancelled & refunded $refundCoins coins"
                )
            )
        }
        sfx.playRefundTone()
        saveNowAsync()
    }

    fun cancelSelectedBuilding() {
        val state = _uiState.value
        val coords = state.selectedCellCoords ?: return
        val idx = state.cells.indexOfFirst { it.x == coords.first && it.z == coords.second }
        if (idx >= 0) cancelOrDemolishAt(idx, state.cells[idx])
    }

    fun startUpgradeSelected() {
        val state = _uiState.value
        val isAr = state.settings.language == AppLanguage.ARABIC
        val coords = state.selectedCellCoords ?: return
        val idx = state.cells.indexOfFirst { it.x == coords.first && it.z == coords.second }
        if (idx < 0) return
        val cell = state.cells[idx]
        val b = cell.building ?: return

        if (b.canUpgradeRoad()) {
            upgradeSelectedRoad(upgradeConnected = false)
            return
        }

        if (b.isConstructing || b.isUpgrading || b.level >= b.type.maxLevel) return
        val cost = b.nextUpgradeCost()
        if (state.coins < cost) {
            setBanner(if (isAr) "رصيد غير كافٍ للترقية ($cost)" else "Need $cost coins to upgrade")
            return
        }
        val duration = b.nextUpgradeDurationSec()
        val updated = state.cells.toMutableList()
        updated[idx] = cell.copy(
            building = b.copy(
                isUpgrading = true,
                upgradeRemainingSec = duration,
                upgradeTotalSec = duration,
                upgradePaidCoins = cost
            )
        )
        val missions = advanceMission(state.missions, MissionGoalType.UPGRADE_BUILDING, 1)
        _uiState.update {
            it.copy(
                cells = updated,
                coins = it.coins - cost,
                missions = missions
            )
        }
        sfx.playUpgradeFanfare()
        saveNowAsync()
    }

    fun upgradeSelectedRoad(upgradeConnected: Boolean = false) {
        val state = _uiState.value
        val coords = state.selectedCellCoords ?: return
        val startCell = state.cells.find { it.x == coords.first && it.z == coords.second } ?: return
        val startB = startCell.building ?: return
        if (!startB.canUpgradeRoad()) return

        if (!upgradeConnected) {
            upgradeRoadTiles(listOf(startCell))
            return
        }

        // BFS across connected ROAD_TWO_WAY tiles starting from selected tile
        val cellMap = state.cells.associateBy { it.x to it.z }
        val visited = HashSet<Pair<Int, Int>>()
        val queue = ArrayDeque<GridCell>()
        val targets = mutableListOf<GridCell>()
        queue.add(startCell)
        visited.add(startCell.x to startCell.z)

        while (queue.isNotEmpty()) {
            val cur = queue.removeFirst()
            targets.add(cur)
            for ((dx, dz) in listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)) {
                val nx = cur.x + dx
                val nz = cur.z + dz
                val key = nx to nz
                if (key !in visited) {
                    val nb = cellMap[key]
                    if (nb?.building?.type == BuildingType.ROAD_TWO_WAY) {
                        visited.add(key)
                        queue.add(nb)
                    }
                }
            }
        }
        upgradeRoadTiles(targets)
    }

    private fun upgradeRoadTiles(targets: List<GridCell>) {
        if (targets.isEmpty()) return
        val state = _uiState.value
        val isAr = state.settings.language == AppLanguage.ARABIC

        var coins = state.coins
        val updated = state.cells.toMutableList()
        val indexMap = HashMap<Pair<Int, Int>, Int>(updated.size * 2)
        updated.forEachIndexed { idx, c -> indexMap[c.x to c.z] = idx }

        var upgradedCount = 0
        var totalCost = 0L
        for (cell in targets) {
            val b = cell.building ?: continue
            val targetType = b.type.upgradedRoadTarget() ?: continue
            val cost = b.roadUpgradeCost()
            if (coins < cost) break
            coins -= cost
            totalCost += cost
            upgradedCount++
            val idx = indexMap[cell.x to cell.z] ?: continue
            updated[idx] = cell.copy(building = b.copy(type = targetType, level = 1))
        }

        if (upgradedCount == 0) {
            val needed = targets.firstOrNull()?.building?.roadUpgradeCost() ?: 35
            setBanner(if (isAr) "رصيد غير كافٍ لترقية الطريق ($needed)" else "Need $needed coins to upgrade road")
            return
        }

        var missions = advanceMission(state.missions, MissionGoalType.UPGRADE_BUILDING, upgradedCount)
        missions = advanceMission(missions, MissionGoalType.BUILD_ROAD, upgradedCount)
        val banner = if (isAr) {
            "تمت ترقية $upgradedCount طريق إلى طريق مزدوج (٢+٢ حارة)"
        } else {
            "Upgraded $upgradedCount road${if (upgradedCount > 1) "s" else ""} to Double-Way (2+2 lanes)"
        }

        _uiState.update { s ->
            recomputeCityStats(
                s.copy(
                    cells = updated,
                    coins = coins,
                    xp = s.xp + (upgradedCount * 10L),
                    missions = missions,
                    statusBanner = banner
                )
            )
        }
        sfx.playUpgradeFanfare()
        saveNowAsync()
    }

    /**
     * Cancel an ongoing upgrade and recover 100% of the upgrade cost!
     */
    fun cancelUpgradeSelected() {
        val state = _uiState.value
        val isAr = state.settings.language == AppLanguage.ARABIC
        val coords = state.selectedCellCoords ?: return
        val idx = state.cells.indexOfFirst { it.x == coords.first && it.z == coords.second }
        if (idx < 0) return
        val cell = state.cells[idx]
        val b = cell.building ?: return
        if (!b.isUpgrading) return

        val refund = b.upgradePaidCoins
        val updated = state.cells.toMutableList()
        updated[idx] = cell.copy(
            building = b.copy(
                isUpgrading = false,
                upgradeRemainingSec = 0,
                upgradeTotalSec = 0,
                upgradePaidCoins = 0
            )
        )
        _uiState.update {
            it.copy(
                cells = updated,
                coins = it.coins + refund,
                statusBanner = if (isAr) "تم إلغاء الترقية واسترداد $refund عملة" else "Upgrade cancelled & refunded $refund coins"
            )
        }
        sfx.playRefundTone()
        saveNowAsync()
    }

    fun startCraftingSelected(recipe: CraftRecipe) {
        val state = _uiState.value
        val isAr = state.settings.language == AppLanguage.ARABIC
        val coords = state.selectedCellCoords ?: return
        val idx = state.cells.indexOfFirst { it.x == coords.first && it.z == coords.second }
        if (idx < 0) return
        val cell = state.cells[idx]
        val b = cell.building ?: return
        if (b.isConstructing || b.craftingRecipe != null) return
        if (state.coins < recipe.inputCost) {
            setBanner(if (isAr) "تكلفة التصنيع ${recipe.inputCost}" else "Need ${recipe.inputCost} coins for materials")
            return
        }

        val updated = state.cells.toMutableList()
        updated[idx] = cell.copy(
            building = b.copy(
                craftingRecipe = recipe,
                craftRemainingSec = recipe.craftTimeSec,
                craftTotalSec = recipe.craftTimeSec
            )
        )
        _uiState.update {
            it.copy(cells = updated, coins = it.coins - recipe.inputCost)
        }
        sfx.playBuildChime()
        saveNowAsync()
    }

    fun sellStoredItem(recipe: CraftRecipe, sellAll: Boolean = false) {
        val state = _uiState.value
        val qty = state.storageInventory[recipe] ?: 0
        if (qty <= 0) return
        val count = if (sellAll) qty else 1
        val earned = recipe.sellValue * count
        val inv = state.storageInventory.toMutableMap()
        if (qty - count <= 0) inv.remove(recipe) else inv[recipe] = qty - count

        _uiState.update {
            it.copy(
                coins = it.coins + earned,
                xp = it.xp + (count * 5L),
                storageInventory = inv
            )
        }
        sfx.playCoinChime()
        saveNowAsync()
    }

    fun collectBonusCoinsAndGems() {
        _uiState.update { state ->
            val earnedCoins = (state.taxPerMinute / 2).coerceAtLeast(100)
            val missions = advanceMission(state.missions, MissionGoalType.COLLECT_TAXES, 1)
            state.copy(
                coins = state.coins + earnedCoins,
                xp = state.xp + 12,
                missions = missions
            )
        }
        sfx.playCoinChime()
        saveNowAsync()
    }

    fun setCustomCoinsAndGems(newCoins: Long, newGems: Int) {
        val safeCoins = newCoins.coerceIn(0L, 999_999_999L)
        val safeGems = newGems.coerceIn(0, 99_999_999)
        _uiState.update { state ->
            val isAr = state.settings.language == AppLanguage.ARABIC
            state.copy(
                coins = safeCoins,
                gems = safeGems,
                statusBanner = if (isAr) "تم تحديث النقود والجواهر بدون فقدان أي تقدم" else "Money & gems updated with all progress preserved"
            )
        }
        sfx.playCoinChime()
        saveNowAsync()
    }

    fun claimMission(missionId: String) {
        _uiState.update { state ->
            val m = state.missions.find { it.id == missionId } ?: return@update state
            if (m.claimed || m.progress < m.targetCount) return@update state

            val updated = state.missions.map {
                if (it.id == missionId) {
                    // Refresh completed daily/weekly mission with next easy cycle!
                    it.copy(progress = 0, targetCount = it.targetCount + 1, claimed = false)
                } else it
            }
            state.copy(
                coins = state.coins + m.rewardCoins,
                gems = state.gems + m.rewardGems,
                xp = state.xp + m.rewardXp,
                missions = updated
            )
        }
        sfx.playUpgradeFanfare()
        saveNowAsync()
    }

    fun resolveResidentEvent(chooseOptionA: Boolean) {
        _uiState.update { state ->
            val ev = state.activeResidentEvent ?: return@update state
            if (chooseOptionA) {
                if (state.coins < ev.optionACostCoins) return@update state
                state.copy(
                    coins = state.coins - ev.optionACostCoins + ev.optionARewardCoins,
                    gems = state.gems + ev.optionARewardGems,
                    xp = state.xp + ev.optionARewardXp,
                    happiness = (state.happiness + ev.optionARewardHappiness).coerceIn(50, 100),
                    activeResidentEvent = null
                )
            } else {
                state.copy(
                    coins = state.coins + ev.optionBRewardCoins,
                    xp = state.xp + ev.optionBRewardXp,
                    happiness = (state.happiness + ev.optionBRewardHappiness).coerceIn(50, 100),
                    activeResidentEvent = null
                )
            }
        }
        sfx.playUpgradeFanfare()
        saveNowAsync()
    }

    fun triggerResidentEventNow() {
        _uiState.update { it.copy(activeResidentEvent = pickRandomResidentEvent()) }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedCellCoords = null) }
    }

    fun clearBanner() {
        _uiState.update { it.copy(statusBanner = null) }
    }

    private fun setBanner(msg: String) {
        _uiState.update { it.copy(statusBanner = msg) }
    }

    // Settings Controls
    fun updateSoundVolume(vol: Float) {
        val v = vol.coerceIn(0f, 1f)
        sfx.volume = if (_uiState.value.settings.sfxEnabled) v else 0f
        musicPlayer.setVolume(v)
        _uiState.update { it.copy(settings = it.settings.copy(soundVolume = v)) }
        saveNowAsync()
    }

    fun updateSfxEnabled(enabled: Boolean) {
        sfx.volume = if (enabled) _uiState.value.settings.soundVolume else 0f
        _uiState.update { it.copy(settings = it.settings.copy(sfxEnabled = enabled)) }
        saveNowAsync()
    }

    fun updateBrightness(b: Float) {
        _uiState.update { it.copy(settings = it.settings.copy(brightness = b.coerceIn(0.35f, 1.25f))) }
        saveNowAsync()
    }

    fun updateLanguage(lang: AppLanguage) {
        _uiState.update { it.copy(settings = it.settings.copy(language = lang)) }
        saveNowAsync()
    }

    fun updateAutoDayNight(enabled: Boolean) {
        _uiState.update { it.copy(settings = it.settings.copy(autoDayNightCycle = enabled)) }
        saveNowAsync()
    }

    fun setSimulationSpeed(speed: Int) {
        _uiState.update { it.copy(settings = it.settings.copy(simulationSpeed = speed.coerceIn(0, 3))) }
        saveNowAsync()
    }

    fun cycleSimulationSpeed() {
        val cur = _uiState.value.settings.simulationSpeed
        val next = when (cur) {
            0 -> 1
            1 -> 2
            2 -> 3
            else -> 0
        }
        setSimulationSpeed(next)
    }

    fun updateTaxPolicy(policy: TaxPolicy) {
        _uiState.update { state ->
            recomputeCityStats(state.copy(settings = state.settings.copy(taxPolicy = policy)))
        }
        saveNowAsync()
    }

    fun updateTrafficDensity(density: TrafficDensity) {
        _uiState.update { it.copy(settings = it.settings.copy(trafficDensity = density)) }
        saveNowAsync()
    }

    fun updateBrightRoadLinesAlways(always: Boolean) {
        _uiState.update { it.copy(settings = it.settings.copy(brightRoadLinesAlways = always)) }
        saveNowAsync()
    }

    fun toggleWeatherShortMode() {
        _uiState.update {
            it.copy(settings = it.settings.copy(weatherShortMode = !it.settings.weatherShortMode))
        }
        saveNowAsync()
    }

    private fun pickWeatherForSeason(season: Season, tick: Int): WeatherCondition {
        val roll = kotlin.math.abs((tick * 31 + season.index * 17) % 10)
        return when (season) {
            Season.SUMMER -> when {
                roll < 6 -> WeatherCondition.SUNNY
                roll < 9 -> WeatherCondition.CLOUDY
                else -> WeatherCondition.RAINY
            }
            Season.SPRING -> when {
                roll < 5 -> WeatherCondition.SUNNY
                roll < 8 -> WeatherCondition.CLOUDY
                else -> WeatherCondition.RAINY
            }
            Season.AUTUMN -> when {
                roll < 3 -> WeatherCondition.SUNNY
                roll < 7 -> WeatherCondition.CLOUDY
                else -> WeatherCondition.RAINY
            }
            Season.WINTER -> when {
                roll < 2 -> WeatherCondition.SUNNY
                roll < 6 -> WeatherCondition.CLOUDY
                else -> WeatherCondition.RAINY
            }
        }
    }

    private fun computeWeatherTemp(season: Season, weather: WeatherCondition, timeOfDay: Float): Int {
        val seasonBase = when (season) {
            Season.SPRING -> 22
            Season.SUMMER -> 31
            Season.AUTUMN -> 17
            Season.WINTER -> 8
        }
        val weatherOffset = when (weather) {
            WeatherCondition.SUNNY -> 3
            WeatherCondition.CLOUDY -> -1
            WeatherCondition.RAINY -> -4
        }
        // Peak warmth near noon (0.5), coolest near midnight (0.0/1.0)
        val diurnal = (-kotlin.math.cos(timeOfDay * 2.0 * kotlin.math.PI) * 3.0).toInt()
        return (seasonBase + weatherOffset + diurnal).coerceIn(-5, 44)
    }

    private fun computeWeatherHumidity(weather: WeatherCondition, season: Season, tick: Int): Int {
        val base = when (weather) {
            WeatherCondition.SUNNY -> 34
            WeatherCondition.CLOUDY -> 58
            WeatherCondition.RAINY -> 84
        }
        val seasonAdj = if (season == Season.SUMMER) -5 else if (season == Season.WINTER) 6 else 0
        val jitter = ((tick / 45) % 9) - 4
        return (base + seasonAdj + jitter).coerceIn(18, 96)
    }

    fun startNewCity() {
        val cur = _uiState.value
        val fresh = recomputeCityStats(
            createStarterCity(cur.playlist).copy(
                settings = cur.settings,
                statusBanner = if (cur.settings.language == AppLanguage.ARABIC) {
                    "تم بدء مدينة جديدة بنجاح"
                } else {
                    "Started a fresh city"
                }
            )
        )
        _uiState.value = fresh
        saveNowAsync()
    }

    fun setTimeOfDay(tod: Float) {
        _uiState.update { state ->
            val clipped = tod.coerceIn(0f, 1f)
            state.copy(
                timeOfDay = clipped,
                weatherTempC = computeWeatherTemp(state.season, state.weather, clipped)
            )
        }
    }

    fun cycleSeason() {
        _uiState.update { state ->
            val nextS = state.season.next()
            val nextW = pickWeatherForSeason(nextS, tickCounter + 7)
            state.copy(
                season = nextS,
                weather = nextW,
                weatherTempC = computeWeatherTemp(nextS, nextW, state.timeOfDay),
                weatherHumidityPct = computeWeatherHumidity(nextW, nextS, tickCounter + 7)
            )
        }
        saveNowAsync()
    }

    fun updateCityName(name: String) {
        if (name.isNotBlank()) {
            _uiState.update { it.copy(cityName = name.trim().take(24)) }
            saveNowAsync()
        }
    }

    // Local Storage Audio Player Controls (manual delete only, never auto-deleted or re-added!)
    fun addLocalSongs(uris: List<Uri>) {
        if (uris.isEmpty()) return
        _uiState.update { state ->
            val existingUris = state.playlist.map { it.uriString }.toSet()
            val added = uris.mapNotNull { uri ->
                val str = uri.toString()
                if (str in existingUris) null
                else AudioTrackItem(str, musicPlayer.resolveTrackTitle(uri))
            }
            val updated = state.playlist + added
            musicPlayer.setPlaylist(updated)
            saveManager.savePlaylistOnly(updated)
            state.copy(playlist = updated)
        }
        saveNowAsync()
    }

    fun removeLocalSong(uriString: String) {
        _uiState.update { state ->
            val updated = state.playlist.filterNot { it.uriString == uriString }
            musicPlayer.setPlaylist(updated)
            saveManager.savePlaylistOnly(updated)
            state.copy(playlist = updated)
        }
        saveNowAsync()
    }

    fun playSongAt(index: Int) = musicPlayer.playTrack(index)
    fun toggleMusicPlayPause() = musicPlayer.togglePlayPause()
    fun nextSong() = musicPlayer.next()
    fun prevSong() = musicPlayer.previous()

    fun saveNowAsync() {
        viewModelScope.launch(Dispatchers.IO) {
            saveMutex.withLock {
                val snapshot = _uiState.value
                val path = saveManager.saveAutoSave(snapshot)
                val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                _uiState.update {
                    it.copy(
                        lastAutoSaveTimeText = timeStr,
                        autoSavePathLabel = path.ifEmpty { it.autoSavePathLabel }
                    )
                }
            }
        }
    }

    private fun advanceMission(
        missions: List<MissionItem>,
        goal: MissionGoalType,
        delta: Int
    ): List<MissionItem> {
        return missions.map { m ->
            if (m.goalType == goal && !m.claimed && m.progress < m.targetCount) {
                m.copy(progress = (m.progress + delta).coerceAtMost(m.targetCount))
            } else m
        }
    }

    private fun pickRandomResidentEvent(): ResidentEvent {
        val pool = listOf(
            ResidentEvent(
                id = "ev_festival",
                titleEn = "Neighborhood Street Festival",
                titleAr = "مهرجان الحي الشعبي",
                descriptionEn = "Citizens want to host a weekend food & lantern festival along the main avenue!",
                descriptionAr = "يرغب السكان في إقامة مهرجان للأطعمة والفوانيس في الشارع الرئيسي!",
                optionAEn = "Sponsor Lanterns & Music",
                optionAAr = "رعاية المهرجان والموسيقى",
                optionACostCoins = 120,
                optionARewardCoins = 340,
                optionARewardGems = 3,
                optionARewardXp = 55,
                optionARewardHappiness = 4,
                optionBEn = "Allow Volunteer Booths",
                optionBAr = "السماح بالأكشاك التطوعية",
                optionBRewardCoins = 140,
                optionBRewardXp = 25,
                optionBRewardHappiness = 2
            ),
            ResidentEvent(
                id = "ev_trees",
                titleEn = "Green Canopy Planting Day",
                titleAr = "يوم التشجير المجتمعي",
                descriptionEn = "Local school students organized a tree-planting initiative near city roads.",
                descriptionAr = "نظم طلاب المدارس مبادرة لزراعة الأشجار على جوانب الطرق.",
                optionAEn = "Fund Saplings & Parks",
                optionAAr = "تمويل الشتلات والحدائق",
                optionACostCoins = 90,
                optionARewardCoins = 220,
                optionARewardGems = 4,
                optionARewardXp = 60,
                optionARewardHappiness = 5,
                optionBEn = "Send City Volunteers",
                optionBAr = "إرسال متطوعي البلدية",
                optionBRewardCoins = 110,
                optionBRewardXp = 30,
                optionBRewardHappiness = 2
            ),
            ResidentEvent(
                id = "ev_highway",
                titleEn = "Highway Commuter Safety Fair",
                titleAr = "حملة السلامة على الطريق السريع",
                descriptionEn = "Truck drivers and bus operators suggest adding reflective lane signs on the outer highway.",
                descriptionAr = "اقترح سائقو الشاحنات والحافلات إضافة لوحات إرشادية على الطريق السريع.",
                optionAEn = "Install Smart Signs",
                optionAAr = "تركيب لوحات ذكية",
                optionACostCoins = 150,
                optionARewardCoins = 420,
                optionARewardGems = 3,
                optionARewardXp = 70,
                optionARewardHappiness = 3,
                optionBEn = "Broadcast Radio Tips",
                optionBAr = "بث نصائح مرورية",
                optionBRewardCoins = 160,
                optionBRewardXp = 30,
                optionBRewardHappiness = 1
            ),
            ResidentEvent(
                id = "ev_market",
                titleEn = "Farmers Fresh Harvest Market",
                titleAr = "سوق المزارعين للمحاصيل الطازجة",
                descriptionEn = "Local farmers brought fresh crops and dairy from the pastures to sell downtown.",
                descriptionAr = "أحضر المزارعون محاصيل طازجة ومنتجات ألبان من المراعي للبيع في المدينة.",
                optionAEn = "Open Grand Market Plaza",
                optionAAr = "افتتاح ساحة السوق الكبرى",
                optionACostCoins = 100,
                optionARewardCoins = 380,
                optionARewardGems = 2,
                optionARewardXp = 50,
                optionARewardHappiness = 4,
                optionBEn = "Issue One-Day Permit",
                optionBAr = "إصدار تصريح ليوم واحد",
                optionBRewardCoins = 180,
                optionBRewardXp = 25,
                optionBRewardHappiness = 2
            )
        )
        return pool.random()
    }

    override fun onCleared() {
        super.onCleared()
        saveManager.saveAutoSave(_uiState.value)
        musicPlayer.stopAndRelease()
    }
}
