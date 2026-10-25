package com.example.ui

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.NearbyDatabase
import com.example.data.model.SavedPeer
import com.example.data.model.UserSettings
import com.example.data.repository.NearbyRepository
import com.example.nearby.ConnectedPeer
import com.example.nearby.DiscoveredPeer
import com.example.nearby.NearbyConnectionManager
import com.example.nearby.PendingConnectionRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TalkUiState(
    val userSettings: UserSettings = UserSettings(id = 1, deviceId = "NT-INIT", displayName = "Radio Operator"),
    val isAdvertising: Boolean = false,
    val isDiscovering: Boolean = false,
    val radioError: String? = null,
    val discoveredPeers: List<DiscoveredPeer> = emptyList(),
    val connectedPeers: List<ConnectedPeer> = emptyList(),
    val savedPeers: List<SavedPeer> = emptyList(),
    val selectedEndpointIds: Set<String> = emptySet(),
    val activeSpeakerName: String? = null,
    val isTransmitting: Boolean = false,
    val isReceiving: Boolean = false,
    val audioLevel: Float = 0f,
    val pendingRequest: PendingConnectionRequest? = null,
    val latestNotice: String? = null
)

class TalkViewModel(application: Application) : AndroidViewModel(application) {
    private val database = NearbyDatabase.getInstance(application)
    val repository = NearbyRepository(database.peerDao(), database.settingsDao())

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = application.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        application.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    val nearbyManager = NearbyConnectionManager(application) { deviceId, displayName ->
        viewModelScope.launch {
            repository.recordPeerConnection(deviceId, displayName)
        }
    }

    private val _selectedEndpointIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedEndpointIds: StateFlow<Set<String>> = _selectedEndpointIds.asStateFlow()

    private val _userSettings = MutableStateFlow(UserSettings(id = 1, deviceId = "NT-LOCAL", displayName = "Operator"))
    val userSettings: StateFlow<UserSettings> = _userSettings.asStateFlow()

    private val _notice = MutableStateFlow<String?>("Ready for offline communications")
    val notice: StateFlow<String?> = _notice.asStateFlow()

    val savedPeers: StateFlow<List<SavedPeer>> = repository.savedPeers.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val uiState: StateFlow<TalkUiState> = combine(
        _userSettings,
        nearbyManager.isAdvertising,
        nearbyManager.isDiscovering,
        nearbyManager.radioError,
        nearbyManager.discoveredPeers,
        nearbyManager.connectedPeers,
        savedPeers,
        _selectedEndpointIds,
        nearbyManager.activeSpeaker,
        nearbyManager.isTransmitting,
        nearbyManager.audioCaptureManager.amplitude,
        nearbyManager.audioPlaybackManager.amplitude,
        nearbyManager.pendingRequest,
        _notice
    ) { values ->
        val settings = values[0] as UserSettings
        val isAdv = values[1] as Boolean
        val isDisc = values[2] as Boolean
        val radioErr = values[3] as? String
        @Suppress("UNCHECKED_CAST")
        val discMap = values[4] as Map<String, DiscoveredPeer>
        @Suppress("UNCHECKED_CAST")
        val connMap = values[5] as Map<String, ConnectedPeer>
        @Suppress("UNCHECKED_CAST")
        val saved = values[6] as List<SavedPeer>
        @Suppress("UNCHECKED_CAST")
        val selectedIds = values[7] as Set<String>
        val activeSpeaker = values[8] as? com.example.nearby.ActiveSpeaker
        val isTx = values[9] as Boolean
        val micAmp = values[10] as Float
        val playAmp = values[11] as Float
        val pendingReq = values[12] as? PendingConnectionRequest
        val note = values[13] as? String

        val isRx = activeSpeaker != null && !isTx
        val effectiveAudioLevel = if (isTx) micAmp else if (isRx) playAmp else 0f

        TalkUiState(
            userSettings = settings,
            isAdvertising = isAdv,
            isDiscovering = isDisc,
            radioError = radioErr,
            discoveredPeers = discMap.values.toList(),
            connectedPeers = connMap.values.toList(),
            savedPeers = saved,
            selectedEndpointIds = selectedIds,
            activeSpeakerName = activeSpeaker?.speakerName,
            isTransmitting = isTx,
            isReceiving = isRx,
            audioLevel = effectiveAudioLevel,
            pendingRequest = pendingReq,
            latestNotice = note
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TalkUiState()
    )

    init {
        viewModelScope.launch {
            val settings = repository.getOrCreateSettings(Build.MODEL ?: "Android Device")
            _userSettings.value = settings
            nearbyManager.initialize(
                deviceId = settings.deviceId,
                displayName = settings.displayName,
                channel = settings.activeChannel,
                autoAcceptConnections = settings.autoAccept
            )
        }

        // Auto-select newly connected peers so user doesn't miss communicating with them
        viewModelScope.launch {
            nearbyManager.connectedPeers.collect { connMap ->
                val current = _selectedEndpointIds.value.toMutableSet()
                current.retainAll(connMap.keys)
                if (current.isEmpty() && connMap.isNotEmpty()) {
                    current.addAll(connMap.keys)
                }
                _selectedEndpointIds.value = current
            }
        }

        // Collect status notices
        viewModelScope.launch {
            nearbyManager.statusMessages.collect { msg ->
                _notice.value = msg
            }
        }
    }

    fun startNearbyServices() {
        val settings = _userSettings.value
        nearbyManager.startAdvertisingAndDiscovery(settings.displayName, settings.activeChannel)
        _notice.value = "Broadcasting & scanning on channel '${settings.activeChannel}'"
    }

    fun stopNearbyServices() {
        nearbyManager.stopAdvertisingAndDiscovery()
        _notice.value = "Discovery stopped"
    }

    fun toggleDiscovery() {
        if (uiState.value.isDiscovering || uiState.value.isAdvertising) {
            stopNearbyServices()
        } else {
            startNearbyServices()
        }
    }

    fun togglePeerSelection(endpointId: String) {
        val current = _selectedEndpointIds.value.toMutableSet()
        if (current.contains(endpointId)) {
            current.remove(endpointId)
        } else {
            current.add(endpointId)
        }
        _selectedEndpointIds.value = current
    }

    fun selectAllConnected() {
        val allIds = nearbyManager.connectedPeers.value.keys
        _selectedEndpointIds.value = allIds
    }

    fun deselectAll() {
        _selectedEndpointIds.value = emptySet()
    }

    fun triggerVibration(durationMs: Long = 40) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }

    fun onPttPress(): Boolean {
        triggerVibration(45)
        val selected = _selectedEndpointIds.value.toList()
        return nearbyManager.startPushToTalk(selected)
    }

    fun onPttRelease() {
        triggerVibration(30)
        nearbyManager.stopPushToTalk()
    }

    fun requestConnection(endpointId: String, peerName: String) {
        nearbyManager.requestConnection(endpointId, peerName)
    }

    fun acceptPendingConnection() {
        val pending = uiState.value.pendingRequest ?: return
        nearbyManager.acceptConnection(pending.endpointId)
    }

    fun rejectPendingConnection() {
        val pending = uiState.value.pendingRequest ?: return
        nearbyManager.rejectConnection(pending.endpointId)
    }

    fun disconnectPeer(endpointId: String) {
        nearbyManager.disconnectPeer(endpointId)
    }

    fun disconnectAllPeers() {
        nearbyManager.disconnectAll()
    }

    fun updateProfile(displayName: String, activeChannel: String, autoAccept: Boolean) {
        viewModelScope.launch {
            val updated = _userSettings.value.copy(
                displayName = displayName.trim().ifBlank { _userSettings.value.displayName },
                activeChannel = activeChannel.trim().ifBlank { "General" },
                autoAccept = autoAccept
            )
            repository.updateSettings(updated)
            _userSettings.value = updated

            val channelChanged = _userSettings.value.activeChannel != updated.activeChannel
            nearbyManager.initialize(
                deviceId = updated.deviceId,
                displayName = updated.displayName,
                channel = updated.activeChannel,
                autoAcceptConnections = updated.autoAccept
            )

            if (channelChanged && (uiState.value.isDiscovering || uiState.value.isAdvertising)) {
                nearbyManager.stopAdvertisingAndDiscovery()
                nearbyManager.startAdvertisingAndDiscovery(updated.displayName, updated.activeChannel)
                _notice.value = "Switched to Channel '${updated.activeChannel}'"
            }
        }
    }

    fun deleteSavedPeer(peer: SavedPeer) {
        viewModelScope.launch {
            repository.deleteSavedPeer(peer)
        }
    }

    override fun onCleared() {
        super.onCleared()
        nearbyManager.cleanup()
    }
}
