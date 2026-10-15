package com.pulse.telemetry.ui.dashboard

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulse.telemetry.domain.model.TileId
import com.pulse.telemetry.ui.theme.MetricCyan
import com.pulse.telemetry.ui.theme.MetricGreen
import com.pulse.telemetry.ui.tiles.ActivityTile
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
    val context = LocalContext.current
    var showSettingsSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Pulse",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 21.sp,
                                letterSpacing = (-0.5).sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Modern minimal active indicator
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(top = 1.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Sensors,
                                    contentDescription = null,
                                    tint = MetricCyan,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Active",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showSettingsSheet = true }) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
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
                top = innerPadding.calculateTopPadding() + 4.dp,
                bottom = innerPadding.calculateBottomPadding() + 20.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Dashboard Tiles (Pure symmetry: exact square 1:1 in compact mode, 2-column full width in expanded mode)
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

                val tileModifier = if (tile.isExpanded) {
                    Modifier
                        .fillMaxWidth()
                        .animateItemPlacement()
                } else {
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .animateItemPlacement()
                }

                when (tile.id) {
                    TileId.BATTERY -> {
                        BatteryTile(
                            telemetry = uiState.battery,
                            isExpanded = tile.isExpanded,
                            onToggleExpand = { viewModel.toggleTileExpansion(TileId.BATTERY) },
                            onMoveUp = onMoveUpAction,
                            onMoveDown = onMoveDownAction,
                            modifier = tileModifier
                        )
                    }
                    TileId.NETWORK -> {
                        NetworkTile(
                            telemetry = uiState.network,
                            isExpanded = tile.isExpanded,
                            onToggleExpand = { viewModel.toggleTileExpansion(TileId.NETWORK) },
                            onMoveUp = onMoveUpAction,
                            onMoveDown = onMoveDownAction,
                            modifier = tileModifier
                        )
                    }
                    TileId.SCREEN_TIME -> {
                        ScreenTimeTile(
                            telemetry = uiState.usage,
                            isExpanded = tile.isExpanded,
                            onToggleExpand = { viewModel.toggleTileExpansion(TileId.SCREEN_TIME) },
                            onMoveUp = onMoveUpAction,
                            onMoveDown = onMoveDownAction,
                            modifier = tileModifier
                        )
                    }
                    TileId.SYSTEM -> {
                        SystemTaskTile(
                            telemetry = uiState.system,
                            isExpanded = tile.isExpanded,
                            onToggleExpand = { viewModel.toggleTileExpansion(TileId.SYSTEM) },
                            onMoveUp = onMoveUpAction,
                            onMoveDown = onMoveDownAction,
                            modifier = tileModifier
                        )
                    }
                    TileId.STORAGE -> {
                        StorageTile(
                            telemetry = uiState.storage,
                            isExpanded = tile.isExpanded,
                            onToggleExpand = { viewModel.toggleTileExpansion(TileId.STORAGE) },
                            onMoveUp = onMoveUpAction,
                            onMoveDown = onMoveDownAction,
                            modifier = tileModifier
                        )
                    }
                    TileId.ACTIVITY -> {
                        ActivityTile(
                            telemetry = uiState.activity,
                            isExpanded = tile.isExpanded,
                            onToggleExpand = { viewModel.toggleTileExpansion(TileId.ACTIVITY) },
                            onMoveUp = onMoveUpAction,
                            onMoveDown = onMoveDownAction,
                            modifier = tileModifier
                        )
                    }
                    TileId.EVENTS -> {
                        DeviceEventsTile(
                            events = uiState.events,
                            isExpanded = tile.isExpanded,
                            onToggleExpand = { viewModel.toggleTileExpansion(TileId.EVENTS) },
                            onMoveUp = onMoveUpAction,
                            onMoveDown = onMoveDownAction,
                            modifier = tileModifier
                        )
                    }
                }
            }
        }
    }

    // Settings Bottom Sheet
    if (showSettingsSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Pulse Settings",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(18.dp))

                // App Update Card
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.SystemUpdate,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Version & Updates",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = "Current: v1.0.1",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (uiState.isUpdateChecking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        if (!uiState.updateStatusMessage.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = uiState.updateStatusMessage ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilledTonalButton(
                                onClick = { viewModel.checkForUpdates() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Check for Updates")
                            }

                            if (!uiState.updateDownloadUrl.isNullOrBlank()) {
                                Button(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uiState.updateDownloadUrl))
                                        context.startActivity(intent)
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Download,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Download")
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Telemetry Sync Card
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Telemetry Sync",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "Foreground monitoring active",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.refreshMetrics()
                                showSettingsSheet = false
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sync")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // GitHub Source Link
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "GitHub Repository",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "jeessunny/Pulse-Android",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/jeessunny/Pulse-Android"))
                                context.startActivity(intent)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.OpenInNew,
                                contentDescription = "Open GitHub",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
