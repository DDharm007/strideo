package com.example.data.model

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

object HaversineMath {
    private const val EARTH_RADIUS_METERS = 6_371_000.0
    const val METERS_PER_MILE = 1609.344
    const val LB_PER_KG = 2.2046226218

    fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        if (!lat1.isFinite() || !lon1.isFinite() || !lat2.isFinite() || !lon2.isFinite()) return 0.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(max(0.0, 1.0 - a)))
        return EARTH_RADIUS_METERS * c
    }

    fun calculateElevationGain(points: List<RoutePoint>): Double {
        if (points.size < 2) return 0.0
        var gain = 0.0
        for (i in 1 until points.size) {
            val delta = points[i].alt - points[i - 1].alt
            if (delta > 1.0 && delta < 80.0) {
                gain += delta
            }
        }
        return gain
    }

    fun estimateCalories(activityType: ActivityType, durationSec: Long, weightKg: Double?): Long {
        val weight = (weightKg ?: 70.0).coerceIn(35.0, 200.0)
        val hours = durationSec.coerceAtLeast(0L) / 3600.0
        return (activityType.metValue * weight * hours).roundToInt().toLong().coerceAtLeast(0L)
    }

    fun calculateBmi(weightKg: Double?, heightCm: Double?): Double? {
        if (weightKg == null || heightCm == null || weightKg <= 0.0 || heightCm <= 0.0) return null
        val heightM = heightCm / 100.0
        return weightKg / (heightM * heightM)
    }

    fun downsampleRoute(points: List<RoutePoint>, maxPoints: Int = 450): List<RoutePoint> {
        if (points.size <= maxPoints) return points
        val result = ArrayList<RoutePoint>(maxPoints)
        val step = (points.size - 1).toDouble() / (maxPoints - 1).toDouble()
        for (i in 0 until maxPoints - 1) {
            val idx = (i * step).toInt().coerceIn(0, points.lastIndex)
            result.add(points[idx])
        }
        result.add(points.last())
        return result
    }

    fun formatDistance(meters: Double, unit: DistanceUnit): String {
        val value = when (unit) {
            DistanceUnit.KM -> meters / 1000.0
            DistanceUnit.MI -> meters / METERS_PER_MILE
        }
        return String.format(Locale.US, "%.2f %s", value, unit.shortLabel)
    }

    fun formatDistanceValue(meters: Double, unit: DistanceUnit): String {
        val value = when (unit) {
            DistanceUnit.KM -> meters / 1000.0
            DistanceUnit.MI -> meters / METERS_PER_MILE
        }
        return String.format(Locale.US, "%.2f", value)
    }

    fun convertWeight(kg: Double, unit: WeightUnit): Double = when (unit) {
        WeightUnit.KG -> kg
        WeightUnit.LB -> kg * LB_PER_KG
    }

    fun formatWeight(kg: Double?, unit: WeightUnit): String {
        if (kg == null) return "—"
        val value = convertWeight(kg, unit)
        return String.format(Locale.US, "%.1f %s", value, unit.shortLabel)
    }

    fun formatSpeed(speedMps: Double, unit: DistanceUnit): String {
        if (speedMps <= 0.05) return "0.0 ${if (unit == DistanceUnit.KM) "km/h" else "mph"}"
        val value = when (unit) {
            DistanceUnit.KM -> speedMps * 3.6
            DistanceUnit.MI -> speedMps * 2.23693629
        }
        val suffix = if (unit == DistanceUnit.KM) "km/h" else "mph"
        return String.format(Locale.US, "%.1f %s", value, suffix)
    }

    fun formatPace(paceSecPerKm: Double, unit: DistanceUnit): String {
        if (paceSecPerKm <= 1.0 || paceSecPerKm > 3600.0) return "— /${unit.shortLabel}"
        val adjustedSec = when (unit) {
            DistanceUnit.KM -> paceSecPerKm
            DistanceUnit.MI -> paceSecPerKm * (METERS_PER_MILE / 1000.0)
        }.roundToInt()
        val mins = adjustedSec / 60
        val secs = adjustedSec % 60
        return String.format(Locale.US, "%d:%02d /%s", mins, secs, unit.shortLabel)
    }

    fun formatDuration(totalSec: Long): String {
        val clamped = totalSec.coerceAtLeast(0L)
        val hours = clamped / 3600
        val minutes = (clamped % 3600) / 60
        val seconds = clamped % 60
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    fun formatSleepDuration(minutes: Long?): String {
        if (minutes == null || minutes <= 0L) return "—"
        val h = minutes / 60
        val m = minutes % 60
        return "${h}h ${m}m"
    }

    fun dateKeyForMillis(epochMs: Long): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return fmt.format(Date(epochMs))
    }

    fun todayDateKey(): String = dateKeyForMillis(System.currentTimeMillis())

    fun dateKeyDaysAgo(daysAgo: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
        return dateKeyForMillis(cal.timeInMillis)
    }

    fun formatDisplayDate(epochMs: Long, use24Hour: Boolean): String {
        val pattern = if (use24Hour) "MMM d, yyyy · HH:mm" else "MMM d, yyyy · h:mm a"
        return SimpleDateFormat(pattern, Locale.US).format(Date(epochMs))
    }

    fun formatShortDate(dateKey: String): String {
        return try {
            val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateKey)
            if (parsed != null) SimpleDateFormat("MMM d", Locale.US).format(parsed) else dateKey
        } catch (_: Exception) {
            dateKey
        }
    }

    fun computeStreaks(activities: List<ActivityRecord>, healthMetrics: List<HealthMetric>): StreakMetrics {
        val activeDates = mutableSetOf<String>()
        activities.forEach { activeDates.add(dateKeyForMillis(it.startedAtEpochMs)) }
        healthMetrics.filter { it.steps >= 5000L }.forEach { activeDates.add(it.dateKey) }

        var currentStreak = 0
        val today = todayDateKey()
        val yesterday = dateKeyDaysAgo(1)
        val startOffset = when {
            activeDates.contains(today) -> 0
            activeDates.contains(yesterday) -> 1
            else -> -1
        }
        if (startOffset >= 0) {
            var day = startOffset
            while (activeDates.contains(dateKeyDaysAgo(day)) && day < 3650) {
                currentStreak++
                day++
            }
        }

        // Longest streak across last 365 days
        var longestStreak = 0
        var running = 0
        for (daysAgo in 364 downTo 0) {
            if (activeDates.contains(dateKeyDaysAgo(daysAgo))) {
                running++
                if (running > longestStreak) longestStreak = running
            } else {
                running = 0
            }
        }

        var activeThisWeek = 0
        for (d in 0..6) {
            if (activeDates.contains(dateKeyDaysAgo(d))) activeThisWeek++
        }

        val totalDistanceKm = activities.sumOf { it.distanceMeters } / 1000.0
        val achievements = listOf(
            AchievementBadge(
                id = "first_session",
                title = "First Mark",
                detail = "Record your first activity",
                unlocked = activities.isNotEmpty()
            ),
            AchievementBadge(
                id = "streak_7",
                title = "Seven-Day Discipline",
                detail = "Maintain a 7-day movement streak",
                unlocked = longestStreak >= 7
            ),
            AchievementBadge(
                id = "dist_50",
                title = "50 km Threshold",
                detail = "Accumulate 50 km across sessions",
                unlocked = totalDistanceKm >= 50.0
            ),
            AchievementBadge(
                id = "week_4",
                title = "Weekly Cadence",
                detail = "Log activity on 4+ days this week",
                unlocked = activeThisWeek >= 4
            )
        )

        return StreakMetrics(
            currentStreakDays = currentStreak,
            longestStreakDays = longestStreak,
            activeDaysThisWeek = activeThisWeek,
            achievements = achievements
        )
    }

    fun computeHeatmap(activities: List<ActivityRecord>, days: Int = 84): List<HeatmapDay> {
        val byDate = activities.groupBy { dateKeyForMillis(it.startedAtEpochMs) }
        return (days - 1 downTo 0).map { offset ->
            val key = dateKeyDaysAgo(offset)
            val list = byDate[key].orEmpty()
            val dist = list.sumOf { it.distanceMeters }
            val minutes = list.sumOf { it.durationSec } / 60L
            val intensity = when {
                list.isEmpty() -> 0
                dist >= 12_000.0 || minutes >= 75L -> 4
                dist >= 7_000.0 || minutes >= 45L -> 3
                dist >= 3_000.0 || minutes >= 20L -> 2
                else -> 1
            }
            HeatmapDay(
                dateKey = key,
                intensityLevel = intensity,
                activityCount = list.size,
                distanceMeters = dist
            )
        }
    }

    fun computeAnalytics(activities: List<ActivityRecord>, range: AnalyticsRange): AnalyticsSnapshot {
        val now = System.currentTimeMillis()
        val dayMs = 86_400_000L

        val weekCutoff = now - 7L * dayMs
        val monthCutoff = now - 30L * dayMs
        val yearCutoff = now - 365L * dayMs
        val rangeCutoff = range.days?.let { now - it.toLong() * dayMs } ?: 0L

        val filtered = activities.filter { it.startedAtEpochMs >= rangeCutoff }
            .sortedBy { it.startedAtEpochMs }

        val weeklyDistance = activities.filter { it.startedAtEpochMs >= weekCutoff }.sumOf { it.distanceMeters }
        val monthlyDistance = activities.filter { it.startedAtEpochMs >= monthCutoff }.sumOf { it.distanceMeters }
        val yearlyDistance = activities.filter { it.startedAtEpochMs >= yearCutoff }.sumOf { it.distanceMeters }
        val rangeDistance = filtered.sumOf { it.distanceMeters }
        val totalActiveSec = filtered.sumOf { it.durationSec }

        val runs = filtered.filter { it.activityTypeEnum == ActivityType.RUNNING && it.avgPaceSecPerKm > 60.0 }
        val avgRunPace = if (runs.isEmpty()) 0.0 else runs.map { it.avgPaceSecPerKm }.average()
        val bestRunPace = runs.minOfOrNull { it.avgPaceSecPerKm } ?: 0.0
        val longest = filtered.maxOfOrNull { it.distanceMeters } ?: 0.0

        val activeDaysLast7 = (0..6).count { offset ->
            val key = dateKeyDaysAgo(offset)
            activities.any { dateKeyForMillis(it.startedAtEpochMs) == key }
        }
        val weeklyConsistency = ((activeDaysLast7 / 7.0) * 100.0).roundToInt()

        val dayFmt = SimpleDateFormat("EEE", Locale.US)
        val weeklyBars = (6 downTo 0).map { offset ->
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -offset)
            val key = dateKeyForMillis(cal.timeInMillis)
            val label = dayFmt.format(cal.time).uppercase(Locale.US).take(3)
            val distKm = activities
                .filter { dateKeyForMillis(it.startedAtEpochMs) == key }
                .sumOf { it.distanceMeters } / 1000.0
            label to distKm
        }

        val bucketCount = when (range) {
            AnalyticsRange.DAYS_7 -> 7
            AnalyticsRange.DAYS_30 -> 6
            AnalyticsRange.DAYS_90 -> 6
            AnalyticsRange.YEAR_1 -> 6
            AnalyticsRange.ALL -> 6
        }
        val spanDays = range.days ?: 90
        val stepDays = max(1, spanDays / bucketCount)
        val trendSeries = (bucketCount - 1 downTo 0).map { b ->
            val endOffset = b * stepDays
            val startOffset = endOffset + stepDays - 1
            val startKey = dateKeyDaysAgo(startOffset)
            val endKey = dateKeyDaysAgo(endOffset)
            val distKm = filtered.filter {
                val dk = dateKeyForMillis(it.startedAtEpochMs)
                dk >= startKey && dk <= endKey
            }.sumOf { it.distanceMeters } / 1000.0
            formatShortDate(endKey) to distKm
        }

        val distribution = ActivityType.entries.mapNotNull { type ->
            val count = filtered.count { it.activityTypeEnum == type }
            if (count > 0) type to count else null
        }.sortedByDescending { it.second }

        val paceSeries = runs.takeLast(8).map { run ->
            formatShortDate(dateKeyForMillis(run.startedAtEpochMs)) to (run.avgPaceSecPerKm / 60.0)
        }

        return AnalyticsSnapshot(
            weeklyDistanceMeters = weeklyDistance,
            monthlyDistanceMeters = monthlyDistance,
            yearlyDistanceMeters = yearlyDistance,
            rangeDistanceMeters = rangeDistance,
            totalActiveSeconds = totalActiveSec,
            avgRunningPaceSecPerKm = avgRunPace,
            bestRunningPaceSecPerKm = bestRunPace,
            longestActivityMeters = longest,
            totalActivities = filtered.size,
            weeklyConsistencyPercent = weeklyConsistency,
            weeklyBars = weeklyBars,
            trendSeries = trendSeries,
            distributionByType = distribution,
            paceProgression = paceSeries
        )
    }

    fun exportToCsv(
        activities: List<ActivityRecord>,
        healthMetrics: List<HealthMetric>
    ): String {
        val sb = StringBuilder()
        sb.appendLine("# STRIDEO ACTIVITIES")
        sb.appendLine("date,title,type,distance_m,duration_sec,pace_sec_km,calories,elevation_m,manual")
        activities.forEach { a ->
            val safeTitle = a.title.replace("\"", "\"\"")
            sb.appendLine(
                "${dateKeyForMillis(a.startedAtEpochMs)},\"$safeTitle\",${a.type},${a.distanceMeters},${a.durationSec},${a.avgPaceSecPerKm},${a.caloriesBurned},${a.elevationGainMeters},${a.manualEntry}"
            )
        }
        sb.appendLine()
        sb.appendLine("# STRIDEO DAILY HEALTH METRICS")
        sb.appendLine("date,steps,calories_burned,water_ml,weight_kg,sleep_min,resting_hr,mood,energy")
        healthMetrics.forEach { h ->
            sb.appendLine(
                "${h.dateKey},${h.steps},${h.caloriesBurned},${h.waterMl},${h.weightKg ?: ""},${h.sleepMinutes ?: ""},${h.restingHeartRate ?: ""},${h.moodLevel ?: ""},${h.energyLevel ?: ""}"
            )
        }
        return sb.toString()
    }

    fun exportToJson(
        profile: UserProfile?,
        activities: List<ActivityRecord>,
        healthMetrics: List<HealthMetric>,
        goals: List<FitnessGoal>
    ): String {
        val root = JSONObject()
        root.put("app", "Strideo")
        root.put("exportedAt", dateKeyForMillis(System.currentTimeMillis()))
        profile?.let {
            root.put("profile", JSONObject().apply {
                put("displayName", it.displayName)
                put("birthDate", it.birthDateFormatted)
                put("ageYears", it.ageYears)
                put("heightCm", it.heightCm)
                put("weightKg", it.weightKg)
                put("unitDistance", it.unitDistance)
                put("unitWeight", it.unitWeight)
            })
        }
        root.put("activities", JSONArray().apply {
            activities.forEach { a ->
                put(JSONObject().apply {
                    put("date", dateKeyForMillis(a.startedAtEpochMs))
                    put("title", a.title)
                    put("type", a.type)
                    put("distanceMeters", a.distanceMeters)
                    put("durationSec", a.durationSec)
                    put("caloriesBurned", a.caloriesBurned)
                    put("elevationGainMeters", a.elevationGainMeters)
                    put("pointCount", a.pointCount)
                })
            }
        })
        root.put("healthMetrics", JSONArray().apply {
            healthMetrics.forEach { h ->
                put(JSONObject().apply {
                    put("dateKey", h.dateKey)
                    put("steps", h.steps)
                    put("caloriesBurned", h.caloriesBurned)
                    put("waterMl", h.waterMl)
                    put("weightKg", h.weightKg)
                    put("sleepMinutes", h.sleepMinutes)
                    put("restingHeartRate", h.restingHeartRate)
                })
            }
        })
        root.put("goals", JSONArray().apply {
            goals.forEach { g ->
                put(JSONObject().apply {
                    put("metricType", g.metricType)
                    put("targetValue", g.targetValue)
                    put("unit", g.unit)
                    put("period", g.period)
                })
            }
        })
        return root.toString(2)
    }
}
