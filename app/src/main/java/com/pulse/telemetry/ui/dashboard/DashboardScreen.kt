package com.pulse.telemetry.ui.dashboard

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import com.pulse.telemetry.data.updater.UpdateState
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
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
                bottom = innerPadding.calculateBottomPadding() + 28.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
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
                        .animateItemPlacement(
                            animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                        )
                } else {
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .animateItemPlacement(
                            animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                        )
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
        val coroutineScope = rememberCoroutineScope()

        fun hideAndDismiss() {
            coroutineScope.launch {
                sheetState.hide()
            }.invokeOnCompletion {
                if (!sheetState.isVisible) {
                    showSettingsSheet = false
                }
            }
        }

        ModalBottomSheet(
            onDismissRequest = { hideAndDismiss() },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            windowInsets = WindowInsets(0, 0, 0, 0)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pulse Settings",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = { hideAndDismiss() }) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Refresh Rate Setting Card
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Speed,
                                contentDescription = null,
                                tint = MetricCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Display Refresh Rate",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = "Optimize smoothness for 90Hz / 120Hz screens",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            uiState.availableRefreshRates.forEach { rateInt ->
                                val isSelected = if (rateInt == 0) {
                                    uiState.preferredRefreshRate == 0f
                                } else {
                                    kotlin.math.abs(uiState.preferredRefreshRate - rateInt) < 2f
                                }

                                val label = if (rateInt == 0) "Auto" else "$rateInt Hz"

                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        viewModel.setPreferredRefreshRate(rateInt.toFloat())
                                    },
                                    label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }

                            // Max Refresh Rate quick button
                            val maxRate = uiState.availableRefreshRates.filter { it > 0 }.maxOrNull() ?: 120
                            val isMaxSelected = kotlin.math.abs(uiState.preferredRefreshRate - maxRate) < 2f
                            FilterChip(
                                selected = isMaxSelected,
                                onClick = {
                                    viewModel.setPreferredRefreshRate(maxRate.toFloat())
                                },
                                label = { Text("Max ($maxRate Hz)", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MetricCyan.copy(alpha = 0.25f),
                                    selectedLabelColor = MaterialTheme.colorScheme.onSurface
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // App Update Card
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
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
                                        text = "Current: v1.0.4",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (uiState.updateState is UpdateState.Checking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        when (val state = uiState.updateState) {
                            is UpdateState.Idle -> {
                                Spacer(modifier = Modifier.height(12.dp))
                                FilledTonalButton(
                                    onClick = { viewModel.checkForUpdates() },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Check for Updates")
                                }
                            }
                            is UpdateState.Checking -> {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Checking GitHub for updates...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            is UpdateState.UpToDate -> {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Pulse is up to date (${state.currentVersion})",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                FilledTonalButton(
                                    onClick = { viewModel.checkForUpdates() },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Check Again")
                                }
                            }
                            is UpdateState.Available -> {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "New release available: ${state.release.tagName}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (state.release.assetSize > 0L) {
                                    val sizeMb = String.format(java.util.Locale.US, "%.1f MB", state.release.assetSize / (1024f * 1024f))
                                    Text(
                                        text = "Package size: $sizeMb",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (state.release.releaseNotes.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = state.release.releaseNotes.take(160) + if (state.release.releaseNotes.length > 160) "..." else "",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { viewModel.startDownload() },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Download,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Download & Install Update")
                                }
                            }
                            is UpdateState.Downloading -> {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Downloading ${state.release.tagName}...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (state.progress >= 0f) {
                                        Text(
                                            text = "${(state.progress * 100).toInt()}%",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                if (state.progress >= 0f) {
                                    LinearProgressIndicator(
                                        progress = { state.progress },
                                        modifier = Modifier.fillMaxWidth().height(6.dp),
                                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                } else {
                                    LinearProgressIndicator(
                                        modifier = Modifier.fillMaxWidth().height(6.dp),
                                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                val downloadedMb = String.format(java.util.Locale.US, "%.1f", state.downloadedBytes / (1024f * 1024f))
                                val totalMb = if (state.totalBytes > 0) String.format(java.util.Locale.US, "%.1f MB", state.totalBytes / (1024f * 1024f)) else "-- MB"
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "$downloadedMb / $totalMb",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    OutlinedButton(
                                        onClick = { viewModel.cancelDownload() },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("Cancel", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                            is UpdateState.ReadyToInstall -> {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Update downloaded: ${state.release.tagName}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (state.needsPermission) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Please allow Pulse to install updates in Android Settings.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = { viewModel.requestInstallPermission(context) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Grant Install Permission")
                                    }
                                } else {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = { viewModel.installDownloadedUpdate(context) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.SystemUpdate,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Install Now")
                                    }
                                }
                            }
                            is UpdateState.Error -> {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                FilledTonalButton(
                                    onClick = { viewModel.checkForUpdates() },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Retry")
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Telemetry Sync Card
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
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
                                hideAndDismiss()
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
                    color = MaterialTheme.colorScheme.surfaceVariant,
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
