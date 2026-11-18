package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.BellScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BellScheduleDao {
    @Query("SELECT * FROM bell_schedules ORDER BY startHour ASC, startMinute ASC, id ASC")
    fun observeAllSchedules(): Flow<List<BellScheduleEntity>>

    @Query("SELECT * FROM bell_schedules ORDER BY startHour ASC, startMinute ASC, id ASC")
    suspend fun getAllSchedulesSync(): List<BellScheduleEntity>

    @Query("SELECT * FROM bell_schedules WHERE id = :id LIMIT 1")
    suspend fun getByIdSync(id: Long): BellScheduleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: BellScheduleEntity): Long

    @Update
    suspend fun updateSchedule(schedule: BellScheduleEntity)

    @Query("DELETE FROM bell_schedules WHERE id = :id")
    suspend fun deleteScheduleById(id: Long)

    @Query("UPDATE bell_schedules SET isEnabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("UPDATE bell_schedules SET daysOfWeekMask = :mask, isEnabled = :enabled WHERE id = :id")
    suspend fun updateDaysAndEnabled(id: Long, mask: Int, enabled: Boolean)

    @Query("UPDATE bell_schedules SET startBellSoundId = :fallbackSoundId WHERE startBellSoundId = :deletedSoundId")
    suspend fun resetStartSoundReferences(deletedSoundId: String, fallbackSoundId: String)

    @Query("UPDATE bell_schedules SET endBellSoundId = :fallbackSoundId WHERE endBellSoundId = :deletedSoundId")
    suspend fun resetEndSoundReferences(deletedSoundId: String, fallbackSoundId: String)
}
