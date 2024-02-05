package com.test.vin_number_scanning_app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.test.vin_number_scanning_app.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {
    //Activity XML binding. Camera and Microphone permission value.
    private val cameraPermission = android.Manifest.permission.CAMERA
    private var microphonePermission = android.Manifest.permission.RECORD_AUDIO
    private var lastScannedBarcode: String? = null

    private lateinit var binding: ActivityMainBinding

    //Stores permission result and calls start scanner method if camera access is given.
    private val requestPermissionLauncherCamera =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startScanner()
            }
        }
    //Stores permission result of microphone and starts the Record Activity screen if microphone access is given.
    private val requestPermissionLauncherMic =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startRecordActivity()
            }
        }


    //Set opening content view when the app starts.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //ActivityMainBinding is the first view on app start.
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Button Listener in root binding view.
        binding.ScanVIN.setOnClickListener {
            requestCameraAndStartScanner()
        }
        //Button Listener for when the user wants to record. Switches for main activity to record activity.
        binding.recordButton.setOnClickListener {
            requestMicrophone()
        }
    }
    private fun startRecordActivity() {
        val myIntent = Intent(this, RecordActivity::class.java).apply {
            putExtra("LastScannedBarcode", lastScannedBarcode)
        }
        startActivity(myIntent)
    }

    //Checks if camera permissions have been granted or not.
    private fun requestCameraAndStartScanner() {
        if (isPermissionGranted(cameraPermission)) {
            startScanner()
        } else {
            requestCameraPermission()
        }
    }

    private fun requestMicrophone() {
        if (isPermissionGranted(microphonePermission)) {
            startRecordActivity()
        } else {
            requestMicrophonePermission()
        }
    }

    //Function to start barcode scanner.
    private fun startScanner() {
        ScannerActivity.startScanner(this) { barcode ->
            binding.VINOUTPUT.text = barcode
            lastScannedBarcode = "VIN: $barcode"

        }
    }
    //If the user has NOT given permissions, a rationale will appear to confirm their decision.
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

    //If the user has NOT given permissions, a rationale will appear to confirm their decision.
    private fun requestMicrophonePermission() {
        when {
            shouldShowRequestPermissionRationale(microphonePermission) -> {
                microphonePermissionRequest {
                    openPermissionSettings()
                }
            }
            //Otherwise, microphone will start listening
            else -> {
                requestPermissionLauncherMic.launch(microphonePermission)
            }
        }
    }
}




