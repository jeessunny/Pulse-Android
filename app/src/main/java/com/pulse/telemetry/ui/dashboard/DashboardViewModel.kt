package com.pulse.telemetry.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pulse.telemetry.data.repository.TelemetryRepository
import com.pulse.telemetry.domain.model.BatteryTelemetry
import com.pulse.telemetry.domain.model.DashboardTile
import com.pulse.telemetry.domain.model.DeviceEventItem
import com.pulse.telemetry.domain.model.NetworkTelemetry
import com.pulse.telemetry.domain.model.StorageTelemetry
import com.pulse.telemetry.domain.model.SystemTelemetry
import com.pulse.telemetry.domain.model.TileId
import com.pulse.telemetry.domain.model.UsageTelemetry
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class DashboardUiState(
    val tiles: List<DashboardTile> = TileId.entries.mapIndexed { index, id -> DashboardTile(id, false, index) },
    val battery: BatteryTelemetry = BatteryTelemetry(),
    val network: NetworkTelemetry = NetworkTelemetry(),
    val usage: UsageTelemetry = UsageTelemetry(),
    val system: SystemTelemetry = SystemTelemetry(),
    val storage: StorageTelemetry = StorageTelemetry(),
    val events: List<DeviceEventItem> = emptyList(),
    val isRefreshing: Boolean = false
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

                    _uiState.update { current ->
                        current.copy(
                            battery = battery,
                            network = network,
                            system = system,
                            storage = storage,
                            usage = usage
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
                val events = repository.getLiveEvents()

                _uiState.update {
                    it.copy(
                        battery = battery,
                        network = network,
                        system = system,
                        storage = storage,
                        usage = usage,
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
}
