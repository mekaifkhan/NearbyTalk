package com.example.ui.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserSettings
import com.example.nearby.PendingConnectionRequest
import com.example.ui.theme.RadioAmber
import com.example.ui.theme.RadioCyan
import com.example.ui.theme.RadioGreen
import com.example.ui.theme.RadioRed
import com.example.ui.theme.RadioSurface
import com.example.ui.theme.RadioSurfaceElevated
import com.example.ui.theme.RadioSurfaceVariant
import com.example.ui.theme.RadioTextPrimary
import com.example.ui.theme.RadioTextSecondary

@Composable
fun IncomingConnectionDialog(
    request: PendingConnectionRequest,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onReject,
        containerColor = RadioSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Podcasts,
                    contentDescription = null,
                    tint = RadioCyan
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Incoming Connection",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = RadioTextPrimary
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "${request.endpointName} wants to establish a peer-to-peer walkie-talkie link.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = RadioTextSecondary
                )
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(RadioSurfaceVariant)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "SECURITY PAIRING CODE",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                            color = RadioTextSecondary
                        )
                        Text(
                            text = request.authDigits,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 4.sp
                            ),
                            color = RadioCyan
                        )
                        Text(
                            text = "Ensure this matches on the other phone",
                            style = MaterialTheme.typography.labelSmall,
                            color = RadioTextSecondary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onAccept,
                modifier = Modifier.testTag("accept_connection_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = RadioGreen, contentColor = Color.Black)
            ) {
                Text("Accept Link", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onReject,
                modifier = Modifier.testTag("reject_connection_btn")
            ) {
                Text("Decline")
            }
        }
    )
}

@Composable
fun ChannelSwitchDialog(
    currentChannel: String,
    onSelectChannel: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val presets = listOf("General", "Tactical", "Rescue", "Alpha Team", "Bravo Team")
    var customChannel by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = RadioSurface,
        title = {
            Text(
                text = "Select Radio Channel",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = RadioTextPrimary
            )
        },
        text = {
            Column {
                Text(
                    text = "Nearby users on the same channel automatically discover each other in an offline cluster.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = RadioTextSecondary
                )
                Spacer(modifier = Modifier.height(14.dp))

                presets.forEach { preset ->
                    val isSelected = preset.equals(currentChannel, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) RadioSurfaceElevated else RadioSurfaceVariant)
                            .border(
                                width = if (isSelected) 1.dp else 0.dp,
                                color = if (isSelected) RadioCyan else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                onSelectChannel(preset)
                                onDismiss()
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CH: $preset",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = if (isSelected) RadioCyan else RadioTextPrimary
                            )
                            if (isSelected) {
                                Text(
                                    text = "ACTIVE",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = RadioCyan
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = customChannel,
                    onValueChange = { customChannel = it },
                    label = { Text("Custom Channel Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            if (customChannel.isNotBlank()) {
                Button(
                    onClick = {
                        onSelectChannel(customChannel.trim())
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RadioCyan, contentColor = Color.Black)
                ) {
                    Text("Set Channel")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    )
}

@Composable
fun ProfileDialog(
    currentSettings: UserSettings,
    onSave: (displayName: String, activeChannel: String, autoAccept: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(currentSettings.displayName) }
    var autoAccept by remember { mutableStateOf(currentSettings.autoAccept) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = RadioSurface,
        title = {
            Text(
                text = "Radio Operator Profile",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = RadioTextPrimary
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Call Sign / Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Device ID: ${currentSettings.deviceId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = RadioTextSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Accept Connections",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = RadioTextPrimary
                        )
                        Text(
                            text = "Instantly connect with reachable peers on this channel",
                            style = MaterialTheme.typography.bodySmall,
                            color = RadioTextSecondary
                        )
                    }
                    Switch(
                        checked = autoAccept,
                        onCheckedChange = { autoAccept = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = RadioCyan,
                            checkedTrackColor = RadioSurfaceElevated
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(name, currentSettings.activeChannel, autoAccept)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = RadioCyan, contentColor = Color.Black)
            ) {
                Text("Save Profile")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DiagnosticsDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = RadioSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = RadioCyan)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Offline Walkie-Talkie Info",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = RadioTextPrimary
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Nearby Talk operates 100% offline using Google Play services Nearby Connections P2P clustering.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = RadioTextSecondary
                )

                DiagnosticRow(
                    icon = Icons.Default.Mic,
                    title = "Microphone",
                    subtitle = "16kHz wideband PCM audio capture with Acoustic Echo Cancellation"
                )
                DiagnosticRow(
                    icon = Icons.Default.Bluetooth,
                    title = "Bluetooth & BLE",
                    subtitle = "Used for initial zero-configuration peer discovery without internet"
                )
                DiagnosticRow(
                    icon = Icons.Default.Wifi,
                    title = "Wi-Fi Direct / Local Hotspot",
                    subtitle = "High-speed peer-to-peer data transport for real-time voice streaming"
                )
                DiagnosticRow(
                    icon = Icons.Default.Security,
                    title = "Floor Control",
                    subtitle = "Speaker lock mechanism prevents simultaneous cross-talk collision"
                )
                DiagnosticRow(
                    icon = Icons.Default.Podcasts,
                    title = "Radio Status (Error 8007)",
                    subtitle = "Nearby Connections requires physical Bluetooth and Wi-Fi enabled. If testing in an emulator, real radios are unavailable; run on physical devices."
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = RadioCyan, contentColor = Color.Black)
            ) {
                Text("Got It")
            }
        }
    )
}

@Composable
private fun DiagnosticRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(RadioSurfaceVariant)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = RadioCyan,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = RadioTextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = RadioTextSecondary
            )
        }
    }
}
