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
import com.neptune.talkplus_calls_android_sample.extensions.closeNotification
import com.neptune.talkplus_calls_android_sample.extensions.intentSerializable
import com.neptune.talkplus_calls_android_sample.extensions.requirePermission
import com.neptune.talkplus_calls_android_sample.extensions.showToast
import com.neptune.talkpluscallsandroid.webrtc.model.EndCallStatus
import com.neptune.talkpluscallsandroid.webrtc.model.TalkPlusCall
import core.TPWebRTCClient
import events.DirectCallListener
import io.talkplus.entity.user.TPNotificationPayload
import kotlinx.coroutines.launch
import model.EndCallInfo
import org.webrtc.PeerConnection

class CallActivity : AppCompatActivity() {
    private val binding: ActivityCallBinding by lazy { ActivityCallBinding.inflate(layoutInflater) }
    private lateinit var tpWebRTCClient: TPWebRTCClient
    private val callViewModel: CallViewModel by lazy { ViewModelProvider(this)[CallViewModel::class.java] }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        checkVideoCallPermissions()
        observeCallUiState()
        setClickListener()
    }

    private fun observeCallUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                callViewModel.callState.collect { callUiState ->
                    handleCallUiState(callUiState)
                }
            }
        }
    }

    private fun handleCallUiState(callUiState: CallUiState) {
        when (callUiState) {
            is CallUiState.Login -> callViewModel.enablePushNotification()
            is CallUiState.JoinChannel -> startConnect()
            is CallUiState.EnablePush -> callViewModel.getFCMToken()
            is CallUiState.RegisterToken -> callViewModel.joinChannel()
            is CallUiState.Failed -> showToast(callUiState.failResult.toString())
        }
    }

    private fun setClickListener() = with(binding) {
        ivAudio.setOnClickListener { toggleAudio() }
        ivVideo.setOnClickListener { toggleVideo() }
        ivEndCall.setOnClickListener { tpWebRTCClient.endCall() }
    }

    private fun toggleVideo() {
        when (callViewModel.isEnableLocalVideo) {
            true -> binding.ivVideo.setBackgroundResource(R.drawable.ic_video_off)
            false -> binding.ivVideo.setBackgroundResource(R.drawable.ic_video_on)
        }
        tpWebRTCClient.enableVideo(!callViewModel.isEnableLocalVideo)
        callViewModel.setLocalVideo(!callViewModel.isEnableLocalVideo)
    }

    private fun toggleAudio() {
        when (callViewModel.isEnableLocalAudio) {
            true -> binding.ivAudio.setBackgroundResource(R.drawable.ic_mic_on)
            false -> binding.ivAudio.setBackgroundResource(R.drawable.ic_mic_off)
        }
        tpWebRTCClient.enableAudio(!callViewModel.isEnableLocalAudio)
        callViewModel.setLocalAudio(!callViewModel.isEnableLocalAudio)
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
                            uuid = payload.uuid,
                            sdp = payload.sdp
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
        tpWebRTCClient = setTpWebRTCClient()
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
    }

    private fun setTpWebRTCClient(): TPWebRTCClient {
        return TPWebRTCClient(
            callViewModel.talkPlusCall,
            directCallListener
        )
    }

    private fun startConnect() {
        with(tpWebRTCClient) {
            setLocalVideo(binding.surfaceLocal)
            setRemoteVideo(binding.surfaceRemote)
        }

        if (intent.hasExtra(INTENT_EXTRA_NOTIFICATION_PAYLOAD)) {
            closeNotification()
            tpWebRTCClient.acceptCall()
        } else {
            tpWebRTCClient.makeCall(callViewModel.talkPlusCall)
        }
    }

    private val directCallListener: DirectCallListener = object : DirectCallListener {
        override fun inComing(talkPlusCall: TalkPlusCall) {

        }

        override fun connected(talkPlusCall: TalkPlusCall) {
            showToast("연결 성공!")
            setSpeakerPhone()
        }

        override fun ended(endCallInfo: EndCallInfo) {
            when (endCallInfo.endReasonCode) {
                EndCallStatus.UNKNOWN.code -> { showToast("비정상 종료")}
                EndCallStatus.COMPLETED.code -> { showToast("정상 종료") }
                EndCallStatus.DECLINED.code -> { showToast("callee가 거절") }
                EndCallStatus.CANCELED.code -> { showToast("caller가 거절") }
            }
        }

        override fun failed(talkPlusCall: TalkPlusCall) {

        }

        override fun stateChanged(talkPlusCall: TalkPlusCall, state: PeerConnection.IceConnectionState) {

        }

        override fun error() {

        }
    }

    fun setSpeakerPhone() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.isSpeakerphoneOn = true
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        volumeControlStream = AudioManager.STREAM_VOICE_CALL
    }

    companion object {
        private const val TAG = "CallActivity!!"

        const val INTENT_EXTRA_CALLEE_ID = "extra_callee_id"
        const val INTENT_EXTRA_CALLER_ID = "extra_caller_id"
        const val INTENT_EXTRA_NOTIFICATION_PAYLOAD = "extra_notification_payload"

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