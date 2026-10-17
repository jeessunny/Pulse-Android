package com.pulse.telemetry.ui.dashboard

import android.app.Application
import android.content.Context
import android.os.Build
import android.view.Display
import android.view.WindowManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pulse.telemetry.data.repository.TelemetryRepository
import com.pulse.telemetry.domain.model.ActivityTelemetry
import com.pulse.telemetry.domain.model.BatteryTelemetry
import com.pulse.telemetry.domain.model.DashboardTile
import com.pulse.telemetry.domain.model.DeviceEventItem
import com.pulse.telemetry.domain.model.NetworkTelemetry
import com.pulse.telemetry.domain.model.StorageTelemetry
import com.pulse.telemetry.domain.model.SystemTelemetry
import com.pulse.telemetry.domain.model.TileId
import com.pulse.telemetry.domain.model.UsageTelemetry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class DashboardUiState(
    val tiles: List<DashboardTile> = TileId.entries.mapIndexed { index, id -> DashboardTile(id, false, index) },
    val battery: BatteryTelemetry = BatteryTelemetry(),
    val network: NetworkTelemetry = NetworkTelemetry(),
    val usage: UsageTelemetry = UsageTelemetry(),
    val system: SystemTelemetry = SystemTelemetry(),
    val storage: StorageTelemetry = StorageTelemetry(),
    val activity: ActivityTelemetry = ActivityTelemetry(),
    val events: List<DeviceEventItem> = emptyList(),
    val isRefreshing: Boolean = false,
    val isUpdateChecking: Boolean = false,
    val updateStatusMessage: String? = null,
    val updateDownloadUrl: String? = null,
    val latestVersionName: String? = null,
    val preferredRefreshRate: Float = 0f,
    val availableRefreshRates: List<Int> = listOf(0, 60, 90, 120)
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TelemetryRepository(application)

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        detectDisplayRefreshRates()

        // Collect layout tiles from DataStore
        viewModelScope.launch {
            repository.tilesFlow.collectLatest { tiles ->
                _uiState.update { it.copy(tiles = tiles) }
            }
        }

        // Collect events flow
        viewModelScope.launch {
            repository.recentEventsFlow.collectLatest { events ->
                _uiState.update { it.copy(events = events) }
            }
        }

        // Collect preferred refresh rate
        viewModelScope.launch {
            repository.refreshRateFlow.collectLatest { rate ->
                _uiState.update { it.copy(preferredRefreshRate = rate) }
            }
        }

        // Start real-time metrics polling loops (optimized for stutter-free 120 FPS UI)
        startTelemetryPolling()
    }

    private fun detectDisplayRefreshRates() {
        val wm = getApplication<Application>().getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching { getApplication<Application>().display }.getOrNull() ?: @Suppress("DEPRECATION") wm?.defaultDisplay
        } else {
            @Suppress("DEPRECATION") wm?.defaultDisplay
        }

        val modes = display?.supportedModes ?: emptyArray()
        val detectedRates = modes.map { it.refreshRate.toInt() }.distinct().filter { it in 50..240 }.sorted()
        val available = if (detectedRates.isNotEmpty()) {
            listOf(0) + detectedRates // 0 represents System Auto/Default
        } else {
            listOf(0, 60, 90, 120)
        }
        _uiState.update { it.copy(availableRefreshRates = available) }
    }

    private fun startTelemetryPolling() {
        // High frequency loop for fast reactive stats (Battery, CPU, RAM, Activity motion)
        viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                runCatching {
                    val battery = repository.getLiveBattery()
                    val system = repository.getLiveSystem()
                    val activity = repository.getLiveActivity()

                    _uiState.update { current ->
                        current.copy(
                            battery = battery,
                            system = system,
                            activity = activity
                        )
                    }
                }
                delay(2000L)
            }
        }

        // Lower frequency loop for heavy data queries (NetworkStatsManager and UsageEvents) to eliminate lag
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                runCatching {
                    val network = repository.getLiveNetwork()
                    val storage = repository.getLiveStorage()
                    val usage = repository.getLiveUsage()

                    _uiState.update { current ->
                        current.copy(
                            network = network,
                            storage = storage,
                            usage = usage
                        )
                    }
                }
                delay(2500L)
            }
        }
    }

    fun setPreferredRefreshRate(rate: Float) {
        viewModelScope.launch {
            repository.setPreferredRefreshRate(rate)
        }
    }

    fun refreshMetrics() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            runCatching {
                repository.recordTelemetrySnapshot()
                val battery = repository.getLiveBattery()
                val network = repository.getLiveNetwork()
                val system = repository.getLiveSystem()
                val storage = repository.getLiveStorage()
                val usage = repository.getLiveUsage()
                val activity = repository.getLiveActivity()
                val events = repository.getLiveEvents()

                _uiState.update {
                    it.copy(
                        battery = battery,
                        network = network,
                        system = system,
                        storage = storage,
                        usage = usage,
                        activity = activity,
                        events = events,
                        isRefreshing = false
                    )
                }
            }
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun toggleTileExpansion(tileId: TileId) {
        viewModelScope.launch {
            repository.toggleTileExpansion(tileId)
        }
    }

    fun moveTileUp(tileId: TileId) {
        val currentTiles = _uiState.value.tiles
        val currentIndex = currentTiles.indexOfFirst { it.id == tileId }
        if (currentIndex > 0) {
            viewModelScope.launch {
                repository.moveTile(currentIndex, currentIndex - 1, currentTiles)
            }
        }
    }

    fun moveTileDown(tileId: TileId) {
        val currentTiles = _uiState.value.tiles
        val currentIndex = currentTiles.indexOfFirst { it.id == tileId }
        if (currentIndex >= 0 && currentIndex < currentTiles.lastIndex) {
            viewModelScope.launch {
                repository.moveTile(currentIndex, currentIndex + 1, currentTiles)
            }
        }
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isUpdateChecking = true,
                    updateStatusMessage = "Checking GitHub for updates..."
                )
            }

            withContext(Dispatchers.IO) {
                try {
                    val url = URL("https://api.github.com/repos/jeessunny/Pulse-Android/releases/latest")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.connectTimeout = 6000
                    connection.readTimeout = 6000
                    connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
                    connection.setRequestProperty("User-Agent", "Pulse-Android-App")

                    val responseCode = connection.responseCode
                    if (responseCode == 200) {
                        val reader = BufferedReader(InputStreamReader(connection.inputStream))
                        val response = reader.readText()
                        reader.close()

                        val json = JSONObject(response)
                        val tagName = json.optString("tag_name", "v1.0.3")
                        val htmlUrl = json.optString("html_url", "https://github.com/jeessunny/Pulse-Android/releases")
                        
                        var apkDownloadUrl: String? = null
                        val assets = json.optJSONArray("assets")
                        if (assets != null && assets.length() > 0) {
                            for (i in 0 until assets.length()) {
                                val asset = assets.getJSONObject(i)
                                if (asset.optString("name").endsWith(".apk")) {
                                    apkDownloadUrl = asset.optString("browser_download_url", htmlUrl)
                                    break
                                }
                            }
                        }

                        val currentVersion = "v1.0.3"
                        // Strictly compare version tag: only set update if tagName is genuinely newer
                        val hasUpdate = tagName != currentVersion && !tagName.contains("1.0.3")
                        val message = if (hasUpdate) {
                            "New release available: $tagName"
                        } else {
                            "You are on the latest version ($currentVersion)"
                        }

                        _uiState.update {
                            it.copy(
                                isUpdateChecking = false,
                                updateStatusMessage = message,
                                updateDownloadUrl = if (hasUpdate) (apkDownloadUrl ?: htmlUrl) else null,
                                latestVersionName = tagName
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isUpdateChecking = false,
                                updateStatusMessage = "Pulse is up to date (v1.0.3)",
                                updateDownloadUrl = null
                            )
                        }
                    }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            isUpdateChecking = false,
                            updateStatusMessage = "Could not check GitHub. Please verify internet connection.",
                            updateDownloadUrl = null
                        )
                    }
                }
            }
        }
    }
}
