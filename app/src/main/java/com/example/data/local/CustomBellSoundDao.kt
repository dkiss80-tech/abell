package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.CustomBellSoundEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomBellSoundDao {
    @Query("SELECT * FROM custom_bell_sounds ORDER BY createdAt DESC")
    fun observeAllCustomSounds(): Flow<List<CustomBellSoundEntity>>

    @Query("SELECT * FROM custom_bell_sounds ORDER BY createdAt DESC")
    suspend fun getAllCustomSoundsSync(): List<CustomBellSoundEntity>

    @Query("SELECT * FROM custom_bell_sounds WHERE id = :id LIMIT 1")
    suspend fun getByIdSync(id: String): CustomBellSoundEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomSound(sound: CustomBellSoundEntity)

    @Query("UPDATE custom_bell_sounds SET displayName = :newName WHERE id = :id")
    suspend fun renameCustomSound(id: String, newName: String)

    @Query("DELETE FROM custom_bell_sounds WHERE id = :id")
    suspend fun deleteCustomSoundById(id: String)
}
