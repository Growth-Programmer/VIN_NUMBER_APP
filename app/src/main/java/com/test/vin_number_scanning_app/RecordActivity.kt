package com.test.vin_number_scanning_app



import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.test.vin_number_scanning_app.databinding.ActivityRecorderBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.io.File
import kotlin.math.sqrt

class RecordActivity : AppCompatActivity(),
    Timer.OnTimerTickListener,
    WavAudioRecorder.OnAudioBufferAvailableListener {
    private lateinit var binding: ActivityRecorderBinding

    // Variables needed for later building the .wav file and dynamic record button
    private var audioRecorder: WavAudioRecorder? = null
    private var dateFormat = SimpleDateFormat("yyyy.MM.DD_hh.mm.ss")
    private var date = dateFormat.format(Date())

    private var recordButton: ImageButton? = null
    private var listOrDoneButton: ImageButton? = null
    private var deleteButton: ImageButton? = null

    private var lastRecordedFilePath : String? = null
    private var currentBufferIndex = 0
    private val waveformPaint = Paint()
    private val waveformBuffer = FloatArray(720) // Adjust size as needed


    private lateinit var waveformView: SurfaceView
    private lateinit var surfaceHolder: SurfaceHolder
    private lateinit var screenTimer: TextView
    private lateinit var timer: Timer


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecorderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        waveformView = findViewById(R.id.waveformView)
        surfaceHolder = waveformView.holder
        waveformPaint.color = Color.parseColor("#D8BFD8")
        waveformPaint.strokeWidth = 2f
        waveformPaint.style = Paint.Style.STROKE

        recordButton = binding.btnRecord
        listOrDoneButton = binding.btnList
        deleteButton = binding.btnDelete
        screenTimer = binding.audioTimer
        timer = Timer(this)


        recordButton!!.setImageResource(R.drawable.ic_mic)

        deleteButton?.apply {
            isEnabled = false
            setImageResource(R.drawable.ic_delete_disabled)
        }

        // Button that starts the recording using the WavAudioRecorderObject
        recordButton!!.setOnClickListener {
            if (audioRecorder?.isRecording == true && audioRecorder?.isPaused == false) {
                timer.pause()
                audioRecorder?.pauseRecording()
                recordButton!!.setImageResource(R.drawable.ic_mic)

                deleteButton!!.isEnabled = true
                listOrDoneButton!!.isEnabled = true

                deleteButton!!.setImageResource(R.drawable.ic_delete)
                listOrDoneButton!!.setImageResource(R.drawable.ic_done)

            } else if (audioRecorder?.isRecording == true && audioRecorder?.isPaused == true) {
                timer.start()
                audioRecorder?.resumeRecording()
                recordButton!!.setImageResource(R.drawable.ic_pause)

                deleteButton!!.isEnabled = false
                listOrDoneButton!!.isEnabled = false

                deleteButton!!.setImageResource(R.drawable.ic_delete_disabled)
                listOrDoneButton!!.setImageResource(R.drawable.ic_done_disabled)

            } else {
                promptForFileNameAndStartRecording()
            }
        }

        listOrDoneButton!!.setOnClickListener {
            if (audioRecorder?.isRecording == true) {
                stopRecording()
                listOrDoneButton!!.setImageResource(R.drawable.ic_list)
                Toast.makeText(this, "Recording Saved", Toast.LENGTH_SHORT).show()
            }

        }

        deleteButton!!.setOnClickListener{
            if (audioRecorder?.isRecording == true && audioRecorder?.isPaused == true) {
                stopRecording()
                deleteRecordingFile()
                listOrDoneButton!!.setImageResource(R.drawable.ic_list)
                listOrDoneButton!!.isEnabled = true

            }
        }

    }

    // Method that starts the recording
    private fun startRecordingWithFileName(fileName: String) {
        // Append .wav if not present and sanitize the file name
        val sanitizedFileName = if (fileName.endsWith(".wav")) fileName else "$fileName.wav"
            .replace("[^a-zA-Z0-9-_\\.]+".toRegex(), "")

        // Create a file in internal storage
        val file = File(filesDir, sanitizedFileName)
        audioRecorder = WavAudioRecorder(file)
        audioRecorder!!.onAudioBufferAvailableListener = this

        recordButton!!.setImageResource(R.drawable.ic_pause)
        listOrDoneButton!!.setImageResource(R.drawable.ic_done_disabled)

        listOrDoneButton!!.isEnabled = false
        deleteButton!!.isEnabled = false

        audioRecorder?.startRecording()
        timer.start()

    }

    // Method that stops the recording
    private fun stopRecording() {
        lastRecordedFilePath = audioRecorder?.getOutputFilePath()
        audioRecorder?.stopRecording()
        audioRecorder = null

        screenTimer.text= "00:00:00"
        timer.stop()

        recordButton!!.setImageResource(R.drawable.ic_mic)

        deleteButton!!.isEnabled = false
        deleteButton!!.setImageResource(R.drawable.ic_delete_disabled)

        clearWaveformBuffer()
        redrawSurfaceView()
    }

    private fun promptForFileNameAndStartRecording() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Enter File Name")

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT
        builder.setView(input)

        builder.setPositiveButton("OK") { _, _ ->
            val fileName = input.text.toString()
            if (validateFileName(fileName)) {
                startRecordingWithFileName(fileName)
            } else {
                Toast.makeText(this,
                    "Invalid file name. No Special characters allowed except \"-\" or \"_\".", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

        builder.show()
    }

    private fun deleteRecordingFile() {
        lastRecordedFilePath?.let { filePath ->
            val file = File(filePath)
            if (file.exists() && file.delete()) {
                Toast.makeText(this, "Recording Deleted", Toast.LENGTH_SHORT).show()
                // Reset the path after successful deletion
                lastRecordedFilePath = null
            } else {
                Toast.makeText(this, "Failed to delete recording or file does not exist", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(this, "No recording to delete", Toast.LENGTH_SHORT).show()
        }
    }



    private fun validateFileName(fileName: String): Boolean {
        // Check for valid file name, return true if valid
        return fileName.matches("[a-zA-Z0-9-_]+".toRegex())
    }


    override fun onTimerTick(duration: String) {

        screenTimer.text = duration

    }

    override fun onAudioBufferAvailable(buffer: ByteArray) {
        val amplitude = processAudioData(buffer)
        updateWaveformBuffer(amplitude)

        runOnUiThread {
            drawWaveform()
        }
    }

    private fun drawWaveform() {
        val canvas = surfaceHolder.lockCanvas()
        if (canvas != null) {
            canvas.drawColor(Color.WHITE) // Clear the canvas
            waveformView.setBackgroundColor(Color.TRANSPARENT)

            // Draw the waveform
            for (i in waveformBuffer.indices) {
                val bufferIndex = (currentBufferIndex + i) % waveformBuffer.size
                val x = i.toFloat() / waveformBuffer.size * canvas.width
                val height = waveformBuffer[bufferIndex] // Amplitude value
                val rect = RectF(x, canvas.height / 2 - height, x + 1, canvas.height / 2 + height)
                canvas.drawRoundRect(rect, 2f, 2f, waveformPaint)
            }

            surfaceHolder.unlockCanvasAndPost(canvas)
        }
    }

    private fun processAudioData(buffer: ByteArray): Float {
        var sum = 0.0
        for (i in buffer.indices step 2) {
            // Combine two bytes to form a 16-bit short value
            val sample = ((buffer[i + 1].toInt() shl 8) or (buffer[i].toInt() and 0xFF))
            // Normalize sample to be between -1.0 and 1.0
            val normalizedSample = sample / 32768.0
            sum += normalizedSample * normalizedSample
        }

        val mean = sum / (buffer.size / 2)
        return sqrt(mean).toFloat() * 32768 // Scale back up to original amplitude range
    }
    private fun updateWaveformBuffer(amplitude: Float) {
        waveformBuffer[currentBufferIndex] = amplitude
        currentBufferIndex = (currentBufferIndex + 1) % waveformBuffer.size
    }

    private fun clearWaveformBuffer() {
        for (i in waveformBuffer.indices) {
            waveformBuffer[i] = 0f
        }
    }

    private fun redrawSurfaceView() {
        val canvas = surfaceHolder.lockCanvas()
        if (canvas != null) {
            canvas.drawColor(Color.WHITE) // Clear the canvas with a white color
            drawWaveform() // You may want to modify this method to handle an empty buffer
            surfaceHolder.unlockCanvasAndPost(canvas)
        }
    }

}





