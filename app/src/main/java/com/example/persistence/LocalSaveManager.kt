package com.example.persistence

import android.content.Context
import com.example.model.AppLanguage
import com.example.model.AudioTrackItem
import com.example.model.BuildingType
import com.example.model.CitySettings
import com.example.model.CraftRecipe
import com.example.model.GameState
import com.example.model.GridCell
import com.example.model.MissionGoalType
import com.example.model.MissionItem
import com.example.model.MissionPeriod
import com.example.model.PlacedBuilding
import com.example.model.Season
import com.example.model.TerrainType
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

data class LoadedSaveResult(
    val state: GameState,
    val foundPath: String
)

class LocalSaveManager(private val context: Context) {

    companion object {
        private const val AUTOSAVE_FILE = "skyline_autosave.json"
        private const val BACKUP_FILE = "skyline_autosave.bak.json"
        private const val PLAYLIST_FILE = "skyline_playlist.json"
        private val SAVE_LOCK = Any()
        private val SAVE_SEQ = AtomicLong(0L)
    }

    private fun candidateSaveDirectories(): List<File> {
        val dirs = mutableListOf<File>()
        context.getExternalFilesDir(null)?.let { dirs.add(it) }
        context.getExternalFilesDir("saves")?.let { dirs.add(it) }
        dirs.add(context.filesDir)
        return dirs.distinctBy { it.absolutePath }
    }

    /**
     * Automatically searches phone storage directories for the newest valid autosave file.
     */
    fun searchAndLoadAutoSave(): LoadedSaveResult? = synchronized(SAVE_LOCK) {
        data class ParsedCandidate(
            val file: File,
            val state: GameState,
            val savedAtMillis: Long,
            val saveSeq: Long,
            val isPrimary: Boolean
        )

        val parsed = mutableListOf<ParsedCandidate>()
        for (dir in candidateSaveDirectories()) {
            val primary = File(dir, AUTOSAVE_FILE)
            val backup = File(dir, BACKUP_FILE)
            listOf(primary to true, backup to false).forEach { (file, isPrimary) ->
                if (file.exists() && file.length() > 10) {
                    try {
                        val json = JSONObject(file.readText())
                        val state = deserializeGameState(json)
                        if (state.cells.isNotEmpty()) {
                            parsed.add(
                                ParsedCandidate(
                                    file = file,
                                    state = state,
                                    savedAtMillis = json.optLong("savedAtMillis", file.lastModified()),
                                    saveSeq = json.optLong("saveSeq", 0L),
                                    isPrimary = isPrimary
                                )
                            )
                        }
                    } catch (_: Exception) {
                        // Try next backup/candidate
                    }
                }
            }
        }

        val best = parsed.maxWithOrNull(
            compareBy<ParsedCandidate> { it.saveSeq }
                .thenBy { it.savedAtMillis }
                .thenBy { it.file.lastModified() }
                .thenBy { if (it.isPrimary) 1 else 0 }
        ) ?: return@synchronized null

        LoadedSaveResult(
            state = best.state.copy(
                resumedFromSave = true,
                autoSavePathLabel = best.file.absolutePath
            ),
            foundPath = best.file.absolutePath
        )
    }

    fun saveAutoSave(state: GameState): String = synchronized(SAVE_LOCK) {
        val json = serializeGameState(state)
        val payload = json.toString()
        var savedPath = ""

        for (dir in candidateSaveDirectories()) {
            try {
                if (!dir.exists()) dir.mkdirs()
                val target = File(dir, AUTOSAVE_FILE)
                val backup = File(dir, BACKUP_FILE)
                val temp = File(dir, "$AUTOSAVE_FILE.tmp")
                // Backup previous save BEFORE writing temp so backup never gets a newer mtime than target
                if (target.exists()) {
                    target.copyTo(backup, overwrite = true)
                }
                temp.writeText(payload)
                if (!temp.renameTo(target)) {
                    target.writeText(payload)
                    temp.delete()
                }
                target.setLastModified(System.currentTimeMillis())
                if (savedPath.isEmpty()) savedPath = target.absolutePath
            } catch (_: Exception) {
                // Continue writing to remaining directories
            }
        }
        savedPath
    }

    fun savePlaylistOnly(playlist: List<AudioTrackItem>) = synchronized(SAVE_LOCK) {
        val arr = JSONArray()
        playlist.forEach { track ->
            arr.put(JSONObject().apply {
                put("uri", track.uriString)
                put("title", track.title)
            })
        }
        for (dir in candidateSaveDirectories()) {
            try {
                if (!dir.exists()) dir.mkdirs()
                File(dir, PLAYLIST_FILE).writeText(arr.toString())
            } catch (_: Exception) {
            }
        }
    }

    fun loadSavedPlaylist(): List<AudioTrackItem> {
        for (dir in candidateSaveDirectories()) {
            val f = File(dir, PLAYLIST_FILE)
            if (f.exists() && f.length() > 2) {
                try {
                    val arr = JSONArray(f.readText())
                    val list = mutableListOf<AudioTrackItem>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val uri = obj.optString("uri")
                        val title = obj.optString("title")
                        if (uri.isNotEmpty()) {
                            list.add(AudioTrackItem(uri, title.ifEmpty { "Track ${i + 1}" }))
                        }
                    }
                    if (list.isNotEmpty()) return list
                } catch (_: Exception) {
                }
            }
        }
        return emptyList()
    }

    fun getPrimarySavePath(): String {
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        return File(dir, AUTOSAVE_FILE).absolutePath
    }

    private fun serializeGameState(state: GameState): JSONObject {
        val now = System.currentTimeMillis()
        val root = JSONObject()
        root.put("version", 1)
        root.put("savedAtMillis", now)
        root.put("saveSeq", SAVE_SEQ.incrementAndGet())
        root.put("cityName", state.cityName)
        root.put("gridSize", state.gridSize)
        root.put("coins", state.coins)
        root.put("gems", state.gems)
        root.put("xp", state.xp)
        root.put("level", state.level)
        root.put("happiness", state.happiness)
        root.put("season", state.season.name)
        root.put("weather", state.weather.name)
        root.put("weatherTempC", state.weatherTempC)
        root.put("weatherHumidityPct", state.weatherHumidityPct)
        root.put("timeOfDay", state.timeOfDay.toDouble())

        // Settings
        root.put("settings", JSONObject().apply {
            put("soundVolume", state.settings.soundVolume.toDouble())
            put("sfxEnabled", state.settings.sfxEnabled)
            put("brightness", state.settings.brightness.toDouble())
            put("language", state.settings.language.name)
            put("autoDayNightCycle", state.settings.autoDayNightCycle)
            put("simulationSpeed", state.settings.simulationSpeed)
            put("taxPolicy", state.settings.taxPolicy.name)
            put("trafficDensity", state.settings.trafficDensity.name)
            put("brightRoadLinesAlways", state.settings.brightRoadLinesAlways)
            put("weatherShortMode", state.settings.weatherShortMode)
        })

        // Storage Inventory
        val invObj = JSONObject()
        state.storageInventory.forEach { (recipe, qty) ->
            if (qty > 0) invObj.put(recipe.id, qty)
        }
        root.put("storageInventory", invObj)

        // Missions
        val missionsArr = JSONArray()
        state.missions.forEach { m ->
            missionsArr.put(JSONObject().apply {
                put("id", m.id)
                put("period", m.period.name)
                put("goalType", m.goalType.name)
                put("titleEn", m.titleEn)
                put("titleAr", m.titleAr)
                put("targetCount", m.targetCount)
                put("progress", m.progress)
                put("rewardCoins", m.rewardCoins)
                put("rewardGems", m.rewardGems)
                put("rewardXp", m.rewardXp)
                put("claimed", m.claimed)
            })
        }
        root.put("missions", missionsArr)

        // Playlist
        val playlistArr = JSONArray()
        state.playlist.forEach { t ->
            playlistArr.put(JSONObject().apply {
                put("uri", t.uriString)
                put("title", t.title)
            })
        }
        root.put("playlist", playlistArr)

        // Grid Cells
        val cellsArr = JSONArray()
        state.cells.forEach { cell ->
            val cellObj = JSONObject().apply {
                put("x", cell.x)
                put("z", cell.z)
                put("terrain", cell.terrain.name)
                put("elevation", cell.elevation.toDouble())
                cell.building?.let { b ->
                    put("building", JSONObject().apply {
                        put("id", b.id)
                        put("typeId", b.type.id)
                        put("level", b.level)
                        put("isConstructing", b.isConstructing)
                        put("constructionRemainingSec", b.constructionRemainingSec)
                        put("constructionTotalSec", b.constructionTotalSec)
                        put("isUpgrading", b.isUpgrading)
                        put("upgradeRemainingSec", b.upgradeRemainingSec)
                        put("upgradeTotalSec", b.upgradeTotalSec)
                        put("upgradePaidCoins", b.upgradePaidCoins)
                        b.craftingRecipe?.let { put("craftingRecipeId", it.id) }
                        put("craftRemainingSec", b.craftRemainingSec)
                        put("craftTotalSec", b.craftTotalSec)
                        put("rotation", b.rotation)
                    })
                }
            }
            cellsArr.put(cellObj)
        }
        root.put("cells", cellsArr)
        return root
    }

    private fun deserializeGameState(root: JSONObject): GameState {
        val savedAt = root.optLong("savedAtMillis", System.currentTimeMillis())
        val elapsedSec = ((System.currentTimeMillis() - savedAt) / 1000L).coerceIn(0L, 600L).toInt()
        val gridSize = root.optInt("gridSize", 22)

        val settingsObj = root.optJSONObject("settings")
        val settings = if (settingsObj != null) {
            CitySettings(
                soundVolume = settingsObj.optDouble("soundVolume", 0.8).toFloat().coerceIn(0f, 1f),
                sfxEnabled = settingsObj.optBoolean("sfxEnabled", true),
                brightness = settingsObj.optDouble("brightness", 1.0).toFloat().coerceIn(0.35f, 1.25f),
                language = runCatching {
                    AppLanguage.valueOf(settingsObj.optString("language", AppLanguage.ENGLISH.name))
                }.getOrDefault(AppLanguage.ENGLISH),
                autoDayNightCycle = settingsObj.optBoolean("autoDayNightCycle", true),
                simulationSpeed = settingsObj.optInt("simulationSpeed", 1).coerceIn(0, 3),
                taxPolicy = runCatching {
                    com.example.model.TaxPolicy.valueOf(settingsObj.optString("taxPolicy", com.example.model.TaxPolicy.BALANCED.name))
                }.getOrDefault(com.example.model.TaxPolicy.BALANCED),
                trafficDensity = runCatching {
                    com.example.model.TrafficDensity.valueOf(settingsObj.optString("trafficDensity", com.example.model.TrafficDensity.BALANCED.name))
                }.getOrDefault(com.example.model.TrafficDensity.BALANCED),
                brightRoadLinesAlways = settingsObj.optBoolean("brightRoadLinesAlways", false),
                weatherShortMode = settingsObj.optBoolean("weatherShortMode", false)
            )
        } else CitySettings()

        val inventory = mutableMapOf<CraftRecipe, Int>()
        val invObj = root.optJSONObject("storageInventory")
        if (invObj != null) {
            val keys = invObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                CraftRecipe.fromId(key)?.let { recipe ->
                    val qty = invObj.optInt(key, 0)
                    if (qty > 0) inventory[recipe] = qty
                }
            }
        }

        val missions = mutableListOf<MissionItem>()
        val missionsArr = root.optJSONArray("missions")
        if (missionsArr != null) {
            for (i in 0 until missionsArr.length()) {
                val m = missionsArr.getJSONObject(i)
                val period = runCatching { MissionPeriod.valueOf(m.optString("period")) }.getOrDefault(MissionPeriod.DAILY)
                val goal = runCatching { MissionGoalType.valueOf(m.optString("goalType")) }.getOrDefault(MissionGoalType.BUILD_ANY)
                missions.add(
                    MissionItem(
                        id = m.optString("id"),
                        period = period,
                        goalType = goal,
                        titleEn = m.optString("titleEn"),
                        titleAr = m.optString("titleAr"),
                        targetCount = m.optInt("targetCount", 1),
                        progress = m.optInt("progress", 0),
                        rewardCoins = m.optInt("rewardCoins", 100),
                        rewardGems = m.optInt("rewardGems", 2),
                        rewardXp = m.optInt("rewardXp", 25),
                        claimed = m.optBoolean("claimed", false)
                    )
                )
            }
        }

        val playlist = mutableListOf<AudioTrackItem>()
        val playlistArr = root.optJSONArray("playlist")
        if (playlistArr != null) {
            for (i in 0 until playlistArr.length()) {
                val p = playlistArr.getJSONObject(i)
                val uri = p.optString("uri")
                if (uri.isNotEmpty()) {
                    playlist.add(AudioTrackItem(uri, p.optString("title", "Audio Track")))
                }
            }
        }
        val finalPlaylist = if (playlist.isNotEmpty()) playlist else loadSavedPlaylist()

        val cells = mutableListOf<GridCell>()
        val cellsArr = root.optJSONArray("cells")
        if (cellsArr != null) {
            for (i in 0 until cellsArr.length()) {
                val c = cellsArr.getJSONObject(i)
                val x = c.optInt("x")
                val z = c.optInt("z")
                val terrain = runCatching {
                    TerrainType.valueOf(c.optString("terrain", TerrainType.GRASS.name))
                }.getOrDefault(TerrainType.GRASS)
                val elevation = c.optDouble("elevation", 0.0).toFloat()
                val bObj = c.optJSONObject("building")
                val placed = if (bObj != null) {
                    val bType = BuildingType.fromId(bObj.optString("typeId"))
                    if (bType != null) {
                        var isConst = bObj.optBoolean("isConstructing", false)
                        var constRem = bObj.optInt("constructionRemainingSec", 0)
                        if (isConst) {
                            constRem = (constRem - elapsedSec).coerceAtLeast(0)
                            if (constRem == 0) isConst = false
                        }

                        var isUpg = bObj.optBoolean("isUpgrading", false)
                        var upgRem = bObj.optInt("upgradeRemainingSec", 0)
                        var lvl = bObj.optInt("level", 1)
                        if (isUpg) {
                            upgRem = (upgRem - elapsedSec).coerceAtLeast(0)
                            if (upgRem == 0) {
                                isUpg = false
                                lvl = (lvl + 1).coerceAtMost(bType.maxLevel)
                            }
                        }

                        var recipe = bObj.optString("craftingRecipeId", "").let {
                            if (it.isNotEmpty()) CraftRecipe.fromId(it) else null
                        }
                        var craftRem = bObj.optInt("craftRemainingSec", 0)
                        if (recipe != null) {
                            craftRem = (craftRem - elapsedSec).coerceAtLeast(0)
                            if (craftRem == 0) {
                                inventory[recipe] = (inventory[recipe] ?: 0) + 1
                                recipe = null
                            }
                        }

                        PlacedBuilding(
                            id = bObj.optString("id"),
                            type = bType,
                            level = lvl,
                            isConstructing = isConst,
                            constructionRemainingSec = constRem,
                            constructionTotalSec = bObj.optInt("constructionTotalSec", bType.buildTimeSec),
                            isUpgrading = isUpg,
                            upgradeRemainingSec = upgRem,
                            upgradeTotalSec = bObj.optInt("upgradeTotalSec", 0),
                            upgradePaidCoins = bObj.optInt("upgradePaidCoins", 0),
                            craftingRecipe = recipe,
                            craftRemainingSec = craftRem,
                            craftTotalSec = bObj.optInt("craftTotalSec", 0),
                            rotation = bObj.optInt("rotation", 0)
                        )
                    } else null
                } else null

                cells.add(GridCell(x = x, z = z, terrain = terrain, elevation = elevation, building = placed))
            }
        }

        val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(savedAt))
        val season = runCatching {
            Season.valueOf(root.optString("season", Season.SPRING.name))
        }.getOrDefault(Season.SPRING)
        val weather = runCatching {
            com.example.model.WeatherCondition.valueOf(root.optString("weather", com.example.model.WeatherCondition.SUNNY.name))
        }.getOrDefault(com.example.model.WeatherCondition.SUNNY)

        return GameState(
            cityName = root.optString("cityName", "New Horizon"),
            gridSize = gridSize,
            cells = cells,
            coins = root.optLong("coins", 3500L),
            gems = root.optInt("gems", 30),
            xp = root.optLong("xp", 0L),
            level = root.optInt("level", 1),
            happiness = root.optInt("happiness", 92),
            storageInventory = inventory,
            season = season,
            weather = weather,
            weatherTempC = root.optInt("weatherTempC", 25).coerceIn(-10, 48),
            weatherHumidityPct = root.optInt("weatherHumidityPct", 42).coerceIn(10, 99),
            timeOfDay = root.optDouble("timeOfDay", 0.36).toFloat().coerceIn(0f, 1f),
            missions = missions,
            settings = settings,
            playlist = finalPlaylist,
            lastAutoSaveTimeText = timeFmt
        )
    }
}
