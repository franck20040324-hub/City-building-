package com.example.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.i18n.AppStrings
import com.example.model.CraftRecipe
import com.example.model.GameState
import com.example.model.GridCell
import com.example.model.MissionPeriod
import com.example.model.ResidentEvent
import com.example.ui.theme.AmberGold
import com.example.ui.theme.AmethystGem
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.SkyBluePrimary
import com.example.ui.theme.SlateBorderDark
import com.example.ui.theme.SlateCardDark

@Composable
fun SelectedCellInspectorCard(
    state: GameState,
    cell: GridCell,
    onClose: () -> Unit,
    onRotateBuilding: () -> Unit,
    onUpgrade: () -> Unit,
    onUpgradeConnectedRoad: () -> Unit,
    onCancelUpgrade: () -> Unit,
    onCancelOrRefund: () -> Unit,
    onStartCraft: (CraftRecipe) -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = state.settings.language
    val isAr = AppStrings.isAr(lang)
    val b = cell.building ?: return

    Surface(
        color = SlateCardDark,
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorderDark),
        modifier = modifier
            .widthIn(max = 540.dp)
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = AppStrings.buildingName(lang, b.type),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        text = if (b.type.isRoad) {
                            when (b.type) {
                                com.example.model.BuildingType.ROAD_TWO_WAY ->
                                    if (isAr) "مسار واحد ذهاب + مسار واحد إياب • قابل للترقية إلى مزدوج"
                                    else "1+1 Opposite Lanes • Upgradeable to Double-Way (2+2)"
                                com.example.model.BuildingType.ROAD_FOUR_LANE ->
                                    if (isAr) "طريق مزدوج داخل المدينة (٢+٢ حارة + إنارة ليلية)"
                                    else "City Double-Way (2+2 Lanes + Night Lights)"
                                else ->
                                    if (isAr) "طريق سريع ٣ حارات بكل اتجاه + مسار طوارئ"
                                    else "3-Lane Highway Each Way + Emergency Shoulder"
                            }
                        } else if (isAr) {
                            "المستوى ${b.level}/${b.type.maxLevel} • السكان: ${b.currentPopulation()} • الضريبة: +${b.currentTaxIncome()}"
                        } else {
                            "Level ${b.level}/${b.type.maxLevel} • Pop: ${b.currentPopulation()} • Tax: +${b.currentTaxIncome()}"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFCBD5E1)
                    )
                }
                if (!b.type.isRoad && !b.isConstructing) {
                    IconButton(onClick = onRotateBuilding, modifier = Modifier.testTag("rotate_building_button")) {
                        Icon(Icons.Filled.RotateRight, contentDescription = "Rotate 90°", tint = SkyBluePrimary)
                    }
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            // Active Construction Banner + Cancel Construction (100% Refund)
            if (b.isConstructing) {
                val total = b.constructionTotalSec.coerceAtLeast(1)
                val pct = 1f - (b.constructionRemainingSec.toFloat() / total.toFloat())
                Text(
                    text = if (isAr) "قيد البناء: متبقي ${AppStrings.formatDuration(b.constructionRemainingSec)}"
                    else "Constructing: ${AppStrings.formatDuration(b.constructionRemainingSec)} left",
                    style = MaterialTheme.typography.labelLarge,
                    color = AmberGold
                )
                LinearProgressIndicator(progress = { pct.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth(), color = AmberGold)
            }

            // Active Upgrade Banner + Cancel Upgrade (100% Refund)
            if (b.isUpgrading) {
                val total = b.upgradeTotalSec.coerceAtLeast(1)
                val pct = 1f - (b.upgradeRemainingSec.toFloat() / total.toFloat())
                Text(
                    text = if (isAr) "جاري الترقية: متبقي ${AppStrings.formatDuration(b.upgradeRemainingSec)}"
                    else "Upgrading: ${AppStrings.formatDuration(b.upgradeRemainingSec)} left",
                    style = MaterialTheme.typography.labelLarge,
                    color = SkyBluePrimary
                )
                LinearProgressIndicator(progress = { pct.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth(), color = SkyBluePrimary)
                Button(
                    onClick = onCancelUpgrade,
                    colors = ButtonDefaults.buttonColors(containerColor = CoralAlert),
                    modifier = Modifier.fillMaxWidth().testTag("cancel_upgrade_button")
                ) {
                    Text(
                        if (isAr) "إلغاء الترقية واسترداد $${b.upgradePaidCoins}"
                        else "Cancel Upgrade (Refund $${b.upgradePaidCoins})"
                    )
                }
            }

            // Factory Crafting Panel (if this building is a Factory)
            val recipes = CraftRecipe.forFactory(b.type)
            if (!b.isConstructing && recipes.isNotEmpty()) {
                val activeRecipe = b.craftingRecipe
                if (activeRecipe != null) {
                    val pct = 1f - (b.craftRemainingSec.toFloat() / b.craftTotalSec.coerceAtLeast(1).toFloat())
                    Text(
                        text = if (isAr) "يصنع: ${AppStrings.recipeName(lang, activeRecipe)} (${AppStrings.formatDuration(b.craftRemainingSec)})"
                        else "Crafting ${AppStrings.recipeName(lang, activeRecipe)} (${AppStrings.formatDuration(b.craftRemainingSec)})",
                        style = MaterialTheme.typography.labelLarge,
                        color = EmeraldAccent
                    )
                    LinearProgressIndicator(progress = { pct.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth(), color = EmeraldAccent)
                } else {
                    Text(
                        text = if (isAr) "إنتاج المصنع (الحد الأقصى ٥ دقائق):" else "Factory Production (Max 5m):",
                        style = MaterialTheme.typography.labelLarge,
                        color = SkyBluePrimary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        recipes.forEach { rec ->
                            OutlinedButton(
                                onClick = { onStartCraft(rec) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(AppStrings.recipeName(lang, rec), style = MaterialTheme.typography.labelSmall, color = Color.White)
                                    Text(
                                        "$${rec.inputCost} • ${AppStrings.formatDuration(rec.craftTimeSec)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AmberGold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Action Buttons: Road Upgrade / Building Upgrade & Cancel/Refund
            if (b.canUpgradeRoad()) {
                val roadCost = b.roadUpgradeCost()
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = onUpgrade,
                        colors = ButtonDefaults.buttonColors(containerColor = SkyBluePrimary),
                        modifier = Modifier.weight(1f).testTag("upgrade_road_button")
                    ) {
                        Icon(Icons.Filled.Upgrade, contentDescription = null, tint = Color(0xFF082F49))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isAr) "ترقية لمزدوج ($$roadCost)" else "Upgrade 2+2 ($$roadCost)",
                            color = Color(0xFF082F49),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    OutlinedButton(
                        onClick = onUpgradeConnectedRoad,
                        modifier = Modifier.weight(1f).testTag("upgrade_connected_road_button")
                    ) {
                        Text(
                            text = if (isAr) "ترقية الشارع المتصل" else "Upgrade Full Street",
                            color = SkyBluePrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                if (!b.canUpgradeRoad() && !b.isConstructing && !b.isUpgrading && b.level < b.type.maxLevel) {
                    val cost = b.nextUpgradeCost()
                    val dur = AppStrings.formatDuration(b.nextUpgradeDurationSec())
                    Button(
                        onClick = onUpgrade,
                        colors = ButtonDefaults.buttonColors(containerColor = SkyBluePrimary),
                        modifier = Modifier.weight(1f).testTag("upgrade_building_button")
                    ) {
                        Icon(Icons.Filled.Upgrade, contentDescription = null, tint = Color(0xFF082F49))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isAr) "ترقية ($cost • $dur)" else "Upgrade ($cost • $dur)",
                            color = Color(0xFF082F49),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Button(
                    onClick = onCancelOrRefund,
                    colors = ButtonDefaults.buttonColors(containerColor = CoralAlert),
                    modifier = Modifier.weight(1f).testTag("cancel_purchase_button")
                ) {
                    Icon(Icons.Filled.DeleteOutline, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (b.isConstructing) {
                            if (isAr) "إلغاء البناء" else "Cancel Build"
                        } else {
                            if (isAr) "إلغاء واسترداد" else "Refund 100%"
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MissionsDialog(
    state: GameState,
    onClaim: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val lang = state.settings.language
    val isAr = AppStrings.isAr(lang)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = SlateCardDark,
            shape = RoundedCornerShape(22.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorderDark)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (isAr) "المهام اليومية والأسبوعية" else "Daily & Weekly Missions",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier.heightIn(max = 220.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.missions, key = { it.id }) { m ->
                        val ready = m.progress >= m.targetCount && !m.claimed
                        val periodTag = if (m.period == MissionPeriod.DAILY) {
                            if (isAr) "يومي" else "DAILY"
                        } else {
                            if (isAr) "أسبوعي" else "WEEKLY"
                        }
                        Surface(
                            color = Color(0xFF0F172A),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (ready) EmeraldAccent else SlateBorderDark)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "[$periodTag] ${if (isAr) m.titleAr else m.titleEn}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "${m.progress}/${m.targetCount}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = SkyBluePrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { (m.progress.toFloat() / m.targetCount.toFloat()).coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxWidth(),
                                    color = if (ready) EmeraldAccent else SkyBluePrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "+$${m.rewardCoins} • +${m.rewardGems}◆ • +${m.rewardXp} XP",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AmberGold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (ready) {
                                        Button(
                                            onClick = { onClaim(m.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent)
                                        ) {
                                            Text(if (isAr) "استلام" else "Claim", color = Color(0xFF022C22), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StorageMarketDialog(
    state: GameState,
    onSellItem: (CraftRecipe, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val lang = state.settings.language
    val isAr = AppStrings.isAr(lang)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = SlateCardDark,
            shape = RoundedCornerShape(22.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorderDark)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isAr) "مستودع المدينة العملاق" else "City Mega Storage",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                        Text(
                            text = "${state.totalStoredItems()} / 999,999",
                            style = MaterialTheme.typography.labelLarge,
                            color = SkyBluePrimary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                val itemsList = state.storageInventory.entries.toList()
                if (itemsList.isEmpty()) {
                    Text(
                        text = if (isAr) "المخزن فارغ حالياً. أنتج بضائع من المصانع وبعها هنا!"
                        else "Storage is currently empty. Craft goods in your factories and sell them here for easy coins!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFCBD5E1),
                        modifier = Modifier.padding(vertical = 20.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 220.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(itemsList, key = { it.key.id }) { (recipe, qty) ->
                            Surface(color = Color(0xFF0F172A), shape = RoundedCornerShape(14.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${AppStrings.recipeName(lang, recipe)} x$qty",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "$${recipe.sellValue} ${if (isAr) "للقطعة" else "each"}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = AmberGold
                                        )
                                    }
                                    Button(
                                        onClick = { onSellItem(recipe, true) },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent)
                                    ) {
                                        Text(
                                            text = if (isAr) "بيع الكل ($${recipe.sellValue * qty})" else "Sell All ($${recipe.sellValue * qty})",
                                            color = Color(0xFF022C22),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ResidentEventDialog(
    event: ResidentEvent,
    state: GameState,
    onChoose: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val lang = state.settings.language
    val isAr = AppStrings.isAr(lang)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = SlateCardDark,
            shape = RoundedCornerShape(22.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, AmberGold)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (isAr) event.titleAr else event.titleEn,
                    style = MaterialTheme.typography.titleLarge,
                    color = AmberGold
                )
                Text(
                    text = if (isAr) event.descriptionAr else event.descriptionEn,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
                Button(
                    onClick = { onChoose(true) },
                    enabled = state.coins >= event.optionACostCoins,
                    colors = ButtonDefaults.buttonColors(containerColor = SkyBluePrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isAr) event.optionAAr else event.optionAEn,
                            color = Color(0xFF082F49),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "-$${event.optionACostCoins} -> +$${event.optionARewardCoins}, +${event.optionARewardGems}◆, +${event.optionARewardHappiness}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF082F49)
                        )
                    }
                }
                OutlinedButton(
                    onClick = { onChoose(false) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isAr) event.optionBAr else event.optionBEn,
                            color = Color.White
                        )
                        Text(
                            text = "+$${event.optionBRewardCoins}, +${event.optionBRewardXp} XP, +${event.optionBRewardHappiness}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = AmethystGem
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CityOverviewDialog(
    state: GameState,
    onTaxPolicyChange: (com.example.model.TaxPolicy) -> Unit,
    onCollectTaxes: () -> Unit,
    onUpgradeAllRoads: () -> Unit,
    onDismiss: () -> Unit
) {
    val lang = state.settings.language
    val isAr = AppStrings.isAr(lang)
    val homes = state.cells.count { it.building?.categoryOrNull() == com.example.model.BuildCategory.RESIDENTIAL }
    val civic = state.cells.count { it.building?.categoryOrNull() == com.example.model.BuildCategory.CIVIC_SAFETY }
    val factories = state.cells.count { it.building?.categoryOrNull() == com.example.model.BuildCategory.FACTORIES_FARMS }
    val upgradeableRoads = state.cells.count { it.building?.canUpgradeRoad() == true }
    val doubleWayRoads = state.cells.count { it.building?.type == com.example.model.BuildingType.ROAD_FOUR_LANE }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = SlateCardDark,
            shape = RoundedCornerShape(22.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorderDark)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (isAr) "نظرة عامة على المدينة والميزانية" else "City Overview & Budget",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Surface(color = Color(0xFF0F172A), shape = RoundedCornerShape(14.dp)) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (isAr) "السكان: ${state.population} • السعادة: ${state.happiness}% • الضريبة: +${state.taxPerMinute}/دقيقة"
                            else "Population: ${state.population} • Happiness: ${state.happiness}% • Tax: +${state.taxPerMinute}/min",
                            style = MaterialTheme.typography.bodyMedium,
                            color = EmeraldAccent
                        )
                        Text(
                            text = if (isAr) "المساكن: $homes • الخدمات: $civic • المصانع والمزارع: $factories"
                            else "Homes: $homes • Civic Services: $civic • Factories & Farms: $factories",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFCBD5E1)
                        )
                        Text(
                            text = if (isAr) "طرق مزدوجة (٢+٢): $doubleWayRoads • طرق قابلة للترقية: $upgradeableRoads"
                            else "Double-Way Roads (2+2): $doubleWayRoads • Upgradeable 2-Way Roads: $upgradeableRoads",
                            style = MaterialTheme.typography.labelSmall,
                            color = SkyBluePrimary
                        )
                    }
                }

                Text(
                    text = if (isAr) "سياسة الضرائب:" else "Tax Policy:",
                    style = MaterialTheme.typography.labelLarge,
                    color = AmberGold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    com.example.model.TaxPolicy.entries.forEach { policy ->
                        val selected = state.settings.taxPolicy == policy
                        val label = when (policy) {
                            com.example.model.TaxPolicy.LOW -> if (isAr) "منخفض (+سعادة)" else "Low (+Happy)"
                            com.example.model.TaxPolicy.BALANCED -> if (isAr) "متوازن" else "Balanced"
                            com.example.model.TaxPolicy.HIGH -> if (isAr) "مرتفع (+دخل)" else "High (+Coins)"
                        }
                        Button(
                            onClick = { onTaxPolicyChange(policy) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selected) SkyBluePrimary else Color(0xFF0F172A)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(label, color = if (selected) Color(0xFF082F49) else Color.White, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = onCollectTaxes,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isAr) "تحصيل الضرائب الآن" else "Collect Taxes", color = Color(0xFF022C22), fontWeight = FontWeight.Bold)
                    }
                    if (upgradeableRoads > 0) {
                        OutlinedButton(
                            onClick = onUpgradeAllRoads,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isAr) "ترقية كل الطرق ($upgradeableRoads)" else "Upgrade Roads ($upgradeableRoads)", color = SkyBluePrimary)
                        }
                    }
                }
            }
        }
    }
}

private fun com.example.model.PlacedBuilding.categoryOrNull() = type.category
