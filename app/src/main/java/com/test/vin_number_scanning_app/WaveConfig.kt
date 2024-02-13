package com.test.vin_number_scanning_app

import android.media.AudioFormat
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * Configuration for recording file.
 * @property [sampleRate] the number of samples that audio carried per second.
 * @property [channels] number and position of sound source when the sound is recording.
 * @property [audioEncoding] size of data per sample.
 */

@RequiresApi(Build.VERSION_CODES.S)
data class WaveConfig(
    var sampleRate: Int = 48000,
    var channels: Int = AudioFormat.CHANNEL_IN_STEREO,
    var audioEncoding: Int = AudioFormat.ENCODING_PCM_32BIT
)

internal fun bitPerSample(audioEncoding: Int) = when (audioEncoding) {
    AudioFormat.ENCODING_PCM_8BIT -> 8
    AudioFormat.ENCODING_PCM_16BIT -> 16
    AudioFormat.ENCODING_PCM_32BIT -> 32
    else -> 32
}