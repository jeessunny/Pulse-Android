package com.pulse.telemetry.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pulse.telemetry.domain.model.DashboardTile
import com.pulse.telemetry.domain.model.TileId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pulse_layout_prefs")

class LayoutPreferences(private val context: Context) {

    private val KEY_TILE_ORDER = stringPreferencesKey("key_tile_order")
    private val KEY_REFRESH_RATE = androidx.datastore.preferences.core.floatPreferencesKey("key_preferred_refresh_rate")

    val refreshRateFlow: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[KEY_REFRESH_RATE] ?: 0f
    }

    val tilesFlow: Flow<List<DashboardTile>> = context.dataStore.data.map { preferences ->
        val rawOrder = preferences[KEY_TILE_ORDER]
        val orderedIds = if (!rawOrder.isNullOrBlank()) {
            rawOrder.split(",")
                .mapNotNull { name -> runCatching { TileId.valueOf(name) }.getOrNull() }
                .let { parsed ->
                    val missing = TileId.entries.filter { it !in parsed }
                    parsed + missing
                }
        } else {
            TileId.entries
        }

        orderedIds.mapIndexed { index, tileId ->
            val isExpandedKey = booleanPreferencesKey("tile_expanded_${tileId.name}")
            val isExpanded = preferences[isExpandedKey] ?: false
            DashboardTile(
                id = tileId,
                isExpanded = isExpanded,
                order = index
            )
        }
    }

    suspend fun saveTileOrder(newOrder: List<TileId>) {
        context.dataStore.edit { preferences ->
            preferences[KEY_TILE_ORDER] = newOrder.joinToString(",") { it.name }
        }
    }

    suspend fun toggleTileExpansion(tileId: TileId) {
        val key = booleanPreferencesKey("tile_expanded_${tileId.name}")
        context.dataStore.edit { preferences ->
            val current = preferences[key] ?: false
            preferences[key] = !current
        }
    }

    suspend fun setTileExpansion(tileId: TileId, expanded: Boolean) {
        val key = booleanPreferencesKey("tile_expanded_${tileId.name}")
        context.dataStore.edit { preferences ->
            preferences[key] = expanded
        }
    }

    suspend fun moveTile(fromIndex: Int, toIndex: Int, currentTiles: List<DashboardTile>) {
        if (fromIndex !in currentTiles.indices || toIndex !in currentTiles.indices) return
        val mutable = currentTiles.map { it.id }.toMutableList()
        val item = mutable.removeAt(fromIndex)
        mutable.add(toIndex, item)
        saveTileOrder(mutable)
    }

    suspend fun setPreferredRefreshRate(rate: Float) {
        context.dataStore.edit { preferences ->
            preferences[KEY_REFRESH_RATE] = rate
        }
    }
}
