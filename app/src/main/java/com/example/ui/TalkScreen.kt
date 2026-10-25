package com.example.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AudioWaveformVisualizer
import com.example.ui.components.ChannelSwitchDialog
import com.example.ui.components.ConnectedUserItem
import com.example.ui.components.DiagnosticsDialog
import com.example.ui.components.DiscoveredPeerItem
import com.example.ui.components.IncomingConnectionDialog
import com.example.ui.components.ProfileDialog
import com.example.ui.components.PttButton
import com.example.ui.components.RecipientControlBar
import com.example.ui.components.SavedPeerItem
import com.example.ui.theme.RadioAmber
import com.example.ui.theme.RadioBackground
import com.example.ui.theme.RadioBorder
import com.example.ui.theme.RadioCyan
import com.example.ui.theme.RadioGreen
import com.example.ui.theme.RadioRed
import com.example.ui.theme.RadioSurface
import com.example.ui.theme.RadioSurfaceElevated
import com.example.ui.theme.RadioSurfaceVariant
import com.example.ui.theme.RadioTextPrimary
import com.example.ui.theme.RadioTextSecondary
import com.example.ui.theme.RadioTextTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TalkScreen(
    viewModel: TalkViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Dialog States
    var showChannelDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showDiagnosticsDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    // Permissions resolution
    val requiredPermissions = remember {
        val list = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            list.add(Manifest.permission.BLUETOOTH_SCAN)
            list.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            list.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
        list.toTypedArray()
    }

    var hasAllPermissions by remember {
        mutableStateOf(
            requiredPermissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasAllPermissions = requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (hasAllPermissions) {
            viewModel.startNearbyServices()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasAllPermissions) {
            permissionLauncher.launch(requiredPermissions)
        }
    }

    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
        hasAllPermissions = requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        onPauseOrDispose {}
    }

    LaunchedEffect(hasAllPermissions) {
        if (hasAllPermissions && !uiState.isDiscovering && !uiState.isAdvertising && uiState.radioError == null) {
            viewModel.startNearbyServices()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(RadioBackground),
        containerColor = RadioBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Tactical Top Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { showChannelDialog = true }
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(RadioSurfaceVariant)
                            .border(1.dp, RadioCyan.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Podcasts,
                            contentDescription = null,
                            tint = if (uiState.isAdvertising) RadioCyan else RadioTextTertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "NEARBY TALK",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.2.sp
                                ),
                                color = RadioTextPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            // Channel Chip
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(RadioSurfaceElevated)
                                    .border(1.dp, RadioCyan.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "CH: ${uiState.userSettings.activeChannel.uppercase()}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = RadioCyan
                                )
                            }
                        }
                        Text(
                            text = "${uiState.userSettings.displayName} (${uiState.userSettings.deviceId})",
                            style = MaterialTheme.typography.bodySmall,
                            color = RadioTextSecondary
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = { showChannelDialog = true },
                        modifier = Modifier.testTag("channel_switch_btn")
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = "Channels", tint = RadioCyan)
                    }
                    IconButton(
                        onClick = { showProfileDialog = true },
                        modifier = Modifier.testTag("profile_btn")
                    ) {
                        Icon(Icons.Default.ManageAccounts, contentDescription = "Profile", tint = RadioTextSecondary)
                    }
                    IconButton(
                        onClick = { showDiagnosticsDialog = true },
                        modifier = Modifier.testTag("diagnostics_btn")
                    ) {
                        Icon(Icons.Default.Info, contentDescription = "Diagnostics", tint = RadioTextTertiary)
                    }
                }
            }

            // Permissions Alert Banner (if needed)
            if (!hasAllPermissions) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = RadioAmber.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RadioAmber)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = RadioAmber)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Permissions Required",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = RadioTextPrimary
                            )
                            Text(
                                text = "Microphone, Location & Nearby Devices are needed for peer walkie-talkie operation.",
                                style = MaterialTheme.typography.bodySmall,
                                color = RadioTextSecondary
                            )
                        }
                        Button(
                            onClick = { permissionLauncher.launch(requiredPermissions) },
                            colors = ButtonDefaults.buttonColors(containerColor = RadioAmber, contentColor = Color.Black)
                        ) {
                            Text("Grant", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Radio Hardware Warning Banner (Status 8007)
            if (uiState.radioError != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = RadioAmber.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RadioAmber)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = RadioAmber)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Radio State Warning (8007)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = RadioTextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = uiState.radioError ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = RadioTextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    try {
                                        context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                                    } catch (_: Exception) {
                                        try {
                                            context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
                                        } catch (_: Exception) {}
                                    }
                                }
                            ) {
                                Text("Open Settings", style = MaterialTheme.typography.labelSmall)
                            }
                            Button(
                                onClick = { viewModel.startNearbyServices() },
                                colors = ButtonDefaults.buttonColors(containerColor = RadioCyan, contentColor = Color.Black)
                            ) {
                                Text("Retry Scan", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Tactical Scanning & Transmission Status Bar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = RadioSurface),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, RadioBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        uiState.isTransmitting -> RadioRed
                                        uiState.isReceiving -> RadioGreen
                                        uiState.activeSpeakerName != null -> RadioAmber
                                        uiState.isDiscovering -> RadioCyan
                                        else -> RadioTextTertiary
                                    }
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                uiState.isTransmitting -> "TRANSMITTING LIVE"
                                uiState.isReceiving -> "RECEIVING: ${uiState.activeSpeakerName?.uppercase()}"
                                uiState.activeSpeakerName != null -> "FLOOR BUSY: ${uiState.activeSpeakerName}"
                                uiState.isDiscovering || uiState.isAdvertising -> "DISCOVERY ACTIVE • ${uiState.connectedPeers.size} PEERS LINKED"
                                else -> "DISCOVERY PAUSED"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            ),
                            color = when {
                                uiState.isTransmitting -> RadioRed
                                uiState.isReceiving -> RadioGreen
                                uiState.activeSpeakerName != null -> RadioAmber
                                uiState.isDiscovering -> RadioCyan
                                else -> RadioTextSecondary
                            }
                        )
                    }

                    // Battery-conscious manual scan toggle
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(RadioSurfaceVariant)
                            .clickable { viewModel.toggleDiscovery() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("toggle_discovery_btn")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Radar,
                                contentDescription = null,
                                tint = if (uiState.isDiscovering) RadioCyan else RadioTextTertiary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (uiState.isDiscovering) "Pause" else "Scan",
                                style = MaterialTheme.typography.labelSmall,
                                color = RadioTextPrimary
                            )
                        }
                    }
                }
            }

            // Audio Waveform Visualizer
            AudioWaveformVisualizer(
                audioLevel = uiState.audioLevel,
                isTransmitting = uiState.isTransmitting,
                isReceiving = uiState.isReceiving,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // Recipient Control Bar
            RecipientControlBar(
                connectedCount = uiState.connectedPeers.size,
                selectedCount = uiState.selectedEndpointIds.size,
                onSelectAll = { viewModel.selectAllConnected() },
                onDeselectAll = { viewModel.deselectAll() }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Large PTT Button
            val isFloorLockedByOther = uiState.activeSpeakerName != null && !uiState.isTransmitting
            PttButton(
                isTransmitting = uiState.isTransmitting,
                isReceiving = uiState.isReceiving,
                isFloorBusy = isFloorLockedByOther,
                floorHolderName = uiState.activeSpeakerName,
                hasRecipients = uiState.selectedEndpointIds.isNotEmpty(),
                onPress = { viewModel.onPttPress() },
                onRelease = { viewModel.onPttRelease() }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Navigation Tabs for Peers Management
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = RadioSurface,
                contentColor = RadioCyan,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = RadioCyan
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "Connected (${uiState.connectedPeers.size})",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "Nearby (${uiState.discoveredPeers.size})",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Text(
                            text = "Saved (${uiState.savedPeers.size})",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Scrollable Peer Lists
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("peer_list"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        // Connected Peers
                        if (uiState.connectedPeers.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    title = "No Peers Connected Yet",
                                    subtitle = "Go to 'Nearby' tab to find devices or wait for peers on Channel '${uiState.userSettings.activeChannel}' to connect."
                                )
                            }
                        } else {
                            items(
                                items = uiState.connectedPeers,
                                key = { it.endpointId }
                            ) { peer ->
                                ConnectedUserItem(
                                    peer = peer,
                                    isSelected = uiState.selectedEndpointIds.contains(peer.endpointId),
                                    onToggleSelect = { viewModel.togglePeerSelection(peer.endpointId) },
                                    onDisconnect = { viewModel.disconnectPeer(peer.endpointId) }
                                )
                            }
                        }
                    }

                    1 -> {
                        // Discovered Nearby Devices
                        if (uiState.discoveredPeers.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    title = if (uiState.isDiscovering) "Scanning For Nearby Devices…" else "Discovery is Paused",
                                    subtitle = "Make sure other phones have Nearby Talk open with Bluetooth and Wi-Fi enabled on channel '${uiState.userSettings.activeChannel}'."
                                )
                            }
                        } else {
                            items(
                                items = uiState.discoveredPeers,
                                key = { it.endpointId }
                            ) { peer ->
                                DiscoveredPeerItem(
                                    peer = peer,
                                    onConnect = { viewModel.requestConnection(peer.endpointId, peer.endpointName) }
                                )
                            }
                        }
                    }

                    2 -> {
                        // Saved History
                        if (uiState.savedPeers.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    title = "No Connection History",
                                    subtitle = "Peers you connect with will be saved locally in your offline history for quick access."
                                )
                            }
                        } else {
                            items(
                                items = uiState.savedPeers,
                                key = { it.deviceId }
                            ) { saved ->
                                val onlinePeer = uiState.discoveredPeers.find {
                                    it.endpointName.contains(saved.deviceId) || it.endpointId == saved.deviceId
                                }
                                SavedPeerItem(
                                    peer = saved,
                                    isCurrentlyOnline = onlinePeer != null,
                                    onlineEndpointId = onlinePeer?.endpointId,
                                    onConnectOnline = { epId -> viewModel.requestConnection(epId, saved.displayName) },
                                    onDelete = { viewModel.deleteSavedPeer(saved) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    uiState.pendingRequest?.let { req ->
        IncomingConnectionDialog(
            request = req,
            onAccept = { viewModel.acceptPendingConnection() },
            onReject = { viewModel.rejectPendingConnection() }
        )
    }

    if (showChannelDialog) {
        ChannelSwitchDialog(
            currentChannel = uiState.userSettings.activeChannel,
            onSelectChannel = { newChannel ->
                viewModel.updateProfile(
                    uiState.userSettings.displayName,
                    newChannel,
                    uiState.userSettings.autoAccept
                )
            },
            onDismiss = { showChannelDialog = false }
        )
    }

    if (showProfileDialog) {
        ProfileDialog(
            currentSettings = uiState.userSettings,
            onSave = { name, ch, autoAccept ->
                viewModel.updateProfile(name, ch, autoAccept)
            },
            onDismiss = { showProfileDialog = false }
        )
    }

    if (showDiagnosticsDialog) {
        DiagnosticsDialog(
            onDismiss = { showDiagnosticsDialog = false }
        )
    }
}

@Composable
private fun EmptyStateCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        colors = CardDefaults.cardColors(containerColor = RadioSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Podcasts,
                contentDescription = null,
                tint = RadioCyan.copy(alpha = 0.5f),
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = RadioTextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = RadioTextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
