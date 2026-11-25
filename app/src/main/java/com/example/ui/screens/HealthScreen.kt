package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.FitnessGoal
import com.example.data.model.GoalMetricType
import com.example.data.model.GoalPeriod
import com.example.data.model.HaversineMath
import com.example.data.model.HealthMetric
import com.example.data.model.HydrationLog
import com.example.data.model.SleepLog
import com.example.data.model.UserProfile
import com.example.ui.components.NoirBarChart
import com.example.ui.components.NoirCard
import com.example.ui.components.NoirDivider
import com.example.ui.components.NoirLineChart
import com.example.ui.components.NoirPrimaryButton
import com.example.ui.components.NoirProgressBar
import com.example.ui.components.NoirSecondaryButton
import com.example.ui.components.NoirSectionHeader
import com.example.ui.components.NoirSelectableChip
import com.example.ui.components.NoirStatusPill
import com.example.ui.components.NoirTextField
import com.example.ui.theme.NoirBackgroundSecondary
import com.example.ui.theme.NoirDanger
import com.example.ui.theme.NoirSuccess
import com.example.ui.theme.NoirTextMuted
import com.example.ui.theme.NoirTextPrimary
import com.example.ui.theme.NoirTextSecondary
import java.util.Locale

@Composable
fun HealthScreen(
    profile: UserProfile,
    healthMetrics: List<HealthMetric>,
    hydrationLogs: List<HydrationLog>,
    sleepLogs: List<SleepLog>,
    goals: List<FitnessGoal>,
    onSaveHealthMetric: (HealthMetric) -> Unit,
    onAddWater: (Long, String?) -> Unit,
    onDeleteWaterLog: (HydrationLog) -> Unit,
    onUpdateWaterGoal: (Double) -> Unit,
    onAddSleep: (bedtime: String, wakeTime: String, durationMin: Long, quality: Long, notes: String?) -> Unit,
    onDeleteSleep: (String) -> Unit
) {
    val todayKey = remember { HaversineMath.todayDateKey() }
    val todayMetric = healthMetrics.firstOrNull { it.dateKey == todayKey }
    val todayHydrationLogs = hydrationLogs.filter { it.dateKey == todayKey }

    val waterGoalMl = goals.firstOrNull { it.metricTypeEnum == GoalMetricType.DAILY_WATER_ML }?.targetValue ?: 2500.0
    val currentWaterMl = todayMetric?.waterMl ?: todayHydrationLogs.sumOf { it.amountMl }
    val waterPct = (currentWaterMl / waterGoalMl).toFloat().coerceIn(0f, 1f)

    var showMetricsDialog by remember { mutableStateOf(false) }
    var showSleepDialog by remember { mutableStateOf(false) }
    var showWaterGoalDialog by remember { mutableStateOf(false) }

    val chronological = remember(healthMetrics) { healthMetrics.sortedBy { it.dateKey }.takeLast(14) }
    val weightSeries = remember(chronological) {
        chronological.mapNotNull { m ->
            m.weightKg?.let { HaversineMath.formatShortDate(m.dateKey) to HaversineMath.convertWeight(it, profile.weightUnitEnum) }
        }
    }
    val hrSeries = remember(chronological) {
        chronological.mapNotNull { m ->
            m.restingHeartRate?.let { HaversineMath.formatShortDate(m.dateKey) to it.toDouble() }
        }
    }
    val sleepSeries = remember(chronological) {
        chronological.takeLast(7).map { m ->
            HaversineMath.formatShortDate(m.dateKey) to ((m.sleepMinutes ?: 0L) / 60.0)
        }
    }
    val waterSeries = remember(chronological) {
        chronological.takeLast(7).map { m ->
            HaversineMath.formatShortDate(m.dateKey) to (m.waterMl / 1000.0)
        }
    }
    val stepsSeries = remember(chronological) {
        chronological.takeLast(7).map { m ->
            HaversineMath.formatShortDate(m.dateKey) to (m.steps / 1000.0)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            NoirSectionHeader(
                eyebrow = "Daily Biometrics",
                title = "Health Dashboard",
                trailing = {
                    NoirPrimaryButton(
                        text = "Log Check-In",
                        icon = Icons.Default.Add,
                        tag = "open_health_checkin_btn",
                        onClick = { showMetricsDialog = true }
                    )
                }
            )
        }

        // Daily Snapshot Card
        item {
            val weight = todayMetric?.weightKg ?: profile.weightKg
            val height = todayMetric?.heightCm ?: profile.heightCm
            val bmi = HaversineMath.calculateBmi(weight, height)
            NoirCard(padding = PaddingValues(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("TODAY · $todayKey", style = MaterialTheme.typography.labelMedium, color = NoirTextMuted)
                    NoirStatusPill(text = "SELF-REPORTED DATA")
                }
                Spacer(modifier = Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    HealthStat("WEIGHT", HaversineMath.formatWeight(weight, profile.weightUnitEnum))
                    HealthStat("BMI", bmi?.let { String.format(Locale.US, "%.1f", it) } ?: "—")
                    HealthStat("RESTING HR", todayMetric?.restingHeartRate?.let { "$it bpm" } ?: "—")
                }
                Spacer(modifier = Modifier.height(12.dp))
                NoirDivider(subtle = true)
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    HealthStat(
                        "BLOOD PRESS.",
                        if (todayMetric?.systolicBp != null && todayMetric.diastolicBp != null) {
                            "${todayMetric.systolicBp}/${todayMetric.diastolicBp}"
                        } else "—"
                    )
                    HealthStat("MOOD", todayMetric?.moodLevel?.let { "$it / 5" } ?: "—")
                    HealthStat("ENERGY", todayMetric?.energyLevel?.let { "$it / 5" } ?: "—")
                }
            }
        }

        // Interactive Hydration Tracker
        item {
            NoirCard(padding = PaddingValues(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("HYDRATION TRACKER", style = MaterialTheme.typography.labelMedium, color = NoirTextMuted)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$currentWaterMl / ${waterGoalMl.toInt()} ml",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = NoirTextPrimary
                        )
                    }
                    NoirSecondaryButton(
                        text = "Goal",
                        onClick = { showWaterGoalDialog = true }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                NoirProgressBar(
                    progress = waterPct,
                    height = 4.dp,
                    fillColor = if (waterPct >= 1f) NoirSuccess else NoirTextPrimary
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NoirPrimaryButton(
                        text = "+250 ml",
                        icon = Icons.Default.WaterDrop,
                        tag = "add_water_250_btn",
                        modifier = Modifier.weight(1f),
                        onClick = { onAddWater(250L, "Glass") }
                    )
                    NoirSecondaryButton(
                        text = "+500 ml",
                        tag = "add_water_500_btn",
                        modifier = Modifier.weight(1f),
                        onClick = { onAddWater(500L, "Bottle") }
                    )
                    NoirSecondaryButton(
                        text = "+750 ml",
                        modifier = Modifier.weight(1f),
                        onClick = { onAddWater(750L, "Flask") }
                    )
                }
                if (todayHydrationLogs.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    NoirDivider(subtle = true)
                    Spacer(modifier = Modifier.height(8.dp))
                    todayHydrationLogs.take(5).forEach { log ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${log.amountMl} ml · ${log.label ?: "Water"}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = NoirTextPrimary
                                )
                                Text(
                                    text = HaversineMath.formatDisplayDate(log.loggedAtEpochMs, profile.use24HourClock),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NoirTextMuted
                                )
                            }
                            IconButton(
                                onClick = { onDeleteWaterLog(log) },
                                modifier = Modifier.testTag("delete_water_${log.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Remove hydration entry",
                                    tint = NoirTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Sleep Tracker
        item {
            NoirCard(padding = PaddingValues(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("SLEEP LOG", style = MaterialTheme.typography.labelMedium, color = NoirTextMuted)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = HaversineMath.formatSleepDuration(sleepLogs.firstOrNull()?.durationMinutes),
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = NoirTextPrimary
                        )
                    }
                    NoirSecondaryButton(
                        text = "Log Sleep",
                        icon = Icons.Default.Bedtime,
                        tag = "open_sleep_log_btn",
                        onClick = { showSleepDialog = true }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Manual sleep tracking records your self-reported schedule and perceived quality. It does not perform medical diagnostics.",
                    style = MaterialTheme.typography.labelSmall,
                    color = NoirTextMuted
                )
                if (sleepLogs.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    NoirDivider(subtle = true)
                    Spacer(modifier = Modifier.height(8.dp))
                    sleepLogs.take(4).forEach { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${HaversineMath.formatShortDate(entry.dateKey)} · ${HaversineMath.formatSleepDuration(entry.durationMinutes)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = NoirTextPrimary
                                )
                                Text(
                                    text = "${entry.bedtimeLabel} → ${entry.wakeTimeLabel} · Quality ${entry.qualityRating}/5",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NoirTextMuted
                                )
                            }
                            IconButton(onClick = { onDeleteSleep(entry.id) }) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete sleep entry",
                                    tint = NoirTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Health Visualizations
        item {
            NoirCard(padding = PaddingValues(18.dp)) {
                Text("WEIGHT PROGRESSION", style = MaterialTheme.typography.labelMedium, color = NoirTextMuted)
                Spacer(modifier = Modifier.height(10.dp))
                NoirLineChart(
                    data = weightSeries,
                    unitSuffix = profile.weightUnitEnum.shortLabel,
                    emptyLabel = "Log weight on 2+ days to plot weight progression."
                )
            }
        }

        item {
            NoirCard(padding = PaddingValues(18.dp)) {
                Text("RESTING HEART RATE TREND", style = MaterialTheme.typography.labelMedium, color = NoirTextMuted)
                Spacer(modifier = Modifier.height(10.dp))
                NoirLineChart(
                    data = hrSeries,
                    unitSuffix = "bpm",
                    emptyLabel = "Log resting heart rate on 2+ days to view trend."
                )
            }
        }

        item {
            NoirCard(padding = PaddingValues(18.dp)) {
                Text("SLEEP HISTORY (HOURS)", style = MaterialTheme.typography.labelMedium, color = NoirTextMuted)
                Spacer(modifier = Modifier.height(10.dp))
                NoirBarChart(data = sleepSeries, unitSuffix = "h", emptyLabel = "No sleep history logged yet.")
            }
        }

        item {
            NoirCard(padding = PaddingValues(18.dp)) {
                Text("DAILY WATER INTAKE (LITERS)", style = MaterialTheme.typography.labelMedium, color = NoirTextMuted)
                Spacer(modifier = Modifier.height(10.dp))
                NoirBarChart(data = waterSeries, unitSuffix = "L", emptyLabel = "No hydration history logged yet.")
            }
        }

        item {
            NoirCard(padding = PaddingValues(18.dp)) {
                Text("WEEKLY STEP COUNT (THOUSANDS)", style = MaterialTheme.typography.labelMedium, color = NoirTextMuted)
                Spacer(modifier = Modifier.height(10.dp))
                NoirBarChart(data = stepsSeries, unitSuffix = "k", emptyLabel = "No daily steps logged yet.")
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }

    if (showMetricsDialog) {
        DailyHealthCheckInDialog(
            existing = todayMetric,
            profile = profile,
            onDismiss = { showMetricsDialog = false },
            onSave = { metric ->
                onSaveHealthMetric(metric)
                showMetricsDialog = false
            }
        )
    }

    if (showSleepDialog) {
        SleepLogDialog(
            onDismiss = { showSleepDialog = false },
            onSave = { bed, wake, mins, qual, notes ->
                onAddSleep(bed, wake, mins, qual, notes)
                showSleepDialog = false
            }
        )
    }

    if (showWaterGoalDialog) {
        WaterGoalDialog(
            currentGoalMl = waterGoalMl.toInt(),
            onDismiss = { showWaterGoalDialog = false },
            onSave = { newGoal ->
                onUpdateWaterGoal(newGoal.toDouble())
                showWaterGoalDialog = false
            }
        )
    }
}

@Composable
private fun HealthStat(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = NoirTextMuted)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = NoirTextPrimary
        )
    }
}

@Composable
private fun DailyHealthCheckInDialog(
    existing: HealthMetric?,
    profile: UserProfile,
    onDismiss: () -> Unit,
    onSave: (HealthMetric) -> Unit
) {
    var steps by remember { mutableStateOf((existing?.steps ?: 0L).toString()) }
    var caloriesBurned by remember { mutableStateOf((existing?.caloriesBurned ?: 0L).toString()) }
    var weightKg by remember { mutableStateOf((existing?.weightKg ?: profile.weightKg)?.toString() ?: "") }
    var heightCm by remember { mutableStateOf((existing?.heightCm ?: profile.heightCm)?.toString() ?: "") }
    var restingHr by remember { mutableStateOf(existing?.restingHeartRate?.toString() ?: "") }
    var systolic by remember { mutableStateOf(existing?.systolicBp?.toString() ?: "") }
    var diastolic by remember { mutableStateOf(existing?.diastolicBp?.toString() ?: "") }
    var caloriesConsumed by remember { mutableStateOf(existing?.caloriesConsumed?.toString() ?: "") }
    var mood by remember { mutableStateOf((existing?.moodLevel ?: 4L).toInt()) }
    var energy by remember { mutableStateOf((existing?.energyLevel ?: 4L).toInt()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NoirBackgroundSecondary,
        title = { Text("Daily Health Check-In", style = MaterialTheme.typography.headlineLarge, color = NoirTextPrimary) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NoirTextField(
                        value = steps,
                        onValueChange = { steps = it },
                        label = "Daily Steps",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    NoirTextField(
                        value = caloriesBurned,
                        onValueChange = { caloriesBurned = it },
                        label = "Kcal Burned",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NoirTextField(
                        value = weightKg,
                        onValueChange = { weightKg = it },
                        label = "Weight (kg)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    NoirTextField(
                        value = heightCm,
                        onValueChange = { heightCm = it },
                        label = "Height (cm)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NoirTextField(
                        value = restingHr,
                        onValueChange = { restingHr = it },
                        label = "Resting HR",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    NoirTextField(
                        value = caloriesConsumed,
                        onValueChange = { caloriesConsumed = it },
                        label = "Kcal Intake",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NoirTextField(
                        value = systolic,
                        onValueChange = { systolic = it },
                        label = "Systolic BP",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    NoirTextField(
                        value = diastolic,
                        onValueChange = { diastolic = it },
                        label = "Diastolic BP",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                Text("MOOD (1-5)", style = MaterialTheme.typography.labelSmall, color = NoirTextMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..5).forEach { level ->
                        NoirSelectableChip(
                            label = level.toString(),
                            selected = mood == level,
                            onClick = { mood = level },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Text("ENERGY (1-5)", style = MaterialTheme.typography.labelSmall, color = NoirTextMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..5).forEach { level ->
                        NoirSelectableChip(
                            label = level.toString(),
                            selected = energy == level,
                            onClick = { energy = level },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            NoirPrimaryButton(
                text = "Save Log",
                tag = "save_health_checkin_btn",
                onClick = {
                    onSave(
                        HealthMetric(
                            id = existing?.id ?: HaversineMath.todayDateKey(),
                            userId = profile.userId,
                            dateKey = HaversineMath.todayDateKey(),
                            steps = steps.toLongOrNull()?.coerceIn(0L, 200_000L) ?: 0L,
                            caloriesBurned = caloriesBurned.toLongOrNull()?.coerceIn(0L, 30_000L) ?: 0L,
                            waterMl = existing?.waterMl ?: 0L,
                            weightKg = weightKg.toDoubleOrNull()?.coerceIn(20.0, 350.0),
                            heightCm = heightCm.toDoubleOrNull()?.coerceIn(50.0, 260.0),
                            sleepMinutes = existing?.sleepMinutes,
                            sleepQuality = existing?.sleepQuality,
                            restingHeartRate = restingHr.toLongOrNull()?.coerceIn(30L, 220L),
                            systolicBp = systolic.toLongOrNull()?.coerceIn(60L, 260L),
                            diastolicBp = diastolic.toLongOrNull()?.coerceIn(30L, 180L),
                            caloriesConsumed = caloriesConsumed.toLongOrNull()?.coerceIn(0L, 25_000L),
                            moodLevel = mood.toLong(),
                            energyLevel = energy.toLong()
                        )
                    )
                }
            )
        },
        dismissButton = {
            NoirSecondaryButton(text = "Cancel", onClick = onDismiss)
        }
    )
}

@Composable
private fun SleepLogDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, Long, Long, String?) -> Unit
) {
    var bedtime by remember { mutableStateOf("23:00") }
    var wakeTime by remember { mutableStateOf("07:00") }
    var durationHours by remember { mutableStateOf("7.5") }
    var quality by remember { mutableStateOf(4) }
    var notes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NoirBackgroundSecondary,
        title = { Text("Log Sleep Session", style = MaterialTheme.typography.headlineLarge, color = NoirTextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NoirTextField(value = bedtime, onValueChange = { bedtime = it }, label = "Bedtime", modifier = Modifier.weight(1f))
                    NoirTextField(value = wakeTime, onValueChange = { wakeTime = it }, label = "Wake Time", modifier = Modifier.weight(1f))
                }
                NoirTextField(
                    value = durationHours,
                    onValueChange = { durationHours = it },
                    label = "Total Duration (hours)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                Text("PERCEIVED QUALITY (1-5)", style = MaterialTheme.typography.labelSmall, color = NoirTextMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..5).forEach { q ->
                        NoirSelectableChip(
                            label = q.toString(),
                            selected = quality == q,
                            onClick = { quality = q },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                NoirTextField(value = notes, onValueChange = { notes = it }, label = "Notes (optional)")
                error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = NoirDanger) }
            }
        },
        confirmButton = {
            NoirPrimaryButton(
                text = "Save Sleep",
                tag = "save_sleep_btn",
                onClick = {
                    val hrs = durationHours.toDoubleOrNull()
                    if (hrs == null || hrs <= 0.1 || hrs > 24.0) {
                        error = "Enter a valid duration between 0.5 and 24 hours."
                    } else {
                        onSave(bedtime, wakeTime, (hrs * 60.0).toLong(), quality.toLong(), notes.ifBlank { null })
                    }
                }
            )
        },
        dismissButton = {
            NoirSecondaryButton(text = "Cancel", onClick = onDismiss)
        }
    )
}

@Composable
private fun WaterGoalDialog(
    currentGoalMl: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var input by remember { mutableStateOf(currentGoalMl.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NoirBackgroundSecondary,
        title = { Text("Daily Hydration Goal", style = MaterialTheme.typography.headlineLarge, color = NoirTextPrimary) },
        text = {
            NoirTextField(
                value = input,
                onValueChange = { input = it },
                label = "Daily Target (ml)",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        },
        confirmButton = {
            NoirPrimaryButton(
                text = "Update Goal",
                onClick = {
                    val parsed = input.toIntOrNull()?.coerceIn(250, 15000) ?: 2500
                    onSave(parsed)
                }
            )
        },
        dismissButton = {
            NoirSecondaryButton(text = "Cancel", onClick = onDismiss)
        }
    )
}
