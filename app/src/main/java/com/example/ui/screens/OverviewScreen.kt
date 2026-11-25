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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ActivityRecord
import com.example.data.model.ActivityType
import com.example.data.model.AnalyticsSnapshot
import com.example.data.model.FitnessGoal
import com.example.data.model.GoalMetricType
import com.example.data.model.HaversineMath
import com.example.data.model.HealthMetric
import com.example.data.model.StreakMetrics
import com.example.data.model.UserProfile
import com.example.ui.components.NoirBarChart
import com.example.ui.components.NoirCard
import com.example.ui.components.NoirDivider
import com.example.ui.components.NoirEmptyState
import com.example.ui.components.NoirKpiCard
import com.example.ui.components.NoirPrimaryButton
import com.example.ui.components.NoirRouteThumbnail
import com.example.ui.components.NoirSecondaryButton
import com.example.ui.components.NoirSectionHeader
import com.example.ui.components.NoirStatusPill
import com.example.ui.theme.NoirSuccess
import com.example.ui.theme.NoirTextMuted
import com.example.ui.theme.NoirTextPrimary
import com.example.ui.theme.NoirTextSecondary
import com.example.ui.theme.NoirWarning
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun OverviewScreen(
    profile: UserProfile,
    activities: List<ActivityRecord>,
    healthMetrics: List<HealthMetric>,
    goals: List<FitnessGoal>,
    analytics: AnalyticsSnapshot,
    streaks: StreakMetrics,
    onStartLiveTracking: () -> Unit,
    onOpenQuickLog: () -> Unit,
    onOpenActivityDetail: (String) -> Unit,
    onOpenHealth: () -> Unit,
    onOpenHistory: () -> Unit
) {
    val todayKey = remember { HaversineMath.todayDateKey() }
    val yesterdayKey = remember { HaversineMath.dateKeyDaysAgo(1) }
    val todayMetric = healthMetrics.firstOrNull { it.dateKey == todayKey }
    val yesterdayMetric = healthMetrics.firstOrNull { it.dateKey == yesterdayKey }
    val latestMetric = healthMetrics.firstOrNull()

    val todayActivities = activities.filter { HaversineMath.dateKeyForMillis(it.startedAtEpochMs) == todayKey }
    val yesterdayActivities = activities.filter { HaversineMath.dateKeyForMillis(it.startedAtEpochMs) == yesterdayKey }

    val stepsTarget = goals.firstOrNull { it.metricTypeEnum == GoalMetricType.DAILY_STEPS }?.targetValue ?: 10000.0
    val calTarget = goals.firstOrNull { it.metricTypeEnum == GoalMetricType.DAILY_CALORIES }?.targetValue ?: 600.0
    val activeMinTarget = goals.firstOrNull { it.metricTypeEnum == GoalMetricType.DAILY_ACTIVE_MIN }?.targetValue ?: 45.0
    val distTargetKm = goals.firstOrNull { it.metricTypeEnum == GoalMetricType.DAILY_DISTANCE_KM }?.targetValue ?: 8.0

    val todaySteps = todayMetric?.steps ?: 0L
    val prevSteps = yesterdayMetric?.steps ?: 0L

    val todayCalories = maxOf(todayMetric?.caloriesBurned ?: 0L, todayActivities.sumOf { it.caloriesBurned })
    val prevCalories = maxOf(yesterdayMetric?.caloriesBurned ?: 0L, yesterdayActivities.sumOf { it.caloriesBurned })

    val todayActiveMin = todayActivities.sumOf { it.durationSec } / 60L
    val prevActiveMin = yesterdayActivities.sumOf { it.durationSec } / 60L

    val todayDistMeters = todayActivities.sumOf { it.distanceMeters }
    val prevDistMeters = yesterdayActivities.sumOf { it.distanceMeters }

    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour < 12 -> "Good morning"
            hour < 18 -> "Good afternoon"
            else -> "Good evening"
        }
    }
    val todayDisplayDate = remember {
        SimpleDateFormat("EEEE, MMMM d", Locale.US).format(Date())
    }
    val firstName = profile.displayName.substringBefore(" ").ifBlank { "Athlete" }
    val unit = profile.distanceUnitEnum

    val healthStatusLabel = when {
        todaySteps >= stepsTarget.toLong() && todayActiveMin >= activeMinTarget.toLong() -> "OPTIMAL OUTPUT"
        todaySteps > 0L || todayActivities.isNotEmpty() -> "IN PROGRESS"
        else -> "AWAITING LOG"
    }

    val runWalkMetersWeek = activities
        .filter {
            it.startedAtEpochMs >= System.currentTimeMillis() - 7L * 86_400_000L &&
                (it.activityTypeEnum == ActivityType.RUNNING || it.activityTypeEnum == ActivityType.WALKING)
        }
        .sumOf { it.distanceMeters }
    val weekSessions = activities.count { it.startedAtEpochMs >= System.currentTimeMillis() - 7L * 86_400_000L }
    val weekDurationSec = activities
        .filter { it.startedAtEpochMs >= System.currentTimeMillis() - 7L * 86_400_000L }
        .sumOf { it.durationSec }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Welcome Editorial Header
        item {
            NoirCard(padding = PaddingValues(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = todayDisplayDate.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = NoirTextMuted
                    )
                    NoirStatusPill(
                        text = healthStatusLabel,
                        accentColor = if (healthStatusLabel == "OPTIMAL OUTPUT") NoirSuccess else NoirTextSecondary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$greeting, $firstName.",
                    style = MaterialTheme.typography.displayMedium,
                    color = NoirTextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${streaks.currentStreakDays}-day streak · ${HaversineMath.formatDistance(analytics.weeklyDistanceMeters, unit)} across $weekSessions sessions this week.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NoirTextSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    NoirPrimaryButton(
                        text = "Live Track",
                        icon = Icons.Default.PlayArrow,
                        tag = "overview_live_track_btn",
                        modifier = Modifier.weight(1f),
                        onClick = onStartLiveTracking
                    )
                    NoirSecondaryButton(
                        text = "Log Entry",
                        icon = Icons.Default.Add,
                        tag = "overview_quick_log_btn",
                        modifier = Modifier.weight(1f),
                        onClick = onOpenQuickLog
                    )
                }
            }
        }

        // 2. Four Primary KPI Cards (2x2 Grid)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    NoirKpiCard(
                        label = "Daily Steps",
                        value = String.format(Locale.US, "%,d", todaySteps),
                        targetText = "Target ${String.format(Locale.US, "%,d", stepsTarget.toLong())}",
                        percent = (todaySteps / stepsTarget).toFloat().coerceIn(0f, 1f),
                        deltaText = formatDelta(todaySteps - prevSteps, "vs yesterday"),
                        deltaPositive = if (prevSteps == 0L) null else todaySteps >= prevSteps,
                        modifier = Modifier.weight(1f)
                    )
                    NoirKpiCard(
                        label = "Calories",
                        value = "$todayCalories kcal",
                        targetText = "Target ${calTarget.toInt()} kcal",
                        percent = (todayCalories / calTarget).toFloat().coerceIn(0f, 1f),
                        deltaText = formatDelta(todayCalories - prevCalories, "kcal vs prev"),
                        deltaPositive = if (prevCalories == 0L) null else todayCalories >= prevCalories,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    NoirKpiCard(
                        label = "Active Time",
                        value = "$todayActiveMin min",
                        targetText = "Target ${activeMinTarget.toInt()} min",
                        percent = (todayActiveMin / activeMinTarget).toFloat().coerceIn(0f, 1f),
                        deltaText = formatDelta(todayActiveMin - prevActiveMin, "min vs prev"),
                        deltaPositive = if (prevActiveMin == 0L) null else todayActiveMin >= prevActiveMin,
                        modifier = Modifier.weight(1f)
                    )
                    val targetDistMeters = distTargetKm * 1000.0
                    NoirKpiCard(
                        label = "Distance",
                        value = HaversineMath.formatDistance(todayDistMeters, unit),
                        targetText = "Target ${HaversineMath.formatDistance(targetDistMeters, unit)}",
                        percent = (todayDistMeters / targetDistMeters).toFloat().coerceIn(0f, 1f),
                        deltaText = if (prevDistMeters == 0.0) "No prior day data" else {
                            val diff = (todayDistMeters - prevDistMeters) / 1000.0
                            String.format(Locale.US, "%+.1f km vs prev", diff)
                        },
                        deltaPositive = if (prevDistMeters == 0.0) null else todayDistMeters >= prevDistMeters,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 3. Weekly Activity Overview Graph
        item {
            NoirCard(padding = PaddingValues(18.dp)) {
                NoirSectionHeader(
                    eyebrow = "Movement Cadence",
                    title = "Weekly Activity"
                )
                Spacer(modifier = Modifier.height(14.dp))
                NoirBarChart(
                    data = analytics.weeklyBars,
                    unitSuffix = "km",
                    emptyLabel = "Record an activity to visualize your 7-day movement volume."
                )
                Spacer(modifier = Modifier.height(14.dp))
                NoirDivider(subtle = true)
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    InlineStat("RUN & WALK", HaversineMath.formatDistance(runWalkMetersWeek, unit))
                    InlineStat("DURATION", HaversineMath.formatDuration(weekDurationSec))
                    InlineStat("FREQUENCY", "$weekSessions / 7d")
                }
            }
        }

        // 4. Health Overview (User-Entered Biometrics Only)
        item {
            NoirCard(padding = PaddingValues(18.dp), onClick = onOpenHealth) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("BIOMETRICS · USER-LOGGED", style = MaterialTheme.typography.labelMedium, color = NoirTextMuted)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Health Overview", style = MaterialTheme.typography.headlineLarge, color = NoirTextPrimary)
                    }
                    NoirStatusPill(text = "LOG METRICS")
                }
                Spacer(modifier = Modifier.height(14.dp))
                val weightVal = todayMetric?.weightKg ?: latestMetric?.weightKg ?: profile.weightKg
                val heightVal = todayMetric?.heightCm ?: latestMetric?.heightCm ?: profile.heightCm
                val bmi = HaversineMath.calculateBmi(weightVal, heightVal)
                val rhr = todayMetric?.restingHeartRate ?: latestMetric?.restingHeartRate
                val sleep = todayMetric?.sleepMinutes ?: latestMetric?.sleepMinutes
                val water = todayMetric?.waterMl ?: 0L

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    InlineStat("RESTING HR", rhr?.let { "$it bpm" } ?: "—")
                    InlineStat("SLEEP", HaversineMath.formatSleepDuration(sleep))
                    InlineStat("WATER", if (water > 0) "$water ml" else "—")
                }
                Spacer(modifier = Modifier.height(12.dp))
                NoirDivider(subtle = true)
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    InlineStat("WEIGHT", HaversineMath.formatWeight(weightVal, profile.weightUnitEnum))
                    InlineStat("BMI", bmi?.let { String.format(Locale.US, "%.1f", it) } ?: "—")
                    InlineStat("CONSISTENCY", "${analytics.weeklyConsistencyPercent}%")
                }
            }
        }

        // 5. Recent Activity Feed Timeline
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("RECENT ACTIVITY TIMELINE", style = MaterialTheme.typography.labelMedium, color = NoirTextMuted)
                NoirSecondaryButton(
                    text = "View All",
                    onClick = onOpenHistory
                )
            }
        }

        if (activities.isEmpty()) {
            item {
                NoirEmptyState(
                    title = "No recorded sessions yet",
                    description = "Start a live GPS workout or add a manual entry to populate your monochrome activity timeline.",
                    actionLabel = "Start Live Session",
                    onAction = onStartLiveTracking
                )
            }
        } else {
            items(activities.take(6), key = { it.id.ifBlank { it.startedAtEpochMs.toString() } }) { act ->
                ActivityFeedRowCard(
                    activity = act,
                    profile = profile,
                    onClick = { onOpenActivityDetail(act.id) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
fun ActivityFeedRowCard(
    activity: ActivityRecord,
    profile: UserProfile,
    onClick: () -> Unit
) {
    val unit = profile.distanceUnitEnum
    NoirCard(
        modifier = Modifier.fillMaxWidth(),
        padding = PaddingValues(14.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NoirRouteThumbnail(
                points = activity.routePoints,
                modifier = Modifier.size(72.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NoirStatusPill(text = activity.activityTypeEnum.label)
                    Text(
                        text = HaversineMath.formatDisplayDate(activity.startedAtEpochMs, profile.use24HourClock),
                        style = MaterialTheme.typography.labelSmall,
                        color = NoirTextMuted,
                        maxLines = 1
                    )
                }
                Text(
                    text = activity.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                    color = NoirTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    InlineCompactMetric(HaversineMath.formatDistance(activity.distanceMeters, unit))
                    InlineCompactMetric(HaversineMath.formatDuration(activity.durationSec))
                    if (activity.avgPaceSecPerKm > 0.0) {
                        InlineCompactMetric(HaversineMath.formatPace(activity.avgPaceSecPerKm, unit))
                    }
                    InlineCompactMetric("${activity.caloriesBurned} kcal")
                }
            }
        }
    }
}

@Composable
private fun InlineCompactMetric(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = NoirTextSecondary,
        maxLines = 1
    )
}

@Composable
private fun InlineStat(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = NoirTextMuted)
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = NoirTextPrimary
        )
    }
}

private fun formatDelta(delta: Long, suffix: String): String {
    return when {
        delta > 0L -> "+$delta $suffix"
        delta < 0L -> "$delta $suffix"
        else -> "No change $suffix"
    }
}
