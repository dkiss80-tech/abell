package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Locale

@Entity(tableName = "custom_bell_sounds")
data class CustomBellSoundEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val localFilePath: String,
    val durationMillis: Long = 0L,
    val mimeType: String = "audio/*",
    val createdAt: Long = System.currentTimeMillis(),
) {
    fun formattedDuration(): String {
        if (durationMillis <= 0L) return "사용자 음원"
        val totalSec = (durationMillis / 1000L).toInt().coerceAtLeast(1)
        val min = totalSec / 60
        val sec = totalSec % 60
        return if (min > 0) {
            String.format(Locale.getDefault(), "%d분 %02d초", min, sec)
        } else {
            "${sec}초"
        }
    }
}
