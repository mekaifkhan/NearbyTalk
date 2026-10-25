package com.example.audio

import android.annotation.SuppressLint
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

class AudioCaptureManager(
    private val onChunkReady: (data: ByteArray, seqNumber: Int) -> Unit
) {
    private val tag = "AudioCaptureManager"
    private var audioRecord: AudioRecord? = null
    private var captureJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private var sequenceCounter = 0

    @SuppressLint("MissingPermission")
    fun startCapture(): Boolean {
        if (_isRecording.value) return true

        val minBufferSize = AudioRecord.getMinBufferSize(
            AudioConstants.SAMPLE_RATE,
            AudioConstants.CHANNEL_IN,
            AudioConstants.ENCODING
        )
        val bufferSize = maxOf(minBufferSize, AudioConstants.BYTES_PER_FRAME * 2)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                AudioConstants.SAMPLE_RATE,
                AudioConstants.CHANNEL_IN,
                AudioConstants.ENCODING,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                // Fallback to standard MIC if VOICE_COMMUNICATION is not supported on certain hardware
                audioRecord?.release()
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    AudioConstants.SAMPLE_RATE,
                    AudioConstants.CHANNEL_IN,
                    AudioConstants.ENCODING,
                    bufferSize
                )
            }

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(tag, "AudioRecord initialization failed")
                return false
            }

            audioRecord?.startRecording()
            _isRecording.value = true
            sequenceCounter = 0

            captureJob = scope.launch {
                val shortBuffer = ShortArray(AudioConstants.SAMPLES_PER_FRAME)
                val byteBuffer = ByteBuffer.allocate(AudioConstants.BYTES_PER_FRAME).order(ByteOrder.LITTLE_ENDIAN)

                while (isActive && _isRecording.value) {
                    val readSamples = audioRecord?.read(shortBuffer, 0, shortBuffer.size) ?: 0
                    if (readSamples > 0) {
                        // Compute RMS amplitude for visualizer
                        var sumSquares = 0.0
                        for (i in 0 until readSamples) {
                            val sample = shortBuffer[i].toDouble()
                            sumSquares += sample * sample
                        }
                        val rms = sqrt(sumSquares / readSamples)
                        // Normalize RMS (range approx 0..32767) to 0.0f..1.0f
                        val normalized = (rms / 8000.0).coerceIn(0.0, 1.0).toFloat()
                        _amplitude.value = normalized

                        // Convert to ByteArray
                        byteBuffer.clear()
                        for (i in 0 until readSamples) {
                            byteBuffer.putShort(shortBuffer[i])
                        }
                        val outBytes = ByteArray(readSamples * 2)
                        System.arraycopy(byteBuffer.array(), 0, outBytes, 0, outBytes.size)

                        onChunkReady(outBytes, sequenceCounter++)
                    }
                }
            }
            return true
        } catch (e: Exception) {
            Log.e(tag, "Failed to start audio recording", e)
            stopCapture()
            return false
        }
    }

    fun stopCapture() {
        _isRecording.value = false
        _amplitude.value = 0f
        captureJob?.cancel()
        captureJob = null

        try {
            if (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                audioRecord?.stop()
            }
            audioRecord?.release()
        } catch (e: Exception) {
            Log.e(tag, "Error stopping audioRecord", e)
        } finally {
            audioRecord = null
        }
    }
}
