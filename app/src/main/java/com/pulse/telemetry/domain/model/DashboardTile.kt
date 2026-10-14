package com.pulse.telemetry.domain.model

enum class TileId(val defaultTitle: String) {
    BATTERY("Battery & Power"),
    NETWORK("Network & Traffic"),
    SCREEN_TIME("Digital Wellbeing"),
    SYSTEM("System & Task Manager"),
    STORAGE("Storage Overview"),
    EVENTS("Activity & Device Events");

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
