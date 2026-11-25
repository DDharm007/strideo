package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.model.ActivityType
import com.example.data.model.HaversineMath
import com.example.data.model.LiveRecordingState
import com.example.data.model.RecordingPhase
import com.example.data.model.UserProfile
import com.example.ui.components.NoirCard
import com.example.ui.components.NoirDivider
import com.example.ui.components.NoirInteractiveMap
import com.example.ui.components.NoirPrimaryButton
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
import com.example.ui.theme.NoirWarning

@Composable
fun LiveTrackingScreen(
    recordingState: LiveRecordingState,
    profile: UserProfile,
    onSelectType: (ActivityType) -> Unit,
    onRequestLocationFix: () -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit,
    onDiscard: () -> Unit,
    onSaveCompleted: (title: String, notes: String?, locationName: String?) -> Unit,
    onAddManualActivity: (
        title: String,
        type: ActivityType,
        distanceInput: Double,
        durationMinutes: Long,
        elevationMeters: Double,
        locationName: String?,
        notes: String?
    ) -> Unit
) {
    val context = LocalContext.current
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionDeniedExplicitly by remember { mutableStateOf(false) }
    var showManualDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = granted
        permissionDeniedExplicitly = !granted
        if (granted) {
            onRequestLocationFix()
        }
    }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            onRequestLocationFix()
        }
    }

    val unit = profile.distanceUnitEnum

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        NoirSectionHeader(
            eyebrow = "GPS Telemetry Engine",
            title = "Live Activity Tracking",
            trailing = {
                NoirSecondaryButton(
                    text = "Manual",
                    icon = Icons.Default.EditNote,
                    tag = "manual_entry_open_button",
                    onClick = { showManualDialog = true }
                )
            }
        )

        // Explicit Location Permission Explanation Card (when not granted)
        if (!hasLocationPermission) {
            NoirCard(padding = PaddingValues(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "FOREGROUND LOCATION ACCESS",
                        style = MaterialTheme.typography.labelMedium,
                        color = NoirWarning
                    )
                    NoirStatusPill(
                        text = if (permissionDeniedExplicitly) "DENIED" else "REQUIRED FOR GPS",
                        accentColor = NoirWarning,
                        borderColor = NoirWarning
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Strideo uses your device's foreground GPS sensor only while an activity session is actively open to compute Haversine distance, live speed, pace, and route coordinates on open-source maps.",
                    style = MaterialTheme.typography.bodySmall,
                    color = NoirTextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NoirPrimaryButton(
                        text = "Grant GPS Permission",
                        icon = Icons.Default.GpsFixed,
                        tag = "grant_gps_button",
                        onClick = {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    )
                    NoirSecondaryButton(
                        text = "Manual Log",
                        onClick = { showManualDialog = true }
                    )
                }
            }
        }

        // Activity Type Selector (locked while active)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActivityType.entries.forEach { type ->
                NoirSelectableChip(
                    label = type.label,
                    selected = recordingState.activityType == type,
                    onClick = { onSelectType(type) },
                    tag = "activity_type_${type.name.lowercase()}"
                )
            }
        }

        // Full-featured Open-Source Interactive Map (OSMDroid)
        NoirInteractiveMap(
            points = recordingState.points,
            currentLat = recordingState.currentLat,
            currentLng = recordingState.currentLng,
            isLiveTracking = recordingState.phase == RecordingPhase.RECORDING,
            height = 300.dp,
            onRequestLocationFix = {
                if (hasLocationPermission) {
                    onRequestLocationFix()
                } else {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            }
        )

        // State Machine Status & Live Primary Telemetry
        NoirCard(padding = PaddingValues(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val stateAccent = when (recordingState.phase) {
                    RecordingPhase.RECORDING -> NoirSuccess
                    RecordingPhase.PAUSED -> NoirWarning
                    RecordingPhase.COMPLETED -> NoirTextPrimary
                    RecordingPhase.IDLE -> NoirTextMuted
                }
                NoirStatusPill(
                    text = recordingState.phase.name,
                    accentColor = stateAccent,
                    borderColor = stateAccent
                )
                Text(
                    text = recordingState.gpsStatusMessage,
                    style = MaterialTheme.typography.labelSmall,
                    color = NoirTextSecondary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Primary Hero Readouts: Elapsed Time & Distance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("ELAPSED TIME", style = MaterialTheme.typography.labelSmall, color = NoirTextMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = HaversineMath.formatDuration(recordingState.elapsedSec),
                        style = MaterialTheme.typography.displayMedium,
                        color = NoirTextPrimary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("DISTANCE", style = MaterialTheme.typography.labelSmall, color = NoirTextMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = HaversineMath.formatDistance(recordingState.distanceMeters, unit),
                        style = MaterialTheme.typography.displayMedium,
                        color = NoirTextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            NoirDivider(subtle = true)
            Spacer(modifier = Modifier.height(14.dp))

            // Secondary Live Telemetry Grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                LiveMetricCell("CURR PACE", HaversineMath.formatPace(recordingState.currentPaceSecPerKm, unit))
                LiveMetricCell("AVG PACE", HaversineMath.formatPace(recordingState.avgPaceSecPerKm, unit))
                LiveMetricCell("CALORIES", "${recordingState.caloriesBurned} kcal")
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                LiveMetricCell("CURR SPEED", HaversineMath.formatSpeed(recordingState.currentSpeedMps, unit))
                LiveMetricCell("AVG SPEED", HaversineMath.formatSpeed(recordingState.avgSpeedMps, unit))
                LiveMetricCell(
                    "GPS ACC",
                    recordingState.gpsAccuracyMeters?.let { "±${it.toInt()} m" } ?: "—"
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            NoirDivider(subtle = true)
            Spacer(modifier = Modifier.height(14.dp))

            // State Machine Controls
            when (recordingState.phase) {
                RecordingPhase.IDLE -> {
                    NoirPrimaryButton(
                        text = "Start ${recordingState.activityType.label}",
                        icon = Icons.Default.PlayArrow,
                        tag = "start_recording_button",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onStart
                    )
                }
                RecordingPhase.RECORDING -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        NoirSecondaryButton(
                            text = "Pause",
                            icon = Icons.Default.Pause,
                            tag = "pause_recording_button",
                            modifier = Modifier.weight(1f),
                            onClick = onPause
                        )
                        NoirPrimaryButton(
                            text = "Finish",
                            icon = Icons.Default.Stop,
                            tag = "finish_recording_button",
                            modifier = Modifier.weight(1f),
                            onClick = onFinish
                        )
                    }
                }
                RecordingPhase.PAUSED -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        NoirPrimaryButton(
                            text = "Resume",
                            icon = Icons.Default.PlayArrow,
                            tag = "resume_recording_button",
                            modifier = Modifier.weight(1f),
                            onClick = onResume
                        )
                        NoirSecondaryButton(
                            text = "Finish",
                            icon = Icons.Default.Stop,
                            tag = "finish_paused_button",
                            modifier = Modifier.weight(1f),
                            onClick = onFinish
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    NoirSecondaryButton(
                        text = "Discard Session",
                        icon = Icons.Default.DeleteOutline,
                        borderColor = NoirDanger,
                        contentColor = NoirDanger,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onDiscard
                    )
                }
                RecordingPhase.COMPLETED -> {
                    Text(
                        text = "Session finished. Review and save your record below.",
                        style = MaterialTheme.typography.bodySmall,
                        color = NoirSuccess
                    )
                }
            }
        }

        // Save / Discard Panel when COMPLETED
        if (recordingState.phase == RecordingPhase.COMPLETED) {
            SaveCompletedActivityCard(
                recordingState = recordingState,
                onSave = onSaveCompleted,
                onDiscard = onDiscard
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showManualDialog) {
        ManualActivityDialog(
            unitShort = unit.shortLabel,
            onDismiss = { showManualDialog = false },
            onSave = { title, type, dist, mins, elev, loc, notes ->
                onAddManualActivity(title, type, dist, mins, elev, loc, notes)
                showManualDialog = false
            }
        )
    }
}

@Composable
private fun SaveCompletedActivityCard(
    recordingState: LiveRecordingState,
    onSave: (String, String?, String?) -> Unit,
    onDiscard: () -> Unit
) {
    var title by remember(recordingState.startedAtEpochMs) {
        mutableStateOf("${recordingState.activityType.label} Session")
    }
    var location by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    NoirCard(elevated = true, padding = PaddingValues(18.dp)) {
        Text("SAVE COMPLETED SESSION", style = MaterialTheme.typography.labelMedium, color = NoirTextMuted)
        Spacer(modifier = Modifier.height(12.dp))
        NoirTextField(value = title, onValueChange = { title = it }, label = "Activity Title", tag = "save_activity_title")
        Spacer(modifier = Modifier.height(10.dp))
        NoirTextField(value = location, onValueChange = { location = it }, label = "Location (optional)")
        Spacer(modifier = Modifier.height(10.dp))
        NoirTextField(
            value = notes,
            onValueChange = { notes = it },
            label = "Notes (optional)",
            singleLine = false,
            maxLines = 3
        )
        Spacer(modifier = Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            NoirSecondaryButton(
                text = "Discard",
                borderColor = NoirDanger,
                contentColor = NoirDanger,
                modifier = Modifier.weight(1f),
                enabled = !recordingState.isSubmitting,
                onClick = onDiscard
            )
            NoirPrimaryButton(
                text = if (recordingState.isSubmitting) "Saving..." else "Save Activity",
                icon = Icons.Default.Check,
                tag = "save_completed_activity_button",
                modifier = Modifier.weight(1f),
                enabled = !recordingState.isSubmitting,
                onClick = { onSave(title, notes.ifBlank { null }, location.ifBlank { null }) }
            )
        }
    }
}

@Composable
fun ManualActivityDialog(
    unitShort: String,
    onDismiss: () -> Unit,
    onSave: (String, ActivityType, Double, Long, Double, String?, String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(ActivityType.RUNNING) }
    var distanceText by remember { mutableStateOf("5.0") }
    var durationText by remember { mutableStateOf("30") }
    var elevationText by remember { mutableStateOf("40") }
    var locationText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NoirBackgroundSecondary,
        title = {
            Text("Log Manual Activity", style = MaterialTheme.typography.headlineLarge, color = NoirTextPrimary)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ActivityType.entries.forEach { t ->
                        NoirSelectableChip(
                            label = t.label,
                            selected = selectedType == t,
                            onClick = { selectedType = t }
                        )
                    }
                }
                NoirTextField(value = title, onValueChange = { title = it }, label = "Title", placeholder = "Morning Tempo Run")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NoirTextField(
                        value = distanceText,
                        onValueChange = { distanceText = it },
                        label = "Distance ($unitShort)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    NoirTextField(
                        value = durationText,
                        onValueChange = { durationText = it },
                        label = "Duration (min)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NoirTextField(
                        value = elevationText,
                        onValueChange = { elevationText = it },
                        label = "Elevation (m)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    NoirTextField(
                        value = locationText,
                        onValueChange = { locationText = it },
                        label = "Location",
                        modifier = Modifier.weight(1f)
                    )
                }
                NoirTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = "Notes",
                    singleLine = false,
                    maxLines = 3
                )
                validationError?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = NoirDanger)
                }
            }
        },
        confirmButton = {
            NoirPrimaryButton(
                text = "Save Entry",
                tag = "confirm_manual_activity_btn",
                onClick = {
                    val dist = distanceText.toDoubleOrNull()
                    val mins = durationText.toLongOrNull()
                    val elev = elevationText.toDoubleOrNull() ?: 0.0
                    if (dist == null || dist < 0.0 || mins == null || mins <= 0L) {
                        validationError = "Enter a valid non-negative distance and positive duration."
                    } else {
                        onSave(
                            title.ifBlank { "${selectedType.label} Session" },
                            selectedType,
                            dist,
                            mins,
                            elev.coerceAtLeast(0.0),
                            locationText.ifBlank { null },
                            notesText.ifBlank { null }
                        )
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
private fun LiveMetricCell(label: String, value: String) {
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
