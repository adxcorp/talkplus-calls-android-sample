package com.neptune.talkplus_calls_android_sample.extensions

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.core.content.ContextCompat

fun Context.checkPermissionsGranted(permissions: Array<String>): Boolean {
    permissions.forEach { permission ->
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            return false
        }
    }
    return true
}

fun Activity.requirePermission(permissions: Array<String>): Boolean {
    permissions.forEach { permission ->
        if (!shouldShowRequestPermissionRationale(this, permission)) {
            return false
        }
    }
    return true
}

fun Context.showToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

