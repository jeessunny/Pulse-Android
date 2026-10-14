package com.pulse.telemetry.domain.model

data class NetworkTelemetry(
    val downloadSpeedBps: Long = 0L,
    val uploadSpeedBps: Long = 0L,
    val sessionRxBytes: Long = 0L,
    val sessionTxBytes: Long = 0L,
    val wifiBytesToday: Long = 0L,
    val cellularBytesToday: Long = 0L,
    val pingMs: Long = 0L,
    val isWifi: Boolean = false,
    val isCellular: Boolean = false,
    val isConnected: Boolean = true,
    val connectionType: String = "Wi-Fi",
    val speedHistory: List<Long> = emptyList()
)
