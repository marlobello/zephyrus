package com.zephyrus.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [SavedLocationEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class ZephyrusDatabase : RoomDatabase() {
    abstract fun savedLocationDao(): SavedLocationDao
}
