package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.alarm.BellType
import com.example.data.model.BuiltInBellSounds
import com.example.data.model.CustomBellSoundEntity
import com.example.data.model.DayOfWeekBitmask
import com.example.ui.ScheduleDraft
import com.example.ui.theme.JetBrainsMonoFamily

private val CLASS_DURATION_PRESETS = listOf(40, 45, 50, 60, 80, 90)
private val BREAK_DURATION_PRESETS = listOf(0, 5, 10, 15, 20)
private val RING_DURATION_OPTIONS = listOf(5 to "5초", 8 to "8초", 10 to "10초", 15 to "15초", 30 to "30초", 0 to "수동 종료")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChainedScheduleSheet(
    draft: ScheduleDraft,
    customSounds: List<CustomBellSoundEntity>,
    previewPlayingSoundId: String?,
    onUpdateDraft: ((ScheduleDraft) -> ScheduleDraft) -> Unit,
    onDismiss: () -> Unit,
    onSave: (andChainNext: Boolean) -> Unit,
    onTogglePreview: (String) -> Unit,
    onUploadCustomSoundForBell: (BellType) -> Unit,
) {
    val useInContainerSheet = LocalInPlaceOverlayHost.current != null
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var customMinutesTarget by remember { mutableStateOf<String?>(null) } // "class" or "break"
    var soundPickerTarget by remember { mutableStateOf<BellType?>(null) }

    val sheetBody: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (useInContainerSheet) Modifier else Modifier.navigationBarsPadding().imePadding()),
        ) {
            // Sheet Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (draft.editingId == 0L) "새 교시 스케줄 추가" else "교시 스케줄 수정",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "닫기")
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                // 1. Period Name
                OutlinedTextField(
                    value = draft.title,
                    onValueChange = { newText -> onUpdateDraft { it.copy(title = newText) } },
                    label = { Text("교시 / 수업 이름") },
                    placeholder = { Text("예: 1교시, 중등 수학반") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("schedule_title_input"),
                    shape = RoundedCornerShape(14.dp),
                )

                // 2. Start & End Time Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TimeDisplayCard(
                        label = "시작종 시각",
                        timeText = draft.formattedStartTime(),
                        accentColor = MaterialTheme.colorScheme.secondary,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        onClick = { showStartTimePicker = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("start_time_picker_button"),
                    )
                    TimeDisplayCard(
                        label = "종료종 시각",
                        timeText = draft.formattedEndTime(),
                        accentColor = MaterialTheme.colorScheme.tertiary,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                        onClick = { showEndTimePicker = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("end_time_picker_button"),
                    )
                }

                // 3. Class Duration Chips (40, 45, 50, 60, 80, 90 + custom)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "수업 시간 (종료종 자동 계산)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        CLASS_DURATION_PRESETS.forEach { mins ->
                            FilterChip(
                                selected = draft.classDurationMinutes == mins,
                                onClick = { onUpdateDraft { it.copy(classDurationMinutes = mins) } },
                                label = { Text("${mins}분") },
                                modifier = Modifier.testTag("class_duration_${mins}"),
                            )
                        }
                        val isCustomClass = draft.classDurationMinutes !in CLASS_DURATION_PRESETS
                        FilterChip(
                            selected = isCustomClass,
                            onClick = { customMinutesTarget = "class" },
                            label = {
                                Text(if (isCustomClass) "${draft.classDurationMinutes}분 (직접)" else "직접 입력")
                            },
                        )
                    }
                }

                // 4. Break Duration Chips + Next Chained Start Preview
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "쉬는 시간 (다음 교시 자동 연결)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        BREAK_DURATION_PRESETS.forEach { mins ->
                            FilterChip(
                                selected = draft.breakMinutes == mins,
                                onClick = { onUpdateDraft { it.copy(breakMinutes = mins) } },
                                label = { Text(if (mins == 0) "없음(0분)" else "${mins}분") },
                                modifier = Modifier.testTag("break_duration_${mins}"),
                            )
                        }
                        val isCustomBreak = draft.breakMinutes !in BREAK_DURATION_PRESETS
                        FilterChip(
                            selected = isCustomBreak,
                            onClick = { customMinutesTarget = "break" },
                            label = {
                                Text(if (isCustomBreak) "${draft.breakMinutes}분 (직접)" else "직접 입력")
                            },
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "다음 교시 이어 추가 시 ${draft.formattedNextStartTime()} 시작 (종료 ${draft.formattedEndTime()} + 쉬는 시간 ${draft.breakMinutes}분)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }

                // 5. Days of Week + Weekly Repeat
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "요일 선택",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            QuickDayPresetChip(
                                label = "평일",
                                selected = draft.daysOfWeekMask == DayOfWeekBitmask.WEEKDAYS,
                                onClick = { onUpdateDraft { it.copy(daysOfWeekMask = DayOfWeekBitmask.WEEKDAYS) } },
                            )
                            QuickDayPresetChip(
                                label = "주말",
                                selected = draft.daysOfWeekMask == DayOfWeekBitmask.WEEKEND,
                                onClick = { onUpdateDraft { it.copy(daysOfWeekMask = DayOfWeekBitmask.WEEKEND) } },
                            )
                            QuickDayPresetChip(
                                label = "매일",
                                selected = draft.daysOfWeekMask == DayOfWeekBitmask.ALL_DAYS,
                                onClick = { onUpdateDraft { it.copy(daysOfWeekMask = DayOfWeekBitmask.ALL_DAYS) } },
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        DayOfWeekBitmask.orderedDays.forEach { dayItem ->
                            val isSelected = DayOfWeekBitmask.hasDay(draft.daysOfWeekMask, dayItem.bit)
                            Surface(
                                onClick = {
                                    onUpdateDraft {
                                        it.copy(daysOfWeekMask = DayOfWeekBitmask.toggleDay(it.daysOfWeekMask, dayItem.bit))
                                    }
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .minimumInteractiveComponentSize()
                                    .testTag("day_chip_${dayItem.shortLabel}"),
                                shape = CircleShape,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = dayItem.shortLabel,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                    )
                                }
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Repeat,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "매주 반복 설정",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        text = if (draft.isWeeklyRepeat) {
                                            "선택한 요일마다 매주 자동으로 울립니다"
                                        } else {
                                            "이번 1회만 울린 뒤 자동으로 해제됩니다"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Switch(
                                checked = draft.isWeeklyRepeat,
                                onCheckedChange = { checked ->
                                    onUpdateDraft { it.copy(isWeeklyRepeat = checked) }
                                },
                                modifier = Modifier.testTag("weekly_repeat_switch"),
                            )
                        }
                    }
                }

                // 6. Start & End Bell Sound Selection
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "시작종 · 종료종 음원 및 알림",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )

                    BellSoundConfigRow(
                        label = "시작종 (${draft.formattedStartTime()})",
                        enabled = draft.startBellEnabled,
                        soundName = BuiltInBellSounds.findPresetName(draft.startBellSoundId, customSounds),
                        onToggleEnabled = { enabled ->
                            onUpdateDraft { it.copy(startBellEnabled = enabled) }
                        },
                        onSelectSound = { soundPickerTarget = BellType.START },
                    )

                    BellSoundConfigRow(
                        label = "종료종 (${draft.formattedEndTime()})",
                        enabled = draft.endBellEnabled,
                        soundName = BuiltInBellSounds.findPresetName(draft.endBellSoundId, customSounds),
                        onToggleEnabled = { enabled ->
                            onUpdateDraft { it.copy(endBellEnabled = enabled) }
                        },
                        onSelectSound = { soundPickerTarget = BellType.END },
                    )
                }

                // 7. Ring Duration Options
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "종소리 울림 시간 (최대 음량)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        RING_DURATION_OPTIONS.forEach { (sec, label) ->
                            FilterChip(
                                selected = draft.ringDurationSeconds == sec,
                                onClick = { onUpdateDraft { it.copy(ringDurationSeconds = sec) } },
                                label = { Text(label) },
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Sticky Dual Action Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = { onSave(false) },
                    modifier = Modifier
                        .weight(0.9f)
                        .height(50.dp)
                        .testTag("save_schedule_button"),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("저장")
                }
                Button(
                    onClick = { onSave(true) },
                    modifier = Modifier
                        .weight(1.4f)
                        .height(50.dp)
                        .testTag("save_and_chain_button"),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(
                        text = "저장 후 다음 추가 (${draft.formattedNextStartTime()})",
                        maxLines = 1,
                    )
                }
            }
        }
    }

    if (useInContainerSheet) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth()
                    .fillMaxHeight(0.95f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    ),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 12.dp,
            ) {
                sheetBody()
            }
        }
    } else {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            sheetMaxWidth = 680.dp,
            dragHandle = null,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            sheetBody()
        }
    }

    if (showStartTimePicker) {
        TimePickerDialogWrapper(
            title = "시작종 시각 선택",
            initialHour = draft.startHour,
            initialMinute = draft.startMinute,
            onDismiss = { showStartTimePicker = false },
            onConfirm = { h, m ->
                onUpdateDraft { it.copy(startHour = h, startMinute = m) }
                showStartTimePicker = false
            },
        )
    }

    if (showEndTimePicker) {
        TimePickerDialogWrapper(
            title = "종료종 시각 직접 선택",
            initialHour = draft.endHour,
            initialMinute = draft.endMinute,
            onDismiss = { showEndTimePicker = false },
            onConfirm = { h, m ->
                val endTotal = h * 60 + m
                val diff = (endTotal - draft.startTotalMinutes + 1440) % 1440
                val validDuration = if (diff == 0) 50 else diff
                onUpdateDraft { it.copy(classDurationMinutes = validDuration) }
                showEndTimePicker = false
            },
        )
    }

    customMinutesTarget?.let { target ->
        val isClass = target == "class"
        CustomMinutesInputDialog(
            title = if (isClass) "수업 시간(분) 직접 입력" else "쉬는 시간(분) 직접 입력",
            initialValue = if (isClass) draft.classDurationMinutes else draft.breakMinutes,
            minValue = if (isClass) 5 else 0,
            maxValue = 360,
            onDismiss = { customMinutesTarget = null },
            onConfirm = { mins ->
                onUpdateDraft {
                    if (isClass) it.copy(classDurationMinutes = mins) else it.copy(breakMinutes = mins)
                }
                customMinutesTarget = null
            },
        )
    }

    soundPickerTarget?.let { bellType ->
        val selectedId = if (bellType == BellType.START) draft.startBellSoundId else draft.endBellSoundId
        SoundSelectionDialog(
            title = "${bellType.label} 음원 선택",
            selectedSoundId = selectedId,
            customSounds = customSounds,
            previewPlayingSoundId = previewPlayingSoundId,
            onSelectSound = { chosenId ->
                onUpdateDraft {
                    if (bellType == BellType.START) {
                        it.copy(startBellSoundId = chosenId)
                    } else {
                        it.copy(endBellSoundId = chosenId)
                    }
                }
                soundPickerTarget = null
            },
            onTogglePreview = onTogglePreview,
            onUploadCustomSound = {
                onUploadCustomSoundForBell(bellType)
            },
            onDismiss = { soundPickerTarget = null },
        )
    }
}

@Composable
private fun TimeDisplayCard(
    label: String,
    timeText: String,
    accentColor: androidx.compose.ui.graphics.Color,
    containerColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = accentColor,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = timeText,
                style = MaterialTheme.typography.headlineMedium.copy(fontFamily = JetBrainsMonoFamily),
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "눌러서 변경",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun QuickDayPresetChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun BellSoundConfigRow(
    label: String,
    enabled: Boolean,
    soundName: String,
    onToggleEnabled: (Boolean) -> Unit,
    onSelectSound: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, onClick = onSelectSound)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = if (enabled) "$soundName (눌러서 음원 변경)" else "알림 꺼짐",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Switch(checked = enabled, onCheckedChange = onToggleEnabled)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialogWrapper(
    title: String,
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true,
    )
    AdaptiveAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleMedium) },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) {
                Text("확인")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        },
    )
}

@Composable
private fun CustomMinutesInputDialog(
    title: String,
    initialValue: Int,
    minValue: Int,
    maxValue: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var inputText by remember { mutableStateOf(initialValue.toString()) }
    AdaptiveAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleMedium) },
        text = {
            OutlinedTextField(
                value = inputText,
                onValueChange = { raw -> inputText = raw.filter { it.isDigit() } },
                label = { Text("분 ($minValue ~ $maxValue)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val value = inputText.toIntOrNull()?.coerceIn(minValue, maxValue) ?: initialValue
                    onConfirm(value)
                }
            ) {
                Text("적용")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        },
    )
}

@Composable
fun SoundSelectionDialog(
    title: String,
    selectedSoundId: String,
    customSounds: List<CustomBellSoundEntity>,
    previewPlayingSoundId: String?,
    onSelectSound: (String) -> Unit,
    onTogglePreview: (String) -> Unit,
    onUploadCustomSound: () -> Unit,
    onDismiss: () -> Unit,
) {
    AdaptiveAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onUploadCustomSound,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("upload_sound_in_picker_button"),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("내 음원 파일 업로드 (MP3, WAV, M4A)")
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (customSounds.isNotEmpty()) {
                        item {
                            Text(
                                text = "업로드한 내 음원",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 4.dp),
                            )
                        }
                        items(customSounds, key = { it.id }) { custom ->
                            SoundPickRow(
                                name = custom.displayName,
                                subtitle = custom.formattedDuration(),
                                isSelected = selectedSoundId == custom.id,
                                isPlaying = previewPlayingSoundId == custom.id,
                                isCustom = true,
                                onSelect = { onSelectSound(custom.id) },
                                onTogglePlay = { onTogglePreview(custom.id) },
                            )
                        }
                    }

                    item {
                        Text(
                            text = "기본 학원 차임벨",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    }
                    items(BuiltInBellSounds.presets, key = { it.id }) { preset ->
                        SoundPickRow(
                            name = preset.name,
                            subtitle = preset.description,
                            isSelected = selectedSoundId == preset.id,
                            isPlaying = previewPlayingSoundId == preset.id,
                            isCustom = false,
                            onSelect = { onSelectSound(preset.id) },
                            onTogglePlay = { onTogglePreview(preset.id) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("닫기") }
        },
    )
}

@Composable
private fun SoundPickRow(
    name: String,
    subtitle: String,
    isSelected: Boolean,
    isPlaying: Boolean,
    isCustom: Boolean,
    onSelect: () -> Unit,
    onTogglePlay: () -> Unit,
) {
    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (isCustom) Icons.Default.AudioFile else Icons.Default.MusicNote,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            IconButton(onClick = onTogglePlay) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "미리듣기 중지" else "미리듣기",
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "선택됨",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
