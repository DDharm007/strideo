package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ActivityRecord
import com.example.data.model.ActivitySortOrder
import com.example.data.model.ActivityType
import com.example.data.model.HaversineMath
import com.example.data.model.UserProfile
import com.example.ui.components.NoirCard
import com.example.ui.components.NoirDivider
import com.example.ui.components.NoirEmptyState
import com.example.ui.components.NoirInteractiveMap
import com.example.ui.components.NoirLineChart
import com.example.ui.components.NoirPrimaryButton
import com.example.ui.components.NoirSecondaryButton
import com.example.ui.components.NoirSectionHeader
import com.example.ui.components.NoirSelectableChip
import com.example.ui.components.NoirStatusPill
import com.example.ui.components.NoirTextField
import com.example.ui.theme.NoirBackgroundSecondary
import com.example.ui.theme.NoirDanger
import com.example.ui.theme.NoirTextMuted
import com.example.ui.theme.NoirTextPrimary
import com.example.ui.theme.NoirTextSecondary

@Composable
fun HistoryScreen(
    title: String,
    eyebrow: String,
    activities: List<ActivityRecord>,
    profile: UserProfile,
    searchQuery: String,
    selectedType: ActivityType?,
    sortOrder: ActivitySortOrder,
    onSearchChange: (String) -> Unit,
    onTypeFilterChange: (ActivityType?) -> Unit,
    onSortChange: (ActivitySortOrder) -> Unit,
    onOpenActivityDetail: (String) -> Unit,
    onOpenQuickLog: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            NoirSectionHeader(
                eyebrow = eyebrow,
                title = title,
                trailing = {
                    NoirPrimaryButton(
                        text = "Add",
                        icon = Icons.Default.Add,
                        tag = "history_add_activity_btn",
                        onClick = onOpenQuickLog
                    )
                }
            )
        }

        // Search Bar
        item {
            NoirTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                label = "Search sessions by title, location, or notes",
                tag = "history_search_input"
            )
        }

        // Type Filters
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NoirSelectableChip(
                    label = "All",
                    selected = selectedType == null,
                    onClick = { onTypeFilterChange(null) }
                )
                ActivityType.entries.forEach { t ->
                    NoirSelectableChip(
                        label = t.label,
                        selected = selectedType == t,
                        onClick = { onTypeFilterChange(t) }
                    )
                }
            }
        }

        // Sort Selector
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${activities.size} SESSIONS",
                    style = MaterialTheme.typography.labelSmall,
                    color = NoirTextMuted
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ActivitySortOrder.entries.forEach { order ->
                        NoirSelectableChip(
                            label = order.label,
                            selected = sortOrder == order,
                            onClick = { onSortChange(order) }
                        )
                    }
                }
            }
        }

        if (activities.isEmpty()) {
            item {
                NoirEmptyState(
                    title = "No matching activities",
                    description = "Adjust your filters or record a new GPS or manual session.",
                    actionLabel = "Log Activity",
                    onAction = onOpenQuickLog
                )
            }
        } else {
            items(activities, key = { it.id.ifBlank { it.startedAtEpochMs.toString() } }) { activity ->
                ActivityFeedRowCard(
                    activity = activity,
                    profile = profile,
                    onClick = { onOpenActivityDetail(activity.id) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
fun ActivityDetailScreen(
    activity: ActivityRecord,
    profile: UserProfile,
    onBack: () -> Unit,
    onUpdate: (title: String, notes: String?, locationName: String?) -> Unit,
    onDelete: () -> Unit
) {
    BackHandler(onBack = onBack)

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val unit = profile.distanceUnitEnum

    val elevationProfile = remember(activity.routePoints) {
        if (activity.routePoints.size < 2) emptyList()
        else activity.routePoints.mapIndexedNotNull { idx, pt ->
            if (idx % maxOf(1, activity.routePoints.size / 12) == 0 || idx == activity.routePoints.lastIndex) {
                "${idx + 1}" to pt.alt
            } else null
        }
    }

    val segmentSpeedSeries = remember(activity.routePoints) {
        if (activity.routePoints.size < 3) emptyList()
        else {
            val pts = activity.routePoints
            (1 until pts.size).mapNotNull { i ->
                val dist = HaversineMath.distanceMeters(pts[i - 1].lat, pts[i - 1].lng, pts[i].lat, pts[i].lng)
                val dtSec = ((pts[i].ts - pts[i - 1].ts) / 1000.0).coerceAtLeast(1.0)
                val kmh = (dist / dtSec) * 3.6
                if (kmh.isFinite() && kmh < 90.0) "S$i" to kmh else null
            }.take(12)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to history",
                        tint = NoirTextPrimary
                    )
                }
                Text("ACTIVITY DETAIL", style = MaterialTheme.typography.labelMedium, color = NoirTextMuted)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(onClick = { showEditDialog = true }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit activity", tint = NoirTextPrimary)
                }
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete activity", tint = NoirDanger)
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                NoirStatusPill(text = activity.activityTypeEnum.label)
                NoirStatusPill(text = if (activity.manualEntry) "MANUAL ENTRY" else "GPS TRACKED")
            }
            Text(
                text = activity.title,
                style = MaterialTheme.typography.displaySmall,
                color = NoirTextPrimary
            )
            Text(
                text = buildString {
                    append(HaversineMath.formatDisplayDate(activity.startedAtEpochMs, profile.use24HourClock))
                    activity.locationName?.takeIf { it.isNotBlank() }?.let { append(" · $it") }
                },
                style = MaterialTheme.typography.bodySmall,
                color = NoirTextSecondary
            )
        }

        // Interactive Google Map Route History
        NoirInteractiveMap(
            points = activity.routePoints,
            isLiveTracking = false,
            height = 260.dp
        )

        // Core Route Statistics
        NoirCard(padding = PaddingValues(18.dp)) {
            Text("ROUTE TELEMETRY", style = MaterialTheme.typography.labelMedium, color = NoirTextMuted)
            Spacer(modifier = Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                DetailStat("DISTANCE", HaversineMath.formatDistance(activity.distanceMeters, unit))
                DetailStat("DURATION", HaversineMath.formatDuration(activity.durationSec))
                DetailStat("AVG PACE", HaversineMath.formatPace(activity.avgPaceSecPerKm, unit))
            }
            Spacer(modifier = Modifier.height(14.dp))
            NoirDivider(subtle = true)
            Spacer(modifier = Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                DetailStat("AVG SPEED", HaversineMath.formatSpeed(activity.avgSpeedMps, unit))
                DetailStat("ELEV GAIN", "${activity.elevationGainMeters.toInt()} m")
                DetailStat("CALORIES", "${activity.caloriesBurned} kcal")
            }
        }

        if (elevationProfile.size >= 2) {
            NoirCard(padding = PaddingValues(18.dp)) {
                Text("ELEVATION PROFILE (METERS)", style = MaterialTheme.typography.labelMedium, color = NoirTextMuted)
                Spacer(modifier = Modifier.height(10.dp))
                NoirLineChart(data = elevationProfile, unitSuffix = "m")
            }
        }

        if (segmentSpeedSeries.size >= 2) {
            NoirCard(padding = PaddingValues(18.dp)) {
                Text("SPEED PROFILE (KM/H)", style = MaterialTheme.typography.labelMedium, color = NoirTextMuted)
                Spacer(modifier = Modifier.height(10.dp))
                NoirLineChart(data = segmentSpeedSeries, unitSuffix = "km/h")
            }
        }

        if (!activity.notes.isNullOrBlank()) {
            NoirCard(padding = PaddingValues(18.dp)) {
                Text("SESSION NOTES", style = MaterialTheme.typography.labelMedium, color = NoirTextMuted)
                Spacer(modifier = Modifier.height(8.dp))
                Text(activity.notes, style = MaterialTheme.typography.bodyMedium, color = NoirTextPrimary)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showEditDialog) {
        var editTitle by remember { mutableStateOf(activity.title) }
        var editLoc by remember { mutableStateOf(activity.locationName ?: "") }
        var editNotes by remember { mutableStateOf(activity.notes ?: "") }
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            containerColor = NoirBackgroundSecondary,
            title = { Text("Edit Activity", style = MaterialTheme.typography.headlineLarge, color = NoirTextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    NoirTextField(value = editTitle, onValueChange = { editTitle = it }, label = "Title")
                    NoirTextField(value = editLoc, onValueChange = { editLoc = it }, label = "Location")
                    NoirTextField(
                        value = editNotes,
                        onValueChange = { editNotes = it },
                        label = "Notes",
                        singleLine = false,
                        maxLines = 4
                    )
                }
            },
            confirmButton = {
                NoirPrimaryButton(
                    text = "Save",
                    onClick = {
                        onUpdate(editTitle, editNotes.ifBlank { null }, editLoc.ifBlank { null })
                        showEditDialog = false
                    }
                )
            },
            dismissButton = {
                NoirSecondaryButton(text = "Cancel", onClick = { showEditDialog = false })
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = NoirBackgroundSecondary,
            title = { Text("Delete Activity?", style = MaterialTheme.typography.headlineLarge, color = NoirTextPrimary) },
            text = {
                Text(
                    "Permanently delete \"${activity.title}\" and its route points? This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NoirTextSecondary
                )
            },
            confirmButton = {
                NoirSecondaryButton(
                    text = "Delete",
                    borderColor = NoirDanger,
                    contentColor = NoirDanger,
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    }
                )
            },
            dismissButton = {
                NoirSecondaryButton(text = "Cancel", onClick = { showDeleteConfirm = false })
            }
        )
    }
}

@Composable
private fun DetailStat(label: String, value: String) {
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
