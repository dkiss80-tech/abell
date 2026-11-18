package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alarm.ActiveRingingState
import com.example.alarm.BellType
import com.example.alarm.NextBellGroup
import com.example.ui.theme.AcademyIndigo
import com.example.ui.theme.AcademyIndigoDark
import com.example.ui.theme.AcademyIndigoMid
import com.example.ui.theme.EndBellTeal
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.StartBellAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HeroCountdownCard(
    nowMillis: Long,
    masterEnabled: Boolean,
    forceMaxVolume: Boolean,
    nextBellGroup: NextBellGroup?,
    activeRinging: ActiveRingingState?,
    onToggleMaster: (Boolean) -> Unit,
    onStopRinging: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val hmFormatter = SimpleDateFormat("HH:mm", Locale.KOREA)
    val secFormatter = SimpleDateFormat(":ss", Locale.KOREA)
    val dateFormatter = SimpleDateFormat("M월 d일 EEEE", Locale.KOREA)
    val dateObj = Date(nowMillis)
    val currentHm = hmFormatter.format(dateObj)
    val currentSec = secFormatter.format(dateObj)
    val currentDateText = dateFormatter.format(dateObj)

    Column(modifier = modifier.fillMaxWidth()) {
        // Active Ringing Live Banner
        AnimatedVisibility(visible = activeRinging != null) {
            activeRinging?.let { ringing ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = if (compact) 8.dp else 12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.25f)),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(if (compact) 12.dp else 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f),
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.16f),
                                modifier = Modifier.size(40.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = "종소리 울리는 중",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "${ringing.scheduleTitle} · ${ringing.bellType.label} 울리는 중",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = if (ringing.isManualDismissOnly) {
                                        "직접 정지할 때까지 재생"
                                    } else {
                                        "${ringing.remainingSeconds}초 후 자동 종료"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onStopRinging,
                            modifier = Modifier.testTag("banner_stop_bell_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = Color.White,
                            ),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("중지", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Main Hero Timekeeper Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = if (masterEnabled) {
                                listOf(AcademyIndigoDark, AcademyIndigo, AcademyIndigoMid)
                            } else {
                                listOf(Color(0xFF1E293B), Color(0xFF334155))
                            }
                        )
                    )
                    .drawBehind {
                        // Decorative subtle acoustic resonance rings
                        val ringCenter = Offset(size.width * 0.92f, size.height * 0.22f)
                        val ringColor = Color.White.copy(alpha = if (masterEnabled) 0.06f else 0.03f)
                        drawCircle(
                            color = ringColor,
                            radius = size.minDimension * 0.42f,
                            center = ringCenter,
                            style = Stroke(width = 1.5.dp.toPx()),
                        )
                        drawCircle(
                            color = ringColor,
                            radius = size.minDimension * 0.68f,
                            center = ringCenter,
                            style = Stroke(width = 1.2.dp.toPx()),
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.04f),
                            radius = size.minDimension * 0.24f,
                            center = ringCenter,
                        )
                    }
                    .padding(if (compact) 16.dp else 22.dp),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color.White.copy(alpha = 0.13f),
                            ) {
                                Text(
                                    text = currentDateText,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFFE2E8F0),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                )
                            }
                            Spacer(modifier = Modifier.height(if (compact) 6.dp else 10.dp))
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(
                                        SpanStyle(
                                            fontFamily = JetBrainsMonoFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = if (compact) 30.sp else 38.sp,
                                            color = Color.White,
                                        )
                                    ) {
                                        append(currentHm)
                                    }
                                    withStyle(
                                        SpanStyle(
                                            fontFamily = JetBrainsMonoFamily,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = if (compact) 20.sp else 24.sp,
                                            color = Color(0xFF93C5FD),
                                        )
                                    ) {
                                        append(currentSec)
                                    }
                                },
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Switch(
                                checked = masterEnabled,
                                onCheckedChange = onToggleMaster,
                                modifier = Modifier.testTag("master_bell_switch"),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = StartBellAmber,
                                    uncheckedThumbColor = Color(0xFFCBD5E1),
                                    uncheckedTrackColor = Color(0xFF475569),
                                ),
                            )
                            Text(
                                text = if (masterEnabled) "벨 운영 중" else "일시정지",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (masterEnabled) Color(0xFFFDE68A) else Color(0xFF94A3B8),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(if (compact) 12.dp else 16.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = 14.dp,
                                    vertical = if (compact) 10.dp else 13.dp,
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            if (!masterEnabled) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsOff,
                                        contentDescription = null,
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "휴원·일시정지 상태 — 예정된 종이 울리지 않습니다",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFCBD5E1),
                                    )
                                }
                            } else if (nextBellGroup != null) {
                                val primary = nextBellGroup.primaryEvent
                                val remainingMs = (nextBellGroup.triggerAtMillis - nowMillis).coerceAtLeast(0L)
                                val dotColor = if (primary.bellType == BellType.START) {
                                    StartBellAmber
                                } else {
                                    EndBellTeal
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .background(dotColor.copy(alpha = 0.25f), CircleShape),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(9.dp)
                                                .background(dotColor, CircleShape),
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "다음 종 · ${primary.scheduleTitle} ${primary.bellType.label} (${formatHourMin(primary.hour, primary.minute)})",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = Color.White,
                                        )
                                        if (forceMaxVolume) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                                    contentDescription = null,
                                                    tint = Color(0xFFFDE68A),
                                                    modifier = Modifier.size(13.dp),
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "최대 음량 자동 송출 대기",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFFFDE68A),
                                                )
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Surface(
                                    color = Color.Black.copy(alpha = 0.24f),
                                    shape = RoundedCornerShape(10.dp),
                                ) {
                                    Text(
                                        text = formatCountdown(remainingMs),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontFamily = JetBrainsMonoFamily,
                                        ),
                                        color = Color(0xFFFDE68A),
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    )
                                }
                            } else {
                                Text(
                                    text = "등록된 종소리 스케줄이 없습니다. 새 교시를 추가해 주세요.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFDCE7F5),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatHourMin(hour: Int, minute: Int): String =
    String.format(Locale.getDefault(), "%02d:%02d", hour, minute)

private fun formatCountdown(remainingMs: Long): String {
    val totalSec = remainingMs / 1000L
    val days = totalSec / 86400L
    val hours = (totalSec % 86400L) / 3600L
    val minutes = (totalSec % 3600L) / 60L
    val seconds = totalSec % 60L
    return if (days > 0) {
        "${days}일 ${String.format(Locale.getDefault(), "%02d:%02d", hours, minutes)}"
    } else {
        String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    }
}
