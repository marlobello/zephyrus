package com.zephyrus.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedLocationDao {

    @Query("SELECT * FROM saved_locations ORDER BY name ASC")
    fun getAll(): Flow<List<SavedLocationEntity>>

    @Query("SELECT * FROM saved_locations WHERE id = :id")
    suspend fun getById(id: Long): SavedLocationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(location: SavedLocationEntity): Long

    @Delete
    suspend fun delete(location: SavedLocationEntity)

    @Query("DELETE FROM saved_locations WHERE id = :id")
    suspend fun deleteById(id: Long)
}
