package com.example.ui.screens

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.GoalMetricType
import com.example.data.model.GoalPeriod
import com.example.data.model.GoalProgressItem
import com.example.data.model.HeatmapDay
import com.example.data.model.StreakMetrics
import com.example.ui.components.NoirActivityHeatmap
import com.example.ui.components.NoirCard
import com.example.ui.components.NoirEmptyState
import com.example.ui.components.NoirPrimaryButton
import com.example.ui.components.NoirProgressBar
import com.example.ui.components.NoirSecondaryButton
import com.example.ui.components.NoirSectionHeader
import com.example.ui.components.NoirSelectableChip
import com.example.ui.components.NoirStatusPill
import com.example.ui.components.NoirTextField
import com.example.ui.theme.NoirBackgroundSecondary
import com.example.ui.theme.NoirSuccess
import com.example.ui.theme.NoirTextMuted
import com.example.ui.theme.NoirTextPrimary
import com.example.ui.theme.NoirTextSecondary
import java.util.Locale

@Composable
fun GoalsScreen(
    goalProgress: List<GoalProgressItem>,
    streaks: StreakMetrics,
    heatmapDays: List<HeatmapDay>,
    onSaveGoal: (GoalMetricType, Double, GoalPeriod, String) -> Unit,
    onDeleteGoal: (String) -> Unit
) {
    var showAddGoalDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            NoirSectionHeader(
                eyebrow = "Targets & Consistency",
                title = "Goals & Streaks",
                trailing = {
                    NoirPrimaryButton(
                        text = "Set Goal",
                        icon = Icons.Default.Add,
                        tag = "add_goal_button",
                        onClick = { showAddGoalDialog = true }
                    )
                }
            )
        }

        // Streak Summary Card
        item {
            NoirCard(padding = PaddingValues(18.dp)) {
                Text("DISCIPLINE STREAKS", style = MaterialTheme.typography.labelMedium, color = NoirTextMuted)
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StreakMetricBlock("CURRENT STREAK", "${streaks.currentStreakDays} days")
                    StreakMetricBlock("LONGEST STREAK", "${streaks.longestStreakDays} days")
                    StreakMetricBlock("THIS WEEK", "${streaks.activeDaysThisWeek} / 7")
                }
            }
        }

        // GitHub-Style Monochrome Activity Heatmap
        item {
            NoirCard(padding = PaddingValues(18.dp)) {
                Text("ACTIVITY CONTRIBUTION MATRIX (12 WEEKS)", style = MaterialTheme.typography.labelMedium, color = NoirTextMuted)
                Spacer(modifier = Modifier.height(12.dp))
                NoirActivityHeatmap(days = heatmapDays)
            }
        }

        item {
            Text("ACTIVE TARGETS", style = MaterialTheme.typography.labelMedium, color = NoirTextMuted)
        }

        if (goalProgress.isEmpty()) {
            item {
                NoirEmptyState(
                    title = "No targets configured",
                    description = "Set daily step, hydration, sleep, or weekly distance targets to track truthful completion.",
                    actionLabel = "Create First Goal",
                    onAction = { showAddGoalDialog = true }
                )
            }
        } else {
            items(goalProgress, key = { it.goal.id }) { item ->
                NoirCard(padding = PaddingValues(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                NoirStatusPill(text = item.goal.periodEnum.label)
                                Text(
                                    text = "${(item.percentComplete * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (item.percentComplete >= 1f) NoirSuccess else NoirTextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = item.goal.metricTypeEnum.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = NoirTextPrimary
                            )
                            Text(
                                text = formatGoalProgressText(item),
                                style = MaterialTheme.typography.bodySmall,
                                color = NoirTextSecondary
                            )
                        }
                        IconButton(onClick = { onDeleteGoal(item.goal.id) }) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete goal",
                                tint = NoirTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    NoirProgressBar(
                        progress = item.percentComplete,
                        height = 4.dp,
                        fillColor = if (item.percentComplete >= 1f) NoirSuccess else NoirTextPrimary
                    )
                }
            }
        }

        // Weekly Achievements derived truthfully from stored records
        item {
            Text("MILESTONES", style = MaterialTheme.typography.labelMedium, color = NoirTextMuted)
        }
        items(streaks.achievements, key = { it.id }) { badge ->
            NoirCard(padding = PaddingValues(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = badge.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (badge.unlocked) NoirTextPrimary else NoirTextMuted
                        )
                        Text(
                            text = badge.detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = NoirTextSecondary
                        )
                    }
                    Icon(
                        imageVector = if (badge.unlocked) Icons.Default.CheckCircleOutline else Icons.Default.Lock,
                        contentDescription = if (badge.unlocked) "Unlocked" else "Locked",
                        tint = if (badge.unlocked) NoirSuccess else NoirTextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }

    if (showAddGoalDialog) {
        GoalEditorDialog(
            onDismiss = { showAddGoalDialog = false },
            onSave = { metric, target, period ->
                onSaveGoal(metric, target, period, "")
                showAddGoalDialog = false
            }
        )
    }
}

@Composable
private fun StreakMetricBlock(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = NoirTextMuted)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
            color = NoirTextPrimary
        )
    }
}

@Composable
private fun GoalEditorDialog(
    onDismiss: () -> Unit,
    onSave: (GoalMetricType, Double, GoalPeriod) -> Unit
) {
    var selectedMetric by remember { mutableStateOf(GoalMetricType.WEEKLY_RUN_KM) }
    var targetText by remember(selectedMetric) { mutableStateOf(selectedMetric.defaultTarget.toInt().toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NoirBackgroundSecondary,
        title = { Text("Configure Target", style = MaterialTheme.typography.headlineLarge, color = NoirTextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    GoalMetricType.entries.forEach { m ->
                        NoirSelectableChip(
                            label = m.title,
                            selected = selectedMetric == m,
                            onClick = { selectedMetric = m }
                        )
                    }
                }
                NoirTextField(
                    value = targetText,
                    onValueChange = { targetText = it },
                    label = "Target (${selectedMetric.defaultUnit}) · ${selectedMetric.defaultPeriod.label}",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        },
        confirmButton = {
            NoirPrimaryButton(
                text = "Save Goal",
                tag = "confirm_goal_button",
                onClick = {
                    val value = targetText.toDoubleOrNull()?.coerceAtLeast(1.0) ?: selectedMetric.defaultTarget
                    onSave(selectedMetric, value, selectedMetric.defaultPeriod)
                }
            )
        },
        dismissButton = {
            NoirSecondaryButton(text = "Cancel", onClick = onDismiss)
        }
    )
}

private fun formatGoalProgressText(item: GoalProgressItem): String {
    return when (item.goal.metricTypeEnum) {
        GoalMetricType.DAILY_SLEEP_MIN -> {
            val curH = item.currentValue / 60.0
            val tgtH = item.goal.targetValue / 60.0
            String.format(Locale.US, "%.1f / %.1f hrs", curH, tgtH)
        }
        GoalMetricType.WEEKLY_RUN_KM, GoalMetricType.MONTHLY_CYCLE_KM, GoalMetricType.DAILY_DISTANCE_KM -> {
            String.format(Locale.US, "%.1f / %.1f %s", item.currentValue, item.goal.targetValue, item.goal.unit)
        }
        else -> {
            String.format(Locale.US, "%,.0f / %,.0f %s", item.currentValue, item.goal.targetValue, item.goal.unit)
        }
    }
}
