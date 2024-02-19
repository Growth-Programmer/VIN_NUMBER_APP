package com.test.vin_number_scanning_app

import android.app.Dialog
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.lifecycleScope
import com.test.vin_number_scanning_app.RecordActivity.Companion.vinsAndRecordingsDataStore
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit


class SavedRecordingsActivity : AppCompatActivity() {


    private val handler = Handler(Looper.getMainLooper()) // Handler to get main thread.

    private var mediaPlayer: MediaPlayer? = null // MediaPlayers instance for playing audio files.

    private var currentPlayingPosition: Int = RecyclerView.NO_POSITION // Tracks the position of currently playing recordings in the list.

    private var currentPlayingHolder: RecordingsAdapter.ViewHolder? = null // Holds a reference to the ViewHolder of the currently playing item.

    private lateinit var recordingsAdapter: RecordingsAdapter //Adapter for managing and displaying the list of recordings.

    private lateinit var recyclerView: RecyclerView // Recycler View for displaying the list of recordings.

    private lateinit var progressRunnable: Runnable // Runnable for updating the progress of the currently playing recording.

    // Called on the start of the Activity
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_saved_recordings)

        // Set up RecyclerView and its Adapter
        recyclerView = findViewById(R.id.recyclerViewRecordings)
        recordingsAdapter = RecordingsAdapter(
            mutableListOf(),
            mutableListOf(),
            mutableListOf(),
            mutableListOf(),
            this::playRecording,
            this::restartRecording,
            this::sendEmail,
            this::deleteRecording,

        )
        recyclerView.adapter = recordingsAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Define a Runnable to update the playback progress.
        progressRunnable = Runnable { // Initialize the runnable here
            mediaPlayer?.let {
                if (it.isPlaying) {
                    val progress = it.currentPosition
                    currentPlayingHolder?.progressBar?.progress = progress
                }
                handler.postDelayed(progressRunnable, 100)
            }
        }
        // Coroutine
        lifecycleScope.launch {
            // Load Recordings
            loadRecordings()
        }
    }

    // Handles playback of a recording.
    private fun playRecording(file: File, holder: RecordingsAdapter.ViewHolder) {
        if (currentPlayingPosition != holder.bindingAdapterPosition) {
            resetCurrentPlayback()

            currentPlayingPosition = holder.bindingAdapterPosition
            currentPlayingHolder = holder

            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnPreparedListener { mp ->
                    mp.start()
                    holder.progressBar.max = mp.duration
                    holder.progressBar.progress = 0
                    startUpdatingProgressBar(mp) // Start updating progress bar with MediaPlayer reference
                }
                setOnCompletionListener {
                    stopUpdatingProgressBar() // Stop progress bar update on completion
                    holder.progressBar.progress = holder.progressBar.max
                }
                prepareAsync()
            }
        }
    }

    // Load data from recordings synchronously
    private suspend fun loadRecordings() {
        val internalStorageDir = filesDir
        val filesList = internalStorageDir.walk()
            .filter { it.isFile && it.extension.equals("wav", ignoreCase = true) }
            .toList()

        val recordingDataList = filesList.map { file ->
            val lastModifiedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(file.lastModified()))
            val duration = getAudioFileDuration(file)

            // Use file name or another unique identifier as the key to retrieve the barcode
            val barcode = getBarcodeForRecording(file.name)

            RecordingData(file, duration, lastModifiedDate, barcode ?: "No Barcode")
        }

        recordingsAdapter.updateRecordings(recordingDataList)
    }

    // Restarts the playback
    private fun restartRecording(file: File, holder: RecordingsAdapter.ViewHolder) {
        resetCurrentPlayback()

        currentPlayingPosition = holder.bindingAdapterPosition
        currentPlayingHolder = holder

        mediaPlayer = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            setOnPreparedListener { mp ->
                mp.start()
                holder.progressBar.max = mp.duration
                holder.progressBar.progress = 0
                startUpdatingProgressBar(mp) // Start updating the progress bar
            }
            setOnCompletionListener {
                stopUpdatingProgressBar() // Stop progress bar update on completion
                holder.progressBar.progress = holder.progressBar.max
            }
            prepareAsync()
        }
    }

    // Delete the recording and its associated data from storage.
    private fun deleteRecording(position: Int) {
        if (position < 0 || position >= recordingsAdapter.recordings.size) {
            return // Ensure the position is valid
        }

        val fileToDelete = recordingsAdapter.recordings[position]
        val keyToDelete = stringPreferencesKey(fileToDelete.name)

        if (fileToDelete.exists()) {
            // Coroutine
            lifecycleScope.launch {
                vinsAndRecordingsDataStore.edit { preferences ->
                    preferences.remove(keyToDelete)
                Log.d("File", "$keyToDelete was deleted.")
                }
            }
            fileToDelete.delete()
        }

        // Clear the data for the deleted item
        recordingsAdapter.recordings.removeAt(position)
        recordingsAdapter.recordingDates.removeAt(position)
        recordingsAdapter.recordingDurations.removeAt(position)
        recordingsAdapter.recordingBarcodes.removeAt(position)

        // Update adapter and UI
        recordingsAdapter.notifyItemRemoved(position)
        recordingsAdapter.notifyItemRangeChanged(position, recordingsAdapter.itemCount)

        // Additional logic if needed, such as stopping the MediaPlayer
        if (currentPlayingPosition == position) {
            resetCurrentPlayback()
            currentPlayingHolder?.resetProgressAnimation()
        } else if (currentPlayingPosition > position) {
            // Adjust the current playing position if an item above it was removed
            currentPlayingPosition--
        }
    }

    // Sends the email with the .wav file attached
    private fun sendEmail(position: Int){

        val fileToEmail = recordingsAdapter.recordings[position]
        val emailDialog =  Dialog(this)

        emailDialog.setContentView(R.layout.email_dialog)

        val emailBinding : EditText = emailDialog.findViewById(R.id.edit_email)
        val subjectBinding : EditText = emailDialog.findViewById(R.id.edit_subject)
        val messageBinding : EditText = emailDialog.findViewById(R.id.edit_message_text)
        val attachmentBinding : TextView = emailDialog.findViewById((R.id.attachment))
        val sendButton : Button = emailDialog.findViewById(R.id.confirmSend)

        attachmentBinding.text = fileToEmail.name.toString()

        emailDialog.show()

        sendButton.setOnClickListener{
            val emailString = emailBinding.text.toString()
            val subject = subjectBinding.text.toString()
            val message = messageBinding.text.toString()

            val waveFileUri = FileProvider.getUriForFile(this, "com.test.vin_number_scanning_app.provider", fileToEmail)
            val emailRecipients = emailString.split(",").map{it.trim()}


            val emailIntent = Intent(Intent.ACTION_SEND)
            emailIntent.type = "text/plain"
            emailIntent.putExtra(Intent.EXTRA_EMAIL, emailRecipients.toTypedArray())
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject)
            emailIntent.putExtra(Intent.EXTRA_TEXT, message)
            emailIntent.putExtra(Intent.EXTRA_STREAM, waveFileUri)
            emailIntent.setType("message/rfc822")

            startActivity(Intent.createChooser(emailIntent, "Choose an email client"))

        }
    }


    // Used to reset or clear any UI or resources that remain after deleting from storage.
    private fun resetCurrentPlayback() {
        mediaPlayer?.apply {
            stopUpdatingProgressBar() // Stop progress bar updates first
            if (isPlaying) {
                stop()
            }
            reset()
            release()
        }
        mediaPlayer = null
        currentPlayingHolder?.progressBar?.progress = 0 // Reset progress bar
        currentPlayingHolder = null
        currentPlayingPosition = RecyclerView.NO_POSITION
    }


    // Starts updating the progress bar based on the MediaPlayer's progress
    private fun startUpdatingProgressBar(mediaPlayer: MediaPlayer) {
        val totalDuration = mediaPlayer.duration
        val updateInterval = (totalDuration / 100).coerceAtMost(100) // Coerce the value to be at least 100 milliseconds

        handler.removeCallbacks(progressRunnable) // Remove any existing callbacks
        progressRunnable = object : Runnable {
            override fun run() {
                if (mediaPlayer.isPlaying && currentPlayingHolder != null) {
                    val progress = mediaPlayer.currentPosition
                    currentPlayingHolder?.progressBar?.progress = progress
                    handler.postDelayed(this, updateInterval.toLong())
                }
            }
        }
        handler.post(progressRunnable)
    }

    private fun stopUpdatingProgressBar() {
        handler.removeCallbacks(progressRunnable)
    }

    // Retrieves the duration of an audio file.
    private fun getAudioFileDuration(file: File): String {
        val mediaPlayer = MediaPlayer()
        var durationFormatted = "Duration: Unknown"
        try {
            mediaPlayer.setDataSource(file.absolutePath)
            mediaPlayer.prepare()
            val durationInMillis = mediaPlayer.duration
            durationFormatted = formatDuration(durationInMillis)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaPlayer.release()
        }
        return durationFormatted
    }

    // Formats the duration from milliseconds to a readable string format.
    private fun formatDuration(durationInMillis: Int): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationInMillis.toLong())
        val seconds = TimeUnit.MILLISECONDS.toSeconds(durationInMillis.toLong()) - TimeUnit.MINUTES.toSeconds(minutes)
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    // Retrieves the associated barcode for a recording from SharedPreferences.
    private suspend fun getBarcodeForRecording(recordingIdentifier: String): String? {
        val fileNameKey = stringPreferencesKey(recordingIdentifier)
        return vinsAndRecordingsDataStore.data
            .map{ preferences ->
                preferences[fileNameKey]
            }
            .firstOrNull()
    }
}

