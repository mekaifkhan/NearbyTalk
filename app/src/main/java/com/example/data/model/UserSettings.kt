package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local user profile and walkie-talkie configuration.
 */
@Entity(tableName = "user_settings")
data class UserSettings(
    @PrimaryKey
    val id: Int = 1,
    val deviceId: String,
    val displayName: String,
    val activeChannel: String = "General",
    val autoAccept: Boolean = true
)
