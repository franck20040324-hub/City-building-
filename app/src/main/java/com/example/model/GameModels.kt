package com.example.model

enum class AppLanguage(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    ARABIC("ar", "العربية")
}

enum class Season(val index: Int) {
    SPRING(0),
    SUMMER(1),
    AUTUMN(2),
    WINTER(3);

    fun next(): Season = entries[(index + 1) % entries.size]
}

enum class WeatherCondition(
    val primaryEmoji: String,
    val twoEmojis: String
) {
    SUNNY("☀️", "☀️⛅"),
    CLOUDY("☁️", "☁️⛅"),
    RAINY("🌧️", "🌧️☁️");

    fun next(): WeatherCondition = entries[(ordinal + 1) % entries.size]
}

enum class BuildCategory {
    ROADS_TRANSIT,
    RESIDENTIAL,
    CIVIC_SAFETY,
    COMMERCIAL_ADMIN,
    FACTORIES_FARMS,
    LANDMARKS_DECOR,
    TERRAIN_TOOLS
}

enum class TerrainType {
    GRASS,
    FERTILE_PLAIN,
    FOREST_LAND,
    WATER,
    OUTSIDE_HIGHWAY
}

enum class ToolMode {
    INSPECT,
    BUILD,
    UPGRADE_ROAD,   // Tap 1+1 two-way road tiles to upgrade into 2+2 double-way roads
    LOWER_GROUND,   // Lowers ground to create pond/lake/river
    RAISE_GROUND,   // Raises ground to turn water back to land
    CANCEL_REFUND   // Cancel construction / demolish & recover money
}

enum class TaxPolicy(val multiplier: Float, val happinessDelta: Int) {
    LOW(0.80f, 6),
    BALANCED(1.00f, 0),
    HIGH(1.35f, -6)
}

enum class TrafficDensity(val scale: Float) {
    LOW(0.5f),
    BALANCED(1.0f),
    HIGH(1.4f)
}

enum class BuildingType(
    val id: String,
    val category: BuildCategory,
    val coinCost: Int,
    val gemCost: Int,
    val buildTimeSec: Int,          // Strictly 30s..300s (0 only for instant roads/trees if desired, or 30s for buildings)
    val basePopulation: Int,
    val baseTaxPerCycle: Int,
    val xpReward: Int,
    val maxLevel: Int = 5,
    val isRoad: Boolean = false,
    val isWaterRequired: Boolean = false
) {
    // Roads & Highway Infrastructure (instant/quick civil works with full cancel-refund!)
    ROAD_TWO_WAY("road_two_way", BuildCategory.ROADS_TRANSIT, 25, 0, 0, 0, 1, 5, 1, isRoad = true),
    ROAD_FOUR_LANE("road_four_lane", BuildCategory.ROADS_TRANSIT, 60, 0, 0, 0, 3, 10, 1, isRoad = true),
    ROAD_HIGHWAY("road_highway", BuildCategory.ROADS_TRANSIT, 120, 0, 0, 0, 6, 18, 1, isRoad = true),
    FUEL_STATION("fuel_station", BuildCategory.ROADS_TRANSIT, 180, 0, 40, 0, 18, 25, 3),
    HIGHWAY_TOLL("highway_toll", BuildCategory.ROADS_TRANSIT, 240, 0, 45, 0, 35, 30, 3),

    // Public Transit, Rail, Aviation, Seaport
    BUS_STATION("bus_station", BuildCategory.ROADS_TRANSIT, 220, 0, 60, 0, 14, 30, 3),
    TAXI_STATION("taxi_station", BuildCategory.ROADS_TRANSIT, 250, 0, 60, 0, 18, 32, 3),
    TRAM_STATION("tram_station", BuildCategory.ROADS_TRANSIT, 380, 0, 90, 0, 26, 45, 3),
    TRAIN_STATION("train_station", BuildCategory.ROADS_TRANSIT, 550, 0, 120, 0, 40, 65, 3),
    METRO_STATION("metro_station", BuildCategory.ROADS_TRANSIT, 700, 0, 150, 0, 52, 80, 3),
    SEAPORT("seaport", BuildCategory.ROADS_TRANSIT, 850, 0, 180, 0, 65, 95, 3, isWaterRequired = true),
    AIRPORT("airport", BuildCategory.ROADS_TRANSIT, 1400, 0, 240, 0, 110, 150, 3),

    // Residential (30s smallest -> 240s skyscraper; upgradeable appearance & capacity)
    GROUND_HOUSE("ground_house", BuildCategory.RESIDENTIAL, 80, 0, 30, 8, 10, 18, 5),
    SMALL_APARTMENT("small_apartment", BuildCategory.RESIDENTIAL, 180, 0, 45, 22, 24, 30, 5),
    LARGE_APARTMENT("large_apartment", BuildCategory.RESIDENTIAL, 420, 0, 90, 55, 55, 55, 5),
    LUXURY_APARTMENT("luxury_apartment", BuildCategory.RESIDENTIAL, 900, 0, 180, 110, 115, 110, 5),
    SKYSCRAPER("skyscraper", BuildCategory.RESIDENTIAL, 1800, 0, 240, 240, 230, 200, 5),

    // Commercial & Luxurious Administrative
    SHOP("shop", BuildCategory.COMMERCIAL_ADMIN, 160, 0, 45, 0, 26, 25, 4),
    RESTAURANT("restaurant", BuildCategory.COMMERCIAL_ADMIN, 240, 0, 60, 0, 36, 35, 4),
    EMPLOYMENT_OFFICE("employment_office", BuildCategory.COMMERCIAL_ADMIN, 360, 0, 90, 0, 42, 48, 3),
    COMPANY("company", BuildCategory.COMMERCIAL_ADMIN, 600, 0, 120, 0, 78, 70, 5),
    LUXURY_ADMIN_BUILDING("luxury_admin", BuildCategory.COMMERCIAL_ADMIN, 1500, 0, 240, 0, 150, 160, 4),

    // Civic, Safety, Water & Amusement
    SCHOOL("school", BuildCategory.CIVIC_SAFETY, 320, 0, 90, 0, 12, 45, 3),
    WATER_PLANT("water_plant", BuildCategory.CIVIC_SAFETY, 340, 0, 90, 0, 15, 45, 3),
    FIRE_STATION("fire_station", BuildCategory.CIVIC_SAFETY, 400, 0, 120, 0, 10, 55, 3),
    POLICE_STATION("police_station", BuildCategory.CIVIC_SAFETY, 420, 0, 120, 0, 10, 55, 3),
    HOSPITAL("hospital", BuildCategory.CIVIC_SAFETY, 620, 0, 150, 0, 18, 75, 3),
    PRISON("prison", BuildCategory.CIVIC_SAFETY, 580, 0, 180, 0, 14, 70, 3),
    AMUSEMENT_PARK("amusement_park", BuildCategory.CIVIC_SAFETY, 850, 0, 180, 0, 85, 100, 3),
    WATER_AMUSEMENT_PARK("water_amusement_park", BuildCategory.CIVIC_SAFETY, 1100, 0, 210, 0, 110, 130, 3),

    // Factories & Agriculture (30s..210s build)
    CROP_FIELD("crop_field", BuildCategory.FACTORIES_FARMS, 70, 0, 30, 0, 14, 15, 3),
    LIVESTOCK_PASTURE("livestock_pasture", BuildCategory.FACTORIES_FARMS, 130, 0, 45, 0, 22, 22, 3),
    FOREST_GROVE("forest_grove", BuildCategory.FACTORIES_FARMS, 50, 0, 30, 0, 4, 12, 1),
    FOOD_FACTORY("food_factory", BuildCategory.FACTORIES_FARMS, 260, 0, 60, 0, 20, 35, 4),
    CLOTHING_FACTORY("clothing_factory", BuildCategory.FACTORIES_FARMS, 350, 0, 90, 0, 28, 45, 4),
    EQUIPMENT_FACTORY("equipment_factory", BuildCategory.FACTORIES_FARMS, 480, 0, 120, 0, 36, 60, 4),
    MEDICINE_FACTORY("medicine_factory", BuildCategory.FACTORIES_FARMS, 620, 0, 150, 0, 48, 75, 4),
    RECYCLING_FACTORY("recycling_factory", BuildCategory.FACTORIES_FARMS, 720, 0, 180, 0, 56, 85, 4),
    TRANSPORT_FACTORY("transport_factory", BuildCategory.FACTORIES_FARMS, 950, 0, 210, 0, 74, 110, 4),

    // World-Famous Real-Life Luxury Landmarks (Gems, max 300s = 5 minutes!)
    LANDMARK_EIFFEL("landmark_eiffel", BuildCategory.LANDMARKS_DECOR, 0, 12, 300, 0, 260, 320, 1),
    LANDMARK_BURJ_KHALIFA("landmark_burj", BuildCategory.LANDMARKS_DECOR, 0, 18, 300, 0, 340, 400, 1),
    LANDMARK_STATUE_LIBERTY("landmark_liberty", BuildCategory.LANDMARKS_DECOR, 0, 14, 300, 0, 280, 340, 1),
    LANDMARK_BIG_BEN("landmark_big_ben", BuildCategory.LANDMARKS_DECOR, 0, 10, 300, 0, 220, 280, 1),
    LANDMARK_COLOSSEUM("landmark_colosseum", BuildCategory.LANDMARKS_DECOR, 0, 15, 300, 0, 290, 350, 1),
    LANDMARK_PYRAMID("landmark_pyramid", BuildCategory.LANDMARKS_DECOR, 0, 16, 300, 0, 310, 380, 1),

    // Cosmetics
    COSMETIC_TREE_GARDEN("cosmetic_trees", BuildCategory.LANDMARKS_DECOR, 40, 0, 30, 0, 5, 10, 1),
    COSMETIC_PARK_BENCH("cosmetic_bench", BuildCategory.LANDMARKS_DECOR, 55, 0, 30, 0, 8, 12, 1),
    COSMETIC_FOUNTAIN("cosmetic_fountain", BuildCategory.LANDMARKS_DECOR, 120, 0, 30, 0, 16, 20, 1),
    COSMETIC_ARCH("cosmetic_arch", BuildCategory.LANDMARKS_DECOR, 200, 0, 45, 0, 24, 30, 1);

    fun upgradedRoadTarget(): BuildingType? = when (this) {
        ROAD_TWO_WAY -> ROAD_FOUR_LANE
        else -> null
    }

    companion object {
        fun fromId(id: String): BuildingType? = entries.find { it.id == id }
    }
}

enum class CraftRecipe(
    val id: String,
    val factoryType: BuildingType,
    val craftTimeSec: Int,  // Strictly 30s..300s (5 minutes max)
    val inputCost: Int,
    val sellValue: Int,
    val xpReward: Int
) {
    FRESH_BREAD("bread", BuildingType.FOOD_FACTORY, 30, 20, 65, 16),
    ORGANIC_MEAL("meal_box", BuildingType.FOOD_FACTORY, 90, 45, 140, 32),

    COTTON_APPAREL("apparel", BuildingType.CLOTHING_FACTORY, 45, 30, 95, 22),
    LUXURY_SUIT("luxury_suit", BuildingType.CLOTHING_FACTORY, 150, 75, 230, 52),

    STEEL_TOOLS("steel_tools", BuildingType.EQUIPMENT_FACTORY, 60, 40, 125, 28),
    HEAVY_GEAR("heavy_gear", BuildingType.EQUIPMENT_FACTORY, 180, 95, 290, 68),

    FIRST_AID_KIT("first_aid", BuildingType.MEDICINE_FACTORY, 90, 55, 175, 38),
    BIOTECH_VACCINE("vaccine", BuildingType.MEDICINE_FACTORY, 240, 120, 380, 90),

    RECYCLED_ALLOY("recycled_alloy", BuildingType.RECYCLING_FACTORY, 60, 35, 120, 30),
    CLEAN_BIOFUEL("clean_biofuel", BuildingType.RECYCLING_FACTORY, 180, 85, 275, 65),

    CITY_EV_PARTS("ev_parts", BuildingType.TRANSPORT_FACTORY, 120, 70, 225, 50),
    LUXURY_COACH("luxury_coach", BuildingType.TRANSPORT_FACTORY, 300, 150, 490, 125);

    companion object {
        fun forFactory(type: BuildingType): List<CraftRecipe> = entries.filter { it.factoryType == type }
        fun fromId(id: String): CraftRecipe? = entries.find { it.id == id }
    }
}

data class PlacedBuilding(
    val id: String,
    val type: BuildingType,
    val level: Int = 1,
    val isConstructing: Boolean = false,
    val constructionRemainingSec: Int = 0,
    val constructionTotalSec: Int = 0,
    val isUpgrading: Boolean = false,
    val upgradeRemainingSec: Int = 0,
    val upgradeTotalSec: Int = 0,
    val upgradePaidCoins: Int = 0,
    val craftingRecipe: CraftRecipe? = null,
    val craftRemainingSec: Int = 0,
    val craftTotalSec: Int = 0,
    val rotation: Int = 0
) {
    fun canUpgradeRoad(): Boolean = type.upgradedRoadTarget() != null

    fun roadUpgradeCost(): Int {
        val target = type.upgradedRoadTarget() ?: return 0
        return (target.coinCost - type.coinCost).coerceAtLeast(25)
    }

    fun currentPopulation(): Int {
        if (isConstructing || type.basePopulation == 0) return 0
        return (type.basePopulation * (1f + (level - 1) * 0.45f)).toInt()
    }

    fun currentTaxIncome(): Int {
        if (isConstructing) return 0
        return (type.baseTaxPerCycle * (1f + (level - 1) * 0.4f)).toInt()
    }

    fun nextUpgradeCost(): Int {
        if (canUpgradeRoad()) return roadUpgradeCost()
        if (level >= type.maxLevel) return 0
        return (type.coinCost * (0.65f * level)).toInt().coerceAtLeast(60)
    }

    fun nextUpgradeDurationSec(): Int {
        // Strictly capped at 300s (5 minutes) and minimum 30s
        val base = type.buildTimeSec.coerceAtLeast(30)
        return (base + level * 25).coerceIn(30, 300)
    }
}

data class GridCell(
    val x: Int,
    val z: Int,
    val terrain: TerrainType = TerrainType.GRASS,
    val elevation: Float = 0f,
    val building: PlacedBuilding? = null
)

enum class MissionPeriod { DAILY, WEEKLY }

enum class MissionGoalType {
    BUILD_ANY,
    BUILD_RESIDENTIAL,
    BUILD_ROAD,
    UPGRADE_BUILDING,
    CRAFT_GOODS,
    COLLECT_TAXES,
    PLANT_OR_TERRAFORM
}

data class MissionItem(
    val id: String,
    val period: MissionPeriod,
    val goalType: MissionGoalType,
    val titleEn: String,
    val titleAr: String,
    val targetCount: Int,
    val progress: Int = 0,
    val rewardCoins: Int,
    val rewardGems: Int,
    val rewardXp: Int,
    val claimed: Boolean = false
)

data class ResidentEvent(
    val id: String,
    val titleEn: String,
    val titleAr: String,
    val descriptionEn: String,
    val descriptionAr: String,
    val optionAEn: String,
    val optionAAr: String,
    val optionACostCoins: Int,
    val optionARewardCoins: Int,
    val optionARewardGems: Int,
    val optionARewardXp: Int,
    val optionARewardHappiness: Int,
    val optionBEn: String,
    val optionBAr: String,
    val optionBRewardCoins: Int,
    val optionBRewardXp: Int,
    val optionBRewardHappiness: Int
)

data class AudioTrackItem(
    val uriString: String,
    val title: String
)

data class CitySettings(
    val soundVolume: Float = 0.8f,
    val sfxEnabled: Boolean = true,
    val brightness: Float = 1.0f,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val autoDayNightCycle: Boolean = true,
    val simulationSpeed: Int = 1, // 0 = paused, 1 = 1x, 2 = 2x, 3 = 3x
    val taxPolicy: TaxPolicy = TaxPolicy.BALANCED,
    val trafficDensity: TrafficDensity = TrafficDensity.BALANCED,
    val brightRoadLinesAlways: Boolean = false,
    val weatherShortMode: Boolean = false
)

data class GameState(
    val cityName: String = "New Horizon",
    val gridSize: Int = 22,
    val cells: List<GridCell> = emptyList(),
    val coins: Long = 3500L,
    val gems: Int = 30,
    val xp: Long = 0L,
    val level: Int = 1,
    val happiness: Int = 92,
    val population: Int = 0,
    val taxPerMinute: Int = 0,
    val storageCapacity: Int = 999_999,
    val storageInventory: Map<CraftRecipe, Int> = emptyMap(),
    val season: Season = Season.SPRING,
    val weather: WeatherCondition = WeatherCondition.SUNNY,
    val weatherTempC: Int = 25,
    val weatherHumidityPct: Int = 42,
    val timeOfDay: Float = 0.36f, // 0..1 (0.25 dawn, 0.5 noon, 0.75 sunset, 0.0/1.0 midnight)
    val toolMode: ToolMode = ToolMode.INSPECT,
    val selectedBuildType: BuildingType? = null,
    val selectedCellCoords: Pair<Int, Int>? = null,
    val activeCategory: BuildCategory = BuildCategory.RESIDENTIAL,
    val missions: List<MissionItem> = emptyList(),
    val activeResidentEvent: ResidentEvent? = null,
    val settings: CitySettings = CitySettings(),
    val playlist: List<AudioTrackItem> = emptyList(),
    val currentTrackIndex: Int = -1,
    val isMusicPlaying: Boolean = false,
    val lastAutoSaveTimeText: String = "",
    val autoSavePathLabel: String = "",
    val resumedFromSave: Boolean = false,
    val statusBanner: String? = null
) {
    fun xpForNextLevel(): Long = (level * 150L) + 100L
    fun totalStoredItems(): Int = storageInventory.values.sum()
}
