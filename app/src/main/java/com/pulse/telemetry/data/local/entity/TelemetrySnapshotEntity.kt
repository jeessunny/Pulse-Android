package com.pulse.telemetry.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "telemetry_snapshots")
data class TelemetrySnapshotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val timestamp: Long,
    val batteryLevel: Int,
    val batteryTemperature: Float,
    val isCharging: Boolean,
    val ramUsedBytes: Long,
    val ramPercentage: Int,
    val cpuPercentage: Float,
    val downloadSpeedBps: Long,
    val uploadSpeedBps: Long
)
