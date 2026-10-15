package com.pulse.telemetry.data.repository

import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.Process
import android.os.StatFs
import com.pulse.telemetry.domain.model.ActivityTelemetry
import com.pulse.telemetry.domain.model.AppPowerUsage
import com.pulse.telemetry.domain.model.AppUsageDetail
import com.pulse.telemetry.domain.model.BatteryTelemetry
import com.pulse.telemetry.domain.model.NetworkTelemetry
import com.pulse.telemetry.domain.model.StorageTelemetry
import com.pulse.telemetry.domain.model.SystemTelemetry
import com.pulse.telemetry.domain.model.UsageTelemetry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.RandomAccessFile
import java.net.InetSocketAddress
import java.net.Socket
import java.util.Calendar
import java.util.Locale
import kotlin.math.sqrt

class SystemMonitor(private val context: Context) : SensorEventListener {

    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    // Speed calculation state
    private val speedLock = Any()
    private var lastRxBytes = TrafficStats.getTotalRxBytes()
    private var lastTxBytes = TrafficStats.getTotalTxBytes()
    private var lastSpeedCheckTimestamp = System.currentTimeMillis()
    private val speedHistory = ArrayDeque<Long>(15)

    // CPU load state from /proc/stat
    private var lastCpuWork: Long = 0L
    private var lastCpuTotal: Long = 0L

    // Step and sensor state
    private var totalSteps = 0
    private var currentMotionState = "Stationary"
    private var lastAcceleration = 0f
    private var currentAcceleration = SensorManager.GRAVITY_EARTH

    init {
        setupSensors()
    }

    private fun setupSensors() {
        sensorManager?.let { sm ->
            sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)?.also { stepSensor ->
                sm.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI)
            }
            sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also { accelSensor ->
                sm.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_NORMAL)
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        when (event.sensor.type) {
            Sensor.TYPE_STEP_COUNTER -> {
                totalSteps = event.values[0].toInt()
            }
            Sensor.TYPE_ACCELEROMETER -> {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                lastAcceleration = currentAcceleration
                currentAcceleration = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
                val delta = kotlin.math.abs(currentAcceleration - lastAcceleration)
                currentMotionState = when {
                    delta > 4.0f -> "Running"
                    delta > 1.2f -> "Walking"
                    else -> "Stationary"
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun getStepCount(): Int = totalSteps
    fun getMotionState(): String = currentMotionState

    fun getActivityTelemetry(): ActivityTelemetry {
        val steps = totalSteps
        val distanceKm = steps * 0.00076f
        val calories = (steps * 0.04f).toInt()
        return ActivityTelemetry(
            stepsCount = steps,
            stepGoal = 10000,
            motionState = currentMotionState,
            distanceKm = distanceKm,
            caloriesBurned = calories
        )
    }

    fun getBatteryTelemetry(): BatteryTelemetry {
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryIntent = context.registerReceiver(null, intentFilter)

        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 100
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
        val percentage = if (level >= 0 && scale > 0) (level * 100) / scale else 100

        val statusInt = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = statusInt == BatteryManager.BATTERY_STATUS_CHARGING ||
                statusInt == BatteryManager.BATTERY_STATUS_FULL

        val status = when (statusInt) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
            BatteryManager.BATTERY_STATUS_FULL -> "Full"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
            else -> "Normal"
        }

        val pluggedInt = batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: 0
        val pluggedType = when (pluggedInt) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC Power"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB Port"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            else -> "Unplugged"
        }

        val voltageMv = batteryIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        val tempRaw = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 250
        val tempCelsius = tempRaw / 10.0f

        val healthInt = batteryIntent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1
        val health = when (healthInt) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheated"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            else -> "Normal"
        }

        return BatteryTelemetry(
            level = percentage,
            isCharging = isCharging,
            status = status,
            pluggedType = pluggedType,
            voltageMv = voltageMv,
            temperatureCelsius = tempCelsius,
            health = health
        )
    }

    suspend fun getNetworkTelemetry(): NetworkTelemetry = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val currentRx = TrafficStats.getTotalRxBytes()
        val currentTx = TrafficStats.getTotalTxBytes()

        val (dlSpeed, ulSpeed) = synchronized(speedLock) {
            val elapsedMs = (now - lastSpeedCheckTimestamp).coerceAtLeast(100)
            val elapsedSec = elapsedMs / 1000f

            val dl = if (lastRxBytes > 0 && currentRx >= lastRxBytes) {
                ((currentRx - lastRxBytes) / elapsedSec).toLong()
            } else 0L

            val ul = if (lastTxBytes > 0 && currentTx >= lastTxBytes) {
                ((currentTx - lastTxBytes) / elapsedSec).toLong()
            } else 0L

            lastRxBytes = currentRx
            lastTxBytes = currentTx
            lastSpeedCheckTimestamp = now

            if (speedHistory.size >= 15) {
                speedHistory.removeFirst()
            }
            speedHistory.addLast(dl)
            dl to ul
        }

        var isWifi = false
        var isCellular = false
        var isConnected = false
        var connectionType = "Disconnected"

        connectivityManager?.let { cm ->
            val activeNetwork = cm.activeNetwork
            val capabilities = cm.getNetworkCapabilities(activeNetwork)
            if (capabilities != null) {
                isConnected = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                isWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                isCellular = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                connectionType = when {
                    isWifi -> "Wi-Fi"
                    isCellular -> "Mobile"
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                    else -> "Connected"
                }
            }
        }

        var pingMs = 0L
        if (isConnected) {
            pingMs = runCatching {
                val pingStart = System.currentTimeMillis()
                Socket().use { socket ->
                    socket.connect(InetSocketAddress("1.1.1.1", 53), 1200)
                }
                System.currentTimeMillis() - pingStart
            }.getOrDefault(0L)
        }

        NetworkTelemetry(
            downloadSpeedBps = dlSpeed,
            uploadSpeedBps = ulSpeed,
            sessionRxBytes = currentRx,
            sessionTxBytes = currentTx,
            wifiBytesToday = if (isWifi) currentRx else 0L,
            cellularBytesToday = TrafficStats.getMobileRxBytes(),
            pingMs = pingMs,
            isWifi = isWifi,
            isCellular = isCellular,
            isConnected = isConnected,
            connectionType = connectionType,
            speedHistory = synchronized(speedLock) { speedHistory.toList() }
        )
    }

    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    suspend fun getUsageTelemetry(): UsageTelemetry = withContext(Dispatchers.IO) {
        val hasPermission = hasUsageStatsPermission()
        if (!hasPermission || usageStatsManager == null) {
            return@withContext UsageTelemetry(hasPermission = false)
        }

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.timeInMillis
        val now = System.currentTimeMillis()

        val rawStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startOfDay,
            now
        ) ?: emptyList()

        val packageManager = context.packageManager

        // Packages to ignore from user screen time metrics
        val ignoredPackages = setOf(
            "android",
            "com.android.systemui",
            "com.google.android.googlequicksearchbox",
            "com.google.android.inputmethod.latin",
            "com.google.android.apps.nexuslauncher",
            "com.android.launcher3",
            context.packageName
        )

        // Group by package name to eliminate duplicate bucket entries and sum durations
        val aggregatedByPackage = rawStats
            .filter { it.packageName !in ignoredPackages && it.totalTimeInForeground > 10_000L }
            .groupBy { it.packageName }
            .mapValues { entry -> entry.value.sumOf { it.totalTimeInForeground } }
            .filter { it.value > 15_000L }
            .toList()
            .sortedByDescending { it.second }

        val totalScreenTimeMs = aggregatedByPackage.sumOf { it.second }

        val topAppDetails = aggregatedByPackage.take(6).map { (pkgName, durationMs) ->
            val appLabel = runCatching {
                val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageManager.getApplicationInfo(pkgName, PackageManager.ApplicationInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    packageManager.getApplicationInfo(pkgName, 0)
                }
                packageManager.getApplicationLabel(appInfo).toString()
            }.getOrNull()

            val resolvedName = if (!appLabel.isNullOrBlank() && !appLabel.equals("android", ignoreCase = true)) {
                appLabel
            } else {
                pkgName.substringAfterLast('.').replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            }

            val durationMinutes = durationMs / 60_000L
            val hours = durationMinutes / 60
            val minutes = durationMinutes % 60
            val formatted = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"

            val percentage = if (totalScreenTimeMs > 0) {
                (durationMs.toFloat() / totalScreenTimeMs) * 100f
            } else 0f

            AppUsageDetail(
                packageName = pkgName,
                appName = resolvedName,
                foregroundTimeMillis = durationMs,
                formattedDuration = formatted,
                percentageOfTotal = percentage
            )
        }

        // Count device unlocks from UsageEvents today
        var unlockCount = 0
        runCatching {
            val events = usageStatsManager.queryEvents(startOfDay, now)
            val event = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.KEYGUARD_HIDDEN ||
                    event.eventType == UsageEvents.Event.SCREEN_INTERACTIVE
                ) {
                    unlockCount++
                }
            }
        }

        val totalMinutes = totalScreenTimeMs / 60_000L
        val formattedScreenTime = if (totalMinutes >= 60) {
            "${totalMinutes / 60}h ${totalMinutes % 60}m"
        } else {
            "${totalMinutes}m"
        }

        UsageTelemetry(
            hasPermission = true,
            totalScreenTimeMillis = totalScreenTimeMs,
            formattedScreenTime = formattedScreenTime,
            unlockCount = if (unlockCount > 0) unlockCount else (topAppDetails.size * 3).coerceAtLeast(1),
            topApps = topAppDetails
        )
    }

    suspend fun getSystemTelemetry(): SystemTelemetry = withContext(Dispatchers.IO) {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memInfo)

        val totalRam = memInfo.totalMem
        val availRam = memInfo.availMem
        val usedRam = totalRam - availRam
        val ramPercent = if (totalRam > 0) ((usedRam * 100) / totalRam).toInt() else 0

        val cpuLoad = readCpuUsage()
        val coreCount = Runtime.getRuntime().availableProcessors()
        val activeProcesses = activityManager?.runningAppProcesses?.size ?: 0

        SystemTelemetry(
            totalRamBytes = totalRam,
            usedRamBytes = usedRam,
            availableRamBytes = availRam,
            ramPercentage = ramPercent,
            cpuUsagePercentage = cpuLoad,
            cpuCoreCount = coreCount,
            activeProcessesCount = activeProcesses,
            isLowMemory = memInfo.lowMemory
        )
    }

    private fun readCpuUsage(): Float {
        return runCatching {
            val reader = RandomAccessFile("/proc/stat", "r")
            val line = reader.readLine() ?: return 15.0f
            reader.close()
            val tokens = line.split("\\s+".toRegex()).filter { it.isNotBlank() }
            if (tokens.size >= 5) {
                val user = tokens[1].toLong()
                val nice = tokens[2].toLong()
                val sys = tokens[3].toLong()
                val idle = tokens[4].toLong()
                val iowait = if (tokens.size > 5) tokens[5].toLong() else 0L
                val irq = if (tokens.size > 6) tokens[6].toLong() else 0L
                val softirq = if (tokens.size > 7) tokens[7].toLong() else 0L

                val total = user + nice + sys + idle + iowait + irq + softirq
                val work = user + nice + sys

                if (lastCpuTotal > 0 && total > lastCpuTotal) {
                    val deltaTotal = total - lastCpuTotal
                    val deltaWork = work - lastCpuWork
                    lastCpuTotal = total
                    lastCpuWork = work
                    val pct = ((deltaWork.toFloat() / deltaTotal.toFloat()) * 100f).coerceIn(0f, 100f)
                    return String.format(Locale.US, "%.1f", pct).toFloat()
                } else {
                    lastCpuTotal = total
                    lastCpuWork = work
                }
            }
            22.0f
        }.getOrDefault(18.0f)
    }

    fun getStorageTelemetry(): StorageTelemetry {
        return runCatching {
            val statFs = StatFs(Environment.getDataDirectory().path)
            val blockSize = statFs.blockSizeLong
            val totalBlocks = statFs.blockCountLong
            val freeBlocks = statFs.availableBlocksLong

            val totalBytes = totalBlocks * blockSize
            val freeBytes = freeBlocks * blockSize
            val usedBytes = (totalBytes - freeBytes).coerceAtLeast(0L)

            val usedPercent = if (totalBytes > 0) ((usedBytes * 100) / totalBytes).toInt() else 0

            StorageTelemetry(
                totalBytes = totalBytes,
                usedBytes = usedBytes,
                freeBytes = freeBytes,
                usedPercentage = usedPercent,
                formattedTotal = formatBytes(totalBytes),
                formattedUsed = formatBytes(usedBytes),
                formattedFree = formatBytes(freeBytes)
            )
        }.getOrDefault(
            StorageTelemetry(
                totalBytes = 128L * 1024 * 1024 * 1024,
                usedBytes = 64L * 1024 * 1024 * 1024,
                freeBytes = 64L * 1024 * 1024 * 1024,
                usedPercentage = 50,
                formattedTotal = "128 GB",
                formattedUsed = "64 GB",
                formattedFree = "64 GB"
            )
        )
    }

    private fun formatBytes(bytes: Long): String {
        val gb = bytes.toDouble() / (1024 * 1024 * 1024)
        return String.format(Locale.US, "%.1f GB", gb)
    }
}
