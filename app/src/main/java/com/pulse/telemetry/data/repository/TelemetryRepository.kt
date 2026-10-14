package com.pulse.telemetry.data.repository

import android.content.Context
import com.pulse.telemetry.data.local.LayoutPreferences
import com.pulse.telemetry.data.local.PulseDatabase
import com.pulse.telemetry.data.local.entity.DeviceEventEntity
import com.pulse.telemetry.data.local.entity.TelemetrySnapshotEntity
import com.pulse.telemetry.domain.model.AppPowerUsage
import com.pulse.telemetry.domain.model.BatteryTelemetry
import com.pulse.telemetry.domain.model.DashboardTile
import com.pulse.telemetry.domain.model.DeviceEventItem
import com.pulse.telemetry.domain.model.NetworkTelemetry
import com.pulse.telemetry.domain.model.StorageTelemetry
import com.pulse.telemetry.domain.model.SystemTelemetry
import com.pulse.telemetry.domain.model.TileId
import com.pulse.telemetry.domain.model.UsageTelemetry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Locale

class TelemetryRepository(private val context: Context) {

    private val database = PulseDatabase.getInstance(context)
    private val telemetryDao = database.telemetryDao()
    private val deviceEventDao = database.deviceEventDao()
    private val systemMonitor = SystemMonitor(context)
    private val layoutPreferences = LayoutPreferences(context)

    val tilesFlow: Flow<List<DashboardTile>> = layoutPreferences.tilesFlow

    val recentEventsFlow: Flow<List<DeviceEventItem>> = deviceEventDao.getRecentEventsFlow(20).map { list ->
        list.map { entity ->
            DeviceEventItem(
                id = entity.id,
                timestamp = entity.timestamp,
                eventType = entity.eventType,
                description = entity.description,
                stepsCount = entity.stepsCount,
                motionState = systemMonitor.getMotionState()
            )
        }
    }

    suspend fun getLiveBattery(): BatteryTelemetry = withContext(Dispatchers.IO) {
        val base = systemMonitor.getBatteryTelemetry()
        val recentSnapshots = telemetryDao.getRecentSnapshots(30)

        // Historical battery levels for sparkline
        val historicalLevels = if (recentSnapshots.isNotEmpty()) {
            recentSnapshots.reversed().map { it.timestamp to it.batteryLevel }
        } else {
            listOf(System.currentTimeMillis() to base.level)
        }

        // Calculate estimated drain rate (%/hour)
        var drainRatePerHour = 0f
        if (recentSnapshots.size >= 2) {
            val oldest = recentSnapshots.last()
            val newest = recentSnapshots.first()
            val timeDiffHours = (newest.timestamp - oldest.timestamp) / (1000f * 60f * 60f)
            if (timeDiffHours > 0.05f) {
                val levelDiff = oldest.batteryLevel - newest.batteryLevel
                if (levelDiff > 0) {
                    drainRatePerHour = String.format(Locale.US, "%.1f", levelDiff / timeDiffHours).toFloat()
                }
            }
        }

        // Per-app battery breakdown approximated via foreground usage
        val usage = systemMonitor.getUsageTelemetry()
        val appUsageList = usage.topApps.map { detail ->
            AppPowerUsage(
                packageName = detail.packageName,
                appName = detail.appName,
                usageDurationMillis = detail.foregroundTimeMillis,
                percentageOfTotal = detail.percentageOfTotal
            )
        }

        base.copy(
            estimatedDrainRatePerHour = drainRatePerHour,
            appUsageList = appUsageList,
            historicalLevels = historicalLevels
        )
    }

    suspend fun getLiveNetwork(): NetworkTelemetry = withContext(Dispatchers.IO) {
        systemMonitor.getNetworkTelemetry()
    }

    suspend fun getLiveUsage(): UsageTelemetry = withContext(Dispatchers.IO) {
        systemMonitor.getUsageTelemetry()
    }

    suspend fun getLiveSystem(): SystemTelemetry = withContext(Dispatchers.IO) {
        val base = systemMonitor.getSystemTelemetry()
        val recentSnapshots = telemetryDao.getRecentSnapshots(20)
        val ramHistory = if (recentSnapshots.isNotEmpty()) {
            recentSnapshots.reversed().map { it.ramPercentage }
        } else {
            listOf(base.ramPercentage)
        }
        base.copy(ramHistory = ramHistory)
    }

    fun getLiveStorage(): StorageTelemetry {
        return systemMonitor.getStorageTelemetry()
    }

    suspend fun getLiveEvents(): List<DeviceEventItem> = withContext(Dispatchers.IO) {
        val entities = deviceEventDao.getRecentEvents(20)
        entities.map { entity ->
            DeviceEventItem(
                id = entity.id,
                timestamp = entity.timestamp,
                eventType = entity.eventType,
                description = entity.description,
                stepsCount = entity.stepsCount,
                motionState = systemMonitor.getMotionState()
            )
        }
    }

    suspend fun recordTelemetrySnapshot() = withContext(Dispatchers.IO) {
        val battery = systemMonitor.getBatteryTelemetry()
        val system = systemMonitor.getSystemTelemetry()
        val network = systemMonitor.getNetworkTelemetry()

        val entity = TelemetrySnapshotEntity(
            timestamp = System.currentTimeMillis(),
            batteryLevel = battery.level,
            batteryTemperature = battery.temperatureCelsius,
            isCharging = battery.isCharging,
            ramUsedBytes = system.usedRamBytes,
            ramPercentage = system.ramPercentage,
            cpuPercentage = system.cpuUsagePercentage,
            downloadSpeedBps = network.downloadSpeedBps,
            uploadSpeedBps = network.uploadSpeedBps
        )
        telemetryDao.insertSnapshot(entity)
    }

    suspend fun recordDeviceEvent(eventType: String, description: String) = withContext(Dispatchers.IO) {
        val entity = DeviceEventEntity(
            timestamp = System.currentTimeMillis(),
            eventType = eventType,
            description = description,
            stepsCount = systemMonitor.getStepCount()
        )
        deviceEventDao.insertEvent(entity)
    }

    suspend fun pruneOldLogs(daysToKeep: Int = 7) = withContext(Dispatchers.IO) {
        val cutoff = System.currentTimeMillis() - (daysToKeep * 24L * 60 * 60 * 1000)
        telemetryDao.deleteOlderThan(cutoff)
        deviceEventDao.deleteOlderThan(cutoff)
    }

    fun hasUsageStatsPermission(): Boolean {
        return systemMonitor.hasUsageStatsPermission()
    }

    suspend fun toggleTileExpansion(tileId: TileId) {
        layoutPreferences.toggleTileExpansion(tileId)
    }

    suspend fun setTileExpansion(tileId: TileId, expanded: Boolean) {
        layoutPreferences.setTileExpansion(tileId, expanded)
    }

    suspend fun reorderTiles(newOrder: List<TileId>) {
        layoutPreferences.saveTileOrder(newOrder)
    }

    suspend fun moveTile(fromIndex: Int, toIndex: Int, currentTiles: List<DashboardTile>) {
        layoutPreferences.moveTile(fromIndex, toIndex, currentTiles)
    }
}
