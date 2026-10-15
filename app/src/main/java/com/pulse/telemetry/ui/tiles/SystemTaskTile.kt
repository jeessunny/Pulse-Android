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
        title = "System",
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
            // Square Compact Mode
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularGauge(
                    percentage = telemetry.ramPercentage,
                    size = 62.dp,
                    strokeWidth = 6.dp,
                    primaryColor = ramColor,
                    subLabel = "RAM"
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$usedGb/$totalGb",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "CPU ${telemetry.cpuUsagePercentage}%",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MetricGreen
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
                            percentage = telemetry.ramPercentage,
                            size = 56.dp,
                            strokeWidth = 5.dp,
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
                                text = "Total RAM: $totalGb",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
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
                            text = "${telemetry.cpuCoreCount} Cores",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CPU Utilization",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${telemetry.cpuUsagePercentage}%",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MetricGreen
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "RAM Pressure Trend",
                    style = MaterialTheme.typography.labelSmall,
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
