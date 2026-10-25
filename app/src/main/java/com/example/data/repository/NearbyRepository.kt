package com.example.data.repository

import com.example.data.db.PeerDao
import com.example.data.db.SettingsDao
import com.example.data.model.SavedPeer
import com.example.data.model.UserSettings
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class NearbyRepository(
    private val peerDao: PeerDao,
    private val settingsDao: SettingsDao
) {
    val savedPeers: Flow<List<SavedPeer>> = peerDao.getAllSavedPeers()
    val userSettings: Flow<UserSettings?> = settingsDao.getSettings()

    suspend fun getOrCreateSettings(defaultName: String): UserSettings {
        val current = settingsDao.getSettingsDirect()
        if (current != null) return current
        val randomSuffix = UUID.randomUUID().toString().take(4).uppercase()
        val generated = UserSettings(
            id = 1,
            deviceId = "NT-$randomSuffix",
            displayName = if (defaultName.isNotBlank()) defaultName else "Operator-$randomSuffix",
            activeChannel = "General",
            autoAccept = true
        )
        settingsDao.saveSettings(generated)
        return generated
    }

    suspend fun updateSettings(settings: UserSettings) {
        settingsDao.saveSettings(settings)
    }

    suspend fun recordPeerConnection(deviceId: String, displayName: String) {
        val existing = peerDao.getPeerByDeviceId(deviceId)
        if (existing != null) {
            peerDao.insertOrUpdatePeer(
                existing.copy(
                    displayName = displayName,
                    lastConnectedTimestamp = System.currentTimeMillis(),
                    connectionCount = existing.connectionCount + 1
                )
            )
        } else {
            peerDao.insertOrUpdatePeer(
                SavedPeer(
                    deviceId = deviceId,
                    displayName = displayName,
                    lastConnectedTimestamp = System.currentTimeMillis(),
                    connectionCount = 1
                )
            )
        }
    }

    suspend fun deleteSavedPeer(peer: SavedPeer) {
        peerDao.deletePeer(peer)
    }
}
