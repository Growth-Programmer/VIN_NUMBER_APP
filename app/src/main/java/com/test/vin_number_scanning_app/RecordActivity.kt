package com.test.vin_number_scanning_app



import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.text.InputType
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.test.vin_number_scanning_app.databinding.ActivityRecorderBinding
import java.io.File
import kotlin.math.sqrt
// Record Activity. Activity where audio is recorded and written to a wave file
class RecordActivity : AppCompatActivity(),
    Timer.OnTimerTickListener, SurfaceHolder.Callback  { // Implements onTimerTickListener to update timer animation and text. SurfaceHolder.Callback to
                                                         // refresh the SurfaceHolder everytime it is loaded from swiping back to RecordActivity.
    private lateinit var binding: ActivityRecorderBinding

    // Member Variables
    private var audioRecorder: WaveRecorder? = null // The object that will record raw audio data and stream it to a wave file.
    private var recordButton: ImageButton? = null // Button that initiates recording.
    private var listOrDoneButton: ImageButton? = null // Button that goes to the SavedRecordingsActivity, or stores a wave file depending on the case.
    private var deleteButton: ImageButton? = null // Deletes the file that is currently being streamed audio data.

    private var lastRecordedFilePath: String? = null // Stores the file path of the most recent stored recording.
    private var currentBufferIndex = 0 // Index for the current position in the waveform buffer.
    private val waveformPaint = Paint() // Waveform color
    private val waveformBuffer = FloatArray(720) // Size of waveform buffer, it can hold up 720 amplitude values.
    private var isWaveformPaused = false // Pauses waveform if recording is paused.

    private lateinit var waveformView: SurfaceView //Surface-view that will display the waveform.
    private lateinit var surfaceHolder: SurfaceHolder // Holds the Surface-view.
    private lateinit var screenTimer: TextView // Timer text.
    private lateinit var timer: Timer // Timer object that allows the program to listen to Timer updates and update the screenTimer text.


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Binds the XML layout and "inflates" it onto the screen.
        binding = ActivityRecorderBinding.inflate(layoutInflater)

        // Sets the activity's content view to the root view of inflated layout.
        setContentView(binding.root)

        // Bind the Waveform-view to the Surface-View in the XML layout.
        // Provide a holder to the WaveformView and add a callback for it to update the Surface-View depending on certain cases.
        waveformView = findViewById(R.id.waveformView)
        surfaceHolder = waveformView.holder
        waveformView.holder.addCallback(this)

        // Waveform art configuration.
        waveformPaint.color = Color.parseColor("#00CED1")
        waveformPaint.strokeWidth = 2f
        waveformPaint.style = Paint.Style.STROKE

        // Buttons and timer text bindings to XML layout.
        recordButton = binding.btnRecord
        listOrDoneButton = binding.btnList
        deleteButton = binding.btnDelete
        screenTimer = binding.audioTimer
        timer = Timer(this)

        // Set image for button.
        recordButton!!.setImageResource(R.drawable.ic_mic)

        // Initially set deleteButton to isEnabled = false since there is no current recording run to delete.
        deleteButton?.apply {
            isEnabled = false
            setImageResource(R.drawable.ic_delete_disabled)
        }

        /* <-------------------------------------------------------------------------------------------- BUTTON LOGIC ----------------------------------------------------------------------------------------------------------> */
        // Button that starts the recording using the WavAudioRecorderObject
        recordButton!!.setOnClickListener {

            // Handles if the recording is paused.
            if (audioRecorder?.isRecording == true && audioRecorder?.isPaused == false) {
                timer.pause()
                audioRecorder?.pauseRecording()
                recordButton!!.setImageResource(R.drawable.ic_mic)// Changes image from pause to microphone.
                isWaveformPaused = true

                // Buttons are Enabled since the recording can be saved or deleted at this point.
                deleteButton!!.isEnabled = true
                listOrDoneButton!!.isEnabled = true
                // The image icons darken when paused, and lighten when recording to signify when they are Enabled or Disabled.
                // It is dark here since this is the paused case.
                deleteButton!!.setImageResource(R.drawable.ic_delete)
                listOrDoneButton!!.setImageResource(R.drawable.ic_done)

            // Handles if the recording is playing.
            } else if (audioRecorder?.isRecording == true && audioRecorder?.isPaused == true) {
                timer.start() // Starts timer
                audioRecorder?.resumeRecording() // Starts recording from where it left off.
                recordButton!!.setImageResource(R.drawable.ic_pause) // Changes image from microphone to pause.
                isWaveformPaused = false

                // Buttons are Disabled since the recording cannot be saved or deleted at this point.
                deleteButton!!.isEnabled = false
                listOrDoneButton!!.isEnabled = false
                // The image icons darken when paused, and lighten when recording to signify when they are Enabled or Disabled.
                // They are light here since this is the recording case.
                deleteButton!!.setImageResource(R.drawable.ic_delete_disabled)
                listOrDoneButton!!.setImageResource(R.drawable.ic_done_disabled)

            } else {
                // Initiates prompting user for a file name.
                promptForFileNameAndStartRecording()
            }
        }
        // This button can either go to the SavedRecordingsActivity which holds a list of all recordings,
        // or completes a recording and stores it into internal storage.
        listOrDoneButton!!.setOnClickListener {

            // This case saves the recording to internal storage.
            if (audioRecorder?.isRecording == true) {
                stopRecording()
                listOrDoneButton!!.setImageResource(R.drawable.ic_list)
                Toast.makeText(this, "Recording Saved", Toast.LENGTH_SHORT).show()

            // This takes the user to the RecordingsSavedActivity.
            } else {
                val myIntent = Intent(this, SavedRecordingsActivity()::class.java)
                startActivity(myIntent)
            }

        }
        // Deletes the current recording run and deletes the file from internal storage that was currently being written to.
        deleteButton!!.setOnClickListener {
            if (audioRecorder?.isRecording == true && audioRecorder?.isPaused == true) {
                stopRecording()
                deleteRecordingFile()

                // Changes ListOrDoneButton to the list image since there is no audio data being taken at the moment.
                listOrDoneButton!!.setImageResource(R.drawable.ic_list)
                listOrDoneButton!!.isEnabled = true

            }
        }

    }
    /* <-------------------------------------------------------------------------------------------- USER INPUT AND RECORDING INITIATION ----------------------------------------------------------------------------------------------------------> */

    // Method for prompting user for a file name for the wave file that will begin being written to after the name is entered.
    private fun promptForFileNameAndStartRecording() {
        val builder = AlertDialog.Builder(this) // Builds alert dialog object
        builder.setTitle("Enter File Name")

        val input = EditText(this) // Edit text widget that allows the user to enter a file name.
        input.inputType = InputType.TYPE_CLASS_TEXT // EditText is set to accept standard input.
        builder.setView(input) // Sets the view of the AlertDialogue to have a text input field where the user can type.

        // Handles case if user presses OK
        builder.setPositiveButton("OK") { _, _ ->
            val fileName = input.text.toString() // Store text to fileName variable.

            // Validate name to see if it follows proper naming rules (A regular expression).
            if (validateFileName(fileName)) {
                startRecordingWithFileName(fileName) // Initiate recording if naming is fine.

            // Return this toast text if naming is incorrect. Recording does not initiate.
            } else {
                Toast.makeText(
                    this,
                    "Invalid file name. No Special characters allowed except \"-\" or \"_\".",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        // Dialog is dismissed if cancel is pressed.
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

        // Displays the alert dialogue on the screen.
        builder.show()
    }

    // Validates a file name depending on the Regular Expression (Regex).
    private fun validateFileName(fileName: String): Boolean {
        // Check for valid file name, return true if valid
        return fileName.matches("[a-zA-Z0-9-_]+".toRegex())
    }

    // Method that starts the recording and takes the filename to write to as the parameter.
    private fun startRecordingWithFileName(fileName: String) {
        // Append .wav if not present and sanitize the file name, and remove any incorrect characters in name if there is any.
        val sanitizedFileName = if (fileName.endsWith(".wav")) fileName else "$fileName.wav"
            .replace("[^a-zA-Z0-9-_.]+".toRegex(), "")

        // Create a file in internal storage.
        val file = File(filesDir, sanitizedFileName)

        // Creates WaveRecorder object, which uses the Audio Record library to take raw audio data.
        audioRecorder = WaveRecorder(file.absolutePath.toString())

        // Listener for audioRecorder that triggers a listener every time there is new audio buffer data set and available during recording.
        audioRecorder?.onAudioBufferListener = { buffer ->
            onAudioBufferAvailable(buffer)
        }

        // Images changes to a pause button, meaning the recorder is currently recording.
        recordButton!!.setImageResource(R.drawable.ic_pause)

        // Change button image to the disabled done icon.
        listOrDoneButton!!.setImageResource(R.drawable.ic_done_disabled)

        // Buttons are disabled since the recording cannot be saved or deleted at this point.
        listOrDoneButton!!.isEnabled = false
        deleteButton!!.isEnabled = false
        isWaveformPaused = false

        // Start recorder and timer object.
        audioRecorder?.startRecording()
        timer.start()

    }
    /* <-------------------------------------------------------------------------------------------- RECORDING STATES ----------------------------------------------------------------------------------------------------------> */

    // Method that stops the recording and saves it to internal storage.
    @SuppressLint("SetTextI18n")
    private fun stopRecording() {

        // Sets the last recorded file path to the output file path of the audio recorder object.
        lastRecordedFilePath = audioRecorder?.getOutputFilePath()
        audioRecorder?.stopRecording() // Stops taking audio data.
        audioRecorder = null // Resets the object to release resources.

        lastRecordedFilePath?.let {
            val recordingFile = File(it) // Creates a new file with the stored file path from lastRecordedFilePath.
            onRecordingCompleted(recordingFile) // Passes the recording file to associate it with a VIN number that was scanned in the ScannerActivity.
        }
        // Resets the screenTimer.
        screenTimer.text = "00:00:00"
        // Stops the timer object.
        timer.stop()

        // Changes image to mic.
        recordButton!!.setImageResource(R.drawable.ic_mic)

        // Delete button is disabled since there is no audio data being recorded at the moment.
        deleteButton!!.isEnabled = false
        deleteButton!!.setImageResource(R.drawable.ic_delete_disabled)

        // Reset the canvas and current buffer index, which are used to draw the waveform animation.
        clearWaveformBuffer()
        currentBufferIndex = 0
        clearCanvas()
    }

    // Handles deleting the current audio recording run.
    private fun deleteRecordingFile() {

        // Finds the file path of the current audio recording run and deletes the file.
        lastRecordedFilePath?.let { filePath ->
            val file = File(filePath)
            if (file.exists() && file.delete()) {
                Toast.makeText(this, "Recording Deleted", Toast.LENGTH_SHORT).show()
                // Reset the path after successful deletion
                lastRecordedFilePath = null
            } else {
                Toast.makeText(
                    this,
                    "Failed to delete recording or file does not exist",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } ?: run {
            Toast.makeText(this, "No recording to delete", Toast.LENGTH_SHORT).show()
        }

    }



    /* <-------------------------------------------------------------------------------------------- WAVEFORM AND TIMER UI ----------------------------------------------------------------------------------------------------------> */

    // Utilizes the interface function which is a callback to the handler loop that "ticks" every 10 milliseconds.
    // This "tick" is listened to by this class to update the timer UI accordingly.
    override fun onTimerTick(duration: String) {
        screenTimer.text = duration

    }

    // Method that takes a buffer which is just an array of bytes that are a chunk of raw audio data.
    private fun onAudioBufferAvailable(buffer: ByteArray) {
        // First checks if the waveform is paused, meaning that the recording has paused.
        if (!isWaveformPaused) {
            val amplitude = processAudioData(buffer) // Method that analyzes the buffer to generate accurate amplitude measurements for the waveform
                                                     // then stores it into the amplitude variable.

            updateWaveformBuffer(amplitude) // This updates the buffer to draw a real-time waveform.

            // drawWaveform is called to paint the waveform on the canvas in real-time. It runs on the UI thread since all UI updates must happen
            // on the main thread.
            runOnUiThread {
                drawWaveform()
            }
        }
    }

    // Method that draws the waveform in real-time
    private fun drawWaveform() {
        // Synchronizes the surface holder so that it can only be accessed by the Main UI Thread.
        synchronized(surfaceHolder) {
            val canvas = surfaceHolder.lockCanvas() // Locks canvas for drawing
            if (canvas != null) {
                try {
                    canvas.drawColor(Color.WHITE)
                    waveformView.setBackgroundColor(Color.TRANSPARENT) // Prevents canvas being obscured by the waveFormView

                    // This iterates over the buffer continuously for amplitude values to apply to generating the waveform shapes. Uses the currentBufferIndex as a starting point.
                    for (i in waveformBuffer.indices) {
                        val bufferIndex = (currentBufferIndex + i) % waveformBuffer.size // Starts from currentBufferIndex for oldest amplitudes, then circles back for newest amplitude.

                        val x = i.toFloat() / waveformBuffer.size * canvas.width // Redraws the entire waveform every time this is called AFTER a new amplitude has been updated in the waveformBuffer.
                        val height = waveformBuffer[bufferIndex] // Amplitude value, which is just the height of a wave or in this case, a slice in the waveform.
                        val rect = RectF(x, canvas.height / 2 - height, x + 1, canvas.height / 2 + height) // Logic for rectangle to be drawn for waveform.
                        canvas.drawRoundRect(rect, 2f, 2f, waveformPaint) // Draws rectangle.
                    }
                } finally {
                    surfaceHolder.unlockCanvasAndPost(canvas) // Ensures that the drawing is posted and the canvas is unlocked for new drawings.
                }
            }
        }
    }

    // Audio Signal Processing Method
    private fun processAudioData(buffer: ByteArray): Float { // buffer is a Byte Array that contains 8bit values.
        // Used to get the mean of the byte array to get amplitude.
        var sum = 0.0

        // Step by two since the audio is processed in 16 bit, and each index hold 8bits values (a byte)
        for (i in buffer.indices step 2) {

            // Second 8 bits are shifted left and first 8 bits are placed after. This follows Little Endian byte placement.
            val sample = ((buffer[i + 1].toInt() shl 8) or (buffer[i].toInt() and 0xFF))

            // Normalizes the sample value to a range of -1.0, and 1.0.
            val normalizedSample = sample / 32768.0  // 16-bit values range from -32768 to 32767. We add one to normalize the range.
            sum += normalizedSample * normalizedSample // Makes sure that the value is positive and emphasizes difference between louder and quieter values.
        }
        // Get mean of buffer, divide it by 2 since it is 16bit values we are doing arithmetic for.
        val mean = sum / (buffer.size / 2)
        // Get the Root Mean Square (RMS), which is the average loudness or amplitude of the chunk of audio data.
        return sqrt(mean).toFloat() * 32768
    }

    // This method updates the buffer array with new amplitudes and updates the currentBufferIndex to correctly redraw the
    // the waveform every iteration in the drawWaveform method
    private fun updateWaveformBuffer(amplitude: Float) {
        waveformBuffer[currentBufferIndex] = amplitude
        currentBufferIndex = (currentBufferIndex + 1) % waveformBuffer.size
    }

    // Redraws the surface-view
    private fun redrawSurfaceView() {
        synchronized(surfaceHolder) {
            val canvas = surfaceHolder.lockCanvas()
            if (canvas != null) {
                try {
                    canvas.drawColor(Color.WHITE)
                    drawWaveform()
                } finally {
                    surfaceHolder.unlockCanvasAndPost(canvas)
                }
            }
        }
    }
    // Resets the buffer by setting all values to 0. Therefore, clearing any waveform drawings.
    private fun clearWaveformBuffer() {
        for (i in waveformBuffer.indices) {
            waveformBuffer[i] = 0f
        }
        redrawSurfaceView()
    }

    // Clears the canvas, making it blank again.
    private fun clearCanvas() {
        synchronized(surfaceHolder) {
            val canvas = surfaceHolder.lockCanvas()
            if (canvas != null) {
                try {
                    canvas.drawColor(Color.WHITE)
                } finally {
                    surfaceHolder.unlockCanvasAndPost(canvas)
                }
            }
        }
    }

    // Callbacks to detect any changes to the surface-view if it was not caused by the code in this class.
    override fun surfaceCreated(holder: SurfaceHolder) {
        redrawSurfaceView()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        redrawSurfaceView()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        synchronized(surfaceHolder) {
            clearWaveformBuffer()
            currentBufferIndex = 0
        }

    }
    /* <-------------------------------------------------------------------------------------------- BARCODE STRING DATA RETRIEVAL ----------------------------------------------------------------------------------------------------------> */

    // Gets the last scanned barcode that was passed to it from Main Activity through "GetStringExtra".
    private fun onRecordingCompleted(recordingFile: File) {

        val barcode = intent.getStringExtra("LastScannedBarcode")
        barcode?.let {

            saveScannedBarcode(recordingFile.name, it)
        }
    }

    // Persistently saves the barcode with its specific recording using the storage "SharedPreferences".
    private fun saveScannedBarcode(recordingIdentifier: String, barcode: String) {
        val sharedPreferences = getSharedPreferences("MySharedPrefs", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString("Barcode_$recordingIdentifier", barcode)
            apply()
        }
    }


}





