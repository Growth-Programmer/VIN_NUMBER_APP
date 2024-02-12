package com.test.vin_number_scanning_app

import java.io.File

// Data class for dat associated with a complete recording file.
data class RecordingData(
    val file: File, // File
    val duration: String, // Formatted duration string
    val date: String,     // Formatted date string
    val barcode: String // Barcode string
)