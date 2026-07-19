package com.example.nava.data.downloads

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [OfflineTrackEntity::class], version = 1, exportSchema = false)
abstract class NavaDatabase : RoomDatabase() {
    abstract fun offlineTrackDao(): OfflineTrackDao
}
