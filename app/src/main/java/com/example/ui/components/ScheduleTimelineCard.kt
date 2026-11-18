package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.BellScheduleEntity
import com.example.data.model.BuiltInBellSounds
import com.example.data.model.CustomBellSoundEntity
import com.example.data.model.DayOfWeekBitmask
import com.example.ui.theme.JetBrainsMonoFamily

@Composable
fun ScheduleTimelineCard(
    schedule: BellScheduleEntity,
    customSounds: List<CustomBellSoundEntity>,
    showChainNextButton: Boolean,
    nextScheduleOnDay: BellScheduleEntity?,
    onToggleEnabled: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onChainNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardAlpha = if (schedule.isEnabled) 1f else 0.56f
    val startSoundName = BuiltInBellSounds.findPresetName(schedule.startBellSoundId, customSounds)
    val endSoundName = BuiltInBellSounds.findPresetName(schedule.endBellSoundId, customSounds)

    Column(modifier = modifier.fillMaxWidth()) {
        Card(
            onClick = onEdit,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("schedule_card_${schedule.id}"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            border = BorderStroke(
                width = 1.dp,
                color = if (schedule.isEnabled) {
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.85f)
                } else {
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                },
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = if (schedule.isEnabled) 2.dp else 0.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
            ) {
                // Sleek left status bar
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .fillMaxHeight()
                        .padding(vertical = 14.dp)
                        .padding(start = 2.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (schedule.isEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            }
                        ),
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 14.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                text = schedule.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = cardAlpha),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = cardAlpha),
                                shape = RoundedCornerShape(50),
                            ) {
                                Text(
                                    text = "${schedule.classDurationMinutes}분 수업",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                )
                            }
                        }

                        Switch(
                            checked = schedule.isEnabled,
                            onCheckedChange = { onToggleEnabled() },
                            modifier = Modifier.testTag("schedule_toggle_${schedule.id}"),
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Connected Start -> End Bell Time Pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        BellTimePill(
                            label = "시작종",
                            time = schedule.formattedStartTime(),
                            soundName = startSoundName,
                            enabled = schedule.startBellEnabled && schedule.isEnabled,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.weight(1f),
                        )

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                            modifier = Modifier.size(16.dp),
                        )

                        BellTimePill(
                            label = "종료종",
                            time = schedule.formattedEndTime(),
                            soundName = endSoundName,
                            enabled = schedule.endBellEnabled && schedule.isEnabled,
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Repeat & Days + Actions Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                            modifier = Modifier.weight(1f, fill = false),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Repeat,
                                    contentDescription = null,
                                    tint = if (schedule.isWeeklyRepeat) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(14.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = buildString {
                                        append(DayOfWeekBitmask.formatDaysSummary(schedule.daysOfWeekMask))
                                        append(" · ")
                                        append(if (schedule.isWeeklyRepeat) "매주 반복" else "1회성")
                                        append(" · ")
                                        append(
                                            if (schedule.ringDurationSeconds == 0) {
                                                "수동 종료"
                                            } else {
                                                "${schedule.ringDurationSeconds}초"
                                            }
                                        )
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        Row {
                            IconButton(
                                onClick = onEdit,
                                modifier = Modifier.testTag("edit_schedule_${schedule.id}"),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = stringResource(R.string.cd_edit_item),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            IconButton(
                                onClick = onDelete,
                                modifier = Modifier.testTag("delete_schedule_${schedule.id}"),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = stringResource(R.string.cd_delete_item),
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }

                    if (showChainNextButton) {
                        Spacer(modifier = Modifier.height(6.dp))
                        FilledTonalButton(
                            onClick = onChainNext,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("chain_next_from_${schedule.id}"),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddCircleOutline,
                                contentDescription = stringResource(R.string.cd_chain_next),
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "쉬는 시간 ${schedule.breakMinutes}분 후 (${schedule.formattedNextChainedStartTime()}) 다음 교시 바로 추가",
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            }
        }

        // Visual break connector between consecutive schedules on the same day
        if (nextScheduleOnDay != null) {
            val gapMinutes = nextScheduleOnDay.startTotalMinutes - schedule.endTotalMinutes
            if (gapMinutes >= 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp, horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(20.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Coffee,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp),
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (gapMinutes == 0) "휴식 없이 바로 연결" else "쉬는 시간 ${gapMinutes}분",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BellTimePill(
    label: String,
    time: String,
    soundName: String,
    enabled: Boolean,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = if (enabled) containerColor.copy(alpha = 0.72f) else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = if (enabled) contentColor.copy(alpha = 0.14f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                modifier = Modifier.size(28.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = if (enabled) contentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (enabled) label else "$label 꺼짐",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (enabled) contentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = time,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = JetBrainsMonoFamily,
                        ),
                        fontWeight = FontWeight.Bold,
                        color = if (enabled) contentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = soundName,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    color = (if (enabled) contentColor else MaterialTheme.colorScheme.onSurfaceVariant)
                        .copy(alpha = 0.82f),
                )
            }
        }
    }
}
