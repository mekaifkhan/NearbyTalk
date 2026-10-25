package com.example.nearby

enum class PeerConnectionStatus {
    DISCOVERED,
    CONNECTING,
    CONNECTED,
    DISCONNECTED,
    RECONNECTING
}

data class DiscoveredPeer(
    val endpointId: String,
    val endpointName: String,
    val status: PeerConnectionStatus = PeerConnectionStatus.DISCOVERED,
    val discoveredTime: Long = System.currentTimeMillis()
)

data class ConnectedPeer(
    val endpointId: String,
    val deviceId: String,
    val displayName: String,
    val connectedTime: Long = System.currentTimeMillis(),
    val channel: String = "General"
)

data class PendingConnectionRequest(
    val endpointId: String,
    val endpointName: String,
    val authDigits: String
)

data class ActiveSpeaker(
    val speakerId: String,
    val speakerName: String,
    val startedAt: Long = System.currentTimeMillis()
)
