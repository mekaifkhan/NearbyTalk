package com.example.audio

import android.media.AudioFormat

object AudioConstants {
    const val SAMPLE_RATE = 16000 // 16kHz PCM (Standard for low-latency voice walkie-talkie)
    const val CHANNEL_IN = AudioFormat.CHANNEL_IN_MONO
    const val CHANNEL_OUT = AudioFormat.CHANNEL_OUT_MONO
    const val ENCODING = AudioFormat.ENCODING_PCM_16BIT

    // 40 ms frame size at 16kHz = 640 samples = 1280 bytes
    const val SAMPLES_PER_FRAME = 640
    const val BYTES_PER_FRAME = SAMPLES_PER_FRAME * 2
}
