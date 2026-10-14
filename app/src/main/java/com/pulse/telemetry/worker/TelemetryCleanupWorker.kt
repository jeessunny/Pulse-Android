package com.pulse.telemetry.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pulse.telemetry.data.repository.TelemetryRepository

class TelemetryCleanupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return runCatching {
            val repository = TelemetryRepository(applicationContext)
            repository.pruneOldLogs(daysToKeep = 7)
            Result.success()
        }.getOrDefault(Result.retry())
    }
}
