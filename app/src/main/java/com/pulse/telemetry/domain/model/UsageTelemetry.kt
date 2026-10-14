package com.pulse.telemetry.domain.model

data class AppUsageDetail(
    val packageName: String,
    val appName: String,
    val foregroundTimeMillis: Long,
    val formattedDuration: String,
    val percentageOfTotal: Float
)

data class UsageTelemetry(
    val hasPermission: Boolean = false,
    val totalScreenTimeMillis: Long = 0L,
    val formattedScreenTime: String = "0h 0m",
    val unlockCount: Int = 0,
    val topApps: List<AppUsageDetail> = emptyList()
)
