package com.pulse.telemetry.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulse.telemetry.ui.theme.MetricCyan

@Composable
fun MetricSparklineChart(
    points: List<Float>,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(110.dp),
    lineColor: Color = MetricCyan,
    minLabel: String = "",
    maxLabel: String = "",
    showGrid: Boolean = true
) {
    if (points.isEmpty()) return

    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(points.size) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(1f, animationSpec = tween(durationMillis = 650))
    }

    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    val textStyle = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

    Box(modifier = modifier.padding(vertical = 4.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val padding = 8.dp.toPx()

            val effectiveHeight = height - (padding * 2)
            val effectiveWidth = width - (padding * 2)

            val maxVal = points.maxOrNull()?.coerceAtLeast(1f) ?: 1f
            val minVal = points.minOrNull() ?: 0f
            val range = if (maxVal == minVal) 1f else maxVal - minVal

            // Draw subtle horizontal grid lines
            if (showGrid) {
                val gridLines = 3
                for (i in 0..gridLines) {
                    val y = padding + (effectiveHeight / gridLines) * i
                    drawLine(
                        color = gridColor,
                        start = Offset(padding, y),
                        end = Offset(width - padding, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                    )
                }
            }

            // Calculate graph coordinates
            val coords = points.mapIndexed { index, value ->
                val x = padding + (index.toFloat() / (points.size - 1).coerceAtLeast(1)) * effectiveWidth
                val normalizedY = (value - minVal) / range
                val y = height - padding - (normalizedY * effectiveHeight * animatedProgress.value)
                Offset(x, y)
            }

            if (coords.size >= 2) {
                // Line Path
                val strokePath = Path().apply {
                    moveTo(coords.first().x, coords.first().y)
                    for (i in 1 until coords.size) {
                        val p0 = coords[i - 1]
                        val p1 = coords[i]
                        val controlX = (p0.x + p1.x) / 2
                        cubicTo(controlX, p0.y, controlX, p1.y, p1.x, p1.y)
                    }
                }

                // Fill gradient underneath
                val fillPath = Path().apply {
                    addPath(strokePath)
                    lineTo(coords.last().x, height - padding)
                    lineTo(coords.first().x, height - padding)
                    close()
                }

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            lineColor.copy(alpha = 0.35f),
                            lineColor.copy(alpha = 0.0f)
                        ),
                        startY = padding,
                        endY = height - padding
                    )
                )

                // Draw main stroke
                drawPath(
                    path = strokePath,
                    color = lineColor,
                    style = Stroke(
                        width = 2.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )

                // Draw glowing pulse dot at the latest point
                val latestPoint = coords.last()
                drawCircle(
                    color = lineColor.copy(alpha = 0.3f),
                    radius = 6.dp.toPx(),
                    center = latestPoint
                )
                drawCircle(
                    color = lineColor,
                    radius = 3.dp.toPx(),
                    center = latestPoint
                )
            }
        }

        // Min and Max Labels
        if (maxLabel.isNotBlank()) {
            Text(
                text = maxLabel,
                style = textStyle,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(horizontal = 4.dp)
            )
        }
        if (minLabel.isNotBlank()) {
            Text(
                text = minLabel,
                style = textStyle,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 4.dp)
            )
        }
    }
}
