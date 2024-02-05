package com.test.vin_number_scanning_app

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.test.vin_number_scanning_app.databinding.ActivityScannerBinding
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors


//Scanner activity class
class ScannerActivity : AppCompatActivity() {

    //Objects needed for class such a binding, which phone camera to use, getting the camera instance
    //and showing the camera in scanner activity screen.
    private lateinit var binding: ActivityScannerBinding
    private lateinit var cameraSelector: CameraSelector
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var processCameraProvider: ProcessCameraProvider
    private lateinit var cameraPreview: Preview
    private lateinit var imageAnalysis: ImageAnalysis

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Binds content view.
        binding = ActivityScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Predefined camera to use if given access.
        cameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
        //Gets context instance of camera in use.
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        //Gets instance of camera and stores it.
        cameraProviderFuture.addListener({ processCameraProvider = cameraProviderFuture.get()
            bindCameraPreview()
            bindInputAnalyzer()},
            ContextCompat.getMainExecutor(this)
        )
    }
    //Binds camera view in order for it in to appear on the screen.
    private fun bindCameraPreview(){
        cameraPreview = Preview.Builder()
            .setTargetRotation(binding.previewView.display.rotation)
            .build()

        cameraPreview.setSurfaceProvider(binding.previewView.surfaceProvider)
        processCameraProvider.bindToLifecycle(this, cameraSelector, cameraPreview)
        processCameraProvider.bindToLifecycle(this, cameraSelector, cameraPreview)
    }

    //Builds the scanner for the camera to read/interpret barcodes.
    private fun bindInputAnalyzer(){
        val barcodeScanner : BarcodeScanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_CODE_128)
                .build()
        )

        //Builds image analyzer for a phone camera to read images.
        imageAnalysis = ImageAnalysis.Builder()
            .setTargetRotation(binding.previewView.display.rotation)
            .build()

        //Allowing concurrent execution for camera. Camera will execute on a remote thread for itself only.
        val cameraExecutor = Executors.newSingleThreadExecutor()

        //The image analyzer takes the single executor thread and sets it as the image analyzer.
        //All image analysis will occur on THIS thread.
        imageAnalysis.setAnalyzer(cameraExecutor) {
            imageProxy -> processImageProxy(barcodeScanner, imageProxy)
        }

        //Lifecycle of the camera, collects camera data such as when it opens, closes and when it is analyzing an image.
        processCameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis)

    }

    //Ignores "UnsafeOptInUsage" error.
    @SuppressLint("UnsafeOptInUsageError")
    private fun processImageProxy(barcodeScanner: BarcodeScanner, imageProxy: ImageProxy) {
        val inputImage = InputImage.fromMediaImage(imageProxy.image!!, imageProxy.imageInfo.rotationDegrees)

        barcodeScanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                if (barcodes.isNotEmpty()) {
                    barcodes.first().rawValue?.let { onScan?.invoke(it) }
                    onScan = null
                    finish()
                }
            }
            .addOnFailureListener {
                it.printStackTrace()
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    companion object {
        private var onScan: ((String) -> Unit)? = null

        fun startScanner(context: Context, onScan: (String) -> Unit) {
            this.onScan = onScan
            Intent(context, ScannerActivity::class.java).also { context.startActivity(it) }
        }
    }
}