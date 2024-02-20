package com.test.vin_number_scanning_app

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat

//This checks if the program has a specific permission or not.
fun Context.isPermissionGranted(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}
//Rationale dialog box. Appears when camera access is not given.
inline fun Context.cameraPermissionRequest(crossinline positive: () -> Unit){
    AlertDialog.Builder(this)
        .setTitle("Camera Permission Required")
        .setMessage("Without accessing the camera, it is not possible to scan VIN Numbers...")
        .setPositiveButton("Allow Camera") { _, _ ->
            positive.invoke()
        }
        .setNegativeButton("Cancel") { _, _ ->
        }
        .show()
}
//Rationale dialog box. Appears when microphone access is not given.
inline fun Context.microphonePermissionRequest(crossinline positive: () -> Unit){
    AlertDialog.Builder(this)
        .setTitle("Audio Permission Required")
        .setMessage("Without accessing the microphone, it is not possible to record audio")
        .setPositiveButton("Allow Microphone") { _, _ ->
            positive.invoke()
        }
        .setNegativeButton("Cancel") { _, _ ->
        }
        .show()
}

//If user accepts program camera access, this method will open their settings for them to give the
//program access.
fun Context.openPermissionSettings() {
    Intent(ACTION_APPLICATION_DETAILS_SETTINGS).also{
        val uri: Uri = Uri.fromParts("package", packageName, null)
        it.data =uri
        startActivity(it)
    }
}