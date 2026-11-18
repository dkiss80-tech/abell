package com.example.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.alarm.ActiveRingingState
import com.example.alarm.BellAlarmScheduler
import com.example.alarm.BellPlaybackService
import com.example.alarm.BellRingingBus
import com.example.alarm.BellType
import com.example.alarm.NextBellCalculator
import com.example.alarm.NextBellGroup
import com.example.audio.BellAudioPlayer
import com.example.data.local.AppPreferences
import com.example.data.local.ThemeMode
import com.example.data.model.BellScheduleEntity
import com.example.data.model.BuiltInBellSounds
import com.example.data.model.CustomBellSoundEntity
import com.example.data.model.DayOfWeekBitmask
import com.example.data.repository.BellRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

data class ScheduleDraft(
    val editingId: Long = 0L,
    val title: String = "1교시",
    val startHour: Int = 14,
    val startMinute: Int = 0,
    val classDurationMinutes: Int = 50,
    val breakMinutes: Int = 10,
    val daysOfWeekMask: Int = DayOfWeekBitmask.WEEKDAYS,
    val isWeeklyRepeat: Boolean = true,
    val startBellEnabled: Boolean = true,
    val endBellEnabled: Boolean = true,
    val startBellSoundId: String = BuiltInBellSounds.BUILTIN_BRIGHT_START,
    val endBellSoundId: String = BuiltInBellSounds.BUILTIN_WARM_END,
    val ringDurationSeconds: Int = 8,
) {
    val startTotalMinutes: Int
        get() = startHour * 60 + startMinute

    val endTotalMinutes: Int
        get() = (startTotalMinutes + classDurationMinutes) % 1440

    val endHour: Int
        get() = endTotalMinutes / 60

    val endMinute: Int
        get() = endTotalMinutes % 60

    val nextStartTotalMinutes: Int
        get() = (endTotalMinutes + breakMinutes.coerceAtLeast(0)) % 1440

    val nextStartHour: Int
        get() = nextStartTotalMinutes / 60

    val nextStartMinute: Int
        get() = nextStartTotalMinutes % 60

    fun formattedStartTime(): String =
        String.format(Locale.getDefault(), "%02d:%02d", startHour, startMinute)

    fun formattedEndTime(): String =
        String.format(Locale.getDefault(), "%02d:%02d", endHour, endMinute)

    fun formattedNextStartTime(): String =
        String.format(Locale.getDefault(), "%02d:%02d", nextStartHour, nextStartMinute)
}

data class AcademyBellUiState(
    val nowMillis: Long = System.currentTimeMillis(),
    val selectedDayTab: Int = -1, // -1 means Today
    val displayedDayBit: Int = DayOfWeekBitmask.todayBit(),
    val daySchedules: List<BellScheduleEntity> = emptyList(),
    val allSchedules: List<BellScheduleEntity> = emptyList(),
    val customSounds: List<CustomBellSoundEntity> = emptyList(),
    val preferences: AppPreferences = AppPreferences(),
    val nextBellGroup: NextBellGroup? = null,
    val activeRinging: ActiveRingingState? = null,
    val previewPlayingSoundId: String? = null,
    val canScheduleExactAlarms: Boolean = true,
    val scheduleDraft: ScheduleDraft? = null,
    val snackbarMessage: String? = null,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val repository = BellRepository(appContext)
    private val previewPlayer = BellAudioPlayer(appContext)

    private val tickerFlow = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1000L)
        }
    }

    private val _selectedDayTab = MutableStateFlow(-1)
    private val _scheduleDraft = MutableStateFlow<ScheduleDraft?>(null)
    private val _previewPlayingSoundId = MutableStateFlow<String?>(null)
    private val _canScheduleExactAlarms = MutableStateFlow(BellAlarmScheduler.canScheduleExactAlarms(appContext))
    private val _snackbarMessage = MutableStateFlow<String?>(null)

    private data class CoreSnapshot(
        val schedules: List<BellScheduleEntity>,
        val customSounds: List<CustomBellSoundEntity>,
        val preferences: AppPreferences,
        val nowMillis: Long,
        val activeRinging: ActiveRingingState?,
    )

    private data class UiControlSnapshot(
        val selectedDayTab: Int,
        val scheduleDraft: ScheduleDraft?,
        val previewPlayingSoundId: String?,
        val canExact: Boolean,
        val snackbar: String?,
    )

    private val coreFlow = combine(
        repository.schedulesFlow,
        repository.customSoundsFlow,
        repository.preferencesFlow,
        tickerFlow,
        BellRingingBus.activeRinging,
    ) { schedules, customSounds, prefs, now, ringing ->
        CoreSnapshot(schedules, customSounds, prefs, now, ringing)
    }

    private val controlFlow = combine(
        _selectedDayTab,
        _scheduleDraft,
        _previewPlayingSoundId,
        _canScheduleExactAlarms,
        _snackbarMessage,
    ) { dayTab, draft, previewId, canExact, snackbar ->
        UiControlSnapshot(dayTab, draft, previewId, canExact, snackbar)
    }

    val uiState: StateFlow<AcademyBellUiState> = combine(coreFlow, controlFlow) { core, ctrl ->
        val todayBit = DayOfWeekBitmask.todayBit(core.nowMillis)
        val displayedBit = if (ctrl.selectedDayTab == -1) todayBit else ctrl.selectedDayTab
        val filtered = core.schedules
            .filter { DayOfWeekBitmask.hasDay(it.daysOfWeekMask, displayedBit) }
            .sortedWith(compareBy({ it.startTotalMinutes }, { it.id }))

        val nextGroup = NextBellCalculator.findNextBellGroup(
            schedules = core.schedules,
            nowMillis = core.nowMillis,
            masterEnabled = core.preferences.masterEnabled,
        )

        AcademyBellUiState(
            nowMillis = core.nowMillis,
            selectedDayTab = ctrl.selectedDayTab,
            displayedDayBit = displayedBit,
            daySchedules = filtered,
            allSchedules = core.schedules,
            customSounds = core.customSounds,
            preferences = core.preferences,
            nextBellGroup = nextGroup,
            activeRinging = core.activeRinging,
            previewPlayingSoundId = ctrl.previewPlayingSoundId,
            canScheduleExactAlarms = ctrl.canExact,
            scheduleDraft = ctrl.scheduleDraft,
            snackbarMessage = ctrl.snackbar,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AcademyBellUiState(),
    )

    init {
        viewModelScope.launch {
            BellAlarmScheduler.rescheduleNextAlarm(appContext, repository)
        }
    }

    fun refreshExactAlarmPermission() {
        _canScheduleExactAlarms.value = BellAlarmScheduler.canScheduleExactAlarms(appContext)
    }

    fun selectDayTab(dayTab: Int) {
        _selectedDayTab.value = dayTab
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun openNewScheduleDraft(chainFrom: BellScheduleEntity? = null) {
        val state = uiState.value
        val prefs = state.preferences

        if (chainFrom != null) {
            val (nextH, nextM) = chainFrom.nextChainedStartHourMinute()
            _scheduleDraft.value = ScheduleDraft(
                editingId = 0L,
                title = incrementPeriodTitle(chainFrom.title),
                startHour = nextH,
                startMinute = nextM,
                classDurationMinutes = chainFrom.classDurationMinutes,
                breakMinutes = chainFrom.breakMinutes,
                daysOfWeekMask = chainFrom.daysOfWeekMask,
                isWeeklyRepeat = chainFrom.isWeeklyRepeat,
                startBellEnabled = chainFrom.startBellEnabled,
                endBellEnabled = chainFrom.endBellEnabled,
                startBellSoundId = chainFrom.startBellSoundId,
                endBellSoundId = chainFrom.endBellSoundId,
                ringDurationSeconds = chainFrom.ringDurationSeconds,
            )
            return
        }

        val lastOnDay = state.daySchedules.lastOrNull()
        if (lastOnDay != null) {
            val (nextH, nextM) = lastOnDay.nextChainedStartHourMinute()
            _scheduleDraft.value = ScheduleDraft(
                editingId = 0L,
                title = incrementPeriodTitle(lastOnDay.title),
                startHour = nextH,
                startMinute = nextM,
                classDurationMinutes = lastOnDay.classDurationMinutes,
                breakMinutes = lastOnDay.breakMinutes,
                daysOfWeekMask = lastOnDay.daysOfWeekMask,
                isWeeklyRepeat = lastOnDay.isWeeklyRepeat,
                startBellEnabled = true,
                endBellEnabled = true,
                startBellSoundId = prefs.defaultStartSoundId,
                endBellSoundId = prefs.defaultEndSoundId,
                ringDurationSeconds = prefs.defaultRingDurationSeconds,
            )
        } else {
            val defaultMask = if (state.selectedDayTab == -1) {
                DayOfWeekBitmask.WEEKDAYS
            } else {
                state.selectedDayTab
            }
            _scheduleDraft.value = ScheduleDraft(
                editingId = 0L,
                title = "1교시",
                startHour = 14,
                startMinute = 0,
                classDurationMinutes = prefs.defaultClassMinutes,
                breakMinutes = prefs.defaultBreakMinutes,
                daysOfWeekMask = defaultMask,
                isWeeklyRepeat = true,
                startBellEnabled = true,
                endBellEnabled = true,
                startBellSoundId = prefs.defaultStartSoundId,
                endBellSoundId = prefs.defaultEndSoundId,
                ringDurationSeconds = prefs.defaultRingDurationSeconds,
            )
        }
    }

    fun openEditScheduleDraft(schedule: BellScheduleEntity) {
        _scheduleDraft.value = ScheduleDraft(
            editingId = schedule.id,
            title = schedule.title,
            startHour = schedule.startHour,
            startMinute = schedule.startMinute,
            classDurationMinutes = schedule.classDurationMinutes,
            breakMinutes = schedule.breakMinutes,
            daysOfWeekMask = schedule.daysOfWeekMask,
            isWeeklyRepeat = schedule.isWeeklyRepeat,
            startBellEnabled = schedule.startBellEnabled,
            endBellEnabled = schedule.endBellEnabled,
            startBellSoundId = schedule.startBellSoundId,
            endBellSoundId = schedule.endBellSoundId,
            ringDurationSeconds = schedule.ringDurationSeconds,
        )
    }

    fun updateDraft(transform: (ScheduleDraft) -> ScheduleDraft) {
        _scheduleDraft.value = _scheduleDraft.value?.let(transform)
    }

    fun dismissDraft() {
        _scheduleDraft.value = null
    }

    fun saveDraft(andChainNext: Boolean) {
        val draft = _scheduleDraft.value ?: return
        if (draft.daysOfWeekMask == 0) {
            _snackbarMessage.value = "최소 하나의 요일을 선택해 주세요."
            return
        }
        if (!draft.startBellEnabled && !draft.endBellEnabled) {
            _snackbarMessage.value = "시작종 또는 종료종 중 최소 하나를 켜 주세요."
            return
        }

        val entity = BellScheduleEntity(
            id = draft.editingId,
            title = draft.title.trim().ifEmpty { "수업" },
            startHour = draft.startHour,
            startMinute = draft.startMinute,
            endHour = draft.endHour,
            endMinute = draft.endMinute,
            breakMinutes = draft.breakMinutes.coerceAtLeast(0),
            daysOfWeekMask = draft.daysOfWeekMask,
            isWeeklyRepeat = draft.isWeeklyRepeat,
            isEnabled = true,
            startBellEnabled = draft.startBellEnabled,
            endBellEnabled = draft.endBellEnabled,
            startBellSoundId = draft.startBellSoundId,
            endBellSoundId = draft.endBellSoundId,
            ringDurationSeconds = draft.ringDurationSeconds,
        )

        viewModelScope.launch {
            repository.saveSchedule(entity)
            BellAlarmScheduler.rescheduleNextAlarm(appContext, repository)

            if (andChainNext) {
                val nextTitle = incrementPeriodTitle(entity.title)
                val nextStartTimeLabel = draft.formattedNextStartTime()
                _scheduleDraft.value = draft.copy(
                    editingId = 0L,
                    title = nextTitle,
                    startHour = draft.nextStartHour,
                    startMinute = draft.nextStartMinute,
                )
                _snackbarMessage.value = "'${entity.title}' 저장 완료 · 다음 '$nextTitle' (${nextStartTimeLabel}) 바로 추가"
            } else {
                _scheduleDraft.value = null
                _snackbarMessage.value = "'${entity.title}' 시간표가 저장되었습니다."
            }
        }
    }

    fun toggleScheduleEnabled(schedule: BellScheduleEntity) {
        viewModelScope.launch {
            repository.setScheduleEnabled(schedule.id, !schedule.isEnabled)
            BellAlarmScheduler.rescheduleNextAlarm(appContext, repository)
        }
    }

    fun deleteSchedule(schedule: BellScheduleEntity) {
        viewModelScope.launch {
            repository.deleteSchedule(schedule.id)
            BellAlarmScheduler.rescheduleNextAlarm(appContext, repository)
            _snackbarMessage.value = "'${schedule.title}' 스케줄을 삭제했습니다."
        }
    }

    fun setMasterEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setMasterEnabled(enabled)
            BellAlarmScheduler.rescheduleNextAlarm(appContext, repository)
            _snackbarMessage.value = if (enabled) {
                "학원 종소리 스케줄이 활성화되었습니다."
            } else {
                "모든 종소리가 일시정지되었습니다."
            }
        }
    }

    fun importCustomSound(uri: Uri, applyToDraftBellType: BellType? = null) {
        viewModelScope.launch {
            val result = repository.importCustomSound(uri)
            result.onSuccess { imported ->
                _snackbarMessage.value = "'${imported.displayName}' 음원이 보관함에 추가되었습니다."
                if (applyToDraftBellType != null) {
                    updateDraft { draft ->
                        when (applyToDraftBellType) {
                            BellType.START -> draft.copy(startBellSoundId = imported.id)
                            BellType.END -> draft.copy(endBellSoundId = imported.id)
                        }
                    }
                }
            }.onFailure { err ->
                _snackbarMessage.value = "음원 업로드 실패: ${err.message ?: "지원되지 않는 파일"}"
            }
        }
    }

    fun renameCustomSound(id: String, newName: String) {
        viewModelScope.launch {
            repository.renameCustomSound(id, newName)
            _snackbarMessage.value = "음원 이름이 변경되었습니다."
        }
    }

    fun deleteCustomSound(sound: CustomBellSoundEntity) {
        viewModelScope.launch {
            if (_previewPlayingSoundId.value == sound.id) {
                stopPreview()
            }
            repository.deleteCustomSound(sound)
            BellAlarmScheduler.rescheduleNextAlarm(appContext, repository)
            _snackbarMessage.value = "'${sound.displayName}' 음원을 삭제했습니다."
        }
    }

    fun togglePreviewSound(soundId: String, forceMaxVolume: Boolean = false) {
        if (_previewPlayingSoundId.value == soundId && !forceMaxVolume) {
            stopPreview()
            return
        }
        viewModelScope.launch {
            val custom = repository.getCustomSoundByIdSync(soundId)
            val prefs = repository.getPreferencesSync()
            _previewPlayingSoundId.value = soundId
            previewPlayer.playBell(
                soundId = soundId,
                customFilePath = custom?.localFilePath,
                forceMaxVolume = forceMaxVolume,
                restoreVolumeAfter = prefs.restoreVolumeAfterRing,
                loopContinuously = false,
                onCompleted = {
                    if (_previewPlayingSoundId.value == soundId) {
                        _previewPlayingSoundId.value = null
                    }
                },
            )
        }
    }

    fun stopPreview() {
        previewPlayer.stop()
        _previewPlayingSoundId.value = null
    }

    fun triggerMaxVolumeTestBell(soundId: String, title: String = "테스트 종소리", bellType: BellType = BellType.START) {
        stopPreview()
        val intent = Intent(appContext, BellPlaybackService::class.java).apply {
            action = BellPlaybackService.ACTION_START_BELL
            putExtra(BellPlaybackService.EXTRA_TITLE, title)
            putExtra(BellPlaybackService.EXTRA_BELL_TYPE, bellType.name)
            putExtra(BellPlaybackService.EXTRA_SOUND_ID, soundId)
            putExtra(BellPlaybackService.EXTRA_DURATION_SEC, 5)
        }
        ContextCompat.startForegroundService(appContext, intent)
    }

    fun stopActiveRinging() {
        BellPlaybackService.stopService(appContext)
    }

    fun updateDefaultClassMinutes(minutes: Int) {
        viewModelScope.launch { repository.setDefaultClassMinutes(minutes) }
    }

    fun updateDefaultBreakMinutes(minutes: Int) {
        viewModelScope.launch { repository.setDefaultBreakMinutes(minutes) }
    }

    fun updateDefaultStartSoundId(soundId: String) {
        viewModelScope.launch { repository.setDefaultStartSoundId(soundId) }
    }

    fun updateDefaultEndSoundId(soundId: String) {
        viewModelScope.launch { repository.setDefaultEndSoundId(soundId) }
    }

    fun updateDefaultRingDurationSeconds(seconds: Int) {
        viewModelScope.launch { repository.setDefaultRingDurationSeconds(seconds) }
    }

    fun updateForceMaxVolume(force: Boolean) {
        viewModelScope.launch { repository.setForceMaxVolume(force) }
    }

    fun updateRestoreVolumeAfterRing(restore: Boolean) {
        viewModelScope.launch { repository.setRestoreVolumeAfterRing(restore) }
    }

    fun updateVibrateOnBell(vibrate: Boolean) {
        viewModelScope.launch { repository.setVibrateOnBell(vibrate) }
    }

    fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    override fun onCleared() {
        previewPlayer.stop()
        super.onCleared()
    }

    companion object {
        fun incrementPeriodTitle(currentTitle: String): String {
            val trimmed = currentTitle.trim()
            val match = Regex("^(.*?)(\\d+)(\\D*)$").find(trimmed)
            return if (match != null) {
                val prefix = match.groupValues[1]
                val num = match.groupValues[2].toIntOrNull() ?: 1
                val suffix = match.groupValues[3]
                "$prefix${num + 1}$suffix"
            } else {
                if (trimmed.isEmpty()) "2교시" else "$trimmed 2"
            }
        }
    }
}
