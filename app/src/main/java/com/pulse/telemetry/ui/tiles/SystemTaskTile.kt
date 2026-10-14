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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulse.telemetry.domain.model.SystemTelemetry
import com.pulse.telemetry.ui.components.CircularGauge
import com.pulse.telemetry.ui.components.MetricSparklineChart
import com.pulse.telemetry.ui.components.TileCard
import com.pulse.telemetry.ui.theme.MetricCyan
import com.pulse.telemetry.ui.theme.MetricGreen
import com.pulse.telemetry.ui.theme.MetricPurple
import com.pulse.telemetry.ui.theme.MetricRed
import java.util.Locale

@Composable
fun SystemTaskTile(
    telemetry: SystemTelemetry,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val ramColor = when {
        telemetry.ramPercentage > 85 -> MetricRed
        telemetry.ramPercentage > 70 -> MetricPurple
        else -> MetricCyan
    }

    TileCard(
        title = "System & Tasks",
        icon = Icons.Rounded.Memory,
        iconTint = MetricPurple,
        isExpanded = isExpanded,
        onToggleExpand = onToggleExpand,
        onMoveUp = onMoveUp,
        onMoveDown = onMoveDown,
        modifier = modifier
    ) {
        val totalGb = String.format(Locale.US, "%.1f GB", telemetry.totalRamBytes / (1024.0 * 1024 * 1024))
        val usedGb = String.format(Locale.US, "%.1f GB", telemetry.usedRamBytes / (1024.0 * 1024 * 1024))

        if (!isExpanded) {
            // Compact Mode
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularGauge(
                    percentage = telemetry.ramPercentage,
                    size = 80.dp,
                    strokeWidth = 7.dp,
                    primaryColor = ramColor,
                    subLabel = "RAM"
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "$usedGb / $totalGb",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "CPU: ${telemetry.cpuUsagePercentage}%",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MetricGreen
                    )
                }
            }
        } else {
            // Expanded Mode
            Column(modifier = Modifier.fillMaxWidth()) {
                // Main stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularGauge(
                            percentage = telemetry.ramPercentage,
                            size = 64.dp,
                            strokeWidth = 6.dp,
                            primaryColor = ramColor
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "$usedGb Used",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Total Capacity: $totalGb",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${telemetry.activeProcessesCount} Tasks",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${telemetry.cpuCoreCount} Active CPU Cores",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // CPU Load row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Overall CPU Load",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${telemetry.cpuUsagePercentage}%",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MetricGreen
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Historical RAM Memory Pressure Graph
                Text(
                    text = "Historical Memory Pressure (RAM %)",
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                val ramPoints = telemetry.ramHistory.map { it.toFloat() }
                MetricSparklineChart(
                    points = if (ramPoints.size >= 2) ramPoints else listOf(telemetry.ramPercentage.toFloat(), telemetry.ramPercentage.toFloat()),
                    lineColor = ramColor,
                    maxLabel = "100%",
                    minLabel = "0%"
                )
            }
        }
    }
}
