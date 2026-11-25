package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.HeatmapDay
import com.example.data.model.RoutePoint
import com.example.ui.theme.NoirAccent
import com.example.ui.theme.NoirBorder
import com.example.ui.theme.NoirBorderSubtle
import com.example.ui.theme.NoirHeat0
import com.example.ui.theme.NoirHeat1
import com.example.ui.theme.NoirHeat2
import com.example.ui.theme.NoirHeat3
import com.example.ui.theme.NoirHeat4
import com.example.ui.theme.NoirTextMuted
import com.example.ui.theme.NoirTextPrimary
import com.example.ui.theme.NoirTextSecondary
import kotlin.math.max

@Composable
fun NoirBarChart(
    data: List<Pair<String, Double>>,
    unitSuffix: String,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 148.dp,
    emptyLabel: String = "No recorded data for this period."
) {
    if (data.isEmpty() || data.all { it.second <= 0.0 }) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(chartHeight),
            contentAlignment = Alignment.Center
        ) {
            Text(emptyLabel, style = MaterialTheme.typography.bodySmall, color = NoirTextMuted)
        }
        return
    }

    var revealed by remember(data) { mutableStateOf(false) }
    LaunchedEffect(data) { revealed = true }
    val animProgress by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = tween(durationMillis = 420),
        label = "bar_reveal"
    )

    val maxVal = max(0.01, data.maxOf { it.second })

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "MAX ${String.format("%.1f", maxVal)} $unitSuffix",
                style = MaterialTheme.typography.labelSmall,
                color = NoirTextMuted
            )
            Text(
                text = "TOTAL ${String.format("%.1f", data.sumOf { it.second })} $unitSuffix",
                style = MaterialTheme.typography.labelSmall,
                color = NoirTextSecondary
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight)
        ) {
            val w = size.width
            val h = size.height
            // Horizontal hairline gridlines
            for (i in 0..3) {
                val y = h * (i / 3f)
                drawLine(
                    color = NoirBorderSubtle,
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1f
                )
            }

            val count = data.size
            val slotWidth = w / count
            val barWidth = (slotWidth * 0.44f).coerceAtLeast(8f)

            data.forEachIndexed { idx, (_, value) ->
                val frac = ((value / maxVal).toFloat() * animProgress).coerceIn(0f, 1f)
                val barHeight = (h * 0.92f) * frac
                val left = idx * slotWidth + (slotWidth - barWidth) / 2f
                val top = h - barHeight
                drawRect(
                    color = if (value >= maxVal * 0.85) NoirAccent else Color(0xFF7A7A7A),
                    topLeft = Offset(left, top),
                    size = Size(barWidth, barHeight.coerceAtLeast(2f))
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            data.forEach { (label, _) ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = NoirTextMuted,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun NoirLineChart(
    data: List<Pair<String, Double>>,
    unitSuffix: String,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 140.dp,
    emptyLabel: String = "Log at least 2 entries to view progression."
) {
    if (data.size < 2) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(chartHeight),
            contentAlignment = Alignment.Center
        ) {
            Text(emptyLabel, style = MaterialTheme.typography.bodySmall, color = NoirTextMuted)
        }
        return
    }

    val minVal = data.minOf { it.second }
    val maxVal = data.maxOf { it.second }
    val span = (maxVal - minVal).takeIf { it > 0.001 } ?: 1.0

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "MIN ${String.format("%.1f", minVal)} $unitSuffix",
                style = MaterialTheme.typography.labelSmall,
                color = NoirTextMuted
            )
            Text(
                text = "MAX ${String.format("%.1f", maxVal)} $unitSuffix",
                style = MaterialTheme.typography.labelSmall,
                color = NoirTextSecondary
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight)
        ) {
            val w = size.width
            val h = size.height
            val padY = 12f

            for (i in 0..3) {
                val y = padY + (h - 2 * padY) * (i / 3f)
                drawLine(NoirBorderSubtle, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
            }

            val path = Path()
            val stepX = w / (data.size - 1).coerceAtLeast(1)
            val coords = data.mapIndexed { idx, (_, value) ->
                val x = idx * stepX
                val norm = ((value - minVal) / span).toFloat().coerceIn(0f, 1f)
                val y = (h - padY) - norm * (h - 2 * padY)
                Offset(x, y)
            }

            coords.forEachIndexed { idx, pt ->
                if (idx == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
            }

            drawPath(
                path = path,
                color = NoirAccent,
                style = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            coords.forEach { pt ->
                drawCircle(color = Color(0xFF080808), radius = 5f, center = pt)
                drawCircle(color = NoirAccent, radius = 3f, center = pt)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(data.first().first, style = MaterialTheme.typography.labelSmall, color = NoirTextMuted)
            if (data.size >= 3) {
                Text(data[data.size / 2].first, style = MaterialTheme.typography.labelSmall, color = NoirTextMuted)
            }
            Text(data.last().first, style = MaterialTheme.typography.labelSmall, color = NoirTextMuted)
        }
    }
}

@Composable
fun NoirRouteThumbnail(
    points: List<RoutePoint>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color(0xFF0B0B0B), RoundedCornerShape(3.dp))
            .border(1.dp, NoirBorder, RoundedCornerShape(3.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (points.size < 2) {
            Text("NO GPS", style = MaterialTheme.typography.labelSmall, color = NoirTextMuted)
        } else {
            Canvas(modifier = Modifier.matchParentSize().padding(8.dp)) {
                val minLat = points.minOf { it.lat }
                val maxLat = points.maxOf { it.lat }
                val minLng = points.minOf { it.lng }
                val maxLng = points.maxOf { it.lng }
                val latSpan = (maxLat - minLat).takeIf { it > 1e-6 } ?: 1e-4
                val lngSpan = (maxLng - minLng).takeIf { it > 1e-6 } ?: 1e-4

                val path = Path()
                points.forEachIndexed { idx, p ->
                    val x = (((p.lng - minLng) / lngSpan).toFloat()) * size.width
                    val y = (1f - ((p.lat - minLat) / latSpan).toFloat()) * size.height
                    if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(
                    path = path,
                    color = NoirAccent,
                    style = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                val first = points.first()
                val last = points.last()
                val sx = (((first.lng - minLng) / lngSpan).toFloat()) * size.width
                val sy = (1f - ((first.lat - minLat) / latSpan).toFloat()) * size.height
                val ex = (((last.lng - minLng) / lngSpan).toFloat()) * size.width
                val ey = (1f - ((last.lat - minLat) / latSpan).toFloat()) * size.height
                drawCircle(NoirTextSecondary, radius = 3.5f, center = Offset(sx, sy))
                drawCircle(NoirAccent, radius = 4f, center = Offset(ex, ey))
            }
        }
    }
}

@Composable
fun NoirActivityHeatmap(
    days: List<HeatmapDay>,
    modifier: Modifier = Modifier
) {
    val weeks = days.chunked(7)
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            weeks.forEach { week ->
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    week.forEach { day ->
                        val color = when (day.intensityLevel) {
                            0 -> NoirHeat0
                            1 -> NoirHeat1
                            2 -> NoirHeat2
                            3 -> NoirHeat3
                            else -> NoirHeat4
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(13.dp)
                                .background(color, RoundedCornerShape(1.5.dp))
                                .border(0.5.dp, NoirBorderSubtle, RoundedCornerShape(1.5.dp))
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("LESS", style = MaterialTheme.typography.labelSmall, color = NoirTextMuted)
            Spacer(modifier = Modifier.width(6.dp))
            listOf(NoirHeat0, NoirHeat1, NoirHeat2, NoirHeat3, NoirHeat4).forEach { c ->
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(c, RoundedCornerShape(1.dp))
                        .border(0.5.dp, NoirBorder, RoundedCornerShape(1.dp))
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text("MORE", style = MaterialTheme.typography.labelSmall, color = NoirTextMuted)
        }
    }
}

@Composable
fun NoirDistributionBars(
    items: List<Pair<String, Int>>,
    modifier: Modifier = Modifier
) {
    val total = items.sumOf { it.second }.coerceAtLeast(1)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEach { (label, count) ->
            val fraction = count.toFloat() / total.toFloat()
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(label, style = MaterialTheme.typography.bodySmall, color = NoirTextPrimary)
                    Text(
                        "$count · ${(fraction * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = NoirTextSecondary
                    )
                }
                Spacer(modifier = Modifier.height(5.dp))
                NoirProgressBar(progress = fraction, height = 4.dp)
            }
        }
    }
}
