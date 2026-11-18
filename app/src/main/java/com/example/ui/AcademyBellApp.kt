package com.example.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.alarm.BellType
import com.example.data.local.ThemeMode
import com.example.ui.components.ChainedScheduleSheet
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SoundLibraryScreen
import com.example.ui.screens.TimetableScreen
import com.example.ui.theme.AcademyBellTheme
import com.example.ui.theme.AcademyIndigo
import com.example.ui.theme.AcademyIndigoMid
import com.example.ui.theme.EndBellTeal
import com.example.ui.theme.StartBellAmber

enum class AppTab(
    val titleRes: Int,
    val subtitle: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String,
) {
    TIMETABLE(
        R.string.nav_timetable,
        "요일·교시별 시작종과 종료종을 최대 음량으로 자동 송출합니다",
        Icons.Filled.Schedule,
        Icons.Outlined.Schedule,
        "tab_timetable",
    ),
    SOUNDS(
        R.string.nav_sounds,
        "기본 차임벨 미리듣기 및 내 음원(MP3·WAV·M4A) 업로드",
        Icons.Filled.MusicNote,
        Icons.Outlined.MusicNote,
        "tab_sounds",
    ),
    SETTINGS(
        R.string.nav_settings,
        "최대 음량 동작, 새 교시 기본 시간 및 시스템 권한 관리",
        Icons.Filled.Settings,
        Icons.Outlined.Settings,
        "tab_settings",
    ),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcademyBellApp(
    viewModel: MainViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }

    var currentTab by rememberSaveable { mutableStateOf(AppTab.TIMETABLE) }
    var pendingUploadBellType by remember { mutableStateOf<BellType?>(null) }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasNotificationPermission = granted
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importCustomSound(uri, applyToDraftBellType = pendingUploadBellType)
        }
        pendingUploadBellType = null
    }

    val openExactAlarmSettings = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(intent) }
        }
    }

    val requestNotificationOrOpenSettings = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshExactAlarmPermission()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    hasNotificationPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS,
                    ) == PackageManager.PERMISSION_GRANTED
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(uiState.snackbarMessage) {
        val msg = uiState.snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearSnackbar()
    }

    BackHandler(enabled = currentTab != AppTab.TIMETABLE && uiState.scheduleDraft == null) {
        currentTab = AppTab.TIMETABLE
    }

    val useDarkTheme = when (uiState.preferences.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    AcademyBellTheme(darkTheme = useDarkTheme) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            val isWideLandscape = maxWidth >= 600.dp

            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                containerColor = MaterialTheme.colorScheme.background,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                    if (!isWideLandscape) {
                        CenterAlignedTopAppBar(
                            title = {
                                Text(
                                    text = stringResource(R.string.app_name),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background,
                            ),
                        )
                    }
                },
                bottomBar = {
                    if (!isWideLandscape) {
                        NavigationBar(modifier = Modifier.testTag("bottom_navigation_bar")) {
                            AppTab.entries.forEach { tab ->
                                val selected = currentTab == tab
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { currentTab = tab },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = stringResource(tab.titleRes),
                                        )
                                    },
                                    label = { Text(stringResource(tab.titleRes)) },
                                    modifier = Modifier.testTag(tab.tag),
                                )
                            }
                        }
                    }
                },
                floatingActionButton = {
                    if (!isWideLandscape && currentTab == AppTab.TIMETABLE) {
                        ExtendedFloatingActionButton(
                            onClick = { viewModel.openNewScheduleDraft(null) },
                            icon = { Icon(Icons.Default.Add, contentDescription = null) },
                            text = { Text(stringResource(R.string.action_add_schedule)) },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.testTag("fab_add_schedule"),
                        )
                    }
                },
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    if (isWideLandscape) {
                        StudioSideNavigationRail(
                            currentTab = currentTab,
                            masterEnabled = uiState.preferences.masterEnabled,
                            activeScheduleCount = uiState.allSchedules.count { it.isEnabled },
                            onSelectTab = { currentTab = it },
                        )
                        VerticalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    ) {
                        if (isWideLandscape) {
                            StudioLandscapeHeader(
                                currentTab = currentTab,
                                masterEnabled = uiState.preferences.masterEnabled,
                                onAddSchedule = { viewModel.openNewScheduleDraft(null) },
                            )
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f),
                            )
                        }

                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            when (currentTab) {
                                AppTab.TIMETABLE -> TimetableScreen(
                                    uiState = uiState,
                                    hasNotificationPermission = hasNotificationPermission,
                                    onRequestExactAlarmPermission = openExactAlarmSettings,
                                    onRequestNotificationPermission = requestNotificationOrOpenSettings,
                                    onToggleMaster = viewModel::setMasterEnabled,
                                    onStopRinging = viewModel::stopActiveRinging,
                                    onSelectDayTab = viewModel::selectDayTab,
                                    onOpenNewSchedule = viewModel::openNewScheduleDraft,
                                    onEditSchedule = viewModel::openEditScheduleDraft,
                                    onToggleScheduleEnabled = viewModel::toggleScheduleEnabled,
                                    onDeleteSchedule = viewModel::deleteSchedule,
                                    modifier = Modifier.fillMaxSize(),
                                )

                                AppTab.SOUNDS -> SoundLibraryScreen(
                                    customSounds = uiState.customSounds,
                                    previewPlayingSoundId = uiState.previewPlayingSoundId,
                                    activeRinging = uiState.activeRinging,
                                    onUploadAudioClick = {
                                        pendingUploadBellType = null
                                        audioPickerLauncher.launch(arrayOf("audio/*"))
                                    },
                                    onTogglePreview = { viewModel.togglePreviewSound(it, forceMaxVolume = false) },
                                    onMaxVolumeTest = { soundId, title ->
                                        viewModel.triggerMaxVolumeTestBell(soundId, title)
                                    },
                                    onStopRinging = viewModel::stopActiveRinging,
                                    onRenameCustomSound = viewModel::renameCustomSound,
                                    onDeleteCustomSound = viewModel::deleteCustomSound,
                                    modifier = Modifier.fillMaxSize(),
                                )

                                AppTab.SETTINGS -> SettingsScreen(
                                    preferences = uiState.preferences,
                                    customSounds = uiState.customSounds,
                                    previewPlayingSoundId = uiState.previewPlayingSoundId,
                                    canScheduleExactAlarms = uiState.canScheduleExactAlarms,
                                    hasNotificationPermission = hasNotificationPermission,
                                    onToggleMaster = viewModel::setMasterEnabled,
                                    onToggleForceMaxVolume = viewModel::updateForceMaxVolume,
                                    onToggleRestoreVolume = viewModel::updateRestoreVolumeAfterRing,
                                    onToggleVibrate = viewModel::updateVibrateOnBell,
                                    onUpdateDefaultClassMinutes = viewModel::updateDefaultClassMinutes,
                                    onUpdateDefaultBreakMinutes = viewModel::updateDefaultBreakMinutes,
                                    onUpdateDefaultRingSeconds = viewModel::updateDefaultRingDurationSeconds,
                                    onUpdateDefaultStartSound = viewModel::updateDefaultStartSoundId,
                                    onUpdateDefaultEndSound = viewModel::updateDefaultEndSoundId,
                                    onUpdateThemeMode = viewModel::updateThemeMode,
                                    onTogglePreview = { viewModel.togglePreviewSound(it, forceMaxVolume = false) },
                                    onUploadCustomSound = {
                                        pendingUploadBellType = null
                                        audioPickerLauncher.launch(arrayOf("audio/*"))
                                    },
                                    onRequestExactAlarmPermission = openExactAlarmSettings,
                                    onRequestNotificationPermission = requestNotificationOrOpenSettings,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                    }
                }
            }

            uiState.scheduleDraft?.let { draft ->
                BackHandler {
                    viewModel.dismissDraft()
                }
                ChainedScheduleSheet(
                    draft = draft,
                    customSounds = uiState.customSounds,
                    previewPlayingSoundId = uiState.previewPlayingSoundId,
                    onUpdateDraft = viewModel::updateDraft,
                    onDismiss = viewModel::dismissDraft,
                    onSave = viewModel::saveDraft,
                    onTogglePreview = { viewModel.togglePreviewSound(it, forceMaxVolume = false) },
                    onUploadCustomSoundForBell = { bellType ->
                        pendingUploadBellType = bellType
                        audioPickerLauncher.launch(arrayOf("audio/*"))
                    },
                )
            }
        }
    }
}

@Composable
private fun StudioSideNavigationRail(
    currentTab: AppTab,
    masterEnabled: Boolean,
    activeScheduleCount: Int,
    onSelectTab: (AppTab) -> Unit,
) {
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxHeight()
            .testTag("navigation_rail"),
        header = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 12.dp, bottom = 12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(
                            brush = Brush.linearGradient(listOf(AcademyIndigo, AcademyIndigoMid)),
                            shape = RoundedCornerShape(15.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = stringResource(R.string.app_name),
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "스마트벨",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        AppTab.entries.forEach { tab ->
            val selected = currentTab == tab
            NavigationRailItem(
                selected = selected,
                onClick = { onSelectTab(tab) },
                icon = {
                    Icon(
                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = stringResource(tab.titleRes),
                    )
                },
                label = {
                    Text(
                        text = stringResource(tab.titleRes),
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    )
                },
                modifier = Modifier.testTag(tab.tag),
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        Spacer(modifier = Modifier.weight(1f))

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (masterEnabled) {
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 14.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = if (masterEnabled) EndBellTeal else MaterialTheme.colorScheme.outline,
                            shape = CircleShape,
                        ),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (masterEnabled) "${activeScheduleCount}개" else "정지",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (masterEnabled) {
                        MaterialTheme.colorScheme.onTertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}

@Composable
private fun StudioLandscapeHeader(
    currentTab: AppTab,
    masterEnabled: Boolean,
    onAddSchedule: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (masterEnabled) {
                            StartBellAmber.copy(alpha = 0.14f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        border = BorderStroke(
                            1.dp,
                            if (masterEnabled) StartBellAmber.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outlineVariant,
                        ),
                    ) {
                        Text(
                            text = if (masterEnabled) "자동 타종 대기 중" else "전체 일시정지",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (masterEnabled) StartBellAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 2.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(currentTab.titleRes),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = currentTab.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (currentTab == AppTab.TIMETABLE) {
                Button(
                    onClick = onAddSchedule,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("fab_add_schedule"),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.action_add_schedule),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
