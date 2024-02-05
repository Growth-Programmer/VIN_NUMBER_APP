package com.test.vin_number_scanning_app

import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class RecordingsAdapter(
    val recordings: MutableList<File>,
    val recordingDates: MutableList<String>,
    val recordingDurations: MutableList<String>,
    val recordingBarcodes: MutableList<String>,
    private val onPlayClicked: (File, ViewHolder) -> Unit,
    private val onRestartClicked: (File, ViewHolder) -> Unit,
    private val onDeleteClicked: (Int) -> Unit
) : RecyclerView.Adapter<RecordingsAdapter.ViewHolder>() {

    private var selectedPosition: Int = RecyclerView.NO_POSITION


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_recordings, parent, false)
        return ViewHolder(view)
    }



    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val recording = recordings[position]
        holder.tvRecordingName.text = recording.name // Access file name from File object
        holder.tvRecordingDate.text = recordingDates[position] // Access date based on position
        holder.tvRecordingDuration.text = recordingDurations[position] // Access duration based on position
        holder.tvRecordingBarcodes.text = recordingBarcodes[position]

        val isSelected = position == selectedPosition

        holder.tvRecordingDate.visibility = View.VISIBLE
        holder.tvRecordingDuration.visibility = View.VISIBLE
        holder.btnPlayPause.visibility = if (isSelected) View.VISIBLE else View.GONE
        holder.btnRestart.visibility = if (isSelected) View.VISIBLE else View.GONE
        holder.btnDeleteRecording.visibility = if (isSelected) View.VISIBLE else View.GONE
        holder.progressBar.visibility = if (isSelected) View.VISIBLE else View.GONE
        holder.cardView.isSelected = isSelected


        holder.cardView.setOnClickListener {
            val previousSelectedPosition = selectedPosition
            selectedPosition = if (isSelected) RecyclerView.NO_POSITION else position

            notifyItemChanged(previousSelectedPosition)
            notifyItemChanged(selectedPosition)

        }

        holder.btnPlayPause.setOnClickListener {
            onPlayClicked(recording, holder)
        }

        holder.btnRestart.setOnClickListener {
            onRestartClicked(recording, holder)
        }

        holder.btnDeleteRecording.setOnClickListener {
            onDeleteClicked(position)
        }
    }


    override fun getItemCount(): Int = recordings.size

    fun deleteItem(position: Int) {
        if (position == selectedPosition) {
            // Reset the current playing position and state
            selectedPosition = RecyclerView.NO_POSITION
        }
        recordings.removeAt(position)
        notifyItemRemoved(position)

        // Notify any moved items to update their state
        notifyItemRangeChanged(position, itemCount - position)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvRecordingDate: TextView = itemView.findViewById(R.id.tvRecordingDate)
        val tvRecordingDuration: TextView = itemView.findViewById(R.id.tvRecordingDuration)
        val tvRecordingBarcodes: TextView = itemView.findViewById(R.id.tvRecordingBarcodes)
        val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)
        val tvRecordingName: TextView = itemView.findViewById(R.id.tvRecordingName)
        val cardView: CardView = itemView.findViewById(R.id.cardViewId)
        val btnPlayPause: ImageButton = itemView.findViewById(R.id.btnPlayPause)
        val btnRestart: ImageButton = itemView.findViewById(R.id.btnRestart)
        val btnDeleteRecording: ImageButton = itemView.findViewById(R.id.btnDeleteRecording)
        var progressAnimator: ObjectAnimator? = null

        fun startProgressAnimation(max: Int) {
            progressBar.visibility = View.VISIBLE
            progressBar.max = max
            progressBar.progress = 0
            progressAnimator = ObjectAnimator.ofInt(progressBar, "progress", 0, max)
            progressAnimator?.duration = max.toLong()  // Duration equal to the length of the recording
            progressAnimator?.start()
        }

        fun resetProgressAnimation() {
            progressBar.progress = 0
            progressAnimator?.cancel()
            progressBar.visibility = View.GONE
        }


    }


    fun updateRecordings(newRecordingData: List<RecordingData>) {
        // Detect additions and removals
        val oldRecordingsSet = recordings.toSet()
        val newRecordingsSet = newRecordingData.map { it.file }.toSet()

        // Handle removals
        oldRecordingsSet.forEach { file ->
            if (file !in newRecordingsSet) {
                val index = recordings.indexOf(file)
                if (index != -1) {
                    recordings.removeAt(index)
                    recordingDates.removeAt(index)
                    recordingDurations.removeAt(index)
                    recordingBarcodes.removeAt(index)
                    notifyItemRemoved(index)
                }
            }
        }

        // Handle additions and updates
        newRecordingData.forEachIndexed { newIndex, recordingData ->
            val existingIndex = recordings.indexOf(recordingData.file)
            if (existingIndex == -1) {
                // New item
                recordings.add(recordingData.file)
                recordingDates.add(recordingData.date)
                recordingDurations.add(recordingData.duration)
                recordingBarcodes.add(recordingData.barcode ?: "No Barcode")
                notifyItemInserted(recordings.size - 1)
            } else {
                // Update existing item
                recordingDates[existingIndex] = recordingData.date
                recordingDurations[existingIndex] = recordingData.duration
                recordingBarcodes[existingIndex] = recordingData.barcode ?: "No Barcode"
                if (existingIndex != newIndex) {
                    // If item has moved, adjust the list
                    val movedRecording = recordings.removeAt(existingIndex)
                    recordings.add(newIndex, movedRecording)
                    notifyItemMoved(existingIndex, newIndex)
                }
                notifyItemChanged(existingIndex) // Update the item
            }
        }
    }

}