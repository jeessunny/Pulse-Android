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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.ElectricBolt
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PowerOff
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulse.telemetry.domain.model.DeviceEventItem
import com.pulse.telemetry.ui.components.TileCard
import com.pulse.telemetry.ui.theme.MetricCyan
import com.pulse.telemetry.ui.theme.MetricGreen
import com.pulse.telemetry.ui.theme.MetricPurple
import com.pulse.telemetry.ui.theme.MetricRed

@Composable
fun DeviceEventsTile(
    events: List<DeviceEventItem>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val latestEvent = events.firstOrNull()

    TileCard(
        title = "Events",
        icon = Icons.Rounded.History,
        iconTint = MetricPurple,
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
                Text(
                    text = "${events.size}",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "System Events",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = latestEvent?.description ?: "Monitoring active",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                if (latestEvent != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = latestEvent.relativeTime,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        } else {
            // Expanded Mode
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${events.size} Recorded Events",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Hardware & connectivity changes",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Live Log",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MetricCyan
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (events.isEmpty()) {
                    Text(
                        text = "System events like charging state, screen status, and network connections will be listed here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    events.take(5).forEach { event ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val (eventIcon, eventTint) = getDeviceEventVisuals(event.eventType)
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(eventTint.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = eventIcon,
                                    contentDescription = null,
                                    tint = eventTint,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = event.description,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    maxLines = 1
                                )
                                Text(
                                    text = "${event.relativeTime} (${event.formattedTime})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getDeviceEventVisuals(eventType: String): Pair<ImageVector, Color> {
    return when (eventType) {
        "SCREEN_ON" -> Icons.Rounded.Visibility to MetricCyan
        "SCREEN_OFF" -> Icons.Rounded.VisibilityOff to Color.Gray
        "POWER_CONNECTED" -> Icons.Rounded.ElectricBolt to MetricGreen
        "POWER_DISCONNECTED" -> Icons.Rounded.PowerOff to MetricRed
        "BATTERY_LOW" -> Icons.Rounded.BatteryAlert to MetricRed
        "NETWORK_CHANGE" -> Icons.Rounded.Wifi to MetricPurple
        else -> Icons.Rounded.Smartphone to MetricCyan
    }
}
