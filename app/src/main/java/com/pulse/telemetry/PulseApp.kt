package com.pulse.telemetry

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.pulse.telemetry.worker.TelemetryCleanupWorker
import java.util.concurrent.TimeUnit

class PulseApp : Application() {

    override fun onCreate() {
        super.onCreate()
        scheduleTelemetryCleanup()
    }

    private fun scheduleTelemetryCleanup() {
        val cleanupRequest = PeriodicWorkRequestBuilder<TelemetryCleanupWorker>(24, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "PulseTelemetryCleanupWork",
            ExistingPeriodicWorkPolicy.KEEP,
            cleanupRequest
        )
    }
}
