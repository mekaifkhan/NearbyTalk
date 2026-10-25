package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

class AudioPlaybackManager {
    private val tag = "AudioPlaybackManager"
    private var audioTrack: AudioTrack? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private var playbackJob: Job? = null
    private val audioQueue = Channel<ByteArray>(Channel.UNLIMITED)

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    @Synchronized
    fun startPlayback() {
        if (_isPlaying.value) return

        val minBufferSize = AudioTrack.getMinBufferSize(
            AudioConstants.SAMPLE_RATE,
            AudioConstants.CHANNEL_OUT,
            AudioConstants.ENCODING
        )
        val bufferSize = maxOf(minBufferSize, AudioConstants.BYTES_PER_FRAME * 4)

        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            val audioFormat = AudioFormat.Builder()
                .setSampleRate(AudioConstants.SAMPLE_RATE)
                .setChannelMask(AudioConstants.CHANNEL_OUT)
                .setEncoding(AudioConstants.ENCODING)
                .build()

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(audioAttributes)
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.play()
            _isPlaying.value = true

            playbackJob = scope.launch {
                val shortBuffer = ShortArray(AudioConstants.SAMPLES_PER_FRAME)
                while (isActive && _isPlaying.value) {
                    val chunk = audioQueue.receiveCatching().getOrNull() ?: break
                    val track = audioTrack ?: break

                    // Compute RMS for UI visualizer
                    if (chunk.isNotEmpty()) {
                        val byteBuf = ByteBuffer.wrap(chunk).order(ByteOrder.LITTLE_ENDIAN)
                        val numShorts = chunk.size / 2
                        var sumSquares = 0.0
                        for (i in 0 until numShorts) {
                            if (byteBuf.hasRemaining()) {
                                val s = byteBuf.short.toDouble()
                                sumSquares += s * s
                            }
                        }
                        if (numShorts > 0) {
                            val rms = sqrt(sumSquares / numShorts)
                            val normalized = (rms / 8000.0).coerceIn(0.0, 1.0).toFloat()
                            _amplitude.value = normalized
                        }
                    }

                    // Write to AudioTrack in blocking mode (runs on IO thread)
                    track.write(chunk, 0, chunk.size, AudioTrack.WRITE_BLOCKING)
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to start AudioTrack", e)
            stopPlayback()
        }
    }

    fun playChunk(data: ByteArray) {
        if (!_isPlaying.value) {
            startPlayback()
        }
        audioQueue.trySend(data)
    }

    @Synchronized
    fun stopPlayback() {
        _isPlaying.value = false
        _amplitude.value = 0f
        playbackJob?.cancel()
        playbackJob = null

        // Drain pending queue
        while (audioQueue.tryReceive().isSuccess) {}

        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            Log.e(tag, "Error releasing AudioTrack", e)
        } finally {
            audioTrack = null
        }
    }
}
