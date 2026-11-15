package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Factory
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material.icons.filled.KeyboardDoubleArrowUp
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalPolice
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.heightIn
import com.example.i18n.AppStrings
import com.example.model.BuildCategory
import com.example.model.BuildingType
import com.example.model.GameState
import com.example.model.ToolMode
import com.example.ui.theme.AmberGold
import com.example.ui.theme.AmethystGem
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.SkyBluePrimary
import com.example.ui.theme.SlateBorderDark
import com.example.ui.theme.SlateSurfaceDark

enum class MarketViewMode {
    COLLAPSED,
    COMPACT,
    EXPANDED
}

@Composable
fun WeatherStatusSquare(
    state: GameState,
    onToggleShortMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isShort = state.settings.weatherShortMode
    val boxSize = if (isShort) 48.dp else 58.dp
    val borderColor = when (state.weather) {
        com.example.model.WeatherCondition.SUNNY -> AmberGold.copy(alpha = 0.7f)
        com.example.model.WeatherCondition.CLOUDY -> SlateBorderDark
        com.example.model.WeatherCondition.RAINY -> SkyBluePrimary.copy(alpha = 0.8f)
    }

    Surface(
        color = SlateSurfaceDark,
        shape = RoundedCornerShape(if (isShort) 12.dp else 14.dp),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, borderColor),
        modifier = modifier
            .size(boxSize)
            .clip(RoundedCornerShape(if (isShort) 12.dp else 14.dp))
            .clickable { onToggleShortMode() }
            .testTag("weather_status_square")
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isShort) {
                Text(
                    text = state.weather.twoEmojis,
                    fontSize = 16.sp,
                    maxLines = 1
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(2.dp)
                ) {
                    Text(
                        text = state.weather.primaryEmoji,
                        fontSize = 17.sp,
                        lineHeight = 18.sp
                    )
                    Text(
                        text = "${state.weatherTempC}°",
                        fontSize = 12.sp,
                        lineHeight = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1
                    )
                    Text(
                        text = "${state.weatherHumidityPct}%",
                        fontSize = 9.sp,
                        lineHeight = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF93C5FD),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun TopMayorHud(
    state: GameState,
    onEditResources: () -> Unit,
    onOpenCityOverview: () -> Unit,
    onCycleSpeed: () -> Unit,
    onOpenMissions: () -> Unit,
    onOpenStorage: () -> Unit,
    onOpenMusic: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleMusicPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = state.settings.language
    val isAr = AppStrings.isAr(lang)
    val isNight = state.timeOfDay < 0.23f || state.timeOfDay > 0.77f
    val readyMissions = state.missions.count { !it.claimed && it.progress >= it.targetCount }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(end = 12.dp, top = 4.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Row 1: Level, Coins, Gems, Pop, Quick Action Icons
        Surface(
            color = SlateSurfaceDark,
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorderDark)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Level & XP
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(SkyBluePrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${state.level}",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFF082F49),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(modifier = Modifier.width(72.dp)) {
                        Text(
                            text = state.cityName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        LinearProgressIndicator(
                            progress = { (state.xp.toFloat() / state.xpForNextLevel().toFloat()).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = SkyBluePrimary,
                            trackColor = Color(0xFF334155)
                        )
                    }
                }

                // Coins (Tap to collect tax bonus or manually customize Money & Gems!)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x33FBBF24))
                        .clickable { onEditResources() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("coins_badge"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.AttachMoney, contentDescription = "Coins", tint = AmberGold, modifier = Modifier.size(16.dp))
                    Text(
                        text = "${state.coins}",
                        style = MaterialTheme.typography.labelLarge,
                        color = AmberGold
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Gems (Tap to manually customize Money & Gems!)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x33A855F7))
                        .clickable { onEditResources() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("gems_badge"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Diamond, contentDescription = "Gems", tint = AmethystGem, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "${state.gems}",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFFE9D5FF)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Settings Button
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("settings_button")
                ) {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }

        // Row 2: Secondary Stats + Missions, Storage, Music Pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatChip(
                icon = Icons.Filled.Groups,
                tint = SkyBluePrimary,
                text = "${AppStrings.popLabel(lang)}: ${state.population} (${state.happiness}%)",
                onClick = onOpenCityOverview,
                testTag = "city_overview_button"
            )
            StatChip(
                icon = Icons.Filled.AttachMoney,
                tint = EmeraldAccent,
                text = AppStrings.taxRateLabel(lang, state.taxPerMinute),
                onClick = onOpenCityOverview
            )
            val spd = state.settings.simulationSpeed
            StatChip(
                icon = Icons.Filled.Speed,
                tint = if (spd == 0) CoralAlert else AmberGold,
                text = if (spd == 0) (if (isAr) "متوقف" else "Paused") else "${spd}x",
                onClick = onCycleSpeed,
                testTag = "speed_toggle_button"
            )
            StatChip(
                icon = if (isNight) Icons.Filled.NightsStay else Icons.Filled.WbSunny,
                tint = if (isNight) Color(0xFF93C5FD) else AmberGold,
                text = AppStrings.seasonName(lang, state.season)
            )
            StatChip(
                icon = Icons.Filled.Inventory2,
                tint = Color(0xFF38BDF8),
                text = "${state.totalStoredItems()}/999K",
                onClick = onOpenStorage,
                testTag = "storage_button"
            )
            StatChip(
                icon = Icons.Filled.Assignment,
                tint = if (readyMissions > 0) EmeraldAccent else AmberGold,
                text = if (isAr) "المهام ($readyMissions)" else "Missions ($readyMissions)",
                onClick = onOpenMissions,
                testTag = "missions_button"
            )
            StatChip(
                icon = if (state.isMusicPlaying) Icons.Filled.Pause else Icons.Filled.MusicNote,
                tint = Color(0xFFF472B6),
                text = if (state.playlist.isEmpty()) {
                    if (isAr) "مشغل الأغاني" else "Music"
                } else {
                    state.playlist.getOrNull(state.currentTrackIndex)?.title?.take(12)
                        ?: if (isAr) "أغاني (${state.playlist.size})" else "Music (${state.playlist.size})"
                },
                onClick = onOpenMusic,
                secondaryActionIcon = if (state.playlist.isNotEmpty()) {
                    if (state.isMusicPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow
                } else null,
                onSecondaryClick = onToggleMusicPlay,
                testTag = "music_button"
            )
        }
    }
}

@Composable
private fun StatChip(
    icon: ImageVector,
    tint: Color,
    text: String,
    onClick: (() -> Unit)? = null,
    secondaryActionIcon: ImageVector? = null,
    onSecondaryClick: (() -> Unit)? = null,
    testTag: String? = null
) {
    Surface(
        color = SlateSurfaceDark,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorderDark),
        modifier = Modifier
            .let { if (testTag != null) it.testTag(testTag) else it }
            .let { if (onClick != null) it.clickable { onClick() } else it }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = text, style = MaterialTheme.typography.labelSmall, color = Color.White, maxLines = 1)
            if (secondaryActionIcon != null && onSecondaryClick != null) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    secondaryActionIcon,
                    contentDescription = "Play/Pause",
                    tint = Color.White,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { onSecondaryClick() }
                )
            }
        }
    }
}

@Composable
fun CameraControlColumn(
    orbitEnabled: Boolean,
    hasPendingEvent: Boolean,
    onToggleOrbit: () -> Unit,
    onRotateLeft: () -> Unit,
    onRotateRight: () -> Unit,
    onResetCamera: () -> Unit,
    onOpenEvent: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(end = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (hasPendingEvent) {
            Surface(
                color = CoralAlert,
                shape = CircleShape,
                modifier = Modifier
                    .size(48.dp)
                    .clickable { onOpenEvent() }
                    .testTag("resident_event_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Celebration, contentDescription = "Resident Event", tint = Color.White)
                }
            }
        }

        CameraBtn(
            icon = Icons.Filled.Explore,
            active = orbitEnabled,
            desc = "Toggle 3D Orbit",
            onClick = onToggleOrbit
        )
        CameraBtn(icon = Icons.Filled.RotateLeft, active = false, desc = "Rotate Left", onClick = onRotateLeft)
        CameraBtn(icon = Icons.Filled.RotateRight, active = false, desc = "Rotate Right", onClick = onRotateRight)
        CameraBtn(icon = Icons.Filled.CenterFocusStrong, active = false, desc = "Reset Camera", onClick = onResetCamera)
    }
}

@Composable
private fun CameraBtn(icon: ImageVector, active: Boolean, desc: String, onClick: () -> Unit) {
    Surface(
        color = if (active) SkyBluePrimary else SlateSurfaceDark,
        shape = CircleShape,
        border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorderDark),
        modifier = Modifier
            .size(44.dp)
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = desc,
                tint = if (active) Color(0xFF082F49) else Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun ActiveToolBanner(
    state: GameState,
    onCancelMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = state.settings.language
    val isAr = AppStrings.isAr(lang)
    val show = state.toolMode != ToolMode.INSPECT

    AnimatedVisibility(visible = show, modifier = modifier) {
        val text = when (state.toolMode) {
            ToolMode.BUILD -> {
                val b = state.selectedBuildType
                if (b != null) {
                    val cost = if (b.gemCost > 0) "${b.gemCost}◆" else "$${b.coinCost}"
                    val time = if (b.buildTimeSec > 0) AppStrings.formatDuration(b.buildTimeSec) else if (isAr) "فوري" else "Instant"
                    if (isAr) "اضغط على الخريطة لبناء: ${AppStrings.buildingName(lang, b)} ($cost • $time)"
                    else "Tap map to build ${AppStrings.buildingName(lang, b)} ($cost • $time)"
                } else ""
            }
            ToolMode.UPGRADE_ROAD -> if (isAr) "اضغط على أي طريق ذهاب وإياب (١+١) لترقيته إلى مزدوج (٢+٢)" else "Tap any 2-Way (1+1) road tile to upgrade to Double-Way (2+2)"
            ToolMode.LOWER_GROUND -> if (isAr) "اضغط لخفض الأرض وتحويلها لمصدر ماء (بركة/بحيرة/نهر)" else "Tap land to lower ground into water (pond/lake/river)"
            ToolMode.RAISE_GROUND -> if (isAr) "اضغط لرفع الأرض وإزالة الماء" else "Tap water to raise ground back to buildable land"
            ToolMode.CANCEL_REFUND -> if (isAr) "اضغط على أي مبنى أو طريق لإلغائه واسترداد كامل النقود" else "Tap any building or road to cancel & recover 100% money"
            ToolMode.INSPECT -> ""
        }

        Surface(
            color = if (state.toolMode == ToolMode.CANCEL_REFUND) Color(0xE67F1D1D) else SlateSurfaceDark,
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, SkyBluePrimary)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onCancelMode,
                    colors = ButtonDefaults.buttonColors(containerColor = CoralAlert),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp).testTag("cancel_tool_button")
                ) {
                    Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isAr) "إلغاء" else "Cancel", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun BottomBuildDock(
    state: GameState,
    marketMode: MarketViewMode,
    onChangeMarketMode: (MarketViewMode) -> Unit,
    onSelectCategory: (BuildCategory) -> Unit,
    onPickBuilding: (BuildingType) -> Unit,
    onSetToolMode: (ToolMode) -> Unit,
    onTriggerEvent: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = state.settings.language
    val isAr = AppStrings.isAr(lang)

    if (marketMode == MarketViewMode.COLLAPSED) {
        // Ultra-short floating pill so 90%+ of the 3D city is completely clear!
        Surface(
            color = SlateSurfaceDark,
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, SkyBluePrimary),
            modifier = modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 8.dp)
                .clickable { onChangeMarketMode(MarketViewMode.COMPACT) }
                .testTag("expand_market_pill")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.Storefront, contentDescription = null, tint = SkyBluePrimary, modifier = Modifier.size(18.dp))
                Text(
                    text = if (isAr) "سوق المباني (إظهار)" else "Buildings Market (Show)",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White
                )
                Icon(Icons.Filled.ExpandLess, contentDescription = "Show Market", tint = SkyBluePrimary, modifier = Modifier.size(20.dp))
            }
        }
        return
    }

    Surface(
        color = SlateSurfaceDark,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorderDark),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(top = 5.dp, bottom = 5.dp)
        ) {
            // Top bar: Category Tabs + Shorten / Expand toggle buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(BuildCategory.entries) { cat ->
                        val selected = state.activeCategory == cat
                        Surface(
                            color = if (selected) SkyBluePrimary else Color(0xFF1E293B),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .clickable { onSelectCategory(cat) }
                                .testTag("cat_${cat.name}")
                        ) {
                            Text(
                                text = AppStrings.categoryName(lang, cat),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) Color(0xFF082F49) else Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Expand / Compact toggle button
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .clickable {
                            onChangeMarketMode(
                                if (marketMode == MarketViewMode.EXPANDED) MarketViewMode.COMPACT
                                else MarketViewMode.EXPANDED
                            )
                        }
                        .testTag("toggle_expand_market")
                ) {
                    Icon(
                        imageVector = if (marketMode == MarketViewMode.EXPANDED) {
                            Icons.Filled.KeyboardDoubleArrowDown
                        } else {
                            Icons.Filled.KeyboardDoubleArrowUp
                        },
                        contentDescription = if (marketMode == MarketViewMode.EXPANDED) "Compact Market" else "Expand Market",
                        tint = SkyBluePrimary,
                        modifier = Modifier.padding(6.dp).size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Shorten / Hide Market button for clear city view
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .clickable { onChangeMarketMode(MarketViewMode.COLLAPSED) }
                        .testTag("collapse_market_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.ExpandMore,
                        contentDescription = "Hide Market",
                        tint = Color(0xFFCBD5E1),
                        modifier = Modifier.padding(6.dp).size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(5.dp))

            if (state.activeCategory == BuildCategory.TERRAIN_TOOLS) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        TerrainToolCard(
                            icon = Icons.Filled.Upgrade,
                            tint = SkyBluePrimary,
                            title = if (isAr) "ترقية طريق لمزدوج (٢+٢)" else "Upgrade Road (1+1→2+2)",
                            selected = state.toolMode == ToolMode.UPGRADE_ROAD,
                            onClick = { onSetToolMode(ToolMode.UPGRADE_ROAD) }
                        )
                    }
                    item {
                        TerrainToolCard(
                            icon = Icons.Filled.ArrowDownward,
                            tint = SkyBluePrimary,
                            title = if (isAr) "خفض الأرض (ماء)" else "Lower Ground (Water)",
                            selected = state.toolMode == ToolMode.LOWER_GROUND,
                            onClick = { onSetToolMode(ToolMode.LOWER_GROUND) }
                        )
                    }
                    item {
                        TerrainToolCard(
                            icon = Icons.Filled.ArrowUpward,
                            tint = EmeraldAccent,
                            title = if (isAr) "رفع الأرض (يابسة)" else "Raise Ground (Land)",
                            selected = state.toolMode == ToolMode.RAISE_GROUND,
                            onClick = { onSetToolMode(ToolMode.RAISE_GROUND) }
                        )
                    }
                    item {
                        TerrainToolCard(
                            icon = Icons.AutoMirrored.Filled.Undo,
                            tint = CoralAlert,
                            title = if (isAr) "إلغاء واسترداد النقود" else "Cancel & Refund",
                            selected = state.toolMode == ToolMode.CANCEL_REFUND,
                            onClick = { onSetToolMode(ToolMode.CANCEL_REFUND) }
                        )
                    }
                    item {
                        TerrainToolCard(
                            icon = Icons.Filled.Celebration,
                            tint = AmberGold,
                            title = if (isAr) "حدث سكاني" else "Resident Event",
                            selected = false,
                            onClick = onTriggerEvent
                        )
                    }
                }
            } else {
                val buildings = BuildingType.entries.filter { it.category == state.activeCategory }
                if (marketMode == MarketViewMode.EXPANDED) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 112.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 178.dp)
                    ) {
                        items(buildings, key = { it.id }) { b ->
                            val selected = state.toolMode == ToolMode.BUILD && state.selectedBuildType == b
                            BuildingCatalogCard(
                                type = b,
                                name = AppStrings.buildingName(lang, b),
                                selected = selected,
                                onClick = {
                                    onPickBuilding(b)
                                    onChangeMarketMode(MarketViewMode.COMPACT)
                                }
                            )
                        }
                    }
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(buildings, key = { it.id }) { b ->
                            val selected = state.toolMode == ToolMode.BUILD && state.selectedBuildType == b
                            BuildingCatalogCard(
                                type = b,
                                name = AppStrings.buildingName(lang, b),
                                selected = selected,
                                onClick = { onPickBuilding(b) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TerrainToolCard(
    icon: ImageVector,
    tint: Color,
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) Color(0xFF0C4A6E) else Color(0xFF1E293B),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, if (selected) SkyBluePrimary else SlateBorderDark),
        modifier = Modifier
            .width(136.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = title, tint = tint, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BuildingCatalogCard(
    type: BuildingType,
    name: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val icon = iconForBuilding(type)
    val costText = if (type.gemCost > 0) "${type.gemCost}◆" else "$${type.coinCost}"
    val costColor = if (type.gemCost > 0) AmethystGem else AmberGold
    val timeText = if (type.buildTimeSec > 0) AppStrings.formatDuration(type.buildTimeSec) else "0s"

    Surface(
        color = if (selected) Color(0xFF0C4A6E) else Color(0xFF1E293B),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, if (selected) SkyBluePrimary else SlateBorderDark),
        modifier = Modifier
            .width(114.dp)
            .clickable { onClick() }
            .testTag("build_${type.id}")
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = name, tint = SkyBluePrimary, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = costText, style = MaterialTheme.typography.labelSmall, color = costColor, fontWeight = FontWeight.Bold)
                Text(text = "• $timeText", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
            }
        }
    }
}

fun iconForBuilding(type: BuildingType): ImageVector = when (type) {
    BuildingType.ROAD_TWO_WAY,
    BuildingType.ROAD_FOUR_LANE,
    BuildingType.ROAD_HIGHWAY -> Icons.Filled.Route
    BuildingType.FUEL_STATION -> Icons.Filled.LocalGasStation
    BuildingType.HIGHWAY_TOLL -> Icons.Filled.AttachMoney
    BuildingType.BUS_STATION,
    BuildingType.TAXI_STATION -> Icons.Filled.DirectionsBus
    BuildingType.TRAM_STATION,
    BuildingType.TRAIN_STATION,
    BuildingType.METRO_STATION -> Icons.Filled.Train
    BuildingType.SEAPORT -> Icons.Filled.WaterDrop
    BuildingType.AIRPORT -> Icons.Filled.Flight
    BuildingType.GROUND_HOUSE -> Icons.Filled.Home
    BuildingType.SMALL_APARTMENT,
    BuildingType.LARGE_APARTMENT,
    BuildingType.LUXURY_APARTMENT,
    BuildingType.SKYSCRAPER -> Icons.Filled.Apartment
    BuildingType.SHOP,
    BuildingType.RESTAURANT -> Icons.Filled.ShoppingBag
    BuildingType.EMPLOYMENT_OFFICE,
    BuildingType.COMPANY,
    BuildingType.LUXURY_ADMIN_BUILDING -> Icons.Filled.AccountBalance
    BuildingType.SCHOOL -> Icons.Filled.School
    BuildingType.WATER_PLANT -> Icons.Filled.WaterDrop
    BuildingType.FIRE_STATION,
    BuildingType.HOSPITAL -> Icons.Filled.LocalHospital
    BuildingType.POLICE_STATION,
    BuildingType.PRISON -> Icons.Filled.LocalPolice
    BuildingType.AMUSEMENT_PARK,
    BuildingType.WATER_AMUSEMENT_PARK -> Icons.Filled.Celebration
    BuildingType.CROP_FIELD,
    BuildingType.LIVESTOCK_PASTURE -> Icons.Filled.Agriculture
    BuildingType.FOREST_GROVE,
    BuildingType.COSMETIC_TREE_GARDEN -> Icons.Filled.Forest
    BuildingType.FOOD_FACTORY,
    BuildingType.CLOTHING_FACTORY,
    BuildingType.EQUIPMENT_FACTORY,
    BuildingType.MEDICINE_FACTORY,
    BuildingType.RECYCLING_FACTORY,
    BuildingType.TRANSPORT_FACTORY -> Icons.Filled.Factory
    BuildingType.LANDMARK_EIFFEL,
    BuildingType.LANDMARK_BURJ_KHALIFA,
    BuildingType.LANDMARK_STATUE_LIBERTY,
    BuildingType.LANDMARK_BIG_BEN,
    BuildingType.LANDMARK_COLOSSEUM,
    BuildingType.LANDMARK_PYRAMID -> Icons.Filled.Star
    BuildingType.COSMETIC_PARK_BENCH,
    BuildingType.COSMETIC_FOUNTAIN,
    BuildingType.COSMETIC_ARCH -> Icons.Filled.Park
}
