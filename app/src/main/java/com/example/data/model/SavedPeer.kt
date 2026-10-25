package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persisted record of previously connected peer devices.
 */
@Entity(tableName = "saved_peers")
data class SavedPeer(
    @PrimaryKey
    val deviceId: String,
    val displayName: String,
    val lastConnectedTimestamp: Long = System.currentTimeMillis(),
    val connectionCount: Int = 1,
    val customAlias: String? = null
)
