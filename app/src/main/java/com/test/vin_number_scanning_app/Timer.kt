package com.test.vin_number_scanning_app

import android.os.Handler
import android.os.Looper

// Timer Class designed to keep track of elapsed time and notify a listener
// about the current time in a formatted string. The class uses "System.nanoTime()" for
// measuring time, which provides high-resolution time stamps.
class Timer(private val listener: OnTimerTickListener) {

    // Interface for a callback method, used to send updated information to other parts of
    // application.
    interface OnTimerTickListener {
        fun onTimerTick(duration: String)
    }

    // Uses Handler to run this task on the UI thread, in order to update Timer UI.
    private var handler = Handler(Looper.getMainLooper())
    private var startTime = 0L
    private var pausedTime = 0L
    private var elapsedTime = 0L
    // Code to update the timer in the background will be ran in its own thread.
    private lateinit var runnable: Runnable

    // Initializes a runnable block that calculates elapsed time and formats it into
    // a readable string. Schedules itself to run again after a delay, creating a loop
    init {
        runnable = Runnable {
            elapsedTime = if (pausedTime == 0L) {
                System.nanoTime() - startTime // Calculates the elapsed time with no paused time.
            } else {
                pausedTime - startTime // Calculates elapsed time if the timer has been paused. pausedTime stores the time that System.nanoTime
                                       // was at when the pause method was called. This is used as a reference point when restarting the timer.
            }
            val durationInMillis = elapsedTime / 1_000_000 // Convert elapsed time to milliseconds.
            listener.onTimerTick(format(durationInMillis)) // Listener updates the format to reflect updates.
            handler.postDelayed(runnable, 10) // Update Timer every 10 milliseconds.
        }
    }

    // Sets the start time and schedules the runnable to be executed after a delay. If the
    // timer is resuming from a pause, it adjusts the start time to account for paused duration.
    fun start() {
        if (pausedTime != 0L) {
            startTime += System.nanoTime() - pausedTime // Shifts the start point by the amount of elapsed time that passed
                                                        // while the recording was paused. This will help reflect the correct
                                                        // resumption time.
            pausedTime = 0L  // Resets paused time.
        } else {
            startTime = System.nanoTime() // Stores the starting time, and is used as reference point to calculate elapsed time.
        }
        handler.postDelayed(runnable, 10) // Updates Timer every 10 milliseconds.
    }

    // Captures the current time as "pausedTime" and stops the runnable from executing
    // effectively pausing the timer.
    fun pause() {
        pausedTime = System.nanoTime()
        handler.removeCallbacks(runnable)
    }

    // Stops the runnable and resets the timer's state.
    fun stop() {
        handler.removeCallbacks(runnable)
        startTime = 0L
        pausedTime = 0L
        elapsedTime = 0L
    }

    // This method converts a duration in milliseconds into a formatted string of hours, minutes, seconds, and tenths of a second.
    private fun format(milliseconds: Long): String {
        val seconds: Long = (milliseconds / 1000) % 60
        val minutes: Long = (milliseconds / (1000 * 60)) % 60
        val hours: Long = milliseconds / (1000 * 60 * 60)
        val tenths = (milliseconds % 1000) / 10 // Displaying tenths of a second.

        // Formatting
        return if (hours > 0)
            "%02d:%02d:%02d:%02d".format(hours, minutes, seconds, tenths)
        else
            "%02d:%02d:%02d".format(minutes, seconds, tenths)
    }
}
