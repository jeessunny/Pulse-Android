package com.pulse.telemetry.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pulse.telemetry.data.local.entity.DeviceEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: DeviceEventEntity): Long

    @Query("SELECT * FROM device_events ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentEventsFlow(limit: Int): Flow<List<DeviceEventEntity>>

    @Query("SELECT * FROM device_events ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentEvents(limit: Int): List<DeviceEventEntity>

    @Query("DELETE FROM device_events WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteOlderThan(cutoffTimestamp: Long): Int
}
