package com.pulse.telemetry.domain.model

data class DeviceEventItem(
    val id: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: String = "INFO",
    val description: String = "",
    val stepsCount: Int = 0,
    val motionState: String = "Stationary"
)
