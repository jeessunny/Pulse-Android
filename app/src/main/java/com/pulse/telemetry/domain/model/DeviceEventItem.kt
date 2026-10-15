package com.pulse.telemetry.domain.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DeviceEventItem(
    val id: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: String = "INFO",
    val description: String = "",
    val stepsCount: Int = 0,
    val motionState: String = "Stationary"
) {
    val formattedTime: String
        get() {
            val format = SimpleDateFormat("HH:mm", Locale.getDefault())
            return format.format(Date(timestamp))
        }

    val relativeTime: String
        get() {
            val diffMs = System.currentTimeMillis() - timestamp
            val diffSec = diffMs / 1000
            val diffMin = diffSec / 60
            val diffHr = diffMin / 60
            return when {
                diffMin < 1 -> "Just now"
                diffMin < 60 -> "${diffMin}m ago"
                diffHr < 24 -> "${diffHr}h ago"
                else -> "${diffHr / 24}d ago"
            }
        }
}
