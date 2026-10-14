package com.pulse.telemetry.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.pulse.telemetry.data.local.dao.DeviceEventDao
import com.pulse.telemetry.data.local.dao.TelemetryDao
import com.pulse.telemetry.data.local.entity.DeviceEventEntity
import com.pulse.telemetry.data.local.entity.TelemetrySnapshotEntity

@Database(
    entities = [
        TelemetrySnapshotEntity::class,
        DeviceEventEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class PulseDatabase : RoomDatabase() {
    abstract fun telemetryDao(): TelemetryDao
    abstract fun deviceEventDao(): DeviceEventDao

    companion object {
        @Volatile
        private var INSTANCE: PulseDatabase? = null

        fun getInstance(context: Context): PulseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PulseDatabase::class.java,
                    "pulse_telemetry.db"
                ).fallbackToDestructiveMigration()
                 .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
