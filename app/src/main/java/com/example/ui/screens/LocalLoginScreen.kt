package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.HaversineMath
import com.example.data.model.UserProfile
import com.example.ui.components.NoirCard
import com.example.ui.components.NoirDivider
import com.example.ui.components.NoirPrimaryButton
import com.example.ui.components.NoirSecondaryButton
import com.example.ui.components.NoirSelectableChip
import com.example.ui.components.NoirStatusPill
import com.example.ui.components.NoirTextField
import com.example.ui.theme.NoirBackgroundPrimary
import com.example.ui.theme.NoirBorder
import com.example.ui.theme.NoirDanger
import com.example.ui.theme.NoirSuccess
import com.example.ui.theme.NoirTextMuted
import com.example.ui.theme.NoirTextPrimary
import com.example.ui.theme.NoirTextSecondary
import java.util.Calendar
import java.util.Locale

@Composable
fun LocalLoginScreen(
    existingProfile: UserProfile?,
    onResumeExisting: () -> Unit,
    onCompleteLocalLogin: (
        displayName: String,
        birthDay: Int,
        birthMonth: Int,
        birthYear: Int,
        heightCm: Double,
        weightKg: Double
    ) -> Unit
) {
    var name by remember(existingProfile?.displayName) {
        mutableStateOf(existingProfile?.displayName ?: "")
    }
    var birthDayText by remember(existingProfile?.birthDay) {
        mutableStateOf(existingProfile?.birthDay?.toString() ?: "")
    }
    var selectedMonth by remember(existingProfile?.birthMonth) {
        mutableIntStateOf(existingProfile?.birthMonth?.coerceIn(1, 12) ?: 1)
    }
    var birthYearText by remember(existingProfile?.birthYear) {
        mutableStateOf(existingProfile?.birthYear?.toString() ?: "")
    }
    var heightText by remember(existingProfile?.heightCm) {
        mutableStateOf(existingProfile?.heightCm?.let { formatTrimmed(it) } ?: "")
    }
    var weightText by remember(existingProfile?.weightKg) {
        mutableStateOf(existingProfile?.weightKg?.let { formatTrimmed(it) } ?: "")
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val parsedDay = birthDayText.toIntOrNull()
    val parsedYear = birthYearText.toIntOrNull()
    val parsedHeight = heightText.toDoubleOrNull()
    val parsedWeight = weightText.toDoubleOrNull()

    val previewBmi = HaversineMath.calculateBmi(parsedWeight, parsedHeight)
    val previewAge = remember(parsedDay, selectedMonth, parsedYear) {
        if (parsedDay != null && parsedYear != null && parsedDay in 1..31 && parsedYear in 1920..2022) {
            computeAge(parsedDay, selectedMonth, parsedYear)
        } else null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NoirBackgroundPrimary)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Brand Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "STRIDEO",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = NoirTextPrimary
                )
                NoirStatusPill(text = "PRIVATE · ON-DEVICE")
            }

            // Editorial Monochrome Hero Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(172.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .border(1.dp, NoirBorder, RoundedCornerShape(4.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_noir_hero),
                    contentDescription = "Monochrome athlete running across a bridge",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Editorial Title
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "ATHLETE PROFILE",
                    style = MaterialTheme.typography.labelMedium,
                    color = NoirTextMuted
                )
                Text(
                    text = "Pure GPS performance. Zero cloud lock-in.",
                    style = MaterialTheme.typography.displayMedium,
                    color = NoirTextPrimary
                )
                Text(
                    text = "Track live open-source GPS routes, pace, elevation, health metrics, and home screen widgets — stored securely on your device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NoirTextSecondary
                )
            }

            // Quick Resume Card if local athlete previously signed out
            if (existingProfile != null) {
                NoirCard(elevated = true, padding = PaddingValues(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "RETURNING ATHLETE",
                                style = MaterialTheme.typography.labelSmall,
                                color = NoirSuccess
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = existingProfile.displayName,
                                style = MaterialTheme.typography.titleLarge,
                                color = NoirTextPrimary
                            )
                            Text(
                                text = "Born ${existingProfile.birthDateFormatted} · ${existingProfile.ageYears} yrs",
                                style = MaterialTheme.typography.bodySmall,
                                color = NoirTextSecondary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    NoirPrimaryButton(
                        text = "Continue as ${existingProfile.displayName.substringBefore(" ")}",
                        icon = Icons.AutoMirrored.Filled.ArrowForward,
                        tag = "resume_local_profile_btn",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onResumeExisting
                    )
                }
            }

            // Local Profile Login Form
            NoirCard(padding = PaddingValues(18.dp)) {
                Text(
                    text = if (existingProfile != null) "UPDATE OR SWITCH LOCAL PROFILE" else "ENTER YOUR ATHLETE DETAILS",
                    style = MaterialTheme.typography.labelMedium,
                    color = NoirTextMuted
                )
                Spacer(modifier = Modifier.height(14.dp))

                NoirTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = null
                    },
                    label = "Full Name",
                    placeholder = "Alex Mercer",
                    tag = "login_name_input",
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                )

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "BIRTH DATE",
                    style = MaterialTheme.typography.labelSmall,
                    color = NoirTextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Month Selector Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    UserProfile.MONTH_SHORT_NAMES.forEachIndexed { idx, monthLabel ->
                        val monthNum = idx + 1
                        NoirSelectableChip(
                            label = monthLabel,
                            selected = selectedMonth == monthNum,
                            onClick = {
                                selectedMonth = monthNum
                                errorMessage = null
                            },
                            tag = "login_month_${monthLabel.lowercase()}"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    NoirTextField(
                        value = birthDayText,
                        onValueChange = { input ->
                            if (input.length <= 2 && input.all { it.isDigit() }) {
                                birthDayText = input
                                errorMessage = null
                            }
                        },
                        label = "Day (1-31)",
                        placeholder = "15",
                        tag = "login_birth_day_input",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    NoirTextField(
                        value = birthYearText,
                        onValueChange = { input ->
                            if (input.length <= 4 && input.all { it.isDigit() }) {
                                birthYearText = input
                                errorMessage = null
                            }
                        },
                        label = "Year (YYYY)",
                        placeholder = "1996",
                        tag = "login_birth_year_input",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1.3f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "BODY METRICS",
                    style = MaterialTheme.typography.labelSmall,
                    color = NoirTextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    NoirTextField(
                        value = heightText,
                        onValueChange = {
                            heightText = it
                            errorMessage = null
                        },
                        label = "Height (cm)",
                        placeholder = "176",
                        tag = "login_height_input",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    NoirTextField(
                        value = weightText,
                        onValueChange = {
                            weightText = it
                            errorMessage = null
                        },
                        label = "Weight (kg)",
                        placeholder = "70",
                        tag = "login_weight_input",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Live Calculated Biometric Preview
                if (previewAge != null || previewBmi != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    NoirDivider(subtle = true)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("AGE", style = MaterialTheme.typography.labelSmall, color = NoirTextMuted)
                            Text(
                                text = previewAge?.let { "$it years" } ?: "—",
                                style = MaterialTheme.typography.titleMedium,
                                color = NoirTextPrimary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("CALCULATED BMI", style = MaterialTheme.typography.labelSmall, color = NoirTextMuted)
                            Text(
                                text = previewBmi?.let { String.format(Locale.US, "%.1f", it) } ?: "—",
                                style = MaterialTheme.typography.titleMedium,
                                color = NoirTextPrimary
                            )
                        }
                    }
                }

                errorMessage?.let { err ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodySmall,
                        color = NoirDanger
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                val submitAction: () -> Unit = {
                    val cleanName = name.trim()
                    val day = birthDayText.toIntOrNull()
                    val year = birthYearText.toIntOrNull()
                    val height = heightText.toDoubleOrNull()
                    val weight = weightText.toDoubleOrNull()
                    val maxDaysInMonth = maxDayForMonth(selectedMonth, year ?: 2000)

                    when {
                        cleanName.length < 2 -> {
                            errorMessage = "Please enter your name (at least 2 characters)."
                        }
                        day == null || day !in 1..maxDaysInMonth -> {
                            errorMessage = "Enter a valid birth day between 1 and $maxDaysInMonth for ${UserProfile.MONTH_SHORT_NAMES[selectedMonth - 1]}."
                        }
                        year == null || year !in 1920..2022 -> {
                            errorMessage = "Enter a valid birth year between 1920 and 2022."
                        }
                        height == null || height !in 80.0..250.0 -> {
                            errorMessage = "Enter a valid height in cm (80 – 250)."
                        }
                        weight == null || weight !in 25.0..300.0 -> {
                            errorMessage = "Enter a valid weight in kg (25 – 300)."
                        }
                        else -> {
                            onCompleteLocalLogin(cleanName, day, selectedMonth, year, height, weight)
                        }
                    }
                }

                if (existingProfile != null) {
                    NoirSecondaryButton(
                        text = "Save Details & Enter Strideo",
                        icon = Icons.Default.Person,
                        tag = "local_login_submit_btn",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = submitAction
                    )
                } else {
                    NoirPrimaryButton(
                        text = "Enter Strideo",
                        icon = Icons.AutoMirrored.Filled.ArrowForward,
                        tag = "local_login_submit_btn",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = submitAction
                    )
                }
            }

            // Local Privacy Footnote
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Local privacy",
                    tint = NoirTextMuted,
                    modifier = Modifier.padding(end = 2.dp)
                )
                Text(
                    text = "100% local SQLite storage. No account servers, no subscriptions, and zero external data sharing.",
                    style = MaterialTheme.typography.bodySmall,
                    color = NoirTextMuted
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun maxDayForMonth(month: Int, year: Int): Int {
    return when (month) {
        2 -> if ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0) 29 else 28
        4, 6, 9, 11 -> 30
        else -> 31
    }
}

private fun computeAge(day: Int, month: Int, year: Int): Int {
    val today = Calendar.getInstance()
    val cy = today.get(Calendar.YEAR)
    val cm = today.get(Calendar.MONTH) + 1
    val cd = today.get(Calendar.DAY_OF_MONTH)
    var age = cy - year
    if (cm < month || (cm == month && cd < day)) age--
    return age.coerceIn(0, 120)
}

private fun formatTrimmed(v: Double): String {
    return if (v % 1.0 == 0.0) v.toInt().toString() else String.format(Locale.US, "%.1f", v)
}
