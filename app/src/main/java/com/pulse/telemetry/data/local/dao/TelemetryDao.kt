package com.pulse.telemetry.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pulse.telemetry.data.local.entity.TelemetrySnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TelemetryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: TelemetrySnapshotEntity): Long

    @Query("SELECT * FROM telemetry_snapshots ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentSnapshotsFlow(limit: Int): Flow<List<TelemetrySnapshotEntity>>

    @Query("SELECT * FROM telemetry_snapshots ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentSnapshots(limit: Int): List<TelemetrySnapshotEntity>

    @Query("SELECT * FROM telemetry_snapshots WHERE timestamp >= :sinceTimestamp ORDER BY timestamp ASC")
    suspend fun getSnapshotsSince(sinceTimestamp: Long): List<TelemetrySnapshotEntity>

    @Query("DELETE FROM telemetry_snapshots WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteOlderThan(cutoffTimestamp: Long): Int
}
