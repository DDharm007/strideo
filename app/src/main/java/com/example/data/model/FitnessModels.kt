package com.example.data.model

import java.util.Calendar
import java.util.Locale

enum class ActivityType(val label: String, val metValue: Double) {
    RUNNING("Running", 9.8),
    WALKING("Walking", 3.8),
    CYCLING("Cycling", 7.5),
    HIKING("Hiking", 6.0),
    GYM("Gym", 5.5),
    OTHER("Custom", 4.5);

    companion object {
        fun fromRaw(raw: String?): ActivityType =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: OTHER
    }
}

enum class DistanceUnit(val label: String, val shortLabel: String) {
    KM("Kilometers", "km"),
    MI("Miles", "mi");

    companion object {
        fun fromRaw(raw: String?): DistanceUnit =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: KM
    }
}

enum class WeightUnit(val label: String, val shortLabel: String) {
    KG("Kilograms", "kg"),
    LB("Pounds", "lb");

    companion object {
        fun fromRaw(raw: String?): WeightUnit =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: KG
    }
}

enum class GoalMetricType(
    val title: String,
    val defaultUnit: String,
    val defaultPeriod: GoalPeriod,
    val defaultTarget: Double
) {
    DAILY_STEPS("Daily Steps", "steps", GoalPeriod.DAILY, 10000.0),
    WEEKLY_RUN_KM("Weekly Running Distance", "km", GoalPeriod.WEEKLY, 30.0),
    MONTHLY_CYCLE_KM("Monthly Cycling Distance", "km", GoalPeriod.MONTHLY, 150.0),
    WEEKLY_WORKOUTS("Weekly Workout Frequency", "sessions", GoalPeriod.WEEKLY, 5.0),
    DAILY_WATER_ML("Daily Water Intake", "ml", GoalPeriod.DAILY, 2500.0),
    DAILY_SLEEP_MIN("Daily Sleep Target", "hrs", GoalPeriod.DAILY, 480.0),
    DAILY_CALORIES("Daily Active Calories", "kcal", GoalPeriod.DAILY, 600.0),
    DAILY_ACTIVE_MIN("Daily Active Minutes", "min", GoalPeriod.DAILY, 45.0),
    DAILY_DISTANCE_KM("Daily Distance", "km", GoalPeriod.DAILY, 8.0);

    companion object {
        fun fromRaw(raw: String?): GoalMetricType =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: DAILY_STEPS
    }
}

enum class GoalPeriod(val label: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly");

    companion object {
        fun fromRaw(raw: String?): GoalPeriod =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: DAILY
    }
}

enum class AnalyticsRange(val label: String, val days: Int?) {
    DAYS_7("7D", 7),
    DAYS_30("30D", 30),
    DAYS_90("90D", 90),
    YEAR_1("1Y", 365),
    ALL("ALL", null)
}

enum class RecordingPhase {
    IDLE,
    RECORDING,
    PAUSED,
    COMPLETED
}

enum class MapDisplayType(val label: String, val shortCode: String) {
    STANDARD("Noir Dark", "DRK"),
    STREET("Street", "MAP"),
    SATELLITE("Satellite", "SAT"),
    TERRAIN("Terrain", "TER")
}

enum class ActivitySortOrder(val label: String) {
    DATE_DESC("Newest"),
    DISTANCE_DESC("Distance"),
    DURATION_DESC("Duration")
}

data class RoutePoint(
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val alt: Double = 0.0,
    val acc: Double = 0.0,
    val ts: Long = 0L
)

data class UserProfile(
    val id: String = LOCAL_PROFILE_ID,
    val userId: String = LOCAL_PROFILE_ID,
    val displayName: String = "Athlete",
    val birthDay: Int = 1,
    val birthMonth: Int = 1,
    val birthYear: Int = 1995,
    val email: String = "",
    val photoUrl: String? = null,
    val heightCm: Double? = null,
    val weightKg: Double? = null,
    val unitDistance: String = DistanceUnit.KM.name,
    val unitWeight: String = WeightUnit.KG.name,
    val use24HourClock: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val privateProfile: Boolean = true,
    val timezone: String? = null,
    val signedIn: Boolean = true,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis()
) {
    val distanceUnitEnum: DistanceUnit get() = DistanceUnit.fromRaw(unitDistance)
    val weightUnitEnum: WeightUnit get() = WeightUnit.fromRaw(unitWeight)

    val ageYears: Int
        get() {
            val today = Calendar.getInstance()
            val currentYear = today.get(Calendar.YEAR)
            val currentMonth = today.get(Calendar.MONTH) + 1
            val currentDay = today.get(Calendar.DAY_OF_MONTH)
            var age = currentYear - birthYear
            if (currentMonth < birthMonth || (currentMonth == birthMonth && currentDay < birthDay)) {
                age--
            }
            return age.coerceIn(0, 120)
        }

    val birthDateFormatted: String
        get() {
            val monthName = MONTH_SHORT_NAMES.getOrElse((birthMonth - 1).coerceIn(0, 11)) { "Jan" }
            return String.format(Locale.US, "%02d %s %04d", birthDay.coerceIn(1, 31), monthName, birthYear)
        }

    companion object {
        const val LOCAL_PROFILE_ID = "local_athlete"
        val MONTH_SHORT_NAMES = listOf(
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
        )
    }
}

data class ActivityRecord(
    val id: String = "",
    val userId: String = UserProfile.LOCAL_PROFILE_ID,
    val title: String = "",
    val type: String = ActivityType.RUNNING.name,
    val startedAtEpochMs: Long = 0L,
    val durationSec: Long = 0L,
    val movingDurationSec: Long = 0L,
    val distanceMeters: Double = 0.0,
    val avgSpeedMps: Double = 0.0,
    val avgPaceSecPerKm: Double = 0.0,
    val caloriesBurned: Long = 0L,
    val elevationGainMeters: Double = 0.0,
    val manualEntry: Boolean = false,
    val pointCount: Long = 0L,
    val routePoints: List<RoutePoint> = emptyList(),
    val notes: String? = null,
    val locationName: String? = null,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis()
) {
    val activityTypeEnum: ActivityType get() = ActivityType.fromRaw(type)
}

data class HealthMetric(
    val id: String = "",
    val userId: String = UserProfile.LOCAL_PROFILE_ID,
    val dateKey: String = "",
    val steps: Long = 0L,
    val caloriesBurned: Long = 0L,
    val waterMl: Long = 0L,
    val weightKg: Double? = null,
    val heightCm: Double? = null,
    val sleepMinutes: Long? = null,
    val sleepQuality: Long? = null,
    val restingHeartRate: Long? = null,
    val systolicBp: Long? = null,
    val diastolicBp: Long? = null,
    val caloriesConsumed: Long? = null,
    val moodLevel: Long? = null,
    val energyLevel: Long? = null,
    val notes: String? = null,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

data class HydrationLog(
    val id: String = "",
    val userId: String = UserProfile.LOCAL_PROFILE_ID,
    val dateKey: String = "",
    val amountMl: Long = 0L,
    val loggedAtEpochMs: Long = 0L,
    val label: String? = null,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

data class SleepLog(
    val id: String = "",
    val userId: String = UserProfile.LOCAL_PROFILE_ID,
    val dateKey: String = "",
    val bedtimeLabel: String = "23:00",
    val wakeTimeLabel: String = "07:00",
    val durationMinutes: Long = 480L,
    val qualityRating: Long = 3L,
    val notes: String? = null,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

data class FitnessGoal(
    val id: String = "",
    val userId: String = UserProfile.LOCAL_PROFILE_ID,
    val metricType: String = GoalMetricType.DAILY_STEPS.name,
    val targetValue: Double = 10000.0,
    val unit: String = "steps",
    val period: String = GoalPeriod.DAILY.name,
    val active: Boolean = true,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis()
) {
    val metricTypeEnum: GoalMetricType get() = GoalMetricType.fromRaw(metricType)
    val periodEnum: GoalPeriod get() = GoalPeriod.fromRaw(period)
}

data class LiveRecordingState(
    val phase: RecordingPhase = RecordingPhase.IDLE,
    val activityType: ActivityType = ActivityType.RUNNING,
    val startedAtEpochMs: Long = 0L,
    val elapsedSec: Long = 0L,
    val movingSec: Long = 0L,
    val distanceMeters: Double = 0.0,
    val currentSpeedMps: Double = 0.0,
    val avgSpeedMps: Double = 0.0,
    val currentPaceSecPerKm: Double = 0.0,
    val avgPaceSecPerKm: Double = 0.0,
    val caloriesBurned: Long = 0L,
    val elevationGainMeters: Double = 0.0,
    val gpsAccuracyMeters: Float? = null,
    val currentLat: Double? = null,
    val currentLng: Double? = null,
    val gpsStatusMessage: String = "Ready for GPS fix",
    val points: List<RoutePoint> = emptyList(),
    val isSubmitting: Boolean = false
)

data class GoalProgressItem(
    val goal: FitnessGoal,
    val currentValue: Double,
    val percentComplete: Float
)

data class StreakMetrics(
    val currentStreakDays: Int,
    val longestStreakDays: Int,
    val activeDaysThisWeek: Int,
    val achievements: List<AchievementBadge>
)

data class AchievementBadge(
    val id: String,
    val title: String,
    val detail: String,
    val unlocked: Boolean
)

data class HeatmapDay(
    val dateKey: String,
    val intensityLevel: Int,
    val activityCount: Int,
    val distanceMeters: Double
)

data class AnalyticsSnapshot(
    val weeklyDistanceMeters: Double,
    val monthlyDistanceMeters: Double,
    val yearlyDistanceMeters: Double,
    val rangeDistanceMeters: Double,
    val totalActiveSeconds: Long,
    val avgRunningPaceSecPerKm: Double,
    val bestRunningPaceSecPerKm: Double,
    val longestActivityMeters: Double,
    val totalActivities: Int,
    val weeklyConsistencyPercent: Int,
    val weeklyBars: List<Pair<String, Double>>,
    val trendSeries: List<Pair<String, Double>>,
    val distributionByType: List<Pair<ActivityType, Int>>,
    val paceProgression: List<Pair<String, Double>>
)

data class AppNotificationItem(
    val id: String,
    val title: String,
    val body: String,
    val timestampLabel: String,
    val unread: Boolean = true
)

sealed interface UiDataState<out T> {
    data object Loading : UiDataState<Nothing>
    data class Success<T>(val data: T) : UiDataState<T>
    data class Error(val message: String) : UiDataState<Nothing>
}
