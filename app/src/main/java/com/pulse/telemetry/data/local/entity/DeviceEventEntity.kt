package com.pulse.telemetry.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "device_events")
data class DeviceEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val timestamp: Long,
    val eventType: String,
    val description: String,
    val stepsCount: Int
)
