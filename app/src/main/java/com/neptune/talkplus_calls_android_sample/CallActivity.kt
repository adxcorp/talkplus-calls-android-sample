package com.neptune.talkplus_calls_android_sample

import android.Manifest
import android.content.Context
import android.media.AudioManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.neptune.talkplus_calls_android_sample.Constant.TEST_CHANNEL_ID
import com.neptune.talkplus_calls_android_sample.databinding.ActivityCallBinding
import com.neptune.talkplus_calls_android_sample.extensions.checkPermissionsGranted
import com.neptune.talkplus_calls_android_sample.extensions.intentSerializable
import com.neptune.talkplus_calls_android_sample.extensions.requirePermission
import com.neptune.talkplus_calls_android_sample.extensions.showToast
import com.neptune.talkpluscallsandroid.webrtc.model.TalkPlusCall
import io.talkplus.entity.user.TPNotificationPayload
import kotlinx.coroutines.launch

class CallActivity : AppCompatActivity() {
    private val binding: ActivityCallBinding by lazy { ActivityCallBinding.inflate(layoutInflater) }
    private val callViewModel: CallViewModel by lazy { ViewModelProvider(this)[CallViewModel::class.java] }
//    private val audioManager by lazy { getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setClickListener()
        observeCallUiState()
    }

    private fun handleCallState() {

    }

    private fun observeCallUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                callViewModel.callState.collect { callUiState ->
                    when (callUiState) {
                        is CallUiState.Login -> { showToast(callUiState.tpUser.toString()) }
                        is CallUiState.Fail -> { showToast(callUiState.failResult.toString()) }
                    }
                }
            }
        }
    }

    private fun setClickListener() = with(binding) {
        ivAudio.setOnClickListener { toggleAudio() }
        ivVideo.setOnClickListener { toggleVideo() }
        ivEndCall.setOnClickListener { endCall() }
    }

    private fun toggleVideo() {
        when (callViewModel.isEnableLocalVideo) {
            true -> binding.ivVideo.setBackgroundResource(R.drawable.ic_video_off)
            false -> binding.ivVideo.setBackgroundResource(R.drawable.ic_video_on)
        }
        callViewModel.setLocalVideo(!callViewModel.isEnableLocalVideo)
    }

    private fun toggleAudio() {
        when (callViewModel.isEnableLocalAudio) {
            true -> binding.ivAudio.setBackgroundResource(R.drawable.ic_mic_on)
            false -> binding.ivAudio.setBackgroundResource(R.drawable.ic_mic_off)
        }
        callViewModel.setLocalAudio(!callViewModel.isEnableLocalAudio)
    }

    private fun endCall() {
        // TODO endCall 로직
    }

    private fun checkVideoCallPermissions() {
        when (checkPermissionsGranted(permissions)){
            true -> {
                setTalkPlusCall()
                login()
            }
            false -> requestVideoCallPermission()
        }
    }

    private fun setTalkPlusCall() = with(intent) {
        when (intent.hasExtra(INTENT_EXTRA_NOTIFICATION_PAYLOAD)) {
            true -> {
                intentSerializable(
                    INTENT_EXTRA_NOTIFICATION_PAYLOAD,
                    TPNotificationPayload::class.java
                )?.let { payload ->
                    callViewModel.setTalkplusCall(
                        TalkPlusCall(
                            callerId = payload.callerId,
                            calleeId = payload.calleeId,
                            channelId = payload.channelId,
                            uuid = payload.uuid
                        )
                    )
                } ?: showToast("payload is null")
            }
            false -> {
                callViewModel.setTalkplusCall(
                    TalkPlusCall(
                        callerId = getStringExtra(INTENT_EXTRA_CALLER_ID) ?: "",
                        calleeId = getStringExtra(INTENT_EXTRA_CALLEE_ID) ?: "",
                        channelId = TEST_CHANNEL_ID
                    )
                )
            }
        }
    }

    private fun login() {
        callViewModel.login(
            userId = callViewModel.talkPlusCall.callerId,
            userName = callViewModel.talkPlusCall.callerId
        )
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
        checkVideoCallPermissions()
    }


    companion object {
        const val INTENT_EXTRA_CALLEE_ID = "extra_callee_id"
        const val INTENT_EXTRA_CALLER_ID = "extra_caller_id"
        const val INTENT_EXTRA_NOTIFICATION_PAYLOAD = "extra_notificationPayload"

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