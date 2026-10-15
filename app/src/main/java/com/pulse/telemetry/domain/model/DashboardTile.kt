package com.pulse.telemetry.domain.model

enum class TileId(val defaultTitle: String) {
    BATTERY("Battery"),
    NETWORK("Network"),
    SCREEN_TIME("Screen Time"),
    SYSTEM("System"),
    STORAGE("Storage"),
    ACTIVITY("Activity"),
    EVENTS("Device Events");

    companion object {
        fun fromString(name: String): TileId {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: BATTERY
        }
    }
}

data class DashboardTile(
    val id: TileId,
    val isExpanded: Boolean = false,
    val order: Int = 0
)
