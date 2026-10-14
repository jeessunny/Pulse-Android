package com.pulse.telemetry.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MotionPhotosOn
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulse.telemetry.domain.model.TileId
import com.pulse.telemetry.ui.theme.MetricCyan
import com.pulse.telemetry.ui.theme.MetricGreen
import com.pulse.telemetry.ui.tiles.BatteryTile
import com.pulse.telemetry.ui.tiles.DeviceEventsTile
import com.pulse.telemetry.ui.tiles.NetworkTile
import com.pulse.telemetry.ui.tiles.ScreenTimeTile
import com.pulse.telemetry.ui.tiles.StorageTile
import com.pulse.telemetry.ui.tiles.SystemTaskTile

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(MetricGreen)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Pulse",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Live Device Telemetry",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.MotionPhotosOn,
                                contentDescription = null,
                                tint = MetricCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Logging Active",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = { viewModel.refreshMetrics() }) {
                        if (uiState.isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Refresh Telemetry",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(
                start = 12.dp,
                end = 12.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 24.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Header hint bar
            item(span = { GridItemSpan(2) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Double-tap or use resize toggle to switch between compact and detailed expanded modes.",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Dashboard Tiles
            itemsIndexed(
                items = uiState.tiles,
                key = { _, tile -> tile.id.name },
                span = { _, tile ->
                    GridItemSpan(if (tile.isExpanded) 2 else 1)
                }
            ) { index, tile ->
                val canMoveUp = index > 0
                val canMoveDown = index < uiState.tiles.lastIndex

                val onMoveUpAction = if (canMoveUp) { { viewModel.moveTileUp(tile.id) } } else null
                val onMoveDownAction = if (canMoveDown) { { viewModel.moveTileDown(tile.id) } } else null

                Box(modifier = Modifier.animateItemPlacement()) {
                    when (tile.id) {
                        TileId.BATTERY -> {
                            BatteryTile(
                                telemetry = uiState.battery,
                                isExpanded = tile.isExpanded,
                                onToggleExpand = { viewModel.toggleTileExpansion(TileId.BATTERY) },
                                onMoveUp = onMoveUpAction,
                                onMoveDown = onMoveDownAction
                            )
                        }
                        TileId.NETWORK -> {
                            NetworkTile(
                                telemetry = uiState.network,
                                isExpanded = tile.isExpanded,
                                onToggleExpand = { viewModel.toggleTileExpansion(TileId.NETWORK) },
                                onMoveUp = onMoveUpAction,
                                onMoveDown = onMoveDownAction
                            )
                        }
                        TileId.SCREEN_TIME -> {
                            ScreenTimeTile(
                                telemetry = uiState.usage,
                                isExpanded = tile.isExpanded,
                                onToggleExpand = { viewModel.toggleTileExpansion(TileId.SCREEN_TIME) },
                                onMoveUp = onMoveUpAction,
                                onMoveDown = onMoveDownAction
                            )
                        }
                        TileId.SYSTEM -> {
                            SystemTaskTile(
                                telemetry = uiState.system,
                                isExpanded = tile.isExpanded,
                                onToggleExpand = { viewModel.toggleTileExpansion(TileId.SYSTEM) },
                                onMoveUp = onMoveUpAction,
                                onMoveDown = onMoveDownAction
                            )
                        }
                        TileId.STORAGE -> {
                            StorageTile(
                                telemetry = uiState.storage,
                                isExpanded = tile.isExpanded,
                                onToggleExpand = { viewModel.toggleTileExpansion(TileId.STORAGE) },
                                onMoveUp = onMoveUpAction,
                                onMoveDown = onMoveDownAction
                            )
                        }
                        TileId.EVENTS -> {
                            DeviceEventsTile(
                                events = uiState.events,
                                isExpanded = tile.isExpanded,
                                onToggleExpand = { viewModel.toggleTileExpansion(TileId.EVENTS) },
                                onMoveUp = onMoveUpAction,
                                onMoveDown = onMoveDownAction
                            )
                        }
                    }
                }
            }
        }
    }
}
