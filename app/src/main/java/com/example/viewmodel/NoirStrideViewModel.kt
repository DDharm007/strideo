package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.location.LocationTracker
import com.example.data.location.LocationUpdateEvent
import com.example.data.model.ActivityRecord
import com.example.data.model.ActivitySortOrder
import com.example.data.model.ActivityType
import com.example.data.model.AnalyticsRange
import com.example.data.model.AnalyticsSnapshot
import com.example.data.model.AppNotificationItem
import com.example.data.model.DistanceUnit
import com.example.data.model.FitnessGoal
import com.example.data.model.GoalMetricType
import com.example.data.model.GoalPeriod
import com.example.data.model.GoalProgressItem
import com.example.data.model.HaversineMath
import com.example.data.model.HealthMetric
import com.example.data.model.HeatmapDay
import com.example.data.model.HydrationLog
import com.example.data.model.LiveRecordingState
import com.example.data.model.RecordingPhase
import com.example.data.model.SleepLog
import com.example.data.model.StreakMetrics
import com.example.data.model.UiDataState
import com.example.data.model.UserProfile
import com.example.data.repository.FitnessRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class NoirStrideViewModel(
    private val repository: FitnessRepository,
    private val locationTracker: LocationTracker
) : ViewModel() {

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _analyticsRange = MutableStateFlow(AnalyticsRange.DAYS_30)
    val analyticsRange: StateFlow<AnalyticsRange> = _analyticsRange.asStateFlow()

    private val _historySearchQuery = MutableStateFlow("")
    val historySearchQuery: StateFlow<String> = _historySearchQuery.asStateFlow()

    private val _historyTypeFilter = MutableStateFlow<ActivityType?>(null)
    val historyTypeFilter: StateFlow<ActivityType?> = _historyTypeFilter.asStateFlow()

    private val _historySortOrder = MutableStateFlow(ActivitySortOrder.DATE_DESC)
    val historySortOrder: StateFlow<ActivitySortOrder> = _historySortOrder.asStateFlow()

    private val _recordingState = MutableStateFlow(LiveRecordingState())
    val recordingState: StateFlow<LiveRecordingState> = _recordingState.asStateFlow()

    private val _readNotificationIds = MutableStateFlow<Set<String>>(emptySet())

    private var timerJob: Job? = null
    private var gpsJob: Job? = null

    private fun <T> Flow<T>.toSafeUiState(label: String): StateFlow<UiDataState<T>> =
        this.map<T, UiDataState<T>> { UiDataState.Success(it) }
            .catch { error ->
                if (error is CancellationException) throw error
                Log.w(TAG, "Error observing $label", error)
                emit(UiDataState.Error(error.localizedMessage ?: "Failed to load $label"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiDataState.Loading
            )

    val profileState: StateFlow<UiDataState<UserProfile?>> =
        repository.observeProfile().toSafeUiState("profile")

    val activitiesState: StateFlow<UiDataState<List<ActivityRecord>>> =
        repository.observeActivities().toSafeUiState("activities")

    val healthMetricsState: StateFlow<UiDataState<List<HealthMetric>>> =
        repository.observeHealthMetrics().toSafeUiState("health metrics")

    val hydrationLogsState: StateFlow<UiDataState<List<HydrationLog>>> =
        repository.observeHydrationLogs().toSafeUiState("hydration logs")

    val sleepLogsState: StateFlow<UiDataState<List<SleepLog>>> =
        repository.observeSleepLogs().toSafeUiState("sleep logs")

    val goalsState: StateFlow<UiDataState<List<FitnessGoal>>> =
        repository.observeGoals().toSafeUiState("goals")

    val storedProfile: StateFlow<UserProfile?> = profileState.map { state ->
        (state as? UiDataState.Success)?.data
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val profile: StateFlow<UserProfile> = storedProfile.map { p ->
        p ?: UserProfile()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UserProfile())

    val activities: StateFlow<List<ActivityRecord>> = activitiesState.map {
        (it as? UiDataState.Success)?.data.orEmpty()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val healthMetrics: StateFlow<List<HealthMetric>> = healthMetricsState.map {
        (it as? UiDataState.Success)?.data.orEmpty()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val hydrationLogs: StateFlow<List<HydrationLog>> = hydrationLogsState.map {
        (it as? UiDataState.Success)?.data.orEmpty()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val sleepLogs: StateFlow<List<SleepLog>> = sleepLogsState.map {
        (it as? UiDataState.Success)?.data.orEmpty()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val goals: StateFlow<List<FitnessGoal>> = goalsState.map {
        (it as? UiDataState.Success)?.data.orEmpty()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val filteredHistoryActivities: StateFlow<List<ActivityRecord>> = combine(
        activities,
        historySearchQuery,
        historyTypeFilter,
        historySortOrder
    ) { list, query, typeFilter, sortOrder ->
        val q = query.trim().lowercase()
        val matched = list.filter { act ->
            val typeOk = typeFilter == null || act.activityTypeEnum == typeFilter
            val queryOk = q.isEmpty() ||
                act.title.lowercase().contains(q) ||
                act.activityTypeEnum.label.lowercase().contains(q) ||
                (act.locationName?.lowercase()?.contains(q) == true) ||
                (act.notes?.lowercase()?.contains(q) == true)
            typeOk && queryOk
        }
        when (sortOrder) {
            ActivitySortOrder.DATE_DESC -> matched.sortedByDescending { it.startedAtEpochMs }
            ActivitySortOrder.DISTANCE_DESC -> matched.sortedByDescending { it.distanceMeters }
            ActivitySortOrder.DURATION_DESC -> matched.sortedByDescending { it.durationSec }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val analyticsSnapshot: StateFlow<AnalyticsSnapshot> = combine(
        activities,
        analyticsRange
    ) { list, range ->
        HaversineMath.computeAnalytics(list, range)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        HaversineMath.computeAnalytics(emptyList(), AnalyticsRange.DAYS_30)
    )

    val streakMetrics: StateFlow<StreakMetrics> = combine(
        activities,
        healthMetrics
    ) { acts, metrics ->
        HaversineMath.computeStreaks(acts, metrics)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        HaversineMath.computeStreaks(emptyList(), emptyList())
    )

    val heatmapDays: StateFlow<List<HeatmapDay>> = activities.map { list ->
        HaversineMath.computeHeatmap(list, 84)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), HaversineMath.computeHeatmap(emptyList(), 84))

    val goalProgressList: StateFlow<List<GoalProgressItem>> = combine(
        goals,
        activities,
        healthMetrics
    ) { goalList, acts, metrics ->
        val today = HaversineMath.todayDateKey()
        val todayMetric = metrics.firstOrNull { it.dateKey == today }
        val now = System.currentTimeMillis()
        val dayMs = 86_400_000L

        goalList.filter { it.active }.map { goal ->
            val currentVal = when (goal.metricTypeEnum) {
                GoalMetricType.DAILY_STEPS -> (todayMetric?.steps ?: 0L).toDouble()
                GoalMetricType.DAILY_WATER_ML -> (todayMetric?.waterMl ?: 0L).toDouble()
                GoalMetricType.DAILY_SLEEP_MIN -> (todayMetric?.sleepMinutes ?: 0L).toDouble()
                GoalMetricType.DAILY_CALORIES -> {
                    val actCal = acts.filter { HaversineMath.dateKeyForMillis(it.startedAtEpochMs) == today }
                        .sumOf { it.caloriesBurned }
                    maxOf(todayMetric?.caloriesBurned ?: 0L, actCal).toDouble()
                }
                GoalMetricType.DAILY_ACTIVE_MIN -> {
                    acts.filter { HaversineMath.dateKeyForMillis(it.startedAtEpochMs) == today }
                        .sumOf { it.durationSec } / 60.0
                }
                GoalMetricType.DAILY_DISTANCE_KM -> {
                    acts.filter { HaversineMath.dateKeyForMillis(it.startedAtEpochMs) == today }
                        .sumOf { it.distanceMeters } / 1000.0
                }
                GoalMetricType.WEEKLY_RUN_KM -> {
                    acts.filter {
                        it.startedAtEpochMs >= now - 7L * dayMs && it.activityTypeEnum == ActivityType.RUNNING
                    }.sumOf { it.distanceMeters } / 1000.0
                }
                GoalMetricType.MONTHLY_CYCLE_KM -> {
                    acts.filter {
                        it.startedAtEpochMs >= now - 30L * dayMs && it.activityTypeEnum == ActivityType.CYCLING
                    }.sumOf { it.distanceMeters } / 1000.0
                }
                GoalMetricType.WEEKLY_WORKOUTS -> {
                    acts.count { it.startedAtEpochMs >= now - 7L * dayMs }.toDouble()
                }
            }
            val pct = if (goal.targetValue > 0.0) {
                ((currentVal / goal.targetValue).toFloat()).coerceIn(0f, 1f)
            } else 0f
            GoalProgressItem(goal = goal, currentValue = currentVal, percentComplete = pct)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val notifications: StateFlow<List<AppNotificationItem>> = combine(
        streakMetrics,
        goalProgressList,
        _readNotificationIds
    ) { streak, progress, readIds ->
        val items = mutableListOf<AppNotificationItem>()
        if (streak.currentStreakDays > 0) {
            items.add(
                AppNotificationItem(
                    id = "streak_${streak.currentStreakDays}",
                    title = "${streak.currentStreakDays}-Day Movement Streak",
                    body = "Consistent daily activity recorded across ${streak.activeDaysThisWeek} days this week.",
                    timestampLabel = "Today",
                    unread = "streak_${streak.currentStreakDays}" !in readIds
                )
            )
        }
        progress.filter { it.percentComplete >= 1f }.take(2).forEach { gp ->
            val id = "goal_done_${gp.goal.id}"
            items.add(
                AppNotificationItem(
                    id = id,
                    title = "Target Completed: ${gp.goal.metricTypeEnum.title}",
                    body = "Reached 100% of your ${gp.goal.periodEnum.label.lowercase()} target.",
                    timestampLabel = "Today",
                    unread = id !in readIds
                )
            )
        }
        if (items.isEmpty()) {
            items.add(
                AppNotificationItem(
                    id = "welcome_note",
                    title = "Welcome to Strideo",
                    body = "Record a live GPS route on open-source maps or add a daily health log to begin.",
                    timestampLabel = "Now",
                    unread = "welcome_note" !in readIds
                )
            )
        }
        items
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // --- Local Authentication & Profile Lifecycle ---

    fun completeLocalLogin(
        displayName: String,
        birthDay: Int,
        birthMonth: Int,
        birthYear: Int,
        heightCm: Double,
        weightKg: Double
    ) {
        viewModelScope.launch {
            val result = repository.loginOrCreateLocalProfile(
                displayName = displayName,
                birthDay = birthDay,
                birthMonth = birthMonth,
                birthYear = birthYear,
                heightCm = heightCm,
                weightKg = weightKg
            )
            if (result.isFailure) {
                postMessage(result.exceptionOrNull()?.localizedMessage ?: "Failed to save local profile")
            }
        }
    }

    fun resumeExistingLocalProfile() {
        viewModelScope.launch {
            repository.resumeExistingProfile()
        }
    }

    fun signOutLocal() {
        stopTrackingJobs()
        viewModelScope.launch {
            repository.signOutLocal()
        }
    }

    fun markAllNotificationsRead() {
        _readNotificationIds.value = notifications.value.map { it.id }.toSet()
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun postMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    fun setAnalyticsRange(range: AnalyticsRange) {
        _analyticsRange.value = range
    }

    fun setHistorySearch(query: String) {
        _historySearchQuery.value = query
    }

    fun setHistoryTypeFilter(type: ActivityType?) {
        _historyTypeFilter.value = type
    }

    fun setHistorySortOrder(order: ActivitySortOrder) {
        _historySortOrder.value = order
    }

    // --- Initial GPS Fix & Live Activity Recording State Machine ---

    fun refreshCurrentLocationFix() {
        if (!locationTracker.hasLocationPermission()) return
        viewModelScope.launch {
            val fix = locationTracker.getLastKnownOrCurrentLocation()
            if (fix != null) {
                _recordingState.update { s ->
                    s.copy(
                        currentLat = fix.lat,
                        currentLng = fix.lng,
                        gpsAccuracyMeters = fix.acc.toFloat(),
                        gpsStatusMessage = if (s.phase == RecordingPhase.IDLE) {
                            "GPS ready · ±${fix.acc.toInt()}m"
                        } else {
                            s.gpsStatusMessage
                        }
                    )
                }
            }
        }
    }

    fun selectRecordingActivityType(type: ActivityType) {
        if (_recordingState.value.phase == RecordingPhase.IDLE) {
            _recordingState.update { it.copy(activityType = type) }
        }
    }

    fun startRecording() {
        val current = _recordingState.value
        if (current.phase != RecordingPhase.IDLE && current.phase != RecordingPhase.COMPLETED) return
        val now = System.currentTimeMillis()
        _recordingState.value = LiveRecordingState(
            phase = RecordingPhase.RECORDING,
            activityType = current.activityType,
            startedAtEpochMs = now,
            currentLat = current.currentLat,
            currentLng = current.currentLng,
            gpsAccuracyMeters = current.gpsAccuracyMeters,
            gpsStatusMessage = if (locationTracker.hasLocationPermission()) {
                "Acquiring live GPS fix..."
            } else {
                "GPS permission not granted — timer only"
            }
        )
        startTimerLoop()
        startGpsCollection()
    }

    fun pauseRecording() {
        if (_recordingState.value.phase != RecordingPhase.RECORDING) return
        stopTrackingJobs()
        _recordingState.update {
            it.copy(
                phase = RecordingPhase.PAUSED,
                currentSpeedMps = 0.0,
                currentPaceSecPerKm = 0.0,
                gpsStatusMessage = "Session paused"
            )
        }
    }

    fun resumeRecording() {
        if (_recordingState.value.phase != RecordingPhase.PAUSED) return
        _recordingState.update {
            it.copy(
                phase = RecordingPhase.RECORDING,
                gpsStatusMessage = "Resuming GPS lock..."
            )
        }
        startTimerLoop()
        startGpsCollection()
    }

    fun finishRecording() {
        val phase = _recordingState.value.phase
        if (phase != RecordingPhase.RECORDING && phase != RecordingPhase.PAUSED) return
        stopTrackingJobs()
        _recordingState.update {
            it.copy(
                phase = RecordingPhase.COMPLETED,
                currentSpeedMps = 0.0,
                gpsStatusMessage = "Session ready to save"
            )
        }
    }

    fun discardRecording() {
        stopTrackingJobs()
        val prev = _recordingState.value
        _recordingState.value = LiveRecordingState(
            activityType = prev.activityType,
            currentLat = prev.currentLat,
            currentLng = prev.currentLng
        )
        postMessage("Recording discarded")
    }

    fun saveCompletedRecording(title: String, notes: String?, locationName: String?) {
        val snap = _recordingState.value
        if (snap.isSubmitting) return
        _recordingState.update { it.copy(isSubmitting = true) }

        viewModelScope.launch {
            val weight = profile.value.weightKg
            val finalDuration = snap.elapsedSec.coerceAtLeast(1L)
            val finalMoving = snap.movingSec.coerceAtLeast(1L)
            val avgSpeed = if (finalMoving > 0L) snap.distanceMeters / finalMoving else 0.0
            val avgPace = if (snap.distanceMeters >= 50.0) finalMoving / (snap.distanceMeters / 1000.0) else 0.0
            val calories = HaversineMath.estimateCalories(snap.activityType, finalDuration, weight)

            val record = ActivityRecord(
                title = title.trim().ifBlank { "${snap.activityType.label} Session" },
                type = snap.activityType.name,
                startedAtEpochMs = snap.startedAtEpochMs.takeIf { it > 0L } ?: System.currentTimeMillis(),
                durationSec = finalDuration,
                movingDurationSec = finalMoving,
                distanceMeters = snap.distanceMeters,
                avgSpeedMps = avgSpeed,
                avgPaceSecPerKm = avgPace,
                caloriesBurned = calories,
                elevationGainMeters = snap.elevationGainMeters,
                manualEntry = snap.points.isEmpty(),
                pointCount = snap.points.size.toLong(),
                routePoints = snap.points,
                notes = notes?.trim()?.takeIf { it.isNotEmpty() },
                locationName = locationName?.trim()?.takeIf { it.isNotEmpty() }
            )

            val result = repository.createActivity(record)
            if (result.isSuccess) {
                _recordingState.value = LiveRecordingState(
                    activityType = snap.activityType,
                    currentLat = snap.currentLat,
                    currentLng = snap.currentLng
                )
                postMessage("Activity saved locally")
            } else {
                _recordingState.update { it.copy(isSubmitting = false) }
                postMessage(result.exceptionOrNull()?.localizedMessage ?: "Failed to save activity")
            }
        }
    }

    private fun startTimerLoop() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive && _recordingState.value.phase == RecordingPhase.RECORDING) {
                delay(1000L)
                val weight = profile.value.weightKg
                _recordingState.update { s ->
                    val nextElapsed = s.elapsedSec + 1L
                    val nextMoving = s.movingSec + 1L
                    val avgSpeed = if (nextMoving > 0L) s.distanceMeters / nextMoving else 0.0
                    val avgPace = if (s.distanceMeters >= 30.0) nextMoving / (s.distanceMeters / 1000.0) else 0.0
                    val cal = HaversineMath.estimateCalories(s.activityType, nextElapsed, weight)
                    s.copy(
                        elapsedSec = nextElapsed,
                        movingSec = nextMoving,
                        avgSpeedMps = avgSpeed,
                        avgPaceSecPerKm = avgPace,
                        caloriesBurned = cal
                    )
                }
            }
        }
    }

    private fun startGpsCollection() {
        gpsJob?.cancel()
        if (!locationTracker.hasLocationPermission()) return
        gpsJob = viewModelScope.launch {
            locationTracker.locationUpdates().collect { event ->
                if (_recordingState.value.phase != RecordingPhase.RECORDING) return@collect
                when (event) {
                    is LocationUpdateEvent.Point -> {
                        val pt = event.routePoint
                        _recordingState.update { s ->
                            val prev = s.points.lastOrNull()
                            val deltaDist = if (prev != null) {
                                val d = HaversineMath.distanceMeters(prev.lat, prev.lng, pt.lat, pt.lng)
                                if (d in 1.5..250.0) d else 0.0
                            } else 0.0
                            val deltaElev = if (prev != null && pt.alt > prev.alt) {
                                val e = pt.alt - prev.alt
                                if (e in 1.0..50.0) e else 0.0
                            } else 0.0
                            val updatedPoints = if (prev == null || deltaDist > 0.0) s.points + pt else s.points
                            val totalDist = s.distanceMeters + deltaDist
                            val instantSpeed = if (event.speedMps > 0.2f) {
                                event.speedMps.toDouble()
                            } else if (prev != null && deltaDist > 0.0) {
                                val dt = ((pt.ts - prev.ts) / 1000.0).coerceAtLeast(1.0)
                                deltaDist / dt
                            } else 0.0
                            val instantPace = if (instantSpeed > 0.3) 1000.0 / instantSpeed else 0.0
                            val avgSpeed = if (s.movingSec > 0L) totalDist / s.movingSec else 0.0
                            val avgPace = if (totalDist >= 30.0) s.movingSec / (totalDist / 1000.0) else 0.0

                            s.copy(
                                points = updatedPoints,
                                distanceMeters = totalDist,
                                elevationGainMeters = s.elevationGainMeters + deltaElev,
                                currentSpeedMps = instantSpeed,
                                currentPaceSecPerKm = instantPace,
                                avgSpeedMps = avgSpeed,
                                avgPaceSecPerKm = avgPace,
                                gpsAccuracyMeters = pt.acc.toFloat(),
                                currentLat = pt.lat,
                                currentLng = pt.lng,
                                gpsStatusMessage = "GPS locked · ±${pt.acc.toInt()}m"
                            )
                        }
                    }
                    is LocationUpdateEvent.LowAccuracy -> {
                        _recordingState.update {
                            it.copy(
                                gpsAccuracyMeters = event.accuracyMeters,
                                gpsStatusMessage = "Weak GPS signal (±${event.accuracyMeters.toInt()}m)"
                            )
                        }
                    }
                    is LocationUpdateEvent.Error -> {
                        _recordingState.update {
                            it.copy(gpsStatusMessage = event.message)
                        }
                    }
                }
            }
        }
    }

    private fun stopTrackingJobs() {
        timerJob?.cancel()
        timerJob = null
        gpsJob?.cancel()
        gpsJob = null
    }

    // --- Manual Activity & Activity CRUD ---

    fun addManualActivity(
        title: String,
        type: ActivityType,
        distanceInput: Double,
        durationMinutes: Long,
        elevationMeters: Double,
        locationName: String?,
        notes: String?
    ) {
        viewModelScope.launch {
            val unit = profile.value.distanceUnitEnum
            val distanceMeters = when (unit) {
                DistanceUnit.KM -> distanceInput.coerceAtLeast(0.0) * 1000.0
                DistanceUnit.MI -> distanceInput.coerceAtLeast(0.0) * HaversineMath.METERS_PER_MILE
            }
            val durationSec = (durationMinutes.coerceAtLeast(1L)) * 60L
            val speed = if (durationSec > 0) distanceMeters / durationSec else 0.0
            val pace = if (distanceMeters >= 50.0) durationSec / (distanceMeters / 1000.0) else 0.0
            val calories = HaversineMath.estimateCalories(type, durationSec, profile.value.weightKg)

            val record = ActivityRecord(
                title = title.trim().ifBlank { "${type.label} Session" },
                type = type.name,
                startedAtEpochMs = System.currentTimeMillis(),
                durationSec = durationSec,
                movingDurationSec = durationSec,
                distanceMeters = distanceMeters,
                avgSpeedMps = speed,
                avgPaceSecPerKm = pace,
                caloriesBurned = calories,
                elevationGainMeters = elevationMeters.coerceAtLeast(0.0),
                manualEntry = true,
                pointCount = 0L,
                routePoints = emptyList(),
                notes = notes?.trim()?.takeIf { it.isNotEmpty() },
                locationName = locationName?.trim()?.takeIf { it.isNotEmpty() }
            )
            val result = repository.createActivity(record)
            postMessage(if (result.isSuccess) "Manual activity recorded" else result.exceptionOrNull()?.localizedMessage ?: "Failed to record activity")
        }
    }

    fun updateActivityDetails(activityId: String, title: String, notes: String?, locationName: String?) {
        viewModelScope.launch {
            val result = repository.updateActivity(activityId, title, notes, locationName)
            postMessage(if (result.isSuccess) "Activity updated" else result.exceptionOrNull()?.localizedMessage ?: "Failed to update activity")
        }
    }

    fun deleteActivity(activityId: String) {
        viewModelScope.launch {
            val result = repository.deleteActivity(activityId)
            postMessage(if (result.isSuccess) "Activity deleted" else result.exceptionOrNull()?.localizedMessage ?: "Failed to delete activity")
        }
    }

    // --- Health, Hydration & Sleep ---

    fun saveDailyHealthMetric(metric: HealthMetric) {
        viewModelScope.launch {
            val result = repository.upsertHealthMetric(metric)
            if (result.isSuccess && (metric.weightKg != null || metric.heightCm != null)) {
                val p = profile.value
                repository.saveProfile(
                    p.copy(
                        weightKg = metric.weightKg ?: p.weightKg,
                        heightCm = metric.heightCm ?: p.heightCm
                    )
                )
            }
            postMessage(if (result.isSuccess) "Daily health log saved" else result.exceptionOrNull()?.localizedMessage ?: "Failed to save health log")
        }
    }

    fun addWater(amountMl: Long, label: String? = null) {
        viewModelScope.launch {
            val result = repository.addHydrationLog(HaversineMath.todayDateKey(), amountMl, label)
            postMessage(if (result.isSuccess) "+${amountMl} ml logged" else result.exceptionOrNull()?.localizedMessage ?: "Failed to add hydration")
        }
    }

    fun removeWaterLog(log: HydrationLog) {
        viewModelScope.launch {
            val result = repository.deleteHydrationLog(log.id, log.dateKey, log.amountMl)
            postMessage(if (result.isSuccess) "Hydration entry removed" else result.exceptionOrNull()?.localizedMessage ?: "Failed to remove entry")
        }
    }

    fun addSleepSession(bedtime: String, wakeTime: String, durationMinutes: Long, quality: Long, notes: String?) {
        viewModelScope.launch {
            val log = SleepLog(
                dateKey = HaversineMath.todayDateKey(),
                bedtimeLabel = bedtime,
                wakeTimeLabel = wakeTime,
                durationMinutes = durationMinutes.coerceIn(1L, 1440L),
                qualityRating = quality.coerceIn(1L, 5L),
                notes = notes
            )
            val result = repository.addSleepLog(log)
            postMessage(if (result.isSuccess) "Sleep session recorded" else result.exceptionOrNull()?.localizedMessage ?: "Failed to record sleep")
        }
    }

    fun deleteSleepSession(logId: String) {
        viewModelScope.launch {
            val result = repository.deleteSleepLog(logId)
            postMessage(if (result.isSuccess) "Sleep entry deleted" else result.exceptionOrNull()?.localizedMessage ?: "Failed to delete sleep entry")
        }
    }

    // --- Goals & Settings ---

    fun saveGoal(metricType: GoalMetricType, targetValue: Double, period: GoalPeriod, existingId: String = "") {
        viewModelScope.launch {
            val goal = FitnessGoal(
                id = existingId,
                metricType = metricType.name,
                targetValue = targetValue.coerceAtLeast(1.0),
                unit = metricType.defaultUnit,
                period = period.name,
                active = true
            )
            val result = repository.upsertGoal(goal)
            postMessage(if (result.isSuccess) "Goal saved" else result.exceptionOrNull()?.localizedMessage ?: "Failed to save goal")
        }
    }

    fun deleteGoal(goalId: String) {
        viewModelScope.launch {
            val result = repository.deleteGoal(goalId)
            postMessage(if (result.isSuccess) "Goal removed" else result.exceptionOrNull()?.localizedMessage ?: "Failed to remove goal")
        }
    }

    fun saveUserProfile(updated: UserProfile) {
        viewModelScope.launch {
            val result = repository.saveProfile(updated)
            postMessage(if (result.isSuccess) "Profile & preferences updated" else result.exceptionOrNull()?.localizedMessage ?: "Failed to update profile")
        }
    }

    fun generateCsvExport(): String = HaversineMath.exportToCsv(activities.value, healthMetrics.value)

    fun generateJsonExport(): String = HaversineMath.exportToJson(
        profile = profile.value,
        activities = activities.value,
        healthMetrics = healthMetrics.value,
        goals = goals.value
    )

    fun deleteAllData() {
        stopTrackingJobs()
        viewModelScope.launch {
            val result = repository.deleteAllUserData()
            postMessage(if (result.isSuccess) "All local records cleared" else result.exceptionOrNull()?.localizedMessage ?: "Failed to delete records")
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopTrackingJobs()
    }

    private companion object {
        const val TAG = "StrideoVM"
    }
}
