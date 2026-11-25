package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AppNotificationItem
import com.example.data.model.GoalMetricType
import com.example.data.model.GoalPeriod
import com.example.data.model.RecordingPhase
import com.example.ui.components.NoirCard
import com.example.ui.components.NoirDivider
import com.example.ui.components.NoirSecondaryButton
import com.example.ui.components.NoirStatusPill
import com.example.ui.screens.ActivityDetailScreen
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.GoalsScreen
import com.example.ui.screens.HealthScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.LiveTrackingScreen
import com.example.ui.screens.ManualActivityDialog
import com.example.ui.screens.OverviewScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.NoirAccent
import com.example.ui.theme.NoirBackgroundPrimary
import com.example.ui.theme.NoirBackgroundSecondary
import com.example.ui.theme.NoirBorder
import com.example.ui.theme.NoirSuccess
import com.example.ui.theme.NoirSurfaceElevated
import com.example.ui.theme.NoirTextMuted
import com.example.ui.theme.NoirTextPrimary
import com.example.ui.theme.NoirTextSecondary
import com.example.viewmodel.NoirStrideViewModel
import com.example.widget.StrideoWidgetUpdater
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class NavSection(val route: String, val label: String, val icon: ImageVector) {
    OVERVIEW("overview", "Overview", Icons.Default.Dashboard),
    ACTIVITY("activity", "Activity", Icons.Default.DirectionsRun),
    LIVE_TRACKING("live_tracking", "Live Tracking", Icons.Default.GpsFixed),
    HEALTH("health", "Health", Icons.Default.FavoriteBorder),
    ANALYTICS("analytics", "Analytics", Icons.AutoMirrored.Filled.ShowChart),
    HISTORY("history", "History", Icons.Default.History),
    GOALS("goals", "Goals", Icons.Default.Flag),
    SETTINGS("settings", "Settings", Icons.Default.Settings)
}

@Composable
fun NoirStrideApp(
    viewModel: NoirStrideViewModel,
    requestedDestination: String? = null,
    onConsumeDestination: () -> Unit = {},
    onSignOut: () -> Unit
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val activities by viewModel.activities.collectAsStateWithLifecycle()
    val filteredHistory by viewModel.filteredHistoryActivities.collectAsStateWithLifecycle()
    val healthMetrics by viewModel.healthMetrics.collectAsStateWithLifecycle()
    val hydrationLogs by viewModel.hydrationLogs.collectAsStateWithLifecycle()
    val sleepLogs by viewModel.sleepLogs.collectAsStateWithLifecycle()
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    val goalProgress by viewModel.goalProgressList.collectAsStateWithLifecycle()
    val analyticsRange by viewModel.analyticsRange.collectAsStateWithLifecycle()
    val analyticsSnapshot by viewModel.analyticsSnapshot.collectAsStateWithLifecycle()
    val streaks by viewModel.streakMetrics.collectAsStateWithLifecycle()
    val heatmapDays by viewModel.heatmapDays.collectAsStateWithLifecycle()
    val recordingState by viewModel.recordingState.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    val historySearch by viewModel.historySearchQuery.collectAsStateWithLifecycle()
    val historyType by viewModel.historyTypeFilter.collectAsStateWithLifecycle()
    val historySort by viewModel.historySortOrder.collectAsStateWithLifecycle()

    var currentSection by remember { mutableStateOf(NavSection.OVERVIEW) }
    var selectedActivityId by remember { mutableStateOf<String?>(null) }
    var sidebarCollapsed by remember { mutableStateOf(false) }
    var showQuickAddModal by remember { mutableStateOf(false) }
    var showNotificationsModal by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Respond to OS Widget deep-links cleanly
    LaunchedEffect(requestedDestination) {
        when (requestedDestination) {
            StrideoWidgetUpdater.DEST_LIVE_TRACKING -> {
                selectedActivityId = null
                currentSection = NavSection.LIVE_TRACKING
                onConsumeDestination()
            }
            StrideoWidgetUpdater.DEST_HEALTH -> {
                selectedActivityId = null
                currentSection = NavSection.HEALTH
                onConsumeDestination()
            }
            StrideoWidgetUpdater.DEST_OVERVIEW -> {
                selectedActivityId = null
                currentSection = NavSection.OVERVIEW
                onConsumeDestination()
            }
        }
    }

    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearSnackbar()
    }

    val navigateTo: (NavSection) -> Unit = { target ->
        selectedActivityId = null
        currentSection = target
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(NoirBackgroundPrimary)
    ) {
        val isWideScreen = maxWidth >= 720.dp

        val mainContent: @Composable () -> Unit = {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = NoirBackgroundPrimary,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    NoirTopHeaderBar(
                        isWideScreen = isWideScreen,
                        isRecordingLive = recordingState.phase == RecordingPhase.RECORDING,
                        unreadNotifications = notifications.count { it.unread },
                        profileInitials = profile.displayName.take(2).uppercase(),
                        onOpenMenu = { scope.launch { drawerState.open() } },
                        onOpenSearch = { navigateTo(NavSection.HISTORY) },
                        onOpenNotifications = {
                            viewModel.markAllNotificationsRead()
                            showNotificationsModal = true
                        },
                        onQuickAdd = { showQuickAddModal = true },
                        onOpenProfile = { navigateTo(NavSection.SETTINGS) }
                    )
                },
                bottomBar = {
                    if (!isWideScreen) {
                        NoirCompactBottomNav(
                            currentSection = currentSection,
                            onSelect = navigateTo
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 920.dp)
                            .fillMaxSize()
                    ) {
                        val activeDetail = selectedActivityId?.let { id ->
                            activities.firstOrNull { it.id == id }
                        }
                        if (activeDetail != null) {
                            ActivityDetailScreen(
                                activity = activeDetail,
                                profile = profile,
                                onBack = { selectedActivityId = null },
                                onUpdate = { t, n, loc ->
                                    viewModel.updateActivityDetails(activeDetail.id, t, n, loc)
                                },
                                onDelete = {
                                    viewModel.deleteActivity(activeDetail.id)
                                    selectedActivityId = null
                                }
                            )
                        } else {
                            when (currentSection) {
                                NavSection.OVERVIEW -> OverviewScreen(
                                    profile = profile,
                                    activities = activities,
                                    healthMetrics = healthMetrics,
                                    goals = goals,
                                    analytics = analyticsSnapshot,
                                    streaks = streaks,
                                    onStartLiveTracking = { navigateTo(NavSection.LIVE_TRACKING) },
                                    onOpenQuickLog = { showQuickAddModal = true },
                                    onOpenActivityDetail = { selectedActivityId = it },
                                    onOpenHealth = { navigateTo(NavSection.HEALTH) },
                                    onOpenHistory = { navigateTo(NavSection.HISTORY) }
                                )

                                NavSection.ACTIVITY -> HistoryScreen(
                                    title = "Activity Hub",
                                    eyebrow = "Workouts & Sessions",
                                    activities = filteredHistory,
                                    profile = profile,
                                    searchQuery = historySearch,
                                    selectedType = historyType,
                                    sortOrder = historySort,
                                    onSearchChange = viewModel::setHistorySearch,
                                    onTypeFilterChange = viewModel::setHistoryTypeFilter,
                                    onSortChange = viewModel::setHistorySortOrder,
                                    onOpenActivityDetail = { selectedActivityId = it },
                                    onOpenQuickLog = { showQuickAddModal = true }
                                )

                                NavSection.LIVE_TRACKING -> LiveTrackingScreen(
                                    recordingState = recordingState,
                                    profile = profile,
                                    onSelectType = viewModel::selectRecordingActivityType,
                                    onRequestLocationFix = viewModel::refreshCurrentLocationFix,
                                    onStart = viewModel::startRecording,
                                    onPause = viewModel::pauseRecording,
                                    onResume = viewModel::resumeRecording,
                                    onFinish = viewModel::finishRecording,
                                    onDiscard = viewModel::discardRecording,
                                    onSaveCompleted = viewModel::saveCompletedRecording,
                                    onAddManualActivity = viewModel::addManualActivity
                                )

                                NavSection.HEALTH -> HealthScreen(
                                    profile = profile,
                                    healthMetrics = healthMetrics,
                                    hydrationLogs = hydrationLogs,
                                    sleepLogs = sleepLogs,
                                    goals = goals,
                                    onSaveHealthMetric = viewModel::saveDailyHealthMetric,
                                    onAddWater = viewModel::addWater,
                                    onDeleteWaterLog = viewModel::removeWaterLog,
                                    onUpdateWaterGoal = { newMl ->
                                        viewModel.saveGoal(
                                            GoalMetricType.DAILY_WATER_ML,
                                            newMl,
                                            GoalPeriod.DAILY
                                        )
                                    },
                                    onAddSleep = viewModel::addSleepSession,
                                    onDeleteSleep = viewModel::deleteSleepSession
                                )

                                NavSection.ANALYTICS -> AnalyticsScreen(
                                    profile = profile,
                                    selectedRange = analyticsRange,
                                    onSelectRange = viewModel::setAnalyticsRange,
                                    snapshot = analyticsSnapshot,
                                    healthMetrics = healthMetrics,
                                    heatmapDays = heatmapDays
                                )

                                NavSection.HISTORY -> HistoryScreen(
                                    title = "Activity History",
                                    eyebrow = "Searchable Route Archive",
                                    activities = filteredHistory,
                                    profile = profile,
                                    searchQuery = historySearch,
                                    selectedType = historyType,
                                    sortOrder = historySort,
                                    onSearchChange = viewModel::setHistorySearch,
                                    onTypeFilterChange = viewModel::setHistoryTypeFilter,
                                    onSortChange = viewModel::setHistorySortOrder,
                                    onOpenActivityDetail = { selectedActivityId = it },
                                    onOpenQuickLog = { showQuickAddModal = true }
                                )

                                NavSection.GOALS -> GoalsScreen(
                                    goalProgress = goalProgress,
                                    streaks = streaks,
                                    heatmapDays = heatmapDays,
                                    onSaveGoal = viewModel::saveGoal,
                                    onDeleteGoal = viewModel::deleteGoal
                                )

                                NavSection.SETTINGS -> SettingsScreen(
                                    profile = profile,
                                    onSaveProfile = viewModel::saveUserProfile,
                                    onExportCsv = viewModel::generateCsvExport,
                                    onExportJson = viewModel::generateJsonExport,
                                    onNotify = viewModel::postMessage,
                                    onSignOut = onSignOut,
                                    onDeleteAccountData = viewModel::deleteAllData
                                )
                            }
                        }
                    }
                }
            }
        }

        if (isWideScreen) {
            Row(modifier = Modifier.fillMaxSize()) {
                NoirSidebar(
                    currentSection = currentSection,
                    collapsed = sidebarCollapsed,
                    onToggleCollapse = { sidebarCollapsed = !sidebarCollapsed },
                    onSelect = navigateTo
                )
                Box(modifier = Modifier.weight(1f)) {
                    mainContent()
                }
            }
        } else {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        drawerContainerColor = NoirBackgroundSecondary,
                        drawerContentColor = NoirTextPrimary,
                        modifier = Modifier.width(270.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .statusBarsPadding()
                                .navigationBarsPadding()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "STRIDEO",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = NoirTextPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
                            )
                            NoirDivider()
                            Spacer(modifier = Modifier.height(8.dp))
                            NavSection.entries.forEach { section ->
                                SidebarNavItem(
                                    section = section,
                                    selected = currentSection == section,
                                    collapsed = false,
                                    onClick = {
                                        navigateTo(section)
                                        scope.launch { drawerState.close() }
                                    }
                                )
                            }
                        }
                    }
                }
            ) {
                mainContent()
            }
        }
    }

    if (showQuickAddModal) {
        ManualActivityDialog(
            unitShort = profile.distanceUnitEnum.shortLabel,
            onDismiss = { showQuickAddModal = false },
            onSave = { title, type, dist, mins, elev, loc, notes ->
                viewModel.addManualActivity(title, type, dist, mins, elev, loc, notes)
                showQuickAddModal = false
            }
        )
    }

    if (showNotificationsModal) {
        NotificationsModal(
            items = notifications,
            onDismiss = { showNotificationsModal = false }
        )
    }
}

@Composable
private fun NoirTopHeaderBar(
    isWideScreen: Boolean,
    isRecordingLive: Boolean,
    unreadNotifications: Int,
    profileInitials: String,
    onOpenMenu: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenNotifications: () -> Unit,
    onQuickAdd: () -> Unit,
    onOpenProfile: () -> Unit
) {
    val dateText = remember {
        SimpleDateFormat("MMM d", Locale.US).format(Date()).uppercase(Locale.US)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(NoirBackgroundPrimary)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!isWideScreen) {
                    IconButton(
                        onClick = onOpenMenu,
                        modifier = Modifier.testTag("nav_drawer_open_btn")
                    ) {
                        Icon(Icons.Default.Menu, contentDescription = "Open navigation menu", tint = NoirTextPrimary)
                    }
                }
                Column {
                    Text(
                        text = "STRIDEO",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = NoirTextPrimary
                    )
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.labelSmall,
                        color = NoirTextMuted
                    )
                }
                if (isRecordingLive) {
                    NoirStatusPill(text = "LIVE", accentColor = NoirSuccess, borderColor = NoirSuccess)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onOpenSearch, modifier = Modifier.testTag("top_search_btn")) {
                    Icon(Icons.Default.Search, contentDescription = "Search activities", tint = NoirTextSecondary)
                }
                Box {
                    IconButton(onClick = onOpenNotifications, modifier = Modifier.testTag("top_notifications_btn")) {
                        Icon(Icons.Default.NotificationsNone, contentDescription = "Notifications", tint = NoirTextSecondary)
                    }
                    if (unreadNotifications > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 10.dp, end = 10.dp)
                                .size(7.dp)
                                .background(NoirAccent, RoundedCornerShape(1.dp))
                        )
                    }
                }
                IconButton(onClick = onQuickAdd, modifier = Modifier.testTag("top_quick_add_btn")) {
                    Icon(Icons.Default.Add, contentDescription = "Quick add activity", tint = NoirTextPrimary)
                }
                Box(
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("top_profile_btn")
                        .size(34.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(NoirSurfaceElevated)
                        .border(1.dp, NoirBorder, RoundedCornerShape(3.dp))
                        .clickable(onClick = onOpenProfile),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = profileInitials,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = NoirTextPrimary
                    )
                }
            }
        }
        NoirDivider()
    }
}

@Composable
private fun NoirSidebar(
    currentSection: NavSection,
    collapsed: Boolean,
    onToggleCollapse: () -> Unit,
    onSelect: (NavSection) -> Unit
) {
    Row(modifier = Modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier
                .width(if (collapsed) 72.dp else 224.dp)
                .fillMaxHeight()
                .background(NoirBackgroundSecondary)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = if (collapsed) Arrangement.Center else Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!collapsed) {
                    Text(
                        text = "STRIDEO",
                        style = MaterialTheme.typography.labelSmall,
                        color = NoirTextMuted
                    )
                }
                IconButton(onClick = onToggleCollapse, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = if (collapsed) Icons.Default.ChevronRight else Icons.Default.ChevronLeft,
                        contentDescription = if (collapsed) "Expand sidebar" else "Collapse sidebar",
                        tint = NoirTextSecondary
                    )
                }
            }
            NoirDivider(subtle = true)
            Spacer(modifier = Modifier.height(6.dp))
            NavSection.entries.forEach { section ->
                SidebarNavItem(
                    section = section,
                    selected = currentSection == section,
                    collapsed = collapsed,
                    onClick = { onSelect(section) }
                )
            }
        }
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(NoirBorder)
        )
    }
}

@Composable
private fun SidebarNavItem(
    section: NavSection,
    selected: Boolean,
    collapsed: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .testTag("nav_${section.route}")
            .clip(RoundedCornerShape(3.dp))
            .background(if (selected) NoirSurfaceElevated else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (collapsed) Arrangement.Center else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(18.dp)
                .background(if (selected) NoirAccent else Color.Transparent)
        )
        Spacer(modifier = Modifier.width(if (collapsed) 4.dp else 10.dp))
        Icon(
            imageVector = section.icon,
            contentDescription = section.label,
            tint = if (selected) NoirTextPrimary else NoirTextSecondary,
            modifier = Modifier.size(19.dp)
        )
        if (!collapsed) {
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = section.label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
                ),
                color = if (selected) NoirTextPrimary else NoirTextSecondary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun NoirCompactBottomNav(
    currentSection: NavSection,
    onSelect: (NavSection) -> Unit
) {
    val primaryItems = listOf(
        NavSection.OVERVIEW,
        NavSection.LIVE_TRACKING,
        NavSection.HEALTH,
        NavSection.ANALYTICS,
        NavSection.GOALS
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(NoirBackgroundSecondary)
            .navigationBarsPadding()
    ) {
        NoirDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            primaryItems.forEach { section ->
                val selected = currentSection == section
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("bottom_nav_${section.route}")
                        .clickable { onSelect(section) }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(18.dp)
                            .height(2.dp)
                            .background(if (selected) NoirAccent else Color.Transparent)
                    )
                    Icon(
                        imageVector = section.icon,
                        contentDescription = section.label,
                        tint = if (selected) NoirTextPrimary else NoirTextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = when (section) {
                            NavSection.LIVE_TRACKING -> "Track"
                            else -> section.label
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) NoirTextPrimary else NoirTextMuted,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationsModal(
    items: List<AppNotificationItem>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NoirBackgroundSecondary,
        title = { Text("Notifications", style = MaterialTheme.typography.headlineLarge, color = NoirTextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items.forEach { n ->
                    NoirCard(padding = PaddingValues(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(n.title, style = MaterialTheme.typography.titleSmall, color = NoirTextPrimary)
                            Text(n.timestampLabel, style = MaterialTheme.typography.labelSmall, color = NoirTextMuted)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(n.body, style = MaterialTheme.typography.bodySmall, color = NoirTextSecondary)
                    }
                }
            }
        },
        confirmButton = {
            NoirSecondaryButton(text = "Close", onClick = onDismiss)
        }
    )
}
