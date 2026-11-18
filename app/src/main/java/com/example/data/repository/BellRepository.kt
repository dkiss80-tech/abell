package com.example.data.repository

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.local.AppDatabase
import com.example.data.local.AppPreferences
import com.example.data.local.AppPreferencesDataStore
import com.example.data.local.ThemeMode
import com.example.data.model.BellScheduleEntity
import com.example.data.model.BuiltInBellSounds
import com.example.data.model.CustomBellSoundEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class BellRepository(private val context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val scheduleDao = db.bellScheduleDao()
    private val customSoundDao = db.customBellSoundDao()
    private val prefsStore = AppPreferencesDataStore(context)

    val schedulesFlow: Flow<List<BellScheduleEntity>> = scheduleDao.observeAllSchedules()
    val customSoundsFlow: Flow<List<CustomBellSoundEntity>> = customSoundDao.observeAllCustomSounds()
    val preferencesFlow: Flow<AppPreferences> = prefsStore.preferencesFlow

    suspend fun getAllSchedulesSync(): List<BellScheduleEntity> = scheduleDao.getAllSchedulesSync()
    suspend fun getScheduleByIdSync(id: Long): BellScheduleEntity? = scheduleDao.getByIdSync(id)
    suspend fun getAllCustomSoundsSync(): List<CustomBellSoundEntity> = customSoundDao.getAllCustomSoundsSync()
    suspend fun getCustomSoundByIdSync(id: String): CustomBellSoundEntity? = customSoundDao.getByIdSync(id)
    suspend fun getPreferencesSync(): AppPreferences = prefsStore.getPreferencesSync()

    suspend fun saveSchedule(schedule: BellScheduleEntity): Long {
        return if (schedule.id == 0L) {
            scheduleDao.insertSchedule(schedule)
        } else {
            scheduleDao.updateSchedule(schedule)
            schedule.id
        }
    }

    suspend fun deleteSchedule(id: Long) = scheduleDao.deleteScheduleById(id)

    suspend fun setScheduleEnabled(id: Long, enabled: Boolean) =
        scheduleDao.setEnabled(id, enabled)

    suspend fun updateScheduleDaysAndEnabled(id: Long, mask: Int, enabled: Boolean) =
        scheduleDao.updateDaysAndEnabled(id, mask, enabled)

    suspend fun setMasterEnabled(enabled: Boolean) = prefsStore.setMasterEnabled(enabled)
    suspend fun setDefaultClassMinutes(minutes: Int) = prefsStore.setDefaultClassMinutes(minutes)
    suspend fun setDefaultBreakMinutes(minutes: Int) = prefsStore.setDefaultBreakMinutes(minutes)
    suspend fun setDefaultStartSoundId(soundId: String) = prefsStore.setDefaultStartSoundId(soundId)
    suspend fun setDefaultEndSoundId(soundId: String) = prefsStore.setDefaultEndSoundId(soundId)
    suspend fun setDefaultRingDurationSeconds(seconds: Int) = prefsStore.setDefaultRingDurationSeconds(seconds)
    suspend fun setForceMaxVolume(force: Boolean) = prefsStore.setForceMaxVolume(force)
    suspend fun setRestoreVolumeAfterRing(restore: Boolean) = prefsStore.setRestoreVolumeAfterRing(restore)
    suspend fun setVibrateOnBell(vibrate: Boolean) = prefsStore.setVibrateOnBell(vibrate)
    suspend fun setThemeMode(mode: ThemeMode) = prefsStore.setThemeMode(mode)

    suspend fun importCustomSound(uri: Uri): Result<CustomBellSoundEntity> = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val mimeType = resolver.getType(uri) ?: "audio/mpeg"
            val originalName = queryDisplayName(uri) ?: "업로드 음원"
            val cleanBaseName = originalName.substringBeforeLast('.').ifBlank { "업로드 음원" }
            val ext = resolveExtension(originalName, mimeType)

            val soundsDir = File(context.filesDir, "bell_sounds").apply { mkdirs() }
            val id = "custom_${UUID.randomUUID().toString().take(8)}"
            val destFile = File(soundsDir, "$id.$ext")

            resolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            } ?: error("선택한 오디오 파일을 읽을 수 없습니다.")

            if (!destFile.exists() || destFile.length() == 0L) {
                destFile.delete()
                error("비어 있는 오디오 파일입니다.")
            }

            val durationMs = extractAudioDuration(destFile.absolutePath)
            val entity = CustomBellSoundEntity(
                id = id,
                displayName = cleanBaseName,
                localFilePath = destFile.absolutePath,
                durationMillis = durationMs,
                mimeType = mimeType,
            )
            customSoundDao.insertCustomSound(entity)
            entity
        }
    }

    suspend fun renameCustomSound(id: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isNotEmpty()) {
            customSoundDao.renameCustomSound(id, trimmed)
        }
    }

    suspend fun deleteCustomSound(sound: CustomBellSoundEntity) = withContext(Dispatchers.IO) {
        customSoundDao.deleteCustomSoundById(sound.id)
        scheduleDao.resetStartSoundReferences(sound.id, BuiltInBellSounds.BUILTIN_BRIGHT_START)
        scheduleDao.resetEndSoundReferences(sound.id, BuiltInBellSounds.BUILTIN_WARM_END)
        val prefs = prefsStore.getPreferencesSync()
        if (prefs.defaultStartSoundId == sound.id) {
            prefsStore.setDefaultStartSoundId(BuiltInBellSounds.BUILTIN_BRIGHT_START)
        }
        if (prefs.defaultEndSoundId == sound.id) {
            prefsStore.setDefaultEndSoundId(BuiltInBellSounds.BUILTIN_WARM_END)
        }
        runCatching { File(sound.localFilePath).delete() }
        Unit
    }

    private fun queryDisplayName(uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) cursor.getString(idx) else null
                    } else null
                }
        }.getOrNull()
    }

    private fun resolveExtension(fileName: String, mimeType: String): String {
        val fromName = fileName.substringAfterLast('.', "").lowercase()
        if (fromName in setOf("mp3", "wav", "m4a", "ogg", "aac", "flac")) return fromName
        return when {
            mimeType.contains("wav") -> "wav"
            mimeType.contains("ogg") -> "ogg"
            mimeType.contains("mp4") || mimeType.contains("m4a") || mimeType.contains("aac") -> "m4a"
            mimeType.contains("flac") -> "flac"
            else -> "mp3"
        }
    }

    private fun extractAudioDuration(filePath: String): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(filePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } catch (_: Exception) {
            0L
        } finally {
            runCatching { retriever.release() }
        }
    }
}
