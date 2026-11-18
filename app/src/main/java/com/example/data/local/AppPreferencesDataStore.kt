package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.BuiltInBellSounds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class ThemeMode(val label: String) {
    SYSTEM("시스템 설정"),
    LIGHT("라이트 모드"),
    DARK("다크 모드"),
}

data class AppPreferences(
    val masterEnabled: Boolean = true,
    val defaultClassMinutes: Int = 50,
    val defaultBreakMinutes: Int = 10,
    val defaultStartSoundId: String = BuiltInBellSounds.BUILTIN_BRIGHT_START,
    val defaultEndSoundId: String = BuiltInBellSounds.BUILTIN_WARM_END,
    val defaultRingDurationSeconds: Int = 8,
    val forceMaxVolume: Boolean = true,
    val restoreVolumeAfterRing: Boolean = true,
    val vibrateOnBell: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "academy_bell_prefs")

class AppPreferencesDataStore(private val context: Context) {

    private object Keys {
        val MASTER_ENABLED = booleanPreferencesKey("master_enabled")
        val DEFAULT_CLASS_MINUTES = intPreferencesKey("default_class_minutes")
        val DEFAULT_BREAK_MINUTES = intPreferencesKey("default_break_minutes")
        val DEFAULT_START_SOUND_ID = stringPreferencesKey("default_start_sound_id")
        val DEFAULT_END_SOUND_ID = stringPreferencesKey("default_end_sound_id")
        val DEFAULT_RING_DURATION_SEC = intPreferencesKey("default_ring_duration_sec")
        val FORCE_MAX_VOLUME = booleanPreferencesKey("force_max_volume")
        val RESTORE_VOLUME = booleanPreferencesKey("restore_volume")
        val VIBRATE_ON_BELL = booleanPreferencesKey("vibrate_on_bell")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    val preferencesFlow: Flow<AppPreferences> = context.dataStore.data.map { prefs ->
        AppPreferences(
            masterEnabled = prefs[Keys.MASTER_ENABLED] ?: true,
            defaultClassMinutes = prefs[Keys.DEFAULT_CLASS_MINUTES] ?: 50,
            defaultBreakMinutes = prefs[Keys.DEFAULT_BREAK_MINUTES] ?: 10,
            defaultStartSoundId = prefs[Keys.DEFAULT_START_SOUND_ID] ?: BuiltInBellSounds.BUILTIN_BRIGHT_START,
            defaultEndSoundId = prefs[Keys.DEFAULT_END_SOUND_ID] ?: BuiltInBellSounds.BUILTIN_WARM_END,
            defaultRingDurationSeconds = prefs[Keys.DEFAULT_RING_DURATION_SEC] ?: 8,
            forceMaxVolume = prefs[Keys.FORCE_MAX_VOLUME] ?: true,
            restoreVolumeAfterRing = prefs[Keys.RESTORE_VOLUME] ?: true,
            vibrateOnBell = prefs[Keys.VIBRATE_ON_BELL] ?: true,
            themeMode = runCatching {
                ThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name)
            }.getOrDefault(ThemeMode.SYSTEM),
        )
    }

    suspend fun getPreferencesSync(): AppPreferences = preferencesFlow.first()

    suspend fun setMasterEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.MASTER_ENABLED] = enabled }
    }

    suspend fun setDefaultClassMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.DEFAULT_CLASS_MINUTES] = minutes.coerceIn(5, 360) }
    }

    suspend fun setDefaultBreakMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.DEFAULT_BREAK_MINUTES] = minutes.coerceIn(0, 180) }
    }

    suspend fun setDefaultStartSoundId(soundId: String) {
        context.dataStore.edit { it[Keys.DEFAULT_START_SOUND_ID] = soundId }
    }

    suspend fun setDefaultEndSoundId(soundId: String) {
        context.dataStore.edit { it[Keys.DEFAULT_END_SOUND_ID] = soundId }
    }

    suspend fun setDefaultRingDurationSeconds(seconds: Int) {
        context.dataStore.edit { it[Keys.DEFAULT_RING_DURATION_SEC] = seconds }
    }

    suspend fun setForceMaxVolume(force: Boolean) {
        context.dataStore.edit { it[Keys.FORCE_MAX_VOLUME] = force }
    }

    suspend fun setRestoreVolumeAfterRing(restore: Boolean) {
        context.dataStore.edit { it[Keys.RESTORE_VOLUME] = restore }
    }

    suspend fun setVibrateOnBell(vibrate: Boolean) {
        context.dataStore.edit { it[Keys.VIBRATE_ON_BELL] = vibrate }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }
}
