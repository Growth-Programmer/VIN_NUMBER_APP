package com.test.vin_number_scanning_app

import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class RecordingsAdapter(
    private val recordings: MutableList<File>,
    private val onItemClicked: (File, ViewHolder) -> Unit // Ensure correct lambda signature
) : RecyclerView.Adapter<RecordingsAdapter.ViewHolder>() {

    private var currentPlayingHolder: ViewHolder? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_recordings, parent, false)
        return ViewHolder(view)
    }



    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val recording = recordings[position]
        holder.tvRecordingName.text = recording.name

        holder.cardView.setOnClickListener {
            currentPlayingHolder?.progressAnimator?.cancel()
            currentPlayingHolder = holder
            onItemClicked(recording, holder)
        }
    }


    override fun getItemCount(): Int = recordings.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvRecordingName: TextView = itemView.findViewById(R.id.tvRecordingName)
        val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)
        val cardView: CardView = itemView.findViewById(R.id.cardViewId) // Replace with actual ID
        var progressAnimator: ObjectAnimator? = null

        fun startProgressAnimation(max: Int) {
            progressBar.visibility = View.VISIBLE
            progressBar.max = max
            progressBar.progress = 0
            progressAnimator = ObjectAnimator.ofInt(progressBar, "progress", 0, max)
            progressAnimator?.duration = max.toLong()  // Duration equal to the length of the recording
            progressAnimator?.start()
        }

        fun stopProgressAnimation() {
            progressAnimator?.cancel()
            progressBar.visibility = View.GONE
        }

    }



    fun updateRecordings(newRecordings: List<File>) {
        // Detect additions and removals
        val oldRecordings = HashSet(recordings)
        val newRecordingsSet = HashSet(newRecordings)

        // Handle removals
        oldRecordings
            .filterNot { it in newRecordingsSet }
            .forEach { file ->
                val index = recordings.indexOf(file)
                recordings.removeAt(index)
                notifyItemRemoved(index)
            }

        // Handle additions
        newRecordings
            .filterNot { it in oldRecordings }
            .forEach { file ->
                val index = newRecordings.indexOf(file)
                recordings.add(index, file)
                notifyItemInserted(index)
            }
    }

}