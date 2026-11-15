package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.BuildingType
import com.example.model.CraftRecipe
import com.example.persistence.LocalSaveManager
import com.example.viewmodel.CityViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read app name from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Skyline Mayor", appName)
  }

  @Test
  fun `all build and craft durations stay between 30s and 5m max`() {
    BuildingType.entries.forEach { b ->
      if (!b.isRoad) {
        assertTrue("${b.id} buildTimeSec=${b.buildTimeSec}", b.buildTimeSec in 30..300)
      }
    }
    assertEquals(30, BuildingType.GROUND_HOUSE.buildTimeSec)
    CraftRecipe.entries.forEach { r ->
      assertTrue("${r.id} craftTimeSec=${r.craftTimeSec}", r.craftTimeSec in 30..300)
    }
  }

  @Test
  fun `auto save searches phone storage and resumes progress`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val vm = CityViewModel(app)
    val initialCoins = vm.uiState.value.coins
    vm.startPlacingBuilding(BuildingType.GROUND_HOUSE)
    vm.onTileTapped(2, 2)
    assertEquals(initialCoins - BuildingType.GROUND_HOUSE.coinCost, vm.uiState.value.coins)

    // Verify save file written & automatically discovered
    val mgr = LocalSaveManager(app)
    mgr.saveAutoSave(vm.uiState.value)
    val loaded = mgr.searchAndLoadAutoSave()
    assertNotNull(loaded)
    assertEquals(vm.uiState.value.coins, loaded!!.state.coins)
    assertTrue(loaded.state.cells.any { it.x == 2 && it.z == 2 && it.building?.type == BuildingType.GROUND_HOUSE })
  }

  @Test
  fun `customizing money and gems preserves all progress`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val vm = CityViewModel(app)
    vm.startPlacingBuilding(BuildingType.GROUND_HOUSE)
    vm.onTileTapped(3, 3)
    val cellsBefore = vm.uiState.value.cells.count { it.building != null }

    vm.setCustomCoinsAndGems(125_000L, 950)
    assertEquals(125_000L, vm.uiState.value.coins)
    assertEquals(950, vm.uiState.value.gems)
    assertEquals(cellsBefore, vm.uiState.value.cells.count { it.building != null })
  }

  @Test
  fun `all building exterior 3d meshes and animated vehicles generate cleanly`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val vm = CityViewModel(app)
    vm.setCustomCoinsAndGems(10_000_000L, 50_000)

    // Place a broad sample of non-road and road buildings on the grid
    var idx = 0
    for (type in BuildingType.entries) {
      val gx = idx % 14
      val gz = (idx / 14) + 1
      if (gz < 13) {
        vm.startPlacingBuilding(type)
        vm.onTileTapped(gx, gz)
      }
      idx++
    }

    val staticBuilder = com.example.engine3d.MeshBuilder()
    com.example.engine3d.CityMeshGenerators.buildStaticWorld(vm.uiState.value, staticBuilder)
    assertTrue("Static city mesh should contain vertices", staticBuilder.vertexCount() > 1000)

    val dynamicBuilder = com.example.engine3d.MeshBuilder()
    com.example.engine3d.AnimatedEntitiesRenderer().buildDynamicFrame(vm.uiState.value, 12.5f, dynamicBuilder)
    assertTrue("Dynamic animated entities should contain vertices", dynamicBuilder.vertexCount() > 500)
  }

  @Test
  fun `road upgrade from two way to double way deducts difference and refunds full total`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val vm = CityViewModel(app)
    vm.setCustomCoinsAndGems(5000L, 20)

    // Place a 2-way road at (2, 2) and adjacent at (2, 3)
    vm.startPlacingBuilding(BuildingType.ROAD_TWO_WAY)
    vm.onTileTapped(2, 2)
    vm.onTileTapped(2, 3)
    val coinsAfterBuild = vm.uiState.value.coins

    // Upgrade single tile with UPGRADE_ROAD tool
    vm.setToolMode(com.example.model.ToolMode.UPGRADE_ROAD)
    vm.onTileTapped(2, 2)
    val cell22 = vm.uiState.value.cells.find { it.x == 2 && it.z == 2 }
    assertEquals(BuildingType.ROAD_FOUR_LANE, cell22?.building?.type)
    assertEquals(coinsAfterBuild - 35L, vm.uiState.value.coins)

    // Cancel/demolish upgraded road recovers full $60
    vm.setToolMode(com.example.model.ToolMode.CANCEL_REFUND)
    vm.onTileTapped(2, 2)
    assertEquals(coinsAfterBuild - 35L + BuildingType.ROAD_FOUR_LANE.coinCost, vm.uiState.value.coins)
  }

  @Test
  fun `building rotation, new settings, and weather status persist cleanly across saves`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val vm = CityViewModel(app)
    vm.startPlacingBuilding(BuildingType.GROUND_HOUSE)
    vm.onTileTapped(4, 4)
    vm.rotateSelectedBuilding()

    vm.setSimulationSpeed(3)
    vm.updateTaxPolicy(com.example.model.TaxPolicy.HIGH)
    vm.updateTrafficDensity(com.example.model.TrafficDensity.HIGH)
    vm.updateSfxEnabled(false)
    vm.updateBrightRoadLinesAlways(true)
    vm.toggleWeatherShortMode()

    val mgr = LocalSaveManager(app)
    mgr.saveAutoSave(vm.uiState.value)
    val loaded = mgr.searchAndLoadAutoSave()!!

    val savedHouse = loaded.state.cells.find { it.x == 4 && it.z == 4 }?.building
    assertEquals(1, savedHouse?.rotation)
    assertEquals(3, loaded.state.settings.simulationSpeed)
    assertEquals(com.example.model.TaxPolicy.HIGH, loaded.state.settings.taxPolicy)
    assertEquals(com.example.model.TrafficDensity.HIGH, loaded.state.settings.trafficDensity)
    assertEquals(false, loaded.state.settings.sfxEnabled)
    assertEquals(true, loaded.state.settings.brightRoadLinesAlways)
    assertEquals(true, loaded.state.settings.weatherShortMode)
    assertEquals(vm.uiState.value.weather, loaded.state.weather)
    assertEquals(vm.uiState.value.weatherTempC, loaded.state.weatherTempC)
    assertEquals(vm.uiState.value.weatherHumidityPct, loaded.state.weatherHumidityPct)
  }
}
