package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AnalyticsRange
import com.example.data.model.AnalyticsSnapshot
import com.example.data.model.HaversineMath
import com.example.data.model.HealthMetric
import com.example.data.model.HeatmapDay
import com.example.data.model.UserProfile
import com.example.ui.components.NoirActivityHeatmap
import com.example.ui.components.NoirBarChart
import com.example.ui.components.NoirCard
import com.example.ui.components.NoirDistributionBars
import com.example.ui.components.NoirDivider
import com.example.ui.components.NoirEmptyState
import com.example.ui.components.NoirLineChart
import com.example.ui.components.NoirSectionHeader
import com.example.ui.components.NoirSelectableChip
import com.example.ui.theme.NoirTextMuted
import com.example.ui.theme.NoirTextPrimary

@Composable
fun AnalyticsScreen(
    profile: UserProfile,
    selectedRange: AnalyticsRange,
    onSelectRange: (AnalyticsRange) -> Unit,
    snapshot: AnalyticsSnapshot,
    healthMetrics: List<HealthMetric>,
    heatmapDays: List<HeatmapDay>
) {
    val unit = profile.distanceUnitEnum
    val weightTrend = remember(healthMetrics, profile.unitWeight) {
        healthMetrics.sortedBy { it.dateKey }.takeLast(12).mapNotNull { m ->
            m.weightKg?.let { HaversineMath.formatShortDate(m.dateKey) to HaversineMath.convertWeight(it, profile.weightUnitEnum) }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            NoirSectionHeader(
                eyebrow = "Performance Intelligence",
                title = "Analytics"
            )
        }

        // Timeframe Switcher (7D / 30D / 90D / 1Y / ALL)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnalyticsRange.entries.forEach { range ->
                    NoirSelectableChip(
                        label = range.label,
                        selected = selectedRange == range,
                        onClick = { onSelectRange(range) },
                        tag = "analytics_range_${range.name.lowercase()}"
                    )
                }
            }
        }

        // Volume Summary Grid
        item {
            NoirCard(padding = PaddingValues(18.dp)) {
                Text("DISTANCE VOLUME", style = MaterialTheme.typography.labelMedium, color = NoirTextMuted)
                Spacer(modifier = Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    AnalyticsStat("WEEKLY", HaversineMath.formatDistance(snapshot.weeklyDistanceMeters, unit))
                    AnalyticsStat("MONTHLY", HaversineMath.formatDistance(snapshot.monthlyDistanceMeters, unit))
                    AnalyticsStat("YEARLY", HaversineMath.formatDistance(snapshot.yearlyDistanceMeters, unit))
                }
                Spacer(modifier = Modifier.height(14.dp))
                NoirDivider(subtle = true)
                Spacer(modifier = Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    AnalyticsStat("ACTIVE TIME", HaversineMath.formatDuration(snapshot.totalActiveSeconds))
                    AnalyticsStat("SESSIONS", snapshot.totalActivities.toString())
                    AnalyticsStat("CONSISTENCY", "${snapshot.weeklyConsistencyPercent}%")
                }
                Spacer(modifier = Modifier.height(14.dp))
                NoirDivider(subtle = true)
                Spacer(modifier = Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    AnalyticsStat("AVG RUN PACE", HaversineMath.formatPace(snapshot.avgRunningPaceSecPerKm, unit))
                    AnalyticsStat("BEST PACE", HaversineMath.formatPace(snapshot.bestRunningPaceSecPerKm, unit))
                    AnalyticsStat("LONGEST", HaversineMath.formatDistance(snapshot.longestActivityMeters, unit))
                }
            }
        }

        if (snapshot.totalActivities == 0) {
            item {
                NoirEmptyState(
                    title = "No activities in ${selectedRange.label}",
                    description = "Every chart derives directly from your recorded workouts. Complete a session or widen your date range."
                )
            }
        } else {
            item {
                NoirCard(padding = PaddingValues(18.dp)) {
                    Text("WEEKLY DISTANCE BAR CHART", style = MaterialTheme.typography.labelMedium, color = NoirTextMuted)
                    Spacer(modifier = Modifier.height(10.dp))
                    NoirBarChart(
                        data = snapshot.weeklyBars,
                        unitSuffix = "km",
                        emptyLabel = "No distance logged over the last 7 days."
                    )
                }
            }

            item {
                NoirCard(padding = PaddingValues(18.dp)) {
                    Text("PERIOD VOLUME TREND (${selectedRange.label})", style = MaterialTheme.typography.labelMedium, color = NoirTextMuted)
                    Spacer(modifier = Modifier.height(10.dp))
                    NoirLineChart(
                        data = snapshot.trendSeries,
                        unitSuffix = "km",
                        emptyLabel = "Record workouts across multiple dates to plot volume trend."
                    )
                }
            }

            item {
                NoirCard(padding = PaddingValues(18.dp)) {
                    Text("ACTIVITY DISTRIBUTION", style = MaterialTheme.typography.labelMedium, color = NoirTextMuted)
                    Spacer(modifier = Modifier.height(12.dp))
                    NoirDistributionBars(
                        items = snapshot.distributionByType.map { it.first.label to it.second }
                    )
                }
            }

            item {
                NoirCard(padding = PaddingValues(18.dp)) {
                    Text("RUNNING PACE PROGRESSION (MIN/KM)", style = MaterialTheme.typography.labelMedium, color = NoirTextMuted)
                    Spacer(modifier = Modifier.height(10.dp))
                    NoirLineChart(
                        data = snapshot.paceProgression,
                        unitSuffix = "m/k",
                        emptyLabel = "Record 2+ runs with distance to chart pace progression."
                    )
                }
            }
        }

        item {
            NoirCard(padding = PaddingValues(18.dp)) {
                Text("BODY WEIGHT TREND", style = MaterialTheme.typography.labelMedium, color = NoirTextMuted)
                Spacer(modifier = Modifier.height(10.dp))
                NoirLineChart(
                    data = weightTrend,
                    unitSuffix = profile.weightUnitEnum.shortLabel,
                    emptyLabel = "Log weight on 2+ days in Health to view biometric trends."
                )
            }
        }

        item {
            NoirCard(padding = PaddingValues(18.dp)) {
                Text("12-WEEK ACTIVITY HEATMAP", style = MaterialTheme.typography.labelMedium, color = NoirTextMuted)
                Spacer(modifier = Modifier.height(12.dp))
                NoirActivityHeatmap(days = heatmapDays)
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun AnalyticsStat(label: String, value: String) {
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
