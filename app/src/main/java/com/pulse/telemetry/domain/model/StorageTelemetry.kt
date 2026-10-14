package com.pulse.telemetry.domain.model

data class StorageTelemetry(
    val totalBytes: Long = 0L,
    val usedBytes: Long = 0L,
    val freeBytes: Long = 0L,
    val usedPercentage: Int = 0,
    val formattedTotal: String = "0 GB",
    val formattedUsed: String = "0 GB",
    val formattedFree: String = "0 GB"
)
