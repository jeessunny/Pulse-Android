package com.pulse.telemetry.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.pulse.telemetry.R
import com.pulse.telemetry.data.repository.TelemetryRepository
import com.pulse.telemetry.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TelemetryLoggingService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)
    private lateinit var repository: TelemetryRepository
    private lateinit var notificationManager: NotificationManager

    private val CHANNEL_ID = "pulse_telemetry_service_channel"
    private val NOTIFICATION_ID = 1001

    private val systemEventReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            serviceScope.launch {
                when (action) {
                    Intent.ACTION_SCREEN_ON -> {
                        repository.recordDeviceEvent("SCREEN_ON", "Display turned on")
                    }
                    Intent.ACTION_SCREEN_OFF -> {
                        repository.recordDeviceEvent("SCREEN_OFF", "Display turned off")
                    }
                    Intent.ACTION_POWER_CONNECTED -> {
                        repository.recordDeviceEvent("POWER_CONNECTED", "Charger connected")
                    }
                    Intent.ACTION_POWER_DISCONNECTED -> {
                        repository.recordDeviceEvent("POWER_DISCONNECTED", "Charger disconnected")
                    }
                    Intent.ACTION_BATTERY_LOW -> {
                        repository.recordDeviceEvent("BATTERY_LOW", "Battery dropped to low level")
                    }
                    ConnectivityManager.CONNECTIVITY_ACTION -> {
                        val net = repository.getLiveNetwork()
                        val desc = if (net.isConnected) "Network connected: ${net.connectionType}" else "Network disconnected"
                        repository.recordDeviceEvent("NETWORK_CHANGE", desc)
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        repository = TelemetryRepository(applicationContext)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_LOW)
            @Suppress("DEPRECATION")
            addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        }
        registerReceiver(systemEventReceiver, filter)

        startForegroundNotification()
        startPeriodicLogging()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.telemetry_notification_channel),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.telemetry_notification_desc)
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundNotification() {
        val notification = buildNotification("Monitoring active", "Collecting system metrics...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(title: String, content: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Pulse: $title")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun startPeriodicLogging() {
        serviceScope.launch {
            while (isActive) {
                runCatching {
                    repository.recordTelemetrySnapshot()
                    val battery = repository.getLiveBattery()
                    val sys = repository.getLiveSystem()
                    val net = repository.getLiveNetwork()

                    val speedKb = net.downloadSpeedBps / 1024
                    val statusText = "⚡ ${battery.level}% | RAM: ${sys.ramPercentage}% | 📶 ${speedKb} KB/s"
                    notificationManager.notify(NOTIFICATION_ID, buildNotification("Active Telemetry", statusText))
                }
                delay(30_000L) // Log snapshot every 30 seconds
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        runCatching { unregisterReceiver(systemEventReceiver) }
    }
}
