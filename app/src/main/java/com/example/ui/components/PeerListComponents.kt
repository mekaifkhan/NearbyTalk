package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SavedPeer
import com.example.nearby.ConnectedPeer
import com.example.nearby.DiscoveredPeer
import com.example.nearby.PeerConnectionStatus
import com.example.ui.theme.RadioAmber
import com.example.ui.theme.RadioCyan
import com.example.ui.theme.RadioGreen
import com.example.ui.theme.RadioRed
import com.example.ui.theme.RadioSurface
import com.example.ui.theme.RadioSurfaceElevated
import com.example.ui.theme.RadioSurfaceVariant
import com.example.ui.theme.RadioTextPrimary
import com.example.ui.theme.RadioTextSecondary
import com.example.ui.theme.RadioTextTertiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecipientControlBar(
    connectedCount: Int,
    selectedCount: Int,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("recipient_control_bar"),
        colors = CardDefaults.cardColors(containerColor = RadioSurfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "TARGET RECIPIENTS",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                    color = RadioTextTertiary
                )
                Text(
                    text = if (connectedCount == 0) "No connected peers" else "$selectedCount of $connectedCount selected",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (selectedCount > 0) RadioCyan else RadioTextSecondary
                )
            }

            if (connectedCount > 0) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onSelectAll,
                        modifier = Modifier.testTag("select_all_button"),
                        contentPadding = ButtonDefaults.TextButtonContentPadding
                    ) {
                        Text("Select All", style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(
                        onClick = onDeselectAll,
                        modifier = Modifier.testTag("deselect_all_button"),
                        contentPadding = ButtonDefaults.TextButtonContentPadding
                    ) {
                        Text("Clear", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectedUserItem(
    peer: ConnectedPeer,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onToggleSelect() }
            .testTag("connected_peer_${peer.endpointId}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) RadioSurfaceElevated else RadioSurface
        ),
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, RadioCyan) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelect() },
                colors = CheckboxDefaults.colors(
                    checkedColor = RadioCyan,
                    checkmarkColor = Color.Black,
                    uncheckedColor = RadioTextTertiary
                ),
                modifier = Modifier.testTag("checkbox_${peer.endpointId}")
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Avatar with online status badge
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(RadioSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = RadioCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(RadioGreen)
                        .border(1.5.dp, RadioSurface, CircleShape)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = peer.displayName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = RadioTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "ID: ${peer.endpointId} • Channel: ${peer.channel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = RadioTextSecondary
                )
            }

            IconButton(
                onClick = onDisconnect,
                modifier = Modifier.testTag("disconnect_${peer.endpointId}")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Disconnect Peer",
                    tint = RadioTextTertiary
                )
            }
        }
    }
}

@Composable
fun DiscoveredPeerItem(
    peer: DiscoveredPeer,
    onConnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("discovered_peer_${peer.endpointId}"),
        colors = CardDefaults.cardColors(containerColor = RadioSurfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(RadioSurfaceElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.NearMe,
                        contentDescription = null,
                        tint = RadioAmber,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = peer.endpointName,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = RadioTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = when (peer.status) {
                            PeerConnectionStatus.CONNECTING -> "Connecting…"
                            PeerConnectionStatus.CONNECTED -> "Connected"
                            PeerConnectionStatus.DISCONNECTED -> "Disconnected"
                            PeerConnectionStatus.RECONNECTING -> "Reconnecting…"
                            PeerConnectionStatus.DISCOVERED -> "Reachable nearby"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = when (peer.status) {
                            PeerConnectionStatus.CONNECTING -> RadioAmber
                            PeerConnectionStatus.CONNECTED -> RadioGreen
                            else -> RadioTextSecondary
                        }
                    )
                }
            }

            FilledTonalButton(
                onClick = onConnect,
                enabled = peer.status == PeerConnectionStatus.DISCOVERED,
                modifier = Modifier.testTag("connect_btn_${peer.endpointId}"),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = RadioCyan,
                    contentColor = Color.Black
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (peer.status == PeerConnectionStatus.CONNECTING) "Connecting" else "Connect",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
fun SavedPeerItem(
    peer: SavedPeer,
    isCurrentlyOnline: Boolean,
    onlineEndpointId: String?,
    onConnectOnline: ((String) -> Unit)?,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateStr = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
        .format(Date(peer.lastConnectedTimestamp))

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("saved_peer_${peer.deviceId}"),
        colors = CardDefaults.cardColors(containerColor = RadioSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(RadioSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = if (isCurrentlyOnline) RadioGreen else RadioTextTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = peer.displayName,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = RadioTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Badge: Online vs Saved Offline
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isCurrentlyOnline) RadioGreen.copy(alpha = 0.2f) else RadioSurfaceVariant)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isCurrentlyOnline) "REACHABLE" else "SAVED HISTORY",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                            color = if (isCurrentlyOnline) RadioGreen else RadioTextTertiary
                        )
                    }
                }
                Text(
                    text = "Last active: $dateStr • Connected ${peer.connectionCount}x",
                    style = MaterialTheme.typography.bodySmall,
                    color = RadioTextSecondary
                )
            }

            if (isCurrentlyOnline && onlineEndpointId != null && onConnectOnline != null) {
                FilledTonalButton(
                    onClick = { onConnectOnline(onlineEndpointId) },
                    contentPadding = ButtonDefaults.TextButtonContentPadding,
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = RadioGreen, contentColor = Color.Black)
                ) {
                    Text("Connect", style = MaterialTheme.typography.labelSmall)
                }
            } else {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Delete from history",
                        tint = RadioTextTertiary
                    )
                }
            }
        }
    }
}
