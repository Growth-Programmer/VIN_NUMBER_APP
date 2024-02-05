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
        val oldRecordings = HashSet(recordings)
        val newRecordingsSet = HashSet(newRecordingData.map { it.file })

        // Handle removals
        oldRecordings
            .filterNot { it in newRecordingsSet }
            .forEach { file ->
                val index = recordings.indexOf(file)
                recordings.removeAt(index)
                recordingDates.removeAt(index)
                recordingDurations.removeAt(index)
                notifyItemRemoved(index)
            }

        // Handle additions and updates
        newRecordingData.forEachIndexed { newIndex, recordingData ->
            val existingIndex = recordings.indexOf(recordingData.file)
            if (existingIndex == -1) {
                // New item
                recordings.add(newIndex, recordingData.file)
                recordingDates.add(newIndex, recordingData.date)
                recordingDurations.add(newIndex, recordingData.duration)
                notifyItemInserted(newIndex)
            } else if (existingIndex != newIndex) {
                // Item moved within the list
                recordings.removeAt(existingIndex)
                recordingDates.removeAt(existingIndex)
                recordingDurations.removeAt(existingIndex)
                recordings.add(newIndex, recordingData.file)
                recordingDates.add(newIndex, recordingData.date)
                recordingDurations.add(newIndex, recordingData.duration)
                notifyItemMoved(existingIndex, newIndex)
                notifyItemChanged(newIndex) // Update the item at the new position
            } else {
                // Item already in the same position
                if (recordingDates[existingIndex] != recordingData.date || recordingDurations[existingIndex] != recordingData.duration) {
                    recordingDates[existingIndex] = recordingData.date
                    recordingDurations[existingIndex] = recordingData.duration
                    notifyItemChanged(existingIndex) // Update the item if date or duration changed
                }
            }
        }
    }

}