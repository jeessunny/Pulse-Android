package com.pulse.telemetry.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.pulse.telemetry.service.TelemetryLoggingService
import com.pulse.telemetry.ui.dashboard.DashboardScreen
import com.pulse.telemetry.ui.dashboard.DashboardViewModel
import com.pulse.telemetry.ui.theme.PulseTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: DashboardViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        startTelemetryService()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        requestRequiredPermissions()
        startTelemetryService()
        observeRefreshRatePreference()

        setContent {
            PulseTheme {
                DashboardScreen(viewModel = viewModel)
            }
        }
    }

    private fun observeRefreshRatePreference() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState
                    .map { it.preferredRefreshRate }
                    .distinctUntilChanged()
                    .collect { rate ->
                        applyRefreshRate(rate)
                    }
            }
        }
    }

    private fun applyRefreshRate(rate: Float) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.attributes = window.attributes.also { layoutParams ->
                    layoutParams.preferredRefreshRate = rate
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val display = @Suppress("DEPRECATION") windowManager.defaultDisplay
                val modes = display?.supportedModes ?: emptyArray()
                if (rate > 0f) {
                    val matchMode = modes.firstOrNull { kotlin.math.abs(it.refreshRate - rate) < 2f }
                    matchMode?.let { mode ->
                        window.attributes = window.attributes.also { layoutParams ->
                            layoutParams.preferredDisplayModeId = mode.modeId
                        }
                    }
                } else {
                    window.attributes = window.attributes.also { layoutParams ->
                        layoutParams.preferredDisplayModeId = 0
                    }
                }
            }
        }
    }

    private fun requestRequiredPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.ACTIVITY_RECOGNITION)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    private fun startTelemetryService() {
        runCatching {
            val serviceIntent = Intent(this, TelemetryLoggingService::class.java)
            ContextCompat.startForegroundService(this, serviceIntent)
        }
    }
}
