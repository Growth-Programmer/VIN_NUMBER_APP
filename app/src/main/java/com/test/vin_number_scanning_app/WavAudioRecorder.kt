package com.test.vin_number_scanning_app

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder


// The purpose of this class is to record audio in real-time from the device microphone,
// and to handle pause and resume actions.
class WavAudioRecorder(private val outputFile: File) {

    interface OnAudioBufferAvailableListener {
        fun onAudioBufferAvailable(buffer: ByteArray)
    }

    // Constructor
    private val audioSource = MediaRecorder.AudioSource.MIC
    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private var bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
    private val pauseLock = Object()
    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    private var fileOutputStream: FileOutputStream? = null

    var isPaused = false
    var isRecording = false
    var onAudioBufferAvailableListener: OnAudioBufferAvailableListener? = null


    // Initializes object
    init {
        try {
            fileOutputStream = FileOutputStream(outputFile)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @SuppressLint("MissingPermission")
    // Starts the recording
    fun startRecording() {
        if (isRecording) return

        audioRecord = AudioRecord(
            audioSource,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )
        if (audioRecord!!.state != AudioRecord.STATE_INITIALIZED) {
            Log.e("AudioRecord", "AudioRecord initialization failed")
        }
        audioRecord?.startRecording()
        isRecording = true


        // This thread run concurrently with main app thread and repeatedly records audio in real-time
        recordingThread = Thread {
            recordAudio()
        }
        writeWavHeader()
        recordingThread?.start()
    }

    private fun recordAudio() {
        val buffer = ByteArray(bufferSize)
        while (isRecording) {
            val bytesRead = audioRecord?.read(buffer, 0, bufferSize) ?: 0
            if (bytesRead > 0) {
                Log.d("AudioRecording", "Bytes read from microphone: $bytesRead")
                synchronized(pauseLock) {
                    while (isPaused) {
                        try {
                            pauseLock.wait()
                        } catch (e: InterruptedException) {
                            e.printStackTrace()
                        }
                    }
                }
                try {
                    fileOutputStream?.write(buffer, 0, bytesRead)
                    val bufferCopy = buffer.copyOf(bytesRead)
                    onAudioBufferAvailableListener?.onAudioBufferAvailable(bufferCopy)

                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun pauseRecording() {
        synchronized(pauseLock) {
            if (isRecording && !isPaused) {
                isPaused = true
            }
        }
    }

    fun resumeRecording() {
        synchronized(pauseLock) {
            if (isRecording && isPaused) {
                isPaused = false
                pauseLock.notifyAll()
            }
        }
    }

    // Stops recording and releases all resources to prevent old data from being used in future calls
    fun stopRecording() {
        if (isRecording || isPaused) {
            isRecording = false
            isPaused = false
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null

            try {
                fileOutputStream?.flush()
                fileOutputStream?.close()
                rewriteWavHeader() // Update the WAV header with correct sizes
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun writeWavHeader() {
        val totalAudioLen = 0L
        val totalDataLen = totalAudioLen + 36
        val longSampleRate = sampleRate.toLong()
        val byteRate = sampleRate * 2 * 1 // sampleRate * 16 (bit rate) * 1 (mono channel) / 8

        val header = ByteArray(44)

        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte() // 'fmt ' chunk
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16 // 4 bytes: size of 'fmt ' chunk
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // format = 1 (PCM)
        header[21] = 0
        header[22] = 1 // mono channel
        header[23] = 0
        header[24] = (longSampleRate and 0xff).toByte()
        header[25] = ((longSampleRate shr 8) and 0xff).toByte()
        header[26] = ((longSampleRate shr 16) and 0xff).toByte()
        header[27] = ((longSampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = 2 // block align
        header[33] = 0
        header[34] = 16 // bits per sample
        header[35] = 0
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (totalAudioLen and 0xff).toByte()
        header[41] = ((totalAudioLen shr 8) and 0xff).toByte()
        header[42] = ((totalAudioLen shr 16) and 0xff).toByte()
        header[43] = ((totalAudioLen shr 24) and 0xff).toByte()

        fileOutputStream?.write(header, 0, 44)
    }

    private fun rewriteWavHeader() {
        try {
            RandomAccessFile(outputFile, "rw").use { randomAccessFile ->
                if (randomAccessFile.length() > 44) {
                    // Calculate the sizes
                    val audioDataLength = randomAccessFile.length() - 44
                    val totalDataLen = audioDataLength + 36

                    // 'RIFF' chunk size (4 bytes after the initial "RIFF")
                    randomAccessFile.seek(4)
                    randomAccessFile.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(totalDataLen.toInt()).array())

                    // 'data' chunk size (4 bytes after "data" header, which starts at byte 36)
                    randomAccessFile.seek(40)
                    randomAccessFile.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(audioDataLength.toInt()).array())
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun getOutputFilePath(): String {
        return outputFile.absolutePath
    }
}