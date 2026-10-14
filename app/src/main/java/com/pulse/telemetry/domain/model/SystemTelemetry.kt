package com.pulse.telemetry.domain.model

data class SystemTelemetry(
    val totalRamBytes: Long = 0L,
    val usedRamBytes: Long = 0L,
    val availableRamBytes: Long = 0L,
    val ramPercentage: Int = 0,
    val cpuUsagePercentage: Float = 0f,
    val cpuCoreCount: Int = 8,
    val activeProcessesCount: Int = 0,
    val isLowMemory: Boolean = false,
    val ramHistory: List<Int> = emptyList()
)
