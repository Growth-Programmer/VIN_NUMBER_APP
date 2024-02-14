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

// Adapter class for a RecyclerView to display a list of recordings.
class RecordingsAdapter(
    val recordings: MutableList<File>,              // List of recording files.
    val recordingDates: MutableList<String>,        // List of recording dates.
    val recordingDurations: MutableList<String>,    // List of recording durations.
    val recordingBarcodes: MutableList<String>,     // List of associated barcodes for each recording.
    private val onPlayClicked: (File, ViewHolder) -> Unit,   // Callback when play button is clicked.
    private val onRestartClicked: (File, ViewHolder) -> Unit,// Callback when restart button is clicked.
    private val onSendEmail: (File, ViewHolder) -> Unit,
    private val onDeleteClicked: (Int) -> Unit              // Callback when delete button is clicked.
) : RecyclerView.Adapter<RecordingsAdapter.ViewHolder>() {

    // Holds the position of the currently selected item in the RecyclerView.
    private var selectedPosition: Int = RecyclerView.NO_POSITION

    // Called when RecyclerView needs a new ViewHolder of the given type to represent an item.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Inflate the layout for each item of the RecyclerView.
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_recordings, parent, false)
        return ViewHolder(view)
    }

    // Called by RecyclerView to display the data at the specified position.
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Resets the state every update to clear any UI resources from card deletions.
        holder.resetViewHolderState()
        val recording = recordings[position]
        holder.tvRecordingName.text = recording.name            // Set the file name.
        holder.tvRecordingDate.text = recordingDates[position]  // Set the recording date.
        holder.tvRecordingDuration.text = recordingDurations[position] // Set the recording duration.
        holder.tvRecordingBarcodes.text = recordingBarcodes[position]  // Set the barcode.

        // Determine if the current item is selected.
        val isSelected = position == selectedPosition

        // Set visibility of controls based on whether the item is selected.
        holder.tvRecordingDate.visibility = View.VISIBLE
        holder.tvRecordingDuration.visibility = View.VISIBLE
        holder.btnPlayPause.visibility = if (isSelected) View.VISIBLE else View.GONE
        holder.btnRestart.visibility = if (isSelected) View.VISIBLE else View.GONE
        holder.btnEmail.visibility = if (isSelected) View.VISIBLE else View.GONE
        holder.btnDeleteRecording.visibility = if (isSelected) View.VISIBLE else View.GONE
        holder.progressBar.visibility = if (isSelected) View.VISIBLE else View.GONE
        holder.cardView.isSelected = isSelected

        // Set click listeners for the card and buttons.
        holder.cardView.setOnClickListener {
            val previousSelectedPosition = selectedPosition
            selectedPosition = if (isSelected) RecyclerView.NO_POSITION else position
            notifyItemChanged(previousSelectedPosition)
            notifyItemChanged(selectedPosition)
        }

        holder.btnPlayPause.setOnClickListener { onPlayClicked(recording, holder) }
        holder.btnRestart.setOnClickListener { onRestartClicked(recording, holder) }
        holder.btnEmail.setOnClickListener{ onSendEmail(recording, holder) }
        holder.btnDeleteRecording.setOnClickListener { onDeleteClicked(position) }
    }

    // Returns the total number of items in the data set held by the adapter.
    override fun getItemCount(): Int = recordings.size


    // ViewHolder class provides a reference to the views for each data item.
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvRecordingName: TextView = itemView.findViewById(R.id.tvRecordingName)
        val tvRecordingDate: TextView = itemView.findViewById(R.id.tvRecordingDate)
        val tvRecordingDuration: TextView = itemView.findViewById(R.id.tvRecordingDuration)
        val tvRecordingBarcodes: TextView = itemView.findViewById(R.id.tvRecordingBarcodes)

        val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)
        val cardView: CardView = itemView.findViewById(R.id.cardViewId)

        val btnPlayPause: ImageButton = itemView.findViewById(R.id.btnPlayPause)
        val btnRestart: ImageButton = itemView.findViewById(R.id.btnRestart)
        val btnEmail: ImageButton = itemView.findViewById(R.id.btnEmail)
        val btnDeleteRecording: ImageButton = itemView.findViewById(R.id.btnDeleteRecording)
        var progressAnimator: ObjectAnimator? = null

        // Starts the animation for the progress bar.
        fun startProgressAnimation(max: Int) {
            progressBar.max = max
            progressBar.progress = 0
            progressAnimator = ObjectAnimator.ofInt(progressBar, "progress", 0, max)
            progressAnimator?.duration = max.toLong()
            progressAnimator?.start()
        }

        // Resets the progress bar animation.
        fun resetProgressAnimation() {
            progressBar.progress = 0
            progressAnimator?.cancel()
            progressBar.visibility = View.GONE
        }

        fun resetViewHolderState() {
            // Reset any specific states here, such as visibility of buttons or progress bar
            progressBar.visibility = View.GONE
            progressBar.progress = 0
            btnPlayPause.visibility = View.GONE
            btnRestart.visibility = View.GONE
            btnDeleteRecording.visibility = View.GONE
        }
    }



    // Updates the adapter with new data.
    fun updateRecordings(newRecordingData: List<RecordingData>) {
        // Logic to update and refresh the adapter's data based on new data received.
        // This includes handling new items, deleted items, and updated items.
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