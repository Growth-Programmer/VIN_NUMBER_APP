package com.test.vin_number_scanning_app

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.IOException

class SavedRecordingsActivity : AppCompatActivity() {

    private lateinit var recordingsAdapter: RecordingsAdapter
    private lateinit var recyclerView: RecyclerView
    private var mediaPlayer: MediaPlayer? = null
    private var currentPlayingPosition: Int = RecyclerView.NO_POSITION
    private var currentPlayingHolder: RecordingsAdapter.ViewHolder? = null
    private var handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_saved_recordings)
        recyclerView = findViewById(R.id.recyclerViewRecordings)

        recordingsAdapter = RecordingsAdapter(mutableListOf()) { file, holder ->
            if (currentPlayingPosition != holder.bindingAdapterPosition) {
                stopCurrentPlayback()
                currentPlayingPosition = holder.bindingAdapterPosition
                currentPlayingHolder?.stopProgressAnimation()

                mediaPlayer = MediaPlayer().apply {
                    try {
                        setDataSource(file.absolutePath)
                        prepare()
                        start()
                        holder.startProgressAnimation(duration)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
                currentPlayingHolder = holder
            }
        }
        recyclerView.adapter = recordingsAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        loadRecordings()
    }


    private fun loadRecordings() {
        val internalStorageDir = filesDir
        val filesList = internalStorageDir.walk()
            .filter { it.isFile && it.extension.equals("wav", ignoreCase = true) }
            .toList()

        recordingsAdapter.updateRecordings(filesList)
    }

    private fun stopCurrentPlayback() {
        mediaPlayer?.release()
        mediaPlayer = null
        currentPlayingHolder?.stopProgressAnimation()
        currentPlayingPosition = RecyclerView.NO_POSITION
    }
}
