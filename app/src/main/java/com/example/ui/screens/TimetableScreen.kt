package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.BellScheduleEntity
import com.example.data.model.DayOfWeekBitmask
import com.example.ui.AcademyBellUiState
import com.example.ui.components.HeroCountdownCard
import com.example.ui.components.ScheduleTimelineCard

@Composable
fun TimetableScreen(
    uiState: AcademyBellUiState,
    hasNotificationPermission: Boolean,
    onRequestExactAlarmPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onToggleMaster: (Boolean) -> Unit,
    onStopRinging: () -> Unit,
    onSelectDayTab: (Int) -> Unit,
    onOpenNewSchedule: (BellScheduleEntity?) -> Unit,
    onEditSchedule: (BellScheduleEntity) -> Unit,
    onToggleScheduleEnabled: (BellScheduleEntity) -> Unit,
    onDeleteSchedule: (BellScheduleEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isExpandedTablet = maxWidth >= 660.dp
        val isCompactHeight = maxHeight < 620.dp

        if (isExpandedTablet) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = if (isCompactHeight) 8.dp else 14.dp)
                    .testTag("tablet_two_pane_timetable"),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                // Left Pane: Clock Hero, Bento Summary, & Permissions
                Column(
                    modifier = Modifier
                        .weight(0.40f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(if (isCompactHeight) 10.dp else 14.dp),
                ) {
                    HeroCountdownCard(
                        nowMillis = uiState.nowMillis,
                        masterEnabled = uiState.preferences.masterEnabled,
                        forceMaxVolume = uiState.preferences.forceMaxVolume,
                        nextBellGroup = uiState.nextBellGroup,
                        activeRinging = uiState.activeRinging,
                        onToggleMaster = onToggleMaster,
                        onStopRinging = onStopRinging,
                        compact = isCompactHeight,
                    )

                    TabletOverviewPanel(
                        daySchedules = uiState.daySchedules,
                        allSchedules = uiState.allSchedules,
                        displayedDayBit = uiState.displayedDayBit,
                        onAddSchedule = { onOpenNewSchedule(null) },
                        compact = isCompactHeight,
                    )

                    if (!uiState.canScheduleExactAlarms || !hasNotificationPermission) {
                        PermissionWarningCard(
                            needsExactAlarm = !uiState.canScheduleExactAlarms,
                            needsNotification = !hasNotificationPermission,
                            onRequestExactAlarm = onRequestExactAlarmPermission,
                            onRequestNotification = onRequestNotificationPermission,
                        )
                    }
                }

                // Right Pane: Day Filter Strip + Schedule Timeline
                Column(
                    modifier = Modifier
                        .weight(0.60f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    DayFilterRow(
                        selectedDayTab = uiState.selectedDayTab,
                        todayBit = DayOfWeekBitmask.todayBit(uiState.nowMillis),
                        allSchedules = uiState.allSchedules,
                        onSelectDayTab = onSelectDayTab,
                    )

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (uiState.daySchedules.isEmpty()) {
                            item {
                                EmptyTimetableCard(
                                    dayLabel = if (uiState.selectedDayTab == -1) {
                                        "오늘(${DayOfWeekBitmask.formatDaysSummary(uiState.displayedDayBit)})"
                                    } else {
                                        DayOfWeekBitmask.formatDaysSummary(uiState.displayedDayBit)
                                    },
                                    onAddFirst = { onOpenNewSchedule(null) },
                                )
                            }
                        } else {
                            itemsIndexed(
                                items = uiState.daySchedules,
                                key = { _, item -> item.id },
                            ) { index, schedule ->
                                ScheduleTimelineCard(
                                    schedule = schedule,
                                    customSounds = uiState.customSounds,
                                    showChainNextButton = index == uiState.daySchedules.lastIndex,
                                    nextScheduleOnDay = uiState.daySchedules.getOrNull(index + 1),
                                    onToggleEnabled = { onToggleScheduleEnabled(schedule) },
                                    onEdit = { onEditSchedule(schedule) },
                                    onDelete = { onDeleteSchedule(schedule) },
                                    onChainNext = { onOpenNewSchedule(schedule) },
                                )
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("compact_timetable_list"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (!uiState.canScheduleExactAlarms || !hasNotificationPermission) {
                    item {
                        PermissionWarningCard(
                            needsExactAlarm = !uiState.canScheduleExactAlarms,
                            needsNotification = !hasNotificationPermission,
                            onRequestExactAlarm = onRequestExactAlarmPermission,
                            onRequestNotification = onRequestNotificationPermission,
                        )
                    }
                }

                item {
                    HeroCountdownCard(
                        nowMillis = uiState.nowMillis,
                        masterEnabled = uiState.preferences.masterEnabled,
                        forceMaxVolume = uiState.preferences.forceMaxVolume,
                        nextBellGroup = uiState.nextBellGroup,
                        activeRinging = uiState.activeRinging,
                        onToggleMaster = onToggleMaster,
                        onStopRinging = onStopRinging,
                    )
                }

                item {
                    DayFilterRow(
                        selectedDayTab = uiState.selectedDayTab,
                        todayBit = DayOfWeekBitmask.todayBit(uiState.nowMillis),
                        allSchedules = uiState.allSchedules,
                        onSelectDayTab = onSelectDayTab,
                    )
                }

                if (uiState.daySchedules.isEmpty()) {
                    item {
                        EmptyTimetableCard(
                            dayLabel = if (uiState.selectedDayTab == -1) {
                                "오늘(${DayOfWeekBitmask.formatDaysSummary(uiState.displayedDayBit)})"
                            } else {
                                DayOfWeekBitmask.formatDaysSummary(uiState.displayedDayBit)
                            },
                            onAddFirst = { onOpenNewSchedule(null) },
                        )
                    }
                } else {
                    itemsIndexed(
                        items = uiState.daySchedules,
                        key = { _, item -> item.id },
                    ) { index, schedule ->
                        ScheduleTimelineCard(
                            schedule = schedule,
                            customSounds = uiState.customSounds,
                            showChainNextButton = index == uiState.daySchedules.lastIndex,
                            nextScheduleOnDay = uiState.daySchedules.getOrNull(index + 1),
                            onToggleEnabled = { onToggleScheduleEnabled(schedule) },
                            onEdit = { onEditSchedule(schedule) },
                            onDelete = { onDeleteSchedule(schedule) },
                            onChainNext = { onOpenNewSchedule(schedule) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TabletOverviewPanel(
    daySchedules: List<BellScheduleEntity>,
    allSchedules: List<BellScheduleEntity>,
    displayedDayBit: Int,
    onAddSchedule: () -> Unit,
    compact: Boolean = false,
) {
    val activeTodayCount = daySchedules.count { it.isEnabled }
    val weeklyRepeatCount = allSchedules.count { it.isWeeklyRepeat && it.isEnabled }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (compact) 14.dp else 18.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "시간표 요약 · ${DayOfWeekBitmask.formatDaysSummary(displayedDayBit)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        text = "전체 ${allSchedules.size}개",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    modifier = Modifier.weight(1f),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            modifier = Modifier.size(34.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "선택 요일 활성",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "${activeTodayCount}개 교시",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    modifier = Modifier.weight(1f),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f),
                            modifier = Modifier.size(34.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Repeat,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(17.dp),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "매주 반복 중",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "${weeklyRepeatCount}개 스케줄",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }

            Button(
                onClick = onAddSchedule,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("tablet_quick_add_schedule_button"),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("새 교시 연속 추가하기", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun DayFilterRow(
    selectedDayTab: Int,
    todayBit: Int,
    allSchedules: List<BellScheduleEntity>,
    onSelectDayTab: (Int) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            item {
                val todayCount = allSchedules.count { DayOfWeekBitmask.hasDay(it.daysOfWeekMask, todayBit) }
                FilterChip(
                    selected = selectedDayTab == -1,
                    onClick = { onSelectDayTab(-1) },
                    label = { Text("오늘 ($todayCount)", fontWeight = FontWeight.SemiBold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    modifier = Modifier.testTag("day_tab_today"),
                )
            }
            items(DayOfWeekBitmask.orderedDays.size) { idx ->
                val day = DayOfWeekBitmask.orderedDays[idx]
                val count = allSchedules.count { DayOfWeekBitmask.hasDay(it.daysOfWeekMask, day.bit) }
                FilterChip(
                    selected = selectedDayTab == day.bit,
                    onClick = { onSelectDayTab(day.bit) },
                    label = {
                        Text(if (count > 0) "${day.shortLabel} ($count)" else day.shortLabel)
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    modifier = Modifier.testTag("day_tab_${day.shortLabel}"),
                )
            }
        }
    }
}

@Composable
private fun PermissionWarningCard(
    needsExactAlarm: Boolean,
    needsNotification: Boolean,
    onRequestExactAlarm: () -> Unit,
    onRequestNotification: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.78f),
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "정시 타종을 위해 권한 허용이 필요합니다",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            if (needsExactAlarm) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "• 알람 및 리마인더(정확한 시간 예약) 허용",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onRequestExactAlarm) {
                        Text("설정 열기", fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (needsNotification) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "• 알림 표시 권한 허용 (종소리 화면 안내)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onRequestNotification) {
                        Text("허용하기", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyTimetableCard(
    dayLabel: String,
    onAddFirst: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(62.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(30.dp),
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "$dayLabel 등록된 수업 종이 없습니다",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "시작 시간, 수업 시간(40~90분), 쉬는 시간만 고르면\n다음 교시까지 연속으로 빠르게 추가할 수 있습니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(22.dp))
            Button(
                onClick = onAddFirst,
                modifier = Modifier
                    .height(48.dp)
                    .testTag("empty_add_schedule_button"),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("첫 교시 시간표 만들기", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
