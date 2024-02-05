package com.test.vin_number_scanning_app

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit


class SavedRecordingsActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private var mediaPlayer: MediaPlayer? = null
    private var currentPlayingPosition: Int = RecyclerView.NO_POSITION
    private var currentPlayingHolder: RecordingsAdapter.ViewHolder? = null
    private lateinit var recordingsAdapter: RecordingsAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressRunnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_saved_recordings)

        recyclerView = findViewById(R.id.recyclerViewRecordings)
        recordingsAdapter = RecordingsAdapter(
            mutableListOf(),
            mutableListOf(),
            mutableListOf(),
            this::playRecording,
            this::restartRecording,
            this::deleteRecording
        )
        recyclerView.adapter = recordingsAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        progressRunnable = Runnable { // Initialize the runnable here
            mediaPlayer?.let {
                if (it.isPlaying) {
                    val progress = it.currentPosition
                    currentPlayingHolder?.progressBar?.progress = progress
                }
                handler.postDelayed(progressRunnable, 1000)
            }
        }


        loadRecordings()
    }

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
                    holder.startProgressAnimation(mp.duration)
                    startUpdatingProgressBar()
                }
                setOnCompletionListener {
                    holder.progressBar.progress = holder.progressBar.max
                }
                prepareAsync()
            }
        }
    }



    private fun loadRecordings() {
        val internalStorageDir = filesDir
        val filesList = internalStorageDir.walk()
            .filter { it.isFile && it.extension.equals("wav", ignoreCase = true) }
            .toList()
        val recordingDataList = filesList.map { file ->
            val lastModifiedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(file.lastModified()))
            val duration = getAudioFileDuration(file)
            RecordingData(file, duration, lastModifiedDate)
        }

        recordingsAdapter.updateRecordings(recordingDataList)
    }

    private fun restartRecording(file: File, holder: RecordingsAdapter.ViewHolder) {
        // Reset current playback
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            reset()
            release()
        }
        mediaPlayer = null

        // Reset the progress bar and start the animation from the beginning
        holder.progressBar.progress = 0
        holder.startProgressAnimation(holder.progressBar.max) // Assuming max is already set

        // Set up and start the MediaPlayer for the selected recording
        mediaPlayer = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            setOnPreparedListener { mp ->
                mp.start()
                startUpdatingProgressBar() // Begin updating the progress bar
            }
            prepareAsync()
        }
    }

    private fun deleteRecording(position: Int) {
        if (position < 0 || position >= recordingsAdapter.recordings.size) {
            return // Ensure the position is valid
        }

        val fileToDelete = recordingsAdapter.recordings[position]
        if (fileToDelete.exists()) {
            fileToDelete.delete()
        }

        // Clear the data for the deleted item
        recordingsAdapter.recordings.removeAt(position)
        recordingsAdapter.recordingDates.removeAt(position)
        recordingsAdapter.recordingDurations.removeAt(position)

        // Update adapter and UI
        recordingsAdapter.notifyItemRemoved(position)

        // Additional logic if needed, such as stopping the MediaPlayer
        if (currentPlayingPosition == position) {
            resetCurrentPlayback()
            currentPlayingHolder?.resetProgressAnimation()
        } else if (currentPlayingPosition > position) {
            // Adjust the current playing position if an item above it was removed
            currentPlayingPosition--
        }
    }




    private fun resetCurrentPlayback() {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            reset()
            release()
        }
        mediaPlayer = null
        handler.removeCallbacks(progressRunnable)
        currentPlayingHolder?.resetProgressAnimation()
        currentPlayingPosition = RecyclerView.NO_POSITION

    }

    private fun startUpdatingProgressBar() {
        handler.post(progressRunnable)
    }


    override fun onStop() {
        super.onStop()
        handler.removeCallbacks(progressRunnable)
        resetCurrentPlayback()
    }

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

    private fun formatDuration(durationInMillis: Int): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationInMillis.toLong())
        val seconds = TimeUnit.MILLISECONDS.toSeconds(durationInMillis.toLong()) - TimeUnit.MINUTES.toSeconds(minutes)
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}

