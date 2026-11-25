package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.DistanceUnit
import com.example.data.model.UserProfile
import com.example.data.model.WeightUnit
import com.example.ui.components.NoirCard
import com.example.ui.components.NoirDivider
import com.example.ui.components.NoirPrimaryButton
import com.example.ui.components.NoirSecondaryButton
import com.example.ui.components.NoirSectionHeader
import com.example.ui.components.NoirSelectableChip
import com.example.ui.components.NoirStatusPill
import com.example.ui.components.NoirTextField
import com.example.ui.theme.NoirAccent
import com.example.ui.theme.NoirBackgroundPrimary
import com.example.ui.theme.NoirBackgroundSecondary
import com.example.ui.theme.NoirBorder
import com.example.ui.theme.NoirDanger
import com.example.ui.theme.NoirSurfaceElevated
import com.example.ui.theme.NoirTextMuted
import com.example.ui.theme.NoirTextPrimary
import com.example.ui.theme.NoirTextSecondary
import com.example.widget.StrideoWidgetUpdater
import java.util.TimeZone

@Composable
fun SettingsScreen(
    profile: UserProfile,
    onSaveProfile: (UserProfile) -> Unit,
    onExportCsv: () -> String,
    onExportJson: () -> String,
    onNotify: (String) -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccountData: () -> Unit
) {
    val context = LocalContext.current

    var displayName by remember(profile.displayName) { mutableStateOf(profile.displayName) }
    var birthDayText by remember(profile.birthDay) { mutableStateOf(profile.birthDay.toString()) }
    var birthMonth by remember(profile.birthMonth) { mutableIntStateOf(profile.birthMonth.coerceIn(1, 12)) }
    var birthYearText by remember(profile.birthYear) { mutableStateOf(profile.birthYear.toString()) }
    var photoUrl by remember(profile.photoUrl) { mutableStateOf(profile.photoUrl) }
    var heightText by remember(profile.heightCm) { mutableStateOf(profile.heightCm?.toString() ?: "") }
    var weightText by remember(profile.weightKg) { mutableStateOf(profile.weightKg?.toString() ?: "") }
    var timezone by remember(profile.timezone) { mutableStateOf(profile.timezone ?: TimeZone.getDefault().id) }
    var unitDistance by remember(profile.unitDistance) { mutableStateOf(profile.distanceUnitEnum) }
    var unitWeight by remember(profile.unitWeight) { mutableStateOf(profile.weightUnitEnum) }
    var use24Hour by remember(profile.use24HourClock) { mutableStateOf(profile.use24HourClock) }
    var notificationsEnabled by remember(profile.notificationsEnabled) { mutableStateOf(profile.notificationsEnabled) }
    var privateProfile by remember(profile.privateProfile) { mutableStateOf(profile.privateProfile) }

    var exportPreview by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Zero-permission Android Photo Picker (Play Policy compliant)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            photoUrl = uri.toString()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            NoirSectionHeader(
                eyebrow = "Identity, Widgets & Preferences",
                title = "Profile & Settings"
            )
        }

        // Profile Details & Photo Picker
        item {
            NoirCard(padding = PaddingValues(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("ATHLETE PROFILE", style = MaterialTheme.typography.labelMedium, color = NoirTextMuted)
                    NoirStatusPill(text = "${profile.ageYears} YRS · LOCAL")
                }
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(NoirSurfaceElevated)
                            .border(1.dp, NoirBorder, RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!photoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = photoUrl,
                                contentDescription = "User profile avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = displayName.take(2).uppercase(),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = NoirTextPrimary
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(displayName, style = MaterialTheme.typography.titleMedium, color = NoirTextPrimary)
                        Text(
                            text = "Born ${profile.birthDateFormatted}",
                            style = MaterialTheme.typography.bodySmall,
                            color = NoirTextSecondary
                        )
                        NoirSecondaryButton(
                            text = "Choose Photo",
                            icon = Icons.Default.PhotoLibrary,
                            tag = "pick_profile_photo_btn",
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                NoirTextField(value = displayName, onValueChange = { displayName = it }, label = "Full Name")

                Spacer(modifier = Modifier.height(12.dp))
                Text("BIRTH DATE", style = MaterialTheme.typography.labelSmall, color = NoirTextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    UserProfile.MONTH_SHORT_NAMES.forEachIndexed { idx, label ->
                        NoirSelectableChip(
                            label = label,
                            selected = birthMonth == idx + 1,
                            onClick = { birthMonth = idx + 1 }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NoirTextField(
                        value = birthDayText,
                        onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) birthDayText = it },
                        label = "Day",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    NoirTextField(
                        value = birthYearText,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) birthYearText = it },
                        label = "Year",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1.3f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NoirTextField(
                        value = heightText,
                        onValueChange = { heightText = it },
                        label = "Height (cm)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    NoirTextField(
                        value = weightText,
                        onValueChange = { weightText = it },
                        label = "Weight (kg)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                NoirTextField(value = timezone, onValueChange = { timezone = it }, label = "Timezone")
            }
        }

        // Android OS Home Screen Widgets Section
        item {
            NoirCard(padding = PaddingValues(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("ANDROID OS WIDGETS", style = MaterialTheme.typography.labelMedium, color = NoirTextMuted)
                    NoirStatusPill(text = "HOME SCREEN")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Pin Strideo widgets directly to your Android home screen for live daily steps, distance, +250 ml hydration, and one-tap GPS launch. Widgets lock automatically when you sign out.",
                    style = MaterialTheme.typography.bodySmall,
                    color = NoirTextSecondary
                )
                Spacer(modifier = Modifier.height(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    NoirPrimaryButton(
                        text = "Add Daily Summary Widget (4×2)",
                        icon = Icons.Default.Widgets,
                        tag = "pin_summary_widget_button",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val pinned = StrideoWidgetUpdater.requestPinSummaryWidget(context)
                            if (pinned) {
                                onNotify("Follow the system prompt to place the Daily Summary widget")
                            } else {
                                onNotify("Long-press your Android home screen → Widgets → Strideo")
                            }
                        }
                    )
                    NoirSecondaryButton(
                        text = "Add Quick GPS Widget (2×2)",
                        icon = Icons.Default.Widgets,
                        tag = "pin_quick_widget_button",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val pinned = StrideoWidgetUpdater.requestPinQuickTrackWidget(context)
                            if (pinned) {
                                onNotify("Follow the system prompt to place the Quick GPS widget")
                            } else {
                                onNotify("Long-press your Android home screen → Widgets → Strideo")
                            }
                        }
                    )
                }
            }
        }

        // Units & Display Settings
        item {
            NoirCard(padding = PaddingValues(18.dp)) {
                Text("UNITS & PREFERENCES", style = MaterialTheme.typography.labelMedium, color = NoirTextMuted)
                Spacer(modifier = Modifier.height(12.dp))

                Text("DISTANCE UNIT", style = MaterialTheme.typography.labelSmall, color = NoirTextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DistanceUnit.entries.forEach { u ->
                        NoirSelectableChip(
                            label = u.label,
                            selected = unitDistance == u,
                            onClick = { unitDistance = u },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("WEIGHT UNIT", style = MaterialTheme.typography.labelSmall, color = NoirTextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WeightUnit.entries.forEach { u ->
                        NoirSelectableChip(
                            label = u.label,
                            selected = unitWeight == u,
                            onClick = { unitWeight = u },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                NoirDivider(subtle = true)
                SettingToggleRow("24-Hour Clock", use24Hour) { use24Hour = it }
                NoirDivider(subtle = true)
                SettingToggleRow("Activity & Goal Notifications", notificationsEnabled) { notificationsEnabled = it }
                NoirDivider(subtle = true)
                SettingToggleRow("Private Health Profile", privateProfile) { privateProfile = it }

                Spacer(modifier = Modifier.height(14.dp))
                NoirPrimaryButton(
                    text = "Save Profile & Preferences",
                    tag = "save_settings_button",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        onSaveProfile(
                            profile.copy(
                                displayName = displayName.trim().ifBlank { "Athlete" },
                                birthDay = (birthDayText.toIntOrNull() ?: profile.birthDay).coerceIn(1, 31),
                                birthMonth = birthMonth.coerceIn(1, 12),
                                birthYear = (birthYearText.toIntOrNull() ?: profile.birthYear).coerceIn(1920, 2022),
                                photoUrl = photoUrl,
                                heightCm = heightText.toDoubleOrNull()?.coerceIn(50.0, 260.0),
                                weightKg = weightText.toDoubleOrNull()?.coerceIn(20.0, 350.0),
                                unitDistance = unitDistance.name,
                                unitWeight = unitWeight.name,
                                use24HourClock = use24Hour,
                                notificationsEnabled = notificationsEnabled,
                                privateProfile = privateProfile,
                                timezone = timezone.trim().ifBlank { TimeZone.getDefault().id }
                            )
                        )
                    }
                )
            }
        }

        // Data Export (CSV / JSON)
        item {
            NoirCard(padding = PaddingValues(18.dp)) {
                Text("DATA PORTABILITY", style = MaterialTheme.typography.labelMedium, color = NoirTextMuted)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Export your full activity history and health metrics stored on-device in CSV or JSON format.",
                    style = MaterialTheme.typography.bodySmall,
                    color = NoirTextSecondary
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    NoirSecondaryButton(
                        text = "Export CSV",
                        icon = Icons.Default.Download,
                        tag = "export_csv_button",
                        modifier = Modifier.weight(1f),
                        onClick = { exportPreview = "CSV Export" to onExportCsv() }
                    )
                    NoirSecondaryButton(
                        text = "Export JSON",
                        icon = Icons.Default.Download,
                        tag = "export_json_button",
                        modifier = Modifier.weight(1f),
                        onClick = { exportPreview = "JSON Export" to onExportJson() }
                    )
                }
            }
        }

        // Local Session & Privacy Controls
        item {
            NoirCard(padding = PaddingValues(18.dp)) {
                Text("PRIVACY & SESSION", style = MaterialTheme.typography.labelMedium, color = NoirTextMuted)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Signing out locks your local Strideo session and hides personal metrics on home screen widgets until you sign back in.",
                    style = MaterialTheme.typography.bodySmall,
                    color = NoirTextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                NoirSecondaryButton(
                    text = "Sign Out & Lock Widgets",
                    icon = Icons.AutoMirrored.Filled.Logout,
                    tag = "sign_out_button",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onSignOut
                )
                Spacer(modifier = Modifier.height(10.dp))
                NoirSecondaryButton(
                    text = "Delete All Local Data",
                    borderColor = NoirDanger,
                    contentColor = NoirDanger,
                    tag = "delete_account_data_button",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showDeleteConfirm = true }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }

    exportPreview?.let { (title, payload) ->
        AlertDialog(
            onDismissRequest = { exportPreview = null },
            containerColor = NoirBackgroundSecondary,
            title = { Text(title, style = MaterialTheme.typography.headlineLarge, color = NoirTextPrimary) },
            text = {
                Column(
                    modifier = Modifier
                        .height(260.dp)
                        .verticalScroll(rememberScrollState())
                        .background(NoirBackgroundPrimary, RoundedCornerShape(3.dp))
                        .border(1.dp, NoirBorder, RoundedCornerShape(3.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = payload.take(4000),
                        style = MaterialTheme.typography.bodySmall,
                        color = NoirTextSecondary
                    )
                }
            },
            confirmButton = {
                NoirPrimaryButton(
                    text = "Share Export",
                    onClick = {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Strideo $title")
                            putExtra(Intent.EXTRA_TEXT, payload)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share $title"))
                        exportPreview = null
                    }
                )
            },
            dismissButton = {
                NoirSecondaryButton(text = "Close", onClick = { exportPreview = null })
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = NoirBackgroundSecondary,
            title = { Text("Delete All Local Records?", style = MaterialTheme.typography.headlineLarge, color = NoirTextPrimary) },
            text = {
                Text(
                    "This permanently erases your local profile, GPS routes, activities, health metrics, hydration logs, sleep logs, and goals from this device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NoirTextSecondary
                )
            },
            confirmButton = {
                NoirSecondaryButton(
                    text = "Delete Everything",
                    borderColor = NoirDanger,
                    contentColor = NoirDanger,
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteAccountData()
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
private fun SettingToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium, color = NoirTextPrimary)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = NoirBackgroundPrimary,
                checkedTrackColor = NoirAccent,
                uncheckedThumbColor = NoirTextMuted,
                uncheckedTrackColor = NoirSurfaceElevated,
                uncheckedBorderColor = NoirBorder
            )
        )
    }
}
