package com.example.nearby

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.audio.AudioCaptureManager
import com.example.audio.AudioPlaybackManager
import com.example.protocol.NearbyPacket
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NearbyConnectionManager(
    private val context: Context,
    private val onPeerConnected: (deviceId: String, displayName: String) -> Unit
) {
    private val tag = "NearbyConnMgr"
    private val connectionsClient: ConnectionsClient = Nearby.getConnectionsClient(context.applicationContext)
    private val scope = CoroutineScope(Dispatchers.Default)

    // Audio Managers
    val audioCaptureManager = AudioCaptureManager { pcmBytes, seqNum ->
        sendAudioChunk(pcmBytes, seqNum)
    }
    val audioPlaybackManager = AudioPlaybackManager()

    // Service State
    private val _isAdvertising = MutableStateFlow(false)
    val isAdvertising: StateFlow<Boolean> = _isAdvertising.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    private val _radioError = MutableStateFlow<String?>(null)
    val radioError: StateFlow<String?> = _radioError.asStateFlow()

    private val _discoveredPeers = MutableStateFlow<Map<String, DiscoveredPeer>>(emptyMap())
    val discoveredPeers: StateFlow<Map<String, DiscoveredPeer>> = _discoveredPeers.asStateFlow()

    private val _connectedPeers = MutableStateFlow<Map<String, ConnectedPeer>>(emptyMap())
    val connectedPeers: StateFlow<Map<String, ConnectedPeer>> = _connectedPeers.asStateFlow()

    private val _pendingRequest = MutableStateFlow<PendingConnectionRequest?>(null)
    val pendingRequest: StateFlow<PendingConnectionRequest?> = _pendingRequest.asStateFlow()

    // Floor Control & Transmission State
    private val _activeSpeaker = MutableStateFlow<ActiveSpeaker?>(null)
    val activeSpeaker: StateFlow<ActiveSpeaker?> = _activeSpeaker.asStateFlow()

    private val _isTransmitting = MutableStateFlow(false)
    val isTransmitting: StateFlow<Boolean> = _isTransmitting.asStateFlow()

    private val _statusMessages = MutableSharedFlow<String>(extraBufferCapacity = 10)
    val statusMessages: SharedFlow<String> = _statusMessages.asSharedFlow()

    private var localDeviceId: String = ""
    private var localDisplayName: String = ""
    private var currentChannel: String = "General"
    private var autoAccept: Boolean = true

    // Current PTT Target Recipient Endpoint IDs
    private var currentTransmissionTargets = listOf<String>()

    val isEmulator: Boolean by lazy {
        Build.FINGERPRINT.startsWith("generic") ||
                Build.FINGERPRINT.startsWith("unknown") ||
                Build.MODEL.contains("google_sdk") ||
                Build.MODEL.contains("Emulator") ||
                Build.MODEL.contains("Android SDK built for") ||
                Build.HARDWARE.contains("goldfish") ||
                Build.HARDWARE.contains("ranchu")
    }

    private fun getServiceId(channel: String): String {
        val sanitized = channel.trim().lowercase().replace(Regex("[^a-z0-9]"), "_")
        return "nearby_talk_$sanitized"
    }

    fun hasRequiredPermissions(): Boolean {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
        return permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun checkRadioStatus(): String? {
        if (!hasRequiredPermissions()) {
            return "Permissions required: Please grant Location (Precise & Approximate), Nearby Devices, and Microphone."
        }

        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val bluetoothAdapter = bluetoothManager?.adapter

        if (isEmulator && (bluetoothAdapter == null || !bluetoothAdapter.isEnabled)) {
            return "Bluetooth radio is unavailable or disabled in this Android Emulator. Physical Bluetooth & Wi-Fi are required for peer discovery. Test between physical Android phones."
        }

        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled) {
            return "Bluetooth is currently turned OFF. Please turn on Bluetooth in Settings to discover nearby walkie-talkie peers."
        }

        return null
    }

    // Callbacks
    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type != Payload.Type.BYTES) return
            val bytes = payload.asBytes() ?: return
            val packet = NearbyPacket.deserialize(bytes) ?: return

            when (packet) {
                is NearbyPacket.FloorRequest -> {
                    val currentSpeaker = _activeSpeaker.value
                    if (currentSpeaker == null || currentSpeaker.speakerId == packet.senderId) {
                        _activeSpeaker.value = ActiveSpeaker(packet.senderId, packet.senderName)
                        if (!_isTransmitting.value) {
                            audioPlaybackManager.startPlayback()
                        }
                    } else if (currentSpeaker.speakerId != packet.senderId) {
                        val busyPacket = NearbyPacket.FloorBusy(currentSpeaker.speakerId, currentSpeaker.speakerName)
                        connectionsClient.sendPayload(endpointId, Payload.fromBytes(NearbyPacket.serialize(busyPacket)))
                    }
                }

                is NearbyPacket.FloorRelease -> {
                    val currentSpeaker = _activeSpeaker.value
                    if (currentSpeaker != null && currentSpeaker.speakerId == packet.senderId) {
                        _activeSpeaker.value = null
                        audioPlaybackManager.stopPlayback()
                    }
                }

                is NearbyPacket.AudioData -> {
                    if (!_isTransmitting.value) {
                        val currentSpeaker = _activeSpeaker.value
                        if (currentSpeaker == null) {
                            val peer = _connectedPeers.value[endpointId]
                            val speakerName = peer?.displayName ?: "Peer ($endpointId)"
                            _activeSpeaker.value = ActiveSpeaker(packet.senderId, speakerName)
                        }
                        audioPlaybackManager.playChunk(packet.pcmData)
                    }
                }

                is NearbyPacket.FloorBusy -> {
                    if (_isTransmitting.value) {
                        stopPushToTalk()
                        scope.launch {
                            _statusMessages.emit("Floor taken by ${packet.currentSpeakerName}")
                        }
                    }
                }

                is NearbyPacket.Ping -> {
                    val existing = _connectedPeers.value[endpointId]
                    if (existing != null) {
                        _connectedPeers.value = _connectedPeers.value + (endpointId to existing.copy(
                            displayName = packet.senderName,
                            channel = packet.channel
                        ))
                    }
                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {}
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            Log.d(tag, "Connection initiated: $endpointId (${connectionInfo.endpointName})")
            if (autoAccept) {
                connectionsClient.acceptConnection(endpointId, payloadCallback)
            } else {
                _pendingRequest.value = PendingConnectionRequest(
                    endpointId = endpointId,
                    endpointName = connectionInfo.endpointName,
                    authDigits = connectionInfo.authenticationDigits
                )
            }
        }

        override fun onConnectionResult(endpointId: String, resolution: ConnectionResolution) {
            when (resolution.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    Log.d(tag, "Connection established with: $endpointId")
                    val discovered = _discoveredPeers.value[endpointId]
                    val peerName = discovered?.endpointName ?: "User $endpointId"
                    val connectedPeer = ConnectedPeer(
                        endpointId = endpointId,
                        deviceId = endpointId,
                        displayName = peerName,
                        channel = currentChannel
                    )
                    _connectedPeers.value = _connectedPeers.value + (endpointId to connectedPeer)
                    _discoveredPeers.value = _discoveredPeers.value - endpointId
                    _pendingRequest.value = null

                    onPeerConnected(endpointId, peerName)

                    val ping = NearbyPacket.Ping(localDeviceId, localDisplayName, currentChannel)
                    connectionsClient.sendPayload(endpointId, Payload.fromBytes(NearbyPacket.serialize(ping)))

                    scope.launch {
                        _statusMessages.emit("Connected to $peerName")
                    }
                }

                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    Log.w(tag, "Connection rejected by: $endpointId")
                    _pendingRequest.value = null
                    scope.launch {
                        _statusMessages.emit("Connection rejected by $endpointId")
                    }
                }

                else -> {
                    Log.e(tag, "Connection failed: ${resolution.status.statusMessage}")
                    _pendingRequest.value = null
                    scope.launch {
                        _statusMessages.emit("Connection failed (${resolution.status.statusCode})")
                    }
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            Log.d(tag, "Peer disconnected: $endpointId")
            val peer = _connectedPeers.value[endpointId]
            _connectedPeers.value = _connectedPeers.value - endpointId

            if (_activeSpeaker.value?.speakerId == endpointId) {
                _activeSpeaker.value = null
                audioPlaybackManager.stopPlayback()
            }

            scope.launch {
                val name = peer?.displayName ?: endpointId
                _statusMessages.emit("$name disconnected")
            }
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            Log.d(tag, "Endpoint found: $endpointId (${info.endpointName})")
            if (!_connectedPeers.value.containsKey(endpointId)) {
                _discoveredPeers.value = _discoveredPeers.value + (endpointId to DiscoveredPeer(
                    endpointId = endpointId,
                    endpointName = info.endpointName,
                    status = PeerConnectionStatus.DISCOVERED
                ))
            }
        }

        override fun onEndpointLost(endpointId: String) {
            Log.d(tag, "Endpoint lost: $endpointId")
            _discoveredPeers.value = _discoveredPeers.value - endpointId
        }
    }

    fun initialize(deviceId: String, displayName: String, channel: String, autoAcceptConnections: Boolean) {
        localDeviceId = deviceId
        localDisplayName = displayName
        currentChannel = channel
        autoAccept = autoAcceptConnections
    }

    fun startAdvertisingAndDiscovery(displayName: String, channel: String) {
        localDisplayName = displayName
        currentChannel = channel

        if (!hasRequiredPermissions()) {
            val missingMsg = "Location & Nearby Devices permissions are required to start peer communication."
            _radioError.value = missingMsg
            scope.launch { _statusMessages.emit(missingMsg) }
            Log.w(tag, "Cannot start services: $missingMsg")
            return
        }

        val hardwareIssue = checkRadioStatus()
        if (hardwareIssue != null) {
            _radioError.value = hardwareIssue
            scope.launch { _statusMessages.emit(hardwareIssue) }
            Log.w(tag, "Radio check warning: $hardwareIssue")
        }

        stopAdvertisingAndDiscovery()

        val serviceId = getServiceId(channel)
        val advOptions = AdvertisingOptions.Builder()
            .setStrategy(Strategy.P2P_CLUSTER)
            .build()

        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val bluetoothAdapter = bluetoothManager?.adapter
        val canAdvertise = bluetoothAdapter != null && bluetoothAdapter.isEnabled &&
                (bluetoothAdapter.isMultipleAdvertisementSupported || !isEmulator)

        if (canAdvertise) {
            connectionsClient.startAdvertising(
                "$displayName ($localDeviceId)",
                serviceId,
                connectionLifecycleCallback,
                advOptions
            ).addOnSuccessListener {
                _isAdvertising.value = true
                _radioError.value = null
                Log.i(tag, "Started advertising on $serviceId as $displayName")

                scope.launch {
                    delay(350)
                    startDiscoveryInternal(serviceId)
                }
            }.addOnFailureListener { e ->
                handleRadioError("Advertising", e)
                scope.launch {
                    delay(400)
                    startDiscoveryInternal(serviceId)
                }
            }
        } else {
            Log.i(tag, "Device advertising is unsupported or disabled; launching discovery directly")
            scope.launch {
                delay(200)
                startDiscoveryInternal(serviceId)
            }
        }
    }

    private fun startDiscoveryInternal(serviceId: String) {
        if (!hasRequiredPermissions()) {
            Log.w(tag, "Skipping discovery: permissions not granted")
            return
        }

        val discOptions = DiscoveryOptions.Builder()
            .setStrategy(Strategy.P2P_CLUSTER)
            .build()

        connectionsClient.startDiscovery(
            serviceId,
            endpointDiscoveryCallback,
            discOptions
        ).addOnSuccessListener {
            _isDiscovering.value = true
            _radioError.value = null
            Log.i(tag, "Started discovery on $serviceId")
        }.addOnFailureListener { e ->
            handleRadioError("Discovery", e)
        }
    }

    private fun handleRadioError(phase: String, e: Exception) {
        val statusCode = (e as? ApiException)?.statusCode ?: -1
        Log.e(tag, "$phase failed with status code $statusCode: ${e.message}")

        if (phase == "Advertising") _isAdvertising.value = false
        if (phase == "Discovery") _isDiscovering.value = false

        when (statusCode) {
            8034, 8035 -> {
                val msg = "Location permission (Approximate & Precise) is required for peer discovery."
                _radioError.value = msg
                scope.launch { _statusMessages.emit(msg) }
            }
            8036 -> {
                val msg = "Microphone permission is required for voice transmission."
                _radioError.value = msg
                scope.launch { _statusMessages.emit(msg) }
            }
            8037, 8038, 8039, 8040 -> {
                val msg = "Nearby Devices permission is required to discover local peers."
                _radioError.value = msg
                scope.launch { _statusMessages.emit(msg) }
            }
            ConnectionsStatusCodes.STATUS_RADIO_ERROR -> {
                val msg = if (isEmulator) {
                    "Bluetooth/Wi-Fi radios are simulated or offline in this Android Emulator (Status 8007). Physical Bluetooth & Wi-Fi are required for peer walkie-talkie networking. Test on two physical Android phones."
                } else {
                    "Radio Error (8007): Bluetooth or Wi-Fi is disabled or busy. Please enable Bluetooth and Wi-Fi in Android Settings."
                }
                _radioError.value = msg
                scope.launch { _statusMessages.emit(msg) }
            }
            ConnectionsStatusCodes.STATUS_ALREADY_ADVERTISING -> {
                _isAdvertising.value = true
            }
            ConnectionsStatusCodes.STATUS_ALREADY_DISCOVERING -> {
                _isDiscovering.value = true
            }
            else -> {
                scope.launch { _statusMessages.emit("$phase failed: ${e.localizedMessage ?: e.message}") }
            }
        }
    }

    fun stopAdvertisingAndDiscovery() {
        try {
            connectionsClient.stopAdvertising()
            _isAdvertising.value = false
        } catch (e: Exception) {
            Log.e(tag, "Error stopping advertising", e)
        }
        try {
            connectionsClient.stopDiscovery()
            _isDiscovering.value = false
        } catch (e: Exception) {
            Log.e(tag, "Error stopping discovery", e)
        }
    }

    fun requestConnection(endpointId: String, peerName: String) {
        val currentDiscovered = _discoveredPeers.value[endpointId]
        if (currentDiscovered != null) {
            _discoveredPeers.value = _discoveredPeers.value + (endpointId to currentDiscovered.copy(
                status = PeerConnectionStatus.CONNECTING
            ))
        }

        connectionsClient.requestConnection(
            "$localDisplayName ($localDeviceId)",
            endpointId,
            connectionLifecycleCallback
        ).addOnSuccessListener {
            Log.d(tag, "Connection requested to $endpointId")
            scope.launch { _statusMessages.emit("Request sent to $peerName") }
        }.addOnFailureListener { e ->
            Log.e(tag, "Failed to request connection to $endpointId", e)
            if (currentDiscovered != null) {
                _discoveredPeers.value = _discoveredPeers.value + (endpointId to currentDiscovered.copy(
                    status = PeerConnectionStatus.DISCOVERED
                ))
            }
            scope.launch { _statusMessages.emit("Connect error: ${e.message}") }
        }
    }

    fun acceptConnection(endpointId: String) {
        connectionsClient.acceptConnection(endpointId, payloadCallback)
        _pendingRequest.value = null
    }

    fun rejectConnection(endpointId: String) {
        connectionsClient.rejectConnection(endpointId)
        _pendingRequest.value = null
    }

    fun disconnectPeer(endpointId: String) {
        try {
            connectionsClient.disconnectFromEndpoint(endpointId)
            _connectedPeers.value = _connectedPeers.value - endpointId
        } catch (e: Exception) {
            Log.e(tag, "Error disconnecting $endpointId", e)
        }
    }

    fun disconnectAll() {
        try {
            connectionsClient.stopAllEndpoints()
            _connectedPeers.value = emptyMap()
            _activeSpeaker.value = null
            audioCaptureManager.stopCapture()
            audioPlaybackManager.stopPlayback()
        } catch (e: Exception) {
            Log.e(tag, "Error disconnecting all", e)
        }
    }

    // PTT Voice Transmission
    fun startPushToTalk(selectedEndpoints: List<String>): Boolean {
        if (selectedEndpoints.isEmpty()) {
            scope.launch { _statusMessages.emit("No recipient peers selected!") }
            return false
        }

        val current = _activeSpeaker.value
        if (current != null && current.speakerId != localDeviceId) {
            scope.launch { _statusMessages.emit("Channel busy: ${current.speakerName} is speaking") }
            return false
        }

        currentTransmissionTargets = selectedEndpoints
        _activeSpeaker.value = ActiveSpeaker(localDeviceId, localDisplayName)
        _isTransmitting.value = true

        val requestPacket = NearbyPacket.FloorRequest(localDeviceId, localDisplayName, currentChannel)
        val payload = Payload.fromBytes(NearbyPacket.serialize(requestPacket))
        connectionsClient.sendPayload(currentTransmissionTargets, payload)

        val started = audioCaptureManager.startCapture()
        if (!started) {
            stopPushToTalk()
            scope.launch { _statusMessages.emit("Microphone capture failed") }
            return false
        }
        return true
    }

    fun stopPushToTalk() {
        if (!_isTransmitting.value) return

        _isTransmitting.value = false
        audioCaptureManager.stopCapture()

        if (_activeSpeaker.value?.speakerId == localDeviceId) {
            _activeSpeaker.value = null
        }

        if (currentTransmissionTargets.isNotEmpty()) {
            val releasePacket = NearbyPacket.FloorRelease(localDeviceId, currentChannel)
            val payload = Payload.fromBytes(NearbyPacket.serialize(releasePacket))
            connectionsClient.sendPayload(currentTransmissionTargets, payload)
        }
        currentTransmissionTargets = emptyList()
    }

    private fun sendAudioChunk(pcmBytes: ByteArray, seqNumber: Int) {
        if (!_isTransmitting.value || currentTransmissionTargets.isEmpty()) return
        val audioPacket = NearbyPacket.AudioData(localDeviceId, seqNumber, pcmBytes)
        val bytes = NearbyPacket.serialize(audioPacket)
        connectionsClient.sendPayload(currentTransmissionTargets, Payload.fromBytes(bytes))
    }

    fun cleanup() {
        stopPushToTalk()
        stopAdvertisingAndDiscovery()
        disconnectAll()
    }
}
