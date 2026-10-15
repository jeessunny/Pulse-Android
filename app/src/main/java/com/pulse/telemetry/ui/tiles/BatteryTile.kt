package com.pulse.telemetry.ui.tiles

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulse.telemetry.domain.model.BatteryTelemetry
import com.pulse.telemetry.ui.components.CircularGauge
import com.pulse.telemetry.ui.components.MetricSparklineChart
import com.pulse.telemetry.ui.components.TileCard
import com.pulse.telemetry.ui.theme.MetricAmber
import com.pulse.telemetry.ui.theme.MetricGreen
import com.pulse.telemetry.ui.theme.MetricRed
import java.util.Locale

@Composable
fun BatteryTile(
    telemetry: BatteryTelemetry,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val levelColor = when {
        telemetry.level > 50 -> MetricGreen
        telemetry.level > 20 -> MetricAmber
        else -> MetricRed
    }

    val icon = if (telemetry.isCharging) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BatteryFull

    TileCard(
        title = "Battery",
        icon = icon,
        iconTint = levelColor,
        isExpanded = isExpanded,
        onToggleExpand = onToggleExpand,
        onMoveUp = onMoveUp,
        onMoveDown = onMoveDown,
        modifier = modifier
    ) {
        if (!isExpanded) {
            // Square Compact Mode
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularGauge(
                    percentage = telemetry.level,
                    size = 62.dp,
                    strokeWidth = 6.dp,
                    primaryColor = levelColor,
                    subLabel = if (telemetry.isCharging) "Charging" else "${telemetry.temperatureCelsius}°C"
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = telemetry.status,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${String.format(Locale.US, "%.1f", telemetry.voltageMv / 1000f)}V",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            // Expanded Mode
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularGauge(
                            percentage = telemetry.level,
                            size = 56.dp,
                            strokeWidth = 5.dp,
                            primaryColor = levelColor
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${telemetry.level}%",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (telemetry.isCharging) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MetricGreen.copy(alpha = 0.15f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = telemetry.pluggedType,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MetricGreen
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "Health: ${telemetry.health} • ${telemetry.temperatureCelsius}°C",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${telemetry.estimatedDrainRatePerHour}%/hr",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (telemetry.estimatedDrainRatePerHour > 12f) MetricRed else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Drain Rate",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Discharge History (24h)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                val points = telemetry.historicalLevels.map { it.second.toFloat() }
                MetricSparklineChart(
                    points = if (points.size >= 2) points else listOf(telemetry.level.toFloat(), telemetry.level.toFloat()),
                    lineColor = levelColor,
                    maxLabel = "100%",
                    minLabel = "0%"
                )

                if (telemetry.appUsageList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Top Power Consumers",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    telemetry.appUsageList.take(3).forEach { app ->
                        Column(modifier = Modifier.padding(vertical = 3.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = app.appName,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    maxLines = 1
                                )
                                Text(
                                    text = "${String.format(Locale.US, "%.1f", app.percentageOfTotal)}%",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            LinearProgressIndicator(
                                progress = { (app.percentageOfTotal / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                strokeCap = StrokeCap.Round
                            )
                        }
                    }
                }
            }
        }
    }
}
