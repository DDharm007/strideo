package com.example.data.repository

import android.content.Context
import com.example.data.local.ActivityEntity
import com.example.data.local.GoalEntity
import com.example.data.local.HealthMetricEntity
import com.example.data.local.HydrationLogEntity
import com.example.data.local.SleepLogEntity
import com.example.data.local.StrideoDao
import com.example.data.local.UserProfileEntity
import com.example.data.model.ActivityRecord
import com.example.data.model.DistanceUnit
import com.example.data.model.FitnessGoal
import com.example.data.model.GoalMetricType
import com.example.data.model.HaversineMath
import com.example.data.model.HealthMetric
import com.example.data.model.HydrationLog
import com.example.data.model.SleepLog
import com.example.data.model.UserProfile
import com.example.data.model.WeightUnit
import com.example.widget.StrideoWidgetUpdater
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.TimeZone
import java.util.UUID

interface FitnessRepository {
    fun observeProfile(): Flow<UserProfile?>
    fun observeActivities(): Flow<List<ActivityRecord>>
    fun observeHealthMetrics(): Flow<List<HealthMetric>>
    fun observeHydrationLogs(): Flow<List<HydrationLog>>
    fun observeSleepLogs(): Flow<List<SleepLog>>
    fun observeGoals(): Flow<List<FitnessGoal>>

    suspend fun loginOrCreateLocalProfile(
        displayName: String,
        birthDay: Int,
        birthMonth: Int,
        birthYear: Int,
        heightCm: Double,
        weightKg: Double
    ): Result<Unit>

    suspend fun resumeExistingProfile(): Result<Unit>
    suspend fun signOutLocal(): Result<Unit>
    suspend fun saveProfile(profile: UserProfile): Result<Unit>
    suspend fun createActivity(activity: ActivityRecord): Result<String>
    suspend fun updateActivity(activityId: String, title: String, notes: String?, locationName: String?): Result<Unit>
    suspend fun deleteActivity(activityId: String): Result<Unit>
    suspend fun upsertHealthMetric(metric: HealthMetric): Result<Unit>
    suspend fun addHydrationLog(dateKey: String, amountMl: Long, label: String?): Result<Unit>
    suspend fun deleteHydrationLog(logId: String, dateKey: String, amountMl: Long): Result<Unit>
    suspend fun addSleepLog(log: SleepLog): Result<Unit>
    suspend fun deleteSleepLog(logId: String): Result<Unit>
    suspend fun upsertGoal(goal: FitnessGoal): Result<Unit>
    suspend fun deleteGoal(goalId: String): Result<Unit>
    suspend fun deleteAllUserData(): Result<Unit>
}

class RoomFitnessRepository(
    private val context: Context,
    private val dao: StrideoDao
) : FitnessRepository {

    private val appContext = context.applicationContext

    private fun refreshWidgets() {
        StrideoWidgetUpdater.refreshAll(appContext)
    }

    override fun observeProfile(): Flow<UserProfile?> =
        dao.observeProfile().map { it?.toDomain() }

    override fun observeActivities(): Flow<List<ActivityRecord>> =
        dao.observeActivities().map { list -> list.map { it.toDomain() } }

    override fun observeHealthMetrics(): Flow<List<HealthMetric>> =
        dao.observeHealthMetrics().map { list -> list.map { it.toDomain() } }

    override fun observeHydrationLogs(): Flow<List<HydrationLog>> =
        dao.observeHydrationLogs().map { list -> list.map { it.toDomain() } }

    override fun observeSleepLogs(): Flow<List<SleepLog>> =
        dao.observeSleepLogs().map { list -> list.map { it.toDomain() } }

    override fun observeGoals(): Flow<List<FitnessGoal>> =
        dao.observeGoals().map { list -> list.map { it.toDomain() } }

    override suspend fun loginOrCreateLocalProfile(
        displayName: String,
        birthDay: Int,
        birthMonth: Int,
        birthYear: Int,
        heightCm: Double,
        weightKg: Double
    ): Result<Unit> = runSafe {
        val now = System.currentTimeMillis()
        val existing = dao.getProfileOnce()
        val safeName = displayName.trim().ifBlank { "Athlete" }.take(80)
        val safeDay = birthDay.coerceIn(1, 31)
        val safeMonth = birthMonth.coerceIn(1, 12)
        val safeYear = birthYear.coerceIn(1920, 2022)
        val safeHeight = heightCm.coerceIn(50.0, 260.0)
        val safeWeight = weightKg.coerceIn(20.0, 350.0)

        val entity = UserProfileEntity(
            id = UserProfile.LOCAL_PROFILE_ID,
            displayName = safeName,
            birthDay = safeDay,
            birthMonth = safeMonth,
            birthYear = safeYear,
            email = existing?.email ?: "",
            photoUrl = existing?.photoUrl,
            heightCm = safeHeight,
            weightKg = safeWeight,
            unitDistance = existing?.unitDistance ?: DistanceUnit.KM.name,
            unitWeight = existing?.unitWeight ?: WeightUnit.KG.name,
            use24HourClock = existing?.use24HourClock ?: true,
            notificationsEnabled = existing?.notificationsEnabled ?: true,
            privateProfile = existing?.privateProfile ?: true,
            timezone = existing?.timezone ?: TimeZone.getDefault().id,
            signedIn = true,
            createdAtEpochMs = existing?.createdAtEpochMs ?: now,
            updatedAtEpochMs = now
        )
        dao.upsertProfile(entity)

        // Seed default goals on first login if none exist yet
        if (dao.countGoals() == 0) {
            val defaultGoals = listOf(
                GoalMetricType.DAILY_STEPS,
                GoalMetricType.DAILY_CALORIES,
                GoalMetricType.DAILY_ACTIVE_MIN,
                GoalMetricType.DAILY_DISTANCE_KM,
                GoalMetricType.WEEKLY_RUN_KM,
                GoalMetricType.DAILY_WATER_ML,
                GoalMetricType.DAILY_SLEEP_MIN
            )
            for (metric in defaultGoals) {
                dao.upsertGoal(
                    GoalEntity(
                        id = "goal_${metric.name.lowercase()}",
                        metricType = metric.name,
                        targetValue = metric.defaultTarget,
                        unit = metric.defaultUnit,
                        period = metric.defaultPeriod.name,
                        active = true,
                        createdAtEpochMs = now,
                        updatedAtEpochMs = now
                    )
                )
            }
        }
        refreshWidgets()
    }

    override suspend fun resumeExistingProfile(): Result<Unit> = runSafe {
        dao.updateSignedIn(signedIn = true, updatedAt = System.currentTimeMillis())
        refreshWidgets()
    }

    override suspend fun signOutLocal(): Result<Unit> = runSafe {
        dao.updateSignedIn(signedIn = false, updatedAt = System.currentTimeMillis())
        refreshWidgets()
    }

    override suspend fun saveProfile(profile: UserProfile): Result<Unit> = runSafe {
        val now = System.currentTimeMillis()
        val existing = dao.getProfileOnce()
        val entity = UserProfileEntity(
            id = UserProfile.LOCAL_PROFILE_ID,
            displayName = profile.displayName.trim().ifBlank { "Athlete" }.take(80),
            birthDay = profile.birthDay.coerceIn(1, 31),
            birthMonth = profile.birthMonth.coerceIn(1, 12),
            birthYear = profile.birthYear.coerceIn(1920, 2022),
            email = profile.email.trim().take(160),
            photoUrl = profile.photoUrl?.take(500),
            heightCm = profile.heightCm?.coerceIn(50.0, 260.0),
            weightKg = profile.weightKg?.coerceIn(20.0, 350.0),
            unitDistance = profile.distanceUnitEnum.name,
            unitWeight = profile.weightUnitEnum.name,
            use24HourClock = profile.use24HourClock,
            notificationsEnabled = profile.notificationsEnabled,
            privateProfile = profile.privateProfile,
            timezone = (profile.timezone ?: TimeZone.getDefault().id).take(80),
            signedIn = profile.signedIn,
            createdAtEpochMs = existing?.createdAtEpochMs ?: profile.createdAtEpochMs,
            updatedAtEpochMs = now
        )
        dao.upsertProfile(entity)
        refreshWidgets()
    }

    override suspend fun createActivity(activity: ActivityRecord): Result<String> = try {
        val now = System.currentTimeMillis()
        val id = activity.id.ifBlank { "act_${UUID.randomUUID().toString().take(12)}" }
        val safePoints = HaversineMath.downsampleRoute(activity.routePoints, 1500)
        dao.upsertActivity(
            ActivityEntity(
                id = id,
                title = activity.title.trim().ifBlank { "${activity.activityTypeEnum.label} Session" }.take(100),
                type = activity.activityTypeEnum.name,
                startedAtEpochMs = activity.startedAtEpochMs.coerceAtLeast(1L),
                durationSec = activity.durationSec.coerceIn(0L, 604_800L),
                movingDurationSec = activity.movingDurationSec.coerceIn(0L, 604_800L),
                distanceMeters = activity.distanceMeters.coerceIn(0.0, 1_000_000.0),
                avgSpeedMps = activity.avgSpeedMps.coerceIn(0.0, 100.0),
                avgPaceSecPerKm = activity.avgPaceSecPerKm.coerceIn(0.0, 10_000.0),
                caloriesBurned = activity.caloriesBurned.coerceIn(0L, 50_000L),
                elevationGainMeters = activity.elevationGainMeters.coerceIn(0.0, 30_000.0),
                manualEntry = activity.manualEntry,
                pointCount = safePoints.size.toLong(),
                routePoints = safePoints,
                notes = activity.notes?.trim()?.takeIf { it.isNotEmpty() }?.take(1000),
                locationName = activity.locationName?.trim()?.takeIf { it.isNotEmpty() }?.take(120),
                createdAtEpochMs = now,
                updatedAtEpochMs = now
            )
        )
        refreshWidgets()
        Result.success(id)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateActivity(
        activityId: String,
        title: String,
        notes: String?,
        locationName: String?
    ): Result<Unit> = runSafe {
        require(activityId.isNotBlank()) { "Activity ID must not be blank" }
        dao.updateActivityMetadata(
            id = activityId,
            title = title.trim().ifBlank { "Untitled Session" }.take(100),
            notes = notes?.trim()?.takeIf { it.isNotEmpty() }?.take(1000),
            locationName = locationName?.trim()?.takeIf { it.isNotEmpty() }?.take(120),
            updatedAt = System.currentTimeMillis()
        )
        refreshWidgets()
    }

    override suspend fun deleteActivity(activityId: String): Result<Unit> = runSafe {
        require(activityId.isNotBlank()) { "Activity ID must not be blank" }
        dao.deleteActivity(activityId)
        refreshWidgets()
    }

    override suspend fun upsertHealthMetric(metric: HealthMetric): Result<Unit> = runSafe {
        val now = System.currentTimeMillis()
        val dateKey = metric.dateKey.ifBlank { HaversineMath.todayDateKey() }
        val existing = dao.getHealthMetricByDate(dateKey)
        dao.upsertHealthMetric(
            HealthMetricEntity(
                dateKey = dateKey,
                steps = metric.steps.coerceIn(0L, 200_000L),
                caloriesBurned = metric.caloriesBurned.coerceIn(0L, 30_000L),
                waterMl = metric.waterMl.coerceIn(0L, 20_000L),
                weightKg = metric.weightKg?.coerceIn(20.0, 350.0) ?: existing?.weightKg,
                heightCm = metric.heightCm?.coerceIn(50.0, 260.0) ?: existing?.heightCm,
                sleepMinutes = metric.sleepMinutes?.coerceIn(0L, 1440L) ?: existing?.sleepMinutes,
                sleepQuality = metric.sleepQuality?.coerceIn(1L, 5L) ?: existing?.sleepQuality,
                restingHeartRate = metric.restingHeartRate?.coerceIn(30L, 220L) ?: existing?.restingHeartRate,
                systolicBp = metric.systolicBp?.coerceIn(60L, 260L) ?: existing?.systolicBp,
                diastolicBp = metric.diastolicBp?.coerceIn(30L, 180L) ?: existing?.diastolicBp,
                caloriesConsumed = metric.caloriesConsumed?.coerceIn(0L, 25_000L) ?: existing?.caloriesConsumed,
                moodLevel = metric.moodLevel?.coerceIn(1L, 5L) ?: existing?.moodLevel,
                energyLevel = metric.energyLevel?.coerceIn(1L, 5L) ?: existing?.energyLevel,
                notes = metric.notes?.trim()?.takeIf { it.isNotEmpty() }?.take(500) ?: existing?.notes,
                createdAtEpochMs = existing?.createdAtEpochMs ?: now,
                updatedAtEpochMs = now
            )
        )
        refreshWidgets()
    }

    override suspend fun addHydrationLog(dateKey: String, amountMl: Long, label: String?): Result<Unit> = runSafe {
        val now = System.currentTimeMillis()
        val safeAmount = amountMl.coerceIn(1L, 5000L)
        val safeDate = dateKey.ifBlank { HaversineMath.todayDateKey() }
        dao.insertHydrationLog(
            HydrationLogEntity(
                id = "hyd_${UUID.randomUUID().toString().take(12)}",
                dateKey = safeDate,
                amountMl = safeAmount,
                loggedAtEpochMs = now,
                label = label?.trim()?.takeIf { it.isNotEmpty() }?.take(60),
                createdAtEpochMs = now,
                updatedAtEpochMs = now
            )
        )
        val existing = dao.getHealthMetricByDate(safeDate)
        val nextWater = ((existing?.waterMl ?: 0L) + safeAmount).coerceAtMost(20_000L)
        dao.upsertHealthMetric(
            existing?.copy(waterMl = nextWater, updatedAtEpochMs = now)
                ?: HealthMetricEntity(
                    dateKey = safeDate,
                    steps = 0L,
                    caloriesBurned = 0L,
                    waterMl = nextWater,
                    weightKg = null,
                    heightCm = null,
                    sleepMinutes = null,
                    sleepQuality = null,
                    restingHeartRate = null,
                    systolicBp = null,
                    diastolicBp = null,
                    caloriesConsumed = null,
                    moodLevel = null,
                    energyLevel = null,
                    notes = null,
                    createdAtEpochMs = now,
                    updatedAtEpochMs = now
                )
        )
        refreshWidgets()
    }

    override suspend fun deleteHydrationLog(logId: String, dateKey: String, amountMl: Long): Result<Unit> = runSafe {
        require(logId.isNotBlank())
        dao.deleteHydrationLog(logId)
        val existing = dao.getHealthMetricByDate(dateKey)
        if (existing != null) {
            dao.upsertHealthMetric(
                existing.copy(
                    waterMl = (existing.waterMl - amountMl).coerceAtLeast(0L),
                    updatedAtEpochMs = System.currentTimeMillis()
                )
            )
        }
        refreshWidgets()
    }

    override suspend fun addSleepLog(log: SleepLog): Result<Unit> = runSafe {
        val now = System.currentTimeMillis()
        val safeDate = log.dateKey.ifBlank { HaversineMath.todayDateKey() }
        val safeDuration = log.durationMinutes.coerceIn(1L, 1440L)
        val safeQuality = log.qualityRating.coerceIn(1L, 5L)
        dao.insertSleepLog(
            SleepLogEntity(
                id = "slp_${UUID.randomUUID().toString().take(12)}",
                dateKey = safeDate,
                bedtimeLabel = log.bedtimeLabel.trim().ifBlank { "23:00" }.take(20),
                wakeTimeLabel = log.wakeTimeLabel.trim().ifBlank { "07:00" }.take(20),
                durationMinutes = safeDuration,
                qualityRating = safeQuality,
                notes = log.notes?.trim()?.takeIf { it.isNotEmpty() }?.take(400),
                createdAtEpochMs = now,
                updatedAtEpochMs = now
            )
        )
        val existing = dao.getHealthMetricByDate(safeDate)
        dao.upsertHealthMetric(
            existing?.copy(
                sleepMinutes = safeDuration,
                sleepQuality = safeQuality,
                updatedAtEpochMs = now
            ) ?: HealthMetricEntity(
                dateKey = safeDate,
                steps = 0L,
                caloriesBurned = 0L,
                waterMl = 0L,
                weightKg = null,
                heightCm = null,
                sleepMinutes = safeDuration,
                sleepQuality = safeQuality,
                restingHeartRate = null,
                systolicBp = null,
                diastolicBp = null,
                caloriesConsumed = null,
                moodLevel = null,
                energyLevel = null,
                notes = null,
                createdAtEpochMs = now,
                updatedAtEpochMs = now
            )
        )
        refreshWidgets()
    }

    override suspend fun deleteSleepLog(logId: String): Result<Unit> = runSafe {
        require(logId.isNotBlank())
        dao.deleteSleepLog(logId)
        refreshWidgets()
    }

    override suspend fun upsertGoal(goal: FitnessGoal): Result<Unit> = runSafe {
        val now = System.currentTimeMillis()
        val id = goal.id.ifBlank { "goal_${goal.metricTypeEnum.name.lowercase()}" }
        dao.upsertGoal(
            GoalEntity(
                id = id,
                metricType = goal.metricTypeEnum.name,
                targetValue = goal.targetValue.coerceIn(1.0, 1_000_000.0),
                unit = goal.unit.trim().ifBlank { goal.metricTypeEnum.defaultUnit }.take(20),
                period = goal.periodEnum.name,
                active = goal.active,
                createdAtEpochMs = goal.createdAtEpochMs,
                updatedAtEpochMs = now
            )
        )
    }

    override suspend fun deleteGoal(goalId: String): Result<Unit> = runSafe {
        require(goalId.isNotBlank())
        dao.deleteGoal(goalId)
    }

    override suspend fun deleteAllUserData(): Result<Unit> = runSafe {
        dao.clearActivities()
        dao.clearHealthMetrics()
        dao.clearHydrationLogs()
        dao.clearSleepLogs()
        dao.clearGoals()
        dao.clearProfiles()
        refreshWidgets()
    }

    private suspend inline fun runSafe(crossinline block: suspend () -> Unit): Result<Unit> {
        return try {
            block()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun UserProfileEntity.toDomain() = UserProfile(
        id = id,
        userId = id,
        displayName = displayName,
        birthDay = birthDay,
        birthMonth = birthMonth,
        birthYear = birthYear,
        email = email,
        photoUrl = photoUrl,
        heightCm = heightCm,
        weightKg = weightKg,
        unitDistance = unitDistance,
        unitWeight = unitWeight,
        use24HourClock = use24HourClock,
        notificationsEnabled = notificationsEnabled,
        privateProfile = privateProfile,
        timezone = timezone,
        signedIn = signedIn,
        createdAtEpochMs = createdAtEpochMs,
        updatedAtEpochMs = updatedAtEpochMs
    )

    private fun ActivityEntity.toDomain() = ActivityRecord(
        id = id,
        userId = UserProfile.LOCAL_PROFILE_ID,
        title = title,
        type = type,
        startedAtEpochMs = startedAtEpochMs,
        durationSec = durationSec,
        movingDurationSec = movingDurationSec,
        distanceMeters = distanceMeters,
        avgSpeedMps = avgSpeedMps,
        avgPaceSecPerKm = avgPaceSecPerKm,
        caloriesBurned = caloriesBurned,
        elevationGainMeters = elevationGainMeters,
        manualEntry = manualEntry,
        pointCount = pointCount,
        routePoints = routePoints,
        notes = notes,
        locationName = locationName,
        createdAtEpochMs = createdAtEpochMs,
        updatedAtEpochMs = updatedAtEpochMs
    )

    private fun HealthMetricEntity.toDomain() = HealthMetric(
        id = dateKey,
        userId = UserProfile.LOCAL_PROFILE_ID,
        dateKey = dateKey,
        steps = steps,
        caloriesBurned = caloriesBurned,
        waterMl = waterMl,
        weightKg = weightKg,
        heightCm = heightCm,
        sleepMinutes = sleepMinutes,
        sleepQuality = sleepQuality,
        restingHeartRate = restingHeartRate,
        systolicBp = systolicBp,
        diastolicBp = diastolicBp,
        caloriesConsumed = caloriesConsumed,
        moodLevel = moodLevel,
        energyLevel = energyLevel,
        notes = notes,
        createdAtEpochMs = createdAtEpochMs,
        updatedAtEpochMs = updatedAtEpochMs
    )

    private fun HydrationLogEntity.toDomain() = HydrationLog(
        id = id,
        userId = UserProfile.LOCAL_PROFILE_ID,
        dateKey = dateKey,
        amountMl = amountMl,
        loggedAtEpochMs = loggedAtEpochMs,
        label = label,
        createdAtEpochMs = createdAtEpochMs,
        updatedAtEpochMs = updatedAtEpochMs
    )

    private fun SleepLogEntity.toDomain() = SleepLog(
        id = id,
        userId = UserProfile.LOCAL_PROFILE_ID,
        dateKey = dateKey,
        bedtimeLabel = bedtimeLabel,
        wakeTimeLabel = wakeTimeLabel,
        durationMinutes = durationMinutes,
        qualityRating = qualityRating,
        notes = notes,
        createdAtEpochMs = createdAtEpochMs,
        updatedAtEpochMs = updatedAtEpochMs
    )

    private fun GoalEntity.toDomain() = FitnessGoal(
        id = id,
        userId = UserProfile.LOCAL_PROFILE_ID,
        metricType = metricType,
        targetValue = targetValue,
        unit = unit,
        period = period,
        active = active,
        createdAtEpochMs = createdAtEpochMs,
        updatedAtEpochMs = updatedAtEpochMs
    )
}
