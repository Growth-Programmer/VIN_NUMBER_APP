package com.test.vin_number_scanning_app

import java.io.File

// RecordingData.kt
data class RecordingData(
    val file: File,
    val duration: String, // Formatted duration string
    val date: String      // Formatted date string
)