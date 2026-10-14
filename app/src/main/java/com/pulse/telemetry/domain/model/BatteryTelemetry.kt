package com.pulse.telemetry.domain.model

data class AppPowerUsage(
    val packageName: String,
    val appName: String,
    val usageDurationMillis: Long,
    val percentageOfTotal: Float
)

data class BatteryTelemetry(
    val level: Int = 100,
    val isCharging: Boolean = false,
    val status: String = "Discharging",
    val pluggedType: String = "Unplugged",
    val voltageMv: Int = 4000,
    val temperatureCelsius: Float = 25.0f,
    val health: String = "Good",
    val estimatedDrainRatePerHour: Float = 0f,
    val appUsageList: List<AppPowerUsage> = emptyList(),
    val historicalLevels: List<Pair<Long, Int>> = emptyList()
)
