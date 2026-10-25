package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.SavedPeer
import com.example.data.model.UserSettings

@Database(
    entities = [SavedPeer::class, UserSettings::class],
    version = 1,
    exportSchema = false
)
abstract class NearbyDatabase : RoomDatabase() {
    abstract fun peerDao(): PeerDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: NearbyDatabase? = null

        fun getInstance(context: Context): NearbyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NearbyDatabase::class.java,
                    "nearby_talk.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
