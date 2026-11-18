package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.alarm.BellType
import com.example.data.local.AppPreferences
import com.example.data.local.ThemeMode
import com.example.data.model.BuiltInBellSounds
import com.example.data.model.CustomBellSoundEntity
import com.example.ui.components.SoundSelectionDialog

private val DEFAULT_CLASS_DURATIONS = listOf(40, 45, 50, 60, 80, 90)
private val DEFAULT_BREAK_DURATIONS = listOf(0, 5, 10, 15, 20)
private val DEFAULT_RING_SECONDS = listOf(5 to "5초", 8 to "8초", 10 to "10초", 15 to "15초", 30 to "30초", 0 to "수동 종료")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    preferences: AppPreferences,
    customSounds: List<CustomBellSoundEntity>,
    previewPlayingSoundId: String?,
    canScheduleExactAlarms: Boolean,
    hasNotificationPermission: Boolean,
    onToggleMaster: (Boolean) -> Unit,
    onToggleForceMaxVolume: (Boolean) -> Unit,
    onToggleRestoreVolume: (Boolean) -> Unit,
    onToggleVibrate: (Boolean) -> Unit,
    onUpdateDefaultClassMinutes: (Int) -> Unit,
    onUpdateDefaultBreakMinutes: (Int) -> Unit,
    onUpdateDefaultRingSeconds: (Int) -> Unit,
    onUpdateDefaultStartSound: (String) -> Unit,
    onUpdateDefaultEndSound: (String) -> Unit,
    onUpdateThemeMode: (ThemeMode) -> Unit,
    onTogglePreview: (String) -> Unit,
    onUploadCustomSound: () -> Unit,
    onRequestExactAlarmPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var defaultSoundPickerTarget by remember { mutableStateOf<BellType?>(null) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isExpandedTablet = maxWidth >= 660.dp

        if (isExpandedTablet) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
                    .testTag("tablet_two_pane_settings"),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                LazyColumn(
                    modifier = Modifier
                        .weight(0.5f)
                        .fillMaxHeight(),
                    contentPadding = PaddingValues(bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    item {
                        AlarmAndVolumeSection(
                            preferences = preferences,
                            onToggleMaster = onToggleMaster,
                            onToggleForceMaxVolume = onToggleForceMaxVolume,
                            onToggleRestoreVolume = onToggleRestoreVolume,
                            onToggleVibrate = onToggleVibrate,
                        )
                    }
                    item {
                        PermissionsSection(
                            canScheduleExactAlarms = canScheduleExactAlarms,
                            hasNotificationPermission = hasNotificationPermission,
                            onRequestExactAlarmPermission = onRequestExactAlarmPermission,
                            onRequestNotificationPermission = onRequestNotificationPermission,
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(0.5f)
                        .fillMaxHeight(),
                    contentPadding = PaddingValues(bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    item {
                        DefaultScheduleSection(
                            preferences = preferences,
                            customSounds = customSounds,
                            onUpdateDefaultClassMinutes = onUpdateDefaultClassMinutes,
                            onUpdateDefaultBreakMinutes = onUpdateDefaultBreakMinutes,
                            onUpdateDefaultRingSeconds = onUpdateDefaultRingSeconds,
                            onPickStartSound = { defaultSoundPickerTarget = BellType.START },
                            onPickEndSound = { defaultSoundPickerTarget = BellType.END },
                        )
                    }
                    item {
                        ThemeSection(
                            themeMode = preferences.themeMode,
                            onUpdateThemeMode = onUpdateThemeMode,
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    AlarmAndVolumeSection(
                        preferences = preferences,
                        onToggleMaster = onToggleMaster,
                        onToggleForceMaxVolume = onToggleForceMaxVolume,
                        onToggleRestoreVolume = onToggleRestoreVolume,
                        onToggleVibrate = onToggleVibrate,
                    )
                }
                item {
                    DefaultScheduleSection(
                        preferences = preferences,
                        customSounds = customSounds,
                        onUpdateDefaultClassMinutes = onUpdateDefaultClassMinutes,
                        onUpdateDefaultBreakMinutes = onUpdateDefaultBreakMinutes,
                        onUpdateDefaultRingSeconds = onUpdateDefaultRingSeconds,
                        onPickStartSound = { defaultSoundPickerTarget = BellType.START },
                        onPickEndSound = { defaultSoundPickerTarget = BellType.END },
                    )
                }
                item {
                    ThemeSection(
                        themeMode = preferences.themeMode,
                        onUpdateThemeMode = onUpdateThemeMode,
                    )
                }
                item {
                    PermissionsSection(
                        canScheduleExactAlarms = canScheduleExactAlarms,
                        hasNotificationPermission = hasNotificationPermission,
                        onRequestExactAlarmPermission = onRequestExactAlarmPermission,
                        onRequestNotificationPermission = onRequestNotificationPermission,
                    )
                }
            }
        }
    }

    defaultSoundPickerTarget?.let { target ->
        val currentId = if (target == BellType.START) {
            preferences.defaultStartSoundId
        } else {
            preferences.defaultEndSoundId
        }
        SoundSelectionDialog(
            title = if (target == BellType.START) "기본 시작종 음원 선택" else "기본 종료종 음원 선택",
            selectedSoundId = currentId,
            customSounds = customSounds,
            previewPlayingSoundId = previewPlayingSoundId,
            onSelectSound = { chosenId ->
                if (target == BellType.START) {
                    onUpdateDefaultStartSound(chosenId)
                } else {
                    onUpdateDefaultEndSound(chosenId)
                }
                defaultSoundPickerTarget = null
            },
            onTogglePreview = onTogglePreview,
            onUploadCustomSound = onUploadCustomSound,
            onDismiss = { defaultSoundPickerTarget = null },
        )
    }
}

@Composable
private fun AlarmAndVolumeSection(
    preferences: AppPreferences,
    onToggleMaster: (Boolean) -> Unit,
    onToggleForceMaxVolume: (Boolean) -> Unit,
    onToggleRestoreVolume: (Boolean) -> Unit,
    onToggleVibrate: (Boolean) -> Unit,
) {
    SettingsSectionCard(
        title = "알람 및 최대 음량 동작",
        icon = Icons.AutoMirrored.Filled.VolumeUp,
    ) {
        SettingSwitchRow(
            title = "학원 종소리 전체 운영",
            subtitle = "끄면 휴원일·방학 기간 동안 모든 종이 울리지 않습니다",
            checked = preferences.masterEnabled,
            onCheckedChange = onToggleMaster,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
        SettingSwitchRow(
            title = "종소리 알람 최대 음량 강제 설정",
            subtitle = "타종 시 알람 음량을 기기 최대치로 자동 설정합니다",
            checked = preferences.forceMaxVolume,
            onCheckedChange = onToggleForceMaxVolume,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
        SettingSwitchRow(
            title = "타종 종료 후 이전 음량 복원",
            subtitle = "종소리가 멈춘 뒤 원래 알람 볼륨으로 되돌립니다",
            checked = preferences.restoreVolumeAfterRing,
            onCheckedChange = onToggleRestoreVolume,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
        SettingSwitchRow(
            title = "타종 시 진동 함께 울림",
            subtitle = "종소리 시작 시 짧은 알림 진동을 제공합니다",
            checked = preferences.vibrateOnBell,
            onCheckedChange = onToggleVibrate,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DefaultScheduleSection(
    preferences: AppPreferences,
    customSounds: List<CustomBellSoundEntity>,
    onUpdateDefaultClassMinutes: (Int) -> Unit,
    onUpdateDefaultBreakMinutes: (Int) -> Unit,
    onUpdateDefaultRingSeconds: (Int) -> Unit,
    onPickStartSound: () -> Unit,
    onPickEndSound: () -> Unit,
) {
    SettingsSectionCard(
        title = "새 교시 기본값 설정",
        icon = Icons.Default.Schedule,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("기본 수업 시간", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DEFAULT_CLASS_DURATIONS.forEach { mins ->
                    FilterChip(
                        selected = preferences.defaultClassMinutes == mins,
                        onClick = { onUpdateDefaultClassMinutes(mins) },
                        label = { Text("${mins}분") },
                    )
                }
            }

            Text("기본 쉬는 시간", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DEFAULT_BREAK_DURATIONS.forEach { mins ->
                    FilterChip(
                        selected = preferences.defaultBreakMinutes == mins,
                        onClick = { onUpdateDefaultBreakMinutes(mins) },
                        label = { Text(if (mins == 0) "없음" else "${mins}분") },
                    )
                }
            }

            Text("기본 종소리 울림 시간", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DEFAULT_RING_SECONDS.forEach { (sec, label) ->
                    FilterChip(
                        selected = preferences.defaultRingDurationSeconds == sec,
                        onClick = { onUpdateDefaultRingSeconds(sec) },
                        label = { Text(label) },
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))

            DefaultSoundPickRow(
                label = "기본 시작종 음원",
                soundName = BuiltInBellSounds.findPresetName(preferences.defaultStartSoundId, customSounds),
                onClick = onPickStartSound,
            )
            DefaultSoundPickRow(
                label = "기본 종료종 음원",
                soundName = BuiltInBellSounds.findPresetName(preferences.defaultEndSoundId, customSounds),
                onClick = onPickEndSound,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThemeSection(
    themeMode: ThemeMode,
    onUpdateThemeMode: (ThemeMode) -> Unit,
) {
    SettingsSectionCard(
        title = "화면 테마",
        icon = Icons.Default.Palette,
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.entries.forEach { mode ->
                FilterChip(
                    selected = themeMode == mode,
                    onClick = { onUpdateThemeMode(mode) },
                    label = { Text(mode.label) },
                )
            }
        }
    }
}

@Composable
private fun PermissionsSection(
    canScheduleExactAlarms: Boolean,
    hasNotificationPermission: Boolean,
    onRequestExactAlarmPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
) {
    SettingsSectionCard(
        title = "시스템 권한 및 정시 타종 상태",
        icon = Icons.Default.VerifiedUser,
    ) {
        PermissionStatusRow(
            title = "정확한 알람 예약 (SCHEDULE_EXACT_ALARM)",
            granted = canScheduleExactAlarms,
            onAction = onRequestExactAlarmPermission,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
        PermissionStatusRow(
            title = "알림 표시 권한 (POST_NOTIFICATIONS)",
            granted = hasNotificationPermission,
            onAction = onRequestNotificationPermission,
        )
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(32.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            content()
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun DefaultSoundPickRow(
    label: String,
    soundName: String,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(soundName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text("변경", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun PermissionStatusRow(
    title: String,
    granted: Boolean,
    onAction: () -> Unit,
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
            Icon(
                imageVector = if (granted) Icons.Default.CheckCircle else Icons.Default.WarningAmber,
                contentDescription = null,
                tint = if (granted) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(
                    text = if (granted) "허용됨" else "허용 필요",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (!granted) {
            TextButton(onClick = onAction) {
                Text("허용하기", fontWeight = FontWeight.Bold)
            }
        }
    }
}
