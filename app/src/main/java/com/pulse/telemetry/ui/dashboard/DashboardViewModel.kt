package com.pulse.telemetry.ui.dashboard

import android.app.Application
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
    val latestVersionName: String? = null
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TelemetryRepository(application)

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
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

        // Start real-time metrics polling loop
        startTelemetryPolling()
    }

    private fun startTelemetryPolling() {
        viewModelScope.launch {
            while (isActive) {
                runCatching {
                    val battery = repository.getLiveBattery()
                    val network = repository.getLiveNetwork()
                    val system = repository.getLiveSystem()
                    val storage = repository.getLiveStorage()
                    val usage = repository.getLiveUsage()
                    val activity = repository.getLiveActivity()

                    _uiState.update { current ->
                        current.copy(
                            battery = battery,
                            network = network,
                            system = system,
                            storage = storage,
                            usage = usage,
                            activity = activity
                        )
                    }
                }
                delay(2000L) // Refresh live UI telemetry every 2 seconds
            }
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
                    updateStatusMessage = "Checking GitHub for releases..."
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
                        val tagName = json.optString("tag_name", "v1.0.0")
                        val htmlUrl = json.optString("html_url", "https://github.com/jeessunny/Pulse-Android/releases")
                        
                        var downloadUrl = htmlUrl
                        val assets = json.optJSONArray("assets")
                        if (assets != null && assets.length() > 0) {
                            for (i in 0 until assets.length()) {
                                val asset = assets.getJSONObject(i)
                                if (asset.optString("name").endsWith(".apk")) {
                                    downloadUrl = asset.optString("browser_download_url", htmlUrl)
                                    break
                                }
                            }
                        }

                        val currentVersion = "v1.0.1"
                        val hasUpdate = tagName != currentVersion && !tagName.contains("1.0.1")
                        val message = if (hasUpdate) {
                            "New release available: $tagName"
                        } else {
                            "You have the latest version ($currentVersion)"
                        }

                        _uiState.update {
                            it.copy(
                                isUpdateChecking = false,
                                updateStatusMessage = message,
                                updateDownloadUrl = downloadUrl,
                                latestVersionName = tagName
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isUpdateChecking = false,
                                updateStatusMessage = "Pulse is up to date (v1.0.0)"
                            )
                        }
                    }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            isUpdateChecking = false,
                            updateStatusMessage = "Could not check GitHub. Please verify internet connection."
                        )
                    }
                }
            }
        }
    }
}
