package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.i18n.AppStrings
import com.example.model.AppLanguage
import com.example.model.GameState
import com.example.ui.theme.AmberGold
import com.example.ui.theme.AmethystGem
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.SkyBluePrimary
import com.example.ui.theme.SlateBorderDark
import com.example.ui.theme.SlateCardDark

@Composable
fun LocalMusicPlayerDialog(
    state: GameState,
    onAddSongs: (List<Uri>) -> Unit,
    onPlayTrack: (Int) -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onRemoveSong: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val lang = state.settings.language
    val isAr = AppStrings.isAr(lang)

    // SAF zero-permission document picker for local audio files
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = { uris ->
            if (uris.isNotEmpty()) onAddSongs(uris)
        }
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = SlateCardDark,
            shape = RoundedCornerShape(22.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorderDark)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.MusicNote, contentDescription = null, tint = Color(0xFFF472B6))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isAr) "مشغل الأغاني المحلي" else "Local Music Player",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Text(
                    text = if (isAr) "تُحفظ الأغاني المضافة تلقائياً ولا تُحذف إلا يدوياً بواسطتك."
                    else "Added songs stay saved permanently until you manually delete them.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Player Controls Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onPrev, enabled = state.playlist.isNotEmpty()) {
                        Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", tint = Color.White)
                    }
                    Button(
                        onClick = onTogglePlayPause,
                        enabled = state.playlist.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = SkyBluePrimary)
                    ) {
                        Icon(
                            if (state.isMusicPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color(0xFF082F49)
                        )
                    }
                    IconButton(onClick = onNext, enabled = state.playlist.isNotEmpty()) {
                        Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = Color.White)
                    }
                    Button(
                        onClick = { pickerLauncher.launch(arrayOf("audio/*")) },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                        modifier = Modifier.testTag("add_songs_button")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = Color(0xFF022C22))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isAr) "إضافة" else "Add", color = Color(0xFF022C22), fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (state.playlist.isEmpty()) {
                    Text(
                        text = if (isAr) "لا توجد أغانٍ بعد. اضغط إضافة لاختيار ملفات صوتية من ذاكرة الهاتف."
                        else "No songs yet. Tap Add to choose audio files from your phone storage.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFCBD5E1),
                        modifier = Modifier.padding(vertical = 18.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 180.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(state.playlist, key = { _, item -> item.uriString }) { idx, track ->
                            val active = idx == state.currentTrackIndex
                            Surface(
                                color = if (active) Color(0xFF0C4A6E) else Color(0xFF0F172A),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onPlayTrack(idx) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        if (active && state.isMusicPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                        contentDescription = null,
                                        tint = if (active) SkyBluePrimary else Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = track.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { onRemoveSong(track.uriString) }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Delete Song", tint = CoralAlert)
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
fun CustomizeResourcesDialog(
    state: GameState,
    onCollectTaxBonus: () -> Unit,
    onApplyCustomAmounts: (Long, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val lang = state.settings.language
    val isAr = AppStrings.isAr(lang)
    var coinsInput by remember(state.coins) { mutableStateOf(state.coins.toString()) }
    var gemsInput by remember(state.gems) { mutableStateOf(state.gems.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = SlateCardDark,
            shape = RoundedCornerShape(22.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorderDark),
            modifier = Modifier.heightIn(max = 340.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (isAr) "تخصيص النقود والجواهر" else "Customize Money & Gems",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Text(
                    text = if (isAr) "يمكنك تعديل رصيد النقود والجواهر يدوياً في أي وقت دون حذف أو إعادة تعيين تقدمك."
                    else "Edit your Money and Gems amounts manually anytime without deleting or resetting any progress.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFCBD5E1)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = coinsInput,
                        onValueChange = { txt -> coinsInput = txt.filter { it.isDigit() }.take(9) },
                        label = { Text(if (isAr) "النقود (العملات)" else "Money (Coins)") },
                        leadingIcon = { Icon(Icons.Filled.AttachMoney, contentDescription = null, tint = AmberGold) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("edit_coins_input")
                    )
                    OutlinedTextField(
                        value = gemsInput,
                        onValueChange = { txt -> gemsInput = txt.filter { it.isDigit() }.take(8) },
                        label = { Text(if (isAr) "الجواهر" else "Gems") },
                        leadingIcon = { Icon(Icons.Filled.Diamond, contentDescription = null, tint = AmethystGem) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("edit_gems_input")
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = {
                            onCollectTaxBonus()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (isAr) "تحصيل ضريبة سريعة" else "Collect Tax Bonus",
                            color = AmberGold
                        )
                    }
                    Button(
                        onClick = {
                            val newCoins = coinsInput.toLongOrNull() ?: state.coins
                            val newGems = gemsInput.toIntOrNull() ?: state.gems
                            onApplyCustomAmounts(newCoins, newGems)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SkyBluePrimary),
                        modifier = Modifier.weight(1f).testTag("apply_resources_button")
                    ) {
                        Text(
                            text = if (isAr) "حفظ الرصيد" else "Apply Amounts",
                            color = Color(0xFF082F49),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsDialog(
    state: GameState,
    onVolumeChange: (Float) -> Unit,
    onSfxEnabledChange: (Boolean) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onBrightRoadLinesAlwaysChange: (Boolean) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onAutoDayNightChange: (Boolean) -> Unit,
    onTimeOfDayChange: (Float) -> Unit,
    onSimSpeedChange: (Int) -> Unit,
    onTaxPolicyChange: (com.example.model.TaxPolicy) -> Unit,
    onTrafficDensityChange: (com.example.model.TrafficDensity) -> Unit,
    onCycleSeason: () -> Unit,
    onCityNameChange: (String) -> Unit,
    onApplyCustomResources: (Long, Int) -> Unit,
    onManualSave: () -> Unit,
    onStartNewCity: () -> Unit,
    onDismiss: () -> Unit
) {
    val lang = state.settings.language
    val isAr = AppStrings.isAr(lang)
    var cityDraft by remember(state.cityName) { mutableStateOf(state.cityName) }
    var coinsDraft by remember(state.coins) { mutableStateOf(state.coins.toString()) }
    var gemsDraft by remember(state.gems) { mutableStateOf(state.gems.toString()) }
    var confirmNewCity by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = SlateCardDark,
            shape = RoundedCornerShape(22.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorderDark),
            modifier = Modifier.heightIn(max = 340.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (isAr) "إعدادات اللعبة" else "Game Settings",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                // 1. Language Toggle (Arabic / English)
                Text(
                    text = if (isAr) "اللغة (Language)" else "Language (اللغة)",
                    style = MaterialTheme.typography.labelLarge,
                    color = SkyBluePrimary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    AppLanguage.entries.forEach { option ->
                        val selected = lang == option
                        Button(
                            onClick = { onLanguageChange(option) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selected) SkyBluePrimary else Color(0xFF0F172A)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("lang_${option.code}")
                        ) {
                            Text(
                                text = option.displayName,
                                color = if (selected) Color(0xFF082F49) else Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // 2. Sound, SFX & Brightness
                Text(
                    text = if (isAr) "مستوى الصوت: ${(state.settings.soundVolume * 100).toInt()}%"
                    else "Sound Volume: ${(state.settings.soundVolume * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
                Slider(
                    value = state.settings.soundVolume,
                    onValueChange = onVolumeChange,
                    colors = SliderDefaults.colors(thumbColor = SkyBluePrimary, activeTrackColor = SkyBluePrimary)
                )

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (isAr) "المؤثرات الصوتية" else "Sound Effects (SFX)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(checked = state.settings.sfxEnabled, onCheckedChange = onSfxEnabledChange)
                }

                Text(
                    text = if (isAr) "سطوع اللعبة: ${(state.settings.brightness * 100).toInt()}%"
                    else "Game Brightness: ${(state.settings.brightness * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
                Slider(
                    value = state.settings.brightness,
                    onValueChange = onBrightnessChange,
                    valueRange = 0.35f..1.25f,
                    colors = SliderDefaults.colors(thumbColor = SkyBluePrimary, activeTrackColor = SkyBluePrimary)
                )

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (isAr) "إبقاء خطوط الطرق والإنارة ساطعة دائماً"
                        else "Keep Road Lines & Lights Glowing Always",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(checked = state.settings.brightRoadLinesAlways, onCheckedChange = onBrightRoadLinesAlwaysChange)
                }

                // 3. Simulation Speed & Traffic Density
                Text(
                    text = if (isAr) "سرعة المحاكاة" else "Simulation Speed",
                    style = MaterialTheme.typography.labelLarge,
                    color = SkyBluePrimary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf(0, 1, 2, 3).forEach { spd ->
                        val selected = state.settings.simulationSpeed == spd
                        val lbl = if (spd == 0) (if (isAr) "إيقاف" else "Pause") else "${spd}x"
                        Button(
                            onClick = { onSimSpeedChange(spd) },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selected) SkyBluePrimary else Color(0xFF0F172A)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(lbl, color = if (selected) Color(0xFF082F49) else Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Text(
                    text = if (isAr) "كثافة المرور والطائرات" else "Traffic & Sky Density",
                    style = MaterialTheme.typography.labelLarge,
                    color = SkyBluePrimary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    com.example.model.TrafficDensity.entries.forEach { d ->
                        val selected = state.settings.trafficDensity == d
                        val lbl = when (d) {
                            com.example.model.TrafficDensity.LOW -> if (isAr) "منخفض" else "Low"
                            com.example.model.TrafficDensity.BALANCED -> if (isAr) "متوازن" else "Balanced"
                            com.example.model.TrafficDensity.HIGH -> if (isAr) "كثيف" else "High"
                        }
                        Button(
                            onClick = { onTrafficDensityChange(d) },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selected) SkyBluePrimary else Color(0xFF0F172A)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(lbl, color = if (selected) Color(0xFF082F49) else Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 4. Day & Night Cycle + Season
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (isAr) "دورة الليل والنهار التلقائية" else "Auto Day & Night Cycle",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(checked = state.settings.autoDayNightCycle, onCheckedChange = onAutoDayNightChange)
                }
                Text(
                    text = if (isAr) "وقت اليوم (ليل / نهار)" else "Time of Day (Night / Day)",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFCBD5E1)
                )
                Slider(
                    value = state.timeOfDay,
                    onValueChange = onTimeOfDayChange,
                    colors = SliderDefaults.colors(thumbColor = Color(0xFFFBBF24), activeTrackColor = Color(0xFFFBBF24))
                )

                OutlinedButton(onClick = onCycleSeason, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (isAr) "الفصل الحالي: ${AppStrings.seasonName(lang, state.season)} (تغيير)"
                        else "Current Season: ${AppStrings.seasonName(lang, state.season)} (Switch)",
                        color = Color.White
                    )
                }

                // 5. Tax Policy & City Name
                Text(
                    text = if (isAr) "سياسة الضرائب" else "City Tax Policy",
                    style = MaterialTheme.typography.labelLarge,
                    color = AmberGold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    com.example.model.TaxPolicy.entries.forEach { policy ->
                        val selected = state.settings.taxPolicy == policy
                        val lbl = when (policy) {
                            com.example.model.TaxPolicy.LOW -> if (isAr) "منخفض" else "Low"
                            com.example.model.TaxPolicy.BALANCED -> if (isAr) "متوازن" else "Balanced"
                            com.example.model.TaxPolicy.HIGH -> if (isAr) "مرتفع" else "High"
                        }
                        Button(
                            onClick = { onTaxPolicyChange(policy) },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selected) AmberGold else Color(0xFF0F172A)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(lbl, color = if (selected) Color(0xFF082F49) else Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                OutlinedTextField(
                    value = cityDraft,
                    onValueChange = {
                        cityDraft = it
                        onCityNameChange(it)
                    },
                    label = { Text(if (isAr) "اسم المدينة" else "City Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // 6. Customize Money & Gems without losing progress
                Text(
                    text = if (isAr) "تخصيص النقود والجواهر (دون حذف التقدم)" else "Customize Money & Gems (No Reset)",
                    style = MaterialTheme.typography.labelLarge,
                    color = AmberGold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = coinsDraft,
                        onValueChange = { t -> coinsDraft = t.filter { it.isDigit() }.take(9) },
                        label = { Text(if (isAr) "النقود" else "Money") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = gemsDraft,
                        onValueChange = { t -> gemsDraft = t.filter { it.isDigit() }.take(8) },
                        label = { Text(if (isAr) "الجواهر" else "Gems") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Button(
                    onClick = {
                        onApplyCustomResources(
                            coinsDraft.toLongOrNull() ?: state.coins,
                            gemsDraft.toIntOrNull() ?: state.gems
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SkyBluePrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isAr) "تطبيق النقود والجواهر" else "Apply Money & Gems",
                        color = Color(0xFF082F49),
                        fontWeight = FontWeight.Bold
                    )
                }

                // 7. Auto-save Status, Manual Save & New City
                Surface(color = Color(0xFF0F172A), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (isAr) "الحفظ التلقائي كل ٥ ثوانٍ في ذاكرة الهاتف"
                            else "Auto-saves every 5s locally on phone storage",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldAccent
                        )
                        if (state.lastAutoSaveTimeText.isNotEmpty()) {
                            Text(
                                text = if (isAr) "آخر حفظ: ${state.lastAutoSaveTimeText}" else "Last saved: ${state.lastAutoSaveTimeText}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                        if (state.autoSavePathLabel.isNotEmpty()) {
                            Text(
                                text = state.autoSavePathLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Button(
                    onClick = onManualSave,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Save, contentDescription = null, tint = Color(0xFF022C22))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isAr) "حفظ التقدم الآن" else "Save Progress Now",
                        color = Color(0xFF022C22),
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!confirmNewCity) {
                    OutlinedButton(
                        onClick = { confirmNewCity = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isAr) "بدء مدينة جديدة" else "Start New City",
                            color = CoralAlert
                        )
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { confirmNewCity = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isAr) "تراجع" else "Keep City", color = Color.White)
                        }
                        Button(
                            onClick = {
                                confirmNewCity = false
                                onStartNewCity()
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CoralAlert),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isAr) "تأكيد البدء" else "Confirm Reset", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
