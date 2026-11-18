package com.example.alarm

import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.AcademyBellTheme
import com.example.ui.theme.EndBellTeal
import com.example.ui.theme.StartBellAmber

class RingingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        enableEdgeToEdge()
        configureLockScreenDisplay()

        val fallbackTitle = intent.getStringExtra(BellPlaybackService.EXTRA_TITLE) ?: "수업 시간"
        val fallbackType = runCatching {
            BellType.valueOf(intent.getStringExtra(BellPlaybackService.EXTRA_BELL_TYPE) ?: BellType.START.name)
        }.getOrDefault(BellType.START)

        setContent {
            AcademyBellTheme(darkTheme = true) {
                val ringingState by BellRingingBus.activeRinging.collectAsStateWithLifecycle()

                // Auto-close once service stops ringing
                LaunchedEffect(ringingState) {
                    if (ringingState == null) {
                        kotlinx.coroutines.delay(500L)
                        if (BellRingingBus.activeRinging.value == null) {
                            finish()
                        }
                    }
                }

                val currentTitle = ringingState?.scheduleTitle ?: fallbackTitle
                val currentType = ringingState?.bellType ?: fallbackType
                val accentColor = if (currentType == BellType.START) StartBellAmber else EndBellTeal

                val infiniteTransition = rememberInfiniteTransition(label = "ringWave")
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 0.92f,
                    targetValue = 1.20f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 850),
                        repeatMode = RepeatMode.Reverse,
                    ),
                    label = "pulse",
                )

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF090F1E), Color(0xFF172D52), Color(0xFF1E3A68))
                            )
                        )
                        .safeDrawingPadding()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    val isWideLandscape = maxWidth >= 600.dp
                    val subtitleText = when {
                        ringingState == null -> "종소리 재생 중..."
                        ringingState!!.isManualDismissOnly -> "직접 멈출 때까지 계속 울립니다"
                        else -> "${ringingState!!.remainingSeconds}초 후 자동으로 멈춥니다"
                    }

                    if (isWideLandscape) {
                        Surface(
                            shape = RoundedCornerShape(32.dp),
                            color = Color.White.copy(alpha = 0.06f),
                            modifier = Modifier.widthIn(max = 760.dp),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 36.dp, vertical = 32.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(36.dp),
                            ) {
                                RingingBellVisual(
                                    accentColor = accentColor,
                                    pulseScale = pulseScale,
                                )

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    BellTypeBadge(currentType = currentType, accentColor = accentColor)
                                    Text(
                                        text = currentTitle,
                                        style = MaterialTheme.typography.headlineLarge,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        text = subtitleText,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color(0xFFCBD5E1),
                                    )
                                    ringingState?.soundName?.let { sName ->
                                        Text(
                                            text = "음원: $sName",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color(0xFF94A3B8),
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    StopRingingActionButton(
                                        onClick = {
                                            BellPlaybackService.stopService(this@RingingActivity)
                                            finish()
                                        },
                                    )
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .widthIn(max = 460.dp)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            BellTypeBadge(currentType = currentType, accentColor = accentColor)
                            Spacer(modifier = Modifier.height(24.dp))
                            RingingBellVisual(accentColor = accentColor, pulseScale = pulseScale)
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = currentTitle,
                                style = MaterialTheme.typography.headlineLarge,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = subtitleText,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color(0xFFCBD5E1),
                                textAlign = TextAlign.Center,
                            )
                            ringingState?.soundName?.let { sName ->
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "음원: $sName",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFF94A3B8),
                                )
                            }
                            Spacer(modifier = Modifier.height(28.dp))
                            StopRingingActionButton(
                                onClick = {
                                    BellPlaybackService.stopService(this@RingingActivity)
                                    finish()
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    private fun configureLockScreenDisplay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }
}

@Composable
private fun BellTypeBadge(
    currentType: BellType,
    accentColor: Color,
) {
    Surface(
        color = accentColor.copy(alpha = 0.22f),
        shape = RoundedCornerShape(50),
    ) {
        Text(
            text = "${currentType.label} 최대 음량 송출 중",
            color = accentColor,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun RingingBellVisual(
    accentColor: Color,
    pulseScale: Float,
) {
    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(164.dp)
                .scale(pulseScale)
                .background(accentColor.copy(alpha = 0.18f), CircleShape),
        )
        Surface(
            modifier = Modifier.size(116.dp),
            shape = CircleShape,
            color = accentColor,
            shadowElevation = 12.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = "종소리 울리는 중",
                    tint = Color.White,
                    modifier = Modifier.size(56.dp),
                )
            }
        }
    }
}

@Composable
private fun StopRingingActionButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .testTag("stop_ringing_button"),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFDC2626),
            contentColor = Color.White,
        ),
    ) {
        Icon(
            imageVector = Icons.Default.Stop,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = "종소리 지금 멈추기",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}
