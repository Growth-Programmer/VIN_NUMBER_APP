package com.test.vin_number_scanning_app

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.test.vin_number_scanning_app.databinding.ActivityMainBinding

// Main Activity. This is the first screen the user will see.
class MainActivity : AppCompatActivity() {

    // Member Variables.
    private val cameraPermission = android.Manifest.permission.CAMERA // String constant that stores Android camera permission dialogue.
    private var microphonePermission = android.Manifest.permission.RECORD_AUDIO // String constant that stores Android mic permission dialogue.
    private var lastScannedBarcode: String? = null // Stores the last scanned barcode from ScannerActivity to persist barcode data for other activities. Can be null.

    private lateinit var binding: ActivityMainBinding // Layout Binding.

    // Handles the result of a permission request (Camera).
    private val requestPermissionLauncherCamera =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startScannerActivity()
            }
        }

    // Handles the result of a permission request (Mic).
    private val requestPermissionLauncherMic =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startRecordActivity()
            }
        }


    // Lifecycle callback method. Called whenever the activity is started.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Binds the XML layout and "inflates" it onto the screen.
        binding = ActivityMainBinding.inflate(layoutInflater)

        // Sets the activity's content view to the root view of inflated layout.
        setContentView(binding.root)

        // Listener for when the ScanVIN button is pressed. Starts ScannerActivity depending on camera permission.
        binding.ScanVIN.setOnClickListener {
            requestCameraAndStartScanner()
        }
        // Listener for when the recordButton is pressed. Starts RecordActivity depending on microphone permission.
        binding.recordButton.setOnClickListener {
            requestMicrophoneAndStartRecord()
        }
    }

    /* <-------------------------------------------------------------------------------------------- ACTIVITIES ----------------------------------------------------------------------------------------------------------> */

    // Starts the RecordActivity and passes it the lastScannedBarcode variable to persist its data through another class.
    private fun startRecordActivity() {
        val myIntent = Intent(this, RecordActivity::class.java).apply {
            putExtra("LastScannedBarcode", lastScannedBarcode)
        }
        startActivity(myIntent)
    }

    // Starts ScannerActivity and binds the xml text value to whatever alphanumeric barcode value was scanned, and stores it to lastScannedBarcode.
    private fun startScannerActivity() {
        ScannerActivity.startScanner(this) { barcode ->
            binding.VINOUTPUT.text = barcode
            lastScannedBarcode = "VIN: $barcode"

        }
    }
    /* <-------------------------------------------------------------------------------------------- PERMISSIONS ----------------------------------------------------------------------------------------------------------> */

    // Handles camera permission request.
    private fun requestCameraAndStartScanner() {
        if (isPermissionGranted(cameraPermission)) {
            startScannerActivity()
        } else {
            requestCameraPermission()
        }
    }

    // Handles microphone permission request.
    private fun requestMicrophoneAndStartRecord() {
        if (isPermissionGranted(microphonePermission)) {
            startRecordActivity()
        } else {
            requestMicrophonePermission()
        }
    }

    // Handles permission request if the user initially rejected giving camera access.
    private fun requestCameraPermission() {
        when {
            shouldShowRequestPermissionRationale(cameraPermission) -> {
                cameraPermissionRequest {
                    openPermissionSettings()
                }
            }
            //Otherwise, camera will start.
            else -> {
                requestPermissionLauncherCamera.launch(cameraPermission)
            }

        }
    }

    // Handles permission request if the user initially rejected giving microphone access.
    private fun requestMicrophonePermission() {
        when {
            shouldShowRequestPermissionRationale(microphonePermission) -> {
                microphonePermissionRequest {
                    openPermissionSettings()
                }
            }

            else -> {
                requestPermissionLauncherMic.launch(microphonePermission)
            }
        }
    }
}




