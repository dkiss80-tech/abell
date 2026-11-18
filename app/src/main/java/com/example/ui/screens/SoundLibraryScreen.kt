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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.alarm.ActiveRingingState
import com.example.data.model.BuiltInBellSounds
import com.example.data.model.CustomBellSoundEntity
import com.example.ui.components.AdaptiveAlertDialog
import com.example.ui.theme.AcademyIndigo
import com.example.ui.theme.AcademyIndigoMid

@Composable
fun SoundLibraryScreen(
    customSounds: List<CustomBellSoundEntity>,
    previewPlayingSoundId: String?,
    activeRinging: ActiveRingingState?,
    onUploadAudioClick: () -> Unit,
    onTogglePreview: (String) -> Unit,
    onMaxVolumeTest: (soundId: String, title: String) -> Unit,
    onStopRinging: () -> Unit,
    onRenameCustomSound: (id: String, newName: String) -> Unit,
    onDeleteCustomSound: (CustomBellSoundEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    var renameTarget by remember { mutableStateOf<CustomBellSoundEntity?>(null) }
    var deleteTarget by remember { mutableStateOf<CustomBellSoundEntity?>(null) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isExpandedTablet = maxWidth >= 660.dp

        if (isExpandedTablet) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
                    .testTag("tablet_two_pane_sounds"),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                // Left Pane: Upload + Custom Uploaded Sounds
                LazyColumn(
                    modifier = Modifier
                        .weight(0.48f)
                        .fillMaxHeight(),
                    contentPadding = PaddingValues(bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        UploadSoundBanner(onUploadAudioClick = onUploadAudioClick)
                    }
                    if (activeRinging != null) {
                        item { StopTestBellBannerButton(onStopRinging = onStopRinging) }
                    }
                    item {
                        Text(
                            text = "업로드한 내 음원 (${customSounds.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    if (customSounds.isEmpty()) {
                        item { EmptyCustomSoundsCard() }
                    } else {
                        items(customSounds, key = { it.id }) { sound ->
                            SoundItemCard(
                                title = sound.displayName,
                                subtitle = "업로드 파일 · ${sound.formattedDuration()}",
                                isCustom = true,
                                isPlayingPreview = previewPlayingSoundId == sound.id,
                                onTogglePreview = { onTogglePreview(sound.id) },
                                onMaxVolumeTest = { onMaxVolumeTest(sound.id, sound.displayName) },
                                onRename = { renameTarget = sound },
                                onDelete = { deleteTarget = sound },
                            )
                        }
                    }
                }

                // Right Pane: Built-in Chimes
                LazyColumn(
                    modifier = Modifier
                        .weight(0.52f)
                        .fillMaxHeight(),
                    contentPadding = PaddingValues(bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Text(
                            text = "기본 내장 차임벨 (${BuiltInBellSounds.presets.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    items(BuiltInBellSounds.presets, key = { it.id }) { preset ->
                        SoundItemCard(
                            title = preset.name,
                            subtitle = preset.description,
                            isCustom = false,
                            isPlayingPreview = previewPlayingSoundId == preset.id,
                            onTogglePreview = { onTogglePreview(preset.id) },
                            onMaxVolumeTest = { onMaxVolumeTest(preset.id, preset.name) },
                            onRename = null,
                            onDelete = null,
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    UploadSoundBanner(onUploadAudioClick = onUploadAudioClick)
                }

                if (activeRinging != null) {
                    item { StopTestBellBannerButton(onStopRinging = onStopRinging) }
                }

                item {
                    Text(
                        text = "업로드한 내 음원 (${customSounds.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }

                if (customSounds.isEmpty()) {
                    item { EmptyCustomSoundsCard() }
                } else {
                    items(customSounds, key = { it.id }) { sound ->
                        SoundItemCard(
                            title = sound.displayName,
                            subtitle = "업로드 파일 · ${sound.formattedDuration()}",
                            isCustom = true,
                            isPlayingPreview = previewPlayingSoundId == sound.id,
                            onTogglePreview = { onTogglePreview(sound.id) },
                            onMaxVolumeTest = { onMaxVolumeTest(sound.id, sound.displayName) },
                            onRename = { renameTarget = sound },
                            onDelete = { deleteTarget = sound },
                        )
                    }
                }

                item {
                    Text(
                        text = "기본 내장 차임벨 (${BuiltInBellSounds.presets.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }

                items(BuiltInBellSounds.presets, key = { it.id }) { preset ->
                    SoundItemCard(
                        title = preset.name,
                        subtitle = preset.description,
                        isCustom = false,
                        isPlayingPreview = previewPlayingSoundId == preset.id,
                        onTogglePreview = { onTogglePreview(preset.id) },
                        onMaxVolumeTest = { onMaxVolumeTest(preset.id, preset.name) },
                        onRename = null,
                        onDelete = null,
                    )
                }
            }
        }
    }

    renameTarget?.let { target ->
        var newName by remember(target.id) { mutableStateOf(target.displayName) }
        AdaptiveAlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("음원 이름 변경") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("음원 표시 이름") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRenameCustomSound(target.id, newName)
                        renameTarget = null
                    }
                ) { Text("저장") }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("취소") }
            },
        )
    }

    deleteTarget?.let { target ->
        AdaptiveAlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("음원 삭제") },
            text = {
                Text("'${target.displayName}' 음원을 삭제할까요? 이 음원을 사용 중이던 스케줄은 기본 차임벨로 자동 전환됩니다.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteCustomSound(target)
                        deleteTarget = null
                    }
                ) { Text("삭제", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("취소") }
            },
        )
    }
}

@Composable
private fun UploadSoundBanner(onUploadAudioClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(AcademyIndigo, AcademyIndigoMid)
                    )
                )
                .padding(20.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.16f),
                        modifier = Modifier.size(44.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "원하는 음원을 학원 종소리로 사용하세요",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                        Text(
                            text = "MP3 · WAV · M4A · OGG 지원",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFFBFDBFE),
                        )
                    }
                }

                Text(
                    text = "오디오 파일을 업로드하면 앱 내부에 안전하게 보관되어 시작종/종료종으로 바로 지정할 수 있습니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFE2E8F0),
                )

                Button(
                    onClick = onUploadAudioClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("upload_custom_sound_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = AcademyIndigo,
                    ),
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("내 음원 파일 업로드", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun StopTestBellBannerButton(onStopRinging: () -> Unit) {
    Button(
        onClick = onStopRinging,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = Color.White,
        ),
        shape = RoundedCornerShape(14.dp),
    ) {
        Icon(Icons.Default.Stop, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("테스트 종소리 지금 멈추기", fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EmptyCustomSoundsCard() {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = "아직 업로드된 음원이 없습니다. 위 버튼을 눌러 학원에서 사용할 벨소리 파일을 추가해 보세요.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(18.dp),
        )
    }
}

@Composable
private fun SoundItemCard(
    title: String,
    subtitle: String,
    isCustom: Boolean,
    isPlayingPreview: Boolean,
    onTogglePreview: () -> Unit,
    onMaxVolumeTest: () -> Unit,
    onRename: (() -> Unit)?,
    onDelete: (() -> Unit)?,
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = if (isPlayingPreview) 1.5.dp else 1.dp,
            color = if (isPlayingPreview) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f)
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isPlayingPreview) 3.dp else 1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isCustom) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    },
                    modifier = Modifier.size(44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isCustom) Icons.Default.AudioFile else Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = if (isCustom) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            },
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (onRename != null) {
                    IconButton(onClick = onRename) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "이름 변경",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "음원 삭제",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onTogglePreview,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(
                        imageVector = if (isPlayingPreview) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isPlayingPreview) "정지" else "미리듣기")
                }
                Button(
                    onClick = onMaxVolumeTest,
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("최대 음량 테스트")
                }
            }
        }
    }
}
