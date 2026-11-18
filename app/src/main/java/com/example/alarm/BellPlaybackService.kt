package com.example.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.audio.BellAudioPlayer
import com.example.data.model.BuiltInBellSounds
import com.example.data.repository.BellRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BellPlaybackService : Service() {

    companion object {
        const val ACTION_START_BELL = "com.example.action.START_BELL"
        const val ACTION_STOP_BELL = "com.example.action.STOP_BELL"

        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_BELL_TYPE = "extra_bell_type"
        const val EXTRA_SOUND_ID = "extra_sound_id"
        const val EXTRA_DURATION_SEC = "extra_duration_sec"

        private const val CHANNEL_ID = "bell_ringing_channel"
        private const val NOTIFICATION_ID = 4001

        fun stopService(context: Context) {
            val stopIntent = Intent(context, BellPlaybackService::class.java).apply {
                action = ACTION_STOP_BELL
            }
            context.startService(stopIntent)
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var audioPlayer: BellAudioPlayer
    private lateinit var repository: BellRepository
    private var countdownJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        audioPlayer = BellAudioPlayer(this)
        repository = BellRepository(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_BELL) {
            stopBellAndFinish()
            return START_NOT_STICKY
        }

        val title = intent?.getStringExtra(EXTRA_TITLE) ?: "수업 시간"
        val bellType = runCatching {
            BellType.valueOf(intent?.getStringExtra(EXTRA_BELL_TYPE) ?: BellType.START.name)
        }.getOrDefault(BellType.START)
        val soundId = intent?.getStringExtra(EXTRA_SOUND_ID) ?: BuiltInBellSounds.BUILTIN_BRIGHT_START
        val durationSec = intent?.getIntExtra(EXTRA_DURATION_SEC, 8) ?: 8

        val notification = buildNotification(title, bellType)
        startForeground(NOTIFICATION_ID, notification)

        countdownJob?.cancel()
        countdownJob = serviceScope.launch {
            val prefs = repository.getPreferencesSync()
            val customSound = repository.getCustomSoundByIdSync(soundId)
            val allCustom = repository.getAllCustomSoundsSync()
            val soundName = BuiltInBellSounds.findPresetName(soundId, allCustom)

            val effectiveDuration = if (durationSec < 0) prefs.defaultRingDurationSeconds else durationSec
            val isManualOnly = effectiveDuration == 0

            BellRingingBus.update(
                ActiveRingingState(
                    scheduleTitle = title,
                    bellType = bellType,
                    soundName = soundName,
                    totalDurationSeconds = effectiveDuration,
                    remainingSeconds = effectiveDuration,
                    isManualDismissOnly = isManualOnly,
                )
            )

            audioPlayer.playBell(
                soundId = soundId,
                customFilePath = customSound?.localFilePath,
                forceMaxVolume = prefs.forceMaxVolume,
                restoreVolumeAfter = prefs.restoreVolumeAfterRing,
                loopContinuously = true,
            )

            if (prefs.vibrateOnBell) {
                triggerBellVibration()
            }

            if (!isManualOnly) {
                for (secLeft in effectiveDuration downTo 1) {
                    BellRingingBus.update(
                        ActiveRingingState(
                            scheduleTitle = title,
                            bellType = bellType,
                            soundName = soundName,
                            totalDurationSeconds = effectiveDuration,
                            remainingSeconds = secLeft,
                            isManualDismissOnly = false,
                        )
                    )
                    delay(1000L)
                }
                stopBellAndFinish()
            }
        }

        return START_NOT_STICKY
    }

    private fun stopBellAndFinish() {
        countdownJob?.cancel()
        audioPlayer.stop()
        cancelVibration()
        BellRingingBus.update(null)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(title: String, bellType: BellType): Notification {
        val fullScreenIntent = Intent(this, RingingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_BELL_TYPE, bellType.name)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            10,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val stopIntent = Intent(this, BellPlaybackService::class.java).apply {
            action = ACTION_STOP_BELL
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            11,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val label = "${title} · ${bellType.label} 울리는 중"
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(label)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setContentIntent(fullScreenPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(
                android.R.drawable.ic_media_pause,
                getString(R.string.action_stop_bell),
                stopPendingIntent,
            )
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_ringing_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = getString(R.string.notification_channel_ringing_desc)
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun triggerBellVibration() {
        val pattern = longArrayOf(0, 350, 250, 350)
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(VibratorManager::class.java)
                vm?.defaultVibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                val v = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v?.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    v?.vibrate(pattern, -1)
                }
            }
        }
    }

    private fun cancelVibration() {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getSystemService(VibratorManager::class.java)?.defaultVibrator?.cancel()
            } else {
                @Suppress("DEPRECATION")
                (getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)?.cancel()
            }
        }
    }

    override fun onDestroy() {
        countdownJob?.cancel()
        audioPlayer.stop()
        BellRingingBus.update(null)
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
