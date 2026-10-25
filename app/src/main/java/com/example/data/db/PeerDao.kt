package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.SavedPeer
import kotlinx.coroutines.flow.Flow

@Dao
interface PeerDao {
    @Query("SELECT * FROM saved_peers ORDER BY lastConnectedTimestamp DESC")
    fun getAllSavedPeers(): Flow<List<SavedPeer>>

    @Query("SELECT * FROM saved_peers WHERE deviceId = :deviceId LIMIT 1")
    suspend fun getPeerByDeviceId(deviceId: String): SavedPeer?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePeer(peer: SavedPeer)

    @Delete
    suspend fun deletePeer(peer: SavedPeer)

    @Query("DELETE FROM saved_peers WHERE deviceId = :deviceId")
    suspend fun deletePeerById(deviceId: String)
}
