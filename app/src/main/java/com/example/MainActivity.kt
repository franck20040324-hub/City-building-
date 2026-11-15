package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.engine3d.CityGLSurfaceView
import com.example.i18n.AppStrings
import com.example.model.AppLanguage
import com.example.model.ToolMode
import com.example.ui.ActiveToolBanner
import com.example.ui.BottomBuildDock
import com.example.ui.CameraControlColumn
import com.example.ui.CityOverviewDialog
import com.example.ui.CustomizeResourcesDialog
import com.example.ui.LocalMusicPlayerDialog
import com.example.ui.MarketViewMode
import com.example.ui.MissionsDialog
import com.example.ui.ResidentEventDialog
import com.example.ui.SelectedCellInspectorCard
import com.example.ui.SettingsDialog
import com.example.ui.StorageMarketDialog
import com.example.ui.TopMayorHud
import com.example.ui.WeatherStatusSquare
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.SkylineMayorTheme
import com.example.ui.theme.SlateSurfaceDark
import com.example.viewmodel.CityViewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val viewModel: CityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SkylineMayorTheme {
                SkylineMayorRoot(viewModel = viewModel)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.saveNowAsync()
    }
}

@Composable
fun SkylineMayorRoot(viewModel: CityViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val layoutDirection = if (state.settings.language == AppLanguage.ARABIC) {
        LayoutDirection.Rtl
    } else {
        LayoutDirection.Ltr
    }

    var showMissions by remember { mutableStateOf(false) }
    var showStorage by remember { mutableStateOf(false) }
    var showMusic by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showCityOverview by remember { mutableStateOf(false) }
    var showResidentEvent by remember { mutableStateOf(false) }
    var showResourceEditor by remember { mutableStateOf(false) }
    var orbitEnabled by remember { mutableStateOf(false) }
    var marketMode by remember { mutableStateOf(MarketViewMode.COMPACT) }
    var glViewRef by remember { mutableStateOf<CityGLSurfaceView?>(null) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, glViewRef) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> glViewRef?.onResume()
                Lifecycle.Event.ON_PAUSE -> {
                    glViewRef?.onPause()
                    viewModel.saveNowAsync()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Auto-clear transient status banner after 3 seconds
    LaunchedEffect(state.statusBanner) {
        if (state.statusBanner != null) {
            delay(3000L)
            viewModel.clearBanner()
        }
    }

    BackHandler(
        enabled = showMissions || showStorage || showMusic || showSettings || showCityOverview || showResidentEvent || showResourceEditor ||
            state.selectedCellCoords != null || state.toolMode != ToolMode.INSPECT
    ) {
        when {
            showMissions -> showMissions = false
            showStorage -> showStorage = false
            showMusic -> showMusic = false
            showSettings -> showSettings = false
            showCityOverview -> showCityOverview = false
            showResidentEvent -> showResidentEvent = false
            showResourceEditor -> showResourceEditor = false
            state.selectedCellCoords != null -> viewModel.clearSelection()
            state.toolMode != ToolMode.INSPECT -> viewModel.cancelPlacementMode()
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. Full-screen True 3D OpenGL ES 2.0 City World
            AndroidView(
                factory = { ctx ->
                    CityGLSurfaceView(ctx) { gx, gz ->
                        viewModel.onTileTapped(gx, gz)
                    }.also { glViewRef = it }
                },
                update = { view ->
                    view.updateGameState(state)
                    view.setOrbitMode(orbitEnabled)
                },
                modifier = Modifier.fillMaxSize()
            )

            // Brightness dimmer overlay if user lowers brightness below 1.0
            if (state.settings.brightness < 1.0f) {
                val dimAlpha = ((1.0f - state.settings.brightness) * 0.65f).coerceIn(0f, 0.55f)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = dimAlpha))
                )
            }

            // 2. Always-On Top-Left Weather Square + Top Mayor HUD Bar
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .windowInsetsPadding(WindowInsets.displayCutout)
                        .padding(start = 10.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WeatherStatusSquare(
                        state = state,
                        onToggleShortMode = { viewModel.toggleWeatherShortMode() },
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                        TopMayorHud(
                            state = state,
                            onEditResources = { showResourceEditor = true },
                            onOpenCityOverview = { showCityOverview = true },
                            onCycleSpeed = { viewModel.cycleSimulationSpeed() },
                            onOpenMissions = { showMissions = true },
                            onOpenStorage = { showStorage = true },
                            onOpenMusic = { showMusic = true },
                            onOpenSettings = { showSettings = true },
                            onToggleMusicPlay = { viewModel.toggleMusicPlayPause() },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 3. Transient Status / Auto-Save Resume Banner
            val bannerText = state.statusBanner
            AnimatedVisibility(
                visible = bannerText != null,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 112.dp)
            ) {
                Surface(
                    color = SlateSurfaceDark,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = bannerText ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = EmeraldAccent,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }

            // 4. Right Camera & Resident Event Column
            CameraControlColumn(
                orbitEnabled = orbitEnabled,
                hasPendingEvent = state.activeResidentEvent != null,
                onToggleOrbit = { orbitEnabled = !orbitEnabled },
                onRotateLeft = { glViewRef?.rotateCameraBy(-25f) },
                onRotateRight = { glViewRef?.rotateCameraBy(25f) },
                onResetCamera = { glViewRef?.resetCamera() },
                onOpenEvent = { showResidentEvent = true },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .windowInsetsPadding(WindowInsets.displayCutout)
                    .padding(bottom = 24.dp)
            )

            // 5. Bottom Area: Active Tool Banner + Selected Tile Inspector + Build Dock
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
            ) {
                val selectedCell = state.selectedCellCoords?.let { (sx, sz) ->
                    state.cells.find { it.x == sx && it.z == sz && it.building != null }
                }

                if (selectedCell != null && state.toolMode == ToolMode.INSPECT) {
                    SelectedCellInspectorCard(
                        state = state,
                        cell = selectedCell,
                        onClose = { viewModel.clearSelection() },
                        onRotateBuilding = { viewModel.rotateSelectedBuilding() },
                        onUpgrade = { viewModel.startUpgradeSelected() },
                        onUpgradeConnectedRoad = { viewModel.upgradeSelectedRoad(upgradeConnected = true) },
                        onCancelUpgrade = { viewModel.cancelUpgradeSelected() },
                        onCancelOrRefund = { viewModel.cancelSelectedBuilding() },
                        onStartCraft = { recipe -> viewModel.startCraftingSelected(recipe) },
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                } else {
                    Column(
                        modifier = Modifier.align(Alignment.BottomCenter),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ActiveToolBanner(
                            state = state,
                            onCancelMode = { viewModel.cancelPlacementMode() },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                        BottomBuildDock(
                            state = state,
                            marketMode = marketMode,
                            onChangeMarketMode = { marketMode = it },
                            onSelectCategory = { viewModel.selectCategory(it) },
                            onPickBuilding = { viewModel.startPlacingBuilding(it) },
                            onSetToolMode = { viewModel.setToolMode(it) },
                            onTriggerEvent = {
                                viewModel.triggerResidentEventNow()
                                showResidentEvent = true
                            }
                        )
                    }
                }
            }

            // 6. Modals
            if (showMissions) {
                MissionsDialog(
                    state = state,
                    onClaim = { viewModel.claimMission(it) },
                    onDismiss = { showMissions = false }
                )
            }

            if (showStorage) {
                StorageMarketDialog(
                    state = state,
                    onSellItem = { r, all -> viewModel.sellStoredItem(r, all) },
                    onDismiss = { showStorage = false }
                )
            }

            if (showMusic) {
                LocalMusicPlayerDialog(
                    state = state,
                    onAddSongs = { viewModel.addLocalSongs(it) },
                    onPlayTrack = { viewModel.playSongAt(it) },
                    onTogglePlayPause = { viewModel.toggleMusicPlayPause() },
                    onNext = { viewModel.nextSong() },
                    onPrev = { viewModel.prevSong() },
                    onRemoveSong = { viewModel.removeLocalSong(it) },
                    onDismiss = { showMusic = false }
                )
            }

            if (showResourceEditor) {
                CustomizeResourcesDialog(
                    state = state,
                    onCollectTaxBonus = { viewModel.collectBonusCoinsAndGems() },
                    onApplyCustomAmounts = { c, g -> viewModel.setCustomCoinsAndGems(c, g) },
                    onDismiss = { showResourceEditor = false }
                )
            }

            if (showCityOverview) {
                CityOverviewDialog(
                    state = state,
                    onTaxPolicyChange = { viewModel.updateTaxPolicy(it) },
                    onCollectTaxes = { viewModel.collectBonusCoinsAndGems() },
                    onUpgradeAllRoads = { viewModel.upgradeAllCityTwoWayRoads() },
                    onDismiss = { showCityOverview = false }
                )
            }

            if (showSettings) {
                SettingsDialog(
                    state = state,
                    onVolumeChange = { viewModel.updateSoundVolume(it) },
                    onSfxEnabledChange = { viewModel.updateSfxEnabled(it) },
                    onBrightnessChange = { viewModel.updateBrightness(it) },
                    onBrightRoadLinesAlwaysChange = { viewModel.updateBrightRoadLinesAlways(it) },
                    onLanguageChange = { viewModel.updateLanguage(it) },
                    onAutoDayNightChange = { viewModel.updateAutoDayNight(it) },
                    onTimeOfDayChange = { viewModel.setTimeOfDay(it) },
                    onSimSpeedChange = { viewModel.setSimulationSpeed(it) },
                    onTaxPolicyChange = { viewModel.updateTaxPolicy(it) },
                    onTrafficDensityChange = { viewModel.updateTrafficDensity(it) },
                    onCycleSeason = { viewModel.cycleSeason() },
                    onCityNameChange = { viewModel.updateCityName(it) },
                    onApplyCustomResources = { c, g -> viewModel.setCustomCoinsAndGems(c, g) },
                    onManualSave = { viewModel.saveNowAsync() },
                    onStartNewCity = { viewModel.startNewCity() },
                    onDismiss = { showSettings = false }
                )
            }

            val pendingEv = state.activeResidentEvent
            if (showResidentEvent && pendingEv != null) {
                ResidentEventDialog(
                    event = pendingEv,
                    state = state,
                    onChoose = { optA ->
                        viewModel.resolveResidentEvent(optA)
                        showResidentEvent = false
                    },
                    onDismiss = { showResidentEvent = false }
                )
            }
        }
    }
}
