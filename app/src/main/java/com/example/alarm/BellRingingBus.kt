package com.example.alarm

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ActiveRingingState(
    val scheduleTitle: String,
    val bellType: BellType,
    val soundName: String,
    val totalDurationSeconds: Int,
    val remainingSeconds: Int,
    val isManualDismissOnly: Boolean,
)

object BellRingingBus {
    private val _activeRinging = MutableStateFlow<ActiveRingingState?>(null)
    val activeRinging: StateFlow<ActiveRingingState?> = _activeRinging.asStateFlow()

    fun update(state: ActiveRingingState?) {
        _activeRinging.value = state
    }
}
