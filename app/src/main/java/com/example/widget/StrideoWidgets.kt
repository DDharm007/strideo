package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.local.StrideoDatabase
import com.example.data.model.ActivityRecord
import com.example.data.model.DistanceUnit
import com.example.data.model.HaversineMath
import com.example.data.model.HealthMetric
import com.example.data.repository.RoomFitnessRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Locale

object StrideoWidgetUpdater {
    const val EXTRA_NAV_DESTINATION = "com.example.strideo.EXTRA_NAV_DESTINATION"
    const val DEST_OVERVIEW = "overview"
    const val DEST_LIVE_TRACKING = "live_tracking"
    const val DEST_HEALTH = "health"

    internal const val ACTION_ADD_WATER_250 = "com.example.strideo.widget.ACTION_ADD_WATER_250"
    internal const val ACTION_WIDGET_REFRESH = "com.example.strideo.widget.ACTION_WIDGET_REFRESH"

    private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun refreshAll(context: Context) {
        val appContext = context.applicationContext
        widgetScope.launch {
            updateAllWidgetsNow(appContext)
        }
    }

    suspend fun updateAllWidgetsNow(context: Context) {
        val appContext = context.applicationContext
        val manager = AppWidgetManager.getInstance(appContext)
        val snapshot = loadSnapshot(appContext)

        val summaryIds = manager.getAppWidgetIds(
            ComponentName(appContext, StrideoSummaryWidgetProvider::class.java)
        )
        for (id in summaryIds) {
            manager.updateAppWidget(id, buildSummaryRemoteViews(appContext, snapshot))
        }

        val quickIds = manager.getAppWidgetIds(
            ComponentName(appContext, StrideoQuickTrackWidgetProvider::class.java)
        )
        for (id in quickIds) {
            manager.updateAppWidget(id, buildQuickTrackRemoteViews(appContext, snapshot))
        }
    }

    fun isPinningSupported(context: Context): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported
    }

    fun requestPinSummaryWidget(context: Context): Boolean =
        requestPinWidget(context, StrideoSummaryWidgetProvider::class.java)

    fun requestPinQuickTrackWidget(context: Context): Boolean =
        requestPinWidget(context, StrideoQuickTrackWidgetProvider::class.java)

    private fun requestPinWidget(context: Context, providerClass: Class<out AppWidgetProvider>): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        val appWidgetManager = AppWidgetManager.getInstance(context)
        if (!appWidgetManager.isRequestPinAppWidgetSupported) return false
        val provider = ComponentName(context, providerClass)
        val callbackIntent = Intent(context, providerClass).apply {
            action = ACTION_WIDGET_REFRESH
            `package` = context.packageName
        }
        val successCallback = PendingIntent.getBroadcast(
            context,
            providerClass.simpleName.hashCode(),
            callbackIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return appWidgetManager.requestPinAppWidget(provider, null, successCallback)
    }

    internal data class WidgetSnapshot(
        val unlocked: Boolean,
        val stepsFormatted: String,
        val distanceFormatted: String,
        val caloriesFormatted: String,
        val waterFormatted: String,
        val streakDays: Int
    )

    internal suspend fun loadSnapshot(context: Context): WidgetSnapshot {
        return try {
            val dao = StrideoDatabase.getInstance(context).strideoDao()
            val profile = dao.getProfileOnce()
            if (profile == null || !profile.signedIn) {
                return WidgetSnapshot(
                    unlocked = false,
                    stepsFormatted = "—",
                    distanceFormatted = "—",
                    caloriesFormatted = "—",
                    waterFormatted = "—",
                    streakDays = 0
                )
            }

            val todayKey = HaversineMath.todayDateKey()
            val todayMetric = dao.getHealthMetricByDate(todayKey)
            val allActivities = dao.getAllActivitiesOnce().map { e ->
                ActivityRecord(
                    id = e.id,
                    title = e.title,
                    type = e.type,
                    startedAtEpochMs = e.startedAtEpochMs,
                    durationSec = e.durationSec,
                    movingDurationSec = e.movingDurationSec,
                    distanceMeters = e.distanceMeters,
                    avgSpeedMps = e.avgSpeedMps,
                    avgPaceSecPerKm = e.avgPaceSecPerKm,
                    caloriesBurned = e.caloriesBurned,
                    elevationGainMeters = e.elevationGainMeters,
                    manualEntry = e.manualEntry,
                    pointCount = e.pointCount,
                    routePoints = e.routePoints,
                    notes = e.notes,
                    locationName = e.locationName
                )
            }
            val allMetrics = dao.getAllHealthMetricsOnce().map { m ->
                HealthMetric(
                    id = m.dateKey,
                    dateKey = m.dateKey,
                    steps = m.steps,
                    caloriesBurned = m.caloriesBurned,
                    waterMl = m.waterMl
                )
            }

            val todayActivities = allActivities.filter { HaversineMath.dateKeyForMillis(it.startedAtEpochMs) == todayKey }
            val unit = DistanceUnit.fromRaw(profile.unitDistance)
            val steps = todayMetric?.steps ?: 0L
            val distanceMeters = todayActivities.sumOf { it.distanceMeters }
            val calories = maxOf(todayMetric?.caloriesBurned ?: 0L, todayActivities.sumOf { it.caloriesBurned })
            val waterMl = todayMetric?.waterMl ?: 0L
            val streak = HaversineMath.computeStreaks(allActivities, allMetrics).currentStreakDays

            WidgetSnapshot(
                unlocked = true,
                stepsFormatted = String.format(Locale.US, "%,d", steps),
                distanceFormatted = HaversineMath.formatDistance(distanceMeters, unit),
                caloriesFormatted = "$calories kcal",
                waterFormatted = "$waterMl ml",
                streakDays = streak
            )
        } catch (_: Exception) {
            WidgetSnapshot(
                unlocked = false,
                stepsFormatted = "—",
                distanceFormatted = "—",
                caloriesFormatted = "—",
                waterFormatted = "—",
                streakDays = 0
            )
        }
    }

    internal fun buildSummaryRemoteViews(context: Context, snap: WidgetSnapshot): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_strideo_summary)

        val openOverviewIntent = createLaunchPendingIntent(context, DEST_OVERVIEW, 101)
        val startGpsIntent = createLaunchPendingIntent(context, DEST_LIVE_TRACKING, 102)

        views.setOnClickPendingIntent(R.id.widget_summary_root, openOverviewIntent)
        views.setOnClickPendingIntent(R.id.widget_btn_start_gps, startGpsIntent)
        views.setOnClickPendingIntent(R.id.widget_btn_unlock, openOverviewIntent)

        // Secure explicit broadcast PendingIntent scoped to our own package
        val addWaterIntent = Intent(context, StrideoSummaryWidgetProvider::class.java).apply {
            action = ACTION_ADD_WATER_250
            `package` = context.packageName
        }
        val addWaterPendingIntent = PendingIntent.getBroadcast(
            context,
            103,
            addWaterIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_btn_add_water, addWaterPendingIntent)

        if (snap.unlocked) {
            views.setViewVisibility(R.id.widget_summary_content, View.VISIBLE)
            views.setViewVisibility(R.id.widget_summary_locked, View.GONE)
            views.setTextViewText(R.id.widget_streak_pill, "${snap.streakDays}D STREAK")
            views.setTextViewText(R.id.widget_steps_value, snap.stepsFormatted)
            views.setTextViewText(R.id.widget_distance_value, snap.distanceFormatted)
            views.setTextViewText(R.id.widget_calories_value, snap.caloriesFormatted)
            views.setTextViewText(R.id.widget_water_value, snap.waterFormatted)
        } else {
            views.setViewVisibility(R.id.widget_summary_content, View.GONE)
            views.setViewVisibility(R.id.widget_summary_locked, View.VISIBLE)
            views.setTextViewText(R.id.widget_streak_pill, "LOCKED")
        }

        return views
    }

    internal fun buildQuickTrackRemoteViews(context: Context, snap: WidgetSnapshot): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_strideo_quick_track)
        val openOverviewIntent = createLaunchPendingIntent(context, DEST_OVERVIEW, 201)
        val startGpsIntent = createLaunchPendingIntent(context, DEST_LIVE_TRACKING, 202)

        views.setOnClickPendingIntent(R.id.widget_quick_root, openOverviewIntent)
        views.setOnClickPendingIntent(R.id.widget_quick_start_btn, startGpsIntent)

        if (snap.unlocked) {
            views.setTextViewText(R.id.widget_quick_streak, "${snap.streakDays}D")
            views.setTextViewText(R.id.widget_quick_label, "TODAY")
            views.setTextViewText(R.id.widget_quick_distance, snap.distanceFormatted)
            views.setTextViewText(R.id.widget_quick_start_btn, "START GPS")
        } else {
            views.setTextViewText(R.id.widget_quick_streak, "LOCKED")
            views.setTextViewText(R.id.widget_quick_label, "STRIDEO")
            views.setTextViewText(R.id.widget_quick_distance, "Private")
            views.setTextViewText(R.id.widget_quick_start_btn, "SIGN IN")
        }

        return views
    }

    private fun createLaunchPendingIntent(
        context: Context,
        destination: String,
        requestCode: Int
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_NAV_DESTINATION, destination)
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

class StrideoSummaryWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val snap = StrideoWidgetUpdater.loadSnapshot(context.applicationContext)
                for (id in appWidgetIds) {
                    appWidgetManager.updateAppWidget(
                        id,
                        StrideoWidgetUpdater.buildSummaryRemoteViews(context.applicationContext, snap)
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return
        if (action == StrideoWidgetUpdater.ACTION_ADD_WATER_250) {
            val pendingResult = goAsync()
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                try {
                    val appContext = context.applicationContext
                    val dao = StrideoDatabase.getInstance(appContext).strideoDao()
                    val profile = dao.getProfileOnce()
                    // Security guard: only write hydration when local profile is actively signed in
                    if (profile != null && profile.signedIn) {
                        val repo = RoomFitnessRepository(appContext, dao)
                        repo.addHydrationLog(HaversineMath.todayDateKey(), 250L, "Widget Quick Add")
                    }
                    StrideoWidgetUpdater.updateAllWidgetsNow(appContext)
                } finally {
                    pendingResult.finish()
                }
            }
        } else if (action == StrideoWidgetUpdater.ACTION_WIDGET_REFRESH) {
            val pendingResult = goAsync()
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                try {
                    StrideoWidgetUpdater.updateAllWidgetsNow(context.applicationContext)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}

class StrideoQuickTrackWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val snap = StrideoWidgetUpdater.loadSnapshot(context.applicationContext)
                for (id in appWidgetIds) {
                    appWidgetManager.updateAppWidget(
                        id,
                        StrideoWidgetUpdater.buildQuickTrackRemoteViews(context.applicationContext, snap)
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == StrideoWidgetUpdater.ACTION_WIDGET_REFRESH) {
            val pendingResult = goAsync()
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                try {
                    StrideoWidgetUpdater.updateAllWidgetsNow(context.applicationContext)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
