package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.NoirAccent
import com.example.ui.theme.NoirBackgroundPrimary
import com.example.ui.theme.NoirBackgroundSecondary
import com.example.ui.theme.NoirBorder
import com.example.ui.theme.NoirBorderSubtle
import com.example.ui.theme.NoirOnAccent
import com.example.ui.theme.NoirSuccess
import com.example.ui.theme.NoirSurfaceElevated
import com.example.ui.theme.NoirTextMuted
import com.example.ui.theme.NoirTextPrimary
import com.example.ui.theme.NoirTextSecondary
import com.example.ui.theme.NoirWarning

@Composable
fun NoirCard(
    modifier: Modifier = Modifier,
    elevated: Boolean = false,
    padding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val bg = if (elevated) NoirSurfaceElevated else NoirBackgroundSecondary
    val shape = RoundedCornerShape(4.dp)
    Surface(
        modifier = modifier
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = shape,
        color = bg,
        border = BorderStroke(1.dp, NoirBorder),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(padding), content = content)
    }
}

@Composable
fun NoirDivider(modifier: Modifier = Modifier, subtle: Boolean = false) {
    HorizontalDivider(
        modifier = modifier,
        thickness = 1.dp,
        color = if (subtle) NoirBorderSubtle else NoirBorder
    )
}

@Composable
fun NoirSectionHeader(
    eyebrow: String,
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = eyebrow.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = NoirTextMuted
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.displaySmall,
                color = NoirTextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(12.dp))
            trailing()
        }
    }
}

@Composable
fun NoirStatusPill(
    text: String,
    modifier: Modifier = Modifier,
    accentColor: Color = NoirTextSecondary,
    borderColor: Color = NoirBorder
) {
    Box(
        modifier = modifier
            .border(1.dp, borderColor, RoundedCornerShape(2.dp))
            .background(NoirSurfaceElevated, RoundedCornerShape(2.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = accentColor,
            maxLines = 1
        )
    }
}

@Composable
fun NoirProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 3.dp,
    fillColor: Color = NoirAccent
) {
    val clamped = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(NoirBorder)
    ) {
        if (clamped > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(clamped)
                    .height(height)
                    .background(fillColor)
            )
        }
    }
}

@Composable
fun NoirKpiCard(
    label: String,
    value: String,
    targetText: String,
    percent: Float,
    deltaText: String,
    deltaPositive: Boolean?,
    modifier: Modifier = Modifier
) {
    NoirCard(modifier = modifier, padding = PaddingValues(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = NoirTextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${(percent * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = if (percent >= 1f) NoirSuccess else NoirTextSecondary
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
            color = NoirTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = targetText,
            style = MaterialTheme.typography.bodySmall,
            color = NoirTextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(10.dp))
        NoirProgressBar(progress = percent)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = deltaText,
            style = MaterialTheme.typography.labelSmall,
            color = when (deltaPositive) {
                true -> NoirSuccess
                false -> NoirWarning
                null -> NoirTextMuted
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun NoirPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    tag: String? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .heightIn(min = 48.dp)
            .then(if (tag != null) Modifier.testTag(tag) else Modifier),
        shape = RoundedCornerShape(3.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = NoirAccent,
            contentColor = NoirOnAccent,
            disabledContainerColor = NoirBorder,
            disabledContentColor = NoirTextMuted
        ),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun NoirSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    tag: String? = null,
    borderColor: Color = NoirBorder,
    contentColor: Color = NoirTextPrimary
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .heightIn(min = 48.dp)
            .then(if (tag != null) Modifier.testTag(tag) else Modifier),
        shape = RoundedCornerShape(3.dp),
        border = BorderStroke(1.dp, borderColor),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = NoirBackgroundSecondary,
            contentColor = contentColor,
            disabledContentColor = NoirTextMuted
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun NoirSelectableChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tag: String? = null
) {
    val bg = if (selected) NoirAccent else NoirBackgroundSecondary
    val fg = if (selected) NoirOnAccent else NoirTextSecondary
    val border = if (selected) NoirAccent else NoirBorder
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .then(if (tag != null) Modifier.testTag(tag) else Modifier)
            .clip(RoundedCornerShape(3.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(3.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            ),
            color = fg,
            maxLines = 1
        )
    }
}

@Composable
fun NoirTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    maxLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    tag: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = MaterialTheme.typography.bodySmall) },
        placeholder = if (placeholder.isNotEmpty()) {
            { Text(placeholder, color = NoirTextMuted, style = MaterialTheme.typography.bodyMedium) }
        } else null,
        singleLine = singleLine,
        maxLines = maxLines,
        keyboardOptions = keyboardOptions,
        modifier = modifier
            .fillMaxWidth()
            .then(if (tag != null) Modifier.testTag(tag) else Modifier),
        shape = RoundedCornerShape(3.dp),
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = NoirTextPrimary),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = NoirAccent,
            unfocusedBorderColor = NoirBorder,
            focusedContainerColor = NoirBackgroundPrimary,
            unfocusedContainerColor = NoirBackgroundPrimary,
            focusedLabelColor = NoirTextPrimary,
            unfocusedLabelColor = NoirTextMuted,
            cursorColor = NoirAccent
        )
    )
}

@Composable
fun NoirEmptyState(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    NoirCard(modifier = modifier.fillMaxWidth(), padding = PaddingValues(24.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = NoirTextPrimary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = NoirTextSecondary
        )
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(16.dp))
            NoirPrimaryButton(text = actionLabel, onClick = onAction)
        }
    }
}
