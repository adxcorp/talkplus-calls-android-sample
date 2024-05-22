package com.neptune.talkplus_calls_android_sample.feature.intro

import android.Manifest
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.neptune.talkplus_calls_android_sample.databinding.ActivityMainBinding
import com.neptune.talkplus_calls_android_sample.extensions.checkPermissionsGranted
import com.neptune.talkplus_calls_android_sample.extensions.requirePermission
import com.neptune.talkplus_calls_android_sample.extensions.showToast
import com.neptune.talkplus_calls_android_sample.feature.call.CallActivity

class MainActivity : AppCompatActivity() {
    private val binding: ActivityMainBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.btnStartCall.setOnClickListener { checkVideoCallPermissions() }
    }

    private fun moveCallActivity() = with(binding)  {
        val intent = Intent(this@MainActivity, CallActivity::class.java).apply {
            putExtra(CallActivity.INTENT_EXTRA_CALLER_ID, etCallerId.text.toString())
            putExtra(CallActivity.INTENT_EXTRA_CALLEE_ID, etCalleeId.text.toString())
        }
        val isValid = (etCalleeId.text.toString().trim().isNotEmpty() && etCallerId.text.toString().trim().isNotEmpty())
        if (isValid) startActivity(intent) else showToast("input caller, callee id")
    }

    private fun checkVideoCallPermissions() {
        when (checkPermissionsGranted(permissions)) {
            true -> moveCallActivity()
            false -> requestVideoCallPermission()
        }
    }

    private fun requestVideoCallPermission(dialogShown: Boolean = false) {
        when (requirePermission(permissions) && !dialogShown) {
            true -> showPermissionDialog()
            false -> requestPermission()
        }
    }

    private fun showPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("카메라, 오디오 권한 요청")
            .setMessage("영상통화를 위해 카메라, 오디오 기능이 필요합니다.")
            .setPositiveButton("수락") { dialog, _ ->
                dialog.dismiss()
                requestVideoCallPermission(true)
            }
            .setNegativeButton("거절") { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(this, "카메라, 오디오 권한이 거절됐습니다.", Toast.LENGTH_LONG).show()
            }
            .show()
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(this, permissions, CAMERA_AUDIO_PERMISSION_REQUEST_CODE)
    }

    companion object {
        private const val CAMERA_PERMISSION = Manifest.permission.CAMERA
        private const val AUDIO_PERMISSION = Manifest.permission.RECORD_AUDIO
        private const val NOTIFICATION_PERMISSION = Manifest.permission.POST_NOTIFICATIONS

        private const val CAMERA_AUDIO_PERMISSION_REQUEST_CODE = 1

        private val permissions: Array<String> = arrayOf(
            CAMERA_PERMISSION,
            AUDIO_PERMISSION,
            NOTIFICATION_PERMISSION
        )
    }
}