package com.example.audio

import android.content.Context
import com.example.data.model.BuiltInBellSounds
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

object ChimeSynthesizer {
    private const val SAMPLE_RATE = 44100

    private data class NoteSpec(
        val freqHz: Double,
        val startSec: Double,
        val durationSec: Double,
        val amplitude: Double = 0.72,
    )

    fun getOrCreateChimeFile(context: Context, soundId: String): File {
        val dir = File(context.filesDir, "builtin_chimes").apply { mkdirs() }
        val file = File(dir, "$soundId.wav")
        if (file.exists() && file.length() > 44L) return file

        val notes = when (soundId) {
            BuiltInBellSounds.BUILTIN_BRIGHT_START -> listOf(
                // C5 - E5 - G5 - C6 bright rising chime
                NoteSpec(523.25, 0.00, 0.75, 0.70),
                NoteSpec(659.25, 0.42, 0.75, 0.72),
                NoteSpec(783.99, 0.84, 0.85, 0.75),
                NoteSpec(1046.50, 1.26, 1.55, 0.82),
            )
            BuiltInBellSounds.BUILTIN_WARM_END -> listOf(
                // G5 - E5 - D5 - C5 warm descending cadence
                NoteSpec(783.99, 0.00, 0.75, 0.72),
                NoteSpec(659.25, 0.48, 0.75, 0.70),
                NoteSpec(587.33, 0.96, 0.80, 0.68),
                NoteSpec(523.25, 1.44, 1.65, 0.78),
            )
            BuiltInBellSounds.BUILTIN_WESTMINSTER -> listOf(
                // Classic Westminster quarters
                NoteSpec(659.25, 0.00, 0.70, 0.70), // E5
                NoteSpec(587.33, 0.55, 0.70, 0.70), // D5
                NoteSpec(523.25, 1.10, 0.70, 0.72), // C5
                NoteSpec(392.00, 1.65, 1.20, 0.78), // G4
                NoteSpec(392.00, 2.75, 0.70, 0.70), // G4
                NoteSpec(587.33, 3.30, 0.70, 0.72), // D5
                NoteSpec(659.25, 3.85, 0.70, 0.72), // E5
                NoteSpec(523.25, 4.40, 1.50, 0.80), // C5
            )
            else -> listOf(
                // Classic 4-note school chime (C5 - E5 - G5 - C5)
                NoteSpec(523.25, 0.00, 0.85, 0.72),
                NoteSpec(659.25, 0.55, 0.85, 0.74),
                NoteSpec(783.99, 1.10, 0.85, 0.76),
                NoteSpec(523.25, 1.65, 1.65, 0.80),
            )
        }

        val totalSeconds = (notes.maxOfOrNull { it.startSec + it.durationSec } ?: 3.0) + 0.35
        val totalSamples = (totalSeconds * SAMPLE_RATE).toInt()
        val mixBuffer = DoubleArray(totalSamples)

        for (note in notes) {
            val startSample = (note.startSec * SAMPLE_RATE).toInt()
            val lenSamples = (note.durationSec * SAMPLE_RATE).toInt()
            val omega = 2.0 * PI * note.freqHz
            for (i in 0 until lenSamples) {
                val idx = startSample + i
                if (idx >= totalSamples) break
                val t = i.toDouble() / SAMPLE_RATE
                val attack = if (t < 0.012) t / 0.012 else 1.0
                val decay = exp(-3.1 * t / note.durationSec)
                // Fundamental + gentle chime 2nd & 3rd partials for metallic bell resonance
                val wave = 0.72 * sin(omega * t) +
                    0.20 * sin(omega * 2.0 * t) * exp(-4.5 * t) +
                    0.08 * sin(omega * 3.0 * t) * exp(-7.0 * t)
                mixBuffer[idx] += note.amplitude * attack * decay * wave
            }
        }

        // Normalize safely to prevent clipping
        val peak = mixBuffer.maxOfOrNull { kotlin.math.abs(it) }?.coerceAtLeast(1.0) ?: 1.0
        val pcmBytes = ByteArray(totalSamples * 2)
        val bb = ByteBuffer.wrap(pcmBytes).order(ByteOrder.LITTLE_ENDIAN)
        for (sample in mixBuffer) {
            val clamped = (sample / peak * 0.92 * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            bb.putShort(clamped.toShort())
        }

        writeWavFile(file, pcmBytes)
        return file
    }

    private fun writeWavFile(file: File, pcmData: ByteArray) {
        val dataSize = pcmData.size
        val chunkSize = 36 + dataSize
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt(chunkSize)
            put("WAVE".toByteArray(Charsets.US_ASCII))
            put("fmt ".toByteArray(Charsets.US_ASCII))
            putInt(16) // PCM subchunk size
            putShort(1.toShort()) // AudioFormat = 1 (PCM)
            putShort(1.toShort()) // Mono
            putInt(SAMPLE_RATE)
            putInt(SAMPLE_RATE * 2) // ByteRate
            putShort(2.toShort()) // BlockAlign
            putShort(16.toShort()) // BitsPerSample
            put("data".toByteArray(Charsets.US_ASCII))
            putInt(dataSize)
        }.array()

        FileOutputStream(file).use { out ->
            out.write(header)
            out.write(pcmData)
        }
    }
}
