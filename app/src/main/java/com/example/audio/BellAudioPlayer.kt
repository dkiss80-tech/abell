package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import com.example.data.model.BuiltInBellSounds
import java.io.File

class BellAudioPlayer(private val context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var mediaPlayer: MediaPlayer? = null
    private var focusRequest: AudioFocusRequest? = null
    private var previousAlarmVolume: Int? = null
    private var shouldRestoreVolume: Boolean = false

    fun playBell(
        soundId: String,
        customFilePath: String?,
        forceMaxVolume: Boolean,
        restoreVolumeAfter: Boolean,
        loopContinuously: Boolean,
        onCompleted: () -> Unit = {},
    ) {
        stop()

        if (forceMaxVolume) {
            val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            previousAlarmVolume = currentVol
            shouldRestoreVolume = restoreVolumeAfter
            runCatching {
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVol, 0)
            }
        }

        requestAlarmAudioFocus()

        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val player = MediaPlayer().apply {
            setAudioAttributes(attrs)
            isLooping = loopContinuously
            setOnCompletionListener {
                if (!loopContinuously) {
                    stop()
                    onCompleted()
                }
            }
            setOnErrorListener { _, _, _ ->
                stop()
                onCompleted()
                true
            }
        }

        val configured = configureDataSource(player, soundId, customFilePath)
        if (!configured) {
            // Safe fallback to classic built-in chime
            val fallback = ChimeSynthesizer.getOrCreateChimeFile(
                context,
                BuiltInBellSounds.BUILTIN_CLASSIC_4,
            )
            player.reset()
            player.setAudioAttributes(attrs)
            player.isLooping = loopContinuously
            player.setDataSource(fallback.absolutePath)
        }

        try {
            player.prepare()
            player.start()
            mediaPlayer = player
        } catch (_: Exception) {
            runCatching { player.release() }
            restoreVolumeAndFocus()
            onCompleted()
        }
    }

    fun stop() {
        mediaPlayer?.let { mp ->
            runCatching {
                if (mp.isPlaying) mp.stop()
            }
            runCatching { mp.reset() }
            runCatching { mp.release() }
        }
        mediaPlayer = null
        restoreVolumeAndFocus()
    }

    private fun configureDataSource(
        player: MediaPlayer,
        soundId: String,
        customFilePath: String?,
    ): Boolean {
        return try {
            when {
                soundId == BuiltInBellSounds.SYSTEM_ALARM -> {
                    val uri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    player.setDataSource(context, uri)
                    true
                }
                customFilePath != null && File(customFilePath).exists() -> {
                    player.setDataSource(customFilePath)
                    true
                }
                soundId.startsWith("builtin_") -> {
                    val file = ChimeSynthesizer.getOrCreateChimeFile(context, soundId)
                    player.setDataSource(file.absolutePath)
                    true
                }
                else -> {
                    val file = ChimeSynthesizer.getOrCreateChimeFile(
                        context,
                        BuiltInBellSounds.BUILTIN_CLASSIC_4,
                    )
                    player.setDataSource(file.absolutePath)
                    true
                }
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun requestAlarmAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(attrs)
                .setWillPauseWhenDucked(false)
                .build()
            focusRequest = req
            runCatching { audioManager.requestAudioFocus(req) }
        } else {
            @Suppress("DEPRECATION")
            runCatching {
                audioManager.requestAudioFocus(
                    null,
                    AudioManager.STREAM_ALARM,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT,
                )
            }
        }
    }

    private fun restoreVolumeAndFocus() {
        if (shouldRestoreVolume) {
            previousAlarmVolume?.let { prev ->
                runCatching {
                    audioManager.setStreamVolume(AudioManager.STREAM_ALARM, prev, 0)
                }
            }
        }
        previousAlarmVolume = null
        shouldRestoreVolume = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { req ->
                runCatching { audioManager.abandonAudioFocusRequest(req) }
            }
            focusRequest = null
        } else {
            @Suppress("DEPRECATION")
            runCatching { audioManager.abandonAudioFocus(null) }
        }
    }
}
