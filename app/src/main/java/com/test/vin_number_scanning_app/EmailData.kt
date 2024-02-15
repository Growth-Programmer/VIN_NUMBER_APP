package com.test.vin_number_scanning_app
import java.io.File
data class Email (
    val emailTo: String,
    val subject: String,
    val messageBody: String,
    val attachmentWave: File,

)